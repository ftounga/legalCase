package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-216-23 : tests unitaires du calculateur Donation entre époux FR
 * (art. 1091-1100 Cciv + art. 265 al. 2 + art. 1527 al. 2 + art. 912-928).
 */
class DonationEntreEpouxCalculatorTest {

    private static DonationEntreEpouxRequest req(
            AvantageMatrimonialTypeEnum type,
            LocalDate dateContrat,
            RegimeMatrimonialEnum regime,
            Boolean mariageDissous,
            Boolean revocabiliteDetectee,
            BienDonneTypeEnum bien,
            Integer valeur,
            Boolean enfantsNonCommuns) {
        return new DonationEntreEpouxRequest(
                type, dateContrat, regime, mariageDissous, revocabiliteDetectee,
                bien, valeur, enfantsNonCommuns);
    }

    // ── AC1 : clause attribution intégrale + enfants non communs → action retranchement ──

    @Test
    void ac1_clause_attribution_integrale_enfants_non_communs_action_retranchement() {
        DonationEntreEpouxRequest r = req(
                AvantageMatrimonialTypeEnum.CLAUSE_ATTRIBUTION_INTEGRALE,
                LocalDate.of(2015, 6, 10),
                RegimeMatrimonialEnum.COMMUNAUTE_UNIVERSELLE,
                false, false,
                BienDonneTypeEnum.IMMOBILIER,
                250_000,
                true);
        DonationEntreEpouxResult res =
                DonationEntreEpouxCalculator.compute(r, "FRANCE");
        assertThat(res.actionRetranchementPossible()).isTrue();
        assertThat(res.revocabilite()).isEqualTo("IRREVOCABLE");
        assertThat(res.validite()).isEqualTo("VALIDE");
        assertThat(res.alertes()).anyMatch(a -> a.contains("retranchement"));
        assertThat(res.alertes()).anyMatch(a -> a.contains("1527"));
        assertThat(res.baseLegale()).contains("1527");
    }

    @Test
    void avantage_matrimonial_avec_enfants_non_communs_action_retranchement() {
        DonationEntreEpouxRequest r = req(
                AvantageMatrimonialTypeEnum.AVANTAGE_MATRIMONIAL,
                null, null,
                false, false,
                null, null,
                true);
        DonationEntreEpouxResult res =
                DonationEntreEpouxCalculator.compute(r, "FRANCE");
        assertThat(res.actionRetranchementPossible()).isTrue();
    }

    @Test
    void clause_attribution_integrale_sans_enfants_non_communs_pas_de_retranchement() {
        DonationEntreEpouxRequest r = req(
                AvantageMatrimonialTypeEnum.CLAUSE_ATTRIBUTION_INTEGRALE,
                null,
                RegimeMatrimonialEnum.COMMUNAUTE_UNIVERSELLE,
                false, false,
                null, null,
                false);
        DonationEntreEpouxResult res =
                DonationEntreEpouxCalculator.compute(r, "FRANCE");
        assertThat(res.actionRetranchementPossible()).isFalse();
    }

    // ── AC2 : divorce prononcé + donation biens futurs → révocation automatique ──

    @Test
    void ac2_divorce_donation_biens_futurs_revocation_automatique() {
        DonationEntreEpouxRequest r = req(
                AvantageMatrimonialTypeEnum.DONATION_BIENS_FUTURS,
                LocalDate.of(2010, 1, 1),
                RegimeMatrimonialEnum.COMMUNAUTE_LEGALE,
                true, false,
                null, null, false);
        DonationEntreEpouxResult res =
                DonationEntreEpouxCalculator.compute(r, "FRANCE");
        assertThat(res.validite()).isEqualTo("REVOQUEE");
        assertThat(res.revocabilite()).isEqualTo("REVOCATION_DE_PLEIN_DROIT");
        assertThat(res.effetDissolution()).contains("265");
        assertThat(res.messages()).anyMatch(m -> m.contains("265 al. 2"));
    }

    @Test
    void divorce_clause_attribution_integrale_revocation_automatique() {
        DonationEntreEpouxRequest r = req(
                AvantageMatrimonialTypeEnum.CLAUSE_ATTRIBUTION_INTEGRALE,
                null, RegimeMatrimonialEnum.COMMUNAUTE_UNIVERSELLE,
                true, false,
                null, null, false);
        DonationEntreEpouxResult res =
                DonationEntreEpouxCalculator.compute(r, "FRANCE");
        assertThat(res.validite()).isEqualTo("REVOQUEE");
        assertThat(res.revocabilite()).isEqualTo("REVOCATION_DE_PLEIN_DROIT");
    }

    @Test
    void divorce_donation_bien_present_pas_de_revocation_automatique() {
        DonationEntreEpouxRequest r = req(
                AvantageMatrimonialTypeEnum.DONATION_BIEN_PRESENT,
                LocalDate.of(2018, 5, 1),
                RegimeMatrimonialEnum.SEPARATION_DE_BIENS,
                true, false,
                BienDonneTypeEnum.IMMOBILIER, 200_000,
                false);
        DonationEntreEpouxResult res =
                DonationEntreEpouxCalculator.compute(r, "FRANCE");
        assertThat(res.validite()).isEqualTo("VALIDE");
        assertThat(res.effetDissolution()).contains("265 al. 1");
    }

    @Test
    void divorce_assurance_vie_pas_de_revocation_automatique() {
        DonationEntreEpouxRequest r = req(
                AvantageMatrimonialTypeEnum.ASSURANCE_VIE,
                null, null,
                true, false,
                null, null, false);
        DonationEntreEpouxResult res =
                DonationEntreEpouxCalculator.compute(r, "FRANCE");
        assertThat(res.validite()).isEqualTo("VALIDE");
        assertThat(res.effetDissolution()).contains("assurance-vie");
    }

