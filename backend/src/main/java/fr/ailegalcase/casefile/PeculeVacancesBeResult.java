package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-20 : résultat brut produit par {@link PeculeVacancesBeCalculator}
 * — fonction <b>pure</b> (sans dépendance HTTP / persistance / horloge).
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link PeculeVacancesBeService} dans
 * {@link PeculeVacancesBeResponse}).</p>
 *
 * <p>Outputs : verdict + ventilation par type de pécule (simple /
 * double / départ), fractions appliquées, base juridique stable.</p>
 */
public record PeculeVacancesBeResult(
        // Inputs (snapshot)
        PeculeVacancesBeStatut statut,
        PeculeVacancesBeTypeCalcul typeCalcul,
        BigDecimal remunerationBruteAnnuelleExerciceEur,
        BigDecimal remunerationMensuelleBruteEur,
        BigDecimal joursCongesPris,
        boolean peculeDejaPaye,
        BigDecimal remunerationBruteAnnuelleExercicePrecedentEur,
        LocalDate dateSortie,
        LocalDate dateFinContrat,
        LocalDate dateReclamation,

        // Outputs calculés
        BigDecimal montantPeculeSimpleEur,
        BigDecimal montantDoublePeculeEur,
        BigDecimal montantPeculeDepartEur,
        BigDecimal montantTotalDuEur,
        BigDecimal fractionLegaleAppliquee,
        boolean prescrit,
        long joursDepuisFaitGenerateur,

        // Verdict + synthèse
        PeculeVacancesBeVerdict verdict,
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
