package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-218-17 : résultat interne business de l'analyse d'ouverture des droits à
 * l'ARE de l'intermittent du spectacle (annexes 8 et 10 Unedic — seuil 507 h /
 * 12 mois). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param annexe annexe applicable retenue.
 * @param dateFinContrat fin du dernier contrat (point de départ de la recherche
 *        de droits).
 * @param heuresTravaillees12Mois heures déclarées sur la période de référence.
 * @param heuresCachets heures issues de la conversion des cachets (annexe 10) —
 *        {@code nombreCachets × 12}.
 * @param heuresFormationRetenues heures d'enseignement retenues après écrêtage au
 *        plafond.
 * @param heuresTotalesRetenues total d'heures retenues pour l'affiliation.
 * @param seuilHeures seuil d'affiliation appliqué (507 h — à actualiser).
 * @param heuresManquantes heures manquantes pour atteindre le seuil (0 si
 *        atteint).
 * @param heuresExcedentaires heures excédentaires au-delà du seuil (0 si non
 *        atteint).
 * @param dateProchainExamen date d'anniversaire indicative du réexamen annuel des
 *        droits ({@code dateFinContrat + 12 mois}).
 * @param ouvertureDroits verdict d'ouverture (OUVERTS / NON_OUVERTS / A_VERIFIER).
 * @param statut statut détaillé (DROITS_OUVERTS / DROITS_NON_OUVERTS /
 *        A_VERIFIER).
 * @param baseJuridique fondements juridiques applicables.
 */
public record IntermittentSpectacleAreResult(
        IntermittentSpectacleAnnexe annexe,
        LocalDate dateFinContrat,
        int heuresTravaillees12Mois,
        int heuresCachets,
        int heuresFormationRetenues,
        int heuresTotalesRetenues,
        int seuilHeures,
        int heuresManquantes,
        int heuresExcedentaires,
        LocalDate dateProchainExamen,
        IntermittentSpectacleAreVerdict ouvertureDroits,
        IntermittentSpectacleAreStatut statut,
        String baseJuridique
) {}
