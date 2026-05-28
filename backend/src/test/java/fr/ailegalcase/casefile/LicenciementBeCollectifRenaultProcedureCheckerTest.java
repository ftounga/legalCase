package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-07 : tests unitaires du {@link
 * LicenciementBeCollectifRenaultProcedureChecker}.
 *
 * <p>Couvre : seuil de déclenchement selon la taille de l'entreprise
 * (PME 20-99 / grande 100-299 / très grande ≥ 300 / petite &lt; 20),
 * vérification des 3 phases procédurales (information préalable,
 * consultation effective, notification autorité régionale), respect du
 * délai d'attente de 30 jours après notification, hiérarchie des
 * verdicts d'invalidité (seuil &gt; information &gt; consultation &gt;
 * notification &gt; délai), validation des inputs et base juridique
 * stable (Loi 13/02/1998 + CCT n° 24 + CCT n° 39).</p>
 */
class LicenciementBeCollectifRenaultProcedureCheckerTest {

    private static final LocalDate PROJET_2026 = LocalDate.of(2026, 1, 10);
    private static final LocalDate NOTIF_AUTORITE_2026 = LocalDate.of(2026, 2, 1);
    private static final LocalDate PREAVIS_APRES_DELAI =
            LocalDate.of(2026, 3, 5); // > NOTIF + 30 jours = 03/03

