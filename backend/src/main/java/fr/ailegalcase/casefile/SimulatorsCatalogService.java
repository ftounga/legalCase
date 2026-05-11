package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * SF-163-01 : résout le catalogue d'outils décisionnels disponibles en mode
 * simulateur autonome (hors dossier) pour l'utilisateur courant, en fonction
 * du workspace primary (legalDomain × country).
 *
 * <p>Lecture pure : aucun side effect, pas de persistance. Aligné sur le
 * pattern de {@link DecisionToolVisibilityService} pour la résolution
 * d'utilisateur (CurrentUserResolver) et de workspace
 * (WorkspaceMemberRepository.findByUserAndPrimaryTrue).</p>
 */
@Service
public class SimulatorsCatalogService {

    private static final Logger log = LoggerFactory.getLogger(SimulatorsCatalogService.class);

    private final DecisionToolVisibilityRuleRepository ruleRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public SimulatorsCatalogService(DecisionToolVisibilityRuleRepository ruleRepository,
                                    WorkspaceMemberRepository workspaceMemberRepository,
                                    CurrentUserResolver currentUserResolver) {
        this.ruleRepository = ruleRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional(readOnly = true)
    public SimulatorsCatalogResponse resolveCatalog(OidcUser oidcUser, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);

        Optional<WorkspaceMember> primary = workspaceMemberRepository.findByUserAndPrimaryTrue(user);
        if (primary.isEmpty()) {
            log.warn("User {} has no primary workspace — returning empty simulators catalog", user.getId());
            return new SimulatorsCatalogResponse(null, null, List.of());
        }

        Workspace workspace = primary.get().getWorkspace();
        String legalDomain = workspace.getLegalDomain();
        String country = workspace.getCountry();

        if (legalDomain == null) {
            log.info("Workspace {} has null legalDomain — returning empty simulators catalog (country={})",
                    workspace.getId(), country);
            return new SimulatorsCatalogResponse(null, country, List.of());
        }

        List<DecisionToolVisibilityRule> rules = ruleRepository.findForDomainAndCountry(legalDomain, country);

        // Tri stable : priority asc, toolId lex.
        // Déduplication : LinkedHashSet sur tool_id (préserve l'ordre d'insertion = ordre trié).
        Comparator<DecisionToolVisibilityRule> byPriorityThenId =
                Comparator.comparingInt(DecisionToolVisibilityRule::getPriority)
                        .thenComparing(DecisionToolVisibilityRule::getToolId);

        LinkedHashSet<String> toolIds = new LinkedHashSet<>();
        rules.stream()
                .sorted(byPriorityThenId)
                .map(DecisionToolVisibilityRule::getToolId)
                .forEach(toolIds::add);

        return new SimulatorsCatalogResponse(legalDomain, country, new ArrayList<>(toolIds));
    }
}
