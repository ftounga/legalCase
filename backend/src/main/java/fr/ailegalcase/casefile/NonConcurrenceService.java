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

/**
 * SF-DT-24-01 : service orchestrant l'analyse de validité d'une clause de
 * non-concurrence (FR) + persistance snapshot.
 */
@Service
public class NonConcurrenceService {

    private final NonConcurrenceRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public NonConcurrenceService(NonConcurrenceRepository repository,
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
    public NonConcurrenceResponse calculate(UUID caseFileId,
                                            NonConcurrenceRequest request,
                                            OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        NonConcurrenceResult result;
        try {
            result = NonConcurrenceCalculator.compute(request.toInput(), country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        NonConcurrenceResponse response = toResponse(caseFileId, request, result);

        NonConcurrenceAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    NonConcurrenceAnalysis a = new NonConcurrenceAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(result.country());
        entity.setSnapshotData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public NonConcurrenceResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        NonConcurrenceAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de clause de non-concurrence trouvée pour ce dossier"));
        return deserialize(entity.getSnapshotData());
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
        if (!"DROIT_DU_TRAVAIL".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
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

    private NonConcurrenceResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, NonConcurrenceResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private NonConcurrenceResponse toResponse(UUID caseFileId,
                                              NonConcurrenceRequest req,
                                              NonConcurrenceResult r) {
        return new NonConcurrenceResponse(
                caseFileId,
                req.clausePresenteContrat(),
                req.limiteTerritoireDefini(),
                req.territoireDescription(),
                req.limiteDureeDefinie(),
                req.dureeMois(),
                req.limiteObjetDefini(),
                req.objetDescription(),
                req.contrepartieFinancierePresente(),
                req.contrepartieMontantMensuelEur(),
                req.salaireMensuelBrutEur(),
                req.secteurActivite(),
                req.datePriseEffet(),
                r.critere1TerritoireOk(),
                r.critere2DureeOk(),
                r.critere3ObjetOk(),
                r.critere4ContrepartieOk(),
                r.ratioContrepartiePct(),
                r.scoreValidite(),
                r.verdictValidite(),
                r.indemniteContrepartieDueEur(),
                r.indemnitePotentielleNulliteEur(),
                r.baseJuridique(),
                r.formule(),
                r.messages(),
                r.country()
        );
    }
}
