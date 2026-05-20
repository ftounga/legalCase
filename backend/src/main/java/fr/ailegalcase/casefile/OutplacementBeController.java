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
 * SF-207-08 : endpoints REST de l'outil outplacement obligatoire 45+ ans BE.
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (l'outil n'existe pas côté FR ; aucun équivalent direct de
 * l'outplacement CCT 82 en droit français).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/outplacement-be-obligatoire-45")
public class OutplacementBeController {

    private final OutplacementBeService service;

    public OutplacementBeController(OutplacementBeService service) {
        this.service = service;
    }

    @PostMapping
    public OutplacementBeResponse calculate(@PathVariable UUID caseFileId,
                                            @Valid @RequestBody OutplacementBeRequest request,
                                            @AuthenticationPrincipal OidcUser oidcUser,
                                            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public OutplacementBeResponse get(@PathVariable UUID caseFileId,
                                      @AuthenticationPrincipal OidcUser oidcUser,
                                      Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
