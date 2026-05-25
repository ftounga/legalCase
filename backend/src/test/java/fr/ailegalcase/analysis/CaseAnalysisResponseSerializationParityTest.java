package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-256 — test de parité JSON externe.
 *
 * Vérifie que le refactor de {@code TravailExtractedData} en sous-records
 * {@code @JsonUnwrapped} (gain ~31 slots libérés sur le constructeur canonical)
 * laisse le JSON HTTP camelCase strictement plat. Les frontends existants
 * (composants `*-section`, helpers `*-prefill-rules`) lisent les champs à
 * plat — pour les 7 outils consolidés (CSP, faute inexcusable, lanceur
 * alerte, modif contrat, mutation, télétravail, mise à pied disciplinaire),
 * la projection JSON ne doit PAS exposer les sous-objets imbriqués.
 *
 * Pour les 3 sous-records nouveaux (rupture anticipée CDD, démission
 * équivoque, PDV/RCC), la projection JSON les expose comme sous-objets
 * structurés — c'est le contrat aligné sur le prompt {@code LegalDomainPromptBuilder}
 * PART9/PART11/PART12 et sur les helpers prefill front qui lisent
 * {@code aiData.pdvRccDetail.pdvRccTypeDispositif}.
 */
class CaseAnalysisResponseSerializationParityTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serialize_unwrapped_subRecords_areFlattened_inHttpJson() throws Exception {
        // GIVEN un record TravailExtractedData rempli sur les 7 outils consolidés
        var travail = CaseAnalysisResponse.TravailExtractedData.builder()
                .cspDetail(new CaseAnalysisResponse.TravailExtractedData.CspDetail(
                        50, true, true, "2024-03-15", false, 2500.0))
                .fauteInexcusableDetail(new CaseAnalysisResponse.TravailExtractedData.FauteInexcusableDetail(
                        true, false, true, 30))
                .lanceurAlerteDetail(new CaseAnalysisResponse.TravailExtractedData.LanceurAlerteDetail(
                        "Corruption", "Interne", true, "Mutation"))
                .modifContratDetail(new CaseAnalysisResponse.TravailExtractedData.ModifContratDetail(
                        "Horaire", true, false, true))
                .mutationMobiliteDetail(new CaseAnalysisResponse.TravailExtractedData.MutationMobiliteDetail(
                        true, true, false, 4, true, false))
                .teletravailDetail(new CaseAnalysisResponse.TravailExtractedData.TeletravailDetail(
                        "Avenant", true, true, 5.5, false, true, false))
                .miseAPiedDetail(new CaseAnalysisResponse.TravailExtractedData.MiseAPiedDetail(
                        "DISCIPLINAIRE", true, true, true, 3, true, false))
                .build();

        // WHEN sérialisé par Jackson 2.19 avec @JsonUnwrapped
        String json = mapper.writeValueAsString(travail);
        JsonNode root = mapper.readTree(json);

        // THEN les champs des 7 sous-records consolidés doivent être à plat
        // au niveau racine — PAS de sous-objet `cspDetail`, `miseAPiedDetail`, etc.
        assertThat(root.has("cspDetail")).isFalse();
        assertThat(root.has("fauteInexcusableDetail")).isFalse();
        assertThat(root.has("lanceurAlerteDetail")).isFalse();
        assertThat(root.has("modifContratDetail")).isFalse();
        assertThat(root.has("mutationMobiliteDetail")).isFalse();
        assertThat(root.has("teletravailDetail")).isFalse();
        assertThat(root.has("miseAPiedDetail")).isFalse();

