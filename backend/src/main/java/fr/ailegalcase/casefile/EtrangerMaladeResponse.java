package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-214-01 : réponse de l'analyse étranger malade L. 425-9 CESEDA.
 */
public record EtrangerMaladeResponse(
        UUID caseFileId,
        String pathologiePrincipale,
        String paysOrigine,
        boolean traitementDisponiblePaysOrigine,
        String avisOFII,
        LocalDate dateAvisOFII,
        String country,
        String verdict,
        LocalDate delaiRecoursTA,
        String motifRecours,
        List<String> chipsCriteresNonRemplis,
        String baseJuridique
) {}
