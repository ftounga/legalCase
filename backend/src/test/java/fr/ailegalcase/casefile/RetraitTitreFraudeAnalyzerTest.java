package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-214-41 : tests unitaires de {@link RetraitTitreFraudeAnalyzer}.
 * Couvre les 3 statuts (RECOURS_POSSIBLE, URGENT &lt; 15 j, PRESCRIT), le calcul
 * du délai de recours TA (dateRetrait + 2 mois), les vices de procédure (absence
 * de contradictoire, délai insuffisant), les moyens de contestation selon le
 * motif et la validation des entrées — date du jour injectée pour le déterminisme.
 */
class RetraitTitreFraudeAnalyzerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 29);

    // ── Vices de procédure ───────────────────────────────────────────────

    @Test
    void analyze_mariageGris_sansMiseEnDemeure_vicesContradictoire() {
        // Retrait notifié il y a 5 j → délai TA dans ~56 j → RECOURS_POSSIBLE.
        RetraitTitreFraudeResult r = RetraitTitreFraudeAnalyzer.analyze(
                TODAY.minusDays(5), RetraitTitreFraudeMotifEnum.MARIAGE_GRIS, false, null, TODAY);

        assertThat(r.vicesDeProcedure()).isNotEmpty();
        assertThat(r.vicesDeProcedure())
                .anyMatch(v -> v.toLowerCase().contains("contradictoire")
                        || v.toLowerCase().contains("mise en demeure"));
        assertThat(r.statut()).isEqualTo(RetraitTitreFraudeStatut.RECOURS_POSSIBLE);
    }

    @Test
    void analyze_miseEnDemeureTropProcheDuRetrait_vicesDelaiInsuffisant() {
        // Mise en demeure 5 j avant le retrait (< 15 j) → vice délai insuffisant.
        LocalDate dateRetrait = TODAY.minusDays(10);
        RetraitTitreFraudeResult r = RetraitTitreFraudeAnalyzer.analyze(
                dateRetrait, RetraitTitreFraudeMotifEnum.FAUSSES_DECLARATIONS, true,
                dateRetrait.minusDays(5), TODAY);

        assertThat(r.vicesDeProcedure())
                .anyMatch(v -> v.toLowerCase().contains("délai insuffisant"));
    }

    // ── Statut RECOURS_POSSIBLE + délai TA ───────────────────────────────

    @Test
    void analyze_recoursPossible_delaiRecoursTaCalcule() {
        LocalDate dateRetrait = TODAY.minusDays(10);
        RetraitTitreFraudeResult r = RetraitTitreFraudeAnalyzer.analyze(
                dateRetrait, RetraitTitreFraudeMotifEnum.FRAUDE_DOCUMENTAIRE, true,
                dateRetrait.minusDays(30), TODAY);

        assertThat(r.delaiRecoursTA()).isEqualTo(dateRetrait.plusMonths(2));
        assertThat(r.statut()).isEqualTo(RetraitTitreFraudeStatut.RECOURS_POSSIBLE);
        assertThat(r.recoursPossible()).isTrue();
        assertThat(r.baseJuridique()).contains("L. 412-7");
    }

    // ── Statut URGENT (< 15 j) ───────────────────────────────────────────

    @Test
    void analyze_delaiExpireDansMoinsDe15Jours_statutUrgent() {
        // Retrait il y a ~50 j → délai TA (2 mois) dans ~10 j → URGENT.
        LocalDate dateRetrait = TODAY.minusMonths(2).plusDays(10);
        RetraitTitreFraudeResult r = RetraitTitreFraudeAnalyzer.analyze(
                dateRetrait, RetraitTitreFraudeMotifEnum.PERTE_CONDITIONS, true,
                dateRetrait.minusDays(20), TODAY);

        assertThat(r.statut()).isEqualTo(RetraitTitreFraudeStatut.URGENT);
        assertThat(r.recoursPossible()).isTrue();
    }

    // ── Statut PRESCRIT (> 2 mois) ───────────────────────────────────────

    @Test
    void analyze_delaiDepasse_statutPrescrit() {
        // Retrait il y a 3 mois → délai TA (2 mois) dépassé → PRESCRIT.
        LocalDate dateRetrait = TODAY.minusMonths(3);
        RetraitTitreFraudeResult r = RetraitTitreFraudeAnalyzer.analyze(
                dateRetrait, RetraitTitreFraudeMotifEnum.MARIAGE_GRIS, false, null, TODAY);

        assertThat(r.statut()).isEqualTo(RetraitTitreFraudeStatut.PRESCRIT);
        assertThat(r.recoursPossible()).isFalse();
        assertThat(r.delaiRecoursTA()).isBefore(TODAY);
    }

    // ── motifsContestation selon le motif ────────────────────────────────

    @Test
    void analyze_mariageGris_motifsContestationCommunauteDeVie() {
        RetraitTitreFraudeResult r = RetraitTitreFraudeAnalyzer.analyze(
                TODAY.minusDays(5), RetraitTitreFraudeMotifEnum.MARIAGE_GRIS, true,
                TODAY.minusDays(40), TODAY);

        assertThat(r.motifsContestation()).isNotEmpty();
        assertThat(r.motifsContestation())
                .anyMatch(m -> m.toLowerCase().contains("communauté de vie"))
                .anyMatch(m -> m.toLowerCase().contains("enfant"));
    }

    @Test
    void analyze_fraudeDocumentaire_motifsContestationAuthenticiteEtErreur() {
        RetraitTitreFraudeResult r = RetraitTitreFraudeAnalyzer.analyze(
                TODAY.minusDays(5), RetraitTitreFraudeMotifEnum.FRAUDE_DOCUMENTAIRE, true,
                TODAY.minusDays(40), TODAY);

        assertThat(r.motifsContestation())
                .anyMatch(m -> m.toLowerCase().contains("authenticité"))
                .anyMatch(m -> m.toLowerCase().contains("bonne foi")
                        || m.toLowerCase().contains("erreur"));
    }

    // ── Validation des entrées ───────────────────────────────────────────

    @Test
    void analyze_dateRetraitNulle_estRejetee() {
        assertThatThrownBy(() -> RetraitTitreFraudeAnalyzer.analyze(
                null, RetraitTitreFraudeMotifEnum.MARIAGE_GRIS, false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void analyze_motifNul_estRejete() {
        assertThatThrownBy(() -> RetraitTitreFraudeAnalyzer.analyze(
                TODAY, null, false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
