package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-21 : tests unitaires du
 * {@link EcoChequesChequesRepasBeCalculator}.
 *
 * <p>Couvre : calculs (plafond éco-chèques 250 EUR/an art. 6 CCT n°98,
 * plafond chèques-repas prorata jours × 8 EUR art. 19bis §2 8° AR
 * 28/11/1969, dépassement plafond / requalification), conditions
 * cumulatives (CCT/convention écrite, paiement électronique post 2016,
 * contribution travailleur min 1,09 EUR, cumul frais de bouche
 * interdit, jour effectivement presté), substitution à rémunération
 * (Cass. 16/10/2017 S.16.0042.F + art. 4 CCT n°98), estimation
 * cotisations ONSS 25 %, base juridique stable, validation des
 * inputs.</p>
 */
class EcoChequesChequesRepasBeCalculatorTest {

    private static final BigDecimal MONTANT_240 = new BigDecimal("240.00");
    private static final BigDecimal MONTANT_250 = new BigDecimal("250.00");
    private static final BigDecimal MONTANT_265 = new BigDecimal("265.00");
    private static final BigDecimal MONTANT_500 = new BigDecimal("500.00");
    private static final BigDecimal VALEUR_FACIALE_8 = new BigDecimal("8.00");
    private static final BigDecimal VALEUR_FACIALE_10 = new BigDecimal("10.00");
    private static final BigDecimal CONTRIB_1_09 = new BigDecimal("1.09");
    private static final BigDecimal CONTRIB_0_50 = new BigDecimal("0.50");
    private static final LocalDate DATE_2024 = LocalDate.of(2024, 6, 1);
    private static final LocalDate DATE_2015 = LocalDate.of(2015, 6, 1);

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static EcoChequesChequesRepasBeRequest reqEcoChequesConforme() {
        return new EcoChequesChequesRepasBeRequest(
                EcoChequesChequesRepasBeTypeAvantage.ECO_CHEQUES,
                MONTANT_240, // ≤ 250 EUR
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                0,
                true, // CCT sectorielle
                false,
                false, // paiement non électronique ok pour éco-chèques
                false,
                false, // pas de substitution
                DATE_2024);
    }

    private static EcoChequesChequesRepasBeRequest reqChequesRepasConforme() {
        return new EcoChequesChequesRepasBeRequest(
                EcoChequesChequesRepasBeTypeAvantage.CHEQUES_REPAS,
                new BigDecimal("1380.00"), // 220 jours × 6,91 < 220 × 8 = 1760
                VALEUR_FACIALE_8,
                CONTRIB_1_09,
                220, // jours
                true,
                false,
                true, // paiement électronique
                false, // pas de cumul frais bouche
                false,
                DATE_2024);
    }

    // ── Cas nominaux ────────────────────────────────────────────────────────

    @Test
    void analyze_ecoCheques_montantInferieurPlafond_conformeIntegrale() {
        EcoChequesChequesRepasBeResult r = EcoChequesChequesRepasBeCalculator
                .analyze(reqEcoChequesConforme());

        assertThat(r.verdict())
                .isEqualTo(EcoChequesChequesRepasBeVerdict
                        .CONFORME_EXONERATION_INTEGRALE);
        assertThat(r.plafondLegalApplicableEur())
                .isEqualByComparingTo("250.00");
        assertThat(r.montantExonereEur()).isEqualByComparingTo("240.00");
        assertThat(r.montantRequalifieEnRemunerationEur())
                .isEqualByComparingTo("0.00");
        assertThat(r.cotisationsOnssEstimeesEur())
                .isEqualByComparingTo("0.00");
        assertThat(r.raison()).isEqualTo("CONFORME_INTEGRALE");
    }

    @Test
    void analyze_ecoCheques_montantEgalPlafond_conformeIntegrale() {
        EcoChequesChequesRepasBeRequest req =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.ECO_CHEQUES,
                        MONTANT_250, // = plafond
                        new BigDecimal("10.00"),
                        BigDecimal.ZERO, 0,
                        true, false, false, false, false, DATE_2024);

