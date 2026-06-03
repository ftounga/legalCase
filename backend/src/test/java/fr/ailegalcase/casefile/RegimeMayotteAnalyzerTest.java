package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-220-02 : tests unitaires de {@link RegimeMayotteAnalyzer}.
 * Couvre portée Mayotte, blocage déplacement, droit commun, et chaque type de titre.
 */
class RegimeMayotteAnalyzerTest {

    @Test
    void titreMayotte_porteeMayotteUniquement_avecObligations() {
        RegimeMayotteResult r = RegimeMayotteAnalyzer.analyze(true, "VPF", false);
        assertThat(r.porteeTerritoriale()).isEqualTo(RegimeMayotteAnalyzer.PORTEE_MAYOTTE_UNIQUEMENT);
        assertThat(r.sousStatutDeplacement()).isEqualTo(RegimeMayotteAnalyzer.DEPLACEMENT_LIBRE);
        assertThat(r.obligationsSpecifiques()).isNotEmpty();
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("2014-464"));
        assertThat(r.demarchesDeplacementMetropole()).isEmpty();
    }

    @Test
    void titreMayotte_avecProjetMetropole_blocageDeplacement_avecDemarches() {
        RegimeMayotteResult r = RegimeMayotteAnalyzer.analyze(true, "SALARIE", true);
        assertThat(r.porteeTerritoriale()).isEqualTo(RegimeMayotteAnalyzer.PORTEE_MAYOTTE_UNIQUEMENT);
        assertThat(r.sousStatutDeplacement()).isEqualTo(RegimeMayotteAnalyzer.DEPLACEMENT_BLOCAGE);
        assertThat(r.demarchesDeplacementMetropole()).isNotEmpty();
        assertThat(r.obligationsSpecifiques()).isNotEmpty();
    }

    @Test
    void titreHorsMayotte_droitCommun_sansBlocage() {
        RegimeMayotteResult r = RegimeMayotteAnalyzer.analyze(false, "RESIDENT", true);
        assertThat(r.porteeTerritoriale()).isEqualTo(RegimeMayotteAnalyzer.PORTEE_DROIT_COMMUN);
        assertThat(r.sousStatutDeplacement()).isEqualTo(RegimeMayotteAnalyzer.DEPLACEMENT_LIBRE);
        // anti-gadget : pas d'obligation dérogatoire ni de démarche inventée hors Mayotte
        assertThat(r.obligationsSpecifiques()).isEmpty();
        assertThat(r.demarchesDeplacementMetropole()).isEmpty();
        assertThat(r.messages()).anyMatch(m -> m.contains("F-IM-05"));
    }

    @Test
    void titreMayotte_etudiant_obligationSpecifiqueEtudiant() {
        RegimeMayotteResult r = RegimeMayotteAnalyzer.analyze(true, "ETUDIANT", false);
        assertThat(r.obligationsSpecifiques()).anyMatch(o -> o.toLowerCase().contains("étudiant"));
    }

    @Test
    void titreMayotte_autre_obligationGenerique() {
        RegimeMayotteResult r = RegimeMayotteAnalyzer.analyze(true, "AUTRE", false);
        assertThat(r.porteeTerritoriale()).isEqualTo(RegimeMayotteAnalyzer.PORTEE_MAYOTTE_UNIQUEMENT);
        assertThat(r.obligationsSpecifiques()).isNotEmpty();
    }

    @Test
    void typeTitreInconnu_leveIllegalArgument() {
        assertThatThrownBy(() -> RegimeMayotteAnalyzer.analyze(true, "PASSEPORT", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeTitre");
    }

    @Test
    void typeTitreNull_leveIllegalArgument() {
        assertThatThrownBy(() -> RegimeMayotteAnalyzer.analyze(true, null, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
