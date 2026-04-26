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
 * SF-FA-16-01 : service orchestrant l'analyse du régime conventionnel de
 * communauté universelle (FR — DROIT_FAMILLE — art. 1526 + 1527 al. 2 Cciv).
 */
@Service
public class CommunauteUniverselleService {

    private final CommunauteUniverselleRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public CommunauteUniverselleService(CommunauteUniverselleRepository repository,
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
    public CommunauteUniverselleResponse calculate(UUID caseFileId,
                                                   CommunauteUniverselleRequest request,
                                                   OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.dispositifAnalyse() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Dispositif d'analyse requis");
        }
        if (request.contratNotarie() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Contrat notarié (oui/non) requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        CommunauteUniverselleResult result;
        try {
            result = CommunauteUniverselleCalculator.compute(
                    request.dispositifAnalyse(),
                    request.contratNotarie(),
                    request.inscriptionEtatCivil(),
                    request.consentementLibreDesEpoux(),
                    request.respectReserveHereditaire(),
                    request.clauseAttributionIntegrale(),
                    request.enfantsNonCommuns(),
                    request.valeurCommunauteEur(),
                    country
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        CommunauteUniverselleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    CommunauteUniverselleAnalysis a = new CommunauteUniverselleAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDispositifAnalyse(result.dispositifAnalyse());
        entity.setContratNotarie(Boolean.TRUE.equals(result.contratNotarie()));
        entity.setCountry(result.country());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public CommunauteUniverselleResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        CommunauteUniverselleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse Communauté universelle trouvée pour ce dossier"));
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private CommunauteUniverselleResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, CommunauteUniverselleResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private CommunauteUniverselleResponse toResponse(UUID caseFileId, CommunauteUniverselleResult r) {
        return new CommunauteUniverselleResponse(
                caseFileId,
                r.dispositifAnalyse(),
                r.verdictValidite(),
                r.scoreValidite(),
                r.actionRetranchementPossible(),
                r.partAttributionConjointPct(),
                r.valeurAttributionEur(),
                r.risquesIdentifies() != null ? r.risquesIdentifies() : List.of(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                r.country()
        );
    }
}
