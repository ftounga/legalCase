package fr.ailegalcase.casefile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * SF-213-01 : payload de création / mise à jour d'une analyse de validité d'une
 * clause de non-concurrence en droit belge du travail (Loi 03/07/1978 art. 65
 * + CCT 13 du 24/02/1971 modif.).
 *
 * <p>Bean Validation appliquée côté contrôleur (400 si incomplet ou hors
 * bornes). Les validations inter-champs (zone internationale + activité
 * prouvée) sont portées par {@link ClauseNonConcurrenceBeValidator}.</p>
 *
 * <p>Pattern mirroré de {@link RccBeConditionsRequest} (SF-207-06) — record +
 * Bean Validation.</p>
 *
 * @see ClauseNonConcurrenceBeValidator
 */
public record ClauseNonConcurrenceBeRequest(

        @NotNull(message = "remunerationAnnuelleBrute est requise")
        @DecimalMin(value = "0.01", message = "remunerationAnnuelleBrute doit être strictement positive")
        BigDecimal remunerationAnnuelleBrute,

        @NotNull(message = "dureeMois est requise")
        @Min(value = 1, message = "dureeMois doit être ≥ 1 mois")
        @Max(value = 120, message = "dureeMois doit être ≤ 120 mois (excessive est traitée par le validator)")
        Integer dureeMois,

        @NotNull(message = "zoneGeographique est requise")
        ClauseNonConcurrenceBeZoneEnum zoneGeographique,

        /**
         * True si le salarié peut prouver une activité internationale réelle
         * (CCT 13 — atténue la nullité d'une zone géographique étendue).
         */
        Boolean activiteInternationaleProuvee,

        /**
         * Seuil de rémunération annuelle en vigueur (€). Optionnel : par défaut
         * {@value ClauseNonConcurrenceBeValidator#SEUIL_REMUNERATION_2024_EUR}
         * (seuil 2024). À renseigner pour les contrats signés une autre année.
         */
        @DecimalMin(value = "0", message = "salaireAnnuelSeuil doit être ≥ 0")
        BigDecimal salaireAnnuelSeuil
) {}
