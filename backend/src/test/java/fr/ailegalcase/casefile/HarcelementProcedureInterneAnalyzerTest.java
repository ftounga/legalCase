package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-27 : tests unitaires de {@link HarcelementProcedureInterneAnalyzer}
 * (F-DT-59, outil FRANCE uniquement).
 *
 * <p>Logique déterministe (art. L.1153-5-1, L.2314-1, L.1152-4, L.4121-1 CT) :
 * <ul>
 *   <li>checklist : référent CSE (≥ 11), référent employeur (≥ 250), affichage,
 *       enquête contradictoire à réception d'un signalement, mesures
 *       conservatoires ;</li>
 *   <li>signalement reçu sans enquête / non contradictoire / délai NON →
 *       CARENCE_GRAVE + risque ELEVE ;</li>
 *   <li>item obligatoire manquant hors enquête → NON_CONFORME + risque MODERE ;</li>
 *   <li>tout conforme → CONFORME + risque FAIBLE ;</li>
 *   <li>délai OUI ≤ 15 j, LIMITE 16–60 j, NON &gt; 60 j.</li>
 * </ul>
 */
class HarcelementProcedureInterneAnalyzerTest {

    @Test
    void toutConforme_signalementTraite_delaiCourt_conforme() {
        HarcelementProcedureInterneResult r = HarcelementProcedureInterneAnalyzer.analyze(
                40, true, false, true, true,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 8),
                true, true);

