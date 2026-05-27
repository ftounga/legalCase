package fr.ailegalcase.jurisprudencemapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * F-JU-01 / SF-JU-01-02 — wrapper {@link AnthropicService} qui demande à Claude
 * Sonnet d'évaluer l'impact d'arrêts entrants sur un mapping existant.
 *
 * <p>Renvoie un {@link ClaudeEvaluation} parsé du JSON Claude. Sur échec
 * (parsing invalide, IA indisponible, confiance hors borne), renvoie un
 * {@link ClaudeEvaluation#none(String)} fallback safe (le service appelant
 * traitera comme « ne rien faire »).</p>
 */
@Component
public class ClaudeJurisprudenceEvaluator {

    private static final Logger log = LoggerFactory.getLogger(ClaudeJurisprudenceEvaluator.class);
    private static final int MAX_TOKENS = 600;
    private static final String SYSTEM_PROMPT = """
            Tu es un juriste senior spécialisé en jurisprudence française.
            Pour un mapping existant (outil décisionnel × branche × arrêt actuellement cité),
            on te présente 1 à N arrêts entrants candidats.
            Décide une action parmi : CONFIRM, ADD, REPLACE, ARCHIVE, NONE.

            Règles :
            - CONFIRM : aucun arrêt entrant ne change la donne, le mapping actuel reste valide
            - ADD : un arrêt entrant enrichit le principe sans le contredire (à ajouter en complément)
            - REPLACE : un arrêt entrant constitue un revirement clair qui rend l'arrêt actuel obsolète
            - ARCHIVE : l'arrêt actuel est explicitement censuré ou n'a plus de portée
            - NONE : aucun arrêt entrant ne concerne ce mapping

            Réponds UNIQUEMENT par un JSON :
            {
              "action": "CONFIRM" | "ADD" | "REPLACE" | "ARCHIVE" | "NONE",
              "arret_choisi_id": "<id JUDILIBRE ou null>",
              "confidence_score": 0.0 à 1.0,
              "raison": "<une phrase>"
            }
            """;

    /**
     * Prompt distinct utilisé en mode bootstrap initial — il n'existe encore aucun
     * mapping pour ce couple (outil × branche), donc CONFIRM / REPLACE / ARCHIVE
     * n'ont pas de sens. Claude doit choisir l'arrêt le plus structurant parmi
     * les candidats, et n'a le droit de renvoyer NONE que si aucun candidat
     * n'est pertinent (SF-JU-01-08).
     */
    private static final String SYSTEM_PROMPT_BOOTSTRAP = """
            Tu es un juriste senior spécialisé en jurisprudence française.
            On démarre le bootstrap initial des mappings de jurisprudence : pour un
            couple (outil décisionnel × branche de calcul), AUCUN arrêt n'est encore
            cité. On te présente 1 à N arrêts entrants candidats issus de JUDILIBRE.
            Tu dois choisir l'arrêt le plus structurant pour fonder ce mapping.

            Actions autorisées : ADD ou NONE uniquement.

            Règles (SF-JU-01-12 — assoupli vs SF-11 trop strict) :
            - ADD : sélectionne l'arrêt LE MOINS ÉLOIGNÉ du sujet parmi les
              candidats, MÊME s'il n'est pas idéal. Tout arrêt de la Cour de
              cassation qui mentionne le sujet de la branche de calcul est
              admissible. Aucune exigence de formation plénière, de publication
              au Bulletin, ou de centralité parfaite. À niveau de pertinence
              équivalent, préfère la chambre la plus adaptée (sociale pour
              droit du travail, etc.) et la date la plus récente. Renseigne
              arret_choisi_id avec son id JUDILIBRE.
            - NONE : utilisation TRÈS exceptionnelle. À réserver aux cas où
              AUCUN candidat ne porte la moindre proximité avec la branche
              de calcul. Si même un seul candidat évoque vaguement le sujet,
              choisis ADD. Le but du bootstrap est d'amorcer le mapping avec
              ce qui est disponible — la veille mensuelle ultérieure se
              chargera d'affiner avec de meilleurs arrêts au fil du temps.
              **Confidence_score** doit refléter l'incertitude (0.3-0.5 pour
              un arrêt marginal mais pertinent) — mais l'action reste ADD.

            RÈGLE DE FORMAT ABSOLUE — non négociable (SF-JU-01-11) :
            Ta réponse DOIT être un objet JSON unique commençant par « { » et
            finissant par « } ». AUCUN texte avant l'accolade ouvrante. AUCUN
            texte après l'accolade fermante. PAS de balises markdown (```json),
            PAS de préambule (« Voici… », « Je propose… »), PAS d'explication
            externe au champ "raison". Si tu hésites, mets l'analyse dans
            "raison" — mais le JSON reste la SEULE chose à produire.

            Exemple de réponse valide (arrêt central, forte confiance) :
            {"action":"ADD","arret_choisi_id":"abc123def456","confidence_score":0.85,"raison":"Cass. soc. qui pose le principe applicable à la branche."}

            Exemple de réponse valide (arrêt marginal mais admissible — toujours ADD) :
            {"action":"ADD","arret_choisi_id":"xyz789","confidence_score":0.40,"raison":"Arrêt secondaire mais mentionnant la branche — meilleur disponible."}

            Exemple NONE (très rare — aucun candidat n'évoque même vaguement le sujet) :
            {"action":"NONE","arret_choisi_id":null,"confidence_score":0.10,"raison":"Aucun candidat ne porte sur la branche (probable mismatch JUDILIBRE FR / branche BE)."}

            Schéma attendu :
            {
              "action": "ADD" | "NONE",
              "arret_choisi_id": "<id JUDILIBRE ou null>",
              "confidence_score": 0.0 à 1.0,
              "raison": "<une phrase>"
            }
            """;

