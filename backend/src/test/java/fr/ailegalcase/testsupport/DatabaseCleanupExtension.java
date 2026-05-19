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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
 * chaque test disposant d'un contexte Spring, elle vide les données de test de
 * la base H2 ({@code SET REFERENTIAL_INTEGRITY FALSE}, puis {@code TRUNCATE TABLE}
 * des tables purement métier et {@code DELETE} ciblé des lignes de test sur les
 * tables de référence — voir ci-dessous). Le nettoyage est centralisé : il ne
 * dépend plus du fait que chaque IT pense à nettoyer dans le bon ordre.
 *
 * <p>Les données de référence seedées par Liquibase ({@code legal_referentials},
 * {@code decision_tool_visibility_rules}…) doivent survivre — sans quoi tous les
 * outils décisionnels qui les consultent échoueraient. La préservation se fait
 * <b>au niveau ligne</b>, et non au niveau table : au premier nettoyage (avant
 * l'exécution du premier test), l'extension capture les valeurs de clé primaire
 * des lignes présentes dans chaque table. Le nettoyage supprime ensuite les
 * lignes de test (clé hors snapshot) et conserve les lignes seedées. C'est
 * indispensable pour les tables à la fois seedées <em>et</em> écrites par des
 * tests (ex. {@code legal_referentials}, où un test peut insérer des entrées
 * temporaires) : une préservation au niveau table laisserait fuir ces écritures
 * d'un test à l'autre. La détection est fiable car aucun IT n'insère de données
 * dans un {@code @BeforeAll} (vérifié F-245). Les tables techniques de Liquibase
 * ({@code DATABASECHANGELOG*}) sont exclues d'emblée.
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
     * Snapshot des données de référence seedées par Liquibase, capturé une seule fois
     * (au premier nettoyage, avant tout test) et partagé pour le fork JVM. Pour chaque
     * table contenant des lignes juste après Liquibase, on retient la valeur de clé
     * primaire de ces lignes. Le nettoyage supprime alors les lignes de test (clé hors
     * snapshot) tout en conservant les lignes de référence — indispensable pour les
     * tables à la fois seedées ET écrites par des tests (ex. {@code legal_referentials}).
     * {@code volatile} + synchronisation par prudence ; les tests s'exécutent en réalité
     * dans un fork mono-thread ({@code forkCount=1}).
     */
    private static volatile Map<String, SeedTable> seedTables;

    /**
     * Une table contenant des données de référence. {@code primaryKeyColumn} est la
     * colonne clé primaire mono-colonne ; {@code null} si la table n'a pas de clé
     * primaire simple exploitable, auquel cas la table est préservée intégralement
     * (cas de repli conservateur). {@code seedKeys} liste les valeurs de clé primaire
     * des lignes seedées par Liquibase.
     */
    private record SeedTable(String primaryKeyColumn, List<Object> seedKeys) {}

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
            Map<String, SeedTable> seeds = seedTables(connection, tables);
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET REFERENTIAL_INTEGRITY FALSE");
                for (String table : tables) {
                    SeedTable seed = seeds.get(table);
                    if (seed == null) {
                        // Table sans donnée de référence → vidée intégralement.
                        statement.execute("TRUNCATE TABLE \"" + table + "\"");
                    } else if (seed.primaryKeyColumn() != null) {
                        // Table de référence : on supprime les lignes de test (clé hors
                        // snapshot) et on conserve les lignes seedées par Liquibase.
                        deleteNonSeedRows(connection, table, seed);
                    }
                    // seed != null && primaryKeyColumn == null → table de référence
                    // sans PK simple : préservée intégralement (repli conservateur).
                }
                statement.execute("SET REFERENTIAL_INTEGRITY TRUE");
            }
        } catch (SQLException e) {
            // Le bruit doit remonter : un échec de nettoyage fait échouer le test.
            throw new IllegalStateException("Échec du nettoyage de la base de test", e);
        }
    }

    /**
     * Capture, une seule fois, le snapshot des données de référence : pour chaque table
     * non vide juste après Liquibase (donc avant tout test — aucun IT n'insère en
     * {@code @BeforeAll}, vérifié F-245), la colonne clé primaire et les valeurs de clé
     * des lignes seedées.
     */
    private Map<String, SeedTable> seedTables(Connection connection, List<String> tables) throws SQLException {
        Map<String, SeedTable> captured = seedTables;
        if (captured != null) {
            return captured;
        }
        synchronized (DatabaseCleanupExtension.class) {
            if (seedTables == null) {
                Map<String, SeedTable> map = new HashMap<>();
                for (String table : tables) {
                    String pk = singleColumnPrimaryKey(connection, table);
                    if (pk == null) {
                        // Pas de PK simple : préservée intégralement si elle est non vide.
                        if (isNonEmpty(connection, table)) {
                            map.put(table, new SeedTable(null, List.of()));
                        }
                        continue;
                    }
                    List<Object> keys = primaryKeyValues(connection, table, pk);
                    if (!keys.isEmpty()) {
                        map.put(table, new SeedTable(pk, keys));
                    }
                }
                seedTables = Map.copyOf(map);
                log.info("DatabaseCleanupExtension — {} table(s) de référence : {}",
                        map.size(), new TreeSet<>(map.keySet()));
            }
            return seedTables;
        }
    }

    /** Colonne clé primaire mono-colonne de la table, ou {@code null} si PK absente / composite. */
    private String singleColumnPrimaryKey(Connection connection, String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (ResultSet rs = connection.getMetaData()
                .getPrimaryKeys(connection.getCatalog(), "PUBLIC", table)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }
        return columns.size() == 1 ? columns.get(0) : null;
    }

    /** Valeurs de la colonne clé primaire {@code pkColumn} de la table (toutes les lignes). */
    private List<Object> primaryKeyValues(Connection connection, String table, String pkColumn)
            throws SQLException {
        List<Object> values = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT \"" + pkColumn + "\" FROM \"" + table + "\"")) {
            while (resultSet.next()) {
                values.add(resultSet.getObject(1));
            }
        }
        return values;
    }

    /** Vrai si la table contient au moins une ligne. */
    private boolean isNonEmpty(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT 1 FROM \"" + table + "\" LIMIT 1")) {
            return resultSet.next();
        }
    }

    /** Supprime de la table les lignes dont la clé primaire n'est pas dans le snapshot seed. */
    private void deleteNonSeedRows(Connection connection, String table, SeedTable seed)
            throws SQLException {
        String placeholders = String.join(",", Collections.nCopies(seed.seedKeys().size(), "?"));
        String sql = "DELETE FROM \"" + table + "\" WHERE \"" + seed.primaryKeyColumn()
                + "\" NOT IN (" + placeholders + ")";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Object key : seed.seedKeys()) {
                statement.setObject(index++, key);
            }
            statement.executeUpdate();
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
