package fr.ailegalcase.casefile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

/**
 * SF-163-03 : dispatcher backend des calculators stateless en mode simulateur
 * autonome (hors dossier).
 *
 * <p>POST {@code /api/v1/simulators/{toolId}/calculate} — auth requise (filtre
 * Spring Security standard). Le body JSON est désérialisé vers le type de
 * requête associé au {@code toolId} via {@link SimulatorCalculatorRegistry},
 * puis le calculator pur est invoqué. <b>Aucune persistance</b> : ni dans les
 * tables {@code *_analyses}, ni dans {@code case_files}.</p>
 *
 * <p>Le pays est résolu depuis le workspace primary de l'utilisateur courant
 * (cohérent avec {@link SimulatorsCatalogService}). Les calculators qui n'en
 * dépendent pas l'ignorent ; ceux qui en dépendent l'utilisent pour valider
 * le régime/motif/etc.</p>
 */
@RestController
@RequestMapping("/api/v1/simulators")
public class SimulatorCalculateController {

    private static final Logger log = LoggerFactory.getLogger(SimulatorCalculateController.class);

    private final SimulatorCalculatorRegistry registry;
    private final SimulatorsCatalogService catalogService;
    private final ObjectMapper objectMapper;

    public SimulatorCalculateController(SimulatorCalculatorRegistry registry,
                                        SimulatorsCatalogService catalogService,
                                        ObjectMapper objectMapper) {
        this.registry = registry;
        this.catalogService = catalogService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/{toolId}/calculate")
    public ResponseEntity<?> calculate(@PathVariable String toolId,
                                       @RequestBody JsonNode body,
                                       @AuthenticationPrincipal OidcUser oidcUser,
                                       Principal principal) {
        Optional<SimulatorCalculatorDescriptor<?>> descOpt = registry.find(toolId);
        if (descOpt.isEmpty()) {
            log.info("Simulator calculate — unknown toolId {}", toolId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Tool not supported in simulator mode",
                    "toolId", toolId
            ));
        }

        String country = resolveCountry(oidcUser, principal);

        log.info("Simulator calculate — toolId={} country={} user={}",
                toolId, country, oidcUser != null ? oidcUser.getSubject() : "anon");

        Object response = invoke(descOpt.get(), body, country);
        return ResponseEntity.ok(response);
    }

    /**
     * Désérialise le body JSON vers le type de requête du descripteur puis
     * invoque le handler. Toute IllegalArgumentException du calculator est
     * traduite en 422 (validation métier), toute erreur Jackson en 400.
     */
    private <Req> Object invoke(SimulatorCalculatorDescriptor<Req> desc,
                                 JsonNode body,
                                 String country) {
        Req request;
        try {
            request = objectMapper.treeToValue(body, desc.requestType());
        } catch (JsonProcessingException e) {
            log.info("Simulator calculate — invalid JSON for {} : {}", desc.toolId(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid JSON payload : " + e.getOriginalMessage());
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty request body");
        }
        try {
            Object result = desc.handler().apply(request, country);
            if (result == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Calculator returned null result");
            }
            return result;
        } catch (IllegalArgumentException e) {
            // Validation métier (motif inconnu, pays incompatible, champ requis manquant…)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        } catch (NullPointerException e) {
            // Désérialisation NPE liée à un champ obligatoire manquant côté record.
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Required field missing : " + e.getMessage());
        }
    }

    /**
     * Résolution du pays depuis le workspace primary. Délègue à
     * {@link SimulatorsCatalogService} (déjà {@code @Transactional}) pour
     * éviter les {@code LazyInitializationException} sur le workspace.
     */
    private String resolveCountry(OidcUser oidcUser, Principal principal) {
        try {
            SimulatorsCatalogResponse catalog = catalogService.resolveCatalog(oidcUser, principal);
            return catalog != null ? catalog.country() : null;
        } catch (Exception e) {
            log.warn("Simulator calculate — country resolution failed : {}", e.getMessage());
            return null;
        }
    }

    /**
     * Wrapper pour transformer les {@link ResponseStatusException} en payload
     * JSON {@code { error, status, toolId? }} cohérent avec le reste de l'API.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                "error", e.getReason() != null ? e.getReason() : "Erreur",
                "status", e.getStatusCode().value()
        ));
    }
}
