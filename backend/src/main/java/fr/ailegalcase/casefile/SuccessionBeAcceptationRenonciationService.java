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
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-217-12 : service orchestrant l'analyse d'option successorale BE
 * (acceptation pure / sous bénéfice d'inventaire / renonciation) + persistance
 * snapshot (un seul résultat courant par dossier, écrasé au recalcul — upsert 1:1).
 */
@Service
public class SuccessionBeAcceptationRenonciationService {

    private final SuccessionBeAcceptationRenonciationRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public SuccessionBeAcceptationRenonciationService(
            SuccessionBeAcceptationRenonciationRepository repository,
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
    public SuccessionBeAcceptationRenonciationResponse calculate(
            UUID caseFileId,
            SuccessionBeAcceptationRenonciationRequest request,
            OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;

        SuccessionBeAcceptationRenonciationResult result;
        try {
            result = SuccessionBeAcceptationRenonciationCalculator.compute(
                    request.toInput(), country, LocalDate.now());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        SuccessionBeAcceptationRenonciationResponse response =
                toResponse(caseFileId, request, result, Instant.now());

        SuccessionBeAcceptationRenonciationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    SuccessionBeAcceptationRenonciationAnalysis a =
                            new SuccessionBeAcceptationRenonciationAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(result.country());
        entity.setSnapshotData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public SuccessionBeAcceptationRenonciationResponse get(UUID caseFileId,
                                                            OidcUser oidcUser,
                                                            Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        SuccessionBeAcceptationRenonciationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse d'option successorale trouvée pour ce dossier"));
        return deserialize(entity.getSnapshotData());
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    /**
     * Résout le dossier en appliquant les gates : 404 dossier inexistant, 403
     * dossier d'un autre workspace, 422 domaine ≠ DROIT_FAMILLE ou pays ≠ BELGIQUE.
     */
    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Dossier introuvable"));
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
        String country = cf.getWorkspace() != null ? cf.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "L'analyse d'option successorale BE est un outil disponible "
                            + "uniquement pour les workspaces de Belgique");
        }
        return cf;
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de sérialisation");
        }
    }

    private SuccessionBeAcceptationRenonciationResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, SuccessionBeAcceptationRenonciationResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private SuccessionBeAcceptationRenonciationResponse toResponse(
            UUID caseFileId,
            SuccessionBeAcceptationRenonciationRequest req,
            SuccessionBeAcceptationRenonciationResult r,
            Instant calculatedAt) {
        return new SuccessionBeAcceptationRenonciationResponse(
                caseFileId,
                req.dateDeces(),
                req.qualiteHeritier(),
                req.etatPatrimoineSuccessoral(),
                req.actesAccomplis(),
                req.volonteClient(),
                req.miseEnDemeureCreancier(),
                req.dateMiseEnDemeureCreancier(),
                req.commentaire(),
                r.verdict(),
                r.optionRecommandee(),
                r.dateLimiteOption(),
                r.joursRestants(),
                r.delaiStatut(),
                r.actionsConcretes(),
                r.risques(),
                r.basesJuridiques(),
                r.messages(),
                r.country(),
                calculatedAt
        );
    }
}
