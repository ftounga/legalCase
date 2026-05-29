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
 * SF-214-37 : service applicatif de l'outil "ITF judiciaire" — interdiction du
 * territoire français prononcée par un juge pénal comme peine complémentaire
 * (C. pén. 131-30). Outil <b>FRANCE UNIQUEMENT</b> (peine pénale française).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_IMMIGRATION (sinon 400) ;</li>
 *   <li>dateCondamnation requise / non future, infraction ≤ 200 car. (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class ItfJudiciaireService {

    private final ItfJudiciaireRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ItfJudiciaireService(ItfJudiciaireRepository repository,
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
    public ItfJudiciaireResponse analyze(UUID caseFileId, ItfJudiciaireRequest request,
                                         OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ITF judiciaire (C. pén. 131-30) — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.dateCondamnation() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dateCondamnation est requise");
        }

        ItfJudiciaireResult result;
        try {
            result = ItfJudiciaireAnalyzer.analyze(
                    request.dateCondamnation(),
                    request.dureeITFAnnees(),
                    request.infractionPrincipale(),
                    request.condamnationDefinitive(),
                    request.dateEcheanceRecoursPenal());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ItfJudiciaireAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ItfJudiciaireAnalysis a = new ItfJudiciaireAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateCondamnation(result.dateCondamnation());
        entity.setDureeItfAnnees(result.dureeITFAnnees());
        entity.setInfractionPrincipale(result.infractionPrincipale());
        entity.setCondamnationDefinitive(result.condamnationDefinitive());
        entity.setStatut(result.statut());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public ItfJudiciaireResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        ItfJudiciaireAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse ITF judiciaire trouvée pour ce dossier"));
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

    private ItfJudiciaireResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, ItfJudiciaireResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private ItfJudiciaireResponse toResponse(UUID caseFileId, String country,
                                             ItfJudiciaireResult r) {
        return new ItfJudiciaireResponse(
                caseFileId,
                r.dateCondamnation(),
                r.dureeITFAnnees(),
                r.infractionPrincipale(),
                r.condamnationDefinitive(),
                r.dateEcheanceReleve(),
                r.voiesRecours(),
                r.requisReleve(),
                r.distinctionItfVsIrtf(),
                r.statut(),
                country,
                r.baseJuridique());
    }
}
