package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-35 : tests unitaires de {@link ReglementInterieurValiditeAnalyzer}
 * (F-DT-100, outil FRANCE uniquement).
 *
 * <p>Logique déterministe (art. L.1311-1 à L.1322-4, L.1321-1 et s. CT) :
 * <ul>
 *   <li>effectif &lt; 50 et pas de RI → NON_REQUIS ;</li>
 *   <li>RI complet + 0 clause interdite + procédure complète → CONFORME /
 *       OPPOSABLE ;</li>
 *   <li>contenu obligatoire manquant → NON_CONFORME + itemsObligatoiresManquants ;</li>
 *   <li>clause interdite présente → NON_CONFORME + clausesInterditesPresentes ;</li>
 *   <li>défaut de procédure (consultation CSE, dépôt) → INOPPOSABLE / INOPPOSABLE
 *       (prime sur le fond) ;</li>
 *   <li>booléen requis null / effectif ≤ 0 → IllegalArgument.</li>
 * </ul>
 */
class ReglementInterieurValiditeAnalyzerTest {

    /** Cas complet conforme : RI complet, aucune clause interdite, procédure complète. */
    private static ReglementInterieurValiditeResult analyzeConforme() {
        return ReglementInterieurValiditeAnalyzer.analyze(
                80, true,
                true, true, true, true,
                false, false,
                true, true, true);
    }

    @Test
    void riComplet_procedureComplete_conforme_opposable() {
        ReglementInterieurValiditeResult r = analyzeConforme();

        assertThat(r.statut()).isEqualTo(ReglementInterieurValiditeStatut.CONFORME);
        assertThat(r.opposabilite()).isEqualTo(ReglementInterieurOpposabilite.OPPOSABLE);
        assertThat(r.itemsObligatoiresManquants()).isZero();
        assertThat(r.clausesInterditesPresentes()).isZero();
        // 4 OBLIGATOIRE + 2 INTERDIT + 3 PROCEDURE
        assertThat(r.checklist()).hasSize(9);
        assertThat(r.baseJuridique()).contains("L.1311-2").contains("L.1321-1").contains("L.1321-4");
    }

    @Test
    void contenuHarcelementManquant_nonConforme_unItemManquant() {
        ReglementInterieurValiditeResult r = ReglementInterieurValiditeAnalyzer.analyze(
                80, true,
                true, true, true, false,
                false, false,
                true, true, true);

        assertThat(r.statut()).isEqualTo(ReglementInterieurValiditeStatut.NON_CONFORME);
        assertThat(r.opposabilite()).isEqualTo(ReglementInterieurOpposabilite.OPPOSABLE);
        assertThat(r.itemsObligatoiresManquants()).isEqualTo(1);
        assertThat(r.clausesInterditesPresentes()).isZero();
    }

    @Test
    void clauseSanctionPecuniairePresente_nonConforme_uneClauseInterdite() {
        ReglementInterieurValiditeResult r = ReglementInterieurValiditeAnalyzer.analyze(
                80, true,
                true, true, true, true,
                false, true,
                true, true, true);

        assertThat(r.statut()).isEqualTo(ReglementInterieurValiditeStatut.NON_CONFORME);
        assertThat(r.clausesInterditesPresentes()).isEqualTo(1);
        // l'item INTERDIT correspondant doit être non conforme (présence de la clause)
        assertThat(r.checklist())
                .filteredOn(i -> i.type() == ReglementInterieurChecklistType.INTERDIT)
                .anySatisfy(i -> assertThat(i.conforme()).isFalse());
    }

    @Test
    void clauseAtteinteLibertesPresente_nonConforme_uneClauseInterdite() {
        ReglementInterieurValiditeResult r = ReglementInterieurValiditeAnalyzer.analyze(
                80, true,
                true, true, true, true,
                true, false,
                true, true, true);

        assertThat(r.statut()).isEqualTo(ReglementInterieurValiditeStatut.NON_CONFORME);
        assertThat(r.clausesInterditesPresentes()).isEqualTo(1);
    }

    @Test
    void defautConsultationCse_inopposable() {
        ReglementInterieurValiditeResult r = ReglementInterieurValiditeAnalyzer.analyze(
                80, true,
                true, true, true, true,
                false, false,
                false, true, true);

        assertThat(r.statut()).isEqualTo(ReglementInterieurValiditeStatut.INOPPOSABLE);
        assertThat(r.opposabilite()).isEqualTo(ReglementInterieurOpposabilite.INOPPOSABLE);
        assertThat(r.consequences()).anySatisfy(c -> assertThat(c).contains("L.1321-4"));
    }

    @Test
    void defautDepotGreffe_inopposable() {
        ReglementInterieurValiditeResult r = ReglementInterieurValiditeAnalyzer.analyze(
                80, true,
                true, true, true, true,
                false, false,
                true, true, false);

        assertThat(r.statut()).isEqualTo(ReglementInterieurValiditeStatut.INOPPOSABLE);
        assertThat(r.opposabilite()).isEqualTo(ReglementInterieurOpposabilite.INOPPOSABLE);
    }

    @Test
    void defautProcedure_primeSurContenuNonConforme_inopposable() {
        // contenu manquant ET procédure défaillante → INOPPOSABLE prime
        ReglementInterieurValiditeResult r = ReglementInterieurValiditeAnalyzer.analyze(
                80, true,
                false, true, true, true,
                false, false,
                false, true, true);

        assertThat(r.statut()).isEqualTo(ReglementInterieurValiditeStatut.INOPPOSABLE);
        assertThat(r.opposabilite()).isEqualTo(ReglementInterieurOpposabilite.INOPPOSABLE);
        // les comptages restent renseignés pour information
        assertThat(r.itemsObligatoiresManquants()).isEqualTo(1);
    }

    @Test
    void effectifInferieur50_sansReglement_nonRequis() {
        ReglementInterieurValiditeResult r = ReglementInterieurValiditeAnalyzer.analyze(
                20, false,
                false, false, false, false,
                false, false,
                false, false, false);

        assertThat(r.statut()).isEqualTo(ReglementInterieurValiditeStatut.NON_REQUIS);
        assertThat(r.opposabilite()).isEqualTo(ReglementInterieurOpposabilite.OPPOSABLE);
        assertThat(r.itemsObligatoiresManquants()).isZero();
        assertThat(r.clausesInterditesPresentes()).isZero();
        assertThat(r.checklist()).isEmpty();
    }

    @Test
    void effectifInferieur50_avecReglement_evalueAuFond() {
        // RI facultatif mais existant → on l'évalue (ici contenu/procédure OK → CONFORME)
        ReglementInterieurValiditeResult r = ReglementInterieurValiditeAnalyzer.analyze(
                20, true,
                true, true, true, true,
                false, false,
                true, true, true);

        assertThat(r.statut()).isEqualTo(ReglementInterieurValiditeStatut.CONFORME);
        assertThat(r.checklist()).hasSize(9);
    }

    @Test
    void booleanRequisNull_illegalArgument() {
        assertThatThrownBy(() -> ReglementInterieurValiditeAnalyzer.analyze(
                80, null,
                true, true, true, true,
                false, false,
                true, true, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void effectifNul_illegalArgument() {
        assertThatThrownBy(() -> ReglementInterieurValiditeAnalyzer.analyze(
                0, true,
                true, true, true, true,
                false, false,
                true, true, true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
