package fr.ailegalcase.analysis;

import jakarta.validation.constraints.NotBlank;

public record AiQuestionAnswerRequest(@NotBlank String answerText) {}
