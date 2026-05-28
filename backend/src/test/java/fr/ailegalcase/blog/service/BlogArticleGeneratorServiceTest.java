package fr.ailegalcase.blog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AiCallContext;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.blog.cost.BlogUsageEventType;
import fr.ailegalcase.blog.cost.IaCostTracker;
import fr.ailegalcase.blog.cost.IaProvider;
import fr.ailegalcase.blog.entity.BlogArticle;
import fr.ailegalcase.blog.entity.BlogArticleStatus;
import fr.ailegalcase.blog.entity.BlogTopic;
import fr.ailegalcase.blog.entity.BlogTopicStatus;
import fr.ailegalcase.blog.repository.BlogArticleRepository;
import fr.ailegalcase.blog.repository.BlogTopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlogArticleGeneratorServiceTest {

    private final AnthropicService anthropicService = mock(AnthropicService.class);
    private final BlogTopicRepository topicRepository = mock(BlogTopicRepository.class);
    private final BlogArticleRepository articleRepository = mock(BlogArticleRepository.class);
    private final TopicSelectorService topicSelectorService = mock(TopicSelectorService.class);
    private final BlogPromptBuilder promptBuilder = new BlogPromptBuilder();
    private final IaCostTracker costTracker = mock(IaCostTracker.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LegalCitationVerifier citationVerifier = mock(LegalCitationVerifier.class);

    private BlogArticleGeneratorService service;
    private BlogTopic topic;

    @BeforeEach
    void setUp() {
        service = new BlogArticleGeneratorService(
                anthropicService, topicRepository, articleRepository,
                topicSelectorService, promptBuilder, citationVerifier, costTracker, objectMapper,
                "claude-sonnet-4-6", 6000,
                new BigDecimal("2.80"), new BigDecimal("14.00"),
                new BigDecimal("0.75"), new BigDecimal("3.75"));
        topic = new BlogTopic();
        topic.setId(UUID.randomUUID());
        topic.setSlug("rupture-conventionnelle-frais-avocat");
        topic.setTitle("Rupture conventionnelle : qui prend en charge les frais d'avocat ?");
        topic.setDescription("Article SEO");
        topic.setCategory("DROIT_DU_TRAVAIL");
        topic.setCountryScope("FR");
        topic.setStatus(BlogTopicStatus.RESERVED);

        when(topicRepository.findById(topic.getId())).thenReturn(Optional.of(topic));
        when(articleRepository.save(any(BlogArticle.class))).thenAnswer(inv -> {
            BlogArticle a = inv.getArgument(0);
            if (a.getId() == null) a.setId(UUID.randomUUID());
            return a;
        });
        when(citationVerifier.verify(any())).thenReturn(LegalCitationVerifier.VerifierOutcome.empty());
    }

    @Test
    void generateForTopic_topicNotFound_returnsGenerationFailed() {
        UUID unknown = UUID.randomUUID();
        when(topicRepository.findById(unknown)).thenReturn(Optional.empty());

        ArticleGenerationResult result = service.generateForTopic(unknown);

        assertThat(result).isInstanceOf(ArticleGenerationResult.GenerationFailed.class);
        assertThat(((ArticleGenerationResult.GenerationFailed) result).reason()).isEqualTo("TOPIC_NOT_FOUND");
    }

    @Test
    void generateForTopic_budgetCapReached_returnsBudgetBlocked_releasesTopic() {
        when(costTracker.canSpend(eq(IaProvider.ANTHROPIC), any())).thenReturn(false);

        ArticleGenerationResult result = service.generateForTopic(topic.getId());

        assertThat(result).isInstanceOf(ArticleGenerationResult.BudgetBlocked.class);
        verify(topicSelectorService).releaseReservation(topic.getId());
        verify(anthropicService, never()).analyzeWithModel(any(AiCallContext.class), any(), any(), any(), anyInt());
        verify(articleRepository, never()).save(any());
    }

    @Test
    void generateForTopic_nominalCase_persistsDraftAndRecordsCosts() {
        when(costTracker.canSpend(eq(IaProvider.ANTHROPIC), any())).thenReturn(true);
        when(anthropicService.analyzeWithModel(any(AiCallContext.class), eq("claude-sonnet-4-6"), anyString(), anyString(), anyInt()))
                .thenReturn(new AnthropicResult(validJson(), "claude-sonnet-4-6", 500, 4000, "end_turn"));

        ArticleGenerationResult result = service.generateForTopic(topic.getId());

        assertThat(result).isInstanceOf(ArticleGenerationResult.Success.class);
        ArticleGenerationResult.Success success = (ArticleGenerationResult.Success) result;
        assertThat(success.status()).isEqualTo(BlogArticleStatus.DRAFT);

        ArgumentCaptor<BlogArticle> articleCaptor = ArgumentCaptor.forClass(BlogArticle.class);
        verify(articleRepository).save(articleCaptor.capture());
        BlogArticle saved = articleCaptor.getValue();
        assertThat(saved.getSlug()).isEqualTo(topic.getSlug());
        assertThat(saved.getTitle()).isEqualTo(topic.getTitle());
        assertThat(saved.getCountry()).isEqualTo("FR");
        assertThat(saved.getLegalDomain()).isEqualTo("DROIT_DU_TRAVAIL");
        assertThat(saved.getStatus()).isEqualTo(BlogArticleStatus.DRAFT);
        assertThat(saved.getBodyMarkdown()).contains("## Le contexte juridique").contains("## Conclusion");
        assertThat(saved.getReadingTimeMinutes()).isGreaterThanOrEqualTo(1);

        verify(costTracker).record(eq(IaProvider.ANTHROPIC), eq("claude-sonnet-4-6"),
                eq(BlogUsageEventType.ARTICLE_GENERATION), any(), eq(500), eq(4000), any());
        verify(topicSelectorService).markAsUsed(eq(topic.getId()), any());
    }

    @Test
    void generateForTopic_invalidJsonTwice_returnsFailed_releasesTopic() {
        when(costTracker.canSpend(eq(IaProvider.ANTHROPIC), any())).thenReturn(true);
        when(anthropicService.analyzeWithModel(any(AiCallContext.class), eq("claude-sonnet-4-6"), anyString(), anyString(), anyInt()))
                .thenReturn(new AnthropicResult("not a json", "claude-sonnet-4-6", 100, 50, "end_turn"));

        ArticleGenerationResult result = service.generateForTopic(topic.getId());

        assertThat(result).isInstanceOf(ArticleGenerationResult.GenerationFailed.class);
        assertThat(((ArticleGenerationResult.GenerationFailed) result).reason()).isEqualTo("INVALID_JSON");
        verify(topicSelectorService).releaseReservation(topic.getId());
        verify(articleRepository, never()).save(any());
        // Sonnet appelé 2 fois (1er + 1 retry)
        verify(anthropicService, times(2)).analyzeWithModel(any(AiCallContext.class), any(), any(), any(), anyInt());
    }

    @Test
    void generateForTopic_metaTitleTooLong_persistsAsNeedsReview() {
        when(costTracker.canSpend(eq(IaProvider.ANTHROPIC), any())).thenReturn(true);
        String json = validJson().replace(
                "\"metaTitle\": \"Rupture conventionnelle : qui paye les frais d'avocat ?\"",
                "\"metaTitle\": \"" + "x".repeat(70) + "\"");
        when(anthropicService.analyzeWithModel(any(AiCallContext.class), eq("claude-sonnet-4-6"), anyString(), anyString(), anyInt()))
                .thenReturn(new AnthropicResult(json, "claude-sonnet-4-6", 500, 4000, "end_turn"));

        ArticleGenerationResult result = service.generateForTopic(topic.getId());

        assertThat(result).isInstanceOf(ArticleGenerationResult.Success.class);
        assertThat(((ArticleGenerationResult.Success) result).status()).isEqualTo(BlogArticleStatus.NEEDS_REVIEW);
    }

    @Test
    void generateForTopic_lessThan3Sections_persistsAsNeedsReview() {
        when(costTracker.canSpend(eq(IaProvider.ANTHROPIC), any())).thenReturn(true);
        String shortJson = """
                {
                  "metaTitle": "Court titre SEO",
                  "metaDescription": "Description meta longue qui respecte la plage 140-160 caractères pour passer la validation. C'est juste un test pour voir.",
                  "subtitle": "Sous-titre",
                  "introduction": "Intro test",
                  "sections": [
                    {"heading": "Une seule section", "content": "Contenu"},
                    {"heading": "Deuxième", "content": "Plus de contenu"}
                  ],
                  "conclusion": "Conclusion test",
                  "ctaBlock": "CTA {{LEGALCASE_CTA}}",
                  "legalCitations": []
                }
                """;
        when(anthropicService.analyzeWithModel(any(AiCallContext.class), eq("claude-sonnet-4-6"), anyString(), anyString(), anyInt()))
                .thenReturn(new AnthropicResult(shortJson, "claude-sonnet-4-6", 200, 1000, "end_turn"));

        ArticleGenerationResult result = service.generateForTopic(topic.getId());

        assertThat(result).isInstanceOf(ArticleGenerationResult.Success.class);
        assertThat(((ArticleGenerationResult.Success) result).status()).isEqualTo(BlogArticleStatus.NEEDS_REVIEW);
    }

    @Test
    void generateForTopic_hallucinationDetected_persistsAsNeedsReview() {
        when(costTracker.canSpend(eq(IaProvider.ANTHROPIC), any())).thenReturn(true);
        when(anthropicService.analyzeWithModel(any(AiCallContext.class), eq("claude-sonnet-4-6"), anyString(), anyString(), anyInt()))
                .thenReturn(new AnthropicResult(validJson(), "claude-sonnet-4-6", 500, 4000, "end_turn"));
        AnthropicResult haikuResult = new AnthropicResult(
                "{\"verdicts\":[{\"reference\":\"X\",\"verdict\":\"LIKELY_HALLUCINATION\",\"rationale\":\"\"}]}",
                "claude-haiku-4-5", 200, 50, "end_turn");
        when(citationVerifier.verify(any())).thenReturn(
                new LegalCitationVerifier.VerifierOutcome(true, true, haikuResult, null));

        ArticleGenerationResult result = service.generateForTopic(topic.getId());

        assertThat(result).isInstanceOf(ArticleGenerationResult.Success.class);
        assertThat(((ArticleGenerationResult.Success) result).status()).isEqualTo(BlogArticleStatus.NEEDS_REVIEW);
        verify(costTracker, atLeastOnce()).record(eq(IaProvider.ANTHROPIC), eq("claude-haiku-4-5"),
                eq(BlogUsageEventType.LEGAL_CITATION_VERIFICATION), any(), anyInt(), anyInt(), any());
    }

    @Test
    void generateForTopic_haikuFails_persistsArticleAnyway() {
        when(costTracker.canSpend(eq(IaProvider.ANTHROPIC), any())).thenReturn(true);
        when(anthropicService.analyzeWithModel(any(AiCallContext.class), eq("claude-sonnet-4-6"), anyString(), anyString(), anyInt()))
                .thenReturn(new AnthropicResult(validJson(), "claude-sonnet-4-6", 500, 4000, "end_turn"));
        when(citationVerifier.verify(any())).thenReturn(LegalCitationVerifier.VerifierOutcome.failed());

        ArticleGenerationResult result = service.generateForTopic(topic.getId());

        assertThat(result).isInstanceOf(ArticleGenerationResult.Success.class);
        verify(articleRepository).save(any());
        // Pas de cost record pour Haiku puisqu'il a échoué
        verify(costTracker, never()).record(eq(IaProvider.ANTHROPIC), eq("claude-haiku-4-5"),
                any(), any(), anyInt(), anyInt(), any());
    }

    @Test
    void computeCost_isLinearInTokens() {
        BigDecimal cost = BlogArticleGeneratorService.computeCost(
                500, 4000,
                new BigDecimal("2.80"), new BigDecimal("14.00"));
        // 500 * 2.80 / 1M + 4000 * 14.00 / 1M = 0.0014 + 0.056 = 0.0574
        assertThat(cost).isEqualByComparingTo(new BigDecimal("0.057400"));
    }

    @Test
    void buildMarkdownBody_concatenatesIntroSectionsConclusionCta() {
        GeneratedArticleJson g = parseSampleGenerated(validJson());
        String markdown = BlogArticleGeneratorService.buildMarkdownBody(g);

        assertThat(markdown).contains("## Le contexte juridique");
        assertThat(markdown).contains("## Conclusion");
        assertThat(markdown).contains("{{LEGALCASE_CTA}}");
    }

    @Test
    void estimateReadingTime_returnsAtLeastOne() {
        assertThat(BlogArticleGeneratorService.estimateReadingTime("")).isEqualTo(1);
        assertThat(BlogArticleGeneratorService.estimateReadingTime(null)).isEqualTo(1);
        assertThat(BlogArticleGeneratorService.estimateReadingTime("a ".repeat(440))).isEqualTo(2);
    }

    private GeneratedArticleJson parseSampleGenerated(String json) {
        try {
            return objectMapper.readValue(json, GeneratedArticleJson.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String validJson() {
        return """
                {
                  "metaTitle": "Rupture conventionnelle : qui paye les frais d'avocat ?",
                  "metaDescription": "Tout savoir sur la prise en charge des frais d'avocat lors d'une rupture conventionnelle en France : règles, exceptions et bonnes pratiques.",
                  "subtitle": "Frais d'avocat : règles et bonnes pratiques",
                  "introduction": "Lors d'une rupture conventionnelle, la question des frais d'avocat se pose presque systématiquement.",
                  "sections": [
                    {"heading": "Le contexte juridique", "content": "La rupture conventionnelle est encadrée par le Code du travail."},
                    {"heading": "Qui prend en charge", "content": "En principe chaque partie supporte ses frais."},
                    {"heading": "Les exceptions", "content": "Une convention écrite peut prévoir une autre répartition."},
                    {"heading": "Bonnes pratiques", "content": "Documenter par écrit et négocier en amont."}
                  ],
                  "conclusion": "Tenir compte du contexte et négocier dès le départ.",
                  "ctaBlock": "Pour aller plus loin, [découvrez LegalCase]({{LEGALCASE_CTA}}).",
                  "legalCitations": [
                    {"reference": "Code du travail, art. L1237-13", "context": "indemnité de rupture conventionnelle"}
                  ]
                }
                """;
    }
}
