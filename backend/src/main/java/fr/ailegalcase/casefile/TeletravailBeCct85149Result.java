package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-16 : résultat brut produit par {@link TeletravailBeCct85149Checker}
 * — fonction <b>pure</b> (sans dépendance HTTP / persistance / horloge).
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par {@link
 * TeletravailBeCct85149Service} dans {@link TeletravailBeCct85149Response}).</p>
 *
 * <p>Outputs dérivés : ventilation par dimension de conformité,
 * écart d'indemnité (différence positive entre l'indemnité réelle et
 * le plafond admis — utile pour signaler un excédent imposable).</p>
 */
public record TeletravailBeCct85149Result(
        // Inputs (snapshot)
        LocalDate dateDebutTeletravail,
        TeletravailBeCct85149TypeTeletravail typeTeletravail,
        boolean volontariatReciproque,
        boolean conventionEcriteIndividuelle,
        boolean equipementFourniOuIndemnise,
        BigDecimal indemniteForfaitaireMensuelleEuros,
        BigDecimal plafondIndemniteMensuelleEuros,
        boolean droitsSociauxMaintenus,
        boolean deconnexionDefinie,
        int effectifEntreprise,

        // Outputs analytiques
        TeletravailBeCct85149Verdict verdict,
        boolean conventionRespectee,
        boolean equipementRespecte,
        boolean droitsRespectes,
        boolean deconnexionRespectee,
        BigDecimal indemniteExcedentaire,

        // Synthèse + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
