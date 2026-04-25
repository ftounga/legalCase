package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-DT-24-01 : input du calcul de validité d'une clause de non-concurrence (FR).
 *
 * <p>Tous les booléens sont nullables (interprétés comme {@code false} par défaut),
 * permettant aux clients de ne transmettre que les champs renseignés.</p>
 */
public record NonConcurrenceInput(
        Boolean clausePresenteContrat,
        Boolean limiteTerritoireDefini,
        String territoireDescription,
        Boolean limiteDureeDefinie,
        Integer dureeMois,
        Boolean limiteObjetDefini,
        String objetDescription,
        Boolean contrepartieFinancierePresente,
        BigDecimal contrepartieMontantMensuelEur,
        BigDecimal salaireMensuelBrutEur,
        NonConcurrenceCalculator.SecteurActivite secteurActivite,
        LocalDate datePriseEffet
) {

    /** Builder utilitaire — facilite la lisibilité des tests unitaires. */
    public static class Builder {
        private Boolean clausePresenteContrat;
        private Boolean limiteTerritoireDefini;
        private String territoireDescription;
        private Boolean limiteDureeDefinie;
        private Integer dureeMois;
        private Boolean limiteObjetDefini;
        private String objetDescription;
        private Boolean contrepartieFinancierePresente;
        private BigDecimal contrepartieMontantMensuelEur;
        private BigDecimal salaireMensuelBrutEur;
        private NonConcurrenceCalculator.SecteurActivite secteurActivite;
        private LocalDate datePriseEffet;

        public Builder clausePresenteContrat(Boolean v) { this.clausePresenteContrat = v; return this; }
        public Builder limiteTerritoireDefini(Boolean v) { this.limiteTerritoireDefini = v; return this; }
        public Builder territoireDescription(String v) { this.territoireDescription = v; return this; }
        public Builder limiteDureeDefinie(Boolean v) { this.limiteDureeDefinie = v; return this; }
        public Builder dureeMois(Integer v) { this.dureeMois = v; return this; }
        public Builder limiteObjetDefini(Boolean v) { this.limiteObjetDefini = v; return this; }
        public Builder objetDescription(String v) { this.objetDescription = v; return this; }
        public Builder contrepartieFinancierePresente(Boolean v) { this.contrepartieFinancierePresente = v; return this; }
        public Builder contrepartieMontantMensuelEur(BigDecimal v) { this.contrepartieMontantMensuelEur = v; return this; }
        public Builder salaireMensuelBrutEur(BigDecimal v) { this.salaireMensuelBrutEur = v; return this; }
        public Builder secteurActivite(NonConcurrenceCalculator.SecteurActivite v) { this.secteurActivite = v; return this; }
        public Builder datePriseEffet(LocalDate v) { this.datePriseEffet = v; return this; }

        public NonConcurrenceInput build() {
            return new NonConcurrenceInput(
                    clausePresenteContrat, limiteTerritoireDefini, territoireDescription,
                    limiteDureeDefinie, dureeMois, limiteObjetDefini, objetDescription,
                    contrepartieFinancierePresente, contrepartieMontantMensuelEur,
                    salaireMensuelBrutEur, secteurActivite, datePriseEffet
            );
        }
    }
}
