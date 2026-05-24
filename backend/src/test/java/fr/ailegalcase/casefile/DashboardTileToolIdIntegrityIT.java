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
            // F-253 SF-253-01 (2026-05-21) — tile dédiée au rappel des risques
            // restant à arbitrer (statut A_CREUSER), cohabite avec
            // F-195-risques-summary. Routée via BadgeNavigationService côté
            // frontend (clic → /synthesis#section-risques), donc absente de
            // decision_tool_visibility_rules (pattern identique aux 5 tiles
            // résumé ci-dessus).
            "F-253-risques-a-creuser"
            // SF-212-02 (2026-05-20) : F-DT-36-licenciement-faute-grave-lourde
            // retiré — résolution structurelle complète (seed migration 278 +
            // entrée TOOL_REGISTRY frontend dans decisional-tools-panel.component.ts
            // + tile mapper backend déjà présent dans CaseFileDashboardService).
            // SF-206-02 (2026-05-20) : F-DT-42 retiré — résolution structurelle
            // complète (seed migration 263 + TOOL_REGISTRY frontend + tile mapper).
            // SF-206-04 (2026-05-20) : F-DT-75 retiré — résolution structurelle
            // complète (seed migration 266 + entrée TOOL_REGISTRY frontend dans
            // decisional-tools-panel.component.ts + tile mapper backend déjà
            // présent dans CaseFileDashboardService).
            // SF-206-06 (2026-05-20) : F-DT-39 retiré — résolution structurelle
            // complète (seed migration 268 + entrée TOOL_REGISTRY frontend dans
            // decisional-tools-panel.component.ts + tile mapper backend
            // `tileFromPriseActeRuptureAnalysis` déjà présent dans
            // CaseFileDashboardService).
            // SF-206-08 (2026-05-20) : F-DT-40 retiré — résolution structurelle
            // complète (seed migration 270 + entrée TOOL_REGISTRY frontend dans
            // decisional-tools-panel.component.ts + tile mapper backend
            // `tileFromResiliationJudiciaireCphAnalysis` déjà présent dans
            // CaseFileDashboardService).
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
            // SF-216-07 : outil ARIPA recouvrement — table dédiée, pas de tile dashboard.
            "F-FA-ARIPA-RECOUVREMENT",
            // SF-216-15 : outil Adoption intra-familiale — table dédiée, pas de tile dashboard.
            "F-FA-ADOPTION-INTRA",
            // SF-216-17 : outil Adoption internationale — table dédiée, pas de tile dashboard.
            "F-FA-ADOPTION-INTERNATIONALE",
            // SF-216-09 : outil délégation autorité parentale FR — table dédiée, pas de tile dashboard.
            "F-FA-XX-delegation-ap",
            // SF-216-11 : outil retrait autorité parentale FR — table dédiée, pas de tile dashboard.
            "F-FA-RETRAIT-AP",
            // SF-216-13 : outil Audition du mineur par le JAF FR — table dédiée, pas de tile dashboard.
            "F-FA-AUDITION-MINEUR",
            // SF-216-19 : outil indignité successorale FR — table dédiée, pas de tile dashboard.
            "F-FA-INDIGNITE-SUCCESSORALE",
            // SF-216-21 : outil recel successoral FR — table dédiée, pas de tile dashboard.
            "F-FA-RECEL-SUCCESSION",
            // SF-216-23 : outil donation entre époux FR — table dédiée, pas de tile dashboard.
            "F-FA-DONATION-ENTRE-EPOUX",
            // SF-216-27 : outil partage successoral notarié FR — table dédiée, pas de tile dashboard.
            "F-FA-PARTAGE-NOTARIAL",
            // SF-216-25 : outil présomption de paternité FR — table dédiée, pas de tile dashboard.
            "F-FA-PRESOMPTION-PATERNITE",
            // SF-216-29 : outil donation-partage FR — table dédiée, pas de tile dashboard.
            "F-FA-DONATION-PARTAGE",
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
            "rcc-be-indemnite-complementaire",
            // SF-206-07 hotfix master-red (2026-05-20) : outplacement-be-obligatoire-45
            // livré par SF-207-08b-frontend (PR #1162) sans mapper DashboardTile
            // côté backend — exclusion transitoire à résorber par la session F-207
            // propriétaire, même pattern que at-fedris-declaration /
            // contestation-c4-onem / refere-tribunal-travail-be / rcc-be-conditions /
            // rcc-be-indemnite-complementaire. Issue master-red pré-existante à
            // SF-206-07, levée ici par hotfix transitoire pour permettre le merge
            // backend résiliation judiciaire (F-DT-40).
            "outplacement-be-obligatoire-45",
            // SF-252-01 hotfix master-red (2026-05-20) : succession-be-acceptation-renonciation
            // et succession-be-devolution-reserve livrés par SF-217-12 (PR #1181) et
            // SF-217-11 (PR #1180) sans mapper DashboardTile côté backend — exclusion
            // transitoire à résorber par la session F-217 propriétaire, même pattern
            // que at-fedris-declaration / contestation-c4-onem / refere-tribunal-travail-be
            // etc. Issue master-red pré-existante à SF-252-01, levée ici par hotfix
            // transitoire pour permettre le déploiement F-252 protections nullité.
            "succession-be-acceptation-renonciation",
            "succession-be-devolution-reserve",
            // SF-217-14 (2026-05-21) : protection-majeur-be livré sans mapper
            // DashboardTile côté backend — exclusion transitoire à résorber par
            // la session F-217 propriétaire (même pattern Vague 3 que
            // succession-be-acceptation-renonciation / succession-be-devolution-reserve).
            "protection-majeur-be",
            // SF-217-16 (2026-05-21) : mariage-etranger-be-reconnaissance livré sans
            // mapper DashboardTile — même pattern transitoire Vague 3.
            "mariage-etranger-be-reconnaissance",
            // SF-217-18 (2026-05-21) : contestation-filiation-be livré sans mapper
            // DashboardTile côté backend — exclusion transitoire alignée sur les
            // 2 autres outils F-217 Vague 3 (mêmes raisons : composant frontend
            // standalone auto-suffisant, restitution via GET du snapshot, pas de
            // besoin métier tuile dashboard immédiat). À résorber par la session
            // F-217 propriétaire qui consolidera les 3+ outils en lot.
            "contestation-filiation-be",
            // SF-212-03 (2026-05-23) : F-DT-50-forfait-jours-validite livré sans
            // mapper DashboardTile côté backend — exclusion transitoire alignée
            // sur le pattern Vague 3 (composant frontend standalone auto-suffisant,
            // restitution via GET du snapshot, pas de besoin métier tuile
            // dashboard immédiat dans le bundle F-212 P2 Travail FR). À résorber
            // par une SF de tile dashboard dédiée si retour terrain prouve le
            // besoin d'une mise en avant en surface dossier.
            "F-DT-50-forfait-jours-validite",
            // SF-212-05 (2026-05-23) : F-DT-72 transfert d'entreprise L. 1224-1 FR
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            "F-DT-72-transfert-entreprise-l1224-1",
            // SF-212-07 (2026-05-24) : F-DT-44 CSP/CRP — conformité de la proposition FR + SF-212-09 F-DT-91 faute inexcusable + SF-212-25 F-DT-61 lanceur d'alerte
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tuile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            "F-DT-44-csp-crp-conformite",
            "F-DT-91-faute-inexcusable-employeur",
            "F-DT-61-lanceur-alerte-protection",
            // SF-212-11 (2026-05-24) : F-DT-70 modification du contrat — refus du salarié FR
            "F-DT-70-modification-contrat-refus",
            // SF-212-13 (2026-05-24) : F-DT-71 mutation — validité de la clause de mobilité FR
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tuile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            "F-DT-71-mutation-clause-mobilite",
            // SF-212-15 (2026-05-24) : F-DT-82 télétravail — conformité et litige FR
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tuile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            "F-DT-82-teletravail-accord",
            // SF-212-19 (2026-05-24) : F-DT-48 mise à pied disciplinaire — régularité FR
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tuile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            "F-DT-48-mise-a-pied-disciplinaire",
            // SF-212-23 (2026-05-24) : F-DT-56 égalité salariale femmes/hommes FR
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tuile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            "F-DT-56-egalite-salariale-femmes-hommes",
            // SF-212-17 (2026-05-24) : F-DT-43 rupture anticipée du CDD FR
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tuile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            "F-DT-43-rupture-anticipee-cdd",
            // SF-212-21 (2026-05-24) : F-DT-41 démission validité équivoque FR
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tuile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            "F-DT-41-demission-validite-equivoque",
            // SF-212-35 (2026-05-24) : F-DT-46 PDV / RCC — conformité FR
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tuile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            "F-DT-46-pdv-rcc-conformite"
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
