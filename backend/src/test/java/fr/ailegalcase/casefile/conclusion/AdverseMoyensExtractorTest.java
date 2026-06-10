package fr.ailegalcase.casefile.conclusion;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AiCallContext;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.analysis.JobType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * F-261 / SF-261-02 + SF-261-04 — tests unitaires de l'extracteur de moyens
 * adverses : travail / immigration / famille FR → parse les moyens (LLM mocké)
 * avec le prompt dédié ; BE / domaine inconnu → vide sans appel ; texte vide →
 * vide ; échec LLM / parsing → vide (fail-open).
 */
class AdverseMoyensExtractorTest {

    private final AnthropicService anthropicService = mock(AnthropicService.class);
    private final AdverseMoyensExtractor extractor =
            new AdverseMoyensExtractor(anthropicService, new ObjectMapper());

    private static final UUID CASE_FILE_ID = UUID.randomUUID();

    @Test
    void extract_travailFr_parsesMoyensFromLlm() {
        String json = """
                [
                  {"these":"Le licenciement repose sur une faute grave justifiant l'absence d'indemnités.",
                   "fondements":["art. L. 1234-1 du Code du travail","art. L. 1234-9 du Code du travail"],
                   "piecesInvoquees":["Lettre de licenciement","Attestations"]},
                  {"these":"Les demandes de rappel de salaire sont prescrites.",
                   "fondements":["art. L. 3245-1 du Code du travail"],
                   "piecesInvoquees":[]}
                ]
                """;
        when(anthropicService.analyze(any(AiCallContext.class), any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(json, "claude-sonnet-4-6", 100, 200, "end_turn"));

        List<AdverseMoyen> moyens = extractor.extract(
                List.of("Texte des conclusions adverses..."), "DROIT_DU_TRAVAIL", "FRANCE", CASE_FILE_ID);

        assertThat(moyens).hasSize(2);
        assertThat(moyens.get(0).these()).contains("faute grave");
        assertThat(moyens.get(0).fondements()).containsExactly(
                "art. L. 1234-1 du Code du travail", "art. L. 1234-9 du Code du travail");
        assertThat(moyens.get(0).piecesInvoquees()).containsExactly(
                "Lettre de licenciement", "Attestations");
        assertThat(moyens.get(1).these()).contains("prescrites");
        assertThat(moyens.get(1).piecesInvoquees()).isEmpty();
    }

    @Test
    void extract_travailFr_usesSystemLevelGateWithCaseFile() {
        when(anthropicService.analyze(any(AiCallContext.class), any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("[]", "claude-sonnet-4-6", 1, 1, "end_turn"));

        extractor.extract(List.of("texte"), "DROIT_DU_TRAVAIL", "FRANCE", CASE_FILE_ID);

        // gate F-257 obligatoire : JobType cohérent + dossier rattaché
        verify(anthropicService).analyze(
                argThat(ctx -> ctx.jobType() == JobType.SYSTEM_CASE_CONCLUSION
                        && CASE_FILE_ID.equals(ctx.caseFileId())),
                any(), any(), anyInt());
    }

    @Test
    void extract_immigrationFr_parsesMoyensWithImmigrationPrompt() {
        String json = """
                [
                  {"these":"L'obligation de quitter le territoire est légale.",
                   "fondements":["art. L. 611-1 du CESEDA","art. 8 de la CEDH"],
                   "piecesInvoquees":["Arrêté préfectoral"]}
                ]
                """;
        when(anthropicService.analyze(any(AiCallContext.class), any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(json, "claude-sonnet-4-6", 100, 200, "end_turn"));

        List<AdverseMoyen> moyens = extractor.extract(
                List.of("Mémoire en défense de la préfecture..."),
                "DROIT_IMMIGRATION", "FRANCE", CASE_FILE_ID);

        assertThat(moyens).hasSize(1);
        assertThat(moyens.get(0).these()).contains("territoire");
        assertThat(moyens.get(0).fondements()).contains("art. L. 611-1 du CESEDA");
        // le prompt immigration (et lui seul) est passé au LLM
        verify(anthropicService).analyze(
                any(AiCallContext.class),
                argThat(prompt -> prompt == AdverseMoyensExtractor.SYSTEM_PROMPT_IMMIGRATION_FR),
                any(), anyInt());
    }

    @Test
    void extract_familleFr_parsesMoyensWithFamillePrompt() {
        String json = """
                [
                  {"these":"La résidence de l'enfant doit être fixée chez la mère.",
                   "fondements":["art. 373-2-9 du Code civil"],
                   "piecesInvoquees":["Attestation de l'école"]}
                ]
                """;
        when(anthropicService.analyze(any(AiCallContext.class), any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(json, "claude-sonnet-4-6", 100, 200, "end_turn"));

        List<AdverseMoyen> moyens = extractor.extract(
                List.of("Conclusions de l'autre parent..."),
                "DROIT_FAMILLE", "FRANCE", CASE_FILE_ID);

        assertThat(moyens).hasSize(1);
        assertThat(moyens.get(0).these()).contains("résidence");
        assertThat(moyens.get(0).fondements()).contains("art. 373-2-9 du Code civil");
        // le prompt famille (et lui seul) est passé au LLM
        verify(anthropicService).analyze(
                any(AiCallContext.class),
                argThat(prompt -> prompt == AdverseMoyensExtractor.SYSTEM_PROMPT_FAMILLE_FR),
                any(), anyInt());
    }

    @Test
    void extract_travailFr_usesTravailPrompt() {
        when(anthropicService.analyze(any(AiCallContext.class), any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("[]", "claude-sonnet-4-6", 1, 1, "end_turn"));

        extractor.extract(List.of("texte"), "DROIT_DU_TRAVAIL", "FRANCE", CASE_FILE_ID);

        verify(anthropicService).analyze(
                any(AiCallContext.class),
                argThat(prompt -> prompt == AdverseMoyensExtractor.SYSTEM_PROMPT_TRAVAIL_FR),
                any(), anyInt());
    }

    @Test
    void extract_travailButNotFrance_returnsEmptyWithoutLlmCall() {
        List<AdverseMoyen> moyens = extractor.extract(
                List.of("Texte adverse"), "DROIT_DU_TRAVAIL", "BELGIQUE", CASE_FILE_ID);

        assertThat(moyens).isEmpty();
        verify(anthropicService, never()).analyze(any(AiCallContext.class), any(), any(), anyInt());
    }

    @Test
    void extract_immigrationButNotFrance_returnsEmptyWithoutLlmCall() {
        List<AdverseMoyen> moyens = extractor.extract(
                List.of("Texte adverse"), "DROIT_IMMIGRATION", "BELGIQUE", CASE_FILE_ID);

        assertThat(moyens).isEmpty();
        verify(anthropicService, never()).analyze(any(AiCallContext.class), any(), any(), anyInt());
    }

    @Test
    void extract_familleButNotFrance_returnsEmptyWithoutLlmCall() {
        List<AdverseMoyen> moyens = extractor.extract(
                List.of("Texte adverse"), "DROIT_FAMILLE", "BELGIQUE", CASE_FILE_ID);

        assertThat(moyens).isEmpty();
        verify(anthropicService, never()).analyze(any(AiCallContext.class), any(), any(), anyInt());
    }

    @Test
    void extract_unknownDomainFr_returnsEmptyWithoutLlmCall() {
        List<AdverseMoyen> moyens = extractor.extract(
                List.of("Texte adverse"), "DROIT_PENAL", "FRANCE", CASE_FILE_ID);

        assertThat(moyens).isEmpty();
        verify(anthropicService, never()).analyze(any(AiCallContext.class), any(), any(), anyInt());
    }

    @Test
    void extract_emptyText_returnsEmptyWithoutLlmCall() {
        List<AdverseMoyen> moyens = extractor.extract(
                java.util.Arrays.asList("   ", "", null), "DROIT_DU_TRAVAIL", "FRANCE", CASE_FILE_ID);

        assertThat(moyens).isEmpty();
        verify(anthropicService, never()).analyze(any(AiCallContext.class), any(), any(), anyInt());
    }

    @Test
    void extract_nullTextList_returnsEmpty() {
        List<AdverseMoyen> moyens = extractor.extract(
                null, "DROIT_DU_TRAVAIL", "FRANCE", CASE_FILE_ID);

        assertThat(moyens).isEmpty();
        verify(anthropicService, never()).analyze(any(AiCallContext.class), any(), any(), anyInt());
    }

    @Test
    void extract_llmThrows_failsOpenWithEmptyList() {
        when(anthropicService.analyze(any(AiCallContext.class), any(), any(), anyInt()))
                .thenThrow(new RuntimeException("Anthropic 529 overloaded"));

        List<AdverseMoyen> moyens = extractor.extract(
                List.of("texte"), "DROIT_DU_TRAVAIL", "FRANCE", CASE_FILE_ID);

        assertThat(moyens).isEmpty();
    }

    @Test
    void extract_emptyAiResponse_failsOpenWithEmptyList() {
        when(anthropicService.analyze(any(AiCallContext.class), any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("  ", "claude-sonnet-4-6", 1, 0, "end_turn"));

        List<AdverseMoyen> moyens = extractor.extract(
                List.of("texte"), "DROIT_DU_TRAVAIL", "FRANCE", CASE_FILE_ID);

        assertThat(moyens).isEmpty();
    }

    @Test
    void extract_invalidJson_failsOpenWithEmptyList() {
        when(anthropicService.analyze(any(AiCallContext.class), any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("ceci n'est pas du JSON", "claude-sonnet-4-6", 1, 1, "end_turn"));

        List<AdverseMoyen> moyens = extractor.extract(
                List.of("texte"), "DROIT_DU_TRAVAIL", "FRANCE", CASE_FILE_ID);

        assertThat(moyens).isEmpty();
    }

    @Test
    void extract_skipsMoyenWithoutThese_butKeepsValidOnes() {
        String json = """
                [
                  {"these":"","fondements":["x"],"piecesInvoquees":[]},
                  {"these":"Moyen valide.","fondements":[],"piecesInvoquees":[]}
                ]
                """;
        when(anthropicService.analyze(any(AiCallContext.class), any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(json, "claude-sonnet-4-6", 1, 1, "end_turn"));

        List<AdverseMoyen> moyens = extractor.extract(
                List.of("texte"), "DROIT_DU_TRAVAIL", "FRANCE", CASE_FILE_ID);

        assertThat(moyens).hasSize(1);
        assertThat(moyens.get(0).these()).isEqualTo("Moyen valide.");
    }

    @Test
    void extract_tolerantToChattyJsonWrapper() {
        String content = "Voici les moyens :\n[{\"these\":\"Moyen.\",\"fondements\":[],\"piecesInvoquees\":[]}]\nFin.";
        when(anthropicService.analyze(any(AiCallContext.class), any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(content, "claude-sonnet-4-6", 1, 1, "end_turn"));

        List<AdverseMoyen> moyens = extractor.extract(
                List.of("texte"), "DROIT_DU_TRAVAIL", "FRANCE", CASE_FILE_ID);

        assertThat(moyens).hasSize(1);
        assertThat(moyens.get(0).these()).isEqualTo("Moyen.");
    }
}
