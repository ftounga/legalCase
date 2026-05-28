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
 * SF-219-22 : endpoints REST de l'outil <i>égalité salariale
 * femmes/hommes BE</i> (Loi du 22/04/2012 + AR du 17/08/2013 + AR
 * du 25/04/2014 + CCT n° 25 + C. pén. social art. 195/1).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only. Le régime français d'index égalité professionnelle (Code
 * du travail art. L. 1142-7 et s. ; Décret n° 2019-15 du 08/01/2019)
 * constitue un dispositif distinct (notation 100 points, plan de
 * rattrapage si &lt; 75/100, publication obligatoire). Restitution
 * séparée par outil.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/egalite-femmes-hommes-be")
public class EgaliteFemmesHommesBeController {

    private final EgaliteFemmesHommesBeService service;

    public EgaliteFemmesHommesBeController(
            EgaliteFemmesHommesBeService service) {
        this.service = service;
    }

    @PostMapping
    public EgaliteFemmesHommesBeResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody EgaliteFemmesHommesBeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public EgaliteFemmesHommesBeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
