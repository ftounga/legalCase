package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-DT-19-01 : requête de calcul de rappel pour heures supplémentaires (FR + BE).
 * Les champs FR et BE sont mutuellement exclusifs et routés selon le country du workspace.
 */
public record HeuresSupRequest(
        BigDecimal tauxHoraireBrut,
        // FR
        Integer heuresSupDeclarees25pct,
        Integer heuresSupDeclarees50pct,
        Integer heuresHorsContingent,
        BigDecimal tauxMajoration25,
        BigDecimal tauxMajoration50,
        // BE
        Integer heuresSupSemaine,
        Integer heuresDimancheJoursFeries
) {}
