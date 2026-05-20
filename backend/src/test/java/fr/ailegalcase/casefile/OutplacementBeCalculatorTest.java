package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-207-08 : tests unitaires du calculateur de conformité de l'outplacement
 * obligatoire 45+ BE (CCT 82 ; CCT 82 bis ; Loi 05/09/2001 art. 13 ;
 * AR 30/05/2018 ; AR 25/11/1991 art. 154).
 *
 * <p>Couvre les 5 verdicts (OUTPLACEMENT_NON_DU avec 4 raisons,
 * OFFRE_NON_FAITE_SANCTION_EMPLOYEUR, OFFRE_NON_CONFORME_SANCTION_EMPLOYEUR,
 * OFFRE_REFUSEE_SANCTION_SALARIE, OFFRE_CONFORME_RESPECTEE), les bornes du
 * délai de 15 jours, le régime temps partiel CCT 82 bis, et la défense en
 * profondeur (null inputs).</p>
 */
class OutplacementBeCalculatorTest {

    /**
     * Cas nominal — salarié 54 ans, ancienneté 12,5 ans, licenciement
     * économique, contrat temps plein, offre conforme dans le délai et
     * acceptée → OFFRE_CONFORME_RESPECTEE, sanction 0 €.
     */
    @Test
    void cas_nominal_offre_conforme_acceptee() {
        var req = new OutplacementBeRequest(
                LocalDate.of(2026, 4, 15),                                 // licenciement
                LocalDate.of(1972, 3, 10),                                 // naissance → 54 ans
                new BigDecimal("12.5"),
                OutplacementBeMotifLicenciement.LICENCIEMENT_ECONOMIQUE,
                true,                                                       // temps plein
                true,                                                       // offre reçue
                LocalDate.of(2026, 4, 20),                                 // offre dans le délai
                true,                                                       // conforme
                true);                                                      // accepte
        var r = OutplacementBeCalculator.compute(req);

        assertThat(r.verdict()).isEqualTo(OutplacementBeResult.Verdict.OFFRE_CONFORME_RESPECTEE);
        assertThat(r.ageALaDateLicenciement()).isEqualTo(54);
        assertThat(r.obligationEmployeurApplicable()).isTrue();
        assertThat(r.raisonNonObligation()).isNull();
        assertThat(r.sanctionEmployeurEuros()).isEqualByComparingTo("0.00");
        assertThat(r.sanctionSalarieRange()).isNull();
        assertThat(r.dateLimiteOffre()).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(r.delaiOffreRespecte()).isTrue();
        assertThat(r.etapeSuivante()).isEqualTo(OutplacementBeResult.EtapeSuivante.AUCUNE);
        assertThat(r.baseJuridique())
                .contains("CCT 82")
                .contains("5 septembre 2001")
                .contains("AR 30/05/2018");
        assertThat(r.formuleCalcul()).contains("outplacement respecté");
    }

    /** Âge &lt; 45 → OUTPLACEMENT_NON_DU + AGE_INFERIEUR_45. */
    @Test
    void age_inferieur_45_outplacement_non_du() {
        var req = new OutplacementBeRequest(
                LocalDate.of(2026, 4, 15),
                LocalDate.of(1985, 4, 16),                                 // 40 ans (anniversaire pas atteint)
                new BigDecimal("10"),
                OutplacementBeMotifLicenciement.LICENCIEMENT_ECONOMIQUE,
                true, false, null, null, null);
        var r = OutplacementBeCalculator.compute(req);

        assertThat(r.verdict()).isEqualTo(OutplacementBeResult.Verdict.OUTPLACEMENT_NON_DU);
        assertThat(r.ageALaDateLicenciement()).isEqualTo(40);
        assertThat(r.obligationEmployeurApplicable()).isFalse();
        assertThat(r.raisonNonObligation()).isEqualTo(OutplacementBeRaisonNonObligation.AGE_INFERIEUR_45);
        assertThat(r.sanctionEmployeurEuros()).isEqualByComparingTo("0.00");
        assertThat(r.sanctionSalarieRange()).isNull();
        assertThat(r.etapeSuivante()).isEqualTo(OutplacementBeResult.EtapeSuivante.AUCUNE);
        assertThat(r.formuleCalcul()).contains("AGE_INFERIEUR_45");
    }

