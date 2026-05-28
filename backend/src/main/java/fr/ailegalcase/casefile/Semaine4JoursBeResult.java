package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-18 : résultat brut produit par {@link Semaine4JoursBeChecker}
 * — fonction <b>pure</b> (sans dépendance HTTP / persistance / horloge).
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par {@link
 * Semaine4JoursBeService} dans {@link Semaine4JoursBeResponse}).</p>
 *
 * <p>Outputs dérivés : ventilation par dimension de conformité,
 * journée maximale effectivement autorisée (9 h 30 par défaut,
 * 10 h si CCT le prévoit).</p>
 */
public record Semaine4JoursBeResult(
        // Inputs (snapshot)
        Semaine4JoursBeStatutDemande statutDemande,
        boolean travailleurTempsPlein,
        boolean demandeEcriteTravailleur,
        LocalDate dateDemande,
        BigDecimal dureeHebdomadaireHeures,
        BigDecimal journeeMaximaleHeures,
        boolean cctAutorise10h,
        boolean avenantEcritSigne,
        boolean reglementTravailModifie,
        BigDecimal dureeAvenantMois,
        boolean avenantRenouvele,
        boolean refusMotiveParEcrit,
        LocalDate dateLicenciement,
        boolean motifLicenciementObjectifEtabli,

        // Outputs analytiques
        Semaine4JoursBeVerdict verdict,
        boolean eligibiliteRespectee,
        boolean demandeRespectee,
        boolean journeeRespectee,
        boolean formalisationRespectee,
        boolean dureeRespectee,
        BigDecimal journeeMaximaleAutoriseeHeures,

        // Synthèse + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
