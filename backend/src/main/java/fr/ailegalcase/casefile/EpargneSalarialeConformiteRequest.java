package fr.ailegalcase.casefile;

/**
 * SF-218-41 : requête POST pour l'analyse de conformité aux obligations
 * d'épargne salariale (intéressement / participation / dispositif de partage de
 * la valeur — F-DT-53). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param effectif effectif de l'entreprise (requis, &gt; 0).
 * @param accordParticipationPresent true si un accord de participation est en
 *        place (requis).
 * @param accordInteressementPresent true si un accord d'intéressement est en
 *        place (requis).
 * @param beneficeNetFiscalPositif3Ans true si le bénéfice net fiscal est au moins
 *        égal à 1 % du chiffre d'affaires sur les 3 derniers exercices (requis).
 * @param entreprise11a49 true si l'entreprise compte entre 11 et 49 salariés
 *        (requis).
 */
public record EpargneSalarialeConformiteRequest(
        Integer effectif,
        Boolean accordParticipationPresent,
        Boolean accordInteressementPresent,
        Boolean beneficeNetFiscalPositif3Ans,
        Boolean entreprise11a49
) {}
