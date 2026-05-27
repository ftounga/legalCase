package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-06 : tests unitaires du {@link
 * LicenciementBeFermetureEntrepriseCalculator}.
 *
 * <p>Couvre : qualification du type de fermeture, seuil d'effectif
 * (≥ 20 ETP), ancienneté minimale (≥ 1 an), calcul de l'indemnité
 * forfaitaire + supplément âge ≥ 45 ans, reprise FFE des créances
 * impayées selon solvabilité de l'employeur, hiérarchie des verdicts
 * d'inéligibilité (type &gt; effectif &gt; ancienneté), validation des
 * inputs et base juridique stable (Loi 26/06/2002 + AR 23/03/2007).</p>
 */
class LicenciementBeFermetureEntrepriseCalculatorTest {

    private static final LocalDate NAISSANCE_1976 = LocalDate.of(1976, 1, 15);
    private static final LocalDate DEBUT_CONTRAT_2010 = LocalDate.of(2010, 1, 15);
    private static final LocalDate FERMETURE_2026 = LocalDate.of(2026, 1, 15);
    // → âge 50 ans à la fermeture, ancienneté 16 ans

    private static LicenciementBeFermetureEntrepriseRequest reqBase() {
        return new LicenciementBeFermetureEntrepriseRequest(
                NAISSANCE_1976,
                DEBUT_CONTRAT_2010,
                FERMETURE_2026,
                new BigDecimal("3200.00"),
                LicenciementBeFermetureEntrepriseTypeFermeture.CESSATION_DEFINITIVE,
                LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE,
                35,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    // ── Cas éligible indemnité seule (solvable) ────────────────────────────

    @Test
    void analyze_solvableAvecIndemnite_returnsEligibleIndemniteSeule() {
        LicenciementBeFermetureEntrepriseResult r =
                LicenciementBeFermetureEntrepriseCalculator.analyze(reqBase());

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeFermetureEntrepriseVerdict.ELIGIBLE_INDEMNITE_SEULE);
        assertThat(r.eligible()).isTrue();
        assertThat(r.ageADateFermeture()).isEqualTo(50);
        assertThat(r.anneesAnciennete()).isEqualTo(16);
        // 16 ans × 167,72 + 5 ans (50-45) × 167,72 = (16+5) × 167.72 = 3522.12
        assertThat(r.indemniteFermeture())
                .isEqualByComparingTo(new BigDecimal("3522.12"));
        assertThat(r.montantTotalCreancesFfe())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.raison()).isEqualTo("ELIGIBLE_INDEMNITE_EMPLOYEUR_SOLVABLE");
        assertThat(r.synthese())
                .contains("Fermeture qualifiante")
                .contains("Employeur solvable");
    }

    // ── Insolvabilité avec créances → reprise FFE ───────────────────────────

