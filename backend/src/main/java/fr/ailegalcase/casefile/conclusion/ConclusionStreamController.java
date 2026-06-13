package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.security.Principal;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * SF-287-01 — flux SSE de la génération des conclusions (réutilise l'infra F-185).
 *
 * <p>{@code GET /api/v1/case-files/{id}/conclusions/stream} ({@code text/event-stream}).
 * Patron exact d'{@code AnalysisStatusStreamController} : l'existence du dossier et
 * l'isolation workspace sont vérifiées <strong>de façon synchrone AVANT</strong> la
 * création de l'emitter (une fois l'emitter renvoyé, le statut HTTP est figé et on ne
 * peut plus signaler 404/403). Si aucune génération n'est active (aucune version
 * {@code PENDING}/{@code PROCESSING}), on court-circuite : l'événement courant
 * ({@code done} si la dernière version est {@code DONE}, sinon rien) est émis puis
 * l'emitter complété — le front retombe sur le {@code GET .../conclusions} existant.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files")
public class ConclusionStreamController {

    /** Aligné sur le flux d'analyse (F-185) — la génération d'un acte tient sous 3 min. */
    private static final long SSE_TIMEOUT_MS = 180_000L;

    private static final Set<CaseConclusionStatus> ACTIVE_STATUSES =
            EnumSet.of(CaseConclusionStatus.PENDING, CaseConclusionStatus.PROCESSING);

    private final ConclusionStreamRegistry registry;
    private final CaseFileRepository caseFileRepository;
    private final CaseConclusionRepository caseConclusionRepository;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public ConclusionStreamController(ConclusionStreamRegistry registry,
                                      CaseFileRepository caseFileRepository,
                                      CaseConclusionRepository caseConclusionRepository,
                                      CurrentUserResolver currentUserResolver,
                                      WorkspaceMemberRepository workspaceMemberRepository) {
        this.registry = registry;
        this.caseFileRepository = caseFileRepository;
        this.caseConclusionRepository = caseConclusionRepository;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @GetMapping(value = "/{id}/conclusions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable UUID id,
                             @AuthenticationPrincipal OidcUser oidcUser,
                             Principal principal) {
        // Isolation workspace synchrone AVANT l'emitter (cf. AnalysisStatusStreamController).
        var user = currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
        var workspace = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(WorkspaceMember::getWorkspace)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));

        var caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Case file not in current workspace");
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        // Court-circuit : aucune génération active → pas d'événement à streamer.
        // On émet l'état terminal courant (done sur la dernière version DONE) puis on
        // complète ; le front bascule alors sur le GET .../conclusions classique.
        boolean active = caseConclusionRepository.existsByCaseFileIdAndStatusIn(id, ACTIVE_STATUSES);
        if (!active) {
            caseConclusionRepository.findFirstByCaseFileIdOrderByVersionNumberDesc(id)
                    .filter(c -> c.getStatus() == CaseConclusionStatus.DONE)
                    .ifPresent(done -> {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data("{\"status\":\"DONE\",\"versionNumber\":%d}"
                                            .formatted(done.getVersionNumber())));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    });
            emitter.complete();
            return emitter;
        }

        registry.register(id, emitter);
        emitter.onCompletion(() -> registry.remove(id, emitter));
        emitter.onTimeout(() -> registry.remove(id, emitter));
        emitter.onError(e -> registry.remove(id, emitter));

        return emitter;
    }
}
