package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-218-21 : résultat interne business de l'analyse du régime du stagiaire
 * (gratification minimale + requalification en CDI, art. L.124-1 et s. du code
 * de l'éducation, F-DT-109). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param dateDebutStage date de début du stage.
 * @param dateFinStage date de fin du stage.
 * @param nombreJoursPresence jours de présence effective.
 * @param heuresPresence heures de présence estimées (nombreJoursPresence × 7 h).
 * @param dureeStageJours durée calendaire du stage en jours (bornes incluses).
 * @param seuilAtteint true si le seuil de déclenchement de la gratification est
 *        atteint (présence supérieure à 2 mois / 44 jours).
 * @param gratificationObligatoire true si la gratification minimale est
 *        obligatoire (= seuilAtteint).
 * @param tauxHoraireApplique taux horaire de gratification retenu (le plus
 *        favorable entre minimum légal et taux conventionnel).
 * @param gratificationMinimaleDue gratification minimale totale due pour la
 *        période (0 si seuil non atteint).
 * @param gratificationVerseeTotale gratification totale réellement versée
 *        (mensuelle × nombre de mois de stage).
 * @param rappelGratification rappel exigible = max(0, due − versée).
 * @param depassementDureeMax true si la durée du stage dépasse 6 mois (924 h).
 * @param missionsHorsProjetPedagogique indice de requalification.
 * @param posteTravailPermanent indice de requalification.
 * @param risqueRequalification niveau de risque de requalification en CDI.
 * @param motifs motifs justifiant le niveau de risque retenu.
 * @param verdictGlobal verdict global de l'analyse.
 * @param baseJuridique fondements juridiques applicables.
 */
public record StagiaireGratificationResult(
        LocalDate dateDebutStage,
        LocalDate dateFinStage,
        int nombreJoursPresence,
        BigDecimal heuresPresence,
        long dureeStageJours,
        boolean seuilAtteint,
        boolean gratificationObligatoire,
        BigDecimal tauxHoraireApplique,
        BigDecimal gratificationMinimaleDue,
        BigDecimal gratificationVerseeTotale,
        BigDecimal rappelGratification,
        boolean depassementDureeMax,
        boolean missionsHorsProjetPedagogique,
        boolean posteTravailPermanent,
        StagiaireRisqueRequalification risqueRequalification,
        List<String> motifs,
        StagiaireGratificationVerdict verdictGlobal,
        String baseJuridique
) {}
