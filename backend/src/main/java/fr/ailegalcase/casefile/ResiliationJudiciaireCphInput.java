package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-206-07 : input du calcul d'analyse d'une demande de résiliation
 * judiciaire du contrat de travail aux torts de l'employeur (FRANCE — Cass.
 * soc. 16/03/1989 ; Cass. soc. 20/01/1998 ; art. L.1411-1 CT ; art. 1224 et
 * 1227-1228 C.civ.).
 *
 * <p>Booléens nullables — l'absence d'information n'est pas un manquement
 * retenu (interprété comme "non coché par l'avocat").</p>
 */
public record ResiliationJudiciaireCphInput(
        Boolean defautPaiementSalaire,
        BigDecimal montantImpayesEur,
        Boolean harcelement,
        Boolean manquementSecurite,
        Boolean modificationUnilateraleContrat,
        Boolean declassement,
        Boolean discrimination,
        Boolean heuresSupNonPayees,
        Boolean nonRespectDureesRepos,
        Boolean manquementsPersistantsAuJourDemande,
        Boolean salarieToujoursEnPoste,
        Boolean licenciementIntervenuEnCoursInstance,
        String manquementsCommentaire
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
        private Boolean manquementsPersistantsAuJourDemande;
        private Boolean salarieToujoursEnPoste;
        private Boolean licenciementIntervenuEnCoursInstance;
        private String manquementsCommentaire;

        public Builder defautPaiementSalaire(Boolean v) { this.defautPaiementSalaire = v; return this; }
        public Builder montantImpayesEur(BigDecimal v) { this.montantImpayesEur = v; return this; }
        public Builder harcelement(Boolean v) { this.harcelement = v; return this; }
        public Builder manquementSecurite(Boolean v) { this.manquementSecurite = v; return this; }
        public Builder modificationUnilateraleContrat(Boolean v) { this.modificationUnilateraleContrat = v; return this; }
        public Builder declassement(Boolean v) { this.declassement = v; return this; }
        public Builder discrimination(Boolean v) { this.discrimination = v; return this; }
        public Builder heuresSupNonPayees(Boolean v) { this.heuresSupNonPayees = v; return this; }
        public Builder nonRespectDureesRepos(Boolean v) { this.nonRespectDureesRepos = v; return this; }
        public Builder manquementsPersistantsAuJourDemande(Boolean v) { this.manquementsPersistantsAuJourDemande = v; return this; }
        public Builder salarieToujoursEnPoste(Boolean v) { this.salarieToujoursEnPoste = v; return this; }
        public Builder licenciementIntervenuEnCoursInstance(Boolean v) { this.licenciementIntervenuEnCoursInstance = v; return this; }
        public Builder manquementsCommentaire(String v) { this.manquementsCommentaire = v; return this; }

        public ResiliationJudiciaireCphInput build() {
            return new ResiliationJudiciaireCphInput(
                    defautPaiementSalaire,
                    montantImpayesEur,
                    harcelement,
                    manquementSecurite,
                    modificationUnilateraleContrat,
                    declassement,
                    discrimination,
                    heuresSupNonPayees,
                    nonRespectDureesRepos,
                    manquementsPersistantsAuJourDemande,
                    salarieToujoursEnPoste,
                    licenciementIntervenuEnCoursInstance,
                    manquementsCommentaire
            );
        }
    }
}
