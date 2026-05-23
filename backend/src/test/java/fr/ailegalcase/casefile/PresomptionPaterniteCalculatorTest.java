package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-216-25 : tests unitaires du calculateur Présomption de paternité du
 * mari et désaveu FR (art. 312-315 Cciv + art. 316 al. 2 + art. 333 al. 1).
 */
class PresomptionPaterniteCalculatorTest {

    private static final LocalDate TODAY_2026 = LocalDate.of(2026, 5, 23);

    private static PresomptionPaterniteRequest req(
            LocalDate dateNaissance,
            LocalDate dateMariage,
            LocalDate dateDisso,
            LocalDate dateAccouchement,
            Boolean conception180,
            Boolean neApresDisso,
            Boolean desaveu,
            Boolean possessionEtat,
            LocalDate dateConnaissance) {
        return new PresomptionPaterniteRequest(
                dateNaissance, dateMariage, dateDisso, dateAccouchement,
                conception180, neApresDisso, desaveu, possessionEtat,
                dateConnaissance);
    }

    // ── AC1 : né 290 jours après dissolution → présomption applicable ──

    @Test
    void ac1_ne_290j_apres_dissolution_presomption_applicable() {
        LocalDate disso = LocalDate.of(2025, 1, 1);
        LocalDate naissance = disso.plusDays(290);
        PresomptionPaterniteRequest r = req(
                naissance, LocalDate.of(2020, 6, 1), disso,
                naissance, false, false, false, false, null);
        PresomptionPaterniteResult res =
                PresomptionPaterniteCalculator.compute(r, "FRANCE", TODAY_2026);
        assertThat(res.presomptionApplicable()).isTrue();
        assertThat(res.presomptionRenversee()).isFalse();
    }

    // ── AC2 : né 310 jours après dissolution → présomption inapplicable ──

    @Test
    void ac2_ne_310j_apres_dissolution_presomption_inapplicable() {
        LocalDate disso = LocalDate.of(2025, 1, 1);
        LocalDate naissance = disso.plusDays(310);
        PresomptionPaterniteRequest r = req(
                naissance, LocalDate.of(2020, 6, 1), disso,
                naissance, false, null, false, false, null);
        PresomptionPaterniteResult res =
                PresomptionPaterniteCalculator.compute(r, "FRANCE", TODAY_2026);
        assertThat(res.presomptionApplicable()).isFalse();
        assertThat(res.presomptionRenversee()).isTrue();
        assertThat(res.voieDesaveu()).isEqualTo("DESAVEU_SANS_OBJET");
        assertThat(res.messages())
                .anyMatch(m -> m.contains("plus de 300 jours"));
    }

    // ── AC3 : possession d'état conforme → alerte désaveu difficile ──

    @Test
    void ac3_possession_etat_conforme_alerte_desaveu_difficile() {
        LocalDate naissance = LocalDate.of(2025, 9, 1);
        PresomptionPaterniteRequest r = req(
                naissance, LocalDate.of(2020, 6, 1), null,
                naissance, false, false, true, true, null);
        PresomptionPaterniteResult res =
                PresomptionPaterniteCalculator.compute(r, "FRANCE", TODAY_2026);
        assertThat(res.voieDesaveu()).isEqualTo("DESAVEU_DIFFICILE_POSSESSION_ETAT");
        assertThat(res.alertes()).anyMatch(a -> a.contains("possession d'état"));
        assertThat(res.alertes()).anyMatch(a -> a.contains("333"));
    }

    // ── AC4 : country=BELGIQUE → IAE ──

    @Test
    void ac4_belgique_throws_illegal_argument() {
        LocalDate naissance = LocalDate.of(2025, 5, 1);
        PresomptionPaterniteRequest r = req(
                naissance, LocalDate.of(2020, 6, 1), null,
                naissance, false, false, false, false, null);
        assertThatThrownBy(() ->
                PresomptionPaterniteCalculator.compute(r, "BELGIQUE", TODAY_2026))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("France");
    }

