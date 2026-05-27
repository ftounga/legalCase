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
 * F-JU-03 / SF-213-07 — vérifie la détection d'usage de l'outil
 * harcelement-be-procedure-formelle via la présence d'une analyse
 * persistée.
 */
class HarcelementBeProcedureFormelleToolUsageContributorTest {

    private HarcelementBeProcedureFormelleAnalysisRepository repository;
    private HarcelementBeProcedureFormelleToolUsageContributor contributor;

    @BeforeEach
    void setUp() {
        repository = mock(HarcelementBeProcedureFormelleAnalysisRepository.class);
        contributor = new HarcelementBeProcedureFormelleToolUsageContributor(repository);
    }

    @Test
    void toolId_returnsExpectedConstant() {
        assertThat(contributor.toolId()).isEqualTo("harcelement-be-procedure-formelle");
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
                .thenReturn(Optional.of(new HarcelementBeProcedureFormelleAnalysis()));

        Optional<ToolUsage> result = contributor.detectUsage(caseFileId);

        assertThat(result).isPresent();
        assertThat(result.get().toolId()).isEqualTo("harcelement-be-procedure-formelle");
        assertThat(result.get().brancheCalculId()).isEqualTo("default");
    }
}
