package fr.ailegalcase.casefile;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-20 : payload de création / mise à jour de l'analyse
 * <i>pécule de vacances BE</i> (Lois coordonnées du 28/06/1971 +
 * AR du 30/03/1967).
 *
 * <h2>Logique métier — champs aiguilleurs</h2>
 * <ol>
 *   <li><b>Statut du travailleur</b> ({@code statut}) — EMPLOYE /
 *       OUVRIER / JEUNE_TRAVAILLEUR / INDETERMINE — détermine qui
 *       paye (employeur direct vs ONVA).</li>
 *   <li><b>Type de calcul demandé</b> ({@code typeCalcul}) —
 *       PECULE_SIMPLE / DOUBLE_PECULE / PECULE_DEPART.</li>
 *   <li><b>Rémunération brute annuelle de l'exercice de vacances</b>
 *       ({@code remunerationBruteAnnuelleExerciceEur}) — base de
 *       calcul du double pécule employé (85 % du salaire mensuel
 *       moyen, art. 38 AR 30/03/1967) et du pécule de départ
 *       (15,34 % de cette base, art. 46).</li>
 *   <li><b>Rémunération mensuelle brute</b>
 *       ({@code remunerationMensuelleBruteEur}) — sert au pécule
 *       simple employé (rémunération maintenue pendant les jours
 *       de congé pris) et au calcul du double pécule sur base
 *       mensuelle.</li>
 *   <li><b>Jours de congé pris / dus</b> ({@code joursCongesPris})
 *       — utilisé pour proratiser le pécule simple employé.</li>
 *   <li><b>Date de sortie</b> ({@code dateSortie}) — applicable
 *       uniquement si {@code typeCalcul == PECULE_DEPART}.</li>
 *   <li><b>Pécule déjà payé</b> ({@code peculeDejaPaye}) — short-
 *       circuit du calcul (créance éteinte).</li>
 *   <li><b>Date fin du contrat</b> ({@code dateFinContrat}) —
 *       sert au calcul de la prescription art. 15 Loi 03/07/1978
 *       (1 an depuis la fin du contrat).</li>
 *   <li><b>Date de la réclamation</b> ({@code dateReclamation}) —
 *       sert au calcul de la prescription.</li>
 *   <li><b>Rémunération exercice précédent</b>
 *       ({@code remunerationBruteAnnuelleExercicePrecedentEur}) —
 *       applicable au pécule de départ pour la fraction de l'année
 *       précédente non encore liquidée (art. 46 al. 2 AR 30/03/1967).</li>
 * </ol>
 */
public record PeculeVacancesBeRequest(

        @NotNull(message = "statut est requis")
        PeculeVacancesBeStatut statut,

        @NotNull(message = "typeCalcul est requis")
        PeculeVacancesBeTypeCalcul typeCalcul,

        @NotNull(message = "remunerationBruteAnnuelleExerciceEur est requise")
        @DecimalMin(value = "0.0", inclusive = true,
                message = "remunerationBruteAnnuelleExerciceEur doit être positive ou nulle")
        @DecimalMax(value = "10000000.0", inclusive = true,
                message = "remunerationBruteAnnuelleExerciceEur plafonnée à 10 M€")
        BigDecimal remunerationBruteAnnuelleExerciceEur,

        @NotNull(message = "remunerationMensuelleBruteEur est requise")
        @DecimalMin(value = "0.0", inclusive = true,
                message = "remunerationMensuelleBruteEur doit être positive ou nulle")
        @DecimalMax(value = "1000000.0", inclusive = true,
                message = "remunerationMensuelleBruteEur plafonnée à 1 M€")
        BigDecimal remunerationMensuelleBruteEur,

        @NotNull(message = "joursCongesPris est requis")
        @DecimalMin(value = "0.0", inclusive = true,
                message = "joursCongesPris doit être positif ou nul")
        @DecimalMax(value = "30.0", inclusive = true,
                message = "joursCongesPris plafonné à 30 jours (4 semaines régime 6 jours)")
        BigDecimal joursCongesPris,

        @NotNull(message = "peculeDejaPaye est requis")
        Boolean peculeDejaPaye,

        @NotNull(message = "remunerationBruteAnnuelleExercicePrecedentEur est requise")
        @DecimalMin(value = "0.0", inclusive = true,
                message = "remunerationBruteAnnuelleExercicePrecedentEur doit être positive ou nulle")
        @DecimalMax(value = "10000000.0", inclusive = true,
                message = "remunerationBruteAnnuelleExercicePrecedentEur plafonnée à 10 M€")
        BigDecimal remunerationBruteAnnuelleExercicePrecedentEur,

        LocalDate dateSortie,

        LocalDate dateFinContrat,

        @NotNull(message = "dateReclamation est requise")
        LocalDate dateReclamation
) {
}
