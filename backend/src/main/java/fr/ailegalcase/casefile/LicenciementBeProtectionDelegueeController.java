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
 * SF-213-08 : endpoints REST de l'outil d'analyse de la protection d'un
 * délégué syndical / candidat non élu BE en cas de licenciement
 * (Loi du 19/03/1991 + CCT n° 5 du 24/05/1971).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (la France a un dispositif distinct — statut protégé délégué /
 * CSE / conseiller prud'homme, L. 2411-1 et s. C. trav.).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-protection-deleguee")
public class LicenciementBeProtectionDelegueeController {

    private final LicenciementBeProtectionDelegueeService service;

    public LicenciementBeProtectionDelegueeController(
            LicenciementBeProtectionDelegueeService service) {
        this.service = service;
    }

    @PostMapping
    public LicenciementBeProtectionDelegueeResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody LicenciementBeProtectionDelegueeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public LicenciementBeProtectionDelegueeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
