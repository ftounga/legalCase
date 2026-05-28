package fr.ailegalcase.casefile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-31 : tests unitaires de
 * {@link CongePaterniteNaissanceBeChecker} — couvre les branches
 * verdict (ELIGIBLE_CONGE_OUVERT, CONGE_EN_COURS_PROTECTION_ACTIVE,
 * CONGE_PRIS_PROTECTION_RESIDUELLE, INELIGIBLE_STATUT_NON_COUVERT,
 * INELIGIBLE_FILIATION_NON_ETABLIE, DROIT_PERDU_DELAI_DEPASSE,
 * INELIGIBLE_NAISSANCE_FUTURE), la durée applicable selon la date de
 * naissance (20 / 15 / 10 jours), le calcul des échéances 4 mois et de
 * la protection 5 mois, l'indemnisation employeur 100 % / mutuelle
 * 82 % et toutes les validations d'entrée.
 */
class CongePaterniteNaissanceBeCheckerTest {

    private static final LocalDate NAIS_2026 = LocalDate.of(2026, 1, 15);
    private static final LocalDate NAIS_2022 = LocalDate.of(2022, 6, 1);
    private static final LocalDate NAIS_2020 = LocalDate.of(2020, 6, 1);
    private static final LocalDate NOTIF_2026 = LocalDate.of(2026, 1, 20);
    private static final LocalDate FIN_PRISE = LocalDate.of(2026, 2, 28);

    /** Naissance survenue 2026, salarié, filiation établie. */
    private static CongePaterniteNaissanceBeRequest reqEligibleOuvert() {
        return new CongePaterniteNaissanceBeRequest(
                CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                CongePaterniteNaissanceBeEtape.NAISSANCE_SURVENUE,
                true, true, false,
                NAIS_2026, null, 0, null);
    }

    /** Congé en cours de prise, notification faite. */
    private static CongePaterniteNaissanceBeRequest reqCongeEnCours() {
        return new CongePaterniteNaissanceBeRequest(
                CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                CongePaterniteNaissanceBeLienFiliation.COPARENT_COMATERNITE,
                CongePaterniteNaissanceBeEtape.CONGE_EN_COURS_DE_PRISE,
                true, true, true,
                NAIS_2026, NOTIF_2026, 5, null);
    }

