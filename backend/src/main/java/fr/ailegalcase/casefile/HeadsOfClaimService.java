package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.HeadOfClaim.HeadCategory;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * F-262 SF-262-01 — Détecte les <strong>chefs de demande applicables</strong> à
 * un dossier (filet de complétude) et signale, pour chaque chef outillé, s'il
 * est couvert (outil calculé).
 *
 * <p>Lecture pure. Réutilise sans dupliquer :</p>
 * <ul>
 *   <li>{@link DecisionToolVisibilityService#resolveVisibleTools} pour
 *       l'<em>applicabilité</em> des chefs outillés (alwaysOn ∪ contextual) ;</li>
 *   <li>{@link CaseFileDashboardService#assembleDecisionToolTiles} pour le statut
 *       <em>{@code addressed}</em> (un toolId présent dans les tiles = calculé).</li>
 * </ul>
 *
 * <p>Distinct de F-258 : F-262 expose des <em>chefs de demande</em> (incluant les
 * transverses non outillés), pas les « outils non calculés ».</p>
 */
@Service
public class HeadsOfClaimService {

    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final DecisionToolVisibilityService visibilityService;
    private final CaseFileDashboardService dashboardService;

    public HeadsOfClaimService(CaseFileRepository caseFileRepository,
                               WorkspaceMemberRepository workspaceMemberRepository,
                               CurrentUserResolver currentUserResolver,
                               DecisionToolVisibilityService visibilityService,
                               CaseFileDashboardService dashboardService) {
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.visibilityService = visibilityService;
        this.dashboardService = dashboardService;
    }

    @Transactional(readOnly = true)
    public HeadsOfClaimResponse detect(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String legalDomain = caseFile.getLegalDomain();
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;

        List<HeadOfClaim> catalog = HeadOfClaimCatalog.forDomainAndCountry(legalDomain, country);
        if (catalog.isEmpty()) {
            // Domaine / pays non couvert (vagues immigration / famille futures).
            return new HeadsOfClaimResponse(List.of());
        }

        // Applicabilité des chefs outillés : outils visibles (alwaysOn ∪ contextual).
        VisibleToolSetResponse visible = visibilityService.resolveVisibleTools(caseFileId, oidcUser, principal);
        Set<String> visibleTools = new HashSet<>(visible.alwaysOn());
        visibleTools.addAll(visible.contextual());

        // Statut addressed : toolIds des outils calculés (tiles dashboard).
        Set<String> calculatedTools = dashboardService.assembleDecisionToolTiles(caseFileId).stream()
                .map(DashboardTile::toolId)
                .collect(Collectors.toSet());

        List<HeadsOfClaimResponse.HeadItem> heads = new ArrayList<>();
        for (HeadOfClaim head : catalog) {
            if (head.category() == HeadCategory.TRANSVERSE) {
                // Toujours applicable au stade contentieux ; addressed=false (rappel).
                heads.add(toItem(head, false));
            } else {
                // INDEMNITAIRE_OUTILLE : applicable ssi l'outil est visible.
                if (head.toolId() != null && visibleTools.contains(head.toolId())) {
                    boolean addressed = calculatedTools.contains(head.toolId());
                    heads.add(toItem(head, addressed));
                }
                // Non visible → exclu de la réponse (anti-gadget).
            }
        }
        return new HeadsOfClaimResponse(heads);
    }

    private static HeadsOfClaimResponse.HeadItem toItem(HeadOfClaim head, boolean addressed) {
        return new HeadsOfClaimResponse.HeadItem(
                head.code(),
                head.label(),
                head.fondement(),
                head.category().name(),
                head.toolId(),
                addressed);
    }

    /**
     * Résout le dossier en garantissant l'isolation workspace (pattern identique
     * à {@link DecisionToolVisibilityService}) : 404 si le dossier n'existe pas
     * ou appartient à un autre workspace que celui de l'utilisateur.
     */
    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        UUID userWorkspaceId = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId())
                .orElse(null);
        if (userWorkspaceId == null || cf.getWorkspace() == null
                || !userWorkspaceId.equals(cf.getWorkspace().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        return cf;
    }
}