    /** Cas de référence : conforme, PME 50 ETP, 15 licenciements, toutes phases OK. */
    private static LicenciementBeCollectifRenaultRequest reqConforme() {
        return new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99,
                50,
                15,
                LicenciementBeCollectifRenaultPhase.DECISION_ET_NOTIFICATION,
                true, true, true, true, true,
                NOTIF_AUTORITE_2026,
                PREAVIS_APRES_DELAI);
    }

    // ── Cas conforme : 3 phases + délai 30 jours échu ──────────────────────

    @Test
    void analyze_phasesOkEtDelaiRespecte_returnsConforme() {
        LicenciementBeCollectifRenaultResult r =
                LicenciementBeCollectifRenaultProcedureChecker.analyze(reqConforme());

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeCollectifRenaultVerdict.CONFORME);
        assertThat(r.conforme()).isTrue();
        assertThat(r.seuilDeclenchementAtteint()).isTrue();
        assertThat(r.seuilLegalApplicable()).isEqualTo(10); // PME = 10
        assertThat(r.dateFinDelaiAttente())
                .isEqualTo(LocalDate.of(2026, 3, 3)); // 01/02 + 30 j
        assertThat(r.delaiAttenteRespecte()).isTrue();
        assertThat(r.raison()).isEqualTo("PROCEDURE_CONFORME");
        assertThat(r.synthese())
                .contains("Procédure Renault conforme")
                .contains("3 phases");
    }

    @Test
    void analyze_conforme_sansPreavisNotifie_returnsConforme() {
        LicenciementBeCollectifRenaultRequest req = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99,
                50, 15,
                LicenciementBeCollectifRenaultPhase.DECISION_ET_NOTIFICATION,
                true, true, true, true, true,
                NOTIF_AUTORITE_2026,
                null);

        LicenciementBeCollectifRenaultResult r =
                LicenciementBeCollectifRenaultProcedureChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeCollectifRenaultVerdict.CONFORME);
        assertThat(r.delaiAttenteRespecte()).isTrue();
    }

    // ── Seuil non atteint ──────────────────────────────────────────────────

    @Test
    void analyze_petiteMoins20_returnsNonApplicableSeuil() {
        LicenciementBeCollectifRenaultRequest req = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.PETITE_MOINS_20,
                15, 5,
                LicenciementBeCollectifRenaultPhase.AUCUNE_PROCEDURE_DEMARREE,
                false, false, false, false, false,
                null, null);

        LicenciementBeCollectifRenaultResult r =
                LicenciementBeCollectifRenaultProcedureChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeCollectifRenaultVerdict.NON_APPLICABLE_SEUIL);
        assertThat(r.conforme()).isFalse();
        assertThat(r.seuilDeclenchementAtteint()).isFalse();
        assertThat(r.raison()).isEqualTo("SEUIL_NON_ATTEINT");
        assertThat(r.synthese()).contains("CCT n° 9");
    }

    @Test
    void analyze_pme_9licenciements_returnsNonApplicableSeuil() {
        LicenciementBeCollectifRenaultRequest req = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99,
                50, 9,   // < seuil PME = 10
                LicenciementBeCollectifRenaultPhase.AUCUNE_PROCEDURE_DEMARREE,
                false, false, false, false, false,
                null, null);

        LicenciementBeCollectifRenaultResult r =
                LicenciementBeCollectifRenaultProcedureChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeCollectifRenaultVerdict.NON_APPLICABLE_SEUIL);
        assertThat(r.seuilLegalApplicable()).isEqualTo(10);
    }

    @Test
    void analyze_pme_exactement10licenciements_returnsConformeSiPhasesOk() {
        LicenciementBeCollectifRenaultRequest req = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99,
                50, 10,  // seuil exact PME = 10
                LicenciementBeCollectifRenaultPhase.DECISION_ET_NOTIFICATION,
                true, true, true, true, true,
                NOTIF_AUTORITE_2026, null);

        LicenciementBeCollectifRenaultResult r =
                LicenciementBeCollectifRenaultProcedureChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeCollectifRenaultVerdict.CONFORME);
        assertThat(r.seuilDeclenchementAtteint()).isTrue();
    }

    @Test
    void analyze_grande150_seuilCalcule15() {
        // 10 % de 150 = 15
        LicenciementBeCollectifRenaultRequest req = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.GRANDE_100_299,
                150, 14, // < 15
                LicenciementBeCollectifRenaultPhase.AUCUNE_PROCEDURE_DEMARREE,
                false, false, false, false, false,
                null, null);

        LicenciementBeCollectifRenaultResult r =
                LicenciementBeCollectifRenaultProcedureChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeCollectifRenaultVerdict.NON_APPLICABLE_SEUIL);
        assertThat(r.seuilLegalApplicable()).isEqualTo(15);
    }

    @Test
    void analyze_grande150_atSeuil15_estApplicable() {
        LicenciementBeCollectifRenaultRequest req = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.GRANDE_100_299,
                150, 15,
                LicenciementBeCollectifRenaultPhase.DECISION_ET_NOTIFICATION,
                true, true, true, true, true,
                NOTIF_AUTORITE_2026, null);

        LicenciementBeCollectifRenaultResult r =
                LicenciementBeCollectifRenaultProcedureChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeCollectifRenaultVerdict.CONFORME);
        assertThat(r.seuilLegalApplicable()).isEqualTo(15);
    }

    @Test
    void analyze_tresGrande500_seuil30() {
        LicenciementBeCollectifRenaultRequest req = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.TRES_GRANDE_300_PLUS,
                500, 29, // < 30
                LicenciementBeCollectifRenaultPhase.AUCUNE_PROCEDURE_DEMARREE,
                false, false, false, false, false,
                null, null);

        LicenciementBeCollectifRenaultResult r =
                LicenciementBeCollectifRenaultProcedureChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeCollectifRenaultVerdict.NON_APPLICABLE_SEUIL);
        assertThat(r.seuilLegalApplicable()).isEqualTo(30);
    }

    @Test
    void analyze_effectifMoyenInferieurA20_returnsNonApplicableSeuil() {
        // Même si tailleEntreprise PME_20_99, effectif réel < 20
        LicenciementBeCollectifRenaultRequest req = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99,
                19, 15,
                LicenciementBeCollectifRenaultPhase.AUCUNE_PROCEDURE_DEMARREE,
                false, false, false, false, false,
                null, null);

        LicenciementBeCollectifRenaultResult r =
                LicenciementBeCollectifRenaultProcedureChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeCollectifRenaultVerdict.NON_APPLICABLE_SEUIL);
    }

    // ── Phase 1 : information préalable incomplète ─────────────────────────

    @Test
    void analyze_informationEcriteAbsente_returnsNonConformePhase1() {
        LicenciementBeCollectifRenaultRequest req = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99,
                50, 15,
                LicenciementBeCollectifRenaultPhase.INFORMATION,
                false, true, false, false, false,
                null, null);

        LicenciementBeCollectifRenaultResult r =
                LicenciementBeCollectifRenaultProcedureChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeCollectifRenaultVerdict.NON_CONFORME_PHASE_INFORMATION_INCOMPLETE);
        assertThat(r.conforme()).isFalse();
        assertThat(r.raison()).isEqualTo("PHASE_INFORMATION_INCOMPLETE");
        assertThat(r.synthese())
                .contains("notification écrite préalable manquante")
                .contains("art. 67");
    }

    @Test
    void analyze_documentsLegauxAbsents_returnsNonConformePhase1() {
        LicenciementBeCollectifRenaultRequest req = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99,
                50, 15,
                LicenciementBeCollectifRenaultPhase.INFORMATION,
                true, false, false, false, false,
                null, null);

        LicenciementBeCollectifRenaultResult r =
                LicenciementBeCollectifRenaultProcedureChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeCollectifRenaultVerdict.NON_CONFORME_PHASE_INFORMATION_INCOMPLETE);
        assertThat(r.synthese())
                .contains("documents légaux non communiqués")
                .contains("critères de sélection");
    }

    // ── Phase 2 : consultation insuffisante ────────────────────────────────

    @Test
    void analyze_aucuneReunion_returnsNonConformeConsultation() {
        LicenciementBeCollectifRenaultRequest req = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99,
                50, 15,
                LicenciementBeCollectifRenaultPhase.CONSULTATION,
                true, true, false, false, false,
                null, null);

        LicenciementBeCollectifRenaultResult r =
                LicenciementBeCollectifRenaultProcedureChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeCollectifRenaultVerdict.NON_CONFORME_CONSULTATION_INSUFFISANTE);
        assertThat(r.raison()).isEqualTo("CONSULTATION_INSUFFISANTE");
        assertThat(r.synthese())
                .contains("aucune réunion de consultation tenue")
                .contains("CCT n° 24");
    }

    @Test
    void analyze_aucuneReponseMotivee_returnsNonConformeConsultation() {
        LicenciementBeCollectifRenaultRequest req = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99,
                50, 15,
                LicenciementBeCollectifRenaultPhase.CONSULTATION,
                true, true, true, false, false,
                null, null);

        LicenciementBeCollectifRenaultResult r =
                LicenciementBeCollectifRenaultProcedureChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeCollectifRenaultVerdict.NON_CONFORME_CONSULTATION_INSUFFISANTE);
        assertThat(r.synthese())
                .contains("aucune réponse motivée");
    }

    // ── Phase 3 : notification autorité régionale manquante ────────────────

    @Test
    void analyze_notificationAutoriteAbsente_returnsNonConformeNotification() {
        LicenciementBeCollectifRenaultRequest req = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99,
                50, 15,
                LicenciementBeCollectifRenaultPhase.CONSULTATION,
                true, true, true, true, false,
                null, null);

        LicenciementBeCollectifRenaultResult r =
                LicenciementBeCollectifRenaultProcedureChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeCollectifRenaultVerdict.NON_CONFORME_NOTIFICATION_AUTORITE_MANQUANTE);
        assertThat(r.raison()).isEqualTo("NOTIFICATION_AUTORITE_MANQUANTE");
        assertThat(r.synthese())
                .contains("Forem / Actiris / VDAB")
                .contains("préavis notifié est nul");
        // Pas de date de fin de délai d'attente si pas de notification.
        assertThat(r.dateFinDelaiAttente()).isNull();
    }

    // ── Délai d'attente 30 jours non respecté ──────────────────────────────

    @Test
    void analyze_preavisAvantFinDelaiAttente_returnsNonConformeDelai() {
        // Notification 01/02, fin délai 03/03 ; préavis 15/02 = violation.
        LicenciementBeCollectifRenaultRequest req = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99,
                50, 15,
                LicenciementBeCollectifRenaultPhase.DECISION_ET_NOTIFICATION,
                true, true, true, true, true,
                NOTIF_AUTORITE_2026,
                LocalDate.of(2026, 2, 15));

        LicenciementBeCollectifRenaultResult r =
                LicenciementBeCollectifRenaultProcedureChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeCollectifRenaultVerdict.NON_CONFORME_DELAI_ATTENTE_NON_RESPECTE);
        assertThat(r.delaiAttenteRespecte()).isFalse();
        assertThat(r.raison()).isEqualTo("DELAI_ATTENTE_NON_RESPECTE");
        assertThat(r.synthese())
                .contains("2026-02-15")
                .contains("2026-03-03")
                .contains("art. 67");
        assertThat(r.dateFinDelaiAttente())
                .isEqualTo(LocalDate.of(2026, 3, 3));
    }

    @Test
    void analyze_preavisExactementALaFinDuDelai_returnsConforme() {
        // Notification 01/02, fin délai 03/03 ; préavis 03/03 = OK (pas before).
        LicenciementBeCollectifRenaultRequest req = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99,
                50, 15,
                LicenciementBeCollectifRenaultPhase.DECISION_ET_NOTIFICATION,
                true, true, true, true, true,
                NOTIF_AUTORITE_2026,
                LocalDate.of(2026, 3, 3));

        LicenciementBeCollectifRenaultResult r =
                LicenciementBeCollectifRenaultProcedureChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeCollectifRenaultVerdict.CONFORME);
    }

    // ── Hiérarchie : seuil > information > consultation > notif > délai ────

    @Test
    void analyze_seuilFails_courtcircuiteTout() {
        LicenciementBeCollectifRenaultRequest req = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99,
                50, 5,    // sous seuil
                LicenciementBeCollectifRenaultPhase.DECISION_ET_NOTIFICATION,
                false, false, false, false, false, // toutes phases KO
                null, null);

        LicenciementBeCollectifRenaultResult r =
                LicenciementBeCollectifRenaultProcedureChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeCollectifRenaultVerdict.NON_APPLICABLE_SEUIL);
    }

    @Test
    void analyze_infoFailsPlusConsultFails_infoWins() {
        LicenciementBeCollectifRenaultRequest req = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99,
                50, 15,
                LicenciementBeCollectifRenaultPhase.CONSULTATION,
                false, false, false, false, false,
                null, null);

        LicenciementBeCollectifRenaultResult r =
                LicenciementBeCollectifRenaultProcedureChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeCollectifRenaultVerdict.NON_CONFORME_PHASE_INFORMATION_INCOMPLETE);
    }

    @Test
    void analyze_consultFailsPlusNotifFails_consultWins() {
        LicenciementBeCollectifRenaultRequest req = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99,
                50, 15,
                LicenciementBeCollectifRenaultPhase.CONSULTATION,
                true, true, false, false, false,
                null, null);

        LicenciementBeCollectifRenaultResult r =
                LicenciementBeCollectifRenaultProcedureChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeCollectifRenaultVerdict.NON_CONFORME_CONSULTATION_INSUFFISANTE);
    }

    // ── Snapshot inputs ────────────────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        LicenciementBeCollectifRenaultResult r =
                LicenciementBeCollectifRenaultProcedureChecker.analyze(reqConforme());

        assertThat(r.dateProjet()).isEqualTo(PROJET_2026);
        assertThat(r.tailleEntreprise())
                .isEqualTo(LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99);
        assertThat(r.effectifMoyen()).isEqualTo(50);
        assertThat(r.nombreLicenciementsEnvisages()).isEqualTo(15);
        assertThat(r.phaseAtteinte())
                .isEqualTo(LicenciementBeCollectifRenaultPhase.DECISION_ET_NOTIFICATION);
        assertThat(r.informationEcriteCePrealable()).isTrue();
        assertThat(r.documentsLegauxCommuniques()).isTrue();
        assertThat(r.consultationEffectiveTenue()).isTrue();
        assertThat(r.reponsesMotiveesEmployeur()).isTrue();
        assertThat(r.notificationAutoriteRegionaleFaite()).isTrue();
        assertThat(r.dateNotificationAutorite()).isEqualTo(NOTIF_AUTORITE_2026);
        assertThat(r.datePremierPreavisNotifie()).isEqualTo(PREAVIS_APRES_DELAI);
    }

    // ── Base juridique + avertissement ─────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceLoi13021998_CCT24_CCT39_Directive() {
        LicenciementBeCollectifRenaultResult r =
                LicenciementBeCollectifRenaultProcedureChecker.analyze(reqConforme());

        assertThat(r.baseJuridique())
                .contains("13/02/1998")
                .contains("CCT n° 24")
                .contains("CCT n° 39")
                .contains("98/59/CE")
                .contains("art. 67");
    }

    @Test
    void analyze_avertissementSystematiquePresent() {
        LicenciementBeCollectifRenaultResult r =
                LicenciementBeCollectifRenaultProcedureChecker.analyze(reqConforme());

        assertThat(r.avertissement())
                .isNotNull()
                .contains("checklist procédurale")
                .contains("PSE français");
    }

    // ── Helper de calcul du seuil ──────────────────────────────────────────

    @Test
    void seuilLegalPour_petite_returns0() {
        assertThat(LicenciementBeCollectifRenaultProcedureChecker
                .seuilLegalPour(LicenciementBeCollectifRenaultTailleEntreprise.PETITE_MOINS_20, 15))
                .isEqualTo(0);
    }

    @Test
    void seuilLegalPour_pme_returns10() {
        assertThat(LicenciementBeCollectifRenaultProcedureChecker
                .seuilLegalPour(LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99, 75))
                .isEqualTo(10);
    }

    @Test
    void seuilLegalPour_grande250_returns25() {
        assertThat(LicenciementBeCollectifRenaultProcedureChecker
                .seuilLegalPour(LicenciementBeCollectifRenaultTailleEntreprise.GRANDE_100_299, 250))
                .isEqualTo(25);
    }

    @Test
    void seuilLegalPour_tresGrande_returns30() {
        assertThat(LicenciementBeCollectifRenaultProcedureChecker
                .seuilLegalPour(LicenciementBeCollectifRenaultTailleEntreprise.TRES_GRANDE_300_PLUS, 500))
                .isEqualTo(30);
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() ->
                LicenciementBeCollectifRenaultProcedureChecker.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_dateProjetNull_throws() {
        LicenciementBeCollectifRenaultRequest bad = new LicenciementBeCollectifRenaultRequest(
                null,
                LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99,
                50, 15,
                LicenciementBeCollectifRenaultPhase.DECISION_ET_NOTIFICATION,
                true, true, true, true, true,
                NOTIF_AUTORITE_2026, null);

        assertThatThrownBy(() ->
                LicenciementBeCollectifRenaultProcedureChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateProjet");
    }

    @Test
    void analyze_tailleEntrepriseNull_throws() {
        LicenciementBeCollectifRenaultRequest bad = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026, null, 50, 15,
                LicenciementBeCollectifRenaultPhase.DECISION_ET_NOTIFICATION,
                true, true, true, true, true,
                NOTIF_AUTORITE_2026, null);

        assertThatThrownBy(() ->
                LicenciementBeCollectifRenaultProcedureChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tailleEntreprise");
    }

    @Test
    void analyze_effectifMoyenNegatif_throws() {
        LicenciementBeCollectifRenaultRequest bad = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99,
                -1, 15,
                LicenciementBeCollectifRenaultPhase.DECISION_ET_NOTIFICATION,
                true, true, true, true, true,
                NOTIF_AUTORITE_2026, null);

        assertThatThrownBy(() ->
                LicenciementBeCollectifRenaultProcedureChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectifMoyen");
    }

    @Test
    void analyze_nombreLicenciementsNegatif_throws() {
        LicenciementBeCollectifRenaultRequest bad = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99,
                50, -1,
                LicenciementBeCollectifRenaultPhase.DECISION_ET_NOTIFICATION,
                true, true, true, true, true,
                NOTIF_AUTORITE_2026, null);

        assertThatThrownBy(() ->
                LicenciementBeCollectifRenaultProcedureChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombreLicenciementsEnvisages");
    }

    @Test
    void analyze_phaseAtteinteNull_throws() {
        LicenciementBeCollectifRenaultRequest bad = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99,
                50, 15, null,
                true, true, true, true, true,
                NOTIF_AUTORITE_2026, null);

        assertThatThrownBy(() ->
                LicenciementBeCollectifRenaultProcedureChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phaseAtteinte");
    }

    @Test
    void analyze_notificationFaiteSansDate_throws() {
        LicenciementBeCollectifRenaultRequest bad = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99,
                50, 15,
                LicenciementBeCollectifRenaultPhase.DECISION_ET_NOTIFICATION,
                true, true, true, true, true,
                null,  // notification = true mais date absente
                null);

        assertThatThrownBy(() ->
                LicenciementBeCollectifRenaultProcedureChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNotificationAutorite");
    }

    @Test
    void analyze_notificationAvantProjet_throws() {
        LicenciementBeCollectifRenaultRequest bad = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99,
                50, 15,
                LicenciementBeCollectifRenaultPhase.DECISION_ET_NOTIFICATION,
                true, true, true, true, true,
                LocalDate.of(2025, 12, 1),
                null);

        assertThatThrownBy(() ->
                LicenciementBeCollectifRenaultProcedureChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNotificationAutorite");
    }

    @Test
    void analyze_preavisAvantNotification_throws() {
        LicenciementBeCollectifRenaultRequest bad = new LicenciementBeCollectifRenaultRequest(
                PROJET_2026,
                LicenciementBeCollectifRenaultTailleEntreprise.PME_20_99,
                50, 15,
                LicenciementBeCollectifRenaultPhase.DECISION_ET_NOTIFICATION,
                true, true, true, true, true,
                NOTIF_AUTORITE_2026,
                LocalDate.of(2026, 1, 25)); // < notification

        assertThatThrownBy(() ->
                LicenciementBeCollectifRenaultProcedureChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("datePremierPreavisNotifie");
    }
}
