package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-DT-20-01 : requête de calcul du rappel de salaire FR
 * (art. L.3242-1 + L.3245-1 + L.3141-26 Code du travail + CCN).
 *
 * @param periodeDebut                     premier mois de la période de rappel (inclus)
 * @param periodeFin                       dernier mois de la période de rappel (inclus)
 * @param montantSalaireDuMensuelEur       salaire mensuel dû (&gt; 0)
 * @param montantSalairePerVerseMensuelEur salaire mensuel effectivement perçu (≥ 0, &lt; dû)
 * @param conventionCollectiveCode         code CCN — nullable (sera normalisé)
 * @param ancienneteAnneesPrime            ancienneté (années) servant à appliquer la prime CCN
 * @param indexInseeRevalorise             true si la revalorisation INSEE doit être appliquée
 * @param tauxRevalorisationPct            taux saisi par l'avocat (0..100), nullable si pas de revalorisation
 * @param methodeCpSurRappel               méthode de calcul des CP sur le rappel
 */
public record RappelSalaireRequest(
        LocalDate periodeDebut,
        LocalDate periodeFin,
        BigDecimal montantSalaireDuMensuelEur,
        BigDecimal montantSalairePerVerseMensuelEur,
        String conventionCollectiveCode,
        Integer ancienneteAnneesPrime,
        Boolean indexInseeRevalorise,
        BigDecimal tauxRevalorisationPct,
        RappelSalaireMethodeCpSurRappel methodeCpSurRappel
) {}
