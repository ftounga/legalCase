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
 * SF-DT-36-01 : service orchestrant l'analyse des nullités de procédure de
 * licenciement + persistance snapshot (un seul résultat courant par dossier).
 */
@Service
public class ProcedureNulliteLicenciementService {

    private final ProcedureNulliteLicenciementRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ProcedureNulliteLicenciementService(ProcedureNulliteLicenciementRepository repository,
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
    public ProcedureNulliteLicenciementResponse calculate(UUID caseFileId,
                                                          ProcedureNulliteLicenciementRequest request,
                                                          OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        ProcedureNulliteResult result;
        try {
            result = ProcedureNulliteLicenciementCalculator.compute(request.toInput(), country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ProcedureNulliteLicenciementResponse response =
                toResponse(caseFileId, request, result, Instant.now());

        ProcedureNulliteLicenciementAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ProcedureNulliteLicenciementAnalysis a = new ProcedureNulliteLicenciementAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(result.country());
        entity.setSnapshotData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public ProcedureNulliteLicenciementResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        ProcedureNulliteLicenciementAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de nullité de procédure trouvée pour ce dossier"));
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

    private ProcedureNulliteLicenciementResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, ProcedureNulliteLicenciementResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private ProcedureNulliteLicenciementResponse toResponse(UUID caseFileId,
                                                            ProcedureNulliteLicenciementRequest req,
                                                            ProcedureNulliteResult r,
                                                            Instant calculatedAt) {
        return new ProcedureNulliteLicenciementResponse(
                caseFileId,
                req.convocationEnvoyee(),
                req.dateConvocationPresentee(),
                req.dateEntretienPrealable(),
                req.entretienTenu(),
                req.dateNotificationLicenciement(),
                req.lettreLicenciementEcrite(),
                req.lettreMotivee(),
                req.motivationSuffisante(),
                req.motivationCommentaire(),
                req.licenciementPourMotifGrave(),
                req.licenciementCollectif(),
                req.procedureCseRespectee(),
                req.conventionCollectiveApplicable(),
                req.conventionCollectiveRespectee(),
                req.conventionCollectiveCommentaire(),
                r.verdict(),
                r.scoreNullite(),
                r.vicesDetectes(),
                r.basesJuridiques(),
                r.messages(),
                r.country(),
                calculatedAt
        );
    }
}
