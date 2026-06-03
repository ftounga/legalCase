package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-218-47 : résultat interne business de l'analyse du congé de proche aidant
 * (art. L.3142-16 à L.3142-27 CT, F-DT-79). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param statut verdict d'éligibilité (ELIGIBLE / NON_ELIGIBLE).
 * @param lienPersonneAidee lien retenu avec la personne aidée.
 * @param personneAideeResideFrance la personne aidée réside en France/EEE.
 * @param dureeSouhaiteeMois durée de congé souhaitée (mois) saisie.
 * @param dureeMaxMois durée maximale légale du congé en mois (= 12, un an sur la
 *        carrière, L.3142-19).
 * @param dureeRetenueMois durée retenue = min(souhaitée, 12), null si non éligible.
 * @param ajpaDemandee true si l'AJPA est demandée auprès de la CAF.
 * @param ajpaJournaliere montant journalier de l'AJPA (≈ 64,54 € en 2026, à
 *        vérifier), null si AJPA non demandée.
 * @param estimationAjpa estimation indicative de l'AJPA totale sur la durée
 *        retenue (plafond 66 jours indemnisés sur la carrière), null si AJPA non
 *        demandée ou non éligible.
 * @param protectionEmploi true — protection de l'emploi / réintégration (L.3142-20 et s.).
 * @param nonImputableCongesPayes true — congé non imputable sur les congés payés.
 * @param notes notes / points de vigilance identifiés.
 * @param baseJuridique fondements juridiques applicables.
 */
public record CongeProcheAidantResult(
        CongeProcheAidantStatut statut,
        CongeProcheAidantLien lienPersonneAidee,
        boolean personneAideeResideFrance,
        int dureeSouhaiteeMois,
        int dureeMaxMois,
        Integer dureeRetenueMois,
        boolean ajpaDemandee,
        BigDecimal ajpaJournaliere,
        BigDecimal estimationAjpa,
        boolean protectionEmploi,
        boolean nonImputableCongesPayes,
        List<String> notes,
        String baseJuridique
) {}
