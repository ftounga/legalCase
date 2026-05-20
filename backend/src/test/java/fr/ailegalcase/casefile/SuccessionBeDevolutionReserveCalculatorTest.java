package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-217-11 : tests unitaires de {@link SuccessionBeDevolutionReserveCalculator}.
 *
 * <p>Couvre les CA du mini-spec : réserve fixe à 1/2 quelle que soit le nombre
 * d'enfants (post-réforme 2017), conjoint usufruit / descendants nue-propriété,
 * dévolution sans réserve, dépassement quotité disponible, qualification
 * incomplète (marié sans régime), représentation, déshérence, validations.</p>
 */
class SuccessionBeDevolutionReserveCalculatorTest {

    private static final LocalDate DATE_DECES = LocalDate.of(2025, 3, 12);

    private static SuccessionBeDevolutionReserveInput input(
            SuccessionBeDevolutionReserveCalculator.EtatCivilDefuntBe etatCivil,
            SuccessionBeDevolutionReserveCalculator.RegimeMatrimonialDefuntBe regime,
            int enfantsVivants,
            int enfantsPredecedes,
            boolean parents,
            boolean freresSoeurs,
            BigDecimal masse,
            BigDecimal liberalites) {
        return new SuccessionBeDevolutionReserveInput(
                DATE_DECES,
                etatCivil,
                regime,
                enfantsVivants,
                enfantsPredecedes,
                parents,
                freresSoeurs,
                masse,
                liberalites,
                null
        );
    }

    @Test
    void mariE_2_enfants_masse_400000_libE0_conjointUsufruit_descendantsNuePro() {
        SuccessionBeDevolutionReserveInput in = input(
                SuccessionBeDevolutionReserveCalculator.EtatCivilDefuntBe.MARIE,
                SuccessionBeDevolutionReserveCalculator.RegimeMatrimonialDefuntBe.COMMUNAUTE_LEGALE,
                2, 0, false, false,
                new BigDecimal("400000"), new BigDecimal("0"));

        SuccessionBeDevolutionReserveResult r = SuccessionBeDevolutionReserveCalculator.compute(in, "BELGIQUE");

        assertThat(r.verdict()).isEqualTo(SuccessionBeDevolutionReserveCalculator.Verdict.RESERVE_RESPECTEE);
        assertThat(r.reserveGlobaleEur()).isEqualByComparingTo("200000");
        assertThat(r.quotiteDisponibleEur()).isEqualByComparingTo("200000");
        assertThat(r.depassementQuotiteEur()).isEqualByComparingTo("0");
        // 1er héritier = conjoint survivant en usufruit
        assertThat(r.heritiers()).isNotEmpty();
        assertThat(r.heritiers().get(0).code())
                .isEqualTo(SuccessionBeDevolutionReserveCalculator.HeritierCodeBe.CONJOINT_SURVIVANT);
        assertThat(r.heritiers().get(0).quotePartGlobaleFraction()).isEqualTo("USUFRUIT_TOTAL");
        // Enfants en nue-propriété, chacun 100 000 € de réserve / 200 000 € de quote-part globale
        assertThat(r.heritiers()).filteredOn(h -> h.code() ==
                        SuccessionBeDevolutionReserveCalculator.HeritierCodeBe.ENFANT)
                .hasSize(2)
                .allSatisfy(h -> {
                    assertThat(h.libelle()).contains("nue-propriété");
                    assertThat(h.quotePartReserveEur()).isEqualByComparingTo("100000");
                    assertThat(h.quotePartGlobaleEur()).isEqualByComparingTo("200000");
                });
    }

