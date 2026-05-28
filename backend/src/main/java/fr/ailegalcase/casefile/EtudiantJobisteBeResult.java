package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-13 : résultat brut produit par {@link EtudiantJobisteBeChecker}
 * — fonction <b>pure</b>.
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par {@link
 * EtudiantJobisteBeService} dans {@link EtudiantJobisteBeResponse}).</p>
 *
 * <p>Outputs dérivés : heures restantes au quota annuel, heures hors
 * quota (déclenchement bascule régime ordinaire), montant indicatif
 * du redressement de cotisations sur l'excédent (sur base
 * remunerationBruteHoraire — ~50 % de cotisations ordinaires), booléens
 * granulaires d'éligibilité pour restitution UI.</p>
 */
public record EtudiantJobisteBeResult(
        // Inputs (snapshot)
        LocalDate dateDebutOccupation,
        EtudiantJobisteBeStatutEtudiant statutEtudiant,
        int heuresDejaPresteesDansAnnee,
        int heuresContratEnCours,
        int quotaAnnuelHeures,
        boolean contratEcritSigne,
        boolean dimonaStuDeclaree,
        boolean cotisationsReduitesAppliquees,
        BigDecimal remunerationBruteHoraire,

        // Outputs analytiques
        EtudiantJobisteBeVerdict verdict,
        boolean statutEligible,
        boolean quotaRespecte,
        boolean formalismeRespecte,
        boolean cotisationsConformes,
        int heuresRestantesAvantContrat,
        int heuresHorsQuota,
        BigDecimal montantRedressementEstime,

        // Synthèse + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
