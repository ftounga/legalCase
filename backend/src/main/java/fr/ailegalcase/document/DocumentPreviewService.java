package fr.ailegalcase.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.UUID;

@Service
public class DocumentPreviewService {

    private static final Logger log = LoggerFactory.getLogger(DocumentPreviewService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DocumentRepository documentRepository;
    private final DocumentExtractionRepository extractionRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public DocumentPreviewService(DocumentRepository documentRepository,
                                  DocumentExtractionRepository extractionRepository,
                                  CaseFileRepository caseFileRepository,
                                  WorkspaceMemberRepository workspaceMemberRepository,
                                  CurrentUserResolver currentUserResolver) {
        this.documentRepository = documentRepository;
        this.extractionRepository = extractionRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional(readOnly = true)
    public DocumentPreviewResponse preview(UUID caseFileId, UUID documentId,
                                            OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);

        WorkspaceMember member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        Workspace workspace = member.getWorkspace();

        CaseFile caseFile = caseFileRepository.findById(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        if (!document.getCaseFile().getId().equals(caseFileId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }

        DocumentExtraction extraction = extractionRepository.findByDocumentId(documentId).orElse(null);

        return toResponse(document, extraction);
    }

    private DocumentPreviewResponse toResponse(Document doc, DocumentExtraction extraction) {
        ExtractionStatus status = extraction != null ? extraction.getExtractionStatus() : ExtractionStatus.PENDING;
        ExtractionFailureReason failureReason = extraction != null ? extraction.getFailureReason() : null;
        String rawText = extraction != null ? extraction.getExtractedText() : null;

        // Parse metadata JSON pour récupérer pageCount, extractor, quotaPages
        Integer pageCount = null;
        DocumentPreviewResponse.ExtractionMethod method = DocumentPreviewResponse.ExtractionMethod.NONE;
        int ocrPagesUsed = 0;
        if (extraction != null && extraction.getExtractionMetadata() != null) {
            try {
                JsonNode meta = MAPPER.readTree(extraction.getExtractionMetadata());
                if (meta.hasNonNull("pageCount")) pageCount = meta.get("pageCount").asInt();
                if (meta.hasNonNull("extractor")) {
                    String ex = meta.get("extractor").asText();
                    method = "TEXTRACT".equalsIgnoreCase(ex) || ex.toLowerCase().contains("ocr")
                            ? DocumentPreviewResponse.ExtractionMethod.OCR
                            : DocumentPreviewResponse.ExtractionMethod.CLASSIC;
                }
                if (meta.hasNonNull("quotaPages")) ocrPagesUsed = meta.get("quotaPages").asInt();
            } catch (Exception e) {
                log.debug("Cannot parse extraction metadata for doc {}: {}", doc.getId(), e.getMessage());
            }
        }
        // Fallback : si statut DONE mais pas de metadata, suppose CLASSIC
        if (status == ExtractionStatus.DONE && method == DocumentPreviewResponse.ExtractionMethod.NONE) {
            method = DocumentPreviewResponse.ExtractionMethod.CLASSIC;
        }

        String truncatedText = null;
        boolean truncated = false;
        int charCount = 0;
        if (status == ExtractionStatus.DONE && rawText != null) {
            charCount = rawText.length();
            if (rawText.length() > DocumentPreviewResponse.TEXT_TRUNCATE_LIMIT) {
                truncatedText = rawText.substring(0, DocumentPreviewResponse.TEXT_TRUNCATE_LIMIT);
                truncated = true;
            } else {
                truncatedText = rawText;
            }
        }

        return new DocumentPreviewResponse(
                doc.getOriginalFilename(),
                doc.getContentType(),
                doc.getFileSize(),
                pageCount,
                doc.getCreatedAt(),
                status,
                method,
                truncatedText,
                charCount,
                truncated,
                ocrPagesUsed,
                failureReason,
                extraction != null ? extraction.getTextEditedAt() : null
        );
    }
}
