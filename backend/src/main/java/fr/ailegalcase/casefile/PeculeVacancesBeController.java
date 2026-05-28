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
 * SF-219-20 : endpoints REST de l'outil <i>pécule de vacances BE</i>
 * (Lois coordonnées du 28/06/1971 + AR du 30/03/1967).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only. L'ICCP française (C. trav. art. L. 3141-24 et suivants —
 * 10 % de la rémunération brute totale ou maintien du salaire) repose
 * sur un mécanisme différent (pas de double pécule, pas de débiteur
 * tiers de type ONVA). Restitution séparée par outil.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/pecule-vacances-be")
public class PeculeVacancesBeController {

    private final PeculeVacancesBeService service;

    public PeculeVacancesBeController(
            PeculeVacancesBeService service) {
        this.service = service;
    }

    @PostMapping
    public PeculeVacancesBeResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody PeculeVacancesBeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public PeculeVacancesBeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
