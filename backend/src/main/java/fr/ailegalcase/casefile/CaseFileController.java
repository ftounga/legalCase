package fr.ailegalcase.casefile;

import fr.ailegalcase.shared.OAuthProviderResolver;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/case-files")
public class CaseFileController {

    private final CaseFileService caseFileService;

    public CaseFileController(CaseFileService caseFileService) {
        this.caseFileService = caseFileService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaseFileResponse create(@Valid @RequestBody CaseFileRequest request,
                                   @AuthenticationPrincipal OidcUser oidcUser,
                                   Principal principal) {
        return caseFileService.create(request, oidcUser, OAuthProviderResolver.resolve(principal));
    }

    @GetMapping
    public Page<CaseFileResponse> list(@AuthenticationPrincipal OidcUser oidcUser,
                                       Principal principal,
                                       @PageableDefault(size = 20) Pageable pageable) {
        return caseFileService.list(oidcUser, OAuthProviderResolver.resolve(principal), pageable);
    }
}
