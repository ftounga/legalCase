package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-220-04 : endpoints POST/GET pour l'analyse VPF au titre d'un PACS
 * L.423-23 (F-IM-50-pacs-vpf-fr). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/pacs-vpf-analysis")
public class PacsVpfController {

    private final PacsVpfService service;

    public PacsVpfController(PacsVpfService service) {
        this.service = service;
    }

    @PostMapping
    public PacsVpfResponse analyze(@PathVariable UUID caseFileId,
                                   @RequestBody PacsVpfRequest request,
                                   @AuthenticationPrincipal OidcUser oidcUser,
                                   Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public PacsVpfResponse get(@PathVariable UUID caseFileId,
                               @AuthenticationPrincipal OidcUser oidcUser,
                               Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
