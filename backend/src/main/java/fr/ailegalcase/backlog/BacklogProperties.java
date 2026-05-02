package fr.ailegalcase.backlog;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * F-178 SF-178-01 : configuration des chemins vers les fichiers Markdown
 * source du backlog (PRODUCT_SPEC.md + MARKETING_BACKLOG.md).
 *
 * Les chemins peuvent être absolus ou relatifs au répertoire de travail.
 * En prod, les fichiers sont packagés dans l'image Docker à
 * {@code /app/docs/PRODUCT_SPEC.md}. En dev, le chemin par défaut
 * {@code docs/PRODUCT_SPEC.md} pointe vers le repo cloné.
 */
@ConfigurationProperties(prefix = "app.backlog")
public record BacklogProperties(
        String productSpecPath,
        String marketingBacklogPath,
        int staleAfterMinutes
) {
    public BacklogProperties {
        if (productSpecPath == null || productSpecPath.isBlank()) {
            productSpecPath = "docs/PRODUCT_SPEC.md";
        }
        if (marketingBacklogPath == null || marketingBacklogPath.isBlank()) {
            marketingBacklogPath = "docs/MARKETING_BACKLOG.md";
        }
        if (staleAfterMinutes <= 0) {
            staleAfterMinutes = 10;
        }
    }
}
