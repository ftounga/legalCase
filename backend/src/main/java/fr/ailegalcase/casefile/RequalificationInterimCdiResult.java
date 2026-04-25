package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-DT-23-01 : résultat de l'analyse de requalification intérim → CDI.
 *
 * @param motifInterimInvoque               motif invoqué par l'employeur
 * @param motifInterdit                     motif interdit identifié par l'avocat
 * @param motifInterditType                 type de motif interdit (nullable)
 * @param successionMissions                succession analysée
 * @param delaiCarenceRespecte              respect de la carence L.1251-36
 * @param dureeMissionsTotaleMois           durée totale cumulée (mois)
 * @param salaireMensuelBrutEur             salaire mensuel brut
 * @param dateFinDerniereMission            date de fin de la dernière mission
 * @param memeEntrepriseUtilisatrice        toutes les missions chez la même UE
 * @param scoreRequalification              score 0-100
 * @param verdictProbabiliteRequalification ELEVEE / MOYENNE / FAIBLE
 * @param indemniteRequalificationEur       art. L.1251-41 al. 2 (1 mois plancher)
 * @param indemniteFinMissionInterimEur     rappel art. L.1251-32 (10 % × rémunérations totales)
 * @param totalDommagesIndemniteEur         somme des deux indemnités
 * @param baseJuridique                     citations articles applicables
 * @param formule                           formule littérale traçable
 * @param messages                          rappels prescription / mises en garde
 */
public record RequalificationInterimCdiResult(
        String motifInterimInvoque,
        boolean motifInterdit,
        String motifInterditType,
        List<RequalificationInterimCdiRequest.MissionInterim> successionMissions,
        boolean delaiCarenceRespecte,
        int dureeMissionsTotaleMois,
        BigDecimal salaireMensuelBrutEur,
        LocalDate dateFinDerniereMission,
        boolean memeEntrepriseUtilisatrice,
        int scoreRequalification,
        String verdictProbabiliteRequalification,
        BigDecimal indemniteRequalificationEur,
        BigDecimal indemniteFinMissionInterimEur,
        BigDecimal totalDommagesIndemniteEur,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