        EcoChequesChequesRepasBeResult r =
                EcoChequesChequesRepasBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(EcoChequesChequesRepasBeVerdict
                        .CONFORME_EXONERATION_INTEGRALE);
        assertThat(r.montantExonereEur()).isEqualByComparingTo("250.00");
    }

    @Test
    void analyze_ecoCheques_depassementFaible_conformePartiellement() {
        // 265 - 250 = 15 < 10 % du plafond (25) → CONFORME_PARTIELLEMENT
        EcoChequesChequesRepasBeRequest req =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.ECO_CHEQUES,
                        MONTANT_265,
                        new BigDecimal("10.00"),
                        BigDecimal.ZERO, 0,
                        true, false, false, false, false, DATE_2024);

        EcoChequesChequesRepasBeResult r =
                EcoChequesChequesRepasBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(EcoChequesChequesRepasBeVerdict
                        .CONFORME_PARTIELLEMENT_EXONERE);
        assertThat(r.montantExonereEur()).isEqualByComparingTo("250.00");
        assertThat(r.montantRequalifieEnRemunerationEur())
                .isEqualByComparingTo("15.00");
        // 15 × 25 % = 3,75
        assertThat(r.cotisationsOnssEstimeesEur())
                .isEqualByComparingTo("3.75");
        assertThat(r.raison())
                .isEqualTo("DEPASSEMENT_FAIBLE_PARTIELLEMENT_EXONERE");
    }

    @Test
    void analyze_ecoCheques_depassementStructurel_nonConforme() {
        // 500 - 250 = 250 > 10 % du plafond (25) → NON_CONFORME
        EcoChequesChequesRepasBeRequest req =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.ECO_CHEQUES,
                        MONTANT_500,
                        new BigDecimal("10.00"),
                        BigDecimal.ZERO, 0,
                        true, false, false, false, false, DATE_2024);

        EcoChequesChequesRepasBeResult r =
                EcoChequesChequesRepasBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(EcoChequesChequesRepasBeVerdict
                        .NON_CONFORME_DEPASSEMENT_PLAFOND);
        assertThat(r.montantExonereEur()).isEqualByComparingTo("250.00");
        assertThat(r.montantRequalifieEnRemunerationEur())
                .isEqualByComparingTo("250.00");
        assertThat(r.cotisationsOnssEstimeesEur())
                .isEqualByComparingTo("62.50");
        assertThat(r.raison()).isEqualTo("DEPASSEMENT_PLAFOND_LEGAL");
    }

    @Test
    void analyze_chequesRepas_montantInferieurPlafond_conformeIntegrale() {
        EcoChequesChequesRepasBeResult r =
                EcoChequesChequesRepasBeCalculator
                        .analyze(reqChequesRepasConforme());

        assertThat(r.verdict())
                .isEqualTo(EcoChequesChequesRepasBeVerdict
                        .CONFORME_EXONERATION_INTEGRALE);
        // plafond = 220 j × 8 = 1760
        assertThat(r.plafondLegalApplicableEur())
                .isEqualByComparingTo("1760.00");
    }

    // ── Conditions cumulatives ──────────────────────────────────────────────

    @Test
    void analyze_ecoCheques_absenceCctEtConvention_nonConforme() {
        EcoChequesChequesRepasBeRequest req =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.ECO_CHEQUES,
                        MONTANT_240,
                        new BigDecimal("10.00"),
                        BigDecimal.ZERO, 0,
                        false, // pas de CCT
                        false, // pas de convention
                        false, false, false, DATE_2024);

        EcoChequesChequesRepasBeResult r =
                EcoChequesChequesRepasBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(EcoChequesChequesRepasBeVerdict
                        .NON_CONFORME_CONDITION_MANQUANTE);
        assertThat(r.raison())
                .isEqualTo("ABSENCE_CCT_OU_CONVENTION_ECRITE");
        // Montant intégralement requalifié + cotisations estimées
        assertThat(r.montantRequalifieEnRemunerationEur())
                .isEqualByComparingTo("240.00");
        assertThat(r.cotisationsOnssEstimeesEur())
                .isEqualByComparingTo("60.00");
    }

    @Test
    void analyze_ecoCheques_conventionIndividuelleEcriteSeule_conforme() {
        EcoChequesChequesRepasBeRequest req =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.ECO_CHEQUES,
                        MONTANT_240,
                        new BigDecimal("10.00"),
                        BigDecimal.ZERO, 0,
                        false, // pas de CCT
                        true, // mais convention individuelle écrite
                        false, false, false, DATE_2024);

        EcoChequesChequesRepasBeResult r =
                EcoChequesChequesRepasBeCalculator.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(EcoChequesChequesRepasBeVerdict
                        .CONFORME_EXONERATION_INTEGRALE);
    }

    @Test
    void analyze_chequesRepas_paiementNonElectroniquePost2016_nonConforme() {
        EcoChequesChequesRepasBeRequest req =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.CHEQUES_REPAS,
                        new BigDecimal("1380.00"),
                        VALEUR_FACIALE_8,
                        CONTRIB_1_09,
                        220,
                        true, false,
                        false, // pas électronique
                        false, false,
                        DATE_2024); // post 01/01/2016

        EcoChequesChequesRepasBeResult r =
                EcoChequesChequesRepasBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(EcoChequesChequesRepasBeVerdict
                        .NON_CONFORME_CONDITION_MANQUANTE);
        assertThat(r.raison())
                .isEqualTo("PAIEMENT_NON_ELECTRONIQUE_POST_2016");
    }

    @Test
    void analyze_chequesRepas_paiementPapierPre2016_toleresAnsConditionsBlocantes() {
        EcoChequesChequesRepasBeRequest req =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.CHEQUES_REPAS,
                        new BigDecimal("1380.00"),
                        VALEUR_FACIALE_8,
                        CONTRIB_1_09,
                        220,
                        true, false,
                        false, // pas électronique
                        false, false,
                        DATE_2015); // pré 01/01/2016 → toléré

        EcoChequesChequesRepasBeResult r =
                EcoChequesChequesRepasBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(EcoChequesChequesRepasBeVerdict
                        .CONFORME_EXONERATION_INTEGRALE);
    }

    @Test
    void analyze_chequesRepas_cumulFraisBouche_nonConforme() {
        EcoChequesChequesRepasBeRequest req =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.CHEQUES_REPAS,
                        new BigDecimal("1380.00"),
                        VALEUR_FACIALE_8,
                        CONTRIB_1_09,
                        220,
                        true, false, true,
                        true, // cumul frais bouche
                        false, DATE_2024);

        EcoChequesChequesRepasBeResult r =
                EcoChequesChequesRepasBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(EcoChequesChequesRepasBeVerdict
                        .NON_CONFORME_CONDITION_MANQUANTE);
        assertThat(r.raison()).isEqualTo("CUMUL_FRAIS_BOUCHE_INTERDIT");
    }

    @Test
    void analyze_chequesRepas_contributionTravailleurInsuffisante_nonConforme() {
        EcoChequesChequesRepasBeRequest req =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.CHEQUES_REPAS,
                        new BigDecimal("1380.00"),
                        VALEUR_FACIALE_8,
                        CONTRIB_0_50, // < 1,09
                        220,
                        true, false, true, false, false, DATE_2024);

        EcoChequesChequesRepasBeResult r =
                EcoChequesChequesRepasBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(EcoChequesChequesRepasBeVerdict
                        .NON_CONFORME_CONDITION_MANQUANTE);
        assertThat(r.raison())
                .isEqualTo("CONTRIBUTION_TRAVAILLEUR_INSUFFISANTE");
    }

    @Test
    void analyze_chequesRepas_valeurFacialeDepasse8_nonConforme() {
        EcoChequesChequesRepasBeRequest req =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.CHEQUES_REPAS,
                        new BigDecimal("1500.00"),
                        VALEUR_FACIALE_10, // > 8
                        CONTRIB_1_09,
                        220,
                        true, false, true, false, false, DATE_2024);

        EcoChequesChequesRepasBeResult r =
                EcoChequesChequesRepasBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(EcoChequesChequesRepasBeVerdict
                        .NON_CONFORME_CONDITION_MANQUANTE);
        assertThat(r.raison()).isEqualTo("VALEUR_FACIALE_DEPASSE_8_EUR");
    }

    @Test
    void analyze_chequesRepas_aucunJourPreste_nonConforme() {
        EcoChequesChequesRepasBeRequest req =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.CHEQUES_REPAS,
                        new BigDecimal("100.00"),
                        VALEUR_FACIALE_8,
                        CONTRIB_1_09,
                        0, // pas de jour presté
                        true, false, true, false, false, DATE_2024);

        EcoChequesChequesRepasBeResult r =
                EcoChequesChequesRepasBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(EcoChequesChequesRepasBeVerdict
                        .NON_CONFORME_CONDITION_MANQUANTE);
        assertThat(r.raison())
                .isEqualTo("AUCUN_JOUR_EFFECTIVEMENT_PRESTE");
    }

    // ── Substitution à rémunération (priorité absolue) ──────────────────────

    @Test
    void analyze_substitutionRemuneration_returnsNonConformeSubstitution() {
        EcoChequesChequesRepasBeRequest req =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.ECO_CHEQUES,
                        MONTANT_240, new BigDecimal("10.00"),
                        BigDecimal.ZERO, 0,
                        true, false, false, false,
                        true, // substitution
                        DATE_2024);

        EcoChequesChequesRepasBeResult r =
                EcoChequesChequesRepasBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(EcoChequesChequesRepasBeVerdict
                        .NON_CONFORME_SUBSTITUTION_REMUNERATION);
        assertThat(r.raison()).isEqualTo("SUBSTITUTION_REMUNERATION");
        // Montant intégralement requalifié
        assertThat(r.montantRequalifieEnRemunerationEur())
                .isEqualByComparingTo("240.00");
        assertThat(r.cotisationsOnssEstimeesEur())
                .isEqualByComparingTo("60.00");
        assertThat(r.synthese()).contains("Cass. 16/10/2017", "art. 4 CCT");
    }

    @Test
    void analyze_substitutionPrimeAbsenceCct() {
        // Substitution prime même sans CCT (le contrôle substitution
        // est en hiérarchie 2 — avant les conditions cumulatives).
        EcoChequesChequesRepasBeRequest req =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.ECO_CHEQUES,
                        MONTANT_240, new BigDecimal("10.00"),
                        BigDecimal.ZERO, 0,
                        false, false, false, false,
                        true, // substitution
                        DATE_2024);

        EcoChequesChequesRepasBeResult r =
                EcoChequesChequesRepasBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(EcoChequesChequesRepasBeVerdict
                        .NON_CONFORME_SUBSTITUTION_REMUNERATION);
    }

    // ── Type indéterminé ────────────────────────────────────────────────────

    @Test
    void analyze_typeIndetermine_returnsAanalyser() {
        EcoChequesChequesRepasBeRequest req =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.INDETERMINE,
                        MONTANT_240, new BigDecimal("10.00"),
                        BigDecimal.ZERO, 0,
                        true, false, false, false, false, DATE_2024);

        EcoChequesChequesRepasBeResult r =
                EcoChequesChequesRepasBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(EcoChequesChequesRepasBeVerdict.A_ANALYSER);
        assertThat(r.raison()).isEqualTo("TYPE_AVANTAGE_INDETERMINE");
    }

    @Test
    void analyze_typeIndeterminePrimePartoutSubstitution() {
        // Hiérarchie : INDETERMINE est en hiérarchie 1, donc prime même
        // sur substitution.
        EcoChequesChequesRepasBeRequest req =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.INDETERMINE,
                        MONTANT_240, new BigDecimal("10.00"),
                        BigDecimal.ZERO, 0,
                        true, false, false, false, true, DATE_2024);

        EcoChequesChequesRepasBeResult r =
                EcoChequesChequesRepasBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(EcoChequesChequesRepasBeVerdict.A_ANALYSER);
    }

    // ── Base juridique + avertissement ──────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceCctEtAr() {
        EcoChequesChequesRepasBeResult r =
                EcoChequesChequesRepasBeCalculator
                        .analyze(reqEcoChequesConforme());

        assertThat(r.baseJuridique())
                .contains("CCT n°98")
                .contains("20/02/2009")
                .contains("250 EUR")
                .contains("Loi du 25/04/2014")
                .contains("AR du 03/02/2010")
                .contains("art. 19bis")
                .contains("6,91 EUR")
                .contains("1,09 EUR")
                .contains("8 EUR")
                .contains("Cass. 16/10/2017")
                .contains("art. 4 CCT n°98");
    }

    @Test
    void analyze_avertissementSystematique() {
        EcoChequesChequesRepasBeResult r =
                EcoChequesChequesRepasBeCalculator
                        .analyze(reqEcoChequesConforme());

        assertThat(r.avertissement())
                .isNotNull()
                .contains("avocat")
                .contains("ONSS")
                .contains("substitution")
                .contains("01/01/2016");
    }

    // ── Snapshot ────────────────────────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        EcoChequesChequesRepasBeResult r =
                EcoChequesChequesRepasBeCalculator
                        .analyze(reqChequesRepasConforme());

        assertThat(r.typeAvantage())
                .isEqualTo(EcoChequesChequesRepasBeTypeAvantage
                        .CHEQUES_REPAS);
        assertThat(r.montantAnnuelEur())
                .isEqualByComparingTo("1380.00");
        assertThat(r.valeurFacialeUnitaireEur())
                .isEqualByComparingTo("8.00");
        assertThat(r.contributionTravailleurUnitaireEur())
                .isEqualByComparingTo("1.09");
        assertThat(r.joursEffectivementPrestes()).isEqualTo(220);
        assertThat(r.cctSectorielleOuEntrepriseExiste()).isTrue();
        assertThat(r.conventionIndividuelleEcrite()).isFalse();
        assertThat(r.paiementElectronique()).isTrue();
        assertThat(r.cumulFraisBouche()).isFalse();
        assertThat(r.substitutionRemuneration()).isFalse();
        assertThat(r.dateAttribution()).isEqualTo(DATE_2024);
    }

    // ── Validation ──────────────────────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() ->
                EcoChequesChequesRepasBeCalculator.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_typeAvantageNull_throws() {
        EcoChequesChequesRepasBeRequest bad =
                new EcoChequesChequesRepasBeRequest(
                        null, MONTANT_240, new BigDecimal("10.00"),
                        BigDecimal.ZERO, 0,
                        true, false, false, false, false, DATE_2024);

        assertThatThrownBy(() ->
                EcoChequesChequesRepasBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeAvantage");
    }

    @Test
    void analyze_montantNegatif_throws() {
        EcoChequesChequesRepasBeRequest bad =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.ECO_CHEQUES,
                        new BigDecimal("-1.00"), new BigDecimal("10.00"),
                        BigDecimal.ZERO, 0,
                        true, false, false, false, false, DATE_2024);

        assertThatThrownBy(() ->
                EcoChequesChequesRepasBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("montantAnnuelEur");
    }

    @Test
    void analyze_valeurFacialeNegative_throws() {
        EcoChequesChequesRepasBeRequest bad =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.ECO_CHEQUES,
                        MONTANT_240, new BigDecimal("-1.00"),
                        BigDecimal.ZERO, 0,
                        true, false, false, false, false, DATE_2024);

        assertThatThrownBy(() ->
                EcoChequesChequesRepasBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valeurFacialeUnitaireEur");
    }

    @Test
    void analyze_contributionTravailleurNegative_throws() {
        EcoChequesChequesRepasBeRequest bad =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.ECO_CHEQUES,
                        MONTANT_240, new BigDecimal("10.00"),
                        new BigDecimal("-1.00"), 0,
                        true, false, false, false, false, DATE_2024);

        assertThatThrownBy(() ->
                EcoChequesChequesRepasBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contributionTravailleurUnitaireEur");
    }

    @Test
    void analyze_joursNegatif_throws() {
        EcoChequesChequesRepasBeRequest bad =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.ECO_CHEQUES,
                        MONTANT_240, new BigDecimal("10.00"),
                        BigDecimal.ZERO, -1,
                        true, false, false, false, false, DATE_2024);

        assertThatThrownBy(() ->
                EcoChequesChequesRepasBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("joursEffectivementPrestes");
    }

    @Test
    void analyze_cctSectorielleNull_throws() {
        EcoChequesChequesRepasBeRequest bad =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.ECO_CHEQUES,
                        MONTANT_240, new BigDecimal("10.00"),
                        BigDecimal.ZERO, 0,
                        null, false, false, false, false, DATE_2024);

        assertThatThrownBy(() ->
                EcoChequesChequesRepasBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cctSectorielleOuEntrepriseExiste");
    }

    @Test
    void analyze_conventionIndividuelleNull_throws() {
        EcoChequesChequesRepasBeRequest bad =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.ECO_CHEQUES,
                        MONTANT_240, new BigDecimal("10.00"),
                        BigDecimal.ZERO, 0,
                        true, null, false, false, false, DATE_2024);

        assertThatThrownBy(() ->
                EcoChequesChequesRepasBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conventionIndividuelleEcrite");
    }

    @Test
    void analyze_paiementElectroniqueNull_throws() {
        EcoChequesChequesRepasBeRequest bad =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.ECO_CHEQUES,
                        MONTANT_240, new BigDecimal("10.00"),
                        BigDecimal.ZERO, 0,
                        true, false, null, false, false, DATE_2024);

        assertThatThrownBy(() ->
                EcoChequesChequesRepasBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("paiementElectronique");
    }

    @Test
    void analyze_cumulFraisBoucheNull_throws() {
        EcoChequesChequesRepasBeRequest bad =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.ECO_CHEQUES,
                        MONTANT_240, new BigDecimal("10.00"),
                        BigDecimal.ZERO, 0,
                        true, false, false, null, false, DATE_2024);

        assertThatThrownBy(() ->
                EcoChequesChequesRepasBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cumulFraisBouche");
    }

    @Test
    void analyze_substitutionRemunerationNull_throws() {
        EcoChequesChequesRepasBeRequest bad =
                new EcoChequesChequesRepasBeRequest(
                        EcoChequesChequesRepasBeTypeAvantage.ECO_CHEQUES,
                        MONTANT_240, new BigDecimal("10.00"),
                        BigDecimal.ZERO, 0,
                        true, false, false, false, null, DATE_2024);

        assertThatThrownBy(() ->
                EcoChequesChequesRepasBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("substitutionRemuneration");
    }
}
