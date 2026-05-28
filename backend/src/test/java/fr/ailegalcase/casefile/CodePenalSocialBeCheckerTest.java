package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-24 : tests unitaires du {@link CodePenalSocialBeChecker}.
 *
 * <p>Couvre : table niveau par type (art. 119-235 C. pén. soc.),
 * ajustement intentionnel (ABSENCE_DMFA + INFRACTION_DETACHEMENT),
 * niveauPropose prime sur défaut, A_QUALIFIER si AUTRE_QUALIFICATION
 * sans niveau, bornes amendes art. 101 (1-2-3-4), emprisonnement
 * niveau 4 art. 103 (6 mois à 3 ans), drapeaux majorations (× nb
 * travailleurs, × 5 personne morale, × 2 récidive art. 110), base
 * juridique stable, avertissement avocat BE, validation inputs.</p>
 */
class CodePenalSocialBeCheckerTest {

    private static final LocalDate DATE_FAITS = LocalDate.of(2024, 3, 15);

    /** Body « simple » — infraction niveau 3 par défaut, sans majoration. */
    private static CodePenalSocialBeRequest reqSimple(
            CodePenalSocialBeTypeInfraction type) {
        return new CodePenalSocialBeRequest(
                type,
                null,
                DATE_FAITS,
                0,
                false, false, false, false);
    }

    // ── Table niveau par type ───────────────────────────────────────────────

    @Test
    void analyze_travailNonDeclare_returnsNiveau4() {
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(
                reqSimple(CodePenalSocialBeTypeInfraction.TRAVAIL_NON_DECLARE));
        assertThat(r.verdict()).isEqualTo(CodePenalSocialBeVerdict.SANCTION_NIVEAU_4);
        assertThat(r.niveauSanction()).isEqualTo(4);
        assertThat(r.emprisonnementApplicable()).isTrue();
        assertThat(r.emprisonnementMinMois()).isEqualTo(6);
        assertThat(r.emprisonnementMaxMois()).isEqualTo(36);
    }

    @Test
    void analyze_absenceDimona_returnsNiveau4() {
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(
                reqSimple(CodePenalSocialBeTypeInfraction.ABSENCE_DIMONA));
        assertThat(r.verdict()).isEqualTo(CodePenalSocialBeVerdict.SANCTION_NIVEAU_4);
    }

    @Test
    void analyze_absenceDmfaNonIntentionnel_returnsNiveau2() {
        CodePenalSocialBeRequest req = new CodePenalSocialBeRequest(
                CodePenalSocialBeTypeInfraction.ABSENCE_DMFA,
                null, DATE_FAITS, 0,
                false, false, false,
                false); // non intentionnel
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(req);
        assertThat(r.verdict()).isEqualTo(CodePenalSocialBeVerdict.SANCTION_NIVEAU_2);
        assertThat(r.niveauSanction()).isEqualTo(2);
        assertThat(r.emprisonnementApplicable()).isFalse();
    }

    @Test
    void analyze_absenceDmfaIntentionnel_returnsNiveau4() {
        CodePenalSocialBeRequest req = new CodePenalSocialBeRequest(
                CodePenalSocialBeTypeInfraction.ABSENCE_DMFA,
                null, DATE_FAITS, 0,
                false, false, false,
                true); // intentionnel
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(req);
        assertThat(r.verdict()).isEqualTo(CodePenalSocialBeVerdict.SANCTION_NIVEAU_4);
        assertThat(r.emprisonnementApplicable()).isTrue();
    }

    @Test
    void analyze_nonPaiementRemuneration_returnsNiveau2() {
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(
                reqSimple(CodePenalSocialBeTypeInfraction.NON_PAIEMENT_REMUNERATION));
        assertThat(r.verdict()).isEqualTo(CodePenalSocialBeVerdict.SANCTION_NIVEAU_2);
        assertThat(r.niveauSanction()).isEqualTo(2);
    }

    @Test
    void analyze_depassementTempsTravail_returnsNiveau3() {
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(
                reqSimple(CodePenalSocialBeTypeInfraction.DEPASSEMENT_TEMPS_TRAVAIL));
        assertThat(r.verdict()).isEqualTo(CodePenalSocialBeVerdict.SANCTION_NIVEAU_3);
        assertThat(r.niveauSanction()).isEqualTo(3);
        assertThat(r.emprisonnementApplicable()).isFalse();
    }

