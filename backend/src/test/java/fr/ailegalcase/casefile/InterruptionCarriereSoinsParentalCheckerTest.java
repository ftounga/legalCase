package fr.ailegalcase.casefile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-32 : tests unitaires de
 * {@link InterruptionCarriereSoinsParentalChecker} — couvre les
 * branches verdict (ELIGIBLE_COMPLET, ELIGIBLE_AVEC_RESERVES,
 * INELIGIBLE_ANCIENNETE, INELIGIBLE_AGE_ENFANT,
 * INELIGIBLE_SOLDE_INSUFFISANT, INELIGIBLE_FORMALISME,
 * DIFFERE_EMPLOYEUR), les durées par forme (4/8/20 mois), les
 * allocations ONEM forfaitaires (250/350/450 EUR), le calcul des
 * échéances et de la fenêtre de protection licenciement art. 101 Loi
 * 22/01/1985, et toutes les validations d'entrée.
 */
class InterruptionCarriereSoinsParentalCheckerTest {

    private static final LocalDate DATE_DEBUT = LocalDate.of(2026, 6, 15);
    private static final LocalDate DATE_NOTIF_OK =
            LocalDate.of(2026, 4, 1); // 75 jours avant — milieu du délai 60-90
    private static final BigDecimal REMU = new BigDecimal("3500.00");

    /** Eligible complet — temps plein, conditions remplies. */
    private static InterruptionCarriereSoinsParentalRequest reqEligibleComplet() {
        return new InterruptionCarriereSoinsParentalRequest(
                InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                24, // ancienneté
                36, // 3 ans
                false, // pas handicap
                4, // solde 4 mois ETP
                InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                true, // employeur accepte
                false, // pas différé
                true, // cumul ONEM
                DATE_DEBUT,
                DATE_NOTIF_OK,
                REMU);
    }

    /** Eligible avec réserves — employeur pas encore accepté. */
    private static InterruptionCarriereSoinsParentalRequest reqEligibleReserves() {
        return new InterruptionCarriereSoinsParentalRequest(
                InterruptionCarriereSoinsParentalForme.MI_TEMPS,
                24, 36, false, 4,
                InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                false, // employeur pas encore accepté
                false,
                true,
                DATE_DEBUT,
                DATE_NOTIF_OK,
                REMU);
    }

    /** Inéligible ancienneté. */
    private static InterruptionCarriereSoinsParentalRequest reqAncienneteInsuffisante() {
        return new InterruptionCarriereSoinsParentalRequest(
                InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                6, // 6 mois < 12 mois
                36, false, 4,
                InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                true, false, true,
                DATE_DEBUT,
                DATE_NOTIF_OK,
                REMU);
    }

    /** Inéligible âge enfant (sans handicap). */
    private static InterruptionCarriereSoinsParentalRequest reqAgeEnfantTropAge() {
        return new InterruptionCarriereSoinsParentalRequest(
                InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                24,
                150, // 12.5 ans > 12 ans
                false, 4,
                InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                true, false, true,
                DATE_DEBUT,
                DATE_NOTIF_OK,
                REMU);
    }

    /** Inéligible solde insuffisant. */
    private static InterruptionCarriereSoinsParentalRequest reqSoldeInsuffisant() {
        return new InterruptionCarriereSoinsParentalRequest(
                InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                24, 36, false,
                2, // solde 2 mois < 4 mois requis
                InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                true, false, true,
                DATE_DEBUT,
                DATE_NOTIF_OK,
                REMU);
    }

    /** Inéligible formalisme — mode AUTRE. */
    private static InterruptionCarriereSoinsParentalRequest reqFormalismeModeAutre() {
        return new InterruptionCarriereSoinsParentalRequest(
                InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                24, 36, false, 4,
                InterruptionCarriereSoinsParentalModeNotification.AUTRE,
                true, false, true,
                DATE_DEBUT,
                DATE_NOTIF_OK,
                REMU);
    }

