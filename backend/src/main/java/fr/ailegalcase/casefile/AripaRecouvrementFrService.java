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
 * SF-216-07 : service orchestrant l'outil ARIPA recouvrement FR (art. L. 581+
 * CSS). Gate FRANCE + DROIT_FAMILLE.
 */
@Service
public class AripaRecouvrementFrService {

    private final AripaRecouvrementFrRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AripaRecouvrementFrService(
            AripaRecouvrementFrRepository repository,
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
    public AripaRecouvrementFrResponse calculate(UUID caseFileId,
                                                  AripaRecouvrementFrRequest request,
                                                  OidcUser oidcUser,
                                                  Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;

        validateRequest(request, country);

        AripaRecouvrementFrResult result;
        try {
            result = AripaRecouvrementFrCalculator.compute(request, country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        AripaRecouvrementFrAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AripaRecouvrementFrAnalysis a = new AripaRecouvrementFrAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public AripaRecouvrementFrResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        AripaRecouvrementFrAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse ARIPA recouvrement trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserializeResult(entity.getResultData()));
    }

    // -----------------------------------------------------------------------
    // Privé
    // -----------------------------------------------------------------------

    private void validateRequest(AripaRecouvrementFrRequest req, String country) {
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outil F-FA-ARIPA-RECOUVREMENT applicable uniquement en France (art. L. 581-1 CSS).");
        }
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête manquant.");
        }
        rejectNegative(req.montantPensionMensuelleEur(), "montantPensionMensuelleEur");
        rejectNegative(req.nombreEnfantsACharge(), "nombreEnfantsACharge");
        // Nombre de mois impayés requis et >= 1 (sinon pas d'impayé → outil sans objet).
        Integer mois = req.nombreMoisImpayes();
        if (mois == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "nombreMoisImpayes est requis.");
        }
        if (mois < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "nombreMoisImpayes doit être >= 1 (sinon pas d'impayé caractérisé).");
        }
        if (req.titreExecutoire() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "titreExecutoire est requis (true / false).");
        }
        if (req.debiteurEnFrance() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "debiteurEnFrance est requis (true / false).");
        }
    }

    private static void rejectNegative(Integer v, String field) {
        if (v != null && v < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " doit être >= 0.");
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

    private AripaRecouvrementFrResult deserializeResult(String json) {
        try {
            return objectMapper.readValue(json, AripaRecouvrementFrResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation.");
        }
    }

    private AripaRecouvrementFrResponse toResponse(UUID caseFileId, String country,
                                                    AripaRecouvrementFrResult r) {
        return new AripaRecouvrementFrResponse(
                caseFileId,
                r.voieRecommandee(),
                r.montantArrieres(),
                r.montantAsfEligibleMensuelEur(),
                r.delaiEstimeJours(),
                r.etapes(),
                r.baseLegale(),
                r.messages(),
                r.alertes(),
                country
        );
    }
}
