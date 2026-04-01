package fr.ailegalcase.analysis;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class ProcedureCheckController {

    private final ProcedureCheckService procedureCheckService;

    public ProcedureCheckController(ProcedureCheckService procedureCheckService) {
        this.procedureCheckService = procedureCheckService;
    }

    @GetMapping("/api/v1/case-files/{caseFileId}/analyses/{analysisId}/procedure-checks")
    public List<ProcedureCheckResponse> list(@PathVariable UUID caseFileId,
                                             @PathVariable UUID analysisId,
                                             @AuthenticationPrincipal OidcUser oidcUser,
                                             Principal principal) {
        return procedureCheckService.list(caseFileId, analysisId, oidcUser, principal);
    }

    @PatchMapping("/api/v1/procedure-checks/{checkId}")
    public ProcedureCheckResponse updateStatus(@PathVariable UUID checkId,
                                               @RequestBody Map<String, String> body,
                                               @AuthenticationPrincipal OidcUser oidcUser,
                                               Principal principal) {
        String statut = body.get("statut");
        if (statut == null || statut.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Champ 'statut' requis");
        }
        return procedureCheckService.updateStatus(checkId, statut, oidcUser, principal);
    }
}
