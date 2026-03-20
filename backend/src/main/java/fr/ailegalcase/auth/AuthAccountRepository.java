package fr.ailegalcase.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthAccountRepository extends JpaRepository<AuthAccount, UUID> {

    Optional<AuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

    void deleteByUser(User user);
}
