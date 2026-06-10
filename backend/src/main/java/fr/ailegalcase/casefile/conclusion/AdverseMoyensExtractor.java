package fr.ailegalcase.casefile.conclusion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AiCallContext;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.analysis.JobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * F-261 / SF-261-02 — extracteur paresseux des moyens de la partie adverse à
 * partir du texte de ses écritures (documents marqués {@code adverse_pleadings},
 * SF-261-01).
 *
 * <p>Switch par domaine (SF-261-04) : <strong>travail FR</strong>,
 * <strong>immigration FR</strong> et <strong>famille FR</strong> sont couverts —
 * chacun déclenche un appel LLM d'extraction avec son prompt dédié
 * ({@code temperature = 0} via {@link AnthropicService#analyze}). Les autres
 * combinaisons domaine/pays (Belgique notamment) renvoient une liste vide : le
 * framework est prêt, leurs prompts dédiés viendront dans des vagues suivantes.</p>
 *
 * <p><strong>Fail-open</strong> (invariant « silence &gt; erreur », cohérent
 * F-179 / SF-98-56) : tout échec — texte vide, IA indisponible, JSON illisible,
 * moyen non identifiable — aboutit à une liste vide journalisée, jamais à une
 * exception propagée. À défaut de moyens fiables, aucune section n'est injectée
 * et la génération se poursuit normalement.</p>
 *
 * <p>Gate Anthropic (F-257) : l'appel passe obligatoirement par un
 * {@link AiCallContext} system-level rattaché au dossier
 * ({@link JobType#SYSTEM_CASE_CONCLUSION}, même JobType que la génération qui
 * l'enveloppe — l'extraction est une étape interne de la génération de
 * conclusions, pas un job distinct).</p>
 */
@Component
public class AdverseMoyensExtractor {

    private static final Logger log = LoggerFactory.getLogger(AdverseMoyensExtractor.class);

    /** Budget de sortie de l'extraction structurée (liste de moyens en JSON). */
    static final int MAX_TOKENS = 2000;

    static final String DROIT_DU_TRAVAIL = "DROIT_DU_TRAVAIL";
    static final String DROIT_IMMIGRATION = "DROIT_IMMIGRATION";
    static final String DROIT_FAMILLE = "DROIT_FAMILLE";
    static final String FRANCE = "FRANCE";

    /**
     * Prompt d'extraction dédié travail FR. Demande les moyens RÉELS du document
     * adverse (thèse + fondements + pièces), strictement issus du texte, sous la
     * forme d'un tableau JSON. Anti-invention impérative.
     */
    static final String SYSTEM_PROMPT_TRAVAIL_FR = """
            Tu es un juriste senior spécialisé en droit du travail français.
            On te transmet le texte des écritures (conclusions / requête) de la
            PARTIE ADVERSE dans un litige prud'homal. Ta tâche : extraire les
            MOYENS que l'adversaire soutient, c'est-à-dire ses arguments de droit.

            Pour CHAQUE moyen identifié, restitue :
            - "these" : la thèse soutenue par l'adversaire (ce qu'il demande au juge
              de retenir), en une phrase claire et neutre.
            - "fondements" : les fondements juridiques qu'il invoque à l'appui
              (articles du Code du travail / Code civil, principes, conventions
              collectives, jurisprudence nommée). Liste de chaînes ; vide si aucun.
            - "piecesInvoquees" : les pièces sur lesquelles il s'appuie (intitulés
              ou numéros de pièces qu'il cite). Liste de chaînes ; vide si aucune.

            RÈGLES IMPÉRATIVES — anti-invention (« silence > erreur ») :
            - N'invente RIEN. N'extrais que des moyens RÉELLEMENT présents dans le
              texte. Ne déduis pas un moyen que l'adversaire n'a pas soulevé.
            - Ne reformule pas en ta faveur : restitue la thèse telle que
              l'adversaire la défend, sans la réfuter ni la commenter.
            - Si le texte ne contient aucun moyen identifiable, renvoie un tableau
              vide [].

            RÈGLE DE FORMAT ABSOLUE — non négociable :
            Réponds UNIQUEMENT par un tableau JSON commençant par « [ » et finissant
            par « ] ». AUCUN texte avant le crochet ouvrant. AUCUN texte après le
            crochet fermant. PAS de balises markdown (```json), PAS de préambule.

            Schéma de chaque élément :
            {"these":"<phrase>","fondements":["<...>"],"piecesInvoquees":["<...>"]}

            Exemple :
            [{"these":"Le licenciement repose sur une faute grave justifiant l'absence d'indemnités.","fondements":["art. L. 1234-1 du Code du travail","art. L. 1234-9 du Code du travail"],"piecesInvoquees":["Lettre de licenciement","Attestations de témoins"]}]
            """;

    /**
     * Prompt d'extraction dédié immigration FR (contentieux des étrangers). Demande
     * les moyens RÉELS de la partie adverse — typiquement l'administration / la
     * préfecture qui défend l'OQTF, le refus de titre ou le rejet d'asile — sous la
     * forme d'un tableau JSON. Anti-invention impérative, même format que travail FR.
     */
    static final String SYSTEM_PROMPT_IMMIGRATION_FR = """
            Tu es un juriste senior spécialisé en droit des étrangers français
            (contentieux administratif des étrangers : OQTF, refus de titre de
            séjour, rejet d'asile, rétention, expulsion). On te transmet le texte
            des écritures (mémoire en défense / observations) de la PARTIE ADVERSE
            — le plus souvent l'administration (préfecture, ministère de l'Intérieur,
            OFPRA) qui défend sa décision. Ta tâche : extraire les MOYENS que
            l'adversaire soutient, c'est-à-dire ses arguments de droit.

            Pour CHAQUE moyen identifié, restitue :
            - "these" : la thèse soutenue par l'adversaire (ce qu'il demande au juge
              de retenir, ex. « la décision est légale », « le moyen est inopérant »),
              en une phrase claire et neutre.
            - "fondements" : les fondements juridiques qu'il invoque à l'appui
              (articles du CESEDA, Code de justice administrative, conventions —
              CEDH, convention de Genève —, principes, jurisprudence nommée). Liste
              de chaînes ; vide si aucun.
            - "piecesInvoquees" : les pièces sur lesquelles il s'appuie (intitulés
              ou numéros de pièces qu'il cite). Liste de chaînes ; vide si aucune.

            RÈGLES IMPÉRATIVES — anti-invention (« silence > erreur ») :
            - N'invente RIEN. N'extrais que des moyens RÉELLEMENT présents dans le
              texte. Ne déduis pas un moyen que l'adversaire n'a pas soulevé.
            - Ne reformule pas en ta faveur : restitue la thèse telle que
              l'adversaire la défend, sans la réfuter ni la commenter.
            - Si le texte ne contient aucun moyen identifiable, renvoie un tableau
              vide [].

            RÈGLE DE FORMAT ABSOLUE — non négociable :
            Réponds UNIQUEMENT par un tableau JSON commençant par « [ » et finissant
            par « ] ». AUCUN texte avant le crochet ouvrant. AUCUN texte après le
            crochet fermant. PAS de balises markdown (```json), PAS de préambule.

            Schéma de chaque élément :
            {"these":"<phrase>","fondements":["<...>"],"piecesInvoquees":["<...>"]}

            Exemple :
            [{"these":"L'obligation de quitter le territoire est légale, l'étranger ne justifiant pas d'une vie privée et familiale protégée.","fondements":["art. L. 611-1 du CESEDA","art. 8 de la CEDH","art. L. 423-23 du CESEDA"],"piecesInvoquees":["Arrêté préfectoral du 12 mars 2026","Relevé de la situation administrative"]}]
            """;

    /**
     * Prompt d'extraction dédié famille FR (contentieux familial). Demande les
     * moyens RÉELS de la partie adverse — l'autre époux / parent (divorce, autorité
     * parentale, pension alimentaire, prestation compensatoire) — sous la forme d'un
     * tableau JSON. Anti-invention impérative, même format que travail FR.
     */
    static final String SYSTEM_PROMPT_FAMILLE_FR = """
            Tu es un juriste senior spécialisé en droit de la famille français
            (divorce, séparation, autorité parentale, résidence de l'enfant, pension
            alimentaire / contribution à l'entretien et l'éducation, prestation
            compensatoire, liquidation du régime matrimonial). On te transmet le
            texte des écritures (conclusions / requête) de la PARTIE ADVERSE — l'autre
            époux ou parent. Ta tâche : extraire les MOYENS que l'adversaire soutient,
            c'est-à-dire ses arguments de droit.

            Pour CHAQUE moyen identifié, restitue :
            - "these" : la thèse soutenue par l'adversaire (ce qu'il demande au juge
              de retenir, ex. « le divorce doit être prononcé aux torts exclusifs »,
              « la résidence de l'enfant doit être fixée chez lui »), en une phrase
              claire et neutre.
            - "fondements" : les fondements juridiques qu'il invoque à l'appui
              (articles du Code civil, principes, jurisprudence nommée). Liste de
              chaînes ; vide si aucun.
            - "piecesInvoquees" : les pièces sur lesquelles il s'appuie (intitulés
              ou numéros de pièces qu'il cite). Liste de chaînes ; vide si aucune.

            RÈGLES IMPÉRATIVES — anti-invention (« silence > erreur ») :
            - N'invente RIEN. N'extrais que des moyens RÉELLEMENT présents dans le
              texte. Ne déduis pas un moyen que l'adversaire n'a pas soulevé.
            - Ne reformule pas en ta faveur : restitue la thèse telle que
              l'adversaire la défend, sans la réfuter ni la commenter.
            - Si le texte ne contient aucun moyen identifiable, renvoie un tableau
              vide [].

            RÈGLE DE FORMAT ABSOLUE — non négociable :
            Réponds UNIQUEMENT par un tableau JSON commençant par « [ » et finissant
            par « ] ». AUCUN texte avant le crochet ouvrant. AUCUN texte après le
            crochet fermant. PAS de balises markdown (```json), PAS de préambule.

            Schéma de chaque élément :
            {"these":"<phrase>","fondements":["<...>"],"piecesInvoquees":["<...>"]}

            Exemple :
            [{"these":"La résidence habituelle de l'enfant doit être fixée au domicile de la mère, dans l'intérêt supérieur de l'enfant.","fondements":["art. 373-2-9 du Code civil","art. 373-2-11 du Code civil"],"piecesInvoquees":["Attestation de l'école","Certificat médical du 5 février 2026"]}]
            """;

    private final AnthropicService anthropicService;
    private final ObjectMapper objectMapper;

    public AdverseMoyensExtractor(AnthropicService anthropicService, ObjectMapper objectMapper) {
        this.anthropicService = anthropicService;
        this.objectMapper = objectMapper;
    }

    /**
     * Extrait les moyens adverses des textes fournis, selon le domaine / pays.
     *
     * @param textesAdverses textes extraits des documents marqués « écritures adverses »
     *                       (peut être {@code null} / vide)
     * @param domain         domaine juridique du dossier (clé {@code DROIT_DU_TRAVAIL} / …)
     * @param country        pays du workspace (clé {@code FRANCE} / …)
     * @param caseFileId     dossier rattaché, propagé au {@link AiCallContext} (traçabilité)
     * @return les moyens adverses extraits (jamais {@code null} ; vide hors domaine
     *         couvert, hors texte exploitable, ou en cas d'échec — fail-open)
     */
    public List<AdverseMoyen> extract(List<String> textesAdverses, String domain, String country,
                                      java.util.UUID caseFileId) {
        String systemPrompt = systemPromptFor(domain, country);
        if (systemPrompt == null) {
            // Domaine/pays non couvert (BE, autres) : framework prêt, prompt à venir.
            return List.of();
        }
        String texte = joinNonBlank(textesAdverses);
        if (texte.isBlank()) {
            return List.of();
        }
        try {
            AiCallContext ctx = AiCallContext.systemLevel(JobType.SYSTEM_CASE_CONCLUSION, caseFileId);
            AnthropicResult result = anthropicService.analyze(
                    ctx, systemPrompt, texte, MAX_TOKENS);
            if (result == null || result.content() == null || result.content().isBlank()) {
                log.warn("Extraction des moyens adverses : réponse IA vide — génération sans section.");
                return List.of();
            }
            return parseMoyens(result.content());
        } catch (Exception e) {
            log.warn("Extraction des moyens adverses indisponible — génération sans section : {}",
                    e.getMessage());
            return List.of();
        }
    }

    /**
     * Sélectionne le prompt système d'extraction selon le domaine et le pays.
     * Couvert en V1 : travail / immigration / famille en {@code FRANCE}. Toute autre
     * combinaison (Belgique notamment) renvoie {@code null} → extraction no-op.
     */
    static String systemPromptFor(String domain, String country) {
        if (!FRANCE.equals(country)) {
            return null;
        }
        if (DROIT_DU_TRAVAIL.equals(domain)) {
            return SYSTEM_PROMPT_TRAVAIL_FR;
        }
        if (DROIT_IMMIGRATION.equals(domain)) {
            return SYSTEM_PROMPT_IMMIGRATION_FR;
        }
        if (DROIT_FAMILLE.equals(domain)) {
            return SYSTEM_PROMPT_FAMILLE_FR;
        }
        return null;
    }

    /** Concatène les textes non vides, séparés, en ignorant null / vides. */
    private static String joinNonBlank(List<String> textes) {
        if (textes == null || textes.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String t : textes) {
            if (t != null && !t.isBlank()) {
                if (sb.length() > 0) {
                    sb.append("\n\n---\n\n");
                }
                sb.append(t.strip());
            }
        }
        return sb.toString();
    }

    /**
     * Parse le tableau JSON des moyens. Robuste / fail-open : un JSON illisible ou
     * un élément mal formé n'interrompt pas l'extraction (élément ignoré), et un
     * échec global renvoie une liste vide.
     */
    List<AdverseMoyen> parseMoyens(String content) {
        String json = extractJsonArray(content);
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) {
                log.warn("Extraction des moyens adverses : JSON non tableau — génération sans section.");
                return List.of();
            }
            List<AdverseMoyen> moyens = new ArrayList<>();
            for (JsonNode node : root) {
                String these = node.path("these").asText("").strip();
                if (these.isBlank()) {
                    continue; // moyen non identifiable → ignoré (anti-invention)
                }
                moyens.add(new AdverseMoyen(
                        these,
                        textList(node.path("fondements")),
                        textList(node.path("piecesInvoquees"))));
            }
            return List.copyOf(moyens);
        } catch (Exception e) {
            log.warn("Extraction des moyens adverses : parsing JSON invalide — génération sans section : {}",
                    e.getMessage());
            return List.of();
        }
    }

    /** Liste de chaînes non vides d'un nœud tableau (vide si absent / non tableau). */
    private static List<String> textList(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray() || arrayNode.isEmpty()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            String value = item.asText("").strip();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    /** Isole le tableau JSON ({@code [ … ]}) d'une réponse éventuellement bavarde. */
    private static String extractJsonArray(String text) {
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}
