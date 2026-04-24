package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * SF-DT-17-01 : calculateur de l'indemnité de fin de contrat à durée déterminée
 * (« prime de précarité ») — art. L.1243-8 Code du travail.
 *
 * Formule : indemnité = total des rémunérations brutes perçues × taux applicable (10 % ou 6 %).
 *
 * Cas d'exclusion (art. L.1243-10) : CDD étudiant pour vacances, saisonnier, d'usage,
 * refus d'un CDI proposé par l'employeur, rupture anticipée à l'initiative du salarié,
 * rupture anticipée pour faute grave ou force majeure.
 *
 * Taux réduit 6 % : applicable uniquement si la convention collective étendue prévoit
 * formellement des contreparties (typiquement une formation qualifiante) — art. L.1243-9.
 */
public final class IndemnitePrecariteCddCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final String ARTICLE = "Art. L.1243-8 Code du travail";

    private static final Map<String, String> EXCLUSIONS = Map.ofEntries(
            Map.entry("CDD_ETUDIANT_VACANCES",
                    "Indemnité non due : CDD conclu avec un jeune pour la période des vacances scolaires ou universitaires (art. L.1243-10 1°)."),
            Map.entry("CDD_SAISONNIER",
                    "Indemnité non due : CDD saisonnier (art. L.1243-10 2° + L.1242-2 3°)."),
            Map.entry("CDD_USAGE",
                    "Indemnité non due : CDD d'usage pour emploi par nature temporaire (art. L.1243-10 2° + L.1242-2 3°)."),
            Map.entry("CDI_REFUSE_PAR_SALARIE",
                    "Indemnité non due : CDI pour le même emploi ou un emploi similaire refusé par le salarié, avec une rémunération au moins équivalente (art. L.1243-10 3°)."),
            Map.entry("RUPTURE_ANTICIPEE_SALARIE",
                    "Indemnité non due : rupture anticipée du CDD à l'initiative du salarié (art. L.1243-10 4°)."),
            Map.entry("RUPTURE_ANTICIPEE_FAUTE_GRAVE",
                    "Indemnité non due : rupture anticipée pour faute grave du salarié ou force majeure (art. L.1243-10 4°).")
    );

    private IndemnitePrecariteCddCalculator() {}

    public static IndemnitePrecariteCddResult compute(BigDecimal totalSalairesBruts,
                                                      int tauxPrecarite,
                                                      String casExclusion) {
        if (totalSalairesBruts == null || totalSalairesBruts.signum() <= 0) {
            throw new IllegalArgumentException("Total des salaires bruts requis et strictement positif");
        }
        if (tauxPrecarite != 10 && tauxPrecarite != 6) {
            throw new IllegalArgumentException("Taux applicable : 10 % (standard) ou 6 % (convention collective avec contrepartie)");
        }
        if (casExclusion != null && !EXCLUSIONS.containsKey(casExclusion)) {
            throw new IllegalArgumentException("Cas d'exclusion inconnu : " + casExclusion
                    + ". Valeurs acceptées : " + EXCLUSIONS.keySet());
        }

        BigDecimal totalRounded = totalSalairesBruts.setScale(2, RoundingMode.HALF_UP);

        if (casExclusion != null) {
            return new IndemnitePrecariteCddResult(
                    totalRounded, tauxPrecarite, casExclusion,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    "0,00 €",
                    ARTICLE + " (exclusion art. L.1243-10)",
                    List.of(EXCLUSIONS.get(casExclusion))
            );
        }

        BigDecimal indemnite = totalRounded
                .multiply(new BigDecimal(tauxPrecarite))
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        String formule = String.format("%d %% × %s € = %s €",
                tauxPrecarite,
                formatMontant(totalRounded),
                formatMontant(indemnite));

        List<String> messages = tauxPrecarite == 6
                ? List.of(
                        "Taux réduit 6 % applicable uniquement si la convention collective étendue prévoit des contreparties formelles (formation qualifiante notamment) — art. L.1243-9.",
                        "Vérifier que la CCN applicable est bien étendue et que la contrepartie est réelle.")
                : List.of(
                        "Indemnité de fin de contrat due à l'issue du CDD, sauf cas d'exclusion de l'art. L.1243-10.",
                        "Versée avec le dernier bulletin de paie et soumise à cotisations sociales + impôt sur le revenu.");

        return new IndemnitePrecariteCddResult(
                totalRounded, tauxPrecarite, null, indemnite, formule, ARTICLE, messages);
    }

    private static String formatMontant(BigDecimal montant) {
        // 18500.00 -> "18 500,00"
        return montant.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
