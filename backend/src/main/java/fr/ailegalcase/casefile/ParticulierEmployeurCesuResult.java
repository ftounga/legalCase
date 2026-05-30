package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-218-13 : résultat interne business du calcul du préavis et de l'indemnité
 * de licenciement / de rupture d'un salarié du particulier employeur (CESU /
 * assistant maternel). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param dateEntree début du contrat.
 * @param dateRupture date de notification de la rupture.
 * @param categorieEmploye catégorie retenue (pilote la CCN).
 * @param causeRupture cause de la rupture retenue.
 * @param salaireMensuelMoyen assiette de l'indemnité (€).
 * @param ancienneteMois ancienneté en mois à la date de rupture.
 * @param dureePreavisJours durée du préavis en jours.
 * @param dureePreavisLibelle libellé lisible du préavis (« 1 semaine », « 1 mois », « 2 mois »).
 * @param eligibiliteIndemnite éligibilité à l'indemnité (DUE / NON_DUE).
 * @param motifNonDue motif lorsque l'indemnité n'est pas due (null sinon).
 * @param indemniteLicenciement montant de l'indemnité de licenciement / de rupture (€).
 * @param methodeCalcul méthode de calcul retenue.
 * @param verdict verdict global de la rupture.
 * @param baseJuridique fondements juridiques applicables.
 */
public record ParticulierEmployeurCesuResult(
        LocalDate dateEntree,
        LocalDate dateRupture,
        ParticulierEmployeurCesuCategorie categorieEmploye,
        ParticulierEmployeurCesuCause causeRupture,
        BigDecimal salaireMensuelMoyen,
        int ancienneteMois,
        int dureePreavisJours,
        String dureePreavisLibelle,
        ParticulierEmployeurCesuEligibilite eligibiliteIndemnite,
        String motifNonDue,
        BigDecimal indemniteLicenciement,
        ParticulierEmployeurCesuMethode methodeCalcul,
        ParticulierEmployeurCesuVerdict verdict,
        String baseJuridique
) {}
