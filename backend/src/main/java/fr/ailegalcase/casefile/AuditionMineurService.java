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
 * SF-216-13 : service orchestrant l'outil Audition du mineur par le JAF FR
 * (art. 388-1 Cciv + art. 1074-1 à 1074-3 CPC). Gate FRANCE + DROIT_FAMILLE.
 */
@Service
public class AuditionMineurService {

    private final AuditionMineurAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AuditionMineurService(
            AuditionMineurAnalysisRepository repository,
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
    public AuditionMineurResponse calculate(UUID caseFileId,
                                            AuditionMineurRequest request,
                                            OidcUser oidcUser,
                                            Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;

        validateRequest(request, country);

        AuditionMineurResult result;
        try {
            result = AuditionMineurCalculator.compute(request, country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        AuditionMineurAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AuditionMineurAnalysis a = new AuditionMineurAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public AuditionMineurResponse get(UUID caseFileId,
                                      OidcUser oidcUser,
                                      Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        AuditionMineurAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse Audition du mineur trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserializeResult(entity.getResultData()));
    }

    // -----------------------------------------------------------------------
    // Privé
    // -----------------------------------------------------------------------

    private void validateRequest(AuditionMineurRequest req, String country) {
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outil F-FA-AUDITION-MINEUR applicable uniquement en France (art. 388-1 Cciv).");
        }
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête manquant.");
        }
        if (req.ageEnfant() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ageEnfant est requis.");
        }
        int age = req.ageEnfant();
        if (age < 0 || age >= AuditionMineurCalculator.AGE_MAJORITE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ageEnfant doit être compris entre 0 et 17 (art. 388-1 Cciv vise les mineurs).");
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

    private AuditionMineurResult deserializeResult(String json) {
        try {
            return objectMapper.readValue(json, AuditionMineurResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation.");
        }
    }

    private AuditionMineurResponse toResponse(UUID caseFileId, String country,
                                              AuditionMineurResult r) {
        return new AuditionMineurResponse(
                caseFileId,
                r.conditionsRemplies(),
                r.droitAuditionReconnu(),
                r.modaliteRecommandee(),
                r.refusContestable(),
                r.verdict(),
                r.baseLegale(),
                r.messages(),
                r.alertes(),
                country
        );
    }
}
