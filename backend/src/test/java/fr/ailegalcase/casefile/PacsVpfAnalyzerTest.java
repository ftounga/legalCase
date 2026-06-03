package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SF-220-04 : tests unitaires de {@link PacsVpfAnalyzer}.
 * Couvre les 4 verdicts (FAISCEAU_FAVORABLE, FAISCEAU_INSUFFISANT, A_CONSOLIDER,
 * NON_ELIGIBLE) + partenaire français vs étranger régulier.
 */
class PacsVpfAnalyzerTest {

    @Test
    void pacsAncienIntensiteFortePartenaireFrancais_faisceauFavorable() {
        PacsVpfResult r = PacsVpfAnalyzer.analyze(
                true, PacsVpfAnalyzer.PARTENAIRE_FRANCAIS, 24,
                PacsVpfAnalyzer.INTENSITE_FORTE, true);
        assertThat(r.eligibilite()).isEqualTo(PacsVpfAnalyzer.FAISCEAU_FAVORABLE);
        assertThat(r.elementsFavorables()).anyMatch(e -> e.toLowerCase().contains("française"));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L.423-23"));
    }

    @Test
    void pacsAncienIntensiteFortePartenaireEtrangerRegulier_faisceauFavorable() {
        PacsVpfResult r = PacsVpfAnalyzer.analyze(
                true, PacsVpfAnalyzer.PARTENAIRE_ETRANGER_REGULIER, 18,
                PacsVpfAnalyzer.INTENSITE_FORTE, false);
        assertThat(r.eligibilite()).isEqualTo(PacsVpfAnalyzer.FAISCEAU_FAVORABLE);
        assertThat(r.elementsFavorables()).anyMatch(e -> e.toLowerCase().contains("régulier"));
    }

    @Test
    void pacsRecent_faisceauInsuffisant() {
        PacsVpfResult r = PacsVpfAnalyzer.analyze(
                true, PacsVpfAnalyzer.PARTENAIRE_FRANCAIS, 3,
                PacsVpfAnalyzer.INTENSITE_FORTE, false);
        assertThat(r.eligibilite()).isEqualTo(PacsVpfAnalyzer.FAISCEAU_INSUFFISANT);
        assertThat(r.elementsManquants()).anyMatch(c -> c.toLowerCase().contains("ancienneté"));
    }

    @Test
    void intensiteFaible_faisceauInsuffisant() {
        PacsVpfResult r = PacsVpfAnalyzer.analyze(
                true, PacsVpfAnalyzer.PARTENAIRE_FRANCAIS, 24,
                PacsVpfAnalyzer.INTENSITE_FAIBLE, false);
        assertThat(r.eligibilite()).isEqualTo(PacsVpfAnalyzer.FAISCEAU_INSUFFISANT);
    }

    @Test
    void ancienneteSuffisanteIntensiteMoyenne_aConsolider() {
        // ancienneté OK, partenaire OK, mais intensité moyenne (ni forte ni faible)
        // → socle réuni mais à consolider, pas FAISCEAU_FAVORABLE.
        PacsVpfResult r = PacsVpfAnalyzer.analyze(
                true, PacsVpfAnalyzer.PARTENAIRE_FRANCAIS, 24,
                PacsVpfAnalyzer.INTENSITE_MOYENNE, false);
        assertThat(r.eligibilite()).isEqualTo(PacsVpfAnalyzer.A_CONSOLIDER);
        assertThat(r.elementsManquants()).anyMatch(c -> c.toLowerCase().contains("intensité"));
    }

    @Test
    void ancienneteInconnueIntensiteForte_aConsolider() {
        // durée non factualisée (null) + intensité forte → à consolider (ancienneté à factualiser).
        PacsVpfResult r = PacsVpfAnalyzer.analyze(
                true, PacsVpfAnalyzer.PARTENAIRE_FRANCAIS, null,
                PacsVpfAnalyzer.INTENSITE_FORTE, false);
        assertThat(r.eligibilite()).isEqualTo(PacsVpfAnalyzer.A_CONSOLIDER);
        assertThat(r.elementsManquants()).anyMatch(c -> c.toLowerCase().contains("non établie"));
    }

    @Test
    void pasDePacs_nonEligible() {
        PacsVpfResult r = PacsVpfAnalyzer.analyze(
                false, PacsVpfAnalyzer.PARTENAIRE_FRANCAIS, 24,
                PacsVpfAnalyzer.INTENSITE_FORTE, false);
        assertThat(r.eligibilite()).isEqualTo(PacsVpfAnalyzer.NON_ELIGIBLE);
        assertThat(r.elementsManquants()).anyMatch(c -> c.toLowerCase().contains("aucun pacs"));
    }

    @Test
    void pacsToujoursRappelePasDeDroitAutomatique() {
        // Anti-doublon F-IM-21 : le message rappelant l'absence de droit automatique est systématique.
        PacsVpfResult r = PacsVpfAnalyzer.analyze(
                true, PacsVpfAnalyzer.PARTENAIRE_FRANCAIS, 24,
                PacsVpfAnalyzer.INTENSITE_FORTE, false);
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("droit automatique"));
    }
}
