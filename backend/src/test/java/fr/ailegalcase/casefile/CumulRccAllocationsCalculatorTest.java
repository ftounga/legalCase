package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-04 : tests unitaires du {@link CumulRccAllocationsCalculator}.
 *
 * <p>Couvre : plafond du cumul (CCT n° 17), régime de disponibilité
 * ajustée selon l'âge / la carrière (AR 03/05/2007 art. 22),
 * bascule pension légale, compatibilité activité accessoire (AR
 * 25/11/1991), hiérarchie des verdicts (pension &gt; activité non
 * déclarée &gt; plafond &gt; conforme), validation des inputs et base
 * juridique stable.</p>
 */
class CumulRccAllocationsCalculatorTest {

    private static final LocalDate NAISSANCE_60 = LocalDate.of(1965, 1, 15);
    private static final LocalDate DEBUT_RCC_2026 = LocalDate.of(2026, 1, 15);
    // Ces 2 dates donnent un âge exact de 61 ans → carrière 35 ans → DISPONIBILITE_ADAPTEE

    private static CumulRccAllocationsRequest req(
            LocalDate naissance,
            LocalDate debutRcc,
            BigDecimal alloc,
            BigDecimal indemnite,
            BigDecimal remRef,
            int carriere,
            CumulRccAllocationsActiviteAutorisee activite,
            Integer ageLegal) {
        return new CumulRccAllocationsRequest(
                naissance, debutRcc, alloc, indemnite, remRef,
                carriere, activite, ageLegal);
    }

    private static CumulRccAllocationsRequest reqBase() {
        // 61 ans, RCC débutant en 2026, 1800 ONEM + 700 indemnité = 2500 ≤ 3200 réf
        // carrière 35 ans (< 42), aucune activité, âge légal par défaut (65)
        return req(NAISSANCE_60, DEBUT_RCC_2026,
                new BigDecimal("1800.00"),
                new BigDecimal("700.00"),
                new BigDecimal("3200.00"),
                35,
                CumulRccAllocationsActiviteAutorisee.AUCUNE,
                null);
    }

    // ── Cas nominal : conforme ─────────────────────────────────────────────

    @Test
    void analyze_cumulSousLePlafond_returnsConforme() {
        CumulRccAllocationsResult r =
                CumulRccAllocationsCalculator.analyze(reqBase());

        assertThat(r.verdict()).isEqualTo(CumulRccAllocationsVerdict.CUMUL_CONFORME);
        assertThat(r.conforme()).isTrue();
        assertThat(r.plafondRespecte()).isTrue();
        assertThat(r.cumulMensuelTotal())
                .isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(r.montantReductionIndemnite()).isNull();
        assertThat(r.ageADateDebutRcc()).isEqualTo(61);
        assertThat(r.ageLegalPensionAtteint()).isFalse();
        assertThat(r.synthese()).contains("conforme").contains("2500.00");
    }

    @Test
    void analyze_cumulEgalAuPlafond_returnsConforme() {
        // Cumul exactement égal à la référence : la CCT 17 autorise le ≤.
        CumulRccAllocationsRequest req = req(
                NAISSANCE_60, DEBUT_RCC_2026,
                new BigDecimal("1800.00"),
                new BigDecimal("1400.00"),
                new BigDecimal("3200.00"),
                35,
                CumulRccAllocationsActiviteAutorisee.AUCUNE,
                null);

        CumulRccAllocationsResult r =
                CumulRccAllocationsCalculator.analyze(req);

        assertThat(r.verdict()).isEqualTo(CumulRccAllocationsVerdict.CUMUL_CONFORME);
        assertThat(r.plafondRespecte()).isTrue();
    }

    // ── Cas plafond dépassé ────────────────────────────────────────────────

    @Test
    void analyze_cumulAuDessusPlafond_returnsDepasseEtCalculeReduction() {
        CumulRccAllocationsRequest req = req(
                NAISSANCE_60, DEBUT_RCC_2026,
                new BigDecimal("1800.00"),
                new BigDecimal("1800.00"), // total 3600 > 3200
                new BigDecimal("3200.00"),
                35,
                CumulRccAllocationsActiviteAutorisee.AUCUNE,
                null);

        CumulRccAllocationsResult r =
                CumulRccAllocationsCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(CumulRccAllocationsVerdict.CUMUL_PLAFOND_DEPASSE);
        assertThat(r.conforme()).isFalse();
        assertThat(r.plafondRespecte()).isFalse();
        assertThat(r.cumulMensuelTotal())
                .isEqualByComparingTo(new BigDecimal("3600.00"));
        assertThat(r.montantReductionIndemnite())
                .isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(r.synthese())
                .contains("Réduire").contains("400.00").contains("CCT n° 17");
    }

