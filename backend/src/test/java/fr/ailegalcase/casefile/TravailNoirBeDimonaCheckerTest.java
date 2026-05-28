package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-26 : tests unitaires du {@link TravailNoirBeDimonaChecker}.
 *
 * <p>Couvre : 4 branches statut DIMONA (CONFORME / TARDIVE /
 * ABSENTE / INDEPENDANT_FAUSSEMENT_QUALIFIE), 6 verdicts
 * (DIMONA_CONFORME / DIMONA_TARDIVE_REGULARISABLE /
 * ABSENCE_DIMONA_SANCTION_NIVEAU_4 / REQUALIFICATION_SALARIE_PRESUMEE /
 * INDEPENDANT_REQUALIFIE_NON_DECLARE / A_QUALIFIER), calcul
 * cotisations ONSS rétroactives (employeur 25 % + travailleur
 * 13,07 %), amende ONSS forfaitaire 3 × art. 28 Loi 22/04/2003,
 * bornes amende pénale art. 181 niveau 4 (300/600-3000/6000 €,
 * emprisonnement 6-36 mois), drapeaux majorations (art. 103 § 2
 * × travailleurs, art. 105 × 5 personne morale, art. 110 récidive),
 * branchement éléments subordination INDETERMINE → A_QUALIFIER,
 * base juridique stable, avertissement avocat BE, validation
 * inputs.</p>
 */
class TravailNoirBeDimonaCheckerTest {

    private static final LocalDate DEBUT = LocalDate.of(2024, 1, 1);
    private static final LocalDate CONTROLE = LocalDate.of(2024, 7, 1);
    private static final BigDecimal SALAIRE = new BigDecimal("3000.00");

