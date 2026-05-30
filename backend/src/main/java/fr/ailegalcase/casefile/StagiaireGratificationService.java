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
 * SF-218-21 : service applicatif de l'outil "Stagiaire — gratification /
 * requalification" (F-DT-109) — vérifie le seuil de gratification minimale
 * obligatoire (au-delà de 2 mois / 44 jours de présence), calcule le rappel
 * exigible et apprécie le risque de requalification du stage en contrat de
 * travail (CDI). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>dates requises et cohérentes, jours / montants ≥ 0 (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class StagiaireGratificationService {

    private final StagiaireGratificationRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public StagiaireGratificationService(StagiaireGratificationRepository repository,
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
    public StagiaireGratificationResponse analyze(UUID caseFileId,
                                                  StagiaireGratificationRequest request,
                                                  OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Stagiaire — gratification / requalification — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        StagiaireGratificationResult result;
        try {
            result = StagiaireGratificationAnalyzer.analyze(
                    request.dateDebutStage(),
                    request.dateFinStage(),
                    request.nombreJoursPresence(),
                    request.gratificationMensuelleVersee(),
                    request.tauxHoraireConventionnel(),
                    request.missionsHorsProjetPedagogique(),
                    request.posteTravailPermanent());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        StagiaireGratificationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    StagiaireGratificationAnalysis a = new StagiaireGratificationAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateDebutStage(result.dateDebutStage());
        entity.setDateFinStage(result.dateFinStage());
        entity.setNombreJoursPresence(result.nombreJoursPresence());
        entity.setSeuilAtteint(result.seuilAtteint());
        entity.setGratificationObligatoire(result.gratificationObligatoire());
        entity.setGratificationMinimaleDue(result.gratificationMinimaleDue());
        entity.setRappelGratification(result.rappelGratification());
        entity.setDepassementDureeMax(result.depassementDureeMax());
        entity.setRisqueRequalification(result.risqueRequalification());
        entity.setVerdictGlobal(result.verdictGlobal());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public StagiaireGratificationResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        StagiaireGratificationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse stagiaire trouvée pour ce dossier"));
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

    private StagiaireGratificationResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, StagiaireGratificationResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private StagiaireGratificationResponse toResponse(UUID caseFileId, String country,
                                                      StagiaireGratificationResult r) {
        return new StagiaireGratificationResponse(
                caseFileId,
                r.dateDebutStage(),
                r.dateFinStage(),
                r.nombreJoursPresence(),
                r.heuresPresence(),
                r.dureeStageJours(),
                r.seuilAtteint(),
                r.gratificationObligatoire(),
                r.tauxHoraireApplique(),
                r.gratificationMinimaleDue(),
                r.gratificationVerseeTotale(),
                r.rappelGratification(),
                r.depassementDureeMax(),
                r.missionsHorsProjetPedagogique(),
                r.posteTravailPermanent(),
                r.risqueRequalification(),
                r.motifs(),
                r.verdictGlobal(),
                country,
                r.baseJuridique());
    }
}
