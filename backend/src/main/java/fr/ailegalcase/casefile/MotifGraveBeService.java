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
public class MotifGraveBeService {

    private final MotifGraveBeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public MotifGraveBeService(MotifGraveBeRepository repository,
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
    public MotifGraveBeResponse calculate(UUID caseFileId, MotifGraveBeRequest request,
                                          OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Motif grave procédure BE uniquement — en France voir F-DT-08/F-DT-01");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.anciennetteAnnees() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "anciennetteAnnees est requis");
        }

        MotifGraveBeResult result;
        try {
            result = MotifGraveBeCalculator.compute(
                    request.dateConnaissanceFait(),
                    request.dateNotificationRupture(),
                    request.dateNotificationMotifs(),
                    request.anciennetteAnnees(),
                    request.salaireMensuelReference());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        MotifGraveBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    MotifGraveBeAnalysis a = new MotifGraveBeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateConnaissanceFait(result.dateConnaissanceFait());
        entity.setDateNotificationRupture(result.dateNotificationRupture());
        entity.setDateNotificationMotifs(result.dateNotificationMotifs());
        entity.setAnciennetteAnnees(result.anciennetteAnnees());
        entity.setSalaireMensuelReference(result.salaireMensuelReference());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public MotifGraveBeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        MotifGraveBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse motif grave BE trouvée pour ce dossier"));
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

    private MotifGraveBeResult deserialize(String json) {
        try { return objectMapper.readValue(json, MotifGraveBeResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private MotifGraveBeResponse toResponse(UUID caseFileId, MotifGraveBeResult r) {
        return new MotifGraveBeResponse(
                caseFileId,
                r.dateConnaissanceFait(),
                r.dateNotificationRupture(),
                r.dateNotificationMotifs(),
                r.anciennetteAnnees(),
                r.salaireMensuelReference(),
                r.delaiRuptureJoursOuvrables(),
                r.delaiMotifsJoursOuvrables(),
                r.motifGraveProceduralementValide(),
                r.indemnitePreavisSiInvalide(),
                r.indemniteManifestementDeraisonnableMin(),
                r.indemniteManifestementDeraisonnableMax(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : java.util.List.of()
        );
    }
}
