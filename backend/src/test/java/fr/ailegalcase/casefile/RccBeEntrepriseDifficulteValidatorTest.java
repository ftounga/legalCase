package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-03 : tests unitaires du
 * {@link RccBeEntrepriseDifficulteValidator}.
 *
 * <p>Vérifie : conditions cumulatives (reconnaissance / âge ≥ seuil plan /
 * carrière ≥ 10 / ancienneté ≥ 5 / licenciement effectif), hiérarchie des
 * raisons d'inéligibilité (démission > reconnaissance > âge > carrière >
 * ancienneté), calcul indicatif de l'indemnité complémentaire (0,5 × diff),
 * snapshot des inputs, validation des bornes (négatifs, null), base
 * juridique stable.</p>
 */
class RccBeEntrepriseDifficulteValidatorTest {

    private static final LocalDate DATE_FIN = LocalDate.of(2027, 3, 31);
    private static final BigDecimal REM_REF = new BigDecimal("3200.00");
    private static final BigDecimal ALLOC_CHOMAGE = new BigDecimal("1800.00");

    private static RccBeEntrepriseDifficulteRequest req(
            RccBeEntrepriseDifficulteTypeEnum typeRec,
            Integer ageReduit,
            Integer ageFin,
            Integer carriere,
            Integer anciennete,
            boolean licenciement,
            BigDecimal rem,
            BigDecimal alloc) {
        return new RccBeEntrepriseDifficulteRequest(
                typeRec, ageReduit, ageFin, carriere, anciennete,
                DATE_FIN, licenciement, rem, alloc);
    }

    /** Cas de base éligible : reconnaissance EN_DIFFICULTE, plan 55, âge 55,
     *  carrière 10, ancienneté 5, licencié. */
    private static RccBeEntrepriseDifficulteRequest reqBase() {
        return req(RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                55, 55, 10, 5, true, null, null);
    }

    // ── Cas nominal : éligible ─────────────────────────────────────────────

    @Test
    void analyze_toutesConditionsRemplies_returnsEligible() {
        RccBeEntrepriseDifficulteResult r =
                RccBeEntrepriseDifficulteValidator.analyze(reqBase());

        assertThat(r.verdict())
                .isEqualTo(RccBeEntrepriseDifficulteVerdict
                        .ELIGIBLE_RCC_ENTREPRISE_DIFFICULTE);
        assertThat(r.eligible()).isTrue();
        assertThat(r.conditionReconnaissanceRemplie()).isTrue();
        assertThat(r.conditionAgeRemplie()).isTrue();
        assertThat(r.conditionCarriereRemplie()).isTrue();
        assertThat(r.conditionAncienneteRemplie()).isTrue();
        assertThat(r.conditionLicenciementRemplie()).isTrue();
        assertThat(r.synthese()).contains("remplies").contains("ouvert");
    }

