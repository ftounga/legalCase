package fr.ailegalcase.blog.scheduler;

import fr.ailegalcase.blog.entity.BlogTopic;
import fr.ailegalcase.blog.entity.BlogTopicStatus;
import fr.ailegalcase.blog.repository.BlogTopicRepository;
import fr.ailegalcase.blog.service.TopicSelectorService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrphanReservationCleanerTest {

    private final BlogTopicRepository repository = mock(BlogTopicRepository.class);
    private final TopicSelectorService topicSelectorService = mock(TopicSelectorService.class);
    private final OrphanReservationCleaner cleaner = new OrphanReservationCleaner(
            repository, topicSelectorService, 3_600_000L);

    @Test
    void noStaleTopics_doesNothing() {
        when(repository.findAllByStatusAndUsedAtBefore(eq(BlogTopicStatus.RESERVED), any(Instant.class)))
                .thenReturn(List.of());

        cleaner.releaseOrphans();

        verify(topicSelectorService, never()).releaseReservation(any());
    }

    @Test
    void releasesAllStaleTopics() {
        BlogTopic t1 = topic();
        BlogTopic t2 = topic();
        BlogTopic t3 = topic();
        when(repository.findAllByStatusAndUsedAtBefore(eq(BlogTopicStatus.RESERVED), any(Instant.class)))
                .thenReturn(List.of(t1, t2, t3));

        cleaner.releaseOrphans();

        verify(topicSelectorService, times(3)).releaseReservation(any());
        verify(topicSelectorService).releaseReservation(t1.getId());
        verify(topicSelectorService).releaseReservation(t2.getId());
        verify(topicSelectorService).releaseReservation(t3.getId());
    }

    @Test
    void exceptionOnOneTopic_continuesWithOthers() {
        BlogTopic t1 = topic();
        BlogTopic t2 = topic();
        when(repository.findAllByStatusAndUsedAtBefore(eq(BlogTopicStatus.RESERVED), any(Instant.class)))
                .thenReturn(List.of(t1, t2));
        org.mockito.Mockito.doThrow(new RuntimeException("boom")).when(topicSelectorService).releaseReservation(t1.getId());

        cleaner.releaseOrphans();

        verify(topicSelectorService).releaseReservation(t1.getId());
        verify(topicSelectorService).releaseReservation(t2.getId());
    }

    private BlogTopic topic() {
        BlogTopic t = new BlogTopic();
        t.setId(UUID.randomUUID());
        t.setStatus(BlogTopicStatus.RESERVED);
        t.setUsedAt(Instant.now().minusSeconds(7200));
        return t;
    }
}
