package fr.ailegalcase.casefile;

import java.util.List;

/**
 * F-294 SF-294-01 — Pièce attendue (« socle ») pour une situation procédurale
 * {@code (legalDomain × country × procedureStage)}.
 *
 * <p>Modèle aligné sur {@link DivorcePiece}, enrichi de :
 * <ul>
 *   <li>{@code stages} : liste de codes de stade procédural (F-243 /
 *       {@code ProcedureStageCatalog}) pour lesquels la pièce est attendue.
 *       Liste vide / {@code null} = pièce <b>générique</b>, attendue quel que
 *       soit le stade.</li>
 *   <li>{@code ordre} : ordre d'affichage indicatif du socle.</li>
 * </ul>
 *
 * <p>Le {@code label} est le <b>libellé canonique</b> : il sert à la fois à
 * l'injection dans le prompt (réutilisation EXACTE par le LLM) et à la
 * canonisation à la matérialisation (F-194).
 */
public record ExpectedPiece(
        String code,
        String label,
        String country,
        List<String> stages,
        boolean obligatoire,
        int ordre
) {
    public ExpectedPiece {
        stages = stages == null ? List.of() : List.copyOf(stages);
    }

    /**
     * @return {@code true} si la pièce est générique (aucun stade renseigné),
     *         donc attendue quel que soit le stade procédural.
     */
    public boolean isGenerique() {
        return stages == null || stages.isEmpty();
    }
}
