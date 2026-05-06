package fr.ailegalcase.analysis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-195 SF-195-01 — UT du matcher keyword-based risque → toolIds.
 *
 * <p>Pattern miroir {@link RetainedPisteToolMatcherTest} (F-192) +
 * {@link ProcedureCheckToolMatcherTest} (F-193).</p>
 */
class RisqueToolMatcherTest {

    @Test
    void harcelement_moral_mapsToHarcelementNullite() {
        assertThat(RisqueToolMatcher.resolveToolIds("Harcèlement moral subi par la salariée"))
                .containsExactly(RisqueToolMatcher.TOOL_HARCELEMENT_NULLITE);
    }

    @Test
    void harcelement_sexuel_mapsToHarcelementNullite() {
        assertThat(RisqueToolMatcher.resolveToolIds("Harcèlement sexuel — agissements de M. X"))
                .containsExactly(RisqueToolMatcher.TOOL_HARCELEMENT_NULLITE);
    }

    @Test
    void harcelement_caseInsensitive() {
        assertThat(RisqueToolMatcher.resolveToolIds("HARCELEMENT au travail"))
                .containsExactly(RisqueToolMatcher.TOOL_HARCELEMENT_NULLITE);
    }

    @Test
    void discrimination_mapsToDiscriminationAndLicenciement() {
        assertThat(RisqueToolMatcher.resolveToolIds("Discrimination liée à l'origine"))
                .containsExactlyInAnyOrder(
                        RisqueToolMatcher.TOOL_DISCRIMINATION,
                        RisqueToolMatcher.TOOL_LICENCIEMENT_VALIDITE);
    }

    @Test
    void prescription_echue_mapsToPrescription() {
        assertThat(RisqueToolMatcher.resolveToolIds("Prescription échue depuis 2024"))
                .containsExactly(RisqueToolMatcher.TOOL_PRESCRIPTION);
    }

    @Test
    void delai_forclos_mapsToPrescription() {
        assertThat(RisqueToolMatcher.resolveToolIds("Délai forclos pour saisine du conseil"))
                .containsExactly(RisqueToolMatcher.TOOL_PRESCRIPTION);
    }

    @Test
    void clause_non_concurrence_mapsToNonConcurrence() {
        assertThat(RisqueToolMatcher.resolveToolIds("Clause non-concurrence abusive"))
                .containsExactly(RisqueToolMatcher.TOOL_NON_CONCURRENCE);
    }

    @Test
    void oqtf_mapsToBothImmigrationTools() {
        assertThat(RisqueToolMatcher.resolveToolIds("OQTF imminente"))
                .containsExactlyInAnyOrder(
                        RisqueToolMatcher.TOOL_OQTF_AVEC_DELAI,
                        RisqueToolMatcher.TOOL_OQTF_SANS_DELAI);
    }

    @Test
    void expulsion_mapsToBothImmigrationTools() {
        assertThat(RisqueToolMatcher.resolveToolIds("Risque d'expulsion vers le pays d'origine"))
                .containsExactlyInAnyOrder(
                        RisqueToolMatcher.TOOL_OQTF_AVEC_DELAI,
                        RisqueToolMatcher.TOOL_OQTF_SANS_DELAI);
    }

    @Test
    void violence_intra_familiale_mapsToOrdonnanceProtection() {
        assertThat(RisqueToolMatcher.resolveToolIds("Violences intra-familiales"))
                .containsExactly(RisqueToolMatcher.TOOL_ORDONNANCE_PROTECTION);
    }

    @Test
    void violence_conjugale_mapsToOrdonnanceProtection() {
        assertThat(RisqueToolMatcher.resolveToolIds("Violences conjugales répétées"))
                .containsExactly(RisqueToolMatcher.TOOL_ORDONNANCE_PROTECTION);
    }

    @Test
    void deplacement_enfant_mapsToDeplacementEnfant() {
        assertThat(RisqueToolMatcher.resolveToolIds("Déplacement illicite d'enfant à l'étranger"))
                .containsExactly(RisqueToolMatcher.TOOL_DEPLACEMENT_ENFANT);
    }

    @Test
    void dilapidation_mapsToDilapidation() {
        assertThat(RisqueToolMatcher.resolveToolIds("Dilapidation patrimoine commun"))
                .containsExactly(RisqueToolMatcher.TOOL_DILAPIDATION);
    }

    @Test
    void noKeyword_returnsEmptyList() {
        assertThat(RisqueToolMatcher.resolveToolIds("Risque général sur le dossier")).isEmpty();
    }

    @Test
    void nullOrBlank_returnsEmptyList() {
        assertThat(RisqueToolMatcher.resolveToolIds(null)).isEmpty();
        assertThat(RisqueToolMatcher.resolveToolIds("")).isEmpty();
        assertThat(RisqueToolMatcher.resolveToolIds("   ")).isEmpty();
    }

    @Test
    void multipleKeywords_returnsAllMatchedTools() {
        // Discrimination + harcèlement → 3 toolIds (F-DT-12 + F-DT-13 + F-DT-08)
        assertThat(RisqueToolMatcher.resolveToolIds(
                "Harcèlement moral et discrimination liée au sexe"))
                .containsExactlyInAnyOrder(
                        RisqueToolMatcher.TOOL_HARCELEMENT_NULLITE,
                        RisqueToolMatcher.TOOL_DISCRIMINATION,
                        RisqueToolMatcher.TOOL_LICENCIEMENT_VALIDITE);
    }

    // ---- isCriticalKeyword ----

    @Test
    void isCriticalKeyword_harcelement_true() {
        assertThat(RisqueToolMatcher.isCriticalKeyword("Harcèlement moral")).isTrue();
    }

    @Test
    void isCriticalKeyword_violence_true() {
        assertThat(RisqueToolMatcher.isCriticalKeyword("Violences conjugales")).isTrue();
    }

    @Test
    void isCriticalKeyword_expulsion_true() {
        assertThat(RisqueToolMatcher.isCriticalKeyword("Expulsion imminente")).isTrue();
    }

    @Test
    void isCriticalKeyword_oqtf_true() {
        assertThat(RisqueToolMatcher.isCriticalKeyword("OQTF imminente")).isTrue();
    }

    @Test
    void isCriticalKeyword_dilapidation_true() {
        assertThat(RisqueToolMatcher.isCriticalKeyword("Dilapidation patrimoine")).isTrue();
    }

    @Test
    void isCriticalKeyword_discriminationDilution_false() {
        // Discrimination est sérieuse mais pas dans la liste "critical" V1
        assertThat(RisqueToolMatcher.isCriticalKeyword("Discrimination liée au sexe")).isFalse();
    }

    @Test
    void isCriticalKeyword_blank_false() {
        assertThat(RisqueToolMatcher.isCriticalKeyword(null)).isFalse();
        assertThat(RisqueToolMatcher.isCriticalKeyword("")).isFalse();
    }
}
