package fr.ailegalcase.blog.scheduler;

import fr.ailegalcase.blog.entity.BlogTopic;
import fr.ailegalcase.blog.entity.BlogTopicStatus;
import fr.ailegalcase.blog.repository.BlogTopicRepository;
import fr.ailegalcase.blog.service.TopicSelectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Cron horaire qui libère les topics RESERVED depuis trop longtemps sans
 * markAsUsed/releaseReservation (génération crashée, JVM redémarrée, etc.).
 */
@Component
public class OrphanReservationCleaner {

    private static final Logger log = LoggerFactory.getLogger(OrphanReservationCleaner.class);

    private final BlogTopicRepository topicRepository;
    private final TopicSelectorService topicSelectorService;
    private final long staleAfterMs;

    public OrphanReservationCleaner(BlogTopicRepository topicRepository,
                                    TopicSelectorService topicSelectorService,
                                    @Value("${blog.cleanup.reservation-stale-after-ms:3600000}") long staleAfterMs) {
        this.topicRepository = topicRepository;
        this.topicSelectorService = topicSelectorService;
        this.staleAfterMs = staleAfterMs;
    }

    @Scheduled(fixedDelayString = "${blog.cleanup.fixed-delay-ms:3600000}",
               initialDelayString = "${blog.cleanup.initial-delay-ms:300000}")
    public void releaseOrphans() {
        Instant cutoff = Instant.now().minusMillis(staleAfterMs);
        List<BlogTopic> stale = topicRepository.findAllByStatusAndUsedAtBefore(
                BlogTopicStatus.RESERVED, cutoff);
        if (stale.isEmpty()) {
            return;
        }
        int released = 0;
        for (BlogTopic topic : stale) {
            try {
                topicSelectorService.releaseReservation(topic.getId());
                released++;
            } catch (Exception e) {
                log.warn("Failed to release orphan topic {}: {}", topic.getId(), e.getMessage());
            }
        }
        log.info("Orphan reservation cleanup: {} topics released (cutoff={})", released, cutoff);
    }
}