    @Test
    void analyze_infractionBienEtre_returnsNiveau3() {
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(
                reqSimple(CodePenalSocialBeTypeInfraction.INFRACTION_BIEN_ETRE));
        assertThat(r.verdict()).isEqualTo(CodePenalSocialBeVerdict.SANCTION_NIVEAU_3);
    }

    @Test
    void analyze_accidentGraveNonDeclare_returnsNiveau4() {
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(
                reqSimple(CodePenalSocialBeTypeInfraction.ACCIDENT_GRAVE_NON_DECLARE));
        assertThat(r.verdict()).isEqualTo(CodePenalSocialBeVerdict.SANCTION_NIVEAU_4);
    }

    @Test
    void analyze_discriminationEmploi_returnsNiveau2() {
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(
                reqSimple(CodePenalSocialBeTypeInfraction.DISCRIMINATION_EMPLOI));
        assertThat(r.verdict()).isEqualTo(CodePenalSocialBeVerdict.SANCTION_NIVEAU_2);
    }

    @Test
    void analyze_entraveInspectionSociale_returnsNiveau3() {
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(
                reqSimple(CodePenalSocialBeTypeInfraction.ENTRAVE_INSPECTION_SOCIALE));
        assertThat(r.verdict()).isEqualTo(CodePenalSocialBeVerdict.SANCTION_NIVEAU_3);
    }

    @Test
    void analyze_fauxSocial_returnsNiveau4() {
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(
                reqSimple(CodePenalSocialBeTypeInfraction.FAUX_SOCIAL));
        assertThat(r.verdict()).isEqualTo(CodePenalSocialBeVerdict.SANCTION_NIVEAU_4);
        assertThat(r.emprisonnementApplicable()).isTrue();
    }

    @Test
    void analyze_absenceRegistrePersonnel_returnsNiveau3() {
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(
                reqSimple(CodePenalSocialBeTypeInfraction.ABSENCE_REGISTRE_PERSONNEL));
        assertThat(r.verdict()).isEqualTo(CodePenalSocialBeVerdict.SANCTION_NIVEAU_3);
    }

    @Test
    void analyze_infractionInterim_returnsNiveau3() {
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(
                reqSimple(CodePenalSocialBeTypeInfraction.INFRACTION_INTERIM));
        assertThat(r.verdict()).isEqualTo(CodePenalSocialBeVerdict.SANCTION_NIVEAU_3);
    }

    @Test
    void analyze_infractionDetachementNonIntentionnel_returnsNiveau3() {
        CodePenalSocialBeRequest req = new CodePenalSocialBeRequest(
                CodePenalSocialBeTypeInfraction.INFRACTION_DETACHEMENT,
                null, DATE_FAITS, 0,
                false, false, false, false);
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(req);
        assertThat(r.verdict()).isEqualTo(CodePenalSocialBeVerdict.SANCTION_NIVEAU_3);
    }

    @Test
    void analyze_infractionDetachementIntentionnel_returnsNiveau4() {
        CodePenalSocialBeRequest req = new CodePenalSocialBeRequest(
                CodePenalSocialBeTypeInfraction.INFRACTION_DETACHEMENT,
                null, DATE_FAITS, 0,
                false, false, false, true);
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(req);
        assertThat(r.verdict()).isEqualTo(CodePenalSocialBeVerdict.SANCTION_NIVEAU_4);
        assertThat(r.emprisonnementApplicable()).isTrue();
    }

    // ── A_QUALIFIER : AUTRE_QUALIFICATION sans niveau ───────────────────────

    @Test
    void analyze_autreQualificationSansNiveau_returnsAqualifier() {
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(
                reqSimple(CodePenalSocialBeTypeInfraction.AUTRE_QUALIFICATION));
        assertThat(r.verdict()).isEqualTo(CodePenalSocialBeVerdict.A_QUALIFIER);
        assertThat(r.niveauSanction()).isZero();
        assertThat(r.amendeAdminMin()).isZero();
        assertThat(r.amendeAdminMax()).isZero();
        assertThat(r.emprisonnementApplicable()).isFalse();
        assertThat(r.raison()).isEqualTo("QUALIFICATION_OUVERTE");
        assertThat(r.synthese()).contains("AUTRE_QUALIFICATION");
    }

