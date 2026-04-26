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
 * SF-FA-18-01 : service orchestrant l'analyse de recevabilité d'une
 * reconnaissance paternelle (FR — DROIT_FAMILLE — art. 316 + 332-335 + 372 Cciv).
 */
@Service
public class ReconnaissancePaterneleService {

    private final ReconnaissancePaterneleRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ReconnaissancePaterneleService(ReconnaissancePaterneleRepository repository,
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
    public ReconnaissancePaterneleResponse calculate(UUID caseFileId,
                                                     ReconnaissancePaterneleRequest request,
                                                     OidcUser oidcUser,
                                                     Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.sousType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sous-type de reconnaissance requis");
        }
        if (request.consentementLibreDuPere() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Consentement libre du père (oui/non) requis");
        }
        if (request.paterniteVraisemblable() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Paternité vraisemblable (oui/non) requise");
        }
        if (request.enfantNonReconnuParAutrePere() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Enfant non reconnu par un autre père (oui/non) requis");
        }
        if (request.procedureRespectee() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Procédure respectée (oui/non) requise");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        ReconnaissancePaterneleResult result;
        try {
            result = ReconnaissancePaterneleCalculator.compute(
                    request.sousType(),
                    request.dateNaissanceEnfant(),
                    request.dateReconnaissance(),
                    request.consentementLibreDuPere(),
                    request.paterniteVraisemblable(),
                    request.enfantNonReconnuParAutrePere(),
                    request.procedureRespectee(),
                    request.presenceParProcuration(),
                    country
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ReconnaissancePaterneleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ReconnaissancePaterneleAnalysis a = new ReconnaissancePaterneleAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setSousType(result.sousType());
        entity.setDateNaissanceEnfant(result.dateNaissanceEnfant());
        entity.setDateReconnaissance(result.dateReconnaissance());
        entity.setConsentementLibreDuPere(result.consentementLibreDuPere());
        entity.setPaterniteVraisemblable(result.paterniteVraisemblable());
        entity.setEnfantNonReconnuParAutrePere(result.enfantNonReconnuParAutrePere());
        entity.setProcedureRespectee(result.procedureRespectee());
        entity.setPresenceParProcuration(result.presenceParProcuration());
        entity.setCountry(result.country());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public ReconnaissancePaterneleResponse get(UUID caseFileId,
                                               OidcUser oidcUser,
                                               Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        ReconnaissancePaterneleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse Reconnaissance paternelle trouvée pour ce dossier"));
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

    private ReconnaissancePaterneleResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, ReconnaissancePaterneleResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private ReconnaissancePaterneleResponse toResponse(UUID caseFileId,
                                                       ReconnaissancePaterneleResult r) {
        return new ReconnaissancePaterneleResponse(
                caseFileId,
                r.sousType(),
                r.verdictRecevabilite(),
                r.scoreEligibilite(),
                r.effetFiliation(),
                r.risquesContestation() != null ? r.risquesContestation() : List.of(),
                r.documentsRequis() != null ? r.documentsRequis() : List.of(),
                r.delaiContestationAns(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                r.country()
        );
    }
}
