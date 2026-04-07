package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecoursGeneratorTest {

    private static final LocalDate NOTIFICATION = LocalDate.of(2026, 3, 1);

    @Test
    void recoursGracieuxFrance_generatesWithPrefetAndCESEDA() {
        GeneratedRecours result = RecoursGenerator.generate(
                "RECOURS_GRACIEUX_PREFET", NOTIFICATION,
                "Dupont", "Jean", "Marocaine", "12 rue de la Paix, 75001 Paris",
                "Préfet de Police de Paris", LocalDate.of(2026, 2, 15), "PREF-2026-001",
                "Le requérant réside en France depuis 5 ans."
        );

        assertThat(result.recoursType()).isEqualTo("RECOURS_GRACIEUX_PREFET");
        assertThat(result.enTete()).contains("Préfecture");
        assertThat(result.enTete()).contains("Jean Dupont");
        assertThat(result.visaTextes()).contains("CESEDA");
        assertThat(result.moyensDroit()).contains("Jean Dupont");
        assertThat(result.conclusions()).contains("ANNULER");
        assertThat(result.piecesJointes()).isNotEmpty();
        assertThat(result.dateLimite()).isEqualTo(NOTIFICATION.plusDays(60));
    }

    @Test
    void recoursCGRA_belgique_generatesWithLoiBelge() {
        GeneratedRecours result = RecoursGenerator.generate(
                "RECOURS_CGRA", NOTIFICATION,
                "Diallo", "Mamadou", "Guinéenne", "Rue de Flandre 45, 1000 Bruxelles",
                "Office des étrangers", LocalDate.of(2026, 2, 20), null,
                null
        );

        assertThat(result.recoursType()).isEqualTo("RECOURS_CGRA");
        assertThat(result.enTete()).contains("CGRA");
        assertThat(result.enTete()).contains("Mamadou Diallo");
        assertThat(result.visaTextes()).contains("Loi du 15 décembre 1980");
        assertThat(result.exposeFaits()).isEqualTo("[À compléter par l'avocat]");
        assertThat(result.dateLimite()).isEqualTo(NOTIFICATION.plusDays(15));
    }

    @Test
    void dateLimiteCorrectlyCalculated() {
        GeneratedRecours result = RecoursGenerator.generate(
                "RECOURS_CNDA", NOTIFICATION,
                "Martin", "Marie", "Syrienne", "5 avenue Victor Hugo, 93100 Montreuil",
                "OFPRA", LocalDate.of(2026, 2, 25), "OFPRA-2026-123",
                "Craintes de persécution."
        );

        // CNDA = 30 jours
        assertThat(result.dateLimite()).isEqualTo(NOTIFICATION.plusDays(30));
    }

    @Test
    void dateLimiteDepassee_generatesWarning() {
        LocalDate oldNotification = LocalDate.of(2020, 1, 1);
        GeneratedRecours result = RecoursGenerator.generate(
                "RECOURS_CONTENTIEUX_TA", oldNotification,
                "Doe", "John", "Nigériane", "1 rue Test, 75001 Paris",
                "Préfet de Paris", LocalDate.of(2019, 12, 15), null,
                "Faits."
        );

        assertThat(result.dateLimiteDepassee()).isTrue();
        assertThat(result.avertissement()).contains("dépassé");
        assertThat(result.avertissement()).contains("60 jours");
    }

    @Test
    void dateLimiteNonDepassee_noWarning() {
        LocalDate recentNotification = LocalDate.now().minusDays(5);
        GeneratedRecours result = RecoursGenerator.generate(
                "RECOURS_GRACIEUX_PREFET", recentNotification,
                "Test", "User", "Française", "Adresse",
                "Préfet", LocalDate.now().minusDays(10), null,
                null
        );

        assertThat(result.dateLimiteDepassee()).isFalse();
        assertThat(result.avertissement()).isNull();
    }

    @Test
    void unknownType_throwsException() {
        assertThatThrownBy(() -> RecoursGenerator.generate(
                "INCONNU", NOTIFICATION,
                "Nom", "Prenom", "Nationalite", "Adresse",
                "Autorite", LocalDate.of(2026, 1, 1), null, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Type de recours inconnu");
    }

    @Test
    void allTypes_generateWithoutError() {
        for (RecoursType type : ImmigrationRecoursReferentiel.getAll()) {
            GeneratedRecours result = RecoursGenerator.generate(
                    type.code(), NOTIFICATION,
                    "TestNom", "TestPrenom", "TestNat", "TestAdresse",
                    "TestAutorite", LocalDate.of(2026, 2, 1), "REF-001",
                    "Exposé des faits."
            );

            assertThat(result.recoursType()).as("type %s", type.code()).isEqualTo(type.code());
            assertThat(result.enTete()).as("enTete %s", type.code()).isNotBlank();
            assertThat(result.objetDemande()).as("objet %s", type.code()).isNotBlank();
            assertThat(result.visaTextes()).as("visa %s", type.code()).isNotBlank();
            assertThat(result.moyensDroit()).as("moyens %s", type.code()).isNotBlank();
            assertThat(result.conclusions()).as("conclusions %s", type.code()).isNotBlank();
            assertThat(result.piecesJointes()).as("pieces %s", type.code()).isNotEmpty();
            assertThat(result.dateLimite()).as("dateLimite %s", type.code()).isNotNull();
        }
    }

    @Test
    void recoursCCE_belgique_generatesCorrectly() {
        GeneratedRecours result = RecoursGenerator.generate(
                "RECOURS_CCE", NOTIFICATION,
                "Yilmaz", "Ayşe", "Turque", "Chaussée de Louvain 120, 1210 Bruxelles",
                "Office des étrangers", LocalDate.of(2026, 2, 10), "OE-2026-456",
                "Refus de renouvellement du titre de séjour."
        );

        assertThat(result.visaTextes()).contains("articles 39/1");
        assertThat(result.moyensDroit()).contains("Ayşe Yilmaz");
        assertThat(result.enTete()).contains("CCE");
        assertThat(result.dateLimite()).isEqualTo(NOTIFICATION.plusDays(30));
    }

    @Test
    void exposeFaitsNull_replacedByPlaceholder() {
        GeneratedRecours result = RecoursGenerator.generate(
                "RECOURS_GRACIEUX_PREFET", NOTIFICATION,
                "Nom", "Prenom", "Nat", "Adresse",
                "Autorite", LocalDate.of(2026, 2, 1), null, null
        );

        assertThat(result.exposeFaits()).isEqualTo("[À compléter par l'avocat]");
    }
}