    @Test
    void analyze_autreQualificationAvecNiveau2_returnsNiveau2() {
        CodePenalSocialBeRequest req = new CodePenalSocialBeRequest(
                CodePenalSocialBeTypeInfraction.AUTRE_QUALIFICATION,
                2,
                DATE_FAITS, 0,
                false, false, false, false);
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(req);
        assertThat(r.verdict()).isEqualTo(CodePenalSocialBeVerdict.SANCTION_NIVEAU_2);
        assertThat(r.niveauSanction()).isEqualTo(2);
    }

    // ── niveauPropose prime sur défaut ──────────────────────────────────────

    @Test
    void analyze_niveauProposePrimeSurDefaut() {
        // TRAVAIL_NON_DECLARE = niveau 4 par défaut ; on force niveau 2.
        CodePenalSocialBeRequest req = new CodePenalSocialBeRequest(
                CodePenalSocialBeTypeInfraction.TRAVAIL_NON_DECLARE,
                2,
                DATE_FAITS, 0,
                false, false, false, false);
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(req);
        assertThat(r.verdict()).isEqualTo(CodePenalSocialBeVerdict.SANCTION_NIVEAU_2);
        assertThat(r.niveauSanction()).isEqualTo(2);
        assertThat(r.emprisonnementApplicable()).isFalse();
    }

    // ── Bornes amendes art. 101 ─────────────────────────────────────────────

    @Test
    void analyze_niveau1_amende10a100() {
        CodePenalSocialBeRequest req = new CodePenalSocialBeRequest(
                CodePenalSocialBeTypeInfraction.AUTRE_QUALIFICATION,
                1, DATE_FAITS, 0,
                false, false, false, false);
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(req);
        assertThat(r.amendeAdminMin()).isEqualTo(10);
        assertThat(r.amendeAdminMax()).isEqualTo(100);
        assertThat(r.amendePenaleMin()).isZero();
        assertThat(r.amendePenaleMax()).isZero();
        assertThat(r.emprisonnementApplicable()).isFalse();
    }

    @Test
    void analyze_niveau2_amende50a500() {
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(
                reqSimple(CodePenalSocialBeTypeInfraction.NON_PAIEMENT_REMUNERATION));
        assertThat(r.amendeAdminMin()).isEqualTo(50);
        assertThat(r.amendeAdminMax()).isEqualTo(500);
        assertThat(r.amendePenaleMin()).isEqualTo(50);
        assertThat(r.amendePenaleMax()).isEqualTo(500);
    }

    @Test
    void analyze_niveau3_amende100a1000() {
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(
                reqSimple(CodePenalSocialBeTypeInfraction.DEPASSEMENT_TEMPS_TRAVAIL));
        assertThat(r.amendeAdminMin()).isEqualTo(100);
        assertThat(r.amendeAdminMax()).isEqualTo(1000);
        assertThat(r.amendePenaleMin()).isEqualTo(100);
        assertThat(r.amendePenaleMax()).isEqualTo(1000);
    }

    @Test
    void analyze_niveau4_amende300_3000_admin_et_600_6000_penale() {
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(
                reqSimple(CodePenalSocialBeTypeInfraction.TRAVAIL_NON_DECLARE));
        assertThat(r.amendeAdminMin()).isEqualTo(300);
        assertThat(r.amendeAdminMax()).isEqualTo(3000);
        assertThat(r.amendePenaleMin()).isEqualTo(600);
        assertThat(r.amendePenaleMax()).isEqualTo(6000);
        assertThat(r.emprisonnementApplicable()).isTrue();
        assertThat(r.emprisonnementMinMois()).isEqualTo(6);
        assertThat(r.emprisonnementMaxMois()).isEqualTo(36);
    }

    // ── Majorations ─────────────────────────────────────────────────────────

