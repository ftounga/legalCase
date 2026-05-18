package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-217-06 : calculateur de la contribution alimentaire des enfants belge
 * selon les principes de la <b>méthode Renard</b> (CC art. 203 / 203bis).
 *
 * <p>Principes appliqués :</p>
 * <ol>
 *   <li>le coût de l'enfant est réparti entre les parents <b>au prorata de
 *       leurs revenus</b> (CC art. 203 §1 — chacun contribue à proportion de
 *       ses facultés) ;</li>
 *   <li>la part hébergement assumée en nature par chaque parent (proportionnelle
 *       aux nuits) est déduite de sa part contributive ;</li>
 *   <li>les allocations familiales sont imputées sur le coût global de
 *       l'enfant ;</li>
 *   <li>les frais extraordinaires sont partagés au même prorata
 *       (CC art. 203bis §3).</li>
 * </ol>
 *
 * <p><b>Pays</b> : BELGIQUE uniquement. La méthode Renard est spécifique au
 * droit belge — aucune réutilisation du barème JAF français (F-FA-02).</p>
 *
 * <p><b>Validation juridique requise</b> : la méthode Renard est une méthode
 * jurisprudentielle de référence, non codifiée. Les forfaits de coût par tranche
 * d'âge sont des valeurs de calage à valider impérativement par un avocat belge
 * avant mise en production. CC art. 203 / 203bis sont à confirmer. Le contenu
 * juridique est centralisé ici (source unique de vérité).</p>
 */
public final class ContributionAlimentaireEnfantsBeCalculator {

    /** Verdict de l'estimation de contribution alimentaire. */
    public enum Verdict {
        CONTRIBUTION_DUE,
        CONTRIBUTION_EQUILIBREE,
        DONNEES_INSUFFISANTES
    }

    /** Tranche d'âge des enfants concernés. */
    public enum TrancheAge {
        ENFANT_0_5,
        ENFANT_6_11,
        ENFANT_12_17,
        ENFANT_18_PLUS
    }

    /** Parent identifié comme débiteur de la contribution nette. */
    public enum ParentDebiteur {
        PARENT_1,
        PARENT_2,
        AUCUN
    }

    /** Nombre maximal d'enfants accepté en saisie. */
    private static final int NB_ENFANTS_MAX = 12;
    /** Seuil en dessous duquel la contribution nette est considérée équilibrée. */
    private static final BigDecimal SEUIL_EQUILIBRE = new BigDecimal("5.00");
    private static final int JOURS_AN = 365;

    private ContributionAlimentaireEnfantsBeCalculator() {}

