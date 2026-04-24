package fr.ailegalcase.casefile;

import java.time.LocalDate;

public record AesHumanitaireRequest(
        LocalDate dateEntreeFrance,
        MotifHumanitaire motifHumanitaireDominant,
        Boolean preuvesMedicales,
        Boolean preuvesViolencesOuTraite,
        Boolean demandeAsileDeposeeEtRejetee,
        Boolean commissionTitreSejourSaisie,
        Boolean menaceOrdrePublic,
        LocalDate dateDepotDemande
) {}
