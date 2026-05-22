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
 * SF-216-11 : service orchestrant l'outil retrait autorité parentale FR
 * (art. 378-381 Cciv + loi 2022-140 LMVSS). Gate FRANCE + DROIT_FAMILLE.
 */
@Service
public class RetraitAutoriteParentaleService {

    private final RetraitApAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public RetraitAutoriteParentaleService(
            RetraitApAnalysisRepository repository,
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
    public RetraitAutoriteParentaleResponse calculate(UUID caseFileId,
                                                      RetraitAutoriteParentaleRequest request,
                                                      OidcUser oidcUser,
                                                      Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;

        validateRequest(request, country);

        RetraitAutoriteParentaleResult result;
        try {
            result = RetraitAutoriteParentaleCalculator.compute(request, country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        RetraitApAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    RetraitApAnalysis a = new RetraitApAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public RetraitAutoriteParentaleResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        RetraitApAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse retrait autorité parentale trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserializeResult(entity.getResultData()));
    }

    // -----------------------------------------------------------------------
    // Privé
    // -----------------------------------------------------------------------

    private void validateRequest(RetraitAutoriteParentaleRequest req, String country) {
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outil F-FA-RETRAIT-AP applicable uniquement en France (art. 378 Cciv).");
        }
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête manquant.");
        }
        if (req.typeRetrait() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "typeRetrait est requis (TOTAL | PARTIEL_EXERCICE | PARTIEL_ATTRIBUTS).");
        }
        if (req.motifRetrait() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "motifRetrait est requis (CONDAMNATION_PENALE | DANGER_CARACTERISE_VIOLENCES | "
                            + "DESINTERET_GRAVE | COMPORTEMENT_GRAVEMENT_COMPROMETTANT | VIOLENCES_LMVSS_2022).");
        }
        Integer age = req.ageEnfant();
        if (age == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ageEnfant est requis.");
        }
        if (age < 0 || age > 18) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ageEnfant doit être compris entre 0 et 18 ans.");
        }
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
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de la famille.");
        return cf;
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation.");
        }
    }

    private RetraitAutoriteParentaleResult deserializeResult(String json) {
        try {
            return objectMapper.readValue(json, RetraitAutoriteParentaleResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation.");
        }
    }

    private RetraitAutoriteParentaleResponse toResponse(UUID caseFileId, String country,
                                                        RetraitAutoriteParentaleResult r) {
        return new RetraitAutoriteParentaleResponse(
                caseFileId,
                r.verdictRetrait(),
                r.voieProcedurale(),
                r.admissibiliteAdoption(),
                r.consequencesJuridiques(),
                r.etapes(),
                r.dureeEstimeeJours(),
                r.baseLegale(),
                r.messages(),
                r.alertes(),
                country
        );
    }
}
