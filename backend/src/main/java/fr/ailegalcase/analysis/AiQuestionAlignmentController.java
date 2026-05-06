package fr.ailegalcase.analysis;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * F-196 SF-196-01 — Contrôleur lecture pure de l'alignement matérialisé des
 * questions complémentaires F-94.
 *
 * <ul>
 *   <li>{@code GET /api/v1/case-files/{id}/ai-questions-alignment} — lecture
 *       pure de l'alignement matérialisé pour la dernière analyse
 *       {@code DONE} du dossier (depuis
 *       {@code case_analyses.ai_questions_alignment_json}).</li>
 * </ul>
 *
 * <p>Pas de mutation côté F-196 : la PUT/POST réponse passe toujours par le
 * contrôleur F-94 ({@link AiQuestionAnswerController}) qui reste STRICTEMENT
 * inchangé. F-196 n'expose qu'un endpoint de lecture en plus.</p>
 *
 * <p>Pattern miroir {@link RetainedPisteAlignmentController} (F-192) +
 * {@link ProcedureCheckAlignmentController} (F-193).</p>
 */
@RestController
public class AiQuestionAlignmentController {

    private final AiQuestionAlignmentService aiQuestionAlignmentService;

    public AiQuestionAlignmentController(AiQuestionAlignmentService aiQuestionAlignmentService) {
        this.aiQuestionAlignmentService = aiQuestionAlignmentService;
    }

    /**
     * Lecture pure. Renvoie {@code []} si aucune analyse {@code DONE} ou si
     * l'alignement est vide / legacy (pré-F-196).
     */
    @GetMapping("/api/v1/case-files/{caseFileId}/ai-questions-alignment")
    public List<AiQuestionAlignment> getAlignment(@PathVariable UUID caseFileId,
                                                   @AuthenticationPrincipal OidcUser oidcUser,
                                                   Principal principal) {
        return aiQuestionAlignmentService.getForLatestAnalysis(caseFileId, oidcUser, principal);
    }
}
