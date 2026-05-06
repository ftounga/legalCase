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
 * F-192 SF-192-01 — Lecture pure de l'alignement matérialisé pistes RETAINED ↔
 * outils décisionnels pour la dernière analyse {@code DONE} d'un dossier.
 *
 * <p>Aucun calcul à la volée — le contenu vient directement de
 * {@code case_analyses.retained_pistes_alignment_json}, calculé en fin de run
 * de Synthèse enrichie ({@code EnrichedAnalysisService}).</p>
 */
@RestController
public class RetainedPisteAlignmentController {

    private final RetainedPisteAlignmentService alignmentService;

    public RetainedPisteAlignmentController(RetainedPisteAlignmentService alignmentService) {
        this.alignmentService = alignmentService;
    }

    @GetMapping("/api/v1/case-files/{caseFileId}/retained-pistes-alignment")
    public List<RetainedPisteAlignment> getAlignment(@PathVariable UUID caseFileId,
                                                     @AuthenticationPrincipal OidcUser oidcUser,
                                                     Principal principal) {
        return alignmentService.getForLatestAnalysis(caseFileId, oidcUser, principal);
    }
}
