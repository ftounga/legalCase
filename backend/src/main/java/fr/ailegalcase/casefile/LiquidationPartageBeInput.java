package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-217-02 : input du suivi de la procédure de liquidation-partage post-divorce
 * belge devant notaire commis (Code judiciaire art. 1207 et s.).
 *
 * <p>Les booléens d'étape sont nullables (interprétés comme {@code false} par
 * défaut) ; les dates et le commentaire sont nullables. Le pays est dérivé du
 * workspace côté service — pas transmis ici.</p>
 */
public record LiquidationPartageBeInput(
        Boolean notaireDesigne,
        LocalDate dateDesignationNotaire,
        Boolean operationsOuvertes,
        LocalDate dateOuvertureOperations,
        Boolean inventaireEtabli,
        Boolean projetLiquidationEtabli,
        LocalDate dateNotificationProjet,
        Boolean contreditsDeposes,
        Boolean procesVerbalDiresEtabli,
        Boolean homologationDemandee,
        LocalDate dateHomologation,
        String commentaire
) {

    /** Builder utilitaire — facilite la lisibilité des tests unitaires. */
    public static class Builder {
        private Boolean notaireDesigne;
        private LocalDate dateDesignationNotaire;
        private Boolean operationsOuvertes;
        private LocalDate dateOuvertureOperations;
        private Boolean inventaireEtabli;
        private Boolean projetLiquidationEtabli;
        private LocalDate dateNotificationProjet;
        private Boolean contreditsDeposes;
        private Boolean procesVerbalDiresEtabli;
        private Boolean homologationDemandee;
        private LocalDate dateHomologation;
        private String commentaire;

        public Builder notaireDesigne(Boolean v) { this.notaireDesigne = v; return this; }
        public Builder dateDesignationNotaire(LocalDate v) { this.dateDesignationNotaire = v; return this; }
        public Builder operationsOuvertes(Boolean v) { this.operationsOuvertes = v; return this; }
        public Builder dateOuvertureOperations(LocalDate v) { this.dateOuvertureOperations = v; return this; }
        public Builder inventaireEtabli(Boolean v) { this.inventaireEtabli = v; return this; }
        public Builder projetLiquidationEtabli(Boolean v) { this.projetLiquidationEtabli = v; return this; }
        public Builder dateNotificationProjet(LocalDate v) { this.dateNotificationProjet = v; return this; }
        public Builder contreditsDeposes(Boolean v) { this.contreditsDeposes = v; return this; }
        public Builder procesVerbalDiresEtabli(Boolean v) { this.procesVerbalDiresEtabli = v; return this; }
        public Builder homologationDemandee(Boolean v) { this.homologationDemandee = v; return this; }
        public Builder dateHomologation(LocalDate v) { this.dateHomologation = v; return this; }
        public Builder commentaire(String v) { this.commentaire = v; return this; }

        public LiquidationPartageBeInput build() {
            return new LiquidationPartageBeInput(
                    notaireDesigne, dateDesignationNotaire,
                    operationsOuvertes, dateOuvertureOperations,
                    inventaireEtabli, projetLiquidationEtabli, dateNotificationProjet,
                    contreditsDeposes, procesVerbalDiresEtabli,
                    homologationDemandee, dateHomologation, commentaire);
        }
    }
}
