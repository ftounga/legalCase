package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-47 : tests unitaires de {@link CongeProcheAidantAnalyzer}
 * (F-DT-79, outil FRANCE uniquement).
 *
 * <p>Logique déterministe (art. L.3142-16 à L.3142-27 CT, loi n° 2020-220) :
 * <ul>
 *   <li>éligibilité = personne aidée résidant en France/EEE (L.3142-16) ;</li>
 *   <li>durée maximale = 12 mois sur la carrière (L.3142-19), retenue =
 *       min(souhaitée, 12) ;</li>
 *   <li>estimation AJPA si demandée = 64,54 €/jour × min(jours, 66) ;</li>
 *   <li>champ requis null / dureeSouhaiteeMois ≤ 0 → IllegalArgument.</li>
 * </ul>
 */
class CongeProcheAidantAnalyzerTest {

    @Test
    void eligible_resideFrance_ascendant_dureeRetenue3() {
        CongeProcheAidantResult r = CongeProcheAidantAnalyzer.analyze(
                CongeProcheAidantLien.ASCENDANT, true, 3, false);

        assertThat(r.statut()).isEqualTo(CongeProcheAidantStatut.ELIGIBLE);
        assertThat(r.dureeMaxMois()).isEqualTo(12);
        assertThat(r.dureeRetenueMois()).isEqualTo(3);
        assertThat(r.estimationAjpa()).isNull();
        assertThat(r.protectionEmploi()).isTrue();
        assertThat(r.nonImputableCongesPayes()).isTrue();
        assertThat(r.baseJuridique()).contains("L.3142-16");
    }

    @Test
    void nonEligible_personneAideeHorsFrance() {
        CongeProcheAidantResult r = CongeProcheAidantAnalyzer.analyze(
                CongeProcheAidantLien.ASCENDANT, false, 3, true);

        assertThat(r.statut()).isEqualTo(CongeProcheAidantStatut.NON_ELIGIBLE);
        assertThat(r.dureeRetenueMois()).isNull();
        assertThat(r.estimationAjpa()).isNull();
        assertThat(r.notes()).anyMatch(n -> n.contains("L.3142-16"));
    }

    @Test
    void dureeMax_toujours12_etPlafonnement() {
        CongeProcheAidantResult r = CongeProcheAidantAnalyzer.analyze(
                CongeProcheAidantLien.CONJOINT, true, 18, false);

        assertThat(r.statut()).isEqualTo(CongeProcheAidantStatut.ELIGIBLE);
        assertThat(r.dureeMaxMois()).isEqualTo(12);
        assertThat(r.dureeRetenueMois()).isEqualTo(12);
        assertThat(r.notes()).anyMatch(n -> n.contains("plafonn"));
    }

    @Test
    void estimationAjpa_demandee_plafonnee66jours() {
        // 3 mois × 22 jours = 66 jours indemnisés × 64,54 € = 4259,64 €
        CongeProcheAidantResult r = CongeProcheAidantAnalyzer.analyze(
                CongeProcheAidantLien.DESCENDANT, true, 3, true);

        assertThat(r.statut()).isEqualTo(CongeProcheAidantStatut.ELIGIBLE);
        assertThat(r.ajpaDemandee()).isTrue();
        assertThat(r.ajpaJournaliere()).isEqualByComparingTo(new BigDecimal("64.54"));
        assertThat(r.estimationAjpa()).isEqualByComparingTo(new BigDecimal("4259.64"));
    }

    @Test
    void estimationAjpa_plafond66_quandDureeLongue() {
        // 12 mois × 22 = 264 jours, plafonné à 66 → identique au cas 3 mois.
        CongeProcheAidantResult r = CongeProcheAidantAnalyzer.analyze(
                CongeProcheAidantLien.CONJOINT, true, 12, true);

        assertThat(r.estimationAjpa()).isEqualByComparingTo(new BigDecimal("4259.64"));
        assertThat(r.notes()).anyMatch(n -> n.contains("plafond de 66 jours")
                || n.contains("66 jours"));
    }

    @Test
    void pasDAjpa_quandNonDemandee() {
        CongeProcheAidantResult r = CongeProcheAidantAnalyzer.analyze(
                CongeProcheAidantLien.COLLATERAL, true, 2, false);

        assertThat(r.ajpaDemandee()).isFalse();
        assertThat(r.ajpaJournaliere()).isNull();
        assertThat(r.estimationAjpa()).isNull();
        assertThat(r.notes()).anyMatch(n -> n.contains("AJPA non demandée"));
    }

    @Test
    void lienSansResidenceCommune_resideFrance_eligible() {
        CongeProcheAidantResult r = CongeProcheAidantAnalyzer.analyze(
                CongeProcheAidantLien.SANS_LIEN_RESIDENCE_COMMUNE, true, 1, false);

        assertThat(r.statut()).isEqualTo(CongeProcheAidantStatut.ELIGIBLE);
        assertThat(r.lienPersonneAidee())
                .isEqualTo(CongeProcheAidantLien.SANS_LIEN_RESIDENCE_COMMUNE);
    }

    @Test
    void protectionNonImputableCongesPayes_toujoursPresente() {
        CongeProcheAidantResult r = CongeProcheAidantAnalyzer.analyze(
                CongeProcheAidantLien.ASCENDANT, true, 3, false);

        assertThat(r.protectionEmploi()).isTrue();
        assertThat(r.nonImputableCongesPayes()).isTrue();
        assertThat(r.notes()).anyMatch(n -> n.contains("congés payés"));
    }

    @Test
    void lienNull_leveIllegalArgument() {
        assertThatThrownBy(() -> CongeProcheAidantAnalyzer.analyze(
                null, true, 3, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resideFranceNull_leveIllegalArgument() {
        assertThatThrownBy(() -> CongeProcheAidantAnalyzer.analyze(
                CongeProcheAidantLien.ASCENDANT, null, 3, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dureeNull_leveIllegalArgument() {
        assertThatThrownBy(() -> CongeProcheAidantAnalyzer.analyze(
                CongeProcheAidantLien.ASCENDANT, true, null, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dureeZeroOuNegative_leveIllegalArgument() {
        assertThatThrownBy(() -> CongeProcheAidantAnalyzer.analyze(
                CongeProcheAidantLien.ASCENDANT, true, 0, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CongeProcheAidantAnalyzer.analyze(
                CongeProcheAidantLien.ASCENDANT, true, -2, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
