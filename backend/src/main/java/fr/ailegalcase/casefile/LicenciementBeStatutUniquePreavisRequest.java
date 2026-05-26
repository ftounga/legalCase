package fr.ailegalcase.casefile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-213-03 : payload de création / mise à jour d'un calcul de préavis
 * statut unique en droit belge du travail (Loi du 26 décembre 2013 —
 * barème art. 37/1 et seq.).
 *
 * <p>Bean Validation appliquée côté contrôleur (400 si incomplet ou hors
 * bornes). Les calculs sont portés par
 * {@link LicenciementBeStatutUniquePreavisCalculator} (fonction pure).</p>
 *
 * <p>Pattern mirroré de {@link ClauseNonConcurrenceBeRequest} (SF-213-01) —
 * record + Bean Validation.</p>
 *
 * @see LicenciementBeStatutUniquePreavisCalculator
 */
public record LicenciementBeStatutUniquePreavisRequest(

        @NotNull(message = "ancienneteAnnees est requise")
        @Min(value = 0, message = "ancienneteAnnees doit être ≥ 0")
        @Max(value = 80, message = "ancienneteAnnees doit être ≤ 80 (plafond carrière humaine)")
        Integer ancienneteAnnees,

        /**
         * Mois supplémentaires au-delà des années complètes (0-11). Optionnel,
         * défaut 0. Permet d'exprimer une ancienneté fine (ex : 9 ans 6 mois).
         */
        @Min(value = 0, message = "ancienneteMoisSupplementaires doit être ≥ 0")
        @Max(value = 11, message = "ancienneteMoisSupplementaires doit être ≤ 11")
        Integer ancienneteMoisSupplementaires,

        @NotNull(message = "salaireHebdomadaireBrut est requis")
        @DecimalMin(value = "0.01", message = "salaireHebdomadaireBrut doit être strictement positif")
        BigDecimal salaireHebdomadaireBrut,

        @NotNull(message = "dateNotificationLicenciement est requise")
        LocalDate dateNotificationLicenciement,

        /**
         * True si l'ancienneté correspond intégralement à la période post-2014
         * (statut unique). False signale un contrat antérieur au 01/01/2014 dont
         * la partie pré-2014 doit être traitée séparément via l'outil Formule
         * Claeys (SF-213-04). Défaut true côté service si null.
         */
        Boolean partieStatutUniqueSeulement
) {}
