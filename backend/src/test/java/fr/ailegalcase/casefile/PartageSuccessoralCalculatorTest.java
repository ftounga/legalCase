package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.PartageSuccessoralCalculator.ModePartage;
import fr.ailegalcase.casefile.PartageSuccessoralCalculator.VerdictRecevabilite;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartageSuccessoralCalculatorTest {

    private static final LocalDate DATE_DECES = LocalDate.of(2025, 6, 15);
    private static final double MASSE = 300_000.0;

    // ============ Verdict ELEVEE ============

    @Test
    void amiable_tousConsentements_pasDesaccord_returnsELEVEE() {
        PartageSuccessoralResult r = PartageSuccessoralCalculator.compute(
                ModePartage.PARTAGE_AMIABLE,
                3, true, false, true, false, DATE_DECES, MASSE, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.ELEVEE);
        assertThat(r.modeRecommande()).isEqualTo(ModePartage.PARTAGE_AMIABLE);
        assertThat(r.basculeMode()).isFalse();
    }

    @Test
    void amiable_avecImmeubles_consentements_returnsELEVEE_messageNotaire() {
        PartageSuccessoralResult r = PartageSuccessoralCalculator.compute(
                ModePartage.PARTAGE_AMIABLE,
                2, true, true, true, false, DATE_DECES, MASSE, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.ELEVEE);
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("notaire")
                || m.contains("AUTHENTIQUE"));
    }

    @Test
    void judiciaire_desaccordMotive_returnsELEVEE() {
        PartageSuccessoralResult r = PartageSuccessoralCalculator.compute(
                ModePartage.PARTAGE_JUDICIAIRE,
                3, false, false, false, true, DATE_DECES, MASSE, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.ELEVEE);
        assertThat(r.modeRecommande()).isEqualTo(ModePartage.PARTAGE_JUDICIAIRE);
    }

    @Test
    void judiciaire_immeuble_desaccord_riskLicitation() {
        PartageSuccessoralResult r = PartageSuccessoralCalculator.compute(
                ModePartage.PARTAGE_JUDICIAIRE,
                2, false, true, false, true, DATE_DECES, MASSE, "FRANCE");
        assertThat(r.risqueLicitation()).isTrue();
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("licitation"));
    }

    // ============ Bascule amiable → judiciaire ============

    @Test
    void amiable_consentementsPartiels_basculeJudiciaire_MOYENNE() {
        PartageSuccessoralResult r = PartageSuccessoralCalculator.compute(
                ModePartage.PARTAGE_AMIABLE,
                3, false, false, true, false, DATE_DECES, MASSE, "FRANCE");
        assertThat(r.basculeMode()).isTrue();
        assertThat(r.modeRecommande()).isEqualTo(ModePartage.PARTAGE_JUDICIAIRE);
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.MOYENNE);
        assertThat(r.messages()).anyMatch(m -> m.contains("BASCULE"));
    }

    @Test
    void amiable_desaccordPersistant_basculeJudiciaire_MOYENNE() {
        PartageSuccessoralResult r = PartageSuccessoralCalculator.compute(
                ModePartage.PARTAGE_AMIABLE,
                2, true, false, true, true, DATE_DECES, MASSE, "FRANCE");
        assertThat(r.basculeMode()).isTrue();
        assertThat(r.modeRecommande()).isEqualTo(ModePartage.PARTAGE_JUDICIAIRE);
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.MOYENNE);
    }

    // ============ Verdict MOYENNE / partiel ============

    @Test
    void partiel_returnsMOYENNE_messageIndivisionResiduelle() {
        PartageSuccessoralResult r = PartageSuccessoralCalculator.compute(
                ModePartage.PARTAGE_PARTIEL,
                3, true, false, true, false, DATE_DECES, MASSE, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.MOYENNE);
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("indivision"));
    }

    // ============ Délai instruction ============

    @Test
    void delai_3_mois_si_amiable() {
        PartageSuccessoralResult r = PartageSuccessoralCalculator.compute(
                ModePartage.PARTAGE_AMIABLE,
                2, true, false, true, false, DATE_DECES, MASSE, "FRANCE");
        assertThat(r.delaiInstructionMois()).isEqualTo(3);
    }

    @Test
    void delai_judiciaire_dans_fourchette_6_18() {
        PartageSuccessoralResult r = PartageSuccessoralCalculator.compute(
                ModePartage.PARTAGE_JUDICIAIRE,
                2, false, true, false, true, DATE_DECES, MASSE, "FRANCE");
        assertThat(r.delaiInstructionMois()).isBetween(6, 18);
    }

    // ============ Frais estimés ============

    @Test
    void frais_1_pct_si_amiable_simple() {
        PartageSuccessoralResult r = PartageSuccessoralCalculator.compute(
                ModePartage.PARTAGE_AMIABLE,
                2, true, false, true, false, DATE_DECES, 100_000.0, "FRANCE");
        assertThat(r.fraisEstimesPct()).isEqualTo(0.01);
        assertThat(r.fraisEstimesEur()).isEqualTo(1_000.0);
    }

    @Test
    void frais_3_pct_si_judiciaire_avec_immeubles() {
        PartageSuccessoralResult r = PartageSuccessoralCalculator.compute(
                ModePartage.PARTAGE_JUDICIAIRE,
                2, false, true, false, true, DATE_DECES, 200_000.0, "FRANCE");
        assertThat(r.fraisEstimesPct()).isEqualTo(0.03);
        assertThat(r.fraisEstimesEur()).isEqualTo(6_000.0);
    }

    // ============ Base juridique ============

    @Test
    void baseJuridique_contient_815_840_1364() {
        PartageSuccessoralResult r = PartageSuccessoralCalculator.compute(
                ModePartage.PARTAGE_AMIABLE,
                2, true, false, true, false, DATE_DECES, MASSE, "FRANCE");
        assertThat(r.baseJuridique()).contains("815");
        assertThat(r.baseJuridique()).contains("840");
        assertThat(r.baseJuridique()).contains("1364");
    }

    // ============ Country ============

    @Test
    void country_FRANCE_normalized() {
        PartageSuccessoralResult r = PartageSuccessoralCalculator.compute(
                ModePartage.PARTAGE_AMIABLE,
                2, true, false, true, false, DATE_DECES, MASSE, "france");
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    @Test
    void country_BELGIQUE_throws() {
        assertThatThrownBy(() -> PartageSuccessoralCalculator.compute(
                ModePartage.PARTAGE_AMIABLE,
                2, true, false, true, false, DATE_DECES, MASSE, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BELGIQUE");
    }

    // ============ Validations ============

    @Test
    void validation_modePartageDemande_null_throws() {
        assertThatThrownBy(() -> PartageSuccessoralCalculator.compute(
                null, 2, true, false, true, false, DATE_DECES, MASSE, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Modalité");
    }

    @Test
    void validation_nombreCoheritiers_inferieur_2_throws() {
        assertThatThrownBy(() -> PartageSuccessoralCalculator.compute(
                ModePartage.PARTAGE_AMIABLE,
                1, true, false, true, false, DATE_DECES, MASSE, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cohéritiers");
    }

    @Test
    void validation_dateDeces_future_throws() {
        LocalDate futur = LocalDate.now().plusMonths(1);
        assertThatThrownBy(() -> PartageSuccessoralCalculator.compute(
                ModePartage.PARTAGE_AMIABLE,
                2, true, false, true, false, futur, MASSE, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }
}
