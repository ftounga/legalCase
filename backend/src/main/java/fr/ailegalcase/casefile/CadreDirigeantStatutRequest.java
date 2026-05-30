package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-218-19 : requête POST pour l'analyse de qualification du cadre dirigeant
 * (art. L.3111-2 CT — 3 critères cumulatifs ; exclusion des règles de durée du
 * travail ; risque de rappel d'heures supplémentaires si la qualification est
 * écartée, F-DT-107). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param independanceEmploiDuTemps critère 1 (requis) — responsabilités
 *        impliquant une grande indépendance dans l'organisation de l'emploi du
 *        temps.
 * @param autonomieDecision critère 2 (requis) — habilitation à prendre des
 *        décisions de façon largement autonome.
 * @param remunerationParmiPlusElevees critère 3 (requis) — rémunération se
 *        situant dans les niveaux les plus élevés des systèmes de rémunération
 *        de l'entreprise.
 * @param participationDirectionEntreprise indice complémentaire (défaut false) —
 *        participation effective à la direction de l'entreprise (Cass. soc.
 *        post-2012).
 * @param niveauRemunerationConstate rémunération mensuelle constatée (optionnel,
 *        ≥ 0 ; élément de preuve, non décisif seul).
 */
public record CadreDirigeantStatutRequest(
        Boolean independanceEmploiDuTemps,
        Boolean autonomieDecision,
        Boolean remunerationParmiPlusElevees,
        Boolean participationDirectionEntreprise,
        BigDecimal niveauRemunerationConstate
) {}
