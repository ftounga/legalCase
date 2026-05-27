package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-03 : payload de création / mise à jour de l'analyse RCC BE
 * <b>entreprise en difficulté / restructuration</b>.
 *
 * <p>Outil <b>BE-only</b> — Loi du 26/12/2013 sur le pacte de solidarité
 * entre les générations + CCT n° 17 du 19/12/1974 + AR du 03/05/2007
 * (RCC) + AR de reconnaissance entreprise en difficulté / restructuration
 * + CCT sectorielle ad hoc. Aucun équivalent strict en droit français.</p>
 *
 * <h2>Logique métier</h2>
 * <p>L'outil vérifie les <b>5 conditions cumulatives</b> :
 * <ol>
 *   <li><b>Reconnaissance ministérielle</b> de l'entreprise — pierre
 *       angulaire du régime dérogatoire (sans elle, pas d'âge réduit).</li>
 *   <li>Âge à la fin du contrat ≥ <b>âge réduit du plan reconnu</b>
 *       (typiquement 55 ans, paramétrable selon la décision ministre).</li>
 *   <li>Carrière professionnelle totale (jours équivalents temps plein)
 *       ≥ <b>10 ans</b>.</li>
 *   <li>Ancienneté dans le secteur / l'entreprise reconnue ≥ <b>5 ans</b>.</li>
 *   <li>Licenciement effectif (préavis presté ou indemnité compensatoire) —
 *       la démission est exclue du dispositif (CCT 17 art. 3).</li>
 * </ol>
 * Si toutes remplies, calcul indicatif de l'indemnité complémentaire mensuelle
 * (50 % de la différence entre rémunération nette de référence et allocation
 * de chômage) à charge de l'employeur, jusqu'à l'âge légal de la pension.</p>
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code typeReconnaissance} — {@link RccBeEntrepriseDifficulteTypeEnum}
 *       (EN_DIFFICULTE, EN_RESTRUCTURATION, NON_RECONNUE).</li>
 *   <li>{@code ageReduitPlan} — âge minimum fixé par le plan ministériel
 *       (souvent 55 ans). Si {@code typeReconnaissance = NON_RECONNUE},
 *       le champ peut être laissé à une valeur arbitraire, il ne sera pas
 *       utilisé pour le verdict (court-circuit RECONNAISSANCE_ABSENTE).</li>
 *   <li>{@code ageFinContrat} — âge en années à la date prévue de fin de
 *       contrat ; doit être ≥ 0.</li>
 *   <li>{@code anneesCarriereTotale} — carrière professionnelle totale en
 *       années ; doit être ≥ 0.</li>
 *   <li>{@code anneesAncienneteSecteur} — ancienneté dans le secteur /
 *       l'entreprise reconnue ; doit être ≥ 0.</li>
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
public record RccBeEntrepriseDifficulteRequest(

        @NotNull(message = "typeReconnaissance est requis")
        RccBeEntrepriseDifficulteTypeEnum typeReconnaissance,

        @NotNull(message = "ageReduitPlan est requis")
        @PositiveOrZero(message = "ageReduitPlan doit être ≥ 0")
        Integer ageReduitPlan,

        @NotNull(message = "ageFinContrat est requis")
        @PositiveOrZero(message = "ageFinContrat doit être ≥ 0")
        Integer ageFinContrat,

        @NotNull(message = "anneesCarriereTotale est requise")
        @PositiveOrZero(message = "anneesCarriereTotale doit être ≥ 0")
        Integer anneesCarriereTotale,

        @NotNull(message = "anneesAncienneteSecteur est requise")
        @PositiveOrZero(message = "anneesAncienneteSecteur doit être ≥ 0")
        Integer anneesAncienneteSecteur,

        @NotNull(message = "dateFinContrat est requise")
        LocalDate dateFinContrat,

        @NotNull(message = "licenciementEffectif est requis")
        Boolean licenciementEffectif,

        BigDecimal remunerationNetteMensuelleReference,

        BigDecimal allocationChomageMensuelleEstimee
) {
}
