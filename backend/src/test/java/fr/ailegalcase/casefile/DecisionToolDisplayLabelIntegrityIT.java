package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SF-238-01 : garde-fou CI dynamique contre la régression « tool_id brut
 * affiché dans le chip catalogue du panel F-IA-04 ».
 *
 * <p>Le test extrait dynamiquement les entrées TOOL_REGISTRY depuis le source
 * frontend ({@code decisional-tools-panel.component.ts}) via regex et assert :
 * <ol>
 *   <li>chaque entrée possède un {@code displayLabel} non vide,</li>
 *   <li>aucun {@code displayLabel} ne contient son propre {@code tool_id}
 *       (anti copier-coller paresseux — interdit « F-DT-08-licenciement-validity »
 *       comme label, force « Licenciement — validité (FR) »).</li>
 * </ol>
 *
 * <p>Pattern miroir de {@link DecisionToolVisibilityIntegrityIT} (refactor
 * PR #918 — extraction dynamique au lieu de liste hardcodée) et de
 * {@code DashboardTileToolIdIntegrityIT} (F-229 SF-229-03). C'est le source
 * frontend qui est la source de vérité ; pas de liste à maintenir côté
 * backend.</p>
 *
 * <p>Pas de {@code @SpringBootTest} nécessaire — pur test JUnit sur fichier
 * source (lecture filesystem + regex). Évite l'overhead du contexte Spring
 * et la dépendance à une DB.</p>
 */
class DecisionToolDisplayLabelIntegrityIT {

    /** Chemin frontend relatif au répertoire de travail Maven (= backend/). */
    private static final String TOOL_REGISTRY_SOURCE =
            "../frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts";

    /**
     * Capture le bloc {@code static readonly TOOL_REGISTRY = new Map(...)}.
     * Identique à {@link DecisionToolVisibilityIntegrityIT}.
     */
    private static final Pattern TOOL_REGISTRY_BLOCK = Pattern.compile(
            "static\\s+readonly\\s+TOOL_REGISTRY[^=]*=\\s*new\\s+Map[^\\[]*\\[(.*?)\\]\\s*\\)\\s*;",
            Pattern.DOTALL);

    /**
     * Capture chaque entrée {@code ['<tool-id>', { ... displayLabel: '<label>', ... }]}.
     * Le {@code displayLabel} doit apparaître dans les premières lignes de
     * l'entrée (convention SF-238-01 : juste après l'accolade ouvrante).
     *
     * <p>Le label peut contenir des apostrophes échappées (\\') et des
     * caractères non-ASCII (accents). On capture jusqu'au prochain {@code '}
     * non échappé.</p>
     */
    private static final Pattern TOOL_REGISTRY_ENTRY_WITH_LABEL = Pattern.compile(
            "\\[\\s*'([^']+)'\\s*,\\s*\\{\\s*\\n[^\\n]*displayLabel:\\s*'((?:\\\\'|[^'])*)'",
            Pattern.DOTALL);

    /** Idem mais sans capturer le label — pour détecter les entrées qui n'en ont pas. */
    private static final Pattern TOOL_REGISTRY_ENTRY_ANY = Pattern.compile(
            "\\[\\s*'([^']+)'\\s*,\\s*\\{");

    @Test
    void chaque_entree_TOOL_REGISTRY_a_un_displayLabel_non_vide_et_humain() throws IOException {
        Path path = Paths.get(TOOL_REGISTRY_SOURCE);
        String source = String.join("\n", Files.readAllLines(path));

        Matcher blockMatcher = TOOL_REGISTRY_BLOCK.matcher(source);
        assertThat(blockMatcher.find())
                .withFailMessage("TOOL_REGISTRY block introuvable dans %s — format changé ?",
                        path.toAbsolutePath())
                .isTrue();
        String registryBody = blockMatcher.group(1);

        // 1. Recense toutes les entrées (avec ou sans displayLabel)
        Set<String> allEntries = new LinkedHashSet<>();
        Matcher anyMatcher = TOOL_REGISTRY_ENTRY_ANY.matcher(registryBody);
        while (anyMatcher.find()) {
            allEntries.add(anyMatcher.group(1));
        }
        assertThat(allEntries)
                .withFailMessage("Aucune entrée TOOL_REGISTRY extraite — vérifier le regex.")
                .isNotEmpty();

        // 2. Recense les entrées avec displayLabel
        Map<String, String> entriesWithLabel = new LinkedHashMap<>();
        Matcher labelMatcher = TOOL_REGISTRY_ENTRY_WITH_LABEL.matcher(registryBody);
        while (labelMatcher.find()) {
            String toolId = labelMatcher.group(1);
            String label = labelMatcher.group(2).replace("\\'", "'");
            entriesWithLabel.put(toolId, label);
        }

        // 3. Identifier les entrées sans displayLabel
        List<String> missing = new ArrayList<>();
        for (String toolId : allEntries) {
            if (!entriesWithLabel.containsKey(toolId)) {
                missing.add(toolId);
            }
        }
        assertThat(missing)
                .withFailMessage(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("SF-238-01 — entrées TOOL_REGISTRY SANS `displayLabel` (")
                      .append(missing.size())
                      .append("/")
                      .append(allEntries.size())
                      .append(") :\n");
                    missing.forEach(t -> sb.append(" - ").append(t).append("\n"));
                    sb.append("\nAction requise : ajouter `displayLabel: 'Libellé humain (FR/Belgique)'`\n")
                      .append("juste après l'accolade ouvrante de chaque entrée concernée\n")
                      .append("dans decisional-tools-panel.component.ts.\n");
                    return sb.toString();
                })
                .isEmpty();

        // 4. Vérifier qu'aucun displayLabel n'est vide
        List<String> emptyLabels = new ArrayList<>();
        for (Map.Entry<String, String> e : entriesWithLabel.entrySet()) {
            if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                emptyLabels.add(e.getKey());
            }
        }
        assertThat(emptyLabels)
                .withFailMessage("SF-238-01 — `displayLabel` vide sur les entrées : %s", emptyLabels)
                .isEmpty();

        // 5. Vérifier qu'aucun displayLabel ne contient son propre tool_id
        // (anti copier-coller paresseux du genre displayLabel: 'F-DT-08-licenciement-validity').
        List<String> selfReferencing = new ArrayList<>();
        for (Map.Entry<String, String> e : entriesWithLabel.entrySet()) {
            if (e.getValue().contains(e.getKey())) {
                selfReferencing.add(e.getKey() + " → " + e.getValue());
            }
        }
        assertThat(selfReferencing)
                .withFailMessage(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("SF-238-01 — `displayLabel` contient le `tool_id` (copier-coller interdit) :\n");
                    selfReferencing.forEach(t -> sb.append(" - ").append(t).append("\n"));
                    sb.append("\nAction requise : remplacer par un libellé humain (ex. 'Licenciement — validité (FR)').\n");
                    return sb.toString();
                })
                .isEmpty();
    }
}
