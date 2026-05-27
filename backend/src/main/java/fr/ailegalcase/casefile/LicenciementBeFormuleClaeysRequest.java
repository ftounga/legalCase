package fr.ailegalcase.casefile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * SF-213-04 : payload de création / mise à jour d'un calcul de préavis selon
 * la <b>Formule Claeys</b> (ancien art. 82 Loi du 03/07/1978) pour les contrats
 * antérieurs au statut unique (01/01/2014), combinable avec le préavis statut
 * unique post-2014 via la clause de sauvegarde (Loi du 26/12/2013 art. 67).
 *
 * <p>Outil <b>BE-only</b> — pas d'équivalent en droit français.</p>
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code ancienneteAnneesPreStatutUnique} : années complètes d'ancienneté
 *       accumulées avant le 01/01/2014.</li>
 *   <li>{@code remunerationAnnuelleBruteEnMilliers} : rémunération brute
 *       annuelle au 31/12/2013, exprimée en milliers d'euros (K€).</li>
 * </ul>
 *
 * <h2>Champs conditionnels (clause de sauvegarde)</h2>
 * <p>Si {@code appliquerClauseSauvegarde = true} (défaut), alors
 * {@code ancienneteAnneesPostStatutUnique} et {@code salaireHebdomadaireBrut}
 * doivent être renseignés — la cohérence est vérifiée côté
 * {@link LicenciementBeFormuleClaeysService} qui renvoie 400 sinon.</p>
 *
 * <p>Pattern mirroré de {@link LicenciementBeStatutUniquePreavisRequest} (SF-213-03).</p>
 *
 * @see LicenciementBeFormuleClaeysCalculator
 */
public record LicenciementBeFormuleClaeysRequest(

        @NotNull(message = "ancienneteAnneesPreStatutUnique est requise")
        @Min(value = 0, message = "ancienneteAnneesPreStatutUnique doit être ≥ 0")
        @Max(value = 80, message = "ancienneteAnneesPreStatutUnique doit être ≤ 80")
        Integer ancienneteAnneesPreStatutUnique,

        /**
         * Mois supplémentaires au-delà des années complètes pré-2014 (0-11).
         * Optionnel, défaut 0.
         */
        @Min(value = 0, message = "ancienneteMoisPreStatutUnique doit être ≥ 0")
        @Max(value = 11, message = "ancienneteMoisPreStatutUnique doit être ≤ 11")
        Integer ancienneteMoisPreStatutUnique,

        @NotNull(message = "remunerationAnnuelleBruteEnMilliers est requise")
        @DecimalMin(value = "0.01",
                message = "remunerationAnnuelleBruteEnMilliers doit être strictement positive")
        BigDecimal remunerationAnnuelleBruteEnMilliers,

        /**
         * Si true (défaut), cumule le préavis Claeys avec le préavis statut
         * unique calculé pour la partie post-2014 — clause de sauvegarde
         * loi 26/12/2013 art. 67.
         *
         * <p>Si {@code null}, traité comme {@code true} par le calculateur.</p>
         */
        Boolean appliquerClauseSauvegarde,

        /**
         * Années complètes d'ancienneté post-01/01/2014 — requis si
         * {@code appliquerClauseSauvegarde = true}.
         */
        @Min(value = 0, message = "ancienneteAnneesPostStatutUnique doit être ≥ 0")
        @Max(value = 80, message = "ancienneteAnneesPostStatutUnique doit être ≤ 80")
        Integer ancienneteAnneesPostStatutUnique,

        /**
         * Salaire hebdomadaire brut au moment du licenciement — requis si
         * {@code appliquerClauseSauvegarde = true} pour calculer l'indemnité
         * compensatoire totale (Claeys + statut unique).
         */
        @DecimalMin(value = "0.01",
                message = "salaireHebdomadaireBrut doit être strictement positif")
        BigDecimal salaireHebdomadaireBrut
) {}
