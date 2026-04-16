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
        String secondaryText,
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
                    actionType = "OPEN_DOCUMENTS_LIST";
                    actionTarget = null;
                }
            }
            case QUESTION_AI -> {
                if (e.getAnchorQuestionId() != null) {
                    actionType = "SCROLL_QA";
                    actionTarget = e.getAnchorQuestionId().toString();
                } else {
                    actionType = "OPEN_QUESTIONS";
                    actionTarget = null;
                }
            }
            case CHECKLIST_F96 -> {
                if (e.getAnchorF96Code() != null) {
                    actionType = "SCROLL_F96";
                    actionTarget = e.getAnchorF96Code();
                } else {
                    actionType = "OPEN_F96_LIST";
                    actionTarget = null;
                }
            }
            case CHAT -> {
                actionType = e.getAnchorChatMessageId() != null ? "OPEN_CHAT" : "OPEN_CHAT";
                actionTarget = e.getAnchorChatMessageId() != null ? e.getAnchorChatMessageId().toString() : null;
            }
            case MISSING_PIECE -> {
                actionType = "OPEN_MISSING_PIECES";
                actionTarget = e.getAnchorPieceIndex() != null ? String.valueOf(e.getAnchorPieceIndex()) : null;
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
                e.getSecondaryText(),
                actionType,
                actionTarget
        );
    }
}