        assertThat(r.statut()).isEqualTo(HarcelementProcedureInterneVerdict.CONFORME);
        assertThat(r.risqueResponsabiliteEmployeur())
                .isEqualTo(HarcelementProcedureInterneRisque.FAIBLE);
        assertThat(r.itemsObligatoiresManquants()).isZero();
        assertThat(r.delaiReactionJours()).isEqualTo(7);
        assertThat(r.delaiRaisonnable())
                .isEqualTo(HarcelementProcedureInterneDelaiRaisonnable.OUI);
        assertThat(r.baseJuridique()).contains("L.2314-1").contains("L.4121-1");
    }

    @Test
    void referentCseManquant_effectif12_nonConforme() {
        HarcelementProcedureInterneResult r = HarcelementProcedureInterneAnalyzer.analyze(
                12, false, false, true, false,
                null, null, false, false);

        // Pas de signalement → pas de carence d'enquête, mais référent CSE
        // obligatoire (≥ 11) non désigné → NON_CONFORME.
        assertThat(r.statut()).isEqualTo(HarcelementProcedureInterneVerdict.NON_CONFORME);
        assertThat(r.risqueResponsabiliteEmployeur())
                .isEqualTo(HarcelementProcedureInterneRisque.MODERE);
        assertThat(r.checklist())
                .anySatisfy(i -> {
                    assertThat(i.item()).contains("Référent CSE");
                    assertThat(i.obligatoire()).isTrue();
                    assertThat(i.conforme()).isFalse();
                });
        assertThat(r.itemsObligatoiresManquants()).isEqualTo(1);
    }

    @Test
    void signalementRecu_sansEnquete_carenceGrave_risqueEleve() {
        HarcelementProcedureInterneResult r = HarcelementProcedureInterneAnalyzer.analyze(
                30, true, false, true, true,
                LocalDate.of(2025, 1, 1), null, false, false);

        assertThat(r.statut()).isEqualTo(HarcelementProcedureInterneVerdict.CARENCE_GRAVE);
        assertThat(r.risqueResponsabiliteEmployeur())
                .isEqualTo(HarcelementProcedureInterneRisque.ELEVE);
        assertThat(r.delaiRaisonnable())
                .isEqualTo(HarcelementProcedureInterneDelaiRaisonnable.NON);
        assertThat(r.consequences())
                .anySatisfy(c -> assertThat(c).contains("L.4121-1"));
    }

    @Test
    void signalementRecu_enqueteNonContradictoire_carenceGrave() {
        HarcelementProcedureInterneResult r = HarcelementProcedureInterneAnalyzer.analyze(
                30, true, false, true, true,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 5),
                false, true);

        assertThat(r.statut()).isEqualTo(HarcelementProcedureInterneVerdict.CARENCE_GRAVE);
        assertThat(r.risqueResponsabiliteEmployeur())
                .isEqualTo(HarcelementProcedureInterneRisque.ELEVE);
    }

    @Test
    void referentEmployeurObligatoire_effectif300_nonDesigne_nonConforme() {
        HarcelementProcedureInterneResult r = HarcelementProcedureInterneAnalyzer.analyze(
                300, true, false, true, false,
                null, null, false, false);

        assertThat(r.checklist())
                .anySatisfy(i -> {
                    assertThat(i.item()).contains("Référent employeur");
                    assertThat(i.obligatoire()).isTrue();
                    assertThat(i.conforme()).isFalse();
                });
        assertThat(r.statut()).isEqualTo(HarcelementProcedureInterneVerdict.NON_CONFORME);
    }

    @Test
    void referentEmployeur_nonObligatoire_sousSeuil250() {
        HarcelementProcedureInterneResult r = HarcelementProcedureInterneAnalyzer.analyze(
                100, true, false, true, false,
                null, null, false, false);

        // À 100 salariés, le référent employeur n'est pas obligatoire → item non
        // obligatoire, tout le reste conforme → CONFORME.
        assertThat(r.checklist())
                .anySatisfy(i -> {
                    assertThat(i.item()).contains("Référent employeur");
                    assertThat(i.obligatoire()).isFalse();
                });
        assertThat(r.statut()).isEqualTo(HarcelementProcedureInterneVerdict.CONFORME);
    }

    @Test
    void delai_OUI_LIMITE_NON_selonNombreDeJours() {
        // 10 jours → OUI
        assertThat(HarcelementProcedureInterneAnalyzer.analyze(
                30, true, false, true, true,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 11), true, true)
                .delaiRaisonnable()).isEqualTo(HarcelementProcedureInterneDelaiRaisonnable.OUI);

        // 30 jours → LIMITE (mais enquête contradictoire et dans le délai limite → CONFORME)
        HarcelementProcedureInterneResult limite = HarcelementProcedureInterneAnalyzer.analyze(
                30, true, false, true, true,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), true, true);
        assertThat(limite.delaiRaisonnable())
                .isEqualTo(HarcelementProcedureInterneDelaiRaisonnable.LIMITE);
        assertThat(limite.delaiReactionJours()).isEqualTo(30);

        // 90 jours → NON → CARENCE_GRAVE
        HarcelementProcedureInterneResult tardif = HarcelementProcedureInterneAnalyzer.analyze(
                30, true, false, true, true,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 4, 1), true, true);
        assertThat(tardif.delaiRaisonnable())
                .isEqualTo(HarcelementProcedureInterneDelaiRaisonnable.NON);
        assertThat(tardif.statut())
                .isEqualTo(HarcelementProcedureInterneVerdict.CARENCE_GRAVE);
    }

    @Test
    void effectifNul_leveIllegalArgument() {
        assertThatThrownBy(() -> HarcelementProcedureInterneAnalyzer.analyze(
                0, true, false, true, false, null, null, false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void champsRequisNull_leveIllegalArgument() {
        assertThatThrownBy(() -> HarcelementProcedureInterneAnalyzer.analyze(
                null, true, false, true, false, null, null, false, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HarcelementProcedureInterneAnalyzer.analyze(
                30, null, false, true, false, null, null, false, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HarcelementProcedureInterneAnalyzer.analyze(
                30, true, false, null, false, null, null, false, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HarcelementProcedureInterneAnalyzer.analyze(
                30, true, false, true, null, null, null, false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dateOuvertureEnqueteAvantSignalement_leveIllegalArgument() {
        assertThatThrownBy(() -> HarcelementProcedureInterneAnalyzer.analyze(
                30, true, false, true, true,
                LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 1), true, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
