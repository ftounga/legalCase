package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static fr.ailegalcase.casefile.LanceurAlerteProtectionCalculator.AnalyseLanceurAlerte;
import static fr.ailegalcase.casefile.LanceurAlerteProtectionCalculator.CodeCritere;
import static fr.ailegalcase.casefile.LanceurAlerteProtectionInput.NatureMesureRepresaille;
import static fr.ailegalcase.casefile.LanceurAlerteProtectionInput.NatureSignalement;
import static fr.ailegalcase.casefile.LanceurAlerteProtectionInput.ProcedureSignalement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-212-25 — tests unitaires du calculateur d'évaluation de la
 * protection du lanceur d'alerte (F-DT-61, FRANCE — L. 1132-3-3 CT ;
 * loi Sapin II ; loi Waserman 2022).
 *
 * <p>Couvre les critères d'acceptation de la mini-spec : 3 verdicts
 * (PROTECTION_FORTE / PARTIELLE / HORS_CHAMP), exclusion contrepartie
 * financière, nullité mesure représaille (L. 1132-3-3), montant min DI
 * 10 000 € loi Waserman, gate country FRANCE.</p>
 */
class LanceurAlerteProtectionCalculatorTest {

    /** Input baseline — signalement crime/délit + interne + référent saisi + pas de représailles. */
    private static LanceurAlerteProtectionInput baseline() {
        return new LanceurAlerteProtectionInput(
                NatureSignalement.CRIME_DELIT,
                false,                      // pas de contrepartie financière
                ProcedureSignalement.INTERNE,
                true,                       // référent saisi
                false,                      // pas de représailles
                NatureMesureRepresaille.AUCUNE
        );
    }

    // ── AC1 : signalement crime/délit + licenciement → PROTECTION_FORTE ──────

    @Test
    void crimeDelit_avecLicenciement_returnsProtectionForte_etNullite() {
        var input = new LanceurAlerteProtectionInput(
                NatureSignalement.CRIME_DELIT,
                false,
                ProcedureSignalement.INTERNE,
                true,
                true,                       // représailles
                NatureMesureRepresaille.LICENCIEMENT
        );
        var r = LanceurAlerteProtectionCalculator.compute(input, "FRANCE");
        assertThat(r.analyseLanceurAlerte()).isEqualTo(AnalyseLanceurAlerte.PROTECTION_FORTE);
        assertThat(r.nulliteMesureRepresaille()).isTrue();
        assertThat(r.montantMinDommagesInterets()).isEqualTo(10_000.0);
        assertThat(r.country()).isEqualTo("FRANCE");
        assertThat(r.pointsAnalyse())
                .anyMatch(p -> p.code() == CodeCritere.DT61_NATURE_SIGNALEMENT)
                .anyMatch(p -> p.code() == CodeCritere.DT61_CONTREPARTIE)
                .anyMatch(p -> p.code() == CodeCritere.DT61_PROCEDURE)
                .anyMatch(p -> p.code() == CodeCritere.DT61_MESURE_REPRESAILLE);
    }

    // ── AC2 : contrepartie financière → HORS_CHAMP ──────────────────────────

    @Test
    void contrepartieFinanciere_returnsHorsChamp_etPasDeMontantDi() {
        var input = new LanceurAlerteProtectionInput(
                NatureSignalement.CRIME_DELIT,
                true,                       // contrepartie financière
                ProcedureSignalement.INTERNE,
                true,
                false,
                NatureMesureRepresaille.AUCUNE
        );
        var r = LanceurAlerteProtectionCalculator.compute(input, "FRANCE");
        assertThat(r.analyseLanceurAlerte()).isEqualTo(AnalyseLanceurAlerte.HORS_CHAMP);
        assertThat(r.nulliteMesureRepresaille()).isFalse();
        assertThat(r.montantMinDommagesInterets()).isEqualTo(0.0);
        assertThat(r.messages()).anyMatch(m -> m.contains("HORS CHAMP"));
    }

    // ── AC3 : procédure interne non suivie → PROTECTION_PARTIELLE ───────────

