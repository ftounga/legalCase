package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-DT-28-01 : calculateur des avantages conventionnels belges (cumul des 4 postes
 * obligatoires en droit du travail belge).
 *
 * <ul>
 *   <li><b>Pécule de vacances simple</b> = 7,67 % de la rémunération annuelle brute
 *       (Loi du 28 juin 1971 sur les vacances annuelles + AR du 30 mars 1967).</li>
 *   <li><b>Double pécule de vacances</b> ≈ 0,50 × salaire mensuel × 12 × 0,92
 *       (approximation après cotisation spéciale de 0,925 % et fiscalité sociale).</li>
 *   <li><b>Prime de fin d'année</b> = 1 mois de salaire si la CCT sectorielle la prévoit
 *       (ex. CP 200 / CP 124 / CP 209).</li>
 *   <li><b>Éco-chèques</b> = 250 €/an (plafond ONSS exonéré, CCT 98 du CNT).</li>
 *   <li><b>Chèques-repas</b> = jours prestés × 8 €/jour (CCT 19octies, valeur médiane).</li>
 * </ul>
 *
 * <p>Outil <b>single-country BE</b>. L'équivalent FR (13e mois CCN) n'a pas de plafond
 * légal national équivalent et est traité par convention individuelle (cf. F-DT-04).
 */
public final class AvantagesConventionnelsBeCalculator {

    /** Taux légal du pécule de vacances simple (employés CCT). */
    public static final BigDecimal TAUX_PECULE_SIMPLE = new BigDecimal("0.0767");

    /** Coefficient de simplification du double pécule (50 % × 92 %). */
    public static final BigDecimal COEFF_DOUBLE_PECULE = new BigDecimal("0.50");
    public static final BigDecimal COEFF_NET_DOUBLE_PECULE = new BigDecimal("0.92");

    /** Plafond légal annuel exonéré ONSS pour les éco-chèques (CCT 98). */
    public static final BigDecimal ECO_CHEQUES_PLAFOND_ANNUEL = new BigDecimal("250.00");

    /** Valeur faciale médiane d'un chèque-repas (CCT 19octies). */
    public static final BigDecimal CHEQUES_REPAS_VALEUR_FACIALE = new BigDecimal("8.00");

    /** Année minimale acceptée (entrée en vigueur de la Loi 28/06/1971). */
    public static final int ANNEE_MIN = 1971;

    /** Commissions paritaires acceptées. */
    public static final Set<String> CP_VALIDES = Set.of("CP_200", "CP_124", "CP_209", "AUTRE");

    private static final String BASE_JURIDIQUE =
            "Loi 28/06/1971 (pécule) + CCT 98 (éco-chèques) + CCT 19octies (chèques-repas)";

    private AvantagesConventionnelsBeCalculator() {
    }

