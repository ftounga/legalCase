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
 * F-JU-03 / SF-219-29 - verifie la detection d'usage de l'outil
 * at-mp-rente-capital-be via la presence d'une analyse persistee.
 */
class AtMpRenteCapitalBeToolUsageContributorTest {

    private AtMpRenteCapitalBeAnalysisRepository repository;
    private AtMpRenteCapitalBeToolUsageContributor contributor;

    @BeforeEach
    void setUp() {
        repository = mock(AtMpRenteCapitalBeAnalysisRepository.class);
        contributor =
                new AtMpRenteCapitalBeToolUsageContributor(repository);
    }

    @Test
    void toolId_returnsExpectedConstant() {
        assertThat(contributor.toolId())
                .isEqualTo("at-mp-rente-capital-be");
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
                .thenReturn(Optional.of(new AtMpRenteCapitalBeAnalysis()));

        Optional<ToolUsage> result = contributor.detectUsage(caseFileId);

        assertThat(result).isPresent();
        assertThat(result.get().toolId())
                .isEqualTo("at-mp-rente-capital-be");
        assertThat(result.get().brancheCalculId()).isEqualTo("default");
    }
}
