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
 * SF-214-43 : service applicatif de l'outil "Autorisation de travail employeur"
 * (L. 5221-1 Code du travail). Outil <b>FRANCE UNIQUEMENT</b> (droit des
 * étrangers — côté employeur).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_IMMIGRATION (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class AutorisationTravailEmployeurService {

    private final AutorisationTravailEmployeurRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AutorisationTravailEmployeurService(
            AutorisationTravailEmployeurRepository repository,
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
    public AutorisationTravailEmployeurResponse analyze(UUID caseFileId,
                                                        AutorisationTravailEmployeurRequest request,
                                                        OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Autorisation de travail employeur (L. 5221-1) — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        AutorisationTravailEmployeurResult result;
        try {
            result = new AutorisationTravailEmployeurAnalyzer().analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        AutorisationTravailEmployeurAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AutorisationTravailEmployeurAnalysis a = new AutorisationTravailEmployeurAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setTypeContrat(result.typeContrat());
        entity.setPosteProposes(result.posteProposes());
        entity.setNationaliteCandidat(result.nationaliteCandidat());
        entity.setDureeContratMois(result.dureeContratMois());
        entity.setRefusAutorisation(result.refusAutorisation());
        entity.setDateRefusAutorisation(result.dateRefusAutorisation());
        entity.setStatut(result.statut());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public AutorisationTravailEmployeurResponse get(UUID caseFileId, OidcUser oidcUser,
                                                    Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        AutorisationTravailEmployeurAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse d'autorisation de travail employeur trouvée pour ce dossier"));
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

    private AutorisationTravailEmployeurResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, AutorisationTravailEmployeurResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private AutorisationTravailEmployeurResponse toResponse(UUID caseFileId, String country,
                                                           AutorisationTravailEmployeurResult r) {
        return new AutorisationTravailEmployeurResponse(
                caseFileId,
                r.typeContrat(),
                r.posteProposes(),
                r.nationaliteCandidat(),
                r.dureeContratMois(),
                r.autorisationRequise(),
                r.obligationsDemande(),
                r.delaiInstructionOFIIMois(),
                r.taxeOFII(),
                r.refusAutorisation(),
                r.dateRefusAutorisation(),
                r.recoursPossible(),
                r.delaiRecoursTa(),
                r.statut(),
                r.recommandation(),
                country,
                r.baseJuridique()
        );
    }
}
