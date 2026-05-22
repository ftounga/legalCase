package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-216-07 : tests unitaires du calculateur ARIPA recouvrement FR.
 * Couvre les règles art. L. 581+ CSS + ASF art. L. 523-1 CSS + Convention La
 * Haye 2007 / Règlement UE 4/2009.
 */
class AripaRecouvrementFrCalculatorTest {

    private static AripaRecouvrementFrRequest req(
            Integer pension, Integer mois,
            SituationAripaEnum sitCreancier, SituationAripaEnum sitDebiteur,
            Boolean titre, Boolean debiteurFr, Boolean employeurPublic,
            Integer nbEnfants) {
        return new AripaRecouvrementFrRequest(pension, mois, sitCreancier, sitDebiteur,
                titre, debiteurFr, employeurPublic, nbEnfants);
    }

    // ── AC1 : titre + débiteur salarié FR → SAISIE_SUR_SALAIRE ──────────────

    @Test
    void ac1_titre_debiteur_salarie_fr_renvoie_saisie_sur_salaire() {
        AripaRecouvrementFrRequest r = req(400, 3, SituationAripaEnum.SALARIE, SituationAripaEnum.SALARIE,
                true, true, false, 1);
        AripaRecouvrementFrResult res = AripaRecouvrementFrCalculator.compute(r, "FRANCE");
        assertThat(res.voieRecommandee()).isEqualTo(VoieRecouvrementAripaEnum.SAISIE_SUR_SALAIRE);
        assertThat(res.montantArrieres()).isEqualTo(1_200); // 400 * 3
        assertThat(res.etapes()).isNotEmpty();
        assertThat(res.baseLegale()).contains("L. 582-1");
    }

    // ── AC2 : sans titre → TITRE_REQUIS ─────────────────────────────────────

    @Test
    void ac2_sans_titre_renvoie_titre_requis() {
        AripaRecouvrementFrRequest r = req(300, 2, SituationAripaEnum.SALARIE, SituationAripaEnum.SALARIE,
                false, true, false, 1);
        AripaRecouvrementFrResult res = AripaRecouvrementFrCalculator.compute(r, "FRANCE");
        assertThat(res.voieRecommandee()).isEqualTo(VoieRecouvrementAripaEnum.TITRE_REQUIS);
        assertThat(res.alertes())
                .anyMatch(a -> a.contains("titre exécutoire") && a.contains("ARIPA"));
        assertThat(res.etapes()).anyMatch(e -> e.toLowerCase().contains("obtenir un titre"));
    }

    // ── AC3 : pays BELGIQUE → IllegalArgumentException ──────────────────────

