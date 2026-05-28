package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-14 : résultat brut produit par {@link InterimBeCct322Checker}
 * — fonction <b>pure</b> (sans dépendance HTTP / persistance / horloge).
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par {@link
 * InterimBeCct322Service} dans {@link InterimBeCct322Response}).</p>
 *
 * <p>Outputs dérivés : éligibilité de la mission (booléens granulaires
 * pour restitution UI), durée excédentaire en jours éventuelle au-delà
 * du plafond légal, écart de parité salariale en € (différence entre
 * référence permanent et taux intérimaire).</p>
 */
public record InterimBeCct322Result(
        // Inputs (snapshot)
        LocalDate dateDebutMission,
        InterimBeCct322MotifMission motifMission,
        boolean remplacementGreveOuLockout,
        int dureeTotaleMissionJours,
        int dureeMaxLegaleJours,
        BigDecimal salaireHoraireIntimaireBrut,
        BigDecimal salaireHoraireReferenceBrut,
        boolean contratEcritSigne,
        boolean dimonaDeclareeParEti,

        // Outputs analytiques
        InterimBeCct322Verdict verdict,
        boolean motifAutorise,
        boolean dureeRespectee,
        boolean pariteRespectee,
        boolean formalismeRespecte,
        int joursExcedentaires,
        BigDecimal ecartPariteSalariale,

        // Synthèse + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
