package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-212-31 : requête HTTP pour l'endpoint de vérification de conformité
 * du processus électoral CSE (F-DT-65-elections-cse-conformite, FRANCE —
 * L. 2314-1 à L. 2314-37 CT ; R. 2314-1+ CT ; ordonnances Macron 22/09/2017
 * instituant le CSE).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record ElectionsCseConformiteRequest(
        Integer effectifEntreprise,
        Boolean papNegocieAvecOS,
        Boolean delaiInvitationOSRespectee,
        Boolean collegesConformes,
        Boolean resultatsContestes,
        LocalDate dateElection,
        String motifContestation
) {

    ElectionsCseConformiteInput toInput() {
        return new ElectionsCseConformiteInput(
                effectifEntreprise,
                papNegocieAvecOS,
                delaiInvitationOSRespectee,
                collegesConformes,
                resultatsContestes,
                dateElection,
                motifContestation
        );
    }
}
