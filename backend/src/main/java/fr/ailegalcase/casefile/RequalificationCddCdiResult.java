package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-DT-22-01 : résultat de l'analyse de requalification CDD → CDI.
 *
 * @param motifCddInvoque                   motif invoqué par l'employeur
 * @param motifInterdit                     motif interdit identifié par l'avocat
 * @param motifInterditType                 type de motif interdit (nullable)
 * @param successionCdd                     succession analysée
 * @param delaiCarenceRespecte              respect de la carence L.1244-3
 * @param dureeContratMois                  durée totale cumulée (mois)
 * @param salaireMensuelBrutEur             salaire mensuel brut
 * @param dateFinDernierContrat             date de fin du dernier CDD
 * @param scoreRequalification              score 0-100
 * @param verdictProbabiliteRequalification ELEVEE / MOYENNE / FAIBLE
 * @param indemniteRequalificationEur       art. L.1245-2 (1 mois plancher)
 * @param indemnitePrecariteEur             rappel art. L.1243-8 (10 % × rémunérations totales)
 * @param totalDommagesIndemniteEur         somme des deux indemnités
 * @param baseJuridique                     citations articles applicables
 * @param formule                           formule littérale traçable
 * @param messages                          rappels prescription / mises en garde
 */
public record RequalificationCddCdiResult(
        String motifCddInvoque,
        boolean motifInterdit,
        String motifInterditType,
        List<RequalificationCddCdiRequest.CddSuccessionEntry> successionCdd,
        boolean delaiCarenceRespecte,
        int dureeContratMois,
        BigDecimal salaireMensuelBrutEur,
        LocalDate dateFinDernierContrat,
        int scoreRequalification,
        String verdictProbabiliteRequalification,
        BigDecimal indemniteRequalificationEur,
        BigDecimal indemnitePrecariteEur,
        BigDecimal totalDommagesIndemniteEur,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
