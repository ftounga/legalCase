package fr.ailegalcase.help;

import fr.ailegalcase.analysis.AiCallContext;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.analysis.JobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HelpChatService {

    private static final Logger log = LoggerFactory.getLogger(HelpChatService.class);

    private static final int MAX_TOKENS = 512;

    private final AnthropicService anthropicService;
    private final String systemPrompt;

    public HelpChatService(AnthropicService anthropicService, HelpDocumentLoader helpDocumentLoader) {
        this.anthropicService = anthropicService;
        this.systemPrompt = buildSystemPrompt(helpDocumentLoader.getDocumentation());
    }

    public HelpChatResponse chat(String message) {
        try {
            AiCallContext ctx = AiCallContext.systemLevel(JobType.SYSTEM_HELP_CHAT);
            var result = anthropicService.analyzeFast(ctx, systemPrompt, message, MAX_TOKENS);
            return new HelpChatResponse(result.content());
        } catch (Exception e) {
            log.warn("Anthropic unavailable for help chat: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Le service d'aide est temporairement indisponible, veuillez réessayer dans quelques instants.");
        }
    }

    private static String buildSystemPrompt(String documentation) {
        return """
                Tu es un assistant d'aide à l'utilisation de AI LegalCase, une application SaaS d'analyse juridique pour avocats.
                Réponds uniquement aux questions relatives à l'utilisation de l'application.
                Si la question ne concerne pas l'utilisation de l'application, réponds poliment que tu n'es disponible que pour aider sur AI LegalCase.
                Réponds en français, de façon concise et pratique, en 3 à 5 phrases maximum.
                Ne fournis pas de conseils juridiques.

                Voici la documentation complète de l'application :

                """ + documentation;
    }
}
