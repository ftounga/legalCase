package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-213-07 : tests unitaires du
 * {@link HarcelementBeProcedureFormelleProcedureChecker}.
 *
 * <p>Vérifie : checklists par étape (5 étapes), délai 90 j enquête CPAP
 * (AR 10/04/2014), fenêtre protection 12 mois (art. 32sexies),
 * avertissements selon taille entreprise / présence CPAP, validation
 * conditionnelle de {@code dateDepotPlainte}.</p>
 */
class HarcelementBeProcedureFormelleProcedureCheckerTest {

    private static final LocalDate DATE_DEPOT = LocalDate.of(2026, 5, 20);

    private static HarcelementBeProcedureFormelleRequest req(
            HarcelementBeTypeEnum type,
            HarcelementBeEtapeEnum etape,
            LocalDate dateDepotPlainte,
            boolean cpap,
            HarcelementBeTailleEntrepriseEnum taille,
            boolean mesureDefavorable,
            Integer delaiDepotJours) {
        return new HarcelementBeProcedureFormelleRequest(
                type, etape, dateDepotPlainte, cpap, taille,
                mesureDefavorable, delaiDepotJours);
    }

    // ── Étape AVANT_DEPOT ──────────────────────────────────────────────────

    @Test
    void analyze_avantDepot_returns3items_pasDeProchainDelaiFatal() {
        HarcelementBeProcedureFormelleResult r =
                HarcelementBeProcedureFormelleProcedureChecker.analyze(
                        req(HarcelementBeTypeEnum.MORAL,
                                HarcelementBeEtapeEnum.AVANT_DEPOT,
                                null, true,
                                HarcelementBeTailleEntrepriseEnum.CINQUANTE_ET_PLUS,
                                false, null));

        assertThat(r.checklistItems()).hasSize(3);
        assertThat(r.checklistItems())
                .extracting(HarcelementBeChecklistItemRecord::statut)
                .containsOnly(HarcelementBeChecklistStatutEnum.A_FAIRE);
        assertThat(r.prochainDelaiFatal()).isNull();
        assertThat(r.representaillesPossibles()).isFalse();
        assertThat(r.dateDebutProtectionRepresailles()).isNull();
        assertThat(r.dateFinProtectionRepresailles()).isNull();
    }

    // ── Étape DEMANDE_INFORMELLE_EN_COURS ──────────────────────────────────

    @Test
    void analyze_demandeInformelle_returns2items() {
        HarcelementBeProcedureFormelleResult r =
                HarcelementBeProcedureFormelleProcedureChecker.analyze(
                        req(HarcelementBeTypeEnum.MORAL,
                                HarcelementBeEtapeEnum.DEMANDE_INFORMELLE_EN_COURS,
                                DATE_DEPOT, true,
                                HarcelementBeTailleEntrepriseEnum.CINQUANTE_ET_PLUS,
                                false, null));

        assertThat(r.checklistItems()).hasSize(2);
        assertThat(r.checklistItems().get(0).statut())
                .isEqualTo(HarcelementBeChecklistStatutEnum.EN_COURS);
        assertThat(r.prochainDelaiFatal()).isNull();
    }

    // ── Étape DEMANDE_FORMELLE_EN_COURS — délais 90 j + protection 12 mois ──

    @Test
    void analyze_demandeFormelle_delai90j_etProtection12mois() {
        HarcelementBeProcedureFormelleResult r =
                HarcelementBeProcedureFormelleProcedureChecker.analyze(
                        req(HarcelementBeTypeEnum.SEXUEL,
                                HarcelementBeEtapeEnum.DEMANDE_FORMELLE_EN_COURS,
                                DATE_DEPOT, true,
                                HarcelementBeTailleEntrepriseEnum.CINQUANTE_ET_PLUS,
                                false, null));

        assertThat(r.checklistItems()).hasSize(3);

        // Item 1 : Enquête CPAP — 90 jours
        HarcelementBeChecklistItemRecord enquete = r.checklistItems().get(0);
        assertThat(enquete.item()).contains("Enquête CPAP").contains("90 jours");
        assertThat(enquete.statut()).isEqualTo(HarcelementBeChecklistStatutEnum.EN_COURS);
        assertThat(enquete.dateEcheance()).isEqualTo(LocalDate.of(2026, 8, 18));

        // Item 3 : Protection 12 mois
        HarcelementBeChecklistItemRecord protection = r.checklistItems().get(2);
        assertThat(protection.item()).contains("Protection").contains("12 mois");
        assertThat(protection.statut()).isEqualTo(HarcelementBeChecklistStatutEnum.ACTIF);
        assertThat(protection.dateEcheance()).isEqualTo(LocalDate.of(2027, 5, 20));

        // prochainDelaiFatal = dateDepot + 90 j
        assertThat(r.prochainDelaiFatal()).isEqualTo(LocalDate.of(2026, 8, 18));

        // Fenêtre de protection 12 mois sur la réponse globale
        assertThat(r.dateDebutProtectionRepresailles()).isEqualTo(DATE_DEPOT);
        assertThat(r.dateFinProtectionRepresailles())
                .isEqualTo(LocalDate.of(2027, 5, 20));
    }

