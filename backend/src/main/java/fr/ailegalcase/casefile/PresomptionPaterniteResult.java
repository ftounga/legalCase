package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-216-25 : résultat du calcul Présomption de paternité du mari et
 * désaveu FR (art. 312-315 Cciv + art. 316 al. 2 + art. 333 al. 1).
 *
 * <ul>
 *   <li>{@code presomptionApplicable} — true si la présomption de
 *       paternité du mari s'applique (art. 312 Cciv : enfant conçu
 *       pendant le mariage + né moins de 300 jours après dissolution).</li>
 *   <li>{@code presomptionRenversee} — true si la présomption est
 *       renversée de plein droit (art. 313 Cciv : enfant né plus de
 *       300 jours après dissolution ou moins de 180 jours après
 *       conclusion du mariage et mari nie + pas de possession d'état).</li>
 *   <li>{@code voieDesaveu} — voie procédurale ouverte au mari :
 *       "DESAVEU_RECEVABLE", "DESAVEU_DELAI_FORCLOS", "DESAVEU_DIFFICILE_POSSESSION_ETAT",
 *       "DESAVEU_SANS_OBJET", "INDETERMINE".</li>
 *   <li>{@code delaiDesaveu} — texte décrivant le délai applicable
 *       (art. 316 al. 2 Cciv : 6 mois à compter de la naissance ou de
 *       la connaissance de la naissance).</li>
 *   <li>{@code possessionEtatImpact} — texte décrivant l'impact de la
 *       possession d'état conforme (art. 333 al. 1 Cciv : neutralisation
 *       de la contestation).</li>
 *   <li>{@code baseLegale} — articles Cciv applicables.</li>
 *   <li>{@code messages} — informations contextuelles.</li>
 *   <li>{@code alertes} — points d'attention bloquants ou de vigilance.</li>
 * </ul>
 */
public record PresomptionPaterniteResult(
        boolean presomptionApplicable,
        boolean presomptionRenversee,
        String voieDesaveu,
        String delaiDesaveu,
        String possessionEtatImpact,
        String baseLegale,
        List<String> messages,
        List<String> alertes
) {}
