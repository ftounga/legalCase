package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-206-05 : input du calcul d'analyse d'une prise d'acte de la rupture aux
 * torts de l'employeur (FRANCE).
 *
 * <p>Booléens nullables — l'absence d'information n'est pas un grief retenu
 * (interprété comme "non coché par l'avocat").</p>
 */
public record PriseActeRuptureInput(
        Boolean defautPaiementSalaire,
        BigDecimal montantImpayesEur,
        Boolean harcelement,
        Boolean manquementSecurite,
        Boolean modificationUnilateraleContrat,
        Boolean declassement,
        Boolean discrimination,
        Boolean heuresSupNonPayees,
        Boolean nonRespectDureesRepos,
        Boolean griefsActuelsEtPersistants,
        Boolean griefRendImpossiblePoursuite,
        String griefsCommentaire
) {

    /** Builder utilitaire — facilite la lisibilité des tests unitaires. */
    public static class Builder {
        private Boolean defautPaiementSalaire;
        private BigDecimal montantImpayesEur;
        private Boolean harcelement;
        private Boolean manquementSecurite;
        private Boolean modificationUnilateraleContrat;
        private Boolean declassement;
        private Boolean discrimination;
        private Boolean heuresSupNonPayees;
        private Boolean nonRespectDureesRepos;
        private Boolean griefsActuelsEtPersistants;
        private Boolean griefRendImpossiblePoursuite;
        private String griefsCommentaire;

        public Builder defautPaiementSalaire(Boolean v) { this.defautPaiementSalaire = v; return this; }
        public Builder montantImpayesEur(BigDecimal v) { this.montantImpayesEur = v; return this; }
        public Builder harcelement(Boolean v) { this.harcelement = v; return this; }
        public Builder manquementSecurite(Boolean v) { this.manquementSecurite = v; return this; }
        public Builder modificationUnilateraleContrat(Boolean v) { this.modificationUnilateraleContrat = v; return this; }
        public Builder declassement(Boolean v) { this.declassement = v; return this; }
        public Builder discrimination(Boolean v) { this.discrimination = v; return this; }
        public Builder heuresSupNonPayees(Boolean v) { this.heuresSupNonPayees = v; return this; }
        public Builder nonRespectDureesRepos(Boolean v) { this.nonRespectDureesRepos = v; return this; }
        public Builder griefsActuelsEtPersistants(Boolean v) { this.griefsActuelsEtPersistants = v; return this; }
        public Builder griefRendImpossiblePoursuite(Boolean v) { this.griefRendImpossiblePoursuite = v; return this; }
        public Builder griefsCommentaire(String v) { this.griefsCommentaire = v; return this; }

        public PriseActeRuptureInput build() {
            return new PriseActeRuptureInput(
                    defautPaiementSalaire,
                    montantImpayesEur,
                    harcelement,
                    manquementSecurite,
                    modificationUnilateraleContrat,
                    declassement,
                    discrimination,
                    heuresSupNonPayees,
                    nonRespectDureesRepos,
                    griefsActuelsEtPersistants,
                    griefRendImpossiblePoursuite,
                    griefsCommentaire
            );
        }
    }
}
