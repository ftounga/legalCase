package fr.ailegalcase.casefile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * SF-219-01 : payload de création / mise à jour de l'analyse d'éligibilité
 * au RCC métiers lourds (régime BE, CCT 17 + AR 03/05/2007 art. 3).
 *
 * <p>Outil <b>BE-only</b> — aucun équivalent FR direct. Distinct du RCC
 * général 60+/40 ans (F-207 SF-207-06) et de l'indemnité complémentaire
 * (F-207 SF-207-07) qui s'applique transversalement à tous les régimes RCC
 * une fois l'éligibilité acquise.</p>
 *
 * <h2>Conditions cumulatives</h2>
 * <ol>
 *   <li>{@code licenciementEffectif=true} — le RCC est interdit en cas de
 *       démission (régime de chômage, requiert un licenciement).</li>
 *   <li>{@code ageFinContrat ≥ 58} ans.</li>
 *   <li>{@code anneesCarriereTotal ≥ 35} ans.</li>
 *   <li>{@code anneesMetierLourdRecent10 ≥ 5} OU
 *       {@code anneesMetierLourdRecent15 ≥ 7} (alternative — il suffit
 *       qu'une des deux soit satisfaite).</li>
 * </ol>
 *
 * <h2>Plages de validation</h2>
 * <ul>
 *   <li>{@code ageFinContrat} : [18, 80] — fourchette utile prud'homale.</li>
 *   <li>{@code anneesCarriereTotal} : [0, 60].</li>
 *   <li>{@code anneesMetierLourdRecent10} : [0, 10].</li>
 *   <li>{@code anneesMetierLourdRecent15} : [0, 15].</li>
 * </ul>
 */
public record RccBeMetiersLourdsRequest(

        @NotNull(message = "ageFinContrat est requis")
        @Min(value = 18, message = "ageFinContrat doit être ≥ 18")
        @Max(value = 80, message = "ageFinContrat doit être ≤ 80")
        Integer ageFinContrat,

        @NotNull(message = "anneesCarriereTotal est requis")
        @Min(value = 0, message = "anneesCarriereTotal doit être ≥ 0")
        @Max(value = 60, message = "anneesCarriereTotal doit être ≤ 60")
        Integer anneesCarriereTotal,

        @NotNull(message = "anneesMetierLourdRecent10 est requis")
        @Min(value = 0, message = "anneesMetierLourdRecent10 doit être ≥ 0")
        @Max(value = 10, message = "anneesMetierLourdRecent10 doit être ≤ 10")
        Integer anneesMetierLourdRecent10,

        @NotNull(message = "anneesMetierLourdRecent15 est requis")
        @Min(value = 0, message = "anneesMetierLourdRecent15 doit être ≥ 0")
        @Max(value = 15, message = "anneesMetierLourdRecent15 doit être ≤ 15")
        Integer anneesMetierLourdRecent15,

        @NotNull(message = "typeMetierLourd est requis")
        RccBeMetiersLourdsTypeEnum typeMetierLourd,

        @NotNull(message = "licenciementEffectif est requis")
        Boolean licenciementEffectif
) {
}
