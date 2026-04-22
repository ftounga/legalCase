package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-146 SF-146-01 : tests du parseur {@link CaseAnalysisResponse#extractItemList}
 * pour le nouveau champ {@code sourceRef}.
 */
class SourceRefParsingTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // U-01 : item avec sourceRef complet → tous les champs peuplés
    @Test
    void extractItemList_withFullSourceRef_parsesAllFields() throws Exception {
        String json = """
                {"faits":[{
                    "texte":"Le contrat de travail a été signé le 2 janvier 2022.",
                    "source":"dossier_complet.pdf",
                    "extrait":"signé le 2 janvier 2022",
                    "sourceRef":{
                        "documentName":"dossier_complet.pdf",
                        "pieceType":"CONTRAT",
                        "pieceLabel":"Contrat de travail Dupont",
                        "pageStart":1,
                        "pageEnd":2
                    }
                }]}
                """;
        JsonNode root = MAPPER.readTree(json);
        List<AnalysisItem> items = CaseAnalysisResponse.extractItemList(root, "faits");

        assertThat(items).hasSize(1);
        AnalysisItem item = items.get(0);
        assertThat(item.texte()).contains("signé le 2 janvier");
        assertThat(item.source()).isEqualTo("dossier_complet.pdf");
        assertThat(item.extrait()).isEqualTo("signé le 2 janvier 2022");
        assertThat(item.sourceRef()).isNotNull();
        assertThat(item.sourceRef().documentName()).isEqualTo("dossier_complet.pdf");
        assertThat(item.sourceRef().pieceType()).isEqualTo("CONTRAT");
        assertThat(item.sourceRef().pieceLabel()).isEqualTo("Contrat de travail Dupont");
        assertThat(item.sourceRef().pageStart()).isEqualTo(1);
        assertThat(item.sourceRef().pageEnd()).isEqualTo(2);
    }

    // U-02 : item sans sourceRef (legacy) → sourceRef == null, autres champs préservés
    @Test
    void extractItemList_withoutSourceRef_keepsLegacyFields() throws Exception {
        String json = """
                {"faits":[{
                    "texte":"Fait legacy",
                    "source":"contrat.pdf",
                    "extrait":"..."
                }]}
                """;
        JsonNode root = MAPPER.readTree(json);
        List<AnalysisItem> items = CaseAnalysisResponse.extractItemList(root, "faits");

        assertThat(items).hasSize(1);
        assertThat(items.get(0).sourceRef()).isNull();
        assertThat(items.get(0).source()).isEqualTo("contrat.pdf");
    }

    // U-03 : sourceRef avec champs partiels (pageEnd manquant) → parse quand même,
    // champs manquants = null
    @Test
    void extractItemList_withPartialSourceRef_parsesAvailableFields() throws Exception {
        String json = """
                {"faits":[{
                    "texte":"…",
                    "sourceRef":{
                        "documentName":"doc.pdf",
                        "pieceType":"SMS",
                        "pageStart":3
                    }
                }]}
                """;
        JsonNode root = MAPPER.readTree(json);
        List<AnalysisItem> items = CaseAnalysisResponse.extractItemList(root, "faits");

        assertThat(items).hasSize(1);
        SourceRef ref = items.get(0).sourceRef();
        assertThat(ref).isNotNull();
        assertThat(ref.documentName()).isEqualTo("doc.pdf");
        assertThat(ref.pieceType()).isEqualTo("SMS");
        assertThat(ref.pageStart()).isEqualTo(3);
        assertThat(ref.pieceLabel()).isNull();
        assertThat(ref.pageEnd()).isNull();
    }

    // U-04 : sourceRef vide {} → null (tous les champs manquants = pas utile)
    @Test
    void extractItemList_emptySourceRef_returnsNull() throws Exception {
        String json = "{\"faits\":[{\"texte\":\"…\",\"sourceRef\":{}}]}";
        JsonNode root = MAPPER.readTree(json);
        List<AnalysisItem> items = CaseAnalysisResponse.extractItemList(root, "faits");
        assertThat(items.get(0).sourceRef()).isNull();
    }

    // U-05 : sourceRef avec types erronés (page en string au lieu d'int) → champ null (fail-open)
    @Test
    void extractItemList_sourceRefWithWrongTypes_failOpen() throws Exception {
        String json = """
                {"faits":[{
                    "texte":"…",
                    "sourceRef":{
                        "documentName":"doc.pdf",
                        "pageStart":"not-a-number"
                    }
                }]}
                """;
        JsonNode root = MAPPER.readTree(json);
        List<AnalysisItem> items = CaseAnalysisResponse.extractItemList(root, "faits");
        SourceRef ref = items.get(0).sourceRef();
        assertThat(ref).isNotNull();
        assertThat(ref.documentName()).isEqualTo("doc.pdf");
        assertThat(ref.pageStart()).isNull(); // fail-open sur type erroné
    }

    // U-06 : item string (plus ancien format) → sourceRef null, source null
    @Test
    void extractItemList_plainString_preservesLegacy() throws Exception {
        String json = "{\"faits\":[\"Fait simple texte sans objet\"]}";
        JsonNode root = MAPPER.readTree(json);
        List<AnalysisItem> items = CaseAnalysisResponse.extractItemList(root, "faits");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).texte()).isEqualTo("Fait simple texte sans objet");
        assertThat(items.get(0).source()).isNull();
        assertThat(items.get(0).sourceRef()).isNull();
    }
}
