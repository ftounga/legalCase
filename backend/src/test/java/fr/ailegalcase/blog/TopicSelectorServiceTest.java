package fr.ailegalcase.blog;

import fr.ailegalcase.blog.entity.BlogArticleStatus;
import fr.ailegalcase.blog.entity.BlogTopic;
import fr.ailegalcase.blog.entity.BlogTopicStatus;
import fr.ailegalcase.blog.repository.BlogArticleRepository;
import fr.ailegalcase.blog.repository.BlogTopicRepository;
import fr.ailegalcase.blog.repository.CategoryCountryCount;
import fr.ailegalcase.blog.service.TopicSelection;
import fr.ailegalcase.blog.service.TopicSelectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicSelectorServiceTest {

    @Mock
    private BlogTopicRepository topicRepository;

    @Mock
    private BlogArticleRepository articleRepository;

    private TopicSelectorService service;

    @BeforeEach
    void setUp() {
        service = new TopicSelectorService(topicRepository, articleRepository);
    }

    @Test
    void selectNext_emptyBase_returnsEmpty() {
        when(articleRepository.countPublishedByCategoryAndCountrySince(
                eq(BlogArticleStatus.PUBLISHED), any()))
                .thenReturn(List.of());
        when(topicRepository.findFirstByStatusAndCategoryAndCountryScopeOrderByCreatedAtAsc(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(topicRepository.findFirstByStatusAndCategoryOrderByCreatedAtAsc(any(), any()))
                .thenReturn(Optional.empty());
        when(topicRepository.findFirstByStatusOrderByCreatedAtAsc(any()))
                .thenReturn(Optional.empty());

        Optional<TopicSelection> result = service.selectNext();

        assertThat(result).isEmpty();
    }

    @Test
    void selectNext_emptyBaseAndTravailAvailable_returnsTravail() {
        when(articleRepository.countPublishedByCategoryAndCountrySince(any(), any()))
                .thenReturn(List.of());
        BlogTopic travailFr = topic("travail-fr", "DROIT_DU_TRAVAIL", "FR");
        when(topicRepository.findFirstByStatusAndCategoryAndCountryScopeOrderByCreatedAtAsc(
                BlogTopicStatus.AVAILABLE, "DROIT_DU_TRAVAIL", "FR"))
                .thenReturn(Optional.of(travailFr));
        when(topicRepository.compareAndSwapStatus(
                eq(travailFr.getId()), eq(BlogTopicStatus.AVAILABLE), eq(BlogTopicStatus.RESERVED),
                any(), eq(null)))
                .thenReturn(1);

        Optional<TopicSelection> result = service.selectNext();

        assertThat(result).isPresent();
        assertThat(result.get().slug()).isEqualTo("travail-fr");
        assertThat(result.get().category()).isEqualTo("DROIT_DU_TRAVAIL");
        assertThat(result.get().countryScope()).isEqualTo("FR");
    }

    @Test
    void selectNext_fallbackCategory_triesImmigrationWhenTravailExhausted() {
        when(articleRepository.countPublishedByCategoryAndCountrySince(any(), any()))
                .thenReturn(List.of());
        // Aucun topic AVAILABLE en Travail (FR + BE + fallback catégorie)
        when(topicRepository.findFirstByStatusAndCategoryAndCountryScopeOrderByCreatedAtAsc(
                BlogTopicStatus.AVAILABLE, "DROIT_DU_TRAVAIL", "FR"))
                .thenReturn(Optional.empty());
        when(topicRepository.findFirstByStatusAndCategoryAndCountryScopeOrderByCreatedAtAsc(
                BlogTopicStatus.AVAILABLE, "DROIT_DU_TRAVAIL", "BE"))
                .thenReturn(Optional.empty());
        when(topicRepository.findFirstByStatusAndCategoryOrderByCreatedAtAsc(
                BlogTopicStatus.AVAILABLE, "DROIT_DU_TRAVAIL"))
                .thenReturn(Optional.empty());
        // Immigration FR disponible
        BlogTopic immigrationFr = topic("imm-fr", "DROIT_IMMIGRATION", "FR");
        when(topicRepository.findFirstByStatusAndCategoryAndCountryScopeOrderByCreatedAtAsc(
                BlogTopicStatus.AVAILABLE, "DROIT_IMMIGRATION", "FR"))
                .thenReturn(Optional.of(immigrationFr));
        when(topicRepository.compareAndSwapStatus(
                eq(immigrationFr.getId()), eq(BlogTopicStatus.AVAILABLE), eq(BlogTopicStatus.RESERVED),
                any(), eq(null)))
                .thenReturn(1);

        Optional<TopicSelection> result = service.selectNext();

        assertThat(result).isPresent();
        assertThat(result.get().slug()).isEqualTo("imm-fr");
    }

    @Test
    void selectNext_fallbackCountry_triesBEWhenFRExhausted() {
        when(articleRepository.countPublishedByCategoryAndCountrySince(any(), any()))
                .thenReturn(List.of());
        // Pas de DT/FR
        when(topicRepository.findFirstByStatusAndCategoryAndCountryScopeOrderByCreatedAtAsc(
                BlogTopicStatus.AVAILABLE, "DROIT_DU_TRAVAIL", "FR"))
                .thenReturn(Optional.empty());
        // Mais DT/BE disponible
        BlogTopic travailBe = topic("travail-be", "DROIT_DU_TRAVAIL", "BE");
        when(topicRepository.findFirstByStatusAndCategoryAndCountryScopeOrderByCreatedAtAsc(
                BlogTopicStatus.AVAILABLE, "DROIT_DU_TRAVAIL", "BE"))
                .thenReturn(Optional.of(travailBe));
        when(topicRepository.compareAndSwapStatus(
                eq(travailBe.getId()), eq(BlogTopicStatus.AVAILABLE), eq(BlogTopicStatus.RESERVED),
                any(), eq(null)))
                .thenReturn(1);

        Optional<TopicSelection> result = service.selectNext();

        assertThat(result).isPresent();
        assertThat(result.get().countryScope()).isEqualTo("BE");
    }

    @Test
    void selectNext_raceLostOnCasUpdate_returnsEmptyForThatSlot() {
        when(articleRepository.countPublishedByCategoryAndCountrySince(any(), any()))
                .thenReturn(List.of());
        BlogTopic travailFr = topic("travail-fr", "DROIT_DU_TRAVAIL", "FR");
        when(topicRepository.findFirstByStatusAndCategoryAndCountryScopeOrderByCreatedAtAsc(
                BlogTopicStatus.AVAILABLE, "DROIT_DU_TRAVAIL", "FR"))
                .thenReturn(Optional.of(travailFr));
        // CAS échoue (rowsAffected=0 = race perdue)
        when(topicRepository.compareAndSwapStatus(
                eq(travailFr.getId()), eq(BlogTopicStatus.AVAILABLE), eq(BlogTopicStatus.RESERVED),
                any(), eq(null)))
                .thenReturn(0);
        // Tout le reste vide
        when(topicRepository.findFirstByStatusAndCategoryAndCountryScopeOrderByCreatedAtAsc(
                BlogTopicStatus.AVAILABLE, "DROIT_DU_TRAVAIL", "BE"))
                .thenReturn(Optional.empty());
        when(topicRepository.findFirstByStatusAndCategoryOrderByCreatedAtAsc(any(), any()))
                .thenReturn(Optional.empty());
        when(topicRepository.findFirstByStatusOrderByCreatedAtAsc(any()))
                .thenReturn(Optional.empty());

        Optional<TopicSelection> result = service.selectNext();

        assertThat(result).isEmpty();
    }

    @Test
    void selectNext_quotaApplied_prioritizesDeficitaryCategory() {
        // Base avec 10 articles tous en travail → immigration et famille en gros déficit
        when(articleRepository.countPublishedByCategoryAndCountrySince(any(), any()))
                .thenReturn(List.of(countRow("DROIT_DU_TRAVAIL", "FR", 10L)));
        BlogTopic imm = topic("imm-fr", "DROIT_IMMIGRATION", "FR");
        when(topicRepository.findFirstByStatusAndCategoryAndCountryScopeOrderByCreatedAtAsc(
                BlogTopicStatus.AVAILABLE, "DROIT_IMMIGRATION", "FR"))
                .thenReturn(Optional.of(imm));
        when(topicRepository.compareAndSwapStatus(
                eq(imm.getId()), any(), any(), any(), any()))
                .thenReturn(1);

        Optional<TopicSelection> result = service.selectNext();

        assertThat(result).isPresent();
        assertThat(result.get().category()).isEqualTo("DROIT_IMMIGRATION");
        // Vérification : la catégorie Travail n'a JAMAIS été interrogée (priorité immigration).
        verify(topicRepository, never())
                .findFirstByStatusAndCategoryAndCountryScopeOrderByCreatedAtAsc(
                        any(), eq("DROIT_DU_TRAVAIL"), any());
    }

    @Test
    void markAsUsed_nominal_callsCompareAndSwap() {
        UUID topicId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        when(topicRepository.compareAndSwapStatus(
                eq(topicId), eq(BlogTopicStatus.RESERVED), eq(BlogTopicStatus.USED),
                any(), eq(articleId)))
                .thenReturn(1);

        service.markAsUsed(topicId, articleId);

        verify(topicRepository).compareAndSwapStatus(
                eq(topicId), eq(BlogTopicStatus.RESERVED), eq(BlogTopicStatus.USED),
                any(), eq(articleId));
    }

    @Test
    void markAsUsed_topicNotReserved_throwsIllegalState() {
        UUID topicId = UUID.randomUUID();
        when(topicRepository.compareAndSwapStatus(
                eq(topicId), eq(BlogTopicStatus.RESERVED), eq(BlogTopicStatus.USED),
                any(), any()))
                .thenReturn(0);

        assertThatThrownBy(() -> service.markAsUsed(topicId, UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RESERVED");
    }

    @Test
    void releaseReservation_nominal_callsCompareAndSwap() {
        UUID topicId = UUID.randomUUID();
        when(topicRepository.compareAndSwapStatus(
                eq(topicId), eq(BlogTopicStatus.RESERVED), eq(BlogTopicStatus.AVAILABLE),
                eq(null), eq(null)))
                .thenReturn(1);

        service.releaseReservation(topicId);

        verify(topicRepository).compareAndSwapStatus(
                eq(topicId), eq(BlogTopicStatus.RESERVED), eq(BlogTopicStatus.AVAILABLE),
                eq(null), eq(null));
    }

    @Test
    void releaseReservation_topicNotReserved_silentNoOp() {
        UUID topicId = UUID.randomUUID();
        when(topicRepository.compareAndSwapStatus(
                eq(topicId), any(), any(), any(), any()))
                .thenReturn(0);

        // Pas d'exception attendue
        service.releaseReservation(topicId);
    }

    private static BlogTopic topic(String slug, String category, String countryScope) {
        BlogTopic t = new BlogTopic();
        t.setId(UUID.randomUUID());
        t.setSlug(slug);
        t.setTitle("Title " + slug);
        t.setCategory(category);
        t.setCountryScope(countryScope);
        t.setStatus(BlogTopicStatus.AVAILABLE);
        t.setCreatedAt(Instant.now());
        return t;
    }

    private static CategoryCountryCount countRow(String category, String country, long count) {
        return new CategoryCountryCount() {
            @Override public String getCategory() { return category; }
            @Override public String getCountry() { return country; }
            @Override public Long getCount() { return count; }
        };
    }
}
