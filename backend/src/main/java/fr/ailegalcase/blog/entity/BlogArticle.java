package fr.ailegalcase.blog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blog_articles")
@Getter
@Setter
public class BlogArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 1000)
    private String subtitle;

    @Column(name = "body_markdown", nullable = false, columnDefinition = "text")
    private String bodyMarkdown;

    @Column(name = "hero_image_url", length = 500)
    private String heroImageUrl;

    @Column(name = "hero_image_alt", length = 500)
    private String heroImageAlt;

    @Column(nullable = false, length = 10)
    private String country;

    @Column(name = "legal_domain", nullable = false, length = 30)
    private String legalDomain;

    @Column(name = "author_name", nullable = false, length = 200)
    private String authorName;

    @Column(name = "author_url", length = 500)
    private String authorUrl;

    @Column(name = "meta_title", nullable = false, length = 70)
    private String metaTitle;

    @Column(name = "meta_description", nullable = false, length = 160)
    private String metaDescription;

    @Column(name = "reading_time_minutes", nullable = false)
    private Integer readingTimeMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BlogArticleStatus status;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onPrePersist() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.status == null) {
            this.status = BlogArticleStatus.DRAFT;
        }
    }

    @PreUpdate
    void onPreUpdate() {
        this.updatedAt = Instant.now();
    }
}
