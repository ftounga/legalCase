package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisResponse;
import fr.ailegalcase.casefile.LitigationTypeMapper.LitigationPeriod;
import fr.ailegalcase.referential.LegalReferentialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static fr.ailegalcase.casefile.ImmigrationProcedureReferentiel.ProcedureJalon;

/**
 * Creates or updates STATUTORY CaseDeadlines from the enriched analysis JSON.
 * — DROIT_DU_TRAVAIL / DROIT_FAMILLE : reads "type_litige_detecte" + "date_reference_prescription"
 * — DROIT_IMMIGRATION : reads "date_expiration_titre" + "type_titre_sejour",
 *   produces a deadline with alertThresholds="90,30,7"
 * Fail-open: any parsing or DB error is logged and swallowed.
 */
@Service
public class StatutoryDeadlineService {

    private static final Logger log = LoggerFactory.getLogger(StatutoryDeadlineService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CaseDeadlineRepository deadlineRepository;
    private final LegalReferentialService referentialService;

    public StatutoryDeadlineService(CaseDeadlineRepository deadlineRepository,
                                    LegalReferentialService referentialService) {
        this.deadlineRepository = deadlineRepository;
        this.referentialService = referentialService;
    }

    @Transactional
    public void createStatutoryDeadlines(CaseAnalysis analysis, String rawJson) {
        if (rawJson == null || rawJson.isBlank()) return;
        try {
            String stripped = CaseAnalysisResponse.stripMarkdownCodeBlock(rawJson);
            JsonNode root = MAPPER.readTree(stripped);

            String legalDomain = analysis.getCaseFile().getLegalDomain();

            if ("DROIT_IMMIGRATION".equals(legalDomain)) {
                createImmigrationTitreDeadline(analysis, root);
                createImmigrationProcedureDeadlines(analysis, root);
            } else {
                createPrescriptionDeadline(analysis, root);
            }
        } catch (Exception e) {
            log.warn("StatutoryDeadline: fail-open for analysis {} — {}",
                    analysis.getId(), e.getMessage());
        }
    }

    private void createImmigrationTitreDeadline(CaseAnalysis analysis, JsonNode root) {
        JsonNode dateNode = root.get("date_expiration_titre");
        if (dateNode == null || dateNode.isNull()) {
            log.debug("StatutoryDeadline: missing date_expiration_titre for analysis {} — skipped",
                    analysis.getId());
            return;
        }
        LocalDate expirationDate;
        try {
            expirationDate = LocalDate.parse(dateNode.asText());
        } catch (Exception e) {
            log.debug("StatutoryDeadline: invalid date_expiration_titre '{}' for analysis {} — skipped",
                    dateNode.asText(), analysis.getId());
            return;
        }

        JsonNode typeNode = root.get("type_titre_sejour");
        String documentType = (typeNode != null && !typeNode.isNull() && !typeNode.asText().isBlank())
                ? typeNode.asText()
                : "TITRE_SEJOUR";

        upsertDeadline(analysis.getCaseFile(), "Expiration titre de séjour",
                expirationDate, "STATUTORY", "90,30,7", documentType);
    }

    private void createImmigrationProcedureDeadlines(CaseAnalysis analysis, JsonNode root) {
        JsonNode typeNode = root.get("type_procedure_detectee");
        if (typeNode == null || typeNode.isNull()) {
            log.debug("StatutoryDeadline: missing type_procedure_detectee for analysis {} — skipped",
                    analysis.getId());
            return;
        }

        String typeProcedure = typeNode.asText();
        List<ProcedureJalon> jalons = referentialService.getImmigrationJalons(typeProcedure);
        if (jalons.isEmpty()) {
            log.debug("StatutoryDeadline: unknown type_procedure_detectee '{}' for analysis {} — skipped",
                    typeProcedure, analysis.getId());
            return;
        }

        JsonNode dateNode = root.get("date_depot_procedure");
        if (dateNode == null || dateNode.isNull()) {
            log.debug("StatutoryDeadline: missing date_depot_procedure for analysis {} — skipped",
                    analysis.getId());
            return;
        }
        LocalDate dateDepot;
        try {
            dateDepot = LocalDate.parse(dateNode.asText());
        } catch (Exception e) {
            log.debug("StatutoryDeadline: invalid date_depot_procedure '{}' for analysis {} — skipped",
                    dateNode.asText(), analysis.getId());
            return;
        }

        for (ProcedureJalon jalon : jalons) {
            LocalDate dueDate = dateDepot.plusDays(jalon.offsetDays());
            upsertDeadline(analysis.getCaseFile(), jalon.label(), dueDate, "STATUTORY", null, typeProcedure);
        }
    }

    private void createPrescriptionDeadline(CaseAnalysis analysis, JsonNode root) {
        JsonNode typeNode = root.get("type_litige_detecte");
        if (typeNode == null || typeNode.isNull()) return;

        Optional<LitigationPeriod> periodOpt =
                referentialService.getLitigationPeriod(typeNode.asText());
        if (periodOpt.isEmpty()) {
            log.debug("StatutoryDeadline: unknown litigation type '{}' for analysis {} — skipped",
                    typeNode.asText(), analysis.getId());
            return;
        }

        LitigationPeriod period = periodOpt.get();
        LocalDate referenceDate = resolveReferenceDate(root, analysis);
        LocalDate dueDate = referenceDate.plusYears(period.years());
        String label = "Prescription — %s (%s)".formatted(period.label(), period.article());

        upsertDeadline(analysis.getCaseFile(), label, dueDate, "STATUTORY", null, null);
    }

    private LocalDate resolveReferenceDate(JsonNode root, CaseAnalysis analysis) {
        JsonNode dateNode = root.get("date_reference_prescription");
        if (dateNode != null && !dateNode.isNull()) {
            try {
                return LocalDate.parse(dateNode.asText());
            } catch (Exception e) {
                log.debug("StatutoryDeadline: invalid date_reference_prescription '{}' — using createdAt fallback",
                        dateNode.asText());
            }
        }
        return analysis.getCaseFile().getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
    }

    private void upsertDeadline(CaseFile caseFile, String label, LocalDate dueDate,
                                String source, String alertThresholds, String documentType) {
        List<CaseDeadline> existing = deadlineRepository
                .findByCaseFileIdOrderByDueDateAsc(caseFile.getId())
                .stream()
                .filter(d -> source.equals(d.getSource()) && label.equals(d.getLabel()))
                .toList();
        existing.forEach(deadlineRepository::delete);
        deadlineRepository.flush();

        CaseDeadline deadline = new CaseDeadline();
        deadline.setCaseFile(caseFile);
        deadline.setLabel(label);
        deadline.setDueDate(dueDate);
        deadline.setSource(source);
        deadline.setAlertThresholds(alertThresholds);
        deadline.setDocumentType(documentType);
        deadlineRepository.save(deadline);
    }
}
