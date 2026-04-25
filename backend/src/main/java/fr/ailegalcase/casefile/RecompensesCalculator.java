package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * SF-FA-15-01 : calculateur des <b>récompenses</b> en régime communautaire
 * (art. 1437 et 1469 Cciv). Cœur technique de la liquidation matrimoniale FR.
 *
 * <h2>Règles (art. 1469 Cciv)</h2>
 * <ul>
 *   <li><b>Al. 1 (règle générale)</b> : récompense = dépense faite</li>
 *   <li><b>Al. 2 (dépenses usuelles / nécessaires)</b> : récompense = la <b>plus faible</b>
 *       des deux sommes (dépense faite ou profit subsistant)</li>
 *   <li><b>Al. 3 (acquisition / conservation / amélioration)</b> : récompense = la
 *       <b>plus forte</b> des deux sommes (dépense faite ou profit subsistant)</li>
 * </ul>
 *
 * <h2>Calcul du profit subsistant</h2>
 * <ul>
 *   <li><b>ACQUISITION_BIEN_PROPRE</b> : profit = valeurActuelle × dépense / valeurInitiale (prix d'achat total)</li>
 *   <li><b>CONSERVATION_BIEN_PROPRE</b> : profit = valeurActuelle × dépense / valeurInitiale (la valeur conservée, ramenée à la part de la dépense)</li>
 *   <li><b>AMELIORATION_BIEN_PROPRE</b> : profit = max(plus-value, valeurActuelle × dépense / valeurInitiale)
 *       — la plus-value attribuable aux travaux. Quand les inputs ne le permettent pas, fallback à la dépense.</li>
 *   <li><b>DEPENSES_USUELLES</b> : profit = dépense par défaut (al. 2 retient la plus faible)</li>
 *   <li><b>AUTRE</b> : pas de profit calculé — al. 1 → récompense = dépense</li>
 * </ul>
 *
 * <p>Outil <b>single-country FR</b>. Régime BE (indemnités art. 1432-1438 anciens C. civ. BE)
 * non couvert par cette SF.
 */
public final class RecompensesCalculator {

    public static final String BASE_JURIDIQUE_GENERALE = "Art. 1437 et 1469 Cciv";

    public static final String REGLE_DEPENSE_FAITE = "DEPENSE_FAITE";
    public static final String REGLE_PLUS_FAIBLE = "PLUS_FAIBLE_DEPENSE_OU_PROFIT";
    public static final String REGLE_PROFIT_SUBSISTANT_PLUS_FORTE = "PROFIT_SUBSISTANT_PLUS_FORTE";

    public static final String DIRECTION_COMMUNAUTE_DOIT_PROPRE = "COMMUNAUTE_DOIT_PROPRE";
    public static final String DIRECTION_PROPRE_DOIT_COMMUNAUTE = "PROPRE_DOIT_COMMUNAUTE";

    private static final Set<String> REGIMES_AVEC_RECOMPENSES = new LinkedHashSet<>(Arrays.asList(
            "COMMUNAUTE_LEGALE",
            "PARTICIPATION_AUX_ACQUETS",
            "COMMUNAUTE_UNIVERSELLE"
    ));

    private static final Set<String> TYPES_OPERATION = new LinkedHashSet<>(Arrays.asList(
            "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE",
            "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE"
    ));

    private static final Set<String> NATURES_BIEN = new LinkedHashSet<>(Arrays.asList(
            "ACQUISITION_BIEN_PROPRE",
            "CONSERVATION_BIEN_PROPRE",
            "AMELIORATION_BIEN_PROPRE",
            "DEPENSES_USUELLES",
            "AUTRE"
    ));

    private RecompensesCalculator() {
    }

    /**
     * Validation préalable : régime applicable.
     * @throws IllegalArgumentException si SEPARATION_BIENS, null, ou inconnu.
     */
    public static void validateRegime(String regime) {
        if (regime == null || regime.isBlank()) {
            throw new IllegalArgumentException("regimeMatrimonial est requis");
        }
        if ("SEPARATION_BIENS".equals(regime)) {
            throw new IllegalArgumentException("Régime de séparation de biens : pas de récompenses (art. 1536 Cciv) — créances entre patrimoines hors scope.");
        }
        if (!REGIMES_AVEC_RECOMPENSES.contains(regime)) {
            throw new IllegalArgumentException("regimeMatrimonial inconnu : " + regime
                    + ". Valeurs : " + REGIMES_AVEC_RECOMPENSES);
        }
    }

    public static RecompensesResult compute(String regimeMatrimonial,
                                            List<RecompensesRequest.OperationRecompense> operations) {
        validateRegime(regimeMatrimonial);

        List<RecompensesResult.RecompenseDetail> details = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        Set<String> idsVus = new LinkedHashSet<>();

        BigDecimal totalDuesParCommunaute = BigDecimal.ZERO;
        BigDecimal totalDuesParEpoux = BigDecimal.ZERO;

        if (operations == null || operations.isEmpty()) {
            messages.add("Aucune opération soumise — totaux à 0.");
            return new RecompensesResult(
                    regimeMatrimonial,
                    details,
                    money(BigDecimal.ZERO),
                    money(BigDecimal.ZERO),
                    money(BigDecimal.ZERO),
                    BASE_JURIDIQUE_GENERALE,
                    messages
            );
        }

        boolean profitSubsistantUtilise = false;

        for (RecompensesRequest.OperationRecompense op : operations) {
            validateOperation(op, idsVus);

            BigDecimal depense = op.montantDepenseEur();
            BigDecimal profit = computeProfitSubsistant(op);

            String regle;
            BigDecimal montantRecompense;
            String formule;

            switch (op.natureBien()) {
                case "DEPENSES_USUELLES" -> {
                    // Al. 2 : la plus faible des deux sommes
                    regle = REGLE_PLUS_FAIBLE;
                    montantRecompense = depense.min(profit);
                    formule = "min(" + format(depense) + " ; " + format(profit) + ") = "
                            + format(montantRecompense)
                            + " (al. 2 — dépense usuelle ; plus faible des 2 sommes)";
                }
                case "ACQUISITION_BIEN_PROPRE",
                     "CONSERVATION_BIEN_PROPRE",
                     "AMELIORATION_BIEN_PROPRE" -> {
                    // Al. 3 : la plus forte des deux sommes
                    regle = REGLE_PROFIT_SUBSISTANT_PLUS_FORTE;
                    montantRecompense = depense.max(profit);
                    formule = "max(" + format(depense) + " ; " + format(profit) + ") = "
                            + format(montantRecompense)
                            + " (al. 3 — " + libelleNature(op.natureBien())
                            + " ; plus forte des 2 sommes — profit subsistant retenu si > dépense)";
                    profitSubsistantUtilise = true;
                }
                default -> {
                    // AUTRE → al. 1 : récompense = dépense
                    regle = REGLE_DEPENSE_FAITE;
                    montantRecompense = depense;
                    formule = "Récompense = dépense faite = " + format(montantRecompense)
                            + " (al. 1 — règle générale)";
                }
            }

            String direction = "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE".equals(op.type())
                    ? DIRECTION_COMMUNAUTE_DOIT_PROPRE
                    : DIRECTION_PROPRE_DOIT_COMMUNAUTE;

            String baseAlinea = switch (regle) {
                case REGLE_DEPENSE_FAITE -> "Art. 1469 al. 1 Cciv (règle générale — dépense faite)";
                case REGLE_PLUS_FAIBLE -> "Art. 1469 al. 2 Cciv (dépense usuelle — plus faible des 2 sommes)";
                default -> "Art. 1469 al. 3 Cciv (acquisition/conservation/amélioration — plus forte des 2 sommes)";
            };

            BigDecimal montantArrondi = money(montantRecompense);
            BigDecimal depenseArrondie = money(depense);
            BigDecimal profitArrondi = profit == null ? null : money(profit);

            details.add(new RecompensesResult.RecompenseDetail(
                    op.id(),
                    op.type(),
                    op.natureBien(),
                    regle,
                    depenseArrondie,
                    profitArrondi,
                    montantArrondi,
                    direction,
                    baseAlinea,
                    formule,
                    op.description()
            ));

            if (DIRECTION_COMMUNAUTE_DOIT_PROPRE.equals(direction)) {
                totalDuesParCommunaute = totalDuesParCommunaute.add(montantArrondi);
            } else {
                totalDuesParEpoux = totalDuesParEpoux.add(montantArrondi);
            }
        }

        BigDecimal soldeNet = totalDuesParCommunaute.subtract(totalDuesParEpoux);

        // Messages génériques
        messages.add("Récompenses calculées sur la base des art. 1437 et 1469 Cciv. "
                + "Les valeurs retenues sont celles au jour de la liquidation (art. 1469 al. 1 in fine).");
        messages.add("La récompense est due par le patrimoine enrichi (communauté ou propre) au profit "
                + "du patrimoine appauvri.");
        if (profitSubsistantUtilise) {
            messages.add("Profit subsistant retenu sur au moins une opération (méthode 1469 al. 3) : "
                    + "la récompense est la plus forte des 2 sommes (dépense ou profit subsistant).");
        }
        if (operations.stream().anyMatch(o -> "DEPENSES_USUELLES".equals(o.natureBien()))) {
            messages.add("Dépense usuelle (al. 2) : la récompense est plafonnée à la plus faible des 2 sommes.");
        }
        messages.add("Outil indicatif : le notaire liquidateur ou le juge peuvent retenir une "
                + "valorisation différente selon les pièces produites (estimations, expertises).");

        return new RecompensesResult(
                regimeMatrimonial,
                details,
                money(totalDuesParCommunaute),
                money(totalDuesParEpoux),
                money(soldeNet),
                BASE_JURIDIQUE_GENERALE,
                messages
        );
    }

    // -----------------------------------------------------------------------
    // Profit subsistant
    // -----------------------------------------------------------------------

    /**
     * Profit subsistant selon la nature du bien.
     * Retourne {@code montantDepense} en cas de données insuffisantes (fallback prudent).
     */
    static BigDecimal computeProfitSubsistant(RecompensesRequest.OperationRecompense op) {
        BigDecimal depense = op.montantDepenseEur();
        BigDecimal vInit = op.valeurInitialeBienEur();
        BigDecimal vAct = op.valeurActuelleBienEur();
        String nature = op.natureBien();

        return switch (nature) {
            case "ACQUISITION_BIEN_PROPRE" -> {
                // profit = valeurActuelle × dépense / valeurInitiale (prix achat total)
                if (vAct == null || vInit == null || vInit.signum() <= 0) {
                    yield depense; // fallback
                }
                yield vAct.multiply(depense).divide(vInit, 8, RoundingMode.HALF_UP);
            }
            case "CONSERVATION_BIEN_PROPRE" -> {
                // profit = valeur conservée — ramenée à la part de la dépense
                // si la dépense égale ou dépasse la valeur initiale, on plafonne à valeurActuelle
                if (vAct == null || vInit == null || vInit.signum() <= 0) {
                    yield depense; // fallback
                }
                BigDecimal ratio = vAct.multiply(depense).divide(vInit, 8, RoundingMode.HALF_UP);
                yield ratio.min(vAct); // borné à valeurActuelle
            }
            case "AMELIORATION_BIEN_PROPRE" -> {
                // profit = plus-value attribuable aux travaux ; à défaut ratio
                if (vAct == null || vInit == null) {
                    yield depense; // fallback
                }
                BigDecimal plusValue = vAct.subtract(vInit);
                if (plusValue.signum() < 0) {
                    plusValue = BigDecimal.ZERO; // pas de profit si moins-value
                }
                if (vInit.signum() <= 0) {
                    yield plusValue.max(depense);
                }
                BigDecimal ratio = vAct.multiply(depense).divide(vInit, 8, RoundingMode.HALF_UP);
                // On retient la plus-value bornée par le ratio (ne pas dépasser proportionnellement)
                yield plusValue.min(ratio);
            }
            case "DEPENSES_USUELLES" -> {
                // par défaut, profit = dépense (consommée — al. 2 retiendra le min)
                // si valeurActuelle/valeurInitiale fournis, on calcule un ratio plus précis
                if (vAct != null && vInit != null && vInit.signum() > 0) {
                    yield vAct.multiply(depense).divide(vInit, 8, RoundingMode.HALF_UP);
                }
                yield depense;
            }
            default -> // AUTRE
                    depense;
        };
    }

    // -----------------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------------

    private static void validateOperation(RecompensesRequest.OperationRecompense op,
                                          Set<String> idsVus) {
        if (op == null) {
            throw new IllegalArgumentException("operation null");
        }
        if (op.id() == null || op.id().isBlank()) {
            throw new IllegalArgumentException("operations[].id requis");
        }
        if (!idsVus.add(op.id())) {
            throw new IllegalArgumentException("operations[].id dupliqué : " + op.id());
        }
        if (op.type() == null || !TYPES_OPERATION.contains(op.type())) {
            throw new IllegalArgumentException("operations[].type invalide : " + op.type()
                    + ". Valeurs : " + TYPES_OPERATION);
        }
        if (op.natureBien() == null || !NATURES_BIEN.contains(op.natureBien())) {
            throw new IllegalArgumentException("operations[].natureBien invalide : " + op.natureBien()
                    + ". Valeurs : " + NATURES_BIEN);
        }
        if (op.montantDepenseEur() == null) {
            throw new IllegalArgumentException("operations[].montantDepenseEur requis");
        }
        if (op.montantDepenseEur().signum() < 0) {
            throw new IllegalArgumentException("operations[].montantDepenseEur ne peut être négatif");
        }
        if (op.valeurInitialeBienEur() != null && op.valeurInitialeBienEur().signum() < 0) {
            throw new IllegalArgumentException("operations[].valeurInitialeBienEur ne peut être négative");
        }
        if (op.valeurActuelleBienEur() != null && op.valeurActuelleBienEur().signum() < 0) {
            throw new IllegalArgumentException("operations[].valeurActuelleBienEur ne peut être négative");
        }
    }

    // -----------------------------------------------------------------------
    // Utilitaires
    // -----------------------------------------------------------------------

    private static BigDecimal money(BigDecimal v) {
        if (v == null) {
            return null;
        }
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static String format(BigDecimal v) {
        if (v == null) {
            return "—";
        }
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String libelleNature(String nature) {
        return switch (nature) {
            case "ACQUISITION_BIEN_PROPRE" -> "acquisition";
            case "CONSERVATION_BIEN_PROPRE" -> "conservation";
            case "AMELIORATION_BIEN_PROPRE" -> "amélioration";
            default -> nature.toLowerCase();
        };
    }
}
