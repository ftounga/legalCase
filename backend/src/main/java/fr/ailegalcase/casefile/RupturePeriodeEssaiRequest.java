package fr.ailegalcase.casefile;

import java.time.LocalDate;

import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.AuteurRupture;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.CategorieSocioProfessionnelle;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.DiscriminationMotif;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.TypeContrat;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.TypeContratPrecedent;

/**
 * SF-DT-38-01 : requête HTTP pour l'endpoint de qualification d'une rupture
 * pendant la période d'essai (FR).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record RupturePeriodeEssaiRequest(
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
        // SF-252-01 — 7 nouveaux champs (audit F-DT-38 2026-05-20)
        Boolean salarieProtege,
        Boolean autorisationInspectionTravailObtenue,
        Boolean lanceurAlerte,
        Boolean temoinOuVictimeHarcelement,
        Boolean droitDeRetraitExerce,
        Boolean grossesseDeclareePostRupture,
        LocalDate dateNotificationGrossesse,
        // SF-252b-01 — 1 nouveau champ pour barème CDD/INTERIM précis (audit 2026-05-20)
        Integer dureePeriodeEssaiContractuelleJours,
        // SF-252c-01 — 3 nouveaux champs (audit 2026-05-20)
        Integer joursSuspensionContrat,
        Integer ancienneteContratPrecedentMois,
        TypeContratPrecedent typeContratPrecedent
) {

    RupturePeriodeEssaiInput toInput() {
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
