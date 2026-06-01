package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-31 : tests unitaires de {@link AccordEntrepriseValiditeAnalyzer}
 * (F-DT-67, outil FRANCE uniquement).
 *
 * <p>Logique déterministe (art. L.2232-12 CT ; L.2261-7 à L.2261-11 CT) :
 * <ul>
 *   <li>signataires ≥ 50 % → MAJORITE_50 + VALIDE ;</li>
 *   <li>30 % ≤ signataires &lt; 50 % + référendum approuvé → REFERENDUM_30 +
 *       VALIDE_SOUS_RESERVE ;</li>
 *   <li>30 % ≤ signataires &lt; 50 % sans référendum → INSUFFISANTE + NON_VALIDE ;</li>
 *   <li>signataires &lt; 30 % → INSUFFISANTE + NON_VALIDE ;</li>
 *   <li>révision avec parties non habilitées → item non conforme + NON_VALIDE ;</li>
 *   <li>dénonciation avec date → dateFinSurvie = date + 15 mois ;</li>
 *   <li>validations : pourcentage hors [0,100], typeOperation null, révision sans
 *       signePartiesHabilitees → IllegalArgumentException.</li>
 * </ul>
 */
class AccordEntrepriseValiditeAnalyzerTest {

    private static BigDecimal pct(String v) {
        return new BigDecimal(v);
    }

    @Test
    void signataires55_conclusion_majorite50_valide() {
        AccordEntrepriseValiditeResult r = AccordEntrepriseValiditeAnalyzer.analyze(
                pct("55"), false, false, AccordTypeOperation.CONCLUSION, null, null, null);

        assertThat(r.conditionMajorite()).isEqualTo(AccordConditionMajorite.MAJORITE_50);
        assertThat(r.statut()).isEqualTo(AccordEntrepriseValiditeStatut.VALIDE);
        assertThat(r.itemsNonConformes()).isZero();
        assertThat(r.baseJuridique()).contains("L.2232-12");
    }

    @Test
    void signataires35_referendumApprouve_referendum30_valideSousReserve() {
        AccordEntrepriseValiditeResult r = AccordEntrepriseValiditeAnalyzer.analyze(
                pct("35"), true, true, AccordTypeOperation.CONCLUSION, null, null, null);

        assertThat(r.conditionMajorite()).isEqualTo(AccordConditionMajorite.REFERENDUM_30);
        assertThat(r.statut()).isEqualTo(AccordEntrepriseValiditeStatut.VALIDE_SOUS_RESERVE);
        assertThat(r.itemsNonConformes()).isZero();
        assertThat(r.consequences()).anySatisfy(c -> assertThat(c).contains("référendum"));
    }

