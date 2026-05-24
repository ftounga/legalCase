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
 * SF-212-25 : service orchestrant l'évaluation de la protection du
 * lanceur d'alerte (F-DT-61, FRANCE — L. 1132-3-3 CT ; loi Sapin II ;
 * loi Waserman 2022) + persistance snapshot (un seul résultat courant
 * par dossier — UNIQUE(case_file_id) côté migration 325).
 */
@Service
public class LanceurAlerteProtectionService {

    private final LanceurAlerteProtectionRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public LanceurAlerteProtectionService(LanceurAlerteProtectionRepository repository,
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
    public LanceurAlerteProtectionResponse calculate(UUID caseFileId,
                                                     LanceurAlerteProtectionRequest request,
                                                     OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        LanceurAlerteProtectionCalculator.Result result;
        try {
            result = LanceurAlerteProtectionCalculator.compute(request.toInput(), country);
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("FRANCE")) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        LanceurAlerteProtectionResponse response =
                toResponse(caseFileId, request, result, Instant.now());

        LanceurAlerteProtectionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    LanceurAlerteProtectionAnalysis a = new LanceurAlerteProtectionAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(result.country());
        entity.setSnapshotData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public LanceurAlerteProtectionResponse get(UUID caseFileId,
                                               OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        LanceurAlerteProtectionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de protection lanceur d'alerte trouvée pour ce dossier"));
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

    private LanceurAlerteProtectionResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, LanceurAlerteProtectionResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private LanceurAlerteProtectionResponse toResponse(UUID caseFileId,
                                                      LanceurAlerteProtectionRequest req,
                                                      LanceurAlerteProtectionCalculator.Result r,
                                                      Instant calculatedAt) {
        return new LanceurAlerteProtectionResponse(
                caseFileId,
                req.natureSignalement(),
                req.contrepartieFinanciere(),
                req.procedureSignalement(),
                req.referentInterneSaisi(),
                req.mesureRepresailleDetectee(),
                req.natureMesureRepresaille(),
                r.analyseLanceurAlerte(),
                r.scoreProtection(),
                r.pointsAnalyse(),
                r.nulliteMesureRepresaille(),
                r.montantMinDommagesInterets(),
                r.basesJuridiques(),
                r.messages(),
                r.country(),
                calculatedAt
        );
    }
}
