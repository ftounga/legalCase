package fr.ailegalcase.analysis;

/**
 * @param stopReason "end_turn" (réponse complète), "max_tokens" (tronquée —
 *                   le budget d'output a été atteint), "stop_sequence" (autre).
 *                   Permet aux consumers de détecter les cas où la réponse a
 *                   été coupée en cours de génération.
 */
public record AnthropicResult(
        String content,
        String modelUsed,
        int promptTokens,
        int completionTokens,
        String stopReason
) {
    public AnthropicResult(String content, String modelUsed, int promptTokens, int completionTokens) {
        this(content, modelUsed, promptTokens, completionTokens, "end_turn");
    }

    public boolean wasTruncated() {
        return "max_tokens".equals(stopReason);
    }
}
