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
 * F-JU-03 / SF-219-21 — vérifie la détection d'usage de l'outil
 * eco-cheques-cheques-repas-be via la présence d'une analyse persistée.
 */
class EcoChequesChequesRepasBeToolUsageContributorTest {

    private EcoChequesChequesRepasBeAnalysisRepository repository;
    private EcoChequesChequesRepasBeToolUsageContributor contributor;

    @BeforeEach
    void setUp() {
        repository = mock(EcoChequesChequesRepasBeAnalysisRepository.class);
        contributor = new EcoChequesChequesRepasBeToolUsageContributor(
                repository);
    }

    @Test
    void toolId_returnsExpectedConstant() {
        assertThat(contributor.toolId())
                .isEqualTo("eco-cheques-cheques-repas-be");
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
                .thenReturn(Optional.of(
                        new EcoChequesChequesRepasBeAnalysis()));

        Optional<ToolUsage> result = contributor.detectUsage(caseFileId);

        assertThat(result).isPresent();
        assertThat(result.get().toolId())
                .isEqualTo("eco-cheques-cheques-repas-be");
        assertThat(result.get().brancheCalculId()).isEqualTo("default");
    }
}
