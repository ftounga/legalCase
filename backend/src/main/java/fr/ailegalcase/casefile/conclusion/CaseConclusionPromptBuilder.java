package fr.ailegalcase.casefile.conclusion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.casefile.DashboardTile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * F-98 / SF-98-01 — assemble le prompt système et le message utilisateur du
 * générateur de conclusions CPH bureau de jugement (fond), FR, demandeur, droit
 * du travail.
 *
 * <p>Le prompt système porte les instructions de rédaction (stables, donc cachables
 * par {@code AnthropicService.analyzeWithSystemCache}). Le message utilisateur porte
 * les données du dossier : stade procédural, synthèse, pièces numérotées, verdicts des
 * outils décisionnels et pistes stratégiques retenues.</p>
 *
 * <p>Conçu pour être étendu cellule par cellule par les SF-98-02→45 (un prompt système
 * distinct par combinaison juridiction/stade/position/domaine/pays).</p>
 */
@Component
public class CaseConclusionPromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(CaseConclusionPromptBuilder.class);

    /**
     * Prompt système — CPH / bureau de jugement (fond) / demandeur (salarié) / droit du
     * travail FR. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du demandeur (salarié) devant le Conseil de prud'hommes, en bureau de jugement.
            Rédige un PROJET DE CONCLUSIONS structuré :
            - en-tête (POUR [demandeur] / CONTRE [défendeur]),
            - FAITS ET PROCÉDURE,
            - DISCUSSION (moyens en droit, un paragraphe argumenté par moyen),
            - PAR CES MOTIFS (dispositif avec demandes chiffrées).
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    private final ObjectMapper objectMapper;

    public CaseConclusionPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** @return le prompt système (instructions de rédaction, cachable). */
    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
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

        return sb.toString();
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
     * @param caseTitle          intitulé du dossier
     * @param jurisdictionLabel  libellé humain de la juridiction
     * @param stageLabel         libellé humain du stade
     * @param positionLabel      libellé humain de la position
     * @param analysisResultJson JSON brut de la synthèse {@code DONE} la plus récente
     * @param pieces             pièces numérotées du dossier (ordre stable)
     * @param toolTiles          verdicts des outils décisionnels remplis
     * @param retainedStrategies pistes stratégiques au statut {@code RETAINED}
     */
    public record ConclusionPromptInput(
            String caseTitle,
            String jurisdictionLabel,
            String stageLabel,
            String positionLabel,
            String analysisResultJson,
            List<NumberedPiece> pieces,
            List<DashboardTile> toolTiles,
            List<RetainedStrategy> retainedStrategies) {

        /** Pièce numérotée du dossier. */
        public record NumberedPiece(int number, String label, String type) {
        }

        /** Piste stratégique retenue par l'avocat. */
        public record RetainedStrategy(String texte, String baseJuridique) {
        }
    }
}
