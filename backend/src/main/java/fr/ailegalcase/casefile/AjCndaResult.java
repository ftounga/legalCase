package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-214-19 : résultat interne business de l'analyse d'éligibilité à l'aide
 * juridictionnelle (AJ) devant la CNDA et des délais (loi n° 91-647 du 10/07/1991 ;
 * L. 532-4 CESEDA — délai de recours CNDA 1 mois, 15 j en procédure accélérée).
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> — distinct de F-IM-12 (recours CNDA) : ici
 * l'objet est l'éligibilité à l'AJ et les délais de demande d'AJ qui doivent
 * précéder le recours.
 *
 * @param eligibleAJ true si ressourcesMensuellesNettes ≤ plafond AJ.
 * @param dateEcheanceRecoursCNDA échéance du recours CNDA (dateDecisionOFPRA + 1 mois,
 *        ou + 15 j si procédure accélérée).
 * @param dateEcheanceDemandeAJ échéance de la demande d'AJ (dateDecisionOFPRA + 15 j) —
 *        la demande d'AJ doit précéder le recours.
 * @param procedureAccelereeDureeReduite true si délai réduit (procédure accélérée).
 */
public record AjCndaResult(
        LocalDate dateDecisionOFPRA,
        double ressourcesMensuellesNettes,
        boolean procedureAcceleree,
        boolean demandeAJDeposee,
        LocalDate dateDepotAJ,
        boolean eligibleAJ,
        LocalDate dateEcheanceRecoursCNDA,
        LocalDate dateEcheanceDemandeAJ,
        boolean procedureAccelereeDureeReduite,
        AjCndaStatut statut,
        List<String> piecesAJ,
        String recommandation,
        String baseJuridique
) {}