    private final AnthropicService anthropic;
    private final ObjectMapper objectMapper;

    public ClaudeJurisprudenceEvaluator(AnthropicService anthropic, ObjectMapper objectMapper) {
        this.anthropic = anthropic;
        this.objectMapper = objectMapper;
    }

    public ClaudeEvaluation evaluate(ToolJurisprudenceMapping mapping, List<JudilibreArret> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return ClaudeEvaluation.none("Pas de candidat à évaluer");
        }
        String systemPrompt = pickSystemPrompt(mapping);
        String userMessage = buildUserMessage(mapping, candidates);
        AnthropicResult result;
        try {
            result = anthropic.analyze(systemPrompt, userMessage, MAX_TOKENS);
        } catch (Exception e) {
            log.warn("F-JU-01 — ClaudeJurisprudenceEvaluator anthropic call failed: {}", e.getMessage());
            return ClaudeEvaluation.none("Claude indisponible: " + e.getMessage());
        }
        if (result == null || result.content() == null || result.content().isBlank()) {
            return ClaudeEvaluation.none("Réponse Claude vide");
        }
        return parseClaudeJson(result.content(), candidates);
    }

    ClaudeEvaluation parseClaudeJson(String text, List<JudilibreArret> candidates) {
        String json = extractJson(text);
        try {
            JsonNode root = objectMapper.readTree(json);
            String actionStr = root.path("action").asText("NONE");
            EvaluationAction action;
            try {
                action = EvaluationAction.valueOf(actionStr);
            } catch (IllegalArgumentException e) {
                return ClaudeEvaluation.none("Action inconnue: " + actionStr);
            }
            String arretId = root.path("arret_choisi_id").asText(null);
            JudilibreArret arretChoisi = arretId == null || arretId.isBlank() || "null".equalsIgnoreCase(arretId)
                    ? null
                    : candidates.stream().filter(a -> arretId.equals(a.judilibreId())).findFirst().orElse(null);
            double score = root.path("confidence_score").asDouble(0.0);
            if (score < 0.0) score = 0.0;
            if (score > 1.0) score = 1.0;
            BigDecimal confidence = BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
            String raison = root.path("raison").asText("");
            if (raison.length() > 2000) raison = raison.substring(0, 2000);
            if ((action == EvaluationAction.ADD || action == EvaluationAction.REPLACE) && arretChoisi == null) {
                return ClaudeEvaluation.none("Action " + action + " sans arret_choisi_id valide");
            }
            return new ClaudeEvaluation(action, arretChoisi, confidence, raison);
        } catch (Exception e) {
            log.warn("F-JU-01 — ClaudeJurisprudenceEvaluator parse fail: {}", e.getMessage());
            return ClaudeEvaluation.none("Parsing JSON invalide");
        }
    }

    private String pickSystemPrompt(ToolJurisprudenceMapping mapping) {
        String ref = mapping == null ? null : mapping.getArretRef();
        if (ref != null && ref.startsWith(JurisprudenceBootstrapService.BOOTSTRAP_MAPPING_REF_PREFIX)) {
            return SYSTEM_PROMPT_BOOTSTRAP;
        }
        return SYSTEM_PROMPT;
    }

    private String buildUserMessage(ToolJurisprudenceMapping mapping, List<JudilibreArret> candidates) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("outil_id", mapping.getToolId());
        payload.put("branche_calcul_id", mapping.getBrancheCalculId());
        Map<String, Object> mappingActuel = new HashMap<>();
        mappingActuel.put("ref", mapping.getArretRef());
        mappingActuel.put("juridiction", mapping.getJuridiction());
        mappingActuel.put("date", String.valueOf(mapping.getDateArret()));
        mappingActuel.put("numero_pourvoi", mapping.getNumeroPourvoi());
        mappingActuel.put("chapeau", mapping.getChapeauOfficiel());
        payload.put("mapping_actuel", mappingActuel);
        List<Map<String, Object>> arretsEntrants = candidates.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.judilibreId());
            m.put("ref", a.ref());
            m.put("juridiction", a.juridiction());
            m.put("date", String.valueOf(a.dateArret()));
            m.put("numero_pourvoi", a.numeroPourvoi());
            m.put("chapeau", a.chapeauOfficiel());
            return m;
        }).toList();
        payload.put("arrets_entrants", arretsEntrants);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return payload.toString();
        }
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}
