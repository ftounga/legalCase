package fr.ailegalcase.casefile;

/**
 * F-283 / SF-283-01 — référentiel des phases procédurales d'un dossier.
 * Une phase = un stade du cycle de vie juridictionnel (distinct du round
 * d'échange F-282 et du libellé « stade courant » statique F-243).
 *
 * <p>Transversal (FR+BE, 3 domaines) : un libellé de phase = un stade procédural
 * daté, sans règle métier domaine-spécifique. L'{@code order} pilote le tri
 * secondaire de la frise (le tri primaire reste la date d'entrée).
 */
public enum CasePhaseType {

    SAISINE(1),
    CONCILIATION(2),
    MISE_EN_ETAT(3),
    FOND(4),
    JUGEMENT(5),
    APPEL(6),
    CASSATION(7),
    EXECUTION(8);

    private final int order;

    CasePhaseType(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }
}
