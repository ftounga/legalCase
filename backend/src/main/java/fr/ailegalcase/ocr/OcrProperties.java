package fr.ailegalcase.ocr;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration AWS Textract — SF-122-01.
 *
 * En dev, {@code enabled=false} pour éviter tout appel AWS.
 * En staging/prod, {@code enabled=true} via variables d'environnement.
 */
@ConfigurationProperties(prefix = "aws.textract")
public record OcrProperties(
        boolean enabled,
        String region,
        int maxSizeMb,
        int maxPages,
        int maxConcurrentCalls
) {
    public OcrProperties {
        if (region == null || region.isBlank()) region = "eu-west-3";
        if (maxSizeMb <= 0) maxSizeMb = 5;
        if (maxPages <= 0) maxPages = 11;
        // SF-122-09 : 3 permits par défaut — limite les calls Textract simultanés
        // pour éviter ProvisionedThroughputExceededException en cas de burst
        // (9 extractions parallèles taperaient sinon 9x AWS en même temps).
        if (maxConcurrentCalls <= 0) maxConcurrentCalls = 3;
    }
}
