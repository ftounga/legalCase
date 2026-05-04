package fr.ailegalcase.superadmin;

import fr.ailegalcase.shared.OAuthProviderResolver;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * F-187 SF-187-01 — Controller endpoint super-admin Export traction commerciale (PDF).
 *
 * <p>Endpoint unique : {@code POST /api/v1/super-admin/traction-onepager/pdf}.
 *
 * <p>Le gate super-admin est appliqué via
 * {@link SuperAdminService#getMetrics(OidcUser, String)} qui jette un 403
 * {@link org.springframework.web.server.ResponseStatusException} si
 * {@code is_super_admin = false} et un 404 si l'utilisateur OIDC est inconnu
 * en base. Le 401 est délégué à Spring Security.
 *
 * <p>La validation du body est appliquée via {@code @Valid} et délègue au
 * {@link fr.ailegalcase.shared.GlobalExceptionHandler} pour le 400.
 */
@RestController
@RequestMapping("/api/v1/super-admin")
public class TractionOnePagerController {

    private static final Logger log = LoggerFactory.getLogger(TractionOnePagerController.class);

    private final TractionOnePagerService tractionOnePagerService;
    private final SuperAdminService superAdminService;

    public TractionOnePagerController(TractionOnePagerService tractionOnePagerService,
                                      SuperAdminService superAdminService) {
        this.tractionOnePagerService = tractionOnePagerService;
        this.superAdminService = superAdminService;
    }

    @PostMapping(value = "/traction-onepager/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportTractionOnePagerPdf(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal,
            @Valid @RequestBody TractionOnePagerInput input) {

        String provider = OAuthProviderResolver.resolve(principal);
        // Gate super-admin + lecture KPIs en un seul appel — getMetrics() fait l'assertion.
        SuperAdminMetricsResponse metrics = superAdminService.getMetrics(oidcUser, provider);

        byte[] pdf = tractionOnePagerService.generate(input, metrics);
        String date = tractionOnePagerService.today();
        String filename = "legalcase-traction-" + date + ".pdf";

        log.info("Generated traction one-pager PDF (size={} bytes, date={})", pdf.length, date);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .cacheControl(CacheControl.noStore())
                .body(pdf);
    }
}
