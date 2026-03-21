package fr.ailegalcase.document;

import fr.ailegalcase.shared.OAuthProviderResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public List<DocumentResponse> list(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return documentService.list(caseFileId, oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<Void> download(
            @PathVariable UUID caseFileId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        String url = documentService.downloadUrl(caseFileId, documentId, oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, url)
                .build();
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(
            @PathVariable UUID caseFileId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return documentService.upload(caseFileId, file, oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }
}
