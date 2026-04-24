package fr.ailegalcase.casefile;

import java.time.LocalDate;

public record Belgian9terRequest(
        LocalDate dateDebutSymptomes,
        Boolean maladieGraveCertifiee,
        Boolean soinsNecessairesDisponiblesBe,
        Boolean soinsInaccessiblesPaysOrigine,
        Boolean menaceOrdrePublic,
        LocalDate dateDepotDemande
) {}
