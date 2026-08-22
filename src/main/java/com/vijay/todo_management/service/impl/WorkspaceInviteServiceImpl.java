package com.vijay.todo_management.service.impl;

import com.vijay.todo_management.dto.WorkspaceInviteDto;
import com.vijay.todo_management.dto.WorkspaceInviteRequestDto;
import com.vijay.todo_management.dto.WorkspaceMemberDto;
import com.vijay.todo_management.entity.User;
import com.vijay.todo_management.entity.Workspace;
import com.vijay.todo_management.entity.WorkspaceInvite;
import com.vijay.todo_management.entity.WorkspaceMember;
import com.vijay.todo_management.repository.UserRepository;
import com.vijay.todo_management.repository.WorkspaceInviteRepository;
import com.vijay.todo_management.repository.WorkspaceMemberRepository;
import com.vijay.todo_management.repository.WorkspaceRepository;
import com.vijay.todo_management.service.WorkspaceInviteService;
import com.vijay.todo_management.service.WorkspaceService;
import com.vijay.todo_management.util.TokenHasher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkspaceInviteServiceImpl implements WorkspaceInviteService {

    @Autowired
    private WorkspaceInviteRepository workspaceInviteRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;
    
    @Autowired
    private WorkspaceService workspaceService;

    private static final int INVITE_EXPIRY_DAYS = 7;
    private final SecureRandom secureRandom = new SecureRandom();

    private WorkspaceInviteDto mapToDto(WorkspaceInvite invite) {
        WorkspaceInviteDto dto = new WorkspaceInviteDto();
        dto.setId(invite.getId());
        dto.setWorkspaceId(invite.getWorkspace().getId());
        dto.setWorkspaceName(invite.getWorkspace().getName());
        dto.setWorkspaceDescription(invite.getWorkspace().getDescription());
        dto.setEmail(invite.getEmail());
        dto.setInvitedBy(invite.getInvitedBy().getId());
        dto.setRole(invite.getRole().name());
        dto.setStatus(invite.getStatus().name());
        dto.setExpiresAt(invite.getExpiresAt());
        dto.setCreatedAt(invite.getCreatedAt());
        return dto;
    }

    private void expireIfStale(WorkspaceInvite invite) {
        if (invite.getStatus() == WorkspaceInvite.Status.PENDING && invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            invite.setStatus(WorkspaceInvite.Status.EXPIRED);
            workspaceInviteRepository.save(invite);
        }
    }

    @Override
    @Transactional
    public WorkspaceInviteDto createInvite(UUID workspaceId, UUID inviterId, WorkspaceInviteRequestDto req) {
        // TODO: enforce SUPER_ADMIN once auth is wired
        
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("Workspace not found: " + workspaceId));
        
        User inviter = userRepository.findById(inviterId)
                .orElseThrow(() -> new RuntimeException("Inviter not found: " + inviterId));

        String targetEmail = req.getEmail();

        // Prevent inviting an active member
        Optional<User> existingUser = userRepository.findByEmail(targetEmail);
        if (existingUser.isPresent()) {
            boolean isMember = workspaceMemberRepository.existsByWorkspace_IdAndUser_Id(workspaceId, existingUser.get().getId());
            if (isMember) {
                throw new RuntimeException("User with email " + targetEmail + " is already an active member of this workspace.");
            }
        }

        // Revoke any existing PENDING invite for the same email and workspace
        workspaceInviteRepository.findByWorkspace_IdAndEmailAndStatus(workspaceId, targetEmail, WorkspaceInvite.Status.PENDING)
                .ifPresent(existingInvite -> {
                    existingInvite.setStatus(WorkspaceInvite.Status.REVOKED);
                    workspaceInviteRepository.save(existingInvite);
                });

        // Generate token
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String tokenHash = TokenHasher.sha256(rawToken);

        WorkspaceInvite invite = new WorkspaceInvite();
        invite.setWorkspace(workspace);
        invite.setEmail(targetEmail);
        invite.setInvitedBy(inviter);
        invite.setRole(WorkspaceMember.Role.valueOf(req.getRole().toUpperCase()));
        invite.setStatus(WorkspaceInvite.Status.PENDING);
        invite.setTokenHash(tokenHash);
        invite.setExpiresAt(LocalDateTime.now().plusDays(INVITE_EXPIRY_DAYS));
        
        WorkspaceInvite saved = workspaceInviteRepository.save(invite);

        // TODO: replace with real email send
        System.out.println("Invite link (do not log in prod): http://localhost:5173/invites/accept?token=" + rawToken);

        return mapToDto(saved);
    }

    @Override
    public List<WorkspaceInviteDto> getPendingInvites(UUID workspaceId) {
        List<WorkspaceInvite> invites = workspaceInviteRepository.findByWorkspace_IdAndStatus(workspaceId, WorkspaceInvite.Status.PENDING);
        invites.forEach(this::expireIfStale);
        
        return invites.stream()
                .filter(i -> i.getStatus() == WorkspaceInvite.Status.PENDING)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkspaceInviteDto> getMyPendingInvites(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        List<WorkspaceInvite> invites = workspaceInviteRepository.findByEmailAndStatus(user.getEmail(), WorkspaceInvite.Status.PENDING);
        invites.forEach(this::expireIfStale);

        return invites.stream()
                .filter(i -> i.getStatus() == WorkspaceInvite.Status.PENDING)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WorkspaceMemberDto acceptInvite(UUID inviteId, UUID acceptingUserId) {
        WorkspaceInvite invite = workspaceInviteRepository.findById(inviteId)
                .orElseThrow(() -> new RuntimeException("Invite not found: " + inviteId));
        
        User acceptingUser = userRepository.findById(acceptingUserId)
                .orElseThrow(() -> new RuntimeException("User not found: " + acceptingUserId));

        if (!acceptingUser.getEmail().equals(invite.getEmail())) {
            throw new RuntimeException("User email does not match invite email.");
        }

        expireIfStale(invite);

        if (invite.getStatus() != WorkspaceInvite.Status.PENDING) {
            throw new RuntimeException("Invite is no longer pending (status: " + invite.getStatus() + ")");
        }

        invite.setStatus(WorkspaceInvite.Status.ACCEPTED);
        invite.setAcceptedAt(LocalDateTime.now());
        workspaceInviteRepository.save(invite);

        WorkspaceMemberDto memberDto = workspaceService.addMember(invite.getWorkspace().getId(), acceptingUserId);
        
        // update role if it's not default
        if (invite.getRole() != WorkspaceMember.Role.USER) {
            memberDto = workspaceService.changeMemberRole(invite.getWorkspace().getId(), acceptingUserId, invite.getRole().name());
        }

        return memberDto;
    }

    @Override
    @Transactional
    public void declineInvite(UUID inviteId, UUID decliningUserId) {
        WorkspaceInvite invite = workspaceInviteRepository.findById(inviteId)
                .orElseThrow(() -> new RuntimeException("Invite not found: " + inviteId));
        
        User decliningUser = userRepository.findById(decliningUserId)
                .orElseThrow(() -> new RuntimeException("User not found: " + decliningUserId));

        if (!decliningUser.getEmail().equals(invite.getEmail())) {
            throw new RuntimeException("User email does not match invite email.");
        }

        expireIfStale(invite);

        if (invite.getStatus() != WorkspaceInvite.Status.PENDING) {
            throw new RuntimeException("Invite is no longer pending (status: " + invite.getStatus() + ")");
        }

        invite.setStatus(WorkspaceInvite.Status.DECLINED);
        workspaceInviteRepository.save(invite);
    }

    @Override
    @Transactional
    public void revokeInvite(UUID inviteId) {
        WorkspaceInvite invite = workspaceInviteRepository.findById(inviteId)
                .orElseThrow(() -> new RuntimeException("Invite not found: " + inviteId));

        if (invite.getStatus() != WorkspaceInvite.Status.PENDING) {
            throw new RuntimeException("Only pending invites can be revoked");
        }

        invite.setStatus(WorkspaceInvite.Status.REVOKED);
        workspaceInviteRepository.save(invite);
    }

    @Override
    @Transactional
    public WorkspaceMemberDto acceptInviteByToken(String rawToken, UUID acceptingUserId) {
        String tokenHash = TokenHasher.sha256(rawToken);
        WorkspaceInvite invite = workspaceInviteRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        expireIfStale(invite);

        if (invite.getStatus() != WorkspaceInvite.Status.PENDING) {
            throw new RuntimeException("Invite is no longer pending (status: " + invite.getStatus() + ")");
        }

        User acceptingUser = userRepository.findById(acceptingUserId)
                .orElseThrow(() -> new RuntimeException("User not found: " + acceptingUserId));

        // TODO: verify acceptingUser.email matches invite.email

        invite.setStatus(WorkspaceInvite.Status.ACCEPTED);
        invite.setAcceptedAt(LocalDateTime.now());
        workspaceInviteRepository.save(invite);

        WorkspaceMemberDto memberDto = workspaceService.addMember(invite.getWorkspace().getId(), acceptingUserId);
        
        if (invite.getRole() != WorkspaceMember.Role.USER) {
            memberDto = workspaceService.changeMemberRole(invite.getWorkspace().getId(), acceptingUserId, invite.getRole().name());
        }

        return memberDto;
    }

    @Override
    @Transactional
    public void declineInviteByToken(String rawToken) {
        String tokenHash = TokenHasher.sha256(rawToken);
        WorkspaceInvite invite = workspaceInviteRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        expireIfStale(invite);

        if (invite.getStatus() != WorkspaceInvite.Status.PENDING) {
            throw new RuntimeException("Invite is no longer pending (status: " + invite.getStatus() + ")");
        }

        invite.setStatus(WorkspaceInvite.Status.DECLINED);
        workspaceInviteRepository.save(invite);
    }
}
