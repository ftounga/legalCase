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
 * SF-FA-18-07 : service orchestrant l'analyse de recevabilité d'une
 * possession d'état (FR — DROIT_FAMILLE — art. 311-1 + 311-2 + 317 Cciv).
 */
@Service
public class PossessionEtatService {

    private final PossessionEtatRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public PossessionEtatService(PossessionEtatRepository repository,
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
    public PossessionEtatResponse calculate(UUID caseFileId,
                                            PossessionEtatRequest request,
                                            OidcUser oidcUser,
                                            Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.dateDebutPossession() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Date de début de possession requise");
        }
        if (request.dateFinPossession() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Date de fin de possession requise");
        }
        if (request.dateFinPossession().isBefore(request.dateDebutPossession())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Date de fin doit être postérieure ou égale à la date de début");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        PossessionEtatResult result;
        try {
            result = PossessionEtatCalculator.compute(
                    request.dateDebutPossession(),
                    request.dateFinPossession(),
                    request.tractatus(),
                    request.fama(),
                    request.nomen(),
                    request.continueCondition(),
                    request.paisible(),
                    request.nonEquivoque(),
                    country
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        PossessionEtatAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    PossessionEtatAnalysis a = new PossessionEtatAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateDebutPossession(result.dateDebutPossession());
        entity.setDateFinPossession(result.dateFinPossession());
        entity.setTractatus(result.tractatus());
        entity.setFama(result.fama());
        entity.setNomen(result.nomen());
        entity.setContinueCondition(result.continueCondition());
        entity.setPaisible(result.paisible());
        entity.setNonEquivoque(result.nonEquivoque());
        entity.setCountry(result.country());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public PossessionEtatResponse get(UUID caseFileId,
                                      OidcUser oidcUser,
                                      Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        PossessionEtatAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse Possession d'état trouvée pour ce dossier"));
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
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de la famille");
        }
        return cf;
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de sérialisation");
        }
    }

    private PossessionEtatResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, PossessionEtatResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private PossessionEtatResponse toResponse(UUID caseFileId,
                                              PossessionEtatResult r) {
        return new PossessionEtatResponse(
                caseFileId,
                r.verdictRecevabilite(),
                r.dispositifApplicable(),
                r.scoreRecevabilite(),
                r.dureePossessionAnnees(),
                r.delaiContestationActeAns(),
                r.delaiContestationCessationAns(),
                r.criteresRemplis() != null ? r.criteresRemplis() : List.of(),
                r.criteresManquants() != null ? r.criteresManquants() : List.of(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                r.country()
        );
    }
}
