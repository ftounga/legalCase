package fr.ailegalcase.analysis;

public record AiQuestionResponse(int orderIndex, String questionText) {

    static AiQuestionResponse from(AiQuestion question) {
        return new AiQuestionResponse(question.getOrderIndex(), question.getQuestionText());
    }
}
