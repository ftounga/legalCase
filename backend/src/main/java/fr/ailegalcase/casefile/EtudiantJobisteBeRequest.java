package fr.ailegalcase.casefile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-13 : payload de création / mise à jour de l'analyse <i>étudiant
 * jobiste BE</i> (Loi du 03/07/1978, Titre VII, modifiée par la
 * Loi-programme du 24/12/2002 et l'AR du 14/07/1995, quota porté à
 * 600 heures/an par la Loi-programme du 22/12/2023 (M.B. 29/12/2023)).
 *
 * <h2>Logique métier — conditions cumulatives</h2>
 * <ol>
 *   <li><b>Statut étudiant</b> ({@code statutEtudiant}) : inscription
 *       régulière en cours OU interruption courte ≤ 12 mois entre
 *       cycles.</li>
 *   <li><b>Quota 600h/an</b> : {@code heuresDejaPresteesDansAnnee}
 *       (compteur Student@work consolidé) + {@code heuresContratEnCours}
 *       ne doivent pas dépasser {@code quotaAnnuelHeures} (= 600 depuis
 *       2023, paramétrable pour les exercices antérieurs où il valait
 *       475).</li>
 *   <li><b>Formalisme</b> : contrat d'occupation étudiant écrit
 *       ({@code contratEcritSigne}) + déclaration Dimona spécifique STU
 *       ({@code dimonaStuDeclaree}) — art. 123 Loi 03/07/1978 + AR
 *       05/11/2002.</li>
 *   <li><b>Cotisations</b> : {@code cotisationsReduitesAppliquees} =
 *       l'employeur applique le barème de solidarité réduit (5,42 % +
 *       2,71 % = 8,13 % au lieu de ~50 % en régime ordinaire).</li>
 * </ol>
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code dateDebutOccupation} — début des prestations.</li>
 *   <li>{@code statutEtudiant} — catégorie de l'étudiant.</li>
 *   <li>{@code heuresDejaPresteesDansAnnee} — compteur Student@work
 *       avant signature de ce contrat (≥ 0).</li>
 *   <li>{@code heuresContratEnCours} — durée du contrat en heures
 *       (&gt; 0).</li>
 *   <li>{@code quotaAnnuelHeures} — quota légal en vigueur (= 600
 *       depuis 2023, &gt; 0).</li>
 *   <li>{@code contratEcritSigne} — drapeau contrat écrit signé.</li>
 *   <li>{@code dimonaStuDeclaree} — drapeau Dimona STU effectuée.</li>
 *   <li>{@code cotisationsReduitesAppliquees} — drapeau barème
 *       AR 14/07/1995 appliqué.</li>
 *   <li>{@code remunerationBruteHoraire} — taux horaire brut (€,
 *       ≥ 0) — base de calcul du redressement éventuel sur les heures
 *       hors quota.</li>
 * </ul>
 */
public record EtudiantJobisteBeRequest(

        @NotNull(message = "dateDebutOccupation est requise")
        LocalDate dateDebutOccupation,

        @NotNull(message = "statutEtudiant est requis")
        EtudiantJobisteBeStatutEtudiant statutEtudiant,

        @NotNull(message = "heuresDejaPresteesDansAnnee est requis")
        @Min(value = 0,
                message = "heuresDejaPresteesDansAnnee doit être positif ou nul")
        Integer heuresDejaPresteesDansAnnee,

        @NotNull(message = "heuresContratEnCours est requis")
        @Min(value = 1,
                message = "heuresContratEnCours doit être strictement positif")
        Integer heuresContratEnCours,

        @NotNull(message = "quotaAnnuelHeures est requis")
        @Min(value = 1,
                message = "quotaAnnuelHeures doit être strictement positif")
        Integer quotaAnnuelHeures,

        @NotNull(message = "contratEcritSigne est requis")
        Boolean contratEcritSigne,

        @NotNull(message = "dimonaStuDeclaree est requis")
        Boolean dimonaStuDeclaree,

        @NotNull(message = "cotisationsReduitesAppliquees est requis")
        Boolean cotisationsReduitesAppliquees,

        @NotNull(message = "remunerationBruteHoraire est requise")
        @DecimalMin(value = "0.0", inclusive = true,
                message = "remunerationBruteHoraire doit être positif ou nul")
        BigDecimal remunerationBruteHoraire
) {
}
