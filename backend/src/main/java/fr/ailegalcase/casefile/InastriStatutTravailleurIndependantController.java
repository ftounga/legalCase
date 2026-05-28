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
 * SF-219-27 : endpoints REST de l'outil <i>INASTI — statut
 * travailleur indépendant et qualification salarié / indépendant</i>
 * (Loi du 27/06/1969 + AR n° 38 du 27/07/1967 + Loi-programme I du
 * 27/12/2006 art. 328 à 333 + critères sectoriels art. 337/2).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier
 *       (upsert unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver
 * l'isolation BE-only. Le régime français de qualification
 * salarié / indépendant (présomption art. L. 8221-6 Code du travail
 * français — présomption d'absence de salariat pour inscrits BCE /
 * RCS, renversement par preuve de subordination juridique permanente
 * ; et inversement art. L. 8221-6-1) est distinct dans ses critères
 * et sa procédure. Restitution séparée par outil.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/inastri-statut-travailleur-independant")
public class InastriStatutTravailleurIndependantController {

    private final InastriStatutTravailleurIndependantService service;

    public InastriStatutTravailleurIndependantController(
            InastriStatutTravailleurIndependantService service) {
        this.service = service;
    }

    @PostMapping
    public InastriStatutTravailleurIndependantResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody InastriStatutTravailleurIndependantRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public InastriStatutTravailleurIndependantResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