    // ── Étape ENQUETE_TERMINEE ─────────────────────────────────────────────

    @Test
    void analyze_enqueteTerminee_returns2items_pasDeDelaiFatal() {
        HarcelementBeProcedureFormelleResult r =
                HarcelementBeProcedureFormelleProcedureChecker.analyze(
                        req(HarcelementBeTypeEnum.LES_DEUX,
                                HarcelementBeEtapeEnum.ENQUETE_TERMINEE,
                                DATE_DEPOT, true,
                                HarcelementBeTailleEntrepriseEnum.CINQUANTE_ET_PLUS,
                                false, null));

        assertThat(r.checklistItems()).hasSize(2);
        assertThat(r.prochainDelaiFatal()).isNull();
    }

    // ── Étape MESURE_DEFAVORABLE_APRES_PLAINTE ─────────────────────────────

    @Test
    void analyze_mesureDefavorable_3items_avecRepresaillesPossibles() {
        HarcelementBeProcedureFormelleResult r =
                HarcelementBeProcedureFormelleProcedureChecker.analyze(
                        req(HarcelementBeTypeEnum.MORAL,
                                HarcelementBeEtapeEnum.MESURE_DEFAVORABLE_APRES_PLAINTE,
                                DATE_DEPOT, true,
                                HarcelementBeTailleEntrepriseEnum.CINQUANTE_ET_PLUS,
                                true, 30));

        assertThat(r.checklistItems()).hasSize(3);
        assertThat(r.checklistItems().get(0).statut())
                .isEqualTo(HarcelementBeChecklistStatutEnum.ACTIF);
        assertThat(r.representaillesPossibles()).isTrue();
        assertThat(r.dateFinProtectionRepresailles())
                .isEqualTo(LocalDate.of(2027, 5, 20));
    }

    // ── Représailles : flag + date ─────────────────────────────────────────

    @Test
    void analyze_mesureDefavorableSansDate_representaillesPossiblesFalse() {
        HarcelementBeProcedureFormelleResult r =
                HarcelementBeProcedureFormelleProcedureChecker.analyze(
                        req(HarcelementBeTypeEnum.MORAL,
                                HarcelementBeEtapeEnum.AVANT_DEPOT,
                                null, true,
                                HarcelementBeTailleEntrepriseEnum.CINQUANTE_ET_PLUS,
                                true, null));

        // Sans dateDepotPlainte, representaillesPossibles ne peut être activé.
        assertThat(r.representaillesPossibles()).isFalse();
        assertThat(r.dateDebutProtectionRepresailles()).isNull();
        assertThat(r.dateFinProtectionRepresailles()).isNull();
    }

    @Test
    void analyze_mesureDefavorableAvecDate_representaillesPossiblesTrue() {
        HarcelementBeProcedureFormelleResult r =
                HarcelementBeProcedureFormelleProcedureChecker.analyze(
                        req(HarcelementBeTypeEnum.MORAL,
                                HarcelementBeEtapeEnum.DEMANDE_FORMELLE_EN_COURS,
                                DATE_DEPOT, true,
                                HarcelementBeTailleEntrepriseEnum.CINQUANTE_ET_PLUS,
                                true, 10));

        assertThat(r.representaillesPossibles()).isTrue();
        assertThat(r.dateDebutProtectionRepresailles()).isEqualTo(DATE_DEPOT);
        assertThat(r.dateFinProtectionRepresailles())
                .isEqualTo(LocalDate.of(2027, 5, 20));
    }

    // ── Avertissements ─────────────────────────────────────────────────────

