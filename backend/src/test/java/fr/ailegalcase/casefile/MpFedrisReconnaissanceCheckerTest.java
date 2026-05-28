package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-28 : tests unitaires du {@link MpFedrisReconnaissanceChecker}.
 *
 * <p>Couvre : 2 branches typeMaladie (LISTE_FERMEE / HORS_LISTE), 6
 * verdicts (MALADIE_LISTE_FERMEE_PRESOMPTION / MALADIE_LISTE_FERMEE_
 * EXPOSITION_INSUFFISANTE / SYSTEME_OUVERT_CAUSALITE_DIRECTE_
 * DETERMINANTE / SYSTEME_OUVERT_CAUSALITE_INSUFFISANTE / DECLARATION_
 * PRESCRITE / A_QUALIFIER), calcul prescription triennale art. 31 lois
 * coordonnees 03/06/1970, drapeaux presomptionApplicable et
 * causaliteRequise, base juridique stable, avertissement avocat BE,
 * validation inputs.</p>
 */
class MpFedrisReconnaissanceCheckerTest {

    private static final LocalDate CONSTATATION = LocalDate.of(2024, 1, 15);
    private static final LocalDate CONNAISSANCE = LocalDate.of(2024, 2, 1);
    private static final LocalDate DECLARATION_PROSPECTIVE = LocalDate.of(2024, 6, 1);

    private static MpFedrisReconnaissanceRequest reqListeFermeeAveree() {
        return new MpFedrisReconnaissanceRequest(
                MpFedrisReconnaissanceTypeMaladie.LISTE_FERMEE,
                "1.404.01 silicose",
                "Silicose pulmonaire",
                CONSTATATION,
                CONNAISSANCE,
                DECLARATION_PROSPECTIVE,
                MpFedrisReconnaissanceExposition.AVEREE,
                MpFedrisReconnaissanceCausalite.NON_APPLICABLE);
    }

    private static MpFedrisReconnaissanceRequest reqHorsListeCausalite() {
        return new MpFedrisReconnaissanceRequest(
                MpFedrisReconnaissanceTypeMaladie.HORS_LISTE,
                null,
                "Burn-out syndrome professionnel",
                CONSTATATION,
                CONNAISSANCE,
                DECLARATION_PROSPECTIVE,
                MpFedrisReconnaissanceExposition.AVEREE,
                MpFedrisReconnaissanceCausalite.DIRECTE_DETERMINANTE);
    }

    // == Branche LISTE_FERMEE ==

    @Test
    void analyze_listeFermee_expositionAveree_returnsPresomption() {
        MpFedrisReconnaissanceResult r =
                MpFedrisReconnaissanceChecker.analyze(reqListeFermeeAveree());
        assertThat(r.verdict())
                .isEqualTo(MpFedrisReconnaissanceVerdict.MALADIE_LISTE_FERMEE_PRESOMPTION);
        assertThat(r.presomptionApplicable()).isTrue();
        assertThat(r.causaliteRequise()).isFalse();
        assertThat(r.prescriteAuJour()).isFalse();
        assertThat(r.raison())
                .isEqualTo("LISTE_FERMEE_EXPOSITION_AVEREE_PRESOMPTION_ART_32");
        assertThat(r.synthese()).contains("art. 32");
        assertThat(r.synthese()).contains("Silicose pulmonaire");
    }

    @Test
    void analyze_listeFermee_expositionInsuffisante_returnsExpositionInsuffisante() {
        MpFedrisReconnaissanceRequest req = new MpFedrisReconnaissanceRequest(
                MpFedrisReconnaissanceTypeMaladie.LISTE_FERMEE,
                "2.2.2 surdité bruit",
                "Surdité de perception professionnelle",
                CONSTATATION, CONNAISSANCE, DECLARATION_PROSPECTIVE,
                MpFedrisReconnaissanceExposition.INSUFFISANTE,
                MpFedrisReconnaissanceCausalite.NON_APPLICABLE);
        MpFedrisReconnaissanceResult r =
                MpFedrisReconnaissanceChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(MpFedrisReconnaissanceVerdict.MALADIE_LISTE_FERMEE_EXPOSITION_INSUFFISANTE);
        assertThat(r.presomptionApplicable()).isFalse();
        assertThat(r.causaliteRequise()).isFalse();
        assertThat(r.synthese()).contains("art. 30bis");
    }

