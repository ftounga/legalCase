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
 * SF-219-08 : endpoints REST de l'outil <i>transfert d'entreprise
 * CCT n° 32bis du 07/06/1985</i> (transfert conventionnel BE — Loi
 * 17/03/1965, Directive 2001/23/CE).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (l'art. L. 1224-1 FR a un régime parent mais sans les
 * spécificités procédurales CCT 32bis / Loi 17/03/1965).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/transfert-entreprise-cct-32bis")
public class TransfertEntrepriseCct32bisController {

    private final TransfertEntrepriseCct32bisService service;

    public TransfertEntrepriseCct32bisController(
            TransfertEntrepriseCct32bisService service) {
        this.service = service;
    }

    @PostMapping
    public TransfertEntrepriseCct32bisResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody TransfertEntrepriseCct32bisRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public TransfertEntrepriseCct32bisResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
