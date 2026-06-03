package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-220-01 : résultat de l'analyse du régime franco-tunisien (accord 17/03/1988).
 * Outil single-country FR.
 */
public record RegimeTunisienResult(
        String categorie,
        Integer dureeSejourEnvisageeMois,
        boolean titreEnCours,
        boolean dejaResident,
        String regime,
        List<String> particularitesApplicables,
        List<String> basesJuridiques,
        boolean renvoiDroitCommun,
        List<String> messages
) {}
