package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SF-164-01 : garde-fou contre la régression "tool_id seedé en DB sans entrée
 * TOOL_REGISTRY frontend" (cas réel staging 2026-04-26 sur dossier E-36 où
 * le panneau F-IA-04 paraissait vide à cause de 13 outils silencieusement
 * masqués via {@code console.warn} dans {@code resolveEntry()}).
 *
 * <p>Principe : tout {@code tool_id} présent dans
 * {@code decision_tool_visibility_rules} doit avoir une entrée correspondante
 * dans {@code TOOL_REGISTRY} côté frontend. La liste {@link #KNOWN_FRONTEND_TOOL_IDS}
 * ci-dessous est la source de vérité partagée — elle doit être mise à jour
 * à chaque ajout d'entrée TOOL_REGISTRY frontend.
 *
 * <p>Symétrique de la règle CLAUDE.md {@code feedback_pre_merge_endpoint_check}
 * (frontend mergé sans backend). Cette règle protège l'inverse : backend
 * mergé sans entrée TOOL_REGISTRY frontend.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
class DecisionToolVisibilityIntegrityIT {

    @Autowired
    JdbcTemplate jdbc;

    /**
     * Source de vérité : tous les {@code tool_id} qui possèdent une entrée
     * {@code TOOL_REGISTRY} dans
     * {@code frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts}.
     *
     * <p>Synchroniser à chaque ajout/retrait d'entrée TOOL_REGISTRY frontend.
     * La règle CLAUDE.md {@code feedback_pre_merge_visibility_seed_check}
     * exige cette mise à jour avant tout merge.
     */
    private static final Set<String> KNOWN_FRONTEND_TOOL_IDS = Set.of(
            "F-DT-03-prescription-litige",
            "F-DT-04-fiche-prudhomale",
            "F-DT-06-requete-tribunal-travail",
            "F-DT-07-anciennete-conges-prime",
            "F-DT-08-licenciement-validity",
            "F-DT-09-comparateur-indemnites",
            "F-DT-10-rupture-conv-validity",
            "F-DT-11-harcelement-licenciement-nul",
            "F-DT-12-discrimination-dommages-interets",
            "F-DT-13-licenciement-economique",
            "F-DT-14-pse-validite",
            "F-DT-15-inaptitude",
            "F-DT-16-licenciement-nul-detection",
            "F-DT-17-indemnite-precarite-cdd",
            "F-DT-18-fin-mission-interim",
            "F-DT-19-heures-sup",
            "F-DT-20-rappel-salaire",
            "F-DT-21-travail-dissimule",
            "F-DT-22-requalification-cdd-cdi",
            "F-DT-23-requalification-interim-cdi",
            "F-DT-24-non-concurrence",
            "F-DT-25-indemnite-preavis",
            "F-DT-26-conges-payes-indemnite",
            "F-DT-27-motif-grave-be",
            "F-DT-28-avantages-conventionnels-be",
            "F-DT-29-credit-temps-be",
            "F-DT-30-protection-rp",
            "F-DT-31-transaction",
            "F-DT-32-documents-fin-contrat",
            "F-DT-33-at-mp",
            "F-DT-34-refere-prudhomal",
            "F-DT-35-contestation-are-fr",
            "F-FA-05-partage-immobilier",
            "F-FA-06-calendrier-garde",
            "F-FA-07-checklist-divorce",
            "F-FA-08-divorce-alteration",
            "F-FA-09-divorce-faute",
            "F-FA-10-divorce-accepte",
            "F-FA-11-desunion-irremediable-be",
            "F-FA-12-mesures-provisoires",
            "F-FA-13-revisions-post-divorce",
            "F-FA-14-ordonnance-protection",
            "F-FA-15-recompenses",
            "F-FA-16-communaute-universelle",
            "F-FA-17-partage-judiciaire",
            "F-FA-18-contestation-paternite",
            "F-FA-18-possession-etat",
            "F-FA-18-recherche-paternite",
            "F-FA-18-reconnaissance-paternelle",
            "F-FA-19-autorite-parentale",
            "F-FA-19-changement-residence",
            "F-FA-19-desaccords-parentaux",
            "F-FA-20-pacs-dissolution",
            "F-FA-21-separation-corps",
            "F-FA-22-indivision",
            "F-FA-23-ordonnance-requete",
            "F-FA-24-devolution-legale",
            "F-FA-24-donation",
            "F-FA-24-rapport-succession",
            "F-FA-24-reserve-heriditaire",
            "F-FA-24-testament-validite",
            "F-FA-25-majeurs-proteges",
            "F-FA-26-changement-etat-civil",
            "F-FA-27-pma-gpa",
            "F-IM-01-checklist-pieces",
            "F-IM-05-arbre-decisionnel-titre",
            "F-IM-06-recours",
            "F-IM-07-droit-au-travail",
            "F-IM-08-annexe13-be",
            "F-IM-08-oqtf-avec-delai-fr",
            "F-IM-08-oqtf-sans-delai-fr",
            "F-IM-08-referes-admin-fr",
            "F-IM-09-aes-etudiant",
            "F-IM-09-aes-famille",
            "F-IM-09-aes-humanitaire",
            "F-IM-09-aes-metiers-tension",
            "F-IM-11-changement-statut",
            "F-IM-12-asile-avance",
            "F-IM-13-naturalisation",
            "F-IM-14-40bis-cohabitant-ue-be",
            "F-IM-14-40ter-familial-belge-be",
            "F-IM-14-9bis-humanitaire-be",
            "F-IM-14-9ter-medical-be",
            "F-IM-17-regime-algerien",
            "F-IM-19-mineurs",
            "F-IM-20-mesures-eloignement",
            "F-132-rupture-amiable-info",
            "F-132-rupture-conv-indemnite",
            "F-136-travail-procedure"
    );

    @Test
    void aucun_tool_id_seede_en_db_n_est_orphelin_dans_TOOL_REGISTRY_frontend() {
        List<String> dbToolIds = jdbc.queryForList(
                "SELECT DISTINCT tool_id FROM decision_tool_visibility_rules ORDER BY tool_id",
                String.class);

        List<String> orphans = dbToolIds.stream()
                .filter(id -> !KNOWN_FRONTEND_TOOL_IDS.contains(id))
                .collect(Collectors.toList());

        assertThat(orphans)
                .withFailMessage(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("SF-164-01 — tool_id seedés en DB mais ABSENTS de TOOL_REGISTRY frontend :\n");
                    orphans.forEach(id -> sb.append(" - ").append(id).append("\n"));
                    sb.append("\nAction requise (CLAUDE.md feedback_pre_merge_visibility_seed_check) :\n")
                      .append("  (a) ajouter l'entrée TOOL_REGISTRY dans decisional-tools-panel.component.ts\n")
                      .append("      ET mettre à jour KNOWN_FRONTEND_TOOL_IDS dans ce test, OU\n")
                      .append("  (b) supprimer le seed via une migration Liquibase DELETE.\n")
                      .append("\nNe jamais merger un INSERT/UPDATE dans decision_tool_visibility_rules\n")
                      .append("sans avoir vérifié l'existence côté frontend.");
                    return sb.toString();
                })
                .isEmpty();
    }
}
