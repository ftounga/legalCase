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
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-216-25 : service orchestrant l'outil Présomption de paternité du
 * mari et désaveu FR (art. 312-315 Cciv + art. 316 al. 2 + art. 333
 * al. 1). Gate FRANCE + DROIT_FAMILLE.
 */
@Service
public class PresomptionPaterniteService {

    private final PresomptionPaterniteAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;
    private Clock clock = Clock.systemDefaultZone();

    public PresomptionPaterniteService(
            PresomptionPaterniteAnalysisRepository repository,
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

    /** Test-only seam — override la {@link Clock} interne. */
    void setClock(Clock clock) {
        this.clock = clock;
    }

    @Transactional
    public PresomptionPaterniteResponse calculate(UUID caseFileId,
                                                  PresomptionPaterniteRequest request,
                                                  OidcUser oidcUser,
                                                  Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;

        validateRequest(request, country);

        PresomptionPaterniteResult result;
        try {
            result = PresomptionPaterniteCalculator.compute(
                    request, country, LocalDate.now(clock));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        PresomptionPaterniteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    PresomptionPaterniteAnalysis a = new PresomptionPaterniteAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public PresomptionPaterniteResponse get(UUID caseFileId,
                                            OidcUser oidcUser,
                                            Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        PresomptionPaterniteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse Présomption de paternité trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserializeResult(entity.getResultData()));
    }

    // -----------------------------------------------------------------------
    // Privé
    // -----------------------------------------------------------------------

    private void validateRequest(PresomptionPaterniteRequest req, String country) {
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outil F-FA-PRESOMPTION-PATERNITE applicable uniquement "
                            + "en France (art. 312 Cciv).");
        }
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Corps de requête manquant.");
        }
        if (req.dateNaissanceEnfant() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateNaissanceEnfant est requis.");
        }
        if (req.dateConclusionMariage() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateConclusionMariage est requis.");
        }
        if (req.dateDissolutionMariage() != null
                && req.dateDissolutionMariage().isBefore(req.dateConclusionMariage())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateDissolutionMariage ne peut pas être antérieure à "
                            + "dateConclusionMariage.");
        }
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId()))
                .orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de la famille.");
        }
        return cf;
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation.");
        }
    }

    private PresomptionPaterniteResult deserializeResult(String json) {
        try {
            return objectMapper.readValue(json, PresomptionPaterniteResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation.");
        }
    }

    private PresomptionPaterniteResponse toResponse(UUID caseFileId, String country,
                                                    PresomptionPaterniteResult r) {
        return new PresomptionPaterniteResponse(
                caseFileId,
                r.presomptionApplicable(),
                r.presomptionRenversee(),
                r.voieDesaveu(),
                r.delaiDesaveu(),
                r.possessionEtatImpact(),
                r.baseLegale(),
                r.messages(),
                r.alertes(),
                country
        );
    }
}
