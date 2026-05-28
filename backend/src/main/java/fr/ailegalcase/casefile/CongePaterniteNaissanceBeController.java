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
 * SF-219-31 : endpoints REST de l'outil <i>Congé paternité / naissance
 * BE — calcul de durée et de protection</i> (Loi du 03/07/1978
 * art. 30 § 2 + Loi du 12/08/2000 + Loi du 07/04/2023 Deal pour
 * l'emploi).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier
 *       (upsert unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver
 * l'isolation BE-only. La France a un régime distinct (Code du travail
 * FR art. L. 1225-35 congé de paternité et d'accueil de l'enfant —
 * 25 jours calendaires dont 7 obligatoires depuis la Loi de financement
 * de la sécurité sociale 2021). Restitution séparée par outil.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/conge-paternite-naissance-be")
public class CongePaterniteNaissanceBeController {

    private final CongePaterniteNaissanceBeService service;

    public CongePaterniteNaissanceBeController(
            CongePaterniteNaissanceBeService service) {
        this.service = service;
    }

    @PostMapping
    public CongePaterniteNaissanceBeResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody CongePaterniteNaissanceBeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public CongePaterniteNaissanceBeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
