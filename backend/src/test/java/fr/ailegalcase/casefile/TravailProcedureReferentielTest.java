package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-136 SF-136-01 : tests du référentiel statique des jalons procéduraux pour le droit du travail.
 *
 * <p>Couvre les 6 procédures (3 FR + 3 BE) et les cas limites (type/country null, mismatch).
 * Les valeurs testées doivent rester strictement alignées avec le seed DB de la migration 130
 * (le test {@link fr.ailegalcase.referential.LegalReferentialDescriptionIntegrityIT} en CI vérifie
 * la présence DB ; ce test-ci vérifie le fallback Java statique).
 */
class TravailProcedureReferentielTest {

    // -------------------- France --------------------

    @Test
    void prudhommesFr_returns5Jalons_avecDelaisOrdonnes() {
        List<TravailProcedureReferentiel.ProcedureJalon> jalons =
                TravailProcedureReferentiel.resolve("PRUDHOMMES_FR", "FR");
        assertThat(jalons).hasSize(5);
        assertThat(jalons.get(0).offsetDays()).isEqualTo(45);   // BCO convocation
        assertThat(jalons.get(1).offsetDays()).isEqualTo(90);   // BCO audience
        assertThat(jalons.get(2).offsetDays()).isEqualTo(180);  // renvoi
        assertThat(jalons.get(3).offsetDays()).isEqualTo(270);  // bureau jugement
        assertThat(jalons.get(4).offsetDays()).isEqualTo(330);  // notification
        assertThat(jalons.get(0).articleRef()).contains("R.1452-3");
        assertThat(jalons.get(1).articleRef()).contains("L.1454-1");
    }

    @Test
    void appelCaSocialeFr_premierJalon30JoursDelaiAppel() {
        List<TravailProcedureReferentiel.ProcedureJalon> jalons =
                TravailProcedureReferentiel.resolve("APPEL_CA_SOCIALE_FR", "FR");
        assertThat(jalons).hasSize(4);
        assertThat(jalons.get(0).offsetDays()).isEqualTo(30);
        assertThat(jalons.get(0).label()).contains("appel");
        assertThat(jalons.get(0).articleRef()).contains("R.1461-1");
    }

    @Test
    void cassationSocialeFr_pourvoi60JoursArt612Cpc() {
        List<TravailProcedureReferentiel.ProcedureJalon> jalons =
                TravailProcedureReferentiel.resolve("CASSATION_SOCIALE_FR", "FR");
        assertThat(jalons).hasSize(4);
        assertThat(jalons.get(0).offsetDays()).isEqualTo(60);
        assertThat(jalons.get(0).articleRef()).contains("612");
    }

    // -------------------- Belgique --------------------

    @Test
    void tribunalTravailBe_returns4Jalons() {
        List<TravailProcedureReferentiel.ProcedureJalon> jalons =
                TravailProcedureReferentiel.resolve("TRIBUNAL_TRAVAIL_BE", "BE");
        assertThat(jalons).hasSize(4);
        assertThat(jalons.get(0).offsetDays()).isEqualTo(30);
        assertThat(jalons.get(0).articleRef()).contains("CJ");
        assertThat(jalons.get(0).articleRef()).contains("700");
    }

    @Test
    void courTravailBe_delaiAppel30Jours_art1051Cj() {
        List<TravailProcedureReferentiel.ProcedureJalon> jalons =
                TravailProcedureReferentiel.resolve("COUR_TRAVAIL_BE", "BE");
        assertThat(jalons).hasSize(4);
        assertThat(jalons.get(0).offsetDays()).isEqualTo(30);
        assertThat(jalons.get(0).articleRef()).contains("1051");
    }

    @Test
    void cassationBe_pourvoi90Jours_art1073Cj() {
        List<TravailProcedureReferentiel.ProcedureJalon> jalons =
                TravailProcedureReferentiel.resolve("CASSATION_BE", "BE");
        assertThat(jalons).hasSize(4);
        assertThat(jalons.get(0).offsetDays()).isEqualTo(90);
        assertThat(jalons.get(0).articleRef()).contains("1073");
    }

    // -------------------- Cas limites --------------------

