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

import java.time.Instant;
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
    // PUT /api/v1/referentials/{id} — modification OWNER/ADMIN
    // -----------------------------------------------------------------------

    @Transactional
    public ReferentialUpdateResponse updateReferential(UUID entryId, UUID workspaceId, UUID userId,
                                                       String newLabel, String newValueJson,
                                                       boolean force,
                                                       ReferentialValidationService validationService) {
        LegalReferential source = repository.findById(entryId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Référentiel introuvable"));

        // Workspace isolation: if workspace entry, must belong to caller's workspace
        if (source.getWorkspaceId() != null && !workspaceId.equals(source.getWorkspaceId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Accès refusé");
        }

        // Validate JSON
        try {
            MAPPER.readTree(newValueJson);
        } catch (Exception e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "valueJson invalide : JSON mal formé");
        }

        // IA validation (skip if force=true)
        if (!force) {
            var validation = validationService.validate(
                    source.getLegalDomain(), source.getReferentialType(),
                    source.getEntryKey(), source.getLabel(),
                    source.getValueJson(), newValueJson);
            if (!validation.valid()) {
                return new ReferentialUpdateResponse(false, null, validation.warning());
            }
        }

        // Upsert: system entry → create/update workspace override; workspace entry → update in-place
        LegalReferential target;
        if (source.getWorkspaceId() == null) {
            List<LegalReferential> overrides = repository.findWorkspaceEntry(
                    workspaceId, source.getLegalDomain(), source.getReferentialType(),
                    source.getEntryKey(), source.getCountry());
            if (!overrides.isEmpty()) {
                target = overrides.get(0);
            } else {
                target = new LegalReferential();
                target.setWorkspaceId(workspaceId);
                target.setLegalDomain(source.getLegalDomain());
                target.setReferentialType(source.getReferentialType());
                target.setEntryKey(source.getEntryKey());
                target.setCountry(source.getCountry());
                target.setSystem(false);
                target.setActive(true);
                target.setSourceRef(source.getSourceRef());
            }
        } else {
            target = source;
        }

        // System entries: label is locked — always use source label
        target.setLabel(source.getWorkspaceId() == null ? source.getLabel() : newLabel);
        target.setValueJson(newValueJson);
        target.setUpdatedAt(Instant.now());
        target.setUpdatedBy(userId);
        LegalReferential saved = repository.save(target);

        return new ReferentialUpdateResponse(true,
                new ReferentialResponse.Entry(saved.getId(), saved.getEntryKey(), saved.getLabel(),
                        saved.getCountry(), saved.getValueJson(), saved.isSystem(), saved.getSourceRef()),
                null);
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
        List<LegalReferential> all = repository.findActiveByDomain(domain, safeWorkspaceId);

        // Deduplicate: workspace override wins over system entry for same (type, key, country).
        // The query orders system DESC, so workspace entries appear last — we keep the first
        // occurrence per (type, key, country) after reversing (workspace entries take priority).
        Map<String, LegalReferential> deduped = new LinkedHashMap<>();
        // First pass: index system entries
        all.stream().filter(LegalReferential::isSystem)
                .forEach(e -> deduped.put(dedupeKey(e), e));
        // Second pass: workspace overrides replace system entries
        all.stream().filter(e -> !e.isSystem())
                .forEach(e -> deduped.put(dedupeKey(e), e));

        Map<String, List<ReferentialResponse.Entry>> sections = deduped.values().stream()
                .sorted(Comparator.comparing(LegalReferential::getReferentialType)
                        .thenComparing(LegalReferential::getEntryKey))
                .collect(Collectors.groupingBy(
                        LegalReferential::getReferentialType,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                e -> new ReferentialResponse.Entry(
                                        e.getId(),
                                        e.getEntryKey(),
                                        e.getLabel(),
                                        e.getCountry(),
                                        e.getValueJson(),
                                        e.isSystem(),
                                        e.getSourceRef()),
                                Collectors.toList())));

        return new ReferentialResponse(domain, sections);
    }

    private static String dedupeKey(LegalReferential e) {
        return e.getReferentialType() + "|" + e.getEntryKey() + "|" + (e.getCountry() != null ? e.getCountry() : "");
    }
}
