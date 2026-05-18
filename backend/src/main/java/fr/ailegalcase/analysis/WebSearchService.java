package fr.ailegalcase.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

/**
 * F-179 SF-179-02 — recherche web publique de vérification d'existence d'une
 * référence jurisprudentielle.
 *
 * <ul>
 *   <li>France → Légifrance ({@code legifrance.gouv.fr}) ;</li>
 *   <li>Belgique → Juportal / Juridat ({@code juportal.be}).</li>
 * </ul>
 *
 * <p>Le service est <strong>tolérant à l'échec</strong> : tout timeout, toute
 * erreur HTTP ou résultat ambigu renvoie {@link WebSearchResult#uncertain()} —
 * jamais d'exception propagée, jamais un faux {@code NOT_FOUND}.</p>
 *
 * <p>Implémentation V1 volontairement minimale : une requête HTTP GET sur la
 * page de recherche du portail public, suivie d'une heuristique simple sur le
 * corps HTML. Le service est dédié à F-179 ; sa généralisation à d'autres use
 * cases (F-120 SF-120-04) est tracée au backlog.</p>
 */
@Service
public class WebSearchService {

    private static final Logger log = LoggerFactory.getLogger(WebSearchService.class);

    /**
     * Indices HTML suggérant qu'aucun résultat n'a été trouvé. Comparés sur une
     * forme sans accent ({@link #stripAccents}) pour être robustes aux écarts
     * d'encodage entre portails.
     */
    private static final String[] NO_RESULT_MARKERS = {
            "aucun resultat", "aucun document", "0 resultat", "aucune decision",
            "geen resultaat", "no result"
    };

    private final RestClient restClient;
    private final String legifranceBaseUrl;
    private final String juridatBaseUrl;

    @org.springframework.beans.factory.annotation.Autowired
    public WebSearchService(
            RestClient.Builder builder,
            @Value("${jurisprudence.web-search.legifrance-base-url:https://www.legifrance.gouv.fr}")
            String legifranceBaseUrl,
            @Value("${jurisprudence.web-search.juridat-base-url:https://juportal.be}")
            String juridatBaseUrl,
            @Value("${jurisprudence.web-search.timeout-ms:8000}") long timeoutMs) {
        this.legifranceBaseUrl = legifranceBaseUrl;
        this.juridatBaseUrl = juridatBaseUrl;
        var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.restClient = builder.requestFactory(requestFactory).build();
    }

    /** Constructeur de test (injection directe du {@link RestClient}). */
    WebSearchService(RestClient restClient, String legifranceBaseUrl, String juridatBaseUrl) {
        this.restClient = restClient;
        this.legifranceBaseUrl = legifranceBaseUrl;
        this.juridatBaseUrl = juridatBaseUrl;
    }

    /**
     * Recherche une référence jurisprudentielle sur le portail public du pays.
     *
     * @param reference libellé de la référence (ex. "Cass. soc. n° 12-17.516")
     * @param country   {@code BELGIQUE} → Juridat ; sinon (dont {@code null}) → Légifrance
     * @return résultat — jamais {@code null}, jamais une exception
     */
    public WebSearchResult searchJurisprudence(String reference, String country) {
        if (reference == null || reference.isBlank()) {
            return WebSearchResult.uncertain();
        }
        boolean belgium = country != null
                && country.trim().toUpperCase(java.util.Locale.ROOT).startsWith("BE");
        String baseUrl = belgium ? juridatBaseUrl : legifranceBaseUrl;

        // Un seul retry sur erreur réseau / 5xx (backoff exponentiel court).
        int[] backoffMs = {0, 500};
        for (int attempt = 0; attempt < backoffMs.length; attempt++) {
            if (backoffMs[attempt] > 0) {
                try {
                    Thread.sleep(backoffMs[attempt]);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return WebSearchResult.uncertain();
                }
            }
            try {
                String uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                        .path("/search")
                        .queryParam("query", reference)
                        .build()
                        .encode()
                        .toUriString();
                String body = restClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(String.class);
                return interpret(body, uri);
            } catch (org.springframework.web.client.HttpServerErrorException e) {
                // 5xx → retry si tentatives restantes, sinon UNCERTAIN.
                if (attempt + 1 >= backoffMs.length) {
                    log.warn("F-179: web search 5xx for '{}' — UNCERTAIN ({})", reference, e.getMessage());
                    return WebSearchResult.uncertain();
                }
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                // 4xx (auth, quota) → pas de retry, UNCERTAIN.
                log.warn("F-179: web search 4xx for '{}' — UNCERTAIN ({})", reference, e.getMessage());
                return WebSearchResult.uncertain();
            } catch (Exception e) {
                // Timeout / réseau / parsing → retry si possible, sinon UNCERTAIN.
                if (attempt + 1 >= backoffMs.length) {
                    log.warn("F-179: web search error for '{}' — UNCERTAIN ({})", reference, e.getMessage());
                    return WebSearchResult.uncertain();
                }
            }
        }
        return WebSearchResult.uncertain();
    }

    /**
     * Heuristique d'interprétation du corps HTML retourné par le portail.
     * Un corps vide / null → {@code UNCERTAIN} ; un marqueur "aucun résultat"
     * → {@code NOT_FOUND} ; sinon → {@code FOUND}.
     */
    private WebSearchResult interpret(String body, String uri) {
        if (body == null || body.isBlank()) {
            return WebSearchResult.uncertain();
        }
        String normalized = stripAccents(body.toLowerCase(java.util.Locale.ROOT));
        for (String marker : NO_RESULT_MARKERS) {
            if (normalized.contains(marker)) {
                return WebSearchResult.notFound();
            }
        }
        return WebSearchResult.found(uri);
    }

    /** Retire les diacritiques pour une comparaison robuste aux encodages. */
    private static String stripAccents(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
