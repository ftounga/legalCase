package fr.ailegalcase.casefile;

import fr.ailegalcase.shared.OAuthProviderResolver;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files")
public class CaseFileController {

    private final CaseFileService caseFileService;
    private final CaseFileStatsService caseFileStatsService;
    private final CaseFileStatusService caseFileStatusService;
    private final CaseFileExportService caseFileExportService;

    public CaseFileController(CaseFileService caseFileService,
                              CaseFileStatsService caseFileStatsService,
                              CaseFileStatusService caseFileStatusService,
                              CaseFileExportService caseFileExportService) {
        this.caseFileService = caseFileService;
        this.caseFileStatsService = caseFileStatsService;
        this.caseFileStatusService = caseFileStatusService;
        this.caseFileExportService = caseFileExportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaseFileResponse create(@Valid @RequestBody CaseFileRequest request,
                                   @AuthenticationPrincipal OidcUser oidcUser,
                                   Principal principal) {
        return caseFileService.create(request, oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    @GetMapping
    public Page<CaseFileResponse> list(@AuthenticationPrincipal OidcUser oidcUser,
                                       Principal principal,
                                       @PageableDefault(size = 20) Pageable pageable) {
        return caseFileService.list(oidcUser, OAuthProviderResolver.resolve(principal), pageable, principal);
    }

    @GetMapping("/{id}")
    public CaseFileResponse getById(@PathVariable java.util.UUID id,
                                    @AuthenticationPrincipal OidcUser oidcUser,
                                    Principal principal) {
        return caseFileService.getById(id, oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    @GetMapping("/{id}/stats")
    public CaseFileStatsResponse getStats(@PathVariable UUID id,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return caseFileStatsService.getStats(id, oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    @PatchMapping("/{id}")
    public CaseFileResponse update(@PathVariable UUID id,
                                   @Valid @RequestBody CaseFileUpdateRequest request,
                                   @AuthenticationPrincipal OidcUser oidcUser,
                                   Principal principal) {
        return caseFileService.update(id, request, oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    @PatchMapping("/{id}/close")
    public CaseFileResponse close(@PathVariable UUID id,
                                  @AuthenticationPrincipal OidcUser oidcUser,
                                  Principal principal) {
        return caseFileStatusService.close(id, oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    @PatchMapping("/{id}/reopen")
    public CaseFileResponse reopen(@PathVariable UUID id,
                                   @AuthenticationPrincipal OidcUser oidcUser,
                                   Principal principal) {
        return caseFileStatusService.reopen(id, oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id,
                       @AuthenticationPrincipal OidcUser oidcUser,
                       Principal principal) {
        caseFileStatusService.delete(id, oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable UUID id,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) throws IOException {
        CaseFileExportResult result = caseFileExportService.export(
                id, oidcUser, OAuthProviderResolver.resolve(principal), principal);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + result.filename() + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(result.zipBytes());
    }
}
