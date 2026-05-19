package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static fr.ailegalcase.casefile.AbandonPostePresomptionDemissionCalculator.CodeContestation;
import static fr.ailegalcase.casefile.AbandonPostePresomptionDemissionCalculator.ModeNotification;
import static fr.ailegalcase.casefile.AbandonPostePresomptionDemissionCalculator.MotifAbsence;
import static fr.ailegalcase.casefile.AbandonPostePresomptionDemissionCalculator.MotifContestation;
import static fr.ailegalcase.casefile.AbandonPostePresomptionDemissionCalculator.Verdict;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-206-01 — tests unitaires du calculateur de contestation d'une présomption
 * de démission par abandon de poste (FR — loi 21/12/2022).
 *
 * <p>Couvre les 10 critères d'acceptation de la mini-spec : délai 14 j vs 15 j
 * vs 20 j, chaque motif légitime, mentions manquantes, reprise dans le délai,
 * cumul d'irrégularités, bornes de score, dateExpirationDelai, validation des
 * bornes.</p>
 */
class AbandonPostePresomptionDemissionCalculatorTest {

    /** Input régulier de référence — aucune irrégularité, verdict DIFFICILE. */
    private static AbandonPostePresomptionDemissionInput regulier() {
        return new AbandonPostePresomptionDemissionInput.Builder()
                .dateMiseEnDemeurePresentee(LocalDate.of(2026, 5, 1))
                .modeNotificationMiseEnDemeure(ModeNotification.LRAR)
                .delaiAccordeJours(15)
                .miseEnDemeureMentionneDelai(true)
                .miseEnDemeureMentionneConsequences(true)
                .motifAbsenceInvoque(MotifAbsence.AUCUN)
                .repriseOuJustificationDansDelai(false)
                .build();
    }

