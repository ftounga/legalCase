package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-213-10 : tests unitaires du
 * {@link LicenciementBeCct109DeraisonnableCalculator}.
 *
 * <p>Vérifie l'attribution de l'échelon CCT n° 109 (art. 9) par priorité
 * descendante : motif valable prouvé + procédures → NON_DERAISONNABLE,
 * discrimination + représailles → 17 semaines, discrimination OU représailles
 * → 12, motif disputé + aggravant → 8, sans motif / vague / procédures
 * non respectées → 3. Vérifie aussi : calcul indemnité = hebdo × semaines,
 * cumul ICP systématique, avertissement non null systématique.</p>
 */
class LicenciementBeCct109DeraisonnableCalculatorTest {

    private static final BigDecimal REM_HEBDO = new BigDecimal("500.00");

    private static LicenciementBeCct109DeraisonnableRequest req(
            boolean motifCommunique,
            LicenciementBeCct109MotifLieAPersonneEnum motif,
            boolean discrimination,
            boolean represailles,
            boolean procedures,
            BigDecimal hebdo,
            String args) {
        return new LicenciementBeCct109DeraisonnableRequest(
                motifCommunique, motif, discrimination, represailles,
                procedures, hebdo, args);
    }

    // ── Cas 1 : motif valable + procédures respectées → NON_DERAISONNABLE ──

    @Test
    void analyze_motifValable_proceduresRespectees_returnsNonDeraisonnable_indemnite0() {
        LicenciementBeCct109DeraisonnableResult r =
                LicenciementBeCct109DeraisonnableCalculator.analyze(
                        req(true,
                                LicenciementBeCct109MotifLieAPersonneEnum.MOTIF_PROUVE_VALIDE,
                                false, false, true, REM_HEBDO, null));

        assertThat(r.echelonCct109())
                .isEqualTo(LicenciementBeCct109EchelonEnum.NON_DERAISONNABLE);
        assertThat(r.nombreSemaines()).isEqualTo(0);
        assertThat(r.indemniteCct109()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.justificationEchelon())
                .contains("valable")
                .contains("procédures");
    }

    // ── Cas 2 : sans motif → minimum 3 semaines ────────────────────────────

