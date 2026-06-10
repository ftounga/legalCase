package fr.ailegalcase.stylelearning;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.storage.StorageService;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * F-98 / SF-98-46 — commandes du corpus de style : téléversement (validation +
 * stockage S3 temporaire + publication RabbitMQ), liste, activation/désactivation,
 * suppression. Toutes les opérations sont isolées au workspace de l'utilisateur
 * (404 sinon — pas de fuite d'existence).
 */
@Service
public class StyleCorpusCommandService {

    private static final Logger log = LoggerFactory.getLogger(StyleCorpusCommandService.class);

    /** Taille maximale d'un document de corpus — 50 Mo. */
    static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024;

    /** Content types acceptés — pdf / doc / docx / txt. */
    static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain");

    /** Extensions acceptées — secours quand le content type fourni est générique. */
    static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx", "txt");

    private final StyleCorpusRepository styleCorpusRepository;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final StorageService storageService;
    private final RabbitTemplate rabbitTemplate;

    public StyleCorpusCommandService(StyleCorpusRepository styleCorpusRepository,
                                     CurrentUserResolver currentUserResolver,
                                     WorkspaceMemberRepository workspaceMemberRepository,
                                     StorageService storageService,
                                     RabbitTemplate rabbitTemplate) {
        this.styleCorpusRepository = styleCorpusRepository;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.storageService = storageService;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Téléverse un document de corpus de style : valide le type / la taille, crée
     * une ligne {@code PENDING} {@code active=true}, stocke le fichier en S3
     * (temporaire) et publie le message RabbitMQ d'extraction.
     *
     * @return la réponse {@code 202} {@code {"id":UUID,"status":"PENDING"}}
     * @throws ResponseStatusException {@code 400} fichier absent / type non supporté
     *                                 / taille > 50 Mo ; {@code 404} workspace de
     *                                 l'URL ≠ workspace de l'utilisateur
     */
    @Transactional
    public StyleCorpusUploadResponse upload(UUID workspaceId, MultipartFile file, OidcUser oidcUser,
                                            String provider, Principal principal) {
        Workspace workspace = resolveWorkspace(workspaceId, oidcUser, provider, principal);
        User user = currentUserResolver.resolve(oidcUser, provider, principal);

        // Garde 400 — fichier requis.
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier est requis.");
        }

        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();

        // Garde 400 — type de fichier supporté (content type OU extension).
        if (!isSupportedFile(contentType, filename)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Type de fichier non supporté — formats acceptés : pdf, doc, docx, txt.");
        }

        // Garde 400 — taille ≤ 50 Mo.
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Fichier trop volumineux — taille maximale 50 Mo.");
        }

        StyleCorpusDocument document = new StyleCorpusDocument();
        document.setWorkspace(workspace);
        document.setUploadedBy(user);
        document.setOriginalFilename(filename != null ? filename : "document");
        document.setContentType(contentType != null ? contentType : "application/octet-stream");
        document.setFileSize(file.getSize());
        document.setStatus(StyleCorpusDocumentStatus.PENDING);
        document.setActive(true);
        document = styleCorpusRepository.save(document);

        UUID documentId = document.getId();
        String storageKey = buildStorageKey(workspace.getId(), documentId, filename);
        try {
            storageService.upload(storageKey, file.getInputStream(),
                    document.getContentType(), file.getSize());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Échec du stockage du fichier.");
        }

        // SF-98-59 — publier le message APRÈS le commit. Sinon le worker
        // (@RabbitListener concurrency=2) peut consommer et faire findById avant que
        // la ligne PENDING soit visible → « introuvable, message ignoré » → bloqué PENDING.
        publishAfterCommit(new StyleCorpusMessage(documentId, storageKey));
        log.info("Style corpus upload — workspace={}, document={}, key={}",
                workspace.getId(), documentId, storageKey);

        return StyleCorpusUploadResponse.pending(documentId);
    }

    /**
     * SF-98-59 — publie le message d'extraction de corpus de style sur RabbitMQ. Si une
     * transaction est active, la publication est différée à {@code afterCommit} (la ligne
     * {@code StyleCorpusDocument} PENDING doit être visible avant que le worker ne la lise) ;
     * sinon (appel hors transaction, ex. tests) elle est immédiate.
     */
    private void publishAfterCommit(StyleCorpusMessage message) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doPublish(message);
                }
            });
        } else {
            doPublish(message);
        }
    }

    private void doPublish(StyleCorpusMessage message) {
        rabbitTemplate.convertAndSend(
                StyleCorpusRabbitMQConfig.STYLE_CORPUS_EXCHANGE,
                StyleCorpusRabbitMQConfig.STYLE_CORPUS_ROUTING_KEY,
                message);
    }

    /**
     * Liste les documents du corpus de style du workspace, date décroissante.
     *
     * @throws ResponseStatusException {@code 404} si le workspace de l'URL ≠
     *                                 workspace de l'utilisateur
     */
    @Transactional(readOnly = true)
    public List<StyleCorpusDocumentSummary> list(UUID workspaceId, OidcUser oidcUser,
                                                 String provider, Principal principal) {
        Workspace workspace = resolveWorkspace(workspaceId, oidcUser, provider, principal);
        return styleCorpusRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspace.getId())
                .stream()
                .map(StyleCorpusDocumentSummary::from)
                .toList();
    }

    /**
     * Active ou désactive un document de corpus.
     *
     * @throws ResponseStatusException {@code 400} si {@code active} est absent ;
     *                                 {@code 404} si le document est inconnu ou
     *                                 appartient à un autre workspace
     */
    @Transactional
    public StyleCorpusDocumentSummary updateActive(UUID workspaceId, UUID documentId, Boolean active,
                                                   OidcUser oidcUser, String provider,
                                                   Principal principal) {
        Workspace workspace = resolveWorkspace(workspaceId, oidcUser, provider, principal);
        if (active == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le champ active est requis.");
        }
        StyleCorpusDocument document = resolveDocument(documentId, workspace);
        document.setActive(active);
        document = styleCorpusRepository.save(document);
        log.info("Style corpus document {} active={}", documentId, active);
        return StyleCorpusDocumentSummary.from(document);
    }

    /**
     * Supprime un document de corpus de style.
     *
     * @throws ResponseStatusException {@code 404} si le document est inconnu ou
     *                                 appartient à un autre workspace
     */
    @Transactional
    public void delete(UUID workspaceId, UUID documentId, OidcUser oidcUser,
                       String provider, Principal principal) {
        Workspace workspace = resolveWorkspace(workspaceId, oidcUser, provider, principal);
        StyleCorpusDocument document = resolveDocument(documentId, workspace);
        styleCorpusRepository.delete(document);
        log.info("Style corpus document {} deleted from workspace {}", documentId, workspace.getId());
    }

    /**
     * Vérifie que le {@code workspaceId} de l'URL est bien le workspace primaire de
     * l'utilisateur courant — 404 sinon (pas de fuite d'existence).
     */
    private Workspace resolveWorkspace(UUID workspaceId, OidcUser oidcUser, String provider,
                                       Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        Workspace workspace = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();
        if (!workspace.getId().equals(workspaceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found");
        }
        return workspace;
    }

    /** Charge un document en exigeant son rattachement au workspace — 404 sinon. */
    private StyleCorpusDocument resolveDocument(UUID documentId, Workspace workspace) {
        return styleCorpusRepository.findByIdAndWorkspaceId(documentId, workspace.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Style corpus document not found"));
    }

    /** Type de fichier accepté si le content type OU l'extension est dans la liste blanche. */
    static boolean isSupportedFile(String contentType, String filename) {
        if (contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return true;
        }
        return ALLOWED_EXTENSIONS.contains(extensionOf(filename));
    }

    /** Extension en minuscules d'un nom de fichier, ou chaîne vide. */
    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** Clé S3 du fichier source temporaire — purgé par le worker après extraction. */
    private static String buildStorageKey(UUID workspaceId, UUID documentId, String filename) {
        String safeName = filename != null ? filename.replaceAll("[^a-zA-Z0-9._-]", "_") : "document";
        return "style-corpus/" + workspaceId + "/" + documentId + "/" + safeName;
    }
}
