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
public class TravailDissimuleService {

    private final TravailDissimuleRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public TravailDissimuleService(TravailDissimuleRepository repository,
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
    public TravailDissimuleResponse calculate(UUID caseFileId, TravailDissimuleRequest request,
                                              OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        TravailDissimuleResult result;
        try {
            result = IndemniteTravailDissimuleCalculator.compute(request.salaireMensuelReference());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        TravailDissimuleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    TravailDissimuleAnalysis a = new TravailDissimuleAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setSalaireMensuelReference(result.salaireMensuelReference());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public TravailDissimuleResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        TravailDissimuleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de travail dissimulé trouvée pour ce dossier"));
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

    private TravailDissimuleResult deserialize(String json) {
        try { return objectMapper.readValue(json, TravailDissimuleResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private TravailDissimuleResponse toResponse(UUID caseFileId, TravailDissimuleResult r) {
        return new TravailDissimuleResponse(
                caseFileId,
                r.salaireMensuelReference(),
                r.indemniteForfaitaire(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : java.util.List.of()
        );
    }
}