    @Test
    void analyze_eligible_restructuration_returnsEligible() {
        RccBeEntrepriseDifficulteResult r =
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_RESTRUCTURATION,
                                55, 58, 15, 8, true, null, null));

        assertThat(r.eligible()).isTrue();
        assertThat(r.synthese()).contains("restructuration");
    }

    @Test
    void analyze_eligible_ageStrictementAuDessusSeuilPlan() {
        RccBeEntrepriseDifficulteResult r =
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                                55, 62, 30, 10, true, null, null));

        assertThat(r.eligible()).isTrue();
    }

    @Test
    void analyze_eligible_ageReduitPlanPersonnalise50() {
        // Le plan peut fixer un âge réduit à 50 ans dans certains cas
        // exceptionnels (restructuration grave). Si l'avocat saisit 50,
        // l'outil respecte le plan.
        RccBeEntrepriseDifficulteResult r =
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_RESTRUCTURATION,
                                50, 51, 15, 8, true, null, null));

        assertThat(r.eligible()).isTrue();
        assertThat(r.synthese()).contains("50");
    }

    // ── Cas inéligible : démission ─────────────────────────────────────────

    @Test
    void analyze_demission_returnsIneligibleDemission() {
        RccBeEntrepriseDifficulteResult r =
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                                55, 56, 12, 6, false, null, null));

        assertThat(r.verdict())
                .isEqualTo(RccBeEntrepriseDifficulteVerdict.INELIGIBLE_DEMISSION);
        assertThat(r.eligible()).isFalse();
        assertThat(r.conditionLicenciementRemplie()).isFalse();
        assertThat(r.synthese())
                .contains("Démission")
                .contains("licenciement");
    }

    // ── Cas inéligible : reconnaissance absente ────────────────────────────

    @Test
    void analyze_nonReconnue_returnsIneligibleReconnaissanceAbsente() {
        RccBeEntrepriseDifficulteResult r =
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.NON_RECONNUE,
                                55, 60, 30, 15, true, null, null));

        assertThat(r.verdict())
                .isEqualTo(RccBeEntrepriseDifficulteVerdict
                        .INELIGIBLE_RECONNAISSANCE_ABSENTE);
        assertThat(r.eligible()).isFalse();
        assertThat(r.conditionReconnaissanceRemplie()).isFalse();
        // Même si l'âge serait OK sur le papier, on ne valide pas tant que
        // pas de reconnaissance (court-circuit)
        assertThat(r.conditionAgeRemplie()).isFalse();
        assertThat(r.synthese()).contains("non reconnue")
                .contains("Moniteur belge");
    }

    // ── Cas inéligible : âge insuffisant ───────────────────────────────────

    @Test
    void analyze_ageEnDessousSeuilPlan_returnsIneligibleAge() {
        RccBeEntrepriseDifficulteResult r =
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                                55, 54, 12, 6, true, null, null));

        assertThat(r.verdict())
                .isEqualTo(RccBeEntrepriseDifficulteVerdict
                        .INELIGIBLE_AGE_INSUFFISANT);
        assertThat(r.eligible()).isFalse();
        assertThat(r.conditionAgeRemplie()).isFalse();
        assertThat(r.conditionCarriereRemplie()).isTrue();
        assertThat(r.conditionAncienneteRemplie()).isTrue();
        assertThat(r.synthese()).contains("< 55").contains("carrière")
                .contains("ancienneté");
    }

    // ── Cas inéligible : carrière insuffisante ─────────────────────────────

    @Test
    void analyze_carriereEnDessousSeuil_returnsIneligibleCarriere() {
        RccBeEntrepriseDifficulteResult r =
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                                55, 56, 9, 5, true, null, null));

        assertThat(r.verdict())
                .isEqualTo(RccBeEntrepriseDifficulteVerdict
                        .INELIGIBLE_CARRIERE_INSUFFISANTE);
        assertThat(r.eligible()).isFalse();
        assertThat(r.conditionAgeRemplie()).isTrue();
        assertThat(r.conditionCarriereRemplie()).isFalse();
        assertThat(r.synthese()).contains("< 10");
    }

    // ── Cas inéligible : ancienneté insuffisante ───────────────────────────

    @Test
    void analyze_ancienneteEnDessousSeuil_returnsIneligibleAnciennete() {
        RccBeEntrepriseDifficulteResult r =
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                                55, 56, 12, 4, true, null, null));

        assertThat(r.verdict())
                .isEqualTo(RccBeEntrepriseDifficulteVerdict
                        .INELIGIBLE_ANCIENNETE_INSUFFISANTE);
        assertThat(r.eligible()).isFalse();
        assertThat(r.conditionAncienneteRemplie()).isFalse();
        assertThat(r.synthese()).contains("< 5");
    }

    // ── Hiérarchie des raisons ─────────────────────────────────────────────

    @Test
    void analyze_demissionEtPasDeReconnaissance_returnsDemissionPriorite() {
        RccBeEntrepriseDifficulteResult r =
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.NON_RECONNUE,
                                55, 40, 3, 1, false, null, null));

        assertThat(r.verdict())
                .isEqualTo(RccBeEntrepriseDifficulteVerdict.INELIGIBLE_DEMISSION);
        // Les détails restent disponibles
        assertThat(r.conditionReconnaissanceRemplie()).isFalse();
        assertThat(r.conditionAgeRemplie()).isFalse();
        assertThat(r.conditionCarriereRemplie()).isFalse();
        assertThat(r.conditionAncienneteRemplie()).isFalse();
        assertThat(r.conditionLicenciementRemplie()).isFalse();
    }

    @Test
    void analyze_pasReconnaissanceEtAgeBas_returnsReconnaissancePriorite() {
        RccBeEntrepriseDifficulteResult r =
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.NON_RECONNUE,
                                55, 40, 3, 1, true, null, null));

        assertThat(r.verdict())
                .isEqualTo(RccBeEntrepriseDifficulteVerdict
                        .INELIGIBLE_RECONNAISSANCE_ABSENTE);
    }

    @Test
    void analyze_ageEtCarriereBas_returnsAgePriorite() {
        RccBeEntrepriseDifficulteResult r =
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                                55, 50, 8, 5, true, null, null));

        assertThat(r.verdict())
                .isEqualTo(RccBeEntrepriseDifficulteVerdict
                        .INELIGIBLE_AGE_INSUFFISANT);
        assertThat(r.synthese())
                .contains("D'autres conditions ne sont pas non plus remplies");
    }

    @Test
    void analyze_carriereEtAncienneteBas_returnsCarrierePriorite() {
        // Note : ancienneté ≤ carrière, donc on garde des valeurs cohérentes
        RccBeEntrepriseDifficulteResult r =
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                                55, 56, 8, 3, true, null, null));

        assertThat(r.verdict())
                .isEqualTo(RccBeEntrepriseDifficulteVerdict
                        .INELIGIBLE_CARRIERE_INSUFFISANTE);
        assertThat(r.synthese())
                .contains("D'autres conditions ne sont pas non plus remplies");
    }

    // ── Indemnité complémentaire indicative ─────────────────────────────────

    @Test
    void analyze_eligible_avecMontants_calculeIndemniteComplementaire() {
        // 0,5 × (3200 - 1800) = 700
        RccBeEntrepriseDifficulteResult r =
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                                55, 57, 15, 8, true, REM_REF, ALLOC_CHOMAGE));

        assertThat(r.eligible()).isTrue();
        assertThat(r.indemniteComplementaireMensuelle())
                .isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(r.avertissement())
                .isNotNull()
                .contains("indicatif")
                .contains("ONSS");
    }

    @Test
    void analyze_eligible_sansMontants_indemniteNullEtPasAvertissement() {
        RccBeEntrepriseDifficulteResult r =
                RccBeEntrepriseDifficulteValidator.analyze(reqBase());

        assertThat(r.eligible()).isTrue();
        assertThat(r.indemniteComplementaireMensuelle()).isNull();
        assertThat(r.avertissement()).isNull();
    }

    @Test
    void analyze_ineligible_avecMontants_pasDeCalculIndemnite() {
        // Démission : pas d'indemnité même si les montants sont fournis
        RccBeEntrepriseDifficulteResult r =
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                                55, 57, 15, 8, false, REM_REF, ALLOC_CHOMAGE));

        assertThat(r.eligible()).isFalse();
        assertThat(r.indemniteComplementaireMensuelle()).isNull();
        assertThat(r.avertissement()).isNull();
    }

    @Test
    void analyze_eligible_unSeulMontant_pasDeCalcul() {
        RccBeEntrepriseDifficulteResult r =
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                                55, 57, 15, 8, true, REM_REF, null));

        assertThat(r.indemniteComplementaireMensuelle()).isNull();
        assertThat(r.avertissement()).isNull();
    }

    @Test
    void analyze_eligible_allocationSuperieureAReference_indemniteZero() {
        RccBeEntrepriseDifficulteResult r =
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                                55, 57, 15, 8, true,
                                new BigDecimal("1500.00"),
                                new BigDecimal("1500.00")));

        assertThat(r.eligible()).isTrue();
        assertThat(r.indemniteComplementaireMensuelle())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void analyze_eligible_allocationStrictementSuperieure_indemniteZero() {
        RccBeEntrepriseDifficulteResult r =
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                                55, 57, 15, 8, true,
                                new BigDecimal("1500.00"),
                                new BigDecimal("1700.00")));

        assertThat(r.indemniteComplementaireMensuelle())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void analyze_eligible_indemniteArrondieAuCent() {
        // 0,5 × (3000 - 1845.55) = 577.225 → 577.23 (HALF_UP)
        RccBeEntrepriseDifficulteResult r =
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                                55, 57, 15, 8, true,
                                new BigDecimal("3000.00"),
                                new BigDecimal("1845.55")));

        assertThat(r.indemniteComplementaireMensuelle())
                .isEqualByComparingTo(new BigDecimal("577.23"));
    }

    // ── Snapshot complet des inputs ─────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        RccBeEntrepriseDifficulteResult r =
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_RESTRUCTURATION,
                                55, 60, 18, 7, true, REM_REF, ALLOC_CHOMAGE));

        assertThat(r.typeReconnaissance())
                .isEqualTo(RccBeEntrepriseDifficulteTypeEnum.EN_RESTRUCTURATION);
        assertThat(r.ageReduitPlan()).isEqualTo(55);
        assertThat(r.ageFinContrat()).isEqualTo(60);
        assertThat(r.anneesCarriereTotale()).isEqualTo(18);
        assertThat(r.anneesAncienneteSecteur()).isEqualTo(7);
        assertThat(r.dateFinContrat()).isEqualTo(DATE_FIN);
        assertThat(r.licenciementEffectif()).isTrue();
        assertThat(r.remunerationNetteMensuelleReference())
                .isEqualByComparingTo(REM_REF);
        assertThat(r.allocationChomageMensuelleEstimee())
                .isEqualByComparingTo(ALLOC_CHOMAGE);
    }

    // ── Base juridique ─────────────────────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceCadreComplet() {
        RccBeEntrepriseDifficulteResult r =
                RccBeEntrepriseDifficulteValidator.analyze(reqBase());

        assertThat(r.baseJuridique())
                .contains("26/12/2013")
                .contains("CCT n° 17")
                .contains("19/12/1974")
                .contains("AR du 03/05/2007")
                .contains("reconnaissance")
                .contains("CCT sectorielle");
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() ->
                RccBeEntrepriseDifficulteValidator.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_typeReconnaissanceNull_throws() {
        RccBeEntrepriseDifficulteRequest bad =
                new RccBeEntrepriseDifficulteRequest(
                        null, 55, 56, 12, 6, DATE_FIN, true, null, null);
        assertThatThrownBy(() ->
                RccBeEntrepriseDifficulteValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeReconnaissance");
    }

    @Test
    void analyze_ageReduitPlanNull_throws() {
        RccBeEntrepriseDifficulteRequest bad =
                new RccBeEntrepriseDifficulteRequest(
                        RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                        null, 56, 12, 6, DATE_FIN, true, null, null);
        assertThatThrownBy(() ->
                RccBeEntrepriseDifficulteValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ageReduitPlan");
    }

    @Test
    void analyze_ageReduitPlanNegatif_throws() {
        assertThatThrownBy(() ->
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                                -1, 56, 12, 6, true, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ageReduitPlan");
    }

    @Test
    void analyze_ageNull_throws() {
        RccBeEntrepriseDifficulteRequest bad =
                new RccBeEntrepriseDifficulteRequest(
                        RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                        55, null, 12, 6, DATE_FIN, true, null, null);
        assertThatThrownBy(() ->
                RccBeEntrepriseDifficulteValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ageFinContrat");
    }

    @Test
    void analyze_ageNegatif_throws() {
        assertThatThrownBy(() ->
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                                55, -1, 12, 6, true, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ageFinContrat");
    }

    @Test
    void analyze_carriereNull_throws() {
        RccBeEntrepriseDifficulteRequest bad =
                new RccBeEntrepriseDifficulteRequest(
                        RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                        55, 56, null, 6, DATE_FIN, true, null, null);
        assertThatThrownBy(() ->
                RccBeEntrepriseDifficulteValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anneesCarriereTotale");
    }

    @Test
    void analyze_carriereNegative_throws() {
        assertThatThrownBy(() ->
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                                55, 56, -1, 6, true, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anneesCarriereTotale");
    }

    @Test
    void analyze_ancienneteNull_throws() {
        RccBeEntrepriseDifficulteRequest bad =
                new RccBeEntrepriseDifficulteRequest(
                        RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                        55, 56, 12, null, DATE_FIN, true, null, null);
        assertThatThrownBy(() ->
                RccBeEntrepriseDifficulteValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anneesAncienneteSecteur");
    }

    @Test
    void analyze_ancienneteNegative_throws() {
        assertThatThrownBy(() ->
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                                55, 56, 12, -1, true, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anneesAncienneteSecteur");
    }

    @Test
    void analyze_ancienneteSuperieureCarriere_throws_coherence() {
        // ancienneté > carrière totale = impossible (l'ancienneté est
        // par définition incluse dans la carrière totale).
        assertThatThrownBy(() ->
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                                55, 56, 10, 12, true, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anneesAncienneteSecteur")
                .hasMessageContaining("anneesCarriereTotale");
    }

    @Test
    void analyze_dateFinContratNull_throws() {
        RccBeEntrepriseDifficulteRequest bad =
                new RccBeEntrepriseDifficulteRequest(
                        RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                        55, 56, 12, 6, null, true, null, null);
        assertThatThrownBy(() ->
                RccBeEntrepriseDifficulteValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateFinContrat");
    }

    @Test
    void analyze_licenciementEffectifNull_throws() {
        RccBeEntrepriseDifficulteRequest bad =
                new RccBeEntrepriseDifficulteRequest(
                        RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                        55, 56, 12, 6, DATE_FIN, null, null, null);
        assertThatThrownBy(() ->
                RccBeEntrepriseDifficulteValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("licenciementEffectif");
    }

    @Test
    void analyze_remunerationNegative_throws() {
        assertThatThrownBy(() ->
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                                55, 56, 12, 6, true,
                                new BigDecimal("-100"), ALLOC_CHOMAGE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remunerationNetteMensuelleReference");
    }

    @Test
    void analyze_allocationNegative_throws() {
        assertThatThrownBy(() ->
                RccBeEntrepriseDifficulteValidator.analyze(
                        req(RccBeEntrepriseDifficulteTypeEnum.EN_DIFFICULTE,
                                55, 56, 12, 6, true, REM_REF,
                                new BigDecimal("-50"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allocationChomageMensuelleEstimee");
    }
}
