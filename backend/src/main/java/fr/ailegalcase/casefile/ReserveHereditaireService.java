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
 * SF-FA-24-07 : service orchestrant le calcul de la réserve héréditaire et
 * de l'action en réduction (FR — DROIT_FAMILLE — art. 913 + 914-1 +
 * 920-928 Cciv).
 */
@Service
public class ReserveHereditaireService {

    private final ReserveHereditaireRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ReserveHereditaireService(ReserveHereditaireRepository repository,
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
    public ReserveHereditaireResponse calculate(UUID caseFileId,
                                                ReserveHereditaireRequest request,
                                                OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.nombreEnfants() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "nombreEnfants requis");
        }
        if (request.conjointSurvivant() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "conjointSurvivant requis");
        }
        if (request.montantSuccession() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "montantSuccession requis");
        }
        if (request.montantLibsTotal() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "montantLibsTotal requis");
        }
        if (request.dateOuvertureSuccession() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateOuvertureSuccession requise");
        }
        if (request.qualiteDuDemandeur() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "qualiteDuDemandeur requise");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        ReserveHereditaireResult result;
        try {
            result = ReserveHereditaireCalculator.compute(
                    request.nombreEnfants(),
                    request.conjointSurvivant(),
                    request.montantSuccession(),
                    request.montantLibsTotal(),
                    request.dateOuvertureSuccession(),
                    request.qualiteDuDemandeur(),
                    country
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ReserveHereditaireAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ReserveHereditaireAnalysis a = new ReserveHereditaireAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setNombreEnfants(result.nombreEnfants());
        entity.setConjointSurvivant(result.conjointSurvivant());
        entity.setMontantSuccession(result.montantSuccession());
        entity.setMontantLibsTotal(result.montantLibsTotal());
        entity.setDateOuvertureSuccession(result.dateOuvertureSuccession());
        entity.setQualiteDuDemandeur(result.qualiteDuDemandeur());
        entity.setCountry(result.country());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public ReserveHereditaireResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        ReserveHereditaireAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse Réserve héréditaire trouvée pour ce dossier"));
        return toResponse(caseFileId, deserialize(entity.getResultData()));
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId()))
                .orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
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

    private ReserveHereditaireResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, ReserveHereditaireResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private ReserveHereditaireResponse toResponse(UUID caseFileId, ReserveHereditaireResult r) {
        return new ReserveHereditaireResponse(
                caseFileId,
                r.nombreEnfants(),
                r.conjointSurvivant(),
                r.montantSuccession(),
                r.montantLibsTotal(),
                r.dateOuvertureSuccession(),
                r.qualiteDuDemandeur(),
                r.quotiteDisponiblePct(),
                r.reservePct(),
                r.montantReserveEur(),
                r.montantQuotiteDispoEur(),
                r.excedentReductibleEur(),
                r.actionReductionRecevable(),
                r.delaiPrescriptionRestantMois(),
                r.verdictRecevabilite(),
                r.scoreEligibilite(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                r.country()
        );
    }
}
