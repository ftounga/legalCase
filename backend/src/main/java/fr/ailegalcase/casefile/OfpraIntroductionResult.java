package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-214-17 : résultat interne business du calcul de la procédure d'introduction
 * de la demande d'asile à l'OFPRA via le GUDA / ADA — délai de 90 jours à compter
 * de l'arrivée en France (art. R. 521-1 et s. CESEDA).
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> — distinct de :
 * <ul>
 *   <li>F-IM-12 (recours CNDA / procédure accélérée Dublin existant) ;</li>
 *   <li>F-IM-22 (recours Dublin) qui traite le transfert vers l'État responsable.</li>
 * </ul>
 *
 * @param joursRestantsIntroduction jours calendaires restants avant l'échéance
 *        des 90 jours (négatif si dépassé).
 * @param procedureAccelereeRisque true si le pays d'origine figure dans la liste
 *        des pays sûrs (L. 531-24) — risque d'examen en procédure accélérée.
 */
public record OfpraIntroductionResult(
        LocalDate dateArriveeEnFrance,
        boolean passageGudaEffectue,
        LocalDate datePassageGuda,
        boolean adaRequise,
        String paysOrigine,
        LocalDate dateEcheanceIntroduction,
        long joursRestantsIntroduction,
        OfpraIntroductionStatut statutDelai,
        boolean procedureAccelereeRisque,
        List<String> etapesAPrendre,
        List<String> piecesRequises,
        String recommandation,
        String baseJuridique
) {}
