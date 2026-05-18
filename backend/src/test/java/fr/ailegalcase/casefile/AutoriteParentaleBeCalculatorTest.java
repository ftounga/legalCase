package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-217-04 : tests unitaires de {@link AutoriteParentaleBeCalculator}.
 */
class AutoriteParentaleBeCalculatorTest {

    /** Construit un input avec tous les booléens à false par défaut. */
    private AutoriteParentaleBeInput input(boolean filiation,
                                           boolean accord,
                                           boolean demandeExclusive,
                                           boolean desinteret,
                                           boolean miseEnDanger,
                                           boolean incapacite,
                                           boolean decisionAnterieure) {
        return new AutoriteParentaleBeInput(
                filiation, accord, demandeExclusive,
                desinteret, miseEnDanger, incapacite, decisionAnterieure,
                AutoriteParentaleBeCalculator.ModeHebergement.HEBERGEMENT_PRINCIPAL_UN_PARENT,
                null);
    }

    @Test
    void compute_pasDeDemandeExclusive_sansAccord_autoriteConjointe() {
        AutoriteParentaleBeResult r = AutoriteParentaleBeCalculator.compute(
                input(true, false, false, false, false, false, false), "BELGIQUE");

        assertThat(r.verdict()).isEqualTo(AutoriteParentaleBeCalculator.Verdict.AUTORITE_CONJOINTE);
        assertThat(r.voieProcedurale())
                .isEqualTo(AutoriteParentaleBeCalculator.VoieProcedurale.AUCUNE_AUTORITE_CONJOINTE_DROIT);
        assertThat(r.country()).isEqualTo("BELGIQUE");
    }

    @Test
    void compute_pasDeDemandeExclusive_avecAccord_voieAccordHomologue() {
        AutoriteParentaleBeResult r = AutoriteParentaleBeCalculator.compute(
                input(true, true, false, false, false, false, false), "BELGIQUE");

        assertThat(r.verdict()).isEqualTo(AutoriteParentaleBeCalculator.Verdict.AUTORITE_CONJOINTE);
        assertThat(r.voieProcedurale())
                .isEqualTo(AutoriteParentaleBeCalculator.VoieProcedurale.ACCORD_HOMOLOGUE_TF);
        assertThat(r.facteurs())
                .anyMatch(f -> f.code() == AutoriteParentaleBeCalculator.FacteurCode.ACCORD_PARENTAL);
    }

    @Test
    void compute_demandeExclusive_avecMiseEnDanger_exclusiveFondee() {
        AutoriteParentaleBeResult r = AutoriteParentaleBeCalculator.compute(
                input(true, false, true, false, true, false, false), "BELGIQUE");

        assertThat(r.verdict())
                .isEqualTo(AutoriteParentaleBeCalculator.Verdict.AUTORITE_EXCLUSIVE_FONDEE);
        assertThat(r.voieProcedurale())
                .isEqualTo(AutoriteParentaleBeCalculator.VoieProcedurale.REQUETE_TRIBUNAL_FAMILLE);
        assertThat(r.facteurs())
                .anyMatch(f -> f.code() == AutoriteParentaleBeCalculator.FacteurCode.MISE_EN_DANGER
                        && f.favorableExclusive());
    }

    @Test
    void compute_demandeExclusive_avecDesinteret_exclusiveFondee() {
        AutoriteParentaleBeResult r = AutoriteParentaleBeCalculator.compute(
                input(true, false, true, true, false, false, false), "BELGIQUE");

        assertThat(r.verdict())
                .isEqualTo(AutoriteParentaleBeCalculator.Verdict.AUTORITE_EXCLUSIVE_FONDEE);
    }

    @Test
    void compute_demandeExclusive_avecIncapacite_exclusiveFondee() {
        AutoriteParentaleBeResult r = AutoriteParentaleBeCalculator.compute(
                input(true, false, true, false, false, true, false), "BELGIQUE");

        assertThat(r.verdict())
                .isEqualTo(AutoriteParentaleBeCalculator.Verdict.AUTORITE_EXCLUSIVE_FONDEE);
    }

    @Test
    void compute_demandeExclusive_sansMotifGrave_exclusiveNonFondee() {
        AutoriteParentaleBeResult r = AutoriteParentaleBeCalculator.compute(
                input(true, false, true, false, false, false, false), "BELGIQUE");

        assertThat(r.verdict())
                .isEqualTo(AutoriteParentaleBeCalculator.Verdict.AUTORITE_EXCLUSIVE_NON_FONDEE);
        assertThat(r.voieProcedurale())
                .isEqualTo(AutoriteParentaleBeCalculator.VoieProcedurale.AUCUNE_AUTORITE_CONJOINTE_DROIT);
    }

    @Test
    void compute_filiationNonEtablie_qualificationIncomplete() {
        AutoriteParentaleBeResult r = AutoriteParentaleBeCalculator.compute(
                input(false, false, true, false, true, false, false), "BELGIQUE");

        assertThat(r.verdict())
                .isEqualTo(AutoriteParentaleBeCalculator.Verdict.QUALIFICATION_INCOMPLETE);
        assertThat(r.voieProcedurale())
                .isEqualTo(AutoriteParentaleBeCalculator.VoieProcedurale.ETABLISSEMENT_FILIATION_PREALABLE);
    }

    @Test
    void compute_facteurDecisionAnterieure_estExposeMaisNonFavorableExclusive() {
        AutoriteParentaleBeResult r = AutoriteParentaleBeCalculator.compute(
                input(true, false, false, false, false, false, true), "BELGIQUE");

        assertThat(r.facteurs())
                .anyMatch(f -> f.code() == AutoriteParentaleBeCalculator.FacteurCode.DECISION_ANTERIEURE
                        && !f.favorableExclusive());
    }

    @Test
    void compute_facteurs_portentUnFondement() {
        AutoriteParentaleBeResult r = AutoriteParentaleBeCalculator.compute(
                input(true, false, true, true, true, true, false), "BELGIQUE");

        assertThat(r.facteurs()).hasSize(3);
        assertThat(r.facteurs()).allSatisfy(f -> {
            assertThat(f.fondement()).isNotBlank();
            assertThat(f.libelle()).isNotBlank();
        });
    }

    @Test
    void compute_paysFrance_rejette() {
        assertThatThrownBy(() -> AutoriteParentaleBeCalculator.compute(
                input(true, false, false, false, false, false, false), "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BELGIQUE");
    }

    @Test
    void compute_booleenManquant_rejette() {
        AutoriteParentaleBeInput partiel = new AutoriteParentaleBeInput(
                null, false, false, false, false, false, false,
                AutoriteParentaleBeCalculator.ModeHebergement.HEBERGEMENT_NON_FIXE, null);
        assertThatThrownBy(() -> AutoriteParentaleBeCalculator.compute(partiel, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("filiationEtablieDeuxParents");
    }

    @Test
    void compute_commentaireTropLong_rejette() {
        AutoriteParentaleBeInput in = new AutoriteParentaleBeInput(
                true, false, false, false, false, false, false,
                AutoriteParentaleBeCalculator.ModeHebergement.HEBERGEMENT_NON_FIXE,
                "x".repeat(1001));
        assertThatThrownBy(() -> AutoriteParentaleBeCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1000");
    }
}