    // ── AC3 : country=BELGIQUE → IAE ──

    @Test
    void ac3_belgique_throws_illegal_argument() {
        DonationEntreEpouxRequest r = req(
                AvantageMatrimonialTypeEnum.DONATION_BIENS_FUTURS,
                null, null,
                false, false,
                null, null, false);
        assertThatThrownBy(() ->
                DonationEntreEpouxCalculator.compute(r, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("France");
    }

    // ── Cas complémentaires ──

    @Test
    void donation_biens_futurs_sans_divorce_revocable_ad_nutum() {
        DonationEntreEpouxRequest r = req(
                AvantageMatrimonialTypeEnum.DONATION_BIENS_FUTURS,
                null, null,
                false, false,
                null, null, false);
        DonationEntreEpouxResult res =
                DonationEntreEpouxCalculator.compute(r, "FRANCE");
        assertThat(res.validite()).isEqualTo("VALIDE");
        assertThat(res.revocabilite()).isEqualTo("REVOCABLE_AD_NUTUM");
        assertThat(res.messages()).anyMatch(m -> m.contains("1094"));
        assertThat(res.impactReserve()).contains("1094-1");
    }

    @Test
    void revocation_expresse_documentee_donation_revoquee() {
        DonationEntreEpouxRequest r = req(
                AvantageMatrimonialTypeEnum.DONATION_BIENS_FUTURS,
                null, null,
                false, true,
                null, null, false);
        DonationEntreEpouxResult res =
                DonationEntreEpouxCalculator.compute(r, "FRANCE");
        assertThat(res.validite()).isEqualTo("REVOQUEE");
        assertThat(res.revocabilite()).isEqualTo("REVOCATION_CONSTATEE");
        assertThat(res.messages()).anyMatch(m -> m.contains("1096 al. 2"));
    }

    @Test
    void revocation_expresse_et_divorce_alerte_double_revocation() {
        DonationEntreEpouxRequest r = req(
                AvantageMatrimonialTypeEnum.DONATION_BIENS_FUTURS,
                null, null,
                true, true,
                null, null, false);
        DonationEntreEpouxResult res =
                DonationEntreEpouxCalculator.compute(r, "FRANCE");
        assertThat(res.validite()).isEqualTo("REVOQUEE");
        assertThat(res.alertes()).anyMatch(a -> a.contains("doublement"));
    }

    @Test
    void avantage_matrimonial_irrevocable_pendant_mariage() {
        DonationEntreEpouxRequest r = req(
                AvantageMatrimonialTypeEnum.AVANTAGE_MATRIMONIAL,
                null, null,
                false, false,
                null, null, false);
        DonationEntreEpouxResult res =
                DonationEntreEpouxCalculator.compute(r, "FRANCE");
        assertThat(res.validite()).isEqualTo("VALIDE");
        assertThat(res.revocabilite()).isEqualTo("IRREVOCABLE");
        assertThat(res.messages()).anyMatch(m -> m.contains("1397"));
    }

    @Test
    void assurance_vie_sans_divorce_revocable() {
        DonationEntreEpouxRequest r = req(
                AvantageMatrimonialTypeEnum.ASSURANCE_VIE,
                null, null,
                false, false,
                null, null, false);
        DonationEntreEpouxResult res =
                DonationEntreEpouxCalculator.compute(r, "FRANCE");
        assertThat(res.validite()).isEqualTo("VALIDE");
        assertThat(res.revocabilite()).isEqualTo("REVOCABLE_AD_NUTUM");
        assertThat(res.impactReserve()).contains("L.132-12");
    }

    @Test
    void type_avantage_null_verdict_a_verifier() {
        DonationEntreEpouxRequest r = req(
                null, null, null,
                false, false,
                null, null, false);
        DonationEntreEpouxResult res =
                DonationEntreEpouxCalculator.compute(r, "FRANCE");
        assertThat(res.validite()).isEqualTo("A_VERIFIER");
        assertThat(res.revocabilite()).isEqualTo("INDETERMINEE");
        assertThat(res.alertes()).anyMatch(a -> a.contains("Type d'avantage"));
    }

    @Test
    void base_legale_inclut_articles_clefs() {
        DonationEntreEpouxRequest r = req(
                AvantageMatrimonialTypeEnum.DONATION_BIENS_FUTURS,
                null, null,
                false, false,
                null, null, false);
        DonationEntreEpouxResult res =
                DonationEntreEpouxCalculator.compute(r, "FRANCE");
        assertThat(res.baseLegale()).contains("1091-1100");
        assertThat(res.baseLegale()).contains("265 al. 2");
        assertThat(res.baseLegale()).contains("1527 al. 2");
        assertThat(res.baseLegale()).contains("912-928");
    }

    @Test
    void donation_bien_present_impact_reserve_heritaire() {
        DonationEntreEpouxRequest r = req(
                AvantageMatrimonialTypeEnum.DONATION_BIEN_PRESENT,
                null,
                RegimeMatrimonialEnum.SEPARATION_DE_BIENS,
                false, false,
                BienDonneTypeEnum.NUMERAIRE, 50_000,
                false);
        DonationEntreEpouxResult res =
                DonationEntreEpouxCalculator.compute(r, "FRANCE");
        assertThat(res.impactReserve()).contains("réserve");
        assertThat(res.impactReserve()).contains("912");
    }
}
