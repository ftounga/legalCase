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
 * SF-219-24 : endpoints REST de l'outil <i>Code pénal social BE —
 * qualification d'infraction + niveau de sanction</i> (Loi du
 * 06/06/2010 introduisant le Code pénal social).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only. Le régime français de répression du travail illégal (Code
 * du travail art. L. 8221-1 et s., Code pénal art. 121-2 personne
 * morale × 5) est distinct dans ses quanta et sa procédure (juges
 * du parquet, pas auditorat ; pas de « règle Una Via » strictement
 * comparable). Restitution séparée par outil.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/code-penal-social-be")
public class CodePenalSocialBeController {

    private final CodePenalSocialBeService service;

    public CodePenalSocialBeController(
            CodePenalSocialBeService service) {
        this.service = service;
    }

    @PostMapping
    public CodePenalSocialBeResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody CodePenalSocialBeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public CodePenalSocialBeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
