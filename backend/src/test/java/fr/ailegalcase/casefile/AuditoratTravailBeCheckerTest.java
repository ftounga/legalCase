package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-25 : tests unitaires du {@link AuditoratTravailBeChecker}.
 *
 * <p>Couvre : verdict par nature de fait (infraction pénale sociale,
 * accident grave, travail non déclaré, discrimination pénale,
 * harcèlement pénal, entrave inspection, litige civil pur, autre),
 * prescription bloquante, basculement inspection préalable,
 * constitution de partie civile recommandée selon recours pénal,
 * avis obligatoire civil pour matières sécurité sociale, urgence
 * signalée à l'auditeur, base juridique stable, avertissement avocat
 * BE, validation inputs.</p>
 */
class AuditoratTravailBeCheckerTest {

    private static final LocalDate DATE_FAITS = LocalDate.of(2024, 6, 1);

    /** Body simple — infraction pénale sociale, dénonciation simple, sans drapeau. */
    private static AuditoratTravailBeRequest req(
            AuditoratTravailBeNatureFait nature) {
        return new AuditoratTravailBeRequest(
                nature,
                AuditoratTravailBeModeSaisine.DENONCIATION_SIMPLE,
                DATE_FAITS,
                false, false, false, false, false, false);
    }

    // ── Verdict par nature de fait ──────────────────────────────────────────

    @Test
    void analyze_infractionPenaleSociale_returnsSaisineRecommandee() {
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(
                req(AuditoratTravailBeNatureFait.INFRACTION_PENALE_SOCIALE));
        assertThat(r.verdict())
                .isEqualTo(AuditoratTravailBeVerdict.SAISINE_AUDITORAT_RECOMMANDEE);
        assertThat(r.unaViaApplicable()).isTrue();
        assertThat(r.modeSaisineRecommande())
                .isEqualTo(AuditoratTravailBeModeSaisine.DENONCIATION_SIMPLE);
    }

    @Test
    void analyze_accidentTravailGrave_returnsSaisineRecommandee() {
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(
                req(AuditoratTravailBeNatureFait.ACCIDENT_TRAVAIL_GRAVE));
        assertThat(r.verdict())
                .isEqualTo(AuditoratTravailBeVerdict.SAISINE_AUDITORAT_RECOMMANDEE);
        assertThat(r.synthese()).contains("accident grave");
    }

    @Test
    void analyze_discriminationPenale_returnsSaisineRecommandee() {
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(
                req(AuditoratTravailBeNatureFait.DISCRIMINATION_PENALE));
        assertThat(r.verdict())
                .isEqualTo(AuditoratTravailBeVerdict.SAISINE_AUDITORAT_RECOMMANDEE);
        assertThat(r.synthese()).contains("Loi du 10/05/2007");
    }

    @Test
    void analyze_harcelementPenal_returnsSaisineRecommandee() {
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(
                req(AuditoratTravailBeNatureFait.HARCELEMENT_PENAL));
        assertThat(r.verdict())
                .isEqualTo(AuditoratTravailBeVerdict.SAISINE_AUDITORAT_RECOMMANDEE);
        assertThat(r.synthese()).contains("442bis");
    }

    @Test
    void analyze_entraveInspection_returnsSaisineRecommandee() {
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(
                req(AuditoratTravailBeNatureFait.ENTRAVE_INSPECTION));
        assertThat(r.verdict())
                .isEqualTo(AuditoratTravailBeVerdict.SAISINE_AUDITORAT_RECOMMANDEE);
        assertThat(r.synthese()).contains("art. 209");
    }

    @Test
    void analyze_litigeCivilPur_returnsSaisineNonPertinente() {
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(
                req(AuditoratTravailBeNatureFait.LITIGE_CIVIL_PUR));
        assertThat(r.verdict())
                .isEqualTo(AuditoratTravailBeVerdict.SAISINE_NON_PERTINENTE);
        assertThat(r.modeSaisineRecommande())
                .isEqualTo(AuditoratTravailBeModeSaisine.VOIE_CIVILE_PARALLELE);
        assertThat(r.voieCivileConcurrente()).isTrue();
        assertThat(r.raison()).isEqualTo("COMPETENCE_TRIBUNAL_TRAVAIL_ART_578");
        assertThat(r.synthese()).contains("578 C. jud.");
    }