    @Test
    void analyze_grandeEntrepriseSansCPAP_avertissementManquementEmployeur() {
        HarcelementBeProcedureFormelleResult r =
                HarcelementBeProcedureFormelleProcedureChecker.analyze(
                        req(HarcelementBeTypeEnum.MORAL,
                                HarcelementBeEtapeEnum.AVANT_DEPOT,
                                null, false,
                                HarcelementBeTailleEntrepriseEnum.CINQUANTE_ET_PLUS,
                                false, null));

        assertThat(r.avertissement())
                .isNotNull()
                .contains("≥ 50")
                .contains("manquement")
                .contains("recours direct tribunal");
    }

    @Test
    void analyze_petiteEntrepriseSansCPAP_avertissementSPFEmploi() {
        HarcelementBeProcedureFormelleResult r =
                HarcelementBeProcedureFormelleProcedureChecker.analyze(
                        req(HarcelementBeTypeEnum.MORAL,
                                HarcelementBeEtapeEnum.AVANT_DEPOT,
                                null, false,
                                HarcelementBeTailleEntrepriseEnum.MOINS_DE_50,
                                false, null));

        assertThat(r.avertissement())
                .isNotNull()
                .contains("< 50")
                .contains("SPF Emploi");
    }

    @Test
    void analyze_avecCPAP_pasAvertissementCPAP() {
        HarcelementBeProcedureFormelleResult r =
                HarcelementBeProcedureFormelleProcedureChecker.analyze(
                        req(HarcelementBeTypeEnum.MORAL,
                                HarcelementBeEtapeEnum.AVANT_DEPOT,
                                null, true,
                                HarcelementBeTailleEntrepriseEnum.CINQUANTE_ET_PLUS,
                                false, null));

        assertThat(r.avertissement()).isNull();
    }

    @Test
    void analyze_mesureAuDela365j_avertissementPresomptionAffaiblie() {
        HarcelementBeProcedureFormelleResult r =
                HarcelementBeProcedureFormelleProcedureChecker.analyze(
                        req(HarcelementBeTypeEnum.MORAL,
                                HarcelementBeEtapeEnum.MESURE_DEFAVORABLE_APRES_PLAINTE,
                                DATE_DEPOT, true,
                                HarcelementBeTailleEntrepriseEnum.CINQUANTE_ET_PLUS,
                                true, 400));

        assertThat(r.avertissement())
                .isNotNull()
                .contains("12 mois")
                .contains("présomption")
                .contains("affaiblie");
    }

    @Test
    void analyze_mesureAvant365j_pasAvertissementPresomptionAffaiblie() {
        HarcelementBeProcedureFormelleResult r =
                HarcelementBeProcedureFormelleProcedureChecker.analyze(
                        req(HarcelementBeTypeEnum.MORAL,
                                HarcelementBeEtapeEnum.MESURE_DEFAVORABLE_APRES_PLAINTE,
                                DATE_DEPOT, true,
                                HarcelementBeTailleEntrepriseEnum.CINQUANTE_ET_PLUS,
                                true, 200));

        assertThat(r.avertissement()).isNull();
    }

    @Test
    void analyze_mesureAExactement365j_borneIncluse_pasDAvertissement() {
        // 365 jours = borne inclusive : pas encore au-delà.
        HarcelementBeProcedureFormelleResult r =
                HarcelementBeProcedureFormelleProcedureChecker.analyze(
                        req(HarcelementBeTypeEnum.MORAL,
                                HarcelementBeEtapeEnum.MESURE_DEFAVORABLE_APRES_PLAINTE,
                                DATE_DEPOT, true,
                                HarcelementBeTailleEntrepriseEnum.CINQUANTE_ET_PLUS,
                                true, 365));

        assertThat(r.avertissement()).isNull();
    }

    // ── Base juridique ─────────────────────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceLoi04_08_1996_et_AR_10_04_2014() {
        HarcelementBeProcedureFormelleResult r =
                HarcelementBeProcedureFormelleProcedureChecker.analyze(
                        req(HarcelementBeTypeEnum.MORAL,
                                HarcelementBeEtapeEnum.AVANT_DEPOT,
                                null, true,
                                HarcelementBeTailleEntrepriseEnum.CINQUANTE_ET_PLUS,
                                false, null));

