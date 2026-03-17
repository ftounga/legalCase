package fr.ailegalcase.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AnthropicServiceTest {

    private MockRestServiceServer server;
    private AnthropicService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new AnthropicService("claude-sonnet-4-6", builder);
    }

    // U-01 : texte valide → appel API et retour du JSON
    @Test
    void analyzeChunk_validText_returnsJsonFromApi() {
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"content": [{"type": "text", "text": "{\\"faits\\": [\\"fait1\\"]}"}]}
                        """, MediaType.APPLICATION_JSON));

        String result = service.analyzeChunk("Texte juridique de test.");

        assertThat(result).isEqualTo("{\"faits\": [\"fait1\"]}");
        server.verify();
    }

    // U-02 : texte vide → IllegalArgumentException
    @Test
    void analyzeChunk_emptyText_throwsException() {
        assertThatThrownBy(() -> service.analyzeChunk(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // U-03 : texte null → IllegalArgumentException
    @Test
    void analyzeChunk_nullText_throwsException() {
        assertThatThrownBy(() -> service.analyzeChunk(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // U-04 : texte blanc → IllegalArgumentException
    @Test
    void analyzeChunk_blankText_throwsException() {
        assertThatThrownBy(() -> service.analyzeChunk("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
