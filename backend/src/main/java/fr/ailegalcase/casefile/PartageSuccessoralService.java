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
 * SF-FA-24-09 : service orchestrant l'analyse de la modalité de partage
 * successoral (FR — DROIT_FAMILLE — art. 815-840 Cciv + 1364 CPC).
 */
@Service
public class PartageSuccessoralService {

    private final PartageSuccessoralRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public PartageSuccessoralService(PartageSuccessoralRepository repository,
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
    public PartageSuccessoralResponse calculate(UUID caseFileId,
                                                PartageSuccessoralRequest request,
                                                OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.modePartageDemande() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modalité de partage requise");
        }
        if (request.nombreCoheritiers() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nombre de cohéritiers requis");
        }
        if (request.consentementsTous() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Consentement de tous les héritiers (oui/non) requis");
        }
        if (request.presenceImmeubles() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Présence d'immeubles (oui/non) requise");
        }
        if (request.accordsValuation() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Accord sur les évaluations (oui/non) requis");
        }
        if (request.desaccordPersistant() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Désaccord persistant (oui/non) requis");
        }
        if (request.dateDeces() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date du décès requise");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        PartageSuccessoralResult result;
        try {
            result = PartageSuccessoralCalculator.compute(
                    request.modePartageDemande(),
                    request.nombreCoheritiers(),
                    request.consentementsTous(),
                    request.presenceImmeubles(),
                    request.accordsValuation(),
                    request.desaccordPersistant(),
                    request.dateDeces(),
                    request.valeurMasseEur(),
                    country
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        PartageSuccessoralAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    PartageSuccessoralAnalysis a = new PartageSuccessoralAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setModePartageDemande(result.modePartageDemande());
        entity.setNombreCoheritiers(result.nombreCoheritiers());
        entity.setConsentementsTous(result.consentementsTous());
        entity.setPresenceImmeubles(result.presenceImmeubles());
        entity.setAccordsValuation(result.accordsValuation());
        entity.setDesaccordPersistant(result.desaccordPersistant());
        entity.setDateDeces(result.dateDeces());
        entity.setValeurMasseEur(result.valeurMasseEur());
        entity.setCountry(result.country());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public PartageSuccessoralResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        PartageSuccessoralAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse Partage successoral trouvée pour ce dossier"));
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

    private PartageSuccessoralResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, PartageSuccessoralResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private PartageSuccessoralResponse toResponse(UUID caseFileId, PartageSuccessoralResult r) {
        return new PartageSuccessoralResponse(
                caseFileId,
                r.verdictRecevabilite(),
                r.modeRecommande(),
                r.basculeMode(),
                r.scoreEligibilite(),
                r.delaiInstructionMois(),
                r.fraisEstimesPct(),
                r.fraisEstimesEur(),
                r.risqueLicitation(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                r.country()
        );
    }
}
