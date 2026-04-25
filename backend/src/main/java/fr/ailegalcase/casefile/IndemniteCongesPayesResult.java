package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-DT-26-01 : résultat du calcul de l'indemnité compensatrice de congés payés
 * (art. L.3141-26 et L.3141-28 Code du travail).
 */
public record IndemniteCongesPayesResult(
        BigDecimal totalRemunerationPeriodeEur,
        int joursAcquisAnnee,
        int joursPris,
        BigDecimal salaireMensuelBrutEur,
        LocalDate dateRupture,
        IndemniteCongesPayesMethode methodeForcee,
        int joursDus,
        BigDecimal montantMethodeDixPourcentEur,
        BigDecimal montantMethodeMaintienEur,
        IndemniteCongesPayesMethode methodeRetenue,
        BigDecimal montantIndemniteEur,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
