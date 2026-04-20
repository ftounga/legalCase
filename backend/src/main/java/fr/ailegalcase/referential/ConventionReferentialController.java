package fr.ailegalcase.referential;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SF-129-01 : expose la liste des conventions collectives seedées en DB pour
 * alimenter le dropdown dynamique frontend (au lieu du hardcode à 5 FR + 3 BE).
 *
 * Endpoint lecture-seule, accessible à tout utilisateur authentifié. Les données
 * sont system-wide (workspace_id NULL) et partagées par tous les workspaces.
 */
@RestController
@RequestMapping("/api/v1/referentials/conventions")
public class ConventionReferentialController {

    private static final Logger log = LoggerFactory.getLogger(ConventionReferentialController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final LegalReferentialRepository repository;

    public ConventionReferentialController(LegalReferentialRepository repository) {
        this.repository = repository;
    }

    /**
     * Renvoie toutes les CCN actives (FR + BE), triées par pays puis label.
     * Format aligné avec le frontend existant : {code, label, country}.
     */
    @GetMapping
    public List<ConventionOption> list() {
        List<LegalReferential> france = repository.findSystemEntriesByTypeAndCountry(
                "DROIT_DU_TRAVAIL", "CONVENTION_BAREMES", "FRANCE");
        List<LegalReferential> belgique = repository.findSystemEntriesByTypeAndCountry(
                "DROIT_DU_TRAVAIL", "CONVENTION_BAREMES", "BELGIQUE");

        List<ConventionOption> result = new ArrayList<>(france.size() + belgique.size());
        result.addAll(france.stream().map(this::toOption).sorted(
                (a, b) -> a.label().compareToIgnoreCase(b.label())).toList());
        result.addAll(belgique.stream().map(this::toOption).sorted(
                (a, b) -> a.label().compareToIgnoreCase(b.label())).toList());
        return result;
    }

    private ConventionOption toOption(LegalReferential entry) {
        return new ConventionOption(entry.getEntryKey(), entry.getLabel(), entry.getCountry());
    }

    /** DTO léger renvoyé au frontend. */
    public record ConventionOption(String code, String label, String country) {}
}
