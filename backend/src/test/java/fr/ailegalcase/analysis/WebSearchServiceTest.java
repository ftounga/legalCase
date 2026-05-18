package fr.ailegalcase.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * F-179 SF-179-02 — tests de {@link WebSearchService} avec serveur HTTP simulé.
 */
class WebSearchServiceTest {

    private static final String LEGIFRANCE = "https://legifrance.test";
    private static final String JURIDAT = "https://juridat.test";

    private MockRestServiceServer server;
    private WebSearchService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new WebSearchService(builder.build(), LEGIFRANCE, JURIDAT);
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
}
