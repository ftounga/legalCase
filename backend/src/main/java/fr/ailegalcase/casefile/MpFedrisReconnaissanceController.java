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
 * SF-219-28 : endpoints REST de l'outil <i>Reconnaissance d'une maladie
 * professionnelle Fedris BE</i> (Lois coordonnees du 03/06/1970 + AR
 * du 28/03/1969 liste fermee modifie 06/12/2018 + AR du 16/12/1985
 * systeme ouvert + Loi du 11/01/2018 reformant Fedris).
 *
 * <ul>
 *   <li>POST — creer / mettre a jour l'analyse pour ce dossier
 *       (upsert unicite sur {@code case_file_id}).</li>
 *   <li>GET — recuperer la derniere analyse persistee (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE cote service — 404 pour preserver l'isolation
 * BE-only. Le regime francais de reconnaissance MP (CPAM, tableaux
 * art. L. 461-2 CSS, CRRMP art. L. 461-1, delai prescription 2 ans
 * art. L. 461-5 CSS) est distinct dans ses tableaux et sa procedure.
 * Restitution separee par outil.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/mp-fedris-reconnaissance")
public class MpFedrisReconnaissanceController {

    private final MpFedrisReconnaissanceService service;

    public MpFedrisReconnaissanceController(
            MpFedrisReconnaissanceService service) {
        this.service = service;
    }

    @PostMapping
    public MpFedrisReconnaissanceResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody MpFedrisReconnaissanceRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public MpFedrisReconnaissanceResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
