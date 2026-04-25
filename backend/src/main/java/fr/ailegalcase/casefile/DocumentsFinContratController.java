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
 * SF-DT-32-01 : endpoints REST pour la conformité documents de fin de contrat
 * (FRANCE — L.1234-19 / R.1234-9 / L.1234-20).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/documents-fin-contrat")
public class DocumentsFinContratController {

    private final DocumentsFinContratService service;

    public DocumentsFinContratController(DocumentsFinContratService service) {
        this.service = service;
    }

    @PostMapping
    public DocumentsFinContratResponse calculate(@PathVariable UUID caseFileId,
                                                 @RequestBody DocumentsFinContratRequest request,
                                                 @AuthenticationPrincipal OidcUser oidcUser,
                                                 Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public DocumentsFinContratResponse get(@PathVariable UUID caseFileId,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
