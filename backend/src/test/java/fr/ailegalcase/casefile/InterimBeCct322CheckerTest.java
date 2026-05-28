package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-14 : tests unitaires du {@link InterimBeCct322Checker}.
 *
 * <p>Couvre : conditions cumulatives (motif autorisé, interdiction
 * grève/lock-out art. 19, durée max selon motif, parité salariale
 * art. 10, formalisme contrat écrit + Dimona), hiérarchie des
 * verdicts, validation des inputs et base juridique stable.</p>
 */
class InterimBeCct322CheckerTest {

    private static final LocalDate DATE_DEBUT = LocalDate.of(2024, 6, 1);

    private static InterimBeCct322Request reqEligible() {
        return new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL,
                false,
                90,
                180,
                new BigDecimal("16.00"),
                new BigDecimal("16.00"),
                true,
                true);
    }

    // ── Cas éligible nominal ───────────────────────────────────────────────────

    @Test
    void analyze_eligibleNominal_returnsMissionReguliere() {
        InterimBeCct322Result r = InterimBeCct322Checker.analyze(reqEligible());

        assertThat(r.verdict())
                .isEqualTo(InterimBeCct322Verdict.ELIGIBLE_MISSION_REGULIERE);
        assertThat(r.motifAutorise()).isTrue();
        assertThat(r.dureeRespectee()).isTrue();
        assertThat(r.pariteRespectee()).isTrue();
        assertThat(r.formalismeRespecte()).isTrue();
        assertThat(r.joursExcedentaires()).isZero();
        assertThat(r.ecartPariteSalariale())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.raison()).isEqualTo("MISSION_REGULIERE");
        assertThat(r.synthese()).contains("pleinement régulière");
    }

    @Test
    void analyze_intermSuperieurReference_eligible() {
        InterimBeCct322Request req = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL,
                false,
                90,
                180,
                new BigDecimal("18.00"),
                new BigDecimal("16.00"),
                true,
                true);

        InterimBeCct322Result r = InterimBeCct322Checker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(InterimBeCct322Verdict.ELIGIBLE_MISSION_REGULIERE);
        assertThat(r.pariteRespectee()).isTrue();
        assertThat(r.ecartPariteSalariale())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void analyze_dureeEgalePlafond_eligible() {
        InterimBeCct322Request req = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL,
                false,
                180,
                180,
                new BigDecimal("16.00"),
                new BigDecimal("16.00"),
                true,
                true);

        assertThat(InterimBeCct322Checker.analyze(req).verdict())
                .isEqualTo(InterimBeCct322Verdict.ELIGIBLE_MISSION_REGULIERE);
    }

    // ── Couverture des motifs autorisés ────────────────────────────────────────

    @Test
    void analyze_motifRemplacement_eligible() {
        InterimBeCct322Request req = withMotif(
                InterimBeCct322MotifMission.REMPLACEMENT_TRAVAILLEUR_PERMANENT);
        InterimBeCct322Result r = InterimBeCct322Checker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(InterimBeCct322Verdict.ELIGIBLE_MISSION_REGULIERE);
        assertThat(r.synthese()).contains("remplacement");
    }

    @Test
    void analyze_motifTravailExceptionnel_eligible() {
        InterimBeCct322Request req = withMotif(
                InterimBeCct322MotifMission.EXECUTION_TRAVAIL_EXCEPTIONNEL);
        assertThat(InterimBeCct322Checker.analyze(req).verdict())
                .isEqualTo(InterimBeCct322Verdict.ELIGIBLE_MISSION_REGULIERE);
    }

    @Test
    void analyze_motifInsertionDurable_eligible() {
        InterimBeCct322Request req = withMotif(
                InterimBeCct322MotifMission.INSERTION_EMBAUCHE_DURABLE);
        InterimBeCct322Result r = InterimBeCct322Checker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(InterimBeCct322Verdict.ELIGIBLE_MISSION_REGULIERE);
        assertThat(r.synthese()).contains("CCT n° 108");
    }

    // ── Hiérarchie 1 : grève / lock-out (prime sur tout) ───────────────────────

    @Test
    void analyze_greveLockout_returnsInterditGreveLockout() {
        InterimBeCct322Request req = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.REMPLACEMENT_TRAVAILLEUR_PERMANENT,
                true,
                30,
                180,
                new BigDecimal("16.00"),
                new BigDecimal("16.00"),
                true,
                true);

        InterimBeCct322Result r = InterimBeCct322Checker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(InterimBeCct322Verdict.INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT);
        assertThat(r.raison()).isEqualTo("MOTIF_INTERDIT_GREVE_LOCKOUT");
        assertThat(r.synthese())
                .contains("art. 19")
                .contains("Présomption irréfragable");
    }

    @Test
    void analyze_greveLockoutPrimeSurMotifNonAutorise_greveWins() {
        InterimBeCct322Request req = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.AUTRE_NON_AUTORISE,
                true,
                30,
                180,
                new BigDecimal("16.00"),
                new BigDecimal("16.00"),
                true,
                true);

        assertThat(InterimBeCct322Checker.analyze(req).verdict())
                .isEqualTo(InterimBeCct322Verdict.INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT);
    }

    // ── Hiérarchie 2 : motif non autorisé ──────────────────────────────────────

    @Test
    void analyze_motifNonAutorise_returnsMotifNonAutorise() {
        InterimBeCct322Request req = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.AUTRE_NON_AUTORISE,
                false,
                30,
                180,
                new BigDecimal("16.00"),
                new BigDecimal("16.00"),
                true,
                true);

        InterimBeCct322Result r = InterimBeCct322Checker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(InterimBeCct322Verdict.INELIGIBLE_MOTIF_NON_AUTORISE);
        assertThat(r.motifAutorise()).isFalse();
        assertThat(r.raison()).isEqualTo("MOTIF_NON_AUTORISE");
        assertThat(r.synthese()).contains("non listé", "recours illégal");
    }

    // ── Hiérarchie 3 : motif artistique / flux → A_ANALYSER ────────────────────

    @Test
    void analyze_motifArtistique_returnsAanalyser() {
        InterimBeCct322Request req = withMotif(
                InterimBeCct322MotifMission.MOTIF_ARTISTIQUE);
        InterimBeCct322Result r = InterimBeCct322Checker.analyze(req);

        assertThat(r.verdict()).isEqualTo(InterimBeCct322Verdict.A_ANALYSER);
        assertThat(r.raison()).isEqualTo("MOTIF_ZONE_GRISE_ANALYSE_CAS_PAR_CAS");
        assertThat(r.synthese()).contains("spectacle vivant");
    }

    @Test
    void analyze_motifFluxExceptionnel_returnsAanalyser() {
        InterimBeCct322Request req = withMotif(
                InterimBeCct322MotifMission.FLUX_TRAVAUX_EXCEPTIONNEL_TRAJET_SCOLAIRE);
        InterimBeCct322Result r = InterimBeCct322Checker.analyze(req);

        assertThat(r.verdict()).isEqualTo(InterimBeCct322Verdict.A_ANALYSER);
        assertThat(r.synthese()).contains("flux de travaux");
    }

    // ── Hiérarchie 4 : durée max dépassée ──────────────────────────────────────

    @Test
    void analyze_dureeDepassee_returnsDureeMaxDepassee_calculeExcedent() {
        InterimBeCct322Request req = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL,
                false,
                400,
                365,
                new BigDecimal("16.00"),
                new BigDecimal("16.00"),
                true,
                true);

        InterimBeCct322Result r = InterimBeCct322Checker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(InterimBeCct322Verdict.INELIGIBLE_DUREE_MAX_DEPASSEE);
        assertThat(r.dureeRespectee()).isFalse();
        assertThat(r.joursExcedentaires()).isEqualTo(35);
        assertThat(r.raison()).isEqualTo("DUREE_MAX_DEPASSEE");
        assertThat(r.synthese())
                .contains("contrats successifs")
                .contains("35 jour");
    }

    // ── Hiérarchie 5 : parité salariale violée (art. 10) ───────────────────────

    @Test
    void analyze_pariteVioleeFortement_returnsPariteVioleeWithEcart() {
        InterimBeCct322Request req = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL,
                false,
                90,
                180,
                new BigDecimal("12.00"),
                new BigDecimal("16.00"),
                true,
                true);

        InterimBeCct322Result r = InterimBeCct322Checker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(InterimBeCct322Verdict.INELIGIBLE_PARITE_SALARIALE_VIOLEE);
        assertThat(r.pariteRespectee()).isFalse();
        assertThat(r.ecartPariteSalariale())
                .isEqualByComparingTo(new BigDecimal("4.00"));
        assertThat(r.raison()).isEqualTo("PARITE_SALARIALE_VIOLEE");
        assertThat(r.synthese())
                .contains("art. 10")
                .contains("4.00");
    }

    @Test
    void analyze_pariteEgale_eligible() {
        InterimBeCct322Request req = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL,
                false,
                90,
                180,
                new BigDecimal("16.00"),
                new BigDecimal("16.00"),
                true,
                true);

        assertThat(InterimBeCct322Checker.analyze(req).verdict())
                .isEqualTo(InterimBeCct322Verdict.ELIGIBLE_MISSION_REGULIERE);
    }

    // ── Hiérarchie 6 : formalisme défaillant ───────────────────────────────────

    @Test
    void analyze_contratEcritAbsent_returnsFragileContrat() {
        InterimBeCct322Request req = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL,
                false,
                90,
                180,
                new BigDecimal("16.00"),
                new BigDecimal("16.00"),
                false,
                true);

        InterimBeCct322Result r = InterimBeCct322Checker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(InterimBeCct322Verdict.FRAGILE_CONTRAT_OU_DIMONA_MANQUANT);
        assertThat(r.formalismeRespecte()).isFalse();
        assertThat(r.raison()).isEqualTo("CONTRAT_OU_DIMONA_MANQUANT");
        assertThat(r.synthese()).contains("contrat de travail écrit");
    }

    @Test
    void analyze_dimonaAbsente_returnsFragileDimona() {
        InterimBeCct322Request req = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL,
                false,
                90,
                180,
                new BigDecimal("16.00"),
                new BigDecimal("16.00"),
                true,
                false);

        InterimBeCct322Result r = InterimBeCct322Checker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(InterimBeCct322Verdict.FRAGILE_CONTRAT_OU_DIMONA_MANQUANT);
        assertThat(r.synthese()).contains("Dimona ETI");
    }

    @Test
    void analyze_contratEtDimonaAbsents_diagCombine() {
        InterimBeCct322Request req = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL,
                false,
                90,
                180,
                new BigDecimal("16.00"),
                new BigDecimal("16.00"),
                false,
                false);

        InterimBeCct322Result r = InterimBeCct322Checker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(InterimBeCct322Verdict.FRAGILE_CONTRAT_OU_DIMONA_MANQUANT);
        assertThat(r.synthese())
                .contains("contrat de travail écrit")
                .contains("Dimona ETI");
    }

    // ── Hiérarchie : grève prime sur durée / parité / formalisme ───────────────

    @Test
    void analyze_greveEtDureeDepassee_greveWins() {
        InterimBeCct322Request req = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL,
                true,
                400,
                180,
                new BigDecimal("16.00"),
                new BigDecimal("16.00"),
                true,
                true);

        assertThat(InterimBeCct322Checker.analyze(req).verdict())
                .isEqualTo(InterimBeCct322Verdict.INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT);
    }

    @Test
    void analyze_motifNonAutoriseEtPariteViolee_motifWins() {
        InterimBeCct322Request req = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.AUTRE_NON_AUTORISE,
                false,
                90,
                180,
                new BigDecimal("12.00"),
                new BigDecimal("16.00"),
                true,
                true);

        assertThat(InterimBeCct322Checker.analyze(req).verdict())
                .isEqualTo(InterimBeCct322Verdict.INELIGIBLE_MOTIF_NON_AUTORISE);
    }

    @Test
    void analyze_dureeDepasseeEtPariteViolee_dureeWins() {
        InterimBeCct322Request req = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL,
                false,
                400,
                180,
                new BigDecimal("12.00"),
                new BigDecimal("16.00"),
                true,
                true);

        assertThat(InterimBeCct322Checker.analyze(req).verdict())
                .isEqualTo(InterimBeCct322Verdict.INELIGIBLE_DUREE_MAX_DEPASSEE);
    }

    @Test
    void analyze_pariteVioleeEtFormalismeManquant_pariteWins() {
        InterimBeCct322Request req = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL,
                false,
                90,
                180,
                new BigDecimal("12.00"),
                new BigDecimal("16.00"),
                false,
                false);

        assertThat(InterimBeCct322Checker.analyze(req).verdict())
                .isEqualTo(InterimBeCct322Verdict.INELIGIBLE_PARITE_SALARIALE_VIOLEE);
    }

    // ── Snapshot inputs ────────────────────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        InterimBeCct322Result r = InterimBeCct322Checker.analyze(reqEligible());

        assertThat(r.dateDebutMission()).isEqualTo(DATE_DEBUT);
        assertThat(r.motifMission())
                .isEqualTo(InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL);
        assertThat(r.remplacementGreveOuLockout()).isFalse();
        assertThat(r.dureeTotaleMissionJours()).isEqualTo(90);
        assertThat(r.dureeMaxLegaleJours()).isEqualTo(180);
        assertThat(r.salaireHoraireIntimaireBrut())
                .isEqualByComparingTo("16.00");
        assertThat(r.salaireHoraireReferenceBrut())
                .isEqualByComparingTo("16.00");
        assertThat(r.contratEcritSigne()).isTrue();
        assertThat(r.dimonaDeclareeParEti()).isTrue();
    }

    // ── Base juridique + avertissement ─────────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceLoiEtCct322() {
        InterimBeCct322Result r = InterimBeCct322Checker.analyze(reqEligible());

        assertThat(r.baseJuridique())
                .contains("Loi du 24/07/1987")
                .contains("CCT n° 322")
                .contains("14/06/2010")
                .contains("art. 1er § 1")
                .contains("art. 10")
                .contains("art. 19")
                .contains("art. 20")
                .contains("Dimona");
    }

    @Test
    void analyze_avertissementSystematiquePresent() {
        InterimBeCct322Result r = InterimBeCct322Checker.analyze(reqEligible());

        assertThat(r.avertissement())
                .isNotNull()
                .contains("avocat")
                .contains("ONSS")
                .contains("requalifié");
    }

    // ── Validation conditionnelle ──────────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() -> InterimBeCct322Checker.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_dateDebutNull_throws() {
        InterimBeCct322Request bad = new InterimBeCct322Request(
                null,
                InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL,
                false, 90, 180,
                new BigDecimal("16.00"),
                new BigDecimal("16.00"),
                true, true);

        assertThatThrownBy(() -> InterimBeCct322Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDebutMission");
    }

    @Test
    void analyze_motifNull_throws() {
        InterimBeCct322Request bad = new InterimBeCct322Request(
                DATE_DEBUT, null, false, 90, 180,
                new BigDecimal("16.00"),
                new BigDecimal("16.00"),
                true, true);

        assertThatThrownBy(() -> InterimBeCct322Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifMission");
    }

    @Test
    void analyze_greveLockoutNull_throws() {
        InterimBeCct322Request bad = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL,
                null, 90, 180,
                new BigDecimal("16.00"),
                new BigDecimal("16.00"),
                true, true);

        assertThatThrownBy(() -> InterimBeCct322Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remplacementGreveOuLockout");
    }

    @Test
    void analyze_dureeTotaleNull_throws() {
        InterimBeCct322Request bad = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL,
                false, null, 180,
                new BigDecimal("16.00"),
                new BigDecimal("16.00"),
                true, true);

        assertThatThrownBy(() -> InterimBeCct322Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeTotaleMissionJours");
    }

    @Test
    void analyze_dureeTotaleNegatif_throws() {
        InterimBeCct322Request bad = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL,
                false, -1, 180,
                new BigDecimal("16.00"),
                new BigDecimal("16.00"),
                true, true);

        assertThatThrownBy(() -> InterimBeCct322Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeTotaleMissionJours");
    }

    @Test
    void analyze_dureeMaxLegaleZero_throws() {
        InterimBeCct322Request bad = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL,
                false, 90, 0,
                new BigDecimal("16.00"),
                new BigDecimal("16.00"),
                true, true);

        assertThatThrownBy(() -> InterimBeCct322Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeMaxLegaleJours");
    }

    @Test
    void analyze_salaireIntimaireNull_throws() {
        InterimBeCct322Request bad = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL,
                false, 90, 180,
                null,
                new BigDecimal("16.00"),
                true, true);

        assertThatThrownBy(() -> InterimBeCct322Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salaireHoraireIntimaireBrut");
    }

    @Test
    void analyze_salaireIntimaireNegatif_throws() {
        InterimBeCct322Request bad = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL,
                false, 90, 180,
                new BigDecimal("-1.00"),
                new BigDecimal("16.00"),
                true, true);

        assertThatThrownBy(() -> InterimBeCct322Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salaireHoraireIntimaireBrut");
    }

    @Test
    void analyze_salaireReferenceZero_throws() {
        InterimBeCct322Request bad = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL,
                false, 90, 180,
                new BigDecimal("16.00"),
                BigDecimal.ZERO,
                true, true);

        assertThatThrownBy(() -> InterimBeCct322Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salaireHoraireReferenceBrut");
    }

    @Test
    void analyze_contratNull_throws() {
        InterimBeCct322Request bad = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL,
                false, 90, 180,
                new BigDecimal("16.00"),
                new BigDecimal("16.00"),
                null, true);

        assertThatThrownBy(() -> InterimBeCct322Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contratEcritSigne");
    }

    @Test
    void analyze_dimonaNull_throws() {
        InterimBeCct322Request bad = new InterimBeCct322Request(
                DATE_DEBUT,
                InterimBeCct322MotifMission.SURCROIT_TEMPORAIRE_DE_TRAVAIL,
                false, 90, 180,
                new BigDecimal("16.00"),
                new BigDecimal("16.00"),
                true, null);

        assertThatThrownBy(() -> InterimBeCct322Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dimonaDeclareeParEti");
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private static InterimBeCct322Request withMotif(
            InterimBeCct322MotifMission motif) {
        return new InterimBeCct322Request(
                DATE_DEBUT,
                motif,
                false,
                90,
                180,
                new BigDecimal("16.00"),
                new BigDecimal("16.00"),
                true,
                true);
    }
}
