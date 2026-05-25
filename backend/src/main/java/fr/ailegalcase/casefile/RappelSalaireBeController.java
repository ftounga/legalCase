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
 * SF-213-02 : endpoints REST de l'outil de calcul de rappel de salaire en
 * droit belge du travail (Loi 12/04/1965 art. 10 — taux moratoires 10 % ;
 * Loi 03/07/1978 art. 15 — prescription).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (l'équivalent FR rappel-salaire suit un régime juridique distinct :
 * intérêts légaux variables semestriels art. 1153 C.civ. + prescription 3 ans
 * C. trav. art. L. 3245-1).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/rappel-salaire-be")
public class RappelSalaireBeController {

    private final RappelSalaireBeService service;

    public RappelSalaireBeController(RappelSalaireBeService service) {
        this.service = service;
    }

    @PostMapping
    public RappelSalaireBeResponse calculate(@PathVariable UUID caseFileId,
                                             @Valid @RequestBody RappelSalaireBeRequest request,
                                             @AuthenticationPrincipal OidcUser oidcUser,
                                             Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RappelSalaireBeResponse get(@PathVariable UUID caseFileId,
                                       @AuthenticationPrincipal OidcUser oidcUser,
                                       Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
