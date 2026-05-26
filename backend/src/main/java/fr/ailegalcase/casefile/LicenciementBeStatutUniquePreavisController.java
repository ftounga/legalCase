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
 * SF-213-03 : endpoints REST de l'outil de calcul du préavis statut unique
 * en droit belge du travail (Loi du 26 décembre 2013 — barème art. 37/1).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (la France suit un régime juridiquement distinct — convention
 * collective ou code du travail FR pour les préavis de licenciement).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-statut-unique-preavis")
public class LicenciementBeStatutUniquePreavisController {

    private final LicenciementBeStatutUniquePreavisService service;

    public LicenciementBeStatutUniquePreavisController(
            LicenciementBeStatutUniquePreavisService service) {
        this.service = service;
    }

    @PostMapping
    public LicenciementBeStatutUniquePreavisResponse calculate(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody LicenciementBeStatutUniquePreavisRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public LicenciementBeStatutUniquePreavisResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
