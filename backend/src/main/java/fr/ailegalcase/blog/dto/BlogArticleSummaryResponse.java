package fr.ailegalcase.blog.dto;

import fr.ailegalcase.blog.entity.BlogArticle;
import fr.ailegalcase.blog.entity.BlogArticleStatus;

import java.time.Instant;
import java.util.UUID;

public record BlogArticleSummaryResponse(
        UUID id,
        String slug,
        String title,
        String subtitle,
        String metaDescription,
        String heroImageUrl,
        String heroImageAlt,
        String country,
        String legalDomain,
        String authorName,
        Integer readingTimeMinutes,
        Instant publishedAt,
        BlogArticleStatus status
) {
    public static BlogArticleSummaryResponse from(BlogArticle a) {
        return new BlogArticleSummaryResponse(
                a.getId(),
                a.getSlug(),
                a.getTitle(),
                a.getSubtitle(),
                a.getMetaDescription(),
                a.getHeroImageUrl(),
                a.getHeroImageAlt(),
                a.getCountry(),
                a.getLegalDomain(),
                a.getAuthorName(),
                a.getReadingTimeMinutes(),
                a.getPublishedAt(),
                a.getStatus()
        );
    }
}
