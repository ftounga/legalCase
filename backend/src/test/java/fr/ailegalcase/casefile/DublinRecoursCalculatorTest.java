package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-208-02 : tests unitaires de {@link DublinRecoursCalculator}.
 */
class DublinRecoursCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    @Test
    void compute_nominal_disponible() {
        // notif 2026-05-04, today same day → expire 2026-05-11, reste 7 j → DISPONIBLE
        DublinRecoursResult r = DublinRecoursCalculator.compute(
                LocalDate.of(2026, 5, 4),
                "ITALIE",
                "DEMANDE_ASILE_AUTRE_ETAT",
                false,
                null,
                clockAt(LocalDate.of(2026, 5, 4)));

        assertThat(r.motifTransfert()).isEqualTo("DEMANDE_ASILE_AUTRE_ETAT");
        assertThat(r.etatMembreResponsable()).isEqualTo("ITALIE");
        assertThat(r.dateExpirationRecours()).isEqualTo(LocalDate.of(2026, 5, 11));
        assertThat(r.dateLimiteTransfertEffectif()).isEqualTo(LocalDate.of(2026, 11, 4));
        assertThat(r.joursRestants()).isEqualTo(7L);
        assertThat(r.statut()).isEqualTo("DISPONIBLE");
        assertThat(r.effetSuspensif()).isEqualTo("AUTOMATIQUE");
        assertThat(r.baseJuridique()).contains("L.572-1").contains("604/2013");
    }

    @Test
    void compute_urgent_when2DaysLeft() {
        // notif 2026-05-04, today 2026-05-09 → reste 2 j → URGENT
        DublinRecoursResult r = DublinRecoursCalculator.compute(
                LocalDate.of(2026, 5, 4),
                "ALLEMAGNE",
                "VISA_DELIVRE_AUTRE_ETAT",
                false,
                null,
                clockAt(LocalDate.of(2026, 5, 9)));

        assertThat(r.joursRestants()).isEqualTo(2L);
        assertThat(r.statut()).isEqualTo("URGENT");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("URGENT"));
    }

    @Test
    void compute_expire_pastDeadline() {
        DublinRecoursResult r = DublinRecoursCalculator.compute(
                LocalDate.of(2026, 4, 1),
                "ESPAGNE",
                "ENTREE_IRREGULIERE_AUTRE_ETAT",
                false,
                null,
                clockAt(LocalDate.of(2026, 5, 4)));

        assertThat(r.joursRestants()).isNegative();
        assertThat(r.statut()).isEqualTo("EXPIRE");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("EXPIRÉ"));
    }

    @Test
    void compute_recoursForme_setsStatut() {
        DublinRecoursResult r = DublinRecoursCalculator.compute(
                LocalDate.of(2026, 5, 4),
                "ITALIE",
                "DEMANDE_ASILE_AUTRE_ETAT",
                true,
                LocalDate.of(2026, 5, 8),
                clockAt(LocalDate.of(2026, 5, 10)));

        assertThat(r.statut()).isEqualTo("RECOURS_FORME");
        assertThat(r.joursRestants()).isZero();
        assertThat(r.dateRecours()).isEqualTo(LocalDate.of(2026, 5, 8));
    }

    @Test
    void compute_motifFamille_addsHumanitaireMessage() {
        DublinRecoursResult r = DublinRecoursCalculator.compute(
                LocalDate.of(2026, 5, 4),
                "BELGIQUE",
                "MEMBRE_FAMILLE_AUTRE_ETAT",
                false,
                null,
                clockAt(LocalDate.of(2026, 5, 4)));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("famille"));
    }

    @Test
    void compute_allMotifsAccepted() {
        for (String motif : java.util.List.of(
                "DEMANDE_ASILE_AUTRE_ETAT", "VISA_DELIVRE_AUTRE_ETAT",
                "ENTREE_IRREGULIERE_AUTRE_ETAT", "MEMBRE_FAMILLE_AUTRE_ETAT", "AUTRE")) {
            DublinRecoursResult r = DublinRecoursCalculator.compute(
                    LocalDate.of(2026, 5, 4),
                    "ITALIE",
                    motif,
                    false,
                    null,
                    clockAt(LocalDate.of(2026, 5, 4)));
            assertThat(r.motifTransfert()).isEqualTo(motif);
            assertThat(r.messages()).isNotEmpty();
        }
    }

    @Test
    void compute_futureNotification_throws() {
        assertThatThrownBy(() -> DublinRecoursCalculator.compute(
                LocalDate.of(2026, 6, 1),
                "ITALIE",
                "DEMANDE_ASILE_AUTRE_ETAT",
                false,
                null,
                clockAt(LocalDate.of(2026, 5, 4))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void compute_unknownMotif_throws() {
        assertThatThrownBy(() -> DublinRecoursCalculator.compute(
                LocalDate.of(2026, 5, 4),
                "ITALIE",
                "MOTIF_BIDON",
                false,
                null,
                clockAt(LocalDate.of(2026, 5, 4))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifTransfert");
    }

    @Test
    void compute_nullNotification_throws() {
        assertThatThrownBy(() -> DublinRecoursCalculator.compute(
                null,
                "ITALIE",
                "DEMANDE_ASILE_AUTRE_ETAT",
                false,
                null,
                clockAt(LocalDate.of(2026, 5, 4))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNotificationDecisionTransfert");
    }

    @Test
    void compute_recoursFormeWithoutDate_throws() {
        assertThatThrownBy(() -> DublinRecoursCalculator.compute(
                LocalDate.of(2026, 5, 4),
                "ITALIE",
                "DEMANDE_ASILE_AUTRE_ETAT",
                true,
                null,
                clockAt(LocalDate.of(2026, 5, 8))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateRecours");
    }
}