    // ── Bascule pension prioritaire ─────────────────────────────────────────

    @Test
    void analyze_ageLegalAtteint_returnsBasculePension_priorite() {
        // Né en 1960, RCC en 2026 → 65 ans (≥ 65 défaut) → bascule pension
        CumulRccAllocationsRequest req = req(
                LocalDate.of(1960, 1, 15),
                LocalDate.of(2025, 6, 1),
                new BigDecimal("1800.00"),
                new BigDecimal("1800.00"), // dépassement plafond
                new BigDecimal("3200.00"),
                35,
                CumulRccAllocationsActiviteAutorisee.ACTIVITE_NON_DECLAREE, // activité non déclarée
                null);

        CumulRccAllocationsResult r =
                CumulRccAllocationsCalculator.analyze(req);

        // Pension > activité non déclarée > plafond
        assertThat(r.verdict())
                .isEqualTo(CumulRccAllocationsVerdict.BASCULE_PENSION_LEGALE);
        assertThat(r.ageLegalPensionAtteint()).isTrue();
        assertThat(r.synthese())
                .contains("Âge légal pension atteint")
                .contains("65");
    }

    @Test
    void analyze_ageLegalConfigurable66_neBasculePas() {
        // 65 ans à la date RCC mais âge légal porté à 66 (réforme 2027)
        CumulRccAllocationsRequest req = req(
                LocalDate.of(1960, 1, 15),
                LocalDate.of(2025, 6, 1),
                new BigDecimal("1800.00"),
                new BigDecimal("700.00"),
                new BigDecimal("3200.00"),
                35,
                CumulRccAllocationsActiviteAutorisee.AUCUNE,
                66);

        CumulRccAllocationsResult r =
                CumulRccAllocationsCalculator.analyze(req);

        assertThat(r.verdict()).isEqualTo(CumulRccAllocationsVerdict.CUMUL_CONFORME);
        assertThat(r.ageLegalPensionAtteint()).isFalse();
    }

    // ── Activité non déclarée prioritaire sur plafond ──────────────────────

    @Test
    void analyze_activiteNonDeclaree_returnsIncompatible_meme_sous_plafond() {
        CumulRccAllocationsRequest req = req(
                NAISSANCE_60, DEBUT_RCC_2026,
                new BigDecimal("1800.00"),
                new BigDecimal("700.00"),
                new BigDecimal("3200.00"),
                35,
                CumulRccAllocationsActiviteAutorisee.ACTIVITE_NON_DECLAREE,
                null);

        CumulRccAllocationsResult r =
                CumulRccAllocationsCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(CumulRccAllocationsVerdict.INCOMPATIBLE_ACTIVITE_NON_AUTORISEE);
        assertThat(r.conforme()).isFalse();
        assertThat(r.synthese())
                .contains("non déclarée").contains("AR 25/11/1991");
    }

    @Test
    void analyze_activiteNonDeclareeEtPlafondDepasse_returnsActivite() {
        // Activité non déclarée prime sur le plafond
        CumulRccAllocationsRequest req = req(
                NAISSANCE_60, DEBUT_RCC_2026,
                new BigDecimal("1800.00"),
                new BigDecimal("1800.00"),
                new BigDecimal("3000.00"),
                35,
                CumulRccAllocationsActiviteAutorisee.ACTIVITE_NON_DECLAREE,
                null);

        CumulRccAllocationsResult r =
                CumulRccAllocationsCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(CumulRccAllocationsVerdict.INCOMPATIBLE_ACTIVITE_NON_AUTORISEE);
    }

    // ── Activités autorisées ne sortent pas du régime ──────────────────────

    @Test
    void analyze_activiteIndependantAccessoire_resteConforme() {
        CumulRccAllocationsRequest req = req(
                NAISSANCE_60, DEBUT_RCC_2026,
                new BigDecimal("1800.00"),
                new BigDecimal("700.00"),
                new BigDecimal("3200.00"),
                35,
                CumulRccAllocationsActiviteAutorisee.ACTIVITE_INDEPENDANT_ACCESSOIRE,
                null);

        CumulRccAllocationsResult r =
                CumulRccAllocationsCalculator.analyze(req);

        assertThat(r.verdict()).isEqualTo(CumulRccAllocationsVerdict.CUMUL_CONFORME);
    }

    @Test
    void analyze_activiteBenevoleDeclaree_resteConforme() {
        CumulRccAllocationsRequest req = req(
                NAISSANCE_60, DEBUT_RCC_2026,
                new BigDecimal("1800.00"),
                new BigDecimal("700.00"),
                new BigDecimal("3200.00"),
                35,
                CumulRccAllocationsActiviteAutorisee.ACTIVITE_BENEVOLE_DECLAREE,
                null);

        CumulRccAllocationsResult r =
                CumulRccAllocationsCalculator.analyze(req);

        assertThat(r.verdict()).isEqualTo(CumulRccAllocationsVerdict.CUMUL_CONFORME);
    }

