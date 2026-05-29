package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-214-31 : résultat interne business du calcul du délai de recours devant le
 * Tribunal administratif de Nantes contre un refus de naturalisation par décret
 * — 2 mois à compter du refus (CJA droit commun ; Cciv 21-15).
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> — distinct de :
 * <ul>
 *   <li>SF-214-29 (recours TJ refus de déclaration de nationalité, Cciv 26-3,
 *       voies mariage / ascendant / mineur, juridiction civile) ;</li>
 *   <li>F-IM-13 (checklist demande de nationalité).</li>
 * </ul>
 *
 * @param joursRestants jours calendaires restants avant l'échéance (négatif si
 *        dépassé).
 * @param motifsRecoursDisponibles motifs invocables ; liste vide lorsque le
 *        recours est PRESCRIT.
 * @param messagePrescription message d'irrecevabilité ; {@code null} hors cas PRESCRIT.
 */
public record NaturalisationRecoursTaNantesResult(
        LocalDate dateRefusDecret,
        String motivationRefus,
        boolean recoursPrerequis,
        LocalDate dateEcheanceRecoursTa,
        long joursRestants,
        String tribunalCompetent,
        List<String> basesJuridiques,
        List<String> motifsRecoursDisponibles,
        NaturalisationRecoursTaNantesStatut statut,
        String messagePrescription,
        String recommandation
) {}
