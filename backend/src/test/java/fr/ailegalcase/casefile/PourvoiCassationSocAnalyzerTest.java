package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-05 : tests unitaires de {@link PourvoiCassationSocAnalyzer}. Couvre le
 * calcul du délai de 2 mois (DELAI_OUVERT / DELAI_URGENT / DELAI_EXPIRE,
 * art. 612 CPC), la force probatoire des cas d'ouverture (art. 604 CPC), le
 * risque de non-admission (filtre NPC, art. 1014 CPC), l'item bloquant avocat
 * aux Conseils (art. 973 CPC), les verdicts globaux et la validation.
 */
class PourvoiCassationSocAnalyzerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 30);

    // ── Cas FORT + moyen sérieux → risque FAIBLE, POURVOI_RECOMMANDE ──

    @Test
    void analyze_violationLoi_moyenSerieux_risqueFaible_pourvoiRecommande() {
        PourvoiCassationSocResult r = PourvoiCassationSocAnalyzer.analyze(
                TODAY.minusDays(10),
                List.of(PourvoiCassationSocCasOuverture.VIOLATION_LOI),
                true, true, TODAY);

        assertThat(r.risqueNonAdmission()).isEqualTo(PourvoiCassationSocRisqueNonAdmission.FAIBLE);
        assertThat(r.verdict()).isEqualTo(PourvoiCassationSocVerdict.POURVOI_RECOMMANDE);
        assertThat(r.verdictDelai()).isEqualTo(PourvoiCassationSocVerdictDelai.DELAI_OUVERT);
        assertThat(r.casOuvertureAnalyses()).hasSize(1);
        assertThat(r.casOuvertureAnalyses().get(0).forceProbatoire())
                .isEqualTo(PourvoiCassationSocForce.FORTE);
        assertThat(r.baseJuridique()).contains("612");
    }

    // ── Cas MOYEN sans moyen sérieux → risque MODERE, POURVOI_RISQUE ──

    @Test
    void analyze_denaturation_sansMoyenSerieux_risqueModere_pourvoiRisque() {
        PourvoiCassationSocResult r = PourvoiCassationSocAnalyzer.analyze(
                TODAY.minusDays(10),
                List.of(PourvoiCassationSocCasOuverture.DENATURATION),
                true, false, TODAY);

        assertThat(r.risqueNonAdmission()).isEqualTo(PourvoiCassationSocRisqueNonAdmission.MODERE);
        assertThat(r.verdict()).isEqualTo(PourvoiCassationSocVerdict.POURVOI_RISQUE);
        assertThat(r.casOuvertureAnalyses().get(0).forceProbatoire())
                .isEqualTo(PourvoiCassationSocForce.MOYENNE);
    }

    // ── Cas FAIBLE seul, pas de moyen sérieux → risque ELEVE, POURVOI_DECONSEILLE ──

    @Test
    void analyze_viceForme_sansMoyenSerieux_risqueEleve_pourvoiDeconseille() {
        PourvoiCassationSocResult r = PourvoiCassationSocAnalyzer.analyze(
                TODAY.minusDays(10),
                List.of(PourvoiCassationSocCasOuverture.VICE_FORME),
                true, false, TODAY);

        assertThat(r.risqueNonAdmission()).isEqualTo(PourvoiCassationSocRisqueNonAdmission.ELEVE);
        assertThat(r.verdict()).isEqualTo(PourvoiCassationSocVerdict.POURVOI_DECONSEILLE);
    }

    // ── Cas FAIBLE mais moyen sérieux identifié → risque FAIBLE (anti-filtre) ──

    @Test
    void analyze_viceForme_avecMoyenSerieux_risqueFaible() {
        PourvoiCassationSocResult r = PourvoiCassationSocAnalyzer.analyze(
                TODAY.minusDays(10),
                List.of(PourvoiCassationSocCasOuverture.VICE_FORME),
                true, true, TODAY);

        assertThat(r.risqueNonAdmission()).isEqualTo(PourvoiCassationSocRisqueNonAdmission.FAIBLE);
        assertThat(r.verdict()).isEqualTo(PourvoiCassationSocVerdict.POURVOI_RECOMMANDE);
    }

    // ── Délai : J-50 → date limite = notification + 2 mois, DELAI_URGENT ──

    @Test
    void analyze_notificationJMoins50_delaiUrgent() {
        LocalDate notification = TODAY.minusDays(50);
        PourvoiCassationSocResult r = PourvoiCassationSocAnalyzer.analyze(
                notification,
                List.of(PourvoiCassationSocCasOuverture.VIOLATION_LOI),
                true, false, TODAY);

        assertThat(r.dateLimitePourvoi()).isEqualTo(notification.plusMonths(2));
        assertThat(r.verdictDelai()).isEqualTo(PourvoiCassationSocVerdictDelai.DELAI_URGENT);
        assertThat(r.joursRestants()).isBetween(0L, 14L);
    }

    // ── Délai : J-70 → DELAI_EXPIRE, verdict global DELAI_EXPIRE ──

    @Test
    void analyze_notificationJMoins70_delaiExpire() {
        PourvoiCassationSocResult r = PourvoiCassationSocAnalyzer.analyze(
                TODAY.minusDays(70),
                List.of(PourvoiCassationSocCasOuverture.VIOLATION_LOI),
                true, true, TODAY);

        assertThat(r.verdictDelai()).isEqualTo(PourvoiCassationSocVerdictDelai.DELAI_EXPIRE);
        assertThat(r.verdict()).isEqualTo(PourvoiCassationSocVerdict.DELAI_EXPIRE);
        assertThat(r.joursRestants()).isNegative();
        assertThat(r.checklist()).anySatisfy(i -> {
            assertThat(i.libelle()).containsIgnoringCase("EXPIRÉ");
            assertThat(i.bloquant()).isTrue();
        });
    }

    // ── Représentation avocat aux Conseils absente → item bloquant (art. 973 CPC) ──

    @Test
    void analyze_sansAvocatCassation_itemBloquant() {
        PourvoiCassationSocResult r = PourvoiCassationSocAnalyzer.analyze(
                TODAY.minusDays(10),
                List.of(PourvoiCassationSocCasOuverture.VIOLATION_LOI),
                false, true, TODAY);

        assertThat(r.representationAvocatCassation()).isFalse();
        assertThat(r.checklist()).anySatisfy(i -> {
            assertThat(i.libelle()).containsIgnoringCase("avocat");
            assertThat(i.baseJuridique()).contains("973");
            assertThat(i.bloquant()).isTrue();
        });
    }

    // ── Représentation présente → item avocat non bloquant ──

    @Test
    void analyze_avecAvocatCassation_avocatNonBloquant() {
        PourvoiCassationSocResult r = PourvoiCassationSocAnalyzer.analyze(
                TODAY.minusDays(10),
                List.of(PourvoiCassationSocCasOuverture.VIOLATION_LOI),
                true, true, TODAY);

        assertThat(r.checklist()).anySatisfy(i -> {
            assertThat(i.baseJuridique()).contains("973");
            assertThat(i.bloquant()).isFalse();
        });
    }

    // ── Plusieurs cas dont un FORT → risque FAIBLE même sans moyen sérieux ──

    @Test
    void analyze_plusieursCasDontFort_risqueFaible() {
        PourvoiCassationSocResult r = PourvoiCassationSocAnalyzer.analyze(
                TODAY.minusDays(5),
                List.of(PourvoiCassationSocCasOuverture.VICE_FORME,
                        PourvoiCassationSocCasOuverture.DEFAUT_BASE_LEGALE),
                true, false, TODAY);

        assertThat(r.risqueNonAdmission()).isEqualTo(PourvoiCassationSocRisqueNonAdmission.FAIBLE);
        assertThat(r.verdict()).isEqualTo(PourvoiCassationSocVerdict.POURVOI_RECOMMANDE);
        assertThat(r.casOuvertureAnalyses()).hasSize(2);
    }

    // ── Représentation null → traité comme false (défaut) ──

    @Test
    void analyze_representationNull_defautFalse() {
        PourvoiCassationSocResult r = PourvoiCassationSocAnalyzer.analyze(
                TODAY.minusDays(10),
                List.of(PourvoiCassationSocCasOuverture.VIOLATION_LOI),
                null, null, TODAY);

        assertThat(r.representationAvocatCassation()).isFalse();
        assertThat(r.moyenSerieuxIdentifie()).isFalse();
    }

    // ── Validation ──

    @Test
    void analyze_dateNotificationNull_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> PourvoiCassationSocAnalyzer.analyze(
                null, List.of(PourvoiCassationSocCasOuverture.VIOLATION_LOI), true, true, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNotificationArret");
    }

    @Test
    void analyze_dateNotificationFuture_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> PourvoiCassationSocAnalyzer.analyze(
                TODAY.plusDays(1), List.of(PourvoiCassationSocCasOuverture.VIOLATION_LOI), true, true, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNotificationArret");
    }

    @Test
    void analyze_casOuvertureVide_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> PourvoiCassationSocAnalyzer.analyze(
                TODAY.minusDays(1), List.of(), true, true, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("casOuverture");
    }
}
