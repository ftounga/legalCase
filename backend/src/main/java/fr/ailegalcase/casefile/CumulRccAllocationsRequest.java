package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-04 : payload de création / mise à jour de l'analyse du cumul
 * RCC BE + allocations chômage / pension.
 *
 * <p>Outil <b>BE-only</b> — CCT n° 17 du 19/12/1974 ; AR du 25/11/1991
 * portant réglementation du chômage (ONEM, plafond et compatibilité
 * cumul) ; régime de disponibilité ajustée selon l'âge (AR du 03/05/2007
 * art. 22 et suiv.). Aucun équivalent strict en droit français.</p>
 *
 * <h2>Logique métier</h2>
 * <p>L'outil agrège 3 vérifications :
 * <ol>
 *   <li><b>Plafond de cumul</b> — total {@code allocations chômage +
 *       indemnité complémentaire} ≤ dernière rémunération nette de
 *       référence (sinon l'employeur réduit l'indemnité — CCT 17).</li>
 *   <li><b>Disponibilité ajustée</b> — détermine le régime applicable
 *       (adaptée / passive / dispense) selon l'âge et l'ancienneté.</li>
 *   <li><b>Bascule pension légale</b> — si l'âge légal de la pension est
 *       atteint à la date considérée, le RCC s'éteint automatiquement.</li>
 *   <li><b>Compatibilité activité</b> — toute reprise non déclarée
 *       entraîne la sortie du régime.</li>
 * </ol></p>
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code dateNaissance} — détermine l'âge à la date considérée et
 *       l'accès à la disponibilité passive / bascule pension.</li>
 *   <li>{@code dateDebutRcc} — date d'entrée dans le régime RCC.</li>
 *   <li>{@code allocationChomageMensuelle} — montant ONEM mensuel.</li>
 *   <li>{@code indemniteComplementaireMensuelle} — montant CCT 17
 *       mensuel à charge de l'employeur (ou ex-employeur).</li>
 *   <li>{@code remunerationNetteReference} — dernière rémunération nette
 *       mensuelle de référence (plafond du cumul).</li>
 *   <li>{@code anneesCarriereTotale} — total carrière (jours ETP),
 *       conditionne la disponibilité passive longue carrière.</li>
 *   <li>{@code activiteComplementaire} — nature de l'activité éventuelle.</li>
 * </ul>
 *
 * <h2>Champs optionnels</h2>
 * <ul>
 *   <li>{@code ageLegalPension} — âge légal pension applicable
 *       (par défaut 65 ans ; permet de tester la bascule progressive
 *       66 ans en 2027, 67 ans en 2030 sans recompiler).</li>
 * </ul>
 */
public record CumulRccAllocationsRequest(

        @NotNull(message = "dateNaissance est requise")
        LocalDate dateNaissance,

        @NotNull(message = "dateDebutRcc est requise")
        LocalDate dateDebutRcc,

        @NotNull(message = "allocationChomageMensuelle est requise")
        @PositiveOrZero(message = "allocationChomageMensuelle doit être ≥ 0")
        BigDecimal allocationChomageMensuelle,

        @NotNull(message = "indemniteComplementaireMensuelle est requise")
        @PositiveOrZero(message = "indemniteComplementaireMensuelle doit être ≥ 0")
        BigDecimal indemniteComplementaireMensuelle,

        @NotNull(message = "remunerationNetteReference est requise")
        @PositiveOrZero(message = "remunerationNetteReference doit être ≥ 0")
        BigDecimal remunerationNetteReference,

        @NotNull(message = "anneesCarriereTotale est requise")
        @PositiveOrZero(message = "anneesCarriereTotale doit être ≥ 0")
        Integer anneesCarriereTotale,

        @NotNull(message = "activiteComplementaire est requise")
        CumulRccAllocationsActiviteAutorisee activiteComplementaire,

        Integer ageLegalPension
) {
}
