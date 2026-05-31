package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-25 : tests unitaires de {@link CdiChantierAnalyzer} (F-DT-37).
 *
 * <p>Logique déterministe (art. L.1223-8 et s. ; L.1236-8 ; R.1234-2 CT) :
 * <ul>
 *   <li>recours valide si accord de branche étendu OU usage constant
 *       (BTP / ingénierie) ; AUCUN → RECOURS_INVALIDE ;</li>
 *   <li>fin de chantier + recours valide → motif FIN_CHANTIER_CRS ;</li>
 *   <li>indemnité R.1234-2 = 1/4 par an ≤ 10 ans + 1/3 au-delà ;</li>
 *   <li>verdict LICENCIEMENT_FONDE / LICENCIEMENT_A_SECURISER / RECOURS_INVALIDE.</li>
 * </ul>
 */
class CdiChantierAnalyzerTest {

    private static final BigDecimal SALAIRE = new BigDecimal("3000.00");

    @Test
    void recoursAccordBranche_btp_chantierAcheve_reclassementPropose_licenciementFonde() {
        CdiChantierResult r = CdiChantierAnalyzer.analyze(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1),
                CdiChantierFondementRecours.ACCORD_BRANCHE_ETENDU, CdiChantierSecteur.BTP,
                true, SALAIRE, true);

