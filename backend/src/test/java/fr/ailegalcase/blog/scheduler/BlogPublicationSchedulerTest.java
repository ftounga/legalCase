package fr.ailegalcase.blog.scheduler;

import fr.ailegalcase.blog.entity.BlogArticle;
import fr.ailegalcase.blog.entity.BlogArticleStatus;
import fr.ailegalcase.blog.image.BlogHeroImageResult;
import fr.ailegalcase.blog.image.BlogHeroImageService;
import fr.ailegalcase.blog.repository.BlogArticleRepository;
import fr.ailegalcase.blog.service.ArticleGenerationResult;
import fr.ailegalcase.blog.service.BlogArticleGeneratorService;
import fr.ailegalcase.blog.service.TopicSelection;
import fr.ailegalcase.blog.service.TopicSelectorService;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlogPublicationSchedulerTest {

    private final TopicSelectorService topicSelectorService = mock(TopicSelectorService.class);
    private final BlogArticleGeneratorService articleGenerator = mock(BlogArticleGeneratorService.class);
    private final BlogHeroImageService heroImageService = mock(BlogHeroImageService.class);
    private final BlogArticleRepository articleRepository = mock(BlogArticleRepository.class);
    private final BlogCircuitBreaker circuitBreaker = mock(BlogCircuitBreaker.class);

    private BlogPublicationScheduler scheduler(boolean enabled) {
        return new BlogPublicationScheduler(enabled, topicSelectorService, articleGenerator,
                heroImageService, articleRepository, circuitBreaker);
    }

    @Test
    void disabled_isNoop() {
        scheduler(false).runDailyPublication();

        verify(circuitBreaker, never()).canGenerateNow();
        verify(topicSelectorService, never()).selectNext();
        verify(articleGenerator, never()).generateForTopic(any());
    }

    @Test
    void dailyLimitReached_skipsRun() {
        when(circuitBreaker.canGenerateNow()).thenReturn(new CircuitBreakerVerdict.DailyLimit(3, 3));

        scheduler(true).runDailyPublication();

        verify(topicSelectorService, never()).selectNext();
    }

    @Test
    void noTopicAvailable_skipsRun() {
        when(circuitBreaker.canGenerateNow()).thenReturn(CircuitBreakerVerdict.ok());
        when(topicSelectorService.selectNext()).thenReturn(Optional.empty());

        scheduler(true).runDailyPublication();

        verify(articleGenerator, never()).generateForTopic(any());
    }

    @Test
    void generationBudgetBlocked_doesNotPublish() {
        UUID topicId = UUID.randomUUID();
        when(circuitBreaker.canGenerateNow()).thenReturn(CircuitBreakerVerdict.ok());
        when(topicSelectorService.selectNext()).thenReturn(Optional.of(
                new TopicSelection(topicId, "slug", "title", "desc", "DROIT_DU_TRAVAIL", "FR")));
        when(articleGenerator.generateForTopic(topicId))
                .thenReturn(new ArticleGenerationResult.BudgetBlocked("CAP"));

        scheduler(true).runDailyPublication();

        verify(heroImageService, never()).generateForArticle(any());
        verify(articleRepository, never()).save(any());
    }

    @Test
    void needsReview_doesNotTriggerHeroNorPublish() {
        UUID topicId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        when(circuitBreaker.canGenerateNow()).thenReturn(CircuitBreakerVerdict.ok());
        when(topicSelectorService.selectNext()).thenReturn(Optional.of(
                new TopicSelection(topicId, "slug", "title", "desc", "DROIT_DU_TRAVAIL", "FR")));
        when(articleGenerator.generateForTopic(topicId))
                .thenReturn(new ArticleGenerationResult.Success(articleId, BlogArticleStatus.NEEDS_REVIEW));

        scheduler(true).runDailyPublication();

        verify(heroImageService, never()).generateForArticle(any());
        verify(articleRepository, never()).save(any());
    }

    @Test
    void draftArticle_triggersHeroAndAutoPublishes() {
        UUID topicId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        when(circuitBreaker.canGenerateNow()).thenReturn(CircuitBreakerVerdict.ok());
        when(topicSelectorService.selectNext()).thenReturn(Optional.of(
                new TopicSelection(topicId, "slug", "title", "desc", "DROIT_DU_TRAVAIL", "FR")));
        when(articleGenerator.generateForTopic(topicId))
                .thenReturn(new ArticleGenerationResult.Success(articleId, BlogArticleStatus.DRAFT));
        when(heroImageService.generateForArticle(articleId))
                .thenReturn(new BlogHeroImageResult.Success(articleId, "/api/v1/blog/articles/x/hero-image"));

        BlogArticle article = new BlogArticle();
        article.setId(articleId);
        article.setStatus(BlogArticleStatus.DRAFT);
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));

        scheduler(true).runDailyPublication();

        verify(heroImageService, times(1)).generateForArticle(articleId);
        verify(articleRepository, times(1)).save(article);
        assertThat(article.getStatus()).isEqualTo(BlogArticleStatus.PUBLISHED);
        assertThat(article.getPublishedAt()).isNotNull();
    }

    @Test
    void heroFails_articleStillPublished() {
        UUID topicId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        when(circuitBreaker.canGenerateNow()).thenReturn(CircuitBreakerVerdict.ok());
        when(topicSelectorService.selectNext()).thenReturn(Optional.of(
                new TopicSelection(topicId, "slug", "title", "desc", "DROIT_DU_TRAVAIL", "FR")));
        when(articleGenerator.generateForTopic(topicId))
                .thenReturn(new ArticleGenerationResult.Success(articleId, BlogArticleStatus.DRAFT));
        when(heroImageService.generateForArticle(articleId))
                .thenReturn(new BlogHeroImageResult.Failed("OPENAI_GENERATION_FAILED", true));

        BlogArticle article = new BlogArticle();
        article.setId(articleId);
        article.setStatus(BlogArticleStatus.DRAFT);
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));

        scheduler(true).runDailyPublication();

        verify(articleRepository, times(1)).save(article);
        assertThat(article.getStatus()).isEqualTo(BlogArticleStatus.PUBLISHED);
        assertThat(article.getHeroImageUrl()).isNull();
    }

    @Test
    void unexpectedException_isCaughtAndDoesNotCrash() {
        when(circuitBreaker.canGenerateNow()).thenThrow(new RuntimeException("boom"));

        // Doit ne pas lever
        scheduler(true).runDailyPublication();
    }
}
