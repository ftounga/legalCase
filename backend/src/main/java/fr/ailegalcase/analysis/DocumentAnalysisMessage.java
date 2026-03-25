package fr.ailegalcase.analysis;

import java.util.UUID;

public record DocumentAnalysisMessage(UUID extractionId, boolean directAnalysis) {}
