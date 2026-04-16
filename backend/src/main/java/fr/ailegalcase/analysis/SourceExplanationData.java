package fr.ailegalcase.analysis;

import java.util.UUID;

/**
 * DTO portant une phrase d'explication de source produite par Haiku
 * (SF-IA-03-15a). Tous les champs d'ancre sont optionnels ; un seul
 * devrait être renseigné à la fois, cohérent avec sourceType.
 */
public record SourceExplanationData(
        String sourceKey,
        String sourceType,
        String label,
        String sentence,
        UUID anchorDocId,
        UUID anchorQuestionId,
        String anchorF96Code,
        UUID anchorChatMessageId
) {}
