package fr.ailegalcase.casefile.conclusion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SF-287-01 — tests unitaires du publisher SSE de génération des conclusions :
 * détection de section, calcul du pourcentage, événements progress/done/failed,
 * et fail-open sur emitter cassé.
 */
class ConclusionStreamPublisherTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // ── Détection de section (dernier titre ## / ###) ─────────────────────────

    @Test
    void lastSection_noHeading_returnsNull() {
        assertThat(ConclusionStreamPublisher.lastSection("Texte sans titre encore.")).isNull();
        assertThat(ConclusionStreamPublisher.lastSection("")).isNull();
        assertThat(ConclusionStreamPublisher.lastSection(null)).isNull();
    }

    @Test
    void lastSection_returnsLastHeadingWithOneBasedIndex() {
        String partial = """
                # POUR
                ## I. FAITS
                blabla
                ## II. DISCUSSION
                ### A. Sur la prescription
                en cours de rédaction…""";
        ConclusionStreamPublisher.Section section = ConclusionStreamPublisher.lastSection(partial);
        assertThat(section).isNotNull();
        assertThat(section.title()).isEqualTo("A. Sur la prescription");
        // ## I. FAITS (1), ## II. DISCUSSION (2), ### A. Sur la prescription (3) — # POUR ignoré
        assertThat(section.index()).isEqualTo(3);
    }

    // ── Événement progress ────────────────────────────────────────────────────

    @Test
    void publishProgress_emitsProgressEventWithPercentAndSection() throws IOException {
        ConclusionStreamRegistry registry = new ConclusionStreamRegistry();
        ConclusionStreamPublisher publisher = new ConclusionStreamPublisher(registry, mapper);
        UUID caseFileId = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);
        registry.register(caseFileId, emitter);

        publisher.publishProgress(caseFileId, "## I. FAITS\ndébut", 4000, 8000);

        ArgumentCaptor<SseEmitter.SseEventBuilder> captor =
                ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(emitter).send(captor.capture());
        String rendered = captor.getValue().build().stream()
                .map(d -> String.valueOf(d.getData())).reduce("", String::concat);
        assertThat(rendered).contains("progress");
        assertThat(rendered).contains("\"percent\":50");
        assertThat(rendered).contains("\"maxTokens\":8000");
        assertThat(rendered).contains("I. FAITS");
    }

    @Test
    void publishProgress_noEmitter_noop() {
        ConclusionStreamRegistry registry = new ConclusionStreamRegistry();
        ConclusionStreamPublisher publisher = new ConclusionStreamPublisher(registry, mapper);
        // Aucun emitter enregistré → aucune exception.
        publisher.publishProgress(UUID.randomUUID(), "## I", 100, 8000);
    }

    // ── Événements terminaux ─────────────────────────────────────────────────

    @Test
    void publishDone_emitsDoneEventWithVersionNumber() throws IOException {
        ConclusionStreamRegistry registry = new ConclusionStreamRegistry();
        ConclusionStreamPublisher publisher = new ConclusionStreamPublisher(registry, mapper);
        UUID caseFileId = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);
        registry.register(caseFileId, emitter);

        publisher.publishDone(caseFileId, 7);

        ArgumentCaptor<SseEmitter.SseEventBuilder> captor =
                ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(emitter).send(captor.capture());
        String rendered = captor.getValue().build().stream()
                .map(d -> String.valueOf(d.getData())).reduce("", String::concat);
        assertThat(rendered).contains("done");
        assertThat(rendered).contains("\"status\":\"DONE\"");
        assertThat(rendered).contains("\"versionNumber\":7");
    }

    @Test
    void publishFailed_emitsFailedEventWithMessage() throws IOException {
        ConclusionStreamRegistry registry = new ConclusionStreamRegistry();
        ConclusionStreamPublisher publisher = new ConclusionStreamPublisher(registry, mapper);
        UUID caseFileId = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);
        registry.register(caseFileId, emitter);

        publisher.publishFailed(caseFileId, "Échec de la génération.");

        ArgumentCaptor<SseEmitter.SseEventBuilder> captor =
                ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(emitter).send(captor.capture());
        String rendered = captor.getValue().build().stream()
                .map(d -> String.valueOf(d.getData())).reduce("", String::concat);
        assertThat(rendered).contains("failed");
        assertThat(rendered).contains("\"status\":\"FAILED\"");
        assertThat(rendered).contains("Échec de la génération.");
    }

    // ── Fail-open : un emitter cassé est retiré, pas d'exception propagée ─────

    @Test
    void send_brokenEmitter_isRemovedAndNotPropagated() throws IOException {
        ConclusionStreamRegistry registry = new ConclusionStreamRegistry();
        ConclusionStreamPublisher publisher = new ConclusionStreamPublisher(registry, mapper);
        UUID caseFileId = UUID.randomUUID();
        SseEmitter broken = mock(SseEmitter.class);
        doThrow(new IOException("client gone")).when(broken).send(any(SseEmitter.SseEventBuilder.class));
        registry.register(caseFileId, broken);

        // Ne doit pas lever.
        publisher.publishProgress(caseFileId, "## I", 100, 8000);

        // Emitter retiré du registre (envoi suivant = no-op).
        assertThat(registry.getEmitters(caseFileId)).isEmpty();
    }
}
