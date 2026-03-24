package fr.ailegalcase.analysis;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SseNotificationServiceTest {

    private final SseEmitterRegistry registry = new SseEmitterRegistry();
    private final SseNotificationService service = new SseNotificationService(registry);

    // U-01 : DONE → send + complete appelés sur l'emitter enregistré
    @Test
    void onAnalysisStatusEvent_done_sendsAndCompletesEmitter() throws IOException {
        UUID caseFileId = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);
        registry.register(caseFileId, emitter);

        service.onAnalysisStatusEvent(new AnalysisStatusEvent(caseFileId, AnalysisStatus.DONE));

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).complete();
    }

    // U-02 : FAILED → send + complete appelés sur l'emitter enregistré
    @Test
    void onAnalysisStatusEvent_failed_sendsAndCompletesEmitter() throws IOException {
        UUID caseFileId = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);
        registry.register(caseFileId, emitter);

        service.onAnalysisStatusEvent(new AnalysisStatusEvent(caseFileId, AnalysisStatus.FAILED));

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).complete();
    }

    // U-03 : emitter retiré du registry après notification
    @Test
    void onAnalysisStatusEvent_removesEmitterFromRegistry() throws IOException {
        UUID caseFileId = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);
        registry.register(caseFileId, emitter);

        service.onAnalysisStatusEvent(new AnalysisStatusEvent(caseFileId, AnalysisStatus.DONE));

        assertThat_registryIsEmpty(caseFileId);
    }

    // U-04 : IOException sur send → emitter retiré sans exception propagée
    @Test
    void onAnalysisStatusEvent_ioExceptionOnSend_doesNotThrow() throws IOException {
        UUID caseFileId = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IOException("broken pipe")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        registry.register(caseFileId, emitter);

        service.onAnalysisStatusEvent(new AnalysisStatusEvent(caseFileId, AnalysisStatus.DONE));

        assertThat_registryIsEmpty(caseFileId);
    }

    // U-05 : aucun emitter enregistré → pas d'appel (pas d'exception)
    @Test
    void onAnalysisStatusEvent_noEmittersRegistered_doesNothing() {
        service.onAnalysisStatusEvent(new AnalysisStatusEvent(UUID.randomUUID(), AnalysisStatus.DONE));
    }

    private void assertThat_registryIsEmpty(UUID caseFileId) {
        org.assertj.core.api.Assertions.assertThat(registry.getEmitters(caseFileId)).isEmpty();
    }
}
