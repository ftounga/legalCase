package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-11 : tests unitaires du {@link CongeEducationPayeRegionChecker}.
 *
 * <p>Couvre : (a) branchement régional (WBR / FLA / BXL) — plafonds
 * distincts par type de formation, (b) verdicts (PLEIN_DROIT / PRORATA /
 * HORS_FORMATION_AGREEE / OCCUPATION_INSUFFISANTE), (c) dérogation
 * publics fragilisés FLA (seuil 1/5 temps), (d) plafonnement combiné
 * heures formation × prorata × plafond régional, (e) base juridique
 * stable + avertissement systématique, (f) validation des inputs.</p>
 */
class CongeEducationPayeRegionCheckerTest {

    private static CongeEducationPayeRegionRequest reqWbrTempsPleinPro() {
        return new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.WALLONIE,
                CongeEducationPayeRegionTypeFormation.PROFESSIONNELLE_QUALIFIANTE,
                150,
                BigDecimal.ONE,
                false,
                true);
    }

    // ── Plein droit Wallonie ───────────────────────────────────────────────

    @Test
    void analyze_walloniePleinTempsPro_returnsPleinDroit180h() {
        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(reqWbrTempsPleinPro());

        assertThat(r.verdict())
                .isEqualTo(CongeEducationPayeRegionVerdict.ELIGIBLE_PLEIN_DROIT);
        assertThat(r.plafondRegionalHeures()).isEqualTo(180);
        assertThat(r.heuresCongeAllouees()).isEqualTo(150);
        assertThat(r.regimeApplicable()).isEqualTo("Wallonie");
        assertThat(r.raison()).isEqualTo("DROIT_RECONNU");
    }

    @Test
    void analyze_wallonieGenerale_plafond120h() {
        CongeEducationPayeRegionRequest req = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.WALLONIE,
                CongeEducationPayeRegionTypeFormation.GENERALE,
                200,
                BigDecimal.ONE, false, true);

        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(req);

        assertThat(r.plafondRegionalHeures()).isEqualTo(120);
        assertThat(r.heuresCongeAllouees()).isEqualTo(120); // plafonné
    }

    // ── Plein droit Flandre (VOV) ──────────────────────────────────────────

    @Test
    void analyze_flandrePleinTempsPro_returnsPleinDroit125h() {
        CongeEducationPayeRegionRequest req = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.FLANDRE,
                CongeEducationPayeRegionTypeFormation.PROFESSIONNELLE_QUALIFIANTE,
                100, BigDecimal.ONE, false, true);

        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(CongeEducationPayeRegionVerdict.ELIGIBLE_PLEIN_DROIT);
        assertThat(r.plafondRegionalHeures()).isEqualTo(125);
        assertThat(r.heuresCongeAllouees()).isEqualTo(100);
        assertThat(r.regimeApplicable()).isEqualTo("Flandre");
        assertThat(r.synthese()).contains("VOV");
    }

    @Test
    void analyze_flandreEnseignementSupAlternance_plafond250h() {
        CongeEducationPayeRegionRequest req = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.FLANDRE,
                CongeEducationPayeRegionTypeFormation.ENSEIGNEMENT_SUPERIEUR_ALTERNANCE,
                300, BigDecimal.ONE, false, true);

        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(req);

        assertThat(r.plafondRegionalHeures()).isEqualTo(250);
        assertThat(r.heuresCongeAllouees()).isEqualTo(250); // plafonné
    }

    // ── Plein droit Bruxelles ──────────────────────────────────────────────

    @Test
    void analyze_bruxellesPleinTempsPro_returnsPleinDroit100h() {
        CongeEducationPayeRegionRequest req = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.BRUXELLES,
                CongeEducationPayeRegionTypeFormation.PROFESSIONNELLE_QUALIFIANTE,
                80, BigDecimal.ONE, false, true);

        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(CongeEducationPayeRegionVerdict.ELIGIBLE_PLEIN_DROIT);
        assertThat(r.plafondRegionalHeures()).isEqualTo(100);
        assertThat(r.heuresCongeAllouees()).isEqualTo(80);
        assertThat(r.regimeApplicable()).isEqualTo("Bruxelles-Capitale");
        assertThat(r.synthese()).contains("Ordonnance 02/07/2015");
    }

    @Test
    void analyze_bruxellesGenerale_plafond80h() {
        CongeEducationPayeRegionRequest req = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.BRUXELLES,
                CongeEducationPayeRegionTypeFormation.GENERALE,
                100, BigDecimal.ONE, false, true);

        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(req);

        assertThat(r.plafondRegionalHeures()).isEqualTo(80);
        assertThat(r.heuresCongeAllouees()).isEqualTo(80);
    }

    // ── Reconversion publics fragilisés ────────────────────────────────────

    @Test
    void analyze_wallonieReconversionPublicFragilise_plafond180h() {
        CongeEducationPayeRegionRequest req = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.WALLONIE,
                CongeEducationPayeRegionTypeFormation.RECONVERSION_PUBLIC_FRAGILISE,
                200, BigDecimal.ONE, true, true);

        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(req);

        assertThat(r.plafondRegionalHeures()).isEqualTo(180);
        assertThat(r.heuresCongeAllouees()).isEqualTo(180);
    }

    @Test
    void analyze_bruxellesReconversionPublicFragilise_plafond120h() {
        CongeEducationPayeRegionRequest req = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.BRUXELLES,
                CongeEducationPayeRegionTypeFormation.RECONVERSION_PUBLIC_FRAGILISE,
                150, BigDecimal.ONE, true, true);

        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(req);

        assertThat(r.plafondRegionalHeures()).isEqualTo(120);
        assertThat(r.heuresCongeAllouees()).isEqualTo(120);
    }

    // ── Prorata mi-temps ───────────────────────────────────────────────────

    @Test
    void analyze_wallonieMitemps_returnsProrata90h() {
        CongeEducationPayeRegionRequest req = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.WALLONIE,
                CongeEducationPayeRegionTypeFormation.PROFESSIONNELLE_QUALIFIANTE,
                150,
                new BigDecimal("0.5"),
                false, true);

        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(CongeEducationPayeRegionVerdict.ELIGIBLE_PRORATA);
        assertThat(r.plafondRegionalHeures()).isEqualTo(180);
        // 180 × 0.5 = 90 h proratisé, formation 150 h → allouées = min(150, 90) = 90
        assertThat(r.heuresCongeAllouees()).isEqualTo(90);
        assertThat(r.synthese()).contains("temps partiel");
    }

    @Test
    void analyze_flandre80pctTemps_returnsProrata100h() {
        CongeEducationPayeRegionRequest req = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.FLANDRE,
                CongeEducationPayeRegionTypeFormation.PROFESSIONNELLE_QUALIFIANTE,
                200,
                new BigDecimal("0.8"),
                false, true);

        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(CongeEducationPayeRegionVerdict.ELIGIBLE_PRORATA);
        // 125 × 0.8 = 100 h proratisé
        assertThat(r.heuresCongeAllouees()).isEqualTo(100);
    }

    // ── Formation hors liste agréée ────────────────────────────────────────

    @Test
    void analyze_formationNonAgreee_returnsHorsListeAgreee() {
        CongeEducationPayeRegionRequest req = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.WALLONIE,
                CongeEducationPayeRegionTypeFormation.PROFESSIONNELLE_QUALIFIANTE,
                150, BigDecimal.ONE, false,
                false); // formationAgreeeRegion = false

        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(CongeEducationPayeRegionVerdict.INELIGIBLE_HORS_FORMATION_AGREEE);
        assertThat(r.heuresCongeAllouees()).isZero();
        assertThat(r.raison()).isEqualTo("FORMATION_HORS_LISTE_AGREEE");
    }

    @Test
    void analyze_typeFormationHorsListeAgreee_returnsHorsListeAgreee() {
        CongeEducationPayeRegionRequest req = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.FLANDRE,
                CongeEducationPayeRegionTypeFormation.HORS_LISTE_AGREEE,
                100, BigDecimal.ONE, false, true);

        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(CongeEducationPayeRegionVerdict.INELIGIBLE_HORS_FORMATION_AGREEE);
        assertThat(r.regimeApplicable()).isEqualTo("Flandre");
    }

    // ── Occupation insuffisante ────────────────────────────────────────────

    @Test
    void analyze_walloniePartielSousMitemps_returnsOccupationInsuffisante() {
        CongeEducationPayeRegionRequest req = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.WALLONIE,
                CongeEducationPayeRegionTypeFormation.PROFESSIONNELLE_QUALIFIANTE,
                150,
                new BigDecimal("0.3"),
                false, true);

        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(CongeEducationPayeRegionVerdict.INELIGIBLE_OCCUPATION_INSUFFISANTE);
        assertThat(r.heuresCongeAllouees()).isZero();
        assertThat(r.raison()).isEqualTo("OCCUPATION_INSUFFISANTE_SEUIL_REGIONAL");
    }

    @Test
    void analyze_bruxellesPartielSousMitemps_returnsOccupationInsuffisante() {
        CongeEducationPayeRegionRequest req = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.BRUXELLES,
                CongeEducationPayeRegionTypeFormation.PROFESSIONNELLE_QUALIFIANTE,
                80,
                new BigDecimal("0.4"),
                false, true);

        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(CongeEducationPayeRegionVerdict.INELIGIBLE_OCCUPATION_INSUFFISANTE);
    }

    // ── Dérogation publics fragilisés Flandre (1/5 temps) ──────────────────

    @Test
    void analyze_flandrePublicFragilise30pct_dérogationFlaApplique() {
        // 30 % ≥ 20 % (seuil dérogatoire FLA) → éligible prorata
        CongeEducationPayeRegionRequest req = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.FLANDRE,
                CongeEducationPayeRegionTypeFormation.RECONVERSION_PUBLIC_FRAGILISE,
                80,
                new BigDecimal("0.3"),
                true, true);

        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(CongeEducationPayeRegionVerdict.ELIGIBLE_PRORATA);
        assertThat(r.plafondRegionalHeures()).isEqualTo(180);
        // 180 × 0.3 = 54 h
        assertThat(r.heuresCongeAllouees()).isEqualTo(54);
    }

    @Test
    void analyze_flandrePublicFragiliseSous20pct_resteInsuffisant() {
        CongeEducationPayeRegionRequest req = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.FLANDRE,
                CongeEducationPayeRegionTypeFormation.RECONVERSION_PUBLIC_FRAGILISE,
                80,
                new BigDecimal("0.1"),
                true, true);

        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(CongeEducationPayeRegionVerdict.INELIGIBLE_OCCUPATION_INSUFFISANTE);
        assertThat(r.synthese()).contains("dérogation");
    }

    @Test
    void analyze_wallonieFragiliseSous50pct_pasDeDerogationHorsFlandre() {
        // La dérogation 1/5 temps est exclusivement FLA — pas WBR / BXL
        CongeEducationPayeRegionRequest req = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.WALLONIE,
                CongeEducationPayeRegionTypeFormation.RECONVERSION_PUBLIC_FRAGILISE,
                80,
                new BigDecimal("0.3"),
                true, true);

        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(CongeEducationPayeRegionVerdict.INELIGIBLE_OCCUPATION_INSUFFISANTE);
    }

    // ── Seuils exacts ──────────────────────────────────────────────────────

    @Test
    void analyze_seuilExactMitemps_estEligible() {
        CongeEducationPayeRegionRequest req = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.WALLONIE,
                CongeEducationPayeRegionTypeFormation.PROFESSIONNELLE_QUALIFIANTE,
                50,
                new BigDecimal("0.5"),
                false, true);

        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(CongeEducationPayeRegionVerdict.ELIGIBLE_PRORATA);
    }

    @Test
    void analyze_seuilExactFlaFragilise_estEligible() {
        CongeEducationPayeRegionRequest req = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.FLANDRE,
                CongeEducationPayeRegionTypeFormation.RECONVERSION_PUBLIC_FRAGILISE,
                30,
                new BigDecimal("0.2"),
                true, true);

        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(CongeEducationPayeRegionVerdict.ELIGIBLE_PRORATA);
    }

    // ── Plafonnement combiné ───────────────────────────────────────────────

    @Test
    void analyze_heuresFormationInferieuresPlafond_alloueHeuresFormation() {
        CongeEducationPayeRegionRequest req = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.WALLONIE,
                CongeEducationPayeRegionTypeFormation.PROFESSIONNELLE_QUALIFIANTE,
                40, BigDecimal.ONE, false, true);

        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(req);

        // 40 h demandées, plafond 180 h → allouées = 40
        assertThat(r.heuresCongeAllouees()).isEqualTo(40);
    }

    @Test
    void analyze_heuresFormationZero_alloueZero() {
        CongeEducationPayeRegionRequest req = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.WALLONIE,
                CongeEducationPayeRegionTypeFormation.PROFESSIONNELLE_QUALIFIANTE,
                0, BigDecimal.ONE, false, true);

        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(req);

        assertThat(r.heuresCongeAllouees()).isZero();
        assertThat(r.verdict())
                .isEqualTo(CongeEducationPayeRegionVerdict.ELIGIBLE_PLEIN_DROIT);
    }

    // ── Hiérarchie : formation hors liste prime sur occupation insuffisante ─

    @Test
    void analyze_horsListeEtOccupationInsuff_horsListeAgreeWins() {
        CongeEducationPayeRegionRequest req = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.WALLONIE,
                CongeEducationPayeRegionTypeFormation.PROFESSIONNELLE_QUALIFIANTE,
                50,
                new BigDecimal("0.3"),
                false, false);

        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(CongeEducationPayeRegionVerdict.INELIGIBLE_HORS_FORMATION_AGREEE);
    }

    // ── Snapshot inputs ─────────────────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(reqWbrTempsPleinPro());

        assertThat(r.region()).isEqualTo(CongeEducationPayeRegionRegion.WALLONIE);
        assertThat(r.typeFormation()).isEqualTo(
                CongeEducationPayeRegionTypeFormation.PROFESSIONNELLE_QUALIFIANTE);
        assertThat(r.heuresFormationAnnuelles()).isEqualTo(150);
        assertThat(r.tauxOccupation()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(r.publicFragilise()).isFalse();
        assertThat(r.formationAgreeeRegion()).isTrue();
    }

    // ── Base juridique + avertissement ──────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceLoi1985_etReformeEtat2014() {
        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(reqWbrTempsPleinPro());

        assertThat(r.baseJuridique())
                .contains("Loi du 22/01/1985")
                .contains("art. 108 à 117")
                .contains("Loi spéciale du 06/01/2014")
                .contains("Décret WBR du 19/02/2014")
                .contains("Vlaams Opleidingsverlof")
                .contains("Ordonnance du 02/07/2015");
    }

    @Test
    void analyze_avertissementSystematiquePresent() {
        CongeEducationPayeRegionResult r =
                CongeEducationPayeRegionChecker.analyze(reqWbrTempsPleinPro());

        assertThat(r.avertissement())
                .isNotNull()
                .contains("plafonds")
                .contains("agréées")
                .contains("avocat")
                .contains("Fonds régional");
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() ->
                CongeEducationPayeRegionChecker.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_regionNull_throws() {
        CongeEducationPayeRegionRequest bad = new CongeEducationPayeRegionRequest(
                null,
                CongeEducationPayeRegionTypeFormation.PROFESSIONNELLE_QUALIFIANTE,
                100, BigDecimal.ONE, false, true);

        assertThatThrownBy(() ->
                CongeEducationPayeRegionChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("region");
    }

    @Test
    void analyze_typeFormationNull_throws() {
        CongeEducationPayeRegionRequest bad = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.WALLONIE,
                null,
                100, BigDecimal.ONE, false, true);

        assertThatThrownBy(() ->
                CongeEducationPayeRegionChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeFormation");
    }

    @Test
    void analyze_heuresNull_throws() {
        CongeEducationPayeRegionRequest bad = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.WALLONIE,
                CongeEducationPayeRegionTypeFormation.GENERALE,
                null, BigDecimal.ONE, false, true);

        assertThatThrownBy(() ->
                CongeEducationPayeRegionChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("heuresFormationAnnuelles");
    }

    @Test
    void analyze_heuresNegatif_throws() {
        CongeEducationPayeRegionRequest bad = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.WALLONIE,
                CongeEducationPayeRegionTypeFormation.GENERALE,
                -5, BigDecimal.ONE, false, true);

        assertThatThrownBy(() ->
                CongeEducationPayeRegionChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("heuresFormationAnnuelles");
    }

    @Test
    void analyze_tauxOccupationNull_throws() {
        CongeEducationPayeRegionRequest bad = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.WALLONIE,
                CongeEducationPayeRegionTypeFormation.GENERALE,
                100, null, false, true);

        assertThatThrownBy(() ->
                CongeEducationPayeRegionChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tauxOccupation");
    }

    @Test
    void analyze_tauxOccupationNegatif_throws() {
        CongeEducationPayeRegionRequest bad = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.WALLONIE,
                CongeEducationPayeRegionTypeFormation.GENERALE,
                100, new BigDecimal("-0.1"), false, true);

        assertThatThrownBy(() ->
                CongeEducationPayeRegionChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tauxOccupation");
    }

    @Test
    void analyze_tauxOccupationSupOne_throws() {
        CongeEducationPayeRegionRequest bad = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.WALLONIE,
                CongeEducationPayeRegionTypeFormation.GENERALE,
                100, new BigDecimal("1.5"), false, true);

        assertThatThrownBy(() ->
                CongeEducationPayeRegionChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tauxOccupation");
    }

    @Test
    void analyze_publicFragiliseNull_throws() {
        CongeEducationPayeRegionRequest bad = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.WALLONIE,
                CongeEducationPayeRegionTypeFormation.GENERALE,
                100, BigDecimal.ONE, null, true);

        assertThatThrownBy(() ->
                CongeEducationPayeRegionChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("publicFragilise");
    }

    @Test
    void analyze_formationAgreeeNull_throws() {
        CongeEducationPayeRegionRequest bad = new CongeEducationPayeRegionRequest(
                CongeEducationPayeRegionRegion.WALLONIE,
                CongeEducationPayeRegionTypeFormation.GENERALE,
                100, BigDecimal.ONE, false, null);

        assertThatThrownBy(() ->
                CongeEducationPayeRegionChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("formationAgreeeRegion");
    }

    // ── Vérification du helper plafondRegional (couverture exhaustive) ─────

    @Test
    void plafondRegional_wallonie_tousTypes() {
        assertThat(CongeEducationPayeRegionChecker.plafondRegional(
                CongeEducationPayeRegionRegion.WALLONIE,
                CongeEducationPayeRegionTypeFormation.GENERALE))
                .isEqualTo(120);
        assertThat(CongeEducationPayeRegionChecker.plafondRegional(
                CongeEducationPayeRegionRegion.WALLONIE,
                CongeEducationPayeRegionTypeFormation.PROFESSIONNELLE_QUALIFIANTE))
                .isEqualTo(180);
        assertThat(CongeEducationPayeRegionChecker.plafondRegional(
                CongeEducationPayeRegionRegion.WALLONIE,
                CongeEducationPayeRegionTypeFormation.ENSEIGNEMENT_SUPERIEUR_ALTERNANCE))
                .isEqualTo(180);
        assertThat(CongeEducationPayeRegionChecker.plafondRegional(
                CongeEducationPayeRegionRegion.WALLONIE,
                CongeEducationPayeRegionTypeFormation.RECONVERSION_PUBLIC_FRAGILISE))
                .isEqualTo(180);
    }

    @Test
    void plafondRegional_flandre_tousTypes() {
        assertThat(CongeEducationPayeRegionChecker.plafondRegional(
                CongeEducationPayeRegionRegion.FLANDRE,
                CongeEducationPayeRegionTypeFormation.GENERALE))
                .isEqualTo(125);
        assertThat(CongeEducationPayeRegionChecker.plafondRegional(
                CongeEducationPayeRegionRegion.FLANDRE,
                CongeEducationPayeRegionTypeFormation.PROFESSIONNELLE_QUALIFIANTE))
                .isEqualTo(125);
        assertThat(CongeEducationPayeRegionChecker.plafondRegional(
                CongeEducationPayeRegionRegion.FLANDRE,
                CongeEducationPayeRegionTypeFormation.ENSEIGNEMENT_SUPERIEUR_ALTERNANCE))
                .isEqualTo(250);
        assertThat(CongeEducationPayeRegionChecker.plafondRegional(
                CongeEducationPayeRegionRegion.FLANDRE,
                CongeEducationPayeRegionTypeFormation.RECONVERSION_PUBLIC_FRAGILISE))
                .isEqualTo(180);
    }

    @Test
    void plafondRegional_bruxelles_tousTypes() {
        assertThat(CongeEducationPayeRegionChecker.plafondRegional(
                CongeEducationPayeRegionRegion.BRUXELLES,
                CongeEducationPayeRegionTypeFormation.GENERALE))
                .isEqualTo(80);
        assertThat(CongeEducationPayeRegionChecker.plafondRegional(
                CongeEducationPayeRegionRegion.BRUXELLES,
                CongeEducationPayeRegionTypeFormation.PROFESSIONNELLE_QUALIFIANTE))
                .isEqualTo(100);
        assertThat(CongeEducationPayeRegionChecker.plafondRegional(
                CongeEducationPayeRegionRegion.BRUXELLES,
                CongeEducationPayeRegionTypeFormation.ENSEIGNEMENT_SUPERIEUR_ALTERNANCE))
                .isEqualTo(100);
        assertThat(CongeEducationPayeRegionChecker.plafondRegional(
                CongeEducationPayeRegionRegion.BRUXELLES,
                CongeEducationPayeRegionTypeFormation.RECONVERSION_PUBLIC_FRAGILISE))
                .isEqualTo(120);
    }
}
