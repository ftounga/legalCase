package fr.ailegalcase.casefile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-30 : tests unitaires de
 * {@link BienEtreRpsConseillerPreventionChecker} — couvre les branches
 * verdict (SAISINE_CONFORME, SAISINE_INFORMELLE_EN_COURS,
 * SAISINE_FORMELLE_EN_COURS, AVIS_RENDU_DELAI_RESPECTE,
 * AVIS_RENDU_DELAI_DEPASSE, NON_CONFORME_FORMALITES_MANQUANTES,
 * NON_CONFORME_PAS_DE_CONSEILLER), le calcul des échéances légales et
 * la fenêtre de protection 12 mois, le respect du formalisme selon le
 * mode (informelle vs formelle), et toutes les validations d'entrée.
 */
class BienEtreRpsConseillerPreventionCheckerTest {

    private static final LocalDate DATE_DEPOT = LocalDate.of(2026, 1, 15);
    private static final LocalDate DATE_AVIS_OK =
            LocalDate.of(2026, 4, 10); // dépôt + ~3 mois
    private static final LocalDate DATE_AVIS_HORS_DELAI =
            LocalDate.of(2026, 8, 15); // dépôt + 7 mois > 6 mois max

    /** Saisine conforme — préparation avec entretien préalable réalisé. */
    private static BienEtreRpsConseillerPreventionRequest reqConformePrep() {
        return new BienEtreRpsConseillerPreventionRequest(
                BienEtreRpsConseillerPreventionTypeRisque.HARCELEMENT_MORAL,
                BienEtreRpsConseillerPreventionModeDemande.FORMELLE_INDIVIDUELLE,
                BienEtreRpsConseillerPreventionEtape.ENTRETIEN_PREALABLE_REALISE,
                true, true, false, false, false,
                null, null, null);
    }

    /** Demande informelle en cours. */
    private static BienEtreRpsConseillerPreventionRequest reqInformelle() {
        return new BienEtreRpsConseillerPreventionRequest(
                BienEtreRpsConseillerPreventionTypeRisque.AUTRES_RPS,
                BienEtreRpsConseillerPreventionModeDemande.INFORMELLE,
                BienEtreRpsConseillerPreventionEtape.DEMANDE_INFORMELLE_EN_COURS,
                true, true, false, false, false,
                null, null, null);
    }

    /** Demande formelle déposée — entretien + écrit + AR + notification. */
    private static BienEtreRpsConseillerPreventionRequest reqFormelleDeposee() {
        return new BienEtreRpsConseillerPreventionRequest(
                BienEtreRpsConseillerPreventionTypeRisque.HARCELEMENT_SEXUEL,
                BienEtreRpsConseillerPreventionModeDemande.FORMELLE_INDIVIDUELLE,
                BienEtreRpsConseillerPreventionEtape.DEMANDE_FORMELLE_DEPOSEE,
                true, true, true, true, true,
                DATE_DEPOT, null, 20);
    }

    /** Demande formelle collective déposée. */
    private static BienEtreRpsConseillerPreventionRequest reqFormelleCollective() {
        return new BienEtreRpsConseillerPreventionRequest(
                BienEtreRpsConseillerPreventionTypeRisque.AUTRES_RPS,
                BienEtreRpsConseillerPreventionModeDemande.FORMELLE_COLLECTIVE,
                BienEtreRpsConseillerPreventionEtape.DEMANDE_FORMELLE_DEPOSEE,
                true, true, true, true, true,
                DATE_DEPOT, null, 30);
    }

    /** Avis rendu dans les délais. */
    private static BienEtreRpsConseillerPreventionRequest reqAvisRespecte() {
        return new BienEtreRpsConseillerPreventionRequest(
                BienEtreRpsConseillerPreventionTypeRisque.VIOLENCE_AU_TRAVAIL,
                BienEtreRpsConseillerPreventionModeDemande.FORMELLE_INDIVIDUELLE,
                BienEtreRpsConseillerPreventionEtape.AVIS_RENDU,
                true, true, true, true, true,
                DATE_DEPOT, DATE_AVIS_OK, 85);
    }