    /** Différé employeur. */
    private static InterruptionCarriereSoinsParentalRequest reqDiffereEmployeur() {
        return new InterruptionCarriereSoinsParentalRequest(
                InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                24, 36, false, 4,
                InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                false,
                true, // employeur a différé
                true,
                DATE_DEBUT,
                DATE_NOTIF_OK,
                REMU);
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Cas nominaux par verdict")
    class NominalCases {

        @Test
        void eligibleComplet_tempsPlein() {
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            reqEligibleComplet());
            assertThat(r.verdict()).isEqualTo(
                    InterruptionCarriereSoinsParentalVerdict.ELIGIBLE_COMPLET);
            assertThat(r.raison()).isEqualTo(
                    "ELIGIBILITE_COMPLETE_CONDITIONS_CUMULATIVES_REUNIES");
            assertThat(r.dureeInterruptionMois()).isEqualTo(4);
            assertThat(r.ancienneteOk()).isTrue();
            assertThat(r.ageEnfantOk()).isTrue();
            assertThat(r.formalismeOk()).isTrue();
            assertThat(r.baseJuridique())
                    .contains("Loi de redressement du 22/01/1985")
                    .contains("AR du 29/10/1997")
                    .contains("CCT n° 64");
        }

        @Test
        void eligibleAvecReserves_employeurNonAccepte() {
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            reqEligibleReserves());
            assertThat(r.verdict()).isEqualTo(
                    InterruptionCarriereSoinsParentalVerdict.ELIGIBLE_AVEC_RESERVES);
            assertThat(r.dureeInterruptionMois()).isEqualTo(8);
        }

