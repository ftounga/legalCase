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
 * SF-214-05 : service de l'analyse d'éligibilité à la VPF liens personnels et
 * familiaux L. 423-23 CESEDA (art. 8 CEDH). Outil single-country FR.
 */
@Service
public class VpfLiensPersonnelsService {

    private final VpfLiensPersonnelsRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public VpfLiensPersonnelsService(VpfLiensPersonnelsRepository repository,
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
    public VpfLiensPersonnelsResponse analyze(UUID caseFileId, VpfLiensPersonnelsRequest request,
                                              OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "VPF liens personnels L.423-23 — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.dureeResidenceFranceMois() == null || request.entreeEnFranceMineur() == null
                || request.enfantsEnFrance() == null || request.conjointEnFrance() == null
                || request.parentsEnFrance() == null || request.niveauIntegration() == null
                || request.ancienneConvictionPenale() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tous les champs sont requis sauf situationFamilialeAlEtranger : "
                    + "dureeResidenceFranceMois, entreeEnFranceMineur, enfantsEnFrance, "
                    + "conjointEnFrance, parentsEnFrance, niveauIntegration, ancienneConvictionPenale");
        }

        VpfLiensPersonnelsResult result;
        try {
            result = VpfLiensPersonnelsAnalyzer.analyze(
                    request.dureeResidenceFranceMois(),
                    request.entreeEnFranceMineur(),
                    request.enfantsEnFrance(),
                    request.conjointEnFrance(),
                    request.parentsEnFrance(),
                    request.situationFamilialeAlEtranger(),
                    request.niveauIntegration(),
                    request.ancienneConvictionPenale());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        VpfLiensPersonnelsAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    VpfLiensPersonnelsAnalysis a = new VpfLiensPersonnelsAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDureeResidenceFranceMois(result.dureeResidenceFranceMois());
        entity.setEntreeEnFranceMineur(result.entreeEnFranceMineur());
        entity.setEnfantsEnFrance(result.enfantsEnFrance());
        entity.setConjointEnFrance(result.conjointEnFrance());
        entity.setParentsEnFrance(result.parentsEnFrance());
        entity.setSituationFamilialeAlEtranger(result.situationFamilialeAlEtranger());
        entity.setNiveauIntegration(result.niveauIntegration());
        entity.setAncienneConvictionPenale(result.ancienneConvictionPenale());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public VpfLiensPersonnelsResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        VpfLiensPersonnelsAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse VPF liens personnels trouvée pour ce dossier"));
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

    private VpfLiensPersonnelsResult deserialize(String json) {
        try { return objectMapper.readValue(json, VpfLiensPersonnelsResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private VpfLiensPersonnelsResponse toResponse(UUID caseFileId, String country,
                                                  VpfLiensPersonnelsResult r) {
        return new VpfLiensPersonnelsResponse(
                caseFileId,
                r.dureeResidenceFranceMois(),
                r.entreeEnFranceMineur(),
                r.enfantsEnFrance(),
                r.conjointEnFrance(),
                r.parentsEnFrance(),
                r.situationFamilialeAlEtranger(),
                r.niveauIntegration(),
                r.ancienneConvictionPenale(),
                country,
                r.score(),
                r.dureeResidenceRemplie(),
                r.verdict(),
                r.chipsCriteresNonRemplis(),
                r.recommandations(),
                r.baseJuridique());
    }
}
