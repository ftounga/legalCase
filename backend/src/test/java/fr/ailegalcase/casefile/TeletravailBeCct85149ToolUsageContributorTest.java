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
 * F-JU-03 / SF-219-16 — vérifie la détection d'usage de l'outil
 * teletravail-be-cct-85-149 via la présence d'une analyse persistée.
 */
class TeletravailBeCct85149ToolUsageContributorTest {

    private TeletravailBeCct85149AnalysisRepository repository;
    private TeletravailBeCct85149ToolUsageContributor contributor;

    @BeforeEach
    void setUp() {
        repository = mock(TeletravailBeCct85149AnalysisRepository.class);
        contributor =
                new TeletravailBeCct85149ToolUsageContributor(repository);
    }

    @Test
    void toolId_returnsExpectedConstant() {
        assertThat(contributor.toolId())
                .isEqualTo("teletravail-be-cct-85-149");
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
                .thenReturn(Optional.of(new TeletravailBeCct85149Analysis()));

        Optional<ToolUsage> result = contributor.detectUsage(caseFileId);

        assertThat(result).isPresent();
        assertThat(result.get().toolId())
                .isEqualTo("teletravail-be-cct-85-149");
        assertThat(result.get().brancheCalculId()).isEqualTo("default");
    }
}
