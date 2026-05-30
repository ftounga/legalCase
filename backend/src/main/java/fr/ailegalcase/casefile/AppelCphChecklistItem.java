package fr.ailegalcase.casefile;

/**
 * SF-218-01 : item de la checklist des formalités de l'appel d'un jugement CPH
 * devant la chambre sociale de la Cour d'appel.
 *
 * @param libelle libellé de la formalité (déclaration RPVA, chefs critiqués…).
 * @param obligatoire true si la formalité conditionne la recevabilité ou la
 *        régularité de l'appel.
 * @param bloquant true si l'item est en défaut au point de bloquer la procédure
 *        (ex. aucune représentation constituée alors qu'elle est obligatoire).
 * @param baseJuridique fondement textuel de la formalité.
 */
public record AppelCphChecklistItem(
        String libelle,
        boolean obligatoire,
        boolean bloquant,
        String baseJuridique
) {}
