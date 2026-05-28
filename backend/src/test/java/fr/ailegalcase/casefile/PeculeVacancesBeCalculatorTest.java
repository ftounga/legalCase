package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-20 : tests unitaires du
 * {@link PeculeVacancesBeCalculator}.
 *
 * <p>Couvre : calculs (pécule simple proratisé, double pécule 85 %
 * du salaire mensuel art. 38 AR 30/03/1967, pécule de départ 15,34 %
 * × (exercice + exercice précédent) art. 46), aiguillage par statut
 * (employé / ouvrier vers ONVA / jeune travailleur / indéterminé),
 * prescription 1 an post-contrat art. 15 Loi 03/07/1978, short-
 * circuits (déjà payé, jours insuffisants), validation des inputs
 * et base juridique stable.</p>
 */
class PeculeVacancesBeCalculatorTest {

    private static final BigDecimal REMUN_ANNUEL = new BigDecimal("36000.00");
    private static final BigDecimal REMUN_ANNUEL_PREC = new BigDecimal("34000.00");
    private static final BigDecimal REMUN_MENSUEL = new BigDecimal("3000.00");
    private static final BigDecimal JOURS_24 = new BigDecimal("24");
    private static final BigDecimal JOURS_12 = new BigDecimal("12");
    private static final LocalDate DATE_RECLAMATION =
            LocalDate.of(2024, 9, 1);
    private static final LocalDate DATE_FIN_CONTRAT_RECENT =
            LocalDate.of(2024, 6, 30);
    private static final LocalDate DATE_FIN_CONTRAT_ANCIEN =
            LocalDate.of(2022, 6, 30);

