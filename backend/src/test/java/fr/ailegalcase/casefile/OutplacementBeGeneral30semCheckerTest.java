package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-05 : tests unitaires du
 * {@link OutplacementBeGeneral30semChecker}.
 *
 * <p>Vérifie : conditions cumulatives (licenciement / motif non grave /
 * préavis ≥ 30 sem / offre dans les 15 jours / programme ≥ 60h sur ≤ 12
 * mois / forme écrite individuelle + contenu minimum), hiérarchie des
 * raisons de non-conformité (démission > motif grave > préavis > offre
 * tardive > durée > forme), indemnité forfaitaire travailleur indicative,
 * snapshot des inputs, validation des bornes (négatifs, null), base
 * juridique stable.</p>
 */
class OutplacementBeGeneral30semCheckerTest {

    private static final LocalDate DATE_FIN = LocalDate.of(2026, 5, 1);
    private static final LocalDate OFFRE_DANS_LES_15J =
            LocalDate.of(2026, 5, 10);
    private static final LocalDate OFFRE_TARDIVE =
            LocalDate.of(2026, 5, 20);

    /** Cas de base conforme : licenciement, motif non grave, préavis 35
     *  semaines, offre J+9, programme 80h sur 12 mois, forme conforme. */
    private static OutplacementBeGeneral30semRequest reqBase() {
        return new OutplacementBeGeneral30semRequest(
                true,                 // licenciementEffectif
                false,                // motifGrave
                35,                   // semainesPreavisOuIndemnite
                DATE_FIN,             // dateFinContrat
                true,                 // offreNotifiee
                OFFRE_DANS_LES_15J,   // dateNotificationOffre
                80,                   // heuresProgramme
                12,                   // dureeProgrammeMois
                true,                 // offreEcrite
                true,                 // offreIndividuelle
                true                  // contenuMinimumRespecte
        );
    }

    // ── Cas nominal : conforme ─────────────────────────────────────────────

