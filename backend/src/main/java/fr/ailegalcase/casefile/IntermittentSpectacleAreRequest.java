package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-218-17 : requête POST pour l'analyse d'ouverture des droits à l'ARE d'un
 * intermittent du spectacle relevant des annexes 8 (techniciens) et 10
 * (artistes) du règlement Unedic — seuil d'affiliation de 507 heures sur 12
 * mois (F-DT-106). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param annexe annexe applicable (ANNEXE_8_TECHNICIENS | ANNEXE_10_ARTISTES,
 *        requis).
 * @param dateFinContrat fin du dernier contrat — point de départ de la recherche
 *        de droits (LocalDate, requis, ne peut pas être future).
 * @param heuresTravaillees12Mois total des heures déclarées sur la période de
 *        référence de 12 mois (int ≥ 0, requis).
 * @param nombreCachets cachets artistiques (annexe 10) à convertir — 1 cachet =
 *        12 heures (int ≥ 0, défaut 0).
 * @param heuresFormationDispensees heures d'enseignement / formation
 *        assimilables, plafonnées dans le décompte (int ≥ 0, défaut 0).
 */
public record IntermittentSpectacleAreRequest(
        IntermittentSpectacleAnnexe annexe,
        LocalDate dateFinContrat,
        Integer heuresTravaillees12Mois,
        Integer nombreCachets,
        Integer heuresFormationDispensees
) {}
