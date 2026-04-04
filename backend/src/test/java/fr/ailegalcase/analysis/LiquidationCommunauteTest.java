package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LiquidationCommunauteTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── Extraction JSON complet ──────────────────────────────────────────────

    @Test
    void jsonComplet_retourneResultNonNull() throws Exception {
        String json = """
                {"liquidation_communaute_data": {
                  "regime_matrimonial": "COMMUNAUTE_LEGALE",
                  "actif_commun": [
                    {"libelle": "Résidence principale", "valeur_estimee": 280000},
                    {"libelle": "Compte joint", "valeur_estimee": 15000}
                  ],
                  "biens_propres_epoux_a": [
                    {"libelle": "Appartement hérité", "valeur_estimee": 120000}
                  ],
                  "biens_propres_epoux_b": [],
                  "passif_commun": [
                    {"libelle": "Crédit immobilier", "montant": 95000}
                  ]
                }}""";
        JsonNode root = MAPPER.readTree(json);
        LiquidationCommunauteResult result = CaseAnalysisResponse.extractLiquidationCommunaute(root);

        assertThat(result).isNotNull();
        assertThat(result.regimeMatrimonial()).isEqualTo("COMMUNAUTE_LEGALE");
        assertThat(result.actifCommun()).hasSize(2);
        assertThat(result.actifCommun().get(0).libelle()).isEqualTo("Résidence principale");
        assertThat(result.actifCommun().get(0).valeur()).isEqualTo(280000.0);
        assertThat(result.biensPropresEpouxA()).hasSize(1);
        assertThat(result.biensPropresEpouxB()).isEmpty();
        assertThat(result.passifCommun()).hasSize(1);
        assertThat(result.passifCommun().get(0).valeur()).isEqualTo(95000.0);
    }

    @Test
    void valeurNull_bienItemConserveLibelle() throws Exception {
        String json = """
                {"liquidation_communaute_data": {
                  "regime_matrimonial": null,
                  "actif_commun": [{"libelle": "Bien non valorisé", "valeur_estimee": null}],
                  "biens_propres_epoux_a": [],
                  "biens_propres_epoux_b": [],
                  "passif_commun": []
                }}""";
        JsonNode root = MAPPER.readTree(json);
        LiquidationCommunauteResult result = CaseAnalysisResponse.extractLiquidationCommunaute(root);

        assertThat(result).isNotNull();
        assertThat(result.regimeMatrimonial()).isNull();
        assertThat(result.actifCommun()).hasSize(1);
        assertThat(result.actifCommun().get(0).libelle()).isEqualTo("Bien non valorisé");
        assertThat(result.actifCommun().get(0).valeur()).isNull();
    }

    @Test
    void champAbsent_retourneNull() throws Exception {
        JsonNode root = MAPPER.readTree("{}");
        assertThat(CaseAnalysisResponse.extractLiquidationCommunaute(root)).isNull();
    }

    @Test
    void listesVides_retourneesVides() throws Exception {
        String json = """
                {"liquidation_communaute_data": {
                  "regime_matrimonial": "SEPARATION_BIENS",
                  "actif_commun": [],
                  "biens_propres_epoux_a": [],
                  "biens_propres_epoux_b": [],
                  "passif_commun": []
                }}""";
        JsonNode root = MAPPER.readTree(json);
        LiquidationCommunauteResult result = CaseAnalysisResponse.extractLiquidationCommunaute(root);

        assertThat(result).isNotNull();
        assertThat(result.actifCommun()).isEmpty();
        assertThat(result.passifCommun()).isEmpty();
    }

    @Test
    void itemSansLibelle_ignore() throws Exception {
        String json = """
                {"liquidation_communaute_data": {
                  "regime_matrimonial": "COMMUNAUTE_LEGALE",
                  "actif_commun": [
                    {"valeur_estimee": 50000},
                    {"libelle": "Voiture", "valeur_estimee": 12000}
                  ],
                  "biens_propres_epoux_a": [],
                  "biens_propres_epoux_b": [],
                  "passif_commun": []
                }}""";
        JsonNode root = MAPPER.readTree(json);
        LiquidationCommunauteResult result = CaseAnalysisResponse.extractLiquidationCommunaute(root);

        assertThat(result).isNotNull();
        assertThat(result.actifCommun()).hasSize(1);
        assertThat(result.actifCommun().get(0).libelle()).isEqualTo("Voiture");
    }

    // ── LegalDomainPromptBuilder ─────────────────────────────────────────────

    @Test
    void promptFamille_contientLiquidationCommunaute() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction)
                .contains("liquidation_communaute_data")
                .contains("regime_matrimonial")
                .contains("actif_commun")
                .contains("biens_propres_epoux_a")
                .contains("passif_commun");
    }

    @Test
    void regimeMatrimonialNull_conserveNull() throws Exception {
        String json = """
                {"liquidation_communaute_data": {
                  "regime_matrimonial": null,
                  "actif_commun": [],
                  "biens_propres_epoux_a": [],
                  "biens_propres_epoux_b": [],
                  "passif_commun": []
                }}""";
        JsonNode root = MAPPER.readTree(json);
        LiquidationCommunauteResult result = CaseAnalysisResponse.extractLiquidationCommunaute(root);
        assertThat(result).isNotNull();
        assertThat(result.regimeMatrimonial()).isNull();
    }

    @Test
    void champNullNode_retourneNull() throws Exception {
        JsonNode root = MAPPER.readTree("{\"liquidation_communaute_data\": null}");
        assertThat(CaseAnalysisResponse.extractLiquidationCommunaute(root)).isNull();
    }
}
