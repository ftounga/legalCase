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
 * SF-FA-18-03 : service orchestrant l'analyse de recevabilité d'une
 * contestation de paternité (FR — DROIT_FAMILLE — art. 332-335 + 311-1 + 321
 * + 372 Cciv).
 */
@Service
public class ContestationPaterniteService {

    private final ContestationPaterniteRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ContestationPaterniteService(ContestationPaterniteRepository repository,
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
    public ContestationPaterniteResponse calculate(UUID caseFileId,
                                                   ContestationPaterniteRequest request,
                                                   OidcUser oidcUser,
                                                   Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.qualiteAagir() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Qualité à agir requise");
        }
        if (request.dateEtablissementFiliation() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Date d'établissement de la filiation contestée requise");
        }
        if (request.dateConnaissanceVerite() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Date de connaissance de la non-filiation requise");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        ContestationPaterniteResult result;
        try {
            result = ContestationPaterniteCalculator.compute(
                    request.qualiteAagir(),
                    request.dateEtablissementFiliation(),
                    request.dateConnaissanceVerite(),
                    request.dateMajoriteEnfant(),
                    request.possessionEtatConforme5Ans(),
                    request.expertiseAdnDemandee(),
                    request.motifsSerieux(),
                    country
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ContestationPaterniteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ContestationPaterniteAnalysis a = new ContestationPaterniteAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setQualiteAagir(result.qualiteAagir());
        entity.setDateEtablissementFiliation(result.dateEtablissementFiliation());
        entity.setDateConnaissanceVerite(result.dateConnaissanceVerite());
        entity.setDateMajoriteEnfant(result.dateMajoriteEnfant());
        entity.setPossessionEtatConforme5Ans(result.possessionEtatConforme5Ans());
        entity.setExpertiseAdnDemandee(result.expertiseAdnDemandee());
        entity.setMotifsSerieux(result.motifsSerieux());
        entity.setCountry(result.country());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public ContestationPaterniteResponse get(UUID caseFileId,
                                             OidcUser oidcUser,
                                             Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        ContestationPaterniteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse Contestation paternité trouvée pour ce dossier"));
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de sérialisation");
        }
    }

    private ContestationPaterniteResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, ContestationPaterniteResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private ContestationPaterniteResponse toResponse(UUID caseFileId,
                                                     ContestationPaterniteResult r) {
        return new ContestationPaterniteResponse(
                caseFileId,
                r.qualiteAagir(),
                r.verdictRecevabilite(),
                r.scoreRecevabilite(),
                r.delaiPrescriptionAns(),
                r.delaiPrescriptionRestantMois(),
                r.expertiseAdnRecommandee(),
                r.risquesRefus() != null ? r.risquesRefus() : List.of(),
                r.documentsRequis() != null ? r.documentsRequis() : List.of(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                r.country()
        );
    }
}
