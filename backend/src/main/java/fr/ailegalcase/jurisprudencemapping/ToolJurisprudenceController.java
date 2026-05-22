package fr.ailegalcase.jurisprudencemapping;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * F-JU-01 — endpoints des citations jurisprudentielles d'outils décisionnels.
 *
 * <p>SF-JU-01-01 : GET de lecture (auth MEMBER, table globale).
 * SF-JU-01-04 : POST signal (auth MEMBER, crée flag USER_SIGNAL).</p>
 */
@RestController
@RequestMapping("/api/v1/tools")
public class ToolJurisprudenceController {

    private static final Pattern TOOL_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,100}$");

    private final ToolJurisprudenceService service;

    public ToolJurisprudenceController(ToolJurisprudenceService service) {
        this.service = service;
    }

    @GetMapping("/{toolId}/jurisprudence-citations")
    public List<ToolJurisprudenceCitationResponse> getCitations(
            @PathVariable String toolId,
            @RequestParam(name = "branch", required = false) String branch) {
        validateToolId(toolId);
        return service.findByToolAndBranch(toolId, branch);
    }

    /**
     * F-JU-01 / SF-JU-01-04 — signalement utilisateur d'une citation
     * potentiellement obsolète ou erronée. Crée un
     * {@link JurisprudenceWatchFlag} source {@code USER_SIGNAL} qui sera
     * arbitré par l'admin via le dashboard SF-JU-01-05.
     */
    @PostMapping("/{toolId}/jurisprudence-citations/{citationId}/signal")
    public ResponseEntity<Void> signalCitation(
            @PathVariable String toolId,
            @PathVariable UUID citationId,
            @Valid @RequestBody(required = false) JurisprudenceSignalRequest request) {
        validateToolId(toolId);
        String comment = request == null ? null : request.comment();
        service.signalProblem(toolId, citationId, comment);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private void validateToolId(String toolId) {
        if (toolId == null || !TOOL_ID_PATTERN.matcher(toolId).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid toolId format (expected ^[a-zA-Z0-9_-]{1,100}$)");
        }
    }
}
