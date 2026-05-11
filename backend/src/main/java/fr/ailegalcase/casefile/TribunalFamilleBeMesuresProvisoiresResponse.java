package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

public record TribunalFamilleBeMesuresProvisoiresResponse(
        UUID caseFileId,
        String country,
        boolean violenceFamiliale,
        boolean deplacementEnfantImminent,
        boolean dilapidationPatrimoine,
        boolean besoinResidenceSeparee,
        boolean besoinContributionAlimentaire,
        boolean besoinAutoriteParentaleExclusive,
        int scoreUrgence,
        String urgenceLevel,
        List<String> mesuresRecommandees,
        String verdict,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
