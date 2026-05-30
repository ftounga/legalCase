package fr.ailegalcase.casefile;

/**
 * SF-218-09 : item de la checklist procédurale de l'action de groupe en
 * discrimination au travail (art. L. 1134-7 à L. 1134-10 Code travail).
 *
 * @param libelle libellé de la condition / formalité.
 * @param obligatoire true si la condition conditionne la recevabilité de
 *        l'action de groupe.
 * @param bloquant true si la condition est en défaut au point de bloquer la
 *        recevabilité (ex. mise en demeure absente, organisation non habilitée).
 * @param baseJuridique fondement textuel de la condition.
 */
public record ActionGroupeDiscriminationChecklistItem(
        String libelle,
        boolean obligatoire,
        boolean bloquant,
        String baseJuridique
) {}
