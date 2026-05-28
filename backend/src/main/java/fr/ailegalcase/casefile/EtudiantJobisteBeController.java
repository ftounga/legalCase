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
 * SF-219-13 : endpoints REST de l'outil <i>étudiant jobiste BE</i>
 * (Loi du 03/07/1978 Titre VII + Loi-programme 24/12/2002 + AR
 * 14/07/1995 + Loi-programme 22/12/2023 — quota 600h/an).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver
 * l'isolation BE-only. La France n'a pas d'équivalent direct : le
 * "job étudiant" relève du droit commun du travail (Code du travail
 * FR art. L. 1242-1 et s.) sans cotisations réduites spécifiques, et
 * la Dimona n'existe pas en France. Restitution séparée par outil.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/etudiant-jobiste-be")
public class EtudiantJobisteBeController {

    private final EtudiantJobisteBeService service;

    public EtudiantJobisteBeController(EtudiantJobisteBeService service) {
        this.service = service;
    }

    @PostMapping
    public EtudiantJobisteBeResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody EtudiantJobisteBeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public EtudiantJobisteBeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
