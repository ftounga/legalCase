package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-02 : tests unitaires du {@link RccBeLongueCarriereValidator}.
 *
 * <p>Vérifie : conditions cumulatives (âge ≥ 59 / carrière ≥ 40 /
 * licenciement effectif), hiérarchie des raisons d'inéligibilité,
 * calcul indicatif de l'indemnité complémentaire (0,5 × diff),
 * snapshot des inputs, validation des bornes (négatifs, null), base
 * juridique stable.</p>
 */
class RccBeLongueCarriereValidatorTest {

    private static final LocalDate DATE_FIN = LocalDate.of(2027, 1, 31);
    private static final BigDecimal REM_REF = new BigDecimal("3200.00");
    private static final BigDecimal ALLOC_CHOMAGE = new BigDecimal("1800.00");

    private static RccBeLongueCarriereRequest req(
            Integer age,
            Integer carriere,
            boolean licenciement,
            BigDecimal rem,
            BigDecimal alloc) {
        return new RccBeLongueCarriereRequest(
                age, carriere, DATE_FIN, licenciement, rem, alloc);
    }

    private static RccBeLongueCarriereRequest reqBase() {
        return req(59, 40, true, null, null);
    }

    // ── Cas nominal : éligible ─────────────────────────────────────────────

    @Test
    void analyze_toutesConditionsRemplies_returnsEligible() {
        // 59 ans (= seuil), 40 ans carrière (= seuil), licencié
        RccBeLongueCarriereResult r =
                RccBeLongueCarriereValidator.analyze(reqBase());

        assertThat(r.verdict())
                .isEqualTo(RccBeLongueCarriereVerdict.ELIGIBLE_RCC_LONGUE_CARRIERE);
        assertThat(r.eligible()).isTrue();
        assertThat(r.conditionAgeRemplie()).isTrue();
        assertThat(r.conditionCarriereRemplie()).isTrue();
        assertThat(r.conditionLicenciementRemplie()).isTrue();
        assertThat(r.synthese()).contains("remplies").contains("ouvert");
    }

    @Test
    void analyze_ageStrictementAuDessusSeuil_returnsEligible() {
        RccBeLongueCarriereResult r =
                RccBeLongueCarriereValidator.analyze(
                        req(63, 42, true, null, null));

        assertThat(r.verdict())
                .isEqualTo(RccBeLongueCarriereVerdict.ELIGIBLE_RCC_LONGUE_CARRIERE);
        assertThat(r.eligible()).isTrue();
    }

    // ── Cas inéligible : âge insuffisant ───────────────────────────────────

    @Test
    void analyze_ageEnDessousSeuil_returnsIneligibleAge() {
        RccBeLongueCarriereResult r =
                RccBeLongueCarriereValidator.analyze(
                        req(58, 40, true, null, null));

        assertThat(r.verdict())
                .isEqualTo(RccBeLongueCarriereVerdict.INELIGIBLE_AGE_INSUFFISANT);
        assertThat(r.eligible()).isFalse();
        assertThat(r.conditionAgeRemplie()).isFalse();
        assertThat(r.conditionCarriereRemplie()).isTrue();
        assertThat(r.conditionLicenciementRemplie()).isTrue();
        assertThat(r.synthese()).contains("< 59").contains("carrière est remplie");
    }

    // ── Cas inéligible : carrière insuffisante ─────────────────────────────

    @Test
    void analyze_carriereEnDessousSeuil_returnsIneligibleCarriere() {
        RccBeLongueCarriereResult r =
                RccBeLongueCarriereValidator.analyze(
                        req(59, 39, true, null, null));

        assertThat(r.verdict())
                .isEqualTo(RccBeLongueCarriereVerdict.INELIGIBLE_CARRIERE_INSUFFISANTE);
        assertThat(r.eligible()).isFalse();
        assertThat(r.conditionAgeRemplie()).isTrue();
        assertThat(r.conditionCarriereRemplie()).isFalse();
        assertThat(r.conditionLicenciementRemplie()).isTrue();
        assertThat(r.synthese()).contains("< 40").contains("d'âge est remplie");
    }

    // ── Cas inéligible : démission ─────────────────────────────────────────

