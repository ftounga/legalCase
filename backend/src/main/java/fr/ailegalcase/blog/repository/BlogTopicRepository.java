package fr.ailegalcase.blog.repository;

import fr.ailegalcase.blog.entity.BlogTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BlogTopicRepository extends JpaRepository<BlogTopic, UUID> {
    Optional<BlogTopic> findBySlug(String slug);
}