    public static AvantagesConventionnelsBeResult compute(BigDecimal salaireMensuelBrutEur,
                                                          int joursTravaillesAnneePrecedente,
                                                          int anciennetteMois,
                                                          String commissionParitaire,
                                                          int annee,
                                                          boolean doublePeculeVacancesPercu,
                                                          boolean primeFinAnneePrevueCcCp,
                                                          boolean ecoChequesPrevuCcCp,
                                                          boolean ecoChequesUtilisationDansAn,
                                                          boolean chequesRepasPrevu,
                                                          int joursPrestesEffectifs) {
        validateInputs(salaireMensuelBrutEur, joursTravaillesAnneePrecedente, anciennetteMois,
                commissionParitaire, annee, joursPrestesEffectifs);

        BigDecimal salaireRef = salaireMensuelBrutEur.setScale(2, RoundingMode.HALF_UP);
        BigDecimal salaireAnnuel = salaireRef.multiply(BigDecimal.valueOf(12))
                .setScale(2, RoundingMode.HALF_UP);

        // 1. Pécule de vacances simple = 7,67 % × salaire annuel brut
        BigDecimal peculeSimple = salaireAnnuel
                .multiply(TAUX_PECULE_SIMPLE)
                .setScale(2, RoundingMode.HALF_UP);

        // 2. Double pécule = 0,50 × salaire annuel × 0,92 (sauf si déjà perçu)
        BigDecimal doublePecule;
        if (doublePeculeVacancesPercu) {
            doublePecule = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            doublePecule = salaireAnnuel
                    .multiply(COEFF_DOUBLE_PECULE)
                    .multiply(COEFF_NET_DOUBLE_PECULE)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // 3. Prime de fin d'année = 1 mois si prévue par la CCT
        BigDecimal primeFinAnnee = primeFinAnneePrevueCcCp
                ? salaireRef
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // 4. Éco-chèques = 250 € si prévus
        BigDecimal ecoCheques = ecoChequesPrevuCcCp
                ? ECO_CHEQUES_PLAFOND_ANNUEL
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // 5. Chèques-repas = jours prestés × 8 € si prévus
        BigDecimal chequesRepas;
        if (chequesRepasPrevu) {
            chequesRepas = CHEQUES_REPAS_VALEUR_FACIALE
                    .multiply(BigDecimal.valueOf(joursPrestesEffectifs))
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            chequesRepas = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal total = peculeSimple
                .add(doublePecule)
                .add(primeFinAnnee)
                .add(ecoCheques)
                .add(chequesRepas)
                .setScale(2, RoundingMode.HALF_UP);

        String formule = String.format(
                "Pécule simple 7,67 %% × %s € = %s € | Double pécule %s | Prime fin d'année %s | "
                        + "Éco-chèques %s | Chèques-repas %s",
                formatMontant(salaireAnnuel), formatMontant(peculeSimple),
                doublePeculeVacancesPercu ? "déjà perçu (0)" : "0,50 × " + formatMontant(salaireAnnuel)
                        + " × 0,92 = " + formatMontant(doublePecule),
                primeFinAnneePrevueCcCp ? "1 mois = " + formatMontant(primeFinAnnee) : "non prévue (0)",
                ecoChequesPrevuCcCp ? formatMontant(ecoCheques) + " € (plafond CCT 98)" : "non prévus (0)",
                chequesRepasPrevu
                        ? joursPrestesEffectifs + " × 8 € = " + formatMontant(chequesRepas) + " €"
                        : "non prévus (0)");

        List<String> messages = buildMessages(commissionParitaire, doublePeculeVacancesPercu,
                primeFinAnneePrevueCcCp, ecoChequesPrevuCcCp, ecoChequesUtilisationDansAn,
                chequesRepasPrevu, joursPrestesEffectifs);

        return new AvantagesConventionnelsBeResult(
                salaireRef, joursTravaillesAnneePrecedente, anciennetteMois,
                commissionParitaire, annee,
                doublePeculeVacancesPercu, primeFinAnneePrevueCcCp,
                ecoChequesPrevuCcCp, ecoChequesUtilisationDansAn, chequesRepasPrevu,
                joursPrestesEffectifs,
                peculeSimple, doublePecule, primeFinAnnee,
                ecoCheques, chequesRepas, total,
                formule, BASE_JURIDIQUE, messages, "BELGIQUE");
    }

    private static List<String> buildMessages(String cp,
                                              boolean doublePecuPercu,
                                              boolean primePrevue,
                                              boolean ecoPrevu,
                                              boolean ecoValide,
                                              boolean chequesPrevus,
                                              int joursPrestes) {
        List<String> msgs = new ArrayList<>();
        msgs.add("Pécule simple = 7,67 % × rémunération annuelle (Loi 28/06/1971 sur les vacances annuelles).");
        if (doublePecuPercu) {
            msgs.add("Double pécule déjà perçu cette année — montant 0 affiché à titre informatif.");
        } else {
            msgs.add("Double pécule de vacances ≈ 50 % de la rémunération annuelle "
                    + "(retenue spéciale ~8 % appliquée, approximation 0,50 × annuel × 0,92).");
        }
        if (primePrevue) {
            msgs.add("Prime de fin d'année prévue par la CCT sectorielle (" + cp
                    + ") — montant standard : 1 mois de rémunération brute.");
        } else {
            msgs.add("Prime de fin d'année non prévue par la CCT applicable — vérifier la convention "
                    + "collective de la commission paritaire " + cp + ".");
        }
        if (ecoPrevu) {
            msgs.add("Éco-chèques : plafond annuel 250 € exonéré ONSS (CCT 98 du CNT).");
            if (!ecoValide) {
                msgs.add("Attention : les éco-chèques doivent être utilisés dans les 24 mois — "
                        + "vérifier la date de validité.");
            }
        }
        if (chequesPrevus) {
            msgs.add("Chèques-repas (CCT 19octies) : valeur faciale médiane 8 €/jour presté. "
                    + "Plafond exonération ONSS = 6,91 € employeur + 1,09 € employé.");
            if (joursPrestes == 0) {
                msgs.add("Attention : 0 jour presté → aucun chèque-repas dû même si prévu par la CCT.");
            }
        }
        msgs.add("Outil informatif — la fiscalité (taxation distincte du pécule) et les retenues "
                + "sociales exactes doivent être validées sur les fiches de paie ONSS.");
        return msgs;
    }

    private static void validateInputs(BigDecimal salaireMensuelBrutEur,
                                       int joursTravaillesAnneePrecedente,
                                       int anciennetteMois,
                                       String commissionParitaire,
                                       int annee,
                                       int joursPrestesEffectifs) {
        if (salaireMensuelBrutEur == null || salaireMensuelBrutEur.signum() <= 0) {
            throw new IllegalArgumentException("salaireMensuelBrutEur requis et strictement positif");
        }
        if (joursTravaillesAnneePrecedente < 0) {
            throw new IllegalArgumentException("joursTravaillesAnneePrecedente doit être ≥ 0");
        }
        if (anciennetteMois < 0) {
            throw new IllegalArgumentException("anciennetteMois doit être ≥ 0");
        }
        if (joursPrestesEffectifs < 0) {
            throw new IllegalArgumentException("joursPrestesEffectifs doit être ≥ 0");
        }
        if (annee < ANNEE_MIN) {
            throw new IllegalArgumentException("annee doit être ≥ " + ANNEE_MIN
                    + " (entrée en vigueur Loi 28/06/1971)");
        }
        if (commissionParitaire == null || commissionParitaire.isBlank()) {
            throw new IllegalArgumentException("commissionParitaire est requise");
        }
        if (!CP_VALIDES.contains(commissionParitaire)) {
            throw new IllegalArgumentException("commissionParitaire inconnue : " + commissionParitaire
                    + ". Valeurs acceptées : " + CP_VALIDES);
        }
    }

    private static String formatMontant(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
