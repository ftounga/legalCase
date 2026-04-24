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
public class Belgian40bisService {

    private final Belgian40bisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public Belgian40bisService(Belgian40bisRepository repository,
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
    public Belgian40bisResponse calculate(UUID caseFileId, Belgian40bisRequest request,
                                          OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "40bis cohabitant UE BE uniquement — en France voir F-IM-03 / F-IM-12 "
                            + "(regroupement familial CESEDA)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.regroupantCitoyenUe() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "regroupantCitoyenUe est requis");
        }
        if (request.ressourcesSuffisantes() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ressourcesSuffisantes est requis");
        }
        if (request.assuranceMaladieUe() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "assuranceMaladieUe est requis");
        }
        if (request.logementSuffisant() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "logementSuffisant est requis");
        }
        if (request.menaceOrdrePublic() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "menaceOrdrePublic est requis");
        }

        Belgian40bisResult result;
        try {
            result = Belgian40bisCalculator.compute(
                    request.lienFamilial(),
                    request.regroupantCitoyenUe(),
                    request.regroupantActiviteCategorie(),
                    request.ressourcesSuffisantes(),
                    request.assuranceMaladieUe(),
                    request.logementSuffisant(),
                    request.menaceOrdrePublic(),
                    request.dateDepotDemande());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        Belgian40bisAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    Belgian40bisAnalysis a = new Belgian40bisAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setLienFamilial(result.lienFamilial());
        entity.setRegroupantCitoyenUe(result.regroupantCitoyenUe());
        entity.setRegroupantActiviteCategorie(result.regroupantActiviteCategorie());
        entity.setRessourcesSuffisantes(result.ressourcesSuffisantes());
        entity.setAssuranceMaladieUe(result.assuranceMaladieUe());
        entity.setLogementSuffisant(result.logementSuffisant());
        entity.setMenaceOrdrePublic(result.menaceOrdrePublic());
        entity.setDateDepotDemande(result.dateDepotDemande());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public Belgian40bisResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        Belgian40bisAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse 40bis BE trouvée pour ce dossier"));
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
        if (!"DROIT_IMMIGRATION".equals(cf.getLegalDomain()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de l'immigration");
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private Belgian40bisResult deserialize(String json) {
        try { return objectMapper.readValue(json, Belgian40bisResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private Belgian40bisResponse toResponse(UUID caseFileId, String country, Belgian40bisResult r) {
        return new Belgian40bisResponse(
                caseFileId,
                r.lienFamilial(),
                r.regroupantCitoyenUe(),
                r.regroupantActiviteCategorie(),
                r.ressourcesSuffisantes(),
                r.assuranceMaladieUe(),
                r.logementSuffisant(),
                r.menaceOrdrePublic(),
                r.dateDepotDemande(),
                country,
                r.lienValide(),
                r.regroupantValide(),
                r.ressourcesOk(),
                r.assuranceOk(),
                r.logementOk(),
                r.pasMenace(),
                r.scoreGlobal(),
                r.verdictProbabilite(),
                r.criteresNonRemplis() != null ? r.criteresNonRemplis() : java.util.List.of(),
                r.dateExpirationInstruction(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : java.util.List.of()
        );
    }
}
