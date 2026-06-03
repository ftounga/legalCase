package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-222-02 : résultat de l'analyse d'éligibilité TGD (art. 41-3-1 CPP).
 */
public record TgdResult(
        VerdictTgdEnum verdict,
        List<String> criteresManquants,
        List<String> basesJuridiques,
        List<String> messages
) {}
