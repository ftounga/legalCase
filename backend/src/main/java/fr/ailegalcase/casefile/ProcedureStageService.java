package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

/**
 * F-243 / SF-243-01 — Service du stade procédural du dossier.
 *
 * <p>Trois opérations : exposer le référentiel des valeurs (endpoint A), lire le stade d'un
 * dossier (B), et mettre à jour le stade après validation de cohérence (C).
 *
 * <p>Isolation workspace : un utilisateur ne peut lire/modifier que les dossiers de son
 * workspace primaire ({@code WorkspaceMemberRepository.findByUserAndPrimaryTrue}). Un dossier
 * d'un autre workspace renvoie {@code 403}.
 */
@Service
public class ProcedureStageService {

    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public ProcedureStageService(CaseFileRepository caseFileRepository,
                                 WorkspaceMemberRepository workspaceMemberRepository,
                                 CurrentUserResolver currentUserResolver) {
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * Endpoint A — référentiel des valeurs valides pour un domaine + pays.
     *
     * @throws ResponseStatusException 400 si {@code domain} ou {@code country} hors valeurs autorisées.
     */
    public ProcedureStageOptionsResponse getOptions(String domain, String country) {
        if (!ProcedureStageCatalog.isKnownDomain(domain)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Domaine invalide — valeurs acceptées : DROIT_DU_TRAVAIL, DROIT_IMMIGRATION, DROIT_FAMILLE");
        }
        if (!ProcedureStageCatalog.isKnownCountry(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pays invalide — valeurs acceptées : FRANCE, BELGIQUE");
        }
        return ProcedureStageOptionsResponse.from(domain, country,
                ProcedureStageCatalog.forDomainAndCountry(domain, country));
    }

    /**
     * Endpoint B — lecture du stade procédural d'un dossier.
     *
     * @throws ResponseStatusException 404 si dossier inexistant, 403 si autre workspace.
     */
    @Transactional(readOnly = true)
    public ProcedureStageResponse getProcedureStage(UUID caseFileId, OidcUser oidcUser,
                                                    String provider, Principal principal) {
        Workspace workspace = resolveWorkspace(oidcUser, provider, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, workspace.getId());
        return ProcedureStageResponse.from(caseFile, workspace.getLegalDomain(), workspace.getCountry());
    }

    /**
     * Endpoint C — mise à jour du stade procédural d'un dossier.
     *
     * <p>Applique la cascade (effacer {@code jurisdiction} efface {@code stage} + {@code position} ;
     * effacer {@code stage} efface {@code position}), valide la cohérence sémantique de la
     * combinaison résultante, puis persiste.
     *
     * @throws ResponseStatusException 404 dossier inexistant, 403 autre workspace,
     *                                 422 combinaison sémantiquement incohérente.
     */
    @Transactional
    public ProcedureStageResponse updateProcedureStage(UUID caseFileId, ProcedureStageUpdateRequest request,
                                                       OidcUser oidcUser, String provider, Principal principal) {
        Workspace workspace = resolveWorkspace(oidcUser, provider, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, workspace.getId());

        String jurisdiction = request.normalizedJurisdiction();
        String stage = request.normalizedStage();
        String position = request.normalizedPosition();

        // Cascade : un enfant ne survit pas à l'effacement de son parent.
        if (jurisdiction == null) {
            stage = null;
            position = null;
        }
        if (stage == null) {
            position = null;
        }

        String domain = workspace.getLegalDomain();
        String country = workspace.getCountry();

        Optional<String> error =
                ProcedureStageCatalog.validate(domain, country, jurisdiction, stage, position);
        if (error.isPresent()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, error.get());
        }

        caseFile.setProcedureJurisdiction(jurisdiction);
        caseFile.setProcedureStage(stage);
        caseFile.setProcedurePosition(position);
        CaseFile saved = caseFileRepository.save(caseFile);

        return ProcedureStageResponse.from(saved, domain, country);
    }

    private Workspace resolveWorkspace(OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        WorkspaceMember member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        return member.getWorkspace();
    }

    private CaseFile resolveCaseFile(UUID caseFileId, UUID workspaceId) {
        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        if (!caseFile.getWorkspace().getId().equals(workspaceId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return caseFile;
    }
}
