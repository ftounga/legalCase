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
 * SF-213-05 : endpoints REST de l'outil d'analyse de la validité d'un
 * licenciement pendant la grossesse / maternité en droit belge
 * (<b>Loi du 16/03/1971 art. 40</b>) et de calcul de l'indemnité
 * forfaitaire de 6 mois de rémunération brute.
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (pas d'équivalent direct FR — la France a une protection
 * similaire mais l'indemnité et la durée de protection diffèrent).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-protection-grossesse")
public class LicenciementBeProtectionGrossesseController {

    private final LicenciementBeProtectionGrossesseService service;

    public LicenciementBeProtectionGrossesseController(
            LicenciementBeProtectionGrossesseService service) {
        this.service = service;
    }

    @PostMapping
    public LicenciementBeProtectionGrossesseResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody LicenciementBeProtectionGrossesseRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public LicenciementBeProtectionGrossesseResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
