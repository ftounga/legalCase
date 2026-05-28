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
 * SF-219-18 : endpoints REST de l'outil <i>semaine de 4 jours BE</i>
 * (Loi du 03/10/2022 « Deal pour l'emploi », art. 5).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only. La semaine de 4 jours française (C. trav. art. L. 3122-2 et
 * suivants — aménagement du temps de travail par accord d'entreprise,
 * Loi 13/06/1998 et Loi 19/01/2000 dite « Aubry II ») constitue un
 * régime distinct (négociation collective majoritairement, et non
 * demande individuelle du salarié). Restitution séparée par outil.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/semaine-4-jours-be")
public class Semaine4JoursBeController {

    private final Semaine4JoursBeService service;

    public Semaine4JoursBeController(
            Semaine4JoursBeService service) {
        this.service = service;
    }

    @PostMapping
    public Semaine4JoursBeResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody Semaine4JoursBeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public Semaine4JoursBeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
