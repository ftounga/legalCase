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
 * SF-219-06 : endpoints REST de l'outil <i>licenciement BE fermeture
 * entreprise</i> (Loi 26/06/2002 + AR 23/03/2007 + CCT n° 9bis, FFE).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (aucun équivalent strict du Fonds Fermeture Entreprises en
 * droit français — le FNGS couvre la garantie salaires, pas l'indemnité
 * de fermeture forfaitaire).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-fermeture-entreprise")
public class LicenciementBeFermetureEntrepriseController {

    private final LicenciementBeFermetureEntrepriseService service;

    public LicenciementBeFermetureEntrepriseController(
            LicenciementBeFermetureEntrepriseService service) {
        this.service = service;
    }

    @PostMapping
    public LicenciementBeFermetureEntrepriseResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody LicenciementBeFermetureEntrepriseRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public LicenciementBeFermetureEntrepriseResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
