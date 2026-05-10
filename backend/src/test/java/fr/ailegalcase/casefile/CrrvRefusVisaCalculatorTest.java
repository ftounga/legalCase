package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-208-03 : tests unitaires de {@link CrrvRefusVisaCalculator}.
 */
class CrrvRefusVisaCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    @Test
    void compute_nominal_disponible() {
        // notif 2026-04-15, today same → expire 2026-06-15, ~61j → DISPONIBLE
        CrrvRefusVisaResult r = CrrvRefusVisaCalculator.compute(
                LocalDate.of(2026, 4, 15),
                "LONG_SEJOUR",
                "Ressources insuffisantes",
                false,
                null,
                clockAt(LocalDate.of(2026, 4, 15)));

        assertThat(r.typeVisa()).isEqualTo("LONG_SEJOUR");
        assertThat(r.motifRefus()).isEqualTo("Ressources insuffisantes");
        assertThat(r.dateExpirationRecoursCrrv()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(r.statut()).isEqualTo("DISPONIBLE");
        assertThat(r.baseJuridique()).contains("L.312").contains("Nantes");
    }

    @Test
    void compute_urgent_when7DaysLeft() {
        // notif 2026-04-15, today 2026-06-08 → reste 7 j → URGENT
        CrrvRefusVisaResult r = CrrvRefusVisaCalculator.compute(
                LocalDate.of(2026, 4, 15),
                "COURT_SEJOUR",
                "Doute identité",
                false,
                null,
                clockAt(LocalDate.of(2026, 6, 8)));

        assertThat(r.joursRestants()).isEqualTo(7L);
        assertThat(r.statut()).isEqualTo("URGENT");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("URGENT"));
    }

    @Test
    void compute_expire_pastDeadline() {
        CrrvRefusVisaResult r = CrrvRefusVisaCalculator.compute(
                LocalDate.of(2026, 1, 15),
                "ETUDIANT",
                "Pré-inscription manquante",
                false,
                null,
                clockAt(LocalDate.of(2026, 5, 1)));

        assertThat(r.joursRestants()).isNegative();
        assertThat(r.statut()).isEqualTo("EXPIRE");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("EXPIRÉ"));
    }

    @Test
    void compute_recoursForme_setsStatut() {
        CrrvRefusVisaResult r = CrrvRefusVisaCalculator.compute(
                LocalDate.of(2026, 4, 15),
                "REGROUPEMENT_FAMILIAL",
                "Ressources OFII",
                true,
                LocalDate.of(2026, 5, 1),
                clockAt(LocalDate.of(2026, 5, 5)));

        assertThat(r.statut()).isEqualTo("RECOURS_FORME");
        assertThat(r.joursRestants()).isZero();
        assertThat(r.dateRecours()).isEqualTo(LocalDate.of(2026, 5, 1));
    }

    @Test
    void compute_typeRegroupement_addsCedhMessage() {
        CrrvRefusVisaResult r = CrrvRefusVisaCalculator.compute(
                LocalDate.of(2026, 4, 15),
                "REGROUPEMENT_FAMILIAL",
                "Conditions logement",
                false,
                null,
                clockAt(LocalDate.of(2026, 4, 20)));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("CEDH"));
    }

    @Test
    void compute_allTypesAccepted() {
        for (String t : java.util.List.of(
                "COURT_SEJOUR", "LONG_SEJOUR", "REGROUPEMENT_FAMILIAL", "ETUDIANT", "AUTRE")) {
            CrrvRefusVisaResult r = CrrvRefusVisaCalculator.compute(
                    LocalDate.of(2026, 4, 15),
                    t,
                    "test",
                    false,
                    null,
                    clockAt(LocalDate.of(2026, 4, 20)));
            assertThat(r.typeVisa()).isEqualTo(t);
            assertThat(r.messages()).isNotEmpty();
        }
    }

    @Test
    void compute_futureNotification_throws() {
        assertThatThrownBy(() -> CrrvRefusVisaCalculator.compute(
                LocalDate.of(2026, 6, 1),
                "LONG_SEJOUR",
                null,
                false,
                null,
                clockAt(LocalDate.of(2026, 5, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void compute_unknownType_throws() {
        assertThatThrownBy(() -> CrrvRefusVisaCalculator.compute(
                LocalDate.of(2026, 4, 15),
                "TYPE_BIDON",
                null,
                false,
                null,
                clockAt(LocalDate.of(2026, 4, 20))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeVisa");
    }

    @Test
    void compute_nullNotification_throws() {
        assertThatThrownBy(() -> CrrvRefusVisaCalculator.compute(
                null,
                "LONG_SEJOUR",
                null,
                false,
                null,
                clockAt(LocalDate.of(2026, 5, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNotificationRefus");
    }

    @Test
    void compute_motifNullAccepted() {
        CrrvRefusVisaResult r = CrrvRefusVisaCalculator.compute(
                LocalDate.of(2026, 4, 15),
                "LONG_SEJOUR",
                null,
                false,
                null,
                clockAt(LocalDate.of(2026, 4, 20)));

        assertThat(r.motifRefus()).isNull();
        assertThat(r.formule()).contains("n.r.");
    }
}
