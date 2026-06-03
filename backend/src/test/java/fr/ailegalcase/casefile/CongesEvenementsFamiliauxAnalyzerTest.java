package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-43 : tests unitaires de {@link CongesEvenementsFamiliauxAnalyzer}
 * (F-DT-76, outil FRANCE uniquement).
 *
 * <p>Logique déterministe (art. L.3142-1 à L.3142-5 CT) :
 * <ul>
 *   <li>durées légales L.3142-4 par type d'évènement ;</li>
 *   <li>durée conventionnelle plus favorable (&gt; légale) retenue (base
 *       CONVENTIONNELLE), sinon durée légale (base LEGALE) ;</li>
 *   <li>maintien intégral du salaire (assimilation temps de travail effectif) ;</li>
 *   <li>décès d'enfant → durée majorée possible ;</li>
 *   <li>déménagement → 0 jour légal, renvoi CCN ;</li>
 *   <li>champ requis null / durée conventionnelle ≤ 0 / promesse conventionnelle
 *       sans durée → IllegalArgument.</li>
 * </ul>
 */
class CongesEvenementsFamiliauxAnalyzerTest {

    @Test
    void mariagePacs_sansConvention_4jours_legale() {
        CongesEvenementsFamiliauxResult r = CongesEvenementsFamiliauxAnalyzer.analyze(
                CongesEvenementsFamiliauxTypeEvenement.MARIAGE_PACS, false, null);

        assertThat(r.dureeLegaleJours()).isEqualTo(4);
        assertThat(r.dureeApplicableJours()).isEqualTo(4);
        assertThat(r.base()).isEqualTo(CongesEvenementsFamiliauxBase.LEGALE);
        assertThat(r.maintienSalaire()).isTrue();
        assertThat(r.assimileTempsTravailEffectif()).isTrue();
        assertThat(r.baseJuridique()).contains("L.3142-4");
    }

    @Test
    void naissance_3jours_legale() {
        CongesEvenementsFamiliauxResult r = CongesEvenementsFamiliauxAnalyzer.analyze(
                CongesEvenementsFamiliauxTypeEvenement.NAISSANCE, false, null);

        assertThat(r.dureeApplicableJours()).isEqualTo(3);
        assertThat(r.base()).isEqualTo(CongesEvenementsFamiliauxBase.LEGALE);
    }

    @Test
    void decesEnfant_5jours_dureeMajoreePossible() {
        CongesEvenementsFamiliauxResult r = CongesEvenementsFamiliauxAnalyzer.analyze(
                CongesEvenementsFamiliauxTypeEvenement.DECES_ENFANT, false, null);

        assertThat(r.dureeLegaleJours()).isEqualTo(5);
        assertThat(r.dureeApplicableJours()).isEqualTo(5);
        assertThat(r.dureeMajoreePossible()).isTrue();
        assertThat(r.notes()).anyMatch(n -> n.contains("7 jours"));
    }

    @Test
    void decesConjoint_etDecesParent_3jours() {
        assertThat(CongesEvenementsFamiliauxAnalyzer.analyze(
                CongesEvenementsFamiliauxTypeEvenement.DECES_CONJOINT_PARTENAIRE, false, null)
                .dureeApplicableJours()).isEqualTo(3);
        assertThat(CongesEvenementsFamiliauxAnalyzer.analyze(
                CongesEvenementsFamiliauxTypeEvenement.DECES_PERE_MERE, false, null)
                .dureeApplicableJours()).isEqualTo(3);
    }

    @Test
    void annonceHandicapEnfant_2jours() {
        CongesEvenementsFamiliauxResult r = CongesEvenementsFamiliauxAnalyzer.analyze(
                CongesEvenementsFamiliauxTypeEvenement.ANNONCE_HANDICAP_ENFANT, false, null);

        assertThat(r.dureeApplicableJours()).isEqualTo(2);
        assertThat(r.dureeMajoreePossible()).isFalse();
    }

    @Test
    void conventionPlusFavorable_retenue_conventionnelle() {
        CongesEvenementsFamiliauxResult r = CongesEvenementsFamiliauxAnalyzer.analyze(
                CongesEvenementsFamiliauxTypeEvenement.NAISSANCE, true, 5);

        assertThat(r.dureeLegaleJours()).isEqualTo(3);
        assertThat(r.dureeApplicableJours()).isEqualTo(5);
        assertThat(r.base()).isEqualTo(CongesEvenementsFamiliauxBase.CONVENTIONNELLE);
    }

    @Test
    void conventionMoinsFavorable_retenue_legale() {
        CongesEvenementsFamiliauxResult r = CongesEvenementsFamiliauxAnalyzer.analyze(
                CongesEvenementsFamiliauxTypeEvenement.MARIAGE_PACS, true, 3);

        assertThat(r.dureeLegaleJours()).isEqualTo(4);
        assertThat(r.dureeApplicableJours()).isEqualTo(4);
        assertThat(r.base()).isEqualTo(CongesEvenementsFamiliauxBase.LEGALE);
    }

    @Test
    void demenagement_0jourLegal_renvoiCcn() {
        CongesEvenementsFamiliauxResult r = CongesEvenementsFamiliauxAnalyzer.analyze(
                CongesEvenementsFamiliauxTypeEvenement.DEMENAGEMENT_NON_LEGAL, false, null);

        assertThat(r.dureeLegaleJours()).isZero();
        assertThat(r.dureeApplicableJours()).isZero();
        assertThat(r.base()).isEqualTo(CongesEvenementsFamiliauxBase.LEGALE);
        assertThat(r.notes()).anyMatch(n -> n.contains("aucun congé légal"));
    }

    @Test
    void demenagement_avecConventionnelle_retenue() {
        CongesEvenementsFamiliauxResult r = CongesEvenementsFamiliauxAnalyzer.analyze(
                CongesEvenementsFamiliauxTypeEvenement.DEMENAGEMENT_NON_LEGAL, true, 1);

        assertThat(r.dureeApplicableJours()).isEqualTo(1);
        assertThat(r.base()).isEqualTo(CongesEvenementsFamiliauxBase.CONVENTIONNELLE);
    }

    @Test
    void maintienSalaire_toujoursVrai() {
        for (CongesEvenementsFamiliauxTypeEvenement t : CongesEvenementsFamiliauxTypeEvenement.values()) {
            CongesEvenementsFamiliauxResult r = CongesEvenementsFamiliauxAnalyzer.analyze(t, false, null);
            assertThat(r.maintienSalaire()).isTrue();
            assertThat(r.assimileTempsTravailEffectif()).isTrue();
        }
    }

    @Test
    void typeEvenementNull_throws() {
        assertThatThrownBy(() -> CongesEvenementsFamiliauxAnalyzer.analyze(null, false, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void conventionPlusFavorableNull_throws() {
        assertThatThrownBy(() -> CongesEvenementsFamiliauxAnalyzer.analyze(
                CongesEvenementsFamiliauxTypeEvenement.MARIAGE_PACS, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void conventionPlusFavorableSansDuree_throws() {
        assertThatThrownBy(() -> CongesEvenementsFamiliauxAnalyzer.analyze(
                CongesEvenementsFamiliauxTypeEvenement.MARIAGE_PACS, true, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dureeConventionnelleNegative_throws() {
        assertThatThrownBy(() -> CongesEvenementsFamiliauxAnalyzer.analyze(
                CongesEvenementsFamiliauxTypeEvenement.MARIAGE_PACS, true, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
