package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Belgian9terCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Brussels");
    private static final LocalDate TODAY = LocalDate.of(2026, 4, 24);

    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    @Test
    void compute_allCriteriaMet_verdictEleve_score100() {
        Belgian9terResult r = Belgian9terCalculator.compute(
                LocalDate.of(2025, 1, 10),
                true, true, true, false,
                LocalDate.of(2026, 3, 1),
                clockAt(TODAY));

        assertThat(r.certificatMedicalType1Ok()).isTrue();
        assertThat(r.soinsRequisOk()).isTrue();
        assertThat(r.inaccessibiliteOk()).isTrue();
        assertThat(r.pasMenace()).isTrue();
        assertThat(r.scoreGlobal()).isEqualTo(100);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("ELEVEE");
        assertThat(r.criteresNonRemplis()).isEmpty();
        assertThat(r.dateExpirationInstruction()).isEqualTo(LocalDate.of(2027, 3, 1));
        assertThat(r.baseJuridique())
                .contains("art. 9ter")
                .contains("Loi 15/12/1980");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("Certificat médical type à actualiser tous les 6 mois"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("médecin-fonctionnaire délégué"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("recours CCE annulation dans 30 jours"));
    }

    @Test
    void compute_allCriteriaOkNoDates_verdictElevee_score100() {
        Belgian9terResult r = Belgian9terCalculator.compute(
                null,
                true, true, true, false, null,
                clockAt(TODAY));

        assertThat(r.scoreGlobal()).isEqualTo(100);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("ELEVEE");
        assertThat(r.dateExpirationInstruction()).isNull();
    }

    @Test
    void compute_certAndSoinsOkSansInaccessibiliteSansMenace_score75Elevee() {
        Belgian9terResult r = Belgian9terCalculator.compute(
                null,
                true, true, false, false, null,
                clockAt(TODAY));

        // cert 25 + soins 25 + !menace 25 = 75
        assertThat(r.scoreGlobal()).isEqualTo(75);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("ELEVEE");
        assertThat(r.criteresNonRemplis())
                .anyMatch(c -> c.contains("Inaccessibilité"));
    }

    @Test
    void compute_certOnlyOkSansMenace_score50Moyenne() {
        Belgian9terResult r = Belgian9terCalculator.compute(
                null,
                true, false, false, false, null,
                clockAt(TODAY));

        // cert 25 + !menace 25 = 50
        assertThat(r.scoreGlobal()).isEqualTo(50);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_allFlagsFalseNoMenace_score25Faible() {
        Belgian9terResult r = Belgian9terCalculator.compute(
                null,
                false, false, false, false, null,
                clockAt(TODAY));

        // !menace 25 = 25
        assertThat(r.scoreGlobal()).isEqualTo(25);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).hasSize(3);
    }

    @Test
    void compute_menaceOrdrePublic_pasMenaceFalse_score75() {
        Belgian9terResult r = Belgian9terCalculator.compute(
                null,
                true, true, true, true, null,
                clockAt(TODAY));

        assertThat(r.pasMenace()).isFalse();
        assertThat(r.scoreGlobal()).isEqualTo(75);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("ELEVEE");
        assertThat(r.criteresNonRemplis()).contains("Menace ordre public présumée");
        assertThat(r.formule()).contains("menace ordre public");
    }

    @Test
    void compute_allFalseWithMenace_verdictFaible() {
        Belgian9terResult r = Belgian9terCalculator.compute(
                null,
                false, false, false, true, null,
                clockAt(TODAY));

        assertThat(r.pasMenace()).isFalse();
        assertThat(r.scoreGlobal()).isEqualTo(0);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("FAIBLE");
    }

    @Test
    void compute_dateExpirationInstruction_nullIfNoDepot() {
        Belgian9terResult r = Belgian9terCalculator.compute(
                null,
                true, true, true, false, null,
                clockAt(TODAY));
        assertThat(r.dateExpirationInstruction()).isNull();
    }

    @Test
    void compute_dateExpirationInstruction_depotPlus12Months() {
        Belgian9terResult r = Belgian9terCalculator.compute(
                null,
                true, true, true, false,
                LocalDate.of(2026, 2, 10),
                clockAt(TODAY));
        assertThat(r.dateExpirationInstruction()).isEqualTo(LocalDate.of(2027, 2, 10));
    }

    @Test
    void compute_futureDateDebutSymptomes_throws() {
        assertThatThrownBy(() -> Belgian9terCalculator.compute(
                LocalDate.of(2027, 1, 1),
                true, true, true, false, null, clockAt(TODAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDebutSymptomes");
    }

    @Test
    void compute_futureDateDepotDemande_throws() {
        assertThatThrownBy(() -> Belgian9terCalculator.compute(
                null,
                true, true, true, false,
                LocalDate.of(2027, 1, 1), clockAt(TODAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDepotDemande");
    }

    @Test
    void compute_dateDepotBeforeDateDebutSymptomes_throws() {
        assertThatThrownBy(() -> Belgian9terCalculator.compute(
                LocalDate.of(2025, 6, 1),
                true, true, true, false,
                LocalDate.of(2025, 1, 1), clockAt(TODAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDepotDemande");
    }

    @Test
    void compute_formule_containsScoreAndVerdict() {
        Belgian9terResult r = Belgian9terCalculator.compute(
                null,
                true, true, true, false,
                LocalDate.of(2026, 3, 1),
                clockAt(TODAY));

        assertThat(r.formule())
                .contains("9ter")
                .contains("art. 9ter Loi 15/12/1980")
                .contains(String.valueOf(r.scoreGlobal()))
                .contains(r.verdictProbabiliteAcceptation())
                .contains("2027-03-01");
    }

    @Test
    void compute_sansCertificat_messageIncluded() {
        Belgian9terResult r = Belgian9terCalculator.compute(
                null,
                false, true, true, false, null,
                clockAt(TODAY));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("CM 1 et 2"));
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Certificat médical"));
    }

    @Test
    void compute_sansInaccessibilite_messagesRecommandations() {
        Belgian9terResult r = Belgian9terCalculator.compute(
                null,
                true, true, false, false, null,
                clockAt(TODAY));

        assertThat(r.messages())
                .anySatisfy(m -> assertThat(m).contains("inaccessibilité"))
                .anySatisfy(m -> assertThat(m).contains("pays d'origine"));
    }
}
