package fr.ailegalcase.casefile;

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
 * SF-217-16 : endpoints REST pour l'analyse de reconnaissance d'un mariage ou
 * divorce étranger en Belgique — incluant le talaq (BELGIQUE).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/mariage-etranger-be-reconnaissance")
public class MariageEtrangerBeReconnaissanceController {

    private final MariageEtrangerBeReconnaissanceService service;

    public MariageEtrangerBeReconnaissanceController(
            MariageEtrangerBeReconnaissanceService service) {
        this.service = service;
    }

    @PostMapping
    public MariageEtrangerBeReconnaissanceResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody MariageEtrangerBeReconnaissanceRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public MariageEtrangerBeReconnaissanceResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
