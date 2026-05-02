package fr.ailegalcase.backlog;

import fr.ailegalcase.backlog.BacklogMarkdownParser.ParsedFeature;
import fr.ailegalcase.backlog.BacklogMarkdownParser.ParsedMarketingTask;
import fr.ailegalcase.backlog.BacklogMarkdownParser.ParsedSubfeature;
import fr.ailegalcase.backlog.BacklogMarkdownParser.ParsedTechnicalBacklog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BacklogMarkdownParserTest {

    private BacklogMarkdownParser parser;

    @BeforeEach
    void setUp() {
        parser = new BacklogMarkdownParser();
    }

    @Test
    void parsesNominalProductSpec() throws IOException {
        String content = readFixture("backlog-fixtures/product-spec-mini.md");

        ParsedTechnicalBacklog result = parser.parseTechnicalBacklog(content, "test.md");

        List<ParsedFeature> features = result.features();
        assertThat(features).hasSize(6);
        assertThat(features.stream().map(ParsedFeature::code))
                .containsExactly("F-01", "F-02", "F-DT-08", "F-DT-09", "F-IM-05", "F-167");
    }

    @Test
    void mapsTerminationStatus() throws IOException {
        String content = readFixture("backlog-fixtures/product-spec-mini.md");

        ParsedTechnicalBacklog result = parser.parseTechnicalBacklog(content, "test.md");
        ParsedFeature f01 = result.features().stream().filter(f -> f.code().equals("F-01")).findFirst().orElseThrow();

        assertThat(f01.status()).isEqualTo(BacklogStatus.DONE);
    }

    @Test
    void mapsAbsorbedStatus() throws IOException {
        String content = readFixture("backlog-fixtures/product-spec-mini.md");

        ParsedTechnicalBacklog result = parser.parseTechnicalBacklog(content, "test.md");
        ParsedFeature f167 = result.features().stream().filter(f -> f.code().equals("F-167")).findFirst().orElseThrow();

        assertThat(f167.status()).isEqualTo(BacklogStatus.ABSORBED);
    }

    @Test
    void mapsPlannedStatus() throws IOException {
        String content = readFixture("backlog-fixtures/product-spec-mini.md");

        ParsedTechnicalBacklog result = parser.parseTechnicalBacklog(content, "test.md");
        ParsedFeature f = result.features().stream().filter(x -> x.code().equals("F-IM-05")).findFirst().orElseThrow();

        assertThat(f.status()).isEqualTo(BacklogStatus.PLANNED);
    }

    @Test
    void detectsDomainFromHeading() throws IOException {
        String content = readFixture("backlog-fixtures/product-spec-mini.md");

        ParsedTechnicalBacklog result = parser.parseTechnicalBacklog(content, "test.md");

        ParsedFeature dt08 = byCode(result.features(), "F-DT-08");
        ParsedFeature im05 = byCode(result.features(), "F-IM-05");
        ParsedFeature f01 = byCode(result.features(), "F-01");

        assertThat(dt08.domain()).isEqualTo(BacklogDomain.WORK);
        assertThat(im05.domain()).isEqualTo(BacklogDomain.IMMIGRATION);
        assertThat(f01.domain()).isEqualTo(BacklogDomain.CROSS);
    }

    @Test
    void detectsPriorityFromEmoji() throws IOException {
        String content = readFixture("backlog-fixtures/product-spec-mini.md");

        ParsedTechnicalBacklog result = parser.parseTechnicalBacklog(content, "test.md");

        assertThat(byCode(result.features(), "F-DT-08").priority()).isEqualTo(BacklogPriority.LOW);
        assertThat(byCode(result.features(), "F-DT-09").priority()).isEqualTo(BacklogPriority.MEDIUM);
        assertThat(byCode(result.features(), "F-IM-05").priority()).isEqualTo(BacklogPriority.HIGH);
        assertThat(byCode(result.features(), "F-01").priority()).isNull();
    }

    @Test
    void extractsTargetVersion() throws IOException {
        String content = readFixture("backlog-fixtures/product-spec-mini.md");

        ParsedTechnicalBacklog result = parser.parseTechnicalBacklog(content, "test.md");
        ParsedFeature dt08 = byCode(result.features(), "F-DT-08");
        ParsedFeature im05 = byCode(result.features(), "F-IM-05");

        assertThat(dt08.targetVersion()).isEqualTo("V1");
        assertThat(im05.targetVersion()).isEqualTo("V8+");
    }

    @Test
    void extractsSubfeatureReferences() throws IOException {
        String content = readFixture("backlog-fixtures/product-spec-mini.md");

        ParsedTechnicalBacklog result = parser.parseTechnicalBacklog(content, "test.md");
        List<ParsedSubfeature> subs = result.subfeatures();

        assertThat(subs).extracting(ParsedSubfeature::code)
                .contains("SF-02-01", "SF-DT-08-01", "SF-DT-08-02");

        ParsedSubfeature merged = subs.stream().filter(s -> s.code().equals("SF-02-01")).findFirst().orElseThrow();
        assertThat(merged.status()).isEqualTo(BacklogStatus.DONE);

        ParsedSubfeature inProgress = subs.stream().filter(s -> s.code().equals("SF-DT-08-02")).findFirst().orElseThrow();
        assertThat(inProgress.status()).isEqualTo(BacklogStatus.IN_PROGRESS);
    }

    @Test
    void parsesNominalMarketingBacklog() throws IOException {
        String content = readFixture("backlog-fixtures/marketing-mini.md");

        List<ParsedMarketingTask> tasks = parser.parseMarketingBacklog(content, "marketing.md");

        assertThat(tasks).hasSize(5);
        assertThat(tasks).extracting(ParsedMarketingTask::code)
                .containsExactly("M-01", "M-02", "M-11", "M-12", "M-13");
    }

    @Test
    void mapsAllMarketingStatusVariants() throws IOException {
        String content = readFixture("backlog-fixtures/marketing-mini.md");

        List<ParsedMarketingTask> tasks = parser.parseMarketingBacklog(content, "marketing.md");

        assertThat(byMarketingCode(tasks, "M-01").status()).isEqualTo(BacklogMarketingStatus.DONE);
        assertThat(byMarketingCode(tasks, "M-02").status()).isEqualTo(BacklogMarketingStatus.DRAFTED);
        assertThat(byMarketingCode(tasks, "M-11").status()).isEqualTo(BacklogMarketingStatus.IN_PROGRESS);
        assertThat(byMarketingCode(tasks, "M-12").status()).isEqualTo(BacklogMarketingStatus.TODO);
        assertThat(byMarketingCode(tasks, "M-13").status()).isEqualTo(BacklogMarketingStatus.BLOCKED);
    }

    @Test
    void capturesMarketingCategoryFromHeading() throws IOException {
        String content = readFixture("backlog-fixtures/marketing-mini.md");

        List<ParsedMarketingTask> tasks = parser.parseMarketingBacklog(content, "marketing.md");

        assertThat(byMarketingCode(tasks, "M-01").category()).isEqualTo("Site web");
        assertThat(byMarketingCode(tasks, "M-11").category()).isEqualTo("Vidéo");
    }

    @Test
    void duplicateCodeKeepsLastOccurrence() {
        String content = """
                | ID | Feature | Description | Statut |
                |----|---------|-------------|--------|
                | F-01 | First | First desc | `Terminée` |
                | F-01 | Second | Second desc | `En cours` |
                """;

        ParsedTechnicalBacklog result = parser.parseTechnicalBacklog(content, "test.md");

        assertThat(result.features()).hasSize(1);
        assertThat(result.features().get(0).title()).isEqualTo("Second");
        assertThat(result.features().get(0).status()).isEqualTo(BacklogStatus.IN_PROGRESS);
    }

    @Test
    void emptyContentReturnsEmpty() {
        assertThat(parser.parseTechnicalBacklog("", "x").features()).isEmpty();
        assertThat(parser.parseTechnicalBacklog(null, "x").features()).isEmpty();
        assertThat(parser.parseMarketingBacklog("", "x")).isEmpty();
        assertThat(parser.parseMarketingBacklog(null, "x")).isEmpty();
    }

    @Test
    void malformedRowDoesNotCrash() {
        String content = """
                | ID | Feature | Description | Statut |
                |----|---------|-------------|--------|
                | F-01 | only two cells |
                | F-02 | proper | valid | `Terminée` |
                """;

        ParsedTechnicalBacklog result = parser.parseTechnicalBacklog(content, "test.md");

        assertThat(result.features()).extracting(ParsedFeature::code).containsExactly("F-02");
    }

    @Test
    void parsesRealProductSpecWithoutCrash() throws IOException {
        String content = readClasspathOrEmpty("../../docs/PRODUCT_SPEC.md");
        if (content.isEmpty()) return;

        ParsedTechnicalBacklog result = parser.parseTechnicalBacklog(content, "PRODUCT_SPEC.md");
        assertThat(result.features().size()).isGreaterThan(50);
    }

    private static ParsedFeature byCode(List<ParsedFeature> features, String code) {
        return features.stream().filter(f -> f.code().equals(code)).findFirst().orElseThrow();
    }

    private static ParsedMarketingTask byMarketingCode(List<ParsedMarketingTask> tasks, String code) {
        return tasks.stream().filter(t -> t.code().equals(code)).findFirst().orElseThrow();
    }

    private String readFixture(String classpathPath) throws IOException {
        var resource = getClass().getClassLoader().getResourceAsStream(classpathPath);
        if (resource == null) throw new IOException("Fixture not found: " + classpathPath);
        return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String readClasspathOrEmpty(String relativePath) {
        try {
            return java.nio.file.Files.readString(java.nio.file.Path.of(relativePath));
        } catch (IOException e) {
            return "";
        }
    }
}
