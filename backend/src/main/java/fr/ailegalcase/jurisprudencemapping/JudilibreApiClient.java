package fr.ailegalcase.jurisprudencemapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * F-JU-01 / SF-JU-01-02 — client HTTP pour JUDILIBRE (Cour de cassation +
 * Conseil d'État via API PISTE).
 *
 * <p>OAuth2 client_credentials avec mise en cache du token (1h), retry
 * exponentiel × 3 sur 5xx, parsing minimal des résultats vers
 * {@link JudilibreArret}.</p>
 *
 * <p>Activation conditionnée par {@code judilibre.client-id} et
 * {@code judilibre.client-secret} non vides (sinon {@link #fetchArretsForPeriod}
 * retourne liste vide + log WARN).</p>
 */
@Component
public class JudilibreApiClient {

    private static final Logger log = LoggerFactory.getLogger(JudilibreApiClient.class);
    private static final String TOKEN_URL = "https://oauth.piste.gouv.fr/api/oauth/token";
    /** Endpoint bulk export par date — utilisé par les crons SF-02/03 sur une fenêtre courte. */
    private static final String EXPORT_URL = "https://api.piste.gouv.fr/cassation/judilibre/v1.0/export";
    /** Endpoint search full-text avec relevance scoring — utilisé par le bootstrap SF-JU-01-13. */
    private static final String SEARCH_URL_FULLTEXT = "https://api.piste.gouv.fr/cassation/judilibre/v1.0/search";
    private static final int MAX_RETRIES = 3;
    private static final Duration[] BACKOFFS = {Duration.ofSeconds(2), Duration.ofSeconds(8), Duration.ofSeconds(30)};

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String clientId;
    private final String clientSecret;

    private volatile String cachedToken;
    private volatile Instant tokenExpiresAt;

    public JudilibreApiClient(@Value("${judilibre.client-id:}") String clientId,
                              @Value("${judilibre.client-secret:}") String clientSecret,
                              ObjectMapper objectMapper) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    /**
     * Récupère les arrêts publiés au Bulletin entre {@code startInclusive} et
     * {@code endExclusive}.
     *
     * <p>Retourne liste vide (sans erreur) si {@code client-id} ou
     * {@code client-secret} sont absents — permet à la CI de tourner et au
     * cron de no-op en local sans config.</p>
     */
    public List<JudilibreArret> fetchArretsForPeriod(LocalDate startInclusive, LocalDate endExclusive) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            log.warn("F-JU-01 — JudilibreApiClient sans credentials configurés, fetchArretsForPeriod no-op");
            return List.of();
        }

        String token = currentToken();
        List<JudilibreArret> all = new ArrayList<>();
        int batchSize = 100;
        int from = 0;
        boolean more = true;

        while (more) {
            String body = doSearchWithRetry(token, startInclusive, endExclusive, from, batchSize);
            try {
                JsonNode root = objectMapper.readTree(body);
                JsonNode results = root.path("results");
                if (!results.isArray() || results.isEmpty()) {
                    more = false;
                } else {
                    for (JsonNode node : results) {
                        JudilibreArret arret = parseArret(node);
                        if (arret != null) {
                            all.add(arret);
                        }
                    }
                    from += batchSize;
                    int total = root.path("total").asInt(all.size());
                    if (from >= total) {
                        more = false;
                    }
                }
            } catch (Exception e) {
                log.warn("F-JU-01 — JudilibreApiClient parsing fail at from={}: {}", from, e.getMessage());
                more = false;
            }
        }

        return all;
    }

    /**
     * SF-JU-01-13 — recherche full-text JUDILIBRE avec relevance scoring.
     *
     * <p>Utilise l'endpoint {@code /v1.0/search} (à ne pas confondre avec
     * {@code /export} qui ne filtre que par date). Retourne les {@code limit}
     * arrêts les plus pertinents pour {@code query} sur la période demandée.</p>
     *
     * <p>Cas usage primaire : bootstrap initial (SF-JU-01-05) où on cherche
     * les arrêts structurants par mot-clé thématique du CSV — sans cette
     * recherche full-text, le bulk export retournait des arrêts aléatoires
     * et Claude répondait NONE à juste titre.</p>
     */
    public List<JudilibreArret> fetchArretsByKeyword(String query, LocalDate startInclusive, LocalDate endExclusive, int limit) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            log.warn("F-JU-01 — JudilibreApiClient sans credentials configurés, fetchArretsByKeyword no-op");
            return List.of();
        }
        if (query == null || query.isBlank()) {
            log.warn("F-JU-01 — JudilibreApiClient fetchArretsByKeyword: query vide, no-op");
            return List.of();
        }
        int pageSize = Math.max(1, Math.min(limit, 50));

        String token = currentToken();
        String body = doFullTextSearchWithRetry(token, query, startInclusive, endExclusive, pageSize);
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode results = root.path("results");
            if (!results.isArray() || results.isEmpty()) {
                return List.of();
            }
            List<JudilibreArret> out = new ArrayList<>();
            for (JsonNode node : results) {
                JudilibreArret arret = parseArret(node);
                if (arret != null) {
                    out.add(arret);
                }
                if (out.size() >= limit) {
                    break;
                }
            }
            return out;
        } catch (Exception e) {
            log.warn("F-JU-01 — JudilibreApiClient fetchArretsByKeyword parsing fail for query='{}': {}",
                    query, e.getMessage());
            return List.of();
        }
    }

    String currentToken() {
        Instant now = Instant.now();
        if (cachedToken != null && tokenExpiresAt != null && now.isBefore(tokenExpiresAt)) {
            return cachedToken;
        }
        return refreshToken();
    }

    private synchronized String refreshToken() {
        Instant now = Instant.now();
        if (cachedToken != null && tokenExpiresAt != null && now.isBefore(tokenExpiresAt)) {
            return cachedToken;
        }
        Map<String, Object> form = new HashMap<>();
        form.put("grant_type", "client_credentials");
        form.put("client_id", clientId);
        form.put("client_secret", clientSecret);
        form.put("scope", "openid");

        String body = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(toFormBody(form))
                .retrieve()
                .body(String.class);
        try {
            JsonNode root = objectMapper.readTree(body);
            cachedToken = root.path("access_token").asText();
            int expiresIn = root.path("expires_in").asInt(3600);
            tokenExpiresAt = Instant.now().plusSeconds(Math.max(60, expiresIn - 60));
            return cachedToken;
        } catch (Exception e) {
            throw new IllegalStateException("F-JU-01 — JudilibreApiClient cannot parse OAuth2 token response", e);
        }
    }

    private String doSearchWithRetry(String token, LocalDate startInclusive, LocalDate endExclusive,
                                     int from, int batchSize) {
        int attempt = 0;
        Exception last = null;
        while (attempt < MAX_RETRIES) {
            try {
                String body = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host("api.piste.gouv.fr")
                                .path("/cassation/judilibre/v1.0/export")
                                .queryParam("date_type", "creation")
                                .queryParam("date_start", startInclusive.toString())
                                .queryParam("date_end", endExclusive.toString())
                                .queryParam("batch_size", batchSize)
                                .queryParam("batch", from / batchSize)
                                .build())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .body(String.class);
                return body == null ? "{}" : body;
            } catch (HttpServerErrorException e) {
                last = e;
                sleep(BACKOFFS[Math.min(attempt, BACKOFFS.length - 1)]);
                attempt++;
            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode().is4xxClientError()) {
                    log.warn("F-JU-01 — JudilibreApiClient 4xx at from={}: {} {}", from, e.getStatusCode(), e.getStatusText());
                    return "{}";
                }
                last = e;
                sleep(BACKOFFS[Math.min(attempt, BACKOFFS.length - 1)]);
                attempt++;
            }
        }
        throw new IllegalStateException("F-JU-01 — JudilibreApiClient retries exhausted", last);
    }

    /**
     * SF-JU-01-13 — appel HTTP vers JUDILIBRE {@code /v1.0/search} (full-text + relevance).
     */
    private String doFullTextSearchWithRetry(String token, String query, LocalDate startInclusive,
                                             LocalDate endExclusive, int pageSize) {
        int attempt = 0;
        Exception last = null;
        while (attempt < MAX_RETRIES) {
            try {
                String body = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host("api.piste.gouv.fr")
                                .path("/cassation/judilibre/v1.0/search")
                                .queryParam("query", query)
                                .queryParam("date_start", startInclusive.toString())
                                .queryParam("date_end", endExclusive.toString())
                                .queryParam("page_size", pageSize)
                                .queryParam("page", 0)
                                .queryParam("sort", "score")
                                .queryParam("order", "desc")
                                .build())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .body(String.class);
                return body == null ? "{}" : body;
            } catch (HttpServerErrorException e) {
                last = e;
                sleep(BACKOFFS[Math.min(attempt, BACKOFFS.length - 1)]);
                attempt++;
            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode().is4xxClientError()) {
                    log.warn("F-JU-01 — JudilibreApiClient /search 4xx for query='{}': {} {}",
                            query, e.getStatusCode(), e.getStatusText());
                    return "{}";
                }
                last = e;
                sleep(BACKOFFS[Math.min(attempt, BACKOFFS.length - 1)]);
                attempt++;
            }
        }
        throw new IllegalStateException("F-JU-01 — JudilibreApiClient /search retries exhausted", last);
    }

    JudilibreArret parseArret(JsonNode node) {
        try {
            String id = node.path("id").asText(null);
            String numero = node.path("number").asText(null);
            String dateStr = node.path("decision_date").asText(null);
            String juridiction = node.path("jurisdiction").asText("");
            String chamberId = node.path("chamber").asText("");
            String chapeau = node.path("summary").asText("");
            if (chapeau.isBlank()) {
                chapeau = node.path("text").asText("");
                if (chapeau.length() > 2000) {
                    chapeau = chapeau.substring(0, 2000);
                }
            }
            LocalDate date = dateStr == null ? null : LocalDate.parse(dateStr);
            String ref = node.path("ref").asText(null);
            if (ref == null && date != null && numero != null) {
                ref = juridiction + (chamberId.isBlank() ? "" : " " + chamberId) + " " + date + ", n° " + numero;
            }
            // SF-JU-06-FIX2 — fallback : l'identifiant JUDILIBRE (hex 24) n'est PAS un
            // id Légifrance (legifrance.gouv.fr/juri/id/<id> → 403). Il est valide sur
            // courdecassation.fr/decision/<id> (HTTP 200 vérifié). On garde solution_url
            // s'il est fourni par JUDILIBRE.
            String url = node.path("solution_url").asText("https://www.courdecassation.fr/decision/" + id);
            return new JudilibreArret(id, ref, juridiction, date, numero, chapeau, url);
        } catch (Exception e) {
            log.warn("F-JU-01 — JudilibreApiClient parseArret fail: {}", e.getMessage());
            return null;
        }
    }

    private static String toFormBody(Map<String, Object> form) {
        StringBuilder sb = new StringBuilder();
        form.forEach((k, v) -> {
            if (sb.length() > 0) sb.append('&');
            sb.append(java.net.URLEncoder.encode(k, java.nio.charset.StandardCharsets.UTF_8))
                    .append('=')
                    .append(java.net.URLEncoder.encode(String.valueOf(v), java.nio.charset.StandardCharsets.UTF_8));
        });
        return sb.toString();
    }

    private static void sleep(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
