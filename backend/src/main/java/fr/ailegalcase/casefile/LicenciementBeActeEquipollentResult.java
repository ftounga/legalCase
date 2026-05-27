package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-213-09 : résultat brut produit par
 * {@link LicenciementBeActeEquipollentAnalyzer} — fonction <b>pure</b>.
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté par
 * {@link LicenciementBeActeEquipollentService} dans
 * {@link LicenciementBeActeEquipollentResponse}).</p>
 *
 * <p>{@code icpIndicatif} est {@code null} si le verdict n'est pas
 * {@link LicenciementBeActeEquipollentVerdict#ACTE_EQUIPOLLENT_PROBABLE}
 * ou si la rémunération hebdomadaire / la durée de préavis n'a pas été
 * fournie. {@code avertissement} est {@code null} si aucune alerte n'est
 * levée.</p>
 */
public record LicenciementBeActeEquipollentResult(
        TypeModificationUnilateraleEnum typeModification,
        AmpleurModificationEnum ampleurModification,
        boolean elementEssentielDuContrat,
        LocalDate dateModification,
        boolean salarieAProteste,
        Integer delaiDepuisModificationJours,
        BigDecimal remunerationHebdomadaireBrute,
        Integer dureePreavisCalculeeSemaines,

        LicenciementBeActeEquipollentVerdict verdict,
        String fondementJuridique,
        BigDecimal icpIndicatif,
        boolean risqueAcceptationTacite,
        int delaiRecommandeProtestationJours,
        String baseJuridique,
        String avertissement
) {
}
