package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-213-04 : résultat brut produit par
 * {@link LicenciementBeFormuleClaeysCalculator}.
 *
 * <p>Fonction pure — n'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link LicenciementBeFormuleClaeysService} dans
 * {@link LicenciementBeFormuleClaeysResponse}).</p>
 *
 * <p>Si la clause de sauvegarde n'est pas appliquée, les champs
 * {@code ancienneteAnneesPostStatutUnique}, {@code salaireHebdomadaireBrut},
 * {@code indemniteClaeysBrute} et {@code indemniteTotaleBrute} peuvent être
 * {@code null} et {@code preavisStatutUniquesSemaines = 0}.</p>
 */
public record LicenciementBeFormuleClaeysResult(
        int ancienneteAnneesPreStatutUnique,
        int ancienneteMoisPreStatutUnique,
        BigDecimal remunerationAnnuelleBruteEnMilliers,
        boolean appliquerClauseSauvegarde,
        Integer ancienneteAnneesPostStatutUnique,
        BigDecimal salaireHebdomadaireBrut,
        BigDecimal preavisClaeysMois,
        int preavisClaeysSemaines,
        int preavisStatutUniquesSemaines,
        int preavisTotalSemaines,
        BigDecimal indemniteClaeysBrute,
        BigDecimal indemniteTotaleBrute,
        String formuleClaeys,
        String baseJuridique,
        String avertissement
) {}
