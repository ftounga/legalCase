package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-211-03 : résultat de l'analyse des mesures provisoires devant le tribunal
 * de la famille belge — CJ art. 1280 + art. 1253ter+.
 * Outil <b>single-country BE</b>.
 */
public record TribunalFamilleBeMesuresProvisoiresResult(
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
