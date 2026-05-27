package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-02 : payload de création / mise à jour de l'analyse RCC BE
 * <b>longue carrière</b>.
 *
 * <p>Outil <b>BE-only</b> — Loi du 26/12/2013 sur le pacte de solidarité
 * entre les générations + CCT n° 17 du 19/12/1974 + AR du 03/05/2007 art. 3
 * (régime spécifique longue carrière, conditions 59 ans / 40 ans de carrière).
 * Aucun équivalent strict en droit français.</p>
 *
 * <h2>Logique métier</h2>
 * <p>L'outil vérifie les <b>3 conditions cumulatives</b> :
 * <ol>
 *   <li>Âge à la fin du contrat ≥ <b>59 ans</b>.</li>
 *   <li>Carrière professionnelle (jours équivalents temps plein) ≥ <b>40 ans</b>.</li>
 *   <li>Licenciement effectif (préavis presté ou indemnité compensatoire) —
 *       la démission est exclue du dispositif.</li>
 * </ol>
 * Si toutes remplies, calcul indicatif de l'indemnité complémentaire mensuelle
 * (50 % de la différence entre rémunération nette de référence et allocation
 * de chômage) à charge de l'employeur, jusqu'à l'âge légal de la pension.</p>
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code ageFinContrat} — âge en années à la date prévue de fin de
 *       contrat ; doit être ≥ 0.</li>
 *   <li>{@code anneesCarriereTotale} — carrière professionnelle totale en
 *       années ; doit être ≥ 0.</li>
 *   <li>{@code dateFinContrat} — date prévue de fin de contrat (ancre la
 *       projection RCC).</li>
 *   <li>{@code licenciementEffectif} — {@code true} si l'employeur licencie
 *       (vs démission).</li>
 * </ul>
 *
 * <h2>Champs optionnels (indemnité complémentaire indicative)</h2>
 * <ul>
 *   <li>{@code remunerationNetteMensuelleReference} — rémunération nette
 *       mensuelle de référence ; si fournie avec
 *       {@code allocationChomageMensuelleEstimee}, calcul de l'indemnité
 *       complémentaire indicative.</li>
 *   <li>{@code allocationChomageMensuelleEstimee} — estimation ONEM de
 *       l'allocation mensuelle de chômage.</li>
 * </ul>
 */
public record RccBeLongueCarriereRequest(

        @NotNull(message = "ageFinContrat est requis")
        @PositiveOrZero(message = "ageFinContrat doit être ≥ 0")
        Integer ageFinContrat,

        @NotNull(message = "anneesCarriereTotale est requise")
        @PositiveOrZero(message = "anneesCarriereTotale doit être ≥ 0")
        Integer anneesCarriereTotale,

        @NotNull(message = "dateFinContrat est requise")
        LocalDate dateFinContrat,

        @NotNull(message = "licenciementEffectif est requis")
        Boolean licenciementEffectif,

        BigDecimal remunerationNetteMensuelleReference,

        BigDecimal allocationChomageMensuelleEstimee
) {
}
