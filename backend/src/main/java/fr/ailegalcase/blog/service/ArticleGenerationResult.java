package fr.ailegalcase.blog.service;

import fr.ailegalcase.blog.entity.BlogArticleStatus;

import java.util.UUID;

/**
 * Résultat de {@link BlogArticleGeneratorService#generateForTopic(UUID)}.
 *
 * <ul>
 *   <li>{@link Success} : article créé en {@code DRAFT} ou {@code NEEDS_REVIEW}.</li>
 *   <li>{@link BudgetBlocked} : cap mensuel atteint, aucun appel IA, topic libéré.</li>
 *   <li>{@link GenerationFailed} : erreur définitive (JSON invalide, retries épuisés…),
 *       topic libéré.</li>
 * </ul>
 */
public sealed interface ArticleGenerationResult
        permits ArticleGenerationResult.Success,
                ArticleGenerationResult.BudgetBlocked,
                ArticleGenerationResult.GenerationFailed {

    record Success(UUID articleId, BlogArticleStatus status) implements ArticleGenerationResult {}

    record BudgetBlocked(String reason) implements ArticleGenerationResult {}

    record GenerationFailed(String reason) implements ArticleGenerationResult {}
}
