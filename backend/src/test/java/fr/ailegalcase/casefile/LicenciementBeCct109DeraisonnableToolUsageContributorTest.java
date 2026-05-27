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
 * F-JU-03 / SF-213-10 — vérifie la détection d'usage de l'outil
 * licenciement-be-cct109-deraisonnable via la présence d'une analyse
 * persistée.
 */
class LicenciementBeCct109DeraisonnableToolUsageContributorTest {

    private LicenciementBeCct109DeraisonnableAnalysisRepository repository;
    private LicenciementBeCct109DeraisonnableToolUsageContributor contributor;

    @BeforeEach
    void setUp() {
        repository = mock(LicenciementBeCct109DeraisonnableAnalysisRepository.class);
        contributor = new LicenciementBeCct109DeraisonnableToolUsageContributor(repository);
    }

    @Test
    void toolId_returnsExpectedConstant() {
        assertThat(contributor.toolId())
                .isEqualTo("licenciement-be-cct109-deraisonnable");
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
                .thenReturn(Optional.of(new LicenciementBeCct109DeraisonnableAnalysis()));

        Optional<ToolUsage> result = contributor.detectUsage(caseFileId);

        assertThat(result).isPresent();
        assertThat(result.get().toolId())
                .isEqualTo("licenciement-be-cct109-deraisonnable");
        assertThat(result.get().brancheCalculId()).isEqualTo("default");
    }
}
