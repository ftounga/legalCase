package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-222-01 : résultat du calcul ASF (art. L. 523-1 CSS).
 */
public record AsfCafResult(
        VerdictAsfEnum verdict,
        int montantMensuelEstime,
        boolean recouvrementApplicable,
        List<String> basesJuridiques,
        List<String> messages
) {}
