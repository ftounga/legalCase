package fr.ailegalcase.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Service
public class SseNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SseNotificationService.class);

    private final SseEmitterRegistry registry;

    public SseNotificationService(SseEmitterRegistry registry) {
        this.registry = registry;
    }

    @EventListener
    public void onAnalysisStatusEvent(AnalysisStatusEvent event) {
        List<SseEmitter> emitters = List.copyOf(registry.getEmitters(event.caseFileId()));
        if (emitters.isEmpty()) return;

        String eventName = event.status() == AnalysisStatus.DONE ? "ANALYSIS_DONE" : "ANALYSIS_FAILED";
        String data = "{\"caseFileId\":\"%s\",\"status\":\"%s\"}".formatted(event.caseFileId(), event.status());

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
                emitter.complete();
            } catch (IOException e) {
                log.warn("SSE send failed for caseFile {} — removing emitter", event.caseFileId());
            } finally {
                registry.remove(event.caseFileId(), emitter);
            }
        }
    }
}
