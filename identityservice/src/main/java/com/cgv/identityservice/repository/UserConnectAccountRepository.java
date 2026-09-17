package com.cgv.identityservice.repository;

import com.cgv.identityservice.entity.UserConnectAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserConnectAccountRepository extends JpaRepository<UserConnectAccount, UUID> {
    Optional<UserConnectAccount> findByProviderAndProviderId(String provider, String providerId);
    List<UserConnectAccount> findByUserId(String userId);
    boolean existsByProviderAndProviderId(String provider, String providerId);
}
