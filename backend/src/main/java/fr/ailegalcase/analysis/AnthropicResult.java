package fr.ailegalcase.analysis;

public record AnthropicResult(String content, String modelUsed, int promptTokens, int completionTokens) {}
