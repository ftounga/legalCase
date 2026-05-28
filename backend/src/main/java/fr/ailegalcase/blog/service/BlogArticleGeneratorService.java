package fr.ailegalcase.blog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AiCallContext;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.analysis.JobType;
import fr.ailegalcase.blog.cost.BlogUsageEventType;
import fr.ailegalcase.blog.cost.IaCostTracker;
import fr.ailegalcase.blog.cost.IaProvider;
import fr.ailegalcase.blog.entity.BlogArticle;
import fr.ailegalcase.blog.entity.BlogArticleStatus;
import fr.ailegalcase.blog.entity.BlogTopic;
import fr.ailegalcase.blog.repository.BlogArticleRepository;
import fr.ailegalcase.blog.repository.BlogTopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * F-120 SF-120-03 — orchestrateur de génération d'article blog.
 *
 * <p>Pipeline complet : récupération du topic réservé → vérification budget IA →
 * appel Sonnet 4.6 (rédaction JSON) → parsing → validation → vérification des
 * citations juridiques par Haiku 4.5 → persistance article + événements de coût
 * → marquage du topic en {@code USED}.
 *
 * <p>Garde-fou principal : {@link IaCostTracker} (cap mensuel Anthropic 30 €).
 *
 * <p>Statut de l'article :
 * <ul>
 *   <li>{@code DRAFT} : tout est OK, prêt pour SF-120-04 (image hero) puis publication.</li>
 *   <li>{@code NEEDS_REVIEW} : ≥ 1 hallucination détectée, ou meta SEO hors plage,
 *       ou < 3 sections — visible super-admin pour relecture (SF-120-08).</li>
 * </ul>
 */
