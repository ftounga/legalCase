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
 * SF-206-01 : service orchestrant l'analyse de contestation d'une présomption
 * de démission par abandon de poste (FR) + persistance snapshot (un seul
 * résultat courant par dossier).
 */
@Service
public class AbandonPostePresomptionDemissionService {

    private final AbandonPostePresomptionDemissionRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AbandonPostePresomptionDemissionService(AbandonPostePresomptionDemissionRepository repository,
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
    public AbandonPostePresomptionDemissionResponse calculate(UUID caseFileId,
                                                              AbandonPostePresomptionDemissionRequest request,
                                                              OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        AbandonPostePresomptionDemissionResult result;
        try {
            result = AbandonPostePresomptionDemissionCalculator.compute(request.toInput(), country);
        } catch (IllegalArgumentException e) {
            // FR-only et input invalide → 422 (outil hors pays) ou 400 (input).
            if (e.getMessage() != null && e.getMessage().contains("FRANCE")) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        AbandonPostePresomptionDemissionResponse response =
                toResponse(caseFileId, request, result, Instant.now());

        AbandonPostePresomptionDemissionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AbandonPostePresomptionDemissionAnalysis a =
                            new AbandonPostePresomptionDemissionAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(result.country());
        entity.setSnapshotData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public AbandonPostePresomptionDemissionResponse get(UUID caseFileId,
                                                        OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        AbandonPostePresomptionDemissionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse d'abandon de poste trouvée pour ce dossier"));
        return deserialize(entity.getSnapshotData());
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier introuvable"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ce dossier appartient à un autre workspace");
        }
        if (!"DROIT_DU_TRAVAIL".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Ce dossier n'est pas un dossier de droit du travail");
        }
        return cf;
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private AbandonPostePresomptionDemissionResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, AbandonPostePresomptionDemissionResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private AbandonPostePresomptionDemissionResponse toResponse(UUID caseFileId,
                                                                AbandonPostePresomptionDemissionRequest req,
                                                                AbandonPostePresomptionDemissionResult r,
                                                                Instant calculatedAt) {
        return new AbandonPostePresomptionDemissionResponse(
                caseFileId,
                req.dateMiseEnDemeurePresentee(),
                req.modeNotificationMiseEnDemeure(),
                req.delaiAccordeJours(),
                req.miseEnDemeureMentionneDelai(),
                req.miseEnDemeureMentionneConsequences(),
                req.motifAbsenceInvoque(),
                req.motifAbsenceCommentaire(),
                req.repriseOuJustificationDansDelai(),
                req.dateRepriseOuJustification(),
                r.verdict(),
                r.scoreContestation(),
                r.motifsContestation(),
                r.dateExpirationDelai(),
                r.basesJuridiques(),
                r.messages(),
                r.country(),
                calculatedAt
        );
    }
}
