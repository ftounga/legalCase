package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-207-04 : tests unitaires du calculateur de délai de déclaration AT Fedris.
 *
 * <p>Plan (12 cas) :
 * <ul>
 *   <li>Mode prospectif × 3 verdicts (OUVERT, IMMINENT, DEPASSE).</li>
 *   <li>Mode rétrospectif × 2 verdicts (DANS_LES_TEMPS, HORS_DELAI).</li>
 *   <li>Bornes du seuil IMMINENT (2 j inclus / 3 j non inclus).</li>
 *   <li>Default {@code dateConnaissance = dateAccident} si non fournie.</li>
 *   <li>Default {@code dateAction = today} si non fournie (mode prospectif).</li>
 *   <li>Mode rétrospectif — déclaration le dernier jour utile = DANS_LES_TEMPS
 *       (frontière inclusive).</li>
 *   <li>Mode rétrospectif — calcul des jours de retard (joursRestants négatif).</li>
 *   <li>Validation interne — {@code dateAccident null} levée
 *       {@link IllegalArgumentException} (défense en profondeur).</li>
 * </ul>
 */
class AtFedrisDeclarationCalculatorTest {

    private static AtFedrisDeclarationRequest req(LocalDate dateAccident,
                                                  LocalDate dateConnaissance,
                                                  LocalDate dateAction,
                                                  LocalDate dateDeclaration) {
        return new AtFedrisDeclarationRequest(dateAccident, dateConnaissance, dateAction, dateDeclaration);
    }

    // =========================================================================
    // Mode prospectif — 3 verdicts
    // =========================================================================

    @Test
    void prospectif_joursRestants5_DELAI_OUVERT() {
        // dateConnaissance = 2026-05-10 → dateLimite = 2026-05-18
        // dateAction = 2026-05-13 → joursRestants = 5 > 2 → DELAI_OUVERT
        var result = AtFedrisDeclarationCalculator.compute(req(
                LocalDate.of(2026, 5, 10),
                null,
                LocalDate.of(2026, 5, 13),
                null));
        assertThat(result.verdict()).isEqualTo("DELAI_OUVERT");
        assertThat(result.joursRestants()).isEqualTo(5);
        assertThat(result.dateLimiteDeclaration()).isEqualTo(LocalDate.of(2026, 5, 18));
        assertThat(result.regleAppliquee()).isEqualTo("8_JOURS_LOI_1971_ART_62");
        assertThat(result.baseJuridique()).contains("10 avril 1971").contains("art. 62");
        assertThat(result.consequencesNonRespect()).contains("Préjudice").contains("intérêts moratoires");
        assertThat(result.formuleCalcul()).contains("8 jours").contains("DELAI_OUVERT");
        assertThat(result.dateDeclarationEffectuee()).isNull();
    }

    @Test
    void prospectif_joursRestants1_DELAI_IMMINENT() {
        // dateConnaissance = 2026-05-10 → dateLimite = 2026-05-18
        // dateAction = 2026-05-17 → joursRestants = 1 ∈ [0, 2] → DELAI_IMMINENT
        var result = AtFedrisDeclarationCalculator.compute(req(
                LocalDate.of(2026, 5, 10),
                null,
                LocalDate.of(2026, 5, 17),
                null));
        assertThat(result.verdict()).isEqualTo("DELAI_IMMINENT");
        assertThat(result.joursRestants()).isEqualTo(1);
    }

    @Test
    void prospectif_joursRestantsNegatifs_DELAI_DEPASSE() {
        // dateConnaissance = 2026-05-01 → dateLimite = 2026-05-09
        // dateAction = 2026-05-15 → joursRestants = -6 → DELAI_DEPASSE
        var result = AtFedrisDeclarationCalculator.compute(req(
                LocalDate.of(2026, 5, 1),
                null,
                LocalDate.of(2026, 5, 15),
                null));
        assertThat(result.verdict()).isEqualTo("DELAI_DEPASSE");
        assertThat(result.joursRestants()).isEqualTo(-6);
        assertThat(result.formuleCalcul()).contains("DELAI_DEPASSE");
    }

    // =========================================================================
    // Bornes du seuil IMMINENT (2 j inclus / 3 j non inclus)
    // =========================================================================

    @Test
    void frontiereExacte_joursRestants2_DELAI_IMMINENT() {
        // dateLimite - dateAction = 2 → seuil inclusif → IMMINENT
        LocalDate dateConnaissance = LocalDate.of(2026, 5, 10);
        LocalDate dateAction = dateConnaissance.plusDays(6); // 2026-05-16 → joursRestants = 2
        var result = AtFedrisDeclarationCalculator.compute(req(
                dateConnaissance, null, dateAction, null));
        assertThat(result.joursRestants()).isEqualTo(2);
        assertThat(result.verdict()).isEqualTo("DELAI_IMMINENT");
    }

    @Test
    void frontiereExacte_joursRestants3_DELAI_OUVERT() {
        // dateLimite - dateAction = 3 → > 2 → OUVERT
        LocalDate dateConnaissance = LocalDate.of(2026, 5, 10);
        LocalDate dateAction = dateConnaissance.plusDays(5); // 2026-05-15 → joursRestants = 3
        var result = AtFedrisDeclarationCalculator.compute(req(
                dateConnaissance, null, dateAction, null));
        assertThat(result.joursRestants()).isEqualTo(3);
        assertThat(result.verdict()).isEqualTo("DELAI_OUVERT");
    }

