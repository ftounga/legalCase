package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-17 : tests unitaires du {@link ClauseEcolageBeValidator}.
 *
 * <p>Couvre : 8 verdicts (VALIDE_REMBOURSEMENT_DEGRESSIF / VALIDE_DUREE_EXPIREE
 * / INOPPOSABLE_MOTIF_DEPART / NULLE_FORME_ECRITE_MANQUANTE /
 * NULLE_FORMATION_OBLIGATOIRE / NULLE_COUT_INSUFFISANT /
 * NULLE_DUREE_EXCESSIVE / A_ANALYSER), dégressivité 100/66/33 % par tiers,
 * plafond 80 % du coût réel, base juridique stable, validation des
 * inputs.</p>
 */
class ClauseEcolageBeValidatorTest {

    // RMMMG mensuel 2024 indicatif ≈ 1 994 € — la valeur exacte est
    // paramétrée en input pour rester ajustable selon la date.
    private static final BigDecimal RMMMG = new BigDecimal("2000.00");
    private static final BigDecimal COUT_FORMATION = new BigDecimal("6000.00");
    private static final LocalDate FIN_FORMATION = LocalDate.of(2023, 1, 1);

    private static ClauseEcolageBeRequest reqNominal() {
        // Départ 6 mois après la fin formation → 1er tiers (sur 36 mois,
        // borne 1 = 12 → moisEcoules=6 < 12 → tier 1, quotité 100 %).
        return new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE,
                true,
                COUT_FORMATION,
                RMMMG,
                36,
                FIN_FORMATION,
                FIN_FORMATION.plusMonths(6),
                ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);
    }

    // ── Cas conforme nominaux ──────────────────────────────────────────────────

    @Test
    void validate_nominalTier1Demission_returnsValideDegressif100Pct() {
        ClauseEcolageBeResult r = ClauseEcolageBeValidator.validate(reqNominal());

        assertThat(r.verdict())
                .isEqualTo(ClauseEcolageBeVerdict.VALIDE_REMBOURSEMENT_DEGRESSIF);
        assertThat(r.tierDureeAuDepart()).isEqualTo(1);
        assertThat(r.quotiteDueRatio())
                .isEqualByComparingTo(ClauseEcolageBeValidator.QUOTITE_TIER_1);
        // 100 % × 6000 = 6000 ; plafond 80 % = 4800 → min = 4800
        assertThat(r.montantBrutDueEuros()).isEqualByComparingTo("6000.00");
        assertThat(r.plafond80Euros()).isEqualByComparingTo("4800.00");
        assertThat(r.montantDuFinalEuros()).isEqualByComparingTo("4800.00");
        assertThat(r.raison()).isEqualTo("VALIDE_DEGRESSIF");
        assertThat(r.synthese()).contains("plafonnée à 80 %");
    }

    @Test
    void validate_tier2_returns66Pct() {
        // 36 mois ; bornes 12 (tier1<) / 24 (tier2<). 18 mois → tier 2.
        ClauseEcolageBeRequest req = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, true,
                COUT_FORMATION, RMMMG, 36,
                FIN_FORMATION, FIN_FORMATION.plusMonths(18),
                ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);

        ClauseEcolageBeResult r = ClauseEcolageBeValidator.validate(req);

        assertThat(r.verdict())
                .isEqualTo(ClauseEcolageBeVerdict.VALIDE_REMBOURSEMENT_DEGRESSIF);
        assertThat(r.tierDureeAuDepart()).isEqualTo(2);
        // 0.6667 × 6000 = 4000.20 ; plafond 4800 ; min = 4000.20
        assertThat(r.montantBrutDueEuros()).isEqualByComparingTo("4000.20");
        assertThat(r.montantDuFinalEuros()).isEqualByComparingTo("4000.20");
    }

    @Test
    void validate_tier3_returns33Pct() {
        // 30 mois écoulés → tier 3 (24 ≤ 30 < 36).
        ClauseEcolageBeRequest req = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, true,
                COUT_FORMATION, RMMMG, 36,
                FIN_FORMATION, FIN_FORMATION.plusMonths(30),
                ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);

        ClauseEcolageBeResult r = ClauseEcolageBeValidator.validate(req);

        assertThat(r.verdict())
                .isEqualTo(ClauseEcolageBeVerdict.VALIDE_REMBOURSEMENT_DEGRESSIF);
        assertThat(r.tierDureeAuDepart()).isEqualTo(3);
        // 0.3333 × 6000 = 1999.80
        assertThat(r.montantBrutDueEuros()).isEqualByComparingTo("1999.80");
        assertThat(r.montantDuFinalEuros()).isEqualByComparingTo("1999.80");
    }

    @Test
    void validate_dureeExpiree_returnsValideDureeExpiree() {
        // 40 mois > 36 mois → tier 4 → VALIDE_DUREE_EXPIREE, 0 € dû.
        ClauseEcolageBeRequest req = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, true,
                COUT_FORMATION, RMMMG, 36,
                FIN_FORMATION, FIN_FORMATION.plusMonths(40),
                ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);

        ClauseEcolageBeResult r = ClauseEcolageBeValidator.validate(req);

        assertThat(r.verdict())
                .isEqualTo(ClauseEcolageBeVerdict.VALIDE_DUREE_EXPIREE);
        assertThat(r.tierDureeAuDepart()).isEqualTo(4);
        assertThat(r.montantDuFinalEuros()).isEqualByComparingTo("0.00");
        assertThat(r.raison()).isEqualTo("DUREE_EFFICACITE_EXPIREE");
    }

    @Test
    void validate_plafond80AppliquéQuandQuotite100() {
        // 100 % × coût = 6000 mais plafond 80 % = 4800 → final 4800.
        ClauseEcolageBeResult r = ClauseEcolageBeValidator.validate(reqNominal());

        assertThat(r.montantBrutDueEuros()).isEqualByComparingTo("6000.00");
        assertThat(r.plafond80Euros()).isEqualByComparingTo("4800.00");
        assertThat(r.montantDuFinalEuros()).isEqualByComparingTo("4800.00");
    }

    // ── Hiérarchie 1 : INDETERMINE → A_ANALYSER ─────────────────────────────────

    @Test
    void validate_typeIndetermine_returnsAanalyser() {
        ClauseEcolageBeRequest req = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.INDETERMINE, true,
                COUT_FORMATION, RMMMG, 36,
                FIN_FORMATION, FIN_FORMATION.plusMonths(6),
                ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);

        ClauseEcolageBeResult r = ClauseEcolageBeValidator.validate(req);

        assertThat(r.verdict()).isEqualTo(ClauseEcolageBeVerdict.A_ANALYSER);
        assertThat(r.raison()).isEqualTo("TYPE_FORMATION_INDETERMINE");
        assertThat(r.montantDuFinalEuros()).isEqualByComparingTo("0.00");
    }

    // ── Hiérarchie 2 : motif inopposable (prioritaire) ──────────────────────────

    @Test
    void validate_licenciementSansMotifGrave_returnsInopposable() {
        ClauseEcolageBeRequest req = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, true,
                COUT_FORMATION, RMMMG, 36,
                FIN_FORMATION, FIN_FORMATION.plusMonths(6),
                ClauseEcolageBeMotifDepart.LICENCIEMENT_EMPLOYEUR_SANS_MOTIF_GRAVE);

        ClauseEcolageBeResult r = ClauseEcolageBeValidator.validate(req);

        assertThat(r.verdict())
                .isEqualTo(ClauseEcolageBeVerdict.INOPPOSABLE_MOTIF_DEPART);
        assertThat(r.raison()).isEqualTo("MOTIF_DEPART_EXCLU");
        assertThat(r.montantDuFinalEuros()).isEqualByComparingTo("0.00");
        assertThat(r.synthese()).contains("art. 22bis § 3");
    }

    @Test
    void validate_ruptureMotifGraveEmployeur_returnsInopposable() {
        ClauseEcolageBeRequest req = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, true,
                COUT_FORMATION, RMMMG, 36,
                FIN_FORMATION, FIN_FORMATION.plusMonths(6),
                ClauseEcolageBeMotifDepart.RUPTURE_MOTIF_GRAVE_EMPLOYEUR);

        assertThat(ClauseEcolageBeValidator.validate(req).verdict())
                .isEqualTo(ClauseEcolageBeVerdict.INOPPOSABLE_MOTIF_DEPART);
    }

    @Test
    void validate_finCddNormale_returnsInopposable() {
        ClauseEcolageBeRequest req = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, true,
                COUT_FORMATION, RMMMG, 36,
                FIN_FORMATION, FIN_FORMATION.plusMonths(6),
                ClauseEcolageBeMotifDepart.FIN_CDD_NORMALE);

        assertThat(ClauseEcolageBeValidator.validate(req).verdict())
                .isEqualTo(ClauseEcolageBeVerdict.INOPPOSABLE_MOTIF_DEPART);
    }

    @Test
    void validate_licenciementMotifGraveTravailleur_actionnable() {
        ClauseEcolageBeRequest req = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, true,
                COUT_FORMATION, RMMMG, 36,
                FIN_FORMATION, FIN_FORMATION.plusMonths(6),
                ClauseEcolageBeMotifDepart.LICENCIEMENT_MOTIF_GRAVE_TRAVAILLEUR);

        assertThat(ClauseEcolageBeValidator.validate(req).verdict())
                .isEqualTo(ClauseEcolageBeVerdict.VALIDE_REMBOURSEMENT_DEGRESSIF);
    }

    @Test
    void validate_motifInopposableEtFormeManquante_motifPrime() {
        // Le motif d'exclusion (art. 22bis § 3) est prioritaire sur la
        // forme écrite (§ 1er) — c'est la doctrine BE : aucune somme
        // exigible, indépendamment de la régularité formelle.
        ClauseEcolageBeRequest req = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, false,
                COUT_FORMATION, RMMMG, 36,
                FIN_FORMATION, FIN_FORMATION.plusMonths(6),
                ClauseEcolageBeMotifDepart.LICENCIEMENT_EMPLOYEUR_SANS_MOTIF_GRAVE);

        assertThat(ClauseEcolageBeValidator.validate(req).verdict())
                .isEqualTo(ClauseEcolageBeVerdict.INOPPOSABLE_MOTIF_DEPART);
    }

    // ── Hiérarchie 3 : formation obligatoire ────────────────────────────────────

    @Test
    void validate_formationObligatoire_returnsNulleObligatoire() {
        ClauseEcolageBeRequest req = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.OBLIGATOIRE_LEGALE, true,
                COUT_FORMATION, RMMMG, 36,
                FIN_FORMATION, FIN_FORMATION.plusMonths(6),
                ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);

        ClauseEcolageBeResult r = ClauseEcolageBeValidator.validate(req);

        assertThat(r.verdict())
                .isEqualTo(ClauseEcolageBeVerdict.NULLE_FORMATION_OBLIGATOIRE);
        assertThat(r.raison()).isEqualTo("FORMATION_OBLIGATOIRE_LEGALE");
        assertThat(r.montantDuFinalEuros()).isEqualByComparingTo("0.00");
    }

    // ── Hiérarchie 4 : forme écrite manquante ───────────────────────────────────

    @Test
    void validate_formeEcriteManquante_returnsNulleForme() {
        ClauseEcolageBeRequest req = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, false,
                COUT_FORMATION, RMMMG, 36,
                FIN_FORMATION, FIN_FORMATION.plusMonths(6),
                ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);

        ClauseEcolageBeResult r = ClauseEcolageBeValidator.validate(req);

        assertThat(r.verdict())
                .isEqualTo(ClauseEcolageBeVerdict.NULLE_FORME_ECRITE_MANQUANTE);
        assertThat(r.raison()).isEqualTo("FORME_ECRITE_MANQUANTE");
        assertThat(r.synthese()).contains("art. 22bis § 1er, al. 1er");
        assertThat(r.montantDuFinalEuros()).isEqualByComparingTo("0.00");
    }

    // ── Hiérarchie 5 : coût insuffisant ─────────────────────────────────────────

    @Test
    void validate_coutEgalDemiRmmmg_returnsNulleCoutInsuffisant() {
        // RMMMG=2000 → seuil = 1000 ; coût=1000 → ≤ → NULLE.
        ClauseEcolageBeRequest req = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, true,
                new BigDecimal("1000.00"), RMMMG, 36,
                FIN_FORMATION, FIN_FORMATION.plusMonths(6),
                ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);

        ClauseEcolageBeResult r = ClauseEcolageBeValidator.validate(req);

        assertThat(r.verdict())
                .isEqualTo(ClauseEcolageBeVerdict.NULLE_COUT_INSUFFISANT);
        assertThat(r.raison()).isEqualTo("COUT_INFERIEUR_MOITIE_RMMMG");
        assertThat(r.coutMinLegalEuros()).isEqualByComparingTo("1000.00");
    }

    @Test
    void validate_coutJusteAuDessus_estValide() {
        // RMMMG=2000 → seuil = 1000 ; coût=1000.01 → > → VALIDE.
        ClauseEcolageBeRequest req = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, true,
                new BigDecimal("1000.01"), RMMMG, 36,
                FIN_FORMATION, FIN_FORMATION.plusMonths(6),
                ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);

        assertThat(ClauseEcolageBeValidator.validate(req).verdict())
                .isEqualTo(ClauseEcolageBeVerdict.VALIDE_REMBOURSEMENT_DEGRESSIF);
    }

    // ── Hiérarchie 6 : durée excessive ──────────────────────────────────────────

    @Test
    void validate_dureeExcessive37mois_returnsNulleDureeExcessive() {
        ClauseEcolageBeRequest req = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, true,
                COUT_FORMATION, RMMMG, 37,
                FIN_FORMATION, FIN_FORMATION.plusMonths(6),
                ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);

        ClauseEcolageBeResult r = ClauseEcolageBeValidator.validate(req);

        assertThat(r.verdict())
                .isEqualTo(ClauseEcolageBeVerdict.NULLE_DUREE_EXCESSIVE);
        assertThat(r.raison()).isEqualTo("DUREE_EFFICACITE_EXCESSIVE");
    }

    @Test
    void validate_dureeLimite36mois_estValide() {
        ClauseEcolageBeRequest req = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, true,
                COUT_FORMATION, RMMMG, 36,
                FIN_FORMATION, FIN_FORMATION.plusMonths(6),
                ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);

        assertThat(ClauseEcolageBeValidator.validate(req).verdict())
                .isEqualTo(ClauseEcolageBeVerdict.VALIDE_REMBOURSEMENT_DEGRESSIF);
    }

    // ── Calcul des tiers — bornes exactes ───────────────────────────────────────

    @Test
    void computeTier_borneTier12_dureeStandard36() {
        // 12 mois sur 36 → borne 1 = 36/3 = 12 → moisEcoules=12 → pas < 12 → tier 2.
        assertThat(ClauseEcolageBeValidator.computeTier(11, 36)).isEqualTo(1);
        assertThat(ClauseEcolageBeValidator.computeTier(12, 36)).isEqualTo(2);
        assertThat(ClauseEcolageBeValidator.computeTier(23, 36)).isEqualTo(2);
        assertThat(ClauseEcolageBeValidator.computeTier(24, 36)).isEqualTo(3);
        assertThat(ClauseEcolageBeValidator.computeTier(35, 36)).isEqualTo(3);
        assertThat(ClauseEcolageBeValidator.computeTier(36, 36)).isEqualTo(4);
    }

    @Test
    void computeTier_dureePersonnalisee24mois() {
        // 24 mois → bornes 8 / 16
        assertThat(ClauseEcolageBeValidator.computeTier(0, 24)).isEqualTo(1);
        assertThat(ClauseEcolageBeValidator.computeTier(7, 24)).isEqualTo(1);
        assertThat(ClauseEcolageBeValidator.computeTier(8, 24)).isEqualTo(2);
        assertThat(ClauseEcolageBeValidator.computeTier(15, 24)).isEqualTo(2);
        assertThat(ClauseEcolageBeValidator.computeTier(16, 24)).isEqualTo(3);
        assertThat(ClauseEcolageBeValidator.computeTier(24, 24)).isEqualTo(4);
    }

    @Test
    void validate_departAvantFinFormation_traiteCommeTier1() {
        // Anomalie input : depart < finFormation. On force moisEcoules=0 → tier 1.
        ClauseEcolageBeRequest req = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, true,
                COUT_FORMATION, RMMMG, 36,
                FIN_FORMATION, FIN_FORMATION.minusMonths(1),
                ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);

        ClauseEcolageBeResult r = ClauseEcolageBeValidator.validate(req);

        assertThat(r.moisEcoulesDepuisFinFormation()).isEqualTo(0);
        assertThat(r.tierDureeAuDepart()).isEqualTo(1);
        assertThat(r.verdict())
                .isEqualTo(ClauseEcolageBeVerdict.VALIDE_REMBOURSEMENT_DEGRESSIF);
    }

    // ── Snapshot inputs ─────────────────────────────────────────────────────────

    @Test
    void validate_snapshot_inclutTousLesInputs() {
        ClauseEcolageBeResult r = ClauseEcolageBeValidator.validate(reqNominal());

        assertThat(r.typeFormation())
                .isEqualTo(ClauseEcolageBeTypeFormation.SPECIFIQUE);
        assertThat(r.clauseEcriteAvantEntreeFormation()).isTrue();
        assertThat(r.coutReelFormationEuros())
                .isEqualByComparingTo("6000.00");
        assertThat(r.rmmmgMensuelEuros())
                .isEqualByComparingTo("2000.00");
        assertThat(r.dureeEfficaciteMois()).isEqualTo(36);
        assertThat(r.dateFinFormation()).isEqualTo(FIN_FORMATION);
        assertThat(r.dateDepartTravailleur())
                .isEqualTo(FIN_FORMATION.plusMonths(6));
        assertThat(r.motifDepart())
                .isEqualTo(ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);
    }

    // ── Base juridique + avertissement ──────────────────────────────────────────

    @Test
    void validate_baseJuridique_referenceLoi1978Art22bisEtRmmmg() {
        ClauseEcolageBeResult r = ClauseEcolageBeValidator.validate(reqNominal());

        assertThat(r.baseJuridique())
                .contains("03/07/1978")
                .contains("22bis")
                .contains("27/12/2006")
                .contains("CCT n° 43")
                .contains("RMMMG")
                .contains("36 mois")
                .contains("80 %");
    }

    @Test
    void validate_avertissementSystematiquePresent() {
        ClauseEcolageBeResult r = ClauseEcolageBeValidator.validate(reqNominal());

        assertThat(r.avertissement())
                .isNotNull()
                .contains("RMMMG")
                .contains("avocat")
                .contains("indexé");
    }

    // ── Validation conditionnelle ───────────────────────────────────────────────

    @Test
    void validate_requestNull_throws() {
        assertThatThrownBy(() -> ClauseEcolageBeValidator.validate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void validate_typeFormationNull_throws() {
        ClauseEcolageBeRequest bad = new ClauseEcolageBeRequest(
                null, true, COUT_FORMATION, RMMMG, 36,
                FIN_FORMATION, FIN_FORMATION.plusMonths(6),
                ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);

        assertThatThrownBy(() -> ClauseEcolageBeValidator.validate(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeFormation");
    }

    @Test
    void validate_ecriteNull_throws() {
        ClauseEcolageBeRequest bad = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, null,
                COUT_FORMATION, RMMMG, 36,
                FIN_FORMATION, FIN_FORMATION.plusMonths(6),
                ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);

        assertThatThrownBy(() -> ClauseEcolageBeValidator.validate(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("clauseEcriteAvantEntreeFormation");
    }

    @Test
    void validate_coutNull_throws() {
        ClauseEcolageBeRequest bad = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, true,
                null, RMMMG, 36,
                FIN_FORMATION, FIN_FORMATION.plusMonths(6),
                ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);

        assertThatThrownBy(() -> ClauseEcolageBeValidator.validate(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("coutReelFormationEuros");
    }

    @Test
    void validate_coutNegatif_throws() {
        ClauseEcolageBeRequest bad = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, true,
                new BigDecimal("-1.00"), RMMMG, 36,
                FIN_FORMATION, FIN_FORMATION.plusMonths(6),
                ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);

        assertThatThrownBy(() -> ClauseEcolageBeValidator.validate(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("coutReelFormationEuros");
    }

    @Test
    void validate_rmmmgNull_throws() {
        ClauseEcolageBeRequest bad = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, true,
                COUT_FORMATION, null, 36,
                FIN_FORMATION, FIN_FORMATION.plusMonths(6),
                ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);

        assertThatThrownBy(() -> ClauseEcolageBeValidator.validate(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rmmmgMensuelEuros");
    }

    @Test
    void validate_rmmmgZero_throws() {
        ClauseEcolageBeRequest bad = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, true,
                COUT_FORMATION, BigDecimal.ZERO, 36,
                FIN_FORMATION, FIN_FORMATION.plusMonths(6),
                ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);

        assertThatThrownBy(() -> ClauseEcolageBeValidator.validate(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rmmmgMensuelEuros");
    }

    @Test
    void validate_dureeNull_throws() {
        ClauseEcolageBeRequest bad = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, true,
                COUT_FORMATION, RMMMG, null,
                FIN_FORMATION, FIN_FORMATION.plusMonths(6),
                ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);

        assertThatThrownBy(() -> ClauseEcolageBeValidator.validate(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeEfficaciteMois");
    }

    @Test
    void validate_dureeZero_throws() {
        ClauseEcolageBeRequest bad = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, true,
                COUT_FORMATION, RMMMG, 0,
                FIN_FORMATION, FIN_FORMATION.plusMonths(6),
                ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);

        assertThatThrownBy(() -> ClauseEcolageBeValidator.validate(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeEfficaciteMois");
    }

    @Test
    void validate_dateFinFormationNull_throws() {
        ClauseEcolageBeRequest bad = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, true,
                COUT_FORMATION, RMMMG, 36,
                null, FIN_FORMATION.plusMonths(6),
                ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);

        assertThatThrownBy(() -> ClauseEcolageBeValidator.validate(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateFinFormation");
    }

    @Test
    void validate_dateDepartNull_throws() {
        ClauseEcolageBeRequest bad = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, true,
                COUT_FORMATION, RMMMG, 36,
                FIN_FORMATION, null,
                ClauseEcolageBeMotifDepart.DEMISSION_TRAVAILLEUR);

        assertThatThrownBy(() -> ClauseEcolageBeValidator.validate(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDepartTravailleur");
    }

    @Test
    void validate_motifNull_throws() {
        ClauseEcolageBeRequest bad = new ClauseEcolageBeRequest(
                ClauseEcolageBeTypeFormation.SPECIFIQUE, true,
                COUT_FORMATION, RMMMG, 36,
                FIN_FORMATION, FIN_FORMATION.plusMonths(6),
                null);

        assertThatThrownBy(() -> ClauseEcolageBeValidator.validate(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifDepart");
    }
}
