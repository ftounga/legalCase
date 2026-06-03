package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-218-41 : résultat interne business de l'analyse de conformité aux
 * obligations d'épargne salariale (intéressement / participation / dispositif de
 * partage de la valeur — F-DT-53). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param effectif effectif de l'entreprise.
 * @param accordParticipationPresent présence d'un accord de participation.
 * @param accordInteressementPresent présence d'un accord d'intéressement.
 * @param beneficeNetFiscalPositif3Ans bénéfice net fiscal au moins égal à 1 % du
 *        CA sur les 3 derniers exercices.
 * @param entreprise11a49 entreprise de 11 à 49 salariés.
 * @param checklist liste des items de conformité.
 * @param obligationsApplicables nombre d'obligations légales applicables.
 * @param obligationsNonRemplies nombre d'obligations applicables non remplies.
 * @param statut verdict de conformité.
 * @param notes notes / points de vigilance.
 * @param baseJuridique fondements juridiques applicables.
 */
public record EpargneSalarialeConformiteResult(
        int effectif,
        boolean accordParticipationPresent,
        boolean accordInteressementPresent,
        boolean beneficeNetFiscalPositif3Ans,
        boolean entreprise11a49,
        List<EpargneSalarialeConformiteItem> checklist,
        int obligationsApplicables,
        int obligationsNonRemplies,
        EpargneSalarialeConformiteStatut statut,
        List<String> notes,
        String baseJuridique
) {}
