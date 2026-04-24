package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SF-DT-11-01 : calculateur de l'indemnité minimum due au salarié en cas de licenciement nul
 * pour harcèlement, discrimination ou violence au travail.
 *
 * <p>Formule commune FR + BE : {@code indemniteMinimumNullite = 6 × salaireMensuelReference}.</p>
 *
 * <ul>
 *   <li>FR — Art. L.1235-3-1 al. 2 Code du travail : plancher 6 mois, le juge peut allouer davantage.</li>
 *   <li>BE — Loi 4 août 1996 art. 32tredecies (harcèlement / violence) + Loi 10 mai 2007
 *       anti-discrimination : plancher 6 mois de rémunération brute.</li>
 * </ul>
 *
 * <p>Les motifs FR et BE sont distinctement préfixés : un motif FR sur un workspace BE (ou
 * l'inverse) est rejeté avec une {@link IllegalArgumentException}.</p>
 */
public final class HarcelementNulliteCalculator {

    /**
     * Motifs de nullité d'un licenciement — 8 FR + 4 BE.
     */
    public enum MotifNullite {
        // --- France (art. L.1235-3-1 Code du travail) ---
        HARCELEMENT_MORAL,
        HARCELEMENT_SEXUEL,
        DISCRIMINATION,
        GROSSESSE,
        SALARIE_PROTEGE,
        LIBERTE_FONDAMENTALE,
        ACTION_JUSTICE,
        ALERTE_ETHIQUE,
        // --- Belgique (Loi 4 août 1996 + Loi 10 mai 2007) ---
        HARCELEMENT_MORAL_BE,
        HARCELEMENT_SEXUEL_BE,
        VIOLENCE_AU_TRAVAIL_BE,
        DISCRIMINATION_BE
    }

    private static final Set<MotifNullite> MOTIFS_FR = EnumSet.of(
            MotifNullite.HARCELEMENT_MORAL,
            MotifNullite.HARCELEMENT_SEXUEL,
            MotifNullite.DISCRIMINATION,
            MotifNullite.GROSSESSE,
            MotifNullite.SALARIE_PROTEGE,
            MotifNullite.LIBERTE_FONDAMENTALE,
            MotifNullite.ACTION_JUSTICE,
            MotifNullite.ALERTE_ETHIQUE
    );

    private static final Set<MotifNullite> MOTIFS_BE = EnumSet.of(
            MotifNullite.HARCELEMENT_MORAL_BE,
            MotifNullite.HARCELEMENT_SEXUEL_BE,
            MotifNullite.VIOLENCE_AU_TRAVAIL_BE,
            MotifNullite.DISCRIMINATION_BE
    );

    private static final BigDecimal SIX_MONTHS = new BigDecimal("6");

    private HarcelementNulliteCalculator() {}

    /**
     * Calcule l'indemnité minimum due en cas de licenciement nul.
     *
     * @param salaireMensuelReference salaire mensuel de référence retenu (>0)
     * @param motif                   motif de nullité (obligatoire)
     * @param country                 pays du workspace ("FRANCE" ou "BELGIQUE")
     * @return résultat structuré avec indemnité, base juridique et messages
     * @throws IllegalArgumentException si salaire invalide, motif absent ou incompatible avec le pays
     */
    public static HarcelementNulliteResult compute(BigDecimal salaireMensuelReference,
                                                   MotifNullite motif,
                                                   String country) {
        if (salaireMensuelReference == null || salaireMensuelReference.signum() <= 0) {
            throw new IllegalArgumentException("Salaire mensuel de référence requis et strictement positif");
        }
        if (motif == null) {
            throw new IllegalArgumentException("Motif de nullité requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized) && !"BELGIQUE".equals(countryNormalized)) {
            throw new IllegalArgumentException("Pays non supporté : " + country);
        }
        if ("FRANCE".equals(countryNormalized) && !MOTIFS_FR.contains(motif)) {
            throw new IllegalArgumentException("Motif non applicable au pays FRANCE : " + motif);
        }
        if ("BELGIQUE".equals(countryNormalized) && !MOTIFS_BE.contains(motif)) {
            throw new IllegalArgumentException("Motif non applicable au pays BELGIQUE : " + motif);
        }

        BigDecimal salaireRounded = salaireMensuelReference.setScale(2, RoundingMode.HALF_UP);
        BigDecimal indemnite = salaireRounded.multiply(SIX_MONTHS).setScale(2, RoundingMode.HALF_UP);

        String formule = String.format("6 mois × %s € = %s €",
                formatMontant(salaireRounded),
                formatMontant(indemnite));

        String baseJuridique = baseJuridiqueFor(motif);
        List<String> messages = messagesFor(motif);

        return new HarcelementNulliteResult(
                salaireRounded,
                motif,
                countryNormalized,
                indemnite,
                formule,
                baseJuridique,
                messages
        );
    }

    private static String baseJuridiqueFor(MotifNullite motif) {
        return switch (motif) {
            case HARCELEMENT_MORAL ->
                    "Art. L.1235-3-1 al. 2 Code du travail (L.1152-3)";
            case HARCELEMENT_SEXUEL ->
                    "Art. L.1235-3-1 al. 2 Code du travail (L.1153-4)";
            case DISCRIMINATION ->
                    "Art. L.1235-3-1 al. 2 Code du travail (L.1134-4)";
            case GROSSESSE ->
                    "Art. L.1235-3-1 al. 2 Code du travail (L.1225-71)";
            case SALARIE_PROTEGE ->
                    "Art. L.1235-3-1 al. 2 Code du travail (L.2411-1 et s.)";
            case LIBERTE_FONDAMENTALE ->
                    "Art. L.1235-3-1 al. 2 Code du travail — violation d'une liberté fondamentale";
            case ACTION_JUSTICE ->
                    "Art. L.1235-3-1 al. 2 Code du travail (L.1132-3-3)";
            case ALERTE_ETHIQUE ->
                    "Art. L.1235-3-1 al. 2 Code du travail (L.1132-3-3) — alerte éthique";
            case HARCELEMENT_MORAL_BE ->
                    "Loi 4 août 1996 art. 32tredecies — harcèlement moral au travail";
            case HARCELEMENT_SEXUEL_BE ->
                    "Loi 4 août 1996 art. 32tredecies — harcèlement sexuel au travail";
            case VIOLENCE_AU_TRAVAIL_BE ->
                    "Loi 4 août 1996 art. 32tredecies — violence au travail";
            case DISCRIMINATION_BE ->
                    "Loi 10 mai 2007 anti-discrimination — 6 mois de rémunération brute";
        };
    }

    private static List<String> messagesFor(MotifNullite motif) {
        String plancher = "Plancher légal 6 mois — le juge peut allouer davantage selon le préjudice effectif.";
        if (motif == MotifNullite.SALARIE_PROTEGE) {
            return List.of(
                    plancher,
                    "Cumul avec l'indemnité compensatrice de préavis et l'indemnité de congés payés. "
                            + "Attention : l'indemnité forfaitaire de 6 mois n'est pas cumulable avec "
                            + "l'indemnité de licenciement pour les salariés protégés (jurisprudence constante).",
                    "Le salarié protégé peut également demander sa réintégration."
            );
        }
        if (MOTIFS_BE.contains(motif)) {
            return List.of(
                    plancher,
                    "Cumul avec les indemnités de rupture (préavis, compensatoire). "
                            + "Le travailleur peut également demander la réintégration dans l'emploi.",
                    "L'indemnité s'entend en rémunération brute (Loi 4 août 1996 / Loi 10 mai 2007)."
            );
        }
        return List.of(
                plancher,
                "Cumul avec les indemnités de rupture (licenciement, préavis, congés payés) selon la jurisprudence constante.",
                "Le salarié peut également demander sa réintégration dans l'entreprise."
        );
    }

    private static String formatMontant(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
