package fr.ailegalcase.referential;

import fr.ailegalcase.analysis.AiCallContext;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.analysis.JobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Validates a proposed referential value against official legal values via Claude Haiku.
 * Fail-open: any Anthropic error is swallowed and treated as "VALID" to avoid blocking the OWNER.
 */
@Service
public class ReferentialValidationService {

    private static final Logger log = LoggerFactory.getLogger(ReferentialValidationService.class);
    private static final int MAX_TOKENS = 512;

    private static final String SYSTEM_PROMPT = """
            Tu es un expert juridique spécialisé en droit français et belge.
            Un administrateur de cabinet d'avocats souhaite modifier une valeur de référentiel métier.
            Ton rôle est de vérifier si la valeur proposée est cohérente avec les valeurs officielles en vigueur.

            Réponds UNIQUEMENT par l'une des deux formes suivantes :
            - VALID (si la valeur proposée est conforme au droit en vigueur)
            - WARNING: [explication courte en français de la valeur officielle correcte] (si divergence)

            Ne fournis aucune autre explication. Sois concis.
            """;

    private final AnthropicService anthropicService;

    public ReferentialValidationService(AnthropicService anthropicService) {
        this.anthropicService = anthropicService;
    }

    public record ValidationResult(boolean valid, String warning) {
        static ValidationResult ok() { return new ValidationResult(true, null); }
        static ValidationResult warn(String warning) { return new ValidationResult(false, warning); }
    }

    public ValidationResult validate(String domain, String type, String key, String label,
                                     String currentValueJson, String proposedValueJson) {
        try {
            String userMessage = """
                    Domaine juridique : %s
                    Type de référentiel : %s
                    Clé : %s
                    Libellé : %s
                    Valeur actuelle : %s
                    Valeur proposée : %s
                    """.formatted(domain, type, key, label, currentValueJson, proposedValueJson);

            AiCallContext ctx = AiCallContext.systemLevel(JobType.SYSTEM_REFERENTIAL_CHECK);
            var result = anthropicService.analyzeFast(ctx, SYSTEM_PROMPT, userMessage, MAX_TOKENS);
            String content = result.content().trim();

            if (content.startsWith("WARNING:")) {
                String warning = content.substring("WARNING:".length()).trim();
                return ValidationResult.warn(warning);
            }
            return ValidationResult.ok();
        } catch (Exception e) {
            log.warn("ReferentialValidation: Anthropic unavailable for {}/{} — fail-open: {}",
                    domain, key, e.getMessage());
            return ValidationResult.ok();
        }
    }
}
