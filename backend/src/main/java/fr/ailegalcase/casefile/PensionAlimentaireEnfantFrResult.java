package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-216-03 : résultat du calcul de la pension alimentaire enfant FR
 * (art. 371-2 Cciv + barème indicatif Cass.).
 */
public record PensionAlimentaireEnfantFrResult(
        List<Integer> montantParEnfantMensuelEur,
        int totalMensuelEur,
        double tauxApplique,
        double coefficientResidence,
        String parentDebiteur,
        String baseJuridique,
        List<String> messages,
        List<String> alertes
) {}
