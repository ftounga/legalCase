package fr.ailegalcase.analysis;

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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files")
public class AnalysisStatusStreamController {

    private static final long SSE_TIMEOUT_MS = 180_000L;

    private static final List<AnalysisStatus> ACTIVE_STATUSES =
            List.of(AnalysisStatus.PENDING, AnalysisStatus.PROCESSING);

    private final SseEmitterRegistry registry;
    private final CaseFileRepository caseFileRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public AnalysisStatusStreamController(SseEmitterRegistry registry,
                                          CaseFileRepository caseFileRepository,
                                          CaseAnalysisRepository caseAnalysisRepository,
                                          AnalysisJobRepository analysisJobRepository,
                                          CurrentUserResolver currentUserResolver,
                                          WorkspaceMemberRepository workspaceMemberRepository) {
        this.registry = registry;
        this.caseFileRepository = caseFileRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @GetMapping(value = "/{id}/analysis-status/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable UUID id,
                             @AuthenticationPrincipal OidcUser oidcUser,
                             Principal principal) {
        // Vérification existence + isolation workspace AVANT la création de l'emitter :
        // une fois l'emitter renvoyé le statut HTTP est figé, on ne peut plus signaler
        // 404/403. On lève donc les exceptions de statut de façon synchrone (même
        // contrat que les contrôleurs frères, ex. AnalysisJobController).
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

        // SF-159-03 — court-circuit uniquement si AUCUN job actif (PENDING/PROCESSING).
        // Avant : la présence d'une CaseAnalysis DONE court-circuitait l'enregistrement
        // de l'emitter, même si un nouveau cycle (DOCUMENT_ANALYSIS / ENRICHED_ANALYSIS)
        // venait d'être déclenché. La bannière "Analyse en cours" ne se clearait jamais
        // côté frontend faute de réception du DONE event correspondant.
        boolean hasActiveJob = analysisJobRepository.existsByCaseFileIdAndStatusIn(id, ACTIVE_STATUSES);
        var done = caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(id, AnalysisStatus.DONE)
                .orElse(null);
        if (done != null && !hasActiveJob) {
            try {
                emitter.send(SseEmitter.event()
                        .name("ANALYSIS_DONE")
                        .data("{\"caseFileId\":\"%s\",\"status\":\"DONE\"}".formatted(id)));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        registry.register(id, emitter);
        emitter.onCompletion(() -> registry.remove(id, emitter));
        emitter.onTimeout(() -> registry.remove(id, emitter));
        emitter.onError(e -> registry.remove(id, emitter));

        return emitter;
    }
}
