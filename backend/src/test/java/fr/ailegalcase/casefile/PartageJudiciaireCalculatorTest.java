package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.PartageJudiciaireCalculator.EtapeProcedure;
import fr.ailegalcase.casefile.PartageJudiciaireCalculator.TypeBienIndivision;
import fr.ailegalcase.casefile.PartageJudiciaireCalculator.VerdictRecevabilite;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartageJudiciaireCalculatorTest {

    private static final double VALEUR = 250_000.0;

    // ============ Verdict ELEVEE ============

    @Test
    void tousCriteres_bienDivisible_returnsELEVEE() {
        PartageJudiciaireResult r = PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL,
                TypeBienIndivision.IMMEUBLE_DIVISIBLE,
                3, VALEUR, true, true, true, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.ELEVEE);
        assertThat(r.scoreEligibilite()).isGreaterThanOrEqualTo(80);
        assertThat(r.risqueLicitation()).isFalse();
    }

    @Test
    void meublesDivers_tousCriteres_returnsELEVEE() {
        PartageJudiciaireResult r = PartageJudiciaireCalculator.compute(
                EtapeProcedure.HOMOLOGATION_AMIABLE_PARTIELLE,
                TypeBienIndivision.MEUBLES_DIVERS,
                2, 50_000.0, true, true, true, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.ELEVEE);
        assertThat(r.risqueLicitation()).isFalse();
    }

    @Test
    void mixte_tousCriteres_returnsELEVEE_dureeIntermediaire() {
        PartageJudiciaireResult r = PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL,
                TypeBienIndivision.MIXTE,
                4, 500_000.0, true, true, true, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.ELEVEE);
        assertThat(r.dureeProcedureMois()).isBetween(12, 18);
    }

    // ============ Verdict MOYENNE (bien indivisible) ============

    @Test
    void bienIndivisible_tousCriteres_returnsMOYENNE_risqueLicitation() {
        PartageJudiciaireResult r = PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL,
                TypeBienIndivision.IMMEUBLE_INDIVISIBLE,
                2, 400_000.0, true, true, true, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.MOYENNE);
        assertThat(r.risqueLicitation()).isTrue();
        assertThat(r.dureeProcedureMois()).isEqualTo(18);
    }

    @Test
    void bienIndivisible_messageLicitation() {
        PartageJudiciaireResult r = PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL,
                TypeBienIndivision.IMMEUBLE_INDIVISIBLE,
                2, 400_000.0, true, true, true, "FRANCE");
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("licitation"));
    }

    // ============ Verdict FAIBLE ============

    @Test
    void pvNonEtabli_returnsFAIBLE() {
        PartageJudiciaireResult r = PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL,
                TypeBienIndivision.IMMEUBLE_DIVISIBLE,
                3, VALEUR, false, true, true, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
        assertThat(r.messages()).anyMatch(m -> m.contains("PV de difficultés NON établi"));
    }

    @Test
    void tentativeAmiableNonEpuisee_returnsFAIBLE() {
        PartageJudiciaireResult r = PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL,
                TypeBienIndivision.IMMEUBLE_DIVISIBLE,
                3, VALEUR, true, false, true, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("amiable"));
    }

    @Test
    void pvNonEtabli_etAmiableNonEpuisee_returnsFAIBLE() {
        PartageJudiciaireResult r = PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL,
                TypeBienIndivision.IMMEUBLE_DIVISIBLE,
                3, VALEUR, false, false, false, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
    }

    // ============ Frais estimés ============

    @Test
    void frais_estimes_dans_fourchette_2_5_pct_bien_divisible() {
        PartageJudiciaireResult r = PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL,
                TypeBienIndivision.IMMEUBLE_DIVISIBLE,
                2, 200_000.0, true, true, true, "FRANCE");
        // 200000 * 3.5% = 7000
        assertThat(r.fraisEstimesEur()).isBetween(200_000.0 * 0.02, 200_000.0 * 0.05);
    }

    @Test
    void frais_estimes_5_pct_si_bien_indivisible() {
        PartageJudiciaireResult r = PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL,
                TypeBienIndivision.IMMEUBLE_INDIVISIBLE,
                2, 200_000.0, true, true, true, "FRANCE");
        // 200000 * 5% = 10000
        assertThat(r.fraisEstimesEur()).isEqualTo(10_000.0);
    }

    @Test
    void frais_estimes_zero_si_valeur_zero() {
        PartageJudiciaireResult r = PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL,
                TypeBienIndivision.IMMEUBLE_DIVISIBLE,
                2, 0.0, true, true, true, "FRANCE");
        assertThat(r.fraisEstimesEur()).isEqualTo(0.0);
    }

    // ============ Durée procédure ============

    @Test
    void duree_max_18_mois_si_bien_indivisible() {
        PartageJudiciaireResult r = PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL,
                TypeBienIndivision.IMMEUBLE_INDIVISIBLE,
                3, VALEUR, true, true, true, "FRANCE");
        assertThat(r.dureeProcedureMois()).isEqualTo(18);
    }

    @Test
    void duree_dans_fourchette_6_18() {
        PartageJudiciaireResult r = PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL,
                TypeBienIndivision.IMMEUBLE_DIVISIBLE,
                3, VALEUR, true, true, true, "FRANCE");
        assertThat(r.dureeProcedureMois()).isBetween(6, 18);
    }

    // ============ Base juridique ============

    @Test
    void baseJuridique_contient_840_1364_1366() {
        PartageJudiciaireResult r = PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL,
                TypeBienIndivision.IMMEUBLE_DIVISIBLE,
                3, VALEUR, true, true, true, "FRANCE");
        assertThat(r.baseJuridique()).contains("840");
        assertThat(r.baseJuridique()).contains("1364");
        assertThat(r.baseJuridique()).contains("1366");
    }

    // ============ Country ============

    @Test
    void country_FRANCE_normalized() {
        PartageJudiciaireResult r = PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL,
                TypeBienIndivision.IMMEUBLE_DIVISIBLE,
                3, VALEUR, true, true, true, "france");
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    @Test
    void country_BELGIQUE_throws() {
        assertThatThrownBy(() -> PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL,
                TypeBienIndivision.IMMEUBLE_DIVISIBLE,
                3, VALEUR, true, true, true, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BELGIQUE");
    }

    // ============ Validations ============

    @Test
    void validation_etapeProcedure_null_throws() {
        assertThatThrownBy(() -> PartageJudiciaireCalculator.compute(
                null, TypeBienIndivision.IMMEUBLE_DIVISIBLE,
                3, VALEUR, true, true, true, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Étape");
    }

    @Test
    void validation_typeBien_null_throws() {
        assertThatThrownBy(() -> PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL, null,
                3, VALEUR, true, true, true, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bien");
    }

    @Test
    void validation_nombreCoindivisaires_inferieur_2_throws() {
        assertThatThrownBy(() -> PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL,
                TypeBienIndivision.IMMEUBLE_DIVISIBLE,
                1, VALEUR, true, true, true, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("co-indivisaires");
    }

    @Test
    void validation_nombreCoindivisaires_null_throws() {
        assertThatThrownBy(() -> PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL,
                TypeBienIndivision.IMMEUBLE_DIVISIBLE,
                null, VALEUR, true, true, true, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validation_valeurNegative_throws() {
        assertThatThrownBy(() -> PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL,
                TypeBienIndivision.IMMEUBLE_DIVISIBLE,
                3, -100.0, true, true, true, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validation_pvDifficultes_null_throws() {
        assertThatThrownBy(() -> PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL,
                TypeBienIndivision.IMMEUBLE_DIVISIBLE,
                3, VALEUR, null, true, true, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PV");
    }

    @Test
    void validation_country_null_throws() {
        assertThatThrownBy(() -> PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL,
                TypeBienIndivision.IMMEUBLE_DIVISIBLE,
                3, VALEUR, true, true, true, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ============ Messages ============

    @Test
    void messages_contiennent_etape_libelle() {
        PartageJudiciaireResult r = PartageJudiciaireCalculator.compute(
                EtapeProcedure.PROCES_VERBAL_DIFFICULTES,
                TypeBienIndivision.IMMEUBLE_DIVISIBLE,
                3, VALEUR, true, true, true, "FRANCE");
        assertThat(r.messages()).anyMatch(m -> m.contains("PV de difficultés"));
    }

    @Test
    void messages_alerte_si_indivisible() {
        PartageJudiciaireResult r = PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL,
                TypeBienIndivision.IMMEUBLE_INDIVISIBLE,
                2, VALEUR, true, true, true, "FRANCE");
        assertThat(r.messages()).anyMatch(m -> m.contains("INDIVISIBLE")
                || m.toLowerCase().contains("licitation"));
    }

    @Test
    void formule_contient_score_et_verdict() {
        PartageJudiciaireResult r = PartageJudiciaireCalculator.compute(
                EtapeProcedure.PARTAGE_JUDICIAIRE_INTEGRAL,
                TypeBienIndivision.IMMEUBLE_DIVISIBLE,
                3, VALEUR, true, true, true, "FRANCE");
        assertThat(r.formule()).contains("score");
        assertThat(r.formule()).contains("ELEVEE");
    }
}
