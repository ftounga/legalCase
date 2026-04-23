package fr.ailegalcase.document;

import fr.ailegalcase.shared.OAuthProviderResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentDeleteService documentDeleteService;
    private final DocumentPreviewService documentPreviewService;
    private final DocumentPieceUpdateService pieceUpdateService;

    public DocumentController(DocumentService documentService,
                              DocumentDeleteService documentDeleteService,
                              DocumentPreviewService documentPreviewService,
                              DocumentPieceUpdateService pieceUpdateService) {
        this.documentService = documentService;
        this.documentDeleteService = documentDeleteService;
        this.documentPreviewService = documentPreviewService;
        this.pieceUpdateService = pieceUpdateService;
    }

    @GetMapping
    public List<DocumentResponse> list(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return documentService.list(caseFileId, oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    @GetMapping("/{documentId}/preview")
    public DocumentPreviewResponse preview(
            @PathVariable UUID caseFileId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return documentPreviewService.preview(caseFileId, documentId, oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
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

    /**
     * SF-127-01 (fix) : stream binaire du PDF via le backend plutôt qu'une redirection
     * 302 vers S3. PDF.js côté frontend ne peut pas suivre la redirection cross-origin
     * avec credentials vers S3 (CORS bloqué sur les URLs présignées). Ce endpoint
     * retourne directement les bytes en same-origin.
     */
    @GetMapping("/{documentId}/content")
    public ResponseEntity<byte[]> content(
            @PathVariable UUID caseFileId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        DocumentService.DocumentContent content = documentService.content(caseFileId, documentId, oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + content.filename().replace("\"", "") + "\"")
                .body(content.bytes());
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID caseFileId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        documentDeleteService.delete(caseFileId, documentId, oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    /**
     * SF-145-11 : permet à l'avocat de reclassifier manuellement une pièce
     * quand Sonnet a produit un type ou un label incorrect (ex. un bail
     * classé AUTRE, un acte de mariage classé ACTE_NAISSANCE).
     */
    @PutMapping("/{documentId}/pieces/{pieceId}")
    public DocumentPieceSummary updatePiece(
            @PathVariable UUID caseFileId,
            @PathVariable UUID documentId,
            @PathVariable UUID pieceId,
            @Valid @RequestBody UpdatePieceRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return pieceUpdateService.update(caseFileId, documentId, pieceId,
                request.type(), request.label(),
                oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(
            @PathVariable UUID caseFileId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "ocrFormsMode", required = false, defaultValue = "false") boolean ocrFormsMode,
            @RequestParam(value = "ocrEnabled", required = false, defaultValue = "true") boolean ocrEnabled,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return documentService.upload(caseFileId, file, ocrFormsMode, ocrEnabled, oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }
}
