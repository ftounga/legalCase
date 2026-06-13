package fr.ailegalcase.casefile.conclusion;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * F-288 / SF-288-01 — composition des conclusions avant génération.
 *
 * <p>Contrat figé (même base que {@link CaseConclusionController}) :
 * <ul>
 *   <li>{@code GET .../conclusions/composition} → {@code 200} liste des dimensions
 *       curables et de leurs items ({@code key}, {@code label}, {@code included}).
 *       Vague 1 : dimension {@code DECISION_TOOL}, items = outils CALCULÉS.</li>
 *   <li>{@code PUT .../conclusions/composition} → {@code 200} (même corps que GET).
 *       Body {@code { exclusions: [ { dimension, itemKey } ] }} ; remplace l'ensemble
 *       d'exclusions des dimensions présentes. {@code 400} si dimension inconnue.</li>
 * </ul>
 * {@code 404} dossier inexistant, {@code 403} dossier d'un autre workspace.
 * Le controller ne porte aucune logique métier : il délègue à
 * {@link ConclusionCompositionService}.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/conclusions")
public class ConclusionCompositionController {

    private final ConclusionCompositionService service;

    public ConclusionCompositionController(ConclusionCompositionService service) {
        this.service = service;
    }

    @GetMapping("/composition")
    public CompositionResponse getComposition(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.getComposition(caseFileId, oidcUser, principal);
    }

    @PutMapping("/composition")
    public CompositionResponse putComposition(
            @PathVariable UUID caseFileId,
            @RequestBody CompositionUpdateRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.putComposition(caseFileId, request, oidcUser, principal);
    }
}
