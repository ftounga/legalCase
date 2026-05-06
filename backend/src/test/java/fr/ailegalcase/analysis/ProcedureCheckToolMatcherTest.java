package fr.ailegalcase.analysis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-193 SF-193-01 — UT du mapping {@code critereCode → toolId}.
 *
 * <p>Pattern miroir {@link RetainedPisteToolMatcherTest} (F-192).</p>
 */
class ProcedureCheckToolMatcherTest {

    @Test
    void licenciementCriteres_mapToFDT08() {
        // FR
        assertThat(ProcedureCheckToolMatcher.resolveToolId("FR_CONVOCATION"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_DT_08_LICENCIEMENT);
        assertThat(ProcedureCheckToolMatcher.resolveToolId("FR_DELAI_NOTIFICATION"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_DT_08_LICENCIEMENT);
        assertThat(ProcedureCheckToolMatcher.resolveToolId("FR_MOTIVATION"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_DT_08_LICENCIEMENT);
        // BE
        assertThat(ProcedureCheckToolMatcher.resolveToolId("BE_NOTIFICATION"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_DT_08_LICENCIEMENT);
        assertThat(ProcedureCheckToolMatcher.resolveToolId("BE_PREAVIS"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_DT_08_LICENCIEMENT);
    }

    @Test
    void ruptureConvCriteres_mapToFDT10() {
        assertThat(ProcedureCheckToolMatcher.resolveToolId("RC_CONSENTEMENT"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_DT_10_RUPTURE_CONV);
        assertThat(ProcedureCheckToolMatcher.resolveToolId("RC_DELAI_RETRACTATION"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_DT_10_RUPTURE_CONV);
        assertThat(ProcedureCheckToolMatcher.resolveToolId("RC_HOMOLOGATION"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_DT_10_RUPTURE_CONV);
    }

    @Test
    void typeRuptureCritere_mapsToFDT09Indemnites() {
        assertThat(ProcedureCheckToolMatcher.resolveToolId("DT09_TYPE_RUPTURE"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_DT_09_INDEMNITES);
    }

    @Test
    void immigrationCriteres_mapToCorrectTools() {
        assertThat(ProcedureCheckToolMatcher.resolveToolId("IM05_MOTIF"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_IM_05_TITRE);
        assertThat(ProcedureCheckToolMatcher.resolveToolId("IM06_RECOURS_TYPE"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_IM_06_RECOURS);
        assertThat(ProcedureCheckToolMatcher.resolveToolId("IM07_TITRE_TYPE"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_IM_07_DROIT_TRAVAIL);
    }

    @Test
    void im21Criteres_mapToFIM21() {
        // 18 critères IM21_* → tous mappent F-IM-21 par préfixe
        assertThat(ProcedureCheckToolMatcher.resolveToolId("IM21_REGULARITE_SEJOUR_FR"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_IM_21_VALIDITE);
        assertThat(ProcedureCheckToolMatcher.resolveToolId("IM21_REGULARITE_SEJOUR_BE"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_IM_21_VALIDITE);
        assertThat(ProcedureCheckToolMatcher.resolveToolId("IM21_RESSOURCES_FR"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_IM_21_VALIDITE);
        assertThat(ProcedureCheckToolMatcher.resolveToolId("IM21_LOGEMENT_BE"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_IM_21_VALIDITE);
    }

    @Test
    void familleCriteres_mapToCorrectTools() {
        // F-FA-05
        assertThat(ProcedureCheckToolMatcher.resolveToolId("FA05_VALEUR_VENALE"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_FA_05_PARTAGE);
        assertThat(ProcedureCheckToolMatcher.resolveToolId("FA05_CAPITAL_RESTANT"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_FA_05_PARTAGE);
        // F-FA-06
        assertThat(ProcedureCheckToolMatcher.resolveToolId("FA06_MODE_GARDE"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_FA_06_GARDE);
        // F-FA-07 FR
        assertThat(ProcedureCheckToolMatcher.resolveToolId("FR_CHOIX_AVOCATS"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_FA_07_DIVORCE);
        assertThat(ProcedureCheckToolMatcher.resolveToolId("FR_DELAI_REFLEXION"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_FA_07_DIVORCE);
        // F-FA-07 BE
        assertThat(ProcedureCheckToolMatcher.resolveToolId("BE_REQUETE_CONJOINTE"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_FA_07_DIVORCE);
    }

    @Test
    void unknownCritereCode_returnsNull() {
        assertThat(ProcedureCheckToolMatcher.resolveToolId("UNKNOWN_CODE")).isNull();
        assertThat(ProcedureCheckToolMatcher.resolveToolId("FR_UNKNOWN")).isNull();
    }

    @Test
    void nullOrBlankCritereCode_returnsNull() {
        assertThat(ProcedureCheckToolMatcher.resolveToolId(null)).isNull();
        assertThat(ProcedureCheckToolMatcher.resolveToolId("")).isNull();
        assertThat(ProcedureCheckToolMatcher.resolveToolId("   ")).isNull();
    }

    @Test
    void critereCode_caseInsensitive() {
        // resolveToolId trim + uppercase, accepte les variantes
        assertThat(ProcedureCheckToolMatcher.resolveToolId("im05_motif"))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_IM_05_TITRE);
        assertThat(ProcedureCheckToolMatcher.resolveToolId("  FR_CONVOCATION  "))
                .isEqualTo(ProcedureCheckToolMatcher.TOOL_DT_08_LICENCIEMENT);
    }
}