    /** Avis rendu hors délai (> 6 mois). */
    private static BienEtreRpsConseillerPreventionRequest reqAvisDepasse() {
        return new BienEtreRpsConseillerPreventionRequest(
                BienEtreRpsConseillerPreventionTypeRisque.HARCELEMENT_MORAL,
                BienEtreRpsConseillerPreventionModeDemande.FORMELLE_INDIVIDUELLE,
                BienEtreRpsConseillerPreventionEtape.AVIS_RENDU,
                true, true, true, true, true,
                DATE_DEPOT, DATE_AVIS_HORS_DELAI, 212);
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Cas nominaux par verdict")
    class NominalCases {

        @Test
        void saisineConforme_preparationConforme() {
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            reqConformePrep());
            assertThat(result.verdict()).isEqualTo(
                    BienEtreRpsConseillerPreventionVerdict.SAISINE_CONFORME);
            assertThat(result.raison()).isEqualTo(
                    "FORMALITES_PREALABLES_CONFORMES");
            assertThat(result.baseJuridique()).contains("Loi du 04/08/1996");
            assertThat(result.baseJuridique()).contains("AR du 10/04/2014");
            assertThat(result.baseJuridique()).contains("art. 32sexies");
        }

        @Test
        void saisineInformelleEnCours() {
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            reqInformelle());
            assertThat(result.verdict()).isEqualTo(
                    BienEtreRpsConseillerPreventionVerdict.SAISINE_INFORMELLE_EN_COURS);
            assertThat(result.protectionRepresaillesActive()).isFalse();
            assertThat(result.formalitesPrealablesCompletes()).isTrue();
        }

