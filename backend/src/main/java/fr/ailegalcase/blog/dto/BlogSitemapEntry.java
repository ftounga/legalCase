package fr.ailegalcase.blog.dto;

import fr.ailegalcase.blog.entity.BlogArticle;

import java.time.Instant;

public record BlogSitemapEntry(
        String slug,
        String country,
        Instant publishedAt,
        Instant updatedAt
) {
    public static BlogSitemapEntry from(BlogArticle a) {
        return new BlogSitemapEntry(
                a.getSlug(),
                a.getCountry(),
                a.getPublishedAt(),
                a.getUpdatedAt()
        );
    }
}
