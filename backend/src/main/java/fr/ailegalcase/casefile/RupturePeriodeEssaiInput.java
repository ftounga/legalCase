package fr.ailegalcase.casefile;

import java.time.LocalDate;

import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.AuteurRupture;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.CategorieSocioProfessionnelle;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.DiscriminationMotif;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.TypeContrat;

/**
 * SF-DT-38-01 : input du calcul de qualification d'une rupture pendant la
 * période d'essai (FR).
 *
 * <p>Champs obligatoires (validés par le Calculator) : {@code categorieSocioProfessionnelle},
 * {@code typeContrat}, {@code dateDebutContrat}, {@code dateRupture},
 * {@code dureePeriodeEssaiContractuelleMois}, {@code auteurRupture}.</p>
 *
 * <p>Les autres booléens sont nullables ; les dates et commentaires sont
 * nullables. Le pays est dérivé du workspace côté service — pas transmis ici.</p>
 */
public record RupturePeriodeEssaiInput(
        CategorieSocioProfessionnelle categorieSocioProfessionnelle,
        TypeContrat typeContrat,
        Integer dureeCddMois,
        LocalDate dateDebutContrat,
        LocalDate dateRupture,
        Integer dureePeriodeEssaiContractuelleMois,
        Boolean renouvellementInvoque,
        Boolean accordBrancheRenouvellement,
        Boolean accordEcritSalarieRenouvellement,
        AuteurRupture auteurRupture,
        Integer delaiPrevenanceJoursAppliques,
        String motifInvoque,
        Boolean motifLieAuxCompetencesProfessionnelles,
        Boolean motifEconomiqueOuOrganisationnel,
        DiscriminationMotif discriminationInvoquee,
        Boolean grossesseAuMomentRupture,
        Boolean arretAccidentTravailEnCours,
        String atteinteLiberteFondamentale,
        Boolean lettreRuptureMotivee,
        Boolean motifsAveresParPieces,
        Boolean conventionCollectiveApplicable,
        Boolean conventionCollectivePlusFavorableRespectee,
        Double salaireMensuelBrut
) {

    /** Builder utilitaire — facilite la lisibilité des tests unitaires. */
    public static class Builder {
        private CategorieSocioProfessionnelle categorieSocioProfessionnelle;
        private TypeContrat typeContrat;
        private Integer dureeCddMois;
        private LocalDate dateDebutContrat;
        private LocalDate dateRupture;
        private Integer dureePeriodeEssaiContractuelleMois;
        private Boolean renouvellementInvoque;
        private Boolean accordBrancheRenouvellement;
        private Boolean accordEcritSalarieRenouvellement;
        private AuteurRupture auteurRupture;
        private Integer delaiPrevenanceJoursAppliques;
        private String motifInvoque;
        private Boolean motifLieAuxCompetencesProfessionnelles;
        private Boolean motifEconomiqueOuOrganisationnel;
        private DiscriminationMotif discriminationInvoquee;
        private Boolean grossesseAuMomentRupture;
        private Boolean arretAccidentTravailEnCours;
        private String atteinteLiberteFondamentale;
        private Boolean lettreRuptureMotivee;
        private Boolean motifsAveresParPieces;
        private Boolean conventionCollectiveApplicable;
        private Boolean conventionCollectivePlusFavorableRespectee;
        private Double salaireMensuelBrut;

        public Builder categorieSocioProfessionnelle(CategorieSocioProfessionnelle v) { this.categorieSocioProfessionnelle = v; return this; }
        public Builder typeContrat(TypeContrat v) { this.typeContrat = v; return this; }
        public Builder dureeCddMois(Integer v) { this.dureeCddMois = v; return this; }
        public Builder dateDebutContrat(LocalDate v) { this.dateDebutContrat = v; return this; }
        public Builder dateRupture(LocalDate v) { this.dateRupture = v; return this; }
        public Builder dureePeriodeEssaiContractuelleMois(Integer v) { this.dureePeriodeEssaiContractuelleMois = v; return this; }
        public Builder renouvellementInvoque(Boolean v) { this.renouvellementInvoque = v; return this; }
        public Builder accordBrancheRenouvellement(Boolean v) { this.accordBrancheRenouvellement = v; return this; }
        public Builder accordEcritSalarieRenouvellement(Boolean v) { this.accordEcritSalarieRenouvellement = v; return this; }
        public Builder auteurRupture(AuteurRupture v) { this.auteurRupture = v; return this; }
        public Builder delaiPrevenanceJoursAppliques(Integer v) { this.delaiPrevenanceJoursAppliques = v; return this; }
        public Builder motifInvoque(String v) { this.motifInvoque = v; return this; }
        public Builder motifLieAuxCompetencesProfessionnelles(Boolean v) { this.motifLieAuxCompetencesProfessionnelles = v; return this; }
        public Builder motifEconomiqueOuOrganisationnel(Boolean v) { this.motifEconomiqueOuOrganisationnel = v; return this; }
        public Builder discriminationInvoquee(DiscriminationMotif v) { this.discriminationInvoquee = v; return this; }
        public Builder grossesseAuMomentRupture(Boolean v) { this.grossesseAuMomentRupture = v; return this; }
        public Builder arretAccidentTravailEnCours(Boolean v) { this.arretAccidentTravailEnCours = v; return this; }
        public Builder atteinteLiberteFondamentale(String v) { this.atteinteLiberteFondamentale = v; return this; }
        public Builder lettreRuptureMotivee(Boolean v) { this.lettreRuptureMotivee = v; return this; }
        public Builder motifsAveresParPieces(Boolean v) { this.motifsAveresParPieces = v; return this; }
        public Builder conventionCollectiveApplicable(Boolean v) { this.conventionCollectiveApplicable = v; return this; }
        public Builder conventionCollectivePlusFavorableRespectee(Boolean v) { this.conventionCollectivePlusFavorableRespectee = v; return this; }
        public Builder salaireMensuelBrut(Double v) { this.salaireMensuelBrut = v; return this; }

        public RupturePeriodeEssaiInput build() {
            return new RupturePeriodeEssaiInput(
                    categorieSocioProfessionnelle, typeContrat, dureeCddMois,
                    dateDebutContrat, dateRupture, dureePeriodeEssaiContractuelleMois,
                    renouvellementInvoque, accordBrancheRenouvellement, accordEcritSalarieRenouvellement,
                    auteurRupture, delaiPrevenanceJoursAppliques,
                    motifInvoque, motifLieAuxCompetencesProfessionnelles,
                    motifEconomiqueOuOrganisationnel, discriminationInvoquee,
                    grossesseAuMomentRupture, arretAccidentTravailEnCours,
                    atteinteLiberteFondamentale, lettreRuptureMotivee, motifsAveresParPieces,
                    conventionCollectiveApplicable, conventionCollectivePlusFavorableRespectee,
                    salaireMensuelBrut
            );
        }
    }
}
