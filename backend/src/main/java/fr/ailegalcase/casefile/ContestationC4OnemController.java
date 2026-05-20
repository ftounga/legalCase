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
 * SF-207-03 : endpoints REST de l'outil de contestation C4 ONEM Travail BE
 * (double délai : recours administratif Directeur 1 mois + recours
 * juridictionnel tribunal du travail 3 mois).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (l'outil n'existe pas côté FR — l'équivalent FR est
 * F-DT-35 contestation-are géré par {@link ContestationAreController}).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/contestation-c4-onem")
public class ContestationC4OnemController {

    private final ContestationC4OnemService service;

    public ContestationC4OnemController(ContestationC4OnemService service) {
        this.service = service;
    }

    @PostMapping
    public ContestationC4OnemResponse calculate(@PathVariable UUID caseFileId,
                                                @Valid @RequestBody ContestationC4OnemRequest request,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ContestationC4OnemResponse get(@PathVariable UUID caseFileId,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
