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
 * SF-218-31 : service applicatif de l'outil "Accord d'entreprise — validité"
 * (F-DT-67) — apprécie la validité d'un accord d'entreprise au regard des
 * conditions de majorité (art. L.2232-12 CT) et des conditions propres à la
 * révision (parties habilitées, L.2261-7) et à la dénonciation (préavis 3 mois,
 * survie 12 mois). Outil <b>FRANCE UNIQUEMENT</b>, distinct de F-DT-66 (NAO —
 * négociation annuelle obligatoire).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>pourcentageSuffragesSignataires ∈ [0, 100], typeOperation présent et
 *       valide, signePartiesHabilitees présent en révision (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class AccordEntrepriseValiditeService {

    private final AccordEntrepriseValiditeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AccordEntrepriseValiditeService(AccordEntrepriseValiditeRepository repository,
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
    public AccordEntrepriseValiditeResponse analyze(UUID caseFileId,
                                                    AccordEntrepriseValiditeRequest request,
                                                    OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Accord d'entreprise — validité — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        AccordEntrepriseValiditeResult result;
        try {
            result = AccordEntrepriseValiditeAnalyzer.analyze(
                    request.pourcentageSuffragesSignataires(),
                    request.referendumOrganise(),
                    request.referendumApprouve(),
                    request.typeOperation(),
                    request.signePartiesHabilitees(),
                    request.preavisDenonciationRespecte(),
                    request.dateDenonciation());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        AccordEntrepriseValiditeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AccordEntrepriseValiditeAnalysis a = new AccordEntrepriseValiditeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setPourcentageSuffragesSignataires(result.pourcentageSuffragesSignataires());
        entity.setTypeOperation(result.typeOperation());
        entity.setReferendumOrganise(result.referendumOrganise());
        entity.setReferendumApprouve(result.referendumApprouve());
        entity.setConditionMajorite(result.conditionMajorite());
        entity.setDateDenonciation(result.dateDenonciation());
        entity.setDateFinSurvie(result.dateFinSurvie());
        entity.setItemsNonConformes(result.itemsNonConformes());
        entity.setStatut(result.statut());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public AccordEntrepriseValiditeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        AccordEntrepriseValiditeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de validité d'accord d'entreprise trouvée pour ce dossier"));
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

    private AccordEntrepriseValiditeResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, AccordEntrepriseValiditeResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private AccordEntrepriseValiditeResponse toResponse(UUID caseFileId, String country,
                                                        AccordEntrepriseValiditeResult r) {
        return new AccordEntrepriseValiditeResponse(
                caseFileId,
                r.pourcentageSuffragesSignataires(),
                r.typeOperation(),
                r.referendumOrganise(),
                r.referendumApprouve(),
                r.conditionMajorite(),
                r.dateDenonciation(),
                r.dateFinSurvie(),
                r.checklist(),
                r.itemsNonConformes(),
                r.statut(),
                r.consequences(),
                country,
                r.baseJuridique());
    }
}
