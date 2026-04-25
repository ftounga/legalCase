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

@Service
public class AvantagesConventionnelsBeService {

    private final AvantagesConventionnelsBeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AvantagesConventionnelsBeService(AvantagesConventionnelsBeRepository repository,
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
    public AvantagesConventionnelsBeResponse calculate(UUID caseFileId,
                                                       AvantagesConventionnelsBeRequest request,
                                                       OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Avantages conventionnels BE uniquement (pécule, prime, éco-chèques, chèques-repas)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.salaireMensuelBrutEur() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "salaireMensuelBrutEur est requis");
        }
        if (request.joursTravaillesAnneePrecedente() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "joursTravaillesAnneePrecedente est requis");
        }
        if (request.anciennetteMois() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "anciennetteMois est requis");
        }
        if (request.annee() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "annee est requise");
        }
        if (request.joursPrestesEffectifs() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "joursPrestesEffectifs est requis");
        }

        AvantagesConventionnelsBeResult result;
        try {
            result = AvantagesConventionnelsBeCalculator.compute(
                    request.salaireMensuelBrutEur(),
                    request.joursTravaillesAnneePrecedente(),
                    request.anciennetteMois(),
                    request.commissionParitaire(),
                    request.annee(),
                    Boolean.TRUE.equals(request.doublePeculeVacancesPercu()),
                    Boolean.TRUE.equals(request.primeFinAnneePrevueCcCp()),
                    Boolean.TRUE.equals(request.ecoChequesPrevuCcCp()),
                    Boolean.TRUE.equals(request.ecoChequesUtilisationDansAn()),
                    Boolean.TRUE.equals(request.chequesRepasPrevu()),
                    request.joursPrestesEffectifs());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        AvantagesConventionnelsBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AvantagesConventionnelsBeAnalysis a = new AvantagesConventionnelsBeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setSalaireMensuelBrutEur(result.salaireMensuelBrutEur());
        entity.setJoursTravaillesAnneePrecedente(result.joursTravaillesAnneePrecedente());
        entity.setAnciennetteMois(result.anciennetteMois());
        entity.setCommissionParitaire(result.commissionParitaire());
        entity.setAnnee(result.annee());
        entity.setDoublePeculeVacancesPercu(result.doublePeculeVacancesPercu());
        entity.setPrimeFinAnneePrevueCcCp(result.primeFinAnneePrevueCcCp());
        entity.setEcoChequesPrevuCcCp(result.ecoChequesPrevuCcCp());
        entity.setEcoChequesUtilisationDansAn(result.ecoChequesUtilisationDansAn());
        entity.setChequesRepasPrevu(result.chequesRepasPrevu());
        entity.setJoursPrestesEffectifs(result.joursPrestesEffectifs());
        entity.setCountry(result.country());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public AvantagesConventionnelsBeResponse get(UUID caseFileId,
                                                 OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        AvantagesConventionnelsBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse avantages conventionnels BE trouvée pour ce dossier"));
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
        if (!"DROIT_DU_TRAVAIL".equals(cf.getLegalDomain()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit du travail");
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private AvantagesConventionnelsBeResult deserialize(String json) {
        try { return objectMapper.readValue(json, AvantagesConventionnelsBeResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private AvantagesConventionnelsBeResponse toResponse(UUID caseFileId,
                                                         AvantagesConventionnelsBeResult r) {
        return new AvantagesConventionnelsBeResponse(
                caseFileId,
                r.salaireMensuelBrutEur(),
                r.joursTravaillesAnneePrecedente(),
                r.anciennetteMois(),
                r.commissionParitaire(),
                r.annee(),
                r.doublePeculeVacancesPercu(),
                r.primeFinAnneePrevueCcCp(),
                r.ecoChequesPrevuCcCp(),
                r.ecoChequesUtilisationDansAn(),
                r.chequesRepasPrevu(),
                r.joursPrestesEffectifs(),
                r.peculeVacancesSimpleEur(),
                r.doublePeculeVacancesEur(),
                r.primeFinAnneeEur(),
                r.ecoChequesValeurAnnuelleEur(),
                r.chequesRepasValeurAnnuelleEur(),
                r.totalAvantagesAnnuelsEur(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : java.util.List.of(),
                r.country());
    }
}
