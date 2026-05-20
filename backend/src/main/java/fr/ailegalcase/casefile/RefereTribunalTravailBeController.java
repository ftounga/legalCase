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
 * SF-207-05 : endpoints REST de l'outil d'éligibilité au référé devant le
 * président du tribunal du travail belge (CJ art. 584).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (l'outil n'existe pas côté FR ; le référé prud'homal FR
 * R.1454-1 CT relève d'un régime distinct).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/refere-tribunal-travail-be")
public class RefereTribunalTravailBeController {

    private final RefereTribunalTravailBeService service;

    public RefereTribunalTravailBeController(RefereTribunalTravailBeService service) {
        this.service = service;
    }

    @PostMapping
    public RefereTribunalTravailBeResponse calculate(@PathVariable UUID caseFileId,
                                                     @Valid @RequestBody RefereTribunalTravailBeRequest request,
                                                     @AuthenticationPrincipal OidcUser oidcUser,
                                                     Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RefereTribunalTravailBeResponse get(@PathVariable UUID caseFileId,
                                               @AuthenticationPrincipal OidcUser oidcUser,
                                               Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