        // Les champs sont visibles à plat (parité avec l'interface TS frontend)
        assertThat(root.get("cspEffectifEntreprise").asInt()).isEqualTo(50);
        assertThat(root.get("cspProposeDetail").asBoolean()).isTrue();
        assertThat(root.get("cspDateRemise").asText()).isEqualTo("2024-03-15");
        assertThat(root.get("fauteInexcusableTauxIpp").asInt()).isEqualTo(30);
        assertThat(root.get("lanceurAlerteNatureSignalement").asText()).isEqualTo("Corruption");
        assertThat(root.get("modifContratElementModifie").asText()).isEqualTo("Horaire");
        assertThat(root.get("mutationDelaiPrevenanceSemaines").asInt()).isEqualTo(4);
        assertThat(root.get("teletravailCadre").asText()).isEqualTo("Avenant");
        assertThat(root.get("mapDisciplinaireNature").asText()).isEqualTo("DISCIPLINAIRE");
        assertThat(root.get("mapDisciplinaireDureeJours").asInt()).isEqualTo(3);
    }

    @Test
    void serialize_subRecords_pourDettes_sontExposeesCommeSousObjets() throws Exception {
        // GIVEN un record avec les 3 sous-records pour les outils en dette (F-DT-43/41/46)
        var travail = CaseAnalysisResponse.TravailExtractedData.builder()
                .ruptureAnticipeeCddDetail(new CaseAnalysisResponse.TravailExtractedData.RuptureAnticipeeCddDetail(
                        "EMPLOYEUR", "FAUTE_GRAVE", "2024-12-31"))
                .demissionEquivoqueDetail(new CaseAnalysisResponse.TravailExtractedData.DemissionEquivoqueDetail(
                        "SMS", true, true, false, true))
                .pdvRccDetail(new CaseAnalysisResponse.TravailExtractedData.PdvRccDetail(
                        "RCC", true, true, true))
                .build();

        // WHEN sérialisé
        String json = mapper.writeValueAsString(travail);
        JsonNode root = mapper.readTree(json);

        // THEN les 3 sous-records sont exposés comme sous-objets structurés
        // (PAS @JsonUnwrapped — alignement avec les helpers prefill frontend
        // qui lisent `aiData.pdvRccDetail.pdvRccTypeDispositif`).
        assertThat(root.has("ruptureAnticipeeCddDetail")).isTrue();
        assertThat(root.get("ruptureAnticipeeCddDetail").get("ruptureAnticipeeCddAuteur").asText())
                .isEqualTo("EMPLOYEUR");
        assertThat(root.get("ruptureAnticipeeCddDetail").get("ruptureAnticipeeCddMotif").asText())
                .isEqualTo("FAUTE_GRAVE");

        assertThat(root.has("demissionEquivoqueDetail")).isTrue();
        assertThat(root.get("demissionEquivoqueDetail").get("demissionModeExpression").asText())
                .isEqualTo("SMS");
        assertThat(root.get("demissionEquivoqueDetail").get("demissionContexteAltercation").asBoolean())
                .isTrue();

        assertThat(root.has("pdvRccDetail")).isTrue();
        assertThat(root.get("pdvRccDetail").get("pdvRccTypeDispositif").asText())
                .isEqualTo("RCC");
        assertThat(root.get("pdvRccDetail").get("pdvRccAccordMajoritaire").asBoolean()).isTrue();
    }

    @Test
    void parser_construit_les_sous_records_pour_les_3_dettes() {
        // GIVEN un JSON LLM (snake_case stocké) avec les 3 sous-objets pour les outils en dette
        CaseAnalysis analysis = analysisFromJson("""
                {
                  "travail_extracted_data": {
                    "rupture_anticipee_cdd_detail": {
                      "rupture_anticipee_cdd_auteur": "EMPLOYEUR",
                      "rupture_anticipee_cdd_motif": "INAPTITUDE",
                      "rupture_anticipee_cdd_date_terme": "2025-06-30"
                    },
                    "demission_equivoque_detail": {
                      "demission_mode_expression": "SMS",
                      "demission_contexte_altercation": true,
                      "demission_pression": true,
                      "demission_retractation": false,
                      "demission_manquements_employeur": false
                    },
                    "pdv_rcc_detail": {
                      "pdv_rcc_type_dispositif": "RCC",
                      "pdv_rcc_accord_majoritaire": true,
                      "pdv_rcc_validation_dreets": true,
                      "pdv_rcc_indemnites_legales": false
                    }
                  }
                }
                """);

        // WHEN parsing
        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);
        var travail = response.travailExtractedData();

        // THEN les 3 sous-records sont construits
        assertThat(travail).isNotNull();
        assertThat(travail.ruptureAnticipeeCddDetail()).isNotNull();
        assertThat(travail.ruptureAnticipeeCddDetail().ruptureAnticipeeCddAuteur()).isEqualTo("EMPLOYEUR");
        assertThat(travail.ruptureAnticipeeCddDetail().ruptureAnticipeeCddMotif()).isEqualTo("INAPTITUDE");
        assertThat(travail.ruptureAnticipeeCddDetail().ruptureAnticipeeCddDateTerme()).isEqualTo("2025-06-30");

        assertThat(travail.demissionEquivoqueDetail()).isNotNull();
        assertThat(travail.demissionEquivoqueDetail().demissionModeExpression()).isEqualTo("SMS");
        assertThat(travail.demissionEquivoqueDetail().demissionContexteAltercation()).isTrue();

        assertThat(travail.pdvRccDetail()).isNotNull();
        assertThat(travail.pdvRccDetail().pdvRccTypeDispositif()).isEqualTo("RCC");
        assertThat(travail.pdvRccDetail().pdvRccAccordMajoritaire()).isTrue();
    }

    @Test
    void parser_normalise_les_enums_invalides_en_null_pour_les_3_dettes() {
        CaseAnalysis analysis = analysisFromJson("""
                {
                  "travail_extracted_data": {
                    "rupture_anticipee_cdd_detail": {
                      "rupture_anticipee_cdd_auteur": "AUTRE_VALEUR",
                      "rupture_anticipee_cdd_motif": "INCONNU"
                    },
                    "pdv_rcc_detail": {
                      "pdv_rcc_type_dispositif": "PSE"
                    }
                  }
                }
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);
        var travail = response.travailExtractedData();

        assertThat(travail.ruptureAnticipeeCddDetail()).isNotNull();
        assertThat(travail.ruptureAnticipeeCddDetail().ruptureAnticipeeCddAuteur()).isNull();
        assertThat(travail.ruptureAnticipeeCddDetail().ruptureAnticipeeCddMotif()).isNull();

        assertThat(travail.pdvRccDetail()).isNotNull();
        assertThat(travail.pdvRccDetail().pdvRccTypeDispositif()).isNull();
    }

    @Test
    void parser_flag_top_level_demission_et_pdv_sont_extraits_depuis_le_record() {
        CaseAnalysis analysis = analysisFromJson("""
                {
                  "travail_extracted_data": {
                    "demission_equivoque_pressentie": true,
                    "pdv_rcc_envisage": true
                  }
                }
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);
        var travail = response.travailExtractedData();

        assertThat(travail.demissionEquivoquePressentie()).isTrue();
        assertThat(travail.pdvRccEnvisage()).isTrue();
    }

    @Test
    void serialize_emptyTravailExtractedData_doesNotEmitConsolidatedSubRecords() throws Exception {
        // GIVEN un record TravailExtractedData totalement vide (tous sous-records = null)
        var travail = CaseAnalysisResponse.TravailExtractedData.builder().build();

        // WHEN sérialisé
        String json = mapper.writeValueAsString(travail);
        JsonNode root = mapper.readTree(json);

        // THEN les noms des sous-records consolidés n'apparaissent pas
        // (@JsonUnwrapped : la valeur null du sous-record est masquée)
        assertThat(root.has("cspDetail")).isFalse();
        assertThat(root.has("miseAPiedDetail")).isFalse();
        assertThat(root.has("teletravailDetail")).isFalse();
        // Les 3 sous-records pour les dettes restent visibles (null mais clés présentes)
        assertThat(root.has("ruptureAnticipeeCddDetail")).isTrue();
        assertThat(root.get("ruptureAnticipeeCddDetail").isNull()).isTrue();
    }

    private CaseAnalysis analysisFromJson(String json) {
        CaseAnalysis a = new CaseAnalysis();
        a.setAnalysisStatus(AnalysisStatus.DONE);
        a.setAnalysisType(AnalysisType.STANDARD);
        a.setVersion(1);
        a.setAnalysisResult(json);
        a.setModelUsed("claude-sonnet-4-6");
        a.setUpdatedAt(Instant.now());
        return a;
    }
}
