package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-282 — tests unitaires du calcul du résumé « round courant / à qui le tour /
 * prochaine échéance » (fil rouge de l'en-tête).
 */
class ContradictoireServiceTest {

    private ContradictoireRound round(int n, ContradictoireParty party, LocalDate dueAt) {
        ContradictoireRound r = new ContradictoireRound();
        r.setRoundNumber(n);
        r.setParty(party);
        r.setDatedAt(LocalDate.of(2026, 6, 1));
        r.setResponseDueAt(dueAt);
        return r;
    }

    @Test
    void summary_noRound_ourTurnToFileTheSaisine() {
        var s = ContradictoireService.computeSummary(List.of());
        assertThat(s.currentRoundNumber()).isZero();
        assertThat(s.awaitingParty()).isEqualTo(ContradictoireParty.OURS);
        assertThat(s.nextDeadline()).isNull();
    }

    @Test
    void summary_lastIsAdverse_thenItIsOurTurn() {
        LocalDate due = LocalDate.of(2026, 7, 14);
        var s = ContradictoireService.computeSummary(List.of(
                round(1, ContradictoireParty.OURS, null),
                round(2, ContradictoireParty.ADVERSE, due)));
        assertThat(s.currentRoundNumber()).isEqualTo(2);
        assertThat(s.awaitingParty()).isEqualTo(ContradictoireParty.OURS);
        assertThat(s.nextDeadline()).isEqualTo(due);
    }

    @Test
    void summary_lastIsOurs_thenWeAwaitAdverse() {
        var s = ContradictoireService.computeSummary(List.of(
                round(1, ContradictoireParty.OURS, null)));
        assertThat(s.currentRoundNumber()).isEqualTo(1);
        assertThat(s.awaitingParty()).isEqualTo(ContradictoireParty.ADVERSE);
    }
}
