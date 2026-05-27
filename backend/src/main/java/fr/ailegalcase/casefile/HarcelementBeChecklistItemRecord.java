package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-213-07 : item d'une checklist d'étape de la procédure interne BE de
 * plainte pour harcèlement moral / sexuel.
 *
 * <p>Chaque item décrit une action concrète à mener (ou un état juridique
 * actif) pour piloter l'accompagnement du client par l'avocat. La
 * combinaison {@code statut + dateEcheance} permet à l'UI de prioriser
 * visuellement les actions urgentes (enquête 90 j, fin protection 12 mois).</p>
 *
 * @param item         libellé clair de l'action ou de l'état juridique
 *                     (ex. « Enquête CPAP — délai 90 jours »).
 * @param statut       statut courant — {@link HarcelementBeChecklistStatutEnum}
 *                     ({@code A_FAIRE}, {@code EN_COURS}, {@code TERMINE},
 *                     {@code ACTIF}).
 * @param dateEcheance date d'échéance ISO (ex. fin enquête, fin protection
 *                     12 mois). {@code null} si l'item n'a pas de date
 *                     fatale associée.
 */
public record HarcelementBeChecklistItemRecord(
        String item,
        HarcelementBeChecklistStatutEnum statut,
        LocalDate dateEcheance
) {
}
