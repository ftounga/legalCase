package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * SF-163-01 : endpoint de lecture du catalogue d'outils décisionnels
 * disponibles en mode simulateur autonome (hors dossier).
 *
 * <p>Renvoie le couple (legalDomain × country) du workspace primary de
 * l'utilisateur courant + la liste dédupliquée des tool_id consommables
 * par la page {@code /simulators} (frontend SF-163-02).</p>
 *
 * <p>Pas d'écriture en V1 (catalogue dérivé de
 * {@link DecisionToolVisibilityRule} déjà alimentée par migrations).</p>
 */
@RestController
@RequestMapping("/api/v1/simulators")
public class SimulatorsCatalogController {

    private final SimulatorsCatalogService service;

    public SimulatorsCatalogController(SimulatorsCatalogService service) {
        this.service = service;
    }

    @GetMapping
    public SimulatorsCatalogResponse resolve(@AuthenticationPrincipal OidcUser oidcUser,
                                             Principal principal) {
        return service.resolveCatalog(oidcUser, principal);
    }
}
