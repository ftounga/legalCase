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
 * SF-213-07 : endpoints REST de l'outil d'analyse de la procédure interne
 * BE de plainte pour harcèlement moral / sexuel
 * (Loi du 04/08/1996 art. 32bis-32sexies + AR du 10/04/2014).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (la France a un dispositif distinct — référent harcèlement,
 * médecin du travail).</p>
 *
 * <p>Outil <b>complémentaire</b> de F-DT-11 (licenciement nul représailles
 * BE) — SF-213-07 intervient avant toute rupture pour guider la procédure
 * interne.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/harcelement-be-procedure-formelle")
public class HarcelementBeProcedureFormelleController {

    private final HarcelementBeProcedureFormelleService service;

    public HarcelementBeProcedureFormelleController(
            HarcelementBeProcedureFormelleService service) {
        this.service = service;
    }

    @PostMapping
    public HarcelementBeProcedureFormelleResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody HarcelementBeProcedureFormelleRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public HarcelementBeProcedureFormelleResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
