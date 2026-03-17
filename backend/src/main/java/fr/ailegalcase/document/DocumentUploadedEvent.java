package fr.ailegalcase.document;

import java.util.UUID;

public record DocumentUploadedEvent(UUID documentId, String storageKey, String contentType) {}