    @Test
    void analyze_listeFermee_expositionIndeterminee_returnsAQualifier() {
        MpFedrisReconnaissanceRequest req = new MpFedrisReconnaissanceRequest(
                MpFedrisReconnaissanceTypeMaladie.LISTE_FERMEE,
                "5.01.01 affection lombaire",
                "Lombalgie chronique professionnelle",
                CONSTATATION, CONNAISSANCE, DECLARATION_PROSPECTIVE,
                MpFedrisReconnaissanceExposition.INDETERMINEE,
                MpFedrisReconnaissanceCausalite.NON_APPLICABLE);
        MpFedrisReconnaissanceResult r =
                MpFedrisReconnaissanceChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(MpFedrisReconnaissanceVerdict.A_QUALIFIER);
        assertThat(r.presomptionApplicable()).isFalse();
        assertThat(r.raison()).isEqualTo("LISTE_FERMEE_EXPOSITION_INDETERMINEE");
    }

    // == Branche SYSTEME_OUVERT ==

    @Test
    void analyze_horsListe_causaliteDirecteDeterminante_returnsSystemeOuvert() {
        MpFedrisReconnaissanceResult r =
                MpFedrisReconnaissanceChecker.analyze(reqHorsListeCausalite());
        assertThat(r.verdict())
                .isEqualTo(MpFedrisReconnaissanceVerdict.SYSTEME_OUVERT_CAUSALITE_DIRECTE_DETERMINANTE);
        assertThat(r.presomptionApplicable()).isFalse();
        assertThat(r.causaliteRequise()).isTrue();
        assertThat(r.synthese()).contains("art. 30bis");
        assertThat(r.synthese()).contains("Burn-out syndrome professionnel");
    }

    @Test
    void analyze_horsListe_causaliteInsuffisante_returnsRefus() {
        MpFedrisReconnaissanceRequest req = new MpFedrisReconnaissanceRequest(
                MpFedrisReconnaissanceTypeMaladie.HORS_LISTE,
                null,
                "Trouble musculo-squelettique non liste",
                CONSTATATION, CONNAISSANCE, DECLARATION_PROSPECTIVE,
                MpFedrisReconnaissanceExposition.AVEREE,
                MpFedrisReconnaissanceCausalite.INSUFFISANTE);
        MpFedrisReconnaissanceResult r =
                MpFedrisReconnaissanceChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(MpFedrisReconnaissanceVerdict.SYSTEME_OUVERT_CAUSALITE_INSUFFISANTE);
        assertThat(r.causaliteRequise()).isTrue();
        assertThat(r.synthese()).contains("refus probable");
    }

    @Test
    void analyze_horsListe_causaliteIndeterminee_returnsAQualifier() {
        MpFedrisReconnaissanceRequest req = new MpFedrisReconnaissanceRequest(
                MpFedrisReconnaissanceTypeMaladie.HORS_LISTE,
                null,
                "Trouble anxieux professionnel",
                CONSTATATION, CONNAISSANCE, DECLARATION_PROSPECTIVE,
                MpFedrisReconnaissanceExposition.AVEREE,
                MpFedrisReconnaissanceCausalite.INDETERMINEE);
        MpFedrisReconnaissanceResult r =
                MpFedrisReconnaissanceChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(MpFedrisReconnaissanceVerdict.A_QUALIFIER);
        assertThat(r.causaliteRequise()).isTrue();
        assertThat(r.raison()).isEqualTo("SYSTEME_OUVERT_CAUSALITE_INDETERMINEE");
    }

    @Test
    void analyze_horsListe_causaliteNonApplicable_returnsAQualifier() {
        MpFedrisReconnaissanceRequest req = new MpFedrisReconnaissanceRequest(
                MpFedrisReconnaissanceTypeMaladie.HORS_LISTE,
                null,
                "Maladie a etudier",
                CONSTATATION, CONNAISSANCE, DECLARATION_PROSPECTIVE,
                MpFedrisReconnaissanceExposition.AVEREE,
                MpFedrisReconnaissanceCausalite.NON_APPLICABLE);
        MpFedrisReconnaissanceResult r =
                MpFedrisReconnaissanceChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(MpFedrisReconnaissanceVerdict.A_QUALIFIER);
    }

