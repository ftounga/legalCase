package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-222-03 : résultat de l'analyse des conditions de l'habilitation familiale
 * (art. 494-1 à 494-12 Cciv).
 */
public record HabilitationFamilialeResult(
        VerdictHabilitationFamilialeEnum verdict,
        ModaliteHabilitationEnum modalite,
        List<String> actesCouverts,
        List<String> conditionsManquantes,
        List<String> basesJuridiques,
        List<String> messages
) {}
