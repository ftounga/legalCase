package fr.ailegalcase.analysis;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class StrategicOptionController {

    private final StrategicOptionService strategicOptionService;

    public StrategicOptionController(StrategicOptionService strategicOptionService) {
        this.strategicOptionService = strategicOptionService;
    }

    @GetMapping("/api/v1/case-files/{caseFileId}/analyses/{analysisId}/strategic-options")
    public List<StrategicOptionResponse> list(@PathVariable UUID caseFileId,
                                              @PathVariable UUID analysisId,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return strategicOptionService.list(caseFileId, analysisId, oidcUser, principal);
    }

    @PatchMapping("/api/v1/strategic-options/{optionId}")
    public StrategicOptionResponse updateStatus(@PathVariable UUID optionId,
                                                @RequestBody Map<String, String> body,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        String statut = body == null ? null : body.get("statut");
        if (statut == null || statut.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Champ 'statut' requis");
        }
        String raisonDiscard = body.get("raisonDiscard");
        return strategicOptionService.updateStatus(optionId, statut, raisonDiscard, oidcUser, principal);
    }
}