    /**
     * Estime la contribution alimentaire des enfants selon la méthode Renard.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace ("BELGIQUE" uniquement supporté)
     * @return résultat structuré
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static ContributionAlimentaireEnfantsBeResult compute(
            ContributionAlimentaireEnfantsBeInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"BELGIQUE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Contribution alimentaire des enfants — outil disponible uniquement en BELGIQUE");
        }
        validate(input);

        List<String> messages = new ArrayList<>();

        BigDecimal coutRetenu = coutMensuelRetenu(input);
        BigDecimal allocations = nvl(input.allocationsFamilialesMensuelles());
        BigDecimal coutNet = coutRetenu.subtract(allocations).max(BigDecimal.ZERO);

        BigDecimal revenu1 = nvl(input.revenuMensuelParent1());
        BigDecimal revenu2 = nvl(input.revenuMensuelParent2());
        BigDecimal revenuTotal = revenu1.add(revenu2);

        // Nuits — normalisation au prorata si la somme n'est pas 365.
        int nuits1 = input.nuitsHebergementParent1();
        int nuits2 = input.nuitsHebergementParent2();
        int sommeNuits = nuits1 + nuits2;
        BigDecimal fractionNuits1;
        BigDecimal fractionNuits2;
        if (sommeNuits == 0) {
            fractionNuits1 = new BigDecimal("0.5");
            fractionNuits2 = new BigDecimal("0.5");
            messages.add("Aucune nuit d'hébergement renseignée — hébergement supposé "
                    + "égalitaire (50/50) faute de donnée.");
        } else {
            fractionNuits1 = BigDecimal.valueOf(nuits1)
                    .divide(BigDecimal.valueOf(sommeNuits), 6, RoundingMode.HALF_UP);
            fractionNuits2 = BigDecimal.ONE.subtract(fractionNuits1);
            if (sommeNuits != JOURS_AN) {
                messages.add("La somme des nuits d'hébergement (" + sommeNuits + ") ne vaut "
                        + "pas 365 — les parts hébergement ont été normalisées au prorata.");
            }
        }

        if (revenuTotal.signum() == 0) {
            messages.add("Les revenus des deux parents sont nuls : le prorata Renard ne peut "
                    + "pas être établi — compléter les revenus pour fiabiliser l'analyse.");
            return new ContributionAlimentaireEnfantsBeResult(
                    Verdict.DONNEES_INSUFFISANTES,
                    money(coutRetenu), money(coutNet),
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, ParentDebiteur.AUCUN,
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    List.of("Coût mensuel retenu : " + money(coutRetenu) + " €"),
                    basesJuridiques(), messages, countryNormalized);
        }

        // Quote-part Renard (prorata des revenus) — CC art. 203 §1.
        BigDecimal quotePart1 = revenu1.divide(revenuTotal, 6, RoundingMode.HALF_UP);
        BigDecimal quotePart2 = BigDecimal.ONE.subtract(quotePart1);

        // Part contributive théorique de chaque parent.
        BigDecimal partContributive1 = coutNet.multiply(quotePart1);
        BigDecimal partContributive2 = coutNet.multiply(quotePart2);

        // Part hébergement assumée en nature (prorata des nuits).
        BigDecimal partHebergement1 = coutNet.multiply(fractionNuits1);
        BigDecimal partHebergement2 = coutNet.multiply(fractionNuits2);

        // Contribution résiduelle = part contributive - part hébergement.
        BigDecimal residuelle1 = partContributive1.subtract(partHebergement1);
        BigDecimal contributionNette = residuelle1.abs();

        ParentDebiteur debiteur;
        Verdict verdict;
        if (contributionNette.compareTo(SEUIL_EQUILIBRE) < 0) {
            verdict = Verdict.CONTRIBUTION_EQUILIBREE;
            debiteur = ParentDebiteur.AUCUN;
        } else {
            verdict = Verdict.CONTRIBUTION_DUE;
            // residuelle1 > 0 → le Parent 1 contribue davantage que sa part en nature.
            debiteur = residuelle1.signum() > 0 ? ParentDebiteur.PARENT_1 : ParentDebiteur.PARENT_2;
        }

        // Frais extraordinaires répartis au même prorata Renard (CC art. 203bis §3).
        BigDecimal frais = nvl(input.fraisExtraordinairesMensuels());
        BigDecimal fraisQuotePart1 = frais.multiply(quotePart1);
        BigDecimal fraisQuotePart2 = frais.multiply(quotePart2);

        List<String> detail = construireDetail(input, coutRetenu, allocations, coutNet,
                quotePart1, quotePart2, nuits1, nuits2, sommeNuits,
                money(contributionNette), debiteur);

        messages.add("Estimation indicative selon les principes de la méthode Renard — "
                + "à affiner avec les justificatifs de revenus et le détail du coût réel "
                + "de l'enfant.");

        return new ContributionAlimentaireEnfantsBeResult(
                verdict,
                money(coutRetenu), money(coutNet),
                percent(quotePart1), percent(quotePart2),
                money(partContributive1), money(partContributive2),
                money(partHebergement1), money(partHebergement2),
                money(contributionNette), debiteur,
                money(fraisQuotePart1), money(fraisQuotePart2),
                detail, basesJuridiques(), messages, countryNormalized);
    }

    private static void validate(ContributionAlimentaireEnfantsBeInput in) {
        if (in.nombreEnfants() < 1 || in.nombreEnfants() > NB_ENFANTS_MAX) {
            throw new IllegalArgumentException(
                    "Le nombre d'enfants doit être compris entre 1 et " + NB_ENFANTS_MAX);
        }
        if (in.trancheAgeEnfants() == null) {
            throw new IllegalArgumentException("trancheAgeEnfants est requis");
        }
        requirePositive(in.revenuMensuelParent1(), "revenuMensuelParent1");
        requirePositive(in.revenuMensuelParent2(), "revenuMensuelParent2");
        if (in.revenuMensuelParent1() == null || in.revenuMensuelParent2() == null) {
            throw new IllegalArgumentException("Les revenus des deux parents sont requis");
        }
        if (in.coutMensuelGlobalEnfants() != null
                && in.coutMensuelGlobalEnfants().signum() < 0) {
            throw new IllegalArgumentException("Le coût mensuel global ne peut être négatif");
        }
        requireNuits(in.nuitsHebergementParent1(), "nuitsHebergementParent1");
        requireNuits(in.nuitsHebergementParent2(), "nuitsHebergementParent2");
        if (in.allocationsFamilialesMensuelles() != null
                && in.allocationsFamilialesMensuelles().signum() < 0) {
            throw new IllegalArgumentException("Les allocations familiales ne peuvent être négatives");
        }
        if (in.fraisExtraordinairesMensuels() != null
                && in.fraisExtraordinairesMensuels().signum() < 0) {
            throw new IllegalArgumentException("Les frais extraordinaires ne peuvent être négatifs");
        }
        if (in.commentaire() != null && in.commentaire().length() > 1000) {
            throw new IllegalArgumentException("Le commentaire ne peut dépasser 1000 caractères");
        }
    }

    private static void requirePositive(BigDecimal v, String name) {
        if (v != null && v.signum() < 0) {
            throw new IllegalArgumentException("Le champ " + name + " ne peut être négatif");
        }
    }

    private static void requireNuits(int nuits, String name) {
        if (nuits < 0 || nuits > JOURS_AN) {
            throw new IllegalArgumentException(
                    "Le champ " + name + " doit être compris entre 0 et " + JOURS_AN);
        }
    }

    /** Coût mensuel retenu : valeur explicite si fournie, sinon forfait par tranche d'âge. */
    private static BigDecimal coutMensuelRetenu(ContributionAlimentaireEnfantsBeInput in) {
        if (in.coutMensuelGlobalEnfants() != null) {
            return in.coutMensuelGlobalEnfants();
        }
        return forfaitParEnfant(in.trancheAgeEnfants())
                .multiply(BigDecimal.valueOf(in.nombreEnfants()));
    }

