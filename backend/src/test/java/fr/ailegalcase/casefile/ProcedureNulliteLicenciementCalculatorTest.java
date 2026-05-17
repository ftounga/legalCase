package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.ProcedureNulliteLicenciementCalculator.CodeVice;
import fr.ailegalcase.casefile.ProcedureNulliteLicenciementCalculator.Gravite;
import fr.ailegalcase.casefile.ProcedureNulliteLicenciementCalculator.Verdict;
import fr.ailegalcase.casefile.ProcedureNulliteLicenciementCalculator.ViceDetecte;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-DT-36-01 : tests unitaires du moteur de détection des vices de procédure
 * de licenciement (FR).
 */
class ProcedureNulliteLicenciementCalculatorTest {

    /**
     * Procédure régulière de référence : convocation présentée le lundi 06/01/2025,
     * entretien le lundi 13/01/2025 (5 jours ouvrables : mar, mer, jeu, ven, lun),
     * notification le vendredi 17/01/2025 (4 jours ouvrables après l'entretien).
     */
    private ProcedureNulliteLicenciementInput.Builder procedureReguliere() {
        return new ProcedureNulliteLicenciementInput.Builder()
                .convocationEnvoyee(true)
                .dateConvocationPresentee(LocalDate.of(2025, 1, 6))
                .dateEntretienPrealable(LocalDate.of(2025, 1, 13))
                .entretienTenu(true)
                .dateNotificationLicenciement(LocalDate.of(2025, 1, 17))
                .lettreLicenciementEcrite(true)
                .lettreMotivee(true)
                .motivationSuffisante(true)
                .licenciementPourMotifGrave(false)
                .licenciementCollectif(false)
                .conventionCollectiveApplicable(false);
    }

    private static List<CodeVice> codes(ProcedureNulliteResult r) {
        return r.vicesDetectes().stream().map(ViceDetecte::code).toList();
    }

    @Test
    void compute_procedureReguliere_aucunVice() {
        var r = ProcedureNulliteLicenciementCalculator.compute(procedureReguliere().build(), "FRANCE");
        assertThat(r.vicesDetectes()).isEmpty();
        assertThat(r.scoreNullite()).isZero();
        assertThat(r.verdict()).isEqualTo(Verdict.PROCEDURE_REGULIERE);
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    // --- Les 10 vices isolés ---

    @Test
    void compute_absenceConvocation_detecte() {
        var in = procedureReguliere().convocationEnvoyee(false).build();
        var r = ProcedureNulliteLicenciementCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).containsExactly(CodeVice.ABSENCE_CONVOCATION);
        assertThat(r.scoreNullite()).isEqualTo(30);
        // Un unique vice AVERE suffit à caractériser la nullité avérée — le verdict
        // est piloté par la gravité, non par un seuil de score.
        assertThat(r.verdict()).isEqualTo(Verdict.NULLITE_AVEREE);
        assertThat(r.basesJuridiques()).contains("Art. L.1232-2 C. trav.");
    }

