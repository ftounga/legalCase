package fr.ailegalcase.legal;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.consent.ConsentAcceptanceService;
import fr.ailegalcase.consent.dto.ConsentAcceptanceRequest;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.List;

/**
 * F-240 SF-240-04 : endpoint public servant le Data Processing Agreement (DPA) RGPD art. 28.
 *
 * <p>Le fichier PDF est chargé depuis le classpath ({@code static/legal/legalcase-dpa-v1.pdf}).
 * L'endpoint est whitelisté dans {@link fr.ailegalcase.auth.SecurityConfig} — accessible
 * sans authentification pour permettre aux prospects de le télécharger avant tout compte.</p>
 *
 * <p>Si l'utilisateur est authentifié, un enregistrement {@code DPA_DOWNLOAD} est créé
 * via {@link ConsentAcceptanceService} en mode "best-effort" : une exception de tracking
 * ne bloque jamais le téléchargement.</p>
 */
@RestController
@RequestMapping("/api/v1/legal")
public class LegalDocumentsController {

    private static final Logger log = LoggerFactory.getLogger(LegalDocumentsController.class);

    static final String DPA_CLASSPATH = "static/legal/legalcase-dpa-v1.pdf";
    static final String DPA_FILENAME = "legalcase-dpa-v1.pdf";
    static final String DPA_VERSION = "2026-05-11";

    private final ConsentAcceptanceService consentAcceptanceService;
    private final CurrentUserResolver currentUserResolver;

    public LegalDocumentsController(ConsentAcceptanceService consentAcceptanceService,
                                    CurrentUserResolver currentUserResolver) {
        this.consentAcceptanceService = consentAcceptanceService;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * Sert le DPA en PDF.
     *
     * <ul>
     *   <li>Public : pas d'authentification requise (whitelist Spring Security).</li>
     *   <li>Si utilisateur authentifié : enregistrement {@code DPA_DOWNLOAD} best-effort.</li>
     *   <li>Content-Disposition attachment pour déclencher le téléchargement côté navigateur.</li>
     * </ul>
     */
    @GetMapping("/dpa")
    public ResponseEntity<byte[]> downloadDpa(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal,
            HttpServletRequest httpRequest) {

        byte[] pdfBytes = loadPdf();

        trackDownloadIfAuthenticated(oidcUser, principal, httpRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(DPA_FILENAME).build());
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private byte[] loadPdf() {
        ClassPathResource resource = new ClassPathResource(DPA_CLASSPATH);
        if (!resource.exists()) {
            log.error("DPA PDF not found on classpath: {}", DPA_CLASSPATH);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "DPA document unavailable");
        }
        try (InputStream in = resource.getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            log.error("Failed to read DPA PDF from classpath: {}", DPA_CLASSPATH, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "DPA document unavailable");
        }
    }

    /**
     * Enregistre le téléchargement si l'utilisateur est authentifié.
     * Erreur silencieuse (best-effort) : le tracking ne doit jamais bloquer la mise à
     * disposition légale du document.
     */
    private void trackDownloadIfAuthenticated(OidcUser oidcUser,
                                               Principal principal,
                                               HttpServletRequest httpRequest) {
        if (oidcUser == null && principal == null) {
            log.debug("Anonymous DPA download from IP={}",
                    httpRequest.getRemoteAddr());
            return;
        }
        try {
            String provider = OAuthProviderResolver.resolve(principal);
            User user = currentUserResolver.resolve(oidcUser, provider, principal);
            ConsentAcceptanceRequest request = new ConsentAcceptanceRequest(
                    List.of("DPA_DOWNLOAD"), DPA_VERSION);
            consentAcceptanceService.accept(request, user, httpRequest);
            log.debug("DPA_DOWNLOAD tracked for userId={}", user.getId());
        } catch (Exception e) {
            log.warn("DPA_DOWNLOAD tracking failed (best-effort, download not blocked): {}",
                    e.getMessage());
        }
    }
}
