package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-216-29 : résultat du calcul Donation-partage FR (art. 1075 à 1075-5
 * Cciv + art. 1078, 1078-1, 1080 + art. 912-928 Cciv).
 *
 * <ul>
 *   <li>{@code conditionsRemplies} — true si les conditions de validité de
 *       la donation-partage sont réunies (au moins un descendant ; cas
 *       petits-enfants seuls nécessite consentement du descendant
 *       intermédiaire — art. 1075-1).</li>
 *   <li>{@code interet} — verdict global ("FORT" / "MOYEN" / "FAIBLE" /
 *       "INADAPTE") synthétisant l'intérêt de la donation-partage par
 *       rapport à une succession ordinaire.</li>
 *   <li>{@code gelValeurEffet} — texte décrivant l'effet de gel de la
 *       valeur au jour de la donation (art. 1078 Cciv).</li>
 *   <li>{@code rapportExclu} — true ; les biens donnés-partagés ne sont
 *       pas sujets au rapport successoral (art. 1075-3 Cciv).</li>
 *   <li>{@code alerteQuotite} — true si la donation excède la quotité
 *       disponible, déclenchant l'alerte sur la réserve héréditaire.</li>
 *   <li>{@code etapesNotariales} — checklist ordonnée des étapes notariales
 *       (consultation, acte authentique, publicité foncière, fiscalité).</li>
 *   <li>{@code baseLegale} — articles Cciv applicables.</li>
 *   <li>{@code messages} — informations contextuelles.</li>
 *   <li>{@code alertes} — points d'attention bloquants ou de vigilance.</li>
 * </ul>
 */
public record DonationPartageResult(
        boolean conditionsRemplies,
        String interet,
        String gelValeurEffet,
        boolean rapportExclu,
        boolean alerteQuotite,
        List<String> etapesNotariales,
        String baseLegale,
        List<String> messages,
        List<String> alertes
) {}
