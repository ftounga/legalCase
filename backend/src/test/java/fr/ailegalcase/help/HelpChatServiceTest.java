package fr.ailegalcase.help;

import fr.ailegalcase.analysis.AiCallContext;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@ExtendWith(MockitoExtension.class)
class HelpChatServiceTest {

    @Mock private AnthropicService anthropicService;
    @Mock private HelpDocumentLoader helpDocumentLoader;

    private HelpChatService service;

    @BeforeEach
    void setUp() {
        when(helpDocumentLoader.getDocumentation()).thenReturn("# Documentation produit\nContenu de test.");
        service = new HelpChatService(anthropicService, helpDocumentLoader);
    }

    // U-01 : message valide → analyzeFast() appelé avec system prompt contenant la doc
    @Test
    void chat_validMessage_callsAnalyzeFastWithDocInSystemPrompt() {
        when(anthropicService.analyzeFast(any(AiCallContext.class), anyString(), anyString(), eq(512)))
                .thenReturn(new AnthropicResult("Voici comment faire.", "haiku", 10, 20));

        service.chat("Comment créer un dossier ?");

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(anthropicService).analyzeFast(any(AiCallContext.class), systemPromptCaptor.capture(), eq("Comment créer un dossier ?"), eq(512));
        assertThat(systemPromptCaptor.getValue()).contains("Documentation produit");
        assertThat(systemPromptCaptor.getValue()).contains("AI LegalCase");
    }

    // U-02 : réponse Anthropic retournée dans HelpChatResponse.answer
    @Test
    void chat_validMessage_returnsAnthropicContentAsAnswer() {
        when(anthropicService.analyzeFast(any(AiCallContext.class), anyString(), anyString(), anyInt()))
                .thenReturn(new AnthropicResult("Pour créer un dossier, cliquez sur Nouveau dossier.", "haiku", 10, 30));

        HelpChatResponse response = service.chat("Comment créer un dossier ?");

        assertThat(response.answer()).isEqualTo("Pour créer un dossier, cliquez sur Nouveau dossier.");
    }

    // U-03 : Anthropic lance exception → 503 propagé
    @Test
    void chat_anthropicThrowsException_throws503() {
        when(anthropicService.analyzeFast(any(AiCallContext.class), anyString(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("Anthropic timeout"));

        assertThatThrownBy(() -> service.chat("Comment créer un dossier ?"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(SERVICE_UNAVAILABLE));
    }
}
