package fr.ailegalcase.casefile.conclusion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SF-287-01 — publication des événements SSE du streaming de génération des
 * conclusions vers les emitters du dossier ({@link ConclusionStreamRegistry}).
 *
 * <p>Trois événements (cf. contrat figé de la mini-spec) :
 * <ul>
 *   <li>{@code progress} → {@link ConclusionStreamProgress} (à chaque fragment, throttlé) ;</li>
 *   <li>{@code done} → {@code {"status":"DONE","versionNumber":N}} (le content final est
 *       relu via {@code GET .../conclusions}) ;</li>
 *   <li>{@code failed} → {@code {"status":"FAILED","message":"…"}}.</li>
 * </ul>
 * Un envoi en échec (client déconnecté) retire l'emitter du registre, sans jamais
 * propager l'exception au worker (fail-open : le streaming est cosmétique, la
 * génération et la finalisation restent inchangées).</p>
 */
@Service
public class ConclusionStreamPublisher {

    private static final Logger log = LoggerFactory.getLogger(ConclusionStreamPublisher.class);

    /** Titres markdown de section : {@code ##} ou {@code ###} en début de ligne. */
    private static final Pattern HEADING = Pattern.compile("(?m)^#{2,3}\\s+(.+?)\\s*$");

    private final ConclusionStreamRegistry registry;
    private final ObjectMapper objectMapper;

    public ConclusionStreamPublisher(ConclusionStreamRegistry registry, ObjectMapper objectMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    /**
     * Calcule l'avancement à partir du markdown partiel et publie un événement
     * {@code progress} aux emitters du dossier.
     *
     * @param caseFileId   dossier ciblé
     * @param partial      markdown accumulé jusqu'ici
     * @param outputTokens tokens de sortie reçus (compteur SDK si exposé, sinon estimation appelant)
     * @param maxTokens    budget de tokens de sortie
     */
    public void publishProgress(UUID caseFileId, String partial, int outputTokens, int maxTokens) {
        int percent = maxTokens <= 0 ? 0
                : Math.min(100, Math.round((float) outputTokens / maxTokens * 100));
        Section section = lastSection(partial);
        ConclusionStreamProgress payload = new ConclusionStreamProgress(
                outputTokens, maxTokens, percent,
                section != null ? section.title() : null,
                section != null ? section.index() : null,
                partial);
        send(caseFileId, "progress", payload);
    }

    /** Publie l'événement terminal {@code done} (le content final est relu via GET). */
    public void publishDone(UUID caseFileId, int versionNumber) {
        send(caseFileId, "done", new DonePayload("DONE", versionNumber));
    }

    /** Publie l'événement terminal {@code failed} (message court, sans jargon technique). */
    public void publishFailed(UUID caseFileId, String message) {
        send(caseFileId, "failed", new FailedPayload("FAILED", message));
    }

    /**
     * Détecte le dernier titre {@code ##}/{@code ###} du partiel et son index 1-based.
     * Retourne {@code null} si aucun titre n'est encore apparu (préambule en cours).
     */
    static Section lastSection(String partial) {
        if (partial == null || partial.isEmpty()) {
            return null;
        }
        Matcher m = HEADING.matcher(partial);
        int count = 0;
        String lastTitle = null;
        while (m.find()) {
            count++;
            lastTitle = m.group(1).trim();
        }
        return lastTitle == null ? null : new Section(lastTitle, count);
    }

    private void send(UUID caseFileId, String eventName, Object payload) {
        List<SseEmitter> emitters = List.copyOf(registry.getEmitters(caseFileId));
        if (emitters.isEmpty()) {
            return;
        }
        String data;
        try {
            data = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("Sérialisation de l'événement {} impossible pour caseFile {} : {}",
                    eventName, caseFileId, e.getMessage());
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException | RuntimeException e) {
                // Client déconnecté ou emitter clos : on retire et on continue.
                log.debug("Envoi SSE {} échoué pour caseFile {} — emitter retiré", eventName, caseFileId);
                registry.remove(caseFileId, emitter);
            }
        }
    }

    /** Dernier titre de section détecté + son index 1-based. */
    record Section(String title, int index) {
    }

    private record DonePayload(String status, int versionNumber) {
    }

    private record FailedPayload(String status, String message) {
    }
}
