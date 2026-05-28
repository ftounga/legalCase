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
 * SF-219-07 : endpoints REST de l'outil <i>licenciement collectif BE
 * Loi Renault</i> (Loi du 13/02/1998 + CCT n° 24 + CCT n° 39 + délai
 * d'attente 30 jours).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour la checklist procédurale pour ce
 *       dossier (upsert unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (PSE français procéduralement très différent — DIRECCTE /
 * DDETS, validation / homologation, expertise CSE).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-collectif-renault")
public class LicenciementBeCollectifRenaultController {

    private final LicenciementBeCollectifRenaultService service;

    public LicenciementBeCollectifRenaultController(
            LicenciementBeCollectifRenaultService service) {
        this.service = service;
    }

    @PostMapping
    public LicenciementBeCollectifRenaultResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody LicenciementBeCollectifRenaultRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public LicenciementBeCollectifRenaultResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
