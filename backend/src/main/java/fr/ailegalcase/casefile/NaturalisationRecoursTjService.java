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
 * SF-214-29 : service applicatif de l'outil "Recours TJ refus déclaration de
 * nationalité" (Cciv 26-3, délai 6 mois). Outil <b>FRANCE UNIQUEMENT</b>
 * (droit de la nationalité — juridiction civile, distincte du TA de Nantes).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_IMMIGRATION (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class NaturalisationRecoursTjService {

    private final NaturalisationRecoursTjRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public NaturalisationRecoursTjService(NaturalisationRecoursTjRepository repository,
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
    public NaturalisationRecoursTjResponse analyze(UUID caseFileId, NaturalisationRecoursTjRequest request,
                                                   OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Recours TJ refus de déclaration de nationalité (Cciv 26-3) — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        NaturalisationRecoursTjResult result;
        try {
            result = NaturalisationRecoursTjCalculator.compute(
                    request.voieNaturalisation(),
                    request.dateRefusDeclaration(),
                    request.typeRefus());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        NaturalisationRecoursTjAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    NaturalisationRecoursTjAnalysis a = new NaturalisationRecoursTjAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setVoieNaturalisation(result.voieNaturalisation());
        entity.setDateRefusDeclaration(result.dateRefusDeclaration());
        entity.setTypeRefus(result.typeRefus());
        entity.setStatut(result.statut());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public NaturalisationRecoursTjResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        NaturalisationRecoursTjAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de recours TJ naturalisation trouvée pour ce dossier"));
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

    private NaturalisationRecoursTjResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, NaturalisationRecoursTjResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private NaturalisationRecoursTjResponse toResponse(UUID caseFileId, String country,
                                                       NaturalisationRecoursTjResult r) {
        return new NaturalisationRecoursTjResponse(
                caseFileId,
                r.voieNaturalisation(),
                r.dateRefusDeclaration(),
                r.typeRefus(),
                r.dateEcheanceRecoursJudicaire(),
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
