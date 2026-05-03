package fr.ailegalcase.blog.repository;

import fr.ailegalcase.blog.entity.BlogTopic;
import fr.ailegalcase.blog.entity.BlogTopicStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlogTopicRepository extends JpaRepository<BlogTopic, UUID> {

    Optional<BlogTopic> findBySlug(String slug);

    Optional<BlogTopic> findFirstByStatusAndCategoryAndCountryScopeOrderByCreatedAtAsc(
            BlogTopicStatus status, String category, String countryScope);

    Optional<BlogTopic> findFirstByStatusAndCategoryOrderByCreatedAtAsc(
            BlogTopicStatus status, String category);

    Optional<BlogTopic> findFirstByStatusOrderByCreatedAtAsc(BlogTopicStatus status);

    List<BlogTopic> findAllByStatusAndUsedAtBefore(BlogTopicStatus status, Instant before);

    /**
     * Transition atomique de statut : ne passe à {@code newStatus} que si le topic
     * est encore dans l'état {@code expectedStatus}. Retourne le nombre de lignes
     * modifiées (1 = acquisition réussie, 0 = course perdue).
     */
    @Modifying
    @Query("""
            UPDATE BlogTopic t
            SET t.status = :newStatus, t.usedAt = :usedAt, t.articleId = :articleId
            WHERE t.id = :id AND t.status = :expectedStatus
            """)
    int compareAndSwapStatus(@Param("id") UUID id,
                             @Param("expectedStatus") BlogTopicStatus expectedStatus,
                             @Param("newStatus") BlogTopicStatus newStatus,
                             @Param("usedAt") Instant usedAt,
                             @Param("articleId") UUID articleId);
}
