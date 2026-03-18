package fr.ailegalcase.analysis;

import java.util.UUID;

public record AiQuestionResponse(UUID id, int orderIndex, String questionText, String answerText) {

    static AiQuestionResponse from(AiQuestion question, String answerText) {
        return new AiQuestionResponse(question.getId(), question.getOrderIndex(), question.getQuestionText(), answerText);
    }
}
