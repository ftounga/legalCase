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
import java.util.UUID;

@Service
public class RequalificationCddCdiService {

    private final RequalificationCddCdiRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public RequalificationCddCdiService(RequalificationCddCdiRepository repository,
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
    public RequalificationCddCdiResponse calculate(UUID caseFileId,
                                                   RequalificationCddCdiRequest request,
                                                   OidcUser oidcUser,
                                                   Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        RequalificationCddCdiResult result;
        try {
            result = RequalificationCddCdiCalculator.compute(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        RequalificationCddCdiAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    RequalificationCddCdiAnalysis a = new RequalificationCddCdiAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setMotifCddInvoque(result.motifCddInvoque());
        entity.setMotifInterdit(result.motifInterdit());
        entity.setMotifInterditType(result.motifInterditType());
        entity.setDelaiCarenceRespecte(result.delaiCarenceRespecte());
        entity.setDureeContratMois(result.dureeContratMois());
        entity.setSalaireMensuelBrutEur(result.salaireMensuelBrutEur());
        entity.setDateFinDernierContrat(result.dateFinDernierContrat());
        entity.setScoreRequalification(result.scoreRequalification());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public RequalificationCddCdiResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        RequalificationCddCdiAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de requalification CDD → CDI trouvée pour ce dossier"));
        RequalificationCddCdiResult result = deserialize(entity.getResultData());
        return toResponse(caseFileId, result);
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
        if (!"DROIT_DU_TRAVAIL".equals(cf.getLegalDomain()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit du travail");
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private RequalificationCddCdiResult deserialize(String json) {
        try { return objectMapper.readValue(json, RequalificationCddCdiResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private RequalificationCddCdiResponse toResponse(UUID caseFileId, RequalificationCddCdiResult r) {
        return new RequalificationCddCdiResponse(
                caseFileId,
                r.motifCddInvoque(),
                r.motifInterdit(),
                r.motifInterditType(),
                r.successionCdd() != null ? r.successionCdd() : java.util.List.of(),
                r.delaiCarenceRespecte(),
                r.dureeContratMois(),
                r.salaireMensuelBrutEur(),
                r.dateFinDernierContrat(),
                r.scoreRequalification(),
                r.verdictProbabiliteRequalification(),
                r.indemniteRequalificationEur(),
                r.indemnitePrecariteEur(),
                r.totalDommagesIndemniteEur(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : java.util.List.of(),
                "FRANCE"
        );
    }
}
