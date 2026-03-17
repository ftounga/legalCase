package fr.ailegalcase.document;

import java.util.List;
import java.util.UUID;

public record ChunkingDoneEvent(UUID extractionId, List<UUID> chunkIds) {}
