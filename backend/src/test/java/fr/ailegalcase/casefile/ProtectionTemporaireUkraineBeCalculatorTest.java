package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-215-19 — UT du calculator "Protection temporaire Ukraine BE" (F-IM-34).
 *
 * <p>Directive 2001/55/CE activée par la décision UE 2022/382 du 04/03/2022 ;
 * transposition belge : Loi du 15/12/1980 art. 57/29+ (droit au travail immédiat
 * sans single permit). Outil BELGIQUE UNIQUEMENT (droit des étrangers).
 *
 * <p>Les bornes de durée restante (renouvellement imminent) sont rendues
 * déterministes en injectant {@code today} ET {@code dateFin} contrôlés — aucune
 * assertion ne dépend de la date réelle.
 */
class ProtectionTemporaireUkraineBeCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);
    private static final LocalDate DATE_FIN = LocalDate.of(2027, 3, 4);

    @Test
    void compute_nationaliteUaAvecResidenceAvant_eligible() {
        ProtectionTemporaireUkraineBeResult r = ProtectionTemporaireUkraineBeCalculator.compute(
                LocalDate.of(2022, 3, 1), true, true, false, false,
                ProtectionTemporaireUkraineBeTitreSejourEnum.TITRE_A, TODAY, DATE_FIN);

        assertThat(r.eligible()).isTrue();
        assertThat(r.nationaliteUkrainienne()).isTrue();
        assertThat(r.residenceUkraineAvant24Fev2022()).isTrue();
        assertThat(r.droitsTravail()).contains("57/29");
        assertThat(r.dateFinProtection()).isEqualTo(DATE_FIN);
        assertThat(r.recommandation()).contains("éligible");
    }

    @Test
    void compute_nationaliteUaSansResidenceAvant_nonEligible() {
        ProtectionTemporaireUkraineBeResult r = ProtectionTemporaireUkraineBeCalculator.compute(
                LocalDate.of(2022, 3, 1), true, false, false, false,
                ProtectionTemporaireUkraineBeTitreSejourEnum.AUCUN, TODAY, DATE_FIN);

        assertThat(r.eligible()).isFalse();
        assertThat(r.recommandation()).contains("NON réunies");
    }

    @Test
    void compute_renouvellementImminent_dateFinProche_alerte() {
        // dateFin = today + 30 jours → 30 < 90 → renouvellement imminent
        LocalDate dateFinProche = TODAY.plusDays(30);
        ProtectionTemporaireUkraineBeResult r = ProtectionTemporaireUkraineBeCalculator.compute(
                LocalDate.of(2022, 3, 1), true, true, false, false,
                ProtectionTemporaireUkraineBeTitreSejourEnum.TITRE_A, TODAY, dateFinProche);

        assertThat(r.dureeProtectionRestante()).isEqualTo(30);
        assertThat(r.prochainRenouvellement()).isTrue();
        assertThat(r.recommandation()).contains("ALERTE");
    }

    @Test
    void compute_dureeRestanteSuperieureSeuil_pasDAlerte() {
        // dateFin = today + 120 jours → 120 >= 90 → pas de renouvellement imminent
        LocalDate dateFinLointaine = TODAY.plusDays(120);
        ProtectionTemporaireUkraineBeResult r = ProtectionTemporaireUkraineBeCalculator.compute(
                LocalDate.of(2022, 3, 1), true, true, false, false,
                ProtectionTemporaireUkraineBeTitreSejourEnum.TITRE_A, TODAY, dateFinLointaine);

        assertThat(r.dureeProtectionRestante()).isEqualTo(120);
        assertThat(r.prochainRenouvellement()).isFalse();
        assertThat(r.recommandation()).doesNotContain("ALERTE");
    }

    @Test
    void compute_apatrideUkraineAvecResidenceAvant_eligible() {
        // pas de nationalité ukrainienne mais apatride résidant en Ukraine avant le 24/02/2022
        ProtectionTemporaireUkraineBeResult r = ProtectionTemporaireUkraineBeCalculator.compute(
                LocalDate.of(2022, 4, 1), false, true, true, false,
                ProtectionTemporaireUkraineBeTitreSejourEnum.ATTESTATION_IMMATRICULATION, TODAY, DATE_FIN);

        assertThat(r.eligible()).isTrue();
        assertThat(r.apatridesUkraine()).isTrue();
        assertThat(r.nationaliteUkrainienne()).isFalse();
    }

    @Test
    void compute_membreFamilleProtegeAvecResidenceAvant_eligible() {
        ProtectionTemporaireUkraineBeResult r = ProtectionTemporaireUkraineBeCalculator.compute(
                LocalDate.of(2022, 5, 1), false, true, false, true,
                ProtectionTemporaireUkraineBeTitreSejourEnum.TITRE_B, TODAY, DATE_FIN);

        assertThat(r.eligible()).isTrue();
        assertThat(r.membreFamilleProtege()).isTrue();
    }

    @Test
    void compute_titreSejourAucun_eligible_recommandeEnregistrementOE() {
        ProtectionTemporaireUkraineBeResult r = ProtectionTemporaireUkraineBeCalculator.compute(
                LocalDate.of(2022, 3, 1), true, true, false, false,
                ProtectionTemporaireUkraineBeTitreSejourEnum.AUCUN, TODAY, DATE_FIN);

        assertThat(r.eligible()).isTrue();
        assertThat(r.titreSejourBE()).isEqualTo(ProtectionTemporaireUkraineBeTitreSejourEnum.AUCUN);
        assertThat(r.recommandation()).contains("Office des étrangers");
    }

    @Test
    void compute_aucunCritereBeneficiaire_nonEligibleMemeAvecResidence() {
        // ni nationalité, ni apatride, ni membre famille → non éligible
        ProtectionTemporaireUkraineBeResult r = ProtectionTemporaireUkraineBeCalculator.compute(
                LocalDate.of(2022, 3, 1), false, true, false, false,
                ProtectionTemporaireUkraineBeTitreSejourEnum.AUCUN, TODAY, DATE_FIN);

        assertThat(r.eligible()).isFalse();
    }

    @Test
    void compute_droitsAidesEtCheminProcedure_nonVides() {
        ProtectionTemporaireUkraineBeResult r = ProtectionTemporaireUkraineBeCalculator.compute(
                LocalDate.of(2022, 3, 1), true, true, false, false,
                ProtectionTemporaireUkraineBeTitreSejourEnum.TITRE_A, TODAY, DATE_FIN);

        assertThat(r.droitsAides()).isNotEmpty();
        assertThat(r.cheminProcedure()).isNotEmpty();
        assertThat(r.baseJuridique()).contains("2001/55/CE").contains("2022/382").contains("57/29");
    }

    @Test
    void compute_dateArriveeAvantActivation_throwsIllegalArgument() {
        assertThatThrownBy(() -> ProtectionTemporaireUkraineBeCalculator.compute(
                LocalDate.of(2022, 2, 23), true, true, false, false,
                ProtectionTemporaireUkraineBeTitreSejourEnum.TITRE_A, TODAY, DATE_FIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("24/02/2022");
    }

    @Test
    void compute_nationaliteNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> ProtectionTemporaireUkraineBeCalculator.compute(
                LocalDate.of(2022, 3, 1), null, true, false, false,
                ProtectionTemporaireUkraineBeTitreSejourEnum.TITRE_A, TODAY, DATE_FIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nationaliteUkrainienne");
    }

    @Test
    void compute_dateArriveeNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> ProtectionTemporaireUkraineBeCalculator.compute(
                null, true, true, false, false,
                ProtectionTemporaireUkraineBeTitreSejourEnum.TITRE_A, TODAY, DATE_FIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateArrivee");
    }
}
