package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-DT-31-01 : requête HTTP pour l'endpoint d'analyse de validité d'un
 * protocole transactionnel (FRANCE).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record TransactionRequest(
        LocalDate dateSignature,
        List<TransactionCalculator.ConcessionEmployeur> concessionsEmployeur,
        List<TransactionCalculator.ConcessionSalarie> concessionsSalarie,
        BigDecimal indemniteTransactionnelleEur,
        BigDecimal salaireMensuelBrutEur,
        Integer ancienneteAnnees,
        Boolean renonciationActionExpresse,
        Boolean delaiReflexion15jOk,
        TransactionCalculator.RupturePrealable rupturePrealable,
        Boolean presenceAvocatAssistance,
        Boolean viceConsentementAllegue
) {

    TransactionInput toInput() {
        return new TransactionInput(
                dateSignature,
                concessionsEmployeur,
                concessionsSalarie,
                indemniteTransactionnelleEur,
                salaireMensuelBrutEur,
                ancienneteAnnees,
                renonciationActionExpresse,
                delaiReflexion15jOk,
                rupturePrealable,
                presenceAvocatAssistance,
                viceConsentementAllegue
        );
    }
}