        @Test
        void saisineFormelleEnCours_individuelle() {
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            reqFormelleDeposee());
            assertThat(result.verdict()).isEqualTo(
                    BienEtreRpsConseillerPreventionVerdict.SAISINE_FORMELLE_EN_COURS);
            assertThat(result.protectionRepresaillesActive()).isTrue();
            assertThat(result.echeanceEnqueteTroisMois())
                    .isEqualTo(DATE_DEPOT.plusMonths(3));
            assertThat(result.finProtectionRepresailles())
                    .isEqualTo(DATE_DEPOT.plusMonths(12));
        }

        @Test
        void saisineFormelleEnCours_collective() {
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            reqFormelleCollective());
            assertThat(result.verdict()).isEqualTo(
                    BienEtreRpsConseillerPreventionVerdict.SAISINE_FORMELLE_EN_COURS);
            assertThat(result.protectionRepresaillesActive()).isTrue();
            assertThat(result.synthese())
                    .contains("FORMELLE COLLECTIVE");
        }

        @Test
        void avisRenduDelaiRespecte() {
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            reqAvisRespecte());
            assertThat(result.verdict()).isEqualTo(
                    BienEtreRpsConseillerPreventionVerdict.AVIS_RENDU_DELAI_RESPECTE);
            assertThat(result.delaiEnqueteRespecte()).isTrue();
            assertThat(result.echeanceMesuresEmployeurDeuxMois())
                    .isEqualTo(DATE_AVIS_OK.plusMonths(2));
        }

        @Test
        void avisRenduDelaiDepasse_plusDeSixMois() {
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            reqAvisDepasse());
            assertThat(result.verdict()).isEqualTo(
                    BienEtreRpsConseillerPreventionVerdict.AVIS_RENDU_DELAI_DEPASSE);
            assertThat(result.delaiEnqueteRespecte()).isFalse();
        }

        @Test
        void mesuresPrisesParEmployeur_delaiRespecte() {
            BienEtreRpsConseillerPreventionRequest req =
                    new BienEtreRpsConseillerPreventionRequest(
                            BienEtreRpsConseillerPreventionTypeRisque.HARCELEMENT_MORAL,
                            BienEtreRpsConseillerPreventionModeDemande.FORMELLE_INDIVIDUELLE,
                            BienEtreRpsConseillerPreventionEtape.MESURES_PRISES_PAR_EMPLOYEUR,
                            true, true, true, true, true,
                            DATE_DEPOT, DATE_AVIS_OK, 120);
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(req);
            assertThat(result.verdict()).isEqualTo(
                    BienEtreRpsConseillerPreventionVerdict.AVIS_RENDU_DELAI_RESPECTE);
            assertThat(result.protectionRepresaillesActive()).isTrue();
        }

        @Test
        void intentionDemande_conformeSiCpapEtFormalitesInformelles() {
            BienEtreRpsConseillerPreventionRequest req =
                    new BienEtreRpsConseillerPreventionRequest(
                            BienEtreRpsConseillerPreventionTypeRisque.AUTRES_RPS,
                            BienEtreRpsConseillerPreventionModeDemande.INFORMELLE,
                            BienEtreRpsConseillerPreventionEtape.INTENTION_DEMANDE,
                            true, true, false, false, false,
                            null, null, null);
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(req);
            assertThat(result.verdict()).isEqualTo(
                    BienEtreRpsConseillerPreventionVerdict.SAISINE_CONFORME);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Cas de non-conformité")
    class NonConformiteCases {

        @Test
        void pasDeConseiller_priorite_absolue() {
            BienEtreRpsConseillerPreventionRequest req =
                    new BienEtreRpsConseillerPreventionRequest(
                            BienEtreRpsConseillerPreventionTypeRisque.HARCELEMENT_MORAL,
                            BienEtreRpsConseillerPreventionModeDemande.FORMELLE_INDIVIDUELLE,
                            BienEtreRpsConseillerPreventionEtape.DEMANDE_FORMELLE_DEPOSEE,
                            false, true, true, true, true,
                            DATE_DEPOT, null, 10);
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(req);
            assertThat(result.verdict()).isEqualTo(
                    BienEtreRpsConseillerPreventionVerdict.NON_CONFORME_PAS_DE_CONSEILLER);
            assertThat(result.raison()).isEqualTo(
                    "ABSENCE_DESIGNATION_CPAP_ART_32SEXIES_PARAGRAPHE_2");
        }

        @Test
        void formalitesManquantes_entretien_pour_formelle() {
            BienEtreRpsConseillerPreventionRequest req =
                    new BienEtreRpsConseillerPreventionRequest(
                            BienEtreRpsConseillerPreventionTypeRisque.HARCELEMENT_MORAL,
                            BienEtreRpsConseillerPreventionModeDemande.FORMELLE_INDIVIDUELLE,
                            BienEtreRpsConseillerPreventionEtape.DEMANDE_FORMELLE_DEPOSEE,
                            true, false, true, true, true,
                            DATE_DEPOT, null, 10);
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(req);
            assertThat(result.verdict()).isEqualTo(
                    BienEtreRpsConseillerPreventionVerdict.NON_CONFORME_FORMALITES_MANQUANTES);
            assertThat(result.synthese()).contains("entretien préalable");
        }

        @Test
        void formalitesManquantes_ecrit_pour_formelle() {
            BienEtreRpsConseillerPreventionRequest req =
                    new BienEtreRpsConseillerPreventionRequest(
                            BienEtreRpsConseillerPreventionTypeRisque.HARCELEMENT_MORAL,
                            BienEtreRpsConseillerPreventionModeDemande.FORMELLE_INDIVIDUELLE,
                            BienEtreRpsConseillerPreventionEtape.DEMANDE_FORMELLE_DEPOSEE,
                            true, true, false, true, true,
                            DATE_DEPOT, null, 10);
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(req);
            assertThat(result.verdict()).isEqualTo(
                    BienEtreRpsConseillerPreventionVerdict.NON_CONFORME_FORMALITES_MANQUANTES);
            assertThat(result.synthese()).contains("demande écrite");
        }

        @Test
        void formalitesManquantes_ar_pour_formelle() {
            BienEtreRpsConseillerPreventionRequest req =
                    new BienEtreRpsConseillerPreventionRequest(
                            BienEtreRpsConseillerPreventionTypeRisque.HARCELEMENT_MORAL,
                            BienEtreRpsConseillerPreventionModeDemande.FORMELLE_INDIVIDUELLE,
                            BienEtreRpsConseillerPreventionEtape.DEMANDE_FORMELLE_DEPOSEE,
                            true, true, true, false, true,
                            DATE_DEPOT, null, 10);
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(req);
            assertThat(result.verdict()).isEqualTo(
                    BienEtreRpsConseillerPreventionVerdict.NON_CONFORME_FORMALITES_MANQUANTES);
            assertThat(result.synthese()).contains("accusé de réception");
        }

        @Test
        void pasDeConseiller_prioritaire_sur_formalites() {
            // Conseiller absent ET formalités incomplètes → priorité conseiller
            BienEtreRpsConseillerPreventionRequest req =
                    new BienEtreRpsConseillerPreventionRequest(
                            BienEtreRpsConseillerPreventionTypeRisque.HARCELEMENT_MORAL,
                            BienEtreRpsConseillerPreventionModeDemande.FORMELLE_INDIVIDUELLE,
                            BienEtreRpsConseillerPreventionEtape.DEMANDE_FORMELLE_DEPOSEE,
                            false, false, false, false, false,
                            DATE_DEPOT, null, 10);
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(req);
            assertThat(result.verdict()).isEqualTo(
                    BienEtreRpsConseillerPreventionVerdict.NON_CONFORME_PAS_DE_CONSEILLER);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Calculs des échéances et de la protection")
    class CalculsEcheancesCases {

        @Test
        void echeanceEnquete_estDepotPlusTroisMois() {
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            reqFormelleDeposee());
            assertThat(result.echeanceEnqueteTroisMois())
                    .isEqualTo(LocalDate.of(2026, 4, 15));
        }

        @Test
        void echeanceMesuresEmployeur_estAvisPlusDeuxMois() {
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            reqAvisRespecte());
            assertThat(result.echeanceMesuresEmployeurDeuxMois())
                    .isEqualTo(DATE_AVIS_OK.plusMonths(2));
        }

        @Test
        void finProtectionRepresailles_estDepotPlusDouzeMois() {
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            reqFormelleDeposee());
            assertThat(result.finProtectionRepresailles())
                    .isEqualTo(LocalDate.of(2027, 1, 15));
        }

        @Test
        void protectionActive_pourFormelle_pasPourInformelle() {
            BienEtreRpsConseillerPreventionResult formelle =
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            reqFormelleDeposee());
            BienEtreRpsConseillerPreventionResult informelle =
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            reqInformelle());

            assertThat(formelle.protectionRepresaillesActive()).isTrue();
            assertThat(informelle.protectionRepresaillesActive()).isFalse();
        }

        @Test
        void echeances_nulles_sans_dateDepot() {
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            reqConformePrep());
            assertThat(result.echeanceEnqueteTroisMois()).isNull();
            assertThat(result.finProtectionRepresailles()).isNull();
        }

        @Test
        void echeanceMesures_nulle_sans_dateAvis() {
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            reqFormelleDeposee());
            assertThat(result.echeanceMesuresEmployeurDeuxMois()).isNull();
        }

        @Test
        void delaiRespecte_avisExactementSixMois() {
            // dépôt + 6 mois exactement = limite respectée (≤ limite)
            BienEtreRpsConseillerPreventionRequest req =
                    new BienEtreRpsConseillerPreventionRequest(
                            BienEtreRpsConseillerPreventionTypeRisque.HARCELEMENT_MORAL,
                            BienEtreRpsConseillerPreventionModeDemande.FORMELLE_INDIVIDUELLE,
                            BienEtreRpsConseillerPreventionEtape.AVIS_RENDU,
                            true, true, true, true, true,
                            DATE_DEPOT, DATE_DEPOT.plusMonths(6), 180);
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(req);
            assertThat(result.verdict()).isEqualTo(
                    BienEtreRpsConseillerPreventionVerdict.AVIS_RENDU_DELAI_RESPECTE);
            assertThat(result.delaiEnqueteRespecte()).isTrue();
        }

        @Test
        void delaiDepasse_avisSixMoisPlusUnJour() {
            BienEtreRpsConseillerPreventionRequest req =
                    new BienEtreRpsConseillerPreventionRequest(
                            BienEtreRpsConseillerPreventionTypeRisque.HARCELEMENT_MORAL,
                            BienEtreRpsConseillerPreventionModeDemande.FORMELLE_INDIVIDUELLE,
                            BienEtreRpsConseillerPreventionEtape.AVIS_RENDU,
                            true, true, true, true, true,
                            DATE_DEPOT, DATE_DEPOT.plusMonths(6).plusDays(1),
                            181);
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(req);
            assertThat(result.verdict()).isEqualTo(
                    BienEtreRpsConseillerPreventionVerdict.AVIS_RENDU_DELAI_DEPASSE);
            assertThat(result.delaiEnqueteRespecte()).isFalse();
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Formalisme par mode (informelle vs formelle)")
    class FormalismeParMode {

        @Test
        void informelle_entretienSuffit_ecritPasExige() {
            // Informelle : seul l'entretien préalable est exigé
            BienEtreRpsConseillerPreventionRequest req =
                    new BienEtreRpsConseillerPreventionRequest(
                            BienEtreRpsConseillerPreventionTypeRisque.AUTRES_RPS,
                            BienEtreRpsConseillerPreventionModeDemande.INFORMELLE,
                            BienEtreRpsConseillerPreventionEtape.DEMANDE_INFORMELLE_EN_COURS,
                            true, true, false, false, false,
                            null, null, null);
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(req);
            assertThat(result.formalitesPrealablesCompletes()).isTrue();
            assertThat(result.verdict()).isEqualTo(
                    BienEtreRpsConseillerPreventionVerdict.SAISINE_INFORMELLE_EN_COURS);
        }

        @Test
        void formelle_individuelle_exigeEntretien_ecrit_ar() {
            BienEtreRpsConseillerPreventionResult resultOk =
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            reqFormelleDeposee());
            assertThat(resultOk.formalitesPrealablesCompletes()).isTrue();
        }

        @Test
        void formelle_collective_exigeMemeFormalisme() {
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            reqFormelleCollective());
            assertThat(result.formalitesPrealablesCompletes()).isTrue();
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Base juridique et avertissement")
    class BaseJuridiqueAvertissement {

        @Test
        void baseJuridique_referencesObligatoires() {
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            reqFormelleDeposee());
            assertThat(result.baseJuridique())
                    .contains("Loi du 04/08/1996")
                    .contains("art. 32sexies")
                    .contains("AR du 10/04/2014")
                    .contains("art. 16")
                    .contains("art. 22")
                    .contains("art. 32")
                    .contains("CCT n° 72")
                    .contains("Cass. BE 23/12/2014")
                    .contains("Cass. BE 20/01/2014");
        }

        @Test
        void avertissement_avocatBeRequis() {
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            reqFormelleDeposee());
            assertThat(result.avertissement())
                    .isNotNull()
                    .contains("droit social BE")
                    .contains("SF-213-07")
                    .contains("F-DT-30");
        }

        @Test
        void synthese_porte_la_qualification_metier() {
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            reqFormelleDeposee());
            assertThat(result.synthese())
                    .contains("FORMELLE INDIVIDUELLE")
                    .contains("trois mois")
                    .contains("art. 22")
                    .contains("32sexies");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Validation d'entrée")
    class ValidationCases {

        @Test
        void requete_nulle_throws() {
            assertThatThrownBy(() ->
                    BienEtreRpsConseillerPreventionChecker.analyze(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Requête requise");
        }

        @Test
        void typeRisqueNull_throws() {
            assertThatThrownBy(() ->
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            new BienEtreRpsConseillerPreventionRequest(
                                    null,
                                    BienEtreRpsConseillerPreventionModeDemande.INFORMELLE,
                                    BienEtreRpsConseillerPreventionEtape.INTENTION_DEMANDE,
                                    true, true, true, true, true,
                                    null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("typeRisque");
        }

        @Test
        void modeDemandeNull_throws() {
            assertThatThrownBy(() ->
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            new BienEtreRpsConseillerPreventionRequest(
                                    BienEtreRpsConseillerPreventionTypeRisque.AUTRES_RPS,
                                    null,
                                    BienEtreRpsConseillerPreventionEtape.INTENTION_DEMANDE,
                                    true, true, true, true, true,
                                    null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("modeDemande");
        }

        @Test
        void etapeProcedureNull_throws() {
            assertThatThrownBy(() ->
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            new BienEtreRpsConseillerPreventionRequest(
                                    BienEtreRpsConseillerPreventionTypeRisque.AUTRES_RPS,
                                    BienEtreRpsConseillerPreventionModeDemande.INFORMELLE,
                                    null,
                                    true, true, true, true, true,
                                    null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("etapeProcedure");
        }

        @Test
        void conseillerIdentifieNull_throws() {
            assertThatThrownBy(() ->
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            new BienEtreRpsConseillerPreventionRequest(
                                    BienEtreRpsConseillerPreventionTypeRisque.AUTRES_RPS,
                                    BienEtreRpsConseillerPreventionModeDemande.INFORMELLE,
                                    BienEtreRpsConseillerPreventionEtape.INTENTION_DEMANDE,
                                    null, true, true, true, true,
                                    null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("conseillerIdentifie");
        }

        @Test
        void entretienPrealableNull_throws() {
            assertThatThrownBy(() ->
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            new BienEtreRpsConseillerPreventionRequest(
                                    BienEtreRpsConseillerPreventionTypeRisque.AUTRES_RPS,
                                    BienEtreRpsConseillerPreventionModeDemande.INFORMELLE,
                                    BienEtreRpsConseillerPreventionEtape.INTENTION_DEMANDE,
                                    true, null, true, true, true,
                                    null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("entretienPrealableRealise");
        }

        @Test
        void demandeEcriteNull_throws() {
            assertThatThrownBy(() ->
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            new BienEtreRpsConseillerPreventionRequest(
                                    BienEtreRpsConseillerPreventionTypeRisque.AUTRES_RPS,
                                    BienEtreRpsConseillerPreventionModeDemande.INFORMELLE,
                                    BienEtreRpsConseillerPreventionEtape.INTENTION_DEMANDE,
                                    true, true, null, true, true,
                                    null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("demandeEcriteSignee");
        }

        @Test
        void accuseReceptionNull_throws() {
            assertThatThrownBy(() ->
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            new BienEtreRpsConseillerPreventionRequest(
                                    BienEtreRpsConseillerPreventionTypeRisque.AUTRES_RPS,
                                    BienEtreRpsConseillerPreventionModeDemande.INFORMELLE,
                                    BienEtreRpsConseillerPreventionEtape.INTENTION_DEMANDE,
                                    true, true, true, null, true,
                                    null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("accuseReceptionRecu");
        }

        @Test
        void notificationEmployeurNull_throws() {
            assertThatThrownBy(() ->
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            new BienEtreRpsConseillerPreventionRequest(
                                    BienEtreRpsConseillerPreventionTypeRisque.AUTRES_RPS,
                                    BienEtreRpsConseillerPreventionModeDemande.INFORMELLE,
                                    BienEtreRpsConseillerPreventionEtape.INTENTION_DEMANDE,
                                    true, true, true, true, null,
                                    null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("notificationEmployeurFaite");
        }

        @Test
        void joursDepuisDepotNegatif_throws() {
            assertThatThrownBy(() ->
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            new BienEtreRpsConseillerPreventionRequest(
                                    BienEtreRpsConseillerPreventionTypeRisque.AUTRES_RPS,
                                    BienEtreRpsConseillerPreventionModeDemande.INFORMELLE,
                                    BienEtreRpsConseillerPreventionEtape.INTENTION_DEMANDE,
                                    true, true, true, true, true,
                                    null, null, -1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("joursDepuisDepot");
        }

        @Test
        void dateDepotManquante_pour_etape_formelle_throws() {
            assertThatThrownBy(() ->
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            new BienEtreRpsConseillerPreventionRequest(
                                    BienEtreRpsConseillerPreventionTypeRisque.HARCELEMENT_MORAL,
                                    BienEtreRpsConseillerPreventionModeDemande.FORMELLE_INDIVIDUELLE,
                                    BienEtreRpsConseillerPreventionEtape.DEMANDE_FORMELLE_DEPOSEE,
                                    true, true, true, true, true,
                                    null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("dateDepotDemande");
        }

        @Test
        void dateAvisManquante_pour_avis_rendu_throws() {
            assertThatThrownBy(() ->
                    BienEtreRpsConseillerPreventionChecker.analyze(
                            new BienEtreRpsConseillerPreventionRequest(
                                    BienEtreRpsConseillerPreventionTypeRisque.HARCELEMENT_MORAL,
                                    BienEtreRpsConseillerPreventionModeDemande.FORMELLE_INDIVIDUELLE,
                                    BienEtreRpsConseillerPreventionEtape.AVIS_RENDU,
                                    true, true, true, true, true,
                                    DATE_DEPOT, null, 90)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("dateAvisRendu");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Tous les types de risque couverts")
    class TypesRisqueCases {

        @Test
        void harcelementMoral_supporte() {
            BienEtreRpsConseillerPreventionRequest req =
                    new BienEtreRpsConseillerPreventionRequest(
                            BienEtreRpsConseillerPreventionTypeRisque.HARCELEMENT_MORAL,
                            BienEtreRpsConseillerPreventionModeDemande.FORMELLE_INDIVIDUELLE,
                            BienEtreRpsConseillerPreventionEtape.DEMANDE_FORMELLE_DEPOSEE,
                            true, true, true, true, true,
                            DATE_DEPOT, null, 5);
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(req);
            assertThat(result.typeRisque()).isEqualTo(
                    BienEtreRpsConseillerPreventionTypeRisque.HARCELEMENT_MORAL);
            assertThat(result.verdict()).isEqualTo(
                    BienEtreRpsConseillerPreventionVerdict.SAISINE_FORMELLE_EN_COURS);
        }

        @Test
        void harcelementSexuel_supporte() {
            BienEtreRpsConseillerPreventionRequest req =
                    new BienEtreRpsConseillerPreventionRequest(
                            BienEtreRpsConseillerPreventionTypeRisque.HARCELEMENT_SEXUEL,
                            BienEtreRpsConseillerPreventionModeDemande.FORMELLE_INDIVIDUELLE,
                            BienEtreRpsConseillerPreventionEtape.DEMANDE_FORMELLE_DEPOSEE,
                            true, true, true, true, true,
                            DATE_DEPOT, null, 5);
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(req);
            assertThat(result.typeRisque()).isEqualTo(
                    BienEtreRpsConseillerPreventionTypeRisque.HARCELEMENT_SEXUEL);
        }

        @Test
        void violence_supportee() {
            BienEtreRpsConseillerPreventionRequest req =
                    new BienEtreRpsConseillerPreventionRequest(
                            BienEtreRpsConseillerPreventionTypeRisque.VIOLENCE_AU_TRAVAIL,
                            BienEtreRpsConseillerPreventionModeDemande.FORMELLE_INDIVIDUELLE,
                            BienEtreRpsConseillerPreventionEtape.DEMANDE_FORMELLE_DEPOSEE,
                            true, true, true, true, true,
                            DATE_DEPOT, null, 5);
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(req);
            assertThat(result.typeRisque()).isEqualTo(
                    BienEtreRpsConseillerPreventionTypeRisque.VIOLENCE_AU_TRAVAIL);
        }

        @Test
        void autresRps_supportes() {
            BienEtreRpsConseillerPreventionRequest req =
                    new BienEtreRpsConseillerPreventionRequest(
                            BienEtreRpsConseillerPreventionTypeRisque.AUTRES_RPS,
                            BienEtreRpsConseillerPreventionModeDemande.FORMELLE_COLLECTIVE,
                            BienEtreRpsConseillerPreventionEtape.DEMANDE_FORMELLE_DEPOSEE,
                            true, true, true, true, true,
                            DATE_DEPOT, null, 5);
            BienEtreRpsConseillerPreventionResult result =
                    BienEtreRpsConseillerPreventionChecker.analyze(req);
            assertThat(result.typeRisque()).isEqualTo(
                    BienEtreRpsConseillerPreventionTypeRisque.AUTRES_RPS);
        }
    }
}
