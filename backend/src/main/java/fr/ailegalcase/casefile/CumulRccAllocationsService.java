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
 * SF-219-04 : service orchestrant l'analyse du cumul RCC BE + allocations
 * chômage / pension (CCT n° 17 du 19/12/1974 + AR 25/11/1991 + AR
 * 03/05/2007 art. 22 et suiv.).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-219-02) + isolation workspace
 * standard + persistance JSON snapshot.</p>
 *
 * <p>Le calculator sous-jacent ({@link CumulRccAllocationsCalculator})
 * est une fonction pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class CumulRccAllocationsService {

    private final CumulRccAllocationsAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public CumulRccAllocationsService(
            CumulRccAllocationsAnalysisRepository repository,
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
    public CumulRccAllocationsResponse analyze(
            UUID caseFileId,
            CumulRccAllocationsRequest request,
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
        // dossier inconnu (aucun équivalent FR du RCC).
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        CumulRccAllocationsResult result;
        try {
            result = CumulRccAllocationsCalculator.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        CumulRccAllocationsResponse response = toResponse(caseFileId, result);

        CumulRccAllocationsAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    CumulRccAllocationsAnalysis a = new CumulRccAllocationsAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public CumulRccAllocationsResponse get(
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

        CumulRccAllocationsAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de cumul RCC + allocations trouvée pour "
                                + "ce dossier"));
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

    private CumulRccAllocationsResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, CumulRccAllocationsResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private CumulRccAllocationsResponse toResponse(
            UUID caseFileId, CumulRccAllocationsResult r) {
        return new CumulRccAllocationsResponse(
                caseFileId,
                r.dateNaissance(),
                r.dateDebutRcc(),
                r.allocationChomageMensuelle(),
                r.indemniteComplementaireMensuelle(),
                r.remunerationNetteReference(),
                r.anneesCarriereTotale(),
                r.activiteComplementaire(),
                r.ageLegalPension(),
                r.verdict(),
                r.conforme(),
                r.ageADateDebutRcc(),
                r.cumulMensuelTotal(),
                r.plafondRespecte(),
                r.montantReductionIndemnite(),
                r.regimeDisponibilite(),
                r.ageLegalPensionAtteint(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}