    @Test
    void signataires35_sansReferendum_insuffisante_nonValide() {
        AccordEntrepriseValiditeResult r = AccordEntrepriseValiditeAnalyzer.analyze(
                pct("35"), false, false, AccordTypeOperation.CONCLUSION, null, null, null);

        assertThat(r.conditionMajorite()).isEqualTo(AccordConditionMajorite.INSUFFISANTE);
        assertThat(r.statut()).isEqualTo(AccordEntrepriseValiditeStatut.NON_VALIDE);
        assertThat(r.itemsNonConformes()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void signataires35_referendumOrganiseNonApprouve_insuffisante_nonValide() {
        AccordEntrepriseValiditeResult r = AccordEntrepriseValiditeAnalyzer.analyze(
                pct("35"), true, false, AccordTypeOperation.CONCLUSION, null, null, null);

        assertThat(r.conditionMajorite()).isEqualTo(AccordConditionMajorite.INSUFFISANTE);
        assertThat(r.statut()).isEqualTo(AccordEntrepriseValiditeStatut.NON_VALIDE);
        // l'item « référendum approuvé » est présent et non conforme
        assertThat(r.checklist()).anySatisfy(i -> {
            assertThat(i.item()).contains("Référendum");
            assertThat(i.conforme()).isFalse();
        });
    }

    @Test
    void signataires25_insuffisante_nonValide() {
        AccordEntrepriseValiditeResult r = AccordEntrepriseValiditeAnalyzer.analyze(
                pct("25"), false, false, AccordTypeOperation.CONCLUSION, null, null, null);

        assertThat(r.conditionMajorite()).isEqualTo(AccordConditionMajorite.INSUFFISANTE);
        assertThat(r.statut()).isEqualTo(AccordEntrepriseValiditeStatut.NON_VALIDE);
        assertThat(r.consequences()).anySatisfy(c -> assertThat(c).contains("30 %"));
    }

    @Test
    void revision_majorite_partiesNonHabilitees_itemNonConforme_nonValide() {
        AccordEntrepriseValiditeResult r = AccordEntrepriseValiditeAnalyzer.analyze(
                pct("60"), false, false, AccordTypeOperation.REVISION, false, null, null);

        // condition de majorité OK mais item parties habilitées KO → NON_VALIDE
        assertThat(r.conditionMajorite()).isEqualTo(AccordConditionMajorite.MAJORITE_50);
        assertThat(r.statut()).isEqualTo(AccordEntrepriseValiditeStatut.NON_VALIDE);
        assertThat(r.checklist()).anySatisfy(i -> {
            assertThat(i.item()).contains("parties habilitées");
            assertThat(i.conforme()).isFalse();
        });
    }

    @Test
    void revision_majorite_partiesHabilitees_valide() {
        AccordEntrepriseValiditeResult r = AccordEntrepriseValiditeAnalyzer.analyze(
                pct("60"), false, false, AccordTypeOperation.REVISION, true, null, null);

        assertThat(r.statut()).isEqualTo(AccordEntrepriseValiditeStatut.VALIDE);
        assertThat(r.itemsNonConformes()).isZero();
    }

    @Test
    void denonciation_avecDate_dateFinSurviePlus15Mois() {
        LocalDate denonciation = LocalDate.of(2025, 1, 15);
        AccordEntrepriseValiditeResult r = AccordEntrepriseValiditeAnalyzer.analyze(
                pct("60"), false, false, AccordTypeOperation.DENONCIATION, null, true, denonciation);

        assertThat(r.dateDenonciation()).isEqualTo(denonciation);
        assertThat(r.dateFinSurvie()).isEqualTo(denonciation.plusMonths(15));
        assertThat(r.dateFinSurvie()).isEqualTo(LocalDate.of(2026, 4, 15));
        assertThat(r.statut()).isEqualTo(AccordEntrepriseValiditeStatut.VALIDE);
    }

    @Test
    void denonciation_preavisNonRespecte_nonValide() {
        AccordEntrepriseValiditeResult r = AccordEntrepriseValiditeAnalyzer.analyze(
                pct("60"), false, false, AccordTypeOperation.DENONCIATION, null, false, null);

        assertThat(r.statut()).isEqualTo(AccordEntrepriseValiditeStatut.NON_VALIDE);
        assertThat(r.checklist()).anySatisfy(i -> {
            assertThat(i.item()).contains("Préavis");
            assertThat(i.conforme()).isFalse();
        });
    }

    @Test
    void pourcentageHorsBorne_illegalArgument() {
        assertThatThrownBy(() -> AccordEntrepriseValiditeAnalyzer.analyze(
                pct("120"), false, false, AccordTypeOperation.CONCLUSION, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AccordEntrepriseValiditeAnalyzer.analyze(
                pct("-1"), false, false, AccordTypeOperation.CONCLUSION, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pourcentageNull_illegalArgument() {
        assertThatThrownBy(() -> AccordEntrepriseValiditeAnalyzer.analyze(
                null, false, false, AccordTypeOperation.CONCLUSION, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void typeOperationNull_illegalArgument() {
        assertThatThrownBy(() -> AccordEntrepriseValiditeAnalyzer.analyze(
                pct("60"), false, false, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void revisionSansSignePartiesHabilitees_illegalArgument() {
        assertThatThrownBy(() -> AccordEntrepriseValiditeAnalyzer.analyze(
                pct("60"), false, false, AccordTypeOperation.REVISION, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