    @Test
    void analyze_avecPersonneMorale_drapeauActif_etSyntheseLeMentionne() {
        CodePenalSocialBeRequest req = new CodePenalSocialBeRequest(
                CodePenalSocialBeTypeInfraction.NON_PAIEMENT_REMUNERATION,
                null, DATE_FAITS, 0,
                true, false, false, false);
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(req);
        assertThat(r.majorationPersonneMorale()).isTrue();
        assertThat(r.synthese()).contains("multipliée par 5")
                .contains("art. 105");
    }

    @Test
    void analyze_avecMultTravailleurs_drapeauActif_etSyntheseLeMentionne() {
        CodePenalSocialBeRequest req = new CodePenalSocialBeRequest(
                CodePenalSocialBeTypeInfraction.TRAVAIL_NON_DECLARE,
                null, DATE_FAITS, 12,
                false, false, false, false);
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(req);
        assertThat(r.multiplicationParTravailleur()).isTrue();
        assertThat(r.synthese())
                .contains("multipliée par le nombre de travailleurs")
                .contains("12")
                .contains("art. 103");
    }

    @Test
    void analyze_avecRecidive_drapeauActif_etSyntheseLeMentionne() {
        CodePenalSocialBeRequest req = new CodePenalSocialBeRequest(
                CodePenalSocialBeTypeInfraction.FAUX_SOCIAL,
                null, DATE_FAITS, 0,
                false, true, false, false);
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(req);
        assertThat(r.majorationRecidive()).isTrue();
        assertThat(r.synthese())
                .contains("récidive")
                .contains("doublement")
                .contains("art. 110");
    }

    @Test
    void analyze_niveau1AvecPersonneMorale_signaleControverse() {
        CodePenalSocialBeRequest req = new CodePenalSocialBeRequest(
                CodePenalSocialBeTypeInfraction.AUTRE_QUALIFICATION,
                1, DATE_FAITS, 0,
                true, false, false, false);
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(req);
        assertThat(r.verdict()).isEqualTo(CodePenalSocialBeVerdict.SANCTION_NIVEAU_1);
        assertThat(r.synthese())
                .contains("doctrinalement controversée");
    }

    @Test
    void analyze_majorationsCumulees() {
        CodePenalSocialBeRequest req = new CodePenalSocialBeRequest(
                CodePenalSocialBeTypeInfraction.TRAVAIL_NON_DECLARE,
                null, DATE_FAITS, 5,
                true, true, true, true);
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(req);
        assertThat(r.multiplicationParTravailleur()).isTrue();
        assertThat(r.majorationPersonneMorale()).isTrue();
        assertThat(r.majorationRecidive()).isTrue();
        assertThat(r.synthese())
                .contains("personne morale")
                .contains("travailleurs")
                .contains("récidive");
    }

    // ── Base juridique stable + avertissement avocat ────────────────────────

    @Test
    void analyze_baseJuridique_estStableEtComplete() {
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(
                reqSimple(CodePenalSocialBeTypeInfraction.TRAVAIL_NON_DECLARE));
        assertThat(r.baseJuridique())
                .contains("Loi du 06/06/2010")
                .contains("Code pénal social")
                .contains("art. 101 à 103")
                .contains("art. 105")
                .contains("art. 109")
                .contains("art. 110")
                .contains("art. 73")
                .contains("Loi du 05/03/1952")
                .contains("Una Via");
    }

    @Test
    void analyze_avertissementAvocat_couvreLesPrincipauxRisques() {
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(
                reqSimple(CodePenalSocialBeTypeInfraction.TRAVAIL_NON_DECLARE));
        assertThat(r.avertissement())
                .contains("avocat spécialisé")
                .contains("indexation")
                .contains("décimes additionnels")
                .contains("Una Via")
                .contains("récidive")
                .contains("préposés ou mandataires");
    }

