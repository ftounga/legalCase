package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-01 : tests unitaires de {@link AppelCphAnalyzer}. Couvre les 4 verdicts
 * (DELAI_OUVERT, DELAI_URGENT, DELAI_EXPIRE, VOIE_FERMEE), le calcul du délai
 * d'appel d'un mois (art. 538 CPC), l'item bloquant représentation, le renvoi
 * vers le pourvoi en cassation (F-DT-87) et la validation.
 */
class AppelCphAnalyzerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 30);

    // ── DELAI_OUVERT : notification J-10, ~20 jours restants ──

    @Test
    void analyze_notificationJ10_delaiOuvert_dateLimitePlusUnMois() {
        LocalDate notification = TODAY.minusDays(10);
        AppelCphResult r = AppelCphAnalyzer.analyze(
                notification, AppelCphPartieAppelante.SALARIE,
                AppelCphModeNotification.SIGNIFICATION, AppelCphRepresentation.AVOCAT,
                false, TODAY);

        assertThat(r.verdict()).isEqualTo(AppelCphVerdict.DELAI_OUVERT);
        assertThat(r.dateLimiteAppel()).isEqualTo(notification.plusMonths(1));
        // notification = 20/05, limite = 20/06, today = 30/05 → 21 jours restants
        assertThat(r.joursRestants()).isEqualTo(21);
        assertThat(r.renvoiPourvoiCassation()).isNull();
    }

    // ── DELAI_URGENT : notification J-29, ~1 jour restant ──

    @Test
    void analyze_notificationJ29_delaiUrgent() {
        LocalDate notification = TODAY.minusDays(29);
        AppelCphResult r = AppelCphAnalyzer.analyze(
                notification, AppelCphPartieAppelante.EMPLOYEUR,
                AppelCphModeNotification.LRAR, AppelCphRepresentation.DEFENSEUR_SYNDICAL,
                false, TODAY);

        assertThat(r.verdict()).isEqualTo(AppelCphVerdict.DELAI_URGENT);
        assertThat(r.joursRestants()).isBetween(1L, 7L);
    }

    // ── DELAI_EXPIRE : notification J-40, délai d'un mois dépassé ──

    @Test
    void analyze_notificationJ40_delaiExpire() {
        AppelCphResult r = AppelCphAnalyzer.analyze(
                TODAY.minusDays(40), AppelCphPartieAppelante.SALARIE,
                AppelCphModeNotification.SIGNIFICATION, AppelCphRepresentation.AVOCAT,
                false, TODAY);

        assertThat(r.verdict()).isEqualTo(AppelCphVerdict.DELAI_EXPIRE);
        assertThat(r.joursRestants()).isNegative();
    }

    // ── VOIE_FERMEE : jugement en dernier ressort → renvoi pourvoi F-DT-87 ──

    @Test
    void analyze_jugementEnDernierRessort_voieFermee_renvoiPourvoi() {
        AppelCphResult r = AppelCphAnalyzer.analyze(
                TODAY.minusDays(5), AppelCphPartieAppelante.SALARIE,
                AppelCphModeNotification.SIGNIFICATION, AppelCphRepresentation.AVOCAT,
                true, TODAY);

        assertThat(r.verdict()).isEqualTo(AppelCphVerdict.VOIE_FERMEE);
        assertThat(r.renvoiPourvoiCassation()).isNotNull();
        assertThat(r.renvoiPourvoiCassation())
                .containsIgnoringCase("pourvoi")
                .contains("F-DT-87");
    }

    // ── Représentation AUCUNE → item checklist bloquant ──

    @Test
    void analyze_representationAucune_itemBloquantRepresentation() {
        AppelCphResult r = AppelCphAnalyzer.analyze(
                TODAY.minusDays(3), AppelCphPartieAppelante.SALARIE,
                AppelCphModeNotification.SIGNIFICATION, AppelCphRepresentation.AUCUNE,
                false, TODAY);

        assertThat(r.checklist()).anySatisfy(item -> {
            assertThat(item.libelle()).containsIgnoringCase("représentation");
            assertThat(item.obligatoire()).isTrue();
            assertThat(item.bloquant()).isTrue();
            assertThat(item.baseJuridique()).contains("R. 1461-2");
        });
    }

    @Test
    void analyze_representationAvocat_aucunItemBloquant() {
        AppelCphResult r = AppelCphAnalyzer.analyze(
                TODAY.minusDays(3), AppelCphPartieAppelante.SALARIE,
                AppelCphModeNotification.SIGNIFICATION, AppelCphRepresentation.AVOCAT,
                false, TODAY);

        assertThat(r.checklist()).noneMatch(AppelCphChecklistItem::bloquant);
    }

    // ── Calcul date limite = notification + 1 mois (changement de mois) ──

    @Test
    void analyze_dateLimiteAppel_egaleNotificationPlusUnMois() {
        LocalDate notification = LocalDate.of(2026, 1, 31);
        AppelCphResult r = AppelCphAnalyzer.analyze(
                notification, AppelCphPartieAppelante.SALARIE,
                AppelCphModeNotification.SIGNIFICATION, AppelCphRepresentation.AVOCAT,
                false, LocalDate.of(2026, 2, 10));

        // 31/01 + 1 mois = 28/02 (résolution fin de mois LocalDate)
        assertThat(r.dateLimiteAppel()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(r.baseJuridique()).contains("538");
    }

    // ── Checklist contient les formalités attendues ──

    @Test
    void analyze_checklist_contientFormalitesAppelSocial() {
        AppelCphResult r = AppelCphAnalyzer.analyze(
                TODAY.minusDays(3), AppelCphPartieAppelante.SALARIE,
                AppelCphModeNotification.SIGNIFICATION, AppelCphRepresentation.AVOCAT,
                false, TODAY);

        assertThat(r.checklist()).anySatisfy(i -> assertThat(i.libelle()).containsIgnoringCase("RPVA"));
        assertThat(r.checklist()).anySatisfy(i -> assertThat(i.baseJuridique()).contains("901"));
        assertThat(r.checklist()).anySatisfy(i -> assertThat(i.baseJuridique()).contains("946"));
    }

    // ── Mode notification / représentation défaut quand null ──

    @Test
    void analyze_modeEtRepresentationNull_appliqueDefauts() {
        AppelCphResult r = AppelCphAnalyzer.analyze(
                TODAY.minusDays(3), AppelCphPartieAppelante.SALARIE,
                null, null, false, TODAY);

        assertThat(r.modeNotification()).isEqualTo(AppelCphModeNotification.SIGNIFICATION);
        assertThat(r.representationConstituee()).isEqualTo(AppelCphRepresentation.AUCUNE);
    }

    // ── Validation ──

    @Test
    void analyze_dateNotificationNull_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> AppelCphAnalyzer.analyze(
                null, AppelCphPartieAppelante.SALARIE,
                AppelCphModeNotification.SIGNIFICATION, AppelCphRepresentation.AVOCAT,
                false, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNotificationJugement");
    }

    @Test
    void analyze_dateNotificationFuture_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> AppelCphAnalyzer.analyze(
                TODAY.plusDays(1), AppelCphPartieAppelante.SALARIE,
                AppelCphModeNotification.SIGNIFICATION, AppelCphRepresentation.AVOCAT,
                false, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNotificationJugement");
    }

    @Test
    void analyze_partieAppelanteNull_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> AppelCphAnalyzer.analyze(
                TODAY.minusDays(1), null,
                AppelCphModeNotification.SIGNIFICATION, AppelCphRepresentation.AVOCAT,
                false, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("partieAppelante");
    }
}