    @Test
    void analyze_autre_returnsAqualifier() {
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(
                req(AuditoratTravailBeNatureFait.AUTRE));
        assertThat(r.verdict())
                .isEqualTo(AuditoratTravailBeVerdict.A_QUALIFIER);
        assertThat(r.modeSaisineRecommande())
                .isEqualTo(AuditoratTravailBeModeSaisine.INDETERMINE);
        assertThat(r.raison()).isEqualTo("QUALIFICATION_OUVERTE");
        assertThat(r.unaViaApplicable()).isFalse();
    }

    // ── Travail non déclaré — basculement inspection préalable ──────────────

    @Test
    void analyze_travailNonDeclareSansInspection_returnsInspectionPrealable() {
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(
                req(AuditoratTravailBeNatureFait.TRAVAIL_NON_DECLARE_SUSPECTE));
        assertThat(r.verdict())
                .isEqualTo(AuditoratTravailBeVerdict.DENONCIATION_INSPECTION_PREALABLE);
        assertThat(r.modeSaisineRecommande())
                .isEqualTo(AuditoratTravailBeModeSaisine.SIGNALEMENT_INSPECTION_SOCIALE);
        assertThat(r.inspectionPrealableRequise()).isTrue();
        assertThat(r.raison())
                .isEqualTo("INSPECTION_PREALABLE_TRAVAIL_NON_DECLARE");
    }

    @Test
    void analyze_travailNonDeclareAvecInspectionDejaSaisie_returnsSaisineRecommandee() {
        AuditoratTravailBeRequest req = new AuditoratTravailBeRequest(
                AuditoratTravailBeNatureFait.TRAVAIL_NON_DECLARE_SUSPECTE,
                AuditoratTravailBeModeSaisine.DENONCIATION_SIMPLE,
                DATE_FAITS,
                false, true, false, false, false, false);
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(AuditoratTravailBeVerdict.SAISINE_AUDITORAT_RECOMMANDEE);
        assertThat(r.inspectionPrealableRequise()).isFalse();
        assertThat(r.avisAuditeurObligatoireCivil()).isFalse();
    }

    // ── Prescription bloquante ──────────────────────────────────────────────

    @Test
    void analyze_faitsPrescrits_returnsSaisineNonPertinente() {
        AuditoratTravailBeRequest req = new AuditoratTravailBeRequest(
                AuditoratTravailBeNatureFait.INFRACTION_PENALE_SOCIALE,
                AuditoratTravailBeModeSaisine.DENONCIATION_SIMPLE,
                DATE_FAITS,
                true, false, false, false, false, false);
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(AuditoratTravailBeVerdict.SAISINE_NON_PERTINENTE);
        assertThat(r.prescriptionBloquante()).isTrue();
        assertThat(r.modeSaisineRecommande())
                .isEqualTo(AuditoratTravailBeModeSaisine.VOIE_CIVILE_PARALLELE);
        assertThat(r.raison()).isEqualTo("PRESCRIPTION_PENALE_ACQUISE");
        assertThat(r.synthese()).contains("art. 81");
    }

    @Test
    void analyze_prescriptionPrimeSurLitigeCivil() {
        // Prescription bloquante doit primer même si nature LITIGE_CIVIL_PUR.
        AuditoratTravailBeRequest req = new AuditoratTravailBeRequest(
                AuditoratTravailBeNatureFait.LITIGE_CIVIL_PUR,
                AuditoratTravailBeModeSaisine.DENONCIATION_SIMPLE,
                DATE_FAITS,
                true, false, false, false, false, false);
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(AuditoratTravailBeVerdict.SAISINE_NON_PERTINENTE);
        assertThat(r.prescriptionBloquante()).isTrue();
    }

    // ── Constitution de partie civile ───────────────────────────────────────