    @Test
    void ac3_pays_belgique_leve_exception() {
        AripaRecouvrementFrRequest r = req(300, 2, SituationAripaEnum.SALARIE, SituationAripaEnum.SALARIE,
                true, true, false, 1);
        assertThatThrownBy(() -> AripaRecouvrementFrCalculator.compute(r, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("France");
    }

    // ── AC4 : débiteur hors France + titre → CONVENTION_INTERNATIONALE ─────

    @Test
    void debiteur_etranger_avec_titre_renvoie_convention_internationale() {
        AripaRecouvrementFrRequest r = req(500, 4, SituationAripaEnum.SALARIE, SituationAripaEnum.INCONNU,
                true, false, false, 2);
        AripaRecouvrementFrResult res = AripaRecouvrementFrCalculator.compute(r, "FRANCE");
        assertThat(res.voieRecommandee()).isEqualTo(VoieRecouvrementAripaEnum.CONVENTION_INTERNATIONALE);
        assertThat(res.etapes()).anyMatch(e -> e.contains("La Haye") || e.contains("Règlement UE"));
        assertThat(res.delaiEstimeJours()).isGreaterThan(60); // procédure internationale longue
    }

    // ── ASF : pension inférieure au seuil → montant ASF > 0 ─────────────────

    @Test
    void pension_inferieure_au_seuil_asf_genere_complement() {
        // 100 €/mois pour 1 enfant → seuil ASF 195, delta = 95.
        AripaRecouvrementFrRequest r = req(100, 2, SituationAripaEnum.SALARIE, SituationAripaEnum.SALARIE,
                true, true, false, 1);
        AripaRecouvrementFrResult res = AripaRecouvrementFrCalculator.compute(r, "FRANCE");
        assertThat(res.montantAsfEligibleMensuelEur()).isEqualTo(95);
        assertThat(res.messages()).anyMatch(m -> m.contains("ASF") && m.contains("complément"));
    }

    // ── ASF : pension >= seuil → pas de complément ──────────────────────────

    @Test
    void pension_superieure_au_seuil_asf_pas_de_complement() {
        AripaRecouvrementFrRequest r = req(250, 2, SituationAripaEnum.SALARIE, SituationAripaEnum.SALARIE,
                true, true, false, 1);
        AripaRecouvrementFrResult res = AripaRecouvrementFrCalculator.compute(r, "FRANCE");
        assertThat(res.montantAsfEligibleMensuelEur()).isEqualTo(0);
    }

    // ── ASF : 2 enfants, 200 €/mois total → 100/enfant → delta 95 × 2 = 190 ─

    @Test
    void asf_deux_enfants_pension_repartie() {
        AripaRecouvrementFrRequest r = req(200, 1, SituationAripaEnum.SALARIE, SituationAripaEnum.SALARIE,
                true, true, false, 2);
        AripaRecouvrementFrResult res = AripaRecouvrementFrCalculator.compute(r, "FRANCE");
        // 200/2 = 100 par enfant, delta = 195-100 = 95, total ASF = 95 * 2 = 190.
        assertThat(res.montantAsfEligibleMensuelEur()).isEqualTo(190);
    }

    // ── Débiteur employeur public + titre → SATD ───────────────────────────

    @Test
    void debiteur_employeur_public_renvoie_satd() {
        AripaRecouvrementFrRequest r = req(400, 3, SituationAripaEnum.SALARIE, SituationAripaEnum.SALARIE,
                true, true, true, 1);
        AripaRecouvrementFrResult res = AripaRecouvrementFrCalculator.compute(r, "FRANCE");
        assertThat(res.voieRecommandee()).isEqualTo(VoieRecouvrementAripaEnum.SATD);
        assertThat(res.etapes()).anyMatch(e -> e.contains("SATD") || e.contains("CAF"));
    }

    // ── Débiteur sans activité + titre → SDR_ARIPA + alerte ─────────────────

    @Test
    void debiteur_sans_activite_renvoie_sdr_aripa_avec_alerte() {
        AripaRecouvrementFrRequest r = req(300, 5, SituationAripaEnum.SALARIE, SituationAripaEnum.SANS_ACTIVITE,
                true, true, false, 1);
        AripaRecouvrementFrResult res = AripaRecouvrementFrCalculator.compute(r, "FRANCE");
        assertThat(res.voieRecommandee()).isEqualTo(VoieRecouvrementAripaEnum.SDR_ARIPA);
        assertThat(res.alertes()).anyMatch(a -> a.contains("sans revenus salariés") || a.contains("ASF"));
    }

    // ── Débiteur inconnu + titre → SDR_ARIPA + alerte investigation ────────

    @Test
    void debiteur_inconnu_renvoie_sdr_aripa_avec_alerte_investigation() {
        AripaRecouvrementFrRequest r = req(300, 6, SituationAripaEnum.SALARIE, SituationAripaEnum.INCONNU,
                true, true, false, 1);
        AripaRecouvrementFrResult res = AripaRecouvrementFrCalculator.compute(r, "FRANCE");
        assertThat(res.voieRecommandee()).isEqualTo(VoieRecouvrementAripaEnum.SDR_ARIPA);
        assertThat(res.alertes()).anyMatch(a -> a.contains("FICOBA") || a.contains("inconnue"));
    }

    // ── Plafond arriérés 24 mois ────────────────────────────────────────────

    @Test
    void arrieres_au_dela_24_mois_genere_message_plafond() {
        AripaRecouvrementFrRequest r = req(300, 30, SituationAripaEnum.SALARIE, SituationAripaEnum.SALARIE,
                true, true, false, 1);
        AripaRecouvrementFrResult res = AripaRecouvrementFrCalculator.compute(r, "FRANCE");
        assertThat(res.montantArrieres()).isEqualTo(9_000); // 300 * 30 — la somme reste calculée
        assertThat(res.messages()).anyMatch(m -> m.contains("24 mois") || m.contains("Décret 2018-1109"));
    }

    // ── Impayé caractérisé ≥ 2 mois → message abandon de famille ───────────

    @Test
    void impaye_deux_mois_mentionne_abandon_de_famille() {
        AripaRecouvrementFrRequest r = req(400, 2, SituationAripaEnum.SALARIE, SituationAripaEnum.SALARIE,
                true, true, false, 1);
        AripaRecouvrementFrResult res = AripaRecouvrementFrCalculator.compute(r, "FRANCE");
        assertThat(res.messages()).anyMatch(m -> m.contains("abandon de famille") || m.contains("227-3"));
    }
}
