package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-DT-28-01 : résultat de l'analyse cumulée des avantages conventionnels belges.
 *
 * <p>Couvre 4 postes :
 * <ul>
 *   <li>Pécule de vacances simple + double (Loi 28/06/1971)</li>
 *   <li>Prime de fin d'année (CCT sectorielle, ex. CP 200)</li>
 *   <li>Éco-chèques (CCT 98)</li>
 *   <li>Chèques-repas (CCT 19octies)</li>
 * </ul>
 */
public record AvantagesConventionnelsBeResult(
        BigDecimal salaireMensuelBrutEur,
        int joursTravaillesAnneePrecedente,
        int anciennetteMois,
        String commissionParitaire,
        int annee,
        boolean doublePeculeVacancesPercu,
        boolean primeFinAnneePrevueCcCp,
        boolean ecoChequesPrevuCcCp,
        boolean ecoChequesUtilisationDansAn,
        boolean chequesRepasPrevu,
        int joursPrestesEffectifs,
        BigDecimal peculeVacancesSimpleEur,
        BigDecimal doublePeculeVacancesEur,
        BigDecimal primeFinAnneeEur,
        BigDecimal ecoChequesValeurAnnuelleEur,
        BigDecimal chequesRepasValeurAnnuelleEur,
        BigDecimal totalAvantagesAnnuelsEur,
        String formule,
        String baseJuridique,
        List<String> messages,
        String country
) {}