    /**
     * Forfait mensuel par enfant selon la tranche d'âge.
     * Valeurs de calage — à valider par un avocat belge avant prod.
     */
    private static BigDecimal forfaitParEnfant(TrancheAge tranche) {
        return switch (tranche) {
            case ENFANT_0_5 -> new BigDecimal("280");
            case ENFANT_6_11 -> new BigDecimal("350");
            case ENFANT_12_17 -> new BigDecimal("420");
            case ENFANT_18_PLUS -> new BigDecimal("500");
        };
    }

    private static List<String> basesJuridiques() {
        return List.of(
                "CC art. 203 §1 (obligation d'entretien — proportionnelle aux facultés des parents)",
                "CC art. 203bis (part contributive et frais extraordinaires)",
                "Méthode Renard (méthode de référence — répartition du coût de l'enfant)");
    }

    private static List<String> construireDetail(ContributionAlimentaireEnfantsBeInput in,
                                                 BigDecimal coutRetenu, BigDecimal allocations,
                                                 BigDecimal coutNet, BigDecimal quotePart1,
                                                 BigDecimal quotePart2, int nuits1, int nuits2,
                                                 int sommeNuits, BigDecimal contributionNette,
                                                 ParentDebiteur debiteur) {
        List<String> d = new ArrayList<>();
        if (in.coutMensuelGlobalEnfants() != null) {
            d.add("Coût mensuel retenu (valeur saisie) : " + money(coutRetenu) + " €");
        } else {
            d.add("Coût mensuel retenu (forfait tranche d'âge × " + in.nombreEnfants()
                    + " enfant(s)) : " + money(coutRetenu) + " €");
        }
        d.add("Coût net après imputation des allocations familiales (" + money(allocations)
                + " €) : " + money(coutNet) + " €");
        d.add("Quote-part Renard : Parent 1 = " + percent(quotePart1) + " % / Parent 2 = "
                + percent(quotePart2) + " % (prorata des revenus — CC art. 203 §1)");
        int base = sommeNuits == 0 ? JOURS_AN : sommeNuits;
        d.add("Part hébergement déduite (prorata des nuits) : Parent 1 = " + nuits1 + "/" + base
                + ", Parent 2 = " + nuits2 + "/" + base);
        if (debiteur == ParentDebiteur.PARENT_1) {
            d.add("Contribution nette due par le Parent 1 au Parent 2 : "
                    + contributionNette + " € / mois");
        } else if (debiteur == ParentDebiteur.PARENT_2) {
            d.add("Contribution nette due par le Parent 2 au Parent 1 : "
                    + contributionNette + " € / mois");
        } else {
            d.add("Les parts contributives résiduelles s'équilibrent — aucune contribution "
                    + "nette significative à verser.");
        }
        return d;
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** Arrondi monétaire à 2 décimales (HALF_UP). */
    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    /** Conversion d'une fraction [0,1] en pourcentage arrondi à 1 décimale. */
    private static BigDecimal percent(BigDecimal fraction) {
        return fraction.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
    }
}
