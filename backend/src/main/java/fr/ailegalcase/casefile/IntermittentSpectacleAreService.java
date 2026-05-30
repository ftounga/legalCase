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
 * SF-218-17 : service applicatif de l'outil "Intermittent du spectacle —
 * ouverture des droits ARE" — vérifie le seuil d'affiliation de 507 heures sur
 * 12 mois (annexes 8 et 10 Unedic), convertit les cachets et écrête les heures
 * de formation, puis détermine l'ouverture ou non des droits. Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>annexe présente, dateFinContrat non future, heures ≥ 0 (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class IntermittentSpectacleAreService {

    private final IntermittentSpectacleAreRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public IntermittentSpectacleAreService(IntermittentSpectacleAreRepository repository,
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
    public IntermittentSpectacleAreResponse analyze(UUID caseFileId,
                                                    IntermittentSpectacleAreRequest request,
                                                    OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Intermittent du spectacle — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        IntermittentSpectacleAreResult result;
        try {
            result = IntermittentSpectacleAreAnalyzer.analyze(
                    request.annexe(),
                    request.dateFinContrat(),
                    request.heuresTravaillees12Mois(),
                    request.nombreCachets(),
                    request.heuresFormationDispensees());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        IntermittentSpectacleAreAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    IntermittentSpectacleAreAnalysis a = new IntermittentSpectacleAreAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setAnnexe(result.annexe());
        entity.setDateFinContrat(result.dateFinContrat());
        entity.setHeuresTravaillees12Mois(result.heuresTravaillees12Mois());
        entity.setHeuresCachets(result.heuresCachets());
        entity.setHeuresFormationRetenues(result.heuresFormationRetenues());
        entity.setHeuresTotalesRetenues(result.heuresTotalesRetenues());
        entity.setHeuresManquantes(result.heuresManquantes());
        entity.setHeuresExcedentaires(result.heuresExcedentaires());
        entity.setDateProchainExamen(result.dateProchainExamen());
        entity.setOuvertureDroits(result.ouvertureDroits());
        entity.setStatut(result.statut());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public IntermittentSpectacleAreResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        IntermittentSpectacleAreAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse intermittent spectacle ARE trouvée pour ce dossier"));
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

    private IntermittentSpectacleAreResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, IntermittentSpectacleAreResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private IntermittentSpectacleAreResponse toResponse(UUID caseFileId, String country,
                                                        IntermittentSpectacleAreResult r) {
        return new IntermittentSpectacleAreResponse(
                caseFileId,
                r.annexe(),
                r.dateFinContrat(),
                r.heuresTravaillees12Mois(),
                r.heuresCachets(),
                r.heuresFormationRetenues(),
                r.heuresTotalesRetenues(),
                r.seuilHeures(),
                r.heuresManquantes(),
                r.heuresExcedentaires(),
                r.dateProchainExamen(),
                r.ouvertureDroits(),
                r.statut(),
                country,
                r.baseJuridique());
    }
}
