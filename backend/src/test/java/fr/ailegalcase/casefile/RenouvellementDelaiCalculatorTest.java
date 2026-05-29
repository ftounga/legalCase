package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-214-13 : tests unitaires du {@link RenouvellementDelaiCalculator}
 * (date du jour fixée). Couvre les statuts EN_AVANCE / A_DEPOSER /
 * A_DEPOSER_URGENT / EXPIRE / DEPOSE, l'alerte de retard et le risque
 * d'interruption.
 */
class RenouvellementDelaiCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);

    @Test
    void enAvance_quandPlusDe60JoursAvantDateOptimale() {
        // expiration = today + 6 mois → optimale = today + 4 mois → ~120 j restants
        LocalDate expiration = TODAY.plusMonths(6);
        RenouvellementDelaiResult r =
                RenouvellementDelaiCalculator.compute(expiration, null, "CARTE_RESIDENT", TODAY);

        assertThat(r.statut()).isEqualTo(RenouvellementDelaiStatut.EN_AVANCE);
        assertThat(r.dateOptimalDepot()).isEqualTo(expiration.minusMonths(2));
        assertThat(r.dateDepotImperatif()).isEqualTo(expiration.minusMonths(1));
        assertThat(r.joursRestantsAvantOptimal()).isGreaterThanOrEqualTo(60);
        assertThat(r.risqueIrruption()).isFalse();
        assertThat(r.alerteRetard()).isFalse();
    }

    @Test
    void aDeposer_quandMoinsDe60JoursMaisAuMoins30AvantDateOptimale() {
        // optimale dans ~45 j → expiration = today + 45j + 2 mois
        LocalDate expiration = TODAY.plusDays(45).plusMonths(2);
        RenouvellementDelaiResult r =
                RenouvellementDelaiCalculator.compute(expiration, null, null, TODAY);

        assertThat(r.statut()).isEqualTo(RenouvellementDelaiStatut.A_DEPOSER);
        assertThat(r.joursRestantsAvantOptimal()).isBetween(30L, 59L);
        assertThat(r.risqueIrruption()).isFalse();
    }

    @Test
    void aDeposerUrgent_quandMoinsDe30JoursAvantDateOptimale() {
        // optimale dans ~10 j → expiration = today + 10j + 2 mois
        LocalDate expiration = TODAY.plusDays(10).plusMonths(2);
        RenouvellementDelaiResult r =
                RenouvellementDelaiCalculator.compute(expiration, null, null, TODAY);

        assertThat(r.statut()).isEqualTo(RenouvellementDelaiStatut.A_DEPOSER_URGENT);
        assertThat(r.joursRestantsAvantOptimal()).isLessThan(30);
        assertThat(r.risqueIrruption()).isFalse();
    }

    @Test
    void aDeposerUrgent_quandDateOptimaleDejaPasseeMaisTitreNonExpire() {
        // expiration = today + 20 j → optimale dans le passé, mais titre non expiré
        LocalDate expiration = TODAY.plusDays(20);
        RenouvellementDelaiResult r =
                RenouvellementDelaiCalculator.compute(expiration, null, null, TODAY);

        assertThat(r.statut()).isEqualTo(RenouvellementDelaiStatut.A_DEPOSER_URGENT);
        assertThat(r.joursRestantsAvantOptimal()).isNegative();
        assertThat(r.risqueIrruption()).isFalse();
    }

    @Test
    void expire_quandTitreExpireSansDepot_risqueIrruptionTrue() {
        LocalDate expiration = TODAY.minusDays(5);
        RenouvellementDelaiResult r =
                RenouvellementDelaiCalculator.compute(expiration, null, null, TODAY);

        assertThat(r.statut()).isEqualTo(RenouvellementDelaiStatut.EXPIRE);
        assertThat(r.risqueIrruption()).isTrue();
        assertThat(r.alerteRetard()).isFalse();
        assertThat(r.joursRestantsAvantOptimal()).isNegative();
    }

    @Test
    void depose_estPrioritaireSurExpiration_pasDeRisque() {
        // titre expiré MAIS dépôt enregistré → statut DEPOSE (prioritaire)
        LocalDate expiration = TODAY.minusDays(5);
        LocalDate depot = TODAY.minusMonths(2);
        RenouvellementDelaiResult r =
                RenouvellementDelaiCalculator.compute(expiration, depot, null, TODAY);

        assertThat(r.statut()).isEqualTo(RenouvellementDelaiStatut.DEPOSE);
        assertThat(r.risqueIrruption()).isFalse();
        assertThat(r.joursRestantsAvantOptimal()).isNull();
        assertThat(r.joursRestantsAvantImperatif()).isNull();
    }

    @Test
    void depose_dansLesDelais_pasDAlerteRetard() {
        LocalDate expiration = TODAY.plusMonths(3);
        LocalDate depot = expiration.minusMonths(2); // dépôt à la date optimale
        RenouvellementDelaiResult r =
                RenouvellementDelaiCalculator.compute(expiration, depot, "ETUDIANT", TODAY);

        assertThat(r.statut()).isEqualTo(RenouvellementDelaiStatut.DEPOSE);
        assertThat(r.alerteRetard()).isFalse();
    }

    @Test
    void depose_tardif_plusDe15JoursApresExpiration_alerteRetardTrue() {
        LocalDate expiration = TODAY.minusMonths(1);
        LocalDate depot = expiration.plusDays(20); // 20 j > 15 j de tolérance
        RenouvellementDelaiResult r =
                RenouvellementDelaiCalculator.compute(expiration, depot, null, TODAY);

        assertThat(r.statut()).isEqualTo(RenouvellementDelaiStatut.DEPOSE);
        assertThat(r.alerteRetard()).isTrue();
        assertThat(r.risqueIrruption()).isFalse();
    }

    @Test
    void depose_dans15JoursApresExpiration_pasDAlerteRetard() {
        LocalDate expiration = TODAY.minusMonths(1);
        LocalDate depot = expiration.plusDays(10); // dans la tolérance de 15 j
        RenouvellementDelaiResult r =
                RenouvellementDelaiCalculator.compute(expiration, depot, null, TODAY);

        assertThat(r.statut()).isEqualTo(RenouvellementDelaiStatut.DEPOSE);
        assertThat(r.alerteRetard()).isFalse();
    }

    @Test
    void dateExpirationNull_leve() {
        assertThatThrownBy(() ->
                RenouvellementDelaiCalculator.compute(null, null, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateExpirationTitre");
    }

    @Test
    void baseJuridique_citeR4331() {
        RenouvellementDelaiResult r = RenouvellementDelaiCalculator.compute(
                TODAY.plusMonths(6), null, null, TODAY);
        assertThat(r.baseJuridique()).contains("R. 433-1");
    }
}
