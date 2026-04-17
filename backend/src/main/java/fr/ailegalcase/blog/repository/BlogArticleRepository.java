package fr.ailegalcase.blog.repository;

import fr.ailegalcase.blog.entity.BlogArticle;
import fr.ailegalcase.blog.entity.BlogArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlogArticleRepository extends JpaRepository<BlogArticle, UUID> {

    Optional<BlogArticle> findBySlugAndStatus(String slug, BlogArticleStatus status);

    @Query("""
            SELECT a FROM BlogArticle a
            WHERE a.status = :status
              AND (:country IS NULL OR a.country = :country)
              AND (:legalDomain IS NULL OR a.legalDomain = :legalDomain)
            """)
    Page<BlogArticle> findPublished(@Param("status") BlogArticleStatus status,
                                    @Param("country") String country,
                                    @Param("legalDomain") String legalDomain,
                                    Pageable pageable);

    @Query("""
            SELECT a FROM BlogArticle a
            WHERE a.status = :status
            ORDER BY a.publishedAt DESC
            """)
    List<BlogArticle> findAllByStatusOrderByPublishedAtDesc(@Param("status") BlogArticleStatus status);
}
