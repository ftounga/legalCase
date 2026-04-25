package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-DT-20-01 : résultat consolidé du calcul du rappel de salaire FR.
 */
public record RappelSalaireResult(
        LocalDate periodeDebut,
        LocalDate periodeFin,
        BigDecimal montantSalaireDuMensuelEur,
        BigDecimal montantSalairePerVerseMensuelEur,
        String conventionCollectiveCode,
        Integer ancienneteAnneesPrime,
        boolean indexInseeRevalorise,
        BigDecimal tauxRevalorisationPct,
        RappelSalaireMethodeCpSurRappel methodeCpSurRappel,
        int nbMoisPeriode,
        BigDecimal differentielMensuelEur,
        BigDecimal totalRappelBrutHorsRevalorisationEur,
        BigDecimal montantRevalorisationEur,
        BigDecimal primeAncienneteEur,
        BigDecimal totalRappelBrutEur,
        BigDecimal congesPayesSurRappelEur,
        BigDecimal totalAvecCpEur,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
