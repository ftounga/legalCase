package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SF-220-03 : tests unitaires de {@link VpfJeuneMajeurAnalyzer}.
 * Couvre les 4 verdicts (ELIGIBLE_L42322, ELIGIBLE_SOUS_RESERVE, NON_ELIGIBLE,
 * ORIENTER_AES) et la variation de l'ancienneté requise selon l'âge d'entrée.
 */
class VpfJeuneMajeurAnalyzerTest {

    @Test
    void entreeAvant16_socleComplet_eligibleL42322() {
        VpfJeuneMajeurResult r = VpfJeuneMajeurAnalyzer.analyze(
                18, true, 14, true, 24, true, true, true, true);
        assertThat(r.eligibilite()).isEqualTo(VpfJeuneMajeurAnalyzer.ELIGIBLE_L42322);
        assertThat(r.ancienneteRequiseMois()).isEqualTo(VpfJeuneMajeurAnalyzer.ANCIENNETE_ENTREE_AVANT_16);
        assertThat(r.criteresManquants()).isEmpty();
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L.423-22"));
    }

    @Test
    void entree16_18_socleComplet_orienterAes_ancienneteSix() {
        VpfJeuneMajeurResult r = VpfJeuneMajeurAnalyzer.analyze(
                18, true, 17, true, 8, true, true, true, true);
        assertThat(r.eligibilite()).isEqualTo(VpfJeuneMajeurAnalyzer.ORIENTER_AES);
        assertThat(r.ancienneteRequiseMois()).isEqualTo(VpfJeuneMajeurAnalyzer.ANCIENNETE_ENTREE_16_18);
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L.435-3"));
    }

    @Test
    void entreeAvant16_formationNonReelle_eligibleSousReserve() {
        VpfJeuneMajeurResult r = VpfJeuneMajeurAnalyzer.analyze(
                17, true, 13, true, 24, true, false, true, true);
        assertThat(r.eligibilite()).isEqualTo(VpfJeuneMajeurAnalyzer.ELIGIBLE_SOUS_RESERVE);
        assertThat(r.criteresManquants())
                .anyMatch(c -> c.toLowerCase().contains("réel et sérieux"));
    }

    @Test
    void entreeAvant16_ancienneteInsuffisanteNonBloquante_resteVoieL42322() {
        // entrée avant 16 → ancienneté requise = 0, donc 12 mois suffit ; reste ELIGIBLE_L42322.
        VpfJeuneMajeurResult r = VpfJeuneMajeurAnalyzer.analyze(
                18, true, 12, true, 12, true, true, true, true);
        assertThat(r.eligibilite()).isEqualTo(VpfJeuneMajeurAnalyzer.ELIGIBLE_L42322);
        assertThat(r.criteresManquants()).isEmpty();
    }

    @Test
    void pasEntreMineur_socleNonReuni_nonEligible() {
        VpfJeuneMajeurResult r = VpfJeuneMajeurAnalyzer.analyze(
                19, false, null, false, null, true, true, true, true);
        assertThat(r.eligibilite()).isEqualTo(VpfJeuneMajeurAnalyzer.NON_ELIGIBLE);
        assertThat(r.criteresManquants()).anyMatch(c -> c.toLowerCase().contains("majorité"));
    }

    @Test
    void ageHorsBornes_nonEligible() {
        VpfJeuneMajeurResult r = VpfJeuneMajeurAnalyzer.analyze(
                24, true, 14, true, 24, true, true, true, true);
        assertThat(r.eligibilite()).isNotEqualTo(VpfJeuneMajeurAnalyzer.ELIGIBLE_L42322);
        assertThat(r.criteresManquants()).anyMatch(c -> c.contains("16-21"));
    }

    @Test
    void entreMineurEtAse_maisSansScolarisation_orienterAes() {
        // socle non réuni (pas de scolarisation) mais entré mineur + ASE → renvoi L.435-3.
        VpfJeuneMajeurResult r = VpfJeuneMajeurAnalyzer.analyze(
                18, true, 14, true, 24, false, false, false, false);
        assertThat(r.eligibilite()).isEqualTo(VpfJeuneMajeurAnalyzer.ORIENTER_AES);
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L.435-3"));
        assertThat(r.messages()).anyMatch(m -> m.contains("L.435-3"));
    }

    @Test
    void entree16_18_ancienneteInsuffisante_critereManquant() {
        VpfJeuneMajeurResult r = VpfJeuneMajeurAnalyzer.analyze(
                18, true, 17, true, 2, true, true, true, true);
        assertThat(r.ancienneteRequiseMois()).isEqualTo(VpfJeuneMajeurAnalyzer.ANCIENNETE_ENTREE_16_18);
        assertThat(r.criteresManquants())
                .anyMatch(c -> c.toLowerCase().contains("ancienneté"));
    }
}
