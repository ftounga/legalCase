package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-216-29 : tests unitaires du calculateur Donation-partage FR (art. 1075
 * à 1075-5 Cciv + art. 1078, 1078-1, 1080 + art. 912-928).
 */
class DonationPartageCalculatorTest {

    private static DonationPartageRequest req(
            Integer nbDesc,
            Boolean petitsEnfants,
            Boolean conjonctive,
            Integer valeur,
            Boolean quotiteRespectee,
            Boolean reincorporation,
            List<Integer> ages) {
        return new DonationPartageRequest(
                nbDesc, petitsEnfants, conjonctive, valeur,
                quotiteRespectee, reincorporation, ages);
    }

    // ── AC1 : 2 descendants + gel valeur ──

    @Test
    void ac1_deux_descendants_gel_valeur_rapport_exclu() {
        DonationPartageRequest r = req(
                2, false, false,
                300_000, true, false,
                List.of(58));
        DonationPartageResult res = DonationPartageCalculator.compute(r, "FRANCE");
        assertThat(res.conditionsRemplies()).isTrue();
        assertThat(res.rapportExclu()).isTrue();
        assertThat(res.alerteQuotite()).isFalse();
        assertThat(res.gelValeurEffet()).contains("1078");
        assertThat(res.interet()).isEqualTo("FORT");
        assertThat(res.messages()).anyMatch(m -> m.contains("1075-3"));
        assertThat(res.baseLegale()).contains("1075");
    }

    // ── AC2 : quotité disponible dépassée → alerte ──

    @Test
    void ac2_quotite_disponible_depassee_alerte() {
        DonationPartageRequest r = req(
                2, false, false,
                1_000_000, false, false,
                List.of(70));
        DonationPartageResult res = DonationPartageCalculator.compute(r, "FRANCE");
        assertThat(res.alerteQuotite()).isTrue();
        assertThat(res.alertes()).anyMatch(a -> a.contains("Quotité"));
        assertThat(res.alertes()).anyMatch(a -> a.contains("912"));
        assertThat(res.interet()).isEqualTo("MOYEN");
    }

    @Test
    void quotite_non_renseignee_alerte_audit() {
        DonationPartageRequest r = req(
                3, false, false,
                500_000, null, false,
                List.of(65));
        DonationPartageResult res = DonationPartageCalculator.compute(r, "FRANCE");
        assertThat(res.alerteQuotite()).isFalse();
        assertThat(res.alertes()).anyMatch(a -> a.contains("audit"));
    }

    // ── AC3 : Belgique → IAE ──

    @Test
    void ac3_belgique_throws_illegal_argument() {
        DonationPartageRequest r = req(
                2, false, false,
                null, true, false,
                List.of(60));
        assertThatThrownBy(() ->
                DonationPartageCalculator.compute(r, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("France");
    }

    // ── Cas complémentaires ──

    @Test
    void zero_descendant_throws() {
        DonationPartageRequest r = req(
                0, false, false,
                null, null, false, null);
        assertThatThrownBy(() ->
                DonationPartageCalculator.compute(r, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombreDescendants");
    }

    @Test
    void nombre_descendants_null_throws() {
        DonationPartageRequest r = req(
                null, false, false,
                null, null, false, null);
        assertThatThrownBy(() ->
                DonationPartageCalculator.compute(r, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void age_donateur_negatif_throws() {
        DonationPartageRequest r = req(
                2, false, false,
                null, true, false,
                List.of(-5));
        assertThatThrownBy(() ->
                DonationPartageCalculator.compute(r, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("âges");
    }

    @Test
    void valeur_negative_throws() {
        DonationPartageRequest r = req(
                2, false, false,
                -100, true, false,
                List.of(60));
        assertThatThrownBy(() ->
                DonationPartageCalculator.compute(r, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valeurPartageTotal");
    }

    @Test
    void petits_enfants_par_substitution_alerte_consentement() {
        DonationPartageRequest r = req(
                3, true, false,
                400_000, true, false,
                List.of(72));
        DonationPartageResult res = DonationPartageCalculator.compute(r, "FRANCE");
        assertThat(res.alertes()).anyMatch(a -> a.contains("Consentement"));
        assertThat(res.alertes()).anyMatch(a -> a.contains("1075-1"));
        assertThat(res.messages()).anyMatch(m -> m.contains("petits-enfants"));
    }

    @Test
    void donation_partage_conjonctive_messages_et_etapes() {
        DonationPartageRequest r = req(
                2, false, true,
                500_000, true, false,
                List.of(63, 60));
        DonationPartageResult res = DonationPartageCalculator.compute(r, "FRANCE");
        assertThat(res.messages()).anyMatch(m -> m.contains("conjonctive"));
        assertThat(res.messages()).anyMatch(m -> m.contains("1075-2"));
        assertThat(res.etapesNotariales()).anyMatch(e -> e.contains("deux donateurs"));
    }

    @Test
    void donation_partage_conjonctive_un_seul_age_alerte() {
        DonationPartageRequest r = req(
                2, false, true,
                500_000, true, false,
                List.of(65));
        DonationPartageResult res = DonationPartageCalculator.compute(r, "FRANCE");
        assertThat(res.alertes()).anyMatch(a -> a.contains("conjonctive"));
    }

    @Test
    void reincorporation_donations_anterieures_etape_specifique() {
        DonationPartageRequest r = req(
                2, false, false,
                400_000, true, true,
                List.of(68));
        DonationPartageResult res = DonationPartageCalculator.compute(r, "FRANCE");
        assertThat(res.messages()).anyMatch(m -> m.contains("Réincorporation"));
        assertThat(res.messages()).anyMatch(m -> m.contains("1078-1"));
        assertThat(res.etapesNotariales()).anyMatch(e -> e.contains("Réincorporation")
                || e.contains("réincorporer"));
    }

    @Test
    void un_seul_descendant_verdict_moyen() {
        DonationPartageRequest r = req(
                1, false, false,
                200_000, true, false,
                List.of(70));
        DonationPartageResult res = DonationPartageCalculator.compute(r, "FRANCE");
        assertThat(res.interet()).isEqualTo("MOYEN");
        assertThat(res.messages()).anyMatch(m -> m.contains("un seul descendant"));
    }

    @Test
    void etapes_notariales_minimum_3_etapes() {
        DonationPartageRequest r = req(
                2, false, false,
                300_000, true, false,
                List.of(60));
        DonationPartageResult res = DonationPartageCalculator.compute(r, "FRANCE");
        assertThat(res.etapesNotariales()).isNotEmpty();
        assertThat(res.etapesNotariales().size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void base_legale_inclut_articles_clefs() {
        DonationPartageRequest r = req(
                2, false, false,
                null, true, false,
                List.of(60));
        DonationPartageResult res = DonationPartageCalculator.compute(r, "FRANCE");
        assertThat(res.baseLegale()).contains("1075");
        assertThat(res.baseLegale()).contains("1078");
        assertThat(res.baseLegale()).contains("912-928");
    }

    @Test
    void quasi_usufruit_message_systematique() {
        DonationPartageRequest r = req(
                2, false, false,
                null, true, false,
                List.of(60));
        DonationPartageResult res = DonationPartageCalculator.compute(r, "FRANCE");
        assertThat(res.messages()).anyMatch(m -> m.contains("1080"));
    }
}
