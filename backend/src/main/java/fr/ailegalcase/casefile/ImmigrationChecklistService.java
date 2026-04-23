package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ImmigrationChecklistService {

    private static final List<String> VALID_STATUTS = List.of("PRESENT", "ABSENT", "INCONNU");

    private final ImmigrationPieceCheckRepository pieceCheckRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ImmigrationPieceAutoFillService autoFillService;

    public ImmigrationChecklistService(ImmigrationPieceCheckRepository pieceCheckRepository,
                                       CaseFileRepository caseFileRepository,
                                       WorkspaceMemberRepository workspaceMemberRepository,
                                       CurrentUserResolver currentUserResolver,
                                       ImmigrationPieceAutoFillService autoFillService) {
        this.pieceCheckRepository = pieceCheckRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.autoFillService = autoFillService;
    }

    @Transactional
    public ImmigrationChecklistResponse get(UUID caseFileId, String titreType, String country,
                                            OidcUser oidcUser, Principal principal) {
        validateParams(titreType, country);
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);

        // SF-IM-01-05 : auto-pré-remplit les statuts PRESENT à partir des pièces
        // détectées (F-145) si aucun check n'existe encore. Idempotent.
        autoFillService.autoFillIfEmpty(caseFile, titreType, country);

        List<String> referentielPieces = ImmigrationPieceReferentiel.getPieces(titreType, country);
        Map<String, String> existingStatuts = pieceCheckRepository
                .findByCaseFileIdAndTitreTypeAndCountry(caseFile.getId(), titreType, country)
                .stream()
                .collect(Collectors.toMap(ImmigrationPieceCheck::getLabel, ImmigrationPieceCheck::getStatut));

        List<ImmigrationChecklistResponse.PieceItem> pieces = referentielPieces.stream()
                .map(label -> new ImmigrationChecklistResponse.PieceItem(
                        label,
                        existingStatuts.getOrDefault(label, "INCONNU")))
                .toList();

        return new ImmigrationChecklistResponse(caseFile.getId(), titreType, country, pieces);
    }

    @Transactional
    public ImmigrationChecklistResponse upsert(UUID caseFileId, ImmigrationChecklistRequest request,
                                               OidcUser oidcUser, Principal principal) {
        validateParams(request.titreType(), request.country());
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);

        List<String> referentielPieces = ImmigrationPieceReferentiel.getPieces(request.titreType(), request.country());

        Map<String, ImmigrationPieceCheck> existingByLabel = pieceCheckRepository
                .findByCaseFileIdAndTitreTypeAndCountry(caseFile.getId(), request.titreType(), request.country())
                .stream()
                .collect(Collectors.toMap(ImmigrationPieceCheck::getLabel, Function.identity()));

        if (request.pieces() != null) {
            for (ImmigrationChecklistRequest.PieceItemRequest item : request.pieces()) {
                if (!referentielPieces.contains(item.label())) continue;
                if (!VALID_STATUTS.contains(item.statut())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Statut invalide : " + item.statut() + ". Valeurs autorisées : " + VALID_STATUTS);
                }
                ImmigrationPieceCheck check = existingByLabel.getOrDefault(item.label(), null);
                if (check == null) {
                    check = new ImmigrationPieceCheck();
                    check.setCaseFile(caseFile);
                    check.setTitreType(request.titreType());
                    check.setCountry(request.country());
                    check.setLabel(item.label());
                }
                check.setStatut(item.statut());
                pieceCheckRepository.save(check);
            }
        }

        return get(caseFileId, request.titreType(), request.country(), oidcUser, principal);
    }

    private void validateParams(String titreType, String country) {
        if (!ImmigrationPieceReferentiel.isTitreTypeValid(titreType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Type de titre inconnu : " + titreType);
        }
        if (!ImmigrationPieceReferentiel.isCountryValid(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pays non supporté : " + country + ". Valeurs : FRANCE, BELGIQUE");
        }
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        String provider = OAuthProviderResolver.resolve(principal);
        return currentUserResolver.resolve(oidcUser, provider, principal);
    }

    private CaseFile resolveCaseFileForUser(UUID caseFileId, User user) {
        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(caseFile.getWorkspace().getId()))
                .orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        if (!"DROIT_IMMIGRATION".equals(caseFile.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de l'immigration");
        }
        return caseFile;
    }
}
