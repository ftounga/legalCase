package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImmigrationTitleDecisionEngineTest {

    @Test
    void ue_courtSejour_returnsEmpty_libreCirculation() {
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "FRANCE", true, "TRAVAIL", "COURT_SEJOUR", null);
        assertThat(result).isEmpty();
    }

    @Test
    void ue_longSejour_france_returnsCarteResident() {
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "FRANCE", true, "TRAVAIL", "LONG_SEJOUR", null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("CARTE_RESIDENT");
    }

    @Test
    void ue_longSejour_belgique_returnsCarteB() {
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "BELGIQUE", true, "TRAVAIL", "LONG_SEJOUR", null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("CARTE_B");
    }

    @Test
    void horsUE_etudes_france_returnsVlsTsEtudiant() {
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "FRANCE", false, "ETUDES", "COURT_SEJOUR", null);
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).code()).isEqualTo("VLS_TS_ETUDIANT");
    }

    @Test
    void horsUE_etudes_longSejour_france_includesAPS() {
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "FRANCE", false, "ETUDES", "LONG_SEJOUR", null);
        assertThat(result).hasSizeGreaterThanOrEqualTo(2);
        assertThat(result).extracting(TitleRecommendation::code).contains("VLS_TS_ETUDIANT", "APS");
    }

    @Test
    void horsUE_travail_belgique_returnsPermisUnique() {
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "BELGIQUE", false, "TRAVAIL", "COURT_SEJOUR", null);
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).code()).isEqualTo("PERMIS_UNIQUE");
    }

    @Test
    void horsUE_travail_longSejour_belgique_includesCarteA() {
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "BELGIQUE", false, "TRAVAIL", "LONG_SEJOUR", null);
        assertThat(result).extracting(TitleRecommendation::code)
                .contains("PERMIS_UNIQUE", "CARTE_A_TRAVAIL");
    }

    @Test
    void horsUE_famille_belgique_returnsCarteAFamille() {
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "BELGIQUE", false, "FAMILLE", "COURT_SEJOUR", null);
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).code()).isEqualTo("CARTE_A_FAMILLE");
    }

    @Test
    void horsUE_famille_france_returnsCstVpf() {
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "FRANCE", false, "FAMILLE", "COURT_SEJOUR", "MARIE");
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).code()).isEqualTo("CST_VPF");
    }

    @Test
    void horsUE_asile_france_returnsRecepisse() {
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "FRANCE", false, "ASILE", "COURT_SEJOUR", null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("RECEPISSE_ASILE");
    }

    @Test
    void horsUE_asile_belgique_returnsAttestationImmatriculation() {
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "BELGIQUE", false, "ASILE", "COURT_SEJOUR", null);
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).code()).isEqualTo("ATTESTATION_IMMATRICULATION");
    }

    @Test
    void horsUE_travail_france_longSejour_returnsMultipleTitles() {
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "FRANCE", false, "TRAVAIL", "LONG_SEJOUR", null);
        assertThat(result).hasSizeGreaterThanOrEqualTo(2);
        assertThat(result.get(0).code()).isEqualTo("VLS_TS_SALARIE");
    }

    @Test
    void maxThreeResults() {
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "FRANCE", false, "TRAVAIL", "LONG_SEJOUR", null);
        assertThat(result).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    void invalidCountry_returnsEmpty() {
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "ALLEMAGNE", false, "TRAVAIL", "LONG_SEJOUR", null);
        assertThat(result).isEmpty();
    }

    @Test
    void invalidMotif_returnsEmpty() {
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "FRANCE", false, "TOURISME", "COURT_SEJOUR", null);
        assertThat(result).isEmpty();
    }

    @Test
    void allResults_haveCorrectCountry() {
        for (String country : List.of("FRANCE", "BELGIQUE")) {
            for (String motif : List.of("TRAVAIL", "ETUDES", "FAMILLE", "ASILE")) {
                List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                        country, false, motif, "LONG_SEJOUR", null);
                assertThat(result).allMatch(t -> country.equals(t.country()),
                        "All titles for %s/%s should be from %s".formatted(country, motif, country));
            }
        }
    }

    // ---- F-IM-18 SF-IM-18-01 : situation familiale branchée ----

    @Test
    void horsUE_famille_france_marie_longSejour_returnsCstVpfAndCarteResident() {
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "FRANCE", false, "FAMILLE", "LONG_SEJOUR", "MARIE");
        assertThat(result).extracting(TitleRecommendation::code)
                .containsExactly("CST_VPF", "CARTE_RESIDENT");
    }

    @Test
    void horsUE_famille_france_pacs_longSejour_returnsOnlyCstVpf() {
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "FRANCE", false, "FAMILLE", "LONG_SEJOUR", "PACS_COHABITATION");
        assertThat(result).extracting(TitleRecommendation::code)
                .containsExactly("CST_VPF");
    }

    @Test
    void horsUE_famille_france_celibataire_longSejour_returnsOnlyCstVpf() {
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "FRANCE", false, "FAMILLE", "LONG_SEJOUR", "CELIBATAIRE");
        assertThat(result).extracting(TitleRecommendation::code)
                .containsExactly("CST_VPF");
    }

    @Test
    void horsUE_famille_france_nullSituation_longSejour_returnsCstVpfAndCarteResident() {
        // Rétrocompat : null déclenche le comportement legacy.
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "FRANCE", false, "FAMILLE", "LONG_SEJOUR", null);
        assertThat(result).extracting(TitleRecommendation::code)
                .containsExactly("CST_VPF", "CARTE_RESIDENT");
    }

    @Test
    void horsUE_famille_belgique_marie_longSejour_returnsCarteAFamilleAndCarteB() {
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "BELGIQUE", false, "FAMILLE", "LONG_SEJOUR", "MARIE");
        assertThat(result).extracting(TitleRecommendation::code)
                .containsExactly("CARTE_A_FAMILLE", "CARTE_B");
    }

    @Test
    void horsUE_famille_belgique_pacs_longSejour_returnsCarteAFamilleAndCarteB() {
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "BELGIQUE", false, "FAMILLE", "LONG_SEJOUR", "PACS_COHABITATION");
        assertThat(result).extracting(TitleRecommendation::code)
                .containsExactly("CARTE_A_FAMILLE", "CARTE_B");
    }

    @Test
    void horsUE_famille_belgique_celibataire_longSejour_returnsOnlyCarteAFamille() {
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "BELGIQUE", false, "FAMILLE", "LONG_SEJOUR", "CELIBATAIRE");
        assertThat(result).extracting(TitleRecommendation::code)
                .containsExactly("CARTE_A_FAMILLE");
    }

    @Test
    void horsUE_famille_belgique_nullSituation_longSejour_returnsCarteAFamilleAndCarteB() {
        // Rétrocompat.
        List<TitleRecommendation> result = ImmigrationTitleDecisionEngine.resolve(
                "BELGIQUE", false, "FAMILLE", "LONG_SEJOUR", null);
        assertThat(result).extracting(TitleRecommendation::code)
                .containsExactly("CARTE_A_FAMILLE", "CARTE_B");
    }

    @Test
    void horsUE_travail_situationFamilialeIgnored() {
        // Hors motif FAMILLE, situationFamiliale n'a aucun effet.
        List<TitleRecommendation> withMarie = ImmigrationTitleDecisionEngine.resolve(
                "FRANCE", false, "TRAVAIL", "LONG_SEJOUR", "MARIE");
        List<TitleRecommendation> withNull = ImmigrationTitleDecisionEngine.resolve(
                "FRANCE", false, "TRAVAIL", "LONG_SEJOUR", null);
        assertThat(withMarie).extracting(TitleRecommendation::code)
                .isEqualTo(withNull.stream().map(TitleRecommendation::code).toList());
    }
}
