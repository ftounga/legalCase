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
 * SF-214-21 : service de l'analyse d'éligibilité au titre victime de la traite
 * des êtres humains L. 425-1 CESEDA. Outil single-country FR.
 */
@Service
public class VictimeTraiteService {

    private final VictimeTraiteRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public VictimeTraiteService(VictimeTraiteRepository repository,
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
    public VictimeTraiteResponse analyze(UUID caseFileId, VictimeTraiteRequest request,
                                         OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Victime traite L.425-1 — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.plainteDeposee() == null || request.collaborationOCRTEH() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Champs requis : plainteDeposee, collaborationOCRTEH");
        }

        boolean presenceAutorite = Boolean.TRUE.equals(request.presenceAutoriteRefugieDetectee());

        VictimeTraiteResult result;
        try {
            result = VictimeTraiteAnalyzer.analyze(
                    request.plainteDeposee(),
                    request.collaborationOCRTEH(),
                    request.datePlainte(),
                    request.titreActuel(),
                    presenceAutorite);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        VictimeTraiteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    VictimeTraiteAnalysis a = new VictimeTraiteAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setPlainteDeposee(result.plainteDeposee());
        entity.setCollaborationOcrteh(result.collaborationOCRTEH());
        entity.setDatePlainte(request.datePlainte());
        entity.setTitreActuel(result.titreActuel());
        entity.setPresenceAutoriteRefugieDetectee(result.presenceAutoriteRefugieDetectee());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public VictimeTraiteResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        VictimeTraiteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse victime traite trouvée pour ce dossier"));
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
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        if (!"DROIT_IMMIGRATION".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de l'immigration");
        }
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private VictimeTraiteResult deserialize(String json) {
        try { return objectMapper.readValue(json, VictimeTraiteResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private VictimeTraiteResponse toResponse(UUID caseFileId, String country, VictimeTraiteResult r) {
        return new VictimeTraiteResponse(
                caseFileId,
                r.plainteDeposee(),
                r.collaborationOCRTEH(),
                r.datePlainte(),
                r.titreActuel(),
                r.presenceAutoriteRefugieDetectee(),
                country,
                r.verdict(),
                r.chipsCriteresManquants(),
                r.mesuresProtection(),
                r.risqueVictimeEnDanger(),
                r.recommandations(),
                r.baseJuridique());
    }
}
