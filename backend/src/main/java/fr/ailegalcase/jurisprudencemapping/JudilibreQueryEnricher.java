package fr.ailegalcase.jurisprudencemapping;

import fr.ailegalcase.analysis.AiCallContext;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.analysis.JobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * F-JU-06 / SF-JU-06-03 — enrichissement automatique des requêtes JUDILIBRE.
 *
 * <p>Les mots-clés du bootstrap sont génériques (« Comparateur d'indemnités »)
 * et ramènent du bruit. Cet enrichisseur transforme le sujet de l'outil en une
 * requête plus discriminante (termes juridiques + articles de loi) avant
 * l'appel JUDILIBRE, afin de remonter de meilleurs candidats. Combiné aux
 * garde-fous SF-JU-06-01, il améliore la couverture sans curation manuelle.</p>
 *
 * <p>Appel LLM gaté ({@link JobType#SYSTEM_JP_BOOTSTRAP}). <strong>Fallback</strong>
 * sur le mot-clé d'origine en cas d'échec/réponse vide — jamais de blocage du
 * bootstrap.</p>
 */
@Component
public class JudilibreQueryEnricher {

    private static final Logger log = LoggerFactory.getLogger(JudilibreQueryEnricher.class);
    private static final int MAX_TOKENS = 120;
    private static final int MAX_QUERY_LEN = 300;

    private static final String SYSTEM_PROMPT = """
            Tu es juriste documentaliste. On cherche dans JUDILIBRE (jurisprudence
            de la Cour de cassation française) les arrêts qui FONDENT un outil
            décisionnel d'avocat. À partir du sujet de l'outil, produis la MEILLEURE
            requête de recherche : 3 à 8 mots-clés juridiques précis, plus les
            articles de loi pertinents s'ils existent. Reste strictement sur le
            sujet (ne l'élargis pas à des thèmes voisins).

            Réponds UNIQUEMENT la requête (mots-clés séparés par des espaces),
            sans phrase, sans ponctuation superflue, sans préambule.
            """;

    private final AnthropicService anthropic;

    public JudilibreQueryEnricher(AnthropicService anthropic) {
        this.anthropic = anthropic;
    }

    /**
     * @return une requête JUDILIBRE ciblée, ou le {@code motCleRecherche} d'origine
     *         en cas d'échec (jamais {@code null} si l'entrée ne l'est pas).
     */
    public String enrich(String toolId, String brancheCalculId, String motCleRecherche) {
        String fallback = motCleRecherche == null ? "" : motCleRecherche;
        String branche = (brancheCalculId == null || brancheCalculId.isBlank()
                || "default".equalsIgnoreCase(brancheCalculId)) ? "" : " / branche " + brancheCalculId;
        String userMessage = "SUJET DE L'OUTIL : " + fallback + " (outil " + toolId + branche + ")";
        try {
            AiCallContext ctx = AiCallContext.systemLevel(JobType.SYSTEM_JP_BOOTSTRAP);
            AnthropicResult result = anthropic.analyze(ctx, SYSTEM_PROMPT, userMessage, MAX_TOKENS);
            if (result == null || result.content() == null || result.content().isBlank()) {
                return fallback;
            }
            String query = result.content().trim().replaceAll("\\s+", " ");
            if (query.length() > MAX_QUERY_LEN) {
                query = query.substring(0, MAX_QUERY_LEN);
            }
            return query.isBlank() ? fallback : query;
        } catch (Exception e) {
            log.warn("F-JU-06 — QueryEnricher échec pour {}, fallback mot-clé d'origine: {}", toolId, e.getMessage());
            return fallback;
        }
    }
}
