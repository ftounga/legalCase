package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-218-37 : requête POST pour l'analyse de monétisation de jours de RTT (rachat
 * de jours de RTT — loi n° 2022-1157 du 16/08/2022 art. 5, F-DT-51). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * @param nombreJoursRttRenonces nombre de jours de RTT auxquels le salarié
 *        renonce (requis, &gt; 0).
 * @param salaireJournalierBrut salaire journalier brut de référence (requis,
 *        &gt; 0).
 * @param tauxMajorationConventionnel taux de majoration applicable, au moins égal
 *        au taux de la première heure supplémentaire (optionnel, défaut 25, borné
 *        10–25).
 * @param joursAcquisDansFenetre true si les jours sont acquis entre le 01/01/2022
 *        et le 31/12/2026 (fenêtre du dispositif ; requis).
 */
public record RttMonetisationRequest(
        Integer nombreJoursRttRenonces,
        BigDecimal salaireJournalierBrut,
        Double tauxMajorationConventionnel,
        Boolean joursAcquisDansFenetre
) {}
