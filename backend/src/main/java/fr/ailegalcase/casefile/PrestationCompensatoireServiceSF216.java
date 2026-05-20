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
 * SF-216-01 : service orchestrant le calcul de la prestation compensatoire
 * (art. 270-281 Cciv). Gate FRANCE + DROIT_FAMILLE strict.
 */
@Service
public class PrestationCompensatoireServiceSF216 {

    private final PrestationCompensatoireRepositorySF216 repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public PrestationCompensatoireServiceSF216(
            PrestationCompensatoireRepositorySF216 repository,
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
    public PrestationCompensatoireResponse calculate(UUID caseFileId,
                                                     PrestationCompensatoireRequest request,
                                                     OidcUser oidcUser,
                                                     Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;

        validateRequest(request, country);

        PrestationCompensatoireResult result;
        try {
            result = PrestationCompensatoireCalculatorSF216.compute(
                    request.dureeMariageAnnees(),
                    request.revenusAnnuelsEpoux1Eur(),
                    request.revenusAnnuelsEpoux2Eur(),
                    request.patrimoinePropre1Eur(),
                    request.patrimoinePropre2Eur(),
                    request.ageEpoux1Annees(),
                    request.ageEpoux2Annees(),
                    request.formePrestationDemandee(),
                    Boolean.TRUE.equals(request.avantageMatrimonialDetecte()),
                    country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        PrestationCompensatoireAnalysisSF216 entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    PrestationCompensatoireAnalysisSF216 a = new PrestationCompensatoireAnalysisSF216();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public PrestationCompensatoireResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        PrestationCompensatoireAnalysisSF216 entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de prestation compensatoire trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserializeResult(entity.getResultData()));
    }

    // -----------------------------------------------------------------------
    // Privé
    // -----------------------------------------------------------------------

    private void validateRequest(PrestationCompensatoireRequest req, String country) {
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outil F-FA-01 applicable uniquement en France (art. 270 Cciv).");
        }
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête manquant.");
        }
        if (req.dureeMariageAnnees() == null || req.dureeMariageAnnees() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dureeMariageAnnees requis et ≥ 0.");
        }
        if (req.revenusAnnuelsEpoux1Eur() == null || req.revenusAnnuelsEpoux1Eur() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "revenusAnnuelsEpoux1Eur requis et ≥ 0.");
        }
        if (req.revenusAnnuelsEpoux2Eur() == null || req.revenusAnnuelsEpoux2Eur() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "revenusAnnuelsEpoux2Eur requis et ≥ 0.");
        }
        if (req.ageEpoux1Annees() == null || req.ageEpoux1Annees() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ageEpoux1Annees requis et ≥ 0.");
        }
        if (req.ageEpoux2Annees() == null || req.ageEpoux2Annees() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ageEpoux2Annees requis et ≥ 0.");
        }
        if (req.patrimoinePropre1Eur() != null && req.patrimoinePropre1Eur() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patrimoinePropre1Eur ≥ 0.");
        }
        if (req.patrimoinePropre2Eur() != null && req.patrimoinePropre2Eur() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patrimoinePropre2Eur ≥ 0.");
        }
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
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de la famille.");
        return cf;
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation.");
        }
    }

    private PrestationCompensatoireResult deserializeResult(String json) {
        try {
            return objectMapper.readValue(json, PrestationCompensatoireResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation.");
        }
    }

    private PrestationCompensatoireResponse toResponse(UUID caseFileId, String country,
                                                       PrestationCompensatoireResult r) {
        return new PrestationCompensatoireResponse(
                caseFileId,
                r.montantCapitalIndicatifEur(),
                r.renteIndicativeMensuelleEur(),
                r.formeRecommandee(),
                r.dispAriteNiveauxVie(),
                r.baseJuridique(),
                r.messages(),
                r.alertes(),
                country
        );
    }
}
