package fr.ailegalcase.casefile;

/**
 * SF-218-03 : item de la checklist des démarches d'exécution forcée d'un
 * jugement CPH (signification, commandement de payer, mandatement huissier,
 * mesures conservatoires, déclaration de créance AGS). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * @param libelle libellé de la démarche.
 * @param obligatoire true si la démarche conditionne la régularité de
 *        l'exécution forcée (ex. signification préalable obligatoire).
 * @param bloquant true si l'item est en défaut au point de bloquer l'exécution
 *        (ex. date d'ouverture de la procédure collective manquante).
 * @param baseJuridique fondement textuel de la démarche.
 */
public record ExecutionJugementCphChecklistItem(
        String libelle,
        boolean obligatoire,
        boolean bloquant,
        String baseJuridique
) {}
