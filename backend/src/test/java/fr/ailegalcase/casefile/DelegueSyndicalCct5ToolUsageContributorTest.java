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
 * F-JU-03 / SF-219-10 — vérifie la détection d'usage de l'outil
 * delegue-syndical-cct-5 via la présence d'une analyse persistée.
 */
class DelegueSyndicalCct5ToolUsageContributorTest {

    private DelegueSyndicalCct5AnalysisRepository repository;
    private DelegueSyndicalCct5ToolUsageContributor contributor;

    @BeforeEach
    void setUp() {
        repository = mock(DelegueSyndicalCct5AnalysisRepository.class);
        contributor = new DelegueSyndicalCct5ToolUsageContributor(repository);
    }

    @Test
    void toolId_returnsExpectedConstant() {
        assertThat(contributor.toolId())
                .isEqualTo("delegue-syndical-cct-5");
    }

    @Test
    void detectUsage_returnsEmpty_whenCaseFileIdNull() {
        assertThat(contributor.detectUsage(null)).isEmpty();
    }

    @Test
    void detectUsage_returnsEmpty_whenNoAnalysisExists() {
        UUID caseFileId = UUID.randomUUID();
        when(repository.findByCaseFileId(caseFileId)).thenReturn(Optional.empty());
        assertThat(contributor.detectUsage(caseFileId)).isEmpty();
    }

    @Test
    void detectUsage_returnsToolUsage_whenAnalysisExists() {
        UUID caseFileId = UUID.randomUUID();
        when(repository.findByCaseFileId(caseFileId))
                .thenReturn(Optional.of(new DelegueSyndicalCct5Analysis()));

        Optional<ToolUsage> result = contributor.detectUsage(caseFileId);

        assertThat(result).isPresent();
        assertThat(result.get().toolId())
                .isEqualTo("delegue-syndical-cct-5");
        assertThat(result.get().brancheCalculId()).isEqualTo("default");
    }
}