    // ── Cas complémentaires ──

    @Test
    void conception_avant_180j_sans_possession_avec_desaveu_renversable() {
        LocalDate mariage = LocalDate.of(2025, 1, 1);
        LocalDate naissance = mariage.plusDays(100); // conception avant mariage
        PresomptionPaterniteRequest r = req(
                naissance, mariage, null, naissance,
                true, false, true, false, null);
        PresomptionPaterniteResult res =
                PresomptionPaterniteCalculator.compute(r, "FRANCE", TODAY_2026);
        assertThat(res.presomptionApplicable()).isTrue();
        assertThat(res.presomptionRenversee()).isTrue();
        assertThat(res.messages()).anyMatch(m -> m.contains("180 premiers jours"));
    }

    @Test
    void conception_avant_180j_avec_possession_etat_presomption_maintenue() {
        LocalDate mariage = LocalDate.of(2025, 1, 1);
        LocalDate naissance = mariage.plusDays(100);
        PresomptionPaterniteRequest r = req(
                naissance, mariage, null, naissance,
                true, false, true, true, null);
        PresomptionPaterniteResult res =
                PresomptionPaterniteCalculator.compute(r, "FRANCE", TODAY_2026);
        assertThat(res.presomptionApplicable()).isTrue();
        assertThat(res.presomptionRenversee()).isFalse();
        assertThat(res.messages()).anyMatch(m -> m.contains("314"));
    }

    @Test
    void delai_desaveu_depasse_forclos() {
        LocalDate naissance = TODAY_2026.minusMonths(8); // 8 mois avant aujourd'hui
        PresomptionPaterniteRequest r = req(
                naissance, naissance.minusYears(3), null,
                naissance, false, false, true, false, null);
        PresomptionPaterniteResult res =
                PresomptionPaterniteCalculator.compute(r, "FRANCE", TODAY_2026);
        assertThat(res.voieDesaveu()).isEqualTo("DESAVEU_DELAI_FORCLOS");
        assertThat(res.alertes()).anyMatch(a -> a.contains("Délai 6 mois"));
        assertThat(res.alertes()).anyMatch(a -> a.contains("DÉPASSÉ"));
    }

    @Test
    void delai_desaveu_dans_6_mois_recevable() {
        LocalDate naissance = TODAY_2026.minusMonths(2);
        PresomptionPaterniteRequest r = req(
                naissance, naissance.minusYears(2), null,
                naissance, false, false, true, false, null);
        PresomptionPaterniteResult res =
                PresomptionPaterniteCalculator.compute(r, "FRANCE", TODAY_2026);
        assertThat(res.voieDesaveu()).isEqualTo("DESAVEU_RECEVABLE");
        assertThat(res.delaiDesaveu()).contains("recevable");
    }

    @Test
    void delai_desaveu_point_depart_connaissance_distincte() {
        // Naissance il y a 10 mois mais connaissance il y a 2 mois (Cass. 2014)
        LocalDate naissance = TODAY_2026.minusMonths(10);
        LocalDate connaissance = TODAY_2026.minusMonths(2);
        PresomptionPaterniteRequest r = req(
                naissance, naissance.minusYears(3), null,
                naissance, false, false, true, false, connaissance);
        PresomptionPaterniteResult res =
                PresomptionPaterniteCalculator.compute(r, "FRANCE", TODAY_2026);
        assertThat(res.voieDesaveu()).isEqualTo("DESAVEU_RECEVABLE");
    }

    @Test
    void desaveu_non_envisage_voie_indeterminee() {
        LocalDate naissance = LocalDate.of(2025, 9, 1);
        PresomptionPaterniteRequest r = req(
                naissance, LocalDate.of(2020, 6, 1), null,
                naissance, false, false, false, false, null);
        PresomptionPaterniteResult res =
                PresomptionPaterniteCalculator.compute(r, "FRANCE", TODAY_2026);
        assertThat(res.voieDesaveu()).isEqualTo("INDETERMINE");
    }

