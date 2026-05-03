package fr.ailegalcase.blog.image;

import java.util.UUID;

/**
 * Résultat de {@link BlogHeroImageService#generateForArticle(UUID)}.
 *
 * <ul>
 *   <li>{@link Success} : image générée, stockée S3, URL persistée.</li>
 *   <li>{@link BudgetBlocked} : cap mensuel OpenAI atteint, aucun appel.</li>
 *   <li>{@link Skipped} : article inéligible (NEEDS_REVIEW, déjà avec hero, etc.).</li>
 *   <li>{@link Failed} : échec après retries — article publié sans hero,
 *       événement loggé.</li>
 * </ul>
 */
public sealed interface BlogHeroImageResult
        permits BlogHeroImageResult.Success,
                BlogHeroImageResult.BudgetBlocked,
                BlogHeroImageResult.Skipped,
                BlogHeroImageResult.Failed {

    record Success(UUID articleId, String heroUrl) implements BlogHeroImageResult {}

    record BudgetBlocked(String reason) implements BlogHeroImageResult {}

    record Skipped(String reason) implements BlogHeroImageResult {}

    record Failed(String reason, boolean wasDefinitive) implements BlogHeroImageResult {}
}
