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
 * SF-219-16 : endpoints REST de l'outil <i>télétravail BE — CCT n° 85
 * (structurel) / CCT n° 149 (occasionnel)</i>.
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only. Le télétravail français (C. trav. art. L. 1222-9 et suiv.,
 * ANI 19/07/2005, Loi El Khomri 08/08/2016 — droit à la déconnexion,
 * ordonnances Macron 22/09/2017) constitue un régime distinct
 * (cadre par charte ou accord d'entreprise, indemnité forfaitaire
 * URSSAF, droit à la déconnexion intégré au C. trav.). Restitution
 * séparée par outil.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/teletravail-be-cct-85-149")
public class TeletravailBeCct85149Controller {

    private final TeletravailBeCct85149Service service;

    public TeletravailBeCct85149Controller(
            TeletravailBeCct85149Service service) {
        this.service = service;
    }

    @PostMapping
    public TeletravailBeCct85149Response analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody TeletravailBeCct85149Request request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public TeletravailBeCct85149Response get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
