package fr.ailegalcase.document;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.PaymentRequiredCode;
import fr.ailegalcase.shared.PaymentRequiredException;
import fr.ailegalcase.storage.StorageService;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            // SF-230-01 : upload natif des pièces images. Textract accepte JPG/PNG/HEIC/WebP
            // nativement — aucune conversion PDF intermédiaire côté backend ou côté avocat.
            "image/jpeg",
            "image/png",
            "image/heic",
            "image/webp",
            // SF-231-01 : ingestion vidéo MP4/MOV (taille / durée validées dans validateFile + upload).
            "video/mp4",
            "video/quicktime"
    );

    /** SF-231-01 : content types vidéo nécessitant la validation header X-Video-Duration-Seconds. */
    private static final Set<String> VIDEO_CONTENT_TYPES = Set.of("video/mp4", "video/quicktime");

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50 MB (PDF/DOC)
    /** SF-231-01 : limite spécifique vidéo, plus haute que les documents (100 Mo / 60 s max). */
    private static final long MAX_VIDEO_FILE_SIZE = 100L * 1024 * 1024;
    /** SF-231-01 : durée maximale d'une vidéo (en secondes). Au-delà → 400 VIDEO_TOO_LONG. */
    private static final long MAX_VIDEO_DURATION_SECONDS = 60L;

    private final DocumentRepository documentRepository;
    private final DocumentExtractionRepository extractionRepository;
    private final CaseFileRepository caseFileRepository;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;
    private final PlanLimitService planLimitService;
    private final DocumentPieceRepository documentPieceRepository;
    private final fr.ailegalcase.video.VideoQuotaService videoQuotaService;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentExtractionRepository extractionRepository,
                           CaseFileRepository caseFileRepository,
                           CurrentUserResolver currentUserResolver,
                           WorkspaceMemberRepository workspaceMemberRepository,
                           StorageService storageService,
                           ApplicationEventPublisher eventPublisher,
                           PlanLimitService planLimitService,
                           DocumentPieceRepository documentPieceRepository,
                           fr.ailegalcase.video.VideoQuotaService videoQuotaService) {
        this.documentRepository = documentRepository;
        this.extractionRepository = extractionRepository;
        this.caseFileRepository = caseFileRepository;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.storageService = storageService;
        this.eventPublisher = eventPublisher;
        this.planLimitService = planLimitService;
        this.documentPieceRepository = documentPieceRepository;
        this.videoQuotaService = videoQuotaService;
    }

    private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 15;

    @Transactional(readOnly = true)
    public String downloadUrl(UUID caseFileId, UUID documentId, OidcUser oidcUser, String provider, Principal principal) {
        User user = resolveUser(oidcUser, provider, principal);
        Workspace workspace = resolveWorkspace(user);
        resolveCaseFile(caseFileId, workspace); // isolation check

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (!document.getCaseFile().getId().equals(caseFileId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }

        return storageService.presignedDownloadUrl(document.getStorageKey(), PRESIGNED_URL_EXPIRATION_MINUTES);
    }

    /** SF-127-01 (fix) : bytes PDF servis en same-origin pour PDF.js (pas de CORS S3). */
    public record DocumentContent(byte[] bytes, String contentType, String filename) {}

    @Transactional(readOnly = true)
    public DocumentContent content(UUID caseFileId, UUID documentId, OidcUser oidcUser, String provider, Principal principal) {
        User user = resolveUser(oidcUser, provider, principal);
        Workspace workspace = resolveWorkspace(user);
        resolveCaseFile(caseFileId, workspace);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (!document.getCaseFile().getId().equals(caseFileId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }

        byte[] bytes = storageService.download(document.getStorageKey());
        return new DocumentContent(bytes, document.getContentType(), document.getOriginalFilename());
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(UUID caseFileId, OidcUser oidcUser, String provider, Principal principal) {
        User user = resolveUser(oidcUser, provider, principal);
        Workspace workspace = resolveWorkspace(user);
        CaseFile caseFile = resolveCaseFile(caseFileId, workspace);
        List<Document> documents = documentRepository.findByCaseFileOrderByCreatedAtDesc(caseFile);
        // SF-121-01 : charge les extractions en batch pour éviter N+1 lors du mapping
        List<UUID> docIds = documents.stream().map(Document::getId).toList();
        Map<UUID, DocumentExtraction> extractionsByDocId = extractionRepository
                .findByDocumentIdIn(docIds)
                .stream()
                .collect(Collectors.toMap(e -> e.getDocument().getId(), e -> e, (a, b) -> a));
        return documents.stream()
                .map(doc -> toResponse(doc, extractionsByDocId.get(doc.getId())))
                .toList();
    }

    /** Surcharge rétrocompat — garde les callers existants (tests, autres services) qui ne passent pas le flag. */
    @Transactional
    public DocumentResponse upload(UUID caseFileId, MultipartFile file, OidcUser oidcUser, String provider, Principal principal) {
        return upload(caseFileId, file, false, true, null, oidcUser, provider, principal);
    }

    /** Surcharge rétrocompat SF-122-03 : ocrEnabled défaut true. */
    @Transactional
    public DocumentResponse upload(UUID caseFileId, MultipartFile file, boolean ocrFormsMode,
                                    OidcUser oidcUser, String provider, Principal principal) {
        return upload(caseFileId, file, ocrFormsMode, true, null, oidcUser, provider, principal);
    }

    /** Surcharge rétrocompat SF-122-07 : pas de durée vidéo. */
    @Transactional
    public DocumentResponse upload(UUID caseFileId, MultipartFile file, boolean ocrFormsMode,
                                    boolean ocrEnabled,
                                    OidcUser oidcUser, String provider, Principal principal) {
        return upload(caseFileId, file, ocrFormsMode, ocrEnabled, null, oidcUser, provider, principal);
    }

    /**
     * SF-231-01 : variante incluant {@code videoDurationSeconds} (header
     * {@code X-Video-Duration-Seconds}). Obligatoire pour les contentTypes vidéo,
     * ignoré sinon. La validation rejette les vidéos > 60 s ou > 100 Mo.
     */
    @Transactional
    public DocumentResponse upload(UUID caseFileId, MultipartFile file, boolean ocrFormsMode,
                                    boolean ocrEnabled,
                                    Long videoDurationSeconds,
                                    OidcUser oidcUser, String provider, Principal principal) {
        validateFile(file, videoDurationSeconds);

        User user = resolveUser(oidcUser, provider, principal);
        Workspace workspace = resolveWorkspace(user);
        CaseFile caseFile = resolveCaseFile(caseFileId, workspace);

        long docCount = documentRepository.countByCaseFileId(caseFileId);
        int maxDocs = planLimitService.getMaxDocumentsPerCaseFileForWorkspace(workspace.getId());
        if (docCount >= maxDocs) {
            throw new PaymentRequiredException(PaymentRequiredCode.DOCUMENT_LIMIT_EXCEEDED,
                    "Limite de documents atteinte pour votre plan.");
        }

        // SF-231-01 : hook quota vidéo (no-op tant que SF-231-03 n'est pas livrée).
        if (isVideoContentType(file.getContentType())) {
            videoQuotaService.checkVideoQuotaForUpload(workspace.getId(),
                    videoDurationSeconds == null ? 0L : videoDurationSeconds);
        }

        String storageKey = buildStorageKey(workspace.getId(), caseFileId, file.getOriginalFilename());

        try {
            storageService.upload(storageKey, file.getInputStream(),
                    file.getContentType(), file.getSize());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed");
        }

        Document document = new Document();
        document.setCaseFile(caseFile);
        document.setUploadedBy(user);
        document.setOriginalFilename(file.getOriginalFilename());
        document.setContentType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setStorageKey(storageKey);
        document.setOcrFormsMode(ocrFormsMode); // SF-122-03
        document.setOcrEnabled(ocrEnabled);     // SF-122-07
        documentRepository.save(document);
        eventPublisher.publishEvent(new DocumentUploadedEvent(document.getId(), storageKey, file.getContentType()));

        return toResponse(document);
    }

    private void validateFile(MultipartFile file, Long videoDurationSeconds) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported file type. Allowed: PDF, DOC, DOCX, TXT, JPG, PNG, HEIC, WebP, MP4, MOV");
        }

        // SF-231-01 : vidéos ont leur propre limite de taille (100 Mo) et exigent
        // le header X-Video-Duration-Seconds.
        if (isVideoContentType(contentType)) {
            if (file.getSize() > MAX_VIDEO_FILE_SIZE) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                        "Video exceeds maximum size of 100 MB");
            }
            if (videoDurationSeconds == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Header X-Video-Duration-Seconds is required for video uploads");
            }
            if (videoDurationSeconds <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid X-Video-Duration-Seconds value");
            }
            if (videoDurationSeconds > MAX_VIDEO_DURATION_SECONDS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "VIDEO_TOO_LONG: video exceeds maximum duration of "
                                + MAX_VIDEO_DURATION_SECONDS + " seconds");
            }
        } else {
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File exceeds maximum size of 50 MB");
            }
        }
    }

    private static boolean isVideoContentType(String contentType) {
        return contentType != null && VIDEO_CONTENT_TYPES.contains(contentType);
    }

    private User resolveUser(OidcUser oidcUser, String provider, Principal principal) {
        return currentUserResolver.resolve(oidcUser, provider, principal);
    }

    private Workspace resolveWorkspace(User user) {
        return workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();
    }

    private CaseFile resolveCaseFile(UUID caseFileId, Workspace workspace) {
        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        return caseFile;
    }

    private String buildStorageKey(UUID workspaceId, UUID caseFileId, String originalFilename) {
        String sanitized = originalFilename != null
                ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_")
                : "file";
        return "%s/%s/%s/%s".formatted(workspaceId, caseFileId, UUID.randomUUID(), sanitized);
    }

    private DocumentResponse toResponse(Document document) {
        return toResponse(document, null);
    }

    private DocumentResponse toResponse(Document document, DocumentExtraction extraction) {
        java.util.List<DocumentPieceSummary> pieces = documentPieceRepository
                .findByDocument_IdOrderByOrderIndexAsc(document.getId())
                .stream()
                .map(DocumentPieceSummary::from)
                .toList();
        return new DocumentResponse(
                document.getId(),
                document.getCaseFile().getId(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getFileSize(),
                document.getCreatedAt(),
                extraction != null && extraction.getExtractionStatus() != null
                        ? extraction.getExtractionStatus().name() : null,
                extraction != null && extraction.getFailureReason() != null
                        ? extraction.getFailureReason().name() : null,
                extraction != null && extraction.isOcrRunning(),
                extraction != null && isOcrExtracted(extraction.getExtractionMetadata()),
                pieces
        );
    }

    /** SF-144-01 : détecte si l'extraction a été produite via Textract (chip `OCR`).
     *  Package-private pour testabilité directe. */
    static boolean isOcrExtracted(String metadata) {
        if (metadata == null) return false;
        return metadata.contains("\"extractor\":\"textract\"")
                || metadata.contains("\"extractor\":\"textract-rasterized\"");
    }
}