    @Test
    void compute_delaiConvocationInsuffisant_detecte() {
        // convocation lundi 06/01, entretien jeudi 09/01 → 3 jours ouvrables (< 5)
        var in = procedureReguliere()
                .dateEntretienPrealable(LocalDate.of(2025, 1, 9))
                .dateNotificationLicenciement(LocalDate.of(2025, 1, 17))
                .build();
        var r = ProcedureNulliteLicenciementCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).containsExactly(CodeVice.DELAI_CONVOCATION_INSUFFISANT);
        assertThat(r.scoreNullite()).isEqualTo(30);
    }

    @Test
    void compute_absenceEntretien_detecte() {
        var in = procedureReguliere().entretienTenu(false).build();
        var r = ProcedureNulliteLicenciementCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).containsExactly(CodeVice.ABSENCE_ENTRETIEN);
    }

    @Test
    void compute_notificationPrematuree_detecte() {
        // entretien lundi 13/01, notification mardi 14/01 → 1 jour ouvrable (< 2)
        var in = procedureReguliere()
                .dateNotificationLicenciement(LocalDate.of(2025, 1, 14))
                .build();
        var r = ProcedureNulliteLicenciementCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).containsExactly(CodeVice.NOTIFICATION_PREMATUREE);
    }

    @Test
    void compute_absenceLettre_detecte() {
        var in = procedureReguliere().lettreLicenciementEcrite(false).build();
        var r = ProcedureNulliteLicenciementCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).containsExactly(CodeVice.ABSENCE_LETTRE);
    }

    @Test
    void compute_lettreNonMotivee_detecte() {
        // lettreMotivee=false → LETTRE_NON_MOTIVEE seul (MOTIVATION_INSUFFISANTE
        // exige lettreMotivee=true) ; motivationSuffisante laissé null.
        var in = procedureReguliere().lettreMotivee(false).motivationSuffisante(null).build();
        var r = ProcedureNulliteLicenciementCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).containsExactly(CodeVice.LETTRE_NON_MOTIVEE);
    }

    @Test
    void compute_motivationInsuffisante_detecte() {
        var in = procedureReguliere().lettreMotivee(true).motivationSuffisante(false).build();
        var r = ProcedureNulliteLicenciementCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).containsExactly(CodeVice.MOTIVATION_INSUFFISANTE);
        assertThat(r.basesJuridiques()).contains("Art. L.1232-6 et L.1235-2 C. trav.");
        // Vice d'appréciation (suffisance de la motivation) → gravité PROBABLE :
        // seul, il rend le verdict NULLITE_PROBABLE et non NULLITE_AVEREE.
        assertThat(r.vicesDetectes()).singleElement()
                .extracting(ViceDetecte::gravite).isEqualTo(Gravite.PROBABLE);
        assertThat(r.scoreNullite()).isEqualTo(20);
        assertThat(r.verdict()).isEqualTo(Verdict.NULLITE_PROBABLE);
    }

    @Test
    void compute_absenceConvocationMotifGrave_detecte() {
        // faute grave + absence de convocation → 2 vices (ABSENCE_CONVOCATION
        // + ABSENCE_CONVOCATION_MOTIF_GRAVE)
        var in = procedureReguliere()
                .convocationEnvoyee(false)
                .licenciementPourMotifGrave(true)
                .build();
        var r = ProcedureNulliteLicenciementCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).containsExactlyInAnyOrder(
                CodeVice.ABSENCE_CONVOCATION,
                CodeVice.ABSENCE_CONVOCATION_MOTIF_GRAVE);
        assertThat(r.verdict()).isEqualTo(Verdict.NULLITE_AVEREE);
    }

    @Test
    void compute_procedureCseNonRespectee_detecte() {
        var in = procedureReguliere()
                .licenciementCollectif(true)
                .procedureCseRespectee(false)
                .build();
        var r = ProcedureNulliteLicenciementCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).containsExactly(CodeVice.PROCEDURE_CSE_NON_RESPECTEE);
    }

    @Test
    void compute_conventionCollectiveNonRespectee_detecte() {
        var in = procedureReguliere()
                .conventionCollectiveApplicable(true)
                .conventionCollectiveRespectee(false)
                .build();
        var r = ProcedureNulliteLicenciementCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).containsExactly(CodeVice.CONVENTION_COLLECTIVE_NON_RESPECTEE);
        // Vice dépendant de la clause exacte de la CCN → gravité PROBABLE.
        assertThat(r.vicesDetectes()).singleElement()
                .extracting(ViceDetecte::gravite).isEqualTo(Gravite.PROBABLE);
        assertThat(r.verdict()).isEqualTo(Verdict.NULLITE_PROBABLE);
    }

    // --- Délais ouvrables avec week-ends ---

    @Test
    void joursOuvrables_excluentSamediEtDimanche() {
        // lundi 06/01 → lundi 13/01 : mar, mer, jeu, ven, lun = 5 (sam/dim exclus)
        assertThat(ProcedureNulliteLicenciementCalculator.joursOuvrablesEntre(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 13))).isEqualTo(5);
        // vendredi 10/01 → lundi 13/01 : sam/dim exclus, lundi = 1
        assertThat(ProcedureNulliteLicenciementCalculator.joursOuvrablesEntre(
                LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 13))).isEqualTo(1);
        // même date → 0
        assertThat(ProcedureNulliteLicenciementCalculator.joursOuvrablesEntre(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 6))).isZero();
    }

    @Test
    void compute_delaiConvocationAvecWeekEnd_detecteVice() {
        // convocation mercredi 08/01, entretien lundi 13/01 : jeu, ven, lun = 3
        // jours ouvrables (sam 11 / dim 12 exclus) → délai insuffisant (< 5)
        var in = procedureReguliere()
                .dateConvocationPresentee(LocalDate.of(2025, 1, 8))
                .dateEntretienPrealable(LocalDate.of(2025, 1, 13))
                .dateNotificationLicenciement(LocalDate.of(2025, 1, 17))
                .build();
        var r = ProcedureNulliteLicenciementCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeVice.DELAI_CONVOCATION_INSUFFISANT);
    }

    @Test
    void compute_notificationLendemainWeekEnd_resteVice() {
        // entretien vendredi 10/01, notification lundi 13/01 : sam/dim exclus →
        // 1 jour ouvrable (< 2) → notification prématurée malgré 3 jours calendaires
        var in = procedureReguliere()
                .dateConvocationPresentee(LocalDate.of(2025, 1, 2))
                .dateEntretienPrealable(LocalDate.of(2025, 1, 10))
                .dateNotificationLicenciement(LocalDate.of(2025, 1, 13))
                .build();
        var r = ProcedureNulliteLicenciementCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeVice.NOTIFICATION_PREMATUREE);
    }

    // --- Cumul → NULLITE_AVEREE ---

    @Test
    void compute_cumulVicesGraves_nulliteAveree() {
        var in = procedureReguliere()
                .lettreLicenciementEcrite(false)
                .lettreMotivee(false)
                .motivationSuffisante(null)
                .build();
        var r = ProcedureNulliteLicenciementCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).containsExactlyInAnyOrder(
                CodeVice.ABSENCE_LETTRE, CodeVice.LETTRE_NON_MOTIVEE);
        assertThat(r.scoreNullite()).isEqualTo(60);
        assertThat(r.verdict()).isEqualTo(Verdict.NULLITE_AVEREE);
    }

    @Test
    void compute_viceAvereEtViceProbable_nulliteAveree() {
        // ABSENCE_CONVOCATION (AVERE) + MOTIVATION_INSUFFISANTE (PROBABLE) :
        // la présence d'un seul vice AVERE prime → verdict NULLITE_AVEREE.
        var in = procedureReguliere()
                .convocationEnvoyee(false)
                .lettreMotivee(true)
                .motivationSuffisante(false)
                .build();
        var r = ProcedureNulliteLicenciementCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).containsExactlyInAnyOrder(
                CodeVice.ABSENCE_CONVOCATION, CodeVice.MOTIVATION_INSUFFISANTE);
        assertThat(r.scoreNullite()).isEqualTo(50);
        assertThat(r.verdict()).isEqualTo(Verdict.NULLITE_AVEREE);
    }

    @Test
    void compute_scorePlafonneA100() {
        // 5 vices avérés (5 × 30 = 150) → plafond 100
        var in = procedureReguliere()
                .convocationEnvoyee(false)
                .entretienTenu(false)
                .lettreLicenciementEcrite(false)
                .lettreMotivee(false)
                .motivationSuffisante(null)
                .licenciementCollectif(true)
                .procedureCseRespectee(false)
                .build();
        var r = ProcedureNulliteLicenciementCalculator.compute(in, "FRANCE");
        assertThat(r.scoreNullite()).isEqualTo(100);
        assertThat(r.verdict()).isEqualTo(Verdict.NULLITE_AVEREE);
    }

    // --- Validation / pays ---

    @Test
    void compute_paysNonFrance_rejete() {
        assertThatThrownBy(() ->
                ProcedureNulliteLicenciementCalculator.compute(procedureReguliere().build(), "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_inputNull_rejete() {
        assertThatThrownBy(() ->
                ProcedureNulliteLicenciementCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_commentaireTropLong_rejete() {
        var in = procedureReguliere()
                .motivationCommentaire("x".repeat(1001))
                .build();
        assertThatThrownBy(() ->
                ProcedureNulliteLicenciementCalculator.compute(in, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
