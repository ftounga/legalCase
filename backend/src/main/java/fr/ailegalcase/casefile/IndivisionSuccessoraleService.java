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

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * SF-FA-24-11 : service applicatif "Indivision successorale" (art. 815 à 832-2
 * + 1873-1 et s. Cciv). Gate FR + DROIT_FAMILLE, isolation workspace,
 * persistance JSON.
 */
@Service
public class IndivisionSuccessoraleService {

    private final IndivisionSuccessoraleRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public IndivisionSuccessoraleService(
            IndivisionSuccessoraleRepository repository,
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
    public IndivisionSuccessoraleResponse calculate(UUID caseFileId,
                                                    IndivisionSuccessoraleRequest request,
                                                    OidcUser oidcUser,
                                                    Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Indivision successorale : procédure propre au droit "
                            + "français (art. 815 à 832-2 + 1873-1 et s. Cciv). "
                            + "BE non couverte par F-FA-24-11.");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Corps de requête requis");
        }
        if (request.dateOuvertureSuccession() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateOuvertureSuccession est requise");
        }
        if (request.typeIndivision() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "typeIndivision est requis");
        }
        if (request.nbHeritiers() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "nbHeritiers est requis");
        }
        if (request.valeurPatrimoineIndivisEur() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "valeurPatrimoineIndivisEur est requise");
        }

        BigDecimal valeurBien = request.valeurBienOccupeEur() != null
                ? request.valeurBienOccupeEur() : BigDecimal.ZERO;
        boolean consentements = Boolean.TRUE.equals(request.consentementsTous());
        boolean occupation = Boolean.TRUE.equals(request.occupationExclusive());
        boolean actesContestes = Boolean.TRUE.equals(request.actesAdministrationContestes());
        boolean partage = Boolean.TRUE.equals(request.demandePartage());

        IndivisionSuccessoraleResult result;
        try {
            result = IndivisionSuccessoraleCalculator.compute(
                    request.dateOuvertureSuccession(),
                    request.typeIndivision(),
                    request.nbHeritiers(),
                    request.valeurPatrimoineIndivisEur(),
                    valeurBien,
                    consentements,
                    occupation,
                    actesContestes,
                    partage);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        IndivisionSuccessoraleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    IndivisionSuccessoraleAnalysis a = new IndivisionSuccessoraleAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateOuvertureSuccession(result.dateOuvertureSuccession());
        entity.setTypeIndivision(result.typeIndivision());
        entity.setNbHeritiers(result.nbHeritiers());
        entity.setValeurPatrimoineIndivisEur(result.valeurPatrimoineIndivisEur());
        entity.setValeurBienOccupeEur(result.valeurBienOccupeEur());
        entity.setConsentementsTous(result.consentementsTous());
        entity.setOccupationExclusive(result.occupationExclusive());
        entity.setActesAdministrationContestes(result.actesAdministrationContestes());
        entity.setDemandePartage(result.demandePartage());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public IndivisionSuccessoraleResponse get(UUID caseFileId,
                                              OidcUser oidcUser,
                                              Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        IndivisionSuccessoraleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse d'indivision successorale trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserialize(entity.getResultData()));
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
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de sérialisation");
        }
    }

    private IndivisionSuccessoraleResult deserialize(String json) {
        try { return objectMapper.readValue(json, IndivisionSuccessoraleResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private IndivisionSuccessoraleResponse toResponse(UUID caseFileId, String country,
                                                      IndivisionSuccessoraleResult r) {
        return new IndivisionSuccessoraleResponse(
                caseFileId,
                r.dateOuvertureSuccession(),
                r.typeIndivision(),
                r.nbHeritiers(),
                r.valeurPatrimoineIndivisEur(),
                r.valeurBienOccupeEur(),
                r.consentementsTous(),
                r.occupationExclusive(),
                r.actesAdministrationContestes(),
                r.demandePartage(),
                r.dureeIndivisionMois(),
                r.verdictGestion(),
                r.dispositifRecommande(),
                r.indemniteOccupationDue(),
                r.indemniteOccupationDueEur(),
                r.fraisGestionEstimesEur(),
                r.scoreConflictualite(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                country
        );
    }
}
