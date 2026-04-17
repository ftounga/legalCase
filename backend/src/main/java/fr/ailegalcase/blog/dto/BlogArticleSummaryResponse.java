package fr.ailegalcase.blog.dto;

import fr.ailegalcase.blog.entity.BlogArticle;

import java.time.Instant;
import java.util.UUID;

public record BlogArticleSummaryResponse(
        UUID id,
        String slug,
        String title,
        String subtitle,
        String heroImageUrl,
        String heroImageAlt,
        String country,
        String legalDomain,
        String authorName,
        Integer readingTimeMinutes,
        Instant publishedAt
) {
    public static BlogArticleSummaryResponse from(BlogArticle a) {
        return new BlogArticleSummaryResponse(
                a.getId(),
                a.getSlug(),
                a.getTitle(),
                a.getSubtitle(),
                a.getHeroImageUrl(),
                a.getHeroImageAlt(),
                a.getCountry(),
                a.getLegalDomain(),
                a.getAuthorName(),
                a.getReadingTimeMinutes(),
                a.getPublishedAt()
        );
    }
}
