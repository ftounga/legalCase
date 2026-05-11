package fr.ailegalcase.casefile;

public record TribunalFamilleBeMesuresProvisoiresRequest(
        Boolean violenceFamiliale,
        Boolean deplacementEnfantImminent,
        Boolean dilapidationPatrimoine,
        Boolean besoinResidenceSeparee,
        Boolean besoinContributionAlimentaire,
        Boolean besoinAutoriteParentaleExclusive
) {}
