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
 * SF-207-03 : service orchestrant l'analyse de contestation d'une décision
 * ONEM — gate <b>BELGIQUE strict</b> (404 côté FR pour préserver l'isolation
 * BE-only) + isolation workspace standard.
 *
 * <p>Pattern mirroré de {@link C4OnemChecklistService} (SF-207-02) et
 * {@link PrescriptionBeLitigeTravailService} (SF-207-01) — gate, persistance
 * JSON, sérialisation Jackson. Le calculateur sous-jacent
 * ({@link ContestationC4OnemCalculator}) est une fonction pure indépendante
 * du contexte HTTP / persistance.</p>
 */
@Service
public class ContestationC4OnemService {

    private final ContestationC4OnemRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ContestationC4OnemService(ContestationC4OnemRepository repository,
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
    public ContestationC4OnemResponse calculate(UUID caseFileId,
                                                ContestationC4OnemRequest request,
                                                OidcUser oidcUser,
                                                Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        // Gate BE-only strict : 404 (et pas 400) pour ne pas divulguer l'existence
        // de l'outil côté FR — réponse indistinguable d'un dossier inconnu.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        ContestationC4OnemResult result;
        try {
            result = ContestationC4OnemCalculator.compute(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ContestationC4OnemResponse response = toResponse(caseFileId, country, result);

        ContestationC4OnemAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ContestationC4OnemAnalysis a = new ContestationC4OnemAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public ContestationC4OnemResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        ContestationC4OnemAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de contestation C4 ONEM trouvée pour ce dossier"));
        return deserialize(entity.getResultData());
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private ContestationC4OnemResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, ContestationC4OnemResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private ContestationC4OnemResponse toResponse(UUID caseFileId, String country,
                                                  ContestationC4OnemResult r) {
        return new ContestationC4OnemResponse(
                caseFileId,
                r.dateNotificationDecisionOnem(),
                r.dateActionEnvisagee(),
                r.recoursAdminDejaForme(),
                r.dateDecisionDirecteur(),
                r.verdict(),
                r.paliers(),
                r.etapeSuivante(),
                r.baseJuridique(),
                r.formuleCalcul(),
                country);
    }
}
