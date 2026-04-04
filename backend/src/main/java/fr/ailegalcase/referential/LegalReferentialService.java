package fr.ailegalcase.referential;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.casefile.ImmigrationPieceReferentiel;
import fr.ailegalcase.casefile.ImmigrationProcedureReferentiel;
import fr.ailegalcase.casefile.ImmigrationProcedureReferentiel.ProcedureJalon;
import fr.ailegalcase.casefile.LitigationTypeMapper;
import fr.ailegalcase.casefile.LitigationTypeMapper.LitigationPeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Fournit les données référentiels métier depuis la base de données.
 * Chaque méthode applique un fallback sur les constantes Java hardcodées
 * si la DB ne retourne aucun résultat (fail-open).
 */
@Service
@Transactional(readOnly = true)
public class LegalReferentialService {

    private static final Logger log = LoggerFactory.getLogger(LegalReferentialService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final LegalReferentialRepository repository;

    public LegalReferentialService(LegalReferentialRepository repository) {
        this.repository = repository;
    }

    // -----------------------------------------------------------------------
    // DROIT_DU_TRAVAIL — délai de prescription par type de litige
    // -----------------------------------------------------------------------

    public Optional<LitigationPeriod> getLitigationPeriod(String type) {
        if (type == null) return Optional.empty();
        try {
            List<LegalReferential> entries = repository.findSystemEntry(
                    "DROIT_DU_TRAVAIL", "LITIGATION_TYPE", type.trim().toUpperCase());
            if (!entries.isEmpty()) {
                JsonNode node = MAPPER.readTree(entries.get(0).getValueJson());
                int years = node.get("years").asInt();
                String article = node.get("article").asText();
                String label = entries.get(0).getLabel() != null ? entries.get(0).getLabel() : type;
                return Optional.of(new LitigationPeriod(years, article, label));
            }
        } catch (Exception e) {
            log.warn("LegalReferentialService: fallback getLitigationPeriod({}) — {}", type, e.getMessage());
        }
        return LitigationTypeMapper.resolve(type);
    }

    // -----------------------------------------------------------------------
    // DROIT_IMMIGRATION — jalons procéduraux par type de procédure
    // -----------------------------------------------------------------------

    public List<ProcedureJalon> getImmigrationJalons(String typeProcedure) {
        if (typeProcedure == null) return List.of();
        try {
            List<LegalReferential> entries = repository.findSystemEntry(
                    "DROIT_IMMIGRATION", "IMMIGRATION_JALONS", typeProcedure);
            if (!entries.isEmpty()) {
                JsonNode array = MAPPER.readTree(entries.get(0).getValueJson());
                List<ProcedureJalon> jalons = new ArrayList<>();
                for (JsonNode item : array) {
                    jalons.add(new ProcedureJalon(
                            item.get("label").asText(),
                            item.get("offsetDays").asInt()));
                }
                return Collections.unmodifiableList(jalons);
            }
        } catch (Exception e) {
            log.warn("LegalReferentialService: fallback getImmigrationJalons({}) — {}", typeProcedure, e.getMessage());
        }
        return ImmigrationProcedureReferentiel.resolve(typeProcedure);
    }

    // -----------------------------------------------------------------------
    // DROIT_IMMIGRATION — pièces requises par type de titre et pays
    // -----------------------------------------------------------------------

    public List<String> getImmigrationPieces(String titreType, String country) {
        if (titreType == null || country == null) return List.of();
        try {
            List<LegalReferential> entries = repository.findActiveByDomainAndType(
                    "DROIT_IMMIGRATION", "IMMIGRATION_PIECES", null);
            Optional<LegalReferential> match = entries.stream()
                    .filter(e -> titreType.equalsIgnoreCase(e.getEntryKey())
                            && country.equalsIgnoreCase(e.getCountry())
                            && e.getWorkspaceId() == null)
                    .findFirst();
            if (match.isPresent()) {
                return MAPPER.readValue(match.get().getValueJson(),
                        new TypeReference<List<String>>() {});
            }
        } catch (Exception e) {
            log.warn("LegalReferentialService: fallback getImmigrationPieces({},{}) — {}", titreType, country, e.getMessage());
        }
        return ImmigrationPieceReferentiel.getPieces(titreType, country);
    }

    // -----------------------------------------------------------------------
    // Endpoint GET /api/v1/referentials — données groupées par type
    // -----------------------------------------------------------------------

    public ReferentialResponse getReferentials(String domain, UUID workspaceId) {
        UUID safeWorkspaceId = workspaceId != null ? workspaceId : UUID.fromString("00000000-0000-0000-0000-000000000000");
        List<LegalReferential> entries = repository.findActiveByDomain(domain, safeWorkspaceId);

        Map<String, List<ReferentialResponse.Entry>> sections = entries.stream()
                .collect(Collectors.groupingBy(
                        LegalReferential::getReferentialType,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                e -> new ReferentialResponse.Entry(
                                        e.getEntryKey(),
                                        e.getLabel(),
                                        e.getCountry(),
                                        e.getValueJson(),
                                        e.isSystem(),
                                        e.getSourceRef()),
                                Collectors.toList())));

        return new ReferentialResponse(domain, sections);
    }
}
