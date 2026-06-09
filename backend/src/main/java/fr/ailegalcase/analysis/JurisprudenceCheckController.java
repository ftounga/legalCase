package fr.ailegalcase.analysis;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * F-179 SF-179-01 — expose les vérifications de jurisprudence citée d'un dossier.
 */
@RestController
public class JurisprudenceCheckController {

    private final JurisprudenceCheckQueryService jurisprudenceCheckQueryService;

    public JurisprudenceCheckController(JurisprudenceCheckQueryService jurisprudenceCheckQueryService) {
        this.jurisprudenceCheckQueryService = jurisprudenceCheckQueryService;
    }

    /**
     * Renvoie les vérifications de jurisprudence de la dernière analyse
     * {@code DONE} du dossier. {@code checks} est vide si aucune référence n'a
     * été détectée.
     */
    @GetMapping("/api/v1/case-files/{caseFileId}/jurisprudence-checks")
    public JurisprudenceCheckResponse list(@PathVariable UUID caseFileId,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return jurisprudenceCheckQueryService.getForCaseFile(caseFileId, oidcUser, principal);
    }

    /**
     * F-98 / SF-98-56 — marque (ou démarque) une citation comme « adverse à réfuter ».
     *
     * <p>Marquage autorisé uniquement sur les statuts réfutables (SUSPECT / NOT_FOUND) :
     * un autre statut renvoie 422. {@code markedAdverse} est obligatoire (400 si absent).
     * Isolation workspace stricte (404 hors workspace).</p>
     */
    @PatchMapping("/api/v1/case-files/{caseFileId}/jurisprudence-checks/{checkId}/adverse-marking")
    public JurisprudenceCheckResponse.Check markAdverse(
            @PathVariable UUID caseFileId,
            @PathVariable UUID checkId,
            @Valid @RequestBody AdverseMarkingRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return jurisprudenceCheckQueryService.markAdverse(
                caseFileId, checkId, request.markedAdverse(), oidcUser, principal);
    }

    /**
     * Corps du PATCH de marquage adverse. {@code markedAdverse} est obligatoire :
     * un body sans ce champ (ou {@code null}) → 400.
     */
    public record AdverseMarkingRequest(@NotNull Boolean markedAdverse) {
    }
}
