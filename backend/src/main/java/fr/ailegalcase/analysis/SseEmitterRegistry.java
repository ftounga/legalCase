package fr.ailegalcase.analysis;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseEmitterRegistry {

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void register(UUID caseFileId, SseEmitter emitter) {
        emitters.computeIfAbsent(caseFileId, k -> new CopyOnWriteArrayList<>()).add(emitter);
    }

    public void remove(UUID caseFileId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(caseFileId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(caseFileId, list);
            }
        }
    }

    public List<SseEmitter> getEmitters(UUID caseFileId) {
        return emitters.getOrDefault(caseFileId, new CopyOnWriteArrayList<>());
    }
}
