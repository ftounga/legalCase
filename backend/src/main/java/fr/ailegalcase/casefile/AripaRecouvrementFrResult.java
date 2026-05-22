package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-216-07 : résultat du calcul ARIPA recouvrement FR (art. L. 581+ CSS).
 *
 * <ul>
 *   <li>{@code voieRecommandee} — voie de recouvrement principale recommandée.</li>
 *   <li>{@code montantArrieres} — pension mensuelle × nombre de mois impayés
 *       (indicatif — l'ARIPA ne récupère que 24 mois d'arriérés au plus).</li>
 *   <li>{@code montantAsfEligibleMensuelEur} — complément ASF mensuel
 *       théorique si la pension est inférieure au seuil (art. L. 523-1 CSS).
 *       0 si pas éligible (pension ≥ seuil).</li>
 *   <li>{@code delaiEstimeJours} — fourchette indicative (jours) avant
 *       premier recouvrement par la voie principale.</li>
 *   <li>{@code etapes} — liste ordonnée d'étapes concrètes pour l'avocat.</li>
 *   <li>{@code baseLegale} — articles CSS / textes applicables.</li>
 *   <li>{@code messages} — informations contextuelles.</li>
 *   <li>{@code alertes} — points d'attention bloquants ou de vigilance.</li>
 * </ul>
 */
public record AripaRecouvrementFrResult(
        VoieRecouvrementAripaEnum voieRecommandee,
        int montantArrieres,
        int montantAsfEligibleMensuelEur,
        int delaiEstimeJours,
        List<String> etapes,
        String baseLegale,
        List<String> messages,
        List<String> alertes
) {}