    // ── Régime de disponibilité ─────────────────────────────────────────────

    @Test
    void analyze_61ansEtCarriereCourte_disponibiliteAdaptee() {
        CumulRccAllocationsResult r =
                CumulRccAllocationsCalculator.analyze(reqBase());

        assertThat(r.regimeDisponibilite())
                .isEqualTo(CumulRccAllocationsDisponibiliteRegime.DISPONIBILITE_ADAPTEE);
        assertThat(r.synthese()).contains("disponibilité adaptée");
    }

    @Test
    void analyze_62ans_disponibilitePassive() {
        CumulRccAllocationsRequest req = req(
                LocalDate.of(1963, 1, 15),
                LocalDate.of(2025, 6, 1), // ~62 ans
                new BigDecimal("1800.00"),
                new BigDecimal("700.00"),
                new BigDecimal("3200.00"),
                35,
                CumulRccAllocationsActiviteAutorisee.AUCUNE,
                null);

        CumulRccAllocationsResult r =
                CumulRccAllocationsCalculator.analyze(req);

        assertThat(r.regimeDisponibilite())
                .isEqualTo(CumulRccAllocationsDisponibiliteRegime.DISPONIBILITE_PASSIVE);
    }

    @Test
    void analyze_carriere42ansMemeJeune_disponibilitePassive() {
        // 60 ans mais 42 ans de carrière (long carrière) → passive
        CumulRccAllocationsRequest req = req(
                NAISSANCE_60, DEBUT_RCC_2026,
                new BigDecimal("1800.00"),
                new BigDecimal("700.00"),
                new BigDecimal("3200.00"),
                42,
                CumulRccAllocationsActiviteAutorisee.AUCUNE,
                null);

        CumulRccAllocationsResult r =
                CumulRccAllocationsCalculator.analyze(req);

        assertThat(r.regimeDisponibilite())
                .isEqualTo(CumulRccAllocationsDisponibiliteRegime.DISPONIBILITE_PASSIVE);
    }

    @Test
    void analyze_64ansAgeLegal65_dispenseRecherche() {
        // 64 ans (= 65 - 1) → DISPENSE_RECHERCHE_EMPLOI (proche pension)
        CumulRccAllocationsRequest req = req(
                LocalDate.of(1961, 1, 15),
                LocalDate.of(2025, 6, 1), // ~64 ans
                new BigDecimal("1800.00"),
                new BigDecimal("700.00"),
                new BigDecimal("3200.00"),
                35,
                CumulRccAllocationsActiviteAutorisee.AUCUNE,
                null);

        CumulRccAllocationsResult r =
                CumulRccAllocationsCalculator.analyze(req);

        assertThat(r.regimeDisponibilite())
                .isEqualTo(CumulRccAllocationsDisponibiliteRegime.DISPENSE_RECHERCHE_EMPLOI);
    }

    // ── Snapshot des inputs ─────────────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        CumulRccAllocationsResult r =
                CumulRccAllocationsCalculator.analyze(reqBase());

