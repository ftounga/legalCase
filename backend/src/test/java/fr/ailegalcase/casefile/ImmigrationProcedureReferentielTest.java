package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImmigrationProcedureReferentielTest {

    @Test
    void renouvellementTitreSejour_returns2Jalons() {
        List<ImmigrationProcedureReferentiel.ProcedureJalon> jalons =
                ImmigrationProcedureReferentiel.resolve("RENOUVELLEMENT_TITRE_SEJOUR");
        assertThat(jalons).hasSize(2);
        assertThat(jalons.get(0).offsetDays()).isEqualTo(120);
        assertThat(jalons.get(1).offsetDays()).isEqualTo(60);
    }

    @Test
    void demandeAsileOfpra_returns2Jalons() {
        List<ImmigrationProcedureReferentiel.ProcedureJalon> jalons =
                ImmigrationProcedureReferentiel.resolve("DEMANDE_ASILE_OFPRA");
        assertThat(jalons).hasSize(2);
        assertThat(jalons.get(0).offsetDays()).isEqualTo(21);
        assertThat(jalons.get(1).offsetDays()).isEqualTo(180);
    }

    @Test
    void recoursCnda_returns2Jalons() {
        List<ImmigrationProcedureReferentiel.ProcedureJalon> jalons =
                ImmigrationProcedureReferentiel.resolve("RECOURS_CNDA");
        assertThat(jalons).hasSize(2);
        assertThat(jalons.get(0).offsetDays()).isEqualTo(150);
        assertThat(jalons.get(1).offsetDays()).isEqualTo(270);
    }

    @Test
    void unknownType_returnsEmptyList() {
        assertThat(ImmigrationProcedureReferentiel.resolve("NATURALISATION")).isEmpty();
    }

    @Test
    void nullType_returnsEmptyList() {
        assertThat(ImmigrationProcedureReferentiel.resolve(null)).isEmpty();
    }
}
