package fr.ailegalcase.blog.service;

import fr.ailegalcase.blog.entity.BlogTopic;

import java.util.UUID;

public record TopicSelection(
        UUID id,
        String slug,
        String title,
        String description,
        String category,
        String countryScope
) {
    public static TopicSelection from(BlogTopic topic) {
        return new TopicSelection(
                topic.getId(),
                topic.getSlug(),
                topic.getTitle(),
                topic.getDescription(),
                topic.getCategory(),
                topic.getCountryScope()
        );
    }
}
