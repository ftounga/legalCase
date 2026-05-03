package fr.ailegalcase.blog.image;

import fr.ailegalcase.blog.cost.BlogUsageEventType;
import fr.ailegalcase.blog.cost.IaCostTracker;
import fr.ailegalcase.blog.cost.IaProvider;
import fr.ailegalcase.blog.entity.BlogArticle;
import fr.ailegalcase.blog.entity.BlogArticleStatus;
import fr.ailegalcase.blog.repository.BlogArticleRepository;
import fr.ailegalcase.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * F-120 SF-120-04 — orchestrateur de génération d'image hero.
 *
 * <p>Pipeline :
 * <ol>
 *   <li>Récupération de l'article (doit être {@code DRAFT}).</li>
 *   <li>Vérification budget OpenAI via {@link IaCostTracker}.</li>
 *   <li>Construction du prompt à partir du titre + catégorie.</li>
 *   <li>Appel {@link RetryableImageGenerator} (2 retries max, transient only).</li>
 *   <li>Upload S3 sous {@code blog/articles/{articleId}/hero.png}.</li>
 *   <li>Persistance {@code heroImageUrl} + {@code heroImageAlt} + cost event.</li>
 * </ol>
 *
 * <p>Stratégie d'échec : si retries épuisés ou erreur définitive, l'article
 * reste sans hero ({@code heroImageUrl=NULL}) — il sera publié quand même
 * (fallback "no hero", géré par SF-120-06 frontend).
 */
@Service
public class BlogHeroImageService {

    private static final Logger log = LoggerFactory.getLogger(BlogHeroImageService.class);

    private static final BigDecimal ESTIMATED_COST_EUR = new BigDecimal("0.05");
    private static final String HERO_KEY_PATTERN = "blog/articles/%s/hero.png";
    private static final String HERO_URL_PATTERN = "/api/v1/blog/articles/%s/hero-image";
    private static final String CONTENT_TYPE = "image/png";

    private static final Map<String, String> CATEGORY_TO_VISUAL = Map.of(
            "DROIT_DU_TRAVAIL",
            "labor law: workplace, employment contracts, professional environment",
            "DROIT_IMMIGRATION",
            "immigration law: residency permits, administrative procedures, integration",
            "DROIT_FAMILLE",
            "family law: domestic life, parenthood, household matters"
    );

    private final BlogArticleRepository articleRepository;
    private final RetryableImageGenerator imageGenerator;
    private final StorageService storageService;
    private final IaCostTracker costTracker;
    private final String imageModel;
    private final BigDecimal costPerImageEur;

    public BlogHeroImageService(BlogArticleRepository articleRepository,
                                RetryableImageGenerator imageGenerator,
                                StorageService storageService,
                                IaCostTracker costTracker,
                                @Value("${blog.ia.openai.image-model:gpt-image-1}") String imageModel,
                                @Value("${blog.ia.openai.image-cost-eur:0.037}") BigDecimal costPerImageEur) {
        this.articleRepository = articleRepository;
        this.imageGenerator = imageGenerator;
        this.storageService = storageService;
        this.costTracker = costTracker;
        this.imageModel = imageModel;
        this.costPerImageEur = costPerImageEur;
    }

    @Transactional
    public BlogHeroImageResult generateForArticle(UUID articleId) {
        Optional<BlogArticle> articleOpt = articleRepository.findById(articleId);
        if (articleOpt.isEmpty()) {
            return new BlogHeroImageResult.Skipped("ARTICLE_NOT_FOUND");
        }
        BlogArticle article = articleOpt.get();

        if (article.getHeroImageUrl() != null && !article.getHeroImageUrl().isBlank()) {
            return new BlogHeroImageResult.Skipped("HERO_ALREADY_PRESENT");
        }
        if (article.getStatus() == BlogArticleStatus.NEEDS_REVIEW) {
            return new BlogHeroImageResult.Skipped("ARTICLE_NEEDS_REVIEW_FIRST");
        }

        if (!costTracker.canSpend(IaProvider.OPENAI, ESTIMATED_COST_EUR)) {
            log.warn("Budget cap reached for OpenAI, skipping hero for article {}", articleId);
            return new BlogHeroImageResult.BudgetBlocked("OPENAI_MONTHLY_CAP_REACHED");
        }

        String prompt = buildPrompt(article);
        RetryableImageGenerator.Outcome outcome = imageGenerator.generate(prompt);

        if (outcome instanceof RetryableImageGenerator.Outcome.Failed failed) {
            log.warn("Hero image generation failed for article {}: definitive={}, cause={}",
                    articleId, failed.wasDefinitive(),
                    failed.cause() != null ? failed.cause().getMessage() : "n/a");
            return new BlogHeroImageResult.Failed("OPENAI_GENERATION_FAILED", failed.wasDefinitive());
        }

        RetryableImageGenerator.Outcome.Success success = (RetryableImageGenerator.Outcome.Success) outcome;
        byte[] imageBytes = success.response().imageBytes();

        String s3Key = HERO_KEY_PATTERN.formatted(articleId);
        storageService.upload(s3Key,
                new ByteArrayInputStream(imageBytes), CONTENT_TYPE, imageBytes.length);

        String heroUrl = HERO_URL_PATTERN.formatted(articleId);
        String heroAlt = "Illustration : " + article.getTitle();
        article.setHeroImageUrl(heroUrl);
        article.setHeroImageAlt(heroAlt.length() > 500 ? heroAlt.substring(0, 500) : heroAlt);
        articleRepository.save(article);

        costTracker.record(IaProvider.OPENAI, success.response().modelUsed(),
                BlogUsageEventType.HERO_IMAGE_GENERATION, articleId,
                0, 0, costPerImageEur);

        log.info("Hero image generated and stored for article {}: key={}", articleId, s3Key);
        return new BlogHeroImageResult.Success(articleId, heroUrl);
    }

    String buildPrompt(BlogArticle article) {
        String visual = CATEGORY_TO_VISUAL.getOrDefault(article.getLegalDomain(),
                "professional, sober legal context");
        return ("Minimal vector illustration for a French/Belgian legal blog article titled \"%s\". "
                + "Subject: %s. Style: clean professional, navy blue (#0B2147) and gold (#C9A54B) accents "
                + "on white background, no text in the image, no photorealism, no logo, 16:9 aspect ratio.")
                .formatted(article.getTitle(), visual);
    }
}
