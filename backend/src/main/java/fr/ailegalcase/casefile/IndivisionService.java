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
 * SF-FA-22-01 : service applicatif "Indivision post-communautaire" (art. 815
 * Cciv + 1364 CPC). Gate FR + DROIT_FAMILLE, isolation workspace, persistance
 * JSON.
 */
@Service
public class IndivisionService {

    private final IndivisionRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public IndivisionService(IndivisionRepository repository,
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
    public IndivisionResponse calculate(UUID caseFileId,
                                        IndivisionRequest request,
                                        OidcUser oidcUser,
                                        Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Indivision post-communautaire : procédure propre au droit "
                            + "français (art. 815 Cciv + art. 1364 CPC). "
                            + "BE non couverte par F-FA-22.");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Corps de requête requis");
        }
        if (request.dateOrigineIndivision() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateOrigineIndivision est requise");
        }
        if (request.natureBiens() == null || request.natureBiens().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "natureBiens : au moins une nature requise");
        }
        if (request.valeurEstimeeTotaleEur() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "valeurEstimeeTotaleEur est requise");
        }
        if (request.nbIndivisaires() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "nbIndivisaires est requis");
        }
        if (request.quotesPart() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "quotesPart est requis");
        }
        if (request.tentativesPartageAmiable() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "tentativesPartageAmiable est requis");
        }
        if (request.indivisionDureeAnnees() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "indivisionDureeAnnees est requis");
        }

        boolean consentement = Boolean.TRUE.equals(request.consentementPartageGlobal());
        boolean occupation = Boolean.TRUE.equals(request.occupationBienParUnIndivisaire());
        boolean mesuresConservatoires = Boolean.TRUE.equals(request.demandeMesuresConservatoires());
        boolean conflitOuvert = Boolean.TRUE.equals(request.conflitOuvertEntreIndivisaires());

        IndivisionResult result;
        try {
            result = IndivisionCalculator.compute(
                    request.dateOrigineIndivision(),
                    request.natureBiens(),
                    request.valeurEstimeeTotaleEur(),
                    request.nbIndivisaires(),
                    request.quotesPart(),
                    request.tentativesPartageAmiable(),
                    consentement,
                    occupation,
                    request.indivisionDureeAnnees(),
                    mesuresConservatoires,
                    conflitOuvert);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        IndivisionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    IndivisionAnalysis a = new IndivisionAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateOrigineIndivision(result.dateOrigineIndivision());
        entity.setNatureBiens(String.join(",", result.natureBiens()));
        entity.setValeurEstimeeTotaleEur(result.valeurEstimeeTotaleEur());
        entity.setNbIndivisaires(result.nbIndivisaires());
        entity.setTentativesPartageAmiable(String.join(",", result.tentativesPartageAmiable()));
        entity.setConsentementPartageGlobal(result.consentementPartageGlobal());
        entity.setOccupationBienParUnIndivisaire(result.occupationBienParUnIndivisaire());
        entity.setIndivisionDureeAnnees(result.indivisionDureeAnnees());
        entity.setDemandeMesuresConservatoires(result.demandeMesuresConservatoires());
        entity.setConflitOuvertEntreIndivisaires(result.conflitOuvertEntreIndivisaires());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public IndivisionResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        IndivisionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse d'indivision trouvée pour ce dossier"));
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

    private IndivisionResult deserialize(String json) {
        try { return objectMapper.readValue(json, IndivisionResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private IndivisionResponse toResponse(UUID caseFileId, String country,
                                          IndivisionResult r) {
        return new IndivisionResponse(
                caseFileId,
                r.dateOrigineIndivision(),
                r.natureBiens() != null ? r.natureBiens() : List.of(),
                r.valeurEstimeeTotaleEur(),
                r.nbIndivisaires(),
                r.quotesPart() != null ? r.quotesPart() : List.of(),
                r.tentativesPartageAmiable() != null ? r.tentativesPartageAmiable() : List.of(),
                r.consentementPartageGlobal(),
                r.occupationBienParUnIndivisaire(),
                r.indivisionDureeAnnees(),
                r.demandeMesuresConservatoires(),
                r.conflitOuvertEntreIndivisaires(),
                r.scoreEligibilitePartageJudiciaire(),
                r.verdictRecommandation(),
                r.indemniteOccupationDueEur(),
                r.expertiseNotarialeRecommandee(),
                r.licitationRecommandee(),
                r.delaiProcedurePartageJudiciaireMois(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                country
        );
    }
}
