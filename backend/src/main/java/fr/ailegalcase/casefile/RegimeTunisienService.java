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
 * SF-220-01 : service de l'outil décisionnel "Régime franco-tunisien (accord du
 * 17/03/1988)" (F-IM-47-regime-tunisien-fr). Outil single-country FR.
 *
 * <p>Pattern miroir de {@link RegroupementFamilialService} (F-IM-26).</p>
 */
@Service
public class RegimeTunisienService {

    private final RegimeTunisienRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public RegimeTunisienService(RegimeTunisienRepository repository,
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
    public RegimeTunisienResponse analyze(UUID caseFileId, RegimeTunisienRequest request,
                                          OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Régime franco-tunisien — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.categorie() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le champ categorie est requis : "
                    + "ETUDIANT | COMMERCANT | SALARIE | FAMILIAL | AUTRE");
        }

        boolean titreEnCours = Boolean.TRUE.equals(request.titreEnCours());
        boolean dejaResident = Boolean.TRUE.equals(request.dejaResident());

        RegimeTunisienResult result;
        try {
            result = RegimeTunisienAnalyzer.analyze(
                    request.categorie(),
                    request.dureeSejourEnvisageeMois(),
                    titreEnCours,
                    dejaResident);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        RegimeTunisienAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    RegimeTunisienAnalysis a = new RegimeTunisienAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCategorie(result.categorie());
        entity.setDureeSejourEnvisageeMois(result.dureeSejourEnvisageeMois());
        entity.setTitreEnCours(result.titreEnCours());
        entity.setDejaResident(result.dejaResident());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public RegimeTunisienResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        RegimeTunisienAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse régime franco-tunisien trouvée pour ce dossier"));
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

    private RegimeTunisienResult deserialize(String json) {
        try { return objectMapper.readValue(json, RegimeTunisienResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private RegimeTunisienResponse toResponse(UUID caseFileId, String country,
                                              RegimeTunisienResult r) {
        return new RegimeTunisienResponse(
                caseFileId,
                r.categorie(),
                r.dureeSejourEnvisageeMois(),
                r.titreEnCours(),
                r.dejaResident(),
                country,
                r.regime(),
                r.particularitesApplicables(),
                r.basesJuridiques(),
                r.renvoiDroitCommun(),
                r.messages());
    }
}
