package fr.ailegalcase.analysis;

import java.util.List;
import java.util.UUID;

public record ProcedureCheckRequalifiedEvent(
        UUID caseFileId,
        String caseFileTitle,
        String creatorEmail,
        List<RequalifiedCheck> requalifiedChecks
) {
    public record RequalifiedCheck(String description, ProcedureCheckStatus newStatus, String raison) {}
}
