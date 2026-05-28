package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-22 : tests unitaires du {@link EgaliteFemmesHommesBeChecker}.
 *
 * <p>Couvre : seuil 50 travailleurs (art. 2 Loi 22/04/2012), statut
 * rapport (déposé complet / déposé incomplet / en préparation / non
 * déposé / indéterminé), contenu cumulatif des 5 sections art. 4 AR
 * 17/08/2013 (niveau de fonction, ancienneté, qualification, régime de
 * travail, composants de rémunération), plan d'action art. 5,
 * médiateur art. 14, hiérarchie des verdicts, validation des inputs,
 * type de formulaire requis (ann. I ≥ 100 ETP vs ann. II 50-99 ETP),
 * et base juridique stable.</p>
 */
class EgaliteFemmesHommesBeCheckerTest {

    private static final LocalDate DATE_DEPOT = LocalDate.of(2024, 6, 1);

    private static EgaliteFemmesHommesBeRequest reqConforme() {
        return new EgaliteFemmesHommesBeRequest(
                75, // 50-99 ETP → ann. II
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, true, true, true, // 5 ventilations OK
                false, // pas d'écart
                null,
                true, // plan d'action (sans objet ici)
                true, // médiateur désigné
                false); // pas de plainte
    }

    // ── Cas conforme nominal ────────────────────────────────────────────────

    @Test
    void analyze_rapportCompletSansEcart_returnsConforme() {
        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(reqConforme());

        assertThat(r.verdict())
                .isEqualTo(EgaliteFemmesHommesBeVerdict.CONFORME_RAPPORT_PLAN_COMPLETS);
        assertThat(r.seuilAtteint()).isTrue();
        assertThat(r.rapportDepose()).isTrue();
        assertThat(r.ventilationComplete()).isTrue();
        assertThat(r.planActionRequis()).isFalse();
        assertThat(r.planActionConforme()).isTrue();
        assertThat(r.typeFormulaireRequis())
                .isEqualTo(EgaliteFemmesHommesBeChecker.FORMULAIRE_SIMPLIFIE);
        assertThat(r.raison()).isEqualTo("CONFORME_RAPPORT_PLAN_COMPLETS");
        assertThat(r.synthese()).contains("rapport d'analyse égalité");
    }