    @Test
    void analyze_demission_returnsIneligibleDemission() {
        RccBeLongueCarriereResult r =
                RccBeLongueCarriereValidator.analyze(
                        req(62, 42, false, null, null));

        assertThat(r.verdict())
                .isEqualTo(RccBeLongueCarriereVerdict.INELIGIBLE_DEMISSION);
        assertThat(r.eligible()).isFalse();
        assertThat(r.conditionLicenciementRemplie()).isFalse();
        assertThat(r.synthese()).contains("Démission")
                .contains("licenciement à l'initiative de l'employeur");
    }

    // ── Hiérarchie des raisons : démission prime sur âge ───────────────────

    @Test
    void analyze_demissionEtAgeBas_returnsDemission_priorite() {
        RccBeLongueCarriereResult r =
                RccBeLongueCarriereValidator.analyze(
                        req(50, 30, false, null, null));

        assertThat(r.verdict())
                .isEqualTo(RccBeLongueCarriereVerdict.INELIGIBLE_DEMISSION);
        // Les détails restent disponibles
        assertThat(r.conditionAgeRemplie()).isFalse();
        assertThat(r.conditionCarriereRemplie()).isFalse();
        assertThat(r.conditionLicenciementRemplie()).isFalse();
    }

    @Test
    void analyze_ageBasEtCarriereBasse_returnsAge_priorite() {
        // Licencié, mais 50 ans + 35 ans carrière : âge prioritaire sur carrière
        RccBeLongueCarriereResult r =
                RccBeLongueCarriereValidator.analyze(
                        req(50, 35, true, null, null));

        assertThat(r.verdict())
                .isEqualTo(RccBeLongueCarriereVerdict.INELIGIBLE_AGE_INSUFFISANT);
        assertThat(r.synthese())
                .contains("carrière n'est pas non plus remplie");
    }

    // ── Indemnité complémentaire indicative ─────────────────────────────────

    @Test
    void analyze_eligible_avecMontants_calculeIndemniteComplementaire() {
        // 0,5 × (3200 - 1800) = 700
        RccBeLongueCarriereResult r =
                RccBeLongueCarriereValidator.analyze(
                        req(60, 41, true, REM_REF, ALLOC_CHOMAGE));

        assertThat(r.verdict())
                .isEqualTo(RccBeLongueCarriereVerdict.ELIGIBLE_RCC_LONGUE_CARRIERE);
        assertThat(r.indemniteComplementaireMensuelle())
                .isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(r.avertissement())
                .isNotNull()
                .contains("indicatif")
                .contains("ONSS");
    }

    @Test
    void analyze_eligible_sansMontants_indemniteNullEtPasAvertissement() {
        RccBeLongueCarriereResult r =
                RccBeLongueCarriereValidator.analyze(reqBase());

        assertThat(r.eligible()).isTrue();
        assertThat(r.indemniteComplementaireMensuelle()).isNull();
        assertThat(r.avertissement()).isNull();
    }

    @Test
    void analyze_ineligible_avecMontants_pasDeCalculIndemnite() {
        // Démission : pas d'indemnité même si les montants sont fournis
        RccBeLongueCarriereResult r =
                RccBeLongueCarriereValidator.analyze(
                        req(60, 41, false, REM_REF, ALLOC_CHOMAGE));

        assertThat(r.eligible()).isFalse();
        assertThat(r.indemniteComplementaireMensuelle()).isNull();
        assertThat(r.avertissement()).isNull();
    }

    @Test
    void analyze_eligible_unSeulMontant_pasDeCalcul() {
        // Si l'avocat ne renseigne qu'un seul des deux : pas de calcul
        RccBeLongueCarriereResult r =
                RccBeLongueCarriereValidator.analyze(
                        req(60, 41, true, REM_REF, null));

        assertThat(r.indemniteComplementaireMensuelle()).isNull();
        assertThat(r.avertissement()).isNull();
    }

