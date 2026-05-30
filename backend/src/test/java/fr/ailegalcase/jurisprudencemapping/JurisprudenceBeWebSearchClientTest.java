package fr.ailegalcase.jurisprudencemapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AiCallContext;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

/**
 * SF-JU-04-02 — couvre le client BE qui interroge Claude+web_search pour
 * JUPORTAL / Cassation BE. Stratégie : mocker {@link AnthropicService} et
 * vérifier le parsing du JSON renvoyé par Claude.
 */
class JurisprudenceBeWebSearchClientTest {

    private AnthropicService anthropic;
    private JurisprudenceBeWebSearchClient client;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        anthropic = mock(AnthropicService.class);
        objectMapper = new ObjectMapper();
        client = new JurisprudenceBeWebSearchClient(anthropic, objectMapper);
    }

    @Test
    void fetchArretsByKeyword_emptyQuery_returnsEmptyAndDoesNotCallAnthropic() {
        List<JudilibreArret> result = client.fetchArretsByKeyword("",
                LocalDate.of(2014, 1, 1), LocalDate.now(), 5);

        assertThat(result).isEmpty();
        verify(anthropic, never()).analyzeWithWebSearch(any(AiCallContext.class), any(), any(), anyInt(), anyInt());
    }

    @Test
    void fetchArretsByKeyword_anthropicReturnsValidJson_parsesArrets() {
        String json = """
                {"arrets":[
                  {
                    "ref":"Cass. 12 mars 2024, n° S.24.0123.F",
                    "juridiction":"Cour de cassation, section néerlandophone",
                    "date_arret":"2024-03-12",
                    "numero_pourvoi":"S.24.0123.F",
                    "lien":"https://juportal.be/content/ECLI:BE:CASS:2024:X",
                    "chapeau":"L'outplacement est obligatoire pour le travailleur de 45 ans et plus..."
                  },
                  {
                    "ref":"Cass. BE 5 nov. 2020, n° C.20.0019.N",
                    "juridiction":"Cour de cassation",
                    "date_arret":"2020-11-05",
                    "numero_pourvoi":"C.20.0019.N",
                    "lien":"https://juportal.be/content/ECLI:BE:CASS:2020:Y",
                    "chapeau":"L'obligation incombe à l'employeur dès la notification du licenciement."
                  }
                ]}
                """;
        when(anthropic.analyzeWithWebSearch(any(AiCallContext.class), any(), any(), anyInt(), anyInt()))
                .thenReturn(new AnthropicResult(json, "claude-sonnet", 100, 200, "end_turn"));

        List<JudilibreArret> result = client.fetchArretsByKeyword("outplacement 45+",
                LocalDate.of(2014, 1, 1), LocalDate.now(), 5);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).ref()).startsWith("Cass. 12 mars 2024");
        assertThat(result.get(0).dateArret()).isEqualTo(LocalDate.of(2024, 3, 12));
        assertThat(result.get(0).juridiction()).contains("Cour de cassation");
        assertThat(result.get(0).judilibreId()).startsWith("be-");
        assertThat(result.get(1).numeroPourvoi()).isEqualTo("C.20.0019.N");
    }

    // SF-JU-04-03 — verrouille le fix : budget max_tokens suffisant pour que le
    // JSON ne soit pas tronqué par les blocs web_search_result, et web_search borné.
    @Test
    void fetchArretsByKeyword_passesAdequateTokenBudgetAndBoundedWebSearches() {
        when(anthropic.analyzeWithWebSearch(any(AiCallContext.class), any(), any(), anyInt(), anyInt()))
                .thenReturn(new AnthropicResult("{\"arrets\":[]}", "claude-sonnet", 50, 20, "end_turn"));

        client.fetchArretsByKeyword("clause de non-concurrence BE",
                LocalDate.of(2014, 1, 1), LocalDate.now(), 3);

        // maxTokens=12000 (≫ 2000 qui tronquait) et maxWebSearches=3.
        verify(anthropic).analyzeWithWebSearch(any(AiCallContext.class), any(), any(), eq(12000), eq(3));
    }

    @Test
    void fetchArretsByKeyword_anthropicReturnsEmptyArrets_returnsEmpty() {
        when(anthropic.analyzeWithWebSearch(any(AiCallContext.class), any(), any(), anyInt(), anyInt()))
                .thenReturn(new AnthropicResult("{\"arrets\":[]}", "claude-sonnet", 50, 20, "end_turn"));

        List<JudilibreArret> result = client.fetchArretsByKeyword("introuvable BE",
                LocalDate.of(2020, 1, 1), LocalDate.now(), 5);

        assertThat(result).isEmpty();
    }

    @Test
    void fetchArretsByKeyword_anthropicReturnsGarbage_returnsEmptyGracefully() {
        when(anthropic.analyzeWithWebSearch(any(AiCallContext.class), any(), any(), anyInt(), anyInt()))
                .thenReturn(new AnthropicResult("Je suis désolé je n'ai pas pu chercher",
                        "claude-sonnet", 50, 20, "end_turn"));

        List<JudilibreArret> result = client.fetchArretsByKeyword("test",
                LocalDate.of(2020, 1, 1), LocalDate.now(), 5);

        assertThat(result).isEmpty();
    }

    @Test
    void fetchArretsByKeyword_anthropicThrows_returnsEmptyGracefully() {
        when(anthropic.analyzeWithWebSearch(any(AiCallContext.class), any(), any(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("anthropic down"));

        List<JudilibreArret> result = client.fetchArretsByKeyword("test",
                LocalDate.of(2020, 1, 1), LocalDate.now(), 5);

        assertThat(result).isEmpty();
    }

    @Test
    void parseArrets_skipsArretsWithoutRef() {
        String json = """
                {"arrets":[
                  {"ref":"","juridiction":"X","date_arret":"2024-01-01","numero_pourvoi":"S.24.0001.F","lien":"https://juportal.be/content/ECLI:BE:CASS:2024:A","chapeau":"X"},
                  {"ref":"Cass. BE valide","juridiction":"Y","date_arret":"2024-01-02","numero_pourvoi":"S.18.0042.F","lien":"https://juportal.be/content/ECLI:BE:CASS:2018:B","chapeau":"Y"}
                ]}
                """;

        List<JudilibreArret> result = client.parseArrets(json, 5, "test");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).ref()).isEqualTo("Cass. BE valide");
    }

    // SF-JU-04-04 — garde-fou anti-hallucination (cas réels du pilote 2026-05-30).
    @Test
    void parseArrets_rejectsArretWhoseLinkIsASearchPage() {
        // divorce-ddi : lien vers une page de recherche juricaf, pas un arrêt.
        String json = """
                {"arrets":[
                  {"ref":"Cass. 29 mai 2015, n° C.15.0251.F","juridiction":"Cour de cassation",
                   "date_arret":"2015-05-29","numero_pourvoi":"C.15.0251.F",
                   "lien":"https://juricaf.org/recherche/%22Code+civil%22","chapeau":"..."}
                ]}
                """;
        assertThat(client.parseArrets(json, 5, "divorce-ddi")).isEmpty();
    }

    @Test
    void parseArrets_rejectsArretWithNonRoleNumber() {
        // autorite-parentale : "numéro" = fragment ECLI, pas un n° de rôle.
        String json = """
                {"arrets":[
                  {"ref":"Cass. 2e ch. 20 mai 2021","juridiction":"Cour de cassation, 2e chambre",
                   "date_arret":"2021-05-20","numero_pourvoi":"ARR.20210520.2F.1",
                   "lien":"https://juportal.be/content/ECLI:BE:CASS:2021:ARR","chapeau":"..."}
                ]}
                """;
        assertThat(client.parseArrets(json, 5, "autorite-parentale")).isEmpty();
    }

    @Test
    void parseArrets_keepsOnlyCredibleArretAmongMixed() {
        // 1 valide (rôle joint) + 1 hallucination (lien recherche) → 1 retenu.
        String json = """
                {"arrets":[
                  {"ref":"Cass. 17 sept. 2020, n° C.18.0294.F-C.18.0611.F","juridiction":"Cour de cassation, 1re ch.",
                   "date_arret":"2020-09-17","numero_pourvoi":"C.18.0294.F-C.18.0611.F",
                   "lien":"https://juportal.be/content/ECLI:BE:CASS:2020:CONC","chapeau":"valide"},
                  {"ref":"Cass. bidon","juridiction":"X","date_arret":"2015-05-29","numero_pourvoi":"inventé",
                   "lien":"https://juportal.be/search?query=divorce","chapeau":"halluciné"}
                ]}
                """;
        List<JudilibreArret> result = client.parseArrets(json, 5, "mixed");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).numeroPourvoi()).startsWith("C.18.0294.F");
    }

    @Test
    void isCredibleBeArret_acceptsConstitutionalCourtNumber() {
        assertThat(JurisprudenceBeWebSearchClient.isCredibleBeArret(
                "121/2013", "https://www.const-court.be/public/f/2013/2013-121f.pdf")).isTrue();
    }
}
