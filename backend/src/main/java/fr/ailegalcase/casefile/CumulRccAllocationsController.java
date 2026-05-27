package fr.ailegalcase.casefile;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-219-04 : endpoints REST de l'outil d'analyse du cumul RCC BE +
 * allocations chômage / pension (CCT n° 17 + AR 25/11/1991 + AR
 * 03/05/2007 art. 22 et suiv.).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (aucun équivalent strict en droit français du RCC).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/cumul-rcc-allocations")
public class CumulRccAllocationsController {

    private final CumulRccAllocationsService service;

    public CumulRccAllocationsController(CumulRccAllocationsService service) {
        this.service = service;
    }

    @PostMapping
    public CumulRccAllocationsResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody CumulRccAllocationsRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public CumulRccAllocationsResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
