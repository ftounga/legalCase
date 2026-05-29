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
 * SF-214-23 : service de l'analyse d'éligibilité à la carte de résident 10 ans
 * L. 426-1 CESEDA. Outil single-country FR.
 */
@Service
public class CarteResidentService {

    private final CarteResidentRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public CarteResidentService(CarteResidentRepository repository,
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
    public CarteResidentResponse analyze(UUID caseFileId, CarteResidentRequest request,
                                         OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Carte de résident L.426-1 — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.dureeSejourRegulierAnnees() == null || request.niveauIntegration() == null
                || request.ressourcesMensuellesNettes() == null
                || request.condamnationsPenalesGraves() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Champs requis : dureeSejourRegulierAnnees, niveauIntegration, "
                    + "ressourcesMensuellesNettes, condamnationsPenalesGraves "
                    + "(typesTitresAnterieurs optionnel)");
        }

        CarteResidentResult result;
        try {
            result = CarteResidentAnalyzer.analyze(
                    request.dureeSejourRegulierAnnees(),
                    request.typesTitresAnterieurs(),
                    request.niveauIntegration(),
                    request.ressourcesMensuellesNettes(),
                    request.condamnationsPenalesGraves());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        CarteResidentAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    CarteResidentAnalysis a = new CarteResidentAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDureeSejourRegulierAnnees(result.dureeSejourRegulierAnnees());
        entity.setTypesTitresAnterieurs(result.typesTitresAnterieurs());
        entity.setNiveauIntegration(result.niveauIntegration());
        entity.setRessourcesMensuellesNettes(result.ressourcesMensuellesNettes());
        entity.setCondamnationsPenalesGraves(result.condamnationsPenalesGraves());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public CarteResidentResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        CarteResidentAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse carte de résident trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserialize(entity.getResultData()));
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
        if (!"DROIT_IMMIGRATION".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de l'immigration");
        }
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private CarteResidentResult deserialize(String json) {
        try { return objectMapper.readValue(json, CarteResidentResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private CarteResidentResponse toResponse(UUID caseFileId, String country, CarteResidentResult r) {
        return new CarteResidentResponse(
                caseFileId,
                r.dureeSejourRegulierAnnees(),
                r.typesTitresAnterieurs(),
                r.niveauIntegration(),
                r.ressourcesMensuellesNettes(),
                r.condamnationsPenalesGraves(),
                country,
                r.verdict(),
                r.smicMensuelNetReference(),
                r.chipsCriteresNonRemplis(),
                r.atouts(),
                r.baseJuridique());
    }
}
