package fr.ailegalcase.casefile.conclusion;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SF-287-01 — registre des {@link SseEmitter} ouverts sur le flux de génération de
 * conclusions, indexés par {@code caseFileId}.
 *
 * <p>Jumeau de {@code SseEmitterRegistry} (F-185) : un registre dédié au flux
 * conclusions évite de mélanger ses événements ({@code progress}/{@code done}/
 * {@code failed}) avec ceux du flux d'analyse (qui complète l'emitter au premier
 * statut terminal). Le flux conclusions reste ouvert pendant toute la génération.</p>
 */
@Component
public class ConclusionStreamRegistry {

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters =
            new ConcurrentHashMap<>();

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
