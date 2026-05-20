package fr.ailegalcase.casefile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-207-07 : payload de création/upsert d'une analyse d'<b>indemnité
 * complémentaire RCC</b> (CCT n°17 art. 5 ; Loi du 3 juillet 1978 ; AR du
 * 3 mai 2007).
 *
 * <p>Bean Validation appliquée côté contrôleur (400 si incomplet ou hors
 * borne). Les contrôles inter-champs supplémentaires (date naissance future,
 * date début RCC &lt; naissance + 50 ans) sont portés par
 * {@link RccBeIndemniteService}.</p>
 *
 * <p>Champs :
 * <ul>
 *   <li>{@code remunerationNetteReference} — rémunération nette mensuelle
 *       de référence à la rupture (€, BigDecimal, ≥ 0). Obligatoire.</li>
 *   <li>{@code allocationOnemMensuelle} — allocation ONEM mensuelle de
 *       chômage (€, BigDecimal, ≥ 0). Obligatoire. Fournie par l'avocat
 *       (formule complexe ONEM hors scope).</li>
 *   <li>{@code dateNaissanceSalarie} — date de naissance (ISO).
 *       Obligatoire. Base de calcul des mois restants jusqu'à pension.</li>
 *   <li>{@code dateDebutRcc} — date de début effective du RCC (ISO).
 *       Obligatoire. Base de comptage du nombre de mensualités.</li>
 *   <li>{@code ageLegalPension} — âge légal de la pension. Optionnel,
 *       défaut 66 (2026 ; 67 ans à partir de 2030). Borné [60, 75] pour
 *       rejeter les saisies aberrantes.</li>
 *   <li>{@code planchersSectoriel} — plancher sectoriel CCT (€/mois, ≥ 0).
 *       Optionnel — si présent et supérieur à l'indemnité CCT 17 calculée,
 *       il est appliqué.</li>
 * </ul>
 *
 * <p>Pattern mirroré de {@link RccBeConditionsRequest} (SF-207-06) — record
 * + Bean Validation + dates ISO {@link LocalDate} + montants
 * {@link BigDecimal} pour précision centimes.</p>
 *
 * @see RccBeIndemniteCalculator
 */
public record RccBeIndemniteRequest(
        @NotNull(message = "remunerationNetteReference est requise")
        @DecimalMin(value = "0.00", message = "remunerationNetteReference doit être ≥ 0")
        BigDecimal remunerationNetteReference,

        @NotNull(message = "allocationOnemMensuelle est requise")
        @DecimalMin(value = "0.00", message = "allocationOnemMensuelle doit être ≥ 0")
        BigDecimal allocationOnemMensuelle,

        @NotNull(message = "dateNaissanceSalarie est requise")
        LocalDate dateNaissanceSalarie,

        @NotNull(message = "dateDebutRcc est requise")
        LocalDate dateDebutRcc,

        /**
         * Âge légal de la pension. Optionnel — défaut {@code 66} (cf.
         * {@link RccBeIndemniteCalculator#AGE_LEGAL_PENSION_DEFAUT}). Borne
         * inférieure défensive 60 ans (aucun régime &lt; 60 plausible),
         * borne supérieure 75 ans (au-delà = saisie erronée).
         */
        @Min(value = 60, message = "ageLegalPension doit être ≥ 60")
        @Max(value = 75, message = "ageLegalPension doit être ≤ 75")
        Integer ageLegalPension,

        /**
         * Plancher sectoriel (CCT sectorielle plus favorable). Optionnel.
         * Si présent et &gt; indemnité CCT 17 calculée, il est appliqué.
         */
        @DecimalMin(value = "0.00", message = "planchersSectoriel doit être ≥ 0")
        BigDecimal planchersSectoriel
) {}