    // == Branche PRESCRIPTION ==

    @Test
    void analyze_prescriptionDepassee_returnsPrescrit() {
        // connaissance 2020-01-01 + 3 ans = 2023-01-01 ; declaration 2024-06-01 → prescrit
        MpFedrisReconnaissanceRequest req = new MpFedrisReconnaissanceRequest(
                MpFedrisReconnaissanceTypeMaladie.LISTE_FERMEE,
                "1.404.01 silicose",
                "Silicose pulmonaire",
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2024, 6, 1),
                MpFedrisReconnaissanceExposition.AVEREE,
                MpFedrisReconnaissanceCausalite.NON_APPLICABLE);
        MpFedrisReconnaissanceResult r =
                MpFedrisReconnaissanceChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(MpFedrisReconnaissanceVerdict.DECLARATION_PRESCRITE);
        assertThat(r.prescriteAuJour()).isTrue();
        assertThat(r.joursAvantPrescription()).isNegative();
        assertThat(r.datePrescription()).isEqualTo(LocalDate.of(2023, 1, 1));
        assertThat(r.synthese()).contains("art. 31");
    }

    @Test
    void analyze_prescriptionImminenteAvant_returnsPresomptionPasPrescrit() {
        // connaissance 2021-06-01 + 3 ans = 2024-06-01 ; declaration 2024-05-30 → ok
        MpFedrisReconnaissanceRequest req = new MpFedrisReconnaissanceRequest(
                MpFedrisReconnaissanceTypeMaladie.LISTE_FERMEE,
                "2.3.1 amiante",
                "Mesotheliome pleural",
                LocalDate.of(2021, 6, 1),
                LocalDate.of(2021, 6, 1),
                LocalDate.of(2024, 5, 30),
                MpFedrisReconnaissanceExposition.AVEREE,
                MpFedrisReconnaissanceCausalite.NON_APPLICABLE);
        MpFedrisReconnaissanceResult r =
                MpFedrisReconnaissanceChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(MpFedrisReconnaissanceVerdict.MALADIE_LISTE_FERMEE_PRESOMPTION);
        assertThat(r.prescriteAuJour()).isFalse();
        assertThat(r.joursAvantPrescription()).isPositive();
    }

    @Test
    void analyze_dateConnaissanceNull_fallbackSurDateConstatation() {
        MpFedrisReconnaissanceRequest req = new MpFedrisReconnaissanceRequest(
                MpFedrisReconnaissanceTypeMaladie.LISTE_FERMEE,
                "1.404.01 silicose",
                "Silicose",
                LocalDate.of(2022, 1, 1),
                null, // pas de date connaissance
                LocalDate.of(2024, 6, 1),
                MpFedrisReconnaissanceExposition.AVEREE,
                MpFedrisReconnaissanceCausalite.NON_APPLICABLE);
        MpFedrisReconnaissanceResult r =
                MpFedrisReconnaissanceChecker.analyze(req);
        // 2022-01-01 + 3 = 2025-01-01
        assertThat(r.datePrescription()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(r.prescriteAuJour()).isFalse();
    }

    @Test
    void analyze_dateDeclarationNull_utiliseAujourdHui() {
        // Le calcul des jours doit se faire avec LocalDate.now() — test que ça
        // ne plante pas et que le résultat est cohérent.
        MpFedrisReconnaissanceRequest req = new MpFedrisReconnaissanceRequest(
                MpFedrisReconnaissanceTypeMaladie.LISTE_FERMEE,
                "1.404.01",
                "Silicose",
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 1),
                null, // pas de date declaration
                MpFedrisReconnaissanceExposition.AVEREE,
                MpFedrisReconnaissanceCausalite.NON_APPLICABLE);
        MpFedrisReconnaissanceResult r =
                MpFedrisReconnaissanceChecker.analyze(req);
        assertThat(r.datePrescription()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(r.verdict()).isNotNull();
    }

    // == Bases juridiques + avertissement ==

    @Test
    void analyze_returnsBaseJuridiqueAndAvertissement() {
        MpFedrisReconnaissanceResult r =
                MpFedrisReconnaissanceChecker.analyze(reqListeFermeeAveree());
        assertThat(r.baseJuridique()).contains("Lois coordonnees du 03/06/1970");
        assertThat(r.baseJuridique()).contains("AR du 28/03/1969");
        assertThat(r.baseJuridique()).contains("AR du 16/12/1985");
        assertThat(r.baseJuridique()).contains("Loi du 11/01/2018");
        assertThat(r.baseJuridique()).contains("Fedris");
        assertThat(r.baseJuridique()).contains("Cass. BE 28/05/2008");
        assertThat(r.avertissement()).contains("Fedris");
        assertThat(r.avertissement()).contains("expert");
    }

    // == Inputs snapshot ==

    @Test
    void analyze_snapshotInputs_dansResult() {
        MpFedrisReconnaissanceResult r =
                MpFedrisReconnaissanceChecker.analyze(reqListeFermeeAveree());
        assertThat(r.typeMaladie())
                .isEqualTo(MpFedrisReconnaissanceTypeMaladie.LISTE_FERMEE);
        assertThat(r.codeMaladieListe()).isEqualTo("1.404.01 silicose");
        assertThat(r.libelleMaladie()).isEqualTo("Silicose pulmonaire");
        assertThat(r.dateConstatationMedicale()).isEqualTo(CONSTATATION);
        assertThat(r.dateConnaissanceProfessionnel()).isEqualTo(CONNAISSANCE);
        assertThat(r.dateDeclarationEnvisagee()).isEqualTo(DECLARATION_PROSPECTIVE);
        assertThat(r.exposition())
                .isEqualTo(MpFedrisReconnaissanceExposition.AVEREE);
        assertThat(r.causalite())
                .isEqualTo(MpFedrisReconnaissanceCausalite.NON_APPLICABLE);
    }

    // == Validation ==

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() -> MpFedrisReconnaissanceChecker.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requete");
    }

    @Test
    void analyze_typeMaladieNull_throws() {
        MpFedrisReconnaissanceRequest req = new MpFedrisReconnaissanceRequest(
                null, "x", "y", CONSTATATION, CONNAISSANCE, DECLARATION_PROSPECTIVE,
                MpFedrisReconnaissanceExposition.AVEREE,
                MpFedrisReconnaissanceCausalite.NON_APPLICABLE);
        assertThatThrownBy(() -> MpFedrisReconnaissanceChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeMaladie");
    }

    @Test
    void analyze_libelleNull_throws() {
        MpFedrisReconnaissanceRequest req = new MpFedrisReconnaissanceRequest(
                MpFedrisReconnaissanceTypeMaladie.LISTE_FERMEE,
                "code", null, CONSTATATION, CONNAISSANCE, DECLARATION_PROSPECTIVE,
                MpFedrisReconnaissanceExposition.AVEREE,
                MpFedrisReconnaissanceCausalite.NON_APPLICABLE);
        assertThatThrownBy(() -> MpFedrisReconnaissanceChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("libelleMaladie");
    }

    @Test
    void analyze_libelleBlank_throws() {
        MpFedrisReconnaissanceRequest req = new MpFedrisReconnaissanceRequest(
                MpFedrisReconnaissanceTypeMaladie.LISTE_FERMEE,
                "code", "   ", CONSTATATION, CONNAISSANCE, DECLARATION_PROSPECTIVE,
                MpFedrisReconnaissanceExposition.AVEREE,
                MpFedrisReconnaissanceCausalite.NON_APPLICABLE);
        assertThatThrownBy(() -> MpFedrisReconnaissanceChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void analyze_dateConstatationNull_throws() {
        MpFedrisReconnaissanceRequest req = new MpFedrisReconnaissanceRequest(
                MpFedrisReconnaissanceTypeMaladie.LISTE_FERMEE,
                "code", "lib", null, CONNAISSANCE, DECLARATION_PROSPECTIVE,
                MpFedrisReconnaissanceExposition.AVEREE,
                MpFedrisReconnaissanceCausalite.NON_APPLICABLE);
        assertThatThrownBy(() -> MpFedrisReconnaissanceChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateConstatationMedicale");
    }

    @Test
    void analyze_expositionNull_throws() {
        MpFedrisReconnaissanceRequest req = new MpFedrisReconnaissanceRequest(
                MpFedrisReconnaissanceTypeMaladie.LISTE_FERMEE,
                "code", "lib", CONSTATATION, CONNAISSANCE, DECLARATION_PROSPECTIVE,
                null,
                MpFedrisReconnaissanceCausalite.NON_APPLICABLE);
        assertThatThrownBy(() -> MpFedrisReconnaissanceChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exposition");
    }

    @Test
    void analyze_causaliteNull_throws() {
        MpFedrisReconnaissanceRequest req = new MpFedrisReconnaissanceRequest(
                MpFedrisReconnaissanceTypeMaladie.HORS_LISTE,
                null, "lib", CONSTATATION, CONNAISSANCE, DECLARATION_PROSPECTIVE,
                MpFedrisReconnaissanceExposition.AVEREE,
                null);
        assertThatThrownBy(() -> MpFedrisReconnaissanceChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("causalite");
    }

    @Test
    void analyze_connaissanceAvantConstatation_throws() {
        MpFedrisReconnaissanceRequest req = new MpFedrisReconnaissanceRequest(
                MpFedrisReconnaissanceTypeMaladie.LISTE_FERMEE,
                "code", "lib",
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2024, 1, 1), // avant
                DECLARATION_PROSPECTIVE,
                MpFedrisReconnaissanceExposition.AVEREE,
                MpFedrisReconnaissanceCausalite.NON_APPLICABLE);
        assertThatThrownBy(() -> MpFedrisReconnaissanceChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateConnaissanceProfessionnel");
    }

    @Test
    void analyze_listeFermeeSansCode_throws() {
        MpFedrisReconnaissanceRequest req = new MpFedrisReconnaissanceRequest(
                MpFedrisReconnaissanceTypeMaladie.LISTE_FERMEE,
                null, // code obligatoire si LISTE_FERMEE
                "Silicose",
                CONSTATATION, CONNAISSANCE, DECLARATION_PROSPECTIVE,
                MpFedrisReconnaissanceExposition.AVEREE,
                MpFedrisReconnaissanceCausalite.NON_APPLICABLE);
        assertThatThrownBy(() -> MpFedrisReconnaissanceChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("codeMaladieListe");
    }

    @Test
    void analyze_listeFermeeAvecCodeBlank_throws() {
        MpFedrisReconnaissanceRequest req = new MpFedrisReconnaissanceRequest(
                MpFedrisReconnaissanceTypeMaladie.LISTE_FERMEE,
                "   ",
                "Silicose",
                CONSTATATION, CONNAISSANCE, DECLARATION_PROSPECTIVE,
                MpFedrisReconnaissanceExposition.AVEREE,
                MpFedrisReconnaissanceCausalite.NON_APPLICABLE);
        assertThatThrownBy(() -> MpFedrisReconnaissanceChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("codeMaladieListe");
    }

    @Test
    void analyze_horsListeSansCode_accepte() {
        // En système ouvert, le code n'est pas obligatoire (la maladie n'est pas sur la liste).
        MpFedrisReconnaissanceRequest req = new MpFedrisReconnaissanceRequest(
                MpFedrisReconnaissanceTypeMaladie.HORS_LISTE,
                null,
                "Burn-out",
                CONSTATATION, CONNAISSANCE, DECLARATION_PROSPECTIVE,
                MpFedrisReconnaissanceExposition.AVEREE,
                MpFedrisReconnaissanceCausalite.DIRECTE_DETERMINANTE);
        MpFedrisReconnaissanceResult r =
                MpFedrisReconnaissanceChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(MpFedrisReconnaissanceVerdict.SYSTEME_OUVERT_CAUSALITE_DIRECTE_DETERMINANTE);
    }
}
