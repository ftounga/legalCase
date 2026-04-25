package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-137 SF-F-137-01 : tests du référentiel statique des jalons procéduraux pour le droit
 * de la famille belge.
 *
 * <p>Couvre les 3 procédures BE (tribunal de la famille, cour d'appel famille, cassation)
 * et les cas limites (type/country null, mismatch FR/DE).
 *
 * <p>Les valeurs testées doivent rester strictement alignées avec le seed DB de la migration
 * 162 (le test {@link fr.ailegalcase.referential.LegalReferentialDescriptionIntegrityIT} en CI
 * vérifie la présence DB ; ce test-ci vérifie le fallback Java statique).
 */
class FamilleProcedureReferentielTest {

    // -------------------- Belgique --------------------

    @Test
    void tribunalFamilleBe_returns5Jalons_avecDelaisOrdonnes() {
        List<FamilleProcedureReferentiel.ProcedureJalon> jalons =
                FamilleProcedureReferentiel.resolve("TRIBUNAL_FAMILLE_BE", "BE");
        assertThat(jalons).hasSize(5);
        assertThat(jalons.get(0).offsetDays()).isEqualTo(0);    // dépôt requête
        assertThat(jalons.get(1).offsetDays()).isEqualTo(30);   // audience MP
        assertThat(jalons.get(2).offsetDays()).isEqualTo(120);  // mise en état
        assertThat(jalons.get(3).offsetDays()).isEqualTo(180);  // plaidoiries fond
        assertThat(jalons.get(4).offsetDays()).isEqualTo(240);  // prononcé
        assertThat(jalons.get(0).articleRef()).contains("572bis");
        assertThat(jalons.get(1).articleRef()).contains("1253ter/2");
    }

    @Test
    void tribunalFamilleBe_audienceMP_jalonPresent() {
        List<FamilleProcedureReferentiel.ProcedureJalon> jalons =
                FamilleProcedureReferentiel.resolve("TRIBUNAL_FAMILLE_BE", "BE");
        // Vérifie qu'un jalon "audience MP" est bien présent et cite un article CJ.
        boolean foundMp = jalons.stream()
                .anyMatch(j -> j.label().toLowerCase().contains("mesures provisoires")
                        && j.articleRef().contains("CJ"));
        assertThat(foundMp).as("Le jalon audience MP doit être présent et citer un article CJ").isTrue();
    }

    @Test
    void courAppelFamilleBe_delaiAppel30Jours_art1051Cj() {
        List<FamilleProcedureReferentiel.ProcedureJalon> jalons =
                FamilleProcedureReferentiel.resolve("COUR_APPEL_FAMILLE_BE", "BE");
        assertThat(jalons).hasSize(4);
        assertThat(jalons.get(0).offsetDays()).isEqualTo(30);
        assertThat(jalons.get(0).label().toLowerCase()).contains("appel");
        assertThat(jalons.get(0).articleRef()).contains("1051");
    }

    @Test
    void cassationFamilleBe_pourvoi90Jours_art1073Cj() {
        List<FamilleProcedureReferentiel.ProcedureJalon> jalons =
                FamilleProcedureReferentiel.resolve("CASSATION_FAMILLE_BE", "BE");
        assertThat(jalons).hasSize(4);
        assertThat(jalons.get(0).offsetDays()).isEqualTo(90);
        assertThat(jalons.get(0).articleRef()).contains("1073");
    }

    // -------------------- Cas limites --------------------

    @Test
    void unknownType_returnsEmptyList() {
        assertThat(FamilleProcedureReferentiel.resolve("INCONNU", "BE")).isEmpty();
        assertThat(FamilleProcedureReferentiel.resolve("INCONNU", "FR")).isEmpty();
    }

    @Test
    void nullType_returnsEmptyList() {
        assertThat(FamilleProcedureReferentiel.resolve(null, "BE")).isEmpty();
    }

    @Test
    void nullCountry_returnsEmptyList() {
        assertThat(FamilleProcedureReferentiel.resolve("TRIBUNAL_FAMILLE_BE", null)).isEmpty();
    }

    @Test
    void mismatchCountryFr_returnsEmptyList() {
        // Les types BE ne doivent pas répondre quand country=FR (FR sera ouvert en SF-F-137-02).
        assertThat(FamilleProcedureReferentiel.resolve("TRIBUNAL_FAMILLE_BE", "FR")).isEmpty();
        assertThat(FamilleProcedureReferentiel.resolve("COUR_APPEL_FAMILLE_BE", "FR")).isEmpty();
        assertThat(FamilleProcedureReferentiel.resolve("CASSATION_FAMILLE_BE", "FR")).isEmpty();
    }

    @Test
    void unknownCountry_returnsEmptyList() {
        assertThat(FamilleProcedureReferentiel.resolve("TRIBUNAL_FAMILLE_BE", "DE")).isEmpty();
    }

    // -------------------- Cohérence transversale --------------------

    @Test
    void allBeTypesHaveCjArticleRef() {
        List<String> beTypes = List.of("TRIBUNAL_FAMILLE_BE", "COUR_APPEL_FAMILLE_BE", "CASSATION_FAMILLE_BE");
        for (String type : beTypes) {
            List<FamilleProcedureReferentiel.ProcedureJalon> jalons =
                    FamilleProcedureReferentiel.resolve(type, "BE");
            assertThat(jalons).as("BE type %s doit avoir au moins 1 jalon", type).isNotEmpty();
            for (FamilleProcedureReferentiel.ProcedureJalon jalon : jalons) {
                assertThat(jalon.articleRef())
                        .as("BE jalon '%s' doit citer un article CJ", jalon.label())
                        .isNotBlank()
                        .contains("CJ");
                assertThat(jalon.label()).as("Label non vide").isNotBlank();
                // offsetDays peut être 0 (dépôt requête = J0) — donc on vérifie >= 0.
                assertThat(jalon.offsetDays()).as("OffsetDays >= 0").isNotNegative();
            }
        }
    }

    @Test
    void allTypesReturnImmutableList() {
        List<FamilleProcedureReferentiel.ProcedureJalon> jalons =
                FamilleProcedureReferentiel.resolve("TRIBUNAL_FAMILLE_BE", "BE");
        // List.of(...) renvoie un ImmutableCollections — toute mutation lève UnsupportedOperationException
        assertThat(jalons).isUnmodifiable();
    }

    @Test
    void countryNormalizedToUpperCase() {
        // L'appel avec country en minuscule doit être supporté (normalisation upper).
        assertThat(FamilleProcedureReferentiel.resolve("TRIBUNAL_FAMILLE_BE", "be")).hasSize(5);
        assertThat(FamilleProcedureReferentiel.resolve("COUR_APPEL_FAMILLE_BE", "be")).hasSize(4);
    }
}
