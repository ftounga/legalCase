package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;

/**
 * F-285 / SF-285-01 — vue de la qualification d'entrée d'un dossier.
 * {@code qualified=false} + {@code qualifiedAt=null} ⇒ dossier jamais qualifié
 * (état vide côté UI).
 */
public record CaseIntakeResponse(
        String disputeType,
        AdmissibilityStatus admissibility,
        PrescriptionStatus prescriptionStatus,
        LocalDate prescriptionDeadline,
        String jurisdiction,
        Long estimatedValueCents,
        String notes,
        Instant qualifiedAt,
        boolean qualified
) {
    static CaseIntakeResponse from(CaseFile caseFile) {
        boolean qualified = caseFile.getIntakeDisputeType() != null
                || caseFile.getIntakeAdmissibility() != null
                || caseFile.getIntakePrescriptionStatus() != null
                || caseFile.getIntakePrescriptionDeadline() != null
                || caseFile.getIntakeJurisdiction() != null
                || caseFile.getIntakeEstimatedValueCents() != null
                || caseFile.getIntakeNotes() != null;
        return new CaseIntakeResponse(
                caseFile.getIntakeDisputeType(),
                caseFile.getIntakeAdmissibility(),
                caseFile.getIntakePrescriptionStatus(),
                caseFile.getIntakePrescriptionDeadline(),
                caseFile.getIntakeJurisdiction(),
                caseFile.getIntakeEstimatedValueCents(),
                caseFile.getIntakeNotes(),
                caseFile.getIntakeQualifiedAt(),
                qualified);
    }
}