    @Test
    void unknownType_returnsEmptyList() {
        assertThat(TravailProcedureReferentiel.resolve("INCONNU", "FR")).isEmpty();
        assertThat(TravailProcedureReferentiel.resolve("INCONNU", "BE")).isEmpty();
    }

    @Test
    void nullType_returnsEmptyList() {
        assertThat(TravailProcedureReferentiel.resolve(null, "FR")).isEmpty();
    }

    @Test
    void nullCountry_returnsEmptyList() {
        assertThat(TravailProcedureReferentiel.resolve("PRUDHOMMES_FR", null)).isEmpty();
    }

    @Test
    void mismatchTypeFrAvecCountryBe_returnsEmptyList() {
        // PRUDHOMMES_FR ne doit pas répondre quand country=BE
        assertThat(TravailProcedureReferentiel.resolve("PRUDHOMMES_FR", "BE")).isEmpty();
        // TRIBUNAL_TRAVAIL_BE ne doit pas répondre quand country=FR
        assertThat(TravailProcedureReferentiel.resolve("TRIBUNAL_TRAVAIL_BE", "FR")).isEmpty();
    }

    @Test
    void unknownCountry_returnsEmptyList() {
        assertThat(TravailProcedureReferentiel.resolve("PRUDHOMMES_FR", "DE")).isEmpty();
    }

    // -------------------- Cohérence transversale --------------------

    @Test
    void allFrTypesHaveCodeTravailOrCpcArticleRef() {
        List<String> frTypes = List.of("PRUDHOMMES_FR", "APPEL_CA_SOCIALE_FR", "CASSATION_SOCIALE_FR");
        for (String type : frTypes) {
            List<TravailProcedureReferentiel.ProcedureJalon> jalons =
                    TravailProcedureReferentiel.resolve(type, "FR");
            assertThat(jalons).as("FR type %s doit avoir au moins 1 jalon", type).isNotEmpty();
            for (TravailProcedureReferentiel.ProcedureJalon jalon : jalons) {
                assertThat(jalon.articleRef())
                        .as("FR jalon '%s' doit citer un article (Code travail ou CPC)", jalon.label())
                        .isNotBlank()
                        .matches(s -> s.contains("Code travail") || s.contains("CPC"));
                assertThat(jalon.label()).as("Label non vide").isNotBlank();
                assertThat(jalon.offsetDays()).as("OffsetDays positif").isPositive();
            }
        }
    }

    @Test
    void allBeTypesHaveCjArticleRef() {
        List<String> beTypes = List.of("TRIBUNAL_TRAVAIL_BE", "COUR_TRAVAIL_BE", "CASSATION_BE");
        for (String type : beTypes) {
            List<TravailProcedureReferentiel.ProcedureJalon> jalons =
                    TravailProcedureReferentiel.resolve(type, "BE");
            assertThat(jalons).as("BE type %s doit avoir au moins 1 jalon", type).isNotEmpty();
            for (TravailProcedureReferentiel.ProcedureJalon jalon : jalons) {
                assertThat(jalon.articleRef())
                        .as("BE jalon '%s' doit citer un article CJ", jalon.label())
                        .isNotBlank()
                        .contains("CJ");
                assertThat(jalon.label()).as("Label non vide").isNotBlank();
                assertThat(jalon.offsetDays()).as("OffsetDays positif").isPositive();
            }
        }
    }

    @Test
    void allTypesReturnImmutableList() {
        List<TravailProcedureReferentiel.ProcedureJalon> jalons =
                TravailProcedureReferentiel.resolve("PRUDHOMMES_FR", "FR");
        // List.of(...) renvoie un ImmutableCollections — toute mutation lève UnsupportedOperationException
        assertThat(jalons).isUnmodifiable();
    }

    @Test
    void countryNormalizedToUpperCase() {
        // L'appel avec country en minuscule doit être supporté (normalisation upper)
        assertThat(TravailProcedureReferentiel.resolve("PRUDHOMMES_FR", "fr")).hasSize(5);
        assertThat(TravailProcedureReferentiel.resolve("TRIBUNAL_TRAVAIL_BE", "be")).hasSize(4);
    }
}
