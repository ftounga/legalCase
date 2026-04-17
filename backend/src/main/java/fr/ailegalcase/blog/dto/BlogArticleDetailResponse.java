package fr.ailegalcase.blog.dto;

import fr.ailegalcase.blog.entity.BlogArticle;

import java.time.Instant;
import java.util.UUID;

public record BlogArticleDetailResponse(
        UUID id,
        String slug,
        String title,
        String subtitle,
        String bodyMarkdown,
        String heroImageUrl,
        String heroImageAlt,
        String country,
        String legalDomain,
        String authorName,
        String authorUrl,
        String metaTitle,
        String metaDescription,
        Integer readingTimeMinutes,
        Instant publishedAt,
        Instant updatedAt
) {
    public static BlogArticleDetailResponse from(BlogArticle a) {
        return new BlogArticleDetailResponse(
                a.getId(),
                a.getSlug(),
                a.getTitle(),
                a.getSubtitle(),
                a.getBodyMarkdown(),
                a.getHeroImageUrl(),
                a.getHeroImageAlt(),
                a.getCountry(),
                a.getLegalDomain(),
                a.getAuthorName(),
                a.getAuthorUrl(),
                a.getMetaTitle(),
                a.getMetaDescription(),
                a.getReadingTimeMinutes(),
                a.getPublishedAt(),
                a.getUpdatedAt()
        );
    }
}
