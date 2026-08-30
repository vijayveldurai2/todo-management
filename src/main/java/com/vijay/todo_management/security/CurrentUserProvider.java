package com.vijay.todo_management.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class CurrentUserProvider {

    public static Optional<UserPrincipal> getCurrentUserPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        if (auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static UserPrincipal requireCurrentUserPrincipal() {
        return getCurrentUserPrincipal()
                .orElseThrow(() -> new RuntimeException("Unauthorized: No authenticated user found in security context"));
    }

    public static Optional<UUID> getCurrentUserIdOpt() {
        return getCurrentUserPrincipal().map(UserPrincipal::getId);
    }

    public static UUID getCurrentUserId() {
        return requireCurrentUserPrincipal().getId();
    }

    public UUID getUserId() {
        return getCurrentUserId();
    }

    public UserPrincipal getPrincipal() {
        return requireCurrentUserPrincipal();
    }
}
