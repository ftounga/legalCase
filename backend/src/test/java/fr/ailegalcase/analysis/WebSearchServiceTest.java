package fr.ailegalcase.analysis;

import fr.ailegalcase.jurisprudencemapping.JudilibreApiClient;
import fr.ailegalcase.jurisprudencemapping.JudilibreArret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * F-179 SF-179-02 / SF-179-05 — tests de {@link WebSearchService} avec serveur
 * HTTP simulé et {@link JudilibreApiClient} mocké.
 */
class WebSearchServiceTest {

    private static final String LEGIFRANCE = "https://legifrance.test";
    private static final String JURIDAT = "https://juridat.test";

    private MockRestServiceServer server;
    private JudilibreApiClient judilibre;
    private WebSearchService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        judilibre = mock(JudilibreApiClient.class);
        // Par défaut JUDILIBRE ne confirme rien (liste vide) → fallback scraping :
        // ne perturbe pas les tests SF-179-02 historiques.
        service = new WebSearchService(builder.build(), LEGIFRANCE, JURIDAT, judilibre);
    }

    /** SF-179-05 — arrêt JUDILIBRE de test (seuls n° de pourvoi, lien et id comptent ici). */
    private JudilibreArret judilibreArret(String numeroPourvoi, String lien) {
        return new JudilibreArret("jid-1", "Cass. soc., n° " + numeroPourvoi,
                "Cour de cassation", LocalDate.of(2000, 12, 12), numeroPourvoi, "Chapeau", lien);
    }

    @Test
    void searchJurisprudence_blankReference_returnsUncertain() {
        WebSearchResult res = service.searchJurisprudence("  ", "FRANCE");
        assertThat(res.outcome()).isEqualTo(WebSearchResult.Outcome.UNCERTAIN);
    }

    @Test
    void searchJurisprudence_frenchReference_queriesLegifrance() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(LEGIFRANCE)))
                .andRespond(withSuccess("<html>Résultat : Cass. soc. trouvée</html>",
                        MediaType.TEXT_HTML));

        WebSearchResult res = service.searchJurisprudence(
                "Cass. soc. 25 sept. 2013, n° 12-17.516", "FRANCE");

        assertThat(res.outcome()).isEqualTo(WebSearchResult.Outcome.FOUND);
        assertThat(res.sourceUrl()).startsWith(LEGIFRANCE);
        server.verify();
    }

    @Test
    void searchJurisprudence_belgianReference_queriesJuridat() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(JURIDAT)))
                .andRespond(withSuccess("<html>arrêt n° 45/2021 disponible</html>",
                        MediaType.TEXT_HTML));

        WebSearchResult res = service.searchJurisprudence("Cour const. n° 45/2021", "BELGIQUE");

        assertThat(res.outcome()).isEqualTo(WebSearchResult.Outcome.FOUND);
        assertThat(res.sourceUrl()).startsWith(JURIDAT);
        server.verify();
    }

    @Test
    void searchJurisprudence_noResultMarker_returnsNotFound() {
        // Charset UTF-8 explicite pour que le décodage côté RestClient soit fidèle.
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(LEGIFRANCE)))
                .andRespond(withSuccess("<html><body>Aucun résultat trouvé</body></html>",
                        new MediaType("text", "html", java.nio.charset.StandardCharsets.UTF_8)));

        WebSearchResult res = service.searchJurisprudence(
                "Cass. soc. 1 janv. 2099, n° 99-99.999", "FRANCE");

        assertThat(res.outcome()).isEqualTo(WebSearchResult.Outcome.NOT_FOUND);
        server.verify();
    }

    @Test
    void searchJurisprudence_serverErrorThenSuccess_retries() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(LEGIFRANCE)))
                .andRespond(withServerError());
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(LEGIFRANCE)))
                .andRespond(withSuccess("<html>Cass. soc. trouvée</html>", MediaType.TEXT_HTML));

        WebSearchResult res = service.searchJurisprudence(
                "Cass. soc. 25 sept. 2013, n° 12-17.516", "FRANCE");

        assertThat(res.outcome()).isEqualTo(WebSearchResult.Outcome.FOUND);
        server.verify();
    }

    @Test
    void searchJurisprudence_persistentServerError_returnsUncertain() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(LEGIFRANCE)))
                .andRespond(withServerError());
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(LEGIFRANCE)))
                .andRespond(withServerError());

        WebSearchResult res = service.searchJurisprudence(
                "Cass. soc. 25 sept. 2013, n° 12-17.516", "FRANCE");

        assertThat(res.outcome()).isEqualTo(WebSearchResult.Outcome.UNCERTAIN);
        server.verify();
    }

    @Test
    void searchJurisprudence_clientError_noRetryReturnsUncertain() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(LEGIFRANCE)))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        WebSearchResult res = service.searchJurisprudence(
                "Cass. soc. 25 sept. 2013, n° 12-17.516", "FRANCE");

        assertThat(res.outcome()).isEqualTo(WebSearchResult.Outcome.UNCERTAIN);
        // Pas de second appel (4xx → pas de retry).
        server.verify();
    }

    // --- SF-179-05 — confirmation d'existence via JUDILIBRE (FR Cour de cassation) ---

    @Test
    void searchJurisprudence_judilibreConfirmsExistence_returnsFoundWithoutScraping() {
        when(judilibre.fetchArretsByKeyword(eq("98-41.609"), any(), any(), anyInt()))
                .thenReturn(List.of(judilibreArret("98-41.609",
                        "https://www.courdecassation.fr/decision/98-41609")));

        WebSearchResult res = service.searchJurisprudence(
                "Cass. soc., 12 décembre 2000, n° 98-41.609", "FRANCE");

        assertThat(res.outcome()).isEqualTo(WebSearchResult.Outcome.FOUND);
        assertThat(res.sourceUrl()).isEqualTo("https://www.courdecassation.fr/decision/98-41609");
        // Aucune requête HTTP de scraping ne doit avoir lieu (server sans attente).
        server.verify();
    }

    @Test
    void searchJurisprudence_judilibreEmpty_fallsBackToScraping() {
        when(judilibre.fetchArretsByKeyword(any(), any(), any(), anyInt()))
                .thenReturn(List.of());
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(LEGIFRANCE)))
                .andRespond(withSuccess("<html>Cass. soc. trouvée</html>", MediaType.TEXT_HTML));

        WebSearchResult res = service.searchJurisprudence(
                "Cass. soc., 30 novembre 1990, n° 88-44.308", "FRANCE");

        assertThat(res.outcome()).isEqualTo(WebSearchResult.Outcome.FOUND);
        assertThat(res.sourceUrl()).startsWith(LEGIFRANCE);
        server.verify();
    }

    @Test
    void searchJurisprudence_judilibreWrongNumber_doesNotConfirm_fallsBack() {
        // JUDILIBRE renvoie un arrêt mais au mauvais numéro → pas de FOUND artificiel.
        when(judilibre.fetchArretsByKeyword(any(), any(), any(), anyInt()))
                .thenReturn(List.of(judilibreArret("11-11.111", "https://x")));
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(LEGIFRANCE)))
                .andRespond(withSuccess("<html><body>Aucun résultat trouvé</body></html>",
                        new MediaType("text", "html", java.nio.charset.StandardCharsets.UTF_8)));

        WebSearchResult res = service.searchJurisprudence(
                "Cass. soc., 12 décembre 2000, n° 98-41.609", "FRANCE");

        assertThat(res.outcome()).isEqualTo(WebSearchResult.Outcome.NOT_FOUND);
        server.verify();
    }

    @Test
    void searchJurisprudence_belgianReference_neverCallsJudilibre() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(JURIDAT)))
                .andRespond(withSuccess("<html>arrêt disponible</html>", MediaType.TEXT_HTML));

        service.searchJurisprudence("Trib. trav. Bruxelles, n° 12-34.567", "BELGIQUE");

        verify(judilibre, never()).fetchArretsByKeyword(any(), any(), any(), anyInt());
        server.verify();
    }

    @Test
    void searchJurisprudence_referenceWithoutPourvoiNumber_neverCallsJudilibre() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(LEGIFRANCE)))
                .andRespond(withSuccess("<html>page</html>", MediaType.TEXT_HTML));

        service.searchJurisprudence("Conseil d'État, avis du 12 mars 2021", "FRANCE");

        verify(judilibre, never()).fetchArretsByKeyword(any(), any(), any(), anyInt());
        server.verify();
    }

    @Test
    void extractPourvoiNumber_variants() {
        assertThat(WebSearchService.extractPourvoiNumber("Cass. soc., n° 98-41.609")).isEqualTo("98-41.609");
        assertThat(WebSearchService.extractPourvoiNumber("aucun numéro ici")).isNull();
        assertThat(WebSearchService.normalizeNumber("98-41.609")).isEqualTo("9841609");
        assertThat(WebSearchService.normalizeNumber("98 41 609")).isEqualTo("9841609");
    }
}
