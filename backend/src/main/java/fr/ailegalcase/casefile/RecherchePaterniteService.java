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
 * SF-FA-18-05 : service orchestrant l'analyse de recevabilité d'une
 * action en recherche de paternité (FR — DROIT_FAMILLE — art. 327 + 340 +
 * 16-11 + 321 Cciv).
 */
@Service
public class RecherchePaterniteService {

    private final RecherchePaterniteRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public RecherchePaterniteService(RecherchePaterniteRepository repository,
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
    public RecherchePaterniteResponse calculate(UUID caseFileId,
                                                RecherchePaterniteRequest request,
                                                OidcUser oidcUser,
                                                Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.qualiteDuDemandeur() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Qualité du demandeur requise");
        }
        if (request.dateNaissanceEnfant() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Date de naissance de l'enfant requise");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        RecherchePaterniteResult result;
        try {
            result = RecherchePaterniteCalculator.compute(
                    request.qualiteDuDemandeur(),
                    request.dateNaissanceEnfant(),
                    request.presomptionPossessionEtat(),
                    request.expertiseAdnDemandee(),
                    request.pereDesigneRefuseADN(),
                    request.motifsSerieux(),
                    country
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        RecherchePaterniteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    RecherchePaterniteAnalysis a = new RecherchePaterniteAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setQualiteDuDemandeur(result.qualiteDuDemandeur());
        entity.setDateNaissanceEnfant(result.dateNaissanceEnfant());
        entity.setPresomptionPossessionEtat(result.presomptionPossessionEtat());
        entity.setExpertiseAdnDemandee(result.expertiseAdnDemandee());
        entity.setPereDesigneRefuseADN(result.pereDesigneRefuseADN());
        entity.setMotifsSerieux(result.motifsSerieux());
        entity.setCountry(result.country());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public RecherchePaterniteResponse get(UUID caseFileId,
                                          OidcUser oidcUser,
                                          Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        RecherchePaterniteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse Recherche paternité trouvée pour ce dossier"));
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

    private RecherchePaterniteResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, RecherchePaterniteResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private RecherchePaterniteResponse toResponse(UUID caseFileId,
                                                  RecherchePaterniteResult r) {
        return new RecherchePaterniteResponse(
                caseFileId,
                r.qualiteDuDemandeur(),
                r.verdictRecevabilite(),
                r.scoreRecevabilite(),
                r.delaiPrescriptionAns(),
                r.delaiPrescriptionRestantMois(),
                r.expertiseAdnRecommandee(),
                r.presomptionRefusADN(),
                r.risquesRefus() != null ? r.risquesRefus() : List.of(),
                r.documentsRequis() != null ? r.documentsRequis() : List.of(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                r.country()
        );
    }
}
