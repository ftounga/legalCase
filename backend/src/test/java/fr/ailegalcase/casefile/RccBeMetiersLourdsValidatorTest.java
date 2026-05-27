package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-01 : tests unitaires du {@link RccBeMetiersLourdsValidator}.
 *
 * <p>Vérifie : conditions cumulatives (licenciement / âge / carrière /
 * métier lourd), alternative 5/10 OU 7/15, ordre d'évaluation
 * (démission > âge > carrière > durée métier lourd), bornes inclusives,
 * validation des inputs (null, hors bornes, cohérence 10 ≤ 15).</p>
 */
class RccBeMetiersLourdsValidatorTest {

    private static RccBeMetiersLourdsRequest req(
            int age, int carriere, int m10, int m15,
            RccBeMetiersLourdsTypeEnum type, boolean licenciement) {
        return new RccBeMetiersLourdsRequest(
                age, carriere, m10, m15, type, licenciement);
    }

    private static RccBeMetiersLourdsRequest eligibleReq() {
        return req(60, 38, 6, 8, RccBeMetiersLourdsTypeEnum.TRAVAIL_PENIBLE, true);
    }

    // ── Verdict ELIGIBLE ───────────────────────────────────────────────────

    @Test
    void analyze_toutesConditionsOK_returnsEligible() {
        RccBeMetiersLourdsResult r = RccBeMetiersLourdsValidator.analyze(eligibleReq());

        assertThat(r.verdict()).isEqualTo(RccBeMetiersLourdsVerdictEnum.ELIGIBLE);
        assertThat(r.raisonIneligibilite()).isNull();
        assertThat(r.synthese()).contains("éligible");
        assertThat(r.synthese()).contains("CCT 17");
        assertThat(r.baseJuridique())
                .contains("CCT n°17")
                .contains("AR du 03/05/2007");
        assertThat(r.avertissement()).contains("vérifier annuellement");
    }

    @Test
    void analyze_alternative5sur10_returnsEligible() {
        // 5 ans / 10 ans suffit (n'a pas besoin de 7/15)
        RccBeMetiersLourdsResult r = RccBeMetiersLourdsValidator.analyze(
                req(58, 35,
                        5 /* >= 5 sur 10 */, 6 /* < 7 sur 15 */,
                        RccBeMetiersLourdsTypeEnum.EQUIPES_SUCCESSIVES_NUIT, true));

        assertThat(r.verdict()).isEqualTo(RccBeMetiersLourdsVerdictEnum.ELIGIBLE);
    }

    @Test
    void analyze_alternative7sur15_returnsEligible() {
        // 7/15 suffit même si 4/10
        RccBeMetiersLourdsResult r = RccBeMetiersLourdsValidator.analyze(
                req(58, 35,
                        4 /* < 5 sur 10 */, 7 /* >= 7 sur 15 */,
                        RccBeMetiersLourdsTypeEnum.INTERRUPTIONS_HORAIRES_VARIABLES,
                        true));

        assertThat(r.verdict()).isEqualTo(RccBeMetiersLourdsVerdictEnum.ELIGIBLE);
    }

    // ── Bornes inclusives ──────────────────────────────────────────────────

    @Test
    void analyze_age58_borneInclusive_returnsEligible() {
        RccBeMetiersLourdsResult r = RccBeMetiersLourdsValidator.analyze(
                req(58, 35, 5, 5,
                        RccBeMetiersLourdsTypeEnum.TRAVAIL_PENIBLE, true));
        assertThat(r.verdict()).isEqualTo(RccBeMetiersLourdsVerdictEnum.ELIGIBLE);
    }

    @Test
    void analyze_carriere35_borneInclusive_returnsEligible() {
        RccBeMetiersLourdsResult r = RccBeMetiersLourdsValidator.analyze(
                req(58, 35, 5, 5,
                        RccBeMetiersLourdsTypeEnum.TRAVAIL_PENIBLE, true));
        assertThat(r.verdict()).isEqualTo(RccBeMetiersLourdsVerdictEnum.ELIGIBLE);
    }

    // ── Verdict INELIGIBLE — démission (priorité 1) ───────────────────────

    @Test
    void analyze_demission_returnsIneligibleDemission() {
        // Même si toutes les autres conditions sont OK
        RccBeMetiersLourdsResult r = RccBeMetiersLourdsValidator.analyze(
                req(65, 45, 8, 12,
                        RccBeMetiersLourdsTypeEnum.TRAVAIL_PENIBLE,
                        false /* pas de licenciement */));

        assertThat(r.verdict()).isEqualTo(RccBeMetiersLourdsVerdictEnum.INELIGIBLE);
        assertThat(r.raisonIneligibilite())
                .isEqualTo(RccBeMetiersLourdsRaisonIneligibilite.DEMISSION_NON_ELIGIBLE);
        assertThat(r.synthese()).contains("démission");
    }

