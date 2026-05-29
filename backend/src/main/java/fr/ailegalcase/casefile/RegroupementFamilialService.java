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
 * SF-214-03 : service de l'analyse d'éligibilité au regroupement familial
 * L. 434-1+ CESEDA. Outil single-country FR.
 */
@Service
public class RegroupementFamilialService {

    private final RegroupementFamilialRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public RegroupementFamilialService(RegroupementFamilialRepository repository,
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
    public RegroupementFamilialResponse analyze(UUID caseFileId, RegroupementFamilialRequest request,
                                                OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Regroupement familial L.434-1 — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.dureeSejourRegulierMois() == null || request.ressourcesMensuellesNettes() == null
                || request.tailleLogementM2() == null || request.nombrePersonnesFoyer() == null
                || request.typeRegroupement() == null || request.membresFamilleARegrouper() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tous les champs sont requis : dureeSejourRegulierMois, ressourcesMensuellesNettes, "
                    + "tailleLogementM2, nombrePersonnesFoyer, typeRegroupement, membresFamilleARegrouper");
        }

        RegroupementFamilialResult result;
        try {
            result = RegroupementFamilialAnalyzer.analyze(
                    request.dureeSejourRegulierMois(),
                    request.ressourcesMensuellesNettes(),
                    request.tailleLogementM2(),
                    request.nombrePersonnesFoyer(),
                    request.typeRegroupement(),
                    request.membresFamilleARegrouper());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        RegroupementFamilialAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    RegroupementFamilialAnalysis a = new RegroupementFamilialAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDureeSejourRegulierMois(result.dureeSejourRegulierMois());
        entity.setRessourcesMensuellesNettes(result.ressourcesMensuellesNettes());
        entity.setTailleLogementM2(result.tailleLogementM2());
        entity.setNombrePersonnesFoyer(result.nombrePersonnesFoyer());
        entity.setTypeRegroupement(result.typeRegroupement());
        entity.setMembresFamilleARegrouper(result.membresFamilleARegrouper());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public RegroupementFamilialResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        RegroupementFamilialAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse regroupement familial trouvée pour ce dossier"));
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

    private RegroupementFamilialResult deserialize(String json) {
        try { return objectMapper.readValue(json, RegroupementFamilialResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private RegroupementFamilialResponse toResponse(UUID caseFileId, String country,
                                                    RegroupementFamilialResult r) {
        return new RegroupementFamilialResponse(
                caseFileId,
                r.dureeSejourRegulierMois(),
                r.ressourcesMensuellesNettes(),
                r.tailleLogementM2(),
                r.nombrePersonnesFoyer(),
                r.typeRegroupement(),
                r.membresFamilleARegrouper(),
                country,
                r.verdict(),
                r.ressourcesRequises(),
                r.surfaceRequise(),
                r.chipsCriteresNonRemplis(),
                r.baseJuridique());
    }
}
