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
 * SF-219-32 : endpoints REST de l'outil <i>Interruption de carrière
 * pour congé parental BE</i> (Loi du 22/01/1985 art. 99 à 107quater +
 * AR du 29/10/1997 + CCT n° 64).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier
 *       (upsert unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver
 * l'isolation BE-only. La France a un régime distinct (Code du travail
 * FR art. L. 1225-47 à L. 1225-60 — congé parental d'éducation, durée
 * et base AT différentes, allocations CAF distinctes ONEM). Restitution
 * séparée par outil.</p>
 *
 * <p>Distinct de F-DT-29 {@code credit-temps-be} (crédit-temps CCT 103
 * régime universel sans motif spécifique). SF-219-32 couvre
 * spécifiquement le congé parental régi par la Loi 22/01/1985 et la
 * CCT 64 (droit individuel par enfant et par parent avec conditions
 * d'âge enfant et formes spécifiques 4 mois temps plein / 8 mois
 * mi-temps / 20 mois cinquième temps).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/interruption-carriere-soins-parental")
public class InterruptionCarriereSoinsParentalController {

    private final InterruptionCarriereSoinsParentalService service;

    public InterruptionCarriereSoinsParentalController(
            InterruptionCarriereSoinsParentalService service) {
        this.service = service;
    }

    @PostMapping
    public InterruptionCarriereSoinsParentalResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody InterruptionCarriereSoinsParentalRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public InterruptionCarriereSoinsParentalResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
