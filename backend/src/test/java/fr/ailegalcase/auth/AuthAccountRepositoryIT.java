package fr.ailegalcase.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AuthAccountRepositoryIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthAccountRepository authAccountRepository;

    private User savedUser() {
        User user = new User();
        user.setEmail("john@example.com");
        user.setStatus("ACTIVE");
        return userRepository.save(user);
    }

    // I-01 : findByProviderAndProviderUserId — existant
    @Test
    void findByProviderAndProviderUserId_existing_returnsAccount() {
        User user = savedUser();

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-sub-123");
        authAccountRepository.save(account);

        Optional<AuthAccount> result = authAccountRepository
                .findByProviderAndProviderUserId("GOOGLE", "google-sub-123");

        assertThat(result).isPresent();
        assertThat(result.get().getProviderUserId()).isEqualTo("google-sub-123");
    }

    // I-02 : findByProviderAndProviderUserId — inconnu
    @Test
    void findByProviderAndProviderUserId_unknown_returnsEmpty() {
        Optional<AuthAccount> result = authAccountRepository
                .findByProviderAndProviderUserId("GOOGLE", "unknown-sub");

        assertThat(result).isEmpty();
    }

    // I-03 : UserRepository save + findById
    @Test
    void userRepository_saveAndFind_persistsUser() {
        User user = new User();
        user.setEmail("jane@example.com");
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setStatus("ACTIVE");

        User saved = userRepository.save(user);
        Optional<User> found = userRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("jane@example.com");
        assertThat(found.get().getStatus()).isEqualTo("ACTIVE");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }
}
