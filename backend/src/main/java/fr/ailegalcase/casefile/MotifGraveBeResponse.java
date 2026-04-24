package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MotifGraveBeResponse(
        UUID caseFileId,
        LocalDate dateConnaissanceFait,
        LocalDate dateNotificationRupture,
        LocalDate dateNotificationMotifs,
        int anciennetteAnnees,
        BigDecimal salaireMensuelReference,
        int delaiRuptureJoursOuvrables,
        int delaiMotifsJoursOuvrables,
        boolean motifGraveProceduralementValide,
        BigDecimal indemnitePreavisSiInvalide,
        BigDecimal indemniteManifestementDeraisonnableMin,
        BigDecimal indemniteManifestementDeraisonnableMax,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
