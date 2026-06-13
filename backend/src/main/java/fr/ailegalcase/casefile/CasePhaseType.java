package fr.ailegalcase.casefile;

/**
 * F-283 / SF-283-01 — référentiel des phases procédurales d'un dossier.
 * Une phase = un stade du cycle de vie juridictionnel (distinct du round
 * d'échange F-282 et du libellé « stade courant » statique F-243).
 *
 * <p>Transversal (FR+BE, 3 domaines) : un libellé de phase = un stade procédural
 * daté, sans règle métier domaine-spécifique. L'{@code order} pilote le tri
 * secondaire de la frise (le tri primaire reste la date d'entrée).
 *
 * <p>SF-283-03 — étendu pour couvrir la procédure administrative (immigration)
 * et la Belgique. Les valeurs sont AJOUTÉES sans toucher aux historiques
 * (rétro-compat) ; chacune fait ≤ 30 caractères (colonne {@code phase} varchar(30),
 * donc aucune migration). Le {@code displayLabel} est un fallback générique par
 * type quand le catalogue (domaine × pays) ne fournit pas de libellé.
 */
public enum CasePhaseType {

    // ── Phases civiles / communes (historiques) ──────────────────────────────
    SAISINE(1, "Saisine"),
    INTRODUCTION(1, "Introduction"),
    RECOURS_PREALABLE(1, "Recours préalable"),
    CONCILIATION(2, "Conciliation"),
    MISE_EN_ETAT(3, "Mise en état"),
    FOND(4, "Jugement au fond"),
    JUGEMENT(5, "Jugement rendu"),
    APPEL(6, "Appel"),
    CASSATION(7, "Cassation"),

    // ── Phases administratives (immigration) ─────────────────────────────────
    TRIBUNAL_ADMINISTRATIF(2, "Tribunal administratif"),
    CNDA(3, "Cour nationale du droit d'asile"),
    CCE(3, "Conseil du Contentieux des Étrangers"),
    COUR_ADMINISTRATIVE_APPEL(6, "Cour administrative d'appel"),
    CONSEIL_ETAT(7, "Conseil d'État"),

    // ── Phase terminale commune ──────────────────────────────────────────────
    EXECUTION(8, "Exécution");

    private final int order;
    private final String displayLabel;

    CasePhaseType(int order, String displayLabel) {
        this.order = order;
        this.displayLabel = displayLabel;
    }

    public int getOrder() {
        return order;
    }

    /** Libellé d'affichage générique par défaut (fallback hors catalogue). */
    public String getDisplayLabel() {
        return displayLabel;
    }
}
