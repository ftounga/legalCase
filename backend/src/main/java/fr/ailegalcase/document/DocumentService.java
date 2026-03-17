package fr.ailegalcase.document;

import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.storage.StorageService;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50 MB

    private final DocumentRepository documentRepository;
    private final CaseFileRepository caseFileRepository;
    private final AuthAccountRepository authAccountRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final StorageService storageService;

    public DocumentService(DocumentRepository documentRepository,
                           CaseFileRepository caseFileRepository,
                           AuthAccountRepository authAccountRepository,
                           WorkspaceMemberRepository workspaceMemberRepository,
                           StorageService storageService) {
        this.documentRepository = documentRepository;
        this.caseFileRepository = caseFileRepository;
        this.authAccountRepository = authAccountRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.storageService = storageService;
    }

    private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 15;

    @Transactional(readOnly = true)
    public String downloadUrl(UUID caseFileId, UUID documentId, OidcUser oidcUser, String provider) {
        User user = resolveUser(oidcUser, provider);
        Workspace workspace = resolveWorkspace(user);
        resolveCaseFile(caseFileId, workspace); // isolation check

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (!document.getCaseFile().getId().equals(caseFileId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }

        return storageService.presignedDownloadUrl(document.getStorageKey(), PRESIGNED_URL_EXPIRATION_MINUTES);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(UUID caseFileId, OidcUser oidcUser, String provider) {
        User user = resolveUser(oidcUser, provider);
        Workspace workspace = resolveWorkspace(user);
        CaseFile caseFile = resolveCaseFile(caseFileId, workspace);
        return documentRepository.findByCaseFileOrderByCreatedAtDesc(caseFile)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public DocumentResponse upload(UUID caseFileId, MultipartFile file, OidcUser oidcUser, String provider) {
        validateFile(file);

        User user = resolveUser(oidcUser, provider);
        Workspace workspace = resolveWorkspace(user);
        CaseFile caseFile = resolveCaseFile(caseFileId, workspace);

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
        documentRepository.save(document);

        return toResponse(document);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File exceeds maximum size of 50 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported file type. Allowed: PDF, DOC, DOCX, TXT");
        }
    }

    private User resolveUser(OidcUser oidcUser, String provider) {
        return authAccountRepository
                .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getUser();
    }

    private Workspace resolveWorkspace(User user) {
        return workspaceMemberRepository
                .findFirstByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();
    }

    private CaseFile resolveCaseFile(UUID caseFileId, Workspace workspace) {
        CaseFile caseFile = caseFileRepository.findById(caseFileId)
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
        return new DocumentResponse(
                document.getId(),
                document.getCaseFile().getId(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getFileSize(),
                document.getCreatedAt()
        );
    }
}
