package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-62 (2026-06-11) — garde-fou d'intégrité de la matrice de conclusions.
 *
 * <p><strong>Origine du bug</strong> : signal terrain (dossier STANOJEVIC, Me Renversez).
 * Le stade {@code BCO} (Bureau de Conciliation et d'Orientation) était proposé par le
 * catalogue procédural F-243 ({@link ProcedureStageCatalog}) mais aucune cellule
 * {@link ConclusionPromptProvider} ne le couvrait — la garde
 * {@code COMBINATION_NOT_SUPPORTED} bloquait alors la génération sans message clair
 * (« l'outil ne me permet pas de générer »). Le trou était invisible car
 * <strong>aucun test ne croisait les combinaisons sélectionnables avec les cellules
 * génératrices</strong> : un bug d'espace négatif (combinaison absente).</p>
 *
 * <p><strong>Principe</strong> : pour chaque combinaison procédurale
 * <em>sélectionnable</em> par l'utilisateur ({@link ProcedureStageCatalog#allSelectableCombinations()}),
 * il doit exister <strong>soit</strong> une cellule de génération dans le registre F-98,
 * <strong>soit</strong> une entrée explicite et justifiée de {@link #KNOWN_UNCOVERED_COMBINATIONS}.
 * Symétriquement, aucune cellule ne doit cibler une combinaison que le catalogue n'offre pas
 * (cellule morte). Ce test échoue dès qu'un futur stade ajouté au catalogue recrée un
 * blocage muet.</p>
 */
@SpringBootTest
class ConclusionCombinationCoverageIT {

    /**
     * Combinaisons sélectionnables volontairement <strong>non</strong> couvertes par une
     * cellule de génération, avec justification. Format
     * {@code domain|country|jurisdiction|stage|position}.
     *
     * <p>Vide à ce jour : la matrice F-98 couvre l'intégralité des combinaisons offertes par
     * le catalogue F-243 (55/55 après l'ajout du stade BCO FR — SF-98-62/63). Toute entrée
     * ajoutée ici doit être accompagnée d'un commentaire expliquant pourquoi la combinaison
     * est offerte sans être génératrice (et, idéalement, d'un message utilisateur explicite).</p>
     */
    private static final Set<String> KNOWN_UNCOVERED_COMBINATIONS = Set.of();

    @Autowired
    private ConclusionPromptRegistry registry;

    @Test
    void chaque_combinaison_selectionnable_a_une_cellule_ou_est_en_liste_blanche() {
        Set<String> selectable = ProcedureStageCatalog.allSelectableCombinations();
        Set<String> registered = registry.registeredCombinations().stream()
                .map(ConclusionCombinationCoverageIT::encode)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> uncovered = new LinkedHashSet<>(selectable);
        uncovered.removeAll(registered);
        uncovered.removeAll(KNOWN_UNCOVERED_COMBINATIONS);

        assertThat(uncovered)
                .as("Combinaison(s) procédurale(s) sélectionnable(s) sans cellule de génération "
                        + "F-98 ni entrée KNOWN_UNCOVERED_COMBINATIONS — risque de blocage muet à la "
                        + "génération de conclusions. Ajouter une cellule ConclusionPromptProvider ou "
                        + "documenter la combinaison en liste blanche.")
                .isEmpty();
    }

    @Test
    void aucune_cellule_ne_cible_une_combinaison_non_selectionnable() {
        Set<String> selectable = ProcedureStageCatalog.allSelectableCombinations();
        Set<String> orphans = registry.registeredCombinations().stream()
                .map(ConclusionCombinationCoverageIT::encode)
                .filter(c -> !selectable.contains(c))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        assertThat(orphans)
                .as("Cellule(s) de génération F-98 ciblant une combinaison que le catalogue F-243 "
                        + "n'offre pas (cellule morte, jamais atteignable).")
                .isEmpty();
    }

    private static String encode(CombinationKey key) {
        return String.join("|", key.domain(), key.country(), key.jurisdiction(),
                key.stage(), key.position());
    }
}