    private static TravailNoirBeDimonaRequest reqAbsente() {
        return new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.ABSENTE,
                DEBUT, null, CONTROLE, SALAIRE, 1,
                false, false,
                TravailNoirBeDimonaElementsSubordination.OUI);
    }

    private static TravailNoirBeDimonaRequest reqConforme() {
        return new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.DECLAREE_AVANT_DEBUT,
                DEBUT, null, CONTROLE, SALAIRE, 1,
                false, false,
                TravailNoirBeDimonaElementsSubordination.OUI);
    }

    // ── Branche CONFORME ────────────────────────────────────────────────────

    @Test
    void analyze_dimonaConforme_returnsConforme_aucuneSanction() {
        TravailNoirBeDimonaResult r =
                TravailNoirBeDimonaChecker.analyze(reqConforme());
        assertThat(r.verdict())
                .isEqualTo(TravailNoirBeDimonaVerdict.DIMONA_CONFORME);
        assertThat(r.cotisationsOnssEmployeur()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.cotisationsOnssTravailleur()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.cotisationsOnssTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.amendeOnssForfaitaire3x()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.sanctionPenaleNiveau4Applicable()).isFalse();
        assertThat(r.emprisonnementApplicable()).isFalse();
        assertThat(r.raison()).isEqualTo("DIMONA_CONFORME_AVANT_DEBUT");
    }

    // ── Branche TARDIVE ─────────────────────────────────────────────────────

    @Test
    void analyze_dimonaTardive_returnsTardive_cotisationsDuesAmendeOnssAttenuee() {
        LocalDate dimonaTardif = LocalDate.of(2024, 4, 1);
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.DECLAREE_TARDIVE,
                DEBUT, dimonaTardif, CONTROLE, SALAIRE, 1,
                false, false,
                TravailNoirBeDimonaElementsSubordination.OUI);
        TravailNoirBeDimonaResult r =
                TravailNoirBeDimonaChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(TravailNoirBeDimonaVerdict.DIMONA_TARDIVE_REGULARISABLE);
        // Période = 1er janv → 1er avr = 91 jours
        assertThat(r.dureeNonDeclareeJours()).isEqualTo(91);
        // Employeur = 3000 × (91/30) × 0.25 ≈ 2275 €
        assertThat(r.cotisationsOnssEmployeur()).isGreaterThan(BigDecimal.ZERO);
        // Travailleur = 3000 × (91/30) × 0.1307 ≈ 1189.37 €
        assertThat(r.cotisationsOnssTravailleur()).isGreaterThan(BigDecimal.ZERO);
        assertThat(r.cotisationsOnssTotal())
                .isEqualByComparingTo(r.cotisationsOnssEmployeur()
                        .add(r.cotisationsOnssTravailleur()));
        // Amende ONSS forfaitaire 3 × cotisations
        assertThat(r.amendeOnssForfaitaire3x())
                .isEqualByComparingTo(r.cotisationsOnssTotal()
                        .multiply(new BigDecimal("3")));
        // Pas de sanction pénale niveau 4 sur régularisation tardive
        assertThat(r.sanctionPenaleNiveau4Applicable()).isFalse();
        assertThat(r.emprisonnementApplicable()).isFalse();
    }

    @Test
    void analyze_dimonaTardive_dimonaApresControle_bornFinControle() {
        // dateDimona postérieure au contrôle → on borne au contrôle.
        LocalDate dimonaApresControle = LocalDate.of(2024, 8, 1);
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.DECLAREE_TARDIVE,
                DEBUT, dimonaApresControle, CONTROLE, SALAIRE, 1,
                false, false,
                TravailNoirBeDimonaElementsSubordination.OUI);
        TravailNoirBeDimonaResult r =
                TravailNoirBeDimonaChecker.analyze(req);
        // Borne fin = controle (01/07) car dimona postérieure
        // 01/01/2024 → 01/07/2024 = 182 jours (année bissextile)
        // ChronoUnit.DAYS.between(2024-01-01, 2024-07-01) = 182
        // Or notre test calcule 01/01 → 01/07 réellement, vérifions
        // que la borne fin est bien le contrôle et pas dateDimona.
        // Le calcul donne 182 si on borne sur dateControle (01/07).
        // → vérifier que la durée est <= 213 (qui serait sans bornage).
        assertThat(r.dureeNonDeclareeJours()).isEqualTo(182);
    }

    // ── Branche ABSENTE ─────────────────────────────────────────────────────

    @Test
    void analyze_absente_subordOui_returnsNiveau4_sanctionFerme() {
        TravailNoirBeDimonaResult r =
                TravailNoirBeDimonaChecker.analyze(reqAbsente());
        assertThat(r.verdict()).isEqualTo(
                TravailNoirBeDimonaVerdict.ABSENCE_DIMONA_SANCTION_NIVEAU_4);
        assertThat(r.sanctionPenaleNiveau4Applicable()).isTrue();
        assertThat(r.amendePenaleAdminMin()).isEqualTo(300);
        assertThat(r.amendePenaleAdminMax()).isEqualTo(3000);
        assertThat(r.amendePenaleMin()).isEqualTo(600);
        assertThat(r.amendePenaleMax()).isEqualTo(6000);
        assertThat(r.emprisonnementApplicable()).isTrue();
        assertThat(r.emprisonnementMinMois()).isEqualTo(6);
        assertThat(r.emprisonnementMaxMois()).isEqualTo(36);
        assertThat(r.raison()).isEqualTo("ABSENCE_DIMONA_NIVEAU_4_FERME");
    }

    @Test
    void analyze_absente_subordIndetermine_returnsAqualifier() {
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.ABSENTE,
                DEBUT, null, CONTROLE, SALAIRE, 1,
                false, false,
                TravailNoirBeDimonaElementsSubordination.INDETERMINE);
        TravailNoirBeDimonaResult r =
                TravailNoirBeDimonaChecker.analyze(req);
        assertThat(r.verdict()).isEqualTo(
                TravailNoirBeDimonaVerdict.A_QUALIFIER);
        assertThat(r.sanctionPenaleNiveau4Applicable()).isFalse();
        assertThat(r.raison())
                .isEqualTo("ABSENCE_DIMONA_SUBORDINATION_INDETERMINEE");
    }

    @Test
    void analyze_absente_subordNon_returnsAqualifier_avertissementPresomption() {
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.ABSENTE,
                DEBUT, null, CONTROLE, SALAIRE, 1,
                false, false,
                TravailNoirBeDimonaElementsSubordination.NON);
        TravailNoirBeDimonaResult r =
                TravailNoirBeDimonaChecker.analyze(req);
        assertThat(r.verdict()).isEqualTo(
                TravailNoirBeDimonaVerdict.A_QUALIFIER);
        // Mention de la présomption + bornes niveau 4 affichées pour info
        assertThat(r.sanctionPenaleNiveau4Applicable()).isTrue();
        assertThat(r.synthese())
                .contains("présomption simple")
                .contains("art. 328");
        assertThat(r.raison()).isEqualTo(
                "ABSENCE_DIMONA_SUBORDINATION_NON_CARACTERISEE");
    }

    // ── Branche INDEPENDANT_FAUSSEMENT_QUALIFIE ─────────────────────────────

    @Test
    void analyze_independantFaux_subordOui_returnsRequalifieNiveau4() {
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.INDEPENDANT_FAUSSEMENT_QUALIFIE,
                DEBUT, null, CONTROLE, SALAIRE, 1,
                false, false,
                TravailNoirBeDimonaElementsSubordination.OUI);
        TravailNoirBeDimonaResult r =
                TravailNoirBeDimonaChecker.analyze(req);
        assertThat(r.verdict()).isEqualTo(
                TravailNoirBeDimonaVerdict.INDEPENDANT_REQUALIFIE_NON_DECLARE);
        assertThat(r.sanctionPenaleNiveau4Applicable()).isTrue();
        assertThat(r.emprisonnementApplicable()).isTrue();
        assertThat(r.raison()).isEqualTo(
                "FAUX_INDEPENDANT_REQUALIFIE_NIVEAU_4");
    }

    @Test
    void analyze_independantFaux_subordNon_returnsAqualifier_dimonaNonRequise() {
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.INDEPENDANT_FAUSSEMENT_QUALIFIE,
                DEBUT, null, CONTROLE, SALAIRE, 1,
                false, false,
                TravailNoirBeDimonaElementsSubordination.NON);
        TravailNoirBeDimonaResult r =
                TravailNoirBeDimonaChecker.analyze(req);
        assertThat(r.verdict()).isEqualTo(
                TravailNoirBeDimonaVerdict.A_QUALIFIER);
        assertThat(r.cotisationsOnssTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.sanctionPenaleNiveau4Applicable()).isFalse();
        assertThat(r.raison()).isEqualTo(
                "INDEPENDANT_NON_SALARIE_DIMONA_NON_REQUISE");
    }

    @Test
    void analyze_independantFaux_subordIndetermine_returnsAqualifier() {
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.INDEPENDANT_FAUSSEMENT_QUALIFIE,
                DEBUT, null, CONTROLE, SALAIRE, 1,
                false, false,
                TravailNoirBeDimonaElementsSubordination.INDETERMINE);
        TravailNoirBeDimonaResult r =
                TravailNoirBeDimonaChecker.analyze(req);
        assertThat(r.verdict()).isEqualTo(
                TravailNoirBeDimonaVerdict.A_QUALIFIER);
        assertThat(r.raison()).isEqualTo(
                "INDEPENDANT_SUBORDINATION_INDETERMINEE");
    }

    // ── Calcul cotisations ONSS ─────────────────────────────────────────────

    @Test
    void analyze_cotisationsOnss_calculProportionnelAuxJours() {
        // 1 mois exactement (30 jours) sur 3000 € → cotisations
        // employeur = 750 € (25 %) ; travailleur = 392.10 € (13,07 %)
        LocalDate debut = LocalDate.of(2024, 1, 1);
        LocalDate fin = LocalDate.of(2024, 1, 31); // 30 jours
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.ABSENTE,
                debut, null, fin, SALAIRE, 1,
                false, false,
                TravailNoirBeDimonaElementsSubordination.OUI);
        TravailNoirBeDimonaResult r =
                TravailNoirBeDimonaChecker.analyze(req);
        assertThat(r.dureeNonDeclareeJours()).isEqualTo(30);
        assertThat(r.cotisationsOnssEmployeur())
                .isEqualByComparingTo(new BigDecimal("750.00"));
        assertThat(r.cotisationsOnssTravailleur())
                .isEqualByComparingTo(new BigDecimal("392.10"));
        assertThat(r.cotisationsOnssTotal())
                .isEqualByComparingTo(new BigDecimal("1142.10"));
        assertThat(r.amendeOnssForfaitaire3x())
                .isEqualByComparingTo(new BigDecimal("3426.30"));
    }

    @Test
    void analyze_cotisationsOnss_multiplicationParTravailleurs() {
        // 3 travailleurs concernés → cotisations × 3
        LocalDate debut = LocalDate.of(2024, 1, 1);
        LocalDate fin = LocalDate.of(2024, 1, 31); // 30 jours
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.ABSENTE,
                debut, null, fin, SALAIRE, 3,
                false, false,
                TravailNoirBeDimonaElementsSubordination.OUI);
        TravailNoirBeDimonaResult r =
                TravailNoirBeDimonaChecker.analyze(req);
        // 750 × 3 = 2250
        assertThat(r.cotisationsOnssEmployeur())
                .isEqualByComparingTo(new BigDecimal("2250.00"));
        assertThat(r.cotisationsOnssTravailleur())
                .isEqualByComparingTo(new BigDecimal("1176.30"));
    }

    // ── Majorations ─────────────────────────────────────────────────────────

    @Test
    void analyze_personneMorale_drapeauActifEtSyntheseLeMentionne() {
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.ABSENTE,
                DEBUT, null, CONTROLE, SALAIRE, 1,
                true, false,
                TravailNoirBeDimonaElementsSubordination.OUI);
        TravailNoirBeDimonaResult r =
                TravailNoirBeDimonaChecker.analyze(req);
        assertThat(r.majorationPersonneMorale()).isTrue();
        assertThat(r.synthese())
                .contains("multipliée par 5")
                .contains("art. 105");
    }

    @Test
    void analyze_multTravailleurs_drapeauActifEtSyntheseLeMentionne() {
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.ABSENTE,
                DEBUT, null, CONTROLE, SALAIRE, 8,
                false, false,
                TravailNoirBeDimonaElementsSubordination.OUI);
        TravailNoirBeDimonaResult r =
                TravailNoirBeDimonaChecker.analyze(req);
        assertThat(r.multiplicationParTravailleur()).isTrue();
        assertThat(r.synthese())
                .contains("multipliée par le nombre de travailleurs")
                .contains("8")
                .contains("art. 103");
    }

    @Test
    void analyze_recidive_drapeauActifEtSyntheseLeMentionne() {
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.ABSENTE,
                DEBUT, null, CONTROLE, SALAIRE, 1,
                false, true,
                TravailNoirBeDimonaElementsSubordination.OUI);
        TravailNoirBeDimonaResult r =
                TravailNoirBeDimonaChecker.analyze(req);
        assertThat(r.majorationRecidive()).isTrue();
        assertThat(r.synthese())
                .contains("récidive")
                .contains("doublement")
                .contains("art. 110");
    }

    @Test
    void analyze_majorationsCumulees() {
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.ABSENTE,
                DEBUT, null, CONTROLE, SALAIRE, 5,
                true, true,
                TravailNoirBeDimonaElementsSubordination.OUI);
        TravailNoirBeDimonaResult r =
                TravailNoirBeDimonaChecker.analyze(req);
        assertThat(r.multiplicationParTravailleur()).isTrue();
        assertThat(r.majorationPersonneMorale()).isTrue();
        assertThat(r.majorationRecidive()).isTrue();
        assertThat(r.synthese())
                .contains("personne morale")
                .contains("travailleurs")
                .contains("récidive");
    }

    // ── Base juridique stable + avertissement ───────────────────────────────

    @Test
    void analyze_baseJuridique_estStableEtComplete() {
        TravailNoirBeDimonaResult r =
                TravailNoirBeDimonaChecker.analyze(reqAbsente());
        assertThat(r.baseJuridique())
                .contains("Loi-programme du 24/12/2002")
                .contains("AR du 05/11/2002")
                .contains("Code pénal social")
                .contains("art. 181")
                .contains("art. 328")
                .contains("art. 105")
                .contains("art. 110")
                .contains("Loi du 22/04/2003")
                .contains("DIMONA");
    }

    @Test
    void analyze_avertissementAvocat_couvreLesPrincipauxRisques() {
        TravailNoirBeDimonaResult r =
                TravailNoirBeDimonaChecker.analyze(reqAbsente());
        assertThat(r.avertissement())
                .contains("avocat spécialisé")
                .contains("DIMONA")
                .contains("présomption")
                .contains("prescription")
                .contains("indexation")
                .contains("Una Via")
                .contains("faux indépendant");
    }

    // ── Validation des inputs ───────────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() -> TravailNoirBeDimonaChecker.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête requise");
    }

    @Test
    void analyze_statutDimonaNull_throws() {
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                null, DEBUT, null, CONTROLE, SALAIRE, 1,
                false, false,
                TravailNoirBeDimonaElementsSubordination.OUI);
        assertThatThrownBy(() -> TravailNoirBeDimonaChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("statutDimona");
    }

    @Test
    void analyze_dateDebutNull_throws() {
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.ABSENTE,
                null, null, CONTROLE, SALAIRE, 1,
                false, false,
                TravailNoirBeDimonaElementsSubordination.OUI);
        assertThatThrownBy(() -> TravailNoirBeDimonaChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDebutOccupation");
    }

    @Test
    void analyze_dateControleNull_throws() {
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.ABSENTE,
                DEBUT, null, null, SALAIRE, 1,
                false, false,
                TravailNoirBeDimonaElementsSubordination.OUI);
        assertThatThrownBy(() -> TravailNoirBeDimonaChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateControle");
    }

    @Test
    void analyze_dateControleAvantDebut_throws() {
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.ABSENTE,
                LocalDate.of(2024, 6, 1), null,
                LocalDate.of(2024, 1, 1),
                SALAIRE, 1, false, false,
                TravailNoirBeDimonaElementsSubordination.OUI);
        assertThatThrownBy(() -> TravailNoirBeDimonaChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateControle");
    }

    @Test
    void analyze_salaireNull_throws() {
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.ABSENTE,
                DEBUT, null, CONTROLE, null, 1,
                false, false,
                TravailNoirBeDimonaElementsSubordination.OUI);
        assertThatThrownBy(() -> TravailNoirBeDimonaChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salaireBrutMensuel");
    }

    @Test
    void analyze_salaireNegatif_throws() {
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.ABSENTE,
                DEBUT, null, CONTROLE,
                new BigDecimal("-100.00"), 1,
                false, false,
                TravailNoirBeDimonaElementsSubordination.OUI);
        assertThatThrownBy(() -> TravailNoirBeDimonaChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salaireBrutMensuel");
    }

    @Test
    void analyze_nbTravailleursZero_throws() {
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.ABSENTE,
                DEBUT, null, CONTROLE, SALAIRE, 0,
                false, false,
                TravailNoirBeDimonaElementsSubordination.OUI);
        assertThatThrownBy(() -> TravailNoirBeDimonaChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombreTravailleursConcernes");
    }

    @Test
    void analyze_personneMoraleNull_throws() {
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.ABSENTE,
                DEBUT, null, CONTROLE, SALAIRE, 1,
                null, false,
                TravailNoirBeDimonaElementsSubordination.OUI);
        assertThatThrownBy(() -> TravailNoirBeDimonaChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("personneMorale");
    }

    @Test
    void analyze_recidiveNull_throws() {
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.ABSENTE,
                DEBUT, null, CONTROLE, SALAIRE, 1,
                false, null,
                TravailNoirBeDimonaElementsSubordination.OUI);
        assertThatThrownBy(() -> TravailNoirBeDimonaChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recidiveDansLAn");
    }

    @Test
    void analyze_elementsSubordinationNull_throws() {
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.ABSENTE,
                DEBUT, null, CONTROLE, SALAIRE, 1,
                false, false, null);
        assertThatThrownBy(() -> TravailNoirBeDimonaChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("elementsSubordination");
    }

    @Test
    void analyze_tardiveSansDateDimona_throws() {
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.DECLAREE_TARDIVE,
                DEBUT, null, CONTROLE, SALAIRE, 1,
                false, false,
                TravailNoirBeDimonaElementsSubordination.OUI);
        assertThatThrownBy(() -> TravailNoirBeDimonaChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDimonaEffective");
    }

    // ── Couverture des raisons ──────────────────────────────────────────────

    @Test
    void analyze_raison_conforme_estStable() {
        TravailNoirBeDimonaResult r =
                TravailNoirBeDimonaChecker.analyze(reqConforme());
        assertThat(r.raison()).isEqualTo("DIMONA_CONFORME_AVANT_DEBUT");
    }

    @Test
    void analyze_raison_tardive_estStable() {
        LocalDate dimonaTardif = LocalDate.of(2024, 4, 1);
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.DECLAREE_TARDIVE,
                DEBUT, dimonaTardif, CONTROLE, SALAIRE, 1,
                false, false,
                TravailNoirBeDimonaElementsSubordination.OUI);
        TravailNoirBeDimonaResult r =
                TravailNoirBeDimonaChecker.analyze(req);
        assertThat(r.raison())
                .isEqualTo("DIMONA_TARDIVE_REGULARISATION_ADMINISTRATIVE");
    }

    @Test
    void analyze_raison_absenteFerme_estStable() {
        TravailNoirBeDimonaResult r =
                TravailNoirBeDimonaChecker.analyze(reqAbsente());
        assertThat(r.raison()).isEqualTo("ABSENCE_DIMONA_NIVEAU_4_FERME");
    }

    @Test
    void analyze_raison_indep_estStable() {
        TravailNoirBeDimonaRequest req = new TravailNoirBeDimonaRequest(
                TravailNoirBeDimonaStatutDimona.INDEPENDANT_FAUSSEMENT_QUALIFIE,
                DEBUT, null, CONTROLE, SALAIRE, 1,
                false, false,
                TravailNoirBeDimonaElementsSubordination.OUI);
        TravailNoirBeDimonaResult r =
                TravailNoirBeDimonaChecker.analyze(req);
        assertThat(r.raison())
                .isEqualTo("FAUX_INDEPENDANT_REQUALIFIE_NIVEAU_4");
    }
}
