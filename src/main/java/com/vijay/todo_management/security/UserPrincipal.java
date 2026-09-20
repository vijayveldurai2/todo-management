package com.vijay.todo_management.security;

import com.vijay.todo_management.entity.User;
import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String username;
    private final String jti;
    private final boolean active;

    public static UserPrincipal fromUser(User user, String jti) {
        return UserPrincipal.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .jti(jti)
                .active(Boolean.TRUE.equals(user.getIsActive()))
                .build();
    }

    public static UserPrincipal fromClaims(Claims claims) {
        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);
        String username = claims.get("username", String.class);
        String jti = claims.getId() != null ? claims.getId() : claims.get("jti", String.class);

        return UserPrincipal.builder()
                .id(userId)
                .email(email)
                .username(username)
                .jti(jti)
                .active(true)
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return username != null ? username : (email != null ? email : (id != null ? id.toString() : ""));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
