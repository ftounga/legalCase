package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-15 : tests unitaires du
 * {@link InterimBeIndemniteFinMissionCalculator}.
 *
 * <p>Couvre : composantes ventilées (pécule vacances FSI on/off,
 * prime fin d'année sectorielle par CP, indemnité rupture anticipée
 * Cass. BE, sursalaire heures sup), hiérarchie des verdicts, validation
 * des inputs et base juridique stable. Vérifie également l'invariant
 * BE : pas de prime de précarité 10 % style FR.</p>
 */
class InterimBeIndemniteFinMissionCalculatorTest {

    private static final LocalDate DATE_DEBUT = LocalDate.of(2024, 6, 1);
    private static final LocalDate DATE_FIN_PREVUE = LocalDate.of(2024, 8, 30);
    private static final LocalDate DATE_FIN_REELLE = LocalDate.of(2024, 8, 30);

    /**
     * Cas de base : mission au terme prévu, 90 jours x 7,6 h/j = 684 h
     * prestées, salaire 16 €/h, pécule non encore versé, CP 200 employés,
     * ancienneté 100 j (≥ seuil 65 j).
     */
    private static InterimBeIndemniteFinMissionRequest reqStandard() {
        return new InterimBeIndemniteFinMissionRequest(
                DATE_DEBUT,
                DATE_FIN_PREVUE,
                DATE_FIN_REELLE,
                90,
                90,
                new BigDecimal("16.00"),
                new BigDecimal("684.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false,
                false,
                InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                100);
    }

    // ── Cas standard ──────────────────────────────────────────────────────

    @Test
    void analyze_standardSansRuptureSansHeuresSup_returnsIndemnitesDues() {
        InterimBeIndemniteFinMissionResult r =
                InterimBeIndemniteFinMissionCalculator.analyze(reqStandard());

        assertThat(r.verdict())
                .isEqualTo(InterimBeIndemniteFinMissionVerdict.INDEMNITES_DUES);
        // Salaire brut total = 16 × 684 = 10 944 €
        assertThat(r.salaireBrutTotalPrestations())
                .isEqualByComparingTo("10944.00");
        // Pécule = 10 944 × 15,38 % = 1 683,19 €
        assertThat(r.peculeVacancesInterim())
                .isEqualByComparingTo("1683.19");
        // Prime fin année CP 200 = 10 944 × 8,33 % = 911,6352 → HALF_UP 911,64 €
        assertThat(r.primeFinAnneeSectorielle())
                .isEqualByComparingTo("911.64");
        assertThat(r.indemniteRuptureAnticipee())
                .isEqualByComparingTo("0.00");
        assertThat(r.sursalaireHeuresSupplementaires())
                .isEqualByComparingTo("0.00");
        // Total = 1 683,19 + 911,64 = 2 594,83 €
        assertThat(r.totalIndemnitesBrutes())
                .isEqualByComparingTo("2594.83");
        assertThat(r.primePrecaritePresumee())
                .isEqualByComparingTo("0.00");
        assertThat(r.ancienneteSectorielleSuffisante()).isTrue();
        assertThat(r.raison()).isEqualTo("INDEMNITES_DUES");
        assertThat(r.synthese())
                .contains("pécule de vacances intérim")
                .contains("AUCUNE prime de précarité forfaitaire 10");
    }

    // ── Hiérarchie 1 : rupture anticipée Cass. BE prime sur tout ──────────

    @Test
    void analyze_ruptureAnticipeeSansMotifGrave_returnsResteACourir() {
        InterimBeIndemniteFinMissionRequest req =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT,
                        DATE_FIN_PREVUE,
                        LocalDate.of(2024, 7, 15),
                        45,
                        90,
                        new BigDecimal("16.00"),
                        new BigDecimal("342.00"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        false,
                        true,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                        100);

        InterimBeIndemniteFinMissionResult r =
                InterimBeIndemniteFinMissionCalculator.analyze(req);

        assertThat(r.verdict()).isEqualTo(
                InterimBeIndemniteFinMissionVerdict.RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR);
        // 45 jours restants × 7,6 h/j × 16 €/h = 5 472 €
        assertThat(r.joursRestantsACourirJusquAuTerme()).isEqualTo(45);
        assertThat(r.indemniteRuptureAnticipee())
                .isEqualByComparingTo("5472.00");
        assertThat(r.raison())
                .isEqualTo("RUPTURE_ANTICIPEE_RESTE_A_COURIR");
        assertThat(r.synthese())
                .contains("Cass. (BE) 04/02/1991")
                .contains("16/12/2002")
                .contains("AUCUNE prime de précarité forfaitaire 10");
    }

    @Test
    void analyze_ruptureAnticipeeFlagButZeroJoursRestants_pasResteACourir() {
        // Si fin réelle == fin prévue, joursRestants = 0 même si flag rupture
        InterimBeIndemniteFinMissionRequest req =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT,
                        DATE_FIN_PREVUE,
                        DATE_FIN_REELLE,
                        90,
                        90,
                        new BigDecimal("16.00"),
                        new BigDecimal("684.00"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        false,
                        true,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                        100);

        InterimBeIndemniteFinMissionResult r =
                InterimBeIndemniteFinMissionCalculator.analyze(req);

        // Pas de verdict rupture car aucun jour restant
        assertThat(r.verdict()).isEqualTo(
                InterimBeIndemniteFinMissionVerdict.INDEMNITES_DUES);
        assertThat(r.indemniteRuptureAnticipee())
                .isEqualByComparingTo("0.00");
    }

    // ── Hiérarchie 2 : CP non recensée ────────────────────────────────────

    @Test
    void analyze_cpNonRecensee_returnsAanalyser() {
        InterimBeIndemniteFinMissionRequest req =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT,
                        DATE_FIN_PREVUE,
                        DATE_FIN_REELLE,
                        90,
                        90,
                        new BigDecimal("16.00"),
                        new BigDecimal("684.00"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        false,
                        false,
                        InterimBeCommissionParitaire.AUTRE_NON_RECENSE,
                        100);

        InterimBeIndemniteFinMissionResult r =
                InterimBeIndemniteFinMissionCalculator.analyze(req);

        assertThat(r.verdict()).isEqualTo(
                InterimBeIndemniteFinMissionVerdict.A_ANALYSER_SECTEUR_NON_RECONNU);
        assertThat(r.primeFinAnneeSectorielle())
                .isEqualByComparingTo("0.00");
        // Pécule reste dû
        assertThat(r.peculeVacancesInterim())
                .isEqualByComparingTo("1683.19");
        assertThat(r.raison()).isEqualTo("SECTEUR_CP_NON_RECENSE");
        assertThat(r.synthese())
                .contains("Commission paritaire")
                .contains("non recensée")
                .contains("renvoi avocat");
    }

    // ── Hiérarchie 3 : aucune indemnité due ───────────────────────────────

    @Test
    void analyze_finAuTermeFsiPayeAncienneteInsuf_returnsAucuneIndem() {
        InterimBeIndemniteFinMissionRequest req =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT,
                        DATE_FIN_PREVUE,
                        DATE_FIN_REELLE,
                        20,
                        20,
                        new BigDecimal("16.00"),
                        new BigDecimal("152.00"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        true,
                        false,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                        20);

        InterimBeIndemniteFinMissionResult r =
                InterimBeIndemniteFinMissionCalculator.analyze(req);

        assertThat(r.verdict()).isEqualTo(
                InterimBeIndemniteFinMissionVerdict.AUCUNE_INDEMNITE_DUE);
        assertThat(r.peculeVacancesInterim())
                .isEqualByComparingTo("0.00");
        assertThat(r.primeFinAnneeSectorielle())
                .isEqualByComparingTo("0.00");
        assertThat(r.totalIndemnitesBrutes())
                .isEqualByComparingTo("0.00");
        assertThat(r.ancienneteSectorielleSuffisante()).isFalse();
        assertThat(r.raison()).isEqualTo("AUCUNE_INDEMNITE_DUE");
        assertThat(r.synthese())
                .contains("Aucune indemnité")
                .contains("AUCUNE prime de précarité forfaitaire 10");
    }

    // ── Pécule de vacances FSI on/off ─────────────────────────────────────

    @Test
    void analyze_peculeDejaVerseParFsi_zeroPecule() {
        InterimBeIndemniteFinMissionRequest req =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT,
                        DATE_FIN_PREVUE,
                        DATE_FIN_REELLE,
                        90,
                        90,
                        new BigDecimal("16.00"),
                        new BigDecimal("684.00"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        true,
                        false,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                        100);

        InterimBeIndemniteFinMissionResult r =
                InterimBeIndemniteFinMissionCalculator.analyze(req);

        assertThat(r.peculeVacancesInterim())
                .isEqualByComparingTo("0.00");
        // Prime fin année reste calculée
        assertThat(r.primeFinAnneeSectorielle())
                .isEqualByComparingTo("911.64");
        assertThat(r.verdict())
                .isEqualTo(InterimBeIndemniteFinMissionVerdict.INDEMNITES_DUES);
    }

    // ── Ancienneté sectorielle insuffisante ───────────────────────────────

    @Test
    void analyze_ancienneteInsuffisante_zeroPrimeFinAnnee() {
        InterimBeIndemniteFinMissionRequest req =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT,
                        DATE_FIN_PREVUE,
                        DATE_FIN_REELLE,
                        90,
                        90,
                        new BigDecimal("16.00"),
                        new BigDecimal("684.00"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        false,
                        false,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                        30);

        InterimBeIndemniteFinMissionResult r =
                InterimBeIndemniteFinMissionCalculator.analyze(req);

        assertThat(r.primeFinAnneeSectorielle())
                .isEqualByComparingTo("0.00");
        assertThat(r.ancienneteSectorielleSuffisante()).isFalse();
        // Pécule reste dû (1 683,19 €)
        assertThat(r.verdict())
                .isEqualTo(InterimBeIndemniteFinMissionVerdict.INDEMNITES_DUES);
        assertThat(r.totalIndemnitesBrutes())
                .isEqualByComparingTo("1683.19");
    }

    // ── Sursalaire heures sup ─────────────────────────────────────────────

    @Test
    void analyze_heuresSupSemaine_sursalaire50pct() {
        InterimBeIndemniteFinMissionRequest req =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT,
                        DATE_FIN_PREVUE,
                        DATE_FIN_REELLE,
                        90,
                        90,
                        new BigDecimal("16.00"),
                        new BigDecimal("684.00"),
                        new BigDecimal("10.00"),
                        BigDecimal.ZERO,
                        true,
                        false,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                        30);

        InterimBeIndemniteFinMissionResult r =
                InterimBeIndemniteFinMissionCalculator.analyze(req);

        // 10 h × 16 €/h × (1 + 0,5) = 240 €
        assertThat(r.sursalaireHeuresSupplementaires())
                .isEqualByComparingTo("240.00");
    }

