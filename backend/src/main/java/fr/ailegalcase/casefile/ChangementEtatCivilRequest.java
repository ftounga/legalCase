package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-26-01 : requête de calcul de changement d'état civil
 * (art. 60 / 61-3-1 / 61-5 Cciv).
 */
public record ChangementEtatCivilRequest(
        String typeChangement,
        String motifInvoque,
        List<String> preuvesProduites,
        Boolean majeurDemandeur,
        Boolean consentementParental,
        Boolean datesDocsConcordants,
        Boolean dejaChangeAuparavant,
        LocalDate dateNaissanceDemandeur,
        String departementDeclaration
) {}
