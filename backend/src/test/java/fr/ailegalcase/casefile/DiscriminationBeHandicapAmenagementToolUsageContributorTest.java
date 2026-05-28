package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * F-JU-03 / SF-219-23 — vérifie la détection d'usage de l'outil
 * discrimination-be-handicap-amenagement via la présence d'une
 * analyse persistée.
 */
class DiscriminationBeHandicapAmenagementToolUsageContributorTest {

    private DiscriminationBeHandicapAmenagementAnalysisRepository repository;
    private DiscriminationBeHandicapAmenagementToolUsageContributor contributor;

    @BeforeEach
    void setUp() {
        repository = mock(DiscriminationBeHandicapAmenagementAnalysisRepository.class);
        contributor =
                new DiscriminationBeHandicapAmenagementToolUsageContributor(repository);
    }

    @Test
    void toolId_returnsExpectedConstant() {
        assertThat(contributor.toolId())
                .isEqualTo("discrimination-be-handicap-amenagement");
    }

    @Test
    void detectUsage_returnsEmpty_whenCaseFileIdNull() {
        assertThat(contributor.detectUsage(null)).isEmpty();
    }

    @Test
    void detectUsage_returnsEmpty_whenNoAnalysisExists() {
        UUID caseFileId = UUID.randomUUID();
        when(repository.findByCaseFileId(caseFileId))
                .thenReturn(Optional.empty());
        assertThat(contributor.detectUsage(caseFileId)).isEmpty();
    }

    @Test
    void detectUsage_returnsToolUsage_whenAnalysisExists() {
        UUID caseFileId = UUID.randomUUID();
        when(repository.findByCaseFileId(caseFileId))
                .thenReturn(Optional.of(new DiscriminationBeHandicapAmenagementAnalysis()));

        Optional<ToolUsage> result = contributor.detectUsage(caseFileId);

        assertThat(result).isPresent();
        assertThat(result.get().toolId())
                .isEqualTo("discrimination-be-handicap-amenagement");
        assertThat(result.get().brancheCalculId()).isEqualTo("default");
    }
}
