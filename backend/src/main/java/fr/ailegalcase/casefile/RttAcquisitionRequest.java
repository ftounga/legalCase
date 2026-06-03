package fr.ailegalcase.casefile;

/**
 * SF-218-49 : requête POST pour l'outil "RTT — acquisition selon accord
 * d'aménagement" (art. L.3121-41 à L.3121-44 CT, F-DT-80). Outil <b>FRANCE
 * UNIQUEMENT</b>.
 *
 * @param horaireHebdomadaireCollectif horaire hebdomadaire fixé par l'accord
 *        d'aménagement (requis, &gt; 35 et &le; 48 ; ex. 37, 39).
 * @param accordCollectifPresent un accord d'aménagement du temps de travail sur
 *        l'année existe (requis ; à défaut → renvoi heures supplémentaires).
 * @param semainesTravailleesAn nombre de semaines effectivement travaillées dans
 *        l'année hors congés (optionnel, défaut 47 ; &gt; 0).
 */
public record RttAcquisitionRequest(
        Double horaireHebdomadaireCollectif,
        Boolean accordCollectifPresent,
        Integer semainesTravailleesAn
) {}
