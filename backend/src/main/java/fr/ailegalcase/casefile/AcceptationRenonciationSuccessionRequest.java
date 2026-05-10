package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-210-03 : requête POST {@code /api/v1/case-files/{id}/acceptation-renonciation-succession}.
 */
public record AcceptationRenonciationSuccessionRequest(
        LocalDate dateOuvertureSuccession,
        String qualiteHeritier,
        Double actifBrutEur,
        Double passifEur,
        Boolean actesEquivalentAcceptationDejaPosesDetected,
        Boolean inventaireRealise,
        Boolean dettesIncertainesDetected,
        String intentionExprimee
) {}