    @Test
    void analyze_sansMotif_retourne3semaines() {
        LicenciementBeCct109DeraisonnableResult r =
                LicenciementBeCct109DeraisonnableCalculator.analyze(
                        req(false,
                                LicenciementBeCct109MotifLieAPersonneEnum.SANS_MOTIF,
                                false, false, true, REM_HEBDO, null));

        assertThat(r.echelonCct109())
                .isEqualTo(LicenciementBeCct109EchelonEnum.TROIS_SEMAINES);
        assertThat(r.nombreSemaines()).isEqualTo(3);
        assertThat(r.indemniteCct109()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    void analyze_motifNonCommunique_retourne3semaines() {
        LicenciementBeCct109DeraisonnableResult r =
                LicenciementBeCct109DeraisonnableCalculator.analyze(
                        req(false,
                                LicenciementBeCct109MotifLieAPersonneEnum.MOTIF_VAGUE,
                                false, false, true, REM_HEBDO, null));

        assertThat(r.echelonCct109())
                .isEqualTo(LicenciementBeCct109EchelonEnum.TROIS_SEMAINES);
        assertThat(r.nombreSemaines()).isEqualTo(3);
    }

    // ── Cas 3 : motif vague sans procédure → 3 semaines ────────────────────

    @Test
    void analyze_motifVague_sansProcedure_retourne3semaines() {
        LicenciementBeCct109DeraisonnableResult r =
                LicenciementBeCct109DeraisonnableCalculator.analyze(
                        req(true,
                                LicenciementBeCct109MotifLieAPersonneEnum.MOTIF_VAGUE,
                                false, false, false, REM_HEBDO, null));

        assertThat(r.echelonCct109())
                .isEqualTo(LicenciementBeCct109EchelonEnum.TROIS_SEMAINES);
        assertThat(r.nombreSemaines()).isEqualTo(3);
    }

    @Test
    void analyze_motifVague_avecProcedure_retourne3semaines() {
        // Motif vague reste à 3 semaines même si les procédures sont OK
        LicenciementBeCct109DeraisonnableResult r =
                LicenciementBeCct109DeraisonnableCalculator.analyze(
                        req(true,
                                LicenciementBeCct109MotifLieAPersonneEnum.MOTIF_VAGUE,
                                false, false, true, REM_HEBDO, null));

        assertThat(r.echelonCct109())
                .isEqualTo(LicenciementBeCct109EchelonEnum.TROIS_SEMAINES);
        assertThat(r.nombreSemaines()).isEqualTo(3);
    }

    // ── Cas 4 : motif disputé + 1 facteur aggravant mineur → 8 semaines ───

    @Test
    void analyze_motifPrecisDispute_proceduresNonRespectees_retourne8semaines() {
        LicenciementBeCct109DeraisonnableResult r =
                LicenciementBeCct109DeraisonnableCalculator.analyze(
                        req(true,
                                LicenciementBeCct109MotifLieAPersonneEnum.MOTIF_PRECIS_DISPUTE,
                                false, false, /* procedures */ false,
                                REM_HEBDO, null));

        assertThat(r.echelonCct109())
                .isEqualTo(LicenciementBeCct109EchelonEnum.HUIT_SEMAINES);
        assertThat(r.nombreSemaines()).isEqualTo(8);
        // 500 × 8 = 4000.00
        assertThat(r.indemniteCct109())
                .isEqualByComparingTo(new BigDecimal("4000.00"));
        assertThat(r.justificationEchelon())
                .contains("disputé")
                .contains("aggravant");
    }

    @Test
    void analyze_motifPrecisDispute_proceduresRespectees_retourne3semaines_fallback() {
        // Motif disputé sans aggravant → fallback prudent 3 semaines
        LicenciementBeCct109DeraisonnableResult r =
                LicenciementBeCct109DeraisonnableCalculator.analyze(
                        req(true,
                                LicenciementBeCct109MotifLieAPersonneEnum.MOTIF_PRECIS_DISPUTE,
                                false, false, true, REM_HEBDO, null));

        assertThat(r.echelonCct109())
                .isEqualTo(LicenciementBeCct109EchelonEnum.TROIS_SEMAINES);
        assertThat(r.nombreSemaines()).isEqualTo(3);
    }

    // ── Cas 5 : discrimination seule → 12 semaines ─────────────────────────

    @Test
    void analyze_discriminationSuspectee_retourne12semaines() {
        LicenciementBeCct109DeraisonnableResult r =
                LicenciementBeCct109DeraisonnableCalculator.analyze(
                        req(true,
                                LicenciementBeCct109MotifLieAPersonneEnum.MOTIF_PRECIS_DISPUTE,
                                /* discrimination */ true, false, true,
                                REM_HEBDO, null));

        assertThat(r.echelonCct109())
                .isEqualTo(LicenciementBeCct109EchelonEnum.DOUZE_SEMAINES);
        assertThat(r.nombreSemaines()).isEqualTo(12);
        assertThat(r.indemniteCct109())
                .isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(r.justificationEchelon())
                .contains("Discrimination")
                .contains("haute");
    }

    @Test
    void analyze_represaillesSeule_retourne12semaines() {
        LicenciementBeCct109DeraisonnableResult r =
                LicenciementBeCct109DeraisonnableCalculator.analyze(
                        req(true,
                                LicenciementBeCct109MotifLieAPersonneEnum.MOTIF_PRECIS_DISPUTE,
                                false, /* represailles */ true, true,
                                REM_HEBDO, null));

        assertThat(r.echelonCct109())
                .isEqualTo(LicenciementBeCct109EchelonEnum.DOUZE_SEMAINES);
        assertThat(r.nombreSemaines()).isEqualTo(12);
        assertThat(r.justificationEchelon()).contains("Représailles");
    }

    // ── Cas 6 : discrimination + représailles → 17 semaines ────────────────

    @Test
    void analyze_discriminationEtRepresailles_retourne17semaines_maximumLegal() {
        LicenciementBeCct109DeraisonnableResult r =
                LicenciementBeCct109DeraisonnableCalculator.analyze(
                        req(true,
                                LicenciementBeCct109MotifLieAPersonneEnum.MOTIF_PRECIS_DISPUTE,
                                /* discrimination */ true,
                                /* represailles */ true, true,
                                REM_HEBDO, null));

        assertThat(r.echelonCct109())
                .isEqualTo(LicenciementBeCct109EchelonEnum.DIX_SEPT_SEMAINES);
        assertThat(r.nombreSemaines()).isEqualTo(17);
        assertThat(r.indemniteCct109())
                .isEqualByComparingTo(new BigDecimal("8500.00"));
        assertThat(r.justificationEchelon())
                .contains("Discrimination ET représailles")
                .contains("maximum");
    }

    // ── Cas 7 : indemnité 500 × 8 = 4 000 € ────────────────────────────────

    @Test
    void analyze_calculIndemnite_500EuroPar8Semaines_egale4000() {
        LicenciementBeCct109DeraisonnableResult r =
                LicenciementBeCct109DeraisonnableCalculator.analyze(
                        req(true,
                                LicenciementBeCct109MotifLieAPersonneEnum.MOTIF_PRECIS_DISPUTE,
                                false, false, false,
                                new BigDecimal("500.00"), null));

        assertThat(r.nombreSemaines()).isEqualTo(8);
        assertThat(r.indemniteCct109())
                .isEqualByComparingTo(new BigDecimal("4000.00"));
    }

    @Test
    void analyze_calculIndemnite_avecBigDecimalsExacts_pasDePerteDePrecision() {
        BigDecimal hebdo = new BigDecimal("847.53");
        LicenciementBeCct109DeraisonnableResult r =
                LicenciementBeCct109DeraisonnableCalculator.analyze(
                        req(false,
                                LicenciementBeCct109MotifLieAPersonneEnum.SANS_MOTIF,
                                false, false, true, hebdo, null));

        // 847.53 × 3 = 2542.59
        assertThat(r.indemniteCct109())
                .isEqualByComparingTo(new BigDecimal("2542.59"));
    }

    // ── Cas 8 : avertissement cumul ICP toujours non null ──────────────────

    @Test
    void analyze_avertissementCumulIcp_toujoursNonNull_caseNonDeraisonnable() {
        LicenciementBeCct109DeraisonnableResult r =
                LicenciementBeCct109DeraisonnableCalculator.analyze(
                        req(true,
                                LicenciementBeCct109MotifLieAPersonneEnum.MOTIF_PROUVE_VALIDE,
                                false, false, true, REM_HEBDO, null));

        assertThat(r.avertissement())
                .isNotNull()
                .contains("ICP")
                .contains("cumule");
        assertThat(r.cumulAvecIcp()).isTrue();
    }

    @Test
    void analyze_avertissementCumulIcp_toujoursNonNull_caseMaximum17() {
        LicenciementBeCct109DeraisonnableResult r =
                LicenciementBeCct109DeraisonnableCalculator.analyze(
                        req(true,
                                LicenciementBeCct109MotifLieAPersonneEnum.MOTIF_PRECIS_DISPUTE,
                                true, true, true, REM_HEBDO, null));

        assertThat(r.avertissement())
                .isNotNull()
                .contains("ICP")
                .contains("Préavis");
        assertThat(r.cumulAvecIcp()).isTrue();
    }

    // ── Base juridique ─────────────────────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceCCT109_2014_et_arretCE_2019() {
        LicenciementBeCct109DeraisonnableResult r =
                LicenciementBeCct109DeraisonnableCalculator.analyze(
                        req(false,
                                LicenciementBeCct109MotifLieAPersonneEnum.SANS_MOTIF,
                                false, false, true, REM_HEBDO, null));

        assertThat(r.baseJuridique())
                .contains("CCT n° 109")
                .contains("12/02/2014")
                .contains("art. 9")
                .contains("245.236")
                .contains("27/06/2019");
    }

    // ── Priorité descendante : discrimination > motif valable disputé ──────

    @Test
    void analyze_prioriteDescendante_motifValableMaisDiscrimination_retourne12() {
        // Si discrimination, on bascule à 12 même si le motif est dit valable
        // mais que les procédures NE sont PAS respectées (donc échappe à
        // NON_DERAISONNABLE)
        LicenciementBeCct109DeraisonnableResult r =
                LicenciementBeCct109DeraisonnableCalculator.analyze(
                        req(true,
                                LicenciementBeCct109MotifLieAPersonneEnum.MOTIF_PROUVE_VALIDE,
                                true, false, false, REM_HEBDO, null));

        assertThat(r.echelonCct109())
                .isEqualTo(LicenciementBeCct109EchelonEnum.DOUZE_SEMAINES);
    }

    // ── Snapshot inputs ────────────────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        LicenciementBeCct109DeraisonnableResult r =
                LicenciementBeCct109DeraisonnableCalculator.analyze(
                        req(true,
                                LicenciementBeCct109MotifLieAPersonneEnum.MOTIF_PRECIS_DISPUTE,
                                true, true, false, new BigDecimal("750.00"),
                                "Réorganisation invoquée"));

        assertThat(r.motifCommunique()).isTrue();
        assertThat(r.motifLieAPersonne())
                .isEqualTo(LicenciementBeCct109MotifLieAPersonneEnum.MOTIF_PRECIS_DISPUTE);
        assertThat(r.discriminationSuspectee()).isTrue();
        assertThat(r.represaillesSuspectees()).isTrue();
        assertThat(r.proceduresRespectees()).isFalse();
        assertThat(r.remunerationHebdomadaireBrute())
                .isEqualByComparingTo(new BigDecimal("750.00"));
        assertThat(r.argumentsPatronal()).isEqualTo("Réorganisation invoquée");
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() ->
                LicenciementBeCct109DeraisonnableCalculator.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_motifCommuniqueNull_throws() {
        LicenciementBeCct109DeraisonnableRequest bad =
                new LicenciementBeCct109DeraisonnableRequest(
                        null,
                        LicenciementBeCct109MotifLieAPersonneEnum.SANS_MOTIF,
                        false, false, true, REM_HEBDO, null);
        assertThatThrownBy(() ->
                LicenciementBeCct109DeraisonnableCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifCommunique");
    }

    @Test
    void analyze_motifLieAPersonneNull_throws() {
        LicenciementBeCct109DeraisonnableRequest bad =
                new LicenciementBeCct109DeraisonnableRequest(
                        true, null, false, false, true, REM_HEBDO, null);
        assertThatThrownBy(() ->
                LicenciementBeCct109DeraisonnableCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifLieAPersonne");
    }

    @Test
    void analyze_remunerationNull_throws() {
        LicenciementBeCct109DeraisonnableRequest bad =
                new LicenciementBeCct109DeraisonnableRequest(
                        true,
                        LicenciementBeCct109MotifLieAPersonneEnum.SANS_MOTIF,
                        false, false, true, null, null);
        assertThatThrownBy(() ->
                LicenciementBeCct109DeraisonnableCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remunerationHebdomadaireBrute");
    }

    @Test
    void analyze_remunerationZero_throws() {
        assertThatThrownBy(() ->
                LicenciementBeCct109DeraisonnableCalculator.analyze(
                        req(true,
                                LicenciementBeCct109MotifLieAPersonneEnum.SANS_MOTIF,
                                false, false, true, BigDecimal.ZERO, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictement positive");
    }

    @Test
    void analyze_remunerationNegative_throws() {
        assertThatThrownBy(() ->
                LicenciementBeCct109DeraisonnableCalculator.analyze(
                        req(true,
                                LicenciementBeCct109MotifLieAPersonneEnum.SANS_MOTIF,
                                false, false, true,
                                new BigDecimal("-100"), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictement positive");
    }
}
