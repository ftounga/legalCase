package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.NonConcurrenceCalculator.SecteurActivite;
import fr.ailegalcase.casefile.NonConcurrenceCalculator.VerdictValidite;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NonConcurrenceCalculatorTest {

    private static final BigDecimal SALAIRE = new BigDecimal("5000.00");
    private static final LocalDate PRISE_EFFET = LocalDate.of(2026, 4, 15);

    private NonConcurrenceInput.Builder baseBuilder() {
        return new NonConcurrenceInput.Builder()
                .salaireMensuelBrutEur(SALAIRE)
                .secteurActivite(SecteurActivite.INFORMATIQUE)
                .datePriseEffet(PRISE_EFFET);
    }

    private NonConcurrenceInput.Builder builderTousCriteresOk() {
        return baseBuilder()
                .clausePresenteContrat(true)
                .limiteTerritoireDefini(true).territoireDescription("France métropolitaine")
                .limiteDureeDefinie(true).dureeMois(24)
                .limiteObjetDefini(true).objetDescription("Édition logiciels SaaS B2B")
                .contrepartieFinancierePresente(true)
                .contrepartieMontantMensuelEur(new BigDecimal("1500.00"));
    }

    // --- Cas nominal VALIDE ---

    @Test
    void compute_quatreCriteresOkRatio30_score100Valide() {
        var r = NonConcurrenceCalculator.compute(builderTousCriteresOk().build(), "FRANCE");
        assertThat(r.scoreValidite()).isEqualTo(100);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.VALIDE);
        assertThat(r.critere1TerritoireOk()).isTrue();
        assertThat(r.critere2DureeOk()).isTrue();
        assertThat(r.critere3ObjetOk()).isTrue();
        assertThat(r.critere4ContrepartieOk()).isTrue();
        assertThat(r.ratioContrepartiePct()).isEqualByComparingTo("30.0");
        // 1500 × 24 = 36000
        assertThat(r.indemniteContrepartieDueEur()).isEqualByComparingTo("36000.00");
        assertThat(r.indemnitePotentielleNulliteEur()).isEqualByComparingTo("0.00");
        assertThat(r.country()).isEqualTo("FRANCE");
        assertThat(r.baseJuridique()).contains("Cass. soc. 10/07/2002").contains("L.1221-1");
    }

    // --- 3 critères OK : un fail à la fois ---

    @Test
    void compute_troisCriteres_territoireKO_risqueNullitePartielle() {
        var input = builderTousCriteresOk()
                .limiteTerritoireDefini(false)
                .territoireDescription(null)
                .build();
        var r = NonConcurrenceCalculator.compute(input, "FRANCE");
        assertThat(r.scoreValidite()).isEqualTo(75);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.RISQUE_NULLITE_PARTIELLE);
        assertThat(r.critere1TerritoireOk()).isFalse();
        assertThat(r.indemniteContrepartieDueEur()).isEqualByComparingTo("0.00");
        // Plancher 1 mois × 5000 = 5000
        assertThat(r.indemnitePotentielleNulliteEur()).isEqualByComparingTo("5000.00");
    }

    @Test
    void compute_quatreCriteresOkMaisRatio20_risqueNullitePartielle() {
        var input = builderTousCriteresOk()
                .contrepartieMontantMensuelEur(new BigDecimal("1000.00")) // 20 % de 5000
                .build();
        var r = NonConcurrenceCalculator.compute(input, "FRANCE");
        // 3/4 critères respectés (critère 4 contrepartie KO car ratio 20 < 25)
        assertThat(r.critere4ContrepartieOk()).isFalse();
        assertThat(r.scoreValidite()).isEqualTo(75);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.RISQUE_NULLITE_PARTIELLE);
        assertThat(r.ratioContrepartiePct()).isEqualByComparingTo("20.0");
    }

    // --- 2 critères → NULLE ---

    @Test
    void compute_deuxCriteresOk_nulle() {
        var input = builderTousCriteresOk()
                .limiteTerritoireDefini(false).territoireDescription(null)
                .limiteDureeDefinie(false).dureeMois(null)
                .build();
        var r = NonConcurrenceCalculator.compute(input, "FRANCE");
        assertThat(r.scoreValidite()).isEqualTo(50);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NULLE);
        assertThat(r.indemniteContrepartieDueEur()).isEqualByComparingTo("0.00");
        assertThat(r.indemnitePotentielleNulliteEur()).isEqualByComparingTo("5000.00");
    }

    // --- Clause absente ---

    @Test
    void compute_clauseAbsente_scoreZeroNulle_pasIndemnite() {
        var input = baseBuilder().clausePresenteContrat(false).build();
        var r = NonConcurrenceCalculator.compute(input, "FRANCE");
        assertThat(r.scoreValidite()).isZero();
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NULLE);
        assertThat(r.critere1TerritoireOk()).isFalse();
        assertThat(r.critere2DureeOk()).isFalse();
        assertThat(r.critere3ObjetOk()).isFalse();
        assertThat(r.critere4ContrepartieOk()).isFalse();
        assertThat(r.indemniteContrepartieDueEur()).isEqualByComparingTo("0.00");
        assertThat(r.indemnitePotentielleNulliteEur()).isEqualByComparingTo("0.00");
    }

    // --- Détection territoires invalides ---

    @Test
    void compute_territoireMondeEntier_critere1KO() {
        var input = builderTousCriteresOk()
                .territoireDescription("monde entier")
                .build();
        var r = NonConcurrenceCalculator.compute(input, "FRANCE");
        assertThat(r.critere1TerritoireOk()).isFalse();
        assertThat(r.scoreValidite()).isEqualTo(75);
    }

    @Test
    void compute_territoireToutPays_critere1KO() {
        var input = builderTousCriteresOk()
                .territoireDescription("Tout pays de l'Union européenne")
                .build();
        var r = NonConcurrenceCalculator.compute(input, "FRANCE");
        assertThat(r.critere1TerritoireOk()).isFalse();
    }

    @Test
    void compute_objetTouteActivite_critere3KO() {
        var input = builderTousCriteresOk()
                .objetDescription("Toute activité concurrentielle")
                .build();
        var r = NonConcurrenceCalculator.compute(input, "FRANCE");
        assertThat(r.critere3ObjetOk()).isFalse();
        assertThat(r.scoreValidite()).isEqualTo(75);
    }

    // --- Critère 2 : durée ---

    @Test
    void compute_duree48Mois_critere2KO() {
        var input = builderTousCriteresOk().dureeMois(48).build();
        var r = NonConcurrenceCalculator.compute(input, "FRANCE");
        assertThat(r.critere2DureeOk()).isFalse();
        assertThat(r.scoreValidite()).isEqualTo(75);
        assertThat(r.messages()).anyMatch(m -> m.contains("36 mois"));
    }

    @Test
    void compute_dureeZero_critere2KO() {
        var input = builderTousCriteresOk().dureeMois(0).limiteDureeDefinie(true).build();
        var r = NonConcurrenceCalculator.compute(input, "FRANCE");
        assertThat(r.critere2DureeOk()).isFalse();
    }

    // --- Critère 4 : contrepartie ---

    @Test
    void compute_contrepartieZero_critere4KO_ratioZero() {
        var input = builderTousCriteresOk()
                .contrepartieMontantMensuelEur(BigDecimal.ZERO)
                .build();
        var r = NonConcurrenceCalculator.compute(input, "FRANCE");
        assertThat(r.critere4ContrepartieOk()).isFalse();
        assertThat(r.ratioContrepartiePct()).isEqualByComparingTo("0.0");
    }

    @Test
    void compute_ratioBorderline25Pct_valide() {
        var input = builderTousCriteresOk()
                .contrepartieMontantMensuelEur(new BigDecimal("1250.00")) // 25 % pile de 5000
                .build();
        var r = NonConcurrenceCalculator.compute(input, "FRANCE");
        assertThat(r.critere4ContrepartieOk()).isTrue();
        assertThat(r.scoreValidite()).isEqualTo(100);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.VALIDE);
        assertThat(r.ratioContrepartiePct()).isEqualByComparingTo("25.0");
    }

    // --- Validation arguments ---

    @Test
    void compute_salaireNull_lance() {
        var input = baseBuilder().salaireMensuelBrutEur(null).build();
        assertThatThrownBy(() -> NonConcurrenceCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Salaire");
    }

    @Test
    void compute_salaireZero_lance() {
        var input = baseBuilder().salaireMensuelBrutEur(BigDecimal.ZERO).build();
        assertThatThrownBy(() -> NonConcurrenceCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_paysBelgique_lance() {
        var input = builderTousCriteresOk().build();
        assertThatThrownBy(() -> NonConcurrenceCalculator.compute(input, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void compute_dureeNegative_lance() {
        var input = builderTousCriteresOk().dureeMois(-3).build();
        assertThatThrownBy(() -> NonConcurrenceCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_contrepartieNegative_lance() {
        var input = builderTousCriteresOk()
                .contrepartieMontantMensuelEur(new BigDecimal("-100"))
                .build();
        assertThatThrownBy(() -> NonConcurrenceCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
