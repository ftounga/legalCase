package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-19 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/droit-deconnexion-be}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219 (miroir
 * {@link Semaine4JoursBeResponse} SF-219-18).</p>
 */
public record DroitDeconnexionBeResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        int effectifEntreprise,
        DroitDeconnexionBeStatutAccord statutAccord,
        LocalDate dateEntreeVigueurInstrument,
        boolean modalitesPratiquesDeconnexionDefinies,
        boolean sensibilisationFormationPrevue,
        boolean modalitesOrganisationTravailDefinies,
        boolean consultationOrganeConcertationEffectuee,
        boolean manquementSignaleCbeOuSpf,

        // Outputs
        DroitDeconnexionBeVerdict verdict,
        boolean seuilAtteint,
        boolean instrumentFormalise,
        boolean contenuComplet,
        boolean modalitesPratiquesRespectees,
        boolean sensibilisationRespectee,
        boolean modalitesOrganisationRespectees,
        boolean consultationRespectee,

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
