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
 * SF-219-29 : endpoints REST de l'outil <i>Rente AT/MP vs
 * capitalisation BE</i> (Loi du 10/04/1971 art. 24 ; Lois coordonnees
 * du 03/06/1970 art. 35 ; AR du 21/12/1971 + AR 24/02/2005 - bareme).
 *
 * <ul>
 *   <li>POST - creer / mettre a jour l'analyse pour ce dossier
 *       (upsert unicite sur {@code case_file_id}).</li>
 *   <li>GET - recuperer la derniere analyse persistee (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE cote service - 404 pour preserver l'isolation
 * BE-only. Le regime francais des rentes AT/MP CPAM (art. L. 434-1 et
 * s. CSS, seuils 10 et 50 pour cent au lieu de 19, voies de recours
 * TASS / Pole social TJ) est distinct dans son bareme et sa procedure.
 * Restitution separee par outil.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/at-mp-rente-capital-be")
public class AtMpRenteCapitalBeController {

    private final AtMpRenteCapitalBeService service;

    public AtMpRenteCapitalBeController(
            AtMpRenteCapitalBeService service) {
        this.service = service;
    }

    @PostMapping
    public AtMpRenteCapitalBeResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody AtMpRenteCapitalBeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AtMpRenteCapitalBeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
