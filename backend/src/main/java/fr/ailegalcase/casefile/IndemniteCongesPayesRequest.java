package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-DT-26-01 : requête de calcul de l'indemnité compensatrice de congés payés
 * (art. L.3141-26 et s. Code du travail).
 *
 * @param totalRemunerationPeriodeEur somme rémunération brute période de référence (>0)
 * @param joursAcquisAnnee            jours CP acquis (≥ 0)
 * @param joursPris                   jours CP déjà pris (≥ 0, ≤ joursAcquisAnnee)
 * @param salaireMensuelBrutEur       salaire mensuel brut de référence (>0)
 * @param dateRupture                 date de rupture (informative)
 * @param methodeForcee               nullable — si null calcul automatique le plus favorable
 */
public record IndemniteCongesPayesRequest(
        BigDecimal totalRemunerationPeriodeEur,
        Integer joursAcquisAnnee,
        Integer joursPris,
        BigDecimal salaireMensuelBrutEur,
        LocalDate dateRupture,
        IndemniteCongesPayesMethode methodeForcee
) {}
