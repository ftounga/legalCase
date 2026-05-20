package fr.ailegalcase.casefile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-207-06 : payload de création/upsert d'une analyse d'éligibilité au
 * Régime de Chômage avec Complément d'entreprise (RCC, BE).
 *
 * <p>Bean Validation appliquée côté contrôleur (400 si incomplet). Les
 * règles inter-champs supplémentaires (date naissance future, date
 * licenciement antérieure à la naissance) sont portées par
 * {@link RccBeConditionsService}.</p>
 *
 * <p>Pattern mirroré de {@link RefereTribunalTravailBeRequest}
 * (SF-207-05) — record + Bean Validation + dates ISO {@link LocalDate}.</p>
 *
 * @see RccBeConditionsCalculator
 */
public record RccBeConditionsRequest(
        @NotNull(message = "dateNaissance est requise")
        LocalDate dateNaissance,

        @NotNull(message = "anneesCarriereProfessionnelle est requise")
        @Min(value = 0, message = "anneesCarriereProfessionnelle doit être ≥ 0")
        @Max(value = 60, message = "anneesCarriereProfessionnelle doit être ≤ 60")
        Integer anneesCarriereProfessionnelle,

        /** True si la situation du salarié relève d'un métier lourd reconnu (CCT 17/13). */
        Boolean metierLourd,

        /** True si la carrière atteint le seuil "longue carrière" (CCT 17 art. 3). */
        Boolean longueCarriere,

        /**
         * True si l'employeur est reconnu entreprise en difficulté par arrêté
         * ministériel (AR 03/05/2007 art. 8).
         */
        Boolean entrepriseEnDifficulte,

        /**
         * Date de licenciement envisagée (optionnelle — défaut : aujourd'hui
         * Europe/Brussels). Utilisée pour le calcul de l'âge au jour du
         * licenciement.
         */
        LocalDate dateLicenciementEnvisagee
) {}
