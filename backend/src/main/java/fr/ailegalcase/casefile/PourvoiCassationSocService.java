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
 * SF-218-05 : service applicatif de l'outil "Pourvoi en cassation sociale" —
 * cas d'ouverture, délai de 2 mois et filtre de non-admission d'un pourvoi
 * devant la chambre sociale de la Cour de cassation (art. 612 CPC ; art. 604
 * CPC ; art. 973 CPC ; art. 1014 CPC). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>dateNotificationArret requise / non future, casOuverture non vide
 *       (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class PourvoiCassationSocService {

    private final PourvoiCassationSocRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public PourvoiCassationSocService(PourvoiCassationSocRepository repository,
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
    public PourvoiCassationSocResponse analyze(UUID caseFileId, PourvoiCassationSocRequest request,
                                               OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pourvoi en cassation sociale (art. 612 CPC) — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        PourvoiCassationSocResult result;
        try {
            result = PourvoiCassationSocAnalyzer.analyze(
                    request.dateNotificationArret(),
                    request.casOuverture(),
                    request.representationAvocatCassation(),
                    request.moyenSerieuxIdentifie());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        PourvoiCassationSocAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    PourvoiCassationSocAnalysis a = new PourvoiCassationSocAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateNotificationArret(result.dateNotificationArret());
        entity.setDateLimitePourvoi(result.dateLimitePourvoi());
        entity.setVerdictDelai(result.verdictDelai());
        entity.setRisqueNonAdmission(result.risqueNonAdmission());
        entity.setRepresentationAvocatCassation(result.representationAvocatCassation());
        entity.setMoyenSerieuxIdentifie(result.moyenSerieuxIdentifie());
        entity.setVerdict(result.verdict());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public PourvoiCassationSocResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        PourvoiCassationSocAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de pourvoi en cassation sociale trouvée pour ce dossier"));
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

    private PourvoiCassationSocResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, PourvoiCassationSocResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private PourvoiCassationSocResponse toResponse(UUID caseFileId, String country,
                                                   PourvoiCassationSocResult r) {
        return new PourvoiCassationSocResponse(
                caseFileId,
                r.dateNotificationArret(),
                r.dateLimitePourvoi(),
                r.joursRestants(),
                r.verdictDelai(),
                r.casOuvertureAnalyses(),
                r.risqueNonAdmission(),
                r.representationAvocatCassation(),
                r.moyenSerieuxIdentifie(),
                r.verdict(),
                r.checklist(),
                country,
                r.baseJuridique());
    }
}