@Service
public class BlogArticleGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(BlogArticleGeneratorService.class);

    private static final BigDecimal ESTIMATED_COST_EUR = new BigDecimal("0.10");
    private static final int MIN_SECTIONS = 3;
    private static final int MAX_SECTIONS = 8;
    private static final int META_TITLE_MAX = 60;
    private static final int META_DESCRIPTION_MIN = 140;
    private static final int META_DESCRIPTION_MAX = 160;
    private static final int WORDS_PER_MINUTE = 220;

    private final AnthropicService anthropicService;
    private final BlogTopicRepository topicRepository;
    private final BlogArticleRepository articleRepository;
    private final TopicSelectorService topicSelectorService;
    private final BlogPromptBuilder promptBuilder;
    private final LegalCitationVerifier citationVerifier;
    private final IaCostTracker costTracker;
    private final ObjectMapper objectMapper;
    private final String sonnetModel;
    private final int sonnetMaxTokens;
    private final BigDecimal sonnetInputEurPerMtok;
    private final BigDecimal sonnetOutputEurPerMtok;
    private final BigDecimal haikuInputEurPerMtok;
    private final BigDecimal haikuOutputEurPerMtok;

    public BlogArticleGeneratorService(
            AnthropicService anthropicService,
            BlogTopicRepository topicRepository,
            BlogArticleRepository articleRepository,
            TopicSelectorService topicSelectorService,
            BlogPromptBuilder promptBuilder,
            LegalCitationVerifier citationVerifier,
            IaCostTracker costTracker,
            ObjectMapper objectMapper,
            @Value("${blog.ia.sonnet.model:claude-sonnet-4-6}") String sonnetModel,
            @Value("${blog.ia.sonnet.max-tokens:6000}") int sonnetMaxTokens,
            @Value("${blog.ia.sonnet.cost-input-eur-per-mtok:2.80}") BigDecimal sonnetInputEurPerMtok,
            @Value("${blog.ia.sonnet.cost-output-eur-per-mtok:14.00}") BigDecimal sonnetOutputEurPerMtok,
            @Value("${blog.ia.haiku.cost-input-eur-per-mtok:0.75}") BigDecimal haikuInputEurPerMtok,
            @Value("${blog.ia.haiku.cost-output-eur-per-mtok:3.75}") BigDecimal haikuOutputEurPerMtok) {
        this.anthropicService = anthropicService;
        this.topicRepository = topicRepository;
        this.articleRepository = articleRepository;
        this.topicSelectorService = topicSelectorService;
        this.promptBuilder = promptBuilder;
        this.citationVerifier = citationVerifier;
        this.costTracker = costTracker;
        this.objectMapper = objectMapper;
        this.sonnetModel = sonnetModel;
        this.sonnetMaxTokens = sonnetMaxTokens;
        this.sonnetInputEurPerMtok = sonnetInputEurPerMtok;
        this.sonnetOutputEurPerMtok = sonnetOutputEurPerMtok;
        this.haikuInputEurPerMtok = haikuInputEurPerMtok;
        this.haikuOutputEurPerMtok = haikuOutputEurPerMtok;
    }

    @Transactional
    public ArticleGenerationResult generateForTopic(UUID topicId) {
        Optional<BlogTopic> topicOpt = topicRepository.findById(topicId);
        if (topicOpt.isEmpty()) {
            return new ArticleGenerationResult.GenerationFailed("TOPIC_NOT_FOUND");
        }
        BlogTopic topic = topicOpt.get();

        if (!costTracker.canSpend(IaProvider.ANTHROPIC, ESTIMATED_COST_EUR)) {
            log.warn("Budget cap reached for Anthropic, releasing topic {}", topicId);
            topicSelectorService.releaseReservation(topicId);
            return new ArticleGenerationResult.BudgetBlocked("ANTHROPIC_MONTHLY_CAP_REACHED");
        }

        BlogPromptBuilder.PromptPair prompts = promptBuilder.buildSonnetPrompt(topic);

        AnthropicResult sonnetResult = callSonnet(prompts);
        if (sonnetResult == null) {
            log.warn("Sonnet call failed after retries, releasing topic {}", topicId);
            topicSelectorService.releaseReservation(topicId);
            return new ArticleGenerationResult.GenerationFailed("ANTHROPIC_GENERATION_FAILED");
        }

        GeneratedArticleJson generated = parseSonnetOutput(sonnetResult.content());
        if (generated == null) {
            // 1 retry sur JSON invalide
            log.info("First Sonnet output not parsable, retrying once for topic {}", topicId);
            AnthropicResult retried = callSonnetOnce(prompts);
            if (retried != null) {
                recordSonnetCost(sonnetResult, null);
                sonnetResult = retried;
                generated = parseSonnetOutput(retried.content());
            }
        }
        if (generated == null) {
            log.warn("Sonnet output not parsable after retry, releasing topic {}", topicId);
            recordSonnetCost(sonnetResult, null);
            topicSelectorService.releaseReservation(topicId);
            return new ArticleGenerationResult.GenerationFailed("INVALID_JSON");
        }

        boolean validationOk = passesValidation(generated);

        LegalCitationVerifier.VerifierOutcome verifierOutcome =
                citationVerifier.verify(generated.legalCitations() != null
                        ? generated.legalCitations()
                        : List.of());
        boolean hasHallucination = verifierOutcome.hasLikelyHallucination();

        BlogArticleStatus targetStatus = (validationOk && !hasHallucination)
                ? BlogArticleStatus.DRAFT
                : BlogArticleStatus.NEEDS_REVIEW;

        BlogArticle article = persistArticle(topic, generated, targetStatus);
        recordSonnetCost(sonnetResult, article.getId());
        if (verifierOutcome.called() && verifierOutcome.anthropicResult() != null) {
            recordHaikuCost(verifierOutcome.anthropicResult(), article.getId());
        }
        topicSelectorService.markAsUsed(topic.getId(), article.getId());

        log.info("Blog article generated: id={}, slug={}, status={}, topicId={}",
                article.getId(), article.getSlug(), targetStatus, topic.getId());
        return new ArticleGenerationResult.Success(article.getId(), targetStatus);
    }

    private AnthropicResult callSonnet(BlogPromptBuilder.PromptPair prompts) {
        try {
            AiCallContext ctx = AiCallContext.systemLevel(JobType.SYSTEM_BLOG_GENERATION);
            return anthropicService.analyzeWithModel(
                    ctx, sonnetModel, prompts.system(), prompts.user(), sonnetMaxTokens);
        } catch (Exception e) {
            log.warn("Sonnet call exception: {}", e.getMessage());
            return null;
        }
    }

    private AnthropicResult callSonnetOnce(BlogPromptBuilder.PromptPair prompts) {
        return callSonnet(prompts);
    }

    GeneratedArticleJson parseSonnetOutput(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String stripped = LegalCitationVerifier.stripJsonFences(content);
        try {
            return objectMapper.readValue(stripped, GeneratedArticleJson.class);
        } catch (Exception e) {
            log.debug("Failed to parse Sonnet JSON: {}", e.getMessage());
            return null;
        }
    }

    boolean passesValidation(GeneratedArticleJson g) {
        if (g.metaTitle() == null || g.metaTitle().length() > META_TITLE_MAX) {
            return false;
        }
        if (g.metaDescription() == null
                || g.metaDescription().length() < META_DESCRIPTION_MIN
                || g.metaDescription().length() > META_DESCRIPTION_MAX) {
            return false;
        }
        if (g.sections() == null
                || g.sections().size() < MIN_SECTIONS
                || g.sections().size() > MAX_SECTIONS) {
            return false;
        }
        if (g.introduction() == null || g.introduction().isBlank()) {
            return false;
        }
        if (g.conclusion() == null || g.conclusion().isBlank()) {
            return false;
        }
        return true;
    }

    private BlogArticle persistArticle(BlogTopic topic,
                                       GeneratedArticleJson g,
                                       BlogArticleStatus status) {
        BlogArticle article = new BlogArticle();
        article.setSlug(topic.getSlug());
        article.setTitle(topic.getTitle());
        article.setSubtitle(safeTrim(g.subtitle(), 1000));
        article.setBodyMarkdown(buildMarkdownBody(g));
        article.setHeroImageUrl(null);
        article.setHeroImageAlt(null);
        article.setCountry(topic.getCountryScope());
        article.setLegalDomain(topic.getCategory());
        article.setAuthorName("LegalCase");
        article.setAuthorUrl(null);
        article.setMetaTitle(safeTrim(g.metaTitle(), 70));
        article.setMetaDescription(safeTrim(g.metaDescription(), 160));
        article.setReadingTimeMinutes(estimateReadingTime(article.getBodyMarkdown()));
        article.setStatus(status);
        article.setTopicId(topic.getId());
        return articleRepository.save(article);
    }

    static String buildMarkdownBody(GeneratedArticleJson g) {
        StringBuilder sb = new StringBuilder();
        if (g.introduction() != null) {
            sb.append(g.introduction().trim()).append("\n\n");
        }
        if (g.sections() != null) {
            for (GeneratedArticleJson.Section s : g.sections()) {
                if (s.heading() != null && !s.heading().isBlank()) {
                    sb.append("## ").append(s.heading().trim()).append("\n\n");
                }
                if (s.content() != null) {
                    sb.append(s.content().trim()).append("\n\n");
                }
            }
        }
        if (g.conclusion() != null) {
            sb.append("## Conclusion\n\n").append(g.conclusion().trim()).append("\n\n");
        }
        if (g.ctaBlock() != null) {
            sb.append(g.ctaBlock().trim()).append("\n");
        }
        return sb.toString().trim();
    }

    static int estimateReadingTime(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return 1;
        }
        String[] words = markdown.trim().split("\\s+");
        int minutes = (int) Math.ceil((double) words.length / WORDS_PER_MINUTE);
        return Math.max(1, minutes);
    }

    private static String safeTrim(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    private void recordSonnetCost(AnthropicResult result, UUID articleId) {
        if (result == null) {
            return;
        }
        BigDecimal cost = computeCost(result.promptTokens(), result.completionTokens(),
                sonnetInputEurPerMtok, sonnetOutputEurPerMtok);
        costTracker.record(IaProvider.ANTHROPIC, sonnetModel,
                BlogUsageEventType.ARTICLE_GENERATION,
                articleId, result.promptTokens(), result.completionTokens(), cost);
    }

    private void recordHaikuCost(AnthropicResult result, UUID articleId) {
        BigDecimal cost = computeCost(result.promptTokens(), result.completionTokens(),
                haikuInputEurPerMtok, haikuOutputEurPerMtok);
        costTracker.record(IaProvider.ANTHROPIC, result.modelUsed(),
                BlogUsageEventType.LEGAL_CITATION_VERIFICATION,
                articleId, result.promptTokens(), result.completionTokens(), cost);
    }

    static BigDecimal computeCost(int inputTokens, int outputTokens,
                                  BigDecimal inputEurPerMtok, BigDecimal outputEurPerMtok) {
        BigDecimal mtok = new BigDecimal("1000000");
        BigDecimal inputCost = inputEurPerMtok.multiply(BigDecimal.valueOf(inputTokens))
                .divide(mtok, 6, RoundingMode.HALF_UP);
        BigDecimal outputCost = outputEurPerMtok.multiply(BigDecimal.valueOf(outputTokens))
                .divide(mtok, 6, RoundingMode.HALF_UP);
        return inputCost.add(outputCost).setScale(6, RoundingMode.HALF_UP);
    }
}
