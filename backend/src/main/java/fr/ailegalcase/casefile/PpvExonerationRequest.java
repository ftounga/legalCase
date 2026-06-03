package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-218-39 : requête POST pour l'analyse d'exonération de la prime de partage de
 * la valeur (PPV — loi n° 2022-1158 du 16/08/2022 art. 1 + loi n° 2023-1107 du
 * 29/11/2023, F-DT-52). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param montantPrime montant de la PPV versée au bénéficiaire sur l'année civile
 *        (requis, &gt; 0).
 * @param accordInteressementPresent true si un accord d'intéressement existe dans
 *        l'entreprise (porte le plafond social à 6 000 €) (requis).
 * @param remunerationAnnuelleBrute rémunération annuelle brute du bénéficiaire,
 *        base du test « &lt; 3 SMIC » pour l'exonération fiscale IR (requis,
 *        &gt; 0).
 * @param effectifMoins50 true si l'entreprise compte moins de 50 salariés
 *        (condition de l'exonération fiscale IR jusqu'au 31/12/2026) (requis).
 * @param versementPlanEpargne true si la prime (ou une partie) a été affectée à
 *        un plan d'épargne salariale (optionnel, défaut false).
 */
public record PpvExonerationRequest(
        BigDecimal montantPrime,
        Boolean accordInteressementPresent,
        BigDecimal remunerationAnnuelleBrute,
        Boolean effectifMoins50,
        Boolean versementPlanEpargne
) {}
