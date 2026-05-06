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
 * F-193 SF-193-01 — Lecture pure de l'alignement matérialisé checks F-96 ↔
 * outils décisionnels pour la dernière analyse {@code DONE} d'un dossier.
 *
 * <p>Pattern miroir {@link RetainedPisteAlignmentController} (F-192). Aucun
 * calcul à la volée — le contenu vient directement de
 * {@code case_analyses.procedure_checks_alignment_json}, calculé en fin de
 * run de Synthèse enrichie par
 * {@link ProcedureCheckAlignmentService#materializeForAnalysis(CaseAnalysis)}.</p>
 */
@RestController
public class ProcedureCheckAlignmentController {

    private final ProcedureCheckAlignmentService alignmentService;

    public ProcedureCheckAlignmentController(ProcedureCheckAlignmentService alignmentService) {
        this.alignmentService = alignmentService;
    }

    @GetMapping("/api/v1/case-files/{caseFileId}/procedure-checks-alignment")
    public List<ProcedureCheckAlignment> getAlignment(@PathVariable UUID caseFileId,
                                                       @AuthenticationPrincipal OidcUser oidcUser,
                                                       Principal principal) {
        return alignmentService.getForLatestAnalysis(caseFileId, oidcUser, principal);
    }
}
