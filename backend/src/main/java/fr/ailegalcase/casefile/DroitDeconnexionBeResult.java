package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-219-19 : résultat brut produit par
 * {@link DroitDeconnexionBeChecker} — fonction <b>pure</b> (sans
 * dépendance HTTP / persistance / horloge).
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link DroitDeconnexionBeService} dans
 * {@link DroitDeconnexionBeResponse}).</p>
 *
 * <p>Outputs dérivés : ventilation par dimension de conformité (seuil
 * 20 travailleurs, instrument formalisé, contenu complet 3 thèmes
 * art. 16 § 2, consultation organe de concertation).</p>
 */
public record DroitDeconnexionBeResult(
        // Inputs (snapshot)
        int effectifEntreprise,
        DroitDeconnexionBeStatutAccord statutAccord,
        LocalDate dateEntreeVigueurInstrument,
        boolean modalitesPratiquesDeconnexionDefinies,
        boolean sensibilisationFormationPrevue,
        boolean modalitesOrganisationTravailDefinies,
        boolean consultationOrganeConcertationEffectuee,
        boolean manquementSignaleCbeOuSpf,

        // Outputs analytiques
        DroitDeconnexionBeVerdict verdict,
        boolean seuilAtteint,
        boolean instrumentFormalise,
        boolean contenuComplet,
        boolean modalitesPratiquesRespectees,
        boolean sensibilisationRespectee,
        boolean modalitesOrganisationRespectees,
        boolean consultationRespectee,

        // Synthèse + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
