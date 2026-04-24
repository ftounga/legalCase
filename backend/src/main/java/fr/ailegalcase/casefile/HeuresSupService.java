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
public class HeuresSupService {

    private final HeuresSupRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public HeuresSupService(HeuresSupRepository repository,
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
    public HeuresSupResponse calculate(UUID caseFileId, HeuresSupRequest request,
                                       OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;

        HeuresSupResult result;
        try {
            result = HeuresSupCalculator.compute(request, country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        HeuresSupAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    HeuresSupAnalysis a = new HeuresSupAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setTauxHoraireBrut(result.tauxHoraireBrut());
        entity.setHeuresSup25pct(result.heuresSupDeclarees25pct());
        entity.setHeuresSup50pct(result.heuresSupDeclarees50pct());
        entity.setHeuresHorsContingent(result.heuresHorsContingent());
        entity.setTauxMajoration25(result.tauxMajoration25());
        entity.setTauxMajoration50(result.tauxMajoration50());
        entity.setHeuresSupSemaineBe(result.heuresSupSemaine());
        entity.setHeuresDimancheJfBe(result.heuresDimancheJoursFeries());
        entity.setCountry(result.country());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public HeuresSupResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        HeuresSupAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse heures supplémentaires trouvée pour ce dossier"));
        return toResponse(caseFileId, deserialize(entity.getResultData()));
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

    private HeuresSupResult deserialize(String json) {
        try { return objectMapper.readValue(json, HeuresSupResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private HeuresSupResponse toResponse(UUID caseFileId, HeuresSupResult r) {
        return new HeuresSupResponse(
                caseFileId,
                r.tauxHoraireBrut(),
                r.heuresSupDeclarees25pct(),
                r.heuresSupDeclarees50pct(),
                r.heuresHorsContingent(),
                r.tauxMajoration25(),
                r.tauxMajoration50(),
                r.heuresSupSemaine(),
                r.heuresDimancheJoursFeries(),
                r.country(),
                r.rappelMajoration25pct(),
                r.rappelMajoration50pct(),
                r.rappelMajoration100pct(),
                r.rappelTotal(),
                r.reposCompensateurHeuresDues(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : java.util.List.of()
        );
    }
}
