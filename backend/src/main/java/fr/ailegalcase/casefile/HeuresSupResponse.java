package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record HeuresSupResponse(
        UUID caseFileId,
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
