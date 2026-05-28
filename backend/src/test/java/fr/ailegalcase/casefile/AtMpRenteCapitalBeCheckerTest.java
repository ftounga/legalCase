package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-29 : tests unitaires du {@link AtMpRenteCapitalBeChecker}.
 *
 * <p>Couvre : 5 verdicts (CAPITAL_FORFAITAIRE_LT_19 / RENTE_ANNUELLE_GE_19
 * / INELIGIBLE_NON_RECONNU / IPP_NON_DETERMINE / A_QUALIFIER), seuil
 * 19% strict, calcul rente annuelle = remuneration x IPP / 100,
 * coefficient d'age interpole Table I-bis AR 24/02/2005, capital de
 * capitalisation = rente x coefficient (cas IPP &lt; 19), conversion
 * partielle 1/3 max apres delai d'epreuve 3 ans art. 45ter, base
 * juridique stable, avertissement avocat BE, validation inputs.</p>
 */
class AtMpRenteCapitalBeCheckerTest {

    private static final LocalDate DATE_NAISSANCE =
            LocalDate.of(1979, 3, 15);
    private static final LocalDate DATE_CONSOLIDATION =
            LocalDate.of(2024, 6, 1);
    private static final BigDecimal REMUNERATION =
            new BigDecimal("45000.00");

