package fr.ailegalcase.analysis;

import java.util.UUID;

/**
 * Réponse frontend : une phrase d'explication de source pour un sourceKey,
 * enrichie du type d'action et de la cible (anchor) pour le lien "Voir la source".
 */
public record SourceExplanationResponse(
        String sourceKey,
        String sourceType,
        String label,
        String sentence,
        String actionType,
        String actionTarget
) {
    public static SourceExplanationResponse from(SourceExplanation e) {
        String actionType;
        String actionTarget;
        switch (e.getSourceType()) {
            case DOCUMENT -> {
                if (e.getAnchorDocId() != null) {
                    actionType = "OPEN_DOCUMENT";
                    actionTarget = e.getAnchorDocId().toString();
                } else {
                    actionType = "NONE";
                    actionTarget = null;
                }
            }
            case QUESTION_AI -> {
                actionType = e.getAnchorQuestionId() != null ? "SCROLL_QA" : "NONE";
                actionTarget = e.getAnchorQuestionId() != null ? e.getAnchorQuestionId().toString() : null;
            }
            case CHECKLIST_F96 -> {
                actionType = e.getAnchorF96Code() != null ? "SCROLL_F96" : "NONE";
                actionTarget = e.getAnchorF96Code();
            }
            case CHAT -> {
                actionType = e.getAnchorChatMessageId() != null ? "OPEN_CHAT" : "NONE";
                actionTarget = e.getAnchorChatMessageId() != null ? e.getAnchorChatMessageId().toString() : null;
            }
            case MISSING_PIECE -> {
                actionType = "MISSING_PIECE";
                actionTarget = null;
            }
            default -> {
                actionType = "NONE";
                actionTarget = null;
            }
        }
        return new SourceExplanationResponse(
                e.getSourceKey(),
                e.getSourceType().name(),
                e.getLabel(),
                e.getSentence(),
                actionType,
                actionTarget
        );
    }
}