        assertThat(r.recoursValide()).isTrue();
        assertThat(r.motifLicenciement()).isEqualTo(CdiChantierMotifLicenciement.FIN_CHANTIER_CRS);
        assertThat(r.indemniteLicenciement()).isGreaterThan(BigDecimal.ZERO);
        assertThat(r.verdictGlobal()).isEqualTo(CdiChantierVerdict.LICENCIEMENT_FONDE);
        assertThat(r.ancienneteAnnees()).isEqualTo(3);
        assertThat(r.baseJuridique()).contains("L.1223-8").contains("L.1236-8");
    }

    @Test
    void recoursUsageConstant_ingenierie_chantierAcheve_recoursValide() {
        CdiChantierResult r = CdiChantierAnalyzer.analyze(
                LocalDate.of(2023, 6, 1), LocalDate.of(2025, 6, 1),
                CdiChantierFondementRecours.USAGE_CONSTANT_SECTEUR, CdiChantierSecteur.INGENIERIE,
                true, SALAIRE, true);

        assertThat(r.recoursValide()).isTrue();
        assertThat(r.motifLicenciement()).isEqualTo(CdiChantierMotifLicenciement.FIN_CHANTIER_CRS);
        assertThat(r.verdictGlobal()).isEqualTo(CdiChantierVerdict.LICENCIEMENT_FONDE);
    }

    @Test
    void recoursAucun_recoursInvalide_noteRequalification() {
        CdiChantierResult r = CdiChantierAnalyzer.analyze(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1),
                CdiChantierFondementRecours.AUCUN, CdiChantierSecteur.BTP,
                true, SALAIRE, true);

        assertThat(r.recoursValide()).isFalse();
        assertThat(r.verdictGlobal()).isEqualTo(CdiChantierVerdict.RECOURS_INVALIDE);
        assertThat(r.motifLicenciement()).isEqualTo(CdiChantierMotifLicenciement.MOTIF_NON_FONDE);
        assertThat(r.consequences()).anySatisfy(c -> assertThat(c).contains("requalification"));
    }

    @Test
    void chantierNonAcheve_motifNonFonde_aSecuriser() {
        CdiChantierResult r = CdiChantierAnalyzer.analyze(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1),
                CdiChantierFondementRecours.ACCORD_BRANCHE_ETENDU, CdiChantierSecteur.BTP,
                false, SALAIRE, true);

        assertThat(r.recoursValide()).isTrue();
        assertThat(r.motifLicenciement()).isEqualTo(CdiChantierMotifLicenciement.MOTIF_NON_FONDE);
        assertThat(r.verdictGlobal()).isEqualTo(CdiChantierVerdict.LICENCIEMENT_A_SECURISER);
    }

    @Test
    void chantierAcheve_reclassementNonPropose_aSecuriser() {
        CdiChantierResult r = CdiChantierAnalyzer.analyze(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1),
                CdiChantierFondementRecours.ACCORD_BRANCHE_ETENDU, CdiChantierSecteur.BTP,
                true, SALAIRE, false);

        assertThat(r.recoursValide()).isTrue();
        assertThat(r.motifLicenciement()).isEqualTo(CdiChantierMotifLicenciement.FIN_CHANTIER_CRS);
        assertThat(r.verdictGlobal()).isEqualTo(CdiChantierVerdict.LICENCIEMENT_A_SECURISER);
    }

    @Test
    void usageConstantHorsSecteurReconnu_autre_aSecuriser() {
        CdiChantierResult r = CdiChantierAnalyzer.analyze(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1),
                CdiChantierFondementRecours.USAGE_CONSTANT_SECTEUR, CdiChantierSecteur.AUTRE,
                true, SALAIRE, true);

        // Le recours reste techniquement invoqué (recoursValide=true) mais l'usage
        // constant n'est pas établi hors BTP / ingénierie → à sécuriser.
        assertThat(r.recoursValide()).isTrue();
        assertThat(r.verdictGlobal()).isEqualTo(CdiChantierVerdict.LICENCIEMENT_A_SECURISER);
        assertThat(r.consequences()).anySatisfy(c -> assertThat(c).contains("accord de branche"));
    }

    @Test
    void indemnite_R1234_2_moinsDe10Ans() {
        // 3 ans × 1/4 × 3000 = 2250.00
        BigDecimal indemnite = CdiChantierAnalyzer.computeIndemnite(SALAIRE, 3);
        assertThat(indemnite).isEqualByComparingTo(new BigDecimal("2250.00"));
    }

    @Test
    void indemnite_R1234_2_plusDe10Ans_basculeUnTiers() {
        // 12 ans : 10 × 1/4 × 3000 = 7500 ; + 2 × 1/3 × 3000 = 2000 → 9500.00 (±arrondi)
        BigDecimal indemnite = CdiChantierAnalyzer.computeIndemnite(SALAIRE, 12);
        assertThat(indemnite).isEqualByComparingTo(new BigDecimal("9500.00"));
    }

    @Test
    void indemnite_ancienneteNulle_zero() {
        BigDecimal indemnite = CdiChantierAnalyzer.computeIndemnite(SALAIRE, 0);
        assertThat(indemnite).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void datesIncoherentes_ruptureAvantEntree_leveIllegalArgument() {
        assertThatThrownBy(() -> CdiChantierAnalyzer.analyze(
                LocalDate.of(2025, 1, 1), LocalDate.of(2022, 1, 1),
                CdiChantierFondementRecours.ACCORD_BRANCHE_ETENDU, CdiChantierSecteur.BTP,
                true, SALAIRE, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void salaireNegatif_leveIllegalArgument() {
        assertThatThrownBy(() -> CdiChantierAnalyzer.analyze(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1),
                CdiChantierFondementRecours.ACCORD_BRANCHE_ETENDU, CdiChantierSecteur.BTP,
                true, new BigDecimal("-1"), true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void champsRequisNull_leveIllegalArgument() {
        assertThatThrownBy(() -> CdiChantierAnalyzer.analyze(
                null, LocalDate.of(2025, 1, 1),
                CdiChantierFondementRecours.ACCORD_BRANCHE_ETENDU, CdiChantierSecteur.BTP,
                true, SALAIRE, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CdiChantierAnalyzer.analyze(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1),
                null, CdiChantierSecteur.BTP, true, SALAIRE, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CdiChantierAnalyzer.analyze(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1),
                CdiChantierFondementRecours.ACCORD_BRANCHE_ETENDU, null, true, SALAIRE, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CdiChantierAnalyzer.analyze(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1),
                CdiChantierFondementRecours.ACCORD_BRANCHE_ETENDU, CdiChantierSecteur.BTP,
                null, SALAIRE, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CdiChantierAnalyzer.analyze(
                LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1),
                CdiChantierFondementRecours.ACCORD_BRANCHE_ETENDU, CdiChantierSecteur.BTP,
                true, null, true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