    @Test
    void frontiereExacte_joursRestants0_DELAI_IMMINENT() {
        // dateAction == dateLimite → joursRestants = 0 → IMMINENT (≥ 0 inclus)
        LocalDate dateConnaissance = LocalDate.of(2026, 5, 10);
        LocalDate dateAction = dateConnaissance.plusDays(8); // 2026-05-18 = dateLimite
        var result = AtFedrisDeclarationCalculator.compute(req(
                dateConnaissance, null, dateAction, null));
        assertThat(result.joursRestants()).isEqualTo(0);
        assertThat(result.verdict()).isEqualTo("DELAI_IMMINENT");
    }

    // =========================================================================
    // Mode rétrospectif — DANS_LES_TEMPS / HORS_DELAI
    // =========================================================================

    @Test
    void retrospectif_declarationAvantDateLimite_DECLARATION_DANS_LES_TEMPS() {
        // dateConnaissance = 2026-05-10 → dateLimite = 2026-05-18
        // dateDeclaration = 2026-05-15 → DANS_LES_TEMPS (joursRestants = 3)
        var result = AtFedrisDeclarationCalculator.compute(req(
                LocalDate.of(2026, 5, 10),
                null,
                null,
                LocalDate.of(2026, 5, 15)));
        assertThat(result.verdict()).isEqualTo("DECLARATION_DANS_LES_TEMPS");
        assertThat(result.joursRestants()).isEqualTo(3);
        assertThat(result.dateDeclarationEffectuee()).isEqualTo(LocalDate.of(2026, 5, 15));
        assertThat(result.dateLimiteDeclaration()).isEqualTo(LocalDate.of(2026, 5, 18));
    }

    @Test
    void retrospectif_declarationLeDernierJour_DECLARATION_DANS_LES_TEMPS() {
        // Frontière inclusive : dateDeclaration == dateLimite → DANS_LES_TEMPS
        LocalDate dateConnaissance = LocalDate.of(2026, 5, 10);
        LocalDate dateLimite = dateConnaissance.plusDays(8); // 2026-05-18
        var result = AtFedrisDeclarationCalculator.compute(req(
                dateConnaissance, null, null, dateLimite));
        assertThat(result.verdict()).isEqualTo("DECLARATION_DANS_LES_TEMPS");
        assertThat(result.joursRestants()).isEqualTo(0);
    }

    @Test
    void retrospectif_declarationApresDateLimite_DECLARATION_HORS_DELAI() {
        // dateConnaissance = 2026-05-10 → dateLimite = 2026-05-18
        // dateDeclaration = 2026-05-25 → HORS_DELAI (retard de 7 jours, joursRestants = -7)
        var result = AtFedrisDeclarationCalculator.compute(req(
                LocalDate.of(2026, 5, 10),
                null,
                null,
                LocalDate.of(2026, 5, 25)));
        assertThat(result.verdict()).isEqualTo("DECLARATION_HORS_DELAI");
        assertThat(result.joursRestants()).isEqualTo(-7);
        assertThat(result.formuleCalcul()).contains("DECLARATION_HORS_DELAI");
    }

    // =========================================================================
    // Defaults
    // =========================================================================

    @Test
    void defaut_dateConnaissanceVautDateAccident() {
        // Aucune dateConnaissance → calcul sur dateAccident.
        // dateAccident = 2026-05-10 → dateLimite = 2026-05-18
        // dateAction = 2026-05-14 → joursRestants = 4
        var result = AtFedrisDeclarationCalculator.compute(req(
                LocalDate.of(2026, 5, 10),
                null,
                LocalDate.of(2026, 5, 14),
                null));
        assertThat(result.dateConnaissanceEmployeur()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(result.dateLimiteDeclaration()).isEqualTo(LocalDate.of(2026, 5, 18));
        assertThat(result.joursRestants()).isEqualTo(4);
    }

    @Test
    void defaut_dateConnaissancePosterieureAccident_decalageDuDelai() {
        // Accident dimanche 2026-05-10, employeur informé lundi 2026-05-11
        // → dateLimite = 2026-05-11 + 8 = 2026-05-19 (et pas 2026-05-18).
        var result = AtFedrisDeclarationCalculator.compute(req(
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 11),
                LocalDate.of(2026, 5, 15),
                null));
        assertThat(result.dateConnaissanceEmployeur()).isEqualTo(LocalDate.of(2026, 5, 11));
        assertThat(result.dateLimiteDeclaration()).isEqualTo(LocalDate.of(2026, 5, 19));
        assertThat(result.joursRestants()).isEqualTo(4);
        assertThat(result.verdict()).isEqualTo("DELAI_OUVERT");
    }

    // =========================================================================
    // Défense en profondeur
    // =========================================================================

    @Test
    void dateAccidentNull_leveeIllegalArgumentException() {
        // dateAccident est null → IllegalArgumentException (le service convertit en 400).
        assertThatThrownBy(() -> AtFedrisDeclarationCalculator.compute(req(
                null,
                null,
                LocalDate.of(2026, 5, 15),
                null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateAccident");
    }

    @Test
    void requestNull_leveeIllegalArgumentException() {
        assertThatThrownBy(() -> AtFedrisDeclarationCalculator.compute(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
