package fr.ailegalcase.jurisprudencemapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AiCallContext;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.analysis.JobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * F-JU-06 / SF-JU-06-01 — 2ᵉ passe de pertinence sémantique.
 *
 * <p>Après que {@link ClaudeJurisprudenceEvaluator} a sélectionné un arrêt, ce
 * garde-fou confronte le <strong>sujet métier de l'outil</strong> (mot-clé de
 * recherche de l'entrée bootstrap) au <strong>contenu réel de l'arrêt</strong>
 * (référence + chapeau) et tranche : cet arrêt FONDE-t-il vraiment cet outil ?</p>
 *
 * <p>Indépendant de l'évaluateur (qui a un biais « choisir ») : ce gate n'a
 * qu'un rôle, REJETER les hors-sujet. Le {@code confidence_score} numérique ne
 * suffit pas (l'arrêt « restauration ferroviaire » du comparateur F-DT-09 était
 * à 0,72) — il faut un jugement sémantique dédié.</p>
 *
 * <p>Invariant « silence &gt; erreur » : sur échec LLM / parsing, le verdict par
 * défaut est <strong>non pertinent</strong> (on ne mappe pas un arrêt douteux).</p>
 */
@Component
public class JurisprudenceRelevanceGate {

    private static final Logger log = LoggerFactory.getLogger(JurisprudenceRelevanceGate.class);
    private static final int MAX_TOKENS = 300;
    private static final int MAX_CHAPEAU = 1500;

    private static final String SYSTEM_PROMPT = """
            Tu es un juriste senior français. On te donne le SUJET d'un outil
            décisionnel d'avocat et UN arrêt candidat (référence + chapeau).
            Question unique : cet arrêt FONDE-t-il réellement cet outil, c'est-à-dire
            porte-t-il directement sur la situation juridique traitée par l'outil ?

            Sois STRICT (« silence > erreur ») : si l'arrêt ne traite que d'un thème
            voisin, d'une convention collective / d'un régime / d'une matière sans
            rapport direct avec le sujet, ou si tu as un doute → pertinent = false.

            Réponds UNIQUEMENT par un JSON, sans texte autour, sans markdown :
            {"pertinent": true | false, "raison": "<une phrase>"}
            """;

    private final AnthropicService anthropic;
    private final ObjectMapper objectMapper;

    public JurisprudenceRelevanceGate(AnthropicService anthropic, ObjectMapper objectMapper) {
        this.anthropic = anthropic;
        this.objectMapper = objectMapper;
    }

    /**
     * @param sujetOutil description métier du sujet de l'outil (mot-clé de recherche bootstrap)
     * @param arret      arrêt choisi par l'évaluateur
     * @return verdict de pertinence — {@code false} par défaut sur échec (silence &gt; erreur)
     */
    public RelevanceVerdict assess(String sujetOutil, JudilibreArret arret) {
        if (arret == null) {
            return new RelevanceVerdict(false, "Aucun arrêt à évaluer");
        }
        String chapeau = arret.chapeauOfficiel() == null ? "" : arret.chapeauOfficiel();
        if (chapeau.length() > MAX_CHAPEAU) {
            chapeau = chapeau.substring(0, MAX_CHAPEAU);
        }
        String userMessage = """
                SUJET DE L'OUTIL : %s
                ARRÊT CANDIDAT :
                - référence : %s
                - juridiction : %s
                - chapeau : %s
                """.formatted(
                sujetOutil == null ? "" : sujetOutil,
                arret.ref() == null ? "" : arret.ref(),
                arret.juridiction() == null ? "" : arret.juridiction(),
                chapeau.isBlank() ? "(aucun chapeau)" : chapeau);

        AnthropicResult result;
        try {
            AiCallContext ctx = AiCallContext.systemLevel(JobType.SYSTEM_JP_BOOTSTRAP);
            result = anthropic.analyze(ctx, SYSTEM_PROMPT, userMessage, MAX_TOKENS);
        } catch (Exception e) {
            log.warn("F-JU-06 — RelevanceGate anthropic call failed, rejet par défaut: {}", e.getMessage());
            return new RelevanceVerdict(false, "Vérification de pertinence indisponible: " + e.getMessage());
        }
        if (result == null || result.content() == null || result.content().isBlank()) {
            return new RelevanceVerdict(false, "Réponse de pertinence vide");
        }
        return parse(result.content());
    }

    RelevanceVerdict parse(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        String json = (start >= 0 && end > start) ? text.substring(start, end + 1) : text;
        try {
            JsonNode root = objectMapper.readTree(json);
            boolean pertinent = root.path("pertinent").asBoolean(false);
            String raison = root.path("raison").asText("");
            if (raison.length() > 2000) raison = raison.substring(0, 2000);
            return new RelevanceVerdict(pertinent, raison);
        } catch (Exception e) {
            log.warn("F-JU-06 — RelevanceGate parse fail, rejet par défaut: {}", e.getMessage());
            return new RelevanceVerdict(false, "Parsing pertinence invalide");
        }
    }

    /** Verdict de la 2ᵉ passe. */
    public record RelevanceVerdict(boolean pertinent, String raison) {
    }
}
