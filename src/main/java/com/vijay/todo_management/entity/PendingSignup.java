package com.vijay.todo_management.entity;

import com.vijay.todo_management.enums.LoginMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pending_signups")
public class PendingSignup {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(length = 36, updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** BCrypt hash; nullable for OAuth-only signup. */
    @Column(length = 255)
    private String password;

    @Column(length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_login_method", nullable = false, length = 20)
    private LoginMethod primaryLoginMethod = LoginMethod.PASSWORD;

    @Column(name = "oauth_provider_subject", length = 255)
    private String oauthProviderSubject;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
