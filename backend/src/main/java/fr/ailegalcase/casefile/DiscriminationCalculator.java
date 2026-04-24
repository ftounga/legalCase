package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SF-DT-12-01 : calculateur de la fourchette indicative de dommages-intérêts
 * pour acte discriminatoire — distinct de F-DT-11 (indemnité nullité 6 mois).
 *
 * Formule : DI = salaire × facteur, facteur ∈ [min, max] selon motif et contexte.
 *
 * Les fourchettes sont basées sur la jurisprudence constante de la chambre sociale
 * de la Cour de cassation (FR) et du Conseil du contentieux (BE). Elles ne constituent
 * pas un plafond légal — le juge peut allouer davantage selon le préjudice effectif.
 */
public final class DiscriminationCalculator {

    // Fourchettes de base par motif (FR)
    private static final Map<String, int[]> FR_MOTIF_RANGES = Map.ofEntries(
            Map.entry("ORIGINE_ETHNIQUE",      new int[]{6, 12}),
            Map.entry("SEXE_GROSSESSE",        new int[]{6, 12}),
            Map.entry("ORIENTATION_SEXUELLE",  new int[]{6, 10}),
            Map.entry("AGE",                    new int[]{3, 8}),
            Map.entry("ETAT_SANTE_HANDICAP",   new int[]{6, 12}),
            Map.entry("OPINIONS_POLITIQUES",   new int[]{6, 12}),
            Map.entry("ORIGINE_SOCIALE",       new int[]{3, 8}),
            Map.entry("SITUATION_FAMILIALE",   new int[]{3, 8})
    );

    // Fourchettes de base par motif (BE)
    private static final Map<String, int[]> BE_MOTIF_RANGES = Map.ofEntries(
            Map.entry("DISCRIMINATION_RACIALE_BE",     new int[]{6, 12}),
            Map.entry("DISCRIMINATION_GENRE_BE",       new int[]{6, 12}),
            Map.entry("DISCRIMINATION_HANDICAP_BE",    new int[]{6, 12}),
            Map.entry("DISCRIMINATION_AGE_BE",         new int[]{3, 8}),
            Map.entry("DISCRIMINATION_ORIENTATION_BE", new int[]{6, 10})
    );

    // Modulation par contexte : [multMin, multMax] appliqués sur la fourchette de base
    private static final Map<String, double[]> CONTEXTE_MODULATION = Map.of(
            "LICENCIEMENT",                    new double[]{1.0, 1.0},
            "HARCELEMENT_LIE_DISCRIMINATION", new double[]{0.8, 1.0},
            "PROMOTION_REFUSEE",              new double[]{0.7, 0.9},
            "SANCTION",                        new double[]{0.6, 0.85},
            "EMBAUCHE_REFUSEE",               new double[]{0.6, 0.85},
            "DIFFERENCE_SALARIALE",           new double[]{0.4, 0.7}
    );

    // Bases légales (FR)
    private static final Map<String, String> FR_BASES = Map.ofEntries(
            Map.entry("ORIGINE_ETHNIQUE",      "Art. L.1132-1 + L.1134-5 Code du travail — origine, nation, ethnie"),
            Map.entry("SEXE_GROSSESSE",        "Art. L.1132-1 + L.1134-5 + L.1225 Code du travail — sexe, grossesse, maternité"),
            Map.entry("ORIENTATION_SEXUELLE",  "Art. L.1132-1 + L.1134-5 Code du travail — orientation sexuelle, identité de genre"),
            Map.entry("AGE",                    "Art. L.1132-1 + L.1134-5 Code du travail — âge"),
            Map.entry("ETAT_SANTE_HANDICAP",   "Art. L.1132-1 + L.1134-5 + L.5213 Code du travail — état de santé, handicap"),
            Map.entry("OPINIONS_POLITIQUES",   "Art. L.1132-1 + L.1134-5 Code du travail — opinions, convictions, activités syndicales"),
            Map.entry("ORIGINE_SOCIALE",       "Art. L.1132-1 + L.1134-5 Code du travail — origine sociale, patronyme, résidence"),
            Map.entry("SITUATION_FAMILIALE",   "Art. L.1132-1 + L.1134-5 Code du travail — situation familiale, charges de famille")
    );

    // Bases légales (BE)
    private static final Map<String, String> BE_BASES = Map.ofEntries(
            Map.entry("DISCRIMINATION_RACIALE_BE",     "Loi 30/7/1981 tendant à réprimer le racisme + Loi 10/5/2007"),
            Map.entry("DISCRIMINATION_GENRE_BE",       "Loi 10/5/2007 tendant à lutter contre les discriminations entre hommes et femmes"),
            Map.entry("DISCRIMINATION_HANDICAP_BE",    "Loi 10/5/2007 + AR 13/6/2014 handicap"),
            Map.entry("DISCRIMINATION_AGE_BE",         "Loi 10/5/2007 — critère âge"),
            Map.entry("DISCRIMINATION_ORIENTATION_BE", "Loi 10/5/2007 — orientation sexuelle")
    );