    @Test
    void aucuneIrregularite_returnsContestationDifficile() {
        var r = AbandonPostePresomptionDemissionCalculator.compute(regulier(), "FRANCE");
        assertThat(r.verdict()).isEqualTo(Verdict.CONTESTATION_DIFFICILE);
        assertThat(r.scoreContestation()).isZero();
        assertThat(r.motifsContestation()).isEmpty();
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    @Test
    void delai14Jours_detecteDelaiInsuffisant() {
        var in = new AbandonPostePresomptionDemissionInput.Builder()
                .dateMiseEnDemeurePresentee(LocalDate.of(2026, 5, 1))
                .modeNotificationMiseEnDemeure(ModeNotification.LRAR)
                .delaiAccordeJours(14)
                .miseEnDemeureMentionneDelai(true)
                .miseEnDemeureMentionneConsequences(true)
                .motifAbsenceInvoque(MotifAbsence.AUCUN)
                .build();
        var r = AbandonPostePresomptionDemissionCalculator.compute(in, "FRANCE");
        assertThat(codes(r.motifsContestation())).containsExactly(CodeContestation.DT42_DELAI_INSUFFISANT);
        assertThat(r.verdict()).isEqualTo(Verdict.CONTESTATION_INCERTAINE);
        assertThat(r.scoreContestation()).isEqualTo(20);
    }

    @Test
    void delai15Jours_aucuneIrregularite() {
        var r = AbandonPostePresomptionDemissionCalculator.compute(regulier(), "FRANCE");
        assertThat(r.motifsContestation()).isEmpty();
    }

    @Test
    void delai20Jours_aucuneIrregularite() {
        var in = new AbandonPostePresomptionDemissionInput.Builder()
                .dateMiseEnDemeurePresentee(LocalDate.of(2026, 5, 1))
                .modeNotificationMiseEnDemeure(ModeNotification.LRAR)
                .delaiAccordeJours(20)
                .miseEnDemeureMentionneDelai(true)
                .miseEnDemeureMentionneConsequences(true)
                .motifAbsenceInvoque(MotifAbsence.AUCUN)
                .build();
        var r = AbandonPostePresomptionDemissionCalculator.compute(in, "FRANCE");
        assertThat(r.motifsContestation()).isEmpty();
        assertThat(r.verdict()).isEqualTo(Verdict.CONTESTATION_DIFFICILE);
    }

    @Test
    void motifMedical_estDetecteEtTireVersSolide() {
        var in = new AbandonPostePresomptionDemissionInput.Builder()
                .dateMiseEnDemeurePresentee(LocalDate.of(2026, 5, 1))
                .modeNotificationMiseEnDemeure(ModeNotification.LRAR)
                .delaiAccordeJours(15)
                .miseEnDemeureMentionneDelai(true)
                .miseEnDemeureMentionneConsequences(true)
                .motifAbsenceInvoque(MotifAbsence.MEDICAL)
                .build();
        var r = AbandonPostePresomptionDemissionCalculator.compute(in, "FRANCE");
        assertThat(codes(r.motifsContestation())).containsExactly(CodeContestation.DT42_MOTIF_LEGITIME);
        assertThat(r.verdict()).isEqualTo(Verdict.CONTESTATION_SOLIDE);
        assertThat(r.scoreContestation()).isEqualTo(50);
    }

    @Test
    void motifsLegitimes_tousDetectesEtSolides() {
        for (MotifAbsence m : new MotifAbsence[]{
                MotifAbsence.DROIT_RETRAIT,
                MotifAbsence.DROIT_GREVE,
                MotifAbsence.MODIFICATION_CONTRAT_REFUSEE,
                MotifAbsence.DEFAUT_PAIEMENT_SALAIRE,
                MotifAbsence.AUTRE
        }) {
            var in = new AbandonPostePresomptionDemissionInput.Builder()
                    .dateMiseEnDemeurePresentee(LocalDate.of(2026, 5, 1))
                    .modeNotificationMiseEnDemeure(ModeNotification.LRAR)
                    .delaiAccordeJours(15)
                    .miseEnDemeureMentionneDelai(true)
                    .miseEnDemeureMentionneConsequences(true)
                    .motifAbsenceInvoque(m)
                    .build();
            var r = AbandonPostePresomptionDemissionCalculator.compute(in, "FRANCE");
            assertThat(codes(r.motifsContestation()))
                    .as("Motif %s doit déclencher DT42_MOTIF_LEGITIME", m)
                    .containsExactly(CodeContestation.DT42_MOTIF_LEGITIME);
            assertThat(r.verdict())
                    .as("Motif %s doit donner verdict SOLIDE", m)
                    .isEqualTo(Verdict.CONTESTATION_SOLIDE);
        }
    }

    @Test
    void medSansMentionDelai_detecteIrregularite() {
        var in = new AbandonPostePresomptionDemissionInput.Builder()
                .dateMiseEnDemeurePresentee(LocalDate.of(2026, 5, 1))
                .modeNotificationMiseEnDemeure(ModeNotification.LRAR)
                .delaiAccordeJours(15)
                .miseEnDemeureMentionneDelai(false)
                .miseEnDemeureMentionneConsequences(true)
                .motifAbsenceInvoque(MotifAbsence.AUCUN)
                .build();
        var r = AbandonPostePresomptionDemissionCalculator.compute(in, "FRANCE");
        assertThat(codes(r.motifsContestation()))
                .containsExactly(CodeContestation.DT42_MED_MENTION_DELAI_MANQUANTE);
        assertThat(r.verdict()).isEqualTo(Verdict.CONTESTATION_INCERTAINE);
    }

    @Test
    void medSansMentionConsequences_detecteIrregularite() {
        var in = new AbandonPostePresomptionDemissionInput.Builder()
                .dateMiseEnDemeurePresentee(LocalDate.of(2026, 5, 1))
                .modeNotificationMiseEnDemeure(ModeNotification.LRAR)
                .delaiAccordeJours(15)
                .miseEnDemeureMentionneDelai(true)
                .miseEnDemeureMentionneConsequences(false)
                .motifAbsenceInvoque(MotifAbsence.AUCUN)
                .build();
        var r = AbandonPostePresomptionDemissionCalculator.compute(in, "FRANCE");
        assertThat(codes(r.motifsContestation()))
                .containsExactly(CodeContestation.DT42_MED_MENTION_CONSEQUENCES_MANQUANTE);
    }

    @Test
    void modeNotificationAutre_detecteIrregularite() {
        var in = new AbandonPostePresomptionDemissionInput.Builder()
                .dateMiseEnDemeurePresentee(LocalDate.of(2026, 5, 1))
                .modeNotificationMiseEnDemeure(ModeNotification.AUTRE)
                .delaiAccordeJours(15)
                .miseEnDemeureMentionneDelai(true)
                .miseEnDemeureMentionneConsequences(true)
                .motifAbsenceInvoque(MotifAbsence.AUCUN)
                .build();
        var r = AbandonPostePresomptionDemissionCalculator.compute(in, "FRANCE");
        assertThat(codes(r.motifsContestation()))
                .containsExactly(CodeContestation.DT42_MODE_NOTIFICATION_IRREGULIER);
    }

    @Test
    void modeNotificationRemiseMainPropre_aucuneIrregularite() {
        var in = new AbandonPostePresomptionDemissionInput.Builder()
                .dateMiseEnDemeurePresentee(LocalDate.of(2026, 5, 1))
                .modeNotificationMiseEnDemeure(ModeNotification.REMISE_MAIN_PROPRE)
                .delaiAccordeJours(15)
                .miseEnDemeureMentionneDelai(true)
                .miseEnDemeureMentionneConsequences(true)
                .motifAbsenceInvoque(MotifAbsence.AUCUN)
                .build();
        var r = AbandonPostePresomptionDemissionCalculator.compute(in, "FRANCE");
        assertThat(r.motifsContestation()).isEmpty();
    }

    @Test
    void repriseDansDelai_estDetecteEtSolide() {
        var in = new AbandonPostePresomptionDemissionInput.Builder()
                .dateMiseEnDemeurePresentee(LocalDate.of(2026, 5, 1))
                .modeNotificationMiseEnDemeure(ModeNotification.LRAR)
                .delaiAccordeJours(15)
                .miseEnDemeureMentionneDelai(true)
                .miseEnDemeureMentionneConsequences(true)
                .motifAbsenceInvoque(MotifAbsence.AUCUN)
                .repriseOuJustificationDansDelai(true)
                .build();
        var r = AbandonPostePresomptionDemissionCalculator.compute(in, "FRANCE");
        assertThat(codes(r.motifsContestation()))
                .containsExactly(CodeContestation.DT42_REPRISE_DANS_DELAI);
        assertThat(r.verdict()).isEqualTo(Verdict.CONTESTATION_SOLIDE);
        assertThat(r.scoreContestation()).isEqualTo(50);
    }

    @Test
    void deuxIrregularitesDeFormeCumulees_donnentSolide() {
        var in = new AbandonPostePresomptionDemissionInput.Builder()
                .dateMiseEnDemeurePresentee(LocalDate.of(2026, 5, 1))
                .modeNotificationMiseEnDemeure(ModeNotification.AUTRE)
                .delaiAccordeJours(10)
                .miseEnDemeureMentionneDelai(true)
                .miseEnDemeureMentionneConsequences(true)
                .motifAbsenceInvoque(MotifAbsence.AUCUN)
                .build();
        var r = AbandonPostePresomptionDemissionCalculator.compute(in, "FRANCE");
        assertThat(codes(r.motifsContestation())).containsExactly(
                CodeContestation.DT42_DELAI_INSUFFISANT,
                CodeContestation.DT42_MODE_NOTIFICATION_IRREGULIER);
        assertThat(r.verdict()).isEqualTo(Verdict.CONTESTATION_SOLIDE);
        assertThat(r.scoreContestation()).isEqualTo(40);
    }

    @Test
    void cumulMassif_borneeAScoreMax100() {
        // 4 irrégularités de forme (4 × 20 = 80) + 1 motif majeur (50) = 130 → 100
        var in = new AbandonPostePresomptionDemissionInput.Builder()
                .dateMiseEnDemeurePresentee(LocalDate.of(2026, 5, 1))
                .modeNotificationMiseEnDemeure(ModeNotification.AUTRE)
                .delaiAccordeJours(5)
                .miseEnDemeureMentionneDelai(false)
                .miseEnDemeureMentionneConsequences(false)
                .motifAbsenceInvoque(MotifAbsence.MEDICAL)
                .repriseOuJustificationDansDelai(true)
                .build();
        var r = AbandonPostePresomptionDemissionCalculator.compute(in, "FRANCE");
        assertThat(r.scoreContestation()).isEqualTo(100);
        assertThat(r.verdict()).isEqualTo(Verdict.CONTESTATION_SOLIDE);
    }

    @Test
    void dateExpirationDelai_egaleDateMedPlusDelai() {
        var in = new AbandonPostePresomptionDemissionInput.Builder()
                .dateMiseEnDemeurePresentee(LocalDate.of(2026, 5, 1))
                .modeNotificationMiseEnDemeure(ModeNotification.LRAR)
                .delaiAccordeJours(15)
                .miseEnDemeureMentionneDelai(true)
                .miseEnDemeureMentionneConsequences(true)
                .motifAbsenceInvoque(MotifAbsence.AUCUN)
                .build();
        var r = AbandonPostePresomptionDemissionCalculator.compute(in, "FRANCE");
        assertThat(r.dateExpirationDelai()).isEqualTo(LocalDate.of(2026, 5, 16));
    }

    @Test
    void basesJuridiques_contientToujoursFondementsLoi2022() {
        var r = AbandonPostePresomptionDemissionCalculator.compute(regulier(), "FRANCE");
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("L.1237-1-1"))
                .anyMatch(b -> b.contains("D.1237-2-1"));
    }