    @Test
    void dissolution_avant_mariage_throws() {
        LocalDate mariage = LocalDate.of(2025, 6, 1);
        LocalDate disso = LocalDate.of(2024, 1, 1);
        PresomptionPaterniteRequest r = req(
                LocalDate.of(2025, 12, 1), mariage, disso,
                LocalDate.of(2025, 12, 1), false, false, false, false, null);
        assertThatThrownBy(() ->
                PresomptionPaterniteCalculator.compute(r, "FRANCE", TODAY_2026))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDissolutionMariage");
    }

    @Test
    void date_naissance_null_throws() {
        PresomptionPaterniteRequest r = req(
                null, LocalDate.of(2020, 1, 1), null,
                null, false, false, false, false, null);
        assertThatThrownBy(() ->
                PresomptionPaterniteCalculator.compute(r, "FRANCE", TODAY_2026))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNaissanceEnfant");
    }

    @Test
    void date_mariage_null_throws() {
        PresomptionPaterniteRequest r = req(
                LocalDate.of(2025, 1, 1), null, null,
                null, false, false, false, false, null);
        assertThatThrownBy(() ->
                PresomptionPaterniteCalculator.compute(r, "FRANCE", TODAY_2026))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateConclusionMariage");
    }

    @Test
    void base_legale_inclut_articles_clefs() {
        LocalDate naissance = LocalDate.of(2025, 9, 1);
        PresomptionPaterniteRequest r = req(
                naissance, LocalDate.of(2020, 6, 1), null,
                naissance, false, false, false, false, null);
        PresomptionPaterniteResult res =
                PresomptionPaterniteCalculator.compute(r, "FRANCE", TODAY_2026);
        assertThat(res.baseLegale()).contains("312");
        assertThat(res.baseLegale()).contains("313");
        assertThat(res.baseLegale()).contains("316");
        assertThat(res.baseLegale()).contains("333");
    }

    @Test
    void naissance_dans_fourchette_critique_270_300_alerte() {
        LocalDate disso = LocalDate.of(2025, 1, 1);
        LocalDate naissance = disso.plusDays(295); // dans fourchette critique
        PresomptionPaterniteRequest r = req(
                naissance, LocalDate.of(2020, 6, 1), disso,
                naissance, false, false, false, false, null);
        PresomptionPaterniteResult res =
                PresomptionPaterniteCalculator.compute(r, "FRANCE", TODAY_2026);
        assertThat(res.alertes()).anyMatch(a -> a.contains("270-300 jours"));
    }

    @Test
    void presomption_applicable_par_defaut() {
        LocalDate naissance = LocalDate.of(2025, 9, 1);
        PresomptionPaterniteRequest r = req(
                naissance, LocalDate.of(2020, 6, 1), null,
                naissance, false, false, false, false, null);
        PresomptionPaterniteResult res =
                PresomptionPaterniteCalculator.compute(r, "FRANCE", TODAY_2026);
        assertThat(res.presomptionApplicable()).isTrue();
        assertThat(res.messages()).anyMatch(m -> m.contains("art. 312"));
    }

    @Test
    void accouchement_distinct_naissance_message() {
        LocalDate naissance = LocalDate.of(2025, 9, 1);
        LocalDate accouchement = LocalDate.of(2025, 8, 28);
        PresomptionPaterniteRequest r = req(
                naissance, LocalDate.of(2020, 6, 1), null,
                accouchement, false, false, false, false, null);
        PresomptionPaterniteResult res =
                PresomptionPaterniteCalculator.compute(r, "FRANCE", TODAY_2026);
        assertThat(res.messages()).anyMatch(m -> m.contains("accouchement"));
    }
}
