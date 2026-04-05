package fr.ailegalcase.referential;

import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReferentialValidationServiceTest {

    @Mock private AnthropicService anthropicService;
    private ReferentialValidationService service;

    @BeforeEach
    void setUp() {
        service = new ReferentialValidationService(anthropicService);
    }

    // VAL-01 : réponse "VALID" → valid=true, warning=null
    @Test
    void validate_VALIDResponse_returnsValid() {
        when(anthropicService.analyzeFast(anyString(), anyString(), eq(512)))
                .thenReturn(new AnthropicResult("VALID", "haiku", 10, 5));

        var result = service.validate("DROIT_DU_TRAVAIL", "LITIGATION_TYPE",
                "DISCRIMINATION", "Discrimination",
                "{\"years\":5}", "{\"years\":5,\"article\":\"Art. L1132-1\"}");

        assertThat(result.valid()).isTrue();
        assertThat(result.warning()).isNull();
    }

    // VAL-02 : réponse "WARNING: ..." → valid=false, warning rempli
    @Test
    void validate_WARNINGResponse_returnsWarning() {
        when(anthropicService.analyzeFast(anyString(), anyString(), eq(512)))
                .thenReturn(new AnthropicResult(
                        "WARNING: La prescription correcte est de 5 ans pour la discrimination (Art. L1132-1).",
                        "haiku", 10, 30));

        var result = service.validate("DROIT_DU_TRAVAIL", "LITIGATION_TYPE",
                "DISCRIMINATION", "Discrimination",
                "{\"years\":5}", "{\"years\":3}");

        assertThat(result.valid()).isFalse();
        assertThat(result.warning()).contains("5 ans");
        assertThat(result.warning()).contains("discrimination");
    }

    // VAL-04 : libellé système verrouillé — validate() ne reçoit que la valeur à comparer
    @Test
    void validate_systemLabelLocked_onlyValueCompared() {
        when(anthropicService.analyzeFast(anyString(), anyString(), eq(512)))
                .thenReturn(new AnthropicResult("VALID", "haiku", 10, 5));

        service.validate("DROIT_DU_TRAVAIL", "LITIGATION_TYPE",
                "DISCRIMINATION", "Discrimination",
                "{\"years\":5}", "{\"years\":4}");

        var userCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(anthropicService).analyzeFast(anyString(), userCaptor.capture(), eq(512));
        assertThat(userCaptor.getValue()).contains("Valeur actuelle");
        assertThat(userCaptor.getValue()).contains("Valeur proposée");
        assertThat(userCaptor.getValue()).doesNotContain("Libellé proposé");
    }

    // VAL-03 : exception Anthropic → fail-open (valid=true)
    @Test
    void validate_anthropicException_failOpen() {
        when(anthropicService.analyzeFast(anyString(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("Anthropic unavailable"));

        var result = service.validate("DROIT_DU_TRAVAIL", "LITIGATION_TYPE",
                "DISCRIMINATION", "Discrimination",
                "{\"years\":5}", "{\"years\":3}");

        assertThat(result.valid()).isTrue();
        assertThat(result.warning()).isNull();
    }
}
