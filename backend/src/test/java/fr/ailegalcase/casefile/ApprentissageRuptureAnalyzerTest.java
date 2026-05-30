package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-23 : tests unitaires de {@link ApprentissageRuptureAnalyzer}.
 *
 * <p>Logique déterministe (art. L.6222-18 et s. CT) :
 * <ul>
 *   <li>seuil de la rupture libre = 45 premiers jours ;</li>
 *   <li>dans les 45 jours : rupture libre → VALIDE quel que soit le motif ;</li>
 *   <li>au-delà : SANS_MOTIF → NON_VALIDE (saisine CPH) ; FAUTE_GRAVE →
 *       A_SECURISER (procédure de licenciement) ; ACCORD_PARTIES /
 *       FORCE_MAJEURE / INAPTITUDE / EXCLUSION_DEFINITIVE_CFA → VALIDE ;</li>
 *   <li>verdict RUPTURE_REGULIERE / RUPTURE_IRREGULIERE / RUPTURE_A_SECURISER.</li>
 * </ul>
 */
class ApprentissageRuptureAnalyzerTest {

    @Test
    void ruptureJ20_sansMotif_dansLes45Jours_ruptureLibreValide() {
        ApprentissageRuptureResult r = ApprentissageRuptureAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 25),
                ApprentissageAuteurRupture.EMPLOYEUR, ApprentissageMotifRupture.SANS_MOTIF, true);

        assertThat(r.periode()).isEqualTo(ApprentissagePeriodeRupture.DANS_45_PREMIERS_JOURS);
        assertThat(r.dansPeriodeLibre()).isTrue();
        assertThat(r.validite()).isEqualTo(ApprentissageRuptureValidite.VALIDE);
        assertThat(r.verdictGlobal()).isEqualTo(ApprentissageRuptureVerdict.RUPTURE_REGULIERE);
        assertThat(r.baseJuridique()).contains("L.6222-18");
    }

    @Test
    void ruptureJ90_sansMotif_apresLes45Jours_irreguliere_saisineCph() {
        ApprentissageRuptureResult r = ApprentissageRuptureAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 4, 6),
                ApprentissageAuteurRupture.EMPLOYEUR, ApprentissageMotifRupture.SANS_MOTIF, true);

        assertThat(r.periode()).isEqualTo(ApprentissagePeriodeRupture.APRES_45_JOURS);
        assertThat(r.dansPeriodeLibre()).isFalse();
        assertThat(r.validite()).isEqualTo(ApprentissageRuptureValidite.NON_VALIDE);
        assertThat(r.verdictGlobal()).isEqualTo(ApprentissageRuptureVerdict.RUPTURE_IRREGULIERE);
        assertThat(r.consequences()).anySatisfy(c -> assertThat(c).contains("CPH"));
    }

    @Test
    void ruptureJ90_accordParties_apresLes45Jours_valide() {
        ApprentissageRuptureResult r = ApprentissageRuptureAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 4, 6),
                ApprentissageAuteurRupture.APPRENTI, ApprentissageMotifRupture.ACCORD_PARTIES, true);

        assertThat(r.validite()).isEqualTo(ApprentissageRuptureValidite.VALIDE);
        assertThat(r.verdictGlobal()).isEqualTo(ApprentissageRuptureVerdict.RUPTURE_REGULIERE);
    }

    @Test
    void ruptureJ90_fauteGrave_apresLes45Jours_aSecuriser_procedureLicenciement() {
        ApprentissageRuptureResult r = ApprentissageRuptureAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 4, 6),
                ApprentissageAuteurRupture.EMPLOYEUR, ApprentissageMotifRupture.FAUTE_GRAVE, true);

        assertThat(r.validite()).isEqualTo(ApprentissageRuptureValidite.A_SECURISER);
        assertThat(r.verdictGlobal()).isEqualTo(ApprentissageRuptureVerdict.RUPTURE_A_SECURISER);
        assertThat(r.consequences()).anySatisfy(c -> assertThat(c).contains("licenciement"));
    }

    @Test
    void ruptureJ120_inaptitude_apresLes45Jours_valide_constatMedecin() {
        ApprentissageRuptureResult r = ApprentissageRuptureAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 5, 6),
                ApprentissageAuteurRupture.EMPLOYEUR, ApprentissageMotifRupture.INAPTITUDE, true);

        assertThat(r.validite()).isEqualTo(ApprentissageRuptureValidite.VALIDE);
        assertThat(r.verdictGlobal()).isEqualTo(ApprentissageRuptureVerdict.RUPTURE_REGULIERE);
        assertThat(r.consequences()).anySatisfy(c -> assertThat(c).contains("L.6222-18-1"));
    }

    @Test
    void ruptureForceMajeure_apresLes45Jours_valide() {
        ApprentissageRuptureResult r = ApprentissageRuptureAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 6, 6),
                ApprentissageAuteurRupture.EMPLOYEUR, ApprentissageMotifRupture.FORCE_MAJEURE, true);

        assertThat(r.validite()).isEqualTo(ApprentissageRuptureValidite.VALIDE);
        assertThat(r.verdictGlobal()).isEqualTo(ApprentissageRuptureVerdict.RUPTURE_REGULIERE);
    }

    @Test
    void ruptureExclusionDefinitiveCfa_apresLes45Jours_valide() {
        ApprentissageRuptureResult r = ApprentissageRuptureAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 6, 6),
                ApprentissageAuteurRupture.EMPLOYEUR,
                ApprentissageMotifRupture.EXCLUSION_DEFINITIVE_CFA, true);

        assertThat(r.validite()).isEqualTo(ApprentissageRuptureValidite.VALIDE);
        assertThat(r.verdictGlobal()).isEqualTo(ApprentissageRuptureVerdict.RUPTURE_REGULIERE);
        assertThat(r.consequences()).anySatisfy(c -> assertThat(c).contains("L.6222-21"));
    }

    @Test
    void seuil45Jours_exactementAu45eJour_estDansLaPeriodeLibre() {
        // Début 2025-01-06, +44 jours d'écart = 45 jours bornes incluses → libre.
        ApprentissageRuptureResult r = ApprentissageRuptureAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 2, 19),
                ApprentissageAuteurRupture.APPRENTI, ApprentissageMotifRupture.SANS_MOTIF, true);

        assertThat(r.joursDepuisDebut()).isEqualTo(45L);
        assertThat(r.periode()).isEqualTo(ApprentissagePeriodeRupture.DANS_45_PREMIERS_JOURS);
        assertThat(r.validite()).isEqualTo(ApprentissageRuptureValidite.VALIDE);

        // Le 46e jour bascule en période limitée.
        ApprentissageRuptureResult r46 = ApprentissageRuptureAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 2, 20),
                ApprentissageAuteurRupture.APPRENTI, ApprentissageMotifRupture.SANS_MOTIF, true);
        assertThat(r46.joursDepuisDebut()).isEqualTo(46L);
        assertThat(r46.periode()).isEqualTo(ApprentissagePeriodeRupture.APRES_45_JOURS);
        assertThat(r46.validite()).isEqualTo(ApprentissageRuptureValidite.NON_VALIDE);
    }

    @Test
    void apprentiMineur_ajouteFormaliteRepresentantLegal() {
        ApprentissageRuptureResult r = ApprentissageRuptureAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 20),
                ApprentissageAuteurRupture.APPRENTI, ApprentissageMotifRupture.SANS_MOTIF, false);

        assertThat(r.apprentiMajeur()).isFalse();
        assertThat(r.consequences()).anySatisfy(c -> assertThat(c).contains("représentant légal"));
    }

    @Test
    void datesIncoherentes_ruptureAvantDebut_leveIllegalArgument() {
        assertThatThrownBy(() -> ApprentissageRuptureAnalyzer.analyze(
                LocalDate.of(2025, 4, 6), LocalDate.of(2025, 1, 6),
                ApprentissageAuteurRupture.EMPLOYEUR, ApprentissageMotifRupture.SANS_MOTIF, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void champsRequisNull_leveIllegalArgument() {
        assertThatThrownBy(() -> ApprentissageRuptureAnalyzer.analyze(
                null, LocalDate.of(2025, 4, 6),
                ApprentissageAuteurRupture.EMPLOYEUR, ApprentissageMotifRupture.SANS_MOTIF, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ApprentissageRuptureAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), null,
                ApprentissageAuteurRupture.EMPLOYEUR, ApprentissageMotifRupture.SANS_MOTIF, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ApprentissageRuptureAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 4, 6),
                null, ApprentissageMotifRupture.SANS_MOTIF, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ApprentissageRuptureAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 4, 6),
                ApprentissageAuteurRupture.EMPLOYEUR, null, true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
