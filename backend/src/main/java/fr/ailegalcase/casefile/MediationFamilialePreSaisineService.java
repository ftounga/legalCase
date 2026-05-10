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

/**
 * SF-210-01 : service orchestrant l'analyse de médiation familiale obligatoire
 * pré-saisine JAF (Cciv art. 373-2-10 al. 3). Gate FRANCE + DROIT_FAMILLE strict.
 */
@Service
public class MediationFamilialePreSaisineService {

    private final MediationFamilialePreSaisineRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public MediationFamilialePreSaisineService(
            MediationFamilialePreSaisineRepository repository,
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
    public MediationFamilialePreSaisineResponse calculate(UUID caseFileId,
                                                          MediationFamilialePreSaisineRequest request,
                                                          OidcUser oidcUser,
                                                          Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Médiation familiale pré-saisine FR uniquement (art. 373-2-10 al. 3 Cciv).");
        }
        if (request == null || request.motifSaisine() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "motifSaisine est requis");
        }
        boolean mediationTentee = request.mediationTentee() != null && request.mediationTentee();
        MediationFamilialePreSaisineResult result;
        try {
            result = MediationFamilialePreSaisineCalculator.compute(
                    request.motifSaisine(),
                    mediationTentee,
                    request.dateMediation(),
                    request.exceptionApplicable(),
                    request.exceptionDetail());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        MediationFamilialePreSaisineAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    MediationFamilialePreSaisineAnalysis a = new MediationFamilialePreSaisineAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setMotifSaisine(result.motifSaisine());
        entity.setMediationTentee(result.mediationTentee());
        entity.setDateMediation(result.dateMediation());
        entity.setExceptionApplicable(result.exceptionApplicable());
        entity.setExceptionDetail(result.exceptionDetail());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public MediationFamilialePreSaisineResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        MediationFamilialePreSaisineAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de médiation familiale trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserializeResult(entity.getResultData()));
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
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de la famille");
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private MediationFamilialePreSaisineResult deserializeResult(String json) {
        try { return objectMapper.readValue(json, MediationFamilialePreSaisineResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private MediationFamilialePreSaisineResponse toResponse(UUID caseFileId, String country,
                                                            MediationFamilialePreSaisineResult r) {
        return new MediationFamilialePreSaisineResponse(
                caseFileId,
                r.motifSaisine(),
                r.mediationTentee(),
                r.dateMediation(),
                r.exceptionApplicable(),
                r.exceptionDetail(),
                r.verdict(),
                r.motifInScope(),
                r.dispenseApplicable(),
                r.piecesAJoindre() != null ? r.piecesAJoindre() : List.of(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                country);
    }
}
