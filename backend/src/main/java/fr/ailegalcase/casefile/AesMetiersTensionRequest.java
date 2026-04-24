package fr.ailegalcase.casefile;

import java.time.LocalDate;

public record AesMetiersTensionRequest(
        LocalDate dateEntreeFrance,
        Integer moisActiviteSalarieeDernieres24Mois,
        Boolean metierEstEnTension,
        String codeMetier,
        Boolean menaceOrdrePublic,
        Boolean contratOuPromesseValide,
        LocalDate dateDepotDemande
) {}
