package fr.ailegalcase.blog.image;

import fr.ailegalcase.blog.cost.BlogUsageEventType;
import fr.ailegalcase.blog.cost.IaCostTracker;
import fr.ailegalcase.blog.cost.IaProvider;
import fr.ailegalcase.blog.entity.BlogArticle;
import fr.ailegalcase.blog.entity.BlogArticleStatus;
import fr.ailegalcase.blog.repository.BlogArticleRepository;
import fr.ailegalcase.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlogHeroImageServiceTest {

    private final BlogArticleRepository articleRepository = mock(BlogArticleRepository.class);
    private final RetryableImageGenerator imageGenerator = mock(RetryableImageGenerator.class);
    private final StorageService storageService = mock(StorageService.class);
    private final IaCostTracker costTracker = mock(IaCostTracker.class);

    private BlogHeroImageService service;
    private BlogArticle article;

    @BeforeEach
    void setUp() {
        service = new BlogHeroImageService(articleRepository, imageGenerator, storageService,
                costTracker, "gpt-image-1", new BigDecimal("0.037"));
        article = new BlogArticle();
        article.setId(UUID.randomUUID());
        article.setSlug("rupture-conventionnelle");
        article.setTitle("Rupture conventionnelle : qui paye les frais d'avocat ?");
        article.setLegalDomain("DROIT_DU_TRAVAIL");
        article.setStatus(BlogArticleStatus.DRAFT);
        article.setHeroImageUrl(null);
        when(articleRepository.findById(article.getId())).thenReturn(Optional.of(article));
        when(articleRepository.save(any(BlogArticle.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void generateForArticle_articleNotFound_skipped() {
        UUID unknown = UUID.randomUUID();
        when(articleRepository.findById(unknown)).thenReturn(Optional.empty());

        BlogHeroImageResult result = service.generateForArticle(unknown);

        assertThat(result).isInstanceOf(BlogHeroImageResult.Skipped.class);
        assertThat(((BlogHeroImageResult.Skipped) result).reason()).isEqualTo("ARTICLE_NOT_FOUND");
    }

    @Test
    void generateForArticle_alreadyHasHero_skipped() {
        article.setHeroImageUrl("/api/v1/blog/articles/X/hero-image");

        BlogHeroImageResult result = service.generateForArticle(article.getId());

        assertThat(result).isInstanceOf(BlogHeroImageResult.Skipped.class);
        verify(imageGenerator, never()).generate(any());
    }

    @Test
    void generateForArticle_needsReview_skipped() {
        article.setStatus(BlogArticleStatus.NEEDS_REVIEW);

        BlogHeroImageResult result = service.generateForArticle(article.getId());

        assertThat(result).isInstanceOf(BlogHeroImageResult.Skipped.class);
        verify(imageGenerator, never()).generate(any());
    }

    @Test
    void generateForArticle_budgetCapReached_returnsBudgetBlocked() {
        when(costTracker.canSpend(eq(IaProvider.OPENAI), any())).thenReturn(false);

        BlogHeroImageResult result = service.generateForArticle(article.getId());

        assertThat(result).isInstanceOf(BlogHeroImageResult.BudgetBlocked.class);
        verify(imageGenerator, never()).generate(any());
        verify(storageService, never()).upload(anyString(), any(InputStream.class), anyString(), anyLong());
    }

    @Test
    void generateForArticle_nominal_uploadsAndPersistsAndRecordsCost() {
        byte[] imageBytes = new byte[]{1, 2, 3, 4, 5};
        when(costTracker.canSpend(eq(IaProvider.OPENAI), any())).thenReturn(true);
        when(imageGenerator.generate(anyString())).thenReturn(
                new RetryableImageGenerator.Outcome.Success(
                        new OpenAiImageClient.ImageGenerationResponse(imageBytes, "gpt-image-1", "p")));

        BlogHeroImageResult result = service.generateForArticle(article.getId());

        assertThat(result).isInstanceOf(BlogHeroImageResult.Success.class);
        BlogHeroImageResult.Success success = (BlogHeroImageResult.Success) result;
        assertThat(success.heroUrl()).isEqualTo("/api/v1/blog/articles/" + article.getId() + "/hero-image");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService).upload(keyCaptor.capture(), any(InputStream.class), eq("image/png"), eq((long) imageBytes.length));
        assertThat(keyCaptor.getValue()).isEqualTo("blog/articles/" + article.getId() + "/hero.png");

        ArgumentCaptor<BlogArticle> articleCaptor = ArgumentCaptor.forClass(BlogArticle.class);
        verify(articleRepository).save(articleCaptor.capture());
        BlogArticle saved = articleCaptor.getValue();
        assertThat(saved.getHeroImageUrl()).isEqualTo(success.heroUrl());
        assertThat(saved.getHeroImageAlt()).startsWith("Illustration : ");

        verify(costTracker).record(eq(IaProvider.OPENAI), eq("gpt-image-1"),
                eq(BlogUsageEventType.HERO_IMAGE_GENERATION), eq(article.getId()),
                eq(0), eq(0), eq(new BigDecimal("0.037")));
    }

    @Test
    void generateForArticle_definitiveFailure_returnsFailed_articleUnchanged() {
        when(costTracker.canSpend(eq(IaProvider.OPENAI), any())).thenReturn(true);
        when(imageGenerator.generate(anyString())).thenReturn(
                new RetryableImageGenerator.Outcome.Failed(true, new RuntimeException("400")));

        BlogHeroImageResult result = service.generateForArticle(article.getId());

        assertThat(result).isInstanceOf(BlogHeroImageResult.Failed.class);
        assertThat(((BlogHeroImageResult.Failed) result).wasDefinitive()).isTrue();
        verify(storageService, never()).upload(anyString(), any(InputStream.class), anyString(), anyLong());
        verify(costTracker, never()).record(any(), anyString(), any(), any(), anyInt(), anyInt(), any());
        // Article reste sans hero
        assertThat(article.getHeroImageUrl()).isNull();
    }

    @Test
    void generateForArticle_transientFailureExhausted_returnsFailed() {
        when(costTracker.canSpend(eq(IaProvider.OPENAI), any())).thenReturn(true);
        when(imageGenerator.generate(anyString())).thenReturn(
                new RetryableImageGenerator.Outcome.Failed(false, new RuntimeException("503")));

        BlogHeroImageResult result = service.generateForArticle(article.getId());

        assertThat(result).isInstanceOf(BlogHeroImageResult.Failed.class);
        assertThat(((BlogHeroImageResult.Failed) result).wasDefinitive()).isFalse();
    }

    @Test
    void buildPrompt_includesTitleCategoryAndStyle() {
        article.setLegalDomain("DROIT_FAMILLE");
        article.setTitle("Divorce et garde alternée");

        String prompt = service.buildPrompt(article);

        assertThat(prompt)
                .contains("Divorce et garde alternée")
                .contains("family law")
                .contains("navy blue (#0B2147)")
                .contains("16:9");
    }

    @Test
    void buildPrompt_unknownCategory_fallback() {
        article.setLegalDomain("DROIT_INCONNU");

        String prompt = service.buildPrompt(article);

        assertThat(prompt).contains("legal context");
    }
}