    @Test
    void mariE_6_enfants_reserveResteFixeA_1_2() {
        // Point central de la réforme 2017 : réserve = 1/2 même avec 6 enfants
        // (différence majeure avec FR qui passerait à 3/4).
        SuccessionBeDevolutionReserveInput in = input(
                SuccessionBeDevolutionReserveCalculator.EtatCivilDefuntBe.VEUF,
                null,
                6, 0, false, false,
                new BigDecimal("600000"), new BigDecimal("0"));

        SuccessionBeDevolutionReserveResult r = SuccessionBeDevolutionReserveCalculator.compute(in, "BELGIQUE");

        assertThat(r.reserveGlobaleEur()).isEqualByComparingTo("300000");
        assertThat(r.reserveGlobaleFraction()).isEqualTo("1/2");
        assertThat(r.quotiteDisponibleFraction()).isEqualTo("1/2");
        assertThat(r.quotiteDisponibleEur()).isEqualByComparingTo("300000");
        assertThat(r.heritiers()).filteredOn(h -> h.code() ==
                        SuccessionBeDevolutionReserveCalculator.HeritierCodeBe.ENFANT)
                .hasSize(6);
    }

    @Test
    void veuf_1_enfant_liberalites60000_depassementQuotite() {
        // Masse 100 000, libéralités 60 000 → quotité disponible 50 000 → dépassement 10 000
        SuccessionBeDevolutionReserveInput in = input(
                SuccessionBeDevolutionReserveCalculator.EtatCivilDefuntBe.VEUF,
                null,
                1, 0, false, false,
                new BigDecimal("100000"), new BigDecimal("60000"));

        SuccessionBeDevolutionReserveResult r = SuccessionBeDevolutionReserveCalculator.compute(in, "BELGIQUE");

        assertThat(r.verdict()).isEqualTo(SuccessionBeDevolutionReserveCalculator.Verdict.QUOTITE_DEPASSEE);
        assertThat(r.reserveGlobaleEur()).isEqualByComparingTo("50000");
        assertThat(r.quotiteDisponibleEur()).isEqualByComparingTo("50000");
        assertThat(r.depassementQuotiteEur()).isEqualByComparingTo("10000");
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("920"));
    }

    @Test
    void celibataire_parentsVivants_devolutionSansReserve() {
        SuccessionBeDevolutionReserveInput in = input(
                SuccessionBeDevolutionReserveCalculator.EtatCivilDefuntBe.CELIBATAIRE,
                null,
                0, 0, true, false,
                new BigDecimal("100000"), new BigDecimal("80000"));

        SuccessionBeDevolutionReserveResult r = SuccessionBeDevolutionReserveCalculator.compute(in, "BELGIQUE");

        assertThat(r.verdict()).isEqualTo(SuccessionBeDevolutionReserveCalculator.Verdict.DEVOLUTION_ETABLIE);
        assertThat(r.reserveGlobaleEur()).isEqualByComparingTo("0");
        assertThat(r.reserveGlobaleFraction()).isEqualTo("0");
        assertThat(r.quotiteDisponibleEur()).isEqualByComparingTo("100000");
        assertThat(r.depassementQuotiteEur()).isEqualByComparingTo("0");
        assertThat(r.heritiers()).anyMatch(h -> h.code() ==
                SuccessionBeDevolutionReserveCalculator.HeritierCodeBe.PARENT);
    }

    @Test
    void cohabitantLegal_1_enfant_descendantsReserveCohabitantDroitsLimites() {
        SuccessionBeDevolutionReserveInput in = input(
                SuccessionBeDevolutionReserveCalculator.EtatCivilDefuntBe.COHABITANT_LEGAL,
                null,
                1, 0, false, false,
                new BigDecimal("200000"), new BigDecimal("0"));

        SuccessionBeDevolutionReserveResult r = SuccessionBeDevolutionReserveCalculator.compute(in, "BELGIQUE");

        assertThat(r.verdict()).isEqualTo(SuccessionBeDevolutionReserveCalculator.Verdict.RESERVE_RESPECTEE);
        assertThat(r.reserveGlobaleEur()).isEqualByComparingTo("100000");
        // Cohabitant légal présent — droits limités
        assertThat(r.heritiers()).anyMatch(h -> h.code() ==
                SuccessionBeDevolutionReserveCalculator.HeritierCodeBe.COHABITANT_LEGAL_SURVIVANT);
        assertThat(r.heritiers())
                .filteredOn(h -> h.code() == SuccessionBeDevolutionReserveCalculator.HeritierCodeBe.COHABITANT_LEGAL_SURVIVANT)
                .first()
                .satisfies(h -> {
                    assertThat(h.quotePartGlobaleFraction()).isEqualTo("USUFRUIT_LOGEMENT_FAMILIAL");
                    assertThat(h.fondement()).contains("1477");
                });
    }

    @Test
    void representation_1_enfantPredecedeAvec2Descendants_2Souches() {
        // 1 enfant vivant + 1 enfant prédécédé laissant des descendants = 2 souches
        SuccessionBeDevolutionReserveInput in = input(
                SuccessionBeDevolutionReserveCalculator.EtatCivilDefuntBe.VEUF,
                null,
                1, 1, false, false,
                new BigDecimal("400000"), new BigDecimal("0"));

        SuccessionBeDevolutionReserveResult r = SuccessionBeDevolutionReserveCalculator.compute(in, "BELGIQUE");

        assertThat(r.verdict()).isEqualTo(SuccessionBeDevolutionReserveCalculator.Verdict.RESERVE_RESPECTEE);
        // Réserve 200 000 / 2 souches = 100 000 par souche
        assertThat(r.heritiers()).filteredOn(h -> h.code() ==
                        SuccessionBeDevolutionReserveCalculator.HeritierCodeBe.ENFANT
                || h.code() == SuccessionBeDevolutionReserveCalculator.HeritierCodeBe.DESCENDANT_REPRESENTATION)
                .hasSize(2)
                .allSatisfy(h -> assertThat(h.quotePartReserveEur()).isEqualByComparingTo("100000"));
        assertThat(r.heritiers()).anyMatch(h -> h.code() ==
                SuccessionBeDevolutionReserveCalculator.HeritierCodeBe.DESCENDANT_REPRESENTATION);
    }

    @Test
    void marie_sansRegimeMatrimonial_qualificationIncomplete() {
        SuccessionBeDevolutionReserveInput in = input(
                SuccessionBeDevolutionReserveCalculator.EtatCivilDefuntBe.MARIE,
                null,
                2, 0, false, false,
                new BigDecimal("400000"), new BigDecimal("0"));

        SuccessionBeDevolutionReserveResult r = SuccessionBeDevolutionReserveCalculator.compute(in, "BELGIQUE");

        assertThat(r.verdict())
                .isEqualTo(SuccessionBeDevolutionReserveCalculator.Verdict.QUALIFICATION_INCOMPLETE);
        assertThat(r.heritiers()).isEmpty();
        assertThat(r.messages()).anyMatch(m -> m.contains("régime matrimonial"));
    }

    @Test
    void aucunHeritier_deshErence_etatHerite() {
        SuccessionBeDevolutionReserveInput in = input(
                SuccessionBeDevolutionReserveCalculator.EtatCivilDefuntBe.CELIBATAIRE,
                null,
                0, 0, false, false,
                new BigDecimal("50000"), new BigDecimal("0"));

        SuccessionBeDevolutionReserveResult r = SuccessionBeDevolutionReserveCalculator.compute(in, "BELGIQUE");

        assertThat(r.verdict()).isEqualTo(SuccessionBeDevolutionReserveCalculator.Verdict.DEVOLUTION_ETABLIE);
        assertThat(r.heritiers()).hasSize(1);
        assertThat(r.heritiers().get(0).code())
                .isEqualTo(SuccessionBeDevolutionReserveCalculator.HeritierCodeBe.ETAT);
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("déshérence"));
    }

    @Test
    void dateDecesAnterieureAReforme_messageDavertissement() {
        SuccessionBeDevolutionReserveInput in = new SuccessionBeDevolutionReserveInput(
                LocalDate.of(2018, 8, 1), // 1 mois avant l'entrée en vigueur
                SuccessionBeDevolutionReserveCalculator.EtatCivilDefuntBe.VEUF,
                null, 1, 0, false, false,
                new BigDecimal("100000"), new BigDecimal("0"), null);

        SuccessionBeDevolutionReserveResult r = SuccessionBeDevolutionReserveCalculator.compute(in, "BELGIQUE");

        assertThat(r.messages()).anyMatch(m -> m.contains("01/09/2018"));
    }

    @Test
    void paysFrance_rejette() {
        SuccessionBeDevolutionReserveInput in = input(
                SuccessionBeDevolutionReserveCalculator.EtatCivilDefuntBe.VEUF,
                null, 1, 0, false, false,
                new BigDecimal("100000"), new BigDecimal("0"));

        assertThatThrownBy(() -> SuccessionBeDevolutionReserveCalculator.compute(in, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BELGIQUE");
    }

    @Test
    void dateDecesFuture_rejette() {
        SuccessionBeDevolutionReserveInput in = new SuccessionBeDevolutionReserveInput(
                LocalDate.now().plusDays(1),
                SuccessionBeDevolutionReserveCalculator.EtatCivilDefuntBe.VEUF,
                null, 1, 0, false, false,
                new BigDecimal("100000"), new BigDecimal("0"), null);

        assertThatThrownBy(() -> SuccessionBeDevolutionReserveCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void dateDecesAbsente_rejette() {
        SuccessionBeDevolutionReserveInput in = new SuccessionBeDevolutionReserveInput(
                null,
                SuccessionBeDevolutionReserveCalculator.EtatCivilDefuntBe.VEUF,
                null, 1, 0, false, false,
                new BigDecimal("100000"), new BigDecimal("0"), null);

        assertThatThrownBy(() -> SuccessionBeDevolutionReserveCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDeces");
    }

    @Test
    void nombreEnfantsNegatif_rejette() {
        SuccessionBeDevolutionReserveInput in = input(
                SuccessionBeDevolutionReserveCalculator.EtatCivilDefuntBe.VEUF,
                null, -1, 0, false, false,
                new BigDecimal("100000"), new BigDecimal("0"));

        assertThatThrownBy(() -> SuccessionBeDevolutionReserveCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombreEnfantsVivants");
    }

    @Test
    void nombreEnfantsAuDelaDuMaximum_rejette() {
        SuccessionBeDevolutionReserveInput in = input(
                SuccessionBeDevolutionReserveCalculator.EtatCivilDefuntBe.VEUF,
                null, 21, 0, false, false,
                new BigDecimal("100000"), new BigDecimal("0"));

        assertThatThrownBy(() -> SuccessionBeDevolutionReserveCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("20");
    }

    @Test
    void masseNegative_rejette() {
        SuccessionBeDevolutionReserveInput in = input(
                SuccessionBeDevolutionReserveCalculator.EtatCivilDefuntBe.VEUF,
                null, 1, 0, false, false,
                new BigDecimal("-1"), new BigDecimal("0"));

        assertThatThrownBy(() -> SuccessionBeDevolutionReserveCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("masseSuccessoraleEur");
    }

    @Test
    void liberalitesNegatives_rejette() {
        SuccessionBeDevolutionReserveInput in = input(
                SuccessionBeDevolutionReserveCalculator.EtatCivilDefuntBe.VEUF,
                null, 1, 0, false, false,
                new BigDecimal("100000"), new BigDecimal("-1"));

        assertThatThrownBy(() -> SuccessionBeDevolutionReserveCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("libertesConsentiesEur");
    }

    @Test
    void commentaireTropLong_rejette() {
        SuccessionBeDevolutionReserveInput in = new SuccessionBeDevolutionReserveInput(
                DATE_DECES,
                SuccessionBeDevolutionReserveCalculator.EtatCivilDefuntBe.VEUF,
                null, 1, 0, false, false,
                new BigDecimal("100000"), new BigDecimal("0"),
                "x".repeat(1001));

        assertThatThrownBy(() -> SuccessionBeDevolutionReserveCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1000");
    }

    @Test
    void etatCivilManquant_rejette() {
        SuccessionBeDevolutionReserveInput in = new SuccessionBeDevolutionReserveInput(
                DATE_DECES, null, null, 1, 0, false, false,
                new BigDecimal("100000"), new BigDecimal("0"), null);

        assertThatThrownBy(() -> SuccessionBeDevolutionReserveCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("etatCivilDefunt");
    }
}
