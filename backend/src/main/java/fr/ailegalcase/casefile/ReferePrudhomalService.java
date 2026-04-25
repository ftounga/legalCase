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
import java.util.List;
import java.util.UUID;

@Service
public class ReferePrudhomalService {

    private final ReferePrudhomalRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ReferePrudhomalService(ReferePrudhomalRepository repository,
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
    public ReferePrudhomalResponse calculate(UUID caseFileId, ReferePrudhomalRequest request,
                                             OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Référé prud'homal — procédure FR uniquement (R.1454-1 CT)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        boolean absenceContestationSerieuse = Boolean.TRUE.equals(request.absenceContestationSerieuse());
        boolean dommageImmediatCarac = Boolean.TRUE.equals(request.dommageImmediatCarac());
        boolean tresorerieEmployeurDouteuse = Boolean.TRUE.equals(request.tresorerieEmployeurDouteuse());

        ReferePrudhomalResult result;
        try {
            result = ReferePrudhomalCalculator.compute(
                    request.typeRefere(),
                    request.natureCreance(),
                    request.montantProvisionDemandeeEur(),
                    absenceContestationSerieuse,
                    request.preuvesUrgenceProduites(),
                    dommageImmediatCarac,
                    tresorerieEmployeurDouteuse,
                    request.dateMiseEnDemeure(),
                    request.ancienneteContratMois());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ReferePrudhomalAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ReferePrudhomalAnalysis a = new ReferePrudhomalAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setTypeRefere(result.typeRefere());
        entity.setNatureCreance(result.natureCreance());
        entity.setMontantProvisionDemandeeEur(result.montantProvisionDemandeeEur());
        entity.setAbsenceContestationSerieuse(result.absenceContestationSerieuse());
        entity.setPreuvesUrgenceProduitesJson(serialize(result.preuvesUrgenceProduites()));
        entity.setDommageImmediatCarac(result.dommageImmediatCarac());
        entity.setTresorerieEmployeurDouteuse(result.tresorerieEmployeurDouteuse());
        entity.setDateMiseEnDemeure(result.dateMiseEnDemeure());
        entity.setAncienneteContratMois(result.ancienneteContratMois());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public ReferePrudhomalResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        ReferePrudhomalAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse référé prud'homal trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserialize(entity.getResultData()));
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        if (!"DROIT_DU_TRAVAIL".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit du travail");
        }
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private ReferePrudhomalResult deserialize(String json) {
        try { return objectMapper.readValue(json, ReferePrudhomalResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private ReferePrudhomalResponse toResponse(UUID caseFileId, String country, ReferePrudhomalResult r) {
        return new ReferePrudhomalResponse(
                caseFileId,
                r.typeRefere(),
                r.natureCreance(),
                r.montantProvisionDemandeeEur(),
                r.absenceContestationSerieuse(),
                r.preuvesUrgenceProduites() != null ? r.preuvesUrgenceProduites() : List.of(),
                r.dommageImmediatCarac(),
                r.tresorerieEmployeurDouteuse(),
                r.dateMiseEnDemeure(),
                r.ancienneteContratMois(),
                r.scoreSuccess(),
                r.verdictRecommandation(),
                r.delaiAudienceJoursPrevisionnel(),
                r.delaiOrdonnanceJoursPrevisionnel(),
                r.montantProvisionRecommandeEur(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                country
        );
    }
}
