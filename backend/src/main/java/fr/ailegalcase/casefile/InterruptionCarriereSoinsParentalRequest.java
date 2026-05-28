package fr.ailegalcase.casefile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-32 : payload de création / mise à jour de l'analyse
 * <i>Interruption de carrière pour congé parental BE</i> (Loi du
 * 22/01/1985 + AR du 29/10/1997 + CCT n° 64).
 *
 * <h2>Champs requis</h2>
 * <ul>
 *   <li>{@link #forme} — TEMPS_PLEIN 4 mois, MI_TEMPS 8 mois,
 *       CINQUIEME_TEMPS 20 mois (AR 29/10/1997 art. 3).</li>
 *   <li>{@link #ancienneteMois} — ancienneté du travailleur auprès du
 *       même employeur en mois ; doit être >= 12 (art. 5 AR
 *       29/10/1997, période 15 mois précédant la demande).</li>
 *   <li>{@link #ageEnfantMois} — âge de l'enfant en mois à la date du
 *       début de l'interruption (art. 4 AR 29/10/1997 : moins de
 *       144 mois = 12 ans, ou moins de 252 mois = 21 ans si
 *       handicap).</li>
 *   <li>{@link #enfantHandicap} — drapeau handicap ≥ 66% qui porte le
 *       plafond d'âge à 21 ans (art. 4 al. 2 AR 29/10/1997).</li>
 *   <li>{@link #soldeRestantMoisEtp} — solde restant du droit
 *       individuel 4 mois ETP par enfant et par parent exprimé en
 *       mois ETP (art. 2 AR 29/10/1997).</li>
 *   <li>{@link #modeNotification} — lettre recommandée, remise en main
 *       propre, ou autre (art. 6 AR 29/10/1997).</li>
 *   <li>{@link #employeurAccepte} — drapeau accord employeur sur la
 *       forme demandée.</li>
 *   <li>{@link #employeurAdiffere} — drapeau employeur a notifié un
 *       différé motivé maximum 6 mois (art. 7 AR 29/10/1997).</li>
 *   <li>{@link #cumulAllocationOnemDemande} — drapeau cumul allocation
 *       ONEM forfaitaire mensuelle (AR 12/08/1991 art. 4).</li>
 * </ul>
 *
 * <h2>Champs optionnels</h2>
 * <ul>
 *   <li>{@link #dateDebutInterruption} — date souhaitée du début ; sert
 *       à calculer la date de fin et la fenêtre de protection art.
 *       101 Loi 22/01/1985.</li>
 *   <li>{@link #dateNotification} — date de notification à l'employeur ;
 *       sert à vérifier le délai 2 à 3 mois avant le début (art. 6
 *       AR 29/10/1997).</li>
 *   <li>{@link #remunerationMensuelleBrute} — rémunération mensuelle
 *       brute en euros ; sert à calculer l'indemnité de protection 6
 *       mois (art. 101 Loi 22/01/1985).</li>
 * </ul>
 */
public record InterruptionCarriereSoinsParentalRequest(

        @NotNull(message = "forme est requis")
        InterruptionCarriereSoinsParentalForme forme,

        @NotNull(message = "ancienneteMois est requis")
        @Min(value = 0, message = "ancienneteMois ne peut pas être négatif")
        Integer ancienneteMois,

        @NotNull(message = "ageEnfantMois est requis")
        @Min(value = 0, message = "ageEnfantMois ne peut pas être négatif")
        Integer ageEnfantMois,

        @NotNull(message = "enfantHandicap est requis")
        Boolean enfantHandicap,

        @NotNull(message = "soldeRestantMoisEtp est requis")
        @Min(value = 0,
                message = "soldeRestantMoisEtp ne peut pas être négatif")
        Integer soldeRestantMoisEtp,

        @NotNull(message = "modeNotification est requis")
        InterruptionCarriereSoinsParentalModeNotification modeNotification,

        @NotNull(message = "employeurAccepte est requis")
        Boolean employeurAccepte,

        @NotNull(message = "employeurAdiffere est requis")
        Boolean employeurAdiffere,

        @NotNull(message = "cumulAllocationOnemDemande est requis")
        Boolean cumulAllocationOnemDemande,

        LocalDate dateDebutInterruption,

        LocalDate dateNotification,

        BigDecimal remunerationMensuelleBrute
) {
}
