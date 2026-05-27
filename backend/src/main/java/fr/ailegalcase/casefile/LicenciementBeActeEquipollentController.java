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
 * SF-213-09 : endpoints REST de l'outil d'analyse de l'acte équipollent à
 * rupture BE (Loi 03/07/1978 art. 20 + Cass. BE 23/12/1957).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (la France a un dispositif distinct — prise d'acte
 * L. 1237-19 C. trav. + résiliation judiciaire).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-acte-equivalent")
public class LicenciementBeActeEquipollentController {

    private final LicenciementBeActeEquipollentService service;

    public LicenciementBeActeEquipollentController(
            LicenciementBeActeEquipollentService service) {
        this.service = service;
    }

    @PostMapping
    public LicenciementBeActeEquipollentResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody LicenciementBeActeEquipollentRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public LicenciementBeActeEquipollentResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
