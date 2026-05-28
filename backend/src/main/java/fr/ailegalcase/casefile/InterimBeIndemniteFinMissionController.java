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
 * SF-219-15 : endpoints REST de l'outil <i>indemnité de fin de mission
 * intérim BE</i> (Loi 24/07/1987 + CCT n° 322 + CCT n° 322bis +
 * CCT sectorielles + jurisprudence Cass. BE).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only. La France a un régime d'IFM forfaitaire 10 % (C. trav. fr.
 * art. L. 1251-32) que la jurisprudence Cass. BE refuse explicitement
 * de transposer (Cass. BE 03/05/2010 et 23/06/2003). Restitution séparée
 * par outil.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/interim-be-indemnite-fin-mission")
public class InterimBeIndemniteFinMissionController {

    private final InterimBeIndemniteFinMissionService service;

    public InterimBeIndemniteFinMissionController(
            InterimBeIndemniteFinMissionService service) {
        this.service = service;
    }

    @PostMapping
    public InterimBeIndemniteFinMissionResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody InterimBeIndemniteFinMissionRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public InterimBeIndemniteFinMissionResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