        assertThat(r.dateNaissance()).isEqualTo(NAISSANCE_60);
        assertThat(r.dateDebutRcc()).isEqualTo(DEBUT_RCC_2026);
        assertThat(r.allocationChomageMensuelle())
                .isEqualByComparingTo(new BigDecimal("1800.00"));
        assertThat(r.indemniteComplementaireMensuelle())
                .isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(r.remunerationNetteReference())
                .isEqualByComparingTo(new BigDecimal("3200.00"));
        assertThat(r.anneesCarriereTotale()).isEqualTo(35);
        assertThat(r.activiteComplementaire())
                .isEqualTo(CumulRccAllocationsActiviteAutorisee.AUCUNE);
        assertThat(r.ageLegalPension()).isNull();
    }

    // ── Base juridique et avertissement ─────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceCCT17_AR1991_AR2007() {
        CumulRccAllocationsResult r =
                CumulRccAllocationsCalculator.analyze(reqBase());

        assertThat(r.baseJuridique())
                .contains("CCT n° 17")
                .contains("19/12/1974")
                .contains("AR du 25/11/1991")
                .contains("AR du 03/05/2007");
    }

    @Test
    void analyze_avertissementSysematiquePresent() {
        CumulRccAllocationsResult r =
                CumulRccAllocationsCalculator.analyze(reqBase());

        assertThat(r.avertissement())
                .isNotNull()
                .contains("indicatif")
                .contains("ONSS");
    }

    // ── Arrondi cumul ───────────────────────────────────────────────────────

    @Test
    void analyze_cumul_arrondiAuCent_HALF_UP() {
        CumulRccAllocationsRequest req = req(
                NAISSANCE_60, DEBUT_RCC_2026,
                new BigDecimal("1800.555"),
                new BigDecimal("700.444"),
                new BigDecimal("3200.00"),
                35,
                CumulRccAllocationsActiviteAutorisee.AUCUNE,
                null);

        CumulRccAllocationsResult r =
                CumulRccAllocationsCalculator.analyze(req);

        // 1800.555 + 700.444 = 2500.999 → 2501.00 HALF_UP
        assertThat(r.cumulMensuelTotal())
                .isEqualByComparingTo(new BigDecimal("2501.00"));
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() ->
                CumulRccAllocationsCalculator.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_dateNaissanceNull_throws() {
        CumulRccAllocationsRequest bad = req(
                null, DEBUT_RCC_2026,
                new BigDecimal("1800"), new BigDecimal("700"),
                new BigDecimal("3200"), 35,
                CumulRccAllocationsActiviteAutorisee.AUCUNE, null);
        assertThatThrownBy(() ->
                CumulRccAllocationsCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNaissance");
    }

    @Test
    void analyze_dateDebutRccAvantNaissance_throws() {
        CumulRccAllocationsRequest bad = req(
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2024, 1, 15),
                new BigDecimal("1800"), new BigDecimal("700"),
                new BigDecimal("3200"), 35,
                CumulRccAllocationsActiviteAutorisee.AUCUNE, null);
        assertThatThrownBy(() ->
                CumulRccAllocationsCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDebutRcc");
    }

    @Test
    void analyze_allocationNull_throws() {
        CumulRccAllocationsRequest bad = req(
                NAISSANCE_60, DEBUT_RCC_2026,
                null, new BigDecimal("700"),
                new BigDecimal("3200"), 35,
                CumulRccAllocationsActiviteAutorisee.AUCUNE, null);
        assertThatThrownBy(() ->
                CumulRccAllocationsCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allocationChomageMensuelle");
    }

    @Test
    void analyze_allocationNegative_throws() {
        CumulRccAllocationsRequest bad = req(
                NAISSANCE_60, DEBUT_RCC_2026,
                new BigDecimal("-100"), new BigDecimal("700"),
                new BigDecimal("3200"), 35,
                CumulRccAllocationsActiviteAutorisee.AUCUNE, null);
        assertThatThrownBy(() ->
                CumulRccAllocationsCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allocationChomageMensuelle");
    }

    @Test
    void analyze_indemniteNegative_throws() {
        CumulRccAllocationsRequest bad = req(
                NAISSANCE_60, DEBUT_RCC_2026,
                new BigDecimal("1800"), new BigDecimal("-50"),
                new BigDecimal("3200"), 35,
                CumulRccAllocationsActiviteAutorisee.AUCUNE, null);
        assertThatThrownBy(() ->
                CumulRccAllocationsCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("indemniteComplementaireMensuelle");
    }

    @Test
    void analyze_remunerationReferenceNull_throws() {
        CumulRccAllocationsRequest bad = req(
                NAISSANCE_60, DEBUT_RCC_2026,
                new BigDecimal("1800"), new BigDecimal("700"),
                null, 35,
                CumulRccAllocationsActiviteAutorisee.AUCUNE, null);
        assertThatThrownBy(() ->
                CumulRccAllocationsCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remunerationNetteReference");
    }

    @Test
    void analyze_carriereNegative_throws() {
        CumulRccAllocationsRequest bad = req(
                NAISSANCE_60, DEBUT_RCC_2026,
                new BigDecimal("1800"), new BigDecimal("700"),
                new BigDecimal("3200"), -1,
                CumulRccAllocationsActiviteAutorisee.AUCUNE, null);
        assertThatThrownBy(() ->
                CumulRccAllocationsCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anneesCarriereTotale");
    }

    @Test
    void analyze_activiteNull_throws() {
        CumulRccAllocationsRequest bad = req(
                NAISSANCE_60, DEBUT_RCC_2026,
                new BigDecimal("1800"), new BigDecimal("700"),
                new BigDecimal("3200"), 35,
                null, null);
        assertThatThrownBy(() ->
                CumulRccAllocationsCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("activiteComplementaire");
    }

    @Test
    void analyze_ageLegalPensionHorsBornes_throws() {
        CumulRccAllocationsRequest bad = req(
                NAISSANCE_60, DEBUT_RCC_2026,
                new BigDecimal("1800"), new BigDecimal("700"),
                new BigDecimal("3200"), 35,
                CumulRccAllocationsActiviteAutorisee.AUCUNE,
                75); // > 70
        assertThatThrownBy(() ->
                CumulRccAllocationsCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ageLegalPension");
    }
}
