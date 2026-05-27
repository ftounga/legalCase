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
 * SF-213-07 : service orchestrant l'analyse de la procédure interne BE de
 * plainte pour harcèlement moral / sexuel (Loi 04/08/1996 art. 32bis-32sexies
 * + AR 10/04/2014).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable d'un
 * dossier inconnu, pattern miroir SF-213-05/06) + isolation workspace
 * standard + persistance JSON snapshot.</p>
 *
 * <p>Le checker sous-jacent
 * ({@link HarcelementBeProcedureFormelleProcedureChecker}) est une fonction
 * pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class HarcelementBeProcedureFormelleService {

    private final HarcelementBeProcedureFormelleAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public HarcelementBeProcedureFormelleService(
            HarcelementBeProcedureFormelleAnalysisRepository repository,
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
    public HarcelementBeProcedureFormelleResponse analyze(
            UUID caseFileId,
            HarcelementBeProcedureFormelleRequest request,
            OidcUser oidcUser,
            Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Corps de requête requis");
        }
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        // Gate BE-only strict : 404 (et pas 400) pour ne pas divulguer
        // l'existence de l'outil côté FR — réponse indistinguable d'un
        // dossier inconnu.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        HarcelementBeProcedureFormelleResult result;
        try {
            result = HarcelementBeProcedureFormelleProcedureChecker.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        HarcelementBeProcedureFormelleResponse response = toResponse(caseFileId, result);

        HarcelementBeProcedureFormelleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    HarcelementBeProcedureFormelleAnalysis a = new HarcelementBeProcedureFormelleAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public HarcelementBeProcedureFormelleResponse get(
            UUID caseFileId,
            OidcUser oidcUser,
            Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        HarcelementBeProcedureFormelleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse harcèlement BE procédure formelle "
                                + "trouvée pour ce dossier"));
        return deserialize(entity.getResultData());
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }
        if (!"DROIT_DU_TRAVAIL".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit du travail");
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

    private HarcelementBeProcedureFormelleResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json,
                    HarcelementBeProcedureFormelleResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private HarcelementBeProcedureFormelleResponse toResponse(
            UUID caseFileId, HarcelementBeProcedureFormelleResult r) {
        return new HarcelementBeProcedureFormelleResponse(
                caseFileId,
                r.typeHarcelement(),
                r.etapeProcedure(),
                r.dateDepotPlainte(),
                r.entreprisePossedeCPAP(),
                r.entrepriseTaille(),
                r.mesureDefavorableApres(),
                r.delaiDepuisDepotJours(),
                r.checklistItems(),
                r.representaillesPossibles(),
                r.dateDebutProtectionRepresailles(),
                r.dateFinProtectionRepresailles(),
                r.prochainDelaiFatal(),
                r.baseJuridique(),
                r.avertissement());
    }
}
