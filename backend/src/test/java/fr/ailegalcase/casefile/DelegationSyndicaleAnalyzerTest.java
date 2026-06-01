package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-33 : tests unitaires de {@link DelegationSyndicaleAnalyzer}
 * (F-DT-69, outil FRANCE uniquement).
 *
 * <p>Logique déterministe (art. L.2143-1 et s., L.2142-1-1, L.2143-3,
 * L.2411-3 CT) :
 * <ul>
 *   <li>DS effectif ≥ 50 + représentatif + score ≥ 10 → REGULIERE ;</li>
 *   <li>DS effectif &lt; 50 → IRREGULIERE ;</li>
 *   <li>DS non représentatif → IRREGULIERE ;</li>
 *   <li>DS sans score → A_VERIFIER ;</li>
 *   <li>RSS non représentatif → REGULIERE (pas de seuil de score) ;</li>
 *   <li>protection : licenciement sans autorisation → ELEVE, avec → FAIBLE,
 *       pas de licenciement → SANS_OBJET ;</li>
 *   <li>statutProtege toujours OUI.</li>
 * </ul>
 */
class DelegationSyndicaleAnalyzerTest {

    private static final BigDecimal SCORE_15 = BigDecimal.valueOf(15);

    @Test
    void ds_effectifSuffisant_representatif_scoreOk_reguliere() {
        DelegationSyndicaleResult r = DelegationSyndicaleAnalyzer.analyze(
                80, MandatSyndicalType.DELEGUE_SYNDICAL, true, SCORE_15,
                null, false, false);

        assertThat(r.statutDesignation()).isEqualTo(DelegationSyndicaleStatutDesignation.REGULIERE);
        assertThat(r.statutProtege()).isEqualTo(DelegationSyndicaleStatutProtege.OUI);
        assertThat(r.risqueNulliteLicenciement()).isEqualTo(DelegationSyndicaleRisqueNullite.SANS_OBJET);
        assertThat(r.checklist()).hasSize(3).allSatisfy(i -> assertThat(i.conforme()).isTrue());
        assertThat(r.baseJuridique()).contains("L.2143-1").contains("L.2411-3");
    }

    @Test
    void ds_effectifInsuffisant_irreguliere() {
        DelegationSyndicaleResult r = DelegationSyndicaleAnalyzer.analyze(
                30, MandatSyndicalType.DELEGUE_SYNDICAL, true, SCORE_15,
                null, false, false);

        assertThat(r.statutDesignation()).isEqualTo(DelegationSyndicaleStatutDesignation.IRREGULIERE);
        assertThat(r.checklist())
                .anySatisfy(i -> {
                    assertThat(i.item()).contains("Effectif");
                    assertThat(i.conforme()).isFalse();
                });
        assertThat(r.consequences()).anySatisfy(c -> assertThat(c).contains("L.2143-3"));
    }

    @Test
    void ds_nonRepresentatif_irreguliere() {
        DelegationSyndicaleResult r = DelegationSyndicaleAnalyzer.analyze(
                80, MandatSyndicalType.DELEGUE_SYNDICAL, false, SCORE_15,
                null, false, false);

        assertThat(r.statutDesignation()).isEqualTo(DelegationSyndicaleStatutDesignation.IRREGULIERE);
        assertThat(r.checklist())
                .anySatisfy(i -> {
                    assertThat(i.item()).contains("représentative");
                    assertThat(i.conforme()).isFalse();
                });
    }

    @Test
    void ds_sansScore_aVerifier() {
        DelegationSyndicaleResult r = DelegationSyndicaleAnalyzer.analyze(
                80, MandatSyndicalType.DELEGUE_SYNDICAL, true, null,
                null, false, false);

        assertThat(r.statutDesignation()).isEqualTo(DelegationSyndicaleStatutDesignation.A_VERIFIER);
        assertThat(r.consequences()).anySatisfy(c -> assertThat(c).contains("score"));
    }

    @Test
    void ds_scoreInsuffisant_irreguliere() {
        DelegationSyndicaleResult r = DelegationSyndicaleAnalyzer.analyze(
                80, MandatSyndicalType.DELEGUE_SYNDICAL, true, BigDecimal.valueOf(5),
                null, false, false);

        assertThat(r.statutDesignation()).isEqualTo(DelegationSyndicaleStatutDesignation.IRREGULIERE);
    }