    /** Congé pris entièrement (20 jours). */
    private static CongePaterniteNaissanceBeRequest reqCongePris() {
        return new CongePaterniteNaissanceBeRequest(
                CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                CongePaterniteNaissanceBeEtape.CONGE_PRIS_ENTIEREMENT,
                true, true, true,
                NAIS_2026, NOTIF_2026, 20, FIN_PRISE);
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Cas nominaux par verdict")
    class NominalCases {

        @Test
        void eligibleCongeOuvert_naissanceSurvenue() {
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(reqEligibleOuvert());
            assertThat(result.verdict()).isEqualTo(
                    CongePaterniteNaissanceBeVerdict.ELIGIBLE_CONGE_OUVERT);
            assertThat(result.dureeApplicableJoursOuvrables()).isEqualTo(20);
            assertThat(result.joursRestantsAPrendre()).isEqualTo(20);
            assertThat(result.echeanceQuatreMoisPostNaissance())
                    .isEqualTo(LocalDate.of(2026, 5, 15));
        }

        @Test
        void eligibleCongeOuvert_notificationFaite_etapeSpecifique() {
            CongePaterniteNaissanceBeRequest req =
                    new CongePaterniteNaissanceBeRequest(
                            CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                            CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                            CongePaterniteNaissanceBeEtape.NOTIFICATION_EMPLOYEUR_FAITE,
                            true, true, true,
                            NAIS_2026, NOTIF_2026, 0, null);
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(req);
            assertThat(result.verdict()).isEqualTo(
                    CongePaterniteNaissanceBeVerdict.ELIGIBLE_CONGE_OUVERT);
            assertThat(result.protectionLicenciementActive()).isTrue();
        }

        @Test
        void congeEnCours_protectionActive() {
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(reqCongeEnCours());
            assertThat(result.verdict()).isEqualTo(
                    CongePaterniteNaissanceBeVerdict.CONGE_EN_COURS_PROTECTION_ACTIVE);
            assertThat(result.protectionLicenciementActive()).isTrue();
            assertThat(result.joursRestantsAPrendre()).isEqualTo(15);
            assertThat(result.indemniteEmployeurJoursCent()).isEqualTo(3);
            assertThat(result.indemniteMutuelleJoursQuatreVingtDeux()).isEqualTo(2);
        }

        @Test
        void congePris_protectionResiduelle() {
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(reqCongePris());
            assertThat(result.verdict()).isEqualTo(
                    CongePaterniteNaissanceBeVerdict.CONGE_PRIS_PROTECTION_RESIDUELLE);
            assertThat(result.joursRestantsAPrendre()).isEqualTo(0);
            assertThat(result.protectionLicenciementActive()).isTrue();
            assertThat(result.finProtectionLicenciement())
                    .isEqualTo(FIN_PRISE.plusMonths(5));
            assertThat(result.indemniteEmployeurJoursCent()).isEqualTo(3);
            assertThat(result.indemniteMutuelleJoursQuatreVingtDeux()).isEqualTo(17);
        }

        @Test
        void avantNaissance_calculIndicatif() {
            CongePaterniteNaissanceBeRequest req =
                    new CongePaterniteNaissanceBeRequest(
                            CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                            CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                            CongePaterniteNaissanceBeEtape.AVANT_NAISSANCE,
                            true, true, false,
                            null, null, null, null);
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(req);
            assertThat(result.verdict()).isEqualTo(
                    CongePaterniteNaissanceBeVerdict.INELIGIBLE_NAISSANCE_FUTURE);
            assertThat(result.dureeApplicableJoursOuvrables()).isEqualTo(20);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Cas d'inéligibilité")
    class IneligibilityCases {

        @Test
        void statutIndependant_ineligible() {
            CongePaterniteNaissanceBeRequest req =
                    new CongePaterniteNaissanceBeRequest(
                            CongePaterniteNaissanceBeStatutTravailleur.INDEPENDANT,
                            CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                            CongePaterniteNaissanceBeEtape.NAISSANCE_SURVENUE,
                            true, true, false,
                            NAIS_2026, null, 0, null);
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(req);
            assertThat(result.verdict()).isEqualTo(
                    CongePaterniteNaissanceBeVerdict.INELIGIBLE_STATUT_NON_COUVERT);
            assertThat(result.synthese()).contains("INASTI");
        }

        @Test
        void statutAgentPublic_ineligible() {
            CongePaterniteNaissanceBeRequest req =
                    new CongePaterniteNaissanceBeRequest(
                            CongePaterniteNaissanceBeStatutTravailleur.AGENT_PUBLIC_STATUTAIRE,
                            CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                            CongePaterniteNaissanceBeEtape.NAISSANCE_SURVENUE,
                            true, true, false,
                            NAIS_2026, null, 0, null);
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(req);
            assertThat(result.verdict()).isEqualTo(
                    CongePaterniteNaissanceBeVerdict.INELIGIBLE_STATUT_NON_COUVERT);
            assertThat(result.synthese()).contains("AR du 19/11/1998");
        }

        @Test
        void statutAutre_ineligible() {
            CongePaterniteNaissanceBeRequest req =
                    new CongePaterniteNaissanceBeRequest(
                            CongePaterniteNaissanceBeStatutTravailleur.AUTRE,
                            CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                            CongePaterniteNaissanceBeEtape.NAISSANCE_SURVENUE,
                            true, true, false,
                            NAIS_2026, null, 0, null);
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(req);
            assertThat(result.verdict()).isEqualTo(
                    CongePaterniteNaissanceBeVerdict.INELIGIBLE_STATUT_NON_COUVERT);
        }

        @Test
        void salarieContratRompu_ineligible() {
            CongePaterniteNaissanceBeRequest req =
                    new CongePaterniteNaissanceBeRequest(
                            CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                            CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                            CongePaterniteNaissanceBeEtape.NAISSANCE_SURVENUE,
                            true, false, false,
                            NAIS_2026, null, 0, null);
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(req);
            assertThat(result.verdict()).isEqualTo(
                    CongePaterniteNaissanceBeVerdict.INELIGIBLE_STATUT_NON_COUVERT);
            assertThat(result.synthese()).contains("contrat de travail");
        }

        @Test
        void filiationNonEtablie_ineligible() {
            CongePaterniteNaissanceBeRequest req =
                    new CongePaterniteNaissanceBeRequest(
                            CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                            CongePaterniteNaissanceBeLienFiliation.COPARENT_RECONNAISSANCE,
                            CongePaterniteNaissanceBeEtape.NAISSANCE_SURVENUE,
                            false, true, false,
                            NAIS_2026, null, 0, null);
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(req);
            assertThat(result.verdict()).isEqualTo(
                    CongePaterniteNaissanceBeVerdict.INELIGIBLE_FILIATION_NON_ETABLIE);
            assertThat(result.raison())
                    .contains("LIEN_FILIATION_NON_ETABLI");
        }

        @Test
        void droitPerduDelaiDepasse_joursRestants() {
            CongePaterniteNaissanceBeRequest req =
                    new CongePaterniteNaissanceBeRequest(
                            CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                            CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                            CongePaterniteNaissanceBeEtape.DELAI_QUATRE_MOIS_DEPASSE,
                            true, true, true,
                            NAIS_2026, NOTIF_2026, 5, null);
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(req);
            assertThat(result.verdict()).isEqualTo(
                    CongePaterniteNaissanceBeVerdict.DROIT_PERDU_DELAI_DEPASSE);
            assertThat(result.joursRestantsAPrendre()).isEqualTo(15);
        }

        @Test
        void delaiDepasse_maisDejaPris_traiteCommePris() {
            CongePaterniteNaissanceBeRequest req =
                    new CongePaterniteNaissanceBeRequest(
                            CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                            CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                            CongePaterniteNaissanceBeEtape.DELAI_QUATRE_MOIS_DEPASSE,
                            true, true, true,
                            NAIS_2026, NOTIF_2026, 20, FIN_PRISE);
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(req);
            // Plus de jours restants → traité comme entièrement pris
            assertThat(result.verdict()).isEqualTo(
                    CongePaterniteNaissanceBeVerdict.CONGE_PRIS_PROTECTION_RESIDUELLE);
            assertThat(result.joursRestantsAPrendre()).isEqualTo(0);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Durée applicable selon date de naissance")
    class DureeApplicableCases {

        @Test
        void naissance_2026_vingtJours() {
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(reqEligibleOuvert());
            assertThat(result.dureeApplicableJoursOuvrables()).isEqualTo(20);
        }

        @Test
        void naissance_2022_quinzeJours() {
            CongePaterniteNaissanceBeRequest req =
                    new CongePaterniteNaissanceBeRequest(
                            CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                            CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                            CongePaterniteNaissanceBeEtape.NAISSANCE_SURVENUE,
                            true, true, false,
                            NAIS_2022, null, 0, null);
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(req);
            assertThat(result.dureeApplicableJoursOuvrables()).isEqualTo(15);
        }

        @Test
        void naissance_2020_dixJours() {
            CongePaterniteNaissanceBeRequest req =
                    new CongePaterniteNaissanceBeRequest(
                            CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                            CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                            CongePaterniteNaissanceBeEtape.NAISSANCE_SURVENUE,
                            true, true, false,
                            NAIS_2020, null, 0, null);
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(req);
            assertThat(result.dureeApplicableJoursOuvrables()).isEqualTo(10);
        }

        @Test
        void naissance_pivot_01_01_2023_vingtJours_inclusif() {
            CongePaterniteNaissanceBeRequest req =
                    new CongePaterniteNaissanceBeRequest(
                            CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                            CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                            CongePaterniteNaissanceBeEtape.NAISSANCE_SURVENUE,
                            true, true, false,
                            LocalDate.of(2023, 1, 1), null, 0, null);
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(req);
            assertThat(result.dureeApplicableJoursOuvrables()).isEqualTo(20);
        }

        @Test
        void naissance_pivot_01_01_2021_quinzeJours_inclusif() {
            CongePaterniteNaissanceBeRequest req =
                    new CongePaterniteNaissanceBeRequest(
                            CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                            CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                            CongePaterniteNaissanceBeEtape.NAISSANCE_SURVENUE,
                            true, true, false,
                            LocalDate.of(2021, 1, 1), null, 0, null);
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(req);
            assertThat(result.dureeApplicableJoursOuvrables()).isEqualTo(15);
        }

        @Test
        void naissance_veille_pivot_dixJours() {
            CongePaterniteNaissanceBeRequest req =
                    new CongePaterniteNaissanceBeRequest(
                            CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                            CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                            CongePaterniteNaissanceBeEtape.NAISSANCE_SURVENUE,
                            true, true, false,
                            LocalDate.of(2020, 12, 31), null, 0, null);
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(req);
            assertThat(result.dureeApplicableJoursOuvrables()).isEqualTo(10);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Calculs échéances et protection")
    class EcheancesCases {

        @Test
        void echeanceQuatreMois_estNaissancePlusQuatre() {
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(reqEligibleOuvert());
            assertThat(result.echeanceQuatreMoisPostNaissance())
                    .isEqualTo(NAIS_2026.plusMonths(4));
        }

        @Test
        void finProtection_estFinPrisePlusCinqMois() {
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(reqCongePris());
            assertThat(result.finProtectionLicenciement())
                    .isEqualTo(FIN_PRISE.plusMonths(5));
        }

        @Test
        void echeance_nulle_sans_dateNaissance() {
            CongePaterniteNaissanceBeRequest req =
                    new CongePaterniteNaissanceBeRequest(
                            CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                            CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                            CongePaterniteNaissanceBeEtape.AVANT_NAISSANCE,
                            true, true, false,
                            null, null, 0, null);
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(req);
            assertThat(result.echeanceQuatreMoisPostNaissance()).isNull();
        }

        @Test
        void protectionActive_seulementSiNotificationOuPriseEnCours() {
            // Étape naissance survenue, notification non faite → pas de protection
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(reqEligibleOuvert());
            assertThat(result.protectionLicenciementActive()).isFalse();
        }

        @Test
        void protectionActive_apresNotification() {
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(reqCongeEnCours());
            assertThat(result.protectionLicenciementActive()).isTrue();
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Calcul indemnités employeur / mutuelle")
    class IndemnitesCases {

        @Test
        void joursPris_zero_indemnitesNulles() {
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(reqEligibleOuvert());
            assertThat(result.indemniteEmployeurJoursCent()).isEqualTo(0);
            assertThat(result.indemniteMutuelleJoursQuatreVingtDeux())
                    .isEqualTo(0);
        }

        @Test
        void joursPris_unADeuxTrois_employeurUniquement() {
            CongePaterniteNaissanceBeRequest req2 =
                    new CongePaterniteNaissanceBeRequest(
                            CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                            CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                            CongePaterniteNaissanceBeEtape.CONGE_EN_COURS_DE_PRISE,
                            true, true, true,
                            NAIS_2026, NOTIF_2026, 2, null);
            CongePaterniteNaissanceBeResult result2 =
                    CongePaterniteNaissanceBeChecker.analyze(req2);
            assertThat(result2.indemniteEmployeurJoursCent()).isEqualTo(2);
            assertThat(result2.indemniteMutuelleJoursQuatreVingtDeux())
                    .isEqualTo(0);
        }

        @Test
        void joursPris_cinq_troisEmployeurDeuxMutuelle() {
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(reqCongeEnCours());
            assertThat(result.indemniteEmployeurJoursCent()).isEqualTo(3);
            assertThat(result.indemniteMutuelleJoursQuatreVingtDeux())
                    .isEqualTo(2);
        }

        @Test
        void joursPris_vingt_troisEmployeurDixSeptMutuelle() {
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(reqCongePris());
            assertThat(result.indemniteEmployeurJoursCent()).isEqualTo(3);
            assertThat(result.indemniteMutuelleJoursQuatreVingtDeux())
                    .isEqualTo(17);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Base juridique et avertissement")
    class BaseJuridiqueAvertissement {

        @Test
        void baseJuridique_referencesObligatoires() {
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(reqEligibleOuvert());
            assertThat(result.baseJuridique())
                    .contains("Loi du 03/07/1978")
                    .contains("art. 30")
                    .contains("Loi du 07/04/2023")
                    .contains("Loi du 12/08/2000")
                    .contains("AR du 17/10/1994")
                    .contains("AR du 03/07/1996")
                    .contains("Cass. BE 16/03/2015")
                    .contains("Cass. BE 25/11/2019")
                    .contains("Cass. BE 12/05/2014");
        }

        @Test
        void avertissement_avocatBeRequis() {
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(reqEligibleOuvert());
            assertThat(result.avertissement())
                    .isNotNull()
                    .contains("droit social BE")
                    .contains("INAMI");
        }

        @Test
        void synthese_porte_la_qualification_metier() {
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(reqEligibleOuvert());
            assertThat(result.synthese())
                    .contains("OUVERT")
                    .contains("20")
                    .contains("Loi du 07/04/2023");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Validation d'entrée")
    class ValidationCases {

        @Test
        void requete_nulle_throws() {
            assertThatThrownBy(() ->
                    CongePaterniteNaissanceBeChecker.analyze(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Requête requise");
        }

        @Test
        void statutTravailleurNull_throws() {
            assertThatThrownBy(() ->
                    CongePaterniteNaissanceBeChecker.analyze(
                            new CongePaterniteNaissanceBeRequest(
                                    null,
                                    CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                                    CongePaterniteNaissanceBeEtape.NAISSANCE_SURVENUE,
                                    true, true, false,
                                    NAIS_2026, null, 0, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("statutTravailleur");
        }

        @Test
        void lienFiliationNull_throws() {
            assertThatThrownBy(() ->
                    CongePaterniteNaissanceBeChecker.analyze(
                            new CongePaterniteNaissanceBeRequest(
                                    CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                                    null,
                                    CongePaterniteNaissanceBeEtape.NAISSANCE_SURVENUE,
                                    true, true, false,
                                    NAIS_2026, null, 0, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("lienFiliation");
        }

        @Test
        void etapeProcedureNull_throws() {
            assertThatThrownBy(() ->
                    CongePaterniteNaissanceBeChecker.analyze(
                            new CongePaterniteNaissanceBeRequest(
                                    CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                                    CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                                    null,
                                    true, true, false,
                                    NAIS_2026, null, 0, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("etapeProcedure");
        }

        @Test
        void filiationEtablieNull_throws() {
            assertThatThrownBy(() ->
                    CongePaterniteNaissanceBeChecker.analyze(
                            new CongePaterniteNaissanceBeRequest(
                                    CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                                    CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                                    CongePaterniteNaissanceBeEtape.NAISSANCE_SURVENUE,
                                    null, true, false,
                                    NAIS_2026, null, 0, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("filiationEtablie");
        }

        @Test
        void contratTravailEnCoursNull_throws() {
            assertThatThrownBy(() ->
                    CongePaterniteNaissanceBeChecker.analyze(
                            new CongePaterniteNaissanceBeRequest(
                                    CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                                    CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                                    CongePaterniteNaissanceBeEtape.NAISSANCE_SURVENUE,
                                    true, null, false,
                                    NAIS_2026, null, 0, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("contratTravailEnCours");
        }

        @Test
        void notificationEmployeurNull_throws() {
            assertThatThrownBy(() ->
                    CongePaterniteNaissanceBeChecker.analyze(
                            new CongePaterniteNaissanceBeRequest(
                                    CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                                    CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                                    CongePaterniteNaissanceBeEtape.NAISSANCE_SURVENUE,
                                    true, true, null,
                                    NAIS_2026, null, 0, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("notificationEmployeurFaite");
        }

        @Test
        void joursDejaPrisNegatif_throws() {
            assertThatThrownBy(() ->
                    CongePaterniteNaissanceBeChecker.analyze(
                            new CongePaterniteNaissanceBeRequest(
                                    CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                                    CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                                    CongePaterniteNaissanceBeEtape.NAISSANCE_SURVENUE,
                                    true, true, false,
                                    NAIS_2026, null, -1, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("joursDejaPrisOuvrables");
        }

        @Test
        void dateNaissanceManquante_pour_etape_post_naissance_throws() {
            assertThatThrownBy(() ->
                    CongePaterniteNaissanceBeChecker.analyze(
                            new CongePaterniteNaissanceBeRequest(
                                    CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                                    CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                                    CongePaterniteNaissanceBeEtape.NAISSANCE_SURVENUE,
                                    true, true, false,
                                    null, null, 0, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("dateNaissance");
        }

        @Test
        void avantNaissance_dateNullPermise() {
            // L'étape AVANT_NAISSANCE n'exige PAS de date — ne doit pas lever
            CongePaterniteNaissanceBeRequest req =
                    new CongePaterniteNaissanceBeRequest(
                            CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                            CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE,
                            CongePaterniteNaissanceBeEtape.AVANT_NAISSANCE,
                            true, true, false,
                            null, null, null, null);
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(req);
            assertThat(result.verdict()).isEqualTo(
                    CongePaterniteNaissanceBeVerdict.INELIGIBLE_NAISSANCE_FUTURE);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Tous les types de lien filiation couverts")
    class LienFiliationCases {

        @Test
        void pereBiologique_supporte() {
            assertEligible(CongePaterniteNaissanceBeLienFiliation.PERE_BIOLOGIQUE);
        }

        @Test
        void coparentComaternite_supporte() {
            assertEligible(CongePaterniteNaissanceBeLienFiliation.COPARENT_COMATERNITE);
        }

        @Test
        void coparentReconnaissance_supporte() {
            assertEligible(CongePaterniteNaissanceBeLienFiliation.COPARENT_RECONNAISSANCE);
        }

        @Test
        void cohabitantLegalOuMarie_supporte() {
            assertEligible(CongePaterniteNaissanceBeLienFiliation.COHABITANT_LEGAL_OU_MARIE);
        }

        @Test
        void autreLien_supporte_si_filiationEtablie() {
            assertEligible(CongePaterniteNaissanceBeLienFiliation.AUTRE);
        }

        private void assertEligible(CongePaterniteNaissanceBeLienFiliation lien) {
            CongePaterniteNaissanceBeRequest req =
                    new CongePaterniteNaissanceBeRequest(
                            CongePaterniteNaissanceBeStatutTravailleur.SALARIE,
                            lien,
                            CongePaterniteNaissanceBeEtape.NAISSANCE_SURVENUE,
                            true, true, false,
                            NAIS_2026, null, 0, null);
            CongePaterniteNaissanceBeResult result =
                    CongePaterniteNaissanceBeChecker.analyze(req);
            assertThat(result.verdict()).isEqualTo(
                    CongePaterniteNaissanceBeVerdict.ELIGIBLE_CONGE_OUVERT);
            assertThat(result.lienFiliation()).isEqualTo(lien);
        }
    }
}
