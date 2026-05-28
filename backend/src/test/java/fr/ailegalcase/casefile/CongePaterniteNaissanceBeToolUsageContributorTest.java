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
 * F-JU-03 / SF-219-31 — vérifie la détection d'usage de l'outil
 * conge-paternite-naissance-be via la présence d'une analyse persistée.
 */
class CongePaterniteNaissanceBeToolUsageContributorTest {

    private CongePaterniteNaissanceBeAnalysisRepository repository;
    private CongePaterniteNaissanceBeToolUsageContributor contributor;

    @BeforeEach
    void setUp() {
        repository = mock(
                CongePaterniteNaissanceBeAnalysisRepository.class);
        contributor =
                new CongePaterniteNaissanceBeToolUsageContributor(
                        repository);
    }

    @Test
    void toolId_returnsExpectedConstant() {
        assertThat(contributor.toolId())
                .isEqualTo("conge-paternite-naissance-be");
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
                        new CongePaterniteNaissanceBeAnalysis()));

        Optional<ToolUsage> result = contributor.detectUsage(caseFileId);

        assertThat(result).isPresent();
        assertThat(result.get().toolId())
                .isEqualTo("conge-paternite-naissance-be");
        assertThat(result.get().brancheCalculId()).isEqualTo("default");
    }
}
