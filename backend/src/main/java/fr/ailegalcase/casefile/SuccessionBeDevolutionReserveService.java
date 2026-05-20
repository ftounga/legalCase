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
import java.time.Instant;
import java.util.UUID;

/**
 * SF-217-11 : service orchestrant l'analyse de dévolution / réserve héréditaire
 * belge + persistance snapshot (un seul résultat courant par dossier).
 */
@Service
public class SuccessionBeDevolutionReserveService {

    private final SuccessionBeDevolutionReserveRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public SuccessionBeDevolutionReserveService(SuccessionBeDevolutionReserveRepository repository,
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
    public SuccessionBeDevolutionReserveResponse calculate(UUID caseFileId,
                                                           SuccessionBeDevolutionReserveRequest request,
                                                           OidcUser oidcUser,
                                                           Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();
        requireBelgique(country);

        SuccessionBeDevolutionReserveResult result;
        try {
            result = SuccessionBeDevolutionReserveCalculator.compute(request.toInput(), country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        SuccessionBeDevolutionReserveResponse response =
                toResponse(caseFileId, request, result, Instant.now());

        SuccessionBeDevolutionReserveAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    SuccessionBeDevolutionReserveAnalysis a = new SuccessionBeDevolutionReserveAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(result.country());
        entity.setSnapshotData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public SuccessionBeDevolutionReserveResponse get(UUID caseFileId,
                                                     OidcUser oidcUser,
                                                     Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        requireBelgique(caseFile.getWorkspace().getCountry());
        SuccessionBeDevolutionReserveAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de dévolution / réserve trouvée pour ce dossier"));
        return deserialize(entity.getSnapshotData());
    }

    private void requireBelgique(String country) {
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Succession (dévolution / réserve) — outil disponible uniquement pour les workspaces BELGIQUE");
        }
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier introuvable"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ce dossier appartient à un autre workspace");
        }
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Ce dossier n'est pas un dossier de droit de la famille");
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

    private SuccessionBeDevolutionReserveResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, SuccessionBeDevolutionReserveResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private SuccessionBeDevolutionReserveResponse toResponse(UUID caseFileId,
                                                             SuccessionBeDevolutionReserveRequest req,
                                                             SuccessionBeDevolutionReserveResult r,
                                                             Instant calculatedAt) {
        return new SuccessionBeDevolutionReserveResponse(
                caseFileId,
                req.dateDeces(),
                req.etatCivilDefunt(),
                req.regimeMatrimonialDefunt(),
                req.nombreEnfantsVivants(),
                req.nombreEnfantsPredecedesAvecDescendants(),
                req.presenceParentsVivants(),
                req.presenceFreresSoeursOuDescendants(),
                req.masseSuccessoraleEur(),
                req.libertesConsentiesEur(),
                req.commentaire(),
                r.verdict(),
                r.heritiers(),
                r.reserveGlobaleEur(),
                r.reserveGlobaleFraction(),
                r.quotiteDisponibleEur(),
                r.quotiteDisponibleFraction(),
                r.depassementQuotiteEur(),
                r.basesJuridiques(),
                r.messages(),
                r.country(),
                calculatedAt
        );
    }
}
