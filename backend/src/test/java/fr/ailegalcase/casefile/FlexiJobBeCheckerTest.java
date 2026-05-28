package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-12 : tests unitaires du {@link FlexiJobBeChecker}.
 *
 * <p>Couvre : conditions cumulatives travailleur (pensionné / salarié
 * 4/5 ETP T-3 / autre), secteur employeur (HoReCa initial + ext.
 * 2015/2018/2023 + zone grise + non éligible), interdiction de cumul
 * chez le même employeur, formalisme (contrat-cadre + Dimona FLX),
 * rémunération (flexi-salaire minimum indexé + plafond annuel exonéré
 * inapplicable aux pensionnés), hiérarchie des verdicts, validation
 * des inputs et base juridique stable.</p>
 */
class FlexiJobBeCheckerTest {

    private static final LocalDate DATE_OCC = LocalDate.of(2024, 6, 1);

    private static FlexiJobBeRequest reqEligible() {
        return new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                false,
                true,
                true,
                new BigDecimal("13.31"),
                new BigDecimal("12.33"),
                new BigDecimal("5000.00"),
                new BigDecimal("12000.00"));
    }

    // ── Cas éligible nominal ────────────────────────────────────────────────

    @Test
    void analyze_eligibleNominal_returnsEligible() {
        FlexiJobBeResult r = FlexiJobBeChecker.analyze(reqEligible());

        assertThat(r.verdict()).isEqualTo(FlexiJobBeVerdict.ELIGIBLE);
        assertThat(r.travailleurEligible()).isTrue();
        assertThat(r.secteurEligible()).isTrue();
        assertThat(r.cumulRespecte()).isTrue();
        assertThat(r.formalismeRespecte()).isTrue();
        assertThat(r.remunerationConforme()).isTrue();
        assertThat(r.montantExcedentaireAuPlafond())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.raison()).isEqualTo("FLEXI_JOB_REGULIER");
        assertThat(r.synthese()).contains("pleinement régulière");
    }

    @Test
    void analyze_pensionne_eligibleSansPlafondCheck() {
        FlexiJobBeRequest req = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.PENSIONNE,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                false, true, true,
                new BigDecimal("13.31"),
                new BigDecimal("12.33"),
                // Revenu très au-dessus du plafond mais pensionné →
                // plafond inapplicable depuis extension 2018.
                new BigDecimal("50000.00"),
                new BigDecimal("12000.00"));

        FlexiJobBeResult r = FlexiJobBeChecker.analyze(req);

        assertThat(r.verdict()).isEqualTo(FlexiJobBeVerdict.ELIGIBLE);
        assertThat(r.remunerationConforme()).isTrue();
        assertThat(r.montantExcedentaireAuPlafond())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.synthese())
                .contains("plafond inapplicable")
                .contains("pensionné");
    }

    // ── Secteur éligible : couverture des extensions ────────────────────────

    @Test
    void analyze_secteurBoulangerieCoiffure_ext2015_eligible() {
        FlexiJobBeRequest req = withSecteur(
                FlexiJobBeSecteurEmployeur.BOULANGERIE_COIFFURE);
        assertThat(FlexiJobBeChecker.analyze(req).verdict())
                .isEqualTo(FlexiJobBeVerdict.ELIGIBLE);
    }

    @Test
    void analyze_secteurCommerceDetail_ext2018_eligible() {
        FlexiJobBeRequest req = withSecteur(
                FlexiJobBeSecteurEmployeur.COMMERCE_DETAIL);
        assertThat(FlexiJobBeChecker.analyze(req).verdict())
                .isEqualTo(FlexiJobBeVerdict.ELIGIBLE);
    }

    @Test
    void analyze_secteurAgriculture_ext2018_eligible() {
        FlexiJobBeRequest req = withSecteur(
                FlexiJobBeSecteurEmployeur.AGRICULTURE);
        assertThat(FlexiJobBeChecker.analyze(req).verdict())
                .isEqualTo(FlexiJobBeVerdict.ELIGIBLE);
    }

    @Test
    void analyze_secteurSoinsSante_ext2018_eligible() {
        FlexiJobBeRequest req = withSecteur(
                FlexiJobBeSecteurEmployeur.SOINS_DE_SANTE);
        assertThat(FlexiJobBeChecker.analyze(req).verdict())
                .isEqualTo(FlexiJobBeVerdict.ELIGIBLE);
    }

    @Test
    void analyze_secteurSportCultureEvenementiel_ext2023_eligible() {
        FlexiJobBeRequest req = withSecteur(
                FlexiJobBeSecteurEmployeur.SPORT_CULTURE_EVENEMENTIEL);
        assertThat(FlexiJobBeChecker.analyze(req).verdict())
                .isEqualTo(FlexiJobBeVerdict.ELIGIBLE);
    }

    @Test
    void analyze_secteurPublic_ext2023_eligible() {
        FlexiJobBeRequest req = withSecteur(
                FlexiJobBeSecteurEmployeur.SECTEUR_PUBLIC);
        assertThat(FlexiJobBeChecker.analyze(req).verdict())
                .isEqualTo(FlexiJobBeVerdict.ELIGIBLE);
    }

    // ── Hiérarchie 1 : travailleur hors condition (sortie immédiate) ────────

    @Test
    void analyze_travailleurAutre_returnsTravailleurHorsCondition() {
        FlexiJobBeRequest req = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.AUTRE,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                false, true, true,
                new BigDecimal("13.31"),
                new BigDecimal("12.33"),
                new BigDecimal("5000.00"),
                new BigDecimal("12000.00"));

        FlexiJobBeResult r = FlexiJobBeChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(FlexiJobBeVerdict.INELIGIBLE_TRAVAILLEUR_HORS_CONDITION);
        assertThat(r.travailleurEligible()).isFalse();
        assertThat(r.raison()).isEqualTo("TRAVAILLEUR_HORS_CONDITION");
        assertThat(r.synthese()).contains("4/5 ETP").contains("T-3");
        assertThat(r.montantExcedentaireAuPlafond())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── Hiérarchie 2 : secteur non éligible (prime sur cumul / formalisme) ─

    @Test
    void analyze_secteurNonEligible_returnsSecteurNonEligible() {
        FlexiJobBeRequest req = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.AUTRE_NON_ELIGIBLE,
                true, false, false,
                new BigDecimal("5.00"),
                new BigDecimal("12.33"),
                new BigDecimal("30000.00"),
                new BigDecimal("12000.00"));

        FlexiJobBeResult r = FlexiJobBeChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(FlexiJobBeVerdict.INELIGIBLE_SECTEUR_NON_ELIGIBLE);
        assertThat(r.travailleurEligible()).isTrue();
        assertThat(r.secteurEligible()).isFalse();
        assertThat(r.raison()).isEqualTo("SECTEUR_NON_ELIGIBLE");
        assertThat(r.synthese()).contains("lois-programmes successives");
    }

    // ── Hiérarchie 3 : zone grise → A_ANALYSER ──────────────────────────────

    @Test
    void analyze_zoneGriseExtensionRecente_returnsAanalyser() {
        FlexiJobBeRequest req = withSecteur(
                FlexiJobBeSecteurEmployeur.ZONE_GRISE_EXTENSION_RECENTE);
        FlexiJobBeResult r = FlexiJobBeChecker.analyze(req);

        assertThat(r.verdict()).isEqualTo(FlexiJobBeVerdict.A_ANALYSER);
        assertThat(r.raison()).isEqualTo("SECTEUR_ZONE_GRISE");
        assertThat(r.synthese()).contains("avocat BE");
    }

    // ── Hiérarchie 4 : cumul interdit même employeur ────────────────────────

    @Test
    void analyze_dejaOccupeMemeEmployeur_returnsCumulInterdit() {
        FlexiJobBeRequest req = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                true, true, true,
                new BigDecimal("13.31"),
                new BigDecimal("12.33"),
                new BigDecimal("5000.00"),
                new BigDecimal("12000.00"));

        FlexiJobBeResult r = FlexiJobBeChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(FlexiJobBeVerdict.INELIGIBLE_CUMUL_INTERDIT_MEME_EMPLOYEUR);
        assertThat(r.cumulRespecte()).isFalse();
        assertThat(r.raison()).isEqualTo("CUMUL_INTERDIT_MEME_EMPLOYEUR");
        assertThat(r.synthese()).contains("anti-substitution");
    }

    // ── Hiérarchie 5 : formalisme défaillant ────────────────────────────────

    @Test
    void analyze_contratCadreAbsent_returnsFragileContrat() {
        FlexiJobBeRequest req = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                false, false, true,
                new BigDecimal("13.31"),
                new BigDecimal("12.33"),
                new BigDecimal("5000.00"),
                new BigDecimal("12000.00"));

        FlexiJobBeResult r = FlexiJobBeChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(FlexiJobBeVerdict.FRAGILE_CONTRAT_OU_DIMONA_MANQUANT);
        assertThat(r.formalismeRespecte()).isFalse();
        assertThat(r.raison()).isEqualTo("CONTRAT_OU_DIMONA_MANQUANT");
        assertThat(r.synthese()).contains("contrat-cadre");
    }

    @Test
    void analyze_dimonaAbsente_returnsFragileDimona() {
        FlexiJobBeRequest req = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                false, true, false,
                new BigDecimal("13.31"),
                new BigDecimal("12.33"),
                new BigDecimal("5000.00"),
                new BigDecimal("12000.00"));

        FlexiJobBeResult r = FlexiJobBeChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(FlexiJobBeVerdict.FRAGILE_CONTRAT_OU_DIMONA_MANQUANT);
        assertThat(r.synthese()).contains("Dimona");
    }

    @Test
    void analyze_lesDeuxAbsents_diagCombine() {
        FlexiJobBeRequest req = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                false, false, false,
                new BigDecimal("13.31"),
                new BigDecimal("12.33"),
                new BigDecimal("5000.00"),
                new BigDecimal("12000.00"));

        FlexiJobBeResult r = FlexiJobBeChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(FlexiJobBeVerdict.FRAGILE_CONTRAT_OU_DIMONA_MANQUANT);
        assertThat(r.synthese())
                .contains("contrat-cadre")
                .contains("Dimona FLX");
    }

    // ── Hiérarchie 6 : plafond / flexi-salaire défaillant ───────────────────

    @Test
    void analyze_salaireSousMinimum_returnsFragilePlafond() {
        FlexiJobBeRequest req = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                false, true, true,
                new BigDecimal("8.00"),    // sous le min 12,33
                new BigDecimal("12.33"),
                new BigDecimal("5000.00"),
                new BigDecimal("12000.00"));

        FlexiJobBeResult r = FlexiJobBeChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(FlexiJobBeVerdict.FRAGILE_PLAFOND_DEPASSE);
        assertThat(r.remunerationConforme()).isFalse();
        assertThat(r.raison()).isEqualTo("PLAFOND_OU_SALAIRE_DEFAILLANT");
        assertThat(r.synthese()).contains("sous le minimum");
    }

    @Test
    void analyze_plafondDepasse_returnsFragilePlafond_etCalculeExcedent() {
        FlexiJobBeRequest req = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                false, true, true,
                new BigDecimal("13.31"),
                new BigDecimal("12.33"),
                new BigDecimal("15000.00"),
                new BigDecimal("12000.00"));

        FlexiJobBeResult r = FlexiJobBeChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(FlexiJobBeVerdict.FRAGILE_PLAFOND_DEPASSE);
        assertThat(r.montantExcedentaireAuPlafond())
                .isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(r.synthese()).contains("excédent");
    }

    @Test
    void analyze_salaireEtPlafondDefaillants_diagCombine() {
        FlexiJobBeRequest req = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                false, true, true,
                new BigDecimal("8.00"),
                new BigDecimal("12.33"),
                new BigDecimal("20000.00"),
                new BigDecimal("12000.00"));

        FlexiJobBeResult r = FlexiJobBeChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(FlexiJobBeVerdict.FRAGILE_PLAFOND_DEPASSE);
        assertThat(r.synthese())
                .contains("sous le minimum")
                .contains("au-delà du plafond")
                .contains("8000");
    }

    @Test
    void analyze_revenuEgalAuPlafond_pasDeDepassement() {
        FlexiJobBeRequest req = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                false, true, true,
                new BigDecimal("13.31"),
                new BigDecimal("12.33"),
                new BigDecimal("12000.00"),
                new BigDecimal("12000.00"));

        FlexiJobBeResult r = FlexiJobBeChecker.analyze(req);

        assertThat(r.verdict()).isEqualTo(FlexiJobBeVerdict.ELIGIBLE);
        assertThat(r.montantExcedentaireAuPlafond())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void analyze_salaireEgalAuMinimum_estConforme() {
        FlexiJobBeRequest req = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                false, true, true,
                new BigDecimal("12.33"),
                new BigDecimal("12.33"),
                new BigDecimal("5000.00"),
                new BigDecimal("12000.00"));

        FlexiJobBeResult r = FlexiJobBeChecker.analyze(req);

        assertThat(r.verdict()).isEqualTo(FlexiJobBeVerdict.ELIGIBLE);
    }

    // ── Hiérarchie : travailleur prime sur secteur / cumul / formalisme ────

    @Test
    void analyze_travailleurAutreEtSecteurNonEligible_travailleurWins() {
        FlexiJobBeRequest req = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.AUTRE,
                FlexiJobBeSecteurEmployeur.AUTRE_NON_ELIGIBLE,
                true, false, false,
                new BigDecimal("5.00"),
                new BigDecimal("12.33"),
                new BigDecimal("30000.00"),
                new BigDecimal("12000.00"));

        assertThat(FlexiJobBeChecker.analyze(req).verdict())
                .isEqualTo(FlexiJobBeVerdict.INELIGIBLE_TRAVAILLEUR_HORS_CONDITION);
    }

    @Test
    void analyze_secteurNonEligibleEtCumulInterdit_secteurWins() {
        FlexiJobBeRequest req = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.PENSIONNE,
                FlexiJobBeSecteurEmployeur.AUTRE_NON_ELIGIBLE,
                true, false, false,
                new BigDecimal("13.31"),
                new BigDecimal("12.33"),
                new BigDecimal("5000.00"),
                new BigDecimal("12000.00"));

        assertThat(FlexiJobBeChecker.analyze(req).verdict())
                .isEqualTo(FlexiJobBeVerdict.INELIGIBLE_SECTEUR_NON_ELIGIBLE);
    }

    @Test
    void analyze_cumulInterditEtFormalismeManquant_cumulWins() {
        FlexiJobBeRequest req = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                true, false, false,
                new BigDecimal("13.31"),
                new BigDecimal("12.33"),
                new BigDecimal("5000.00"),
                new BigDecimal("12000.00"));

        assertThat(FlexiJobBeChecker.analyze(req).verdict())
                .isEqualTo(FlexiJobBeVerdict.INELIGIBLE_CUMUL_INTERDIT_MEME_EMPLOYEUR);
    }

    @Test
    void analyze_formalismeOkPlafondDepasse_plafondVerdict() {
        FlexiJobBeRequest req = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                false, true, true,
                new BigDecimal("13.31"),
                new BigDecimal("12.33"),
                new BigDecimal("20000.00"),
                new BigDecimal("12000.00"));

        // formalisme OK → on tombe sur plafond (priorité formalisme avant
        // plafond garantie).
        assertThat(FlexiJobBeChecker.analyze(req).verdict())
                .isEqualTo(FlexiJobBeVerdict.FRAGILE_PLAFOND_DEPASSE);
    }

    @Test
    void analyze_formalismeManquantEtPlafondDepasse_formalismeWins() {
        FlexiJobBeRequest req = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                false, false, false,
                new BigDecimal("13.31"),
                new BigDecimal("12.33"),
                new BigDecimal("20000.00"),
                new BigDecimal("12000.00"));

        // formalisme prime sur plafond — pattern hiérarchie 5 > 6.
        assertThat(FlexiJobBeChecker.analyze(req).verdict())
                .isEqualTo(FlexiJobBeVerdict.FRAGILE_CONTRAT_OU_DIMONA_MANQUANT);
    }

    // ── Snapshot inputs ─────────────────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        FlexiJobBeResult r = FlexiJobBeChecker.analyze(reqEligible());

        assertThat(r.datePremiereOccupationFlexi()).isEqualTo(DATE_OCC);
        assertThat(r.statutTravailleur())
                .isEqualTo(FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3);
        assertThat(r.secteurEmployeur())
                .isEqualTo(FlexiJobBeSecteurEmployeur.HORECA_302);
        assertThat(r.dejaOccupeChezMemeEmployeur()).isFalse();
        assertThat(r.contratCadreSigne()).isTrue();
        assertThat(r.dimonaFlxDeclaree()).isTrue();
        assertThat(r.flexiSalaireHoraireBrut())
                .isEqualByComparingTo("13.31");
        assertThat(r.flexiSalaireMinimumApplicable())
                .isEqualByComparingTo("12.33");
        assertThat(r.revenuAnnuelFlexiCumule())
                .isEqualByComparingTo("5000.00");
        assertThat(r.plafondAnnuelExonere())
                .isEqualByComparingTo("12000.00");
    }

    // ── Base juridique + avertissement ──────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceLoisProgrammeEtExtensions() {
        FlexiJobBeResult r = FlexiJobBeChecker.analyze(reqEligible());

        assertThat(r.baseJuridique())
                .contains("Loi-programme du 26 décembre 2013")
                .contains("art. 13")
                .contains("Loi du 16/11/2015")
                .contains("Loi-programme du 30/10/2018")
                .contains("Loi-programme du 28/12/2023")
                .contains("AR du 02/06/2024")
                .contains("Dimona");
    }

    @Test
    void analyze_avertissementSystematiquePresent() {
        FlexiJobBeResult r = FlexiJobBeChecker.analyze(reqEligible());

        assertThat(r.avertissement())
                .isNotNull()
                .contains("avocat")
                .contains("ONSS");
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() -> FlexiJobBeChecker.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_dateNull_throws() {
        FlexiJobBeRequest bad = new FlexiJobBeRequest(
                null,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                false, true, true,
                new BigDecimal("13.31"),
                new BigDecimal("12.33"),
                new BigDecimal("5000.00"),
                new BigDecimal("12000.00"));

        assertThatThrownBy(() -> FlexiJobBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("datePremiereOccupationFlexi");
    }

    @Test
    void analyze_statutTravailleurNull_throws() {
        FlexiJobBeRequest bad = new FlexiJobBeRequest(
                DATE_OCC, null,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                false, true, true,
                new BigDecimal("13.31"),
                new BigDecimal("12.33"),
                new BigDecimal("5000.00"),
                new BigDecimal("12000.00"));

        assertThatThrownBy(() -> FlexiJobBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("statutTravailleur");
    }

    @Test
    void analyze_secteurNull_throws() {
        FlexiJobBeRequest bad = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                null,
                false, true, true,
                new BigDecimal("13.31"),
                new BigDecimal("12.33"),
                new BigDecimal("5000.00"),
                new BigDecimal("12000.00"));

        assertThatThrownBy(() -> FlexiJobBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("secteurEmployeur");
    }

    @Test
    void analyze_cumulNull_throws() {
        FlexiJobBeRequest bad = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                null, true, true,
                new BigDecimal("13.31"),
                new BigDecimal("12.33"),
                new BigDecimal("5000.00"),
                new BigDecimal("12000.00"));

        assertThatThrownBy(() -> FlexiJobBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dejaOccupeChezMemeEmployeur");
    }

    @Test
    void analyze_contratNull_throws() {
        FlexiJobBeRequest bad = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                false, null, true,
                new BigDecimal("13.31"),
                new BigDecimal("12.33"),
                new BigDecimal("5000.00"),
                new BigDecimal("12000.00"));

        assertThatThrownBy(() -> FlexiJobBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contratCadreSigne");
    }

    @Test
    void analyze_dimonaNull_throws() {
        FlexiJobBeRequest bad = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                false, true, null,
                new BigDecimal("13.31"),
                new BigDecimal("12.33"),
                new BigDecimal("5000.00"),
                new BigDecimal("12000.00"));

        assertThatThrownBy(() -> FlexiJobBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dimonaFlxDeclaree");
    }

    @Test
    void analyze_salaireHoraireNull_throws() {
        FlexiJobBeRequest bad = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                false, true, true,
                null,
                new BigDecimal("12.33"),
                new BigDecimal("5000.00"),
                new BigDecimal("12000.00"));

        assertThatThrownBy(() -> FlexiJobBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("flexiSalaireHoraireBrut");
    }

    @Test
    void analyze_salaireHoraireNegatif_throws() {
        FlexiJobBeRequest bad = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                false, true, true,
                new BigDecimal("-1.00"),
                new BigDecimal("12.33"),
                new BigDecimal("5000.00"),
                new BigDecimal("12000.00"));

        assertThatThrownBy(() -> FlexiJobBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("flexiSalaireHoraireBrut");
    }

    @Test
    void analyze_salaireMinimumZero_throws() {
        FlexiJobBeRequest bad = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                false, true, true,
                new BigDecimal("13.31"),
                BigDecimal.ZERO,
                new BigDecimal("5000.00"),
                new BigDecimal("12000.00"));

        assertThatThrownBy(() -> FlexiJobBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("flexiSalaireMinimumApplicable");
    }

    @Test
    void analyze_revenuAnnuelNull_throws() {
        FlexiJobBeRequest bad = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                false, true, true,
                new BigDecimal("13.31"),
                new BigDecimal("12.33"),
                null,
                new BigDecimal("12000.00"));

        assertThatThrownBy(() -> FlexiJobBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revenuAnnuelFlexiCumule");
    }

    @Test
    void analyze_plafondZero_throws() {
        FlexiJobBeRequest bad = new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                FlexiJobBeSecteurEmployeur.HORECA_302,
                false, true, true,
                new BigDecimal("13.31"),
                new BigDecimal("12.33"),
                new BigDecimal("5000.00"),
                BigDecimal.ZERO);

        assertThatThrownBy(() -> FlexiJobBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("plafondAnnuelExonere");
    }

    // ── Helper ─────────────────────────────────────────────────────────────

    private static FlexiJobBeRequest withSecteur(
            FlexiJobBeSecteurEmployeur secteur) {
        return new FlexiJobBeRequest(
                DATE_OCC,
                FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3,
                secteur,
                false, true, true,
                new BigDecimal("13.31"),
                new BigDecimal("12.33"),
                new BigDecimal("5000.00"),
                new BigDecimal("12000.00"));
    }
}
