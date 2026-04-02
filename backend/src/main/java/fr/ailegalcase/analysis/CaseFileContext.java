package fr.ailegalcase.analysis;

import java.util.UUID;

public record CaseFileContext(UUID workspaceId, String legalDomain, String country, UUID userId) {}
