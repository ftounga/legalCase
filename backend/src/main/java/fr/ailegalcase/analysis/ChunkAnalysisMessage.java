package fr.ailegalcase.analysis;

import java.util.UUID;

public record ChunkAnalysisMessage(
        UUID chunkId,
        UUID caseFileId,
        UUID workspaceId,
        String legalDomain,
        String country,
        UUID userId) {

    /** Backward-compat factory: creates a message with no pre-fetched context (fields will be resolved from DB). */
    public static ChunkAnalysisMessage forChunk(UUID chunkId) {
        return new ChunkAnalysisMessage(chunkId, null, null, null, null, null);
    }
}
