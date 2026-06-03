package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-220-01 : tests unitaires de {@link RegimeTunisienAnalyzer}.
 * Couvre chaque catégorie, le renvoi au droit commun et le verdict MIXTE.
 */
class RegimeTunisienAnalyzerTest {

    @Test
    void etudiant_estDerogatoire_avecParticularites() {
        RegimeTunisienResult r = RegimeTunisienAnalyzer.analyze("ETUDIANT", 12, false, false);
        assertThat(r.regime()).isEqualTo(RegimeTunisienAnalyzer.REGIME_ACCORD_1988_DEROGATOIRE);
        assertThat(r.particularitesApplicables()).isNotEmpty();
        assertThat(r.renvoiDroitCommun()).isFalse();
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("17/03/1988"));
    }

    @Test
    void commercant_estDerogatoire_avecParticularites() {
        RegimeTunisienResult r = RegimeTunisienAnalyzer.analyze("COMMERCANT", null, false, false);
        assertThat(r.regime()).isEqualTo(RegimeTunisienAnalyzer.REGIME_ACCORD_1988_DEROGATOIRE);
        assertThat(r.particularitesApplicables()).isNotEmpty();
        assertThat(r.renvoiDroitCommun()).isFalse();
    }

    @Test
    void salarie_estMixte_avecParticularites() {
        RegimeTunisienResult r = RegimeTunisienAnalyzer.analyze("SALARIE", 24, true, true);
        assertThat(r.regime()).isEqualTo(RegimeTunisienAnalyzer.REGIME_MIXTE);
        assertThat(r.particularitesApplicables()).isNotEmpty();
        assertThat(r.renvoiDroitCommun()).isFalse();
        // contexte non bloquant reflété dans les messages
        assertThat(r.messages()).isNotEmpty();
    }

    @Test
    void familial_renvoyeAuDroitCommunCeseda_sansFauxRegimeFerme() {
        RegimeTunisienResult r = RegimeTunisienAnalyzer.analyze("FAMILIAL", null, false, false);
        assertThat(r.regime()).isEqualTo(RegimeTunisienAnalyzer.REGIME_DROIT_COMMUN_CESEDA);
        assertThat(r.renvoiDroitCommun()).isTrue();
        assertThat(r.messages()).anyMatch(m -> m.contains("F-IM-05"));
        // anti-gadget : pas de particularité dérogatoire inventée pour le familial
        assertThat(r.particularitesApplicables()).isEmpty();
    }

    @Test
    void autre_renvoyeAuDroitCommunCeseda() {
        RegimeTunisienResult r = RegimeTunisienAnalyzer.analyze("AUTRE", null, false, false);
        assertThat(r.regime()).isEqualTo(RegimeTunisienAnalyzer.REGIME_DROIT_COMMUN_CESEDA);
        assertThat(r.renvoiDroitCommun()).isTrue();
        assertThat(r.particularitesApplicables()).isEmpty();
    }

    @Test
    void categorieInconnue_leveIllegalArgument() {
        assertThatThrownBy(() -> RegimeTunisienAnalyzer.analyze("AVOCAT", null, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("categorie");
    }

    @Test
    void categorieNull_leveIllegalArgument() {
        assertThatThrownBy(() -> RegimeTunisienAnalyzer.analyze(null, null, false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dureeNegative_leveIllegalArgument() {
        assertThatThrownBy(() -> RegimeTunisienAnalyzer.analyze("ETUDIANT", -3, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeSejourEnvisageeMois");
    }
}
