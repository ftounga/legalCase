package fr.ailegalcase.analysis;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-153 SF-153-01 : tests de {@link JafReferenceTable} + intégration dans les
 * calculateurs existants.
 */
class JafReferenceTableTest {

    @Test
    void U01_pensionAlim_rangeCoherent_p25_p50_p75() {
        JurisprudenceRange r = JafReferenceTable.pensionAlimentaireRange(300.0, "FRANCE");
        assertThat(r).isNotNull();
        assertThat(r.p25()).isLessThan(r.p50());
        assertThat(r.p50()).isLessThan(r.p75());
        assertThat(r.p50()).isEqualTo(300);
        assertThat(r.label()).contains("JAF");
    }

    @Test
    void U02_prestationComp_rangeLarge_dispersion50pourcent() {
        JurisprudenceRange r = JafReferenceTable.prestationCompensatoireRange(50_000.0, "FRANCE");
        assertThat(r).isNotNull();
        assertThat(r.p25()).isEqualTo(25_000); // 50 %
        assertThat(r.p50()).isEqualTo(50_000);
        assertThat(r.p75()).isEqualTo(75_000); // 150 %
    }

    @Test
    void U03_paysBE_labelDifferent() {
        JurisprudenceRange r = JafReferenceTable.pensionAlimentaireRange(300.0, "BELGIQUE");
        assertThat(r.label()).contains("Belgique");
        assertThat(r.sourceRef()).contains("CGKR");
    }

    @Test
    void U04_montantNulOuNegatif_rangeNull() {
        assertThat(JafReferenceTable.pensionAlimentaireRange(0.0, "FRANCE")).isNull();
        assertThat(JafReferenceTable.pensionAlimentaireRange(-50.0, "FRANCE")).isNull();
        assertThat(JafReferenceTable.prestationCompensatoireRange(0.0, "FRANCE")).isNull();
    }

    @Test
    void U05_pensionAlimentaireCalculator_enrichitRange_quandCompletes() {
        Optional<PensionAlimentaireCalculator.PensionAlimentaireEstimate> e =
                PensionAlimentaireCalculator.calculate(3000.0, 2, "EXCLUSIVE", "FRANCE");
        assertThat(e).isPresent();
        assertThat(e.get().jurisprudenceRange()).isNotNull();
        assertThat(e.get().jurisprudenceRange().p50()).isGreaterThan(0);
    }

    @Test
    void U06_pensionAlim_donneesPartielles_rangeNull() {
        Optional<PensionAlimentaireCalculator.PensionAlimentaireEstimate> e =
                PensionAlimentaireCalculator.calculate(null, 2, "EXCLUSIVE", "FRANCE");
        assertThat(e).isPresent();
        assertThat(e.get().donneesPartielles()).isTrue();
        assertThat(e.get().jurisprudenceRange()).isNull();
    }

    @Test
    void U07_prestationComp_enrichitRange_quandCompletes() {
        Optional<PrestationCompensatoireCalculator.PrestationCompensatoireEstimate> e =
                PrestationCompensatoireCalculator.calculate(4000.0, 2000.0, 15, "FRANCE");
        assertThat(e).isPresent();
        assertThat(e.get().jurisprudenceRange()).isNotNull();
        assertThat(e.get().jurisprudenceRange().p50()).isGreaterThan(0);
    }
}
