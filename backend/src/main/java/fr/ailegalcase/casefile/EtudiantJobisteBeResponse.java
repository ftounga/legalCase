package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-13 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/etudiant-jobiste-be}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219 (miroir {@link
 * FlexiJobBeResponse} SF-219-12).</p>
 */
public record EtudiantJobisteBeResponse(
        UUID caseFileId,

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

        // Outputs
        EtudiantJobisteBeVerdict verdict,
        boolean statutEligible,
        boolean quotaRespecte,
        boolean formalismeRespecte,
        boolean cotisationsConformes,
        int heuresRestantesAvantContrat,
        int heuresHorsQuota,
        BigDecimal montantRedressementEstime,

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