    @Test
    void paysBelgique_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                AbandonPostePresomptionDemissionCalculator.compute(regulier(), "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void dateMedManquante_throwsIllegalArgument() {
        var in = new AbandonPostePresomptionDemissionInput.Builder()
                .modeNotificationMiseEnDemeure(ModeNotification.LRAR)
                .delaiAccordeJours(15)
                .build();
        assertThatThrownBy(() ->
                AbandonPostePresomptionDemissionCalculator.compute(in, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateMiseEnDemeurePresentee");
    }

    @Test
    void delaiNegatif_throwsIllegalArgument() {
        var in = new AbandonPostePresomptionDemissionInput.Builder()
                .dateMiseEnDemeurePresentee(LocalDate.of(2026, 5, 1))
                .modeNotificationMiseEnDemeure(ModeNotification.LRAR)
                .delaiAccordeJours(-3)
                .build();
        assertThatThrownBy(() ->
                AbandonPostePresomptionDemissionCalculator.compute(in, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void commentaireTropLong_throwsIllegalArgument() {
        var in = new AbandonPostePresomptionDemissionInput.Builder()
                .dateMiseEnDemeurePresentee(LocalDate.of(2026, 5, 1))
                .modeNotificationMiseEnDemeure(ModeNotification.LRAR)
                .delaiAccordeJours(15)
                .motifAbsenceCommentaire("x".repeat(1001))
                .build();
        assertThatThrownBy(() ->
                AbandonPostePresomptionDemissionCalculator.compute(in, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static List<CodeContestation> codes(List<MotifContestation> motifs) {
        return motifs.stream().map(MotifContestation::code).toList();
    }
}
