package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DivorceDesunionIrremediableBeCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Brussels");

    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    @Test
    void compute_consentue_separation7mois_preuvesPlusDoc_pasReconciliation_elevee() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        DivorceDesunionIrremediableBeResult r = DivorceDesunionIrremediableBeCalculator.compute(
                LocalDate.of(2025, 9, 25), // ~7 mois
                true,  // consentue
                true,  // preuves séparation
                true,  // preuves documentaires
                false, // pas réconciliation
                null,
                clockAt(today));

        assertThat(r.dureeSeparationMois()).isEqualTo(7);
        assertThat(r.seuilSeparationMois()).isEqualTo(6);
        assertThat(r.delaiObjectifOk()).isTrue();
        assertThat(r.conditionsReunies()).isTrue();
        assertThat(r.scoreGlobal()).isEqualTo(100); // 40+30+15+15
        assertThat(r.verdictProbabilite()).isEqualTo("ELEVEE");
        assertThat(r.baseJuridique()).contains("229").contains("Code civil belge");
        assertThat(r.formule()).contains("ELEVEE").contains("100/100").contains("consentue");
    }

    @Test
    void compute_unilaterale_separation13mois_preuvesPlusDoc_pasReconciliation_elevee() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        DivorceDesunionIrremediableBeResult r = DivorceDesunionIrremediableBeCalculator.compute(
                LocalDate.of(2025, 3, 25), // 13 mois
                false, // unilatérale
                true, true, false,
                null,
                clockAt(today));

        assertThat(r.dureeSeparationMois()).isEqualTo(13);
        assertThat(r.seuilSeparationMois()).isEqualTo(12);
        assertThat(r.delaiObjectifOk()).isTrue();
        assertThat(r.conditionsReunies()).isTrue();
        assertThat(r.scoreGlobal()).isEqualTo(100);
        assertThat(r.verdictProbabilite()).isEqualTo("ELEVEE");
    }

    @Test
    void compute_consentue_separation4mois_delaiKo() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        DivorceDesunionIrremediableBeResult r = DivorceDesunionIrremediableBeCalculator.compute(
                LocalDate.of(2025, 12, 25), // 4 mois
                true, true, true, false,
                null,
                clockAt(today));

        assertThat(r.dureeSeparationMois()).isEqualTo(4);
        assertThat(r.seuilSeparationMois()).isEqualTo(6);
        assertThat(r.delaiObjectifOk()).isFalse();
        assertThat(r.conditionsReunies()).isFalse();
        // 0 + 30 + 15 + 15 = 60 → MOYENNE
        assertThat(r.scoreGlobal()).isEqualTo(60);
        assertThat(r.verdictProbabilite()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_unilaterale_separation8mois_entre6et12_delaiKo() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        DivorceDesunionIrremediableBeResult r = DivorceDesunionIrremediableBeCalculator.compute(
                LocalDate.of(2025, 8, 25), // 8 mois
                false, // unilatérale → seuil 12
                true, true, false,
                null,
                clockAt(today));

        assertThat(r.dureeSeparationMois()).isEqualTo(8);
        assertThat(r.seuilSeparationMois()).isEqualTo(12);
        assertThat(r.delaiObjectifOk()).isFalse();
        // Message conseillant la demande conjointe
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("conjointe"));
    }

    @Test
    void compute_unilaterale_separation12moisPile_delaiOk() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        DivorceDesunionIrremediableBeResult r = DivorceDesunionIrremediableBeCalculator.compute(
                LocalDate.of(2025, 4, 25), // 12 mois pile
                false,
                true, true, false,
                null,
                clockAt(today));

        assertThat(r.dureeSeparationMois()).isEqualTo(12);
        assertThat(r.delaiObjectifOk()).isTrue();
        assertThat(r.scoreGlobal()).isEqualTo(100);
    }

    @Test
    void compute_dateAssignation_utiliseePourCalculDelai() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        // Séparation 2024-01-01, assignation 2024-04-01 → 3 mois (< 6)
        DivorceDesunionIrremediableBeResult r = DivorceDesunionIrremediableBeCalculator.compute(
                LocalDate.of(2024, 1, 1),
                true,
                true, true, false,
                LocalDate.of(2024, 4, 1),
                clockAt(today));

        assertThat(r.dureeSeparationMois()).isEqualTo(3);
        assertThat(r.delaiObjectifOk()).isFalse();
    }

    @Test
    void compute_tentativesReconciliation_avecPreuvesDoc_conditionsReunies() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        // Tentatives réconciliation + preuves doc → fallback OK
        DivorceDesunionIrremediableBeResult r = DivorceDesunionIrremediableBeCalculator.compute(
                LocalDate.of(2025, 9, 25), // 7 mois
                true,
                true,
                true,  // preuvesDoc
                true,  // réconciliation
                null,
                clockAt(today));

        assertThat(r.conditionsReunies()).isTrue();
        // 40 + 30 + 15 + 0 = 85
        assertThat(r.scoreGlobal()).isEqualTo(85);
        assertThat(r.verdictProbabilite()).isEqualTo("ELEVEE");
    }

    @Test
    void compute_tentativesReconciliation_sansPreuvesDoc_conditionsKo() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        DivorceDesunionIrremediableBeResult r = DivorceDesunionIrremediableBeCalculator.compute(
                LocalDate.of(2025, 9, 25),
                true,
                true,
                false, // pas preuvesDoc
                true,  // réconciliation
                null,
                clockAt(today));

        assertThat(r.conditionsReunies()).isFalse();
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("réconciliation"));
    }

    @Test
    void compute_pasPreuvesSeparation_conditionsKo_messageAjoute() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        DivorceDesunionIrremediableBeResult r = DivorceDesunionIrremediableBeCalculator.compute(
                LocalDate.of(2025, 9, 25),
                true,
                false, // pas preuves séparation
                true, false,
                null,
                clockAt(today));

        assertThat(r.conditionsReunies()).isFalse();
        // 40 + 0 + 15 + 15 = 70 → MOYENNE
        assertThat(r.scoreGlobal()).isEqualTo(70);
        assertThat(r.verdictProbabilite()).isEqualTo("MOYENNE");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("Preuves de séparation insuffisantes"));
    }

    @Test
    void compute_verdict_seuils() {
        LocalDate today = LocalDate.of(2026, 4, 25);

        // ELEVEE : score 75 minimum (40+30 + 15-... OR 40 + 30 + 15 = 85)
        DivorceDesunionIrremediableBeResult rElevee = DivorceDesunionIrremediableBeCalculator.compute(
                LocalDate.of(2025, 9, 25), true, true, true, true, null, clockAt(today));
        assertThat(rElevee.scoreGlobal()).isEqualTo(85); // 40+30+15+0
        assertThat(rElevee.verdictProbabilite()).isEqualTo("ELEVEE");

        // MOYENNE : 40 + 15 = 55
        DivorceDesunionIrremediableBeResult rMoyenne = DivorceDesunionIrremediableBeCalculator.compute(
                LocalDate.of(2025, 9, 25), true, false, true, true, null, clockAt(today));
        assertThat(rMoyenne.scoreGlobal()).isEqualTo(55); // 40+0+15+0
        assertThat(rMoyenne.verdictProbabilite()).isEqualTo("MOYENNE");

        // FAIBLE : 30 (preuves séparation seules)
        DivorceDesunionIrremediableBeResult rFaible = DivorceDesunionIrremediableBeCalculator.compute(
                LocalDate.of(2026, 1, 25), false, true, false, true, null, clockAt(today));
        assertThat(rFaible.scoreGlobal()).isEqualTo(30); // 0+30+0+0
        assertThat(rFaible.verdictProbabilite()).isEqualTo("FAIBLE");
    }

    @Test
    void compute_messages_incluentBaseJuridiqueEtArt301() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        DivorceDesunionIrremediableBeResult r = DivorceDesunionIrremediableBeCalculator.compute(
                LocalDate.of(2025, 9, 25), true, true, true, false, null, clockAt(today));

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("art. 229 §3"));
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("Loi du 27/04/2007"));
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("art. 301"));
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("Registre national"));
    }

    @Test
    void compute_consentueDelaiKo_messageAlternative230() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        DivorceDesunionIrremediableBeResult r = DivorceDesunionIrremediableBeCalculator.compute(
                LocalDate.of(2026, 1, 25), // 3 mois
                true,
                true, false, false,
                null,
                clockAt(today));

        assertThat(r.delaiObjectifOk()).isFalse();
        // Message alternative consentement mutuel art. 230
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("art. 230"));
    }

    @Test
    void compute_allKo_scoreZero_faible() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        DivorceDesunionIrremediableBeResult r = DivorceDesunionIrremediableBeCalculator.compute(
                LocalDate.of(2026, 3, 25), // 1 mois
                false, // unilatérale
                false, false, true, // tout KO
                null,
                clockAt(today));

        assertThat(r.scoreGlobal()).isEqualTo(0);
        assertThat(r.verdictProbabilite()).isEqualTo("FAIBLE");
        assertThat(r.conditionsReunies()).isFalse();
    }

    @Test
    void compute_nullDateSeparation_throws() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        assertThatThrownBy(() -> DivorceDesunionIrremediableBeCalculator.compute(
                null, true, true, true, false, null, clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateSeparation");
    }

    @Test
    void compute_futureDateSeparation_throws() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        assertThatThrownBy(() -> DivorceDesunionIrremediableBeCalculator.compute(
                LocalDate.of(2027, 1, 1), true, true, true, false, null, clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void compute_dateAssignationAvantDateSeparation_throws() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        assertThatThrownBy(() -> DivorceDesunionIrremediableBeCalculator.compute(
                LocalDate.of(2025, 6, 1),
                true, true, true, false,
                LocalDate.of(2025, 1, 1),
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateAssignation");
    }

    @Test
    void compute_baseJuridique_referenceLoi27042007() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        DivorceDesunionIrremediableBeResult r = DivorceDesunionIrremediableBeCalculator.compute(
                LocalDate.of(2025, 9, 25), true, true, true, false, null, clockAt(today));

        assertThat(r.baseJuridique())
                .contains("229 §3")
                .contains("Code civil belge")
                .contains("Loi 27/04/2007");
    }

    @Test
    void compute_formule_indiqueTypeDemandeEtComparaison() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        // Séparation 7 mois consentue → "≥ 6 mois (consentue)"
        DivorceDesunionIrremediableBeResult r = DivorceDesunionIrremediableBeCalculator.compute(
                LocalDate.of(2025, 9, 25), true, true, true, false, null, clockAt(today));
        assertThat(r.formule()).contains("7 mois").contains("≥ seuil 6").contains("consentue");

        // Séparation 8 mois unilatérale → "< 12 mois (unilatérale)"
        DivorceDesunionIrremediableBeResult r2 = DivorceDesunionIrremediableBeCalculator.compute(
                LocalDate.of(2025, 8, 25), false, true, true, false, null, clockAt(today));
        assertThat(r2.formule()).contains("8 mois").contains("< seuil 12").contains("unilatérale");
    }
}
