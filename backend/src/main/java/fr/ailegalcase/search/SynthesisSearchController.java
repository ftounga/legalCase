package fr.ailegalcase.search;

import fr.ailegalcase.shared.OAuthProviderResolver;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/search")
@Validated
public class SynthesisSearchController {

    private final SynthesisSearchService searchService;

    public SynthesisSearchController(SynthesisSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SynthesisSearchResponse search(
            @RequestParam @Size(min = 2, max = 200) String q,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        if (q == null || q.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query must not be blank");
        }
        String provider = OAuthProviderResolver.resolve(principal);
        return searchService.search(q.trim(), oidcUser, provider, principal);
    }
}
