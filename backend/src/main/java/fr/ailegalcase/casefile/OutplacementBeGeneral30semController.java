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
 * SF-219-05 : endpoints REST de l'outil d'analyse de conformité de
 * l'outplacement BE général au titre du régime "préavis ≥ 30 semaines"
 * (Loi du 05/09/2001 art. 11 + AR du 21/10/2007).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (aucun équivalent strict en droit français du régime
 * outplacement général 30 semaines).</p>
 */
@RestController
@RequestMapping(
        "/api/v1/case-files/{caseFileId}/decision-tools/outplacement-be-general-30sem")
public class OutplacementBeGeneral30semController {

    private final OutplacementBeGeneral30semService service;

    public OutplacementBeGeneral30semController(
            OutplacementBeGeneral30semService service) {
        this.service = service;
    }

    @PostMapping
    public OutplacementBeGeneral30semResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody OutplacementBeGeneral30semRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public OutplacementBeGeneral30semResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
