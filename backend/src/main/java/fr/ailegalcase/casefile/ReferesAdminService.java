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
public class ReferesAdminService {

    private final ReferesAdminRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ReferesAdminService(ReferesAdminRepository repository,
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
    public ReferesAdminResponse calculate(UUID caseFileId, ReferesAdminRequest request,
                                          OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Référés administratifs FR — procédure CJA non applicable en Belgique");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        boolean urgenceCaracterisee = Boolean.TRUE.equals(request.urgenceCaracterisee());
        boolean atteinteLiberteFondamentale = Boolean.TRUE.equals(request.atteinteLiberteFondamentale());
        boolean doutesSerieuxLegalite = Boolean.TRUE.equals(request.doutesSerieuxLegalite());
        boolean demandeurDejaPrived = Boolean.TRUE.equals(request.demandeurDejaPrived());

        ReferesAdminResult result;
        try {
            result = ReferesAdminCalculator.compute(
                    request.typeRefere(),
                    request.decisionContestee(),
                    request.dateNotificationDecision(),
                    urgenceCaracterisee,
                    atteinteLiberteFondamentale,
                    doutesSerieuxLegalite,
                    request.preuvesUrgence(),
                    demandeurDejaPrived);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ReferesAdminAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ReferesAdminAnalysis a = new ReferesAdminAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setTypeRefere(result.typeRefere());
        entity.setDecisionContestee(result.decisionContestee());
        entity.setDateNotificationDecision(result.dateNotificationDecision());
        entity.setUrgenceCaracterisee(result.urgenceCaracterisee());
        entity.setAtteinteLiberteFondamentale(result.atteinteLiberteFondamentale());
        entity.setDoutesSerieuxLegalite(result.doutesSerieuxLegalite());
        entity.setDemandeurDejaPrived(result.demandeurDejaPrived());
        entity.setPreuvesUrgenceJson(serialize(result.preuvesUrgence()));
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public ReferesAdminResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        ReferesAdminAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse référés admin trouvée pour ce dossier"));
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
        if (!"DROIT_IMMIGRATION".equals(cf.getLegalDomain()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de l'immigration");
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private ReferesAdminResult deserialize(String json) {
        try { return objectMapper.readValue(json, ReferesAdminResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private ReferesAdminResponse toResponse(UUID caseFileId, String country, ReferesAdminResult r) {
        return new ReferesAdminResponse(
                caseFileId,
                r.typeRefere(),
                r.decisionContestee(),
                r.dateNotificationDecision(),
                r.urgenceCaracterisee(),
                r.atteinteLiberteFondamentale(),
                r.doutesSerieuxLegalite(),
                r.preuvesUrgence() != null ? r.preuvesUrgence() : List.of(),
                r.demandeurDejaPrived(),
                r.scoreSuccessProbabiliteSuspension(),
                r.scoreSuccessProbabiliteLiberte(),
                r.verdictRecommandation(),
                r.delaiJugeTaJoursL521_1(),
                r.delaiJugeTaHeuresL521_2(),
                r.conditionsCumulativesL521_1Ok(),
                r.conditionsCumulativesL521_2Ok(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                country
        );
    }
}
