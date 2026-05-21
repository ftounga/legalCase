package fr.ailegalcase.casefile;

import java.time.LocalDate;

import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.AuteurRupture;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.CategorieSocioProfessionnelle;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.DiscriminationMotif;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.TypeContrat;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.TypeContratPrecedent;

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
        Double salaireMensuelBrut,
        // SF-252-01 — 7 nouveaux champs pour les 6 protections nullité additionnelles
        // (audit F-DT-38 du 2026-05-20). Tous nullables.
        Boolean salarieProtege,                       // L.2411-1 (élu CSE, DS, etc.)
        Boolean autorisationInspectionTravailObtenue, // L.2411-1 (autorisation reçue ?)
        Boolean lanceurAlerte,                        // L.1132-3-3
        Boolean temoinOuVictimeHarcelement,           // L.1132-3-1 / L.1152-2 / L.1153-2-3
        Boolean droitDeRetraitExerce,                 // L.4131-3
        Boolean grossesseDeclareePostRupture,         // L.1225-5 (déclarée APRÈS rupture)
        LocalDate dateNotificationGrossesse,          // L.1225-5 (date notif employeur)
        // SF-252b-01 — 1 nouveau champ pour barème CDD/INTERIM précis (audit 2026-05-20).
        // Pour CDD (L.1242-10) ou INTERIM (L.1251-14), l'essai contractuel est souvent
        // exprimé en jours dans le contrat. Si renseigné, utilisé en priorité ;
        // sinon `dureePeriodeEssaiContractuelleMois × 30` fait office d'approximation.
        Integer dureePeriodeEssaiContractuelleJours,
        // SF-252c-01 — 3 nouveaux champs pour les gaps moyens (audit 2026-05-20).
        // Cumul des jours de suspension du contrat (arrêt maladie, congés non rémunérés,
        // grève) qui prolongent d'autant la fin de la période d'essai
        // (Cass. soc., 31/01/2018, n° 16-19.836).
        Integer joursSuspensionContrat,
        // Ancienneté (en mois) d'un stage > 2 mois OU d'un CDD précédent dans la même
        // entreprise et même fonction, déduite de la durée de la période d'essai
        // (L.1243-11 + Cass. soc., 09/10/2013, n° 12-19.512).
        Integer ancienneteContratPrecedentMois,
        // Type du contrat précédent ayant donné lieu à la reprise d'ancienneté.
        TypeContratPrecedent typeContratPrecedent
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
        // SF-252-01
        private Boolean salarieProtege;
        private Boolean autorisationInspectionTravailObtenue;
        private Boolean lanceurAlerte;
        private Boolean temoinOuVictimeHarcelement;
        private Boolean droitDeRetraitExerce;
        private Boolean grossesseDeclareePostRupture;
        private LocalDate dateNotificationGrossesse;
        // SF-252b-01
        private Integer dureePeriodeEssaiContractuelleJours;
        // SF-252c-01
        private Integer joursSuspensionContrat;
        private Integer ancienneteContratPrecedentMois;
        private TypeContratPrecedent typeContratPrecedent;

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
        public Builder salarieProtege(Boolean v) { this.salarieProtege = v; return this; }
        public Builder autorisationInspectionTravailObtenue(Boolean v) { this.autorisationInspectionTravailObtenue = v; return this; }
        public Builder lanceurAlerte(Boolean v) { this.lanceurAlerte = v; return this; }
        public Builder temoinOuVictimeHarcelement(Boolean v) { this.temoinOuVictimeHarcelement = v; return this; }
        public Builder droitDeRetraitExerce(Boolean v) { this.droitDeRetraitExerce = v; return this; }
        public Builder grossesseDeclareePostRupture(Boolean v) { this.grossesseDeclareePostRupture = v; return this; }
        public Builder dateNotificationGrossesse(LocalDate v) { this.dateNotificationGrossesse = v; return this; }
        public Builder dureePeriodeEssaiContractuelleJours(Integer v) { this.dureePeriodeEssaiContractuelleJours = v; return this; }
        public Builder joursSuspensionContrat(Integer v) { this.joursSuspensionContrat = v; return this; }
        public Builder ancienneteContratPrecedentMois(Integer v) { this.ancienneteContratPrecedentMois = v; return this; }
        public Builder typeContratPrecedent(TypeContratPrecedent v) { this.typeContratPrecedent = v; return this; }

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
                    salaireMensuelBrut,
                    salarieProtege, autorisationInspectionTravailObtenue,
                    lanceurAlerte, temoinOuVictimeHarcelement, droitDeRetraitExerce,
                    grossesseDeclareePostRupture, dateNotificationGrossesse,
                    dureePeriodeEssaiContractuelleJours,
                    joursSuspensionContrat, ancienneteContratPrecedentMois,
                    typeContratPrecedent
            );
        }
    }
}
