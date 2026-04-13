package fr.ailegalcase.analysis;

import java.util.UUID;

public record AiQuestionResponse(UUID id, int orderIndex, String questionText, String answerText,
                                  String critereCode, String expectedValue) {

    static AiQuestionResponse from(AiQuestion question, String answerText) {
        return new AiQuestionResponse(question.getId(), question.getOrderIndex(),
                question.getQuestionText(), answerText, question.getCritereCode(),
                question.getExpectedValue());
    }
}
