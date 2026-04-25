package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-DT-31-01 : input du calcul de validité d'un protocole transactionnel
 * (art. 2044 à 2052 Cciv + Cass. soc. 16/12/2010 — concessions réciproques).
 *
 * <p>Tous les booléens sont nullables (interprétés comme {@code false} par défaut).
 * Les listes de concessions sont nullables (interprétées comme listes vides).</p>
 */
public record TransactionInput(
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

    /** Builder utilitaire — facilite la lisibilité des tests unitaires. */
    public static class Builder {
        private LocalDate dateSignature;
        private List<TransactionCalculator.ConcessionEmployeur> concessionsEmployeur;
        private List<TransactionCalculator.ConcessionSalarie> concessionsSalarie;
        private BigDecimal indemniteTransactionnelleEur;
        private BigDecimal salaireMensuelBrutEur;
        private Integer ancienneteAnnees;
        private Boolean renonciationActionExpresse;
        private Boolean delaiReflexion15jOk;
        private TransactionCalculator.RupturePrealable rupturePrealable;
        private Boolean presenceAvocatAssistance;
        private Boolean viceConsentementAllegue;

        public Builder dateSignature(LocalDate v) { this.dateSignature = v; return this; }
        public Builder concessionsEmployeur(List<TransactionCalculator.ConcessionEmployeur> v) { this.concessionsEmployeur = v; return this; }
        public Builder concessionsSalarie(List<TransactionCalculator.ConcessionSalarie> v) { this.concessionsSalarie = v; return this; }
        public Builder indemniteTransactionnelleEur(BigDecimal v) { this.indemniteTransactionnelleEur = v; return this; }
        public Builder salaireMensuelBrutEur(BigDecimal v) { this.salaireMensuelBrutEur = v; return this; }
        public Builder ancienneteAnnees(Integer v) { this.ancienneteAnnees = v; return this; }
        public Builder renonciationActionExpresse(Boolean v) { this.renonciationActionExpresse = v; return this; }
        public Builder delaiReflexion15jOk(Boolean v) { this.delaiReflexion15jOk = v; return this; }
        public Builder rupturePrealable(TransactionCalculator.RupturePrealable v) { this.rupturePrealable = v; return this; }
        public Builder presenceAvocatAssistance(Boolean v) { this.presenceAvocatAssistance = v; return this; }
        public Builder viceConsentementAllegue(Boolean v) { this.viceConsentementAllegue = v; return this; }

        public TransactionInput build() {
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
}
