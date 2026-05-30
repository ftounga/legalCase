package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-218-21 : requête POST pour l'analyse du régime du stagiaire — gratification
 * minimale obligatoire (au-delà de 2 mois / 44 jours de présence) et risque de
 * requalification du stage en contrat de travail (CDI). Art. L.124-1 et s. du
 * code de l'éducation (F-DT-109). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param dateDebutStage date de début du stage (requise).
 * @param dateFinStage date de fin du stage (requise, ≥ dateDebutStage).
 * @param nombreJoursPresence nombre de jours de présence effective au cours de
 *        l'année d'enseignement (requis, ≥ 0 ; 1 jour = 7 h ; règle des 22
 *        jours / mois).
 * @param gratificationMensuelleVersee gratification mensuelle réellement perçue
 *        (défaut 0, ≥ 0).
 * @param tauxHoraireConventionnel taux horaire de gratification prévu par la
 *        convention de branche si plus favorable que le minimum légal
 *        (optionnel, ≥ 0).
 * @param missionsHorsProjetPedagogique true si le stagiaire exécute des tâches
 *        sans lien avec son projet pédagogique (défaut false).
 * @param posteTravailPermanent true si le stagiaire occupe un poste
 *        correspondant à une tâche régulière et permanente de l'entreprise
 *        (défaut false).
 */
public record StagiaireGratificationRequest(
        LocalDate dateDebutStage,
        LocalDate dateFinStage,
        Integer nombreJoursPresence,
        BigDecimal gratificationMensuelleVersee,
        BigDecimal tauxHoraireConventionnel,
        Boolean missionsHorsProjetPedagogique,
        Boolean posteTravailPermanent
) {}
