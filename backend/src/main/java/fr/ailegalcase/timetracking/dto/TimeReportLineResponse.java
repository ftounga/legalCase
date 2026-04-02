package fr.ailegalcase.timetracking.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TimeReportLineResponse(
        UUID caseFileId,
        String caseFileTitle,
        UUID userId,
        String userEmail,
        long totalSeconds,
        BigDecimal ratePerHour,
        BigDecimal amount
) {}
