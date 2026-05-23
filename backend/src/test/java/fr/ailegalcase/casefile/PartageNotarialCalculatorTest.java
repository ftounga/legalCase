package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-216-27 : tests unitaires du calculateur Partage successoral notarié
 * FR (art. 816 et s. Cciv + art. 870 Cciv + art. 1592 CGI + art. 641 CGI
 * + art. 840 Cciv).
 */
class PartageNotarialCalculatorTest {

    private static PartageNotarialRequest req(
            LocalDate dateOuv,
            Integer nbHer,
            Boolean consentTous,
            Boolean immeuble,
            Boolean desaccord,
            Integer valeur,
            Boolean notaireDesigne,
            LocalDate echeance) {
        return new PartageNotarialRequest(
                dateOuv, nbHer, consentTous, immeuble, desaccord,
                valeur, notaireDesigne, echeance);
    }

    // ── AC1 : immeuble + accord → notaire obligatoire + calendrier 5 étapes ──

    @Test
    void ac1_immeuble_et_accord_tous_calendrier_complet() {
        PartageNotarialRequest r = req(
                LocalDate.now().minusMonths(2),
                3,
                true, true, false,
                500_000, true, null);
        PartageNotarialResult res =
                PartageNotarialCalculator.compute(r, "FRANCE", LocalDate.now());
        assertThat(res.notaireObligatoire()).isTrue();
        assertThat(res.calendrierEtapes()).hasSize(5);
        assertThat(res.calendrierEtapes().get(0)).contains("Désignation");
        assertThat(res.calendrierEtapes().get(2)).contains("Attestation");
        assertThat(res.orientationJudiciaire()).isFalse();
    }

    @Test
    void sans_immeuble_notaire_recommande_pas_obligatoire() {
        PartageNotarialRequest r = req(
                LocalDate.now().minusMonths(1),
                2,
                true, false, false,
                50_000, false, null);
        PartageNotarialResult res =
                PartageNotarialCalculator.compute(r, "FRANCE", LocalDate.now());
        assertThat(res.notaireObligatoire()).isFalse();
        assertThat(res.calendrierEtapes()).hasSize(5);
        assertThat(res.calendrierEtapes().get(2)).contains("Sans objet");
        assertThat(res.messages()).anyMatch(m -> m.contains("recommandé"));
    }

    // ── AC2 : désaccord persistant → orientation judiciaire ──

    @Test
    void ac2_desaccord_persistant_orientation_judiciaire() {
        PartageNotarialRequest r = req(
                LocalDate.now().minusMonths(3),
                4,
                false, true, true,
                300_000, false, null);
        PartageNotarialResult res =
                PartageNotarialCalculator.compute(r, "FRANCE", LocalDate.now());
        assertThat(res.orientationJudiciaire()).isTrue();
        assertThat(res.alertes()).anyMatch(a -> a.contains("F-FA-17-partage-judiciaire"));
        assertThat(res.alertes()).anyMatch(a -> a.contains("840 Cciv"));
        // notaire toujours obligatoire car immeuble
        assertThat(res.notaireObligatoire()).isTrue();
    }

    @Test
    void desaccord_sans_immeuble_orientation_judiciaire_notaire_recommande() {
        PartageNotarialRequest r = req(
                LocalDate.now().minusMonths(1),
                3,
                false, false, true,
                null, false, null);
        PartageNotarialResult res =
                PartageNotarialCalculator.compute(r, "FRANCE", LocalDate.now());
        assertThat(res.orientationJudiciaire()).isTrue();
        assertThat(res.notaireObligatoire()).isFalse();
    }

    // ── AC3 : délai fiscal dépassé → alerte ──

    @Test
    void ac3_delai_fiscal_depasse_alerte() {
        // décès il y a 8 mois → délai dépassé.
        LocalDate ouv = LocalDate.now().minusMonths(8);
        PartageNotarialRequest r = req(
                ouv, 2,
                true, true, false,
                200_000, true, null);
        PartageNotarialResult res =
                PartageNotarialCalculator.compute(r, "FRANCE", LocalDate.now());
        assertThat(res.alerteDelai()).isTrue();
        assertThat(res.alertes()).anyMatch(a -> a.contains("Délai fiscal dépassé"));
        assertThat(res.alertes()).anyMatch(a -> a.contains("641 CGI"));
        assertThat(res.delaiDeclarationFiscale()).isEqualTo(ouv.plusMonths(6));
    }

