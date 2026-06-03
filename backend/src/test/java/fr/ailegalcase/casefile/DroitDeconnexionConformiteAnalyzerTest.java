package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-53 : tests unitaires de {@link DroitDeconnexionConformiteAnalyzer}
 * (F-DT-83, outil FRANCE uniquement).
 *
 * <p>Logique déterministe (art. L.2242-17 7° CT) :
 * <ul>
 *   <li>effectif ≥ 50 + DS + tous items remplis → CONFORME ;</li>
 *   <li>effectif ≥ 50 + DS, accord/charte absent → NON_CONFORME ;</li>
 *   <li>effectif ≥ 50 + DS, charte sans avis CSE → NON_CONFORME ;</li>
 *   <li>effectif &lt; 50 → NON_REQUIS ;</li>
 *   <li>≥ 50 sans DS → NON_REQUIS ;</li>
 *   <li>comptage itemsManquants ; champ requis null / effectif ≤ 0 → IllegalArgument.</li>
 * </ul>
 */
class DroitDeconnexionConformiteAnalyzerTest {

    @Test
    void obligationApplicable_tousItemsRemplis_conforme() {
        DroitDeconnexionConformiteResult r = DroitDeconnexionConformiteAnalyzer.analyze(
                120, true, true, true, true, true);

        assertThat(r.statut()).isEqualTo(DroitDeconnexionConformiteStatut.CONFORME);
        assertThat(r.obligationDeNegocier()).isTrue();
        assertThat(r.itemsManquants()).isEqualTo(0);
        assertThat(r.baseJuridique()).contains("L.2242-17");
        assertThat(r.checklist())
                .anyMatch(i -> "OBLIGATION".equals(i.type()) && i.conforme());
    }

    @Test
    void obligationApplicable_accordCharteAbsent_nonConforme() {
        DroitDeconnexionConformiteResult r = DroitDeconnexionConformiteAnalyzer.analyze(
                120, true, false, true, true, true);

        assertThat(r.statut()).isEqualTo(DroitDeconnexionConformiteStatut.NON_CONFORME);
        assertThat(r.obligationDeNegocier()).isTrue();
        assertThat(r.itemsManquants()).isEqualTo(1);
        assertThat(r.checklist())
                .anyMatch(i -> "OBLIGATION".equals(i.type()) && !i.conforme());
    }

    @Test
    void obligationApplicable_charteSansAvisCse_nonConforme() {
        DroitDeconnexionConformiteResult r = DroitDeconnexionConformiteAnalyzer.analyze(
                120, true, true, true, true, false);

        assertThat(r.statut()).isEqualTo(DroitDeconnexionConformiteStatut.NON_CONFORME);
        assertThat(r.itemsManquants()).isEqualTo(1);
        assertThat(r.checklist())
                .anyMatch(i -> i.item().contains("CSE") && !i.conforme());
    }

    @Test
    void effectifSous50_nonRequis() {
        DroitDeconnexionConformiteResult r = DroitDeconnexionConformiteAnalyzer.analyze(
                30, true, false, false, false, false);

        assertThat(r.statut()).isEqualTo(DroitDeconnexionConformiteStatut.NON_REQUIS);
        assertThat(r.obligationDeNegocier()).isFalse();
        assertThat(r.itemsManquants()).isEqualTo(0);
        assertThat(r.checklist())
                .anyMatch(i -> "INFORMATION".equals(i.type()));
    }

    @Test
    void effectif50PlusSansDelegueSyndical_nonRequis() {
        DroitDeconnexionConformiteResult r = DroitDeconnexionConformiteAnalyzer.analyze(
                120, false, true, true, true, true);

        assertThat(r.statut()).isEqualTo(DroitDeconnexionConformiteStatut.NON_REQUIS);
        assertThat(r.obligationDeNegocier()).isFalse();
        assertThat(r.itemsManquants()).isEqualTo(0);
    }

    @Test
    void seuilExact50_avecDs_obligationApplicable() {
        DroitDeconnexionConformiteResult r = DroitDeconnexionConformiteAnalyzer.analyze(
                50, true, true, true, true, true);

        assertThat(r.obligationDeNegocier()).isTrue();
        assertThat(r.statut()).isEqualTo(DroitDeconnexionConformiteStatut.CONFORME);
    }

    @Test
    void plusieursItemsManquants_compteTous() {
        // accord/charte absent + plages absentes + sensibilisation absente +
        // avis CSE absent → 4 items manquants.
        DroitDeconnexionConformiteResult r = DroitDeconnexionConformiteAnalyzer.analyze(
                200, true, false, false, false, false);

        assertThat(r.statut()).isEqualTo(DroitDeconnexionConformiteStatut.NON_CONFORME);
        assertThat(r.itemsManquants()).isEqualTo(4);
    }

    @Test
    void champRequisNull_effectifNul_throwIllegalArgument() {
        assertThatThrownBy(() -> DroitDeconnexionConformiteAnalyzer.analyze(
                null, true, true, true, true, true))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> DroitDeconnexionConformiteAnalyzer.analyze(
                0, true, true, true, true, true))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> DroitDeconnexionConformiteAnalyzer.analyze(
                120, null, true, true, true, true))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> DroitDeconnexionConformiteAnalyzer.analyze(
                120, true, null, true, true, true))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> DroitDeconnexionConformiteAnalyzer.analyze(
                120, true, true, null, true, true))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> DroitDeconnexionConformiteAnalyzer.analyze(
                120, true, true, true, null, true))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> DroitDeconnexionConformiteAnalyzer.analyze(
                120, true, true, true, true, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