        @Test
        void inEligibleAnciennete() {
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            reqAncienneteInsuffisante());
            assertThat(r.verdict()).isEqualTo(
                    InterruptionCarriereSoinsParentalVerdict.INELIGIBLE_ANCIENNETE);
            assertThat(r.ancienneteOk()).isFalse();
            assertThat(r.raison()).contains("ANCIENNETE_INFERIEURE");
        }

        @Test
        void inEligibleAgeEnfant_sansHandicap() {
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            reqAgeEnfantTropAge());
            assertThat(r.verdict()).isEqualTo(
                    InterruptionCarriereSoinsParentalVerdict.INELIGIBLE_AGE_ENFANT);
            assertThat(r.ageEnfantOk()).isFalse();
            assertThat(r.synthese()).contains("douze ans");
        }

        @Test
        void inEligibleAgeEnfant_avecHandicap_borneMontee() {
            // Enfant 12 ans (144 mois) handicapé → encore éligible (< 21 ans)
            InterruptionCarriereSoinsParentalRequest req =
                    new InterruptionCarriereSoinsParentalRequest(
                            InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                            24, 144, true, 4,
                            InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                            true, false, true,
                            DATE_DEBUT, DATE_NOTIF_OK, REMU);
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(req);
            assertThat(r.ageEnfantOk()).isTrue();
            assertThat(r.verdict()).isNotEqualTo(
                    InterruptionCarriereSoinsParentalVerdict.INELIGIBLE_AGE_ENFANT);
        }

        @Test
        void inEligibleAgeEnfant_avecHandicap_hors21ans() {
            // 21 ans (252 mois) handicapé → inéligible
            InterruptionCarriereSoinsParentalRequest req =
                    new InterruptionCarriereSoinsParentalRequest(
                            InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                            24, 252, true, 4,
                            InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                            true, false, true,
                            DATE_DEBUT, DATE_NOTIF_OK, REMU);
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(req);
            assertThat(r.verdict()).isEqualTo(
                    InterruptionCarriereSoinsParentalVerdict.INELIGIBLE_AGE_ENFANT);
            assertThat(r.synthese()).contains("vingt et un ans");
        }

        @Test
        void inEligibleSoldeInsuffisant() {
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            reqSoldeInsuffisant());
            assertThat(r.verdict()).isEqualTo(
                    InterruptionCarriereSoinsParentalVerdict.INELIGIBLE_SOLDE_INSUFFISANT);
        }

        @Test
        void inEligibleFormalisme_modeAutre() {
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            reqFormalismeModeAutre());
            assertThat(r.verdict()).isEqualTo(
                    InterruptionCarriereSoinsParentalVerdict.INELIGIBLE_FORMALISME);
            assertThat(r.formalismeOk()).isFalse();
            assertThat(r.synthese()).contains("mode de notification");
        }

        @Test
        void inEligibleFormalisme_delaiTropCourt() {
            // Notification 30 jours avant → < 60 jours minimum
            InterruptionCarriereSoinsParentalRequest req =
                    new InterruptionCarriereSoinsParentalRequest(
                            InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                            24, 36, false, 4,
                            InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                            true, false, true,
                            DATE_DEBUT,
                            DATE_DEBUT.minusDays(30),
                            REMU);
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(req);
            assertThat(r.verdict()).isEqualTo(
                    InterruptionCarriereSoinsParentalVerdict.INELIGIBLE_FORMALISME);
            assertThat(r.synthese()).contains("trop court");
        }

        @Test
        void inEligibleFormalisme_delaiTropLong() {
            // Notification 120 jours avant → > 90 jours maximum
            InterruptionCarriereSoinsParentalRequest req =
                    new InterruptionCarriereSoinsParentalRequest(
                            InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                            24, 36, false, 4,
                            InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                            true, false, true,
                            DATE_DEBUT,
                            DATE_DEBUT.minusDays(120),
                            REMU);
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(req);
            assertThat(r.verdict()).isEqualTo(
                    InterruptionCarriereSoinsParentalVerdict.INELIGIBLE_FORMALISME);
            assertThat(r.synthese()).contains("trop long");
        }

        @Test
        void differeEmployeur() {
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            reqDiffereEmployeur());
            assertThat(r.verdict()).isEqualTo(
                    InterruptionCarriereSoinsParentalVerdict.DIFFERE_EMPLOYEUR);
            assertThat(r.synthese()).contains("art. 7");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Durées par forme et allocations ONEM")
    class DureesEtAllocationsCases {

        @Test
        void duree_tempsPlein_quatreMois() {
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            reqEligibleComplet());
            assertThat(r.dureeInterruptionMois()).isEqualTo(4);
        }

        @Test
        void duree_miTemps_huitMois() {
            InterruptionCarriereSoinsParentalRequest req =
                    new InterruptionCarriereSoinsParentalRequest(
                            InterruptionCarriereSoinsParentalForme.MI_TEMPS,
                            24, 36, false, 4,
                            InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                            true, false, true,
                            DATE_DEBUT, DATE_NOTIF_OK, REMU);
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(req);
            assertThat(r.dureeInterruptionMois()).isEqualTo(8);
        }

        @Test
        void duree_cinquiemeTemps_vingtMois() {
            InterruptionCarriereSoinsParentalRequest req =
                    new InterruptionCarriereSoinsParentalRequest(
                            InterruptionCarriereSoinsParentalForme.CINQUIEME_TEMPS,
                            24, 36, false, 4,
                            InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                            true, false, true,
                            DATE_DEBUT, DATE_NOTIF_OK, REMU);
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(req);
            assertThat(r.dureeInterruptionMois()).isEqualTo(20);
        }

        @Test
        void allocation_tempsPlein_250() {
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            reqEligibleComplet());
            assertThat(r.allocationOnemMensuelle())
                    .isEqualByComparingTo(new BigDecimal("250.00"));
            assertThat(r.allocationOnemTotale())
                    .isEqualByComparingTo(new BigDecimal("1000.00"));
        }

        @Test
        void allocation_miTemps_350() {
            InterruptionCarriereSoinsParentalRequest req =
                    new InterruptionCarriereSoinsParentalRequest(
                            InterruptionCarriereSoinsParentalForme.MI_TEMPS,
                            24, 36, false, 4,
                            InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                            true, false, true,
                            DATE_DEBUT, DATE_NOTIF_OK, REMU);
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(req);
            assertThat(r.allocationOnemMensuelle())
                    .isEqualByComparingTo(new BigDecimal("350.00"));
            assertThat(r.allocationOnemTotale())
                    .isEqualByComparingTo(new BigDecimal("2800.00"));
        }

        @Test
        void allocation_cinquieme_450() {
            InterruptionCarriereSoinsParentalRequest req =
                    new InterruptionCarriereSoinsParentalRequest(
                            InterruptionCarriereSoinsParentalForme.CINQUIEME_TEMPS,
                            24, 36, false, 4,
                            InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                            true, false, true,
                            DATE_DEBUT, DATE_NOTIF_OK, REMU);
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(req);
            assertThat(r.allocationOnemMensuelle())
                    .isEqualByComparingTo(new BigDecimal("450.00"));
            assertThat(r.allocationOnemTotale())
                    .isEqualByComparingTo(new BigDecimal("9000.00"));
        }

        @Test
        void allocation_zero_siCumulNonDemande() {
            InterruptionCarriereSoinsParentalRequest req =
                    new InterruptionCarriereSoinsParentalRequest(
                            InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                            24, 36, false, 4,
                            InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                            true, false,
                            false, // cumul ONEM non demandé
                            DATE_DEBUT, DATE_NOTIF_OK, REMU);
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(req);
            assertThat(r.allocationOnemMensuelle())
                    .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(r.allocationOnemTotale())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Calculs dates fin et protection licenciement")
    class CalculsDatesCases {

        @Test
        void dateFin_estDebutPlusDuree_tempsPlein() {
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            reqEligibleComplet());
            assertThat(r.dateFinInterruption())
                    .isEqualTo(DATE_DEBUT.plusMonths(4));
        }

        @Test
        void finProtection_estFinPlusTroisMois() {
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            reqEligibleComplet());
            assertThat(r.finProtectionLicenciement())
                    .isEqualTo(DATE_DEBUT.plusMonths(4).plusMonths(3));
        }

        @Test
        void protectionActive_siDateDebutPresente() {
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            reqEligibleComplet());
            assertThat(r.protectionLicenciementActive()).isTrue();
        }

        @Test
        void protectionActive_false_siPasDateDebut() {
            InterruptionCarriereSoinsParentalRequest req =
                    new InterruptionCarriereSoinsParentalRequest(
                            InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                            24, 36, false, 4,
                            InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                            true, false, true,
                            null, null, REMU);
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(req);
            assertThat(r.protectionLicenciementActive()).isFalse();
            assertThat(r.dateFinInterruption()).isNull();
            assertThat(r.finProtectionLicenciement()).isNull();
        }

        @Test
        void indemniteProtection_estSixFoisRemu() {
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            reqEligibleComplet());
            // 6 × 3500 = 21 000
            assertThat(r.indemniteProtectionEnCasLicenciement())
                    .isEqualByComparingTo(new BigDecimal("21000.00"));
        }

        @Test
        void indemniteProtection_null_siPasRemu() {
            InterruptionCarriereSoinsParentalRequest req =
                    new InterruptionCarriereSoinsParentalRequest(
                            InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                            24, 36, false, 4,
                            InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                            true, false, true,
                            DATE_DEBUT, DATE_NOTIF_OK, null);
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(req);
            assertThat(r.indemniteProtectionEnCasLicenciement()).isNull();
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Solde restant après imputation")
    class SoldeApresImputationCases {

        @Test
        void soldeApres_estSoldeAvantMoinsQuatre() {
            // Solde 4 → après = 0
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            reqEligibleComplet());
            assertThat(r.soldeRestantApresImputationMoisEtp()).isEqualTo(0);
        }

        @Test
        void soldeApres_calcul_avec8MoisRestants() {
            // Solde 8 → après = 4
            InterruptionCarriereSoinsParentalRequest req =
                    new InterruptionCarriereSoinsParentalRequest(
                            InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                            24, 36, false, 8,
                            InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                            true, false, true,
                            DATE_DEBUT, DATE_NOTIF_OK, REMU);
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(req);
            assertThat(r.soldeRestantApresImputationMoisEtp()).isEqualTo(4);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Base juridique et avertissement")
    class BaseJuridiqueAvertissement {

        @Test
        void baseJuridique_referencesObligatoires() {
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            reqEligibleComplet());
            assertThat(r.baseJuridique())
                    .contains("Loi de redressement du 22/01/1985")
                    .contains("art. 99")
                    .contains("art. 101")
                    .contains("AR du 29/10/1997")
                    .contains("art. 4")
                    .contains("art. 5")
                    .contains("art. 6")
                    .contains("CCT n° 64")
                    .contains("AR du 12/08/1991")
                    .contains("Cass. BE 26/05/2008")
                    .contains("Cass. BE 16/01/2017");
        }

        @Test
        void avertissement_avocatBeRequis() {
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            reqEligibleComplet());
            assertThat(r.avertissement())
                    .isNotNull()
                    .contains("droit social BE")
                    .contains("ONEM")
                    .contains("F-DT-29");
        }

        @Test
        void synthese_porteLaQualificationMetier() {
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            reqEligibleComplet());
            assertThat(r.synthese())
                    .contains("TEMPS PLEIN")
                    .contains("art. 101")
                    .contains("quatre mois");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Validation d'entrée")
    class ValidationCases {

        @Test
        void requete_nulle_throws() {
            assertThatThrownBy(() ->
                    InterruptionCarriereSoinsParentalChecker.analyze(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Requête requise");
        }

        @Test
        void formeNull_throws() {
            assertThatThrownBy(() ->
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            new InterruptionCarriereSoinsParentalRequest(
                                    null, 24, 36, false, 4,
                                    InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                                    true, false, true,
                                    DATE_DEBUT, DATE_NOTIF_OK, REMU)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forme");
        }

        @Test
        void ancienneteNull_throws() {
            assertThatThrownBy(() ->
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            new InterruptionCarriereSoinsParentalRequest(
                                    InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                                    null, 36, false, 4,
                                    InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                                    true, false, true,
                                    DATE_DEBUT, DATE_NOTIF_OK, REMU)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ancienneteMois");
        }

        @Test
        void ancienneteNegatif_throws() {
            assertThatThrownBy(() ->
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            new InterruptionCarriereSoinsParentalRequest(
                                    InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                                    -1, 36, false, 4,
                                    InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                                    true, false, true,
                                    DATE_DEBUT, DATE_NOTIF_OK, REMU)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ancienneteMois");
        }

        @Test
        void ageEnfantNull_throws() {
            assertThatThrownBy(() ->
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            new InterruptionCarriereSoinsParentalRequest(
                                    InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                                    24, null, false, 4,
                                    InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                                    true, false, true,
                                    DATE_DEBUT, DATE_NOTIF_OK, REMU)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ageEnfantMois");
        }

        @Test
        void enfantHandicapNull_throws() {
            assertThatThrownBy(() ->
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            new InterruptionCarriereSoinsParentalRequest(
                                    InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                                    24, 36, null, 4,
                                    InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                                    true, false, true,
                                    DATE_DEBUT, DATE_NOTIF_OK, REMU)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("enfantHandicap");
        }

        @Test
        void soldeNull_throws() {
            assertThatThrownBy(() ->
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            new InterruptionCarriereSoinsParentalRequest(
                                    InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                                    24, 36, false, null,
                                    InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                                    true, false, true,
                                    DATE_DEBUT, DATE_NOTIF_OK, REMU)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("soldeRestantMoisEtp");
        }

        @Test
        void soldeNegatif_throws() {
            assertThatThrownBy(() ->
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            new InterruptionCarriereSoinsParentalRequest(
                                    InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                                    24, 36, false, -1,
                                    InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                                    true, false, true,
                                    DATE_DEBUT, DATE_NOTIF_OK, REMU)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("soldeRestantMoisEtp");
        }

        @Test
        void modeNotificationNull_throws() {
            assertThatThrownBy(() ->
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            new InterruptionCarriereSoinsParentalRequest(
                                    InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                                    24, 36, false, 4,
                                    null,
                                    true, false, true,
                                    DATE_DEBUT, DATE_NOTIF_OK, REMU)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("modeNotification");
        }

        @Test
        void employeurAccepteNull_throws() {
            assertThatThrownBy(() ->
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            new InterruptionCarriereSoinsParentalRequest(
                                    InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                                    24, 36, false, 4,
                                    InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                                    null, false, true,
                                    DATE_DEBUT, DATE_NOTIF_OK, REMU)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employeurAccepte");
        }

        @Test
        void employeurAdiffereNull_throws() {
            assertThatThrownBy(() ->
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            new InterruptionCarriereSoinsParentalRequest(
                                    InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                                    24, 36, false, 4,
                                    InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                                    true, null, true,
                                    DATE_DEBUT, DATE_NOTIF_OK, REMU)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employeurAdiffere");
        }

        @Test
        void cumulOnemNull_throws() {
            assertThatThrownBy(() ->
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            new InterruptionCarriereSoinsParentalRequest(
                                    InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                                    24, 36, false, 4,
                                    InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                                    true, false, null,
                                    DATE_DEBUT, DATE_NOTIF_OK, REMU)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cumulAllocationOnemDemande");
        }

        @Test
        void remuNegative_throws() {
            assertThatThrownBy(() ->
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            new InterruptionCarriereSoinsParentalRequest(
                                    InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                                    24, 36, false, 4,
                                    InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                                    true, false, true,
                                    DATE_DEBUT, DATE_NOTIF_OK,
                                    new BigDecimal("-100"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("remunerationMensuelleBrute");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Toutes les formes couvertes")
    class FormesCases {

        @Test
        void tempsPlein_supporte() {
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(
                            reqEligibleComplet());
            assertThat(r.forme()).isEqualTo(
                    InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN);
        }

        @Test
        void miTemps_supporte() {
            InterruptionCarriereSoinsParentalRequest req =
                    new InterruptionCarriereSoinsParentalRequest(
                            InterruptionCarriereSoinsParentalForme.MI_TEMPS,
                            24, 36, false, 4,
                            InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                            true, false, true,
                            DATE_DEBUT, DATE_NOTIF_OK, REMU);
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(req);
            assertThat(r.forme()).isEqualTo(
                    InterruptionCarriereSoinsParentalForme.MI_TEMPS);
        }

        @Test
        void cinquiemeTemps_supporte() {
            InterruptionCarriereSoinsParentalRequest req =
                    new InterruptionCarriereSoinsParentalRequest(
                            InterruptionCarriereSoinsParentalForme.CINQUIEME_TEMPS,
                            24, 36, false, 4,
                            InterruptionCarriereSoinsParentalModeNotification.LETTRE_RECOMMANDEE,
                            true, false, true,
                            DATE_DEBUT, DATE_NOTIF_OK, REMU);
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(req);
            assertThat(r.forme()).isEqualTo(
                    InterruptionCarriereSoinsParentalForme.CINQUIEME_TEMPS);
        }

        @Test
        void remiseEnMain_supportee() {
            InterruptionCarriereSoinsParentalRequest req =
                    new InterruptionCarriereSoinsParentalRequest(
                            InterruptionCarriereSoinsParentalForme.TEMPS_PLEIN,
                            24, 36, false, 4,
                            InterruptionCarriereSoinsParentalModeNotification.REMISE_EN_MAIN,
                            true, false, true,
                            DATE_DEBUT, DATE_NOTIF_OK, REMU);
            InterruptionCarriereSoinsParentalResult r =
                    InterruptionCarriereSoinsParentalChecker.analyze(req);
            assertThat(r.verdict()).isEqualTo(
                    InterruptionCarriereSoinsParentalVerdict.ELIGIBLE_COMPLET);
            assertThat(r.formalismeOk()).isTrue();
        }
    }
}
