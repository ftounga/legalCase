package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-18 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/semaine-4-jours-be}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219 (miroir
 * {@link TeletravailBeCct85149Response} SF-219-16).</p>
 */
public record Semaine4JoursBeResponse(
        UUID caseFileId,

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

        // Outputs
        Semaine4JoursBeVerdict verdict,
        boolean eligibiliteRespectee,
        boolean demandeRespectee,
        boolean journeeRespectee,
        boolean formalisationRespectee,
        boolean dureeRespectee,
        BigDecimal journeeMaximaleAutoriseeHeures,

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
