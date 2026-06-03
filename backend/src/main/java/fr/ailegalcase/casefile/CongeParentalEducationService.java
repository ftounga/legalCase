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
 * SF-218-45 : service applicatif de l'outil "Congé parental d'éducation"
 * (art. L.1225-47 à L.1225-60 CT, F-DT-78) — détermine l'éligibilité (un an
 * d'ancienneté minimum à la naissance / adoption) et la date de fin maximale du
 * droit (3e anniversaire de l'enfant). Outil <b>FRANCE UNIQUEMENT</b>, distinct
 * du congé de paternité/maternité (F-212) et du congé pour évènements familiaux
 * (F-DT-76).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>champs requis présents et valides (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class CongeParentalEducationService {

    private final CongeParentalEducationRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public CongeParentalEducationService(CongeParentalEducationRepository repository,
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
    public CongeParentalEducationResponse analyze(UUID caseFileId,
                                                  CongeParentalEducationRequest request,
                                                  OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Congé parental d'éducation — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        CongeParentalEducationResult result;
        try {
            result = CongeParentalEducationAnalyzer.analyze(
                    request.ancienneteMois(),
                    request.modalite(),
                    request.nombreEnfants(),
                    request.dateNaissanceOuAdoption());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        CongeParentalEducationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    CongeParentalEducationAnalysis a = new CongeParentalEducationAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setStatut(result.statut());
        entity.setAncienneteMois(result.ancienneteMois());
        entity.setModalite(result.modaliteRetenue());
        entity.setNombreEnfants(result.nombreEnfants());
        entity.setDateNaissanceOuAdoption(result.dateNaissanceOuAdoption());
        entity.setDateFinMax(result.dateFinMax());
        entity.setDureeMaxMois(result.dureeMaxMois());
        entity.setProtectionReintegration(result.protectionReintegration());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public CongeParentalEducationResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        CongeParentalEducationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de congé parental d'éducation trouvée pour ce dossier"));
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

    private CongeParentalEducationResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, CongeParentalEducationResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private CongeParentalEducationResponse toResponse(UUID caseFileId, String country,
                                                      CongeParentalEducationResult r) {
        return new CongeParentalEducationResponse(
                caseFileId,
                r.statut(),
                r.ancienneteMois(),
                r.modaliteRetenue(),
                r.nombreEnfants(),
                r.dateNaissanceOuAdoption(),
                r.dateFinMax(),
                r.dureeMaxMois(),
                r.protectionReintegration(),
                r.mentionPreparE(),
                r.notes(),
                country,
                r.baseJuridique());
    }
}
