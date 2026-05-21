package fr.ailegalcase.jurisprudencemapping;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.regex.Pattern;

/**
 * F-JU-01 / SF-JU-01-01 — endpoint de lecture des citations jurisprudentielles
 * d'une branche d'un outil décisionnel.
 *
 * <p>Authentification requise (MEMBER minimum), pas d'isolation workspace (table
 * globale). Aucun endpoint d'écriture exposé dans cette SF — l'alimentation est
 * différée à SF-JU-01-02 (cron veille) et SF-JU-01-05 (dashboard admin
 * SUPER_ADMIN).</p>
 */
@RestController
@RequestMapping("/api/v1/tools")
public class ToolJurisprudenceController {

    private static final Pattern TOOL_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,100}$");

    private final ToolJurisprudenceService service;

    public ToolJurisprudenceController(ToolJurisprudenceService service) {
        this.service = service;
    }

    /**
     * Récupère les 0 à 3 arrêts mappés pour une branche de calcul d'un outil.
     *
     * <p>Si {@code branch} est absent ou vide : retour 200 + liste vide
     * (comportement intentionnel, le composant frontend affichera simplement
     * aucune citation). Si le {@code toolId} n'a aucun mapping : retour 200
     * + liste vide (l'outil existe peut-être sans citation curatée).</p>
     *
     * @param toolId identifiant de l'outil ({@code TOOL_REGISTRY})
     * @param branch identifiant de la branche de calcul active (optionnel)
     * @return 200 + JSON liste de 0 à 3 {@link ToolJurisprudenceCitationResponse}
     * @throws ResponseStatusException 400 si {@code toolId} est invalide
     */
    @GetMapping("/{toolId}/jurisprudence-citations")
    public List<ToolJurisprudenceCitationResponse> getCitations(
            @PathVariable String toolId,
            @RequestParam(name = "branch", required = false) String branch) {
        if (toolId == null || !TOOL_ID_PATTERN.matcher(toolId).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid toolId format (expected ^[a-zA-Z0-9_-]{1,100}$)");
        }
        return service.findByToolAndBranch(toolId, branch);
    }
}
