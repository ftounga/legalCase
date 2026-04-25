package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.PseCalculator.AvisCse;
import fr.ailegalcase.casefile.PseCalculator.ContenuMesure;
import fr.ailegalcase.casefile.PseCalculator.CritereValidite;
import fr.ailegalcase.casefile.PseCalculator.ModeAdoption;
import fr.ailegalcase.casefile.PseCalculator.StatutDreets;
import fr.ailegalcase.casefile.PseCalculator.VerdictValidite;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PseCalculatorTest {

    private static final LocalDate DATE_PROJET = LocalDate.of(2026, 3, 15);
    private static final LocalDate DATE_DREETS = LocalDate.of(2026, 4, 1);

    private static final List<ContenuMesure> CONTENU_OK = List.of(
            ContenuMesure.RECLASSEMENT_INTERNE,
            ContenuMesure.FORMATION,
            ContenuMesure.AIDE_CREATION
    );

    // ============ Déclenchement (PSE requis) ============

    @Test
    void pseRequis_10licenciements_30jours_50salaries_returnsTrue() {
        PseResult r = compute(50, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                StatutDreets.VALIDE, CONTENU_OK);
        assertThat(r.pseRequis()).isTrue();
    }

    @Test
    void pseRequis_9licenciements_returnsFalse() {
        PseResult r = compute(50, 9, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                StatutDreets.VALIDE, CONTENU_OK);
        assertThat(r.pseRequis()).isFalse();
    }

    @Test
    void pseRequis_10licenciements_31jours_returnsFalse() {
        PseResult r = compute(50, 10, 31,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                StatutDreets.VALIDE, CONTENU_OK);
        assertThat(r.pseRequis()).isFalse();
    }

    @Test
    void pseRequis_10licenciements_entreprise49_returnsFalse() {
        PseResult r = compute(49, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                StatutDreets.VALIDE, CONTENU_OK);
        assertThat(r.pseRequis()).isFalse();
    }

    @Test
    void pseNonRequis_returnsVerdictVALIDE_score100() {
        PseResult r = compute(30, 5, 30,
                ModeAdoption.DOCUMENT_UNILATERAL, AvisCse.NON_CONSULTE,
                StatutDreets.EN_COURS, List.of());
        assertThat(r.pseRequis()).isFalse();
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.VALIDE);
        assertThat(r.scoreConformite()).isEqualTo(100);
        assertThat(r.criteresManquants()).isEmpty();
    }

    // ============ Critères bloquants → NUL ============

    @Test
    void pseRequis_cseNonConsulte_returnsNUL() {
        PseResult r = compute(50, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.NON_CONSULTE,
                StatutDreets.VALIDE, CONTENU_OK);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(r.criteresManquants()).contains(CritereValidite.CSE_CONSULTE);
    }

    @Test
    void pseRequis_dreetsRefus_returnsNUL() {
        PseResult r = compute(50, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                StatutDreets.REFUS, CONTENU_OK);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(r.criteresManquants()).contains(CritereValidite.DREETS_VALIDE_OU_HOMOLOGUE);
    }

    // ============ DREETS EN_COURS → CONTESTABLE ============

    @Test
    void pseRequis_dreetsEnCours_returnsCONTESTABLE() {
        PseResult r = compute(50, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                StatutDreets.EN_COURS, CONTENU_OK);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.CONTESTABLE);
        assertThat(r.scoreConformite()).isLessThan(100);
    }

    // ============ Cohérence mode / DREETS ============

    @Test
    void pseRequis_accordEtHomologue_mismatch_returnsCONTESTABLE() {
        PseResult r = compute(50, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                StatutDreets.HOMOLOGUE, CONTENU_OK);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.CONTESTABLE);
        assertThat(r.criteresManquants()).contains(CritereValidite.MODE_DREETS_COHERENT);
    }

    @Test
    void pseRequis_documentUnilateralEtValide_mismatch_returnsCONTESTABLE() {
        PseResult r = compute(50, 10, 30,
                ModeAdoption.DOCUMENT_UNILATERAL, AvisCse.FAVORABLE,
                StatutDreets.VALIDE, CONTENU_OK);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.CONTESTABLE);
        assertThat(r.criteresManquants()).contains(CritereValidite.MODE_DREETS_COHERENT);
    }

    @Test
    void pseRequis_accordEtValide_modeCoherent() {
        PseResult r = compute(50, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                StatutDreets.VALIDE, CONTENU_OK);
        assertThat(r.criteresRemplis()).contains(CritereValidite.MODE_DREETS_COHERENT);
    }

    @Test
    void pseRequis_documentUnilateralEtHomologue_modeCoherent() {
        PseResult r = compute(50, 10, 30,
                ModeAdoption.DOCUMENT_UNILATERAL, AvisCse.FAVORABLE,
                StatutDreets.HOMOLOGUE, CONTENU_OK);
        assertThat(r.criteresRemplis()).contains(CritereValidite.MODE_DREETS_COHERENT);
    }

    // ============ Contenu PSE ============

    @Test
    void pseRequis_sansReclassementInterne_returnsCONTESTABLE() {
        List<ContenuMesure> sansReclassement = List.of(ContenuMesure.FORMATION, ContenuMesure.AIDE_CREATION);
        PseResult r = compute(50, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                StatutDreets.VALIDE, sansReclassement);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.CONTESTABLE);
        assertThat(r.criteresManquants()).contains(CritereValidite.CONTENU_RECLASSEMENT);
    }

    @Test
    void pseRequis_uneSeuleMesure_returnsCONTESTABLE() {
        List<ContenuMesure> uneMesure = List.of(ContenuMesure.RECLASSEMENT_INTERNE);
        PseResult r = compute(50, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                StatutDreets.VALIDE, uneMesure);
        // 1 mesure : reclassement interne OK mais MESURES_MULTIPLES manquant → CONTESTABLE
        assertThat(r.criteresManquants()).contains(CritereValidite.MESURES_MULTIPLES);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.CONTESTABLE);
    }

    // ============ Cas nominal VALIDE ============

    @Test
    void pseRequis_toutOK_returnsVALIDE_score100() {
        PseResult r = compute(50, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                StatutDreets.VALIDE, CONTENU_OK);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.VALIDE);
        assertThat(r.scoreConformite()).isEqualTo(100);
        assertThat(r.criteresManquants()).isEmpty();
        assertThat(r.criteresRemplis()).contains(
                CritereValidite.CSE_CONSULTE,
                CritereValidite.DREETS_VALIDE_OU_HOMOLOGUE,
                CritereValidite.CONTENU_RECLASSEMENT,
                CritereValidite.MODE_DREETS_COHERENT,
                CritereValidite.MESURES_MULTIPLES);
    }

    @Test
    void pseRequis_toutOK_5mesures_returnsVALIDE() {
        List<ContenuMesure> cinqMesures = List.of(
                ContenuMesure.RECLASSEMENT_INTERNE,
                ContenuMesure.RECLASSEMENT_EXTERNE,
                ContenuMesure.FORMATION,
                ContenuMesure.AIDE_CREATION,
                ContenuMesure.INDEMNITES_SUPRA);
        PseResult r = compute(250, 30, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                StatutDreets.VALIDE, cinqMesures);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.VALIDE);
        assertThat(r.scoreConformite()).isEqualTo(100);
    }

    // ============ Score de conformité — déductions ============

    @Test
    void score_cseNonConsulte_deduit50() {
        PseResult r = compute(50, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.NON_CONSULTE,
                StatutDreets.VALIDE, CONTENU_OK);
        assertThat(r.scoreConformite()).isEqualTo(50);
    }

    @Test
    void score_dreetsEnCours_deduit25() {
        PseResult r = compute(50, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                StatutDreets.EN_COURS, CONTENU_OK);
        assertThat(r.scoreConformite()).isEqualTo(75);
    }

    @Test
    void score_mismatchMode_deduit20() {
        PseResult r = compute(50, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                StatutDreets.HOMOLOGUE, CONTENU_OK);
        assertThat(r.scoreConformite()).isEqualTo(80);
    }

    @Test
    void score_sansReclassement_deduit20() {
        List<ContenuMesure> sansR = List.of(ContenuMesure.FORMATION, ContenuMesure.AIDE_CREATION);
        PseResult r = compute(50, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                StatutDreets.VALIDE, sansR);
        assertThat(r.scoreConformite()).isEqualTo(80);
    }

    @Test
    void score_uneSeuleMesure_deduit10() {
        List<ContenuMesure> une = List.of(ContenuMesure.RECLASSEMENT_INTERNE);
        PseResult r = compute(50, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                StatutDreets.VALIDE, une);
        assertThat(r.scoreConformite()).isEqualTo(90);
    }

    // ============ Délai contestation ============

    @Test
    void delaiContestation_alwaysReturns60() {
        PseResult requis = compute(50, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                StatutDreets.VALIDE, CONTENU_OK);
        PseResult nonRequis = compute(30, 5, 30,
                ModeAdoption.DOCUMENT_UNILATERAL, AvisCse.FAVORABLE,
                StatutDreets.HOMOLOGUE, CONTENU_OK);
        assertThat(requis.delaiContestationJours()).isEqualTo(60);
        assertThat(nonRequis.delaiContestationJours()).isEqualTo(60);
    }

    // ============ Listes dérivées ============

    @Test
    void criteresRemplis_nonVide_quandToutOK() {
        PseResult r = compute(50, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                StatutDreets.VALIDE, CONTENU_OK);
        assertThat(r.criteresRemplis()).hasSize(5);
    }

    @Test
    void criteresManquants_listeBien() {
        List<ContenuMesure> sansR = List.of(ContenuMesure.FORMATION);
        PseResult r = compute(50, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                StatutDreets.VALIDE, sansR);
        assertThat(r.criteresManquants())
                .contains(CritereValidite.CONTENU_RECLASSEMENT,
                        CritereValidite.MESURES_MULTIPLES);
    }

    // ============ Validation ============

    @Test
    void validation_countryBELGIQUE_throws() {
        assertThatThrownBy(() -> PseCalculator.compute(50, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                StatutDreets.VALIDE, DATE_DREETS, CONTENU_OK, DATE_PROJET, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BELGIQUE");
    }

    @Test
    void validation_modeAdoptionNull_throws() {
        assertThatThrownBy(() -> PseCalculator.compute(50, 10, 30,
                null, AvisCse.FAVORABLE,
                StatutDreets.VALIDE, DATE_DREETS, CONTENU_OK, DATE_PROJET, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mode");
    }

    @Test
    void validation_dreetsStatutNull_throws() {
        assertThatThrownBy(() -> PseCalculator.compute(50, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                null, DATE_DREETS, CONTENU_OK, DATE_PROJET, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DREETS");
    }

    @Test
    void validation_avisCseNull_throws() {
        assertThatThrownBy(() -> PseCalculator.compute(50, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, null,
                StatutDreets.VALIDE, DATE_DREETS, CONTENU_OK, DATE_PROJET, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CSE");
    }

    @Test
    void validation_dateProjetNull_throws() {
        assertThatThrownBy(() -> PseCalculator.compute(50, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                StatutDreets.VALIDE, DATE_DREETS, CONTENU_OK, null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Date");
    }

    @Test
    void validation_negativeValues_throws() {
        assertThatThrownBy(() -> PseCalculator.compute(-1, 10, 30,
                ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE, AvisCse.FAVORABLE,
                StatutDreets.VALIDE, DATE_DREETS, CONTENU_OK, DATE_PROJET, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ============ Helper ============

    private PseResult compute(int taille, int nombre, int periode,
                              ModeAdoption mode, AvisCse cse, StatutDreets dreets,
                              List<ContenuMesure> mesures) {
        return PseCalculator.compute(taille, nombre, periode,
                mode, cse, dreets, DATE_DREETS, mesures, DATE_PROJET, "FRANCE");
    }
}
