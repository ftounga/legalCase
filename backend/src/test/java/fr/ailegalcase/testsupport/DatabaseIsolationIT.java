package fr.ailegalcase.testsupport;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prouve l'isolation test-à-test garantie par {@link DatabaseCleanupExtension}
 * — F-245 / SF-245-01, critère d'acceptation CA7.
 *
 * <p>Le premier test insère des lignes SANS jamais les nettoyer ; le second
 * vérifie que la base est vide à son démarrage. Le seul mécanisme entre les
 * deux est le {@code TRUNCATE} exécuté par l'extension avant chaque test : si le
 * second test voyait des lignes résiduelles, c'est que l'isolation ne
 * fonctionne pas.
 */
@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
class DatabaseIsolationIT {

    @Autowired
    private UserRepository userRepository;

    @Test
    @Order(1)
    void test1_insere_des_lignes_sans_les_nettoyer() {
        userRepository.save(newUser("isolation-a@legalcase.test"));
        userRepository.save(newUser("isolation-b@legalcase.test"));

        assertThat(userRepository.count()).isEqualTo(2);
    }

    @Test
    @Order(2)
    void test2_demarre_sur_une_base_vide() {
        // Aucun nettoyage explicite dans test1 : si la base n'est pas vide ici,
        // le TRUNCATE inter-test de DatabaseCleanupExtension ne fonctionne pas.
        assertThat(userRepository.count()).isZero();
    }

    private User newUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setStatus("ACTIVE");
        return user;
    }
}