    @Test
    void analyze_toutesConditionsRemplies_returnsConforme() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(reqBase());

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeGeneral30semVerdict.CONFORME);
        assertThat(r.conforme()).isTrue();
        assertThat(r.conditionLicenciementRemplie()).isTrue();
        assertThat(r.conditionMotifNonGraveRemplie()).isTrue();
        assertThat(r.conditionPreavis30semRemplie()).isTrue();
        assertThat(r.conditionOffreDansLes15joursRemplie()).isTrue();
        assertThat(r.conditionDureeProgrammeRemplie()).isTrue();
        assertThat(r.conditionFormeOffreRemplie()).isTrue();
        assertThat(r.indemniteForfaitaireTravailleur()).isNull();
        assertThat(r.avertissement()).isNull();
        assertThat(r.synthese()).contains("régulière").contains("30 semaines");
    }

    @Test
    void analyze_conformePreavisExact30Semaines_returnsConforme() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(
                        new OutplacementBeGeneral30semRequest(
                                true, false, 30, DATE_FIN,
                                true, OFFRE_DANS_LES_15J,
                                60, 12, true, true, true));

        assertThat(r.conforme()).isTrue();
    }

    @Test
    void analyze_conformeOffreJourMemeFinContrat_returnsConforme() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(
                        new OutplacementBeGeneral30semRequest(
                                true, false, 35, DATE_FIN,
                                true, DATE_FIN,
                                60, 12, true, true, true));

        assertThat(r.conforme()).isTrue();
    }

    @Test
    void analyze_conformeOffreJ15Exact_returnsConforme() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(
                        new OutplacementBeGeneral30semRequest(
                                true, false, 35, DATE_FIN,
                                true, DATE_FIN.plusDays(15),
                                60, 12, true, true, true));

        assertThat(r.conforme()).isTrue();
    }

    @Test
    void analyze_conformeOffreAnticipee_returnsConforme() {
        // Offre faite avant la fin du contrat (anticipation favorable) : OK
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(
                        new OutplacementBeGeneral30semRequest(
                                true, false, 35, DATE_FIN,
                                true, DATE_FIN.minusDays(10),
                                80, 12, true, true, true));

        assertThat(r.conforme()).isTrue();
    }

    // ── Verdicts NON_DU_* ──────────────────────────────────────────────────

    @Test
    void analyze_demission_returnsNonDuDemission() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(
                        new OutplacementBeGeneral30semRequest(
                                false, false, 35, DATE_FIN,
                                true, OFFRE_DANS_LES_15J,
                                80, 12, true, true, true));

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeGeneral30semVerdict.NON_DU_DEMISSION);
        assertThat(r.conforme()).isFalse();
        assertThat(r.conditionLicenciementRemplie()).isFalse();
        assertThat(r.indemniteForfaitaireTravailleur()).isNull();
        assertThat(r.synthese()).contains("Démission");
    }

    @Test
    void analyze_motifGrave_returnsNonDuMotifGrave() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(
                        new OutplacementBeGeneral30semRequest(
                                true, true, 35, DATE_FIN,
                                true, OFFRE_DANS_LES_15J,
                                80, 12, true, true, true));

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeGeneral30semVerdict.NON_DU_MOTIF_GRAVE);
        assertThat(r.conditionMotifNonGraveRemplie()).isFalse();
        assertThat(r.indemniteForfaitaireTravailleur()).isNull();
        assertThat(r.synthese()).contains("motif grave");
    }

    @Test
    void analyze_preavisInsuffisant_returnsNonDuPreavis() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(
                        new OutplacementBeGeneral30semRequest(
                                true, false, 29, DATE_FIN,
                                true, OFFRE_DANS_LES_15J,
                                80, 12, true, true, true));

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeGeneral30semVerdict.NON_DU_PREAVIS_INSUFFISANT);
        assertThat(r.conditionPreavis30semRemplie()).isFalse();
        assertThat(r.indemniteForfaitaireTravailleur()).isNull();
        assertThat(r.synthese()).contains("< 30 semaines").contains("45+");
    }

    // ── Verdicts NON_CONFORME_* ────────────────────────────────────────────

    @Test
    void analyze_offreNonNotifiee_returnsNonConformeOffreTardive() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(
                        new OutplacementBeGeneral30semRequest(
                                true, false, 35, DATE_FIN,
                                false, null,
                                80, 12, true, true, true));

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeGeneral30semVerdict.NON_CONFORME_OFFRE_TARDIVE);
        assertThat(r.conditionOffreDansLes15joursRemplie()).isFalse();
        assertThat(r.indemniteForfaitaireTravailleur())
                .isEqualByComparingTo(new BigDecimal("1800.00"));
        assertThat(r.avertissement()).isNotNull().contains("indicative").contains("ONEM");
    }

    @Test
    void analyze_offreApresJ15_returnsNonConformeOffreTardive() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(
                        new OutplacementBeGeneral30semRequest(
                                true, false, 35, DATE_FIN,
                                true, OFFRE_TARDIVE,
                                80, 12, true, true, true));

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeGeneral30semVerdict.NON_CONFORME_OFFRE_TARDIVE);
        assertThat(r.conditionOffreDansLes15joursRemplie()).isFalse();
        assertThat(r.indemniteForfaitaireTravailleur())
                .isEqualByComparingTo(new BigDecimal("1800.00"));
    }

    @Test
    void analyze_offreJ16_returnsNonConformeOffreTardive() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(
                        new OutplacementBeGeneral30semRequest(
                                true, false, 35, DATE_FIN,
                                true, DATE_FIN.plusDays(16),
                                80, 12, true, true, true));

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeGeneral30semVerdict.NON_CONFORME_OFFRE_TARDIVE);
    }

    @Test
    void analyze_heuresInsuffisantes_returnsNonConformeDuree() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(
                        new OutplacementBeGeneral30semRequest(
                                true, false, 35, DATE_FIN,
                                true, OFFRE_DANS_LES_15J,
                                40, 12, true, true, true));

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeGeneral30semVerdict.NON_CONFORME_DUREE_INSUFFISANTE);
        assertThat(r.conditionDureeProgrammeRemplie()).isFalse();
        assertThat(r.indemniteForfaitaireTravailleur())
                .isEqualByComparingTo(new BigDecimal("1800.00"));
    }

    @Test
    void analyze_dureeMoisExcessive_returnsNonConformeDuree() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(
                        new OutplacementBeGeneral30semRequest(
                                true, false, 35, DATE_FIN,
                                true, OFFRE_DANS_LES_15J,
                                80, 18, true, true, true));

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeGeneral30semVerdict.NON_CONFORME_DUREE_INSUFFISANTE);
        assertThat(r.conditionDureeProgrammeRemplie()).isFalse();
    }

    @Test
    void analyze_dureeMoisZero_returnsNonConformeDuree() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(
                        new OutplacementBeGeneral30semRequest(
                                true, false, 35, DATE_FIN,
                                true, OFFRE_DANS_LES_15J,
                                80, 0, true, true, true));

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeGeneral30semVerdict.NON_CONFORME_DUREE_INSUFFISANTE);
    }

    @Test
    void analyze_offrePasEcrite_returnsNonConformeForme() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(
                        new OutplacementBeGeneral30semRequest(
                                true, false, 35, DATE_FIN,
                                true, OFFRE_DANS_LES_15J,
                                80, 12, false, true, true));

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeGeneral30semVerdict.NON_CONFORME_FORME);
        assertThat(r.conditionFormeOffreRemplie()).isFalse();
        assertThat(r.indemniteForfaitaireTravailleur())
                .isEqualByComparingTo(new BigDecimal("1800.00"));
        assertThat(r.synthese()).contains("écrit").contains("individuel");
    }

    @Test
    void analyze_offrePasIndividuelle_returnsNonConformeForme() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(
                        new OutplacementBeGeneral30semRequest(
                                true, false, 35, DATE_FIN,
                                true, OFFRE_DANS_LES_15J,
                                80, 12, true, false, true));

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeGeneral30semVerdict.NON_CONFORME_FORME);
    }

    @Test
    void analyze_contenuMinimumAbsent_returnsNonConformeForme() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(
                        new OutplacementBeGeneral30semRequest(
                                true, false, 35, DATE_FIN,
                                true, OFFRE_DANS_LES_15J,
                                80, 12, true, true, false));

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeGeneral30semVerdict.NON_CONFORME_FORME);
    }

    // ── Hiérarchie des verdicts ────────────────────────────────────────────

    @Test
    void analyze_demissionEtPreavisInsuffisant_returnsDemissionPriorite() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(
                        new OutplacementBeGeneral30semRequest(
                                false, true, 10, DATE_FIN,
                                false, null,
                                10, 1, false, false, false));

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeGeneral30semVerdict.NON_DU_DEMISSION);
        assertThat(r.indemniteForfaitaireTravailleur()).isNull();
    }

    @Test
    void analyze_motifGraveEtPreavisInsuffisant_returnsMotifGravePriorite() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(
                        new OutplacementBeGeneral30semRequest(
                                true, true, 10, DATE_FIN,
                                false, null,
                                10, 1, false, false, false));

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeGeneral30semVerdict.NON_DU_MOTIF_GRAVE);
        assertThat(r.indemniteForfaitaireTravailleur()).isNull();
    }

    @Test
    void analyze_preavisInsuffisantEtOffreManquante_returnsPreavisPriorite() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(
                        new OutplacementBeGeneral30semRequest(
                                true, false, 10, DATE_FIN,
                                false, null,
                                10, 1, false, false, false));

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeGeneral30semVerdict.NON_DU_PREAVIS_INSUFFISANT);
        assertThat(r.indemniteForfaitaireTravailleur()).isNull();
    }

    @Test
    void analyze_offreTardiveEtDureeInsuffisante_returnsOffreTardivePriorite() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(
                        new OutplacementBeGeneral30semRequest(
                                true, false, 35, DATE_FIN,
                                true, OFFRE_TARDIVE,
                                40, 1, false, false, false));

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeGeneral30semVerdict.NON_CONFORME_OFFRE_TARDIVE);
        // Toutes les autres conditions restent visibles via les flags
        assertThat(r.conditionDureeProgrammeRemplie()).isFalse();
        assertThat(r.conditionFormeOffreRemplie()).isFalse();
        assertThat(r.synthese())
                .contains("D'autres défauts de conformité sont également relevés");
    }

    @Test
    void analyze_dureeEtFormeKO_returnsDureePriorite() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(
                        new OutplacementBeGeneral30semRequest(
                                true, false, 35, DATE_FIN,
                                true, OFFRE_DANS_LES_15J,
                                40, 12, false, false, false));

        assertThat(r.verdict())
                .isEqualTo(OutplacementBeGeneral30semVerdict.NON_CONFORME_DUREE_INSUFFISANTE);
        assertThat(r.synthese())
                .contains("forme de l'offre présente également des défauts");
    }

    // ── Snapshot complet des inputs ─────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(
                        new OutplacementBeGeneral30semRequest(
                                true, false, 40, DATE_FIN,
                                true, OFFRE_DANS_LES_15J,
                                72, 9, true, true, true));

        assertThat(r.licenciementEffectif()).isTrue();
        assertThat(r.motifGrave()).isFalse();
        assertThat(r.semainesPreavisOuIndemnite()).isEqualTo(40);
        assertThat(r.dateFinContrat()).isEqualTo(DATE_FIN);
        assertThat(r.offreNotifiee()).isTrue();
        assertThat(r.dateNotificationOffre()).isEqualTo(OFFRE_DANS_LES_15J);
        assertThat(r.heuresProgramme()).isEqualTo(72);
        assertThat(r.dureeProgrammeMois()).isEqualTo(9);
        assertThat(r.offreEcrite()).isTrue();
        assertThat(r.offreIndividuelle()).isTrue();
        assertThat(r.contenuMinimumRespecte()).isTrue();
    }

    // ── Base juridique ─────────────────────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceCadreComplet() {
        OutplacementBeGeneral30semResult r =
                OutplacementBeGeneral30semChecker.analyze(reqBase());

        assertThat(r.baseJuridique())
                .contains("05/09/2001")
                .contains("art. 11")
                .contains("AR du 21/10/2007")
                .contains("CCT n° 82");
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() ->
                OutplacementBeGeneral30semChecker.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_licenciementEffectifNull_throws() {
        OutplacementBeGeneral30semRequest bad =
                new OutplacementBeGeneral30semRequest(
                        null, false, 35, DATE_FIN, true, OFFRE_DANS_LES_15J,
                        80, 12, true, true, true);
        assertThatThrownBy(() -> OutplacementBeGeneral30semChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("licenciementEffectif");
    }

    @Test
    void analyze_motifGraveNull_throws() {
        OutplacementBeGeneral30semRequest bad =
                new OutplacementBeGeneral30semRequest(
                        true, null, 35, DATE_FIN, true, OFFRE_DANS_LES_15J,
                        80, 12, true, true, true);
        assertThatThrownBy(() -> OutplacementBeGeneral30semChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifGrave");
    }

    @Test
    void analyze_semainesPreavisNull_throws() {
        OutplacementBeGeneral30semRequest bad =
                new OutplacementBeGeneral30semRequest(
                        true, false, null, DATE_FIN, true, OFFRE_DANS_LES_15J,
                        80, 12, true, true, true);
        assertThatThrownBy(() -> OutplacementBeGeneral30semChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("semainesPreavisOuIndemnite");
    }

    @Test
    void analyze_semainesPreavisNegatif_throws() {
        OutplacementBeGeneral30semRequest bad =
                new OutplacementBeGeneral30semRequest(
                        true, false, -1, DATE_FIN, true, OFFRE_DANS_LES_15J,
                        80, 12, true, true, true);
        assertThatThrownBy(() -> OutplacementBeGeneral30semChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("semainesPreavisOuIndemnite");
    }

    @Test
    void analyze_dateFinContratNull_throws() {
        OutplacementBeGeneral30semRequest bad =
                new OutplacementBeGeneral30semRequest(
                        true, false, 35, null, true, OFFRE_DANS_LES_15J,
                        80, 12, true, true, true);
        assertThatThrownBy(() -> OutplacementBeGeneral30semChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateFinContrat");
    }

    @Test
    void analyze_offreNotifieeNull_throws() {
        OutplacementBeGeneral30semRequest bad =
                new OutplacementBeGeneral30semRequest(
                        true, false, 35, DATE_FIN, null, OFFRE_DANS_LES_15J,
                        80, 12, true, true, true);
        assertThatThrownBy(() -> OutplacementBeGeneral30semChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offreNotifiee");
    }

    @Test
    void analyze_heuresProgrammeNull_throws() {
        OutplacementBeGeneral30semRequest bad =
                new OutplacementBeGeneral30semRequest(
                        true, false, 35, DATE_FIN, true, OFFRE_DANS_LES_15J,
                        null, 12, true, true, true);
        assertThatThrownBy(() -> OutplacementBeGeneral30semChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("heuresProgramme");
    }

    @Test
    void analyze_heuresProgrammeNegatif_throws() {
        OutplacementBeGeneral30semRequest bad =
                new OutplacementBeGeneral30semRequest(
                        true, false, 35, DATE_FIN, true, OFFRE_DANS_LES_15J,
                        -1, 12, true, true, true);
        assertThatThrownBy(() -> OutplacementBeGeneral30semChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("heuresProgramme");
    }

    @Test
    void analyze_dureeProgrammeNull_throws() {
        OutplacementBeGeneral30semRequest bad =
                new OutplacementBeGeneral30semRequest(
                        true, false, 35, DATE_FIN, true, OFFRE_DANS_LES_15J,
                        80, null, true, true, true);
        assertThatThrownBy(() -> OutplacementBeGeneral30semChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeProgrammeMois");
    }

    @Test
    void analyze_dureeProgrammeNegative_throws() {
        OutplacementBeGeneral30semRequest bad =
                new OutplacementBeGeneral30semRequest(
                        true, false, 35, DATE_FIN, true, OFFRE_DANS_LES_15J,
                        80, -1, true, true, true);
        assertThatThrownBy(() -> OutplacementBeGeneral30semChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeProgrammeMois");
    }

    @Test
    void analyze_offreEcriteNull_throws() {
        OutplacementBeGeneral30semRequest bad =
                new OutplacementBeGeneral30semRequest(
                        true, false, 35, DATE_FIN, true, OFFRE_DANS_LES_15J,
                        80, 12, null, true, true);
        assertThatThrownBy(() -> OutplacementBeGeneral30semChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offreEcrite");
    }

    @Test
    void analyze_offreIndividuelleNull_throws() {
        OutplacementBeGeneral30semRequest bad =
                new OutplacementBeGeneral30semRequest(
                        true, false, 35, DATE_FIN, true, OFFRE_DANS_LES_15J,
                        80, 12, true, null, true);
        assertThatThrownBy(() -> OutplacementBeGeneral30semChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offreIndividuelle");
    }

    @Test
    void analyze_contenuMinimumNull_throws() {
        OutplacementBeGeneral30semRequest bad =
                new OutplacementBeGeneral30semRequest(
                        true, false, 35, DATE_FIN, true, OFFRE_DANS_LES_15J,
                        80, 12, true, true, null);
        assertThatThrownBy(() -> OutplacementBeGeneral30semChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contenuMinimumRespecte");
    }

    @Test
    void analyze_offreNotifieeTrueMaisSansDate_throws_coherence() {
        OutplacementBeGeneral30semRequest bad =
                new OutplacementBeGeneral30semRequest(
                        true, false, 35, DATE_FIN, true, null,
                        80, 12, true, true, true);
        assertThatThrownBy(() -> OutplacementBeGeneral30semChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNotificationOffre");
    }
}
