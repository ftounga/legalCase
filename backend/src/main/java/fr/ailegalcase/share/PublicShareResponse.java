package fr.ailegalcase.share;

import fr.ailegalcase.analysis.CaseAnalysisResponse;

import java.time.Instant;
import java.util.UUID;

public record PublicShareResponse(
        UUID caseFileId,
        String caseFileTitle,
        String legalDomain,
        Instant expiresAt,
        CaseAnalysisResponse synthesis
) {}
