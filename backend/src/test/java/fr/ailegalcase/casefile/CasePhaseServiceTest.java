package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-283 / SF-283-01 — tests unitaires du tri déterministe des phases et du
 * calcul de la phase courante (la transition la plus récente).
 */
class CasePhaseServiceTest {

    private CasePhase phase(CasePhaseType type, LocalDate enteredAt) {
        CasePhase p = new CasePhase();
        p.setPhase(type);
        p.setEnteredAt(enteredAt);
        return p;
    }

    @Test
    void currentPhase_empty_isNull() {
        assertThat(CasePhaseService.currentPhase(List.of())).isNull();
    }

    @Test
    void currentPhase_isMostRecentTransition() {
        var sorted = CasePhaseService.sorted(List.of(
                phase(CasePhaseType.SAISINE, LocalDate.of(2026, 1, 10)),
                phase(CasePhaseType.FOND, LocalDate.of(2026, 4, 1))));
        assertThat(CasePhaseService.currentPhase(sorted)).isEqualTo(CasePhaseType.FOND);
    }

    @Test
    void sorted_ordersByEnteredAtThenReferentialOrder() {
        var sorted = CasePhaseService.sorted(List.of(
                phase(CasePhaseType.FOND, LocalDate.of(2026, 4, 1)),
                phase(CasePhaseType.SAISINE, LocalDate.of(2026, 1, 10)),
                // même date : départage par ordre de référentiel (CONCILIATION=2 < MISE_EN_ETAT=3)
                phase(CasePhaseType.MISE_EN_ETAT, LocalDate.of(2026, 2, 1)),
                phase(CasePhaseType.CONCILIATION, LocalDate.of(2026, 2, 1))));

        assertThat(sorted).extracting(CasePhase::getPhase).containsExactly(
                CasePhaseType.SAISINE,
                CasePhaseType.CONCILIATION,
                CasePhaseType.MISE_EN_ETAT,
                CasePhaseType.FOND);
    }
}