    /**
     * Borne exacte 45 ans — salarié pile 45 ans à la date du licenciement →
     * obligation applicable (le seuil est ≥ 45).
     */
    @Test
    void age_pile_45_obligation_applicable() {
        var req = new OutplacementBeRequest(
                LocalDate.of(2026, 4, 15),
                LocalDate.of(1981, 4, 15),                                 // exactement 45 ans
                new BigDecimal("10"),
                OutplacementBeMotifLicenciement.LICENCIEMENT_AUTRE,
                true, false, null, null, null);
        var r = OutplacementBeCalculator.compute(req);

        assertThat(r.ageALaDateLicenciement()).isEqualTo(45);
        assertThat(r.obligationEmployeurApplicable()).isTrue();
        assertThat(r.verdict()).isEqualTo(OutplacementBeResult.Verdict.OFFRE_NON_FAITE_SANCTION_EMPLOYEUR);
    }

    /** Ancienneté &lt; 1 → OUTPLACEMENT_NON_DU + ANCIENNETE_INFERIEURE_1AN. */
    @Test
    void anciennete_inferieure_1_outplacement_non_du() {
        var req = new OutplacementBeRequest(
                LocalDate.of(2026, 4, 15),
                LocalDate.of(1972, 3, 10),                                 // 54 ans
                new BigDecimal("0.5"),                                     // 6 mois
                OutplacementBeMotifLicenciement.LICENCIEMENT_ECONOMIQUE,
                true, false, null, null, null);
        var r = OutplacementBeCalculator.compute(req);

        assertThat(r.verdict()).isEqualTo(OutplacementBeResult.Verdict.OUTPLACEMENT_NON_DU);
        assertThat(r.raisonNonObligation())
                .isEqualTo(OutplacementBeRaisonNonObligation.ANCIENNETE_INFERIEURE_1AN);
        assertThat(r.formuleCalcul()).contains("ANCIENNETE_INFERIEURE_1AN");
    }

    /** Motif FAUTE_GRAVE → OUTPLACEMENT_NON_DU + FAUTE_GRAVE. */
    @Test
    void faute_grave_outplacement_non_du() {
        var req = new OutplacementBeRequest(
                LocalDate.of(2026, 4, 15),
                LocalDate.of(1972, 3, 10),
                new BigDecimal("12.5"),
                OutplacementBeMotifLicenciement.FAUTE_GRAVE,
                true, false, null, null, null);
        var r = OutplacementBeCalculator.compute(req);

        assertThat(r.verdict()).isEqualTo(OutplacementBeResult.Verdict.OUTPLACEMENT_NON_DU);
        assertThat(r.raisonNonObligation()).isEqualTo(OutplacementBeRaisonNonObligation.FAUTE_GRAVE);
        assertThat(r.sanctionEmployeurEuros()).isEqualByComparingTo("0.00");
        assertThat(r.formuleCalcul()).contains("FAUTE_GRAVE");
    }

    /** Motif DEMISSION → OUTPLACEMENT_NON_DU + DEMISSION. */
    @Test
    void demission_outplacement_non_du() {
        var req = new OutplacementBeRequest(
                LocalDate.of(2026, 4, 15),
                LocalDate.of(1972, 3, 10),
                new BigDecimal("12.5"),
                OutplacementBeMotifLicenciement.DEMISSION,
                true, false, null, null, null);
        var r = OutplacementBeCalculator.compute(req);

        assertThat(r.verdict()).isEqualTo(OutplacementBeResult.Verdict.OUTPLACEMENT_NON_DU);
        assertThat(r.raisonNonObligation()).isEqualTo(OutplacementBeRaisonNonObligation.DEMISSION);
        assertThat(r.formuleCalcul()).contains("DEMISSION");
    }

