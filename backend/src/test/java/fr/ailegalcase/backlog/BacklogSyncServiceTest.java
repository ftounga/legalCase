package fr.ailegalcase.backlog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@Transactional
class BacklogSyncServiceTest {

    @Autowired private BacklogSyncService syncService;
    @Autowired private BacklogFeatureRepository featureRepository;
    @Autowired private BacklogSubfeatureRepository subfeatureRepository;
    @Autowired private BacklogMarketingTaskRepository marketingRepository;
    @Autowired private BacklogSyncRunRepository runRepository;

    @Autowired private TestPaths testPaths;

    @BeforeEach
    void setUp() throws IOException {
        runRepository.deleteAll();
        subfeatureRepository.deleteAll();
        featureRepository.deleteAll();
        marketingRepository.deleteAll();
        testPaths.writeProductSpec(SAMPLE_PRODUCT_SPEC);
        testPaths.writeMarketing(SAMPLE_MARKETING);
    }

    @Test
    void syncPersistsFeaturesSubfeaturesMarketing() {
        BacklogDtos.BacklogSyncResult result = syncService.sync(SyncTrigger.MANUAL);

        assertThat(result.success()).isTrue();
        assertThat(result.featuresCount()).isEqualTo(2);
        assertThat(result.subfeaturesCount()).isGreaterThanOrEqualTo(1);
        assertThat(result.marketingCount()).isEqualTo(2);

        assertThat(featureRepository.findByCode("F-DT-08")).isPresent();
        assertThat(marketingRepository.findByCode("M-01")).isPresent();
    }

    @Test
    void syncIsIdempotent() {
        syncService.sync(SyncTrigger.MANUAL);
        long beforeCount = featureRepository.count();

        syncService.sync(SyncTrigger.MANUAL);

        assertThat(featureRepository.count()).isEqualTo(beforeCount);
    }

    @Test
    void missingCodeBetweenSyncsIsMarkedOrphan() throws IOException {
        syncService.sync(SyncTrigger.MANUAL);
        assertThat(featureRepository.findByCode("F-DT-08").orElseThrow().isOrphaned()).isFalse();

        testPaths.writeProductSpec(SAMPLE_PRODUCT_SPEC_WITHOUT_DT08);
        BacklogDtos.BacklogSyncResult result = syncService.sync(SyncTrigger.MANUAL);

        assertThat(result.orphansMarked()).isGreaterThanOrEqualTo(1);
        assertThat(featureRepository.findByCode("F-DT-08").orElseThrow().isOrphaned()).isTrue();
    }

    @Test
    void runAuditTracksTrigger() {
        syncService.sync(SyncTrigger.SCHEDULED);

        BacklogSyncRunEntity last = runRepository.findFirstByOrderByStartedAtDesc().orElseThrow();
        assertThat(last.getTriggeredBy()).isEqualTo(SyncTrigger.SCHEDULED);
        assertThat(last.isSuccess()).isTrue();
        assertThat(last.getDurationMs()).isNotNull();
    }

    @Test
    void duplicateFeatureCodeKeepsLastOccurrenceWithoutFailing() throws IOException {
        String specWithDuplicate = """
                # PRODUCT SPEC
                ### Features métier — Droit du travail
                | ID | Feature | Cible | Notes |
                |----|---------|-------|-------|
                | F-DT-08 | First occurrence | V1 — **Terminée** | first |
                | F-DT-08 | Second occurrence | V2 — **En cours** | second |
                """;
        testPaths.writeProductSpec(specWithDuplicate);

        BacklogDtos.BacklogSyncResult result = syncService.sync(SyncTrigger.MANUAL);

        assertThat(result.success()).isTrue();
        assertThat(featureRepository.count()).isEqualTo(1);
        BacklogFeatureEntity persisted = featureRepository.findByCode("F-DT-08").orElseThrow();
        assertThat(persisted.getTitle()).isEqualTo("Second occurrence");
    }

    @Test
    void missingFileFailsRunWithExceptionAndPersistsAuditRow() throws IOException {
        Files.deleteIfExists(testPaths.productSpecPath());

        assertThatThrownBy(() -> syncService.sync(SyncTrigger.MANUAL))
                .isInstanceOf(BacklogSyncService.BacklogSyncException.class);

        List<BacklogSyncRunEntity> runs = runRepository.findAll();
        assertThat(runs).anyMatch(r -> !r.isSuccess() && r.getErrorMessage() != null);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        BacklogProperties testBacklogProperties() throws IOException {
            Path tempDir = Files.createTempDirectory("backlog-sync-test");
            tempDir.toFile().deleteOnExit();
            Path techFile = tempDir.resolve("PRODUCT_SPEC.md");
            Path mktgFile = tempDir.resolve("MARKETING_BACKLOG.md");
            Files.writeString(techFile, "# placeholder");
            Files.writeString(mktgFile, "# placeholder");
            return new BacklogProperties(techFile.toString(), mktgFile.toString(), 10);
        }

        @Bean
        TestPaths testPaths(BacklogProperties props) {
            return new TestPaths(Path.of(props.productSpecPath()), Path.of(props.marketingBacklogPath()));
        }
    }

    static class TestPaths {
        private final Path productSpec;
        private final Path marketing;

        TestPaths(Path productSpec, Path marketing) {
            this.productSpec = productSpec;
            this.marketing = marketing;
        }

        Path productSpecPath() {
            return productSpec;
        }

        void writeProductSpec(String content) throws IOException {
            Files.writeString(productSpec, content);
        }

        void writeMarketing(String content) throws IOException {
            Files.writeString(marketing, content);
        }
    }

    private static final String SAMPLE_PRODUCT_SPEC = """
            # PRODUCT SPEC
            ### Features métier — Droit du travail
            | ID | Feature | Cible | Notes |
            |----|---------|-------|-------|
            | F-DT-08 | Validité licenciement | V1 — **Terminée** | 🟢 SF-DT-08-01 mergée. |
            | F-DT-09 | Comparateur indemnités | V1 — **En cours** | 🟡 |
            """;

    private static final String SAMPLE_PRODUCT_SPEC_WITHOUT_DT08 = """
            # PRODUCT SPEC
            ### Features métier — Droit du travail
            | ID | Feature | Cible | Notes |
            |----|---------|-------|-------|
            | F-DT-09 | Comparateur indemnités | V1 — **En cours** | 🟡 |
            """;

    private static final String SAMPLE_MARKETING = """
            # MARKETING BACKLOG
            ## Site
            | ID | Action | Priorité | Statut | Notes |
            |----|--------|----------|--------|-------|
            | M-01 | Landing page | Haute | `Terminé` | Done. |
            | M-02 | Politique de confidentialité | Haute | `Rédigé` | À publier. |
            """;
}
