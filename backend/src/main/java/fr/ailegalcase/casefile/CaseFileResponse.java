package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.UUID;

public record CaseFileResponse(UUID id, String title, String legalDomain, String description,
                                String status, Instant createdAt) {
}
