package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-215-19 : endpoints REST de l'outil "Protection temporaire Ukraine BE"
 * (F-IM-34 — directive 2001/55/CE, décision UE 2022/382, Loi 15/12/1980 art. 57/29+).
 * Outil BELGIQUE UNIQUEMENT (droit des étrangers).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/protection-temporaire-ukraine-be-analysis")
public class ProtectionTemporaireUkraineBeController {

    private final ProtectionTemporaireUkraineBeService service;

    public ProtectionTemporaireUkraineBeController(ProtectionTemporaireUkraineBeService service) {
        this.service = service;
    }

    @PostMapping
    public ProtectionTemporaireUkraineBeResponse calculate(@PathVariable UUID caseFileId,
                                                          @RequestBody ProtectionTemporaireUkraineBeRequest request,
                                                          @AuthenticationPrincipal OidcUser oidcUser,
                                                          Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ProtectionTemporaireUkraineBeResponse get(@PathVariable UUID caseFileId,
                                                    @AuthenticationPrincipal OidcUser oidcUser,
                                                    Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