    @Test
    void rss_nonRepresentatif_reguliere_sansSeuilScore() {
        DelegationSyndicaleResult r = DelegationSyndicaleAnalyzer.analyze(
                12, MandatSyndicalType.RSS, false, null,
                null, false, false);

        assertThat(r.statutDesignation()).isEqualTo(DelegationSyndicaleStatutDesignation.REGULIERE);
        assertThat(r.statutProtege()).isEqualTo(DelegationSyndicaleStatutProtege.OUI);
        // RSS : pas d'item de score personnel (2 items seulement).
        assertThat(r.checklist()).hasSize(2);
    }

    @Test
    void rss_designeParSyndicatRepresentatif_irreguliere() {
        DelegationSyndicaleResult r = DelegationSyndicaleAnalyzer.analyze(
                80, MandatSyndicalType.RSS, true, null,
                null, false, false);

        assertThat(r.statutDesignation()).isEqualTo(DelegationSyndicaleStatutDesignation.IRREGULIERE);
    }

    @Test
    void protection_licenciement_eleve_faible_sansObjet() {
        // Licenciement envisagé sans autorisation → ELEVE.
        DelegationSyndicaleResult eleve = DelegationSyndicaleAnalyzer.analyze(
                80, MandatSyndicalType.DELEGUE_SYNDICAL, true, SCORE_15,
                null, true, false);
        assertThat(eleve.risqueNulliteLicenciement()).isEqualTo(DelegationSyndicaleRisqueNullite.ELEVE);
        assertThat(eleve.consequences()).anySatisfy(c -> assertThat(c).contains("L.2411-3"));

        // Licenciement envisagé avec autorisation → FAIBLE.
        DelegationSyndicaleResult faible = DelegationSyndicaleAnalyzer.analyze(
                80, MandatSyndicalType.DELEGUE_SYNDICAL, true, SCORE_15,
                null, true, true);
        assertThat(faible.risqueNulliteLicenciement()).isEqualTo(DelegationSyndicaleRisqueNullite.FAIBLE);

        // Pas de licenciement → SANS_OBJET.
        DelegationSyndicaleResult sansObjet = DelegationSyndicaleAnalyzer.analyze(
                80, MandatSyndicalType.DELEGUE_SYNDICAL, true, SCORE_15,
                null, false, false);
        assertThat(sansObjet.risqueNulliteLicenciement()).isEqualTo(DelegationSyndicaleRisqueNullite.SANS_OBJET);
    }

    @Test
    void validations_leventIllegalArgument() {
        // effectif null
        assertThatThrownBy(() -> DelegationSyndicaleAnalyzer.analyze(
                null, MandatSyndicalType.DELEGUE_SYNDICAL, true, SCORE_15, null, false, false))
                .isInstanceOf(IllegalArgumentException.class);
        // effectif 0
        assertThatThrownBy(() -> DelegationSyndicaleAnalyzer.analyze(
                0, MandatSyndicalType.DELEGUE_SYNDICAL, true, SCORE_15, null, false, false))
                .isInstanceOf(IllegalArgumentException.class);
        // typeMandat null
        assertThatThrownBy(() -> DelegationSyndicaleAnalyzer.analyze(
                80, null, true, SCORE_15, null, false, false))
                .isInstanceOf(IllegalArgumentException.class);
        // syndicatRepresentatif null
        assertThatThrownBy(() -> DelegationSyndicaleAnalyzer.analyze(
                80, MandatSyndicalType.DELEGUE_SYNDICAL, null, SCORE_15, null, false, false))
                .isInstanceOf(IllegalArgumentException.class);
        // score 150 (> 100)
        assertThatThrownBy(() -> DelegationSyndicaleAnalyzer.analyze(
                80, MandatSyndicalType.DELEGUE_SYNDICAL, true, BigDecimal.valueOf(150), null, false, false))
                .isInstanceOf(IllegalArgumentException.class);
        // score négatif
        assertThatThrownBy(() -> DelegationSyndicaleAnalyzer.analyze(
                80, MandatSyndicalType.DELEGUE_SYNDICAL, true, BigDecimal.valueOf(-1), null, false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
