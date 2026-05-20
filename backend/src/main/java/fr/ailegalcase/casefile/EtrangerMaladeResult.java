package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-214-01 : résultat de l'analyse étranger malade L. 425-9 CESEDA.
 * Outil single-country FR.
 */
public record EtrangerMaladeResult(
        String pathologiePrincipale,
        String paysOrigine,
        boolean traitementDisponiblePaysOrigine,
        String avisOFII,
        LocalDate dateAvisOFII,
        String verdict,
        LocalDate delaiRecoursTA,
        String motifRecours,
        List<String> chipsCriteresNonRemplis,
        String baseJuridique
) {}
