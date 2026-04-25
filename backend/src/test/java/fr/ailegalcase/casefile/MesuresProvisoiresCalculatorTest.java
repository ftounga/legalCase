package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-FA-12-01 : tests unitaires de MesuresProvisoiresCalculator.
 * Couvre les 5 mesures (PA, logement, résidence enfants, contribution charges,
 * mesure conservatoire) + edge cases violences / âge enfants / revenus.
 */
class MesuresProvisoiresCalculatorTest {

    private static final LocalDate AUDIENCE = LocalDate.of(2026, 6, 15);

    private static MesuresProvisoiresResult.EnfantMineurInfo enfant(String prenom, int age) {
        return new MesuresProvisoiresResult.EnfantMineurInfo(prenom, age);
    }

    @Test
    void compute_nominal_alternee_pasViolences_revenusDifferents() {
        MesuresProvisoiresResult r = MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("3500"), new BigDecimal("2000"),
                "Maison Lyon, 130m²", "EN_INDIVISION",
                List.of(enfant("Léa", 8), enfant("Tom", 12)),
                "ALTERNEE",
                false, true, false);

        assertThat(r.differentielRevenus()).isEqualByComparingTo("1500.00");
        assertThat(r.pensionAlimentairePropose()).isEqualByComparingTo("750.00");
        // logement = revenus moindres = défendeur
        assertThat(r.attributionLogementRecommande()).isEqualTo("DEFENDEUR");
        assertThat(r.residenceEnfantsRecommande()).isEqualTo("ALTERNEE");
        assertThat(r.contributionCharges()).isEqualByComparingTo("1750.00");
        assertThat(r.mesureConservatoireRecommande()).isFalse();
        assertThat(r.scoreCohesionMesures()).isEqualTo(100);
        assertThat(r.verdictAcceptabilite()).isEqualTo("ELEVEE");
        assertThat(r.baseJuridique()).contains("254").contains("256");
    }

    @Test
    void compute_violences_attributionLogement_demandeur_residence_exclusive() {
        MesuresProvisoiresResult r = MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("4000"), new BigDecimal("1500"),
                "Appartement Paris", "PROPRIETE_DEFENDEUR",
                List.of(enfant("Mia", 4)),
                "ALTERNEE",
                true, false, false);

        // Violences → logement au demandeur même si défendeur propriétaire
        assertThat(r.attributionLogementRecommande()).isEqualTo("DEMANDEUR");
        // Violences → résidence exclusive demandeur (parent non-violent)
        assertThat(r.residenceEnfantsRecommande()).isEqualTo("EXCLUSIVE_DEMANDEUR");
        // souhait ALTERNEE divergence → -20 ; logement défendeur propriétaire
        // mais attribué demandeur → -10. Score = 70.
        assertThat(r.scoreCohesionMesures()).isEqualTo(70);
        assertThat(r.verdictAcceptabilite()).isEqualTo("MOYENNE");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("ordonnance"));
    }

    @Test
    void compute_logement_proprieteDemandeur_pasViolences() {
        MesuresProvisoiresResult r = MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("2500"), new BigDecimal("3000"),
                "Maison Lille", "PROPRIETE_DEMANDEUR",
                List.of(),
                "ALTERNEE",
                false, false, false);
        assertThat(r.attributionLogementRecommande()).isEqualTo("DEMANDEUR");
        assertThat(r.residenceEnfantsRecommande()).isEqualTo("SANS_OBJET");
    }

    @Test
    void compute_logement_proprieteDefendeur_pasViolences() {
        MesuresProvisoiresResult r = MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("3000"), new BigDecimal("3000"),
                "Maison Bordeaux", "PROPRIETE_DEFENDEUR",
                List.of(),
                "EXCLUSIVE_DEMANDEUR",
                false, false, false);
        assertThat(r.attributionLogementRecommande()).isEqualTo("DEFENDEUR");
    }

    @Test
    void compute_revenusEgaux_indivision_indivisionMaintenue() {
        MesuresProvisoiresResult r = MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("2500"), new BigDecimal("2500"),
                "Maison Toulouse", "EN_INDIVISION",
                List.of(enfant("Hugo", 10)),
                "ALTERNEE",
                false, false, false);
        assertThat(r.attributionLogementRecommande()).isEqualTo("INDIVISION_MAINTENUE");
        assertThat(r.differentielRevenus()).isEqualByComparingTo("0.00");
        assertThat(r.pensionAlimentairePropose()).isEqualByComparingTo("0.00");
        // -15 indivision maintenue + revenus égaux
        assertThat(r.scoreCohesionMesures()).isEqualTo(85);
        assertThat(r.verdictAcceptabilite()).isEqualTo("ELEVEE");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("indivision"));
    }

    @Test
    void compute_residenceAlterneeRefusee_enfantsTropJeunes() {
        MesuresProvisoiresResult r = MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("3500"), new BigDecimal("2000"),
                "Maison Strasbourg", "EN_INDIVISION",
                List.of(enfant("Bébé", 2)),
                "ALTERNEE",
                false, false, false);
        assertThat(r.residenceEnfantsRecommande()).isEqualTo("EXCLUSIVE_DEMANDEUR");
        // souhait ALTERNEE diverge de recommandation → -20
        assertThat(r.scoreCohesionMesures()).isEqualTo(80);
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("< 6 ans"));
    }

    @Test
    void compute_residenceAlternee_acceptee_enfantsAge6Pile() {
        MesuresProvisoiresResult r = MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("3000"), new BigDecimal("2000"),
                "Maison Nantes", "EN_INDIVISION",
                List.of(enfant("Jules", 6), enfant("Manon", 9)),
                "ALTERNEE",
                false, false, false);
        assertThat(r.residenceEnfantsRecommande()).isEqualTo("ALTERNEE");
    }

    @Test
    void compute_residenceExclusive_demandeur_souhaitRespecte() {
        MesuresProvisoiresResult r = MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("2500"), new BigDecimal("3500"),
                "Appartement Rennes", "EN_INDIVISION",
                List.of(enfant("Lucas", 3)),
                "EXCLUSIVE_DEMANDEUR",
                false, false, false);
        assertThat(r.residenceEnfantsRecommande()).isEqualTo("EXCLUSIVE_DEMANDEUR");
        assertThat(r.scoreCohesionMesures()).isEqualTo(100);
    }

    @Test
    void compute_pasEnfants_residenceSansObjet() {
        MesuresProvisoiresResult r = MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("3000"), new BigDecimal("2000"),
                "Studio Paris", "LOCATION_COMMUNE",
                null,
                "ALTERNEE",
                false, false, false);
        assertThat(r.residenceEnfantsRecommande()).isEqualTo("SANS_OBJET");
        // SANS_OBJET ne pénalise pas le score
        assertThat(r.scoreCohesionMesures()).isEqualTo(100);
    }

    @Test
    void compute_mesureConservatoire_recommandee() {
        MesuresProvisoiresResult r = MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("5000"), new BigDecimal("2500"),
                "Maison + résidence secondaire", "EN_INDIVISION",
                List.of(),
                "EXCLUSIVE_DEMANDEUR",
                false, true, true);
        assertThat(r.mesureConservatoireRecommande()).isTrue();
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("220-1"));
    }

    @Test
    void compute_mesureConservatoire_nonRecommandee_sansPatrimoine() {
        MesuresProvisoiresResult r = MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("3500"), new BigDecimal("2000"),
                "Studio", "LOCATION_COMMUNE",
                List.of(),
                "EXCLUSIVE_DEMANDEUR",
                false, false, true);
        assertThat(r.mesureConservatoireRecommande()).isFalse();
    }

    @Test
    void compute_mesureConservatoire_nonRecommandee_sansDemande() {
        MesuresProvisoiresResult r = MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("3500"), new BigDecimal("2000"),
                "Maison + portefeuille titres", "EN_INDIVISION",
                List.of(),
                "EXCLUSIVE_DEMANDEUR",
                false, true, false);
        assertThat(r.mesureConservatoireRecommande()).isFalse();
    }

    @Test
    void compute_pension_revenusEgaux_zero() {
        MesuresProvisoiresResult r = MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("2500"), new BigDecimal("2500"),
                "Studio", "LOCATION_COMMUNE",
                List.of(),
                "EXCLUSIVE_DEMANDEUR",
                false, false, false);
        assertThat(r.pensionAlimentairePropose()).isEqualByComparingTo("0.00");
    }

    @Test
    void compute_pension_demandeurMoinsRicheQueDefendeur_signePositif() {
        MesuresProvisoiresResult r = MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("1500"), new BigDecimal("4000"),
                "Maison", "PROPRIETE_DEFENDEUR",
                List.of(),
                "EXCLUSIVE_DEMANDEUR",
                false, false, false);
        // différentiel = -2500, mais PA = abs(-2500)/2 = 1250
        assertThat(r.differentielRevenus()).isEqualByComparingTo("-2500.00");
        assertThat(r.pensionAlimentairePropose()).isEqualByComparingTo("1250.00");
    }

    @Test
    void compute_contributionCharges_max_demi() {
        MesuresProvisoiresResult r = MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("4500.00"), new BigDecimal("2000.00"),
                "Maison", "EN_INDIVISION",
                List.of(),
                "EXCLUSIVE_DEMANDEUR",
                false, false, false);
        // max(4500, 2000) / 2 = 2250
        assertThat(r.contributionCharges()).isEqualByComparingTo("2250.00");
    }

    @Test
    void compute_verdict_seuils_moyenne() {
        // souhait diverge -20, indivision maintenue -15 → 65
        MesuresProvisoiresResult r = MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("3000"), new BigDecimal("3000"),
                "Maison", "EN_INDIVISION",
                List.of(enfant("Bébé", 1)),
                "ALTERNEE",
                false, false, false);
        assertThat(r.scoreCohesionMesures()).isEqualTo(65);
        assertThat(r.verdictAcceptabilite()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_formule_inclutLesCinqMesures() {
        MesuresProvisoiresResult r = MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("3500"), new BigDecimal("2000"),
                "Maison", "EN_INDIVISION",
                List.of(enfant("Léa", 8)),
                "ALTERNEE",
                false, false, false);
        assertThat(r.formule()).contains("PA").contains("logement")
                .contains("résidence").contains("contribution");
    }

    @Test
    void compute_messages_inclutAOMP_et_F_FA_02() {
        MesuresProvisoiresResult r = MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("3000"), new BigDecimal("2000"),
                "Maison", "EN_INDIVISION",
                List.of(),
                "EXCLUSIVE_DEMANDEUR",
                false, false, false);
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("AOMP"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("F-FA-02"));
    }

    @Test
    void compute_dateAudienceNull_throws() {
        assertThatThrownBy(() -> MesuresProvisoiresCalculator.compute(
                null,
                new BigDecimal("3000"), new BigDecimal("2000"),
                "Maison", "EN_INDIVISION",
                List.of(),
                "EXCLUSIVE_DEMANDEUR",
                false, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateAudienceAOMP");
    }

    @Test
    void compute_revenusNegatifs_throws() {
        assertThatThrownBy(() -> MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("-100"), new BigDecimal("2000"),
                "Maison", "EN_INDIVISION",
                List.of(),
                "EXCLUSIVE_DEMANDEUR",
                false, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Demandeur");
    }

    @Test
    void compute_logementProprietaireInvalide_throws() {
        assertThatThrownBy(() -> MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("3000"), new BigDecimal("2000"),
                "Maison", "INVALIDE",
                List.of(),
                "EXCLUSIVE_DEMANDEUR",
                false, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("logementProprietaire");
    }

    @Test
    void compute_souhaitResidenceInvalide_throws() {
        assertThatThrownBy(() -> MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("3000"), new BigDecimal("2000"),
                "Maison", "EN_INDIVISION",
                List.of(),
                "INVALIDE",
                false, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("souhaitResidenceEnfants");
    }

    @Test
    void compute_ageEnfantInvalide_throws() {
        assertThatThrownBy(() -> MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("3000"), new BigDecimal("2000"),
                "Maison", "EN_INDIVISION",
                List.of(enfant("Adulte", 18)),
                "EXCLUSIVE_DEMANDEUR",
                false, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Âge");
    }

    @Test
    void compute_locationCommune_revenusEgaux_indivisionMaintenue() {
        MesuresProvisoiresResult r = MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("3000"), new BigDecimal("3000"),
                "Studio", "LOCATION_COMMUNE",
                List.of(),
                "EXCLUSIVE_DEMANDEUR",
                false, false, false);
        assertThat(r.attributionLogementRecommande()).isEqualTo("INDIVISION_MAINTENUE");
    }

    @Test
    void compute_souhaitRespecte_aucunePenalite_score100() {
        MesuresProvisoiresResult r = MesuresProvisoiresCalculator.compute(
                AUDIENCE,
                new BigDecimal("4000"), new BigDecimal("2000"),
                "Maison", "EN_INDIVISION",
                List.of(enfant("Tom", 12)),
                "ALTERNEE",
                false, false, false);
        // logement défendeur (revenus moindres) + alternée OK → score 100
        assertThat(r.scoreCohesionMesures()).isEqualTo(100);
    }
}
