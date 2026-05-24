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
 * SF-212-11 : service orchestrant l'analyse de la modification du contrat
 * refusée par le salarié (F-DT-70, FRANCE — Cass. soc. ; L. 1222-6 CT) +
 * persistance snapshot (un seul résultat courant par dossier — UNIQUE
 * (case_file_id) côté migration 323).
 */
@Service
public class ModificationContratRefusService {

    private final ModificationContratRefusRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ModificationContratRefusService(ModificationContratRefusRepository repository,
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
    public ModificationContratRefusResponse calculate(UUID caseFileId,
                                                     ModificationContratRefusRequest request,
                                                     OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        ModificationContratRefusCalculator.Result result;
        try {
            result = ModificationContratRefusCalculator.compute(request.toInput(), country);
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("FRANCE")) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ModificationContratRefusResponse response =
                toResponse(caseFileId, request, result, Instant.now());

        ModificationContratRefusAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ModificationContratRefusAnalysis a = new ModificationContratRefusAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(result.country());
        entity.setSnapshotData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public ModificationContratRefusResponse get(UUID caseFileId,
                                                OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        ModificationContratRefusAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de modification du contrat trouvée pour ce dossier"));
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

    private ModificationContratRefusResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, ModificationContratRefusResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private ModificationContratRefusResponse toResponse(UUID caseFileId,
                                                       ModificationContratRefusRequest req,
                                                       ModificationContratRefusCalculator.Result r,
                                                       Instant calculatedAt) {
        return new ModificationContratRefusResponse(
                caseFileId,
                req.elementModifie(),
                req.elementExplicitementContractualise(),
                req.motifEconomique(),
                req.notificationEcriteL1222_6(),
                req.delaiReflexionRespecteMois(),
                req.reponseSalarie(),
                r.analyseModification(),
                r.scoreModificationContrat(),
                r.consequences(),
                r.alerteDelaiReflexion(),
                r.basesJuridiques(),
                r.messages(),
                r.country(),
                calculatedAt
        );
    }
}
