package fr.ailegalcase.billing;

import fr.ailegalcase.shared.OAuthProviderResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * F-255 SF-255-01 — endpoints super-admin de gestion des codes promo.
 *
 * <p>Sécurité : {@link PromoCodeService} appelle systématiquement
 * {@link fr.ailegalcase.superadmin.SuperAdminService#assertSuperAdmin}. Pas
 * d'annotation de pré-filtrage Spring Security au niveau handler — pattern
 * existant du projet : la vérification est centralisée dans le service.
 */
@RestController
@RequestMapping("/api/v1/super-admin/promo-codes")
public class PromoCodeAdminController {

    private final PromoCodeService promoCodeService;

    public PromoCodeAdminController(PromoCodeService promoCodeService) {
        this.promoCodeService = promoCodeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromoCodeDto create(@Valid @RequestBody PromoCodeCreateRequest request,
                               @AuthenticationPrincipal OidcUser oidcUser,
                               Principal principal) {
        return promoCodeService.createCode(request, oidcUser,
                OAuthProviderResolver.resolve(principal));
    }

    @GetMapping
    public List<PromoCodeDto> list(@AuthenticationPrincipal OidcUser oidcUser,
                                   Principal principal) {
        return promoCodeService.listCodes(oidcUser, OAuthProviderResolver.resolve(principal));
    }

    @PostMapping("/{id}/deactivate")
    public PromoCodeDto deactivate(@PathVariable UUID id,
                                   @AuthenticationPrincipal OidcUser oidcUser,
                                   Principal principal) {
        return promoCodeService.deactivateCode(id, oidcUser,
                OAuthProviderResolver.resolve(principal));
    }
}
