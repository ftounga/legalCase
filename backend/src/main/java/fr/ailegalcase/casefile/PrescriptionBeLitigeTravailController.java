package fr.ailegalcase.casefile;

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
 * SF-207-01 : endpoint REST de l'outil de prescription Travail BE.
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (l'outil n'existe pas côté FR).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/prescription-be-litige-travail")
public class PrescriptionBeLitigeTravailController {

    private final PrescriptionBeLitigeTravailService service;

    public PrescriptionBeLitigeTravailController(PrescriptionBeLitigeTravailService service) {
        this.service = service;
    }

    @PostMapping
    public PrescriptionBeLitigeTravailResponse calculate(@PathVariable UUID caseFileId,
                                                         @RequestBody PrescriptionBeLitigeTravailRequest request,
                                                         @AuthenticationPrincipal OidcUser oidcUser,
                                                         Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public PrescriptionBeLitigeTravailResponse get(@PathVariable UUID caseFileId,
                                                   @AuthenticationPrincipal OidcUser oidcUser,
                                                   Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
