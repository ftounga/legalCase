package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesHumanitaireCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");
    private static final LocalDate TODAY = LocalDate.of(2026, 4, 24);

    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    @Test
    void compute_victimeViolences_avecPreuves_verdictEleve() {
        AesHumanitaireResult r = AesHumanitaireCalculator.compute(
                LocalDate.of(2022, 1, 10),
                MotifHumanitaire.VICTIME_VIOLENCES,
                false,
                true,
                false,
                true,
                false,
                LocalDate.of(2026, 4, 1),
                clockAt(TODAY));

        assertThat(r.motifEligible()).isTrue();
        assertThat(r.preuvesAdaptees()).isTrue();
        assertThat(r.pasMenace()).isTrue();
        assertThat(r.commissionRequise()).isTrue();
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("ELEVEE");
        assertThat(r.scoreGlobal()).isGreaterThanOrEqualTo(70);
        assertThat(r.criteresNonRemplis()).isEmpty();
        assertThat(r.dateExpirationInstruction()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(r.baseJuridique()).contains("L.435-2").contains("L.432-14");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("CIDFF"));
    }

    @Test
    void compute_victimeTraite_avecPreuves_bonusElevee() {
        AesHumanitaireResult r = AesHumanitaireCalculator.compute(
                LocalDate.of(2023, 1, 10),
                MotifHumanitaire.VICTIME_TRAITE,
                false,
                true,
                false,
                true,
                false,
                null,
                clockAt(TODAY));

        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("ELEVEE");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("L.425-1"));
    }

    @Test
    void compute_medicalPrecaire_sansPreuves_verdictFaible() {
        AesHumanitaireResult r = AesHumanitaireCalculator.compute(
                LocalDate.of(2022, 1, 10),
                MotifHumanitaire.SITUATION_MEDICALE_PRECAIRE_HORS_L425_9,
                false,
                false,
                false,
                false,
                false,
                null,
                clockAt(TODAY));

        assertThat(r.preuvesAdaptees()).isFalse();
        assertThat(r.commissionRequise()).isTrue();
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis())
                .anyMatch(c -> c.contains("Preuves non adaptées"));
        assertThat(r.criteresNonRemplis())
                .anyMatch(c -> c.contains("Commission"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("L.425-9"));
    }

    @Test
    void compute_medicalPrecaire_avecPreuvesEtCommission_verdictElevee() {
        AesHumanitaireResult r = AesHumanitaireCalculator.compute(
                LocalDate.of(2021, 6, 1),
                MotifHumanitaire.SITUATION_MEDICALE_PRECAIRE_HORS_L425_9,
                true,
                false,
                false,
                true,
                false,
                LocalDate.of(2026, 3, 1),
                clockAt(TODAY));

        assertThat(r.preuvesAdaptees()).isTrue();
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("ELEVEE");
        assertThat(r.dateExpirationInstruction()).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    void compute_risquesAuRetour_sansAsile_proposeAsileEnAlternative() {
        AesHumanitaireResult r = AesHumanitaireCalculator.compute(
                LocalDate.of(2023, 1, 10),
                MotifHumanitaire.RISQUES_AU_RETOUR,
                false,
                false,
                false,
                false,
                false,
                null,
                clockAt(TODAY));

        assertThat(r.preuvesAdaptees()).isTrue();
        assertThat(r.commissionRequise()).isTrue();
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("OFPRA"));
    }

    @Test
    void compute_risquesAuRetour_asileRejete_bonusScore() {
        AesHumanitaireResult r = AesHumanitaireCalculator.compute(
                LocalDate.of(2022, 1, 10),
                MotifHumanitaire.RISQUES_AU_RETOUR,
                false,
                false,
                true,
                true,
                false,
                null,
                clockAt(TODAY));

        assertThat(r.scoreGlobal()).isGreaterThanOrEqualTo(70);
        assertThat(r.messages()).noneMatch(m -> m.contains("OFPRA"));
    }

    @Test
    void compute_isolementTotal_commissionNonRequise() {
        AesHumanitaireResult r = AesHumanitaireCalculator.compute(
                LocalDate.of(2022, 1, 10),
                MotifHumanitaire.ISOLEMENT_TOTAL,
                false,
                false,
                false,
                false,
                false,
                null,
                clockAt(TODAY));

        assertThat(r.preuvesAdaptees()).isTrue();
        assertThat(r.commissionRequise()).isFalse();
        assertThat(r.criteresNonRemplis()).noneMatch(c -> c.contains("Commission"));
    }

    @Test
    void compute_autreHumanitaire_sansPreuves_motifNonEligible() {
        AesHumanitaireResult r = AesHumanitaireCalculator.compute(
                LocalDate.of(2022, 1, 10),
                MotifHumanitaire.AUTRE_HUMANITAIRE,
                false,
                false,
                false,
                false,
                false,
                null,
                clockAt(TODAY));

        assertThat(r.motifEligible()).isFalse();
        assertThat(r.preuvesAdaptees()).isFalse();
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis())
                .anyMatch(c -> c.contains("Motif humanitaire insuffisamment caractérisé"));
    }

    @Test
    void compute_autreHumanitaire_avecPreuvesViolences_devientEligible() {
        AesHumanitaireResult r = AesHumanitaireCalculator.compute(
                LocalDate.of(2022, 1, 10),
                MotifHumanitaire.AUTRE_HUMANITAIRE,
                false,
                true,
                false,
                false,
                false,
                null,
                clockAt(TODAY));

        assertThat(r.motifEligible()).isTrue();
        assertThat(r.preuvesAdaptees()).isTrue();
    }

    @Test
    void compute_menaceOrdrePublic_scoreZeroEtVerdictFaible() {
        AesHumanitaireResult r = AesHumanitaireCalculator.compute(
                LocalDate.of(2022, 1, 10),
                MotifHumanitaire.VICTIME_VIOLENCES,
                false,
                true,
                false,
                true,
                true,
                null,
                clockAt(TODAY));

        assertThat(r.pasMenace()).isFalse();
        assertThat(r.scoreGlobal()).isEqualTo(0);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).contains("Menace ordre public présumée");
        assertThat(r.formule()).contains("menace ordre public");
    }

    @Test
    void compute_commissionRequiseMaisNonSaisie_penaliteScore() {
        AesHumanitaireResult rSansCommission = AesHumanitaireCalculator.compute(
                LocalDate.of(2022, 1, 10),
                MotifHumanitaire.VICTIME_VIOLENCES,
                false,
                true,
                false,
                false,
                false,
                null,
                clockAt(TODAY));

        AesHumanitaireResult rAvecCommission = AesHumanitaireCalculator.compute(
                LocalDate.of(2022, 1, 10),
                MotifHumanitaire.VICTIME_VIOLENCES,
                false,
                true,
                false,
                true,
                false,
                null,
                clockAt(TODAY));

        assertThat(rSansCommission.scoreGlobal())
                .isLessThan(rAvecCommission.scoreGlobal());
        assertThat(rSansCommission.criteresNonRemplis())
                .anyMatch(c -> c.contains("Commission"));
    }

    @Test
    void compute_dateExpirationInstruction_nullIfNoDepot() {
        AesHumanitaireResult r = AesHumanitaireCalculator.compute(
                LocalDate.of(2022, 1, 10),
                MotifHumanitaire.ISOLEMENT_TOTAL,
                false,
                false,
                false,
                false,
                false,
                null,
                clockAt(TODAY));
        assertThat(r.dateExpirationInstruction()).isNull();
    }

    @Test
    void compute_dateExpirationInstruction_depotPlus6Months() {
        AesHumanitaireResult r = AesHumanitaireCalculator.compute(
                LocalDate.of(2022, 1, 10),
                MotifHumanitaire.ISOLEMENT_TOTAL,
                false,
                false,
                false,
                false,
                false,
                LocalDate.of(2026, 2, 10),
                clockAt(TODAY));
        assertThat(r.dateExpirationInstruction()).isEqualTo(LocalDate.of(2026, 8, 10));
    }

    @Test
    void compute_nullDateEntree_throws() {
        assertThatThrownBy(() -> AesHumanitaireCalculator.compute(
                null,
                MotifHumanitaire.ISOLEMENT_TOTAL,
                false, false, false, false, false, null, clockAt(TODAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateEntreeFrance");
    }

    @Test
    void compute_futureDateEntree_throws() {
        assertThatThrownBy(() -> AesHumanitaireCalculator.compute(
                LocalDate.of(2027, 1, 1),
                MotifHumanitaire.ISOLEMENT_TOTAL,
                false, false, false, false, false, null, clockAt(TODAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void compute_nullMotif_throws() {
        assertThatThrownBy(() -> AesHumanitaireCalculator.compute(
                LocalDate.of(2022, 1, 10),
                null,
                false, false, false, false, false, null, clockAt(TODAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifHumanitaireDominant");
    }

    @Test
    void compute_dateDepotBeforeEntree_throws() {
        assertThatThrownBy(() -> AesHumanitaireCalculator.compute(
                LocalDate.of(2022, 1, 10),
                MotifHumanitaire.ISOLEMENT_TOTAL,
                false, false, false, false, false,
                LocalDate.of(2021, 1, 1),
                clockAt(TODAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDepotDemande");
    }

    @Test
    void compute_formule_containsScoreAndVerdict() {
        AesHumanitaireResult r = AesHumanitaireCalculator.compute(
                LocalDate.of(2022, 1, 10),
                MotifHumanitaire.VICTIME_TRAITE,
                false, true, false, true, false,
                LocalDate.of(2026, 3, 1),
                clockAt(TODAY));

        assertThat(r.formule())
                .contains("L.435-2")
                .contains("VICTIME_TRAITE")
                .contains(String.valueOf(r.scoreGlobal()))
                .contains(r.verdictProbabiliteAcceptation())
                .contains("2026-09-01");
    }
}
