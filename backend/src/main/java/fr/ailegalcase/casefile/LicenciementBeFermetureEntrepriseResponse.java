package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-06 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-fermeture-entreprise}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219 (miroir {@link
 * CumulRccAllocationsResponse} SF-219-04).</p>
 */
public record LicenciementBeFermetureEntrepriseResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        LocalDate dateNaissance,
        LocalDate dateDebutContrat,
        LocalDate dateFermeture,
        BigDecimal remunerationMensuelleBrute,
        LicenciementBeFermetureEntrepriseTypeFermeture typeFermeture,
        LicenciementBeFermetureEntrepriseStatutEmployeur statutEmployeur,
        Integer effectifEtp,
        BigDecimal salairesImpayes,
        BigDecimal peculeVacancesImpaye,
        BigDecimal indemniteRuptureImpayee,

        // Outputs
        LicenciementBeFermetureEntrepriseVerdict verdict,
        boolean eligible,
        int ageADateFermeture,
        int anneesAnciennete,
        BigDecimal indemniteFermeture,
        BigDecimal montantForfaitaireParAnnee,
        BigDecimal supplementAgeMensuel,
        BigDecimal montantTotalCreancesFfe,

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
