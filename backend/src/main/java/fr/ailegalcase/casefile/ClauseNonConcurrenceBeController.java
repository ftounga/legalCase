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
 * SF-213-01 : endpoints REST de l'outil d'analyse de validité d'une clause de
 * non-concurrence en droit belge du travail (Loi 03/07/1978 art. 65 + CCT 13).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (l'équivalent FR F-DT-24 suit un régime juridique distinct :
 * montant libre, pas de plancher légal ½ rémunération).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/clause-non-concurrence-be")
public class ClauseNonConcurrenceBeController {

    private final ClauseNonConcurrenceBeService service;

    public ClauseNonConcurrenceBeController(ClauseNonConcurrenceBeService service) {
        this.service = service;
    }

    @PostMapping
    public ClauseNonConcurrenceBeResponse calculate(@PathVariable UUID caseFileId,
                                                    @Valid @RequestBody ClauseNonConcurrenceBeRequest request,
                                                    @AuthenticationPrincipal OidcUser oidcUser,
                                                    Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ClauseNonConcurrenceBeResponse get(@PathVariable UUID caseFileId,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