    @Test
    void analyze_faillite_avec_creances_returnsRepriseFfe() {
        LicenciementBeFermetureEntrepriseRequest req = new LicenciementBeFermetureEntrepriseRequest(
                NAISSANCE_1976, DEBUT_CONTRAT_2010, FERMETURE_2026,
                new BigDecimal("3200.00"),
                LicenciementBeFermetureEntrepriseTypeFermeture.FAILLITE_LIQUIDATION_JUDICIAIRE,
                LicenciementBeFermetureEntrepriseStatutEmployeur.FAILLITE_DECLAREE,
                35,
                new BigDecimal("4800.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("9600.00"));

        LicenciementBeFermetureEntrepriseResult r =
                LicenciementBeFermetureEntrepriseCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeFermetureEntrepriseVerdict.ELIGIBLE_FFE_REPRISE_CREANCES);
        assertThat(r.eligible()).isTrue();
        assertThat(r.montantTotalCreancesFfe())
                .isEqualByComparingTo(new BigDecimal("16400.00"));
        assertThat(r.raison()).isEqualTo("ELIGIBLE_FFE_REPRISE_CREANCES");
        assertThat(r.synthese())
                .contains("insolvabilité avérée")
                .contains("16400.00");
    }

    // ── Insolvabilité sans créance impayée → FFE complet ───────────────────

    @Test
    void analyze_insolvable_sansCreance_returnsFfeComplet() {
        LicenciementBeFermetureEntrepriseRequest req = new LicenciementBeFermetureEntrepriseRequest(
                NAISSANCE_1976, DEBUT_CONTRAT_2010, FERMETURE_2026,
                new BigDecimal("3200.00"),
                LicenciementBeFermetureEntrepriseTypeFermeture.CESSATION_DEFINITIVE,
                LicenciementBeFermetureEntrepriseStatutEmployeur.INSOLVABLE_AVERE,
                35,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        LicenciementBeFermetureEntrepriseResult r =
                LicenciementBeFermetureEntrepriseCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeFermetureEntrepriseVerdict.ELIGIBLE_FFE_COMPLET);
        assertThat(r.raison()).isEqualTo("ELIGIBLE_FFE_INDEMNITE_FFE");
        assertThat(r.montantTotalCreancesFfe())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── Type de fermeture non qualifiant ───────────────────────────────────

    @Test
    void analyze_transfertCct32bis_returnsIneligible() {
        LicenciementBeFermetureEntrepriseRequest req = new LicenciementBeFermetureEntrepriseRequest(
                NAISSANCE_1976, DEBUT_CONTRAT_2010, FERMETURE_2026,
                new BigDecimal("3200.00"),
                LicenciementBeFermetureEntrepriseTypeFermeture.TRANSFERT_CCT_32BIS,
                LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE,
                35,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        LicenciementBeFermetureEntrepriseResult r =
                LicenciementBeFermetureEntrepriseCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeFermetureEntrepriseVerdict.INELIGIBLE_TYPE_FERMETURE);
        assertThat(r.eligible()).isFalse();
        assertThat(r.indemniteFermeture()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.raison()).isEqualTo("TYPE_FERMETURE_NON_QUALIFIANT");
        assertThat(r.synthese())
                .contains("transfert d'entreprise CCT 32bis")
                .contains("Renault");
    }

    @Test
    void analyze_cessationPartielle_returnsIneligibleType() {
        LicenciementBeFermetureEntrepriseRequest req = new LicenciementBeFermetureEntrepriseRequest(
                NAISSANCE_1976, DEBUT_CONTRAT_2010, FERMETURE_2026,
                new BigDecimal("3200.00"),
                LicenciementBeFermetureEntrepriseTypeFermeture.CESSATION_PARTIELLE,
                LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE,
                35,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        LicenciementBeFermetureEntrepriseResult r =
                LicenciementBeFermetureEntrepriseCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeFermetureEntrepriseVerdict.INELIGIBLE_TYPE_FERMETURE);
    }

    @Test
    void analyze_fusionSansSuppression_returnsIneligibleType() {
        LicenciementBeFermetureEntrepriseRequest req = new LicenciementBeFermetureEntrepriseRequest(
                NAISSANCE_1976, DEBUT_CONTRAT_2010, FERMETURE_2026,
                new BigDecimal("3200.00"),
                LicenciementBeFermetureEntrepriseTypeFermeture.FUSION_SANS_SUPPRESSION,
                LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE,
                35,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        LicenciementBeFermetureEntrepriseResult r =
                LicenciementBeFermetureEntrepriseCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeFermetureEntrepriseVerdict.INELIGIBLE_TYPE_FERMETURE);
    }

    @Test
    void analyze_fermetureTemporaire_returnsIneligibleType() {
        LicenciementBeFermetureEntrepriseRequest req = new LicenciementBeFermetureEntrepriseRequest(
                NAISSANCE_1976, DEBUT_CONTRAT_2010, FERMETURE_2026,
                new BigDecimal("3200.00"),
                LicenciementBeFermetureEntrepriseTypeFermeture.FERMETURE_TEMPORAIRE,
                LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE,
                35,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        LicenciementBeFermetureEntrepriseResult r =
                LicenciementBeFermetureEntrepriseCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeFermetureEntrepriseVerdict.INELIGIBLE_TYPE_FERMETURE);
    }

    // ── Seuil d'effectif < 20 ETP ──────────────────────────────────────────

    @Test
    void analyze_effectif19_returnsIneligibleSeuilEffectif() {
        LicenciementBeFermetureEntrepriseRequest req = new LicenciementBeFermetureEntrepriseRequest(
                NAISSANCE_1976, DEBUT_CONTRAT_2010, FERMETURE_2026,
                new BigDecimal("3200.00"),
                LicenciementBeFermetureEntrepriseTypeFermeture.CESSATION_DEFINITIVE,
                LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE,
                19,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        LicenciementBeFermetureEntrepriseResult r =
                LicenciementBeFermetureEntrepriseCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeFermetureEntrepriseVerdict.INELIGIBLE_SEUIL_EFFECTIF);
        assertThat(r.raison()).isEqualTo("SEUIL_EFFECTIF_NON_ATTEINT");
        assertThat(r.synthese())
                .contains("19 ETP")
                .contains("AR 23/03/2007");
    }

    @Test
    void analyze_effectifExactement20_returnsEligible() {
        LicenciementBeFermetureEntrepriseRequest req = new LicenciementBeFermetureEntrepriseRequest(
                NAISSANCE_1976, DEBUT_CONTRAT_2010, FERMETURE_2026,
                new BigDecimal("3200.00"),
                LicenciementBeFermetureEntrepriseTypeFermeture.CESSATION_DEFINITIVE,
                LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE,
                20,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        LicenciementBeFermetureEntrepriseResult r =
                LicenciementBeFermetureEntrepriseCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeFermetureEntrepriseVerdict.ELIGIBLE_INDEMNITE_SEULE);
    }

    // ── Ancienneté < 1 an ──────────────────────────────────────────────────

    @Test
    void analyze_anciennete6Mois_returnsIneligibleAnciennete() {
        LicenciementBeFermetureEntrepriseRequest req = new LicenciementBeFermetureEntrepriseRequest(
                NAISSANCE_1976,
                LocalDate.of(2025, 8, 15),
                FERMETURE_2026, // 5 mois
                new BigDecimal("3200.00"),
                LicenciementBeFermetureEntrepriseTypeFermeture.CESSATION_DEFINITIVE,
                LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE,
                35,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        LicenciementBeFermetureEntrepriseResult r =
                LicenciementBeFermetureEntrepriseCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeFermetureEntrepriseVerdict.INELIGIBLE_ANCIENNETE_INSUFFISANTE);
        assertThat(r.raison()).isEqualTo("ANCIENNETE_INSUFFISANTE");
        assertThat(r.anneesAnciennete()).isEqualTo(0);
    }

    // ── Hiérarchie : type prime sur effectif et ancienneté ─────────────────

    @Test
    void analyze_typeNonQualifiantEtEffectifInsuffisantEtAncienneteCourte_typeWins() {
        LicenciementBeFermetureEntrepriseRequest req = new LicenciementBeFermetureEntrepriseRequest(
                NAISSANCE_1976,
                LocalDate.of(2025, 8, 15),
                FERMETURE_2026,
                new BigDecimal("3200.00"),
                LicenciementBeFermetureEntrepriseTypeFermeture.CESSATION_PARTIELLE,
                LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE,
                5,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        LicenciementBeFermetureEntrepriseResult r =
                LicenciementBeFermetureEntrepriseCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeFermetureEntrepriseVerdict.INELIGIBLE_TYPE_FERMETURE);
    }

    @Test
    void analyze_effectifInsuffisantEtAncienneteCourte_effectifWins() {
        LicenciementBeFermetureEntrepriseRequest req = new LicenciementBeFermetureEntrepriseRequest(
                NAISSANCE_1976,
                LocalDate.of(2025, 8, 15),
                FERMETURE_2026,
                new BigDecimal("3200.00"),
                LicenciementBeFermetureEntrepriseTypeFermeture.CESSATION_DEFINITIVE,
                LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE,
                5,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        LicenciementBeFermetureEntrepriseResult r =
                LicenciementBeFermetureEntrepriseCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeFermetureEntrepriseVerdict.INELIGIBLE_SEUIL_EFFECTIF);
    }

    // ── Calcul indemnité : supplément âge ──────────────────────────────────

    @Test
    void analyze_age44_pasDeSupplementAge() {
        LicenciementBeFermetureEntrepriseRequest req = new LicenciementBeFermetureEntrepriseRequest(
                LocalDate.of(1982, 1, 15),   // 44 ans à la fermeture
                DEBUT_CONTRAT_2010,
                FERMETURE_2026,
                new BigDecimal("3200.00"),
                LicenciementBeFermetureEntrepriseTypeFermeture.CESSATION_DEFINITIVE,
                LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE,
                35,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        LicenciementBeFermetureEntrepriseResult r =
                LicenciementBeFermetureEntrepriseCalculator.analyze(req);

        // 16 ans × 167.72 = 2683.52, pas de supplément
        assertThat(r.indemniteFermeture())
                .isEqualByComparingTo(new BigDecimal("2683.52"));
        assertThat(r.synthese()).doesNotContain("Supplément âge");
    }

    @Test
    void analyze_age70_supplementAgePlafonneA20Annees() {
        LicenciementBeFermetureEntrepriseRequest req = new LicenciementBeFermetureEntrepriseRequest(
                LocalDate.of(1956, 1, 15),  // 70 ans à la fermeture
                DEBUT_CONTRAT_2010,
                FERMETURE_2026,
                new BigDecimal("3200.00"),
                LicenciementBeFermetureEntrepriseTypeFermeture.CESSATION_DEFINITIVE,
                LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE,
                35,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        LicenciementBeFermetureEntrepriseResult r =
                LicenciementBeFermetureEntrepriseCalculator.analyze(req);

        // 16 ans ancienneté + 20 ans supplément max (plafond 65) = 36 × 167.72
        assertThat(r.indemniteFermeture())
                .isEqualByComparingTo(new BigDecimal("6037.92"));
    }

    // ── Plafond ancienneté à 25 années ──────────────────────────────────────

    @Test
    void analyze_anciennete30Ans_plafonneeA25() {
        LicenciementBeFermetureEntrepriseRequest req = new LicenciementBeFermetureEntrepriseRequest(
                LocalDate.of(1966, 1, 15),  // 60 ans à la fermeture
                LocalDate.of(1995, 1, 15),  // 31 ans d'ancienneté
                FERMETURE_2026,
                new BigDecimal("3200.00"),
                LicenciementBeFermetureEntrepriseTypeFermeture.CESSATION_DEFINITIVE,
                LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE,
                35,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        LicenciementBeFermetureEntrepriseResult r =
                LicenciementBeFermetureEntrepriseCalculator.analyze(req);

        // 25 plafond × 167.72 + 15 × 167.72 (60-45 supplément) = 40 × 167.72
        assertThat(r.anneesAnciennete()).isEqualTo(31);
        assertThat(r.indemniteFermeture())
                .isEqualByComparingTo(new BigDecimal("6708.80"));
    }

    // ── Snapshot inputs ─────────────────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        LicenciementBeFermetureEntrepriseResult r =
                LicenciementBeFermetureEntrepriseCalculator.analyze(reqBase());

        assertThat(r.dateNaissance()).isEqualTo(NAISSANCE_1976);
        assertThat(r.dateDebutContrat()).isEqualTo(DEBUT_CONTRAT_2010);
        assertThat(r.dateFermeture()).isEqualTo(FERMETURE_2026);
        assertThat(r.remunerationMensuelleBrute())
                .isEqualByComparingTo(new BigDecimal("3200.00"));
        assertThat(r.typeFermeture())
                .isEqualTo(LicenciementBeFermetureEntrepriseTypeFermeture.CESSATION_DEFINITIVE);
        assertThat(r.statutEmployeur())
                .isEqualTo(LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE);
        assertThat(r.effectifEtp()).isEqualTo(35);
    }

    // ── Base juridique + avertissement ──────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceLoi260602002_AR230307_CCT9bis() {
        LicenciementBeFermetureEntrepriseResult r =
                LicenciementBeFermetureEntrepriseCalculator.analyze(reqBase());

        assertThat(r.baseJuridique())
                .contains("26/06/2002")
                .contains("AR du 23/03/2007")
                .contains("CCT n° 9bis")
                .contains("FFE");
    }

    @Test
    void analyze_avertissementSystematiquePresent() {
        LicenciementBeFermetureEntrepriseResult r =
                LicenciementBeFermetureEntrepriseCalculator.analyze(reqBase());

        assertThat(r.avertissement())
                .isNotNull()
                .contains("indicatifs")
                .contains("FFE");
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() ->
                LicenciementBeFermetureEntrepriseCalculator.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_dateNaissanceNull_throws() {
        LicenciementBeFermetureEntrepriseRequest bad = new LicenciementBeFermetureEntrepriseRequest(
                null, DEBUT_CONTRAT_2010, FERMETURE_2026,
                new BigDecimal("3200"),
                LicenciementBeFermetureEntrepriseTypeFermeture.CESSATION_DEFINITIVE,
                LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE,
                35, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThatThrownBy(() ->
                LicenciementBeFermetureEntrepriseCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNaissance");
    }

    @Test
    void analyze_dateFermetureAvantNaissance_throws() {
        LicenciementBeFermetureEntrepriseRequest bad = new LicenciementBeFermetureEntrepriseRequest(
                LocalDate.of(2030, 1, 15),
                DEBUT_CONTRAT_2010,
                FERMETURE_2026,
                new BigDecimal("3200"),
                LicenciementBeFermetureEntrepriseTypeFermeture.CESSATION_DEFINITIVE,
                LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE,
                35, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThatThrownBy(() ->
                LicenciementBeFermetureEntrepriseCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateFermeture");
    }

    @Test
    void analyze_dateFermetureAvantDebutContrat_throws() {
        LicenciementBeFermetureEntrepriseRequest bad = new LicenciementBeFermetureEntrepriseRequest(
                NAISSANCE_1976,
                LocalDate.of(2030, 1, 15),
                FERMETURE_2026,
                new BigDecimal("3200"),
                LicenciementBeFermetureEntrepriseTypeFermeture.CESSATION_DEFINITIVE,
                LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE,
                35, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThatThrownBy(() ->
                LicenciementBeFermetureEntrepriseCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateFermeture");
    }

    @Test
    void analyze_typeFermetureNull_throws() {
        LicenciementBeFermetureEntrepriseRequest bad = new LicenciementBeFermetureEntrepriseRequest(
                NAISSANCE_1976, DEBUT_CONTRAT_2010, FERMETURE_2026,
                new BigDecimal("3200"),
                null,
                LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE,
                35, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThatThrownBy(() ->
                LicenciementBeFermetureEntrepriseCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeFermeture");
    }

    @Test
    void analyze_statutEmployeurNull_throws() {
        LicenciementBeFermetureEntrepriseRequest bad = new LicenciementBeFermetureEntrepriseRequest(
                NAISSANCE_1976, DEBUT_CONTRAT_2010, FERMETURE_2026,
                new BigDecimal("3200"),
                LicenciementBeFermetureEntrepriseTypeFermeture.CESSATION_DEFINITIVE,
                null,
                35, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThatThrownBy(() ->
                LicenciementBeFermetureEntrepriseCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("statutEmployeur");
    }

    @Test
    void analyze_effectifNegatif_throws() {
        LicenciementBeFermetureEntrepriseRequest bad = new LicenciementBeFermetureEntrepriseRequest(
                NAISSANCE_1976, DEBUT_CONTRAT_2010, FERMETURE_2026,
                new BigDecimal("3200"),
                LicenciementBeFermetureEntrepriseTypeFermeture.CESSATION_DEFINITIVE,
                LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE,
                -1, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThatThrownBy(() ->
                LicenciementBeFermetureEntrepriseCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectifEtp");
    }

    @Test
    void analyze_remunerationNegative_throws() {
        LicenciementBeFermetureEntrepriseRequest bad = new LicenciementBeFermetureEntrepriseRequest(
                NAISSANCE_1976, DEBUT_CONTRAT_2010, FERMETURE_2026,
                new BigDecimal("-100"),
                LicenciementBeFermetureEntrepriseTypeFermeture.CESSATION_DEFINITIVE,
                LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE,
                35, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThatThrownBy(() ->
                LicenciementBeFermetureEntrepriseCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remunerationMensuelleBrute");
    }

    @Test
    void analyze_salairesImpayesNegatif_throws() {
        LicenciementBeFermetureEntrepriseRequest bad = new LicenciementBeFermetureEntrepriseRequest(
                NAISSANCE_1976, DEBUT_CONTRAT_2010, FERMETURE_2026,
                new BigDecimal("3200"),
                LicenciementBeFermetureEntrepriseTypeFermeture.CESSATION_DEFINITIVE,
                LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE,
                35,
                new BigDecimal("-50"),
                BigDecimal.ZERO, BigDecimal.ZERO);

        assertThatThrownBy(() ->
                LicenciementBeFermetureEntrepriseCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salairesImpayes");
    }

    @Test
    void analyze_peculeImpayeNegatif_throws() {
        LicenciementBeFermetureEntrepriseRequest bad = new LicenciementBeFermetureEntrepriseRequest(
                NAISSANCE_1976, DEBUT_CONTRAT_2010, FERMETURE_2026,
                new BigDecimal("3200"),
                LicenciementBeFermetureEntrepriseTypeFermeture.CESSATION_DEFINITIVE,
                LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE,
                35,
                BigDecimal.ZERO,
                new BigDecimal("-1"),
                BigDecimal.ZERO);

        assertThatThrownBy(() ->
                LicenciementBeFermetureEntrepriseCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("peculeVacancesImpaye");
    }

    @Test
    void analyze_indemniteRuptureImpayeeNegative_throws() {
        LicenciementBeFermetureEntrepriseRequest bad = new LicenciementBeFermetureEntrepriseRequest(
                NAISSANCE_1976, DEBUT_CONTRAT_2010, FERMETURE_2026,
                new BigDecimal("3200"),
                LicenciementBeFermetureEntrepriseTypeFermeture.CESSATION_DEFINITIVE,
                LicenciementBeFermetureEntrepriseStatutEmployeur.SOLVABLE,
                35,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("-1"));

        assertThatThrownBy(() ->
                LicenciementBeFermetureEntrepriseCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("indemniteRuptureImpayee");
    }
}