    @Test
    void analyze_recoursPenalEnvisage_recommandePlainteDirecte_etPartieCivile() {
        AuditoratTravailBeRequest req = new AuditoratTravailBeRequest(
                AuditoratTravailBeNatureFait.HARCELEMENT_PENAL,
                AuditoratTravailBeModeSaisine.DENONCIATION_SIMPLE,
                DATE_FAITS,
                false, false, false, false,
                true, // recoursPenalEnvisage
                false);
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(AuditoratTravailBeVerdict.SAISINE_AUDITORAT_RECOMMANDEE);
        assertThat(r.modeSaisineRecommande())
                .isEqualTo(AuditoratTravailBeModeSaisine.PLAINTE_DIRECTE);
        assertThat(r.constitutionPartieCivileRecommandee()).isTrue();
        assertThat(r.synthese()).contains("partie civile");
    }

    @Test
    void analyze_sansRecoursPenal_recommandeDenonciationSimple_etPasDePartieCivile() {
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(
                req(AuditoratTravailBeNatureFait.INFRACTION_PENALE_SOCIALE));
        assertThat(r.modeSaisineRecommande())
                .isEqualTo(AuditoratTravailBeModeSaisine.DENONCIATION_SIMPLE);
        assertThat(r.constitutionPartieCivileRecommandee()).isFalse();
        assertThat(r.synthese()).contains("dénonciation simple");
    }

    // ── Avis obligatoire civil ──────────────────────────────────────────────

    @Test
    void analyze_infractionPenaleSocialeAvecPlainteCivile_declencheAvisObligatoire() {
        AuditoratTravailBeRequest req = new AuditoratTravailBeRequest(
                AuditoratTravailBeNatureFait.INFRACTION_PENALE_SOCIALE,
                AuditoratTravailBeModeSaisine.DENONCIATION_SIMPLE,
                DATE_FAITS,
                false, false,
                true, // plainteCivileDeposee
                false, false, false);
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(req);
        assertThat(r.avisAuditeurObligatoireCivil()).isTrue();
        assertThat(r.voieCivileConcurrente()).isTrue();
        assertThat(r.synthese()).contains("avis obligatoire");
    }

    @Test
    void analyze_harcelementAvecPlainteCivile_avisFacultatif() {
        AuditoratTravailBeRequest req = new AuditoratTravailBeRequest(
                AuditoratTravailBeNatureFait.HARCELEMENT_PENAL,
                AuditoratTravailBeModeSaisine.DENONCIATION_SIMPLE,
                DATE_FAITS,
                false, false, true, false, false, false);
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(req);
        assertThat(r.avisAuditeurObligatoireCivil()).isFalse();
        assertThat(r.voieCivileConcurrente()).isTrue();
    }

    // ── Urgence ─────────────────────────────────────────────────────────────

    @Test
    void analyze_urgenceSecurite_drapeauActif_etSyntheseLeMentionne() {
        AuditoratTravailBeRequest req = new AuditoratTravailBeRequest(
                AuditoratTravailBeNatureFait.ACCIDENT_TRAVAIL_GRAVE,
                AuditoratTravailBeModeSaisine.PLAINTE_DIRECTE,
                DATE_FAITS,
                false, false, false,
                true, // urgence
                false, false);
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(req);
        assertThat(r.urgenceSignalee()).isTrue();
        assertThat(r.synthese())
                .contains("URGENT")
                .contains("art. 28sexies");
    }

    @Test
    void analyze_voieCivileEnCours_syntheseMentionneSursisAStatuer() {
        AuditoratTravailBeRequest req = new AuditoratTravailBeRequest(
                AuditoratTravailBeNatureFait.INFRACTION_PENALE_SOCIALE,
                AuditoratTravailBeModeSaisine.DENONCIATION_SIMPLE,
                DATE_FAITS,
                false, false, true, false, false, false);
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(req);
        assertThat(r.synthese())
                .contains("criminel tient le civil")
                .contains("art. 4");
    }

    // ── Base juridique stable + avertissement avocat ────────────────────────

    @Test
    void analyze_baseJuridique_estStableEtComplete() {
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(
                req(AuditoratTravailBeNatureFait.INFRACTION_PENALE_SOCIALE));
        assertThat(r.baseJuridique())
                .contains("art. 138bis")
                .contains("Code judiciaire")
                .contains("art. 24")
                .contains("Loi du 03/08/1992")
                .contains("Loi du 06/06/2010")
                .contains("art. 76 C. pén. soc.")
                .contains("art. 81 C. pén. soc.")
                .contains("Una Via")
                .contains("art. 578");
    }

