package fr.ailegalcase.testsupport;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Nettoyage déterministe de la base H2 de test AVANT chaque test — F-245 / SF-245-01.
 *
 * <p>Les 171 tests d'intégration partageaient une base H2 mémoire nommée
 * ({@code jdbc:h2:mem:legalcasedb}, maintenue vivante par {@code DB_CLOSE_DELAY=-1})
 * dans un fork JVM unique. Le nettoyage manuel ({@code repository.deleteAll()} dans
 * un {@code @BeforeEach}) n'était présent que dans 30 IT sur 171, avec des
 * sous-ensembles de tables et des ordres de suppression différents : les données
 * s'accumulaient de test en test et provoquaient des violations de contraintes,
 * puis une cascade d'échecs de chargement de contexte. {@code mvnw verify} global
 * n'était ni fiable ni déterministe.
 *
 * <p>Cette extension est enregistrée automatiquement pour tous les tests
 * (cf. {@code junit-platform.properties} +
 * {@code META-INF/services/org.junit.jupiter.api.extension.Extension}). Avant
 * chaque test disposant d'un contexte Spring, elle vide les tables métier de la
 * base H2 ({@code SET REFERENTIAL_INTEGRITY FALSE} puis {@code TRUNCATE TABLE}).
 * Le nettoyage est centralisé : il ne dépend plus du fait que chaque IT pense à
 * nettoyer dans le bon ordre.
 *
 * <p>Les tables seedées par Liquibase (données de référence :
 * {@code legal_referentials}, {@code decision_tool_visibility_rules}…) ne sont
 * jamais tronquées — sans quoi tous les outils décisionnels qui les consultent
 * échoueraient. Elles sont détectées au premier nettoyage : toute table non vide
 * juste après Liquibase (donc avant l'exécution du premier test) contient des
 * données de référence et est préservée pour la durée du fork JVM. La détection
 * est fiable car aucun IT n'insère de données métier dans un {@code @BeforeAll}
 * (vérifié F-245). Les tables techniques de Liquibase ({@code DATABASECHANGELOG*})
 * sont préservées de la même façon.
 *
 * <p>Le nettoyage s'exécute en {@link BeforeEachCallback} (et non après le test) :
 * il est ainsi hors de toute transaction de test ouverte (pas de conflit de verrou
 * avec un {@code @DataJpaTest} transactionnel) et garantit un état propre quel que
 * soit l'ordre des tests ou un échec du test précédent.
 */
public class DatabaseCleanupExtension implements BeforeEachCallback {

    private static final Logger log = LoggerFactory.getLogger(DatabaseCleanupExtension.class);

    /** Tables techniques de Liquibase — jamais tronquées. */
    private static final List<String> SYSTEM_TABLES = List.of("DATABASECHANGELOG", "DATABASECHANGELOGLOCK");

    /**
     * Tables de référence (non vides juste après Liquibase) à préserver — capturé
     * une seule fois, au premier nettoyage, partagé pour tout le fork JVM.
     * {@code volatile} + synchronisation par prudence ; les tests s'exécutent
     * en réalité dans un fork mono-thread ({@code forkCount=1}).
     */
    private static volatile Set<String> seededTables;

    @Override
    public void beforeEach(ExtensionContext context) {
        Optional<Class<?>> testClass = context.getTestClass();
        if (testClass.isEmpty() || !isSpringTest(testClass.get())) {
            // Test unitaire pur sans contexte Spring → rien à nettoyer.
            return;
        }
        ApplicationContext applicationContext;
        try {
            applicationContext = SpringExtension.getApplicationContext(context);
        } catch (Exception e) {
            // Contexte Spring indisponible (échec de chargement déjà signalé par
            // Spring) → ne pas ajouter de bruit avec une seconde erreur.
            return;
        }
        DataSource dataSource = applicationContext.getBeanProvider(DataSource.class).getIfUnique();
        if (dataSource == null) {
            return;
        }
        cleanBusinessTables(dataSource);
    }

    private void cleanBusinessTables(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (!"H2".equalsIgnoreCase(product)) {
                // Garde-fou : ne jamais TRUNCATE une base non-H2 (ex. PostgreSQL).
                log.warn("DatabaseCleanupExtension désactivée : base non-H2 ({}).", product);
                return;
            }
            List<String> tables = applicationTables(connection);
            Set<String> preserved = preservedTables(connection, tables);
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET REFERENTIAL_INTEGRITY FALSE");
                for (String table : tables) {
                    if (!preserved.contains(table)) {
                        statement.execute("TRUNCATE TABLE \"" + table + "\"");
                    }
                }
                statement.execute("SET REFERENTIAL_INTEGRITY TRUE");
            }
        } catch (SQLException e) {
            // Le bruit doit remonter : un échec de nettoyage fait échouer le test.
            throw new IllegalStateException("Échec du nettoyage de la base de test", e);
        }
    }

    /**
     * Tables de référence à préserver (jamais tronquées) : capturées une fois, au
     * premier nettoyage, comme l'ensemble des tables non vides juste après
     * Liquibase — soit les données de référence seedées par les migrations.
     */
    private Set<String> preservedTables(Connection connection, List<String> tables) throws SQLException {
        Set<String> captured = seededTables;
        if (captured != null) {
            return captured;
        }
        synchronized (DatabaseCleanupExtension.class) {
            if (seededTables == null) {
                Set<String> nonEmpty = new HashSet<>();
                for (String table : tables) {
                    if (rowCount(connection, table) > 0) {
                        nonEmpty.add(table);
                    }
                }
                seededTables = Set.copyOf(nonEmpty);
                log.info("DatabaseCleanupExtension — {} table(s) de référence préservée(s) : {}",
                        nonEmpty.size(), new TreeSet<>(nonEmpty));
            }
            return seededTables;
        }
    }

    private long rowCount(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM \"" + table + "\"")) {
            return resultSet.next() ? resultSet.getLong(1) : 0L;
        }
    }

    private List<String> applicationTables(Connection connection) throws SQLException {
        List<String> tables = new ArrayList<>();
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                String name = resultSet.getString("TABLE_NAME");
                if (!isSystemTable(name)) {
                    tables.add(name);
                }
            }
        }
        return tables;
    }

    /** Vrai si la table est une table technique de Liquibase (à préserver). */
    static boolean isSystemTable(String tableName) {
        return SYSTEM_TABLES.contains(tableName.toUpperCase(Locale.ROOT));
    }

    /**
     * Vrai si la classe de test est pilotée par Spring, c.-à-d. méta-annotée
     * {@code @ExtendWith(SpringExtension.class)} ({@code @SpringBootTest},
     * {@code @DataJpaTest}, {@code @WebMvcTest}…). Détecté par réflexion, sans
     * déclencher de chargement de contexte.
     */
    static boolean isSpringTest(Class<?> testClass) {
        return AnnotationSupport.findRepeatableAnnotations(testClass, ExtendWith.class).stream()
                .map(ExtendWith::value)
                .flatMap(Arrays::stream)
                .anyMatch(SpringExtension.class::isAssignableFrom);
    }
}
