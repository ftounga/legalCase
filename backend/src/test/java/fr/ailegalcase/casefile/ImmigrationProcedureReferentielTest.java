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

    // ---- F-IM-16 SF-IM-16-01 : extension enum type_procedure_detectee ----

    @Test
    void regularisationExceptionnelle_returns2Jalons_rattrapageJavaDb() {
        // Rattrapage Java vs DB : REGULARISATION_EXCEPTIONNELLE était en DB depuis la migration 049
        // mais absent du fallback Java. Aligné par SF-IM-16-01.
        List<ImmigrationProcedureReferentiel.ProcedureJalon> jalons =
                ImmigrationProcedureReferentiel.resolve("REGULARISATION_EXCEPTIONNELLE");
        assertThat(jalons).hasSize(2);
        assertThat(jalons.get(0).offsetDays()).isEqualTo(180);
        assertThat(jalons.get(1).offsetDays()).isEqualTo(120);
    }

    @Test
    void regroupementFamilial_returns2Jalons() {
        List<ImmigrationProcedureReferentiel.ProcedureJalon> jalons =
                ImmigrationProcedureReferentiel.resolve("REGROUPEMENT_FAMILIAL");
        assertThat(jalons).hasSize(2);
        assertThat(jalons).allMatch(j -> j.offsetDays() == 180);
        assertThat(jalons.get(0).label()).contains("OFII");
    }

    @Test
    void naturalisationDecret_returns2Jalons_18mois() {
        List<ImmigrationProcedureReferentiel.ProcedureJalon> jalons =
                ImmigrationProcedureReferentiel.resolve("NATURALISATION_DECRET");
        assertThat(jalons).hasSize(2);
        assertThat(jalons).allMatch(j -> j.offsetDays() == 540);
    }

    @Test
    void changementStatut_returns2Jalons_4mois() {
        List<ImmigrationProcedureReferentiel.ProcedureJalon> jalons =
                ImmigrationProcedureReferentiel.resolve("CHANGEMENT_STATUT");
        assertThat(jalons).hasSize(2);
        assertThat(jalons).allMatch(j -> j.offsetDays() == 120);
    }

    @Test
    void aesSalarie_returns2Jalons_6mois() {
        List<ImmigrationProcedureReferentiel.ProcedureJalon> jalons =
                ImmigrationProcedureReferentiel.resolve("AES_SALARIE");
        assertThat(jalons).hasSize(2);
        assertThat(jalons).allMatch(j -> j.offsetDays() == 180);
        assertThat(jalons.get(0).label()).contains("salarié");
    }

    @Test
    void oqtfAvecDelai_returns3Jalons() {
        List<ImmigrationProcedureReferentiel.ProcedureJalon> jalons =
                ImmigrationProcedureReferentiel.resolve("OQTF_AVEC_DELAI");
        assertThat(jalons).hasSize(3);
        assertThat(jalons.get(0).offsetDays()).isEqualTo(30);   // DDV
        assertThat(jalons.get(1).offsetDays()).isEqualTo(90);   // audience
        assertThat(jalons.get(2).offsetDays()).isEqualTo(180);  // décision
    }

    @Test
    void oqtfSansDelai_returns2JalonsEnHeuresEffectives() {
        List<ImmigrationProcedureReferentiel.ProcedureJalon> jalons =
                ImmigrationProcedureReferentiel.resolve("OQTF_SANS_DELAI");
        assertThat(jalons).hasSize(2);
        assertThat(jalons.get(0).offsetDays()).isEqualTo(4);
        assertThat(jalons.get(1).offsetDays()).isEqualTo(7);
        assertThat(jalons.get(0).label()).contains("JLD");
    }
}
