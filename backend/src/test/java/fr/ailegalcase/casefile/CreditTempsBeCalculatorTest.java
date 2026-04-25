package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreditTempsBeCalculatorTest {

    private static final LocalDate DATE_DEMANDE = LocalDate.of(2026, 4, 25);

    @Test
    void compute_avecMotif_soinsEnfant_anciennete30_eligibleAndDuree51() {
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "AVEC_MOTIF", "SOINS_ENFANT_LT_8_ANS", 30, 50,
                "MOITIE", 35, DATE_DEMANDE);

        assertThat(r.eligible()).isTrue();
        assertThat(r.criteresNonRemplis()).isEmpty();
        assertThat(r.dureeMaximaleMois()).isEqualTo(51);
        assertThat(r.regime()).isEqualTo("AVEC_MOTIF");
        assertThat(r.scoreGlobal()).isEqualTo(100);
        assertThat(r.verdictEligibilite()).isEqualTo("ELEVEE");
        assertThat(r.baseJuridique()).contains("CCT 103").contains("AR 29/10/1997");
    }

    @Test
    void compute_avecMotif_soinsParent_duree12() {
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "AVEC_MOTIF", "SOINS_PARENT_MALADE", 30, 50,
                "MOITIE", 35, DATE_DEMANDE);

        assertThat(r.eligible()).isTrue();
        assertThat(r.dureeMaximaleMois()).isEqualTo(12);
    }

    @Test
    void compute_avecMotif_formation_duree36() {
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "AVEC_MOTIF", "FORMATION", 30, 50,
                "MOITIE", 35, DATE_DEMANDE);

        assertThat(r.eligible()).isTrue();
        assertThat(r.dureeMaximaleMois()).isEqualTo(36);
    }

    @Test
    void compute_avecMotif_autre_duree36() {
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "AVEC_MOTIF", "AUTRE", 30, 50,
                "MOITIE", 35, DATE_DEMANDE);

        assertThat(r.eligible()).isTrue();
        assertThat(r.dureeMaximaleMois()).isEqualTo(36);
    }

    @Test
    void compute_avecMotif_sansMotif_criteresNonRemplisContainsMotifRequis() {
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "AVEC_MOTIF", null, 30, 50,
                "MOITIE", 35, DATE_DEMANDE);

        assertThat(r.eligible()).isFalse();
        assertThat(r.criteresNonRemplis())
                .anySatisfy(c -> assertThat(c).containsIgnoringCase("Motif requis"));
    }

    @Test
    void compute_avecMotif_anciennete12_criteresNonRemplisContainsAnciennete() {
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "AVEC_MOTIF", "SOINS_ENFANT_LT_8_ANS", 12, 50,
                "MOITIE", 35, DATE_DEMANDE);

        assertThat(r.eligible()).isFalse();
        assertThat(r.criteresNonRemplis())
                .anySatisfy(c -> assertThat(c).contains("Ancienneté"));
    }

    @Test
    void compute_sansMotif_etp5_criteresNonRemplisContainsTaille() {
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "SANS_MOTIF", null, 30, 5,
                "MOITIE", 35, DATE_DEMANDE);

        assertThat(r.eligible()).isFalse();
        assertThat(r.criteresNonRemplis())
                .anySatisfy(c -> assertThat(c).contains("10 ETP"));
    }

    @Test
    void compute_sansMotif_etp20_eligible() {
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "SANS_MOTIF", null, 30, 20,
                "MOITIE", 35, DATE_DEMANDE);

        assertThat(r.eligible()).isTrue();
        assertThat(r.criteresNonRemplis()).isEmpty();
    }

    @Test
    void compute_finCarriere_age50_criteresNonRemplisContainsAge() {
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "FIN_CARRIERE", null, 30, 50,
                "MOITIE", 50, DATE_DEMANDE);

        assertThat(r.eligible()).isFalse();
        assertThat(r.criteresNonRemplis())
                .anySatisfy(c -> assertThat(c).contains("55"));
    }

    @Test
    void compute_finCarriere_age60_eligible() {
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "FIN_CARRIERE", null, 30, 50,
                "MOITIE", 60, DATE_DEMANDE);

        assertThat(r.eligible()).isTrue();
        assertThat(r.criteresNonRemplis()).isEmpty();
        assertThat(r.dureeMaximaleMois()).isEqualTo(120);
    }

    @Test
    void compute_finCarriere_tempsPlein_criteresNonRemplisContainsTempsPlein() {
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "FIN_CARRIERE", null, 30, 50,
                "TEMPS_PLEIN", 60, DATE_DEMANDE);

        assertThat(r.eligible()).isFalse();
        assertThat(r.criteresNonRemplis())
                .anySatisfy(c -> assertThat(c).contains("TEMPS_PLEIN"));
    }

    @Test
    void compute_finCarriere_anyMissingCritere_scoreFloorOrFaible() {
        // âge 50 + ancienneté 0 = 2 critères bloquants (40 + 50 = 90 déductions) → score 10 (FAIBLE)
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "FIN_CARRIERE", null, 0, 50,
                "MOITIE", 50, DATE_DEMANDE);

        assertThat(r.scoreGlobal()).isLessThan(60);
        assertThat(r.verdictEligibilite()).isEqualTo("FAIBLE");
    }

    @Test
    void compute_avecMotif_cinquieme_indemniteApprox150() {
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "AVEC_MOTIF", "FORMATION", 30, 50,
                "CINQUIEME", 35, DATE_DEMANDE);

        assertThat(r.indemniteOnemMensuelle()).isEqualByComparingTo("150.00");
    }

    @Test
    void compute_avecMotif_moitie_indemniteApprox400() {
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "AVEC_MOTIF", "FORMATION", 30, 50,
                "MOITIE", 35, DATE_DEMANDE);

        assertThat(r.indemniteOnemMensuelle()).isEqualByComparingTo("400.00");
    }

    @Test
    void compute_avecMotif_tempsPlein_indemniteApprox700() {
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "AVEC_MOTIF", "FORMATION", 30, 50,
                "TEMPS_PLEIN", 35, DATE_DEMANDE);

        assertThat(r.indemniteOnemMensuelle()).isEqualByComparingTo("700.00");
    }

    @Test
    void compute_sansMotif_indemniteIs70PercentOfAvecMotif() {
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "SANS_MOTIF", null, 30, 20,
                "MOITIE", 35, DATE_DEMANDE);

        // 0.70 × 400 = 280
        assertThat(r.indemniteOnemMensuelle()).isEqualByComparingTo("280.00");
    }

    @Test
    void compute_finCarriere_cinquieme_indemnite200() {
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "FIN_CARRIERE", null, 30, 50,
                "CINQUIEME", 60, DATE_DEMANDE);

        assertThat(r.indemniteOnemMensuelle()).isEqualByComparingTo("200.00");
    }

    @Test
    void compute_finCarriere_moitie_indemnite500() {
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "FIN_CARRIERE", null, 30, 50,
                "MOITIE", 60, DATE_DEMANDE);

        assertThat(r.indemniteOnemMensuelle()).isEqualByComparingTo("500.00");
    }

    @Test
    void compute_score100_verdictElevee() {
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "AVEC_MOTIF", "SOINS_ENFANT_LT_8_ANS", 30, 50,
                "MOITIE", 35, DATE_DEMANDE);

        assertThat(r.scoreGlobal()).isEqualTo(100);
        assertThat(r.verdictEligibilite()).isEqualTo("ELEVEE");
    }

    @Test
    void compute_etp5_score70_verdictMoyenne() {
        // SANS_MOTIF + ETP < 10 → -30 → 70 (MOYENNE 60..79)
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "SANS_MOTIF", null, 30, 5,
                "MOITIE", 35, DATE_DEMANDE);

        assertThat(r.scoreGlobal()).isEqualTo(70);
        assertThat(r.verdictEligibilite()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_finCarriereTempsPleinAnciennete0_verdictFaible() {
        // ancienneté 0 (-40) + TEMPS_PLEIN sur fin de carrière (-30) = score 30 (FAIBLE)
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "FIN_CARRIERE", null, 0, 50,
                "TEMPS_PLEIN", 60, DATE_DEMANDE);

        assertThat(r.scoreGlobal()).isLessThan(60);
        assertThat(r.verdictEligibilite()).isEqualTo("FAIBLE");
    }

    @Test
    void compute_messagesContainsLegalReferences() {
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "AVEC_MOTIF", "SOINS_ENFANT_LT_8_ANS", 30, 50,
                "MOITIE", 35, DATE_DEMANDE);

        assertThat(r.messages()).isNotEmpty();
        assertThat(String.join(" | ", r.messages())).contains("CCT 103");
    }

    @Test
    void compute_formuleMentionsRegimeAndIndemnite() {
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "AVEC_MOTIF", "FORMATION", 30, 50,
                "MOITIE", 35, DATE_DEMANDE);

        assertThat(r.formule())
                .contains("AVEC_MOTIF")
                .contains("MOITIE")
                .contains("400");
    }

    @Test
    void compute_nullRegime_throws() {
        assertThatThrownBy(() -> CreditTempsBeCalculator.compute(
                null, "FORMATION", 30, 50, "MOITIE", 35, DATE_DEMANDE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("regime");
    }

    @Test
    void compute_unknownRegime_throws() {
        assertThatThrownBy(() -> CreditTempsBeCalculator.compute(
                "FOO_BAR", "FORMATION", 30, 50, "MOITIE", 35, DATE_DEMANDE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("regime");
    }

    @Test
    void compute_nullDureeReductionType_throws() {
        assertThatThrownBy(() -> CreditTempsBeCalculator.compute(
                "AVEC_MOTIF", "FORMATION", 30, 50, null, 35, DATE_DEMANDE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeReductionType");
    }

    @Test
    void compute_ageOver75_throws() {
        assertThatThrownBy(() -> CreditTempsBeCalculator.compute(
                "AVEC_MOTIF", "FORMATION", 30, 50, "MOITIE", 80, DATE_DEMANDE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ageDemandeur");
    }

    @Test
    void compute_negativeAnciennete_throws() {
        assertThatThrownBy(() -> CreditTempsBeCalculator.compute(
                "AVEC_MOTIF", "FORMATION", -1, 50, "MOITIE", 35, DATE_DEMANDE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ancienneteEntrepriseMois");
    }

    @Test
    void compute_nullDateDemande_throws() {
        assertThatThrownBy(() -> CreditTempsBeCalculator.compute(
                "AVEC_MOTIF", "FORMATION", 30, 50, "MOITIE", 35, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDemande");
    }

    @Test
    void compute_indemniteIsBigDecimalNotDouble() {
        CreditTempsBeResult r = CreditTempsBeCalculator.compute(
                "AVEC_MOTIF", "FORMATION", 30, 50,
                "MOITIE", 35, DATE_DEMANDE);

        assertThat(r.indemniteOnemMensuelle()).isInstanceOf(BigDecimal.class);
    }
}
