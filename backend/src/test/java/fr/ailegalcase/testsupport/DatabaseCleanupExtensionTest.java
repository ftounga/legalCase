package fr.ailegalcase.testsupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Tests unitaires de {@link DatabaseCleanupExtension} — F-245 / SF-245-01. */
class DatabaseCleanupExtensionTest {

    @Test
    void isSystemTable_reconnait_les_tables_techniques_liquibase() {
        assertThat(DatabaseCleanupExtension.isSystemTable("DATABASECHANGELOG")).isTrue();
        assertThat(DatabaseCleanupExtension.isSystemTable("DATABASECHANGELOGLOCK")).isTrue();
        // Insensible à la casse.
        assertThat(DatabaseCleanupExtension.isSystemTable("databasechangelog")).isTrue();
    }

    @Test
    void isSystemTable_faux_pour_les_tables_applicatives() {
        assertThat(DatabaseCleanupExtension.isSystemTable("USERS")).isFalse();
        assertThat(DatabaseCleanupExtension.isSystemTable("CASE_FILE")).isFalse();
        assertThat(DatabaseCleanupExtension.isSystemTable("WORKSPACE")).isFalse();
    }

    @Test
    void isSpringTest_vrai_pour_une_classe_spring() {
        // @SpringBootTest est méta-annoté @ExtendWith(SpringExtension.class).
        assertThat(DatabaseCleanupExtension.isSpringTest(SpringBootTestSample.class)).isTrue();
        // @ExtendWith(SpringExtension.class) direct.
        assertThat(DatabaseCleanupExtension.isSpringTest(ExtendWithSpringSample.class)).isTrue();
    }

    @Test
    void isSpringTest_faux_pour_un_test_unitaire_pur() {
        assertThat(DatabaseCleanupExtension.isSpringTest(String.class)).isFalse();
        assertThat(DatabaseCleanupExtension.isSpringTest(DatabaseCleanupExtensionTest.class)).isFalse();
    }

    @Test
    void beforeEach_no_op_sans_contexte_spring() {
        ExtensionContext context = mock(ExtensionContext.class);
        when(context.getTestClass()).thenReturn(Optional.<Class<?>>of(String.class));

        // Test unitaire pur → aucune exception, aucun accès base.
        assertThatCode(() -> new DatabaseCleanupExtension().beforeEach(context))
                .doesNotThrowAnyException();
    }

    @SpringBootTest
    private static class SpringBootTestSample {
    }

    @ExtendWith(SpringExtension.class)
    private static class ExtendWithSpringSample {
    }
}
