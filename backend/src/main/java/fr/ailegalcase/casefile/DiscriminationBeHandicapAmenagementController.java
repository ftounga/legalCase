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
 * SF-219-23 : endpoints REST de l'outil <i>refus d'aménagements
 * raisonnables handicap BE</i> (Loi du 10/05/2007 art. 14 + art. 17
 * + art. 28 + CCT n° 95 + Directive 2000/78/CE art. 5 + Convention
 * ONU 13/12/2006 art. 27 + Convention OIT n° 159).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only. Le dispositif français d'OETH (Code du travail art. L.
 * 5213-1 et s. + L. 5213-6 obligation d'aménagement raisonnable + L.
 * 1132-1 prohibition discrimination) constitue un régime distinct.
 * Restitution séparée par outil.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/discrimination-be-handicap-amenagement")
public class DiscriminationBeHandicapAmenagementController {

    private final DiscriminationBeHandicapAmenagementService service;

    public DiscriminationBeHandicapAmenagementController(
            DiscriminationBeHandicapAmenagementService service) {
        this.service = service;
    }

    @PostMapping
    public DiscriminationBeHandicapAmenagementResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody DiscriminationBeHandicapAmenagementRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public DiscriminationBeHandicapAmenagementResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
