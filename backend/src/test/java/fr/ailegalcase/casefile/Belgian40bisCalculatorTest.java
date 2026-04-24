package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Belgian40bisCalculatorTest {

    @Test
    void compute_toutesConditionsOk_verdictElevee() {
        Belgian40bisResult r = Belgian40bisCalculator.compute(
                "CONJOINT",
                true,
                "TRAVAILLEUR",
                true,
                true,
                true,
                false,
                LocalDate.of(2026, 4, 1));

        assertThat(r.lienValide()).isTrue();
        assertThat(r.regroupantValide()).isTrue();
        assertThat(r.ressourcesOk()).isTrue();
        assertThat(r.assuranceOk()).isTrue();
        assertThat(r.logementOk()).isTrue();
        assertThat(r.pasMenace()).isTrue();
        assertThat(r.scoreGlobal()).isEqualTo(100);
        assertThat(r.verdictProbabilite()).isEqualTo("ELEVEE");
        assertThat(r.criteresNonRemplis()).isEmpty();
        assertThat(r.dateExpirationInstruction()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(r.baseJuridique()).contains("40bis").contains("15/12/1980");
        assertThat(r.messages()).anyMatch(m -> m.contains("Carte F"));
        assertThat(r.messages()).anyMatch(m -> m.contains("CCE"));
    }

    @Test
    void compute_categorieAutre_pasDeBonusMaisScoreValide() {
        Belgian40bisResult r = Belgian40bisCalculator.compute(
                "CONJOINT",
                true,
                "AUTRE",
                true,
                true,
                true,
                false,
                null);

        // 6 conditions OK * 15 = 90, pas de bonus car AUTRE
        assertThat(r.scoreGlobal()).isEqualTo(90);
        assertThat(r.verdictProbabilite()).isEqualTo("ELEVEE");
        assertThat(r.dateExpirationInstruction()).isNull();
    }

    @Test
    void compute_descendantMineur_accepte() {
        Belgian40bisResult r = Belgian40bisCalculator.compute(
                "DESCENDANT_MINEUR",
                true,
                "TRAVAILLEUR",
                true,
                true,
                true,
                false,
                null);

        assertThat(r.lienValide()).isTrue();
        assertThat(r.verdictProbabilite()).isEqualTo("ELEVEE");
    }

    @Test
    void compute_ascendantCharge_accepte() {
        Belgian40bisResult r = Belgian40bisCalculator.compute(
                "ASCENDANT_CHARGE",
                true,
                "INACTIF_AVEC_RESSOURCES",
                true,
                true,
                true,
                false,
                null);

        assertThat(r.lienValide()).isTrue();
        assertThat(r.regroupantValide()).isTrue();
        assertThat(r.verdictProbabilite()).isEqualTo("ELEVEE");
    }

    @Test
    void compute_partenaireEnregistre_accepte() {
        Belgian40bisResult r = Belgian40bisCalculator.compute(
                "PARTENAIRE_ENREGISTRE",
                true,
                "ETUDIANT",
                true,
                true,
                true,
                false,
                null);

        assertThat(r.lienValide()).isTrue();
        assertThat(r.verdictProbabilite()).isEqualTo("ELEVEE");
    }

    @Test
    void compute_regroupantNonCitoyenUe_regroupantInvalide() {
        Belgian40bisResult r = Belgian40bisCalculator.compute(
                "CONJOINT",
                false,                  // pas citoyen UE
                "TRAVAILLEUR",
                true,
                true,
                true,
                false,
                null);

        assertThat(r.regroupantValide()).isFalse();
        // 15+0+15+15+15+15 = 75 → ELEVEE (seuil exact)
        assertThat(r.scoreGlobal()).isEqualTo(75);
        assertThat(r.verdictProbabilite()).isEqualTo("ELEVEE");
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("citoyen UE"));
    }

    @Test
    void compute_menaceOrdrePublic_pasMenaceKo() {
        Belgian40bisResult r = Belgian40bisCalculator.compute(
                "CONJOINT",
                true,
                "TRAVAILLEUR",
                true,
                true,
                true,
                true,
                null);

        assertThat(r.pasMenace()).isFalse();
        // 15+15+15+15+15+0 + bonus 10 = 85 → ELEVEE
        assertThat(r.scoreGlobal()).isEqualTo(85);
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("ordre public"));
    }

    @Test
    void compute_sansRessources_scoreMoyenne() {
        Belgian40bisResult r = Belgian40bisCalculator.compute(
                "CONJOINT",
                true,
                "TRAVAILLEUR",
                false,
                false,
                true,
                false,
                null);

        // 15+15+0+0+15+15 + bonus 10 = 70 → MOYENNE
        assertThat(r.scoreGlobal()).isEqualTo(70);
        assertThat(r.verdictProbabilite()).isEqualTo("MOYENNE");
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.toLowerCase().contains("ressources"));
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.toLowerCase().contains("assurance"));
    }

    @Test
    void compute_plusieursCriteresManquants_verdictFaible() {
        Belgian40bisResult r = Belgian40bisCalculator.compute(
                "CONJOINT",
                true,
                "TRAVAILLEUR",
                false,
                false,
                false,
                true,
                null);

        // 15+15+0+0+0+0 + bonus 10 = 40 → FAIBLE
        assertThat(r.scoreGlobal()).isEqualTo(40);
        assertThat(r.verdictProbabilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    void compute_scorePlafonneA100() {
        Belgian40bisResult r = Belgian40bisCalculator.compute(
                "CONJOINT",
                true,
                "TRAVAILLEUR",
                true, true, true, false,
                null);

        // 6*15 = 90 + bonus 10 = 100, pas de dépassement
        assertThat(r.scoreGlobal()).isLessThanOrEqualTo(100);
    }

    @Test
    void compute_dateDepotSetsExpirationAuMoisSix() {
        Belgian40bisResult r = Belgian40bisCalculator.compute(
                "CONJOINT",
                true,
                "TRAVAILLEUR",
                true, true, true, false,
                LocalDate.of(2026, 1, 15));

        assertThat(r.dateExpirationInstruction()).isEqualTo(LocalDate.of(2026, 7, 15));
    }

    @Test
    void compute_lienInvalide_throws() {
        assertThatThrownBy(() -> Belgian40bisCalculator.compute(
                "CONCUBIN_NON_ENREGISTRE",
                true, "TRAVAILLEUR", true, true, true, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lienFamilial");
    }

    @Test
    void compute_categorieInvalide_throws() {
        assertThatThrownBy(() -> Belgian40bisCalculator.compute(
                "CONJOINT",
                true, "FREELANCE_NON_RECONNU", true, true, true, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("regroupantActiviteCategorie");
    }

    @Test
    void compute_lienNull_throws() {
        assertThatThrownBy(() -> Belgian40bisCalculator.compute(
                null,
                true, "TRAVAILLEUR", true, true, true, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lienFamilial");
    }

    @Test
    void compute_dateDepotFutur_throws() {
        LocalDate futur = LocalDate.now().plusYears(1);
        assertThatThrownBy(() -> Belgian40bisCalculator.compute(
                "CONJOINT",
                true, "TRAVAILLEUR", true, true, true, false, futur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDepotDemande");
    }

    @Test
    void compute_formuleContientDateExpiration() {
        Belgian40bisResult r = Belgian40bisCalculator.compute(
                "CONJOINT",
                true, "TRAVAILLEUR", true, true, true, false,
                LocalDate.of(2026, 4, 1));

        assertThat(r.formule()).contains("2026-10-01");
        assertThat(r.formule()).contains("40bis");
    }
}
