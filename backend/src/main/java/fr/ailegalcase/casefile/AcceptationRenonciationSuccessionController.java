package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/acceptation-renonciation-succession")
public class AcceptationRenonciationSuccessionController {

    private final AcceptationRenonciationSuccessionService service;

    public AcceptationRenonciationSuccessionController(AcceptationRenonciationSuccessionService service) {
        this.service = service;
    }

    @PostMapping
    public AcceptationRenonciationSuccessionResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody AcceptationRenonciationSuccessionRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AcceptationRenonciationSuccessionResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
