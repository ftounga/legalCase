package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SF-220-05 : tests unitaires de {@link DecheanceNationaliteAnalyzer}.
 * Couvre les 4 verdicts (CONDITIONS_REUNIES, MESURE_CONTESTABLE,
 * MESURE_IRREGULIERE, INDETERMINE non atteignable ici) + apatridie + hors délai
 * + calcul du délai de recours + variation par motif.
 */
class DecheanceNationaliteAnalyzerTest {

    @Test
    void binationalDelaiRespecteMotifTerrorisme_conditionsReunies() {
        DecheanceNationaliteResult r = DecheanceNationaliteAnalyzer.analyze(
                DecheanceNationaliteAnalyzer.MOTIF_TERRORISME,
                true,
                LocalDate.of(2010, 1, 1),
                LocalDate.of(2015, 6, 1),
                false,
                null);
        assertThat(r.validite()).isEqualTo(DecheanceNationaliteAnalyzer.CONDITIONS_REUNIES);
        assertThat(r.conditionsManquantes()).isEmpty();
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("art. 25"));
    }

    @Test
    void nonBinational_mesureIrreguliereApatridie() {
        DecheanceNationaliteResult r = DecheanceNationaliteAnalyzer.analyze(
                DecheanceNationaliteAnalyzer.MOTIF_TERRORISME,
                false,
                LocalDate.of(2010, 1, 1),
                LocalDate.of(2012, 1, 1),
                false,
                null);
        assertThat(r.validite()).isEqualTo(DecheanceNationaliteAnalyzer.MESURE_IRREGULIERE);
        assertThat(r.conditionsManquantes()).anyMatch(c -> c.toLowerCase().contains("apatride"));
    }

    @Test
    void faitsHorsDelai_mesureIrreguliere() {
        // Faits commis plus de 15 ans après l'acquisition.
        DecheanceNationaliteResult r = DecheanceNationaliteAnalyzer.analyze(
                DecheanceNationaliteAnalyzer.MOTIF_ATTEINTE_INTERETS_NATION,
                true,
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2020, 6, 1),
                false,
                null);
        assertThat(r.validite()).isEqualTo(DecheanceNationaliteAnalyzer.MESURE_IRREGULIERE);
        assertThat(r.conditionsManquantes()).anyMatch(c -> c.toLowerCase().contains("délai"));
    }

    @Test
    void faitsAnterieursAcquisition_mesureIrreguliere() {
        DecheanceNationaliteResult r = DecheanceNationaliteAnalyzer.analyze(
                DecheanceNationaliteAnalyzer.MOTIF_FRAUDE_ACQUISITION,
                true,
                LocalDate.of(2015, 1, 1),
                LocalDate.of(2010, 1, 1),
                false,
                null);
        assertThat(r.validite()).isEqualTo(DecheanceNationaliteAnalyzer.MESURE_IRREGULIERE);
        assertThat(r.conditionsManquantes()).anyMatch(c -> c.toLowerCase().contains("antérieurs"));
    }

    @Test
    void mesurePrononcee_calculDelaiRecoursEtVoieConseilEtat() {
        DecheanceNationaliteResult r = DecheanceNationaliteAnalyzer.analyze(
                DecheanceNationaliteAnalyzer.MOTIF_TERRORISME,
                true,
                LocalDate.of(2010, 1, 1),
                LocalDate.of(2015, 1, 1),
                true,
                LocalDate.now().minusDays(10));
        assertThat(r.delaiRecoursJours()).isEqualTo(DecheanceNationaliteAnalyzer.DELAI_RECOURS_CE_JOURS);
        assertThat(r.voiesRecours()).anyMatch(v -> v.toLowerCase().contains("conseil d'état"));
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("en cours"));
    }

    @Test
    void mesurePrononceeDelaiExpire_messageDelaiExpire() {
        DecheanceNationaliteResult r = DecheanceNationaliteAnalyzer.analyze(
                DecheanceNationaliteAnalyzer.MOTIF_TERRORISME,
                true,
                LocalDate.of(2010, 1, 1),
                LocalDate.of(2015, 1, 1),
                true,
                LocalDate.now().minusDays(120));
        assertThat(r.delaiRecoursJours()).isEqualTo(DecheanceNationaliteAnalyzer.DELAI_RECOURS_CE_JOURS);
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("expiré"));
    }

    @Test
    void binationaliteInconnue_mesureContestable() {
        DecheanceNationaliteResult r = DecheanceNationaliteAnalyzer.analyze(
                DecheanceNationaliteAnalyzer.MOTIF_TERRORISME,
                null,
                LocalDate.of(2010, 1, 1),
                LocalDate.of(2015, 1, 1),
                false,
                null);
        assertThat(r.validite()).isEqualTo(DecheanceNationaliteAnalyzer.MESURE_CONTESTABLE);
        assertThat(r.conditionsManquantes()).anyMatch(c -> c.toLowerCase().contains("binationalité"));
    }

    @Test
    void motifAutre_mesureContestable() {
        DecheanceNationaliteResult r = DecheanceNationaliteAnalyzer.analyze(
                DecheanceNationaliteAnalyzer.MOTIF_AUTRE,
                true,
                LocalDate.of(2010, 1, 1),
                LocalDate.of(2015, 1, 1),
                false,
                null);
        assertThat(r.validite()).isEqualTo(DecheanceNationaliteAnalyzer.MESURE_CONTESTABLE);
        assertThat(r.conditionsManquantes()).anyMatch(c -> c.toLowerCase().contains("motif"));
    }

    @Test
    void rappelProportionnaliteSystematique() {
        DecheanceNationaliteResult r = DecheanceNationaliteAnalyzer.analyze(
                DecheanceNationaliteAnalyzer.MOTIF_TERRORISME,
                true,
                LocalDate.of(2010, 1, 1),
                LocalDate.of(2015, 1, 1),
                false,
                null);
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("proportionnalité"));
    }
}
