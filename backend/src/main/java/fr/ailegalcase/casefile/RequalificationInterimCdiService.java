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
public class RequalificationInterimCdiService {

    private final RequalificationInterimCdiRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public RequalificationInterimCdiService(RequalificationInterimCdiRepository repository,
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
    public RequalificationInterimCdiResponse calculate(UUID caseFileId,
                                                       RequalificationInterimCdiRequest request,
                                                       OidcUser oidcUser,
                                                       Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        RequalificationInterimCdiResult result;
        try {
            result = RequalificationInterimCdiCalculator.compute(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        RequalificationInterimCdiAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    RequalificationInterimCdiAnalysis a = new RequalificationInterimCdiAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setMotifInterimInvoque(result.motifInterimInvoque());
        entity.setMotifInterdit(result.motifInterdit());
        entity.setMotifInterditType(result.motifInterditType());
        entity.setDelaiCarenceRespecte(result.delaiCarenceRespecte());
        entity.setDureeMissionsTotaleMois(result.dureeMissionsTotaleMois());
        entity.setSalaireMensuelBrutEur(result.salaireMensuelBrutEur());
        entity.setDateFinDerniereMission(result.dateFinDerniereMission());
        entity.setMemeEntrepriseUtilisatrice(result.memeEntrepriseUtilisatrice());
        entity.setScoreRequalification(result.scoreRequalification());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public RequalificationInterimCdiResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        RequalificationInterimCdiAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de requalification intérim → CDI trouvée pour ce dossier"));
        RequalificationInterimCdiResult result = deserialize(entity.getResultData());
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

    private RequalificationInterimCdiResult deserialize(String json) {
        try { return objectMapper.readValue(json, RequalificationInterimCdiResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private RequalificationInterimCdiResponse toResponse(UUID caseFileId, RequalificationInterimCdiResult r) {
        return new RequalificationInterimCdiResponse(
                caseFileId,
                r.motifInterimInvoque(),
                r.motifInterdit(),
                r.motifInterditType(),
                r.successionMissions() != null ? r.successionMissions() : java.util.List.of(),
                r.delaiCarenceRespecte(),
                r.dureeMissionsTotaleMois(),
                r.salaireMensuelBrutEur(),
                r.dateFinDerniereMission(),
                r.memeEntrepriseUtilisatrice(),
                r.scoreRequalification(),
                r.verdictProbabiliteRequalification(),
                r.indemniteRequalificationEur(),
                r.indemniteFinMissionInterimEur(),
                r.totalDommagesIndemniteEur(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : java.util.List.of(),
                "FRANCE"
        );
    }
}
