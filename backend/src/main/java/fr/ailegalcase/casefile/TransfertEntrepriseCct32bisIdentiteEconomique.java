package fr.ailegalcase.casefile;

/**
 * SF-219-08 : appréciation de la préservation de l'<b>identité économique</b>
 * de l'entité transférée — critère central de la CCT 32bis (transposition
 * Directive 2001/23/CE, art. 1).
 *
 * <p>L'entité économique est définie comme un ensemble organisé de
 * personnes et d'éléments permettant l'exercice d'une activité économique
 * poursuivant un objectif propre. Elle doit <b>garder son identité</b>
 * après le transfert (continuité d'activité, reprise d'éléments
 * essentiels : clientèle, savoir-faire, organisation, parfois personnel).</p>
 *
 * <p>Jurisprudence de référence : CJUE 11/03/1997 <i>Süzen</i>, CJUE
 * 20/11/2003 <i>Abler</i>, Cass. BE 17/05/1999, Cass. BE 14/02/2011.</p>
 */
public enum TransfertEntrepriseCct32bisIdentiteEconomique {
    /** Identité économique manifestement préservée (reprise complète d'activité). */
    PRESERVEE,

    /** Identité économique non préservée (simple cession d'actifs isolés). */
    NON_PRESERVEE,

    /** Appréciation ambiguë — intervention avocat BE requise. */
    A_ANALYSER
}
