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
 * SF-219-09 : endpoints REST de l'outil <i>élections sociales BE</i>
 * (Loi du 04/12/2007 + AR du 25/05/2012 + Loi 04/08/1996 art. 49 +
 * Loi 19/03/1991 protection des candidats).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour le calcul de chronologie +
 *       obligation pour ce dossier (upsert unicité sur
 *       {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si
 *       aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver
 * l'isolation BE-only (élections CSE FR procéduralement très
 * différentes — calendrier libre, seuils 11/50/300, pas de jour Y
 * national imposé).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/elections-sociales-be")
public class ElectionsSocialesBeController {

    private final ElectionsSocialesBeService service;

    public ElectionsSocialesBeController(ElectionsSocialesBeService service) {
        this.service = service;
    }

    @PostMapping
    public ElectionsSocialesBeResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody ElectionsSocialesBeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ElectionsSocialesBeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
