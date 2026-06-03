package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SF-220-06 : tests unitaires de {@link SignalementSisAnalyzer}.
 * Couvre les verdicts actionPossible selon l'État signalant, le conflit titre
 * valide / non-admission (consultation entre États), la radiation par l'État
 * signalant, le droit d'accès quand l'État est inconnu, et la distinction avec
 * l'IRTF (F-IM-20).
 */
class SignalementSisAnalyzerTest {

    @Test
    void signalantFrance_radiationAutoriteFr() {
        SignalementSisResult r = SignalementSisAnalyzer.analyze(
                true,
                SignalementSisAnalyzer.ETAT_FRANCE,
                SignalementSisAnalyzer.MOTIF_IRTF,
                false);
        assertThat(r.actionPossible()).isEqualTo(SignalementSisAnalyzer.RADIATION_AUTORITE_FR);
        assertThat(r.autoriteCompetente()).containsIgnoringCase("français");
        assertThat(r.demarches()).isNotEmpty();
    }

    @Test
    void signalantAutreEtatTitreInvalide_radiationEtatSignalant() {
        SignalementSisResult r = SignalementSisAnalyzer.analyze(
                true,
                SignalementSisAnalyzer.ETAT_AUTRE_ETAT_MEMBRE,
                SignalementSisAnalyzer.MOTIF_MESURE_ELOIGNEMENT_ETRANGERE,
                false);
        assertThat(r.actionPossible()).isEqualTo(SignalementSisAnalyzer.RADIATION_ETAT_SIGNALANT);
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("état signalant"));
        // Orientation vers le droit d'accès / rectification (pas de radiation FR directe).
        assertThat(r.demarches()).anyMatch(d -> d.toLowerCase().contains("rectification"));
    }

    @Test
    void signalantAutreEtatTitreValide_consultationEntreEtats() {
        SignalementSisResult r = SignalementSisAnalyzer.analyze(
                true,
                SignalementSisAnalyzer.ETAT_AUTRE_ETAT_MEMBRE,
                SignalementSisAnalyzer.MOTIF_MENACE_ORDRE_PUBLIC,
                true);
        assertThat(r.actionPossible()).isEqualTo(SignalementSisAnalyzer.CONSULTATION_ENTRE_ETATS);
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("consultation"));
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("conflit"));
    }

    @Test
    void etatInconnu_droitAccesRectification() {
        SignalementSisResult r = SignalementSisAnalyzer.analyze(
                false,
                SignalementSisAnalyzer.ETAT_INCONNU,
                SignalementSisAnalyzer.MOTIF_AUTRE,
                null);
        assertThat(r.actionPossible()).isEqualTo(SignalementSisAnalyzer.DROIT_ACCES_RECTIFICATION);
        assertThat(r.demarches()).anyMatch(d -> d.toLowerCase().contains("droit d'accès"));
    }

    @Test
    void etatNull_droitAccesRectification() {
        SignalementSisResult r = SignalementSisAnalyzer.analyze(
                null, null, null, null);
        assertThat(r.actionPossible()).isEqualTo(SignalementSisAnalyzer.DROIT_ACCES_RECTIFICATION);
    }

    @Test
    void distinctionIrtfSystematique() {
        SignalementSisResult r = SignalementSisAnalyzer.analyze(
                true,
                SignalementSisAnalyzer.ETAT_FRANCE,
                SignalementSisAnalyzer.MOTIF_IRTF,
                false);
        assertThat(r.messages()).anyMatch(m -> m.contains("IRTF") && m.toLowerCase().contains("f-im-20"));
    }

    @Test
    void basesJuridiquesContiennentReglEtCeseda() {
        SignalementSisResult r = SignalementSisAnalyzer.analyze(
                true,
                SignalementSisAnalyzer.ETAT_FRANCE,
                SignalementSisAnalyzer.MOTIF_IRTF,
                false);
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("2018/1860"));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L.312-3"));
    }

    @Test
    void titreValideEtSignalantFrance_messageCoherence() {
        SignalementSisResult r = SignalementSisAnalyzer.analyze(
                true,
                SignalementSisAnalyzer.ETAT_FRANCE,
                SignalementSisAnalyzer.MOTIF_IRTF,
                true);
        assertThat(r.actionPossible()).isEqualTo(SignalementSisAnalyzer.RADIATION_AUTORITE_FR);
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("cohérence"));
    }
}
