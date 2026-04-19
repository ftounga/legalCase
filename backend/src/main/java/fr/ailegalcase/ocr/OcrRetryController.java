package fr.ailegalcase.ocr;

import fr.ailegalcase.shared.OAuthProviderResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * SF-122-05 : endpoints du bouton "Relancer avec OCR" au niveau dossier.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}")
public class OcrRetryController {

    private final OcrRetryService ocrRetryService;

    public OcrRetryController(OcrRetryService ocrRetryService) {
        this.ocrRetryService = ocrRetryService;
    }

    @GetMapping("/ocr-retry-preview")
    public OcrRetryPreviewResponse preview(@PathVariable UUID caseFileId,
                                            @AuthenticationPrincipal OidcUser oidcUser,
                                            Principal principal) {
        return ocrRetryService.preview(caseFileId, oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    @PostMapping("/ocr-retry")
    public ResponseEntity<Map<String, Integer>> retry(@PathVariable UUID caseFileId,
                                                       @AuthenticationPrincipal OidcUser oidcUser,
                                                       Principal principal) {
        int count = ocrRetryService.retry(caseFileId, oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
        return ResponseEntity.ok(Map.of("retryedCount", count));
    }
}
