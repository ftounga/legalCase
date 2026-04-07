package fr.ailegalcase.casefile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.List;
import java.util.UUID;

@Service
public class ImmigrationTitleDecisionService {

    private static final List<String> VALID_DUREES = List.of("COURT_SEJOUR", "LONG_SEJOUR");
    private static final List<String> VALID_SITUATIONS = List.of("CELIBATAIRE", "MARIE", "PACS_COHABITATION");

    private final ImmigrationTitleDecisionRepository decisionRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ImmigrationTitleDecisionService(ImmigrationTitleDecisionRepository decisionRepository,
                                           CaseFileRepository caseFileRepository,
                                           WorkspaceMemberRepository workspaceMemberRepository,
                                           CurrentUserResolver currentUserResolver,
                                           ObjectMapper objectMapper) {
        this.decisionRepository = decisionRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ImmigrationTitleDecisionResponse resolve(UUID caseFileId,
                                                     ImmigrationTitleDecisionRequest request,
                                                     OidcUser oidcUser,
                                                     Principal principal) {
        validateRequest(request);
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);

        List<TitleRecommendation> recommendations = ImmigrationTitleDecisionEngine.resolve(
                request.country(),
                request.nationaliteUe(),
                request.motif(),
                request.duree(),
                request.situationFamiliale()
        );

        ImmigrationTitleDecision decision = decisionRepository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ImmigrationTitleDecision d = new ImmigrationTitleDecision();
                    d.setCaseFile(caseFile);
                    return d;
                });

        decision.setCountry(request.country());
        decision.setNationaliteUe(request.nationaliteUe());
        decision.setMotif(request.motif());
        decision.setDuree(request.duree());
        decision.setSituationFamiliale(request.situationFamiliale());
        decision.setRecommendedTitles(serializeRecommendations(recommendations));

        decisionRepository.save(decision);

        return toResponse(caseFileId, decision, recommendations);
    }

    @Transactional(readOnly = true)
    public ImmigrationTitleDecisionResponse get(UUID caseFileId,
                                                 OidcUser oidcUser,
                                                 Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFileForUser(caseFileId, user);

        ImmigrationTitleDecision decision = decisionRepository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune décision trouvée pour ce dossier"));

        List<TitleRecommendation> recommendations = deserializeRecommendations(decision.getRecommendedTitles());
        return toResponse(caseFileId, decision, recommendations);
    }

    private void validateRequest(ImmigrationTitleDecisionRequest request) {
        if (!ImmigrationTitleReferentiel.isCountryValid(request.country())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pays non supporté : " + request.country() + ". Valeurs : FRANCE, BELGIQUE");
        }
        if (!ImmigrationTitleReferentiel.isMotifValid(request.motif())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Motif inconnu : " + request.motif() + ". Valeurs : TRAVAIL, ETUDES, FAMILLE, ASILE, AUTRE");
        }
        if (!VALID_DUREES.contains(request.duree())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Durée invalide : " + request.duree() + ". Valeurs : COURT_SEJOUR, LONG_SEJOUR");
        }
        if (request.situationFamiliale() != null && !VALID_SITUATIONS.contains(request.situationFamiliale())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Situation familiale invalide : " + request.situationFamiliale()
                            + ". Valeurs : CELIBATAIRE, MARIE, PACS_COHABITATION");
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

    private String serializeRecommendations(List<TitleRecommendation> recommendations) {
        try {
            return objectMapper.writeValueAsString(recommendations);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de sérialisation des recommandations");
        }
    }

    private List<TitleRecommendation> deserializeRecommendations(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private ImmigrationTitleDecisionResponse toResponse(UUID caseFileId,
                                                         ImmigrationTitleDecision decision,
                                                         List<TitleRecommendation> recommendations) {
        return new ImmigrationTitleDecisionResponse(
                caseFileId,
                decision.getCountry(),
                decision.isNationaliteUe(),
                decision.getMotif(),
                decision.getDuree(),
                decision.getSituationFamiliale(),
                recommendations
        );
    }
}
