package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SF-164-01 : garde-fou contre la régression "tool_id seedé en DB sans entrée
 * TOOL_REGISTRY frontend" (cas réel staging 2026-04-26 sur dossier E-36 où
 * le panneau F-IA-04 paraissait vide à cause de 13 outils silencieusement
 * masqués via {@code console.warn} dans {@code resolveEntry()}).
 *
 * <p>Refactor 2026-05-10 : suppression de la liste hardcodée
 * {@code KNOWN_FRONTEND_TOOL_IDS} (qui pouvait être contournée en l'éditant
 * sans livrer le composant — cas réel F-208 où 4 tool_id avaient été ajoutés
 * au test sans wrapper Angular). Le test extrait désormais les tool_id
 * directement du source frontend
 * {@code decisional-tools-panel.component.ts} via regex — pattern miroir de
 * {@link DashboardTileToolIdIntegrityIT} (F-229 SF-229-03).</p>
 *
 * <p>Symétrique de la règle CLAUDE.md {@code feedback_pre_merge_endpoint_check}
 * (frontend mergé sans backend). Cette règle protège l'inverse : backend
 * mergé sans entrée TOOL_REGISTRY frontend.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
class DecisionToolVisibilityIntegrityIT {

    @Autowired
    JdbcTemplate jdbc;

    /** Chemin frontend relatif au répertoire de travail Maven (= backend/). */
    private static final String TOOL_REGISTRY_SOURCE =
            "../frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts";

    /**
     * Capture le début de la {@code TOOL_REGISTRY} map et tout son contenu jusqu'à
     * la fin (closing {@code ]);}). Utilisé pour ne scanner que les entrées du
     * registry, pas les autres tableaux du fichier.
     */
    private static final Pattern TOOL_REGISTRY_BLOCK = Pattern.compile(
            "static\\s+readonly\\s+TOOL_REGISTRY[^=]*=\\s*new\\s+Map[^\\[]*\\[(.*?)\\]\\s*\\)\\s*;",
            Pattern.DOTALL);

    /**
     * Capture chaque clé string littérale d'entrée TOOL_REGISTRY :
     * {@code ['<tool-id>', {...}]}.
     */
    private static final Pattern TOOL_REGISTRY_ENTRY = Pattern.compile(
            "\\[\\s*'([^']+)'\\s*,\\s*\\{");

    @Test
    void aucun_tool_id_seede_en_db_n_est_orphelin_dans_TOOL_REGISTRY_frontend() throws IOException {
        Set<String> frontendToolIds = extractToolIdsFromFrontendRegistry();
        assertThat(frontendToolIds)
                .withFailMessage("Aucun tool_id extrait du frontend — vérifier le regex et le chemin source : %s",
                        Paths.get(TOOL_REGISTRY_SOURCE).toAbsolutePath())
                .isNotEmpty();

        List<String> dbToolIds = jdbc.queryForList(
                "SELECT DISTINCT tool_id FROM decision_tool_visibility_rules ORDER BY tool_id",
                String.class);

        List<String> orphans = dbToolIds.stream()
                .filter(id -> !frontendToolIds.contains(id))
                .sorted()
                .collect(Collectors.toList());

        assertThat(orphans)
                .withFailMessage(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("SF-164-01 — tool_id seedés en DB mais ABSENTS de TOOL_REGISTRY frontend\n")
                      .append("(extraction dynamique de ").append(TOOL_REGISTRY_SOURCE).append(") :\n");
                    orphans.forEach(id -> sb.append(" - ").append(id).append("\n"));
                    sb.append("\nAction requise (CLAUDE.md feedback_pre_merge_visibility_seed_check) :\n")
                      .append("  (a) ajouter l'entrée TOOL_REGISTRY dans decisional-tools-panel.component.ts\n")
                      .append("      (avec composant Angular wrapper minimum), OU\n")
                      .append("  (b) supprimer le seed via une migration Liquibase DELETE.\n")
                      .append("\nLe test extrait dynamiquement TOOL_REGISTRY — pas de liste hardcodée à\n")
                      .append("mettre à jour : c'est le source frontend qui est la source de vérité.\n")
                      .append("Cf. pattern F-229 SF-229-03 (DashboardTileToolIdIntegrityIT).");
                    return sb.toString();
                })
                .isEmpty();
    }

    /**
     * SF-165-FIX (bug prod 2026-06-03) : outils Travail FR "core" qui DOIVENT
     * conserver au moins une règle de visibilité en (DROIT_DU_TRAVAIL, FRANCE).
     * F-DT-08 (validité licenciement) et F-DT-09 (comparateur d'indemnités) ont
     * été rendus invisibles sur tout dossier de licenciement FR par l'effet
     * combiné des migrations 106 (suppression CONTEXTUAL) + 194 (suppression
     * ALWAYS_ON) → 0 règle restante. Restaurés par la migration 544.
     * Ce garde-fou couvre le sens "outil frontend sans règle DB" — inverse du
     * test ci-dessus (règle DB sans entrée frontend).
     */
    private static final List<String> CORE_TRAVAIL_FR_TOOLS = List.of(
            "F-DT-08-licenciement-validity",
            "F-DT-09-comparateur-indemnites");

    @Test
    void les_outils_licenciement_core_travail_fr_ont_au_moins_une_regle_de_visibilite() {
        List<String> missing = CORE_TRAVAIL_FR_TOOLS.stream()
                .filter(toolId -> {
                    Integer count = jdbc.queryForObject(
                            "SELECT count(*) FROM decision_tool_visibility_rules "
                                    + "WHERE tool_id = ? AND legal_domain = 'DROIT_DU_TRAVAIL' AND country = 'FRANCE'",
                            Integer.class, toolId);
                    return count == null || count == 0;
                })
                .sorted()
                .collect(Collectors.toList());

        assertThat(missing)
                .withFailMessage(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("SF-165-FIX — outils Travail FR 'core' SANS aucune règle de visibilité\n")
                      .append("en (DROIT_DU_TRAVAIL, FRANCE) → silencieusement invisibles sur les dossiers\n")
                      .append("de licenciement (ni alwaysOn, ni contextual, ni catalog) :\n");
                    missing.forEach(id -> sb.append(" - ").append(id).append("\n"));
                    sb.append("\nRégression type : un DELETE de règles (ALWAYS_ON et/ou CONTEXTUAL) sans\n")
                      .append("réinsertion — cf. migrations 106 + 194 (bug 2026-06-03), corrigé par 544.\n")
                      .append("Action : ajouter une migration Liquibase qui (ré)insère au moins une règle\n")
                      .append("de visibilité FR pour ces outils.");
                    return sb.toString();
                })
                .isEmpty();
    }

    private Set<String> extractToolIdsFromFrontendRegistry() throws IOException {
        Path path = Paths.get(TOOL_REGISTRY_SOURCE);
        String source = String.join("\n", Files.readAllLines(path));

        Matcher blockMatcher = TOOL_REGISTRY_BLOCK.matcher(source);
        if (!blockMatcher.find()) {
            throw new IllegalStateException(
                    "TOOL_REGISTRY block introuvable dans " + path.toAbsolutePath()
                            + " — le format a-t-il changé ? (regex attendu : `static readonly TOOL_REGISTRY ... new Map[...]`)");
        }
        String registryBody = blockMatcher.group(1);

        Set<String> toolIds = new LinkedHashSet<>();
        Matcher entryMatcher = TOOL_REGISTRY_ENTRY.matcher(registryBody);
        while (entryMatcher.find()) {
            toolIds.add(entryMatcher.group(1));
        }
        return toolIds;
    }
}
