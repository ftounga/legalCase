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
 * SF-213-04 : endpoints REST de l'outil de calcul du préavis selon la
 * Formule Claeys (ancien art. 82 Loi 03/07/1978, avant loi 26/12/2013) en
 * droit belge du travail.
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (pas d'équivalent FR — la France relève d'un régime juridiquement
 * distinct).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-formule-claeys")
public class LicenciementBeFormuleClaeysController {

    private final LicenciementBeFormuleClaeysService service;

    public LicenciementBeFormuleClaeysController(
            LicenciementBeFormuleClaeysService service) {
        this.service = service;
    }

    @PostMapping
    public LicenciementBeFormuleClaeysResponse calculate(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody LicenciementBeFormuleClaeysRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public LicenciementBeFormuleClaeysResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
