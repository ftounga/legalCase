package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-218-19 : résultat interne business de l'analyse de qualification du cadre
 * dirigeant (art. L.3111-2 CT — 3 critères cumulatifs). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * @param independanceEmploiDuTemps critère 1 — grande indépendance dans
 *        l'organisation de l'emploi du temps.
 * @param autonomieDecision critère 2 — habilitation à prendre des décisions de
 *        façon largement autonome.
 * @param remunerationParmiPlusElevees critère 3 — rémunération se situant dans
 *        les niveaux les plus élevés de l'entreprise.
 * @param participationDirectionEntreprise indice complémentaire — participation
 *        effective à la direction de l'entreprise (exigence jurisprudentielle).
 * @param niveauRemunerationConstate rémunération mensuelle constatée (élément de
 *        preuve, non décisif seul ; null si non renseignée).
 * @param criteres évaluation détaillée des 3 critères cumulatifs.
 * @param criteresRemplis nombre de critères légaux remplis (0..3).
 * @param qualification verdict de qualification.
 * @param exclusionDureeTravail conséquence sur l'application des règles de durée
 *        du travail.
 * @param risqueRappelHeuresSupp niveau de risque de rappel d'heures
 *        supplémentaires.
 * @param baseJuridique fondements juridiques applicables.
 */
public record CadreDirigeantStatutResult(
        boolean independanceEmploiDuTemps,
        boolean autonomieDecision,
        boolean remunerationParmiPlusElevees,
        boolean participationDirectionEntreprise,
        BigDecimal niveauRemunerationConstate,
        List<CadreDirigeantCritere> criteres,
        int criteresRemplis,
        CadreDirigeantQualification qualification,
        CadreDirigeantExclusionDureeTravail exclusionDureeTravail,
        CadreDirigeantRisqueRappelHeuresSupp risqueRappelHeuresSupp,
        String baseJuridique
) {}
