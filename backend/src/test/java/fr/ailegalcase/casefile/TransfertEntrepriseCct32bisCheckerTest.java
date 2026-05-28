package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-08 : tests unitaires du {@link
 * TransfertEntrepriseCct32bisChecker}.
 *
 * <p>Couvre : qualification du type d'opération (5 qualifiants /
 * 4 non), préservation de l'identité économique (3 états),
 * hiérarchie des verdicts (type &gt; identité &gt; procédure), respect
 * de l'information-consultation préalable (cumul CE + représentants),
 * respect du maintien des droits (4 dimensions), calcul de la date de
 * fin de responsabilité solidaire (1 an), validation des inputs et base
 * juridique stable (CCT 32bis + Loi 17/03/1965 + Directive 2001/23/CE).</p>
 */
class TransfertEntrepriseCct32bisCheckerTest {

    private static final LocalDate DATE_TRANSFERT = LocalDate.of(2026, 3, 15);

    private static TransfertEntrepriseCct32bisRequest reqConforme() {
        return new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.VENTE_FONDS_COMMERCE,
                TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE,
                true, true, true, true, true, true);
    }

    // ── Cas conforme : transfert qualifiant + procédure + maintien ─────────

    @Test
    void analyze_transfertConforme_returnsTransfertConforme() {
        TransfertEntrepriseCct32bisResult r =
                TransfertEntrepriseCct32bisChecker.analyze(reqConforme());

        assertThat(r.verdict())
                .isEqualTo(TransfertEntrepriseCct32bisVerdict.TRANSFERT_CONFORME);
        assertThat(r.eligibleCct32bis()).isTrue();
        assertThat(r.procedureConforme()).isTrue();
        assertThat(r.droitsMaintenus()).isTrue();
        assertThat(r.dateFinResponsabiliteSolidaire())
                .isEqualTo(LocalDate.of(2027, 3, 15));
        assertThat(r.raison()).isEqualTo("TRANSFERT_CONFORME");
        assertThat(r.synthese())
                .contains("CCT n° 32bis")
                .contains("vente de fonds de commerce")
                .contains("information-consultation préalable respectée")
                .contains("2027-03-15");
    }

    // ── Info-consult non respectée → TRANSFERT_NON_CONFORME ────────────────

    @Test
    void analyze_infoConsultNonRespectee_returnsNonConforme() {
        TransfertEntrepriseCct32bisRequest req = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.FUSION_ABSORPTION,
                TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE,
                false, true, true, true, true, true);

        TransfertEntrepriseCct32bisResult r =
                TransfertEntrepriseCct32bisChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(TransfertEntrepriseCct32bisVerdict.TRANSFERT_NON_CONFORME_INFO_CONSULT);
        assertThat(r.eligibleCct32bis()).isTrue();
        assertThat(r.procedureConforme()).isFalse();
        assertThat(r.droitsMaintenus()).isTrue();
        assertThat(r.raison()).isEqualTo("INFO_CONSULT_PREALABLE_NON_RESPECTEE");
        assertThat(r.synthese())
                .contains("Loi 17/03/1965")
                .contains("ordre public");
    }

    @Test
    void analyze_conseilEntrepriseNonConsulte_returnsNonConforme() {
        TransfertEntrepriseCct32bisRequest req = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.SCISSION,
                TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE,
                true, false, true, true, true, true);

        TransfertEntrepriseCct32bisResult r =
                TransfertEntrepriseCct32bisChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(TransfertEntrepriseCct32bisVerdict.TRANSFERT_NON_CONFORME_INFO_CONSULT);
        assertThat(r.procedureConforme()).isFalse();
    }

    // ── Type non qualifiant : 4 cas exhaustifs ─────────────────────────────

    @Test
    void analyze_faillite_returnsIneligibleType() {
        TransfertEntrepriseCct32bisRequest req = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.FAILLITE,
                TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE,
                true, true, true, true, true, true);

        TransfertEntrepriseCct32bisResult r =
                TransfertEntrepriseCct32bisChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(TransfertEntrepriseCct32bisVerdict.INELIGIBLE_TYPE_OPERATION);
        assertThat(r.eligibleCct32bis()).isFalse();
        assertThat(r.dateFinResponsabiliteSolidaire()).isNull();
        assertThat(r.raison()).isEqualTo("TYPE_OPERATION_NON_QUALIFIANT");
        assertThat(r.synthese())
                .contains("faillite judiciaire")
                .contains("CCT n° 102");
    }

    @Test
    void analyze_liquidationJudiciaire_returnsIneligibleType() {
        TransfertEntrepriseCct32bisRequest req = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.LIQUIDATION_JUDICIAIRE,
                TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE,
                true, true, true, true, true, true);

        TransfertEntrepriseCct32bisResult r =
                TransfertEntrepriseCct32bisChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(TransfertEntrepriseCct32bisVerdict.INELIGIBLE_TYPE_OPERATION);
        assertThat(r.synthese()).contains("liquidation judiciaire");
    }

    @Test
    void analyze_cessionActions_returnsIneligibleType() {
        TransfertEntrepriseCct32bisRequest req = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.CESSION_ACTIONS,
                TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE,
                true, true, true, true, true, true);

        TransfertEntrepriseCct32bisResult r =
                TransfertEntrepriseCct32bisChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(TransfertEntrepriseCct32bisVerdict.INELIGIBLE_TYPE_OPERATION);
        assertThat(r.synthese())
                .contains("cession d'actions")
                .contains("personne morale employeur ne change pas");
    }

    @Test
    void analyze_intraGroupeSansCession_returnsIneligibleType() {
        TransfertEntrepriseCct32bisRequest req = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.TRANSFERT_INTRA_GROUPE_SANS_CESSION,
                TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE,
                true, true, true, true, true, true);

        TransfertEntrepriseCct32bisResult r =
                TransfertEntrepriseCct32bisChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(TransfertEntrepriseCct32bisVerdict.INELIGIBLE_TYPE_OPERATION);
        assertThat(r.synthese()).contains("intra-groupe");
    }

    // ── Tous les types qualifiants → éligibles ──────────────────────────────

    @Test
    void analyze_apportUniversalite_returnsConforme() {
        TransfertEntrepriseCct32bisRequest req = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.APPORT_UNIVERSALITE,
                TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE,
                true, true, true, true, true, true);

        TransfertEntrepriseCct32bisResult r =
                TransfertEntrepriseCct32bisChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(TransfertEntrepriseCct32bisVerdict.TRANSFERT_CONFORME);
        assertThat(r.synthese())
                .contains("apport d'universalité ou de branche d'activité");
    }

    @Test
    void analyze_demembrement_returnsConforme() {
        TransfertEntrepriseCct32bisRequest req = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.DEMEMBREMENT,
                TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE,
                true, true, true, true, true, true);

        TransfertEntrepriseCct32bisResult r =
                TransfertEntrepriseCct32bisChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(TransfertEntrepriseCct32bisVerdict.TRANSFERT_CONFORME);
    }

    // ── Identité économique non préservée → INELIGIBLE ─────────────────────

    @Test
    void analyze_identiteNonPreservee_returnsIneligibleEntite() {
        TransfertEntrepriseCct32bisRequest req = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.VENTE_FONDS_COMMERCE,
                TransfertEntrepriseCct32bisIdentiteEconomique.NON_PRESERVEE,
                true, true, true, true, true, true);

        TransfertEntrepriseCct32bisResult r =
                TransfertEntrepriseCct32bisChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(TransfertEntrepriseCct32bisVerdict.INELIGIBLE_PAS_ENTITE_ECONOMIQUE);
        assertThat(r.eligibleCct32bis()).isFalse();
        assertThat(r.dateFinResponsabiliteSolidaire()).isNull();
        assertThat(r.raison()).isEqualTo("PAS_ENTITE_ECONOMIQUE_PRESERVEE");
        assertThat(r.synthese())
                .contains("Süzen")
                .contains("Abler");
    }

    // ── Identité économique A_ANALYSER ──────────────────────────────────────

    @Test
    void analyze_identiteAanalyser_returnsAanalyser() {
        TransfertEntrepriseCct32bisRequest req = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.VENTE_FONDS_COMMERCE,
                TransfertEntrepriseCct32bisIdentiteEconomique.A_ANALYSER,
                true, true, true, true, true, true);

        TransfertEntrepriseCct32bisResult r =
                TransfertEntrepriseCct32bisChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(TransfertEntrepriseCct32bisVerdict.A_ANALYSER);
        assertThat(r.raison()).isEqualTo("IDENTITE_ECONOMIQUE_A_ANALYSER");
        assertThat(r.synthese())
                .contains("ambiguë")
                .contains("avocat spécialisé");
    }

    // ── Hiérarchie : type prime sur identité et procédure ──────────────────

    @Test
    void analyze_typeNonQualifiantEtIdentiteNonPreservee_typeWins() {
        TransfertEntrepriseCct32bisRequest req = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.FAILLITE,
                TransfertEntrepriseCct32bisIdentiteEconomique.NON_PRESERVEE,
                false, false, false, false, false, false);

        TransfertEntrepriseCct32bisResult r =
                TransfertEntrepriseCct32bisChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(TransfertEntrepriseCct32bisVerdict.INELIGIBLE_TYPE_OPERATION);
    }

    @Test
    void analyze_identiteNonPreserveeEtProcedureNonConforme_identiteWins() {
        TransfertEntrepriseCct32bisRequest req = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.VENTE_FONDS_COMMERCE,
                TransfertEntrepriseCct32bisIdentiteEconomique.NON_PRESERVEE,
                false, false, false, false, false, false);

        TransfertEntrepriseCct32bisResult r =
                TransfertEntrepriseCct32bisChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(TransfertEntrepriseCct32bisVerdict.INELIGIBLE_PAS_ENTITE_ECONOMIQUE);
    }

    // ── Maintien droits partiel : TRANSFERT_CONFORME avec réserves ─────────

    @Test
    void analyze_procedureOkButContratsNonMaintenus_returnsConformeAvecReserves() {
        TransfertEntrepriseCct32bisRequest req = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.VENTE_FONDS_COMMERCE,
                TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE,
                true, true, false, true, true, true);

        TransfertEntrepriseCct32bisResult r =
                TransfertEntrepriseCct32bisChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(TransfertEntrepriseCct32bisVerdict.TRANSFERT_CONFORME);
        assertThat(r.procedureConforme()).isTrue();
        assertThat(r.droitsMaintenus()).isFalse();
        assertThat(r.raison()).isEqualTo("TRANSFERT_CONFORME_AVEC_RESERVES_MAINTIEN");
        assertThat(r.synthese())
                .contains("non maintenu")
                .contains("ordre public")
                .contains("art. 7 CCT 32bis");
    }

    @Test
    void analyze_procedureOkButAncienneteNonRespectee_returnsConformeAvecReserves() {
        TransfertEntrepriseCct32bisRequest req = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.VENTE_FONDS_COMMERCE,
                TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE,
                true, true, true, false, true, true);

        TransfertEntrepriseCct32bisResult r =
                TransfertEntrepriseCct32bisChecker.analyze(req);

        assertThat(r.droitsMaintenus()).isFalse();
        assertThat(r.raison()).isEqualTo("TRANSFERT_CONFORME_AVEC_RESERVES_MAINTIEN");
    }

    @Test
    void analyze_procedureOkButRemunerationNonRespectee_returnsConformeAvecReserves() {
        TransfertEntrepriseCct32bisRequest req = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.VENTE_FONDS_COMMERCE,
                TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE,
                true, true, true, true, false, true);

        TransfertEntrepriseCct32bisResult r =
                TransfertEntrepriseCct32bisChecker.analyze(req);

        assertThat(r.droitsMaintenus()).isFalse();
        assertThat(r.raison()).isEqualTo("TRANSFERT_CONFORME_AVEC_RESERVES_MAINTIEN");
    }

    @Test
    void analyze_procedureOkButCctNonMaintenues_returnsConformeAvecReserves() {
        TransfertEntrepriseCct32bisRequest req = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.VENTE_FONDS_COMMERCE,
                TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE,
                true, true, true, true, true, false);

        TransfertEntrepriseCct32bisResult r =
                TransfertEntrepriseCct32bisChecker.analyze(req);

        assertThat(r.droitsMaintenus()).isFalse();
        assertThat(r.raison()).isEqualTo("TRANSFERT_CONFORME_AVEC_RESERVES_MAINTIEN");
    }

    // ── Procédure non conforme + droits non maintenus ──────────────────────

    @Test
    void analyze_procedureEtDroitsNonRespectes_returnsNonConforme() {
        TransfertEntrepriseCct32bisRequest req = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.VENTE_FONDS_COMMERCE,
                TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE,
                false, false, false, false, false, false);

        TransfertEntrepriseCct32bisResult r =
                TransfertEntrepriseCct32bisChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(TransfertEntrepriseCct32bisVerdict.TRANSFERT_NON_CONFORME_INFO_CONSULT);
        assertThat(r.procedureConforme()).isFalse();
        assertThat(r.droitsMaintenus()).isFalse();
        assertThat(r.synthese())
                .contains("droit déclaré non maintenu");
    }

    // ── Snapshot inputs ─────────────────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        TransfertEntrepriseCct32bisResult r =
                TransfertEntrepriseCct32bisChecker.analyze(reqConforme());

        assertThat(r.dateTransfert()).isEqualTo(DATE_TRANSFERT);
        assertThat(r.typeOperation())
                .isEqualTo(TransfertEntrepriseCct32bisTypeOperation.VENTE_FONDS_COMMERCE);
        assertThat(r.identiteEconomique())
                .isEqualTo(TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE);
        assertThat(r.infoConsultPrealableRespectee()).isTrue();
        assertThat(r.conseilEntrepriseConsulte()).isTrue();
        assertThat(r.maintienContratsAutomatique()).isTrue();
        assertThat(r.maintienAncienneteRespecte()).isTrue();
        assertThat(r.maintienRemunerationRespecte()).isTrue();
        assertThat(r.conventionsCollectivesMaintenues()).isTrue();
    }

    // ── Base juridique + avertissement ──────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceCct32bis_Loi17031965_Directive2001_23() {
        TransfertEntrepriseCct32bisResult r =
                TransfertEntrepriseCct32bisChecker.analyze(reqConforme());

        assertThat(r.baseJuridique())
                .contains("CCT n° 32bis du 07/06/1985")
                .contains("Loi du 17/03/1965")
                .contains("Directive 2001/23/CE")
                .contains("Süzen")
                .contains("Abler");
    }

    @Test
    void analyze_avertissementSystematiquePresent() {
        TransfertEntrepriseCct32bisResult r =
                TransfertEntrepriseCct32bisChecker.analyze(reqConforme());

        assertThat(r.avertissement())
                .isNotNull()
                .contains("avocat spécialisé")
                .contains("responsabilité solidaire");
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() ->
                TransfertEntrepriseCct32bisChecker.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_dateTransfertNull_throws() {
        TransfertEntrepriseCct32bisRequest bad = new TransfertEntrepriseCct32bisRequest(
                null,
                TransfertEntrepriseCct32bisTypeOperation.VENTE_FONDS_COMMERCE,
                TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE,
                true, true, true, true, true, true);

        assertThatThrownBy(() ->
                TransfertEntrepriseCct32bisChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateTransfert");
    }

    @Test
    void analyze_typeOperationNull_throws() {
        TransfertEntrepriseCct32bisRequest bad = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT, null,
                TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE,
                true, true, true, true, true, true);

        assertThatThrownBy(() ->
                TransfertEntrepriseCct32bisChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeOperation");
    }

    @Test
    void analyze_identiteEconomiqueNull_throws() {
        TransfertEntrepriseCct32bisRequest bad = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.VENTE_FONDS_COMMERCE,
                null,
                true, true, true, true, true, true);

        assertThatThrownBy(() ->
                TransfertEntrepriseCct32bisChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identiteEconomique");
    }

    @Test
    void analyze_infoConsultNull_throws() {
        TransfertEntrepriseCct32bisRequest bad = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.VENTE_FONDS_COMMERCE,
                TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE,
                null, true, true, true, true, true);

        assertThatThrownBy(() ->
                TransfertEntrepriseCct32bisChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("infoConsultPrealableRespectee");
    }

    @Test
    void analyze_conseilEntrepriseNull_throws() {
        TransfertEntrepriseCct32bisRequest bad = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.VENTE_FONDS_COMMERCE,
                TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE,
                true, null, true, true, true, true);

        assertThatThrownBy(() ->
                TransfertEntrepriseCct32bisChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conseilEntrepriseConsulte");
    }

    @Test
    void analyze_maintienContratsNull_throws() {
        TransfertEntrepriseCct32bisRequest bad = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.VENTE_FONDS_COMMERCE,
                TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE,
                true, true, null, true, true, true);

        assertThatThrownBy(() ->
                TransfertEntrepriseCct32bisChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maintienContratsAutomatique");
    }

    @Test
    void analyze_maintienAncienneteNull_throws() {
        TransfertEntrepriseCct32bisRequest bad = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.VENTE_FONDS_COMMERCE,
                TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE,
                true, true, true, null, true, true);

        assertThatThrownBy(() ->
                TransfertEntrepriseCct32bisChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maintienAncienneteRespecte");
    }

    @Test
    void analyze_maintienRemunerationNull_throws() {
        TransfertEntrepriseCct32bisRequest bad = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.VENTE_FONDS_COMMERCE,
                TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE,
                true, true, true, true, null, true);

        assertThatThrownBy(() ->
                TransfertEntrepriseCct32bisChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maintienRemunerationRespecte");
    }

    @Test
    void analyze_cctMaintenuesNull_throws() {
        TransfertEntrepriseCct32bisRequest bad = new TransfertEntrepriseCct32bisRequest(
                DATE_TRANSFERT,
                TransfertEntrepriseCct32bisTypeOperation.VENTE_FONDS_COMMERCE,
                TransfertEntrepriseCct32bisIdentiteEconomique.PRESERVEE,
                true, true, true, true, true, null);

        assertThatThrownBy(() ->
                TransfertEntrepriseCct32bisChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conventionsCollectivesMaintenues");
    }
}