    @Test
    void analyze_eligible_allocationSuperieureAReference_indemniteZero() {
        // Cas plancher : si allocation ≥ référence → indemnité = 0
        RccBeLongueCarriereResult r =
                RccBeLongueCarriereValidator.analyze(
                        req(60, 41, true,
                                new BigDecimal("1500.00"),
                                new BigDecimal("1500.00")));

        assertThat(r.eligible()).isTrue();
        assertThat(r.indemniteComplementaireMensuelle())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void analyze_eligible_allocationStrictementSuperieure_indemniteZero() {
        RccBeLongueCarriereResult r =
                RccBeLongueCarriereValidator.analyze(
                        req(60, 41, true,
                                new BigDecimal("1500.00"),
                                new BigDecimal("1700.00")));

        assertThat(r.indemniteComplementaireMensuelle())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void analyze_eligible_indemniteArrondieAuCent() {
        // 0,5 × (3000 - 1845.55) = 577.225 → 577.23 (HALF_UP)
        RccBeLongueCarriereResult r =
                RccBeLongueCarriereValidator.analyze(
                        req(60, 41, true,
                                new BigDecimal("3000.00"),
                                new BigDecimal("1845.55")));

        assertThat(r.indemniteComplementaireMensuelle())
                .isEqualByComparingTo(new BigDecimal("577.23"));
    }

    // ── Snapshot complet des inputs ─────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        RccBeLongueCarriereResult r =
                RccBeLongueCarriereValidator.analyze(
                        req(62, 41, true, REM_REF, ALLOC_CHOMAGE));

        assertThat(r.ageFinContrat()).isEqualTo(62);
        assertThat(r.anneesCarriereTotale()).isEqualTo(41);
        assertThat(r.dateFinContrat()).isEqualTo(DATE_FIN);
        assertThat(r.licenciementEffectif()).isTrue();
        assertThat(r.remunerationNetteMensuelleReference())
                .isEqualByComparingTo(REM_REF);
        assertThat(r.allocationChomageMensuelleEstimee())
                .isEqualByComparingTo(ALLOC_CHOMAGE);
    }

    // ── Base juridique ─────────────────────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceLoi2013_CCT17_AR2007() {
        RccBeLongueCarriereResult r =
                RccBeLongueCarriereValidator.analyze(reqBase());

        assertThat(r.baseJuridique())
                .contains("26/12/2013")
                .contains("CCT n° 17")
                .contains("19/12/1974")
                .contains("AR du 03/05/2007")
                .contains("art. 3");
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() ->
                RccBeLongueCarriereValidator.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_ageNull_throws() {
        RccBeLongueCarriereRequest bad =
                new RccBeLongueCarriereRequest(
                        null, 40, DATE_FIN, true, null, null);
        assertThatThrownBy(() ->
                RccBeLongueCarriereValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ageFinContrat");
    }

    @Test
    void analyze_ageNegatif_throws() {
        assertThatThrownBy(() ->
                RccBeLongueCarriereValidator.analyze(
                        req(-1, 40, true, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ageFinContrat");
    }

    @Test
    void analyze_carriereNull_throws() {
        RccBeLongueCarriereRequest bad =
                new RccBeLongueCarriereRequest(
                        59, null, DATE_FIN, true, null, null);
        assertThatThrownBy(() ->
                RccBeLongueCarriereValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anneesCarriereTotale");
    }

    @Test
    void analyze_carriereNegative_throws() {
        assertThatThrownBy(() ->
                RccBeLongueCarriereValidator.analyze(
                        req(59, -1, true, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anneesCarriereTotale");
    }

    @Test
    void analyze_dateFinContratNull_throws() {
        RccBeLongueCarriereRequest bad =
                new RccBeLongueCarriereRequest(
                        59, 40, null, true, null, null);
        assertThatThrownBy(() ->
                RccBeLongueCarriereValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateFinContrat");
    }

    @Test
    void analyze_licenciementEffectifNull_throws() {
        RccBeLongueCarriereRequest bad =
                new RccBeLongueCarriereRequest(
                        59, 40, DATE_FIN, null, null, null);
        assertThatThrownBy(() ->
                RccBeLongueCarriereValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("licenciementEffectif");
    }

    @Test
    void analyze_remunerationNegative_throws() {
        assertThatThrownBy(() ->
                RccBeLongueCarriereValidator.analyze(
                        req(59, 40, true,
                                new BigDecimal("-100"), ALLOC_CHOMAGE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remunerationNetteMensuelleReference");
    }

    @Test
    void analyze_allocationNegative_throws() {
        assertThatThrownBy(() ->
                RccBeLongueCarriereValidator.analyze(
                        req(59, 40, true, REM_REF,
                                new BigDecimal("-50"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allocationChomageMensuelleEstimee");
    }
}
