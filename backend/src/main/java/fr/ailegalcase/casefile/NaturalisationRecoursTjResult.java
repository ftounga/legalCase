package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-214-29 : résultat interne business du calcul du délai de recours devant le
 * Tribunal judiciaire contre un refus de déclaration de nationalité française —
 * 6 mois à compter du refus (Cciv 26-3).
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> — distinct de :
 * <ul>
 *   <li>SF-214-31 (recours décret de naturalisation devant le TA de Nantes,
 *       voie DECRET_21_15) ;</li>
 *   <li>F-IM-13 (checklist demande de nationalité).</li>
 * </ul>
 *
 * @param joursRestants jours calendaires restants avant l'échéance (négatif si
 *        dépassé).
 * @param motifsRecoursDisponibles motifs invocables selon la voie ; liste vide
 *        lorsque le recours est PRESCRIT.
 * @param messagePrescription message d'irrecevabilité ; {@code null} hors cas PRESCRIT.
 */
public record NaturalisationRecoursTjResult(
        NaturalisationRecoursTjVoieEnum voieNaturalisation,
        LocalDate dateRefusDeclaration,
        NaturalisationRecoursTjTypeRefusEnum typeRefus,
        LocalDate dateEcheanceRecoursJudicaire,
        long joursRestants,
        String tribunalCompetent,
        List<String> basesJuridiques,
        List<String> motifsRecoursDisponibles,
        NaturalisationRecoursTjStatut statut,
        String messagePrescription,
        String recommandation
) {}