    private static final List<String> COMMON_MESSAGES_FR = List.of(
            "Régime probatoire (art. L.1134-1) : le salarié présente des éléments de fait laissant supposer l'existence d'une discrimination. À l'employeur de prouver que sa décision est justifiée par des éléments objectifs étrangers à toute discrimination.",
            "Cumul : les dommages-intérêts réparent le préjudice spécifique de la discrimination (préjudice moral + perte de chance de carrière). Ils se cumulent avec l'indemnité de nullité du licenciement (6 mois, voir outil F-DT-11) si le licenciement est l'acte discriminatoire.",
            "Prescription : 5 ans à compter de la révélation de la discrimination (art. L.1134-5 al. 1er).",
            "Fourchette indicative basée sur la jurisprudence de la chambre sociale de la Cour de cassation — le juge peut allouer davantage selon le préjudice effectif démontré."
    );

    private static final List<String> COMMON_MESSAGES_BE = List.of(
            "Régime probatoire (loi 10/5/2007 art. 28) : la victime présente des faits laissant présumer une discrimination. Charge à la partie adverse de prouver l'absence de discrimination.",
            "Cumul : les dommages-intérêts réparent le préjudice spécifique. Ils se cumulent avec l'indemnité forfaitaire de protection (6 mois) si le licenciement a été motivé par discrimination.",
            "Prescription : 5 ans à compter de la révélation (art. 2262bis Code civil BE).",
            "Fourchette indicative — le juge du fond peut allouer davantage selon preuve du préjudice."
    );

    private DiscriminationCalculator() {}

    public static DiscriminationResult compute(BigDecimal salaireMensuelReference,
                                               String motifDiscrimination,
                                               String contexteActe,
                                               String country) {
        if (salaireMensuelReference == null || salaireMensuelReference.signum() <= 0) {
            throw new IllegalArgumentException("Salaire mensuel de référence requis et strictement positif");
        }
        if (motifDiscrimination == null || motifDiscrimination.isBlank()) {
            throw new IllegalArgumentException("Motif de discrimination requis");
        }
        if (contexteActe == null || !CONTEXTE_MODULATION.containsKey(contexteActe)) {
            throw new IllegalArgumentException("Contexte d'acte inconnu : " + contexteActe
                    + ". Valeurs : " + CONTEXTE_MODULATION.keySet());
        }
        if (!"FRANCE".equals(country) && !"BELGIQUE".equals(country)) {
            throw new IllegalArgumentException("Country requis : FRANCE ou BELGIQUE (actuel : " + country + ")");
        }

        Set<String> frMotifs = FR_MOTIF_RANGES.keySet();
        Set<String> beMotifs = BE_MOTIF_RANGES.keySet();
        boolean isFr = frMotifs.contains(motifDiscrimination);
        boolean isBe = beMotifs.contains(motifDiscrimination);

        if (!isFr && !isBe) {
            throw new IllegalArgumentException("Motif inconnu : " + motifDiscrimination
                    + ". Valeurs FR : " + frMotifs + " — Valeurs BE : " + beMotifs);
        }
        if (isFr && !"FRANCE".equals(country)) {
            throw new IllegalArgumentException("Motif FR '" + motifDiscrimination
                    + "' incompatible avec workspace " + country);
        }
        if (isBe && !"BELGIQUE".equals(country)) {
            throw new IllegalArgumentException("Motif BE '" + motifDiscrimination
                    + "' incompatible avec workspace " + country);
        }

        int[] motifRange = isFr ? FR_MOTIF_RANGES.get(motifDiscrimination)
                                : BE_MOTIF_RANGES.get(motifDiscrimination);
        double[] modulation = CONTEXTE_MODULATION.get(contexteActe);

        double facteurMin = motifRange[0] * modulation[0];
        double facteurMax = motifRange[1] * modulation[1];
        double facteurMediane = (facteurMin + facteurMax) / 2.0;

        BigDecimal salaireRounded = salaireMensuelReference.setScale(2, RoundingMode.HALF_UP);
        BigDecimal min = salaireRounded.multiply(BigDecimal.valueOf(facteurMin)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal med = salaireRounded.multiply(BigDecimal.valueOf(facteurMediane)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal max = salaireRounded.multiply(BigDecimal.valueOf(facteurMax)).setScale(2, RoundingMode.HALF_UP);

        String formule = String.format(
                "Salaire %s € × [%.2f ; %.2f] mois (motif %s, contexte %s) = [%s € ; %s €], médiane %s €",
                formatMontant(salaireRounded), facteurMin, facteurMax,
                motifDiscrimination, contexteActe,
                formatMontant(min), formatMontant(max), formatMontant(med));

        String baseJuridique = isFr ? FR_BASES.get(motifDiscrimination)
                                    : BE_BASES.get(motifDiscrimination);

        return new DiscriminationResult(
                salaireRounded, motifDiscrimination, contexteActe,
                country, min, med, max, formule, baseJuridique,
                isFr ? COMMON_MESSAGES_FR : COMMON_MESSAGES_BE);
    }

    public static Set<String> getFrMotifs() { return FR_MOTIF_RANGES.keySet(); }
    public static Set<String> getBeMotifs() { return BE_MOTIF_RANGES.keySet(); }
    public static Set<String> getContextes() { return CONTEXTE_MODULATION.keySet(); }

    private static String formatMontant(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