    private static PeculeVacancesBeRequest reqDoublePeculeEmploye() {
        return new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.EMPLOYE,
                PeculeVacancesBeTypeCalcul.DOUBLE_PECULE,
                REMUN_ANNUEL,
                REMUN_MENSUEL,
                JOURS_24,
                false, // pas déjà payé
                REMUN_ANNUEL_PREC,
                null, // dateSortie
                null, // dateFinContrat (pas de prescription en cours de contrat)
                DATE_RECLAMATION);
    }

    private static PeculeVacancesBeRequest reqPeculeSimpleEmploye(
            BigDecimal joursPris) {
        return new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.EMPLOYE,
                PeculeVacancesBeTypeCalcul.PECULE_SIMPLE,
                REMUN_ANNUEL,
                REMUN_MENSUEL,
                joursPris,
                false,
                REMUN_ANNUEL_PREC,
                null, null, DATE_RECLAMATION);
    }

    private static PeculeVacancesBeRequest reqPeculeDepartEmploye() {
        return new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.EMPLOYE,
                PeculeVacancesBeTypeCalcul.PECULE_DEPART,
                REMUN_ANNUEL,
                REMUN_MENSUEL,
                BigDecimal.ZERO, // pas de jours pris au moment du départ
                false,
                REMUN_ANNUEL_PREC,
                LocalDate.of(2024, 6, 30),
                DATE_FIN_CONTRAT_RECENT,
                DATE_RECLAMATION);
    }

    // ── Cas nominal : double pécule employé ─────────────────────────────────

    @Test
    void analyze_doublePeculeEmploye_calculOk() {
        PeculeVacancesBeResult r = PeculeVacancesBeCalculator
                .analyze(reqDoublePeculeEmploye());

        assertThat(r.verdict())
                .isEqualTo(PeculeVacancesBeVerdict.DU_DOUBLE_PECULE_CALCULE);
        // 3000 × 85 % = 2550
        assertThat(r.montantDoublePeculeEur())
                .isEqualByComparingTo("2550.00");
        assertThat(r.montantTotalDuEur())
                .isEqualByComparingTo("2550.00");
        assertThat(r.fractionLegaleAppliquee())
                .isEqualByComparingTo("0.85");
        assertThat(r.raison()).isEqualTo("DOUBLE_PECULE_CALCULE");
        assertThat(r.synthese()).contains("85", "vacances principales");
        assertThat(r.prescrit()).isFalse();
    }

    @Test
    void analyze_peculeSimpleEmploye_proratisationOk() {
        // 12 jours sur 24 = moitié = 1500
        PeculeVacancesBeResult r = PeculeVacancesBeCalculator
                .analyze(reqPeculeSimpleEmploye(JOURS_12));

        assertThat(r.verdict())
                .isEqualTo(PeculeVacancesBeVerdict.DU_PECULE_SIMPLE_CALCULE);
        assertThat(r.montantPeculeSimpleEur())
                .isEqualByComparingTo("1500.00");
        assertThat(r.montantTotalDuEur())
                .isEqualByComparingTo("1500.00");
        assertThat(r.raison()).isEqualTo("PECULE_SIMPLE_CALCULE");
    }

    @Test
    void analyze_peculeSimpleEmploye_24jours_egalSalaireComplet() {
        // 24 / 24 = 1 → rémunération mensuelle complète
        PeculeVacancesBeResult r = PeculeVacancesBeCalculator
                .analyze(reqPeculeSimpleEmploye(JOURS_24));

        assertThat(r.montantPeculeSimpleEur())
                .isEqualByComparingTo("3000.00");
    }

    @Test
    void analyze_peculeDepartEmploye_calculDeuxExercices() {
        // (36000 + 34000) × 15,34 % = 70000 × 0,1534 = 10738
        PeculeVacancesBeResult r = PeculeVacancesBeCalculator
                .analyze(reqPeculeDepartEmploye());

        assertThat(r.verdict())
                .isEqualTo(PeculeVacancesBeVerdict.DU_PECULE_DEPART_CALCULE);
        assertThat(r.montantPeculeDepartEur())
                .isEqualByComparingTo("10738.00");
        assertThat(r.montantTotalDuEur())
                .isEqualByComparingTo("10738.00");
        assertThat(r.fractionLegaleAppliquee())
                .isEqualByComparingTo("0.1534");
        assertThat(r.raison()).isEqualTo("PECULE_DEPART_CALCULE");
        assertThat(r.synthese()).contains("15,34", "art. 46");
    }

    // ── Hiérarchie 1 : statut indéterminé ───────────────────────────────────

    @Test
    void analyze_statutIndetermine_returnsAanalyser() {
        PeculeVacancesBeRequest req = new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.INDETERMINE,
                PeculeVacancesBeTypeCalcul.DOUBLE_PECULE,
                REMUN_ANNUEL, REMUN_MENSUEL, JOURS_24, false,
                REMUN_ANNUEL_PREC, null, null, DATE_RECLAMATION);

        PeculeVacancesBeResult r = PeculeVacancesBeCalculator.analyze(req);
        assertThat(r.verdict()).isEqualTo(PeculeVacancesBeVerdict.A_ANALYSER);
        assertThat(r.raison()).isEqualTo("STATUT_INDETERMINE");
        assertThat(r.montantTotalDuEur()).isEqualByComparingTo("0.00");
    }

    // ── Hiérarchie 2 : déjà payé ────────────────────────────────────────────

    @Test
    void analyze_dejaPaye_returnsNonDuDejaPaye() {
        PeculeVacancesBeRequest req = new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.EMPLOYE,
                PeculeVacancesBeTypeCalcul.DOUBLE_PECULE,
                REMUN_ANNUEL, REMUN_MENSUEL, JOURS_24, true,
                REMUN_ANNUEL_PREC, null, null, DATE_RECLAMATION);

        PeculeVacancesBeResult r = PeculeVacancesBeCalculator.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(PeculeVacancesBeVerdict.NON_DU_DEJA_PAYE);
        assertThat(r.raison()).isEqualTo("PECULE_DEJA_PAYE");
        assertThat(r.montantTotalDuEur()).isEqualByComparingTo("0.00");
    }

    // ── Hiérarchie 3 : ouvrier → ONVA ───────────────────────────────────────

    @Test
    void analyze_ouvrierPeculeSimple_returnsNonDuViaOnva() {
        PeculeVacancesBeRequest req = new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.OUVRIER,
                PeculeVacancesBeTypeCalcul.PECULE_SIMPLE,
                REMUN_ANNUEL, REMUN_MENSUEL, JOURS_24, false,
                REMUN_ANNUEL_PREC, null, null, DATE_RECLAMATION);

        PeculeVacancesBeResult r = PeculeVacancesBeCalculator.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(PeculeVacancesBeVerdict.NON_DU_OUVRIER_VIA_ONVA);
        assertThat(r.raison()).isEqualTo("OUVRIER_VIA_ONVA");
        assertThat(r.synthese()).contains("ONVA", "art. 19 AR 30/03/1967");
        assertThat(r.fractionLegaleAppliquee())
                .isEqualByComparingTo("0.1538");
    }

    @Test
    void analyze_ouvrierDoublePecule_returnsNonDuViaOnva() {
        PeculeVacancesBeRequest req = new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.OUVRIER,
                PeculeVacancesBeTypeCalcul.DOUBLE_PECULE,
                REMUN_ANNUEL, REMUN_MENSUEL, JOURS_24, false,
                REMUN_ANNUEL_PREC, null, null, DATE_RECLAMATION);

        assertThat(PeculeVacancesBeCalculator.analyze(req).verdict())
                .isEqualTo(PeculeVacancesBeVerdict.NON_DU_OUVRIER_VIA_ONVA);
    }

    @Test
    void analyze_ouvrierPeculeDepart_returnsNonDuViaOnva() {
        // Le pécule de départ ouvrier est intégré dans la liquidation
        // ONVA de l'année suivante — pas d'action directe employeur.
        PeculeVacancesBeRequest req = new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.OUVRIER,
                PeculeVacancesBeTypeCalcul.PECULE_DEPART,
                REMUN_ANNUEL, REMUN_MENSUEL, BigDecimal.ZERO, false,
                REMUN_ANNUEL_PREC, LocalDate.of(2024, 6, 30),
                DATE_FIN_CONTRAT_RECENT, DATE_RECLAMATION);

        assertThat(PeculeVacancesBeCalculator.analyze(req).verdict())
                .isEqualTo(PeculeVacancesBeVerdict.NON_DU_OUVRIER_VIA_ONVA);
    }

    // ── Hiérarchie 4 : prescription ─────────────────────────────────────────

    @Test
    void analyze_finContrat2022_reclamation2024_prescrit() {
        PeculeVacancesBeRequest req = new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.EMPLOYE,
                PeculeVacancesBeTypeCalcul.PECULE_DEPART,
                REMUN_ANNUEL, REMUN_MENSUEL, BigDecimal.ZERO, false,
                REMUN_ANNUEL_PREC, DATE_FIN_CONTRAT_ANCIEN,
                DATE_FIN_CONTRAT_ANCIEN, DATE_RECLAMATION);

        PeculeVacancesBeResult r = PeculeVacancesBeCalculator.analyze(req);
        assertThat(r.verdict()).isEqualTo(PeculeVacancesBeVerdict.PRESCRIT);
        assertThat(r.prescrit()).isTrue();
        assertThat(r.raison()).isEqualTo("PRESCRIT_ART_15_LOI_03_07_1978");
        assertThat(r.synthese()).contains("art. 15 Loi 03/07/1978");
        assertThat(r.joursDepuisFaitGenerateur()).isGreaterThan(365L);
    }

    @Test
    void analyze_finContratIlYa364Jours_nonPrescrit() {
        LocalDate finContrat = DATE_RECLAMATION.minusDays(364);
        PeculeVacancesBeRequest req = new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.EMPLOYE,
                PeculeVacancesBeTypeCalcul.PECULE_DEPART,
                REMUN_ANNUEL, REMUN_MENSUEL, BigDecimal.ZERO, false,
                REMUN_ANNUEL_PREC, finContrat,
                finContrat, DATE_RECLAMATION);

        PeculeVacancesBeResult r = PeculeVacancesBeCalculator.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(PeculeVacancesBeVerdict.DU_PECULE_DEPART_CALCULE);
        assertThat(r.prescrit()).isFalse();
        assertThat(r.joursDepuisFaitGenerateur()).isEqualTo(364L);
    }

    @Test
    void analyze_finContratIlYa366Jours_prescrit() {
        LocalDate finContrat = DATE_RECLAMATION.minusDays(366);
        PeculeVacancesBeRequest req = new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.EMPLOYE,
                PeculeVacancesBeTypeCalcul.PECULE_DEPART,
                REMUN_ANNUEL, REMUN_MENSUEL, BigDecimal.ZERO, false,
                REMUN_ANNUEL_PREC, finContrat,
                finContrat, DATE_RECLAMATION);

        assertThat(PeculeVacancesBeCalculator.analyze(req).verdict())
                .isEqualTo(PeculeVacancesBeVerdict.PRESCRIT);
    }

    @Test
    void analyze_pasFinContrat_pasPrescrit() {
        // En cours de contrat — pas de prescription
        PeculeVacancesBeResult r = PeculeVacancesBeCalculator
                .analyze(reqDoublePeculeEmploye());
        assertThat(r.prescrit()).isFalse();
        assertThat(r.joursDepuisFaitGenerateur()).isZero();
    }

    // ── Hiérarchie 5 : jeune travailleur ────────────────────────────────────

    @Test
    void analyze_jeuneTravailleurAucunJour_returnsJoursInsuffisants() {
        PeculeVacancesBeRequest req = new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.JEUNE_TRAVAILLEUR,
                PeculeVacancesBeTypeCalcul.PECULE_SIMPLE,
                REMUN_ANNUEL, REMUN_MENSUEL, BigDecimal.ZERO, false,
                REMUN_ANNUEL_PREC, null, null, DATE_RECLAMATION);

        PeculeVacancesBeResult r = PeculeVacancesBeCalculator.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(PeculeVacancesBeVerdict.NON_DU_JOURS_INSUFFISANTS);
        assertThat(r.raison())
                .isEqualTo("JEUNE_TRAVAILLEUR_AUCUN_JOUR_PRIS");
        assertThat(r.synthese()).contains("art. 9", "ONEM");
    }

    @Test
    void analyze_jeuneTravailleurAvecJours_calculOk() {
        PeculeVacancesBeRequest req = new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.JEUNE_TRAVAILLEUR,
                PeculeVacancesBeTypeCalcul.PECULE_SIMPLE,
                REMUN_ANNUEL, REMUN_MENSUEL, JOURS_12, false,
                REMUN_ANNUEL_PREC, null, null, DATE_RECLAMATION);

        PeculeVacancesBeResult r = PeculeVacancesBeCalculator.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(PeculeVacancesBeVerdict.DU_PECULE_SIMPLE_CALCULE);
        assertThat(r.montantPeculeSimpleEur())
                .isEqualByComparingTo("1500.00");
    }

    // ── Priorisation ────────────────────────────────────────────────────────

    @Test
    void analyze_dejaPayePrimeOuvrier() {
        // déjà payé primé sur ouvrier ONVA (logique du calculator)
        PeculeVacancesBeRequest req = new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.OUVRIER,
                PeculeVacancesBeTypeCalcul.PECULE_SIMPLE,
                REMUN_ANNUEL, REMUN_MENSUEL, JOURS_24, true, // déjà payé
                REMUN_ANNUEL_PREC, null, null, DATE_RECLAMATION);

        assertThat(PeculeVacancesBeCalculator.analyze(req).verdict())
                .isEqualTo(PeculeVacancesBeVerdict.NON_DU_DEJA_PAYE);
    }

    @Test
    void analyze_ouvrierPrimePrescription() {
        // Ouvrier court-circuite la prescription (action ONVA d'abord)
        PeculeVacancesBeRequest req = new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.OUVRIER,
                PeculeVacancesBeTypeCalcul.PECULE_DEPART,
                REMUN_ANNUEL, REMUN_MENSUEL, BigDecimal.ZERO, false,
                REMUN_ANNUEL_PREC, LocalDate.of(2022, 6, 30),
                DATE_FIN_CONTRAT_ANCIEN, DATE_RECLAMATION);

        assertThat(PeculeVacancesBeCalculator.analyze(req).verdict())
                .isEqualTo(PeculeVacancesBeVerdict.NON_DU_OUVRIER_VIA_ONVA);
    }

    // ── Snapshot ────────────────────────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        PeculeVacancesBeResult r = PeculeVacancesBeCalculator
                .analyze(reqDoublePeculeEmploye());

        assertThat(r.statut()).isEqualTo(PeculeVacancesBeStatut.EMPLOYE);
        assertThat(r.typeCalcul())
                .isEqualTo(PeculeVacancesBeTypeCalcul.DOUBLE_PECULE);
        assertThat(r.remunerationBruteAnnuelleExerciceEur())
                .isEqualByComparingTo("36000.00");
        assertThat(r.remunerationMensuelleBruteEur())
                .isEqualByComparingTo("3000.00");
        assertThat(r.joursCongesPris()).isEqualByComparingTo("24");
        assertThat(r.peculeDejaPaye()).isFalse();
        assertThat(r.remunerationBruteAnnuelleExercicePrecedentEur())
                .isEqualByComparingTo("34000.00");
        assertThat(r.dateSortie()).isNull();
        assertThat(r.dateFinContrat()).isNull();
        assertThat(r.dateReclamation()).isEqualTo(DATE_RECLAMATION);
    }

    @Test
    void analyze_outputsCalculesToujoursPresents() {
        // Même quand le verdict est NON_DU, les montants théoriques
        // sont calculés pour permettre une restitution UI complète.
        PeculeVacancesBeRequest req = new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.EMPLOYE,
                PeculeVacancesBeTypeCalcul.DOUBLE_PECULE,
                REMUN_ANNUEL, REMUN_MENSUEL, JOURS_24, true,
                REMUN_ANNUEL_PREC, null, null, DATE_RECLAMATION);

        PeculeVacancesBeResult r = PeculeVacancesBeCalculator.analyze(req);
        assertThat(r.montantPeculeSimpleEur()).isNotNull();
        assertThat(r.montantDoublePeculeEur())
                .isEqualByComparingTo("2550.00");
        assertThat(r.montantPeculeDepartEur())
                .isEqualByComparingTo("10738.00");
        // total dû à 0 puisque déjà payé
        assertThat(r.montantTotalDuEur()).isEqualByComparingTo("0.00");
    }

    // ── Base juridique + avertissement ──────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceLois1971Et1967() {
        PeculeVacancesBeResult r = PeculeVacancesBeCalculator
                .analyze(reqDoublePeculeEmploye());

        assertThat(r.baseJuridique())
                .contains("28/06/1971")
                .contains("AR du 30/03/1967")
                .contains("art. 19")
                .contains("art. 38")
                .contains("art. 46")
                .contains("ONVA")
                .contains("15,38")
                .contains("15,34")
                .contains("Loi du 03/07/1978")
                .contains("art. 15");
    }

    @Test
    void analyze_avertissementSystematique() {
        PeculeVacancesBeResult r = PeculeVacancesBeCalculator
                .analyze(reqDoublePeculeEmploye());

        assertThat(r.avertissement())
                .isNotNull()
                .contains("avocat")
                .contains("ONVA")
                .contains("108");
    }

    // ── Validation ──────────────────────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() -> PeculeVacancesBeCalculator.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_statutNull_throws() {
        PeculeVacancesBeRequest bad = new PeculeVacancesBeRequest(
                null, PeculeVacancesBeTypeCalcul.DOUBLE_PECULE,
                REMUN_ANNUEL, REMUN_MENSUEL, JOURS_24, false,
                REMUN_ANNUEL_PREC, null, null, DATE_RECLAMATION);

        assertThatThrownBy(() -> PeculeVacancesBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("statut");
    }

    @Test
    void analyze_typeCalculNull_throws() {
        PeculeVacancesBeRequest bad = new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.EMPLOYE, null,
                REMUN_ANNUEL, REMUN_MENSUEL, JOURS_24, false,
                REMUN_ANNUEL_PREC, null, null, DATE_RECLAMATION);

        assertThatThrownBy(() -> PeculeVacancesBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeCalcul");
    }

    @Test
    void analyze_remunAnnuelleNegative_throws() {
        PeculeVacancesBeRequest bad = new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.EMPLOYE,
                PeculeVacancesBeTypeCalcul.DOUBLE_PECULE,
                new BigDecimal("-1"), REMUN_MENSUEL, JOURS_24, false,
                REMUN_ANNUEL_PREC, null, null, DATE_RECLAMATION);

        assertThatThrownBy(() -> PeculeVacancesBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remunerationBruteAnnuelleExerciceEur");
    }

    @Test
    void analyze_remunMensuelleNegative_throws() {
        PeculeVacancesBeRequest bad = new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.EMPLOYE,
                PeculeVacancesBeTypeCalcul.DOUBLE_PECULE,
                REMUN_ANNUEL, new BigDecimal("-1"), JOURS_24, false,
                REMUN_ANNUEL_PREC, null, null, DATE_RECLAMATION);

        assertThatThrownBy(() -> PeculeVacancesBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remunerationMensuelleBruteEur");
    }

    @Test
    void analyze_joursNull_throws() {
        PeculeVacancesBeRequest bad = new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.EMPLOYE,
                PeculeVacancesBeTypeCalcul.DOUBLE_PECULE,
                REMUN_ANNUEL, REMUN_MENSUEL, null, false,
                REMUN_ANNUEL_PREC, null, null, DATE_RECLAMATION);

        assertThatThrownBy(() -> PeculeVacancesBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("joursCongesPris");
    }

    @Test
    void analyze_joursNegatif_throws() {
        PeculeVacancesBeRequest bad = new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.EMPLOYE,
                PeculeVacancesBeTypeCalcul.DOUBLE_PECULE,
                REMUN_ANNUEL, REMUN_MENSUEL, new BigDecimal("-1"),
                false, REMUN_ANNUEL_PREC, null, null, DATE_RECLAMATION);

        assertThatThrownBy(() -> PeculeVacancesBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("joursCongesPris");
    }

    @Test
    void analyze_dejaPayeNull_throws() {
        PeculeVacancesBeRequest bad = new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.EMPLOYE,
                PeculeVacancesBeTypeCalcul.DOUBLE_PECULE,
                REMUN_ANNUEL, REMUN_MENSUEL, JOURS_24, null,
                REMUN_ANNUEL_PREC, null, null, DATE_RECLAMATION);

        assertThatThrownBy(() -> PeculeVacancesBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("peculeDejaPaye");
    }

    @Test
    void analyze_remunPrecedentNull_throws() {
        PeculeVacancesBeRequest bad = new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.EMPLOYE,
                PeculeVacancesBeTypeCalcul.PECULE_DEPART,
                REMUN_ANNUEL, REMUN_MENSUEL, BigDecimal.ZERO, false,
                null, null, null, DATE_RECLAMATION);

        assertThatThrownBy(() -> PeculeVacancesBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remunerationBruteAnnuelleExercicePrecedentEur");
    }

    @Test
    void analyze_dateReclamationNull_throws() {
        PeculeVacancesBeRequest bad = new PeculeVacancesBeRequest(
                PeculeVacancesBeStatut.EMPLOYE,
                PeculeVacancesBeTypeCalcul.DOUBLE_PECULE,
                REMUN_ANNUEL, REMUN_MENSUEL, JOURS_24, false,
                REMUN_ANNUEL_PREC, null, null, null);

        assertThatThrownBy(() -> PeculeVacancesBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateReclamation");
    }
}
