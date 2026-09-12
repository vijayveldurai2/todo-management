package com.vijay.todo_management.service;
import com.vijay.todo_management.entity.*;
import com.vijay.todo_management.exception.*;
import com.vijay.todo_management.repository.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AttachmentSettingsServiceTest {
    @Test void onlyWorkspaceSuperAdminCanSetLimitWithinCap() {
        var workspaces=mock(WorkspaceRepository.class); var members=mock(WorkspaceMemberRepository.class);
        var service=new AttachmentSettingsService(workspaces,members);
        UUID ws=UUID.randomUUID(),user=UUID.randomUUID();
        var member=new WorkspaceMember(); member.setRole(WorkspaceMember.Role.USER);
        when(members.findByWorkspace_IdAndUser_Id(ws,user)).thenReturn(Optional.of(member));
        assertThrows(ForbiddenException.class,()->service.update(ws,user,10L));
        member.setRole(WorkspaceMember.Role.SUPER_ADMIN);
        assertThrows(BadRequestException.class,()->service.update(ws,user,0L));
        assertThrows(BadRequestException.class,()->service.update(ws,user,26214401L));
        assertThrows(BadRequestException.class,()->service.update(ws,user,null));
        Workspace workspace=new Workspace(); when(workspaces.findById(ws)).thenReturn(Optional.of(workspace));
        assertEquals(10485760,service.get(ws,user).maxFileBytes());
        assertEquals(1024,service.update(ws,user,1024L).maxFileBytes());
        assertEquals(1024,workspace.getAttachmentMaxBytes());
    }
    @Test void outsidersCannotReadSettings() {
        var service=new AttachmentSettingsService(mock(WorkspaceRepository.class),mock(WorkspaceMemberRepository.class));
        assertThrows(ForbiddenException.class,()->service.get(UUID.randomUUID(),UUID.randomUUID()));
    }
}
