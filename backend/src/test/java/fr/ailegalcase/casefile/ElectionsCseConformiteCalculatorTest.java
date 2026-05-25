package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static fr.ailegalcase.casefile.ElectionsCseConformiteCalculator.AnalyseElectionsCse;
import static fr.ailegalcase.casefile.ElectionsCseConformiteCalculator.CodeCritere;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-212-31 — tests unitaires du calculateur de conformité de la procédure
 * d'élections CSE (F-DT-65-elections-cse-conformite, FRANCE — L. 2314-1 à
 * L. 2314-37 CT ; R. 2314-1+ CT ; ordonnance n°2017-1386 du 22/09/2017).
 *
 * <p>Couvre les critères d'acceptation de la mini-spec : verdict 3 niveaux,
 * délai de contestation 15 jours invariant, alerte délai si date connue,
 * gate country FRANCE, effectif &lt; 11 → message CSE non obligatoire,
 * isolation workspace via le controller IT.</p>
 */
class ElectionsCseConformiteCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 25);

    /** Baseline conforme : 25 salariés, PAP négocié, OS invitées, 2 collèges, pas de contestation. */
    private static ElectionsCseConformiteInput baselineConforme() {
        return new ElectionsCseConformiteInput(
                25,            // effectif
                true,          // PAP négocié
                true,          // OS invitées dans les délais
                true,          // 2 collèges conformes
                false,         // pas de contestation
                null,          // pas de date élection (pas d'alerte)
                null           // pas de motif
        );
    }

    // ── AC1 : PAP absent → IRREGULARITE_MAJEURE ──────────────────────────────

    @Test
    void papAbsent_returnsIrregulariteMajeure() {
        var input = new ElectionsCseConformiteInput(
                25, false, true, true, false, null, null);
        var r = ElectionsCseConformiteCalculator.compute(input, "FRANCE", TODAY);
        assertThat(r.analyseElectionsCse()).isEqualTo(AnalyseElectionsCse.IRREGULARITE_MAJEURE);
        assertThat(r.pointsIrregularite())
                .extracting(ElectionsCseConformiteCalculator.PointIrregularite::code)
                .contains(CodeCritere.DT65_PAP_NEGOCIE);
        assertThat(r.messages()).anyMatch(m -> m.contains("IRRÉGULARITÉ MAJEURE"));
    }

    // ── AC2 : délai contestation invariant 15 jours ──────────────────────────

    @Test
    void delaiContestationJoursVaut15() {
        var r = ElectionsCseConformiteCalculator.compute(baselineConforme(), "FRANCE", TODAY);
        assertThat(r.delaiContestationJours())
                .isEqualTo(ElectionsCseConformiteCalculator.DELAI_CONTESTATION_JOURS)
                .isEqualTo(15);
    }

    // ── AC3 : alerte délai contestation si date connue + < 15 j ──────────────

    @Test
    void dateElectionRecente_avecJoursRestants_alerteTrue() {
        // Élection 10 jours avant aujourd'hui → délai limite = aujourd'hui + 5 j → alerte.
        LocalDate dateElection = TODAY.minusDays(10);
        var input = new ElectionsCseConformiteInput(
                25, true, true, true, true, dateElection, "Irrégularité PAP");
        var r = ElectionsCseConformiteCalculator.compute(input, "FRANCE", TODAY);
        assertThat(r.alerteDelaiContestation()).isTrue();
        assertThat(r.dateLimitContestationSiConnue())
                .isEqualTo(dateElection.plusDays(15));
        assertThat(r.messages()).anyMatch(m -> m.contains("ALERTE DÉLAI"));
    }

    @Test
    void dateElectionTresAncienne_delaiExpire_alerteFalse_messageDelaiExpire() {
        LocalDate dateElection = TODAY.minusDays(40);
        var input = new ElectionsCseConformiteInput(
                25, true, true, true, true, dateElection, null);
        var r = ElectionsCseConformiteCalculator.compute(input, "FRANCE", TODAY);
        assertThat(r.alerteDelaiContestation()).isFalse();
        assertThat(r.messages()).anyMatch(m -> m.contains("DÉLAI EXPIRÉ"));
    }

    @Test
    void dateElectionNull_pasDeDateLimitNiAlerte() {
        var r = ElectionsCseConformiteCalculator.compute(baselineConforme(), "FRANCE", TODAY);
        assertThat(r.dateLimitContestationSiConnue()).isNull();
        assertThat(r.alerteDelaiContestation()).isFalse();
    }

    // ── AC4 : effectif < 11 → message CSE non obligatoire ────────────────────

    @Test
    void effectifInferieurA11_messageCseNonObligatoire() {
        var input = new ElectionsCseConformiteInput(
                8, true, true, true, false, null, null);
        var r = ElectionsCseConformiteCalculator.compute(input, "FRANCE", TODAY);
        assertThat(r.messages()).anyMatch(m -> m.contains("CSE non obligatoire"));
    }

    @Test
    void effectifAuSeuil_pasDeMessageCseNonObligatoire() {
        var input = new ElectionsCseConformiteInput(
                11, true, true, true, false, null, null);
        var r = ElectionsCseConformiteCalculator.compute(input, "FRANCE", TODAY);
        assertThat(r.messages()).noneMatch(m -> m.contains("CSE non obligatoire"));
    }

    // ── AC5 : gate country FRANCE ────────────────────────────────────────────

    @Test
    void countryBelgique_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                ElectionsCseConformiteCalculator.compute(baselineConforme(), "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void countryNull_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                ElectionsCseConformiteCalculator.compute(baselineConforme(), null, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inputNull_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                ElectionsCseConformiteCalculator.compute(null, "FRANCE", TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manquantes");
    }

    // ── AC6 : verdict CONFORME si rien à signaler ────────────────────────────

    @Test
    void baselineConforme_returnsConforme() {
        var r = ElectionsCseConformiteCalculator.compute(baselineConforme(), "FRANCE", TODAY);
        assertThat(r.analyseElectionsCse()).isEqualTo(AnalyseElectionsCse.CONFORME);
        assertThat(r.pointsIrregularite()).isEmpty();
        assertThat(r.scoreConformite()).isEqualTo(100);
    }

    // ── AC7 : verdict IRREGULARITE_MINEURE — délai invitation OS non respecté ─

    @Test
    void delaiInvitationOSNonRespecte_returnsIrregulariteMineure() {
        var input = new ElectionsCseConformiteInput(
                25, true, false, true, false, null, null);
        var r = ElectionsCseConformiteCalculator.compute(input, "FRANCE", TODAY);
        assertThat(r.analyseElectionsCse()).isEqualTo(AnalyseElectionsCse.IRREGULARITE_MINEURE);
        assertThat(r.pointsIrregularite())
                .extracting(ElectionsCseConformiteCalculator.PointIrregularite::code)
                .contains(CodeCritere.DT65_DELAI_INVITATION);
    }

    // ── AC8 : collèges non conformes → IRREGULARITE_MAJEURE ──────────────────

    @Test
    void collegesNonConformes_returnsIrregulariteMajeure() {
        var input = new ElectionsCseConformiteInput(
                25, true, true, false, false, null, null);
        var r = ElectionsCseConformiteCalculator.compute(input, "FRANCE", TODAY);
        assertThat(r.analyseElectionsCse()).isEqualTo(AnalyseElectionsCse.IRREGULARITE_MAJEURE);
        assertThat(r.pointsIrregularite())
                .extracting(ElectionsCseConformiteCalculator.PointIrregularite::code)
                .contains(CodeCritere.DT65_COLLEGES);
    }

    // ── AC9 : résultats contestés → point ajouté + base L. 2314-32 CT ────────

    @Test
    void resultatsContestes_ajoutePointContestationEtBase() {
        var input = new ElectionsCseConformiteInput(
                25, true, true, true, true, TODAY.minusDays(5), "Vote électronique non encadré");
        var r = ElectionsCseConformiteCalculator.compute(input, "FRANCE", TODAY);
        assertThat(r.pointsIrregularite())
                .extracting(ElectionsCseConformiteCalculator.PointIrregularite::code)
                .contains(CodeCritere.DT65_CONTESTATION);
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 2314-32"));
        assertThat(r.messages()).anyMatch(m -> m.contains("Vote électronique non encadré"));
    }

    // ── AC10 : score borné [0, 100] ──────────────────────────────────────────

    @Test
    void scoreBorneEntreZeroEtCent() {
        var allBadInput = new ElectionsCseConformiteInput(
                500, false, false, false, true, null, null);
        var r = ElectionsCseConformiteCalculator.compute(allBadInput, "FRANCE", TODAY);
        assertThat(r.scoreConformite()).isBetween(0, 100);

        var r2 = ElectionsCseConformiteCalculator.compute(baselineConforme(), "FRANCE", TODAY);
        assertThat(r2.scoreConformite()).isBetween(0, 100);
    }

    // ── AC11 : bases juridiques contiennent les fondements CSE ───────────────

    @Test
    void basesJuridiques_contiennentOrdonnance2017EtCT() {
        var r = ElectionsCseConformiteCalculator.compute(baselineConforme(), "FRANCE", TODAY);
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("2017-1386"));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 2314-1"));
    }

    // ── AC12 : country FRANCE en minuscules accepté ──────────────────────────

    @Test
    void countryFranceMinuscule_accepteCaseInsensitive() {
        var r = ElectionsCseConformiteCalculator.compute(baselineConforme(), "france", TODAY);
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    // ── AC13 : delai à pile la limite — joursRestants = 0 → alerte ───────────

    @Test
    void dateElectionExactement15JoursAvant_alerteTrue() {
        LocalDate dateElection = TODAY.minusDays(15);
        var input = new ElectionsCseConformiteInput(
                25, true, true, true, false, dateElection, null);
        var r = ElectionsCseConformiteCalculator.compute(input, "FRANCE", TODAY);
        assertThat(r.alerteDelaiContestation()).isTrue();
    }
}
