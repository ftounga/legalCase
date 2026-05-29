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
 * SF-214-31 : service applicatif de l'outil "Recours TA Nantes refus de
 * naturalisation par décret" (CJA L. 213-1, délai 2 mois ; Cciv 21-15). Outil
 * <b>FRANCE UNIQUEMENT</b> (refus décret de naturalisation — juridiction
 * administrative, compétence exclusive du TA de Nantes ; distinct du recours TJ
 * refus de déclaration de nationalité, SF-214-29).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_IMMIGRATION (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class NaturalisationRecoursTaNantesService {

    private final NaturalisationRecoursTaNantesRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public NaturalisationRecoursTaNantesService(NaturalisationRecoursTaNantesRepository repository,
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
    public NaturalisationRecoursTaNantesResponse analyze(UUID caseFileId,
                                                         NaturalisationRecoursTaNantesRequest request,
                                                         OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Recours TA Nantes refus de naturalisation par décret (CJA L. 213-1) — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        NaturalisationRecoursTaNantesResult result;
        try {
            result = NaturalisationRecoursTaNantesCalculator.compute(
                    request.dateRefusDecret(),
                    request.motivationRefus(),
                    request.recoursPrerequis());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        NaturalisationRecoursTaNantesAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    NaturalisationRecoursTaNantesAnalysis a = new NaturalisationRecoursTaNantesAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateRefusDecret(result.dateRefusDecret());
        entity.setRecoursPrerequis(result.recoursPrerequis());
        entity.setStatut(result.statut());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public NaturalisationRecoursTaNantesResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        NaturalisationRecoursTaNantesAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de recours TA Nantes naturalisation trouvée pour ce dossier"));
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private NaturalisationRecoursTaNantesResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, NaturalisationRecoursTaNantesResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private NaturalisationRecoursTaNantesResponse toResponse(UUID caseFileId, String country,
                                                            NaturalisationRecoursTaNantesResult r) {
        return new NaturalisationRecoursTaNantesResponse(
                caseFileId,
                r.dateRefusDecret(),
                r.motivationRefus(),
                r.recoursPrerequis(),
                r.dateEcheanceRecoursTa(),
                r.joursRestants(),
                r.tribunalCompetent(),
                r.basesJuridiques(),
                r.motifsRecoursDisponibles(),
                r.statut(),
                r.messagePrescription(),
                r.recommandation(),
                country
        );
    }
}
