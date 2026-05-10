package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-210-03 : résultat de l'analyse d'option successorale (Cciv art. 768+).
 */
public record AcceptationRenonciationSuccessionResult(
        LocalDate dateOuvertureSuccession,
        String qualiteHeritier,
        double actifBrutEur,
        double passifEur,
        boolean actesEquivalentAcceptationDejaPosesDetected,
        boolean inventaireRealise,
        boolean dettesIncertainesDetected,
        String intentionExprimee,
        List<String> optionsOuvertes,
        String optionRecommandee,
        int delaiRestantJours,
        int delaiTotalJours,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
