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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * F-260 / SF-260-01 : réordonnancement explicite des pièces d'un dossier.
 *
 * <p>L'avocat envoie l'ordre cible complet des pièces ; le service réassigne le
 * {@code piece_number} persistant 1..N selon cet ordre (renumérotation voulue,
 * la seule qui touche les numéros existants). Isolation workspace stricte
 * (résolution user → workspace primaire → vérification dossier), sur le modèle de
 * {@link DocumentPieceUpdateService}.</p>
 */
@Service
public class PieceOrderService {

    private final DocumentPieceRepository pieceRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final CurrentUserResolver currentUserResolver;

    public PieceOrderService(DocumentPieceRepository pieceRepository,
                             CaseFileRepository caseFileRepository,
                             WorkspaceMemberRepository memberRepository,
                             CurrentUserResolver currentUserResolver) {
        this.pieceRepository = pieceRepository;
        this.caseFileRepository = caseFileRepository;
        this.memberRepository = memberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * Réassigne {@code piece_number} 1..N dans l'ordre fourni.
     *
     * @return les pièces du dossier dans le nouvel ordre (numéros à jour)
     * @throws ResponseStatusException 404 si dossier d'un autre workspace ou pièce
     *         introuvable ; 400 si la liste ne correspond pas exactement à
     *         l'ensemble des pièces du dossier (doublon, manque, pièce étrangère).
     */
    @Transactional
    public List<DocumentPieceSummary> reorder(UUID caseFileId, List<UUID> orderedPieceIds,
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

        if (orderedPieceIds == null || orderedPieceIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderedPieceIds is required");
        }

        List<DocumentPiece> existing = pieceRepository.findByCaseFileIdOrderByPieceNumber(caseFileId);
        Set<UUID> existingIds = existing.stream().map(DocumentPiece::getId).collect(Collectors.toSet());

        // La liste fournie doit correspondre EXACTEMENT à l'ensemble des pièces du
        // dossier : pas de doublon, pas de manque, pas de pièce étrangère.
        Set<UUID> requestedUnique = new LinkedHashSet<>(orderedPieceIds);
        if (requestedUnique.size() != orderedPieceIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "orderedPieceIds contains duplicate ids");
        }
        if (!requestedUnique.equals(new HashSet<>(existingIds))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "orderedPieceIds must match exactly the pieces of the case file");
        }

        Map<UUID, DocumentPiece> byId = existing.stream()
                .collect(Collectors.toMap(DocumentPiece::getId, Function.identity()));

        int number = 1;
        List<DocumentPiece> reordered = new ArrayList<>(orderedPieceIds.size());
        for (UUID id : orderedPieceIds) {
            DocumentPiece piece = byId.get(id);
            piece.setPieceNumber(number++);
            pieceRepository.save(piece);
            reordered.add(piece);
        }

        return reordered.stream().map(DocumentPieceSummary::from).toList();
    }
}
