package fr.ailegalcase.casefile;

/**
 * SF-213-06 : item d'une checklist de renonciations expresses dans une
 * transaction de fin de contrat en droit belge.
 *
 * <p>Chaque item correspond à une entrée de la liste
 * {@code renonciationsListees} passée en input. La V1 ne fait pas
 * d'analyse fine clause par clause : tous les items sont marqués
 * {@code valide=true} sauf si {@code renonciationOrdrePublicDetectee=true}
 * au niveau global (auquel cas tous les items sont {@code valide=false} —
 * stratégie conservatrice signalant que la transaction comporte au moins
 * une renonciation à un droit d'ordre public).</p>
 *
 * <p>L'analyse détaillée du contenu des clauses (lecture sémantique du
 * document) est explicitement <b>hors scope SF-213-06</b> selon la
 * mini-spec — sera couverte par un outil ultérieur basé sur la
 * cartographie des dispositions impératives de la Loi 03/07/1978.</p>
 *
 * @param item    libellé de la renonciation (ex. « Renonciation à l'action
 *                en indemnité de rupture »)
 * @param valide  {@code true} si la renonciation est conforme au régime
 *                BE (renonciation à un droit acquis disponible) ;
 *                {@code false} si elle est suspectée de porter sur un
 *                droit d'ordre public
 */
public record TransactionBeTravailChecklistItem(String item, boolean valide) {
}