    @Test
    void procedureInterne_referentNonSaisi_returnsProtectionPartielle() {
        var input = new LanceurAlerteProtectionInput(
                NatureSignalement.CRIME_DELIT,
                false,
                ProcedureSignalement.INTERNE,
                false,                      // référent NON saisi
                false,
                NatureMesureRepresaille.AUCUNE
        );
        var r = LanceurAlerteProtectionCalculator.compute(input, "FRANCE");
        assertThat(r.analyseLanceurAlerte()).isEqualTo(AnalyseLanceurAlerte.PROTECTION_PARTIELLE);
        assertThat(r.montantMinDommagesInterets()).isEqualTo(10_000.0);
        assertThat(r.messages()).anyMatch(m -> m.contains("PROTECTION PARTIELLE"));
    }

    // ── AC4 : procédure externe → PROTECTION_FORTE ──────────────────────────

    @Test
    void procedureExterne_returnsProtectionForte() {
        var input = new LanceurAlerteProtectionInput(
                NatureSignalement.VIOLATION_DROIT_UE,
                false,
                ProcedureSignalement.EXTERNE,
                null,
                false,
                NatureMesureRepresaille.AUCUNE
        );
        var r = LanceurAlerteProtectionCalculator.compute(input, "FRANCE");
        assertThat(r.analyseLanceurAlerte()).isEqualTo(AnalyseLanceurAlerte.PROTECTION_FORTE);
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("Directive UE 2019/1937"))
                .anyMatch(b -> b.contains("Waserman"));
    }

    // ── AC5 : divulgation publique → PROTECTION_PARTIELLE ───────────────────

    @Test
    void divulgationPublique_returnsProtectionPartielle() {
        var input = new LanceurAlerteProtectionInput(
                NatureSignalement.MENACE_INTERET_GENERAL,
                false,
                ProcedureSignalement.DIVULGATION_PUBLIQUE,
                null,
                false,
                NatureMesureRepresaille.AUCUNE
        );
        var r = LanceurAlerteProtectionCalculator.compute(input, "FRANCE");
        assertThat(r.analyseLanceurAlerte()).isEqualTo(AnalyseLanceurAlerte.PROTECTION_PARTIELLE);
        assertThat(r.montantMinDommagesInterets()).isEqualTo(10_000.0);
    }

    // ── AC6 : sanction de représailles → nullité ────────────────────────────

    @Test
    void sanctionRepresailles_nulliteMesureRepresailleTrue() {
        var input = new LanceurAlerteProtectionInput(
                NatureSignalement.CRIME_DELIT,
                false,
                ProcedureSignalement.EXTERNE,
                null,
                true,
                NatureMesureRepresaille.SANCTION
        );
        var r = LanceurAlerteProtectionCalculator.compute(input, "FRANCE");
        assertThat(r.nulliteMesureRepresaille()).isTrue();
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 1132-3-3"));
        assertThat(r.messages()).anyMatch(m -> m.contains("nullité de plein droit"));
    }

    // ── AC7 : mesure discriminatoire de représailles → nullité ──────────────

    @Test
    void mesureDiscriminatoire_nulliteMesureRepresailleTrue() {
        var input = new LanceurAlerteProtectionInput(
                NatureSignalement.MENACE_INTERET_GENERAL,
                false,
                ProcedureSignalement.INTERNE,
                true,
                true,
                NatureMesureRepresaille.MESURE_DISCRIMINATOIRE
        );
        var r = LanceurAlerteProtectionCalculator.compute(input, "FRANCE");
        assertThat(r.nulliteMesureRepresaille()).isTrue();
        assertThat(r.analyseLanceurAlerte()).isEqualTo(AnalyseLanceurAlerte.PROTECTION_FORTE);
    }

    // ── AC8 : nature signalement AUTRE → HORS_CHAMP ─────────────────────────

    @Test
    void natureSignalementAutre_returnsHorsChamp() {
        var input = new LanceurAlerteProtectionInput(
                NatureSignalement.AUTRE,
                false,
                ProcedureSignalement.INTERNE,
                true,
                false,
                NatureMesureRepresaille.AUCUNE
        );
        var r = LanceurAlerteProtectionCalculator.compute(input, "FRANCE");
        assertThat(r.analyseLanceurAlerte()).isEqualTo(AnalyseLanceurAlerte.HORS_CHAMP);
        assertThat(r.montantMinDommagesInterets()).isEqualTo(0.0);
        assertThat(r.messages()).anyMatch(m -> m.contains("HORS CHAMP"));
    }

    // ── AC9 : gate country FRANCE ───────────────────────────────────────────

    @Test
    void countryBelgique_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                LanceurAlerteProtectionCalculator.compute(baseline(), "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void countryNull_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                LanceurAlerteProtectionCalculator.compute(baseline(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inputNull_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                LanceurAlerteProtectionCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manquantes");
    }

    @Test
    void natureSignalementNull_throwsIllegalArgument() {
        var input = new LanceurAlerteProtectionInput(
                null, false, ProcedureSignalement.INTERNE, true, false,
                NatureMesureRepresaille.AUCUNE);
        assertThatThrownBy(() ->
                LanceurAlerteProtectionCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nature");
    }

    @Test
    void procedureNull_throwsIllegalArgument() {
        var input = new LanceurAlerteProtectionInput(
                NatureSignalement.CRIME_DELIT, false, null, true, false,
                NatureMesureRepresaille.AUCUNE);
        assertThatThrownBy(() ->
                LanceurAlerteProtectionCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Procédure");
    }

    // ── Score borné ──────────────────────────────────────────────────────────

    @Test
    void score_borneEntreZeroEtCent() {
        var input = new LanceurAlerteProtectionInput(
                NatureSignalement.CRIME_DELIT, false,
                ProcedureSignalement.EXTERNE, null,
                true, NatureMesureRepresaille.LICENCIEMENT);
        var r = LanceurAlerteProtectionCalculator.compute(input, "FRANCE");
        assertThat(r.scoreProtection()).isBetween(0, 100);
    }

    // ── Bases juridiques contiennent L. 1132-3-3 si protection ──────────────

    @Test
    void protectionForte_basesContiennent_L1132_3_3() {
        var r = LanceurAlerteProtectionCalculator.compute(baseline(), "FRANCE");
        assertThat(r.analyseLanceurAlerte()).isEqualTo(AnalyseLanceurAlerte.PROTECTION_FORTE);
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 1132-3-3"));
    }

    // ── Points d'analyse F-IA-03 : les 4 codes critères sont présents ───────

    @Test
    void pointsAnalyse_contiennentLes4CodesCriteres() {
        var r = LanceurAlerteProtectionCalculator.compute(baseline(), "FRANCE");
        assertThat(r.pointsAnalyse())
                .extracting(LanceurAlerteProtectionCalculator.PointAnalyse::code)
                .containsExactlyInAnyOrder(
                        CodeCritere.DT61_NATURE_SIGNALEMENT,
                        CodeCritere.DT61_CONTREPARTIE,
                        CodeCritere.DT61_PROCEDURE,
                        CodeCritere.DT61_MESURE_REPRESAILLE);
    }

    // ── Montant 10 000 € seulement si protection forte/partielle ────────────

    @Test
    void montantDi_zeroSiHorsChamp() {
        var input = new LanceurAlerteProtectionInput(
                NatureSignalement.AUTRE, false,
                ProcedureSignalement.INTERNE, true,
                false, NatureMesureRepresaille.AUCUNE);
        var r = LanceurAlerteProtectionCalculator.compute(input, "FRANCE");
        assertThat(r.montantMinDommagesInterets()).isEqualTo(0.0);
    }

    @Test
    void montantDi_dixMilleSiProtectionForte() {
        var r = LanceurAlerteProtectionCalculator.compute(baseline(), "FRANCE");
        assertThat(r.montantMinDommagesInterets()).isEqualTo(10_000.0);
    }
}
