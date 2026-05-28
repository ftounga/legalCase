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
 * SF-219-14 : endpoints REST de l'outil <i>statut intérim BE — CCT
 * n° 322</i> (Loi 24/07/1987 + CCT n° 322 du 14/06/2010 du CNT).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only. La France a un régime de « travail temporaire » (C. trav.
 * art. L. 1251-1 et suivants) qui partage la structure tripartite mais
 * avec des motifs (CDD d'usage, accroissement temporaire d'activité,
 * remplacement), des durées (18 mois max en règle générale art.
 * L. 1251-12), des indemnités (IFM 10 % art. L. 1251-32, ICCP 10 %) et
 * une jurisprudence Cass. soc. radicalement distincts. Pas de CCT
 * n° 322 équivalente. Restitution séparée par outil.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/interim-be-cct-322")
public class InterimBeCct322Controller {

    private final InterimBeCct322Service service;

    public InterimBeCct322Controller(InterimBeCct322Service service) {
        this.service = service;
    }

    @PostMapping
    public InterimBeCct322Response analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody InterimBeCct322Request request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public InterimBeCct322Response get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
