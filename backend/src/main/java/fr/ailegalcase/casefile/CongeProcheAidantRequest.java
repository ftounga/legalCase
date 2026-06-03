package fr.ailegalcase.casefile;

/**
 * SF-218-47 : requête POST pour l'analyse du congé de proche aidant (art.
 * L.3142-16 à L.3142-27 CT, F-DT-79). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param lienPersonneAidee lien avec la personne aidée (requis) ∈ {CONJOINT,
 *        ASCENDANT, DESCENDANT, COLLATERAL, SANS_LIEN_RESIDENCE_COMMUNE}.
 * @param personneAideeResideFrance la personne aidée réside en France/EEE de
 *        façon stable et régulière (requis ; condition d'éligibilité L.3142-16).
 * @param dureeSouhaiteeMois durée de congé souhaitée, en mois (requis, &gt; 0 ;
 *        plafonnée à 12 — un an sur l'ensemble de la carrière, L.3142-19).
 * @param ajpaDemandee le salarié demande l'allocation journalière du proche
 *        aidant (AJPA) auprès de la CAF (défaut false).
 */
public record CongeProcheAidantRequest(
        CongeProcheAidantLien lienPersonneAidee,
        Boolean personneAideeResideFrance,
        Integer dureeSouhaiteeMois,
        Boolean ajpaDemandee
) {}
