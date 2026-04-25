package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-FA-19-05 : tests unitaires de DesaccordsParentauxCalculator (art. 373-2-10 Cciv).
 *
 * <p>Couvre les 5 contributions du score (domaine, intensité, médiation,
 * intérêt supérieur, urgence), les seuils de verdict, les recommandations
 * (médiation, audition art. 388-1, expertise psy), le délai prévisionnel et
 * les erreurs de validation.
 */
class DesaccordsParentauxCalculatorTest {

    private static final LocalDate REQUETE = LocalDate.of(2026, 5, 10);

    @Test
    void compute_nominal_eleveeScore() {
        // Scolarité (+20) + MAJEUR (+30) + 2 tentatives (+25) = 75
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "SCOLARITE", "MAJEUR",
                List.of("MEDIATION_FAMILIALE", "DISCUSSIONS_DIRECTES"),
                List.of(10, 14),
                false, false, false, REQUETE);

        assertThat(r.scoreEligibiliteJaf()).isEqualTo(75);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("ELEVEE");
        assertThat(r.mediationPrealableRecommandee()).isFalse();
        assertThat(r.auditionEnfantsRecommandee()).isTrue(); // 14 >= 13
        assertThat(r.expertisePsyRecommandee()).isTrue(); // MAJEUR && !expertiseDeja
        assertThat(r.delaiTraitementJoursPrevisionnel()).isEqualTo(90);
        assertThat(r.baseJuridique()).contains("373-2-10");
    }

    @Test
    void compute_domaineAutre_intensiteMineur_aucuneMediation_score5_faible() {
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "AUTRE", "MINEUR",
                List.of(),
                List.of(),
                false, false, false, REQUETE);

        assertThat(r.scoreEligibiliteJaf()).isEqualTo(5);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("FAIBLE");
        assertThat(r.mediationPrealableRecommandee()).isTrue();
        assertThat(r.auditionEnfantsRecommandee()).isFalse();
        assertThat(r.expertisePsyRecommandee()).isFalse();
    }

    @Test
    void compute_tousLesCriteres_score100Plafonne() {
        // 20 + 30 + 25 + 15 + 10 = 100
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "DEMENAGEMENT", "MAJEUR",
                List.of("MEDIATION_FAMILIALE", "MEDIATION_JUDICIAIRE"),
                List.of(15, 16),
                true, false, true, REQUETE);

        assertThat(r.scoreEligibiliteJaf()).isEqualTo(100);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("ELEVEE");
        assertThat(r.delaiTraitementJoursPrevisionnel()).isEqualTo(30); // urgence
    }

    @Test
    void compute_seuilMoyenne_intensiteMoyen_uneMediation() {
        // 20 (domaine) + 15 (MOYEN) + 10 (1 tentative) = 45 → MOYENNE
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "SANTE", "MOYEN",
                List.of("DISCUSSIONS_DIRECTES"),
                List.of(8),
                false, false, false, REQUETE);

        assertThat(r.scoreEligibiliteJaf()).isEqualTo(45);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_aucuneSeule_compteCommeZeroMediation() {
        // 20 + 15 + 0 (AUCUNE seule) = 35 → FAIBLE
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "RELIGION", "MOYEN",
                List.of("AUCUNE"),
                List.of(),
                false, false, false, REQUETE);

        assertThat(r.scoreEligibiliteJaf()).isEqualTo(35);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("FAIBLE");
        assertThat(r.mediationPrealableRecommandee()).isTrue();
    }

    @Test
    void compute_mediationVide_recommandee() {
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "SCOLARITE", "MAJEUR",
                List.of(),
                List.of(),
                false, false, false, REQUETE);
        assertThat(r.mediationPrealableRecommandee()).isTrue();
    }

    @Test
    void compute_mediationAvecTentative_pasRecommandee() {
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "SCOLARITE", "MAJEUR",
                List.of("MEDIATION_FAMILIALE"),
                List.of(),
                false, false, false, REQUETE);
        assertThat(r.mediationPrealableRecommandee()).isFalse();
    }

    @Test
    void compute_auditionRecommandee_unEnfant13Plus() {
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "SCOLARITE", "MOYEN",
                List.of(),
                List.of(8, 13),
                false, false, false, REQUETE);
        assertThat(r.auditionEnfantsRecommandee()).isTrue();
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("388-1"));
    }

    @Test
    void compute_auditionNonRecommandee_aucunEnfant13Plus() {
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "SCOLARITE", "MOYEN",
                List.of(),
                List.of(5, 10, 12),
                false, false, false, REQUETE);
        assertThat(r.auditionEnfantsRecommandee()).isFalse();
    }

    @Test
    void compute_auditionNonRecommandee_aucunEnfant() {
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "SCOLARITE", "MOYEN",
                List.of(),
                List.of(),
                false, false, false, REQUETE);
        assertThat(r.auditionEnfantsRecommandee()).isFalse();
    }

    @Test
    void compute_expertisePsyRecommandee_majeurEtPasExpertise() {
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "SANTE", "MAJEUR",
                List.of(),
                List.of(),
                false, false, false, REQUETE);
        assertThat(r.expertisePsyRecommandee()).isTrue();
    }

    @Test
    void compute_expertisePsyNonRecommandee_majeurMaisExpertiseDeja() {
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "SANTE", "MAJEUR",
                List.of(),
                List.of(),
                false, true, false, REQUETE);
        assertThat(r.expertisePsyRecommandee()).isFalse();
    }

    @Test
    void compute_expertisePsyNonRecommandee_intensiteMoyen() {
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "SANTE", "MOYEN",
                List.of(),
                List.of(),
                false, false, false, REQUETE);
        assertThat(r.expertisePsyRecommandee()).isFalse();
    }

    @Test
    void compute_delai30_siUrgence() {
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "SCOLARITE", "MAJEUR",
                List.of(),
                List.of(),
                false, false, true, REQUETE);
        assertThat(r.delaiTraitementJoursPrevisionnel()).isEqualTo(30);
    }

    @Test
    void compute_delai90_siNonUrgence() {
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "SCOLARITE", "MOYEN",
                List.of(),
                List.of(),
                false, false, false, REQUETE);
        assertThat(r.delaiTraitementJoursPrevisionnel()).isEqualTo(90);
    }

    @Test
    void compute_interetSuperieurAjoute15() {
        // 0 (AUTRE) + 5 (MINEUR) + 0 + 15 (intérêt) + 0 = 20
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "AUTRE", "MINEUR",
                List.of(),
                List.of(),
                true, false, false, REQUETE);
        assertThat(r.scoreEligibiliteJaf()).isEqualTo(20);
    }

    @Test
    void compute_urgenceAjoute10() {
        // 0 (AUTRE) + 5 (MINEUR) + 0 + 0 + 10 (urgence) = 15
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "AUTRE", "MINEUR",
                List.of(),
                List.of(),
                false, false, true, REQUETE);
        assertThat(r.scoreEligibiliteJaf()).isEqualTo(15);
    }

    @Test
    void compute_formuleContientDomaineEtIntensite() {
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "SCOLARITE", "MAJEUR",
                List.of("MEDIATION_FAMILIALE", "DISCUSSIONS_DIRECTES"),
                List.of(),
                false, false, false, REQUETE);
        assertThat(r.formule()).contains("SCOLARITE").contains("MAJEUR")
                .contains("75").contains("ELEVEE");
    }

    @Test
    void compute_messagesContiennentBaseJuridique() {
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "SCOLARITE", "MAJEUR",
                List.of("MEDIATION_FAMILIALE"),
                List.of(),
                false, false, false, REQUETE);
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("373-2-10"));
    }

    @Test
    void compute_messageDemenagement_present() {
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "DEMENAGEMENT", "MAJEUR",
                List.of(),
                List.of(),
                false, false, false, REQUETE);
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("373-2"));
    }

    @Test
    void compute_messageSante_present() {
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "SANTE", "MAJEUR",
                List.of(),
                List.of(),
                false, false, false, REQUETE);
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("usuel"));
    }

    @Test
    void compute_messageReligion_present() {
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "RELIGION", "MAJEUR",
                List.of(),
                List.of(),
                false, false, false, REQUETE);
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("Religion"));
    }

    @Test
    void compute_domaineInvalide_throws() {
        assertThatThrownBy(() -> DesaccordsParentauxCalculator.compute(
                "INVALIDE", "MAJEUR",
                List.of(),
                List.of(),
                false, false, false, REQUETE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("domaineDesaccord");
    }

    @Test
    void compute_intensiteInvalide_throws() {
        assertThatThrownBy(() -> DesaccordsParentauxCalculator.compute(
                "SCOLARITE", "EXTREME",
                List.of(),
                List.of(),
                false, false, false, REQUETE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("intensiteDesaccord");
    }

    @Test
    void compute_mediationInvalide_throws() {
        assertThatThrownBy(() -> DesaccordsParentauxCalculator.compute(
                "SCOLARITE", "MAJEUR",
                List.of("INVALIDE"),
                List.of(),
                false, false, false, REQUETE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tentativesMediation");
    }

    @Test
    void compute_ageEnfantInvalide_throws() {
        assertThatThrownBy(() -> DesaccordsParentauxCalculator.compute(
                "SCOLARITE", "MAJEUR",
                List.of(),
                List.of(18),
                false, false, false, REQUETE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ageEnfantsConcernes");
    }

    @Test
    void compute_dateRequeteNull_throws() {
        assertThatThrownBy(() -> DesaccordsParentauxCalculator.compute(
                "SCOLARITE", "MAJEUR",
                List.of(),
                List.of(),
                false, false, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateRequete");
    }

    @Test
    void compute_tentativesDoublons_dedupliquees() {
        DesaccordsParentauxResult r = DesaccordsParentauxCalculator.compute(
                "SCOLARITE", "MAJEUR",
                List.of("MEDIATION_FAMILIALE", "MEDIATION_FAMILIALE", "DISCUSSIONS_DIRECTES"),
                List.of(),
                false, false, false, REQUETE);
        assertThat(r.tentativesMediation())
                .containsExactly("MEDIATION_FAMILIALE", "DISCUSSIONS_DIRECTES");
        // 20 (SCOLARITE) + 30 (MAJEUR) + 25 (2 tentatives effectives) = 75
        assertThat(r.scoreEligibiliteJaf()).isEqualTo(75);
    }
}