    @Test
    void analyze_avertissementAvocat_couvreLesPrincipauxRisques() {
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(
                req(AuditoratTravailBeNatureFait.INFRACTION_PENALE_SOCIALE));
        assertThat(r.avertissement())
                .contains("avocat spécialisé")
                .contains("prescription")
                .contains("Una Via")
                .contains("inspection sociale")
                .contains("partie civile")
                .contains("transaction administrative");
    }

    // ── Validation des inputs ───────────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() -> AuditoratTravailBeChecker.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête requise");
    }

    @Test
    void analyze_natureFaitNull_throws() {
        AuditoratTravailBeRequest req = new AuditoratTravailBeRequest(
                null,
                AuditoratTravailBeModeSaisine.DENONCIATION_SIMPLE,
                DATE_FAITS,
                false, false, false, false, false, false);
        assertThatThrownBy(() -> AuditoratTravailBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("natureFait");
    }

    @Test
    void analyze_modeSaisineEnvisageNull_throws() {
        AuditoratTravailBeRequest req = new AuditoratTravailBeRequest(
                AuditoratTravailBeNatureFait.INFRACTION_PENALE_SOCIALE,
                null,
                DATE_FAITS,
                false, false, false, false, false, false);
        assertThatThrownBy(() -> AuditoratTravailBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("modeSaisineEnvisage");
    }

    @Test
    void analyze_faitsPrescritsNull_throws() {
        AuditoratTravailBeRequest req = new AuditoratTravailBeRequest(
                AuditoratTravailBeNatureFait.INFRACTION_PENALE_SOCIALE,
                AuditoratTravailBeModeSaisine.DENONCIATION_SIMPLE,
                DATE_FAITS,
                null, false, false, false, false, false);
        assertThatThrownBy(() -> AuditoratTravailBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("faitsPrescrits");
    }

    @Test
    void analyze_inspectionDejaSaisieNull_throws() {
        AuditoratTravailBeRequest req = new AuditoratTravailBeRequest(
                AuditoratTravailBeNatureFait.INFRACTION_PENALE_SOCIALE,
                AuditoratTravailBeModeSaisine.DENONCIATION_SIMPLE,
                DATE_FAITS,
                false, null, false, false, false, false);
        assertThatThrownBy(() -> AuditoratTravailBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inspectionDejaSaisie");
    }

    @Test
    void analyze_plainteCivileDeposeeNull_throws() {
        AuditoratTravailBeRequest req = new AuditoratTravailBeRequest(
                AuditoratTravailBeNatureFait.INFRACTION_PENALE_SOCIALE,
                AuditoratTravailBeModeSaisine.DENONCIATION_SIMPLE,
                DATE_FAITS,
                false, false, null, false, false, false);
        assertThatThrownBy(() -> AuditoratTravailBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("plainteCivileDeposee");
    }

    @Test
    void analyze_urgenceSecuritePersonnesNull_throws() {
        AuditoratTravailBeRequest req = new AuditoratTravailBeRequest(
                AuditoratTravailBeNatureFait.INFRACTION_PENALE_SOCIALE,
                AuditoratTravailBeModeSaisine.DENONCIATION_SIMPLE,
                DATE_FAITS,
                false, false, false, null, false, false);
        assertThatThrownBy(() -> AuditoratTravailBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("urgenceSecuritePersonnes");
    }

    @Test
    void analyze_recoursPenalEnvisageNull_throws() {
        AuditoratTravailBeRequest req = new AuditoratTravailBeRequest(
                AuditoratTravailBeNatureFait.INFRACTION_PENALE_SOCIALE,
                AuditoratTravailBeModeSaisine.DENONCIATION_SIMPLE,
                DATE_FAITS,
                false, false, false, false, null, false);
        assertThatThrownBy(() -> AuditoratTravailBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recoursPenalEnvisage");
    }

    @Test
    void analyze_employeurPersonneMoraleNull_throws() {
        AuditoratTravailBeRequest req = new AuditoratTravailBeRequest(
                AuditoratTravailBeNatureFait.INFRACTION_PENALE_SOCIALE,
                AuditoratTravailBeModeSaisine.DENONCIATION_SIMPLE,
                DATE_FAITS,
                false, false, false, false, false, null);
        assertThatThrownBy(() -> AuditoratTravailBeChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("employeurPersonneMorale");
    }

    // ── Couverture des codes raison ─────────────────────────────────────────

    @Test
    void analyze_raisonInfractionPenaleSociale_estStable() {
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(
                req(AuditoratTravailBeNatureFait.INFRACTION_PENALE_SOCIALE));
        assertThat(r.raison()).isEqualTo("SAISINE_INFRACTION_PENALE_SOCIALE");
    }

    @Test
    void analyze_raisonAccidentGrave_estStable() {
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(
                req(AuditoratTravailBeNatureFait.ACCIDENT_TRAVAIL_GRAVE));
        assertThat(r.raison()).isEqualTo("SAISINE_ACCIDENT_GRAVE_AT");
    }

    @Test
    void analyze_raisonDiscriminationPenale_estStable() {
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(
                req(AuditoratTravailBeNatureFait.DISCRIMINATION_PENALE));
        assertThat(r.raison()).isEqualTo("SAISINE_DISCRIMINATION_PENALE");
    }

    @Test
    void analyze_raisonHarcelementPenal_estStable() {
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(
                req(AuditoratTravailBeNatureFait.HARCELEMENT_PENAL));
        assertThat(r.raison()).isEqualTo("SAISINE_HARCELEMENT_PENAL");
    }

    @Test
    void analyze_raisonEntraveInspection_estStable() {
        AuditoratTravailBeResult r = AuditoratTravailBeChecker.analyze(
                req(AuditoratTravailBeNatureFait.ENTRAVE_INSPECTION));
        assertThat(r.raison()).isEqualTo("SAISINE_ENTRAVE_INSPECTION");
    }

    // ── Détermination du mode recommandé ────────────────────────────────────

    @Test
    void determineModeRecommande_recoursPenal_returnsPlainteDirecte() {
        assertThat(AuditoratTravailBeChecker
                .determineModeRecommande(true, false))
                .isEqualTo(AuditoratTravailBeModeSaisine.PLAINTE_DIRECTE);
    }

    @Test
    void determineModeRecommande_sansRecoursPenal_returnsDenonciationSimple() {
        assertThat(AuditoratTravailBeChecker
                .determineModeRecommande(false, false))
                .isEqualTo(AuditoratTravailBeModeSaisine.DENONCIATION_SIMPLE);
    }

    @Test
    void determineModeRecommande_inspectionDejaSaisie_returnsDenonciationSimple() {
        assertThat(AuditoratTravailBeChecker
                .determineModeRecommande(false, true))
                .isEqualTo(AuditoratTravailBeModeSaisine.DENONCIATION_SIMPLE);
    }

    @Test
    void estMatiereAvisObligatoire_travailNonDeclare_returnsTrue() {
        assertThat(AuditoratTravailBeChecker
                .estMatiereAvisObligatoire(
                        AuditoratTravailBeNatureFait.TRAVAIL_NON_DECLARE_SUSPECTE))
                .isTrue();
    }

    @Test
    void estMatiereAvisObligatoire_infractionPenaleSociale_returnsTrue() {
        assertThat(AuditoratTravailBeChecker
                .estMatiereAvisObligatoire(
                        AuditoratTravailBeNatureFait.INFRACTION_PENALE_SOCIALE))
                .isTrue();
    }

    @Test
    void estMatiereAvisObligatoire_harcelement_returnsFalse() {
        assertThat(AuditoratTravailBeChecker
                .estMatiereAvisObligatoire(
                        AuditoratTravailBeNatureFait.HARCELEMENT_PENAL))
                .isFalse();
    }

    @Test
    void estMatiereAvisObligatoire_discriminationPenale_returnsFalse() {
        assertThat(AuditoratTravailBeChecker
                .estMatiereAvisObligatoire(
                        AuditoratTravailBeNatureFait.DISCRIMINATION_PENALE))
                .isFalse();
    }
}
