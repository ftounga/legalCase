package fr.ailegalcase.casefile.conclusion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.casefile.DashboardTile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * F-98 — assemble le prompt système et le message utilisateur du générateur de
 * conclusions.
 *
 * <p>Le prompt système porte les instructions de rédaction (stables, donc cachables
 * par {@code AnthropicService.analyzeWithSystemCache}). Le message utilisateur porte
 * les données du dossier : stade procédural, synthèse, pièces numérotées, verdicts des
 * outils décisionnels et pistes stratégiques retenues.</p>
 *
 * <p>Le prompt système de base dépend de la cellule de matrice
 * ({@link CombinationKey}) : il est fourni par le {@link ConclusionPromptProvider}
 * correspondant, résolu via {@link ConclusionPromptRegistry}. La consigne de style
 * F-98-47 est appliquée par-dessus, identiquement pour toutes les cellules.</p>
 */
@Component
public class CaseConclusionPromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(CaseConclusionPromptBuilder.class);

    /**
     * F-98 / SF-98-47 — en-tête de la consigne d'adaptation de style, injectée au prompt
     * système quand le cabinet dispose d'au moins une signature de style active.
     */
    static final String STYLE_INSTRUCTION_HEADER =
            "Adopte le style rédactionnel suivant, appris des conclusions de l'avocat :";

    /**
     * F-242 / SF-242-01 — garde anti-hallucination transverse sur la jurisprudence,
     * appliquée à toutes les cellules de matrice (symétrique du « n'invente aucun
     * chiffre » des prompts de base). L'IA ne doit citer que les références qui lui ont
     * été explicitement fournies dans la section JURISPRUDENCE À L'APPUI.
     */
    static final String JURISPRUDENCE_GUARD =
            "Garde jurisprudence : ne cite aucune référence de jurisprudence (arrêt, "
                    + "décision, numéro de pourvoi) qui ne figure pas dans l'une des deux "
                    + "sections « JURISPRUDENCE À L'APPUI » (F-242, références saisies par "
                    + "l'avocat) ou « JURISPRUDENCE APPLICABLE PAR OUTIL » (F-JU-02, arrêts "
                    + "curatés par LegalCase pour les outils décisionnels utilisés). Si l'une "
                    + "de ces sections contient des références, appuie-toi exclusivement sur "
                    + "celles-ci ; sinon, n'invente aucun arrêt ni décision.";

    private final ObjectMapper objectMapper;
    private final ConclusionPromptRegistry promptRegistry;

    public CaseConclusionPromptBuilder(ObjectMapper objectMapper,
                                       ConclusionPromptRegistry promptRegistry) {
        this.objectMapper = objectMapper;
        this.promptRegistry = promptRegistry;
    }

    /**
     * F-98 / SF-98-47 — assemble le prompt système de la cellule {@code key} en y
     * intégrant, le cas échéant, une consigne d'adaptation au style rédactionnel appris
     * du cabinet.
     *
     * <p>Le prompt de base provient de la cellule de matrice correspondant à {@code key}
     * (registre {@link ConclusionPromptRegistry}). Quand {@code styleSignatures} contient
     * au moins une description de style non vide, le prompt système reprend ces
     * descriptions sous une consigne « adopte le style rédactionnel suivant... ». Sans
     * signature exploitable, le prompt système est le prompt de base seul.</p>
     *
     * @param key             cellule de matrice du dossier (jamais {@code null})
     * @param styleSignatures descriptions de style actives du workspace (jamais
     *                        {@code null} ; les entrées {@code null} / vides sont ignorées)
     * @return le prompt système, enrichi de la consigne de style si applicable
     * @throws IllegalStateException si aucune cellule ne couvre {@code key}
     */
    public String buildSystemPrompt(CombinationKey key, List<String> styleSignatures) {
        String basePrompt = promptRegistry.systemPrompt(key)
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun provider de prompt pour la combinaison " + key));
        // F-242 — garde anti-hallucination jurisprudence, appliquée à toutes les cellules.
        StringBuilder sb = new StringBuilder(basePrompt);
        sb.append('\n').append(JURISPRUDENCE_GUARD).append('\n');
        List<String> usable = sanitizeSignatures(styleSignatures);
        if (!usable.isEmpty()) {
            sb.append(STYLE_INSTRUCTION_HEADER).append('\n');
            for (String signature : usable) {
                sb.append("- ").append(signature.strip()).append('\n');
            }
        }
        return sb.toString();
    }

    /** Filtre les signatures de style exploitables (non nulles, non vides). */
    private static List<String> sanitizeSignatures(List<String> styleSignatures) {
        if (styleSignatures == null || styleSignatures.isEmpty()) {
            return List.of();
        }
        List<String> usable = new java.util.ArrayList<>();
        for (String signature : styleSignatures) {
            if (signature != null && !signature.isBlank()) {
                usable.add(signature);
            }
        }
        return usable;
    }

    /**
     * Assemble le message utilisateur à partir des données du dossier.
     *
     * @param input agrégat des intrants du dossier (jamais {@code null} ; ses champs
     *              listes peuvent être vides)
     * @return le message utilisateur prêt pour l'appel IA
     */
    public String buildUserMessage(ConclusionPromptInput input) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== DOSSIER ===\n");
        sb.append("Intitulé du dossier : ").append(nullSafe(input.caseTitle())).append('\n');
        sb.append("Stade procédural : ").append(nullSafe(input.jurisdictionLabel()))
                .append(" — ").append(nullSafe(input.stageLabel()))
                .append(" — ").append(nullSafe(input.positionLabel())).append('\n');

        appendSynthesis(sb, input.analysisResultJson());

        sb.append("\n=== PIÈCES NUMÉROTÉES DU DOSSIER ===\n");
        if (input.pieces() == null || input.pieces().isEmpty()) {
            sb.append("Aucune pièce numérotée identifiée.\n");
        } else {
            for (ConclusionPromptInput.NumberedPiece p : input.pieces()) {
                sb.append("Pièce n° ").append(p.number()).append(" — ")
                        .append(nullSafe(p.label()));
                if (p.type() != null) {
                    sb.append(" (").append(p.type()).append(')');
                }
                sb.append('\n');
            }
        }

        sb.append("\n=== VERDICTS DES OUTILS DÉCISIONNELS REMPLIS ===\n");
        if (input.toolTiles() == null || input.toolTiles().isEmpty()) {
            sb.append("Aucun outil décisionnel rempli sur ce dossier.\n");
        } else {
            for (DashboardTile t : input.toolTiles()) {
                sb.append("- ").append(nullSafe(t.label())).append(" : ")
                        .append(nullSafe(t.primaryValue()));
                if (t.secondaryValue() != null && !t.secondaryValue().isBlank()) {
                    sb.append(" — ").append(t.secondaryValue());
                }
                sb.append('\n');
            }
        }

        sb.append("\n=== PISTES STRATÉGIQUES RETENUES ===\n");
        if (input.retainedStrategies() == null || input.retainedStrategies().isEmpty()) {
            sb.append("Aucune piste stratégique retenue.\n");
        } else {
            for (ConclusionPromptInput.RetainedStrategy s : input.retainedStrategies()) {
                sb.append("- ").append(nullSafe(s.texte()));
                if (s.baseJuridique() != null && !s.baseJuridique().isBlank()) {
                    sb.append(" [Base juridique : ").append(s.baseJuridique()).append(']');
                }
                sb.append('\n');
            }
        }

        appendJurisprudenceCitations(sb, input.jurisprudenceCitations());
        appendToolJurisprudenceCitations(sb, input.toolJurisprudenceByTool());

        return sb.toString();
    }

    /**
     * F-JU-02 / SF-JU-02-01 — section automatique des arrêts mappés F-JU-01
     * par outil décisionnel utilisé sur le dossier. Section absente si vide.
     */
    private void appendToolJurisprudenceCitations(
            StringBuilder sb,
            List<fr.ailegalcase.jurisprudencemapping.ToolJurisprudenceCitationByTool> byTool) {
        if (byTool == null || byTool.isEmpty()) {
            return;
        }
        sb.append("\n=== JURISPRUDENCE APPLICABLE PAR OUTIL ===\n");
        sb.append("(Arrêts curatés par LegalCase pour les outils décisionnels utilisés — citation indicative, l'avocat reste seul juge.)\n");
        for (fr.ailegalcase.jurisprudencemapping.ToolJurisprudenceCitationByTool entry : byTool) {
            if (entry == null || entry.citations() == null || entry.citations().isEmpty()) {
                continue;
            }
            sb.append("\nOutil : ").append(nullSafe(entry.toolId()));
            if (entry.brancheCalculId() != null && !entry.brancheCalculId().isBlank()) {
                sb.append(" — branche : ").append(entry.brancheCalculId());
            }
            sb.append('\n');
            for (var citation : entry.citations()) {
                sb.append("  - ").append(nullSafe(citation.arretRef()));
                if (citation.chapeauOfficiel() != null && !citation.chapeauOfficiel().isBlank()) {
                    sb.append(" — « ").append(citation.chapeauOfficiel().strip()).append(" »");
                }
                sb.append('\n');
            }
        }
    }

    /**
     * F-242 / SF-242-01 — ajoute la section des citations de jurisprudence d'appui,
     * regroupées par point juridique : pour chaque point, son texte (snapshot) puis ses
     * références numérotées avec leur portée. L'ordre des points suit l'ordre d'arrivée
     * des citations (déjà trié par index de point juridique côté repository).
     *
     * <p>Sans citation, la section indique explicitement l'absence de référence — pour
     * que la garde anti-hallucination du prompt système soit sans ambiguïté.</p>
     */
    private void appendJurisprudenceCitations(
            StringBuilder sb,
            List<ConclusionPromptInput.JurisprudenceCitationForPrompt> citations) {
        sb.append("\n=== JURISPRUDENCE À L'APPUI ===\n");
        if (citations == null || citations.isEmpty()) {
            sb.append("Aucune référence de jurisprudence fournie.\n");
            return;
        }
        // Regroupement par point juridique (index + snapshot du texte), ordre d'arrivée préservé.
        java.util.Map<String, List<ConclusionPromptInput.JurisprudenceCitationForPrompt>> byPoint =
                new java.util.LinkedHashMap<>();
        for (ConclusionPromptInput.JurisprudenceCitationForPrompt c : citations) {
            if (c == null) {
                continue;
            }
            String groupKey = c.pointJuridiqueIndex() + "|" + nullSafe(c.pointJuridiqueTexte());
            byPoint.computeIfAbsent(groupKey, k -> new java.util.ArrayList<>()).add(c);
        }
        for (List<ConclusionPromptInput.JurisprudenceCitationForPrompt> group : byPoint.values()) {
            ConclusionPromptInput.JurisprudenceCitationForPrompt first = group.get(0);
            sb.append("\nPoint juridique : ").append(nullSafe(first.pointJuridiqueTexte())).append('\n');
            for (ConclusionPromptInput.JurisprudenceCitationForPrompt c : group) {
                sb.append("  - ").append(nullSafe(c.reference()));
                if (c.portee() != null && !c.portee().isBlank()) {
                    sb.append(" — Portée : ").append(c.portee().strip());
                }
                sb.append('\n');
            }
        }
    }

    /**
     * Extrait les sections faits / points juridiques / risques du JSON de synthèse et
     * les ajoute au message. Fail-open : un JSON absent ou illisible n'interrompt pas
     * l'assemblage (la section synthèse est simplement marquée indisponible).
     */
    private void appendSynthesis(StringBuilder sb, String analysisResultJson) {
        sb.append("\n=== SYNTHÈSE DU DOSSIER ===\n");
        if (analysisResultJson == null || analysisResultJson.isBlank()) {
            sb.append("Synthèse indisponible.\n");
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(analysisResultJson);
            appendTextList(sb, "Faits", root.path("faits"));
            appendTextList(sb, "Points juridiques", root.path("points_juridiques"));
            appendTextList(sb, "Risques", root.path("risques"));
        } catch (Exception ex) {
            log.warn("Synthèse JSON illisible pour la génération de conclusions : {}", ex.getMessage());
            sb.append("Synthèse indisponible (format inattendu).\n");
        }
    }

    /**
     * Ajoute une liste de la synthèse. Chaque élément peut être une chaîne (format legacy)
     * ou un objet portant un champ {@code texte}.
     */
    private void appendTextList(StringBuilder sb, String title, JsonNode arrayNode) {
        sb.append('\n').append(title).append(" :\n");
        if (arrayNode == null || !arrayNode.isArray() || arrayNode.isEmpty()) {
            sb.append("  (aucun)\n");
            return;
        }
        for (JsonNode item : arrayNode) {
            String texte;
            if (item.isTextual()) {
                texte = item.asText();
            } else {
                texte = item.path("texte").asText("");
            }
            if (!texte.isBlank()) {
                sb.append("  - ").append(texte).append('\n');
            }
        }
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    /**
     * F-98 / SF-98-01 — agrégat des intrants du dossier pour la construction du prompt.
     *
     * @param caseTitle               intitulé du dossier
     * @param jurisdictionLabel       libellé humain de la juridiction
     * @param stageLabel              libellé humain du stade
     * @param positionLabel           libellé humain de la position
     * @param analysisResultJson      JSON brut de la synthèse {@code DONE} la plus récente
     * @param pieces                  pièces numérotées du dossier (ordre stable)
     * @param toolTiles               verdicts des outils décisionnels remplis
     * @param retainedStrategies      pistes stratégiques au statut {@code RETAINED}
     * @param jurisprudenceCitations  F-242 — citations de jurisprudence d'appui saisies
     *                                par l'avocat (ordre stable par point juridique)
     */
    public record ConclusionPromptInput(
            String caseTitle,
            String jurisdictionLabel,
            String stageLabel,
            String positionLabel,
            String analysisResultJson,
            List<NumberedPiece> pieces,
            List<DashboardTile> toolTiles,
            List<RetainedStrategy> retainedStrategies,
            List<JurisprudenceCitationForPrompt> jurisprudenceCitations,
            List<fr.ailegalcase.jurisprudencemapping.ToolJurisprudenceCitationByTool> toolJurisprudenceByTool) {

        /** F-JU-02 / SF-JU-02-01 — constructeur back-compat (pré-F-JU-02). */
        public ConclusionPromptInput(
                String caseTitle, String jurisdictionLabel, String stageLabel, String positionLabel,
                String analysisResultJson, List<NumberedPiece> pieces, List<DashboardTile> toolTiles,
                List<RetainedStrategy> retainedStrategies,
                List<JurisprudenceCitationForPrompt> jurisprudenceCitations) {
            this(caseTitle, jurisdictionLabel, stageLabel, positionLabel, analysisResultJson,
                    pieces, toolTiles, retainedStrategies, jurisprudenceCitations, java.util.List.of());
        }

        /** Pièce numérotée du dossier. */
        public record NumberedPiece(int number, String label, String type) {
        }

        /** Piste stratégique retenue par l'avocat. */
        public record RetainedStrategy(String texte, String baseJuridique) {
        }

        /**
         * F-242 / SF-242-01 — citation de jurisprudence d'appui rattachée à un point
         * juridique de la synthèse.
         *
         * @param pointJuridiqueIndex index du point juridique dans la synthèse
         * @param pointJuridiqueTexte snapshot du texte du point juridique
         * @param reference           libellé de la référence
         * @param portee              ligne de portée ({@code null} si non renseignée)
         */
        public record JurisprudenceCitationForPrompt(
                int pointJuridiqueIndex,
                String pointJuridiqueTexte,
                String reference,
                String portee) {
        }
    }
}