    @Test
    void analyze_heuresSupDimancheFerie_sursalaire100pct() {
        InterimBeIndemniteFinMissionRequest req =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT,
                        DATE_FIN_PREVUE,
                        DATE_FIN_REELLE,
                        90,
                        90,
                        new BigDecimal("16.00"),
                        new BigDecimal("684.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("8.00"),
                        true,
                        false,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                        30);

        InterimBeIndemniteFinMissionResult r =
                InterimBeIndemniteFinMissionCalculator.analyze(req);

        // 8 h × 16 €/h × (1 + 1,0) = 256 €
        assertThat(r.sursalaireHeuresSupplementaires())
                .isEqualByComparingTo("256.00");
    }

    @Test
    void analyze_heuresSupCombinees_additionne() {
        InterimBeIndemniteFinMissionRequest req =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT,
                        DATE_FIN_PREVUE,
                        DATE_FIN_REELLE,
                        90,
                        90,
                        new BigDecimal("16.00"),
                        new BigDecimal("684.00"),
                        new BigDecimal("10.00"),
                        new BigDecimal("8.00"),
                        true,
                        false,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                        30);

        InterimBeIndemniteFinMissionResult r =
                InterimBeIndemniteFinMissionCalculator.analyze(req);

        // 240 + 256 = 496 €
        assertThat(r.sursalaireHeuresSupplementaires())
                .isEqualByComparingTo("496.00");
    }

    // ── Couverture des CP ─────────────────────────────────────────────────

    @Test
    void analyze_cp124Construction_taux912pct() {
        InterimBeIndemniteFinMissionRequest req = withCp(
                InterimBeCommissionParitaire.CP_124_CONSTRUCTION);
        InterimBeIndemniteFinMissionResult r =
                InterimBeIndemniteFinMissionCalculator.analyze(req);

        assertThat(r.tauxPrimeFinAnneeApplique())
                .isEqualByComparingTo("0.0912");
        // 10 944 × 9,12 % = 998,0928 → HALF_UP 998,09 €
        assertThat(r.primeFinAnneeSectorielle())
                .isEqualByComparingTo("998.09");
    }

    @Test
    void analyze_cp140Transport_taux833pct() {
        InterimBeIndemniteFinMissionRequest req = withCp(
                InterimBeCommissionParitaire.CP_140_TRANSPORT_LOGISTIQUE);
        InterimBeIndemniteFinMissionResult r =
                InterimBeIndemniteFinMissionCalculator.analyze(req);

        assertThat(r.tauxPrimeFinAnneeApplique())
                .isEqualByComparingTo("0.0833");
        assertThat(r.primeFinAnneeSectorielle())
                .isEqualByComparingTo("911.64");
    }

    @Test
    void analyze_cp209Metaux_taux833pct() {
        InterimBeIndemniteFinMissionRequest req = withCp(
                InterimBeCommissionParitaire.CP_209_METAUX_FABRICATIONS_METALLIQUES);
        InterimBeIndemniteFinMissionResult r =
                InterimBeIndemniteFinMissionCalculator.analyze(req);

        assertThat(r.tauxPrimeFinAnneeApplique())
                .isEqualByComparingTo("0.0833");
    }

    @Test
    void analyze_cp218NonMarchand_taux833pct() {
        InterimBeIndemniteFinMissionRequest req = withCp(
                InterimBeCommissionParitaire.CP_218_NON_MARCHAND);
        InterimBeIndemniteFinMissionResult r =
                InterimBeIndemniteFinMissionCalculator.analyze(req);

        assertThat(r.tauxPrimeFinAnneeApplique())
                .isEqualByComparingTo("0.0833");
    }

    @Test
    void analyze_cp322Interim_pasDePrimePropre() {
        InterimBeIndemniteFinMissionRequest req = withCp(
                InterimBeCommissionParitaire.CP_322_INTERIM);
        InterimBeIndemniteFinMissionResult r =
                InterimBeIndemniteFinMissionCalculator.analyze(req);

        assertThat(r.tauxPrimeFinAnneeApplique())
                .isEqualByComparingTo(BigDecimal.ZERO);
        // Prime fin année = 10 944 × 0 = 0 €
        assertThat(r.primeFinAnneeSectorielle())
                .isEqualByComparingTo("0.00");
    }

    // ── Snapshot inputs ───────────────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        InterimBeIndemniteFinMissionResult r =
                InterimBeIndemniteFinMissionCalculator.analyze(reqStandard());

        assertThat(r.dateDebutMission()).isEqualTo(DATE_DEBUT);
        assertThat(r.dateFinPrevue()).isEqualTo(DATE_FIN_PREVUE);
        assertThat(r.dateFinReelle()).isEqualTo(DATE_FIN_REELLE);
        assertThat(r.dureeReellePrestationJours()).isEqualTo(90);
        assertThat(r.dureePrevueJours()).isEqualTo(90);
        assertThat(r.salaireHoraireBrut()).isEqualByComparingTo("16.00");
        assertThat(r.heuresPrestees()).isEqualByComparingTo("684.00");
        assertThat(r.peculeDejaVerseParFsi()).isFalse();
        assertThat(r.ruptureAnticipeeParEtiSansMotifGrave()).isFalse();
        assertThat(r.commissionParitaireUtilisateur())
                .isEqualTo(InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES);
        assertThat(r.ancienneteSectorielleJours()).isEqualTo(100);
    }

    // ── Base juridique + avertissement ────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceTextesEtJurisprudence() {
        InterimBeIndemniteFinMissionResult r =
                InterimBeIndemniteFinMissionCalculator.analyze(reqStandard());

        assertThat(r.baseJuridique())
                .contains("Loi du 24/07/1987")
                .contains("AR du 30/03/1967")
                .contains("CCT n° 322")
                .contains("CCT n° 322bis")
                .contains("Cass. (BE) 04/02/1991")
                .contains("16/12/2002")
                .contains("Cass. (BE) 03/05/2010")
                .contains("23/06/2003")
                .contains("Loi du 16/03/1971");
    }

    @Test
    void analyze_avertissement_explicitePasDePrecarite10ptcStyleFr() {
        InterimBeIndemniteFinMissionResult r =
                InterimBeIndemniteFinMissionCalculator.analyze(reqStandard());

        assertThat(r.avertissement())
                .isNotNull()
                .contains("Aucune prime de précarité forfaitaire 10")
                .contains("L. 1251-32")
                .contains("avocat");
    }

    // ── Invariant BE : prime précarité = 0 quel que soit le verdict ───────

    @Test
    void analyze_primePrecaritePresumeeToujoursZero_invariantBE() {
        // Standard
        assertThat(InterimBeIndemniteFinMissionCalculator.analyze(reqStandard())
                .primePrecaritePresumee()).isEqualByComparingTo("0.00");

        // CP non recensée
        InterimBeIndemniteFinMissionRequest req2 =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT, DATE_FIN_PREVUE, DATE_FIN_REELLE,
                        90, 90, new BigDecimal("16.00"),
                        new BigDecimal("684.00"), BigDecimal.ZERO,
                        BigDecimal.ZERO, false, false,
                        InterimBeCommissionParitaire.AUTRE_NON_RECENSE, 100);
        assertThat(InterimBeIndemniteFinMissionCalculator.analyze(req2)
                .primePrecaritePresumee()).isEqualByComparingTo("0.00");

        // Aucune indemnité
        InterimBeIndemniteFinMissionRequest req3 =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT, DATE_FIN_PREVUE, DATE_FIN_REELLE,
                        20, 20, new BigDecimal("16.00"),
                        new BigDecimal("152.00"), BigDecimal.ZERO,
                        BigDecimal.ZERO, true, false,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES, 20);
        assertThat(InterimBeIndemniteFinMissionCalculator.analyze(req3)
                .primePrecaritePresumee()).isEqualByComparingTo("0.00");
    }

    // ── Constantes paramétriques exposées ─────────────────────────────────

    @Test
    void constantes_tauxPeculeEtSeuilAncienneteCoherents() {
        assertThat(InterimBeIndemniteFinMissionCalculator
                .TAUX_PECULE_VACANCES_INTERIM)
                .isEqualByComparingTo("0.1538");
        assertThat(InterimBeIndemniteFinMissionCalculator
                .ANCIENNETE_SECTORIELLE_MINIMALE_JOURS)
                .isEqualTo(65);
        assertThat(InterimBeIndemniteFinMissionCalculator
                .TAUX_SURSALAIRE_SEMAINE)
                .isEqualByComparingTo("0.50");
        assertThat(InterimBeIndemniteFinMissionCalculator
                .TAUX_SURSALAIRE_DIMANCHE_FERIE)
                .isEqualByComparingTo("1.00");
    }

    // ── Validation ────────────────────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(
                () -> InterimBeIndemniteFinMissionCalculator.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_dateDebutNull_throws() {
        InterimBeIndemniteFinMissionRequest bad =
                new InterimBeIndemniteFinMissionRequest(
                        null, DATE_FIN_PREVUE, DATE_FIN_REELLE, 90, 90,
                        new BigDecimal("16.00"), new BigDecimal("684.00"),
                        BigDecimal.ZERO, BigDecimal.ZERO, false, false,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                        100);
        assertThatThrownBy(
                () -> InterimBeIndemniteFinMissionCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDebutMission");
    }

    @Test
    void analyze_dateFinPrevueNull_throws() {
        InterimBeIndemniteFinMissionRequest bad =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT, null, DATE_FIN_REELLE, 90, 90,
                        new BigDecimal("16.00"), new BigDecimal("684.00"),
                        BigDecimal.ZERO, BigDecimal.ZERO, false, false,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                        100);
        assertThatThrownBy(
                () -> InterimBeIndemniteFinMissionCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateFinPrevue");
    }

    @Test
    void analyze_dateFinReelleNull_throws() {
        InterimBeIndemniteFinMissionRequest bad =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT, DATE_FIN_PREVUE, null, 90, 90,
                        new BigDecimal("16.00"), new BigDecimal("684.00"),
                        BigDecimal.ZERO, BigDecimal.ZERO, false, false,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                        100);
        assertThatThrownBy(
                () -> InterimBeIndemniteFinMissionCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateFinReelle");
    }

    @Test
    void analyze_dureeReelleNull_throws() {
        InterimBeIndemniteFinMissionRequest bad =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT, DATE_FIN_PREVUE, DATE_FIN_REELLE,
                        null, 90,
                        new BigDecimal("16.00"), new BigDecimal("684.00"),
                        BigDecimal.ZERO, BigDecimal.ZERO, false, false,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                        100);
        assertThatThrownBy(
                () -> InterimBeIndemniteFinMissionCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeReellePrestationJours");
    }

    @Test
    void analyze_dureeReelleNegative_throws() {
        InterimBeIndemniteFinMissionRequest bad =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT, DATE_FIN_PREVUE, DATE_FIN_REELLE,
                        -1, 90,
                        new BigDecimal("16.00"), new BigDecimal("684.00"),
                        BigDecimal.ZERO, BigDecimal.ZERO, false, false,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                        100);
        assertThatThrownBy(
                () -> InterimBeIndemniteFinMissionCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeReellePrestationJours");
    }

    @Test
    void analyze_dureePrevueZero_throws() {
        InterimBeIndemniteFinMissionRequest bad =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT, DATE_FIN_PREVUE, DATE_FIN_REELLE,
                        90, 0,
                        new BigDecimal("16.00"), new BigDecimal("684.00"),
                        BigDecimal.ZERO, BigDecimal.ZERO, false, false,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                        100);
        assertThatThrownBy(
                () -> InterimBeIndemniteFinMissionCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureePrevueJours");
    }

    @Test
    void analyze_salaireHoraireZero_throws() {
        InterimBeIndemniteFinMissionRequest bad =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT, DATE_FIN_PREVUE, DATE_FIN_REELLE,
                        90, 90,
                        BigDecimal.ZERO, new BigDecimal("684.00"),
                        BigDecimal.ZERO, BigDecimal.ZERO, false, false,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                        100);
        assertThatThrownBy(
                () -> InterimBeIndemniteFinMissionCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salaireHoraireBrut");
    }

    @Test
    void analyze_heuresPrestnegatif_throws() {
        InterimBeIndemniteFinMissionRequest bad =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT, DATE_FIN_PREVUE, DATE_FIN_REELLE,
                        90, 90,
                        new BigDecimal("16.00"), new BigDecimal("-1.00"),
                        BigDecimal.ZERO, BigDecimal.ZERO, false, false,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                        100);
        assertThatThrownBy(
                () -> InterimBeIndemniteFinMissionCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("heuresPrestees");
    }

    @Test
    void analyze_heuresSupSemaineNegative_throws() {
        InterimBeIndemniteFinMissionRequest bad =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT, DATE_FIN_PREVUE, DATE_FIN_REELLE,
                        90, 90,
                        new BigDecimal("16.00"), new BigDecimal("684.00"),
                        new BigDecimal("-1.00"), BigDecimal.ZERO,
                        false, false,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                        100);
        assertThatThrownBy(
                () -> InterimBeIndemniteFinMissionCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("heuresSupplementairesSemaine");
    }

    @Test
    void analyze_heuresSupDimancheNegative_throws() {
        InterimBeIndemniteFinMissionRequest bad =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT, DATE_FIN_PREVUE, DATE_FIN_REELLE,
                        90, 90,
                        new BigDecimal("16.00"), new BigDecimal("684.00"),
                        BigDecimal.ZERO, new BigDecimal("-1.00"),
                        false, false,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                        100);
        assertThatThrownBy(
                () -> InterimBeIndemniteFinMissionCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("heuresSupplementairesDimancheFerie");
    }

    @Test
    void analyze_peculeFsiNull_throws() {
        InterimBeIndemniteFinMissionRequest bad =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT, DATE_FIN_PREVUE, DATE_FIN_REELLE,
                        90, 90,
                        new BigDecimal("16.00"), new BigDecimal("684.00"),
                        BigDecimal.ZERO, BigDecimal.ZERO,
                        null, false,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                        100);
        assertThatThrownBy(
                () -> InterimBeIndemniteFinMissionCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("peculeDejaVerseParFsi");
    }

    @Test
    void analyze_ruptureAnticipeeNull_throws() {
        InterimBeIndemniteFinMissionRequest bad =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT, DATE_FIN_PREVUE, DATE_FIN_REELLE,
                        90, 90,
                        new BigDecimal("16.00"), new BigDecimal("684.00"),
                        BigDecimal.ZERO, BigDecimal.ZERO,
                        false, null,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                        100);
        assertThatThrownBy(
                () -> InterimBeIndemniteFinMissionCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ruptureAnticipeeParEtiSansMotifGrave");
    }

    @Test
    void analyze_cpNull_throws() {
        InterimBeIndemniteFinMissionRequest bad =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT, DATE_FIN_PREVUE, DATE_FIN_REELLE,
                        90, 90,
                        new BigDecimal("16.00"), new BigDecimal("684.00"),
                        BigDecimal.ZERO, BigDecimal.ZERO,
                        false, false, null, 100);
        assertThatThrownBy(
                () -> InterimBeIndemniteFinMissionCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("commissionParitaireUtilisateur");
    }

    @Test
    void analyze_ancienneteNull_throws() {
        InterimBeIndemniteFinMissionRequest bad =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT, DATE_FIN_PREVUE, DATE_FIN_REELLE,
                        90, 90,
                        new BigDecimal("16.00"), new BigDecimal("684.00"),
                        BigDecimal.ZERO, BigDecimal.ZERO,
                        false, false,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                        null);
        assertThatThrownBy(
                () -> InterimBeIndemniteFinMissionCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ancienneteSectorielleJours");
    }

    @Test
    void analyze_ancienneteNegative_throws() {
        InterimBeIndemniteFinMissionRequest bad =
                new InterimBeIndemniteFinMissionRequest(
                        DATE_DEBUT, DATE_FIN_PREVUE, DATE_FIN_REELLE,
                        90, 90,
                        new BigDecimal("16.00"), new BigDecimal("684.00"),
                        BigDecimal.ZERO, BigDecimal.ZERO,
                        false, false,
                        InterimBeCommissionParitaire.CP_200_AUXILIAIRE_EMPLOYES,
                        -1);
        assertThatThrownBy(
                () -> InterimBeIndemniteFinMissionCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ancienneteSectorielleJours");
    }

    // ── Helper ─────────────────────────────────────────────────────────────

    private static InterimBeIndemniteFinMissionRequest withCp(
            InterimBeCommissionParitaire cp) {
        return new InterimBeIndemniteFinMissionRequest(
                DATE_DEBUT,
                DATE_FIN_PREVUE,
                DATE_FIN_REELLE,
                90, 90,
                new BigDecimal("16.00"),
                new BigDecimal("684.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false, false,
                cp, 100);
    }
}
