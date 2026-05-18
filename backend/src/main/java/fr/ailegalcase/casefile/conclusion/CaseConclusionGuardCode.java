package fr.ailegalcase.casefile.conclusion;

/**
 * F-98 / SF-98-01 — codes de garde 409 du déclenchement de génération de conclusions.
 * Sérialisés tels quels dans le champ {@code error} de la réponse 409 (contrat API figé).
 */
public enum CaseConclusionGuardCode {

    /** Stade procédural du dossier incomplet (juridiction, stade ou position null). */
    STAGE_NOT_SET("Renseignez le stade procédural du dossier (onglet Dossier) "
            + "avant de générer les conclusions."),

    /** Aucune analyse de dossier au statut DONE. */
    ANALYSIS_NOT_READY("Lancez et terminez l'analyse du dossier avant de générer "
            + "les conclusions."),

    /** Combinaison procédurale (juridiction/stade/position) non couverte par une cellule de la matrice F-98. */
    COMBINATION_NOT_SUPPORTED("La génération de conclusions n'est pas encore disponible pour la "
            + "combinaison procédurale de ce dossier (juridiction, stade, position)."),

    /** Génération déjà PENDING ou PROCESSING pour ce dossier. */
    ALREADY_GENERATING("Une génération est déjà en cours pour ce dossier."),

    /** Passage à VALIDATED/DEPOSITED d'une version dont la génération n'est pas DONE. */
    LIFECYCLE_REQUIRES_DONE("Seule une version générée peut être validée ou déposée."),

    /** Édition du contenu d'une version dont la génération n'est pas DONE (SF-98-49). */
    CONTENT_REQUIRES_DONE("La version n'est pas encore générée."),

    /** Édition du contenu d'une version VALIDATED/DEPOSITED — seul un brouillon est modifiable (SF-98-49). */
    CONTENT_NOT_EDITABLE("Seul un brouillon peut être modifié.");

    private final String message;

    CaseConclusionGuardCode(String message) {
        this.message = message;
    }

    /** @return le message destiné à l'avocat (français, prêt à afficher). */
    public String message() {
        return message;
    }
}
