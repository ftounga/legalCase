package fr.ailegalcase.analysis;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SseEmitterRegistryTest {

    private final SseEmitterRegistry registry = new SseEmitterRegistry();

    // U-01 : register ajoute l'emitter pour le caseFileId
    @Test
    void register_addsEmitterForCaseFileId() {
        UUID id = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);

        registry.register(id, emitter);

        assertThat(registry.getEmitters(id)).containsExactly(emitter);
    }

    // U-02 : register supporte plusieurs emitters pour le même caseFileId
    @Test
    void register_supportsMultipleEmittersPerCaseFile() {
        UUID id = UUID.randomUUID();
        SseEmitter e1 = mock(SseEmitter.class);
        SseEmitter e2 = mock(SseEmitter.class);

        registry.register(id, e1);
        registry.register(id, e2);

        assertThat(registry.getEmitters(id)).hasSize(2).contains(e1, e2);
    }

    // U-03 : remove retire l'emitter — registry vide après retrait du dernier
    @Test
    void remove_cleansUpEntryWhenLastEmitterRemoved() {
        UUID id = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);

        registry.register(id, emitter);
        registry.remove(id, emitter);

        assertThat(registry.getEmitters(id)).isEmpty();
    }

    // U-04 : remove sans registration préalable ne lève pas d'exception
    @Test
    void remove_unknownIdDoesNotThrow() {
        assertThat(registry.getEmitters(UUID.randomUUID())).isEmpty();
    }

    // U-05 : getEmitters retourne liste vide si aucun emitter enregistré
    @Test
    void getEmitters_returnsEmptyListForUnknownId() {
        assertThat(registry.getEmitters(UUID.randomUUID())).isEmpty();
    }
}
