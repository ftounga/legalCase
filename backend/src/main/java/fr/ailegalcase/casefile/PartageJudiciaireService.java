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
 * SF-FA-17-01 : service orchestrant le calcul de recevabilité du partage
 * judiciaire (FR — DROIT_FAMILLE — art. 840 et s. Cciv + 1364 et s. CPC).
 */
@Service
public class PartageJudiciaireService {

    private final PartageJudiciaireRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public PartageJudiciaireService(PartageJudiciaireRepository repository,
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
    public PartageJudiciaireResponse calculate(UUID caseFileId,
                                               PartageJudiciaireRequest request,
                                               OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.etapeProcedure() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Étape de procédure requise");
        }
        if (request.typeBienIndivision() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Type de bien en indivision requis");
        }
        if (request.nombreCoindivisaires() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nombre de co-indivisaires requis");
        }
        if (request.valeurEstimeeBiensEur() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Valeur estimée des biens requise");
        }
        if (request.pvDifficultesEtabli() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "PV de difficultés (oui/non) requis");
        }
        if (request.tentativeAmiableEpuiseuee() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tentative amiable (oui/non) requise");
        }
        if (request.desaccordMotive() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Désaccord motivé (oui/non) requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        PartageJudiciaireResult result;
        try {
            result = PartageJudiciaireCalculator.compute(
                    request.etapeProcedure(),
                    request.typeBienIndivision(),
                    request.nombreCoindivisaires(),
                    request.valeurEstimeeBiensEur(),
                    request.pvDifficultesEtabli(),
                    request.tentativeAmiableEpuiseuee(),
                    request.desaccordMotive(),
                    country
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        PartageJudiciaireAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    PartageJudiciaireAnalysis a = new PartageJudiciaireAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setEtapeProcedure(result.etapeProcedure());
        entity.setTypeBienIndivision(result.typeBienIndivision());
        entity.setNombreCoindivisaires(result.nombreCoindivisaires());
        entity.setValeurEstimeeBiensEur(result.valeurEstimeeBiensEur());
        entity.setPvDifficultesEtabli(result.pvDifficultesEtabli());
        entity.setTentativeAmiableEpuiseuee(result.tentativeAmiableEpuiseuee());
        entity.setDesaccordMotive(result.desaccordMotive());
        entity.setCountry(result.country());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public PartageJudiciaireResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        PartageJudiciaireAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse Partage judiciaire trouvée pour ce dossier"));
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

    private PartageJudiciaireResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, PartageJudiciaireResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private PartageJudiciaireResponse toResponse(UUID caseFileId, PartageJudiciaireResult r) {
        return new PartageJudiciaireResponse(
                caseFileId,
                r.verdictRecevabilite(),
                r.scoreEligibilite(),
                r.dureeProcedureMois(),
                r.fraisEstimesEur(),
                r.risqueLicitation(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                r.country()
        );
    }
}
