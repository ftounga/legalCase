package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-229 SF-229-03 (2026-05-09) — garde-fou symétrique au
 * {@link DecisionToolVisibilityIntegrityIT} (F-164 SF-164-01) côté backend
 * dashboard. Empêche la régression "toolId hardcodé dans
 * {@code CaseFileDashboardService} sans correspondance Liquibase".
 *
 * <p>Origine du bug : audit transversal F-229 SF-229-03 a révélé 5 mismatches
 * en plus de F-192 (déjà fixé SF-229-02) sur des toolIds Immigration FR
 * (F-IM-08 OQTF avec/sans délai, référés admin, F-IM-19 mineurs) — la
 * convention "raccourcie" du backend (sans `-fr` / avec `-immigration`) ne
 * matchait pas la convention canonique des migrations Liquibase. Les tiles
 * étaient cassées silencieusement au clic dashboard.</p>
 *
 * <p>Principe : extrait par regex tous les {@code new DashboardTile("F-XX-...",
 * ...)} du source {@code CaseFileDashboardService.java} et vérifie que chaque
 * toolId est <strong>soit</strong> présent dans
 * {@code decision_tool_visibility_rules} (source de vérité Liquibase),
 * <strong>soit</strong> dans la liste {@link #KNOWN_SUMMARY_TILE_IDS} des
 * tiles "résumé" qui ne sont pas instanciées comme outils mais routées via
 * {@code BadgeNavigationService} côté frontend.</p>
 *
 * <p>Symétrique de {@link DecisionToolVisibilityIntegrityIT} qui protège
 * l'inverse (DB seedée sans entrée TOOL_REGISTRY frontend).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
class DashboardTileToolIdIntegrityIT {

    @Autowired
    JdbcTemplate jdbc;

    /**
     * Tiles "résumé" du dashboard décisionnel qui ne sont pas instanciées
     * comme outils — elles passent par {@link
     * fr.ailegalcase.casefile.CaseFileDashboardService} pour l'émission, mais
     * sont routées côté frontend via {@code BadgeNavigationService} (cible
     * canonique de la grille de badges F-162). Donc absentes de
     * {@code decision_tool_visibility_rules}.
     *
     * <p>Cf. F-229 SF-229-01 + SF-229-02 + SF-229-03 (handler F-193 ajouté).</p>
     */
    private static final Set<String> KNOWN_SUMMARY_TILE_IDS = Set.of(
            "F-192-retained-pistes-summary",
            "F-193-procedure-checks-summary",
            "F-194-pieces-summary",
            "F-195-risques-summary",
            "F-196-questions-summary"
    );

    /** Source à scanner — chemin relatif au répertoire de travail Maven (= backend/). */
    private static final String DASHBOARD_SERVICE_PATH =
            "src/main/java/fr/ailegalcase/casefile/CaseFileDashboardService.java";

    /**
     * Capture le 1er argument string littéral de tout {@code new DashboardTile(...)}.
     * Tolère whitespace + saut de ligne après la parenthèse ouvrante.
     */
    private static final Pattern DASHBOARD_TILE_TOOL_ID = Pattern.compile(
            "new\\s+DashboardTile\\s*\\(\\s*\"([^\"]+)\"");

    @Test
    void aucun_toolId_hardcode_dans_CaseFileDashboardService_n_est_orphelin() throws IOException {
        Set<String> hardcodedToolIds = extractToolIdsFromDashboardService();
        assertThat(hardcodedToolIds)
                .withFailMessage("Aucun toolId extrait — vérifier le regex et le chemin source")
                .isNotEmpty();

        Set<String> dbToolIds = new HashSet<>(jdbc.queryForList(
                "SELECT DISTINCT tool_id FROM decision_tool_visibility_rules",
                String.class));

        List<String> orphans = hardcodedToolIds.stream()
                .filter(id -> !dbToolIds.contains(id))
                .filter(id -> !KNOWN_SUMMARY_TILE_IDS.contains(id))
                .sorted()
                .collect(Collectors.toList());

        assertThat(orphans)
                .withFailMessage(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("F-229 SF-229-03 — toolIds hardcodés dans CaseFileDashboardService\n")
                      .append("ABSENTS de decision_tool_visibility_rules ET hors liste\n")
                      .append("KNOWN_SUMMARY_TILE_IDS :\n");
                    orphans.forEach(id -> sb.append(" - ").append(id).append("\n"));
                    sb.append("\nAction requise :\n")
                      .append("  (a) corriger le toolId dans CaseFileDashboardService.java\n")
                      .append("      pour qu'il matche la migration Liquibase\n")
                      .append("      (decision_tool_visibility_rules.tool_id), OU\n")
                      .append("  (b) ajouter une migration Liquibase INSERT dans\n")
                      .append("      decision_tool_visibility_rules pour ce toolId\n")
                      .append("      (avec entrée TOOL_REGISTRY frontend correspondante,\n")
                      .append("      cf. règle CLAUDE.md feedback_pre_merge_visibility_seed_check), OU\n")
                      .append("  (c) si c'est une nouvelle tile résumé routée via\n")
                      .append("      BadgeNavigationService, l'ajouter à\n")
                      .append("      KNOWN_SUMMARY_TILE_IDS dans ce test.\n")
                      .append("\nNe jamais émettre une DashboardTile sans correspondance DB —\n")
                      .append("le clic dashboard sera silencieusement cassé en runtime.");
                    return sb.toString();
                })
                .isEmpty();
    }

    private Set<String> extractToolIdsFromDashboardService() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(DASHBOARD_SERVICE_PATH));
        String source = String.join("\n", lines);
        Set<String> toolIds = new LinkedHashSet<>();
        Matcher matcher = DASHBOARD_TILE_TOOL_ID.matcher(source);
        while (matcher.find()) {
            toolIds.add(matcher.group(1));
        }
        return toolIds;
    }
}