    // ── Verdict INELIGIBLE — âge (priorité 2) ─────────────────────────────

    @Test
    void analyze_age57_returnsIneligibleAge() {
        RccBeMetiersLourdsResult r = RccBeMetiersLourdsValidator.analyze(
                req(57, 38, 6, 8,
                        RccBeMetiersLourdsTypeEnum.TRAVAIL_PENIBLE, true));

        assertThat(r.verdict()).isEqualTo(RccBeMetiersLourdsVerdictEnum.INELIGIBLE);
        assertThat(r.raisonIneligibilite())
                .isEqualTo(RccBeMetiersLourdsRaisonIneligibilite.AGE_INSUFFISANT);
    }

    // ── Verdict INELIGIBLE — carrière (priorité 3) ────────────────────────

    @Test
    void analyze_carriere34_returnsIneligibleCarriere() {
        RccBeMetiersLourdsResult r = RccBeMetiersLourdsValidator.analyze(
                req(60, 34, 6, 8,
                        RccBeMetiersLourdsTypeEnum.TRAVAIL_PENIBLE, true));

        assertThat(r.verdict()).isEqualTo(RccBeMetiersLourdsVerdictEnum.INELIGIBLE);
        assertThat(r.raisonIneligibilite())
                .isEqualTo(RccBeMetiersLourdsRaisonIneligibilite.CARRIERE_INSUFFISANTE);
    }

    // ── Verdict INELIGIBLE — durée métier lourd (priorité 4) ──────────────

    @Test
    void analyze_metierLourdInsuffisant_returnsIneligibleDuree() {
        // 4/10 ET 6/15 → ni 5/10 ni 7/15
        RccBeMetiersLourdsResult r = RccBeMetiersLourdsValidator.analyze(
                req(60, 38, 4, 6,
                        RccBeMetiersLourdsTypeEnum.AUTRE, true));

        assertThat(r.verdict()).isEqualTo(RccBeMetiersLourdsVerdictEnum.INELIGIBLE);
        assertThat(r.raisonIneligibilite())
                .isEqualTo(RccBeMetiersLourdsRaisonIneligibilite.DUREE_METIER_LOURD_INSUFFISANTE);
    }

    // ── Ordre d'évaluation (démission prioritaire) ────────────────────────

    @Test
    void analyze_demissionEtAgeInsuffisant_returnsDemissionEnPriorite() {
        // Démission ET âge insuffisant ET carrière insuffisante ET métier
        // lourd insuffisant → DEMISSION_NON_ELIGIBLE en premier
        RccBeMetiersLourdsResult r = RccBeMetiersLourdsValidator.analyze(
                req(50, 20, 2, 3,
                        RccBeMetiersLourdsTypeEnum.AUTRE, false));

        assertThat(r.raisonIneligibilite())
                .isEqualTo(RccBeMetiersLourdsRaisonIneligibilite.DEMISSION_NON_ELIGIBLE);
    }

    @Test
    void analyze_ageEtCarriereInsuffisants_returnsAgeEnPriorite() {
        // Âge insuffisant ET carrière insuffisante → AGE_INSUFFISANT
        RccBeMetiersLourdsResult r = RccBeMetiersLourdsValidator.analyze(
                req(50, 20, 2, 3,
                        RccBeMetiersLourdsTypeEnum.AUTRE, true));

        assertThat(r.raisonIneligibilite())
                .isEqualTo(RccBeMetiersLourdsRaisonIneligibilite.AGE_INSUFFISANT);
    }

    @Test
    void analyze_carriereEtMetierLourdInsuffisants_returnsCarriereEnPriorite() {
        // Âge OK, carrière insuffisante, métier lourd insuffisant → CARRIERE
        RccBeMetiersLourdsResult r = RccBeMetiersLourdsValidator.analyze(
                req(60, 30, 3, 4,
                        RccBeMetiersLourdsTypeEnum.AUTRE, true));

        assertThat(r.raisonIneligibilite())
                .isEqualTo(RccBeMetiersLourdsRaisonIneligibilite.CARRIERE_INSUFFISANTE);
    }

