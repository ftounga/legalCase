package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-13 : tests unitaires du {@link EtudiantJobisteBeChecker}.
 *
 * <p>Couvre : conditions cumulatives (statut étudiant principal /
 * interruption courte / vide pédagogique / non étudiant), respect
 * du quota 600h annuel + calcul du dépassement + montant
 * redressement estimé, formalisme (contrat écrit + Dimona STU),
 * cotisations réduites barème AR 14/07/1995, hiérarchie des
 * verdicts (statut prime sur quota / quota prime sur formalisme /
 * formalisme prime sur cotisations), validation des inputs, base
 * juridique stable et avertissement.</p>
 */
class EtudiantJobisteBeCheckerTest {

    private static final LocalDate DATE_OCC = LocalDate.of(2024, 7, 1);

    private static EtudiantJobisteBeRequest reqEligible() {
        return new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                100,
                200,
                600,
                true,
                true,
                true,
                new BigDecimal("12.50"));
    }

    // ── Cas éligible nominal ────────────────────────────────────────────────

    @Test
    void analyze_eligibleNominal_returnsEligible() {
        EtudiantJobisteBeResult r = EtudiantJobisteBeChecker.analyze(reqEligible());

        assertThat(r.verdict()).isEqualTo(EtudiantJobisteBeVerdict.ELIGIBLE);
        assertThat(r.statutEligible()).isTrue();
        assertThat(r.quotaRespecte()).isTrue();
        assertThat(r.formalismeRespecte()).isTrue();
        assertThat(r.cotisationsConformes()).isTrue();
        assertThat(r.heuresRestantesAvantContrat()).isEqualTo(500);
        assertThat(r.heuresHorsQuota()).isZero();
        assertThat(r.montantRedressementEstime())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.raison()).isEqualTo("ETUDIANT_JOBISTE_REGULIER");
        assertThat(r.synthese()).contains("pleinement régulière");
    }

    @Test
    void analyze_interruptionCourte_eligible() {
        EtudiantJobisteBeRequest req = withStatut(
                EtudiantJobisteBeStatutEtudiant.INTERRUPTION_COURTE_MOINS_12_MOIS);
        EtudiantJobisteBeResult r = EtudiantJobisteBeChecker.analyze(req);

        assertThat(r.verdict()).isEqualTo(EtudiantJobisteBeVerdict.ELIGIBLE);
        assertThat(r.synthese()).contains("interruption courte");
    }

    // ── Hiérarchie 1 : statut non étudiant ──────────────────────────────────

    @Test
    void analyze_nonEtudiant_returnsStatutNonEtudiant() {
        EtudiantJobisteBeRequest req = withStatut(
                EtudiantJobisteBeStatutEtudiant.NON_ETUDIANT);
        EtudiantJobisteBeResult r = EtudiantJobisteBeChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(EtudiantJobisteBeVerdict.INELIGIBLE_STATUT_NON_ETUDIANT);
        assertThat(r.statutEligible()).isFalse();
        assertThat(r.raison()).isEqualTo("STATUT_NON_ETUDIANT");
        assertThat(r.synthese()).contains("Loi 03/07/1978").contains("art. 120");
    }

    // ── Hiérarchie 2 : vide pédagogique → A_ANALYSER ────────────────────────

    @Test
    void analyze_videPedagogique_returnsAanalyser() {
        EtudiantJobisteBeRequest req = withStatut(
                EtudiantJobisteBeStatutEtudiant.VIDE_PEDAGOGIQUE_PROLONGE);
        EtudiantJobisteBeResult r = EtudiantJobisteBeChecker.analyze(req);

        assertThat(r.verdict()).isEqualTo(EtudiantJobisteBeVerdict.A_ANALYSER);
        assertThat(r.raison()).isEqualTo("VIDE_PEDAGOGIQUE_ZONE_GRISE");
        assertThat(r.synthese()).contains("avocat BE");
    }

    // ── Hiérarchie 3 : quota dépassé ────────────────────────────────────────

    @Test
    void analyze_quotaDepasse_returnsQuotaDepasse_etCalculeExcedent() {
        EtudiantJobisteBeRequest req = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                500,
                200,   // 500 + 200 = 700 > 600 → 100 h hors quota
                600,
                true, true, true,
                new BigDecimal("12.50"));

        EtudiantJobisteBeResult r = EtudiantJobisteBeChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(EtudiantJobisteBeVerdict.INELIGIBLE_QUOTA_DEPASSE);
        assertThat(r.quotaRespecte()).isFalse();
        assertThat(r.heuresHorsQuota()).isEqualTo(100);
        // 100 h * 12.50 € * 0.4187 = 523.375 → arrondi 523.38
        assertThat(r.montantRedressementEstime())
                .isEqualByComparingTo(new BigDecimal("523.38"));
        assertThat(r.raison()).isEqualTo("QUOTA_DEPASSE");
        assertThat(r.synthese())
                .contains("100 h")
                .contains("hors quota")
                .contains("Student@work");
    }

    @Test
    void analyze_quotaPileAtteint_estEligible() {
        EtudiantJobisteBeRequest req = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                400,
                200,   // 400 + 200 = 600 = quota → 0 hors quota
                600,
                true, true, true,
                new BigDecimal("12.50"));

        EtudiantJobisteBeResult r = EtudiantJobisteBeChecker.analyze(req);

        assertThat(r.verdict()).isEqualTo(EtudiantJobisteBeVerdict.ELIGIBLE);
        assertThat(r.heuresHorsQuota()).isZero();
    }

    @Test
    void analyze_quotaTransitoire475_pourExercicesAnterieurs() {
        // Avant la Loi-programme 22/12/2023, le quota valait 475 h/an.
        EtudiantJobisteBeRequest req = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                400,
                200,   // 400 + 200 = 600 > 475 → 125 h hors quota
                475,
                true, true, true,
                new BigDecimal("12.50"));

        EtudiantJobisteBeResult r = EtudiantJobisteBeChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(EtudiantJobisteBeVerdict.INELIGIBLE_QUOTA_DEPASSE);
        assertThat(r.heuresHorsQuota()).isEqualTo(125);
    }

    // ── Hiérarchie 4 : formalisme défaillant ────────────────────────────────

    @Test
    void analyze_contratAbsent_returnsFragileContrat() {
        EtudiantJobisteBeRequest req = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                100, 200, 600,
                false, true, true,
                new BigDecimal("12.50"));

        EtudiantJobisteBeResult r = EtudiantJobisteBeChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(EtudiantJobisteBeVerdict.FRAGILE_CONTRAT_OU_DIMONA_MANQUANT);
        assertThat(r.formalismeRespecte()).isFalse();
        assertThat(r.raison()).isEqualTo("CONTRAT_OU_DIMONA_MANQUANT");
        assertThat(r.synthese()).contains("contrat d'occupation étudiant");
    }

    @Test
    void analyze_dimonaAbsente_returnsFragileDimona() {
        EtudiantJobisteBeRequest req = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                100, 200, 600,
                true, false, true,
                new BigDecimal("12.50"));

        EtudiantJobisteBeResult r = EtudiantJobisteBeChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(EtudiantJobisteBeVerdict.FRAGILE_CONTRAT_OU_DIMONA_MANQUANT);
        assertThat(r.synthese()).contains("Dimona");
    }

    @Test
    void analyze_lesDeuxAbsents_diagCombine() {
        EtudiantJobisteBeRequest req = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                100, 200, 600,
                false, false, true,
                new BigDecimal("12.50"));

        EtudiantJobisteBeResult r = EtudiantJobisteBeChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(EtudiantJobisteBeVerdict.FRAGILE_CONTRAT_OU_DIMONA_MANQUANT);
        assertThat(r.synthese())
                .contains("contrat")
                .contains("Dimona");
    }

    // ── Hiérarchie 5 : cotisations hors barème ──────────────────────────────

    @Test
    void analyze_cotisationsNonReduites_returnsFragileCotisations() {
        EtudiantJobisteBeRequest req = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                100, 200, 600,
                true, true, false,
                new BigDecimal("12.50"));

        EtudiantJobisteBeResult r = EtudiantJobisteBeChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(EtudiantJobisteBeVerdict.FRAGILE_COTISATIONS_NON_REDUITES);
        assertThat(r.cotisationsConformes()).isFalse();
        assertThat(r.raison()).isEqualTo("COTISATIONS_NON_REDUITES");
        assertThat(r.synthese())
                .contains("5,42 %")
                .contains("2,71 %")
                .contains("AR du 14/07/1995");
    }

    // ── Hiérarchie : statut prime sur tout ──────────────────────────────────

    @Test
    void analyze_nonEtudiantEtToutLeReste_statutWins() {
        EtudiantJobisteBeRequest req = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.NON_ETUDIANT,
                500, 200, 600,
                false, false, false,
                new BigDecimal("12.50"));

        assertThat(EtudiantJobisteBeChecker.analyze(req).verdict())
                .isEqualTo(EtudiantJobisteBeVerdict.INELIGIBLE_STATUT_NON_ETUDIANT);
    }

    @Test
    void analyze_quotaPrimeurSurFormalismeEtCotisations() {
        EtudiantJobisteBeRequest req = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                500, 200, 600,
                false, false, false,
                new BigDecimal("12.50"));

        // statut OK, quota dépassé → quota prime sur formalisme/cotisations
        assertThat(EtudiantJobisteBeChecker.analyze(req).verdict())
                .isEqualTo(EtudiantJobisteBeVerdict.INELIGIBLE_QUOTA_DEPASSE);
    }

    @Test
    void analyze_formalismePrimeSurCotisations() {
        EtudiantJobisteBeRequest req = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                100, 200, 600,
                false, true, false,
                new BigDecimal("12.50"));

        // statut+quota OK, formalisme défaillant + cotisations défaillantes
        // → formalisme prime sur cotisations.
        assertThat(EtudiantJobisteBeChecker.analyze(req).verdict())
                .isEqualTo(EtudiantJobisteBeVerdict.FRAGILE_CONTRAT_OU_DIMONA_MANQUANT);
    }

    @Test
    void analyze_videPedagogiqueAvecQuotaDepasseEtFormalismeKO_videWins() {
        EtudiantJobisteBeRequest req = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.VIDE_PEDAGOGIQUE_PROLONGE,
                500, 200, 600,
                false, false, false,
                new BigDecimal("12.50"));

        // vide pédagogique = zone grise statut → A_ANALYSER prime.
        assertThat(EtudiantJobisteBeChecker.analyze(req).verdict())
                .isEqualTo(EtudiantJobisteBeVerdict.A_ANALYSER);
    }

    // ── Calcul redressement ─────────────────────────────────────────────────

    @Test
    void analyze_redressement_calculCorrect() {
        EtudiantJobisteBeRequest req = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                550,
                100,   // 550 + 100 = 650 → 50 h hors quota
                600,
                true, true, true,
                new BigDecimal("10.00"));

        EtudiantJobisteBeResult r = EtudiantJobisteBeChecker.analyze(req);

        // 50 * 10 * 0.4187 = 209.35
        assertThat(r.montantRedressementEstime())
                .isEqualByComparingTo(new BigDecimal("209.35"));
    }

    @Test
    void analyze_heuresRestantesAvantContrat_calculCorrect() {
        EtudiantJobisteBeRequest req = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                350, 200, 600,
                true, true, true,
                new BigDecimal("12.50"));

        EtudiantJobisteBeResult r = EtudiantJobisteBeChecker.analyze(req);

        assertThat(r.heuresRestantesAvantContrat()).isEqualTo(250);
    }

    @Test
    void analyze_heuresDejaPresteesAuDela_heuresRestantesZero() {
        EtudiantJobisteBeRequest req = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                700, 100, 600,
                true, true, true,
                new BigDecimal("12.50"));

        EtudiantJobisteBeResult r = EtudiantJobisteBeChecker.analyze(req);

        assertThat(r.heuresRestantesAvantContrat()).isZero();
        assertThat(r.heuresHorsQuota()).isEqualTo(200);
        assertThat(r.verdict())
                .isEqualTo(EtudiantJobisteBeVerdict.INELIGIBLE_QUOTA_DEPASSE);
    }

    // ── Snapshot inputs ─────────────────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        EtudiantJobisteBeResult r = EtudiantJobisteBeChecker.analyze(reqEligible());

        assertThat(r.dateDebutOccupation()).isEqualTo(DATE_OCC);
        assertThat(r.statutEtudiant())
                .isEqualTo(EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL);
        assertThat(r.heuresDejaPresteesDansAnnee()).isEqualTo(100);
        assertThat(r.heuresContratEnCours()).isEqualTo(200);
        assertThat(r.quotaAnnuelHeures()).isEqualTo(600);
        assertThat(r.contratEcritSigne()).isTrue();
        assertThat(r.dimonaStuDeclaree()).isTrue();
        assertThat(r.cotisationsReduitesAppliquees()).isTrue();
        assertThat(r.remunerationBruteHoraire())
                .isEqualByComparingTo("12.50");
    }

    // ── Base juridique + avertissement ──────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceTousLesTextes() {
        EtudiantJobisteBeResult r = EtudiantJobisteBeChecker.analyze(reqEligible());

        assertThat(r.baseJuridique())
                .contains("Loi du 03/07/1978")
                .contains("Titre VII")
                .contains("Loi-programme du 24/12/2002")
                .contains("AR du 14/07/1995")
                .contains("5,42")
                .contains("2,71")
                .contains("AR du 05/11/2002")
                .contains("Loi-programme du 22/12/2023")
                .contains("600 heures/an");
    }

    @Test
    void analyze_avertissementSystematiquePresent() {
        EtudiantJobisteBeResult r = EtudiantJobisteBeChecker.analyze(reqEligible());

        assertThat(r.avertissement())
                .isNotNull()
                .contains("avocat")
                .contains("ONSS")
                .contains("Student@work");
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() -> EtudiantJobisteBeChecker.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_dateNull_throws() {
        EtudiantJobisteBeRequest bad = new EtudiantJobisteBeRequest(
                null,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                100, 200, 600, true, true, true,
                new BigDecimal("12.50"));

        assertThatThrownBy(() -> EtudiantJobisteBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDebutOccupation");
    }

    @Test
    void analyze_statutNull_throws() {
        EtudiantJobisteBeRequest bad = new EtudiantJobisteBeRequest(
                DATE_OCC, null,
                100, 200, 600, true, true, true,
                new BigDecimal("12.50"));

        assertThatThrownBy(() -> EtudiantJobisteBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("statutEtudiant");
    }

    @Test
    void analyze_heuresDejaNull_throws() {
        EtudiantJobisteBeRequest bad = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                null, 200, 600, true, true, true,
                new BigDecimal("12.50"));

        assertThatThrownBy(() -> EtudiantJobisteBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("heuresDejaPresteesDansAnnee");
    }

    @Test
    void analyze_heuresDejaNegatif_throws() {
        EtudiantJobisteBeRequest bad = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                -1, 200, 600, true, true, true,
                new BigDecimal("12.50"));

        assertThatThrownBy(() -> EtudiantJobisteBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("heuresDejaPresteesDansAnnee");
    }

    @Test
    void analyze_heuresContratNull_throws() {
        EtudiantJobisteBeRequest bad = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                100, null, 600, true, true, true,
                new BigDecimal("12.50"));

        assertThatThrownBy(() -> EtudiantJobisteBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("heuresContratEnCours");
    }

    @Test
    void analyze_heuresContratZero_throws() {
        EtudiantJobisteBeRequest bad = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                100, 0, 600, true, true, true,
                new BigDecimal("12.50"));

        assertThatThrownBy(() -> EtudiantJobisteBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("heuresContratEnCours");
    }

    @Test
    void analyze_quotaNull_throws() {
        EtudiantJobisteBeRequest bad = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                100, 200, null, true, true, true,
                new BigDecimal("12.50"));

        assertThatThrownBy(() -> EtudiantJobisteBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quotaAnnuelHeures");
    }

    @Test
    void analyze_quotaZero_throws() {
        EtudiantJobisteBeRequest bad = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                100, 200, 0, true, true, true,
                new BigDecimal("12.50"));

        assertThatThrownBy(() -> EtudiantJobisteBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quotaAnnuelHeures");
    }

    @Test
    void analyze_contratNull_throws() {
        EtudiantJobisteBeRequest bad = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                100, 200, 600, null, true, true,
                new BigDecimal("12.50"));

        assertThatThrownBy(() -> EtudiantJobisteBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contratEcritSigne");
    }

    @Test
    void analyze_dimonaNull_throws() {
        EtudiantJobisteBeRequest bad = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                100, 200, 600, true, null, true,
                new BigDecimal("12.50"));

        assertThatThrownBy(() -> EtudiantJobisteBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dimonaStuDeclaree");
    }

    @Test
    void analyze_cotisationsNull_throws() {
        EtudiantJobisteBeRequest bad = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                100, 200, 600, true, true, null,
                new BigDecimal("12.50"));

        assertThatThrownBy(() -> EtudiantJobisteBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cotisationsReduitesAppliquees");
    }

    @Test
    void analyze_remunerationNull_throws() {
        EtudiantJobisteBeRequest bad = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                100, 200, 600, true, true, true,
                null);

        assertThatThrownBy(() -> EtudiantJobisteBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remunerationBruteHoraire");
    }

    @Test
    void analyze_remunerationNegative_throws() {
        EtudiantJobisteBeRequest bad = new EtudiantJobisteBeRequest(
                DATE_OCC,
                EtudiantJobisteBeStatutEtudiant.ETUDIANT_PRINCIPAL,
                100, 200, 600, true, true, true,
                new BigDecimal("-1.00"));

        assertThatThrownBy(() -> EtudiantJobisteBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remunerationBruteHoraire");
    }

    // ── Helper ─────────────────────────────────────────────────────────────

    private static EtudiantJobisteBeRequest withStatut(
            EtudiantJobisteBeStatutEtudiant statut) {
        return new EtudiantJobisteBeRequest(
                DATE_OCC,
                statut,
                100, 200, 600,
                true, true, true,
                new BigDecimal("12.50"));
    }
}
