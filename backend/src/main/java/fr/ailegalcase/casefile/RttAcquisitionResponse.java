package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-218-49 : réponse de l'outil "RTT — acquisition selon accord d'aménagement"
 * (art. L.3121-41 à L.3121-44 CT, F-DT-80). Outil <b>FRANCE UNIQUEMENT</b>.
 */
public record RttAcquisitionResponse(
        UUID caseFileId,
        RttAcquisitionStatut statut,
        double horaireHebdomadaireCollectif,
        boolean accordCollectifPresent,
        int semainesTravailleesAn,
        Double nombreJrttTheorique,
        String base,
        List<String> notes,
        String country,
        String baseJuridique
) {}
