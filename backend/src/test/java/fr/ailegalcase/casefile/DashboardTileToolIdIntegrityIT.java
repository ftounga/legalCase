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
            "F-196-questions-summary",
            // SF-206-05 (2026-05-20) : exclusion transitoire F-DT-39 prise d'acte
            // de la rupture — la tuile dashboard est livrée backend (mapper
            // `tileFromPriseActeRuptureAnalysis` + thème DIAGNOSTIC) mais le seed
            // `decision_tool_visibility_rules` + entrée TOOL_REGISTRY frontend
            // sont livrés ensemble par SF-206-06 (pattern prouvé F-DT-42 /
            // F-DT-75). À résorber par SF-206-06 (retrait de cette entrée +
            // migration 268 visibility).
            "F-DT-39-prise-acte-rupture"
            // SF-206-02 (2026-05-20) : F-DT-42 retiré — résolution structurelle
            // complète (seed migration 263 + TOOL_REGISTRY frontend + tile mapper).
            // SF-206-04 (2026-05-20) : F-DT-75 retiré — résolution structurelle
            // complète (seed migration 266 + entrée TOOL_REGISTRY frontend dans
            // decisional-tools-panel.component.ts + tile mapper backend déjà
            // présent dans CaseFileDashboardService).
    );

    /**
     * SF-DT-36-03 — outils décisionnels seedés dans
     * {@code decision_tool_visibility_rules} (donc visibles dans le panel
     * F-IA-04) qui n'émettent <strong>volontairement</strong> aucune
     * {@code DashboardTile} : outils "formule/PDF" sans persistance de
     * résultat, composants UI Angular auto-suffisants, référentiels.
     *
     * <p>Sert au garde-fou inverse {@link
     * #tout_toolId_seede_en_visibilite_a_une_tile_dashboard_ou_est_explicitement_exclu()}.
     * Tout ajout ici doit être justifié : un outil disposant d'une table de
     * résultat + d'un endpoint de calcul ne doit JAMAIS y figurer — il doit
     * avoir sa tuile dashboard (sinon son résultat est calculé mais jamais
     * affiché, cf. bug F-DT-36 corrigé par SF-DT-36-03).</p>
     */
    private static final Set<String> KNOWN_NO_DASHBOARD_TILE_IDS = Set.of(
            // Outils info-only / formule / PDF — pas de table de résultat persistée.
            "F-DT-03-prescription-litige",
            "F-DT-04-fiche-prudhomale",
            "F-DT-06-requete-tribunal-travail",
            "F-132-rupture-amiable-info",
            // Wrappers frontend auto-suffisants (F-198) — pas de backend de calcul.
            "F-152-divorce-consentement-scoring",
            "F-153-fourchettes-jaf",
            "F-FA-01-prestation-compensatoire",
            "F-FA-02-pension-alimentaire",
            "F-FA-04-liquidation-communaute",
            // Checklist référentielle — pas de résultat décisionnel persisté.
            "F-IM-01-checklist-pieces",
            // F-207 — exclusions transitoires (à résorber par la session F-207) :
            // outils livrés sans mapper DashboardTile dans CaseFileDashboardService,
            // hotfix de mitigation master-red posé par F-206 (2026-05-20).
            // Le câblage des vraies tuiles revient à la SF F-207 propriétaire
            // (mêmes mappings que prescription-be-litige-travail + c4-onem-checklist
            // posés par #1141).
            "at-fedris-declaration",
            "contestation-c4-onem",
            // F-245 hotfix (2026-05-20) — prescription-be-litige-travail retiré ici :
            // la tuile dashboard est désormais câblée par
            // CaseFileDashboardService.tileFromPrescriptionBeLitigeTravailAnalysis().
            // L'exclusion temporaire posée par le commit 3e8cb1da (#1129) est
            // résorbée par la vraie correction structurelle.
            // SF-206-02 hotfix master-red (2026-05-20) : refere-tribunal-travail-be
            // livré par SF-207-05b (PR #1149) sans mapper DashboardTile —
            // exclusion transitoire à résorber par la session F-207 propriétaire,
            // même pattern que at-fedris-declaration / contestation-c4-onem.
            "refere-tribunal-travail-be",
            // SF-206-03 hotfix master-red (2026-05-20) : rcc-be-conditions et
            // rcc-be-indemnite-complementaire livrés par SF-207-06-backend (PR #1151)
            // et SF-207-07-backend (PR #1155) sans mapper DashboardTile —
            // exclusion transitoire à résorber par la session F-207 propriétaire,
            // même pattern que at-fedris-declaration / contestation-c4-onem /
            // refere-tribunal-travail-be.
            "rcc-be-conditions",
            "rcc-be-indemnite-complementaire"
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

    /**
     * SF-DT-36-03 — garde-fou <strong>inverse</strong> de {@link
     * #aucun_toolId_hardcode_dans_CaseFileDashboardService_n_est_orphelin()}.
     *
     * <p>Origine du bug : F-DT-36 (nullité de procédure de licenciement) a été
     * livré avec sa table de résultat, son endpoint de calcul, son seed
     * {@code decision_tool_visibility_rules} et son entrée {@code TOOL_REGISTRY}
     * frontend — mais sans tuile dans {@code CaseFileDashboardService}. Le
     * résultat calculé était persisté mais jamais affiché au dashboard. Le test
     * direct ne détecte pas ce cas (il vérifie « tuile émise → seed DB », pas
     * « seed DB → tuile émise »).</p>
     *
     * <p>Principe : tout {@code tool_id} de {@code decision_tool_visibility_rules}
     * doit <strong>soit</strong> être émis comme {@code DashboardTile} par
     * {@code CaseFileDashboardService}, <strong>soit</strong> figurer dans
     * {@link #KNOWN_NO_DASHBOARD_TILE_IDS} (outil sans tuile par conception).</p>
     */
    @Test
    void tout_toolId_seede_en_visibilite_a_une_tile_dashboard_ou_est_explicitement_exclu()
            throws IOException {
        Set<String> emittedToolIds = extractToolIdsFromDashboardService();
        Set<String> dbToolIds = new HashSet<>(jdbc.queryForList(
                "SELECT DISTINCT tool_id FROM decision_tool_visibility_rules",
                String.class));
        assertThat(dbToolIds)
                .withFailMessage("Aucun tool_id en DB — vérifier que les migrations "
                        + "decision_tool_visibility_rules ont bien tourné")
                .isNotEmpty();

        List<String> missingTiles = dbToolIds.stream()
                .filter(id -> !emittedToolIds.contains(id))
                .filter(id -> !KNOWN_NO_DASHBOARD_TILE_IDS.contains(id))
                .sorted()
                .collect(Collectors.toList());

        assertThat(missingTiles)
                .withFailMessage(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("SF-DT-36-03 — outils seedés dans\n")
                      .append("decision_tool_visibility_rules SANS tuile émise par\n")
                      .append("CaseFileDashboardService ET hors KNOWN_NO_DASHBOARD_TILE_IDS :\n");
                    missingTiles.forEach(id -> sb.append(" - ").append(id).append("\n"));
                    sb.append("\nLeur résultat serait calculé/persisté mais jamais\n")
                      .append("affiché au dashboard (bug F-DT-36). Action requise :\n")
                      .append("  (a) ajouter un addSafely(tiles, () -> tileFromXxx(...))\n")
                      .append("      + la méthode tileFromXxx() dans\n")
                      .append("      CaseFileDashboardService, OU\n")
                      .append("  (b) si l'outil n'a volontairement pas de tuile dashboard\n")
                      .append("      (outil formule/PDF sans persistance de résultat,\n")
                      .append("      composant UI auto-suffisant, référentiel),\n")
                      .append("      l'ajouter à KNOWN_NO_DASHBOARD_TILE_IDS avec\n")
                      .append("      justification dans ce test.");
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