    /**
     * Obligation applicable + aucune offre reçue → OFFRE_NON_FAITE + sanction
     * 1 800 € + étape PLAINTE_INSPECTION_LOIS_SOCIALES.
     */
    @Test
    void offre_non_recue_sanction_employeur_1800() {
        var req = new OutplacementBeRequest(
                LocalDate.of(2026, 4, 15),
                LocalDate.of(1972, 3, 10),
                new BigDecimal("12.5"),
                OutplacementBeMotifLicenciement.LICENCIEMENT_ECONOMIQUE,
                true,
                false,                                                      // aucune offre
                null, null, null);
        var r = OutplacementBeCalculator.compute(req);

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeResult.Verdict.OFFRE_NON_FAITE_SANCTION_EMPLOYEUR);
        assertThat(r.obligationEmployeurApplicable()).isTrue();
        assertThat(r.sanctionEmployeurEuros()).isEqualByComparingTo("1800.00");
        assertThat(r.sanctionSalarieRange()).isNull();
        assertThat(r.etapeSuivante())
                .isEqualTo(OutplacementBeResult.EtapeSuivante.PLAINTE_INSPECTION_LOIS_SOCIALES);
        assertThat(r.formuleCalcul())
                .contains("1 800")
                .contains("AR 30/05/2018");
    }

    /**
     * Offre reçue mais non conforme CCT 82 (offreConformeCCT82=false) →
     * OFFRE_NON_CONFORME_SANCTION_EMPLOYEUR + 1 800 €.
     */
    @Test
    void offre_non_conforme_cct82_sanction_employeur_1800() {
        var req = new OutplacementBeRequest(
                LocalDate.of(2026, 4, 15),
                LocalDate.of(1972, 3, 10),
                new BigDecimal("12.5"),
                OutplacementBeMotifLicenciement.LICENCIEMENT_ECONOMIQUE,
                true, true,
                LocalDate.of(2026, 4, 20),                                 // dans le délai
                false,                                                      // non conforme
                null);
        var r = OutplacementBeCalculator.compute(req);

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeResult.Verdict.OFFRE_NON_CONFORME_SANCTION_EMPLOYEUR);
        assertThat(r.sanctionEmployeurEuros()).isEqualByComparingTo("1800.00");
        assertThat(r.delaiOffreRespecte()).isTrue();
        assertThat(r.etapeSuivante())
                .isEqualTo(OutplacementBeResult.EtapeSuivante.PLAINTE_INSPECTION_LOIS_SOCIALES);
        assertThat(r.formuleCalcul()).contains("non conforme");
    }

    /**
     * Offre reçue mais HORS délai (dateOffre &gt; dateLicenciement + 15 j) →
     * OFFRE_NON_CONFORME_SANCTION_EMPLOYEUR + 1 800 € + delaiOffreRespecte=false.
     */
    @Test
    void offre_hors_delai_15j_sanction_employeur() {
        var req = new OutplacementBeRequest(
                LocalDate.of(2026, 4, 15),
                LocalDate.of(1972, 3, 10),
                new BigDecimal("12.5"),
                OutplacementBeMotifLicenciement.LICENCIEMENT_ECONOMIQUE,
                true, true,
                LocalDate.of(2026, 5, 1),                                  // au-delà 30/04
                true,                                                       // conforme sur le fond
                null);
        var r = OutplacementBeCalculator.compute(req);

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeResult.Verdict.OFFRE_NON_CONFORME_SANCTION_EMPLOYEUR);
        assertThat(r.delaiOffreRespecte()).isFalse();
        assertThat(r.dateLimiteOffre()).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(r.sanctionEmployeurEuros()).isEqualByComparingTo("1800.00");
        assertThat(r.formuleCalcul()).contains("tardive");
    }

    /**
     * Borne exacte du délai — dateOffre = dateLicenciement + 15 jours
     * exactement (dernier jour acceptable) → délai respecté.
     */
    @Test
    void offre_borne_exacte_15j_delai_respecte() {
        var req = new OutplacementBeRequest(
                LocalDate.of(2026, 4, 15),
                LocalDate.of(1972, 3, 10),
                new BigDecimal("12.5"),
                OutplacementBeMotifLicenciement.LICENCIEMENT_ECONOMIQUE,
                true, true,
                LocalDate.of(2026, 4, 30),                                 // exactement J+15
                true, true);
        var r = OutplacementBeCalculator.compute(req);

        assertThat(r.delaiOffreRespecte()).isTrue();
        assertThat(r.verdict()).isEqualTo(OutplacementBeResult.Verdict.OFFRE_CONFORME_RESPECTEE);
    }

    /**
     * Offre conforme dans le délai mais refusée par le salarié →
     * OFFRE_REFUSEE_SANCTION_SALARIE + range 4-52 sem + étape
     * PRESENTATION_C4_ONEM.
     */
    @Test
    void offre_conforme_refusee_sanction_salarie_4_52_semaines() {
        var req = new OutplacementBeRequest(
                LocalDate.of(2026, 4, 15),
                LocalDate.of(1972, 3, 10),
                new BigDecimal("12.5"),
                OutplacementBeMotifLicenciement.LICENCIEMENT_ECONOMIQUE,
                true, true,
                LocalDate.of(2026, 4, 20),
                true,
                false);                                                     // refuse
        var r = OutplacementBeCalculator.compute(req);

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeResult.Verdict.OFFRE_REFUSEE_SANCTION_SALARIE);
        assertThat(r.sanctionEmployeurEuros()).isEqualByComparingTo("0.00");
        assertThat(r.sanctionSalarieRange()).isNotNull();
        assertThat(r.sanctionSalarieRange().minSemaines()).isEqualTo(4);
        assertThat(r.sanctionSalarieRange().maxSemaines()).isEqualTo(52);
        assertThat(r.etapeSuivante())
                .isEqualTo(OutplacementBeResult.EtapeSuivante.PRESENTATION_C4_ONEM);
        assertThat(r.formuleCalcul())
                .contains("4")
                .contains("52")
                .contains("AR 25/11/1991");
    }

    /**
     * Offre conforme + salarié pas encore décidé (null) →
     * OFFRE_CONFORME_RESPECTEE (pas de sanction par défaut).
     */
    @Test
    void offre_conforme_acceptation_null_par_defaut_conforme() {
        var req = new OutplacementBeRequest(
                LocalDate.of(2026, 4, 15),
                LocalDate.of(1972, 3, 10),
                new BigDecimal("12.5"),
                OutplacementBeMotifLicenciement.LICENCIEMENT_AUTRE,
                true, true,
                LocalDate.of(2026, 4, 20),
                true,
                null);                                                      // pas décidé
        var r = OutplacementBeCalculator.compute(req);

        assertThat(r.verdict()).isEqualTo(OutplacementBeResult.Verdict.OFFRE_CONFORME_RESPECTEE);
        assertThat(r.sanctionEmployeurEuros()).isEqualByComparingTo("0.00");
        assertThat(r.sanctionSalarieRange()).isNull();
    }

    /**
     * Régime CCT 82 bis — contrat temps partiel (contratTempsPlein=false).
     * L'obligation reste applicable et le verdict identique, seule la
     * formule mentionne le régime CCT 82 bis.
     */
    @Test
    void temps_partiel_cct82_bis_obligation_applicable() {
        var req = new OutplacementBeRequest(
                LocalDate.of(2026, 4, 15),
                LocalDate.of(1972, 3, 10),
                new BigDecimal("8"),
                OutplacementBeMotifLicenciement.LICENCIEMENT_ECONOMIQUE,
                false,                                                      // temps partiel
                false, null, null, null);
        var r = OutplacementBeCalculator.compute(req);

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeResult.Verdict.OFFRE_NON_FAITE_SANCTION_EMPLOYEUR);
        assertThat(r.contratTempsPlein()).isFalse();
        assertThat(r.formuleCalcul()).contains("CCT 82 bis");
    }

    /** dateLimiteOffre = dateLicenciement + 15 jours calendaires — vérification dédiée. */
    @Test
    void dateLimiteOffre_egale_licenciement_plus_15_jours() {
        var req = new OutplacementBeRequest(
                LocalDate.of(2026, 4, 15),
                LocalDate.of(1972, 3, 10),
                new BigDecimal("12.5"),
                OutplacementBeMotifLicenciement.LICENCIEMENT_AUTRE,
                true, false, null, null, null);
        var r = OutplacementBeCalculator.compute(req);

        assertThat(r.dateLimiteOffre()).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    /**
     * Priorité des règles d'exclusion — un salarié de moins de 45 ans en
     * faute grave doit déclencher la raison AGE_INFERIEUR_45 (1ʳᵉ règle
     * matched) plutôt que FAUTE_GRAVE. Confirme l'ordre prioritaire de
     * la mini-spec.
     */
    @Test
    void priorite_regles_age_avant_faute_grave() {
        var req = new OutplacementBeRequest(
                LocalDate.of(2026, 4, 15),
                LocalDate.of(1990, 1, 1),                                  // 36 ans
                new BigDecimal("5"),
                OutplacementBeMotifLicenciement.FAUTE_GRAVE,
                true, false, null, null, null);
        var r = OutplacementBeCalculator.compute(req);

        assertThat(r.raisonNonObligation())
                .isEqualTo(OutplacementBeRaisonNonObligation.AGE_INFERIEUR_45);
    }

    // =========================================================================
    // Défenses en profondeur — requête null, champs requis null
    // =========================================================================

    @Test
    void requestNull_leveIllegalArgument() {
        assertThatThrownBy(() -> OutplacementBeCalculator.compute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outplacement");
    }

    @Test
    void dateLicenciementNull_leveIllegalArgument() {
        var req = new OutplacementBeRequest(
                null, LocalDate.of(1972, 3, 10), new BigDecimal("12.5"),
                OutplacementBeMotifLicenciement.LICENCIEMENT_ECONOMIQUE,
                true, false, null, null, null);
        assertThatThrownBy(() -> OutplacementBeCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateLicenciement");
    }

    @Test
    void dateNaissanceNull_leveIllegalArgument() {
        var req = new OutplacementBeRequest(
                LocalDate.of(2026, 4, 15), null, new BigDecimal("12.5"),
                OutplacementBeMotifLicenciement.LICENCIEMENT_ECONOMIQUE,
                true, false, null, null, null);
        assertThatThrownBy(() -> OutplacementBeCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNaissanceSalarie");
    }

    @Test
    void ancienneteNull_leveIllegalArgument() {
        var req = new OutplacementBeRequest(
                LocalDate.of(2026, 4, 15), LocalDate.of(1972, 3, 10), null,
                OutplacementBeMotifLicenciement.LICENCIEMENT_ECONOMIQUE,
                true, false, null, null, null);
        assertThatThrownBy(() -> OutplacementBeCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ancienneteAnnees");
    }

    @Test
    void motifLicenciementNull_leveIllegalArgument() {
        var req = new OutplacementBeRequest(
                LocalDate.of(2026, 4, 15), LocalDate.of(1972, 3, 10),
                new BigDecimal("12.5"), null,
                true, false, null, null, null);
        assertThatThrownBy(() -> OutplacementBeCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifLicenciement");
    }

    @Test
    void contratTempsPleinNull_leveIllegalArgument() {
        var req = new OutplacementBeRequest(
                LocalDate.of(2026, 4, 15), LocalDate.of(1972, 3, 10),
                new BigDecimal("12.5"),
                OutplacementBeMotifLicenciement.LICENCIEMENT_ECONOMIQUE,
                null, false, null, null, null);
        assertThatThrownBy(() -> OutplacementBeCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contratTempsPlein");
    }

    @Test
    void offreOutplacementRecueNull_leveIllegalArgument() {
        var req = new OutplacementBeRequest(
                LocalDate.of(2026, 4, 15), LocalDate.of(1972, 3, 10),
                new BigDecimal("12.5"),
                OutplacementBeMotifLicenciement.LICENCIEMENT_ECONOMIQUE,
                true, null, null, null, null);
        assertThatThrownBy(() -> OutplacementBeCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offreOutplacementRecue");
    }
}