    private static AtMpRenteCapitalBeRequest reqIppFaible() {
        return new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL,
                AtMpRenteCapitalBeStatutReconnaissance.RECONNU,
                DATE_CONSOLIDATION,
                new BigDecimal("10"),
                REMUNERATION,
                DATE_NAISSANCE,
                false, null, null);
    }

    private static AtMpRenteCapitalBeRequest reqIppEleve() {
        return new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.MALADIE_PROFESSIONNELLE,
                AtMpRenteCapitalBeStatutReconnaissance.RECONNU,
                DATE_CONSOLIDATION,
                new BigDecimal("35"),
                REMUNERATION,
                DATE_NAISSANCE,
                false, null, null);
    }

    // == Branche CAPITAL_FORFAITAIRE_LT_19 ==

    @Test
    void analyze_ippInferieur19_returnsCapitalForfaitaire() {
        AtMpRenteCapitalBeResult r =
                AtMpRenteCapitalBeChecker.analyze(reqIppFaible());
        assertThat(r.verdict())
                .isEqualTo(AtMpRenteCapitalBeVerdict.CAPITAL_FORFAITAIRE_LT_19);
        assertThat(r.capitalisationDoffice()).isTrue();
        assertThat(r.conversionPartiellePossible()).isFalse();
        assertThat(r.renteAnnuelle()).isEqualByComparingTo("4500.00");
        // capital = rente x coefficient d'age (45 ans : 9,0)
        assertThat(r.capitalCapitalisation()).isNotNull();
        assertThat(r.capitalCapitalisation())
                .isGreaterThan(BigDecimal.ZERO);
        assertThat(r.raison())
                .isEqualTo("IPP_LT_19_CAPITALISATION_OFFICE_ART_24_AL_2");
    }

    @Test
    void analyze_ipp18_99_returnsCapitalForfaitaire_seuilStrict() {
        // 18.99 < 19 → capital
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL,
                AtMpRenteCapitalBeStatutReconnaissance.RECONNU,
                DATE_CONSOLIDATION,
                new BigDecimal("18.99"),
                REMUNERATION,
                DATE_NAISSANCE,
                false, null, null);
        AtMpRenteCapitalBeResult r =
                AtMpRenteCapitalBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(AtMpRenteCapitalBeVerdict.CAPITAL_FORFAITAIRE_LT_19);
    }

    @Test
    void analyze_ipp19_returnsRenteAnnuelle_seuilEgal() {
        // 19 >= 19 → rente
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL,
                AtMpRenteCapitalBeStatutReconnaissance.RECONNU,
                DATE_CONSOLIDATION,
                new BigDecimal("19"),
                REMUNERATION,
                DATE_NAISSANCE,
                false, null, null);
        AtMpRenteCapitalBeResult r =
                AtMpRenteCapitalBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(AtMpRenteCapitalBeVerdict.RENTE_ANNUELLE_GE_19);
        assertThat(r.capitalisationDoffice()).isFalse();
    }

    // == Branche RENTE_ANNUELLE_GE_19 ==

    @Test
    void analyze_ippSuperieurOuEgal19_returnsRenteAnnuelle() {
        AtMpRenteCapitalBeResult r =
                AtMpRenteCapitalBeChecker.analyze(reqIppEleve());
        assertThat(r.verdict())
                .isEqualTo(AtMpRenteCapitalBeVerdict.RENTE_ANNUELLE_GE_19);
        assertThat(r.capitalisationDoffice()).isFalse();
        // rente = 45000 x 35 / 100 = 15750
        assertThat(r.renteAnnuelle()).isEqualByComparingTo("15750.00");
        assertThat(r.capitalCapitalisation()).isNull();
        assertThat(r.raison())
                .isEqualTo("IPP_GE_19_RENTE_VIAGERE_ART_24_AL_1");
        assertThat(r.synthese()).contains("RENTE ANNUELLE VIAGERE");
    }

    @Test
    void analyze_renteIpp100_calculeRemunerationTotale() {
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL,
                AtMpRenteCapitalBeStatutReconnaissance.RECONNU,
                DATE_CONSOLIDATION,
                new BigDecimal("100"),
                REMUNERATION,
                DATE_NAISSANCE,
                false, null, null);
        AtMpRenteCapitalBeResult r =
                AtMpRenteCapitalBeChecker.analyze(req);
        assertThat(r.renteAnnuelle()).isEqualByComparingTo("45000.00");
    }

    // == Conversion partielle ==

    @Test
    void analyze_conversionDemandee_delaiNonEcoule_conversionImpossible() {
        // consolidation 2024-06-01, demande 2025-01-01 → < 3 ans
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL,
                AtMpRenteCapitalBeStatutReconnaissance.RECONNU,
                DATE_CONSOLIDATION,
                new BigDecimal("40"),
                REMUNERATION,
                DATE_NAISSANCE,
                true, LocalDate.of(2025, 1, 1), null);
        AtMpRenteCapitalBeResult r =
                AtMpRenteCapitalBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(AtMpRenteCapitalBeVerdict.RENTE_ANNUELLE_GE_19);
        assertThat(r.conversionPartiellePossible()).isFalse();
        assertThat(r.capitalConversionPartielle()).isNull();
        assertThat(r.synthese()).contains("delai d'epreuve");
    }

    @Test
    void analyze_conversionDemandee_delaiEcoule_conversionPossible() {
        // consolidation 2024-06-01, demande 2027-07-01 → > 3 ans
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL,
                AtMpRenteCapitalBeStatutReconnaissance.RECONNU,
                DATE_CONSOLIDATION,
                new BigDecimal("40"),
                REMUNERATION,
                DATE_NAISSANCE,
                true, LocalDate.of(2027, 7, 1), null);
        AtMpRenteCapitalBeResult r =
                AtMpRenteCapitalBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(AtMpRenteCapitalBeVerdict.RENTE_ANNUELLE_GE_19);
        assertThat(r.conversionPartiellePossible()).isTrue();
        assertThat(r.capitalConversionPartielle()).isNotNull();
        assertThat(r.capitalConversionPartielle())
                .isGreaterThan(BigDecimal.ZERO);
        assertThat(r.renteResiduelleApresConversion()).isNotNull();
        // residuelle = rente - 1/3
        // rente = 45000 x 40 / 100 = 18000
        // 1/3 = 6000 → residuelle 12000
        assertThat(r.renteResiduelleApresConversion())
                .isCloseTo(new BigDecimal("12000.00"),
                        org.assertj.core.api.Assertions.within(
                                new BigDecimal("1.00")));
    }

    @Test
    void analyze_conversionPartielleNonDemandee_pasDeCapitalConversion() {
        AtMpRenteCapitalBeResult r =
                AtMpRenteCapitalBeChecker.analyze(reqIppEleve());
        assertThat(r.capitalConversionPartielle()).isNull();
        assertThat(r.renteResiduelleApresConversion()).isNull();
    }

    // == Branche INELIGIBLE_NON_RECONNU ==

    @Test
    void analyze_statutConteste_returnsIneligibleNonReconnu() {
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL,
                AtMpRenteCapitalBeStatutReconnaissance.CONTESTE,
                DATE_CONSOLIDATION,
                new BigDecimal("30"),
                REMUNERATION,
                DATE_NAISSANCE,
                false, null, null);
        AtMpRenteCapitalBeResult r =
                AtMpRenteCapitalBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(AtMpRenteCapitalBeVerdict.INELIGIBLE_NON_RECONNU);
        assertThat(r.renteAnnuelle()).isNull();
        assertThat(r.capitalCapitalisation()).isNull();
        assertThat(r.synthese()).contains("tribunal du travail");
    }

    @Test
    void analyze_statutContesteMP_libellePrecise() {
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.MALADIE_PROFESSIONNELLE,
                AtMpRenteCapitalBeStatutReconnaissance.CONTESTE,
                DATE_CONSOLIDATION,
                new BigDecimal("30"),
                REMUNERATION,
                DATE_NAISSANCE,
                false, null, null);
        AtMpRenteCapitalBeResult r =
                AtMpRenteCapitalBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(AtMpRenteCapitalBeVerdict.INELIGIBLE_NON_RECONNU);
        assertThat(r.synthese()).contains("maladie professionnelle");
    }

    // == Branche IPP_NON_DETERMINE ==

    @Test
    void analyze_statutEnCours_returnsIppNonDetermine() {
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL,
                AtMpRenteCapitalBeStatutReconnaissance.EN_COURS,
                null,
                null,
                REMUNERATION,
                DATE_NAISSANCE,
                false, null, null);
        AtMpRenteCapitalBeResult r =
                AtMpRenteCapitalBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(AtMpRenteCapitalBeVerdict.IPP_NON_DETERMINE);
        assertThat(r.synthese()).contains("consolidation");
    }

    @Test
    void analyze_tauxIppNullStatutReconnu_returnsIppNonDetermine() {
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL,
                AtMpRenteCapitalBeStatutReconnaissance.EN_COURS,
                DATE_CONSOLIDATION,
                null,
                REMUNERATION,
                DATE_NAISSANCE,
                false, null, null);
        AtMpRenteCapitalBeResult r =
                AtMpRenteCapitalBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(AtMpRenteCapitalBeVerdict.IPP_NON_DETERMINE);
    }

    // == Calcul coefficient d'age ==

    @Test
    void analyze_coefficientAge_jeune_eleve() {
        // Jeune travailleur → coefficient eleve
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL,
                AtMpRenteCapitalBeStatutReconnaissance.RECONNU,
                LocalDate.of(2024, 6, 1),
                new BigDecimal("10"),
                REMUNERATION,
                LocalDate.of(2004, 6, 1), // 20 ans
                false, null, null);
        AtMpRenteCapitalBeResult r =
                AtMpRenteCapitalBeChecker.analyze(req);
        assertThat(r.ageConsolidationCalcule()).isEqualTo(20);
        // 20 ans → coefficient = 12.5
        assertThat(r.coefficientAge())
                .isEqualByComparingTo("12.5000");
    }

    @Test
    void analyze_coefficientAge_age_avance_faible() {
        // Travailleur age → coefficient faible
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL,
                AtMpRenteCapitalBeStatutReconnaissance.RECONNU,
                LocalDate.of(2024, 6, 1),
                new BigDecimal("10"),
                REMUNERATION,
                LocalDate.of(1954, 6, 1), // 70 ans
                false, null, null);
        AtMpRenteCapitalBeResult r =
                AtMpRenteCapitalBeChecker.analyze(req);
        assertThat(r.ageConsolidationCalcule()).isEqualTo(70);
        assertThat(r.coefficientAge())
                .isEqualByComparingTo("4.1500");
    }

    @Test
    void analyze_coefficientAge_interpolation() {
        // 47 ans (entre 45 et 50)
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL,
                AtMpRenteCapitalBeStatutReconnaissance.RECONNU,
                LocalDate.of(2024, 6, 1),
                new BigDecimal("10"),
                REMUNERATION,
                LocalDate.of(1977, 6, 1), // 47 ans
                false, null, null);
        AtMpRenteCapitalBeResult r =
                AtMpRenteCapitalBeChecker.analyze(req);
        assertThat(r.ageConsolidationCalcule()).isEqualTo(47);
        // 45 ans = 9.0, 50 ans = 8.05 → diff = -0.95
        // 47 = 45 + 2/5 * -0.95 = 9.0 - 0.38 = 8.62
        assertThat(r.coefficientAge())
                .isCloseTo(new BigDecimal("8.62"),
                        org.assertj.core.api.Assertions.within(
                                new BigDecimal("0.01")));
    }

    @Test
    void analyze_ageOverride_utilise() {
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL,
                AtMpRenteCapitalBeStatutReconnaissance.RECONNU,
                LocalDate.of(2024, 6, 1),
                new BigDecimal("10"),
                REMUNERATION,
                DATE_NAISSANCE,
                false, null, 50); // override age
        AtMpRenteCapitalBeResult r =
                AtMpRenteCapitalBeChecker.analyze(req);
        assertThat(r.ageConsolidationCalcule()).isEqualTo(50);
        assertThat(r.coefficientAge())
                .isEqualByComparingTo("8.0500");
    }

    // == Bases juridiques + avertissement ==

    @Test
    void analyze_returnsBaseJuridiqueAndAvertissement() {
        AtMpRenteCapitalBeResult r =
                AtMpRenteCapitalBeChecker.analyze(reqIppFaible());
        assertThat(r.baseJuridique()).contains("Loi du 10/04/1971");
        assertThat(r.baseJuridique()).contains("art. 24");
        assertThat(r.baseJuridique()).contains("Lois coordonnees du 03/06/1970");
        assertThat(r.baseJuridique()).contains("AR du 21/12/1971");
        assertThat(r.baseJuridique()).contains("AR du 24/02/2005");
        assertThat(r.baseJuridique()).contains("art. 45ter");
        assertThat(r.baseJuridique()).contains("Cass. BE 06/11/2000");
        assertThat(r.baseJuridique()).contains("Cass. BE 26/05/2003");
        assertThat(r.baseJuridique()).contains("Cass. BE 23/03/2009");
        assertThat(r.avertissement()).contains("tribunal du travail");
        assertThat(r.avertissement()).contains("consolidation");
    }

    // == Inputs snapshot ==

    @Test
    void analyze_snapshotInputs_dansResult() {
        AtMpRenteCapitalBeResult r =
                AtMpRenteCapitalBeChecker.analyze(reqIppEleve());
        assertThat(r.origine())
                .isEqualTo(AtMpRenteCapitalBeOrigine.MALADIE_PROFESSIONNELLE);
        assertThat(r.statutReconnaissance())
                .isEqualTo(AtMpRenteCapitalBeStatutReconnaissance.RECONNU);
        assertThat(r.tauxIpp()).isEqualByComparingTo("35");
        assertThat(r.remunerationBaseAnnuelle())
                .isEqualByComparingTo(REMUNERATION);
        assertThat(r.dateNaissance()).isEqualTo(DATE_NAISSANCE);
        assertThat(r.dateConsolidation()).isEqualTo(DATE_CONSOLIDATION);
    }

    // == Validation ==

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() -> AtMpRenteCapitalBeChecker.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requete");
    }

    @Test
    void analyze_origineNull_throws() {
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                null,
                AtMpRenteCapitalBeStatutReconnaissance.RECONNU,
                DATE_CONSOLIDATION,
                new BigDecimal("10"),
                REMUNERATION, DATE_NAISSANCE,
                false, null, null);
        assertThatThrownBy(() -> AtMpRenteCapitalBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("origine");
    }

    @Test
    void analyze_statutReconnaissanceNull_throws() {
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL,
                null,
                DATE_CONSOLIDATION,
                new BigDecimal("10"),
                REMUNERATION, DATE_NAISSANCE,
                false, null, null);
        assertThatThrownBy(() -> AtMpRenteCapitalBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("statutReconnaissance");
    }

    @Test
    void analyze_remunerationNull_throws() {
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL,
                AtMpRenteCapitalBeStatutReconnaissance.RECONNU,
                DATE_CONSOLIDATION,
                new BigDecimal("10"),
                null, DATE_NAISSANCE,
                false, null, null);
        assertThatThrownBy(() -> AtMpRenteCapitalBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remuneration");
    }

    @Test
    void analyze_remunerationZero_throws() {
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL,
                AtMpRenteCapitalBeStatutReconnaissance.RECONNU,
                DATE_CONSOLIDATION,
                new BigDecimal("10"),
                BigDecimal.ZERO, DATE_NAISSANCE,
                false, null, null);
        assertThatThrownBy(() -> AtMpRenteCapitalBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void analyze_dateNaissanceNull_throws() {
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL,
                AtMpRenteCapitalBeStatutReconnaissance.RECONNU,
                DATE_CONSOLIDATION,
                new BigDecimal("10"),
                REMUNERATION, null,
                false, null, null);
        assertThatThrownBy(() -> AtMpRenteCapitalBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNaissance");
    }

    @Test
    void analyze_tauxIppNegatif_throws() {
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL,
                AtMpRenteCapitalBeStatutReconnaissance.RECONNU,
                DATE_CONSOLIDATION,
                new BigDecimal("-5"),
                REMUNERATION, DATE_NAISSANCE,
                false, null, null);
        assertThatThrownBy(() -> AtMpRenteCapitalBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tauxIpp");
    }

    @Test
    void analyze_tauxIppSuperieur100_throws() {
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL,
                AtMpRenteCapitalBeStatutReconnaissance.RECONNU,
                DATE_CONSOLIDATION,
                new BigDecimal("150"),
                REMUNERATION, DATE_NAISSANCE,
                false, null, null);
        assertThatThrownBy(() -> AtMpRenteCapitalBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tauxIpp");
    }

    @Test
    void analyze_dateNaissanceApresConsolidation_throws() {
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL,
                AtMpRenteCapitalBeStatutReconnaissance.RECONNU,
                LocalDate.of(2020, 1, 1),
                new BigDecimal("10"),
                REMUNERATION,
                LocalDate.of(2024, 6, 1), // post-consolidation
                false, null, null);
        assertThatThrownBy(() -> AtMpRenteCapitalBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNaissance");
    }

    @Test
    void analyze_statutReconnuSansConsolidationNiOverride_throws() {
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL,
                AtMpRenteCapitalBeStatutReconnaissance.RECONNU,
                null,
                new BigDecimal("10"),
                REMUNERATION, DATE_NAISSANCE,
                false, null, null);
        assertThatThrownBy(() -> AtMpRenteCapitalBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateConsolidation");
    }

    @Test
    void analyze_statutReconnuAvecOverrideSansConsolidation_ok() {
        AtMpRenteCapitalBeRequest req = new AtMpRenteCapitalBeRequest(
                AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL,
                AtMpRenteCapitalBeStatutReconnaissance.RECONNU,
                null,
                new BigDecimal("10"),
                REMUNERATION, DATE_NAISSANCE,
                false, null, 45);
        AtMpRenteCapitalBeResult r =
                AtMpRenteCapitalBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(AtMpRenteCapitalBeVerdict.CAPITAL_FORFAITAIRE_LT_19);
        assertThat(r.ageConsolidationCalcule()).isEqualTo(45);
    }
}
