package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-218-39 : résultat interne business de l'analyse d'exonération de la prime de
 * partage de la valeur (PPV — loi n° 2022-1158 du 16/08/2022 art. 1 + loi
 * n° 2023-1107 du 29/11/2023, F-DT-52). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param montantPrime montant de la PPV versée sur l'année civile.
 * @param accordInteressementPresent présence d'un accord d'intéressement.
 * @param remunerationAnnuelleBrute rémunération annuelle brute du bénéficiaire.
 * @param effectifMoins50 entreprise de moins de 50 salariés.
 * @param versementPlanEpargne affectation à un plan d'épargne salariale.
 * @param plafondSocialApplique plafond d'exonération sociale retenu (3 000 € ou
 *        6 000 €).
 * @param montantExonere fraction exonérée de cotisations sociales (≤ plafond).
 * @param montantImposable fraction excédentaire réintégrée (0 si conforme).
 * @param exonerationFiscaleIr true si la part exonérée socialement est également
 *        exonérée d'IR (effectif &lt; 50 + rémunération &lt; 3 SMIC).
 * @param statut verdict de conformité au plafond.
 * @param notes notes / points de vigilance identifiés.
 * @param baseJuridique fondements juridiques applicables.
 */
public record PpvExonerationResult(
        BigDecimal montantPrime,
        boolean accordInteressementPresent,
        BigDecimal remunerationAnnuelleBrute,
        boolean effectifMoins50,
        boolean versementPlanEpargne,
        BigDecimal plafondSocialApplique,
        BigDecimal montantExonere,
        BigDecimal montantImposable,
        boolean exonerationFiscaleIr,
        PpvExonerationStatut statut,
        List<String> notes,
        String baseJuridique
) {}
