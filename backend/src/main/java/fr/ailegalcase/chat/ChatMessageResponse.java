package fr.ailegalcase.chat;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        String question,
        String answer,
        String modelUsed,
        boolean useEnriched,
        Instant createdAt
) {
    static ChatMessageResponse from(ChatMessage m) {
        return new ChatMessageResponse(
                m.getId(), m.getQuestion(), m.getAnswer(),
                m.getModelUsed(), m.isUseEnriched(), m.getCreatedAt());
    }
}
