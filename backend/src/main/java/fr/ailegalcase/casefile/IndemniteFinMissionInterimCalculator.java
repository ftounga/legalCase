package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * SF-DT-18-01 : calculateur de l'indemnité de fin de mission intérim
 * (« indemnité de fin de mission » / IFM) — art. L.1251-32 Code du travail.
 *
 * Formule : indemnité = total des rémunérations brutes perçues pendant la mission × 10 %.
 *
 * Cas d'exclusion (L.1251-32 al. 2 et L.1251-33) :
 * - CDI proposé pour le même emploi à l'issue de la mission ;
 * - rupture anticipée à l'initiative du salarié ;
 * - rupture anticipée pour faute grave ou force majeure ;
 * - mission "pépinière" qualifiante ;
 * - refus du salarié d'une proposition de CDI à conditions équivalentes.
 *
 * Pattern miroir strict de IndemnitePrecariteCddCalculator (F-DT-17), situation
 * métier distincte (intérim ≠ CDD), invariant "un outil = une situation".
 *
 * Différence clé : pas de taux 6 % — le taux 10 % est figé par la loi pour le
 * travail temporaire, sans alternative CCN avec contrepartie.
 */
public final class IndemniteFinMissionInterimCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TAUX_LEGAL_DECIMAL = new BigDecimal("0.10");
    private static final int TAUX_LEGAL_PCT = 10;
    private static final String ARTICLE = "Art. L.1251-32 Code du travail";
    private static final String COUNTRY = "FRANCE";

    private static final Map<String, String> EXCLUSIONS = Map.ofEntries(
            Map.entry("CONTRAT_INDETERMINEE_PROPOSE",
                    "Indemnité non due : un CDI a été proposé au salarié à l'issue de la mission, pour le même emploi ou un emploi similaire (art. L.1251-32 al. 2)."),
            Map.entry("RUPTURE_ANTICIPEE_SALARIE",
                    "Indemnité non due : rupture anticipée du contrat de mission à l'initiative du salarié intérimaire (art. L.1251-32 al. 2)."),
            Map.entry("FAUTE_GRAVE",
                    "Indemnité non due : rupture anticipée pour faute grave du salarié intérimaire (art. L.1251-32 al. 2)."),
            Map.entry("FORCE_MAJEURE",
                    "Indemnité non due : rupture anticipée du contrat de mission pour cas de force majeure (art. L.1251-32 al. 2)."),
            Map.entry("MISSION_PEPINIERE_QUALIFIANTE",
                    "Indemnité non due : contrat de mission conclu dans le cadre d'une formation qualifiante / mission pépinière (art. L.1251-33)."),
            Map.entry("INTERIMAIRE_REFUS_PROPOSITION_CDI",
                    "Indemnité non due : le salarié intérimaire a refusé une proposition de CDI pour le même emploi à conditions équivalentes (art. L.1251-32 al. 2).")
    );

    private IndemniteFinMissionInterimCalculator() {}

    public static IndemniteFinMissionInterimResult compute(BigDecimal totalRemunerationsBrutesEur,
                                                           int dureeMissionJours,
                                                           String motifExclusion,
                                                           LocalDate dateFinMission) {
        if (totalRemunerationsBrutesEur == null || totalRemunerationsBrutesEur.signum() <= 0) {
            throw new IllegalArgumentException("Total des rémunérations brutes requis et strictement positif");
        }
        if (dureeMissionJours <= 0) {
            throw new IllegalArgumentException("Durée de la mission requise et strictement positive (en jours)");
        }
        if (motifExclusion != null && !EXCLUSIONS.containsKey(motifExclusion)) {
            throw new IllegalArgumentException("Motif d'exclusion inconnu : " + motifExclusion
                    + ". Valeurs acceptées : " + EXCLUSIONS.keySet());
        }

        BigDecimal totalRounded = totalRemunerationsBrutesEur.setScale(2, RoundingMode.HALF_UP);

        if (motifExclusion != null) {
            return new IndemniteFinMissionInterimResult(
                    totalRounded,
                    dureeMissionJours,
                    motifExclusion,
                    dateFinMission,
                    TAUX_LEGAL_DECIMAL,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    true,
                    ARTICLE + " (exclusion L.1251-32 al. 2 / L.1251-33)",
                    "0,00 €",
                    List.of(EXCLUSIONS.get(motifExclusion)),
                    COUNTRY
            );
        }

        BigDecimal indemnite = totalRounded
                .multiply(new BigDecimal(TAUX_LEGAL_PCT))
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        String formule = String.format("%s € × %d %% = %s €",
                formatMontant(totalRounded),
                TAUX_LEGAL_PCT,
                formatMontant(indemnite));

        List<String> messages = List.of(
                "Indemnité de fin de mission due à l'issue de la mission d'intérim, sauf cas d'exclusion (art. L.1251-32 al. 2 et L.1251-33).",
                "Versée avec le dernier bulletin de paie de la mission, soumise à cotisations sociales et à l'impôt sur le revenu.",
                "Le taux 10 % est figé par la loi — il n'existe pas d'équivalent CCN avec contrepartie permettant un taux réduit (différence avec le CDD, L.1243-9).");

        return new IndemniteFinMissionInterimResult(
                totalRounded,
                dureeMissionJours,
                null,
                dateFinMission,
                TAUX_LEGAL_DECIMAL,
                indemnite,
                false,
                ARTICLE,
                formule,
                messages,
                COUNTRY);
    }

    private static String formatMontant(BigDecimal montant) {
        // 12000.00 -> "12 000,00" — séparateur milliers + virgule décimale fr-FR
        String plain = montant.setScale(2, RoundingMode.HALF_UP).toPlainString();
        int dotIndex = plain.indexOf('.');
        String integerPart = plain.substring(0, dotIndex);
        String decimalPart = plain.substring(dotIndex + 1);
        // Insère les séparateurs de milliers
        StringBuilder sb = new StringBuilder();
        boolean negative = integerPart.startsWith("-");
        String digits = negative ? integerPart.substring(1) : integerPart;
        int len = digits.length();
        for (int i = 0; i < len; i++) {
            if (i > 0 && (len - i) % 3 == 0) {
                sb.append(' ');
            }
            sb.append(digits.charAt(i));
        }
        return (negative ? "-" : "") + sb + "," + decimalPart;
    }
}
