package fr.ailegalcase.casefile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.Instant;
import java.util.UUID;

/**
 * SF-217-16 : service orchestrant l'analyse de reconnaissance d'un mariage ou
 * divorce étranger en Belgique (incluant le talaq) + persistance snapshot
 * (un seul résultat courant par dossier, écrasé au recalcul — upsert 1:1).
 */
@Service
public class MariageEtrangerBeReconnaissanceService {

    private final MariageEtrangerBeReconnaissanceRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public MariageEtrangerBeReconnaissanceService(
            MariageEtrangerBeReconnaissanceRepository repository,
            CaseFileRepository caseFileRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            CurrentUserResolver currentUserResolver,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MariageEtrangerBeReconnaissanceResponse calculate(
            UUID caseFileId,
            MariageEtrangerBeReconnaissanceRequest request,
            OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;

        MariageEtrangerBeReconnaissanceResult result;
        try {
            result = MariageEtrangerBeReconnaissanceCalculator.compute(
                    request.toInput(), country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        MariageEtrangerBeReconnaissanceResponse response =
                toResponse(caseFileId, request, result, Instant.now());

        MariageEtrangerBeReconnaissanceAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    MariageEtrangerBeReconnaissanceAnalysis a =
                            new MariageEtrangerBeReconnaissanceAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(result.country());
        entity.setSnapshotData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public MariageEtrangerBeReconnaissanceResponse get(UUID caseFileId,
                                                       OidcUser oidcUser,
                                                       Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        MariageEtrangerBeReconnaissanceAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de reconnaissance trouvée pour ce dossier"));
        return deserialize(entity.getSnapshotData());
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    /**
     * Résout le dossier en appliquant les gates : 404 dossier inexistant, 403
     * dossier d'un autre workspace, 422 domaine != DROIT_FAMILLE ou pays != BELGIQUE.
     */
    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Dossier introuvable"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ce dossier appartient à un autre workspace");
        }
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Ce dossier n'est pas un dossier de droit de la famille");
        }
        String country = cf.getWorkspace() != null ? cf.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "La reconnaissance d'un mariage / divorce étranger est un outil disponible "
                            + "uniquement pour les workspaces de Belgique");
        }
        return cf;
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de sérialisation");
        }
    }

    private MariageEtrangerBeReconnaissanceResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, MariageEtrangerBeReconnaissanceResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private MariageEtrangerBeReconnaissanceResponse toResponse(
            UUID caseFileId,
            MariageEtrangerBeReconnaissanceRequest req,
            MariageEtrangerBeReconnaissanceResult r,
            Instant calculatedAt) {
        return new MariageEtrangerBeReconnaissanceResponse(
                caseFileId,
                req.natureActe(),
                req.paysOrigine(),
                req.dateActe(),
                req.residenceHabituelleAuMoinsUnePartie(),
                req.nationaliteAuMoinsUnePartie(),
                req.conformiteDroitFondPersonnel(),
                req.conformiteFormeLocusRegitActum(),
                req.consentementEpouse(),
                req.epousePresente(),
                req.procedureContradictoire(),
                req.decisionEcriteOfficielle(),
                req.conventionBilateraleApplicable(),
                req.commentaire(),
                r.verdict(),
                r.motifsRefus(),
                r.motifsReserve(),
                r.actesAProduire(),
                r.basesJuridiques(),
                r.messages(),
                r.country(),
                calculatedAt
        );
    }
}
