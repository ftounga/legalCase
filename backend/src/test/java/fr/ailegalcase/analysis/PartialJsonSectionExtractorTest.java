package fr.ailegalcase.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-185 SF-185-01 — couverture unitaire du parseur incrémental :
 * sections complétées dans l'ordre, sections fragmentées sur plusieurs deltas,
 * objets imbriqués, chaînes contenant accolades, scalaires, JSON malformé,
 * idempotence des sections déjà émises.
 */
class PartialJsonSectionExtractorTest {

    @Test
    void emitsNothingWhileObjectIsIncomplete() {
        PartialJsonSectionExtractor ext = new PartialJsonSectionExtractor();
        assertThat(ext.append("{\"faits\": [")).isEmpty();
        assertThat(ext.append("{\"texte\": \"a\"}")).isEmpty();
    }

    @Test
    void emitsArraySectionAsSoonAsBracketsBalance() {
        PartialJsonSectionExtractor ext = new PartialJsonSectionExtractor();
        // Le `]` ferme le tableau ⇒ la section faits est immédiatement émise,
        // pas besoin d'attendre la virgule séparatrice.
        List<Map.Entry<String, String>> emitted = ext.append("{\"faits\": [{\"texte\": \"a\"}]");
        assertThat(emitted).hasSize(1);
        assertThat(emitted.get(0).getKey()).isEqualTo("faits");
        assertThat(emitted.get(0).getValue()).isEqualTo("[{\"texte\": \"a\"}]");
        emitted = ext.append(", \"risques\": []");
        assertThat(emitted).hasSize(1);
        assertThat(emitted.get(0).getKey()).isEqualTo("risques");
    }

    @Test
    void emitsMultipleSectionsAcrossChunks() {
        PartialJsonSectionExtractor ext = new PartialJsonSectionExtractor();
        ext.append("{\"timeline\": [");
        ext.append("{\"date\": \"2025-01-01\", \"evenement\": \"x\"}");
        ext.append("], \"faits\": [");
        ext.append("{\"texte\": \"f1\"}], \"risques\": []");
        // snapshot reflète l'ordre d'arrivée cumulé sur tous les append
        assertThat(ext.snapshot().keySet()).containsExactly("timeline", "faits", "risques");
    }

    @Test
    void doesNotReemitSectionsAlreadyClosed() {
        PartialJsonSectionExtractor ext = new PartialJsonSectionExtractor();
        ext.append("{\"faits\": [], \"risques\": [");
        List<Map.Entry<String, String>> first = ext.append("{\"texte\": \"r\"}]");
        assertThat(first).extracting(Map.Entry::getKey).containsExactly("risques");
        // append plus tard d'une section qui ne ferme rien de neuf : 0 émission.
        List<Map.Entry<String, String>> next = ext.append(", \"questions_ouvertes\": [");
        assertThat(next).isEmpty();
    }

    @Test
    void handlesNestedObjectsInsideSection() {
        PartialJsonSectionExtractor ext = new PartialJsonSectionExtractor();
        String chunk = "{\"travail_extracted_data\": {\"salaireBrut\": 3000, \"heuresSup\": {\"present\": true}}, \"faits\": []";
        List<Map.Entry<String, String>> emitted = ext.append(chunk);
        assertThat(emitted).extracting(Map.Entry::getKey).containsExactly("travail_extracted_data", "faits");
        assertThat(emitted.get(0).getValue()).contains("\"heuresSup\":");
    }

    @Test
    void handlesEscapedQuotesInsideStrings() {
        PartialJsonSectionExtractor ext = new PartialJsonSectionExtractor();
        String chunk = "{\"faits\": [{\"texte\": \"il a dit \\\"bonjour\\\" puis a signé\"}], \"risques\": []";
        List<Map.Entry<String, String>> emitted = ext.append(chunk);
        assertThat(emitted).extracting(Map.Entry::getKey).containsExactly("faits", "risques");
    }

    @Test
    void handlesScalarValues() {
        PartialJsonSectionExtractor ext = new PartialJsonSectionExtractor();
        ext.append("{\"risk_level\": \"MOYEN\", \"risk_score\": 65,");
        ext.append(" \"faits\": []");
        Map<String, String> snap = ext.snapshot();
        assertThat(snap.keySet()).containsExactly("risk_level", "risk_score", "faits");
        assertThat(snap.get("risk_level")).isEqualTo("\"MOYEN\"");
        assertThat(snap.get("risk_score")).isEqualTo("65");
    }

    @Test
    void snapshotReturnsSectionsInArrivalOrder() {
        PartialJsonSectionExtractor ext = new PartialJsonSectionExtractor();
        ext.append("{\"timeline\": [], \"faits\": [], \"risques\": []");
        Map<String, String> snap = ext.snapshot();
        assertThat(snap.keySet()).containsExactly("timeline", "faits", "risques");
    }

    @Test
    void noEmissionUntilRootObjectStarts() {
        PartialJsonSectionExtractor ext = new PartialJsonSectionExtractor();
        // ignore le préambule whitespace / texte avant l'object root
        assertThat(ext.append("   \n\t")).isEmpty();
        List<Map.Entry<String, String>> emitted = ext.append("{\"faits\": []");
        assertThat(emitted).extracting(Map.Entry::getKey).containsExactly("faits");
    }

    @Test
    void appendNullOrEmptyIsNoop() {
        PartialJsonSectionExtractor ext = new PartialJsonSectionExtractor();
        assertThat(ext.append(null)).isEmpty();
        assertThat(ext.append("")).isEmpty();
    }

    @Test
    void doesNotThrowOnIncompleteString() {
        PartialJsonSectionExtractor ext = new PartialJsonSectionExtractor();
        // chaîne ouverte non fermée : ne doit rien retourner et ne pas planter
        List<Map.Entry<String, String>> emitted = ext.append("{\"faits\": [{\"texte\": \"unfinished");
        assertThat(emitted).isEmpty();
    }

    @Test
    void closesObjectSectionWithImbricatedArrays() {
        PartialJsonSectionExtractor ext = new PartialJsonSectionExtractor();
        String chunk = "{\"detection\": {\"answers\": [{\"q\": 1}, {\"q\": 2}]}, \"risques\": []";
        List<Map.Entry<String, String>> emitted = ext.append(chunk);
        assertThat(emitted).extracting(Map.Entry::getKey).containsExactly("detection", "risques");
    }
}
