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
 * F-JU-03 / SF-219-08 — vérifie la détection d'usage de l'outil
 * transfert-entreprise-cct-32bis via la présence d'une analyse
 * persistée.
 */
class TransfertEntrepriseCct32bisToolUsageContributorTest {

    private TransfertEntrepriseCct32bisAnalysisRepository repository;
    private TransfertEntrepriseCct32bisToolUsageContributor contributor;

    @BeforeEach
    void setUp() {
        repository = mock(TransfertEntrepriseCct32bisAnalysisRepository.class);
        contributor = new TransfertEntrepriseCct32bisToolUsageContributor(
                repository);
    }

    @Test
    void toolId_returnsExpectedConstant() {
        assertThat(contributor.toolId())
                .isEqualTo("transfert-entreprise-cct-32bis");
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
                .thenReturn(Optional.of(new TransfertEntrepriseCct32bisAnalysis()));

        Optional<ToolUsage> result = contributor.detectUsage(caseFileId);

        assertThat(result).isPresent();
        assertThat(result.get().toolId())
                .isEqualTo("transfert-entreprise-cct-32bis");
        assertThat(result.get().brancheCalculId()).isEqualTo("default");
    }
}