    @Test
    void delai_fiscal_proche_alerte_30_jours() {
        // today = 2025-06-01, ouv = 2024-12-15, échéance = 2025-06-15
        // → reste 14 jours (≤ 30) → alerte "Délai fiscal proche".
        LocalDate today = LocalDate.of(2025, 6, 1);
        LocalDate ouv = LocalDate.of(2024, 12, 15);
        PartageNotarialRequest r = req(
                ouv, 2,
                true, false, false,
                null, false, null);
        PartageNotarialResult res =
                PartageNotarialCalculator.compute(r, "FRANCE", today);
        assertThat(res.alerteDelai()).isFalse();
        assertThat(res.alertes()).anyMatch(a -> a.contains("Délai fiscal proche"));
    }

    @Test
    void delai_fiscal_loin_message_informatif() {
        // décès il y a 1 mois → reste 5 mois.
        LocalDate ouv = LocalDate.now().minusMonths(1);
        PartageNotarialRequest r = req(
                ouv, 2,
                true, false, false,
                null, false, null);
        PartageNotarialResult res =
                PartageNotarialCalculator.compute(r, "FRANCE", LocalDate.now());
        assertThat(res.alerteDelai()).isFalse();
        assertThat(res.alertes()).noneMatch(a -> a.contains("Délai fiscal"));
        assertThat(res.messages()).anyMatch(m -> m.contains("Déclaration de succession"));
    }

    @Test
    void echeance_explicite_prioritaire_sur_calcul() {
        LocalDate echeance = LocalDate.of(2026, 1, 15);
        PartageNotarialRequest r = req(
                LocalDate.of(2025, 1, 10),
                2,
                true, false, false,
                null, false, echeance);
        PartageNotarialResult res = PartageNotarialCalculator.compute(
                r, "FRANCE", LocalDate.of(2025, 6, 1));
        assertThat(res.delaiDeclarationFiscale()).isEqualTo(echeance);
    }

    // ── AC4 : country=BELGIQUE → IAE ──

    @Test
    void ac4_belgique_throws_illegal_argument() {
        PartageNotarialRequest r = req(
                LocalDate.now().minusMonths(1), 2,
                true, false, false,
                null, false, null);
        assertThatThrownBy(() ->
                PartageNotarialCalculator.compute(r, "BELGIQUE", LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("France");
    }

    // ── Cas complémentaires ──

    @Test
    void heritiers_nombreux_message_vigilance() {
        PartageNotarialRequest r = req(
                LocalDate.now().minusMonths(2), 8,
                true, true, false,
                1_000_000, true, null);
        PartageNotarialResult res =
                PartageNotarialCalculator.compute(r, "FRANCE", LocalDate.now());
        assertThat(res.messages()).anyMatch(m -> m.contains("cohéritiers"));
        assertThat(res.messages()).anyMatch(m -> m.contains("attributions préférentielles"));
    }

    @Test
    void notaire_deja_designe_etape_1_marquee() {
        PartageNotarialRequest r = req(
                LocalDate.now().minusMonths(2), 2,
                true, true, false,
                null, true, null);
        PartageNotarialResult res =
                PartageNotarialCalculator.compute(r, "FRANCE", LocalDate.now());
        assertThat(res.calendrierEtapes().get(0)).contains("DÉJÀ FAITE");
    }

    @Test
    void accord_pas_documente_sans_desaccord_alerte_consentement() {
        PartageNotarialRequest r = req(
                LocalDate.now().minusMonths(2), 3,
                false, true, false,
                null, false, null);
        PartageNotarialResult res =
                PartageNotarialCalculator.compute(r, "FRANCE", LocalDate.now());
        assertThat(res.orientationJudiciaire()).isFalse();
        assertThat(res.alertes()).anyMatch(a -> a.contains("consentement"));
    }

    @Test
    void base_legale_inclut_articles_clefs() {
        PartageNotarialRequest r = req(
                LocalDate.now().minusMonths(1), 2,
                true, false, false,
                null, false, null);
        PartageNotarialResult res =
                PartageNotarialCalculator.compute(r, "FRANCE", LocalDate.now());
        assertThat(res.baseLegale()).contains("816");
        assertThat(res.baseLegale()).contains("1592 CGI");
        assertThat(res.baseLegale()).contains("641 CGI");
        assertThat(res.baseLegale()).contains("840 Cciv");
    }

    @Test
    void overload_sans_today_utilise_localdate_now() {
        PartageNotarialRequest r = req(
                LocalDate.now().minusMonths(1), 2,
                true, false, false,
                null, false, null);
        PartageNotarialResult res = PartageNotarialCalculator.compute(r, "FRANCE");
        // alerteDelai dépend de today — au pire 0 jour de différence avec
        // la version explicite. On vérifie juste que ça ne plante pas.
        assertThat(res.calendrierEtapes()).hasSize(5);
    }
}
