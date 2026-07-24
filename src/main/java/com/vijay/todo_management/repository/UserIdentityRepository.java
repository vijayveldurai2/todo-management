package com.vijay.todo_management.repository;

import com.vijay.todo_management.entity.UserIdentity;
import com.vijay.todo_management.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {
    Optional<UserIdentity> findByProviderAndProviderSubject(AuthProvider provider, String providerSubject);
}
