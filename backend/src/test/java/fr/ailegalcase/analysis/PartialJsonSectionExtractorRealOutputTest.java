package fr.ailegalcase.analysis;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-185 SF-185-07 — reproducer du bug "chunks=446 sections=0" observé staging
 * 2026-05-03 sur dossier Chen 7.
 *
 * <p>Stream le JSON réel produit par Sonnet 4.6 (~40 ko, sortie complète d'une
 * analyse Chen 7 v3+ DONE) en chunks de 50-100 chars (simule la taille typique
 * des text_delta Anthropic) et vérifie que l'extracteur émet bien plusieurs
 * sections au fil de l'eau.
 *
 * <p>Si ce test échoue → le bug de production est reproduit en isolation,
 * fixable sur l'extracteur sans dépendance Anthropic / RestClient.
 */
class PartialJsonSectionExtractorRealOutputTest {

    @Test
    void streamRealChen7Output_emitsMultipleSections() throws IOException {
        String json = Files.readString(
                Paths.get("src/test/resources/streaming-fixtures/chen7-real-output.json"),
                StandardCharsets.UTF_8);

        // Simule un streaming par chunks de 80 chars (taille typique text_delta Sonnet)
        PartialJsonSectionExtractor ext = new PartialJsonSectionExtractor();
        int totalEmitted = 0;
        int chunkSize = 80;
        for (int i = 0; i < json.length(); i += chunkSize) {
            String chunk = json.substring(i, Math.min(i + chunkSize, json.length()));
            List<Map.Entry<String, String>> emitted = ext.append(chunk);
            totalEmitted += emitted.size();
        }

        Map<String, String> snapshot = ext.snapshot();
        // L'analyse Chen 7 v3+ contient typiquement les sections suivantes :
        // timeline, faits, points_juridiques, risques, questions_ouvertes,
        // pieces_manquantes, points_procedure, pistes_strategiques,
        // score_risque, delais_detectes, source_explanations
        assertThat(snapshot.keySet())
                .as("Sections détectées sur output réel Chen 7 (40 ko, 80-char chunks)")
                .contains("timeline");
        assertThat(snapshot.size())
                .as("Au moins 5 sections devraient être détectées sur ce JSON")
                .isGreaterThanOrEqualTo(5);
        assertThat(totalEmitted)
                .as("Total emissions cumulées (au moins une par section)")
                .isGreaterThanOrEqualTo(snapshot.size());
    }

    // F-185 SF-185-07 — reproducer du bug réel : Sonnet stream le JSON avec un
    // préambule Markdown ```json\n et un suffixe \n```. L'extracteur actuel
    // abandonne sur le 1er backtick (n'est pas un `{`) et reste stuck pour
    // toujours → chunks=N sections=0 observé en prod 2026-05-03 sur Chen 7.
    @Test
    void streamRealChen7Output_withMarkdownCodeBlock_emitsSections() throws IOException {
        String json = Files.readString(
                Paths.get("src/test/resources/streaming-fixtures/chen7-real-output.json"),
                StandardCharsets.UTF_8);
        // Wrapper exact que Sonnet produit en streaming
        String wrapped = "```json\n" + json + "\n```";

        PartialJsonSectionExtractor ext = new PartialJsonSectionExtractor();
        int totalEmitted = 0;
        int chunkSize = 80;
        for (int i = 0; i < wrapped.length(); i += chunkSize) {
            String chunk = wrapped.substring(i, Math.min(i + chunkSize, wrapped.length()));
            totalEmitted += ext.append(chunk).size();
        }

        assertThat(ext.snapshot().keySet())
                .as("Doit détecter les sections malgré le wrapper Markdown")
                .contains("timeline");
        assertThat(ext.snapshot().size()).isGreaterThanOrEqualTo(5);
    }

    @Test
    void streamRealChen7Output_singleAppend_emitsAllSections() throws IOException {
        // Test de contrôle : si on append le JSON complet en une seule fois,
        // l'extracteur émet tout d'un coup. Si ce test passe mais le précédent
        // échoue → bug de stateful parsing entre chunks (cursor/buffer state).
        String json = Files.readString(
                Paths.get("src/test/resources/streaming-fixtures/chen7-real-output.json"),
                StandardCharsets.UTF_8);

        PartialJsonSectionExtractor ext = new PartialJsonSectionExtractor();
        List<Map.Entry<String, String>> emitted = ext.append(json);

        assertThat(emitted)
                .as("Single-append doit émettre toutes les sections du JSON Chen 7")
                .hasSizeGreaterThanOrEqualTo(5);
    }
}
