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
 * SF-217-14 : service orchestrant l'analyse de protection du majeur BE
 * (loi 17/03/2013 — statut unique d'administration) + persistance snapshot
 * (un seul résultat courant par dossier, écrasé au recalcul — upsert 1:1).
 */
@Service
public class ProtectionMajeurBeService {

    private final ProtectionMajeurBeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ProtectionMajeurBeService(
            ProtectionMajeurBeRepository repository,
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
    public ProtectionMajeurBeResponse calculate(
            UUID caseFileId,
            ProtectionMajeurBeRequest request,
            OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;

        ProtectionMajeurBeResult result;
        try {
            result = ProtectionMajeurBeCalculator.compute(request.toInput(), country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ProtectionMajeurBeResponse response =
                toResponse(caseFileId, request, result, Instant.now());

        ProtectionMajeurBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ProtectionMajeurBeAnalysis a = new ProtectionMajeurBeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(result.country());
        entity.setSnapshotData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public ProtectionMajeurBeResponse get(UUID caseFileId,
                                          OidcUser oidcUser,
                                          Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        ProtectionMajeurBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de protection du majeur trouvée pour ce dossier"));
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
                    "L'analyse de protection du majeur BE est un outil disponible "
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

    private ProtectionMajeurBeResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, ProtectionMajeurBeResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private ProtectionMajeurBeResponse toResponse(
            UUID caseFileId,
            ProtectionMajeurBeRequest req,
            ProtectionMajeurBeResult r,
            Instant calculatedAt) {
        return new ProtectionMajeurBeResponse(
                caseFileId,
                req.natureAlteration(),
                req.graviteIncapacite(),
                req.mandatExtraJudiciaireSigne(),
                req.mandatExtraJudiciaireDateSignature(),
                req.declarationAnticipeeExiste(),
                req.environnementFamilialProtecteur(),
                req.niveauUrgence(),
                req.modeSaisineEnvisage(),
                req.commentaire(),
                r.verdict(),
                r.mesureRecommandee(),
                r.juridictionCompetente(),
                r.fondementProcedural(),
                r.actesProteges(),
                r.actionsConcretes(),
                r.basesJuridiques(),
                r.messages(),
                r.country(),
                calculatedAt
        );
    }
}