    // ── Validation des inputs ───────────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() -> CodePenalSocialBeChecker.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête requise");
    }

    @Test
    void analyze_typeInfractionNull_throws() {
        CodePenalSocialBeRequest req = new CodePenalSocialBeRequest(
                null, null, DATE_FAITS, 0,
                false, false, false, false);
        assertThatThrownBy(() -> CodePenalSocialBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeInfraction");
    }

    @Test
    void analyze_niveauProposeHorsBornes_throws() {
        CodePenalSocialBeRequest req = new CodePenalSocialBeRequest(
                CodePenalSocialBeTypeInfraction.TRAVAIL_NON_DECLARE,
                5,
                DATE_FAITS, 0,
                false, false, false, false);
        assertThatThrownBy(() -> CodePenalSocialBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("niveauPropose");
    }

    @Test
    void analyze_niveauProposeZero_throws() {
        CodePenalSocialBeRequest req = new CodePenalSocialBeRequest(
                CodePenalSocialBeTypeInfraction.TRAVAIL_NON_DECLARE,
                0,
                DATE_FAITS, 0,
                false, false, false, false);
        assertThatThrownBy(() -> CodePenalSocialBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("niveauPropose");
    }

    @Test
    void analyze_nombreTravailleursConcernesNegatif_throws() {
        CodePenalSocialBeRequest req = new CodePenalSocialBeRequest(
                CodePenalSocialBeTypeInfraction.TRAVAIL_NON_DECLARE,
                null, DATE_FAITS, -1,
                false, false, false, false);
        assertThatThrownBy(() -> CodePenalSocialBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombreTravailleursConcernes");
    }

    @Test
    void analyze_personneMoraleNull_throws() {
        CodePenalSocialBeRequest req = new CodePenalSocialBeRequest(
                CodePenalSocialBeTypeInfraction.TRAVAIL_NON_DECLARE,
                null, DATE_FAITS, 0,
                null, false, false, false);
        assertThatThrownBy(() -> CodePenalSocialBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("personneMorale");
    }

    @Test
    void analyze_recidiveDansLAnNull_throws() {
        CodePenalSocialBeRequest req = new CodePenalSocialBeRequest(
                CodePenalSocialBeTypeInfraction.TRAVAIL_NON_DECLARE,
                null, DATE_FAITS, 0,
                false, null, false, false);
        assertThatThrownBy(() -> CodePenalSocialBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recidiveDansLAn");
    }

    @Test
    void analyze_prevenuPreposeOuMandataireNull_throws() {
        CodePenalSocialBeRequest req = new CodePenalSocialBeRequest(
                CodePenalSocialBeTypeInfraction.TRAVAIL_NON_DECLARE,
                null, DATE_FAITS, 0,
                false, false, null, false);
        assertThatThrownBy(() -> CodePenalSocialBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prevenuPreposeOuMandataire");
    }

    @Test
    void analyze_elementMoralIntentionnelNull_throws() {
        CodePenalSocialBeRequest req = new CodePenalSocialBeRequest(
                CodePenalSocialBeTypeInfraction.TRAVAIL_NON_DECLARE,
                null, DATE_FAITS, 0,
                false, false, false, null);
        assertThatThrownBy(() -> CodePenalSocialBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("elementMoralIntentionnel");
    }

    // ── Couverture exhaustive des codes raison ──────────────────────────────

    @Test
    void analyze_raisonNiveau1_estStable() {
        CodePenalSocialBeRequest req = new CodePenalSocialBeRequest(
                CodePenalSocialBeTypeInfraction.AUTRE_QUALIFICATION,
                1, DATE_FAITS, 0,
                false, false, false, false);
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(req);
        assertThat(r.raison()).isEqualTo("SANCTION_NIVEAU_1_ADMINISTRATIVE_SEULE");
    }

    @Test
    void analyze_raisonNiveau2_estStable() {
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(
                reqSimple(CodePenalSocialBeTypeInfraction.NON_PAIEMENT_REMUNERATION));
        assertThat(r.raison()).isEqualTo("SANCTION_NIVEAU_2_ADMIN_OU_PENALE");
    }

    @Test
    void analyze_raisonNiveau3_estStable() {
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(
                reqSimple(CodePenalSocialBeTypeInfraction.DEPASSEMENT_TEMPS_TRAVAIL));
        assertThat(r.raison()).isEqualTo("SANCTION_NIVEAU_3_ADMIN_OU_PENALE");
    }

    @Test
    void analyze_raisonNiveau4_estStable() {
        CodePenalSocialBeResult r = CodePenalSocialBeChecker.analyze(
                reqSimple(CodePenalSocialBeTypeInfraction.TRAVAIL_NON_DECLARE));
        assertThat(r.raison()).isEqualTo("SANCTION_NIVEAU_4_AMENDE_ET_OU_EMPRISONNEMENT");
    }
}
