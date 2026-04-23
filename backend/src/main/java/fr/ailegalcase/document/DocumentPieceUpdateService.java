package fr.ailegalcase.document;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;

/**
 * SF-145-11 : reclassification manuelle d'une pièce identifiée (type + label)
 * par l'avocat quand la détection Sonnet s'est trompée. Vérifie l'isolation
 * workspace et le domaine métier.
 */
@Service
public class DocumentPieceUpdateService {

    private static final int MAX_LABEL_LENGTH = 500;

    private final DocumentPieceRepository pieceRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final CurrentUserResolver currentUserResolver;

    public DocumentPieceUpdateService(DocumentPieceRepository pieceRepository,
                                      CaseFileRepository caseFileRepository,
                                      WorkspaceMemberRepository memberRepository,
                                      CurrentUserResolver currentUserResolver) {
        this.pieceRepository = pieceRepository;
        this.caseFileRepository = caseFileRepository;
        this.memberRepository = memberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * Met à jour le type et/ou le label d'une pièce. Vérifie :
     * <ul>
     *   <li>pièce existe + appartient bien au document fourni</li>
     *   <li>document appartient bien au case file du workspace de l'utilisateur</li>
     *   <li>type cible appartient à l'enum applicable au domaine du workspace</li>
     *   <li>label longueur ≤ 500 caractères</li>
     * </ul>
     *
     * @return pièce mise à jour
     */
    @Transactional
    public DocumentPieceSummary update(UUID caseFileId, UUID documentId, UUID pieceId,
                                       String typeRaw, String label,
                                       OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        Workspace workspace = memberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));

        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        DocumentPiece piece = pieceRepository.findById(pieceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Piece not found"));

        if (!piece.getDocument().getId().equals(documentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Piece not found");
        }

        if (!piece.getDocument().getCaseFile().getId().equals(caseFileId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Piece not found");
        }

        // Valide le type demandé vs enum applicable au domaine
        DocumentPieceType targetType;
        try {
            targetType = DocumentPieceType.valueOf(typeRaw == null ? "" : typeRaw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid piece type: " + typeRaw);
        }
        String legalDomain = workspace.getLegalDomain();
        Set<DocumentPieceType> allowed = DocumentPieceType.applicableFor(legalDomain);
        if (!allowed.contains(targetType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Piece type " + targetType + " is not applicable to domain " + legalDomain);
        }

        String safeLabel = label == null ? null : label.trim();
        if (safeLabel != null && safeLabel.isEmpty()) safeLabel = null;
        if (safeLabel != null && safeLabel.length() > MAX_LABEL_LENGTH) {
            safeLabel = safeLabel.substring(0, MAX_LABEL_LENGTH);
        }

        piece.setType(targetType);
        piece.setLabel(safeLabel);
        pieceRepository.save(piece);

        return DocumentPieceSummary.from(piece);
    }
}
