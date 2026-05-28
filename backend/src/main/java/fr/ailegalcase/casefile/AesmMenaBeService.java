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
 * SF-215-11 : service applicatif de l'outil composite AESM + tutelle MENA BE —
 * F-IM-30-aesm-mena-be (Loi 04/05/2007 tutelle MENA + loi 15/12/1980 art. 9bis
 * adapté MENA + circulaire OE 15/09/2005).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = BELGIQUE (sinon 400 — outil BE-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_IMMIGRATION (sinon 400) ;</li>
 *   <li>{@code ageActuel < 18} (sinon 400 — porté par {@link AesmMenaBeCalculator}) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 *
 * <p>Pattern miroir de {@link NaturalisationConjointBelgeBeService} (SF-215-09).
 */
@Service
public class AesmMenaBeService {

    private final AesmMenaBeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AesmMenaBeService(
            AesmMenaBeRepository repository,
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
    public AesmMenaBeResponse calculate(UUID caseFileId,
                                        AesmMenaBeRequest request,
                                        OidcUser oidcUser,
                                        Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outil AESM/MENA BE uniquement — aucun équivalent FR (MNA FR voir "
                            + "F-IM-19 mineurs : ordonnance JE + L.435-3 CESEDA)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Corps de requête requis");
        }

        AesmMenaBeResult result;
        try {
            result = AesmMenaBeCalculator.compute(
                    request.ageActuel(),
                    request.dateArriveeBelgique(),
                    request.tuteurDesigne(),
                    request.integrationScolaire(),
                    request.dureeScolaire(),
                    request.projetVieElabore(),
                    request.perspectiveAutonomie(),
                    request.menaceOrdrePublic());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        AesmMenaBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AesmMenaBeAnalysis a = new AesmMenaBeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setAgeActuel(result.ageActuel());
        entity.setDateArriveeBelgique(result.dateArriveeBelgique());
        entity.setTuteurDesigne(result.tuteurDesigne());
        entity.setIntegrationScolaire(result.integrationScolaire());
        entity.setDureeScolaire(result.dureeScolaire());
        entity.setProjetVieElabore(result.projetVieElabore());
        entity.setPerspectiveAutonomie(result.perspectiveAutonomie());
        entity.setMenaceOrdrePublic(result.menaceOrdrePublic());
        entity.setScoreIntegration(result.scoreIntegration());
        entity.setVerdictAesm(result.verdictAESM());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public AesmMenaBeResponse get(UUID caseFileId,
                                  OidcUser oidcUser,
                                  Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        AesmMenaBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse AESM/MENA BE trouvée pour ce dossier"));
        return toResponse(caseFileId, deserialize(entity.getResultData()));
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(
                oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId()))
                .orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        if (!"DROIT_IMMIGRATION".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de l'immigration");
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

    private AesmMenaBeResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, AesmMenaBeResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private AesmMenaBeResponse toResponse(UUID caseFileId, AesmMenaBeResult r) {
        return new AesmMenaBeResponse(
                caseFileId,
                r.ageActuel(),
                r.dateArriveeBelgique(),
                r.tuteurDesigne(),
                r.integrationScolaire(),
                r.dureeScolaire(),
                r.projetVieElabore(),
                r.perspectiveAutonomie(),
                r.menaceOrdrePublic(),
                r.etapeTutelle(),
                r.delaiDesignationTuteur(),
                r.scoreIntegration(),
                r.bonus(),
                r.verdictAESM(),
                r.prioriteUrgence(),
                r.criteresNonRemplis() != null ? r.criteresNonRemplis() : java.util.List.of(),
                r.prochainActe(),
                r.baseJuridique()
        );
    }
}
