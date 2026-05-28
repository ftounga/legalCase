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
 * SF-219-11 : endpoints REST de l'outil <i>congé-éducation payé
 * régionalisé</i> (Loi 22/01/1985 régionalisée 2014 — WBR / FLA /
 * BXL).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (la France a un régime structurellement différent — CPF /
 * Pro-A / Plan de développement des compétences, art. L. 6111-1 et s.
 * C. trav.). L'outil unique branche en interne sur le régime régional
 * applicable (Wallonie / Flandre — VOV / Bruxelles-Capitale).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/conge-education-paye-region")
public class CongeEducationPayeRegionController {

    private final CongeEducationPayeRegionService service;

    public CongeEducationPayeRegionController(
            CongeEducationPayeRegionService service) {
        this.service = service;
    }

    @PostMapping
    public CongeEducationPayeRegionResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody CongeEducationPayeRegionRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public CongeEducationPayeRegionResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