    @Test
    void analyze_rapportCompletAvecEcartEtPlanAction_returnsConforme() {
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                150,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, true, true, true,
                true, BigDecimal.valueOf(8.5),
                true, true, false);

        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(EgaliteFemmesHommesBeVerdict.CONFORME_RAPPORT_PLAN_COMPLETS);
        assertThat(r.planActionRequis()).isTrue();
        assertThat(r.planActionConforme()).isTrue();
        assertThat(r.typeFormulaireRequis())
                .isEqualTo(EgaliteFemmesHommesBeChecker.FORMULAIRE_DETAILLE);
        assertThat(r.synthese())
                .contains("écart non justifié")
                .contains("plan d'action");
    }

    @Test
    void analyze_conformeMaisMediateurAbsent_warningDansSynthese() {
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                80,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, true, true, true,
                false, null,
                true, false, false);

        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(EgaliteFemmesHommesBeVerdict.CONFORME_RAPPORT_PLAN_COMPLETS);
        assertThat(r.synthese()).contains("médiateur");
    }

    @Test
    void analyze_conformeMaisPlainteIefh_warningDansSynthese() {
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                80,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, true, true, true,
                false, null,
                true, true,
                true); // plainte IEFH

        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(EgaliteFemmesHommesBeVerdict.CONFORME_RAPPORT_PLAN_COMPLETS);
        assertThat(r.synthese()).contains("IEFH", "Bilka");
    }

    // ── Hiérarchie 1 : statut indéterminé / en préparation → A_ANALYSER ─────

    @Test
    void analyze_statutIndetermine_returnsAanalyser() {
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                75,
                EgaliteFemmesHommesBeStatutRapport.INDETERMINE,
                null,
                true, true, true, true, true,
                false, null, true, true, false);

        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(req);
        assertThat(r.verdict()).isEqualTo(EgaliteFemmesHommesBeVerdict.A_ANALYSER);
        assertThat(r.raison()).isEqualTo("STATUT_RAPPORT_INDETERMINE");
        assertThat(r.synthese()).contains("indéterminé");
    }

    @Test
    void analyze_enPreparation_returnsAanalyser() {
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                120,
                EgaliteFemmesHommesBeStatutRapport.EN_PREPARATION,
                null,
                false, false, false, false, false,
                false, null, false, false, false);

        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(req);
        assertThat(r.verdict()).isEqualTo(EgaliteFemmesHommesBeVerdict.A_ANALYSER);
        assertThat(r.raison()).isEqualTo("RAPPORT_EN_PREPARATION");
        assertThat(r.rapportDepose()).isFalse();
    }

    // ── Hiérarchie 2 : seuil non atteint ────────────────────────────────────

    @Test
    void analyze_effectif40_returnsHorsChamp() {
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                40,
                EgaliteFemmesHommesBeStatutRapport.NON_DEPOSE_DELAI_DEPASSE,
                null,
                false, false, false, false, false,
                false, null, false, false, false);

        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(EgaliteFemmesHommesBeVerdict.HORS_CHAMP_SEUIL_NON_ATTEINT);
        assertThat(r.seuilAtteint()).isFalse();
        assertThat(r.raison()).isEqualTo("EFFECTIF_INFERIEUR_SEUIL");
        assertThat(r.typeFormulaireRequis())
                .isEqualTo(EgaliteFemmesHommesBeChecker.FORMULAIRE_NON_APPLICABLE);
        assertThat(r.synthese()).contains("40", "50");
    }

    @Test
    void analyze_seuilExact50_estDansLeChamp() {
        // 50 = seuil inclusif → obligation applicable
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                50,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, true, true, true,
                false, null, true, true, false);

        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(EgaliteFemmesHommesBeVerdict.CONFORME_RAPPORT_PLAN_COMPLETS);
        assertThat(r.typeFormulaireRequis())
                .isEqualTo(EgaliteFemmesHommesBeChecker.FORMULAIRE_SIMPLIFIE);
    }

    @Test
    void analyze_seuilJuste49_horsChamp() {
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                49,
                EgaliteFemmesHommesBeStatutRapport.NON_DEPOSE_DELAI_DEPASSE,
                null,
                false, false, false, false, false,
                false, null, false, false, false);

        assertThat(EgaliteFemmesHommesBeChecker.analyze(req).verdict())
                .isEqualTo(EgaliteFemmesHommesBeVerdict.HORS_CHAMP_SEUIL_NON_ATTEINT);
    }

    @Test
    void analyze_seuilExact100_basculeFormulaireDetaille() {
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                100,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, true, true, true,
                false, null, true, true, false);

        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(req);
        assertThat(r.typeFormulaireRequis())
                .isEqualTo(EgaliteFemmesHommesBeChecker.FORMULAIRE_DETAILLE);
    }

    @Test
    void analyze_seuilJuste99_resteFormulaireSimplifie() {
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                99,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, true, true, true,
                false, null, true, true, false);

        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(req);
        assertThat(r.typeFormulaireRequis())
                .isEqualTo(EgaliteFemmesHommesBeChecker.FORMULAIRE_SIMPLIFIE);
    }

    // ── Hiérarchie 3 : rapport non déposé → MANQUEMENT_GRAVE ────────────────

    @Test
    void analyze_rapportNonDepose_returnsManquementGrave() {
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                80,
                EgaliteFemmesHommesBeStatutRapport.NON_DEPOSE_DELAI_DEPASSE,
                null,
                false, false, false, false, false,
                false, null, false, false, true);

        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(EgaliteFemmesHommesBeVerdict.MANQUEMENT_GRAVE_RAPPORT_NON_DEPOSE);
        assertThat(r.raison()).isEqualTo("RAPPORT_NON_DEPOSE");
        assertThat(r.synthese()).contains("art. 195/1", "niveau 2", "IEFH");
        assertThat(r.rapportDepose()).isFalse();
    }

    // ── Hiérarchie 4 : ventilation incomplète ────────────────────────────────

    @Test
    void analyze_niveauFonctionManquant_returnsRapportIncomplet() {
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                80,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                false, true, true, true, true,
                false, null, true, true, false);

        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(EgaliteFemmesHommesBeVerdict.NON_CONFORME_RAPPORT_INCOMPLET);
        assertThat(r.ventilationComplete()).isFalse();
        assertThat(r.raison()).isEqualTo("VENTILATION_INCOMPLETE");
        assertThat(r.synthese()).contains("niveau de fonction");
    }

    @Test
    void analyze_anciennteManquante_returnsRapportIncomplet() {
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                80,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, false, true, true, true,
                false, null, true, true, false);

        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(EgaliteFemmesHommesBeVerdict.NON_CONFORME_RAPPORT_INCOMPLET);
        assertThat(r.synthese()).contains("ancienneté");
    }

    @Test
    void analyze_qualificationManquante_returnsRapportIncomplet() {
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                80,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, false, true, true,
                false, null, true, true, false);

        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(EgaliteFemmesHommesBeVerdict.NON_CONFORME_RAPPORT_INCOMPLET);
        assertThat(r.synthese()).contains("qualification");
    }

    @Test
    void analyze_regimeTravailManquant_returnsRapportIncomplet() {
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                80,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, true, false, true,
                false, null, true, true, false);

        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(EgaliteFemmesHommesBeVerdict.NON_CONFORME_RAPPORT_INCOMPLET);
        assertThat(r.synthese()).contains("régime de travail");
    }

    @Test
    void analyze_composantsManquants_returnsRapportIncomplet() {
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                80,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, true, true, false,
                false, null, true, true, false);

        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(EgaliteFemmesHommesBeVerdict.NON_CONFORME_RAPPORT_INCOMPLET);
        assertThat(r.synthese()).contains("composants");
    }

    @Test
    void analyze_statutDeposeIncomplet_returnsRapportIncomplet() {
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                80,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_INCOMPLET,
                DATE_DEPOT,
                true, true, true, true, true,
                false, null, true, true, false);

        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(EgaliteFemmesHommesBeVerdict.NON_CONFORME_RAPPORT_INCOMPLET);
    }

    @Test
    void analyze_cinqVentilationsManquantes_listeesDansSynthese() {
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                80,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                false, false, false, false, false,
                false, null, true, true, false);

        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(EgaliteFemmesHommesBeVerdict.NON_CONFORME_RAPPORT_INCOMPLET);
        assertThat(r.synthese())
                .contains("niveau de fonction", "ancienneté",
                        "qualification", "régime de travail", "composants");
    }

    // ── Hiérarchie 5 : plan d'action manquant ────────────────────────────────

    @Test
    void analyze_ecartConstateSansPlanAction_returnsPlanActionManquant() {
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                80,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, true, true, true,
                true, BigDecimal.valueOf(12.3),
                false, // pas de plan d'action
                true, false);

        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(EgaliteFemmesHommesBeVerdict.NON_CONFORME_PLAN_ACTION_MANQUANT);
        assertThat(r.planActionRequis()).isTrue();
        assertThat(r.planActionConforme()).isFalse();
        assertThat(r.raison()).isEqualTo("PLAN_ACTION_MANQUANT");
        assertThat(r.synthese()).contains("12.3", "plan d'action");
    }

    @Test
    void analyze_ecartConstateSansPlanActionSansPourcentage_synthseSansEcart() {
        // pas de pourcentage fourni : la synthèse ne doit pas afficher
        // " (écart de l'ordre de null %)" malformé
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                80,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, true, true, true,
                true, null,
                false, true, false);

        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(EgaliteFemmesHommesBeVerdict.NON_CONFORME_PLAN_ACTION_MANQUANT);
        assertThat(r.synthese()).doesNotContain("null");
    }

    // ── Priorisation entre verdicts ─────────────────────────────────────────

    @Test
    void analyze_seuilNonAtteintPrimeRapportNonDepose() {
        // effectif 20 + NON_DEPOSE → hors champ gagne (pas d'obligation)
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                20,
                EgaliteFemmesHommesBeStatutRapport.NON_DEPOSE_DELAI_DEPASSE,
                null,
                false, false, false, false, false,
                false, null, false, false, false);

        assertThat(EgaliteFemmesHommesBeChecker.analyze(req).verdict())
                .isEqualTo(EgaliteFemmesHommesBeVerdict.HORS_CHAMP_SEUIL_NON_ATTEINT);
    }

    @Test
    void analyze_statutIndeterminePrimeSeuilNonAtteint() {
        // INDETERMINE prime même hors seuil — on ne préjuge pas
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                20,
                EgaliteFemmesHommesBeStatutRapport.INDETERMINE,
                null,
                false, false, false, false, false,
                false, null, false, false, false);

        assertThat(EgaliteFemmesHommesBeChecker.analyze(req).verdict())
                .isEqualTo(EgaliteFemmesHommesBeVerdict.A_ANALYSER);
    }

    @Test
    void analyze_enPreparationPrimeSeuilNonAtteint() {
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                10,
                EgaliteFemmesHommesBeStatutRapport.EN_PREPARATION,
                null,
                false, false, false, false, false,
                false, null, false, false, false);

        assertThat(EgaliteFemmesHommesBeChecker.analyze(req).verdict())
                .isEqualTo(EgaliteFemmesHommesBeVerdict.A_ANALYSER);
    }

    @Test
    void analyze_ventilationIncompletePrimePlanActionManquant() {
        // ventilation incomplète + plan action manquant : ventilation gagne
        EgaliteFemmesHommesBeRequest req = new EgaliteFemmesHommesBeRequest(
                80,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                false, true, true, true, true,
                true, BigDecimal.valueOf(15),
                false, true, false);

        assertThat(EgaliteFemmesHommesBeChecker.analyze(req).verdict())
                .isEqualTo(EgaliteFemmesHommesBeVerdict.NON_CONFORME_RAPPORT_INCOMPLET);
    }

    // ── Snapshot inputs ─────────────────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(reqConforme());

        assertThat(r.effectifEntreprise()).isEqualTo(75);
        assertThat(r.statutRapport())
                .isEqualTo(EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET);
        assertThat(r.dateDepotRapport()).isEqualTo(DATE_DEPOT);
        assertThat(r.ventilationNiveauFonctionFournie()).isTrue();
        assertThat(r.ventilationAncienneteFournie()).isTrue();
        assertThat(r.ventilationQualificationFournie()).isTrue();
        assertThat(r.ventilationRegimeTravailFournie()).isTrue();
        assertThat(r.ventilationComposantsRemunerationFournie()).isTrue();
        assertThat(r.ecartSalarialNonJustifieConstate()).isFalse();
        assertThat(r.planActionEtabli()).isTrue();
        assertThat(r.mediateurDesigne()).isTrue();
        assertThat(r.plainteIefhOuInspectionEnCours()).isFalse();
    }

    // ── Base juridique + avertissement ──────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceLoi22042012EtArticles() {
        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(reqConforme());

        assertThat(r.baseJuridique())
                .contains("Loi du 22/04/2012")
                .contains("art. 2")
                .contains("art. 5")
                .contains("art. 12")
                .contains("art. 14")
                .contains("17/08/2013")
                .contains("25/04/2014")
                .contains("CCT n° 25")
                .contains("10/05/2007")
                .contains("art. 195/1")
                .contains("04/12/2007");
    }

    @Test
    void analyze_avertissementSystematiquePresent() {
        EgaliteFemmesHommesBeResult r =
                EgaliteFemmesHommesBeChecker.analyze(reqConforme());

        assertThat(r.avertissement())
                .isNotNull()
                .contains("avocat")
                .contains("ETP")
                .contains("CCT")
                .contains("IEFH");
    }

    // ── Validation conditionnelle ───────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() -> EgaliteFemmesHommesBeChecker.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_effectifNull_throws() {
        EgaliteFemmesHommesBeRequest bad = new EgaliteFemmesHommesBeRequest(
                null,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, true, true, true,
                false, null, true, true, false);

        assertThatThrownBy(() -> EgaliteFemmesHommesBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectifEntreprise");
    }

    @Test
    void analyze_effectifNegatif_throws() {
        EgaliteFemmesHommesBeRequest bad = new EgaliteFemmesHommesBeRequest(
                -1,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, true, true, true,
                false, null, true, true, false);

        assertThatThrownBy(() -> EgaliteFemmesHommesBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectifEntreprise");
    }

    @Test
    void analyze_statutRapportNull_throws() {
        EgaliteFemmesHommesBeRequest bad = new EgaliteFemmesHommesBeRequest(
                75,
                null,
                DATE_DEPOT,
                true, true, true, true, true,
                false, null, true, true, false);

        assertThatThrownBy(() -> EgaliteFemmesHommesBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("statutRapport");
    }

    @Test
    void analyze_ventilationNiveauFonctionNull_throws() {
        EgaliteFemmesHommesBeRequest bad = new EgaliteFemmesHommesBeRequest(
                75,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                null, true, true, true, true,
                false, null, true, true, false);

        assertThatThrownBy(() -> EgaliteFemmesHommesBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ventilationNiveauFonctionFournie");
    }

    @Test
    void analyze_ventilationAncienneteNull_throws() {
        EgaliteFemmesHommesBeRequest bad = new EgaliteFemmesHommesBeRequest(
                75,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, null, true, true, true,
                false, null, true, true, false);

        assertThatThrownBy(() -> EgaliteFemmesHommesBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ventilationAncienneteFournie");
    }

    @Test
    void analyze_ventilationQualificationNull_throws() {
        EgaliteFemmesHommesBeRequest bad = new EgaliteFemmesHommesBeRequest(
                75,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, null, true, true,
                false, null, true, true, false);

        assertThatThrownBy(() -> EgaliteFemmesHommesBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ventilationQualificationFournie");
    }

    @Test
    void analyze_ventilationRegimeTravailNull_throws() {
        EgaliteFemmesHommesBeRequest bad = new EgaliteFemmesHommesBeRequest(
                75,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, true, null, true,
                false, null, true, true, false);

        assertThatThrownBy(() -> EgaliteFemmesHommesBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ventilationRegimeTravailFournie");
    }

    @Test
    void analyze_ventilationComposantsNull_throws() {
        EgaliteFemmesHommesBeRequest bad = new EgaliteFemmesHommesBeRequest(
                75,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, true, true, null,
                false, null, true, true, false);

        assertThatThrownBy(() -> EgaliteFemmesHommesBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ventilationComposantsRemunerationFournie");
    }

    @Test
    void analyze_ecartConstateNull_throws() {
        EgaliteFemmesHommesBeRequest bad = new EgaliteFemmesHommesBeRequest(
                75,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, true, true, true,
                null, null, true, true, false);

        assertThatThrownBy(() -> EgaliteFemmesHommesBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ecartSalarialNonJustifieConstate");
    }

    @Test
    void analyze_planActionEtabliNull_throws() {
        EgaliteFemmesHommesBeRequest bad = new EgaliteFemmesHommesBeRequest(
                75,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, true, true, true,
                false, null, null, true, false);

        assertThatThrownBy(() -> EgaliteFemmesHommesBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("planActionEtabli");
    }

    @Test
    void analyze_mediateurDesigneNull_throws() {
        EgaliteFemmesHommesBeRequest bad = new EgaliteFemmesHommesBeRequest(
                75,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, true, true, true,
                false, null, true, null, false);

        assertThatThrownBy(() -> EgaliteFemmesHommesBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mediateurDesigne");
    }

    @Test
    void analyze_plainteIefhNull_throws() {
        EgaliteFemmesHommesBeRequest bad = new EgaliteFemmesHommesBeRequest(
                75,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, true, true, true,
                false, null, true, true, null);

        assertThatThrownBy(() -> EgaliteFemmesHommesBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("plainteIefhOuInspectionEnCours");
    }

    @Test
    void analyze_pourcentageEcartNegatif_throws() {
        EgaliteFemmesHommesBeRequest bad = new EgaliteFemmesHommesBeRequest(
                75,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, true, true, true,
                true, BigDecimal.valueOf(-1.0), true, true, false);

        assertThatThrownBy(() -> EgaliteFemmesHommesBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pourcentageEcartConstate");
    }

    @Test
    void analyze_pourcentageEcartSuperieur100_throws() {
        EgaliteFemmesHommesBeRequest bad = new EgaliteFemmesHommesBeRequest(
                75,
                EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET,
                DATE_DEPOT,
                true, true, true, true, true,
                true, BigDecimal.valueOf(150), true, true, false);

        assertThatThrownBy(() -> EgaliteFemmesHommesBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pourcentageEcartConstate");
    }
}
