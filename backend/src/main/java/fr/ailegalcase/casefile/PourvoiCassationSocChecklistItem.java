package fr.ailegalcase.casefile;

/**
 * SF-218-05 : item de la checklist des démarches du pourvoi en cassation
 * sociale (constitution d'un avocat aux Conseils, déclaration de pourvoi,
 * mémoire ampliatif, etc.). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param libelle libellé de la démarche.
 * @param obligatoire true si la démarche conditionne la régularité du pourvoi
 *        (ex. constitution d'un avocat aux Conseils — art. 973 CPC).
 * @param bloquant true si l'item est en défaut au point de bloquer le pourvoi
 *        (ex. absence d'avocat aux Conseils constitué).
 * @param baseJuridique fondement textuel de la démarche.
 */
public record PourvoiCassationSocChecklistItem(
        String libelle,
        boolean obligatoire,
        boolean bloquant,
        String baseJuridique
) {}
