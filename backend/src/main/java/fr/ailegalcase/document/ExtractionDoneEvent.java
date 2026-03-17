package fr.ailegalcase.document;

import java.util.UUID;

public record ExtractionDoneEvent(UUID extractionId, String extractedText) {}
