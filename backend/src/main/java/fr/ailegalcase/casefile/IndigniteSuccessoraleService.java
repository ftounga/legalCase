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
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-216-19 : service orchestrant l'outil Indignité successorale FR
 * (art. 726-729-1 Cciv). Gate FRANCE + DROIT_FAMILLE.
 */
@Service
public class IndigniteSuccessoraleService {

    private final IndigniteSuccessoraleAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public IndigniteSuccessoraleService(
            IndigniteSuccessoraleAnalysisRepository repository,
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
    public IndigniteSuccessoraleResponse calculate(UUID caseFileId,
                                                   IndigniteSuccessoraleRequest request,
                                                   OidcUser oidcUser,
                                                   Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;

        validateRequest(request, country);

        IndigniteSuccessoraleResult result;
        try {
            result = IndigniteSuccessoraleCalculator.compute(request, country, LocalDate.now());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        IndigniteSuccessoraleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    IndigniteSuccessoraleAnalysis a = new IndigniteSuccessoraleAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public IndigniteSuccessoraleResponse get(UUID caseFileId,
                                             OidcUser oidcUser,
                                             Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        IndigniteSuccessoraleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse Indignité successorale trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserializeResult(entity.getResultData()));
    }

    // -----------------------------------------------------------------------
    // Privé
    // -----------------------------------------------------------------------

    private void validateRequest(IndigniteSuccessoraleRequest req, String country) {
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outil F-FA-INDIGNITE-SUCCESSORALE applicable uniquement en France (art. 726 Cciv).");
        }
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête manquant.");
        }
        if (req.motifIndignite() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "motifIndignite est requis.");
        }
        if (req.dateOuvertureSuccession() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateOuvertureSuccession est requise.");
        }
        if (req.dateOuvertureSuccession().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateOuvertureSuccession ne peut pas être future.");
        }
        if (req.nbCoheritiersRestants() != null && req.nbCoheritiersRestants() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "nbCoheritiersRestants doit être >= 0.");
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

    private IndigniteSuccessoraleResult deserializeResult(String json) {
        try {
            return objectMapper.readValue(json, IndigniteSuccessoraleResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation.");
        }
    }

    private IndigniteSuccessoraleResponse toResponse(UUID caseFileId, String country,
                                                     IndigniteSuccessoraleResult r) {
        return new IndigniteSuccessoraleResponse(
                caseFileId,
                r.typeIndignite(),
                r.verdictIndignite(),
                r.pardonNeutralisant(),
                r.representationPossible(),
                r.delaiAction(),
                r.delaiForclos(),
                r.effetDevolution(),
                r.baseLegale(),
                r.messages(),
                r.alertes(),
                country
        );
    }
}
