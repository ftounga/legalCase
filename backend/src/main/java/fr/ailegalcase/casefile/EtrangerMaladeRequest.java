package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-214-01 : requête POST pour l'analyse étranger malade L. 425-9 CESEDA.
 * Outil single-country FR.
 */
public record EtrangerMaladeRequest(
        LocalDate dateDepotDossierOFII,
        String pathologiePrincipale,
        String paysOrigine,
        Boolean traitementDisponiblePaysOrigine,
        Boolean avisOFIIRendu,
        String avisOFII,
        LocalDate dateAvisOFII
) {}