        assertThat(r.baseJuridique())
                .contains("04/08/1996")
                .contains("32bis-32sexies")
                .contains("10/04/2014");
    }

    // ── Snapshot inputs/outputs ────────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        HarcelementBeProcedureFormelleResult r =
                HarcelementBeProcedureFormelleProcedureChecker.analyze(
                        req(HarcelementBeTypeEnum.LES_DEUX,
                                HarcelementBeEtapeEnum.DEMANDE_FORMELLE_EN_COURS,
                                DATE_DEPOT, true,
                                HarcelementBeTailleEntrepriseEnum.MOINS_DE_50,
                                false, 15));

        assertThat(r.typeHarcelement()).isEqualTo(HarcelementBeTypeEnum.LES_DEUX);
        assertThat(r.etapeProcedure())
                .isEqualTo(HarcelementBeEtapeEnum.DEMANDE_FORMELLE_EN_COURS);
        assertThat(r.dateDepotPlainte()).isEqualTo(DATE_DEPOT);
        assertThat(r.entreprisePossedeCPAP()).isTrue();
        assertThat(r.entrepriseTaille())
                .isEqualTo(HarcelementBeTailleEntrepriseEnum.MOINS_DE_50);
        assertThat(r.mesureDefavorableApres()).isFalse();
        assertThat(r.delaiDepuisDepotJours()).isEqualTo(15);
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() ->
                HarcelementBeProcedureFormelleProcedureChecker.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_dateDepotManquanteHorsAvantDepot_throws() {
        // etape != AVANT_DEPOT mais dateDepotPlainte == null → 400
        assertThatThrownBy(() ->
                HarcelementBeProcedureFormelleProcedureChecker.analyze(
                        req(HarcelementBeTypeEnum.MORAL,
                                HarcelementBeEtapeEnum.DEMANDE_FORMELLE_EN_COURS,
                                null, true,
                                HarcelementBeTailleEntrepriseEnum.CINQUANTE_ET_PLUS,
                                false, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Date de dépôt requise");
    }

    @Test
    void analyze_avantDepotSansDate_pasDException() {
        // etape == AVANT_DEPOT + dateDepotPlainte == null → OK
        HarcelementBeProcedureFormelleResult r =
                HarcelementBeProcedureFormelleProcedureChecker.analyze(
                        req(HarcelementBeTypeEnum.MORAL,
                                HarcelementBeEtapeEnum.AVANT_DEPOT,
                                null, true,
                                HarcelementBeTailleEntrepriseEnum.MOINS_DE_50,
                                false, null));

        assertThat(r.etapeProcedure()).isEqualTo(HarcelementBeEtapeEnum.AVANT_DEPOT);
    }

    @Test
    void analyze_typeHarcelementNull_throws() {
        HarcelementBeProcedureFormelleRequest bad =
                new HarcelementBeProcedureFormelleRequest(
                        null,
                        HarcelementBeEtapeEnum.AVANT_DEPOT,
                        null, true,
                        HarcelementBeTailleEntrepriseEnum.MOINS_DE_50,
                        false, null);
        assertThatThrownBy(() ->
                HarcelementBeProcedureFormelleProcedureChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeHarcelement");
    }

    @Test
    void analyze_etapeNull_throws() {
        HarcelementBeProcedureFormelleRequest bad =
                new HarcelementBeProcedureFormelleRequest(
                        HarcelementBeTypeEnum.MORAL,
                        null,
                        null, true,
                        HarcelementBeTailleEntrepriseEnum.MOINS_DE_50,
                        false, null);
        assertThatThrownBy(() ->
                HarcelementBeProcedureFormelleProcedureChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("etapeProcedure");
    }

    @Test
    void analyze_entrepriseTailleNull_throws() {
        HarcelementBeProcedureFormelleRequest bad =
                new HarcelementBeProcedureFormelleRequest(
                        HarcelementBeTypeEnum.MORAL,
                        HarcelementBeEtapeEnum.AVANT_DEPOT,
                        null, true,
                        null,
                        false, null);
        assertThatThrownBy(() ->
                HarcelementBeProcedureFormelleProcedureChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entrepriseTaille");
    }

    @Test
    void analyze_delaiDepuisDepotNegatif_throws() {
        assertThatThrownBy(() ->
                HarcelementBeProcedureFormelleProcedureChecker.analyze(
                        req(HarcelementBeTypeEnum.MORAL,
                                HarcelementBeEtapeEnum.DEMANDE_FORMELLE_EN_COURS,
                                DATE_DEPOT, true,
                                HarcelementBeTailleEntrepriseEnum.CINQUANTE_ET_PLUS,
                                false, -5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delaiDepuisDepotJours");
    }
}