    // ── Snapshot inputs préservés ─────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        RccBeMetiersLourdsRequest in = req(62, 40, 7, 10,
                RccBeMetiersLourdsTypeEnum.EQUIPES_SUCCESSIVES_NUIT, true);

        RccBeMetiersLourdsResult r = RccBeMetiersLourdsValidator.analyze(in);

        assertThat(r.ageFinContrat()).isEqualTo(62);
        assertThat(r.anneesCarriereTotal()).isEqualTo(40);
        assertThat(r.anneesMetierLourdRecent10()).isEqualTo(7);
        assertThat(r.anneesMetierLourdRecent15()).isEqualTo(10);
        assertThat(r.typeMetierLourd())
                .isEqualTo(RccBeMetiersLourdsTypeEnum.EQUIPES_SUCCESSIVES_NUIT);
        assertThat(r.licenciementEffectif()).isTrue();
    }

    // ── Validation : null ─────────────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() -> RccBeMetiersLourdsValidator.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_ageNull_throws() {
        RccBeMetiersLourdsRequest bad = new RccBeMetiersLourdsRequest(
                null, 35, 5, 5,
                RccBeMetiersLourdsTypeEnum.AUTRE, true);
        assertThatThrownBy(() -> RccBeMetiersLourdsValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ageFinContrat");
    }

    @Test
    void analyze_carriereNull_throws() {
        RccBeMetiersLourdsRequest bad = new RccBeMetiersLourdsRequest(
                58, null, 5, 5,
                RccBeMetiersLourdsTypeEnum.AUTRE, true);
        assertThatThrownBy(() -> RccBeMetiersLourdsValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anneesCarriereTotal");
    }

    @Test
    void analyze_m10Null_throws() {
        RccBeMetiersLourdsRequest bad = new RccBeMetiersLourdsRequest(
                58, 35, null, 5,
                RccBeMetiersLourdsTypeEnum.AUTRE, true);
        assertThatThrownBy(() -> RccBeMetiersLourdsValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anneesMetierLourdRecent10");
    }

    @Test
    void analyze_m15Null_throws() {
        RccBeMetiersLourdsRequest bad = new RccBeMetiersLourdsRequest(
                58, 35, 5, null,
                RccBeMetiersLourdsTypeEnum.AUTRE, true);
        assertThatThrownBy(() -> RccBeMetiersLourdsValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anneesMetierLourdRecent15");
    }

    @Test
    void analyze_typeNull_throws() {
        RccBeMetiersLourdsRequest bad = new RccBeMetiersLourdsRequest(
                58, 35, 5, 5, null, true);
        assertThatThrownBy(() -> RccBeMetiersLourdsValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeMetierLourd");
    }

    @Test
    void analyze_licenciementEffectifNull_throws() {
        RccBeMetiersLourdsRequest bad = new RccBeMetiersLourdsRequest(
                58, 35, 5, 5,
                RccBeMetiersLourdsTypeEnum.AUTRE, null);
        assertThatThrownBy(() -> RccBeMetiersLourdsValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("licenciementEffectif");
    }

    // ── Validation : bornes ──────────────────────────────────────────────

    @Test
    void analyze_age17_throws() {
        RccBeMetiersLourdsRequest bad = req(17, 35, 5, 5,
                RccBeMetiersLourdsTypeEnum.AUTRE, true);
        assertThatThrownBy(() -> RccBeMetiersLourdsValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ageFinContrat");
    }

    @Test
    void analyze_age81_throws() {
        RccBeMetiersLourdsRequest bad = req(81, 35, 5, 5,
                RccBeMetiersLourdsTypeEnum.AUTRE, true);
        assertThatThrownBy(() -> RccBeMetiersLourdsValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ageFinContrat");
    }

    @Test
    void analyze_carriereNegative_throws() {
        RccBeMetiersLourdsRequest bad = req(58, -1, 5, 5,
                RccBeMetiersLourdsTypeEnum.AUTRE, true);
        assertThatThrownBy(() -> RccBeMetiersLourdsValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anneesCarriereTotal");
    }

    @Test
    void analyze_carriere61_throws() {
        RccBeMetiersLourdsRequest bad = req(58, 61, 5, 5,
                RccBeMetiersLourdsTypeEnum.AUTRE, true);
        assertThatThrownBy(() -> RccBeMetiersLourdsValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anneesCarriereTotal");
    }

    @Test
    void analyze_m10Hors10_throws() {
        RccBeMetiersLourdsRequest bad = req(58, 35, 11, 11,
                RccBeMetiersLourdsTypeEnum.AUTRE, true);
        assertThatThrownBy(() -> RccBeMetiersLourdsValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anneesMetierLourdRecent10");
    }

    @Test
    void analyze_m15Hors15_throws() {
        RccBeMetiersLourdsRequest bad = req(58, 35, 5, 16,
                RccBeMetiersLourdsTypeEnum.AUTRE, true);
        assertThatThrownBy(() -> RccBeMetiersLourdsValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anneesMetierLourdRecent15");
    }

    @Test
    void analyze_m10SuperieurM15_throwsCoherence() {
        // 6/10 et 4/15 → incohérent (les 10 dernières sont dans les 15)
        RccBeMetiersLourdsRequest bad = req(58, 35, 6, 4,
                RccBeMetiersLourdsTypeEnum.AUTRE, true);
        assertThatThrownBy(() -> RccBeMetiersLourdsValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anneesMetierLourdRecent10")
                .hasMessageContaining("supérieur");
    }
}
