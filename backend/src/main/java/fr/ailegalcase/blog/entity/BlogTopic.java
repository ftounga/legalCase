package fr.ailegalcase.blog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blog_topics")
@Getter
@Setter
public class BlogTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(name = "country_scope", nullable = false, length = 10)
    private String countryScope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BlogTopicStatus status;

    @Column(name = "article_id")
    private UUID articleId;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = BlogTopicStatus.AVAILABLE;
        }
    }
}
