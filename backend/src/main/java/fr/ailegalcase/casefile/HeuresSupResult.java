package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-DT-19-01 : résultat du calcul de rappel pour heures supplémentaires (FR + BE).
 * Les champs non pertinents selon le pays sont mis à zéro (jamais null) pour simplifier
 * la sérialisation et la consommation frontend.
 */
public record HeuresSupResult(
        BigDecimal tauxHoraireBrut,
        int heuresSupDeclarees25pct,
        int heuresSupDeclarees50pct,
        int heuresHorsContingent,
        BigDecimal tauxMajoration25,
        BigDecimal tauxMajoration50,
        int heuresSupSemaine,
        int heuresDimancheJoursFeries,
        String country,
        BigDecimal rappelMajoration25pct,
        BigDecimal rappelMajoration50pct,
        BigDecimal rappelMajoration100pct,
        BigDecimal rappelTotal,
        BigDecimal reposCompensateurHeuresDues,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
