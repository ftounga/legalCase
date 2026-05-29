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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * F-JU-04 SF-JU-04-02 — recherche de jurisprudence BELGE par mot-clé via
 * l'outil natif {@code web_search} d'Anthropic (Claude cherche sur
 * JUPORTAL / Cassation BE et retourne le résultat structuré).
 *
 * <p>Raison d'être : il n'existe <b>pas d'équivalent PISTE/JUDILIBRE</b>
 * pour la Belgique en 2026. JUPORTAL (https://juportal.be) est gratuit
 * mais sans API documentée. Le Central register BE (loi 2022) est à
 * surveiller pour une future API publique.</p>
 *
 * <p>Pattern symétrique à {@link JudilibreApiClient#fetchArretsByKeyword} pour
 * faciliter le routage côté {@link JurisprudenceBootstrapService}.</p>
 *
 * <p>Cf. mémoire {@code reference_be_jurisprudence_sources}.</p>
 */
@Component
public class JurisprudenceBeWebSearchClient {

    private static final Logger log = LoggerFactory.getLogger(JurisprudenceBeWebSearchClient.class);
    /** Limite haute du nombre d'appels web_search par recherche (coût/latence). */
    private static final int MAX_WEB_SEARCHES = 3;
    /**
     * Budget output Claude. SF-JU-04-03 : relevé de 2000 → 12000. Avec l'outil
     * web_search, les blocs {@code web_search_result} comptent dans {@code max_tokens} ;
     * 2000 étaient épuisés AVANT l'émission du JSON {@code {arrets:[…]}} →
     * {@code stop_reason=max_tokens} → JSON tronqué → parse fail → 0 arrêt.
     */
    private static final int MAX_TOKENS = 12000;

    private static final String SYSTEM_PROMPT = """
            Tu es un juriste senior spécialisé en droit belge.
            Tu disposes de l'outil web_search pour chercher sur internet.

            Ta mission : pour un mot-clé donné par l'utilisateur, trouver les
            arrêts les plus structurants de la jurisprudence BELGE.

            Sources prioritaires :
            - https://juportal.be (portail officiel des cours et tribunaux belges)
            - https://www.const-court.be (Cour constitutionnelle belge)
            - https://www.cass.be (Cour de cassation belge, si accessible)

            Approche : effectue UNE OU DEUX recherches web ciblées sur le sujet.
            Sélectionne ensuite jusqu'à 3 arrêts pertinents, en priorité ceux qui
            posent un principe applicable au mot-clé.

            Le champ "chapeau" doit faire AU PLUS 600 caractères (principe posé,
            condensé) — ne recopie pas l'arrêt en entier.

            RÈGLE DE FORMAT ABSOLUE — non négociable :
            Ta réponse DOIT être un objet JSON unique commençant par « { » et
            finissant par « } ». AUCUN texte avant l'accolade ouvrante. AUCUN
            texte après l'accolade fermante. PAS de balises markdown.

            Schéma de réponse :
            {
              "arrets": [
                {
                  "ref": "Cass. 12 mars 2024, n° X.YY.0123.F",
                  "juridiction": "Cour de cassation, section néerlandophone",
                  "date_arret": "2024-03-12",
                  "numero_pourvoi": "X.YY.0123.F",
                  "lien": "https://juportal.be/content/ECLI:BE:CASS:...",
                  "chapeau": "Texte du chapeau / sommaire / principe posé par l'arrêt..."
                },
                ...
              ]
            }

            Si aucun arrêt belge pertinent n'est trouvé, réponds par :
            {"arrets": []}
            """;

    private final AnthropicService anthropic;
    private final ObjectMapper objectMapper;

    public JurisprudenceBeWebSearchClient(AnthropicService anthropic, ObjectMapper objectMapper) {
        this.anthropic = anthropic;
        this.objectMapper = objectMapper;
    }

    /**
     * Recherche full-text sur JUPORTAL / Cassation BE via Claude + web_search.
     *
     * <p>Signature volontairement symétrique à
     * {@link JudilibreApiClient#fetchArretsByKeyword} pour faciliter le routage
     * conditionnel BE/FR dans {@link JurisprudenceBootstrapService}.</p>
     *
     * @param query           mot-clé thématique (ex. "Outplacement obligatoire 45+")
     * @param startInclusive  borne inférieure de date (filtrage Claude best-effort)
     * @param endExclusive    borne supérieure de date (idem)
     * @param limit           nombre max d'arrêts retournés (tronqué à 5 côté prompt)
     */
    public List<JudilibreArret> fetchArretsByKeyword(String query, LocalDate startInclusive,
                                                     LocalDate endExclusive, int limit) {
        if (query == null || query.isBlank()) {
            log.warn("F-JU-04 — JurisprudenceBeWebSearchClient: query vide, no-op");
            return List.of();
        }

        String userMessage = """
                Mot-clé / sujet : %s
                Période préférentielle : %s à %s.
                Limite : maximum %d arrêts les plus structurants.
                Cherche sur juportal.be / cour de cassation BE / cour constitutionnelle BE
                puis retourne le JSON structuré conforme au schéma système.
                """.formatted(query, startInclusive, endExclusive, Math.max(1, Math.min(limit, 3)));

        AnthropicResult result;
        try {
            AiCallContext ctx = AiCallContext.systemLevel(JobType.SYSTEM_JP_BOOTSTRAP);
            result = anthropic.analyzeWithWebSearch(ctx, SYSTEM_PROMPT, userMessage, MAX_TOKENS, MAX_WEB_SEARCHES);
        } catch (Exception e) {
            log.warn("F-JU-04 — JurisprudenceBeWebSearchClient anthropic call failed for query='{}': {}",
                    query, e.getMessage());
            return List.of();
        }
        if (result == null || result.content() == null || result.content().isBlank()) {
            log.warn("F-JU-04 — JurisprudenceBeWebSearchClient empty response for query='{}'", query);
            return List.of();
        }

        return parseArrets(result.content(), limit, query);
    }

    List<JudilibreArret> parseArrets(String rawText, int limit, String queryForLog) {
        String json = extractJson(rawText);
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode arrets = root.path("arrets");
            if (!arrets.isArray() || arrets.isEmpty()) {
                log.info("F-JU-04 — Aucun arrêt BE trouvé pour query='{}'", queryForLog);
                return List.of();
            }
            List<JudilibreArret> out = new ArrayList<>();
            for (JsonNode node : arrets) {
                JudilibreArret arret = parseOne(node, queryForLog);
                if (arret != null) {
                    out.add(arret);
                }
                if (out.size() >= limit) {
                    break;
                }
            }
            return out;
        } catch (Exception e) {
            log.warn("F-JU-04 — JurisprudenceBeWebSearchClient parse fail for query='{}': {}",
                    queryForLog, e.getMessage());
            return List.of();
        }
    }

    private JudilibreArret parseOne(JsonNode node, String queryForLog) {
        try {
            String ref = node.path("ref").asText(null);
            String juridiction = node.path("juridiction").asText("Cour de cassation BE");
            String dateStr = node.path("date_arret").asText(null);
            String numero = node.path("numero_pourvoi").asText("n/a");
            String lien = node.path("lien").asText("");
            String chapeau = node.path("chapeau").asText("");
            if (chapeau.length() > 2000) chapeau = chapeau.substring(0, 2000);
            if (ref == null || ref.isBlank()) {
                return null;
            }
            LocalDate date = (dateStr == null || dateStr.isBlank()) ? null : LocalDate.parse(dateStr);
            // Synthèse d'un id stable côté BE pour Claude evaluator
            // (JUPORTAL n'a pas d'id équivalent JUDILIBRE — on dérive de ECLI ou ref).
            String id = "be-" + Integer.toHexString(ref.hashCode());
            return new JudilibreArret(id, ref, juridiction, date, numero, chapeau, lien);
        } catch (Exception e) {
            log.warn("F-JU-04 — JurisprudenceBeWebSearchClient parseOne fail for query='{}': {}",
                    queryForLog, e.getMessage());
            return null;
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
