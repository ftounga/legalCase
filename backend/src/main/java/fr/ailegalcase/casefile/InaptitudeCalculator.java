package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SF-DT-15-01 : calculateur de l'indemnité due au salarié licencié pour inaptitude
 * après avis du médecin du travail (FR + BE).
 *
 * <p>Distinction essentielle FR : <strong>origine professionnelle vs non-professionnelle</strong>.</p>
 * <ul>
 *   <li>FR pro (L.1226-10) : indemnité légale doublée (L.1226-14), préavis indemnisé,
 *       damages reclassement = 12 × salaire minimum (L.1226-15) si reclassement non respecté.</li>
 *   <li>FR non-pro (L.1226-2) : indemnité légale simple, pas de préavis, obligation de
 *       reclassement existe mais n'ouvre pas automatiquement droit aux dommages L.1226-15.</li>
 *   <li>BE pro / non-pro (art. 34 Loi 03/07/1978) : force majeure médicale, pas d'indemnité
 *       forfaitaire mais damages reclassement 3 × salaire possibles si manquement employeur
 *       (AR 28/05/2003, trajet de réintégration).</li>
 * </ul>
 *
 * <p>Les origines FR et BE sont distinctement préfixées : une origine FR sur un workspace BE
 * (ou l'inverse) est rejetée avec une {@link IllegalArgumentException}.</p>
 */
public final class InaptitudeCalculator {

    /**
     * Origines de l'inaptitude — 2 FR + 2 BE.
     */
    public enum OrigineInaptitude {
        // --- France ---
        PROFESSIONNELLE,          // L.1226-10 : accident du travail / maladie professionnelle
        NON_PROFESSIONNELLE,      // L.1226-2  : maladie / accident non professionnel
        // --- Belgique ---
        PROFESSIONNELLE_BE,       // art. 34 Loi 03/07/1978 + force majeure médicale AT/MP
        NON_PROFESSIONNELLE_BE    // art. 34 Loi 03/07/1978 + maladie ordinaire prolongée
    }

    private static final Set<OrigineInaptitude> ORIGINES_FR = EnumSet.of(
            OrigineInaptitude.PROFESSIONNELLE,
            OrigineInaptitude.NON_PROFESSIONNELLE
    );

    private static final Set<OrigineInaptitude> ORIGINES_BE = EnumSet.of(
            OrigineInaptitude.PROFESSIONNELLE_BE,
            OrigineInaptitude.NON_PROFESSIONNELLE_BE
    );

    private static final BigDecimal QUARTER = new BigDecimal(4);
    private static final BigDecimal THIRD = new BigDecimal(3);
    private static final BigDecimal TWO = new BigDecimal(2);
    private static final BigDecimal TWELVE = new BigDecimal(12);
    private static final BigDecimal THREE = new BigDecimal(3);

    /** Préavis FR par défaut (L.1234-1, ≥ 2 ans d'ancienneté). Approximation — la CCN peut prévoir mieux. */
    private static final int PREAVIS_MOIS_DEFAUT_FR = 2;

    private InaptitudeCalculator() {}

    /**
     * Calcule les indemnités dues au salarié licencié pour inaptitude.
     *
     * @param salaireMensuelReference salaire mensuel de référence (>0)
     * @param ancienneteAnnees        ancienneté du salarié en années (≥ 0)
     * @param origine                 origine de l'inaptitude (obligatoire)
     * @param reclassementRespecte    true si l'employeur a respecté l'obligation de reclassement
     * @param avisMedecinTravailDate  date de l'avis du médecin du travail (nullable)
     * @param country                 pays du workspace ("FRANCE" ou "BELGIQUE")
     * @return résultat structuré (indemnités + formule + base juridique + messages)
     * @throws IllegalArgumentException si paramètre invalide ou cross-country
     */
    public static InaptitudeResult compute(BigDecimal salaireMensuelReference,
                                           int ancienneteAnnees,
                                           OrigineInaptitude origine,
                                           boolean reclassementRespecte,
                                           LocalDate avisMedecinTravailDate,
                                           String country) {
        if (salaireMensuelReference == null || salaireMensuelReference.signum() <= 0) {
            throw new IllegalArgumentException("Salaire mensuel de référence requis et strictement positif");
        }
        if (ancienneteAnnees < 0) {
            throw new IllegalArgumentException("Ancienneté doit être ≥ 0");
        }
        if (origine == null) {
            throw new IllegalArgumentException("Origine de l'inaptitude requise");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized) && !"BELGIQUE".equals(countryNormalized)) {
            throw new IllegalArgumentException("Pays non supporté : " + country);
        }
        if ("FRANCE".equals(countryNormalized) && !ORIGINES_FR.contains(origine)) {
            throw new IllegalArgumentException("Origine non applicable au pays FRANCE : " + origine);
        }
        if ("BELGIQUE".equals(countryNormalized) && !ORIGINES_BE.contains(origine)) {
            throw new IllegalArgumentException("Origine non applicable au pays BELGIQUE : " + origine);
        }

        BigDecimal salaireRounded = salaireMensuelReference.setScale(2, RoundingMode.HALF_UP);

        if ("FRANCE".equals(countryNormalized)) {
            return computeFrance(salaireRounded, ancienneteAnnees, origine, reclassementRespecte,
                    avisMedecinTravailDate, countryNormalized);
        }
        return computeBelgium(salaireRounded, ancienneteAnnees, origine, reclassementRespecte,
                avisMedecinTravailDate, countryNormalized);
    }

    private static InaptitudeResult computeFrance(BigDecimal salaire,
                                                  int ancienneteAnnees,
                                                  OrigineInaptitude origine,
                                                  boolean reclassementRespecte,
                                                  LocalDate avisMedecinTravailDate,
                                                  String country) {
        boolean pro = origine == OrigineInaptitude.PROFESSIONNELLE;

        // 1) Indemnité légale R.1234-2 (¼ + ⅓ après 10 ans), seuil 1 an d'ancienneté.
        BigDecimal indemniteLegaleBase = computeIndemniteLegaleR1234_2(salaire, ancienneteAnnees);
        // Doublement L.1226-14 si origine professionnelle.
        BigDecimal indemniteLegale = pro
                ? indemniteLegaleBase.multiply(TWO).setScale(2, RoundingMode.HALF_UP)
                : indemniteLegaleBase;

        // 2) Préavis : indemnisé si pro, non dû si non-pro.
        BigDecimal preavis = pro
                ? salaire.multiply(new BigDecimal(PREAVIS_MOIS_DEFAUT_FR)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2);

        // 3) Damages reclassement : 12 × salaire min (L.1226-15) si pro + reclassement non respecté.
        BigDecimal damages = (pro && !reclassementRespecte)
                ? salaire.multiply(TWELVE).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2);

        BigDecimal total = indemniteLegale.add(preavis).add(damages).setScale(2, RoundingMode.HALF_UP);

        String formule = buildFormuleFR(salaire, ancienneteAnnees, pro, reclassementRespecte,
                indemniteLegaleBase, indemniteLegale, preavis, damages, total);

        String baseJuridique = pro
                ? "Art. L.1226-10 Code du travail (+ L.1226-14 doublement + L.1226-15 reclassement)"
                : "Art. L.1226-2 Code du travail (+ R.1234-2 indemnité légale)";

        List<String> messages = messagesFR(pro, reclassementRespecte, ancienneteAnnees,
                avisMedecinTravailDate);

        return new InaptitudeResult(
                salaire,
                ancienneteAnnees,
                origine,
                reclassementRespecte,
                avisMedecinTravailDate,
                country,
                indemniteLegale,
                preavis,
                damages,
                total,
                formule,
                baseJuridique,
                messages
        );
    }

    private static InaptitudeResult computeBelgium(BigDecimal salaire,
                                                   int ancienneteAnnees,
                                                   OrigineInaptitude origine,
                                                   boolean reclassementRespecte,
                                                   LocalDate avisMedecinTravailDate,
                                                   String country) {
        boolean pro = origine == OrigineInaptitude.PROFESSIONNELLE_BE;

        // Force majeure médicale : pas d'indemnité forfaitaire de rupture.
        BigDecimal indemniteLegale = BigDecimal.ZERO.setScale(2);
        BigDecimal preavis = BigDecimal.ZERO.setScale(2);

        // Damages reclassement BE : 3 × salaire indicatif si l'employeur n'a pas proposé
        // de reclassement raisonnable (AR 28/05/2003, trajet de réintégration).
        BigDecimal damages = !reclassementRespecte
                ? salaire.multiply(THREE).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2);

        BigDecimal total = indemniteLegale.add(preavis).add(damages).setScale(2, RoundingMode.HALF_UP);

        String formule = reclassementRespecte
                ? String.format("Pas d'indemnité forfaitaire (force majeure médicale art. 34)")
                : String.format("3 × %s € = %s € (damages reclassement indicatif — AR 28/05/2003)",
                        formatMontant(salaire), formatMontant(damages));

        String baseJuridique = pro
                ? "Art. 34 Loi 03/07/1978 (force majeure médicale AT/MP) + Loi bien-être 04/08/1996 art. 31§1"
                : "Art. 34 Loi 03/07/1978 (force majeure médicale — maladie prolongée) + AR 28/05/2003";

        List<String> messages = messagesBE(pro, reclassementRespecte, avisMedecinTravailDate);

        return new InaptitudeResult(
                salaire,
                ancienneteAnnees,
                origine,
                reclassementRespecte,
                avisMedecinTravailDate,
                country,
                indemniteLegale,
                preavis,
                damages,
                total,
                formule,
                baseJuridique,
                messages
        );
    }

    /**
     * Indemnité légale FR art. R.1234-2 : ¼ de mois × min(10, ancienneté) + ⅓ de mois × max(0, ancienneté-10).
     * Seuil d'ouverture retenu : 1 an (approximation ; strict L.1234-9 = 8 mois continus).
     */
    private static BigDecimal computeIndemniteLegaleR1234_2(BigDecimal salaire, int ancienneteAnnees) {
        if (ancienneteAnnees < 1) {
            return BigDecimal.ZERO.setScale(2);
        }
        int part1Years = Math.min(10, ancienneteAnnees);
        int part2Years = Math.max(0, ancienneteAnnees - 10);

        BigDecimal part1 = salaire.multiply(new BigDecimal(part1Years))
                .divide(QUARTER, 4, RoundingMode.HALF_UP);
        BigDecimal part2 = salaire.multiply(new BigDecimal(part2Years))
                .divide(THIRD, 4, RoundingMode.HALF_UP);
        return part1.add(part2).setScale(2, RoundingMode.HALF_UP);
    }

    private static String buildFormuleFR(BigDecimal salaire,
                                         int ancienneteAnnees,
                                         boolean pro,
                                         boolean reclassementRespecte,
                                         BigDecimal indemniteLegaleBase,
                                         BigDecimal indemniteLegale,
                                         BigDecimal preavis,
                                         BigDecimal damages,
                                         BigDecimal total) {
        StringBuilder sb = new StringBuilder();
        sb.append("Indemnité légale R.1234-2 = ").append(formatMontant(indemniteLegaleBase)).append(" €");
        if (pro) {
            sb.append(" × 2 (doublement L.1226-14) = ").append(formatMontant(indemniteLegale)).append(" €");
            sb.append(" + préavis ").append(PREAVIS_MOIS_DEFAUT_FR).append(" × ")
              .append(formatMontant(salaire)).append(" € = ").append(formatMontant(preavis)).append(" €");
            if (!reclassementRespecte) {
                sb.append(" + damages reclassement 12 × ").append(formatMontant(salaire))
                  .append(" € = ").append(formatMontant(damages)).append(" € (L.1226-15)");
            }
        }
        sb.append(" — Total = ").append(formatMontant(total)).append(" €");
        return sb.toString();
    }

    private static List<String> messagesFR(boolean pro,
                                           boolean reclassementRespecte,
                                           int ancienneteAnnees,
                                           LocalDate avisMedecinTravailDate) {
        List<String> msgs = new ArrayList<>();
        if (pro) {
            msgs.add("Origine professionnelle (L.1226-10) : indemnité légale doublée (L.1226-14) et "
                    + "indemnité compensatrice de préavis due même si préavis non exécuté.");
            msgs.add("Consultation obligatoire du CSE sur les propositions de reclassement (L.1226-10).");
            if (!reclassementRespecte) {
                msgs.add("Obligation de reclassement non respectée : dommages-intérêts minimum 12 mois "
                        + "de salaire (art. L.1226-15) — le juge peut allouer davantage selon le préjudice.");
            } else {
                msgs.add("Obligation de reclassement respectée : pas de dommages-intérêts L.1226-15 dus, "
                        + "mais cumul possible avec l'indemnité de congés payés et l'indemnité de préavis.");
            }
        } else {
            msgs.add("Origine non professionnelle (L.1226-2) : indemnité légale simple sans doublement, "
                    + "pas de préavis dû ni indemnisé.");
            msgs.add("Obligation de reclassement existe mais n'ouvre pas les dommages-intérêts "
                    + "plancher 12 mois de L.1226-15 (applicables uniquement en origine pro).");
        }
        msgs.add("Rappel : l'employeur ne peut licencier qu'à l'issue d'un délai d'1 mois suivant "
                + "l'avis d'inaptitude, sans reclassement possible (art. L.1226-11) — passé ce délai, "
                + "le salaire reste dû.");
        if (ancienneteAnnees < 1) {
            msgs.add("Ancienneté inférieure à 1 an : indemnité légale de licenciement non due (seuil R.1234-2).");
        }
        if (avisMedecinTravailDate != null) {
            msgs.add("Avis du médecin du travail du " + avisMedecinTravailDate
                    + " : délai de reclassement court jusqu'à " + avisMedecinTravailDate.plusMonths(1) + ".");
        }
        return msgs;
    }

    private static List<String> messagesBE(boolean pro,
                                           boolean reclassementRespecte,
                                           LocalDate avisMedecinTravailDate) {
        List<String> msgs = new ArrayList<>();
        msgs.add("Force majeure médicale (art. 34 Loi 03/07/1978) : la rupture ne donne pas droit à "
                + "une indemnité forfaitaire de rupture (ni préavis ni indemnité compensatoire).");
        if (pro) {
            msgs.add("Origine professionnelle (AT/MP) : indemnisation complémentaire via le Fonds des "
                    + "accidents du travail (Fedris) ou l'assurance-loi.");
        } else {
            msgs.add("Maladie ordinaire prolongée : indemnisation via la mutuelle (INAMI) et, le cas "
                    + "échéant, via l'assurance-groupe complémentaire.");
        }
        if (!reclassementRespecte) {
            msgs.add("Trajet de réintégration non respecté (AR 28/05/2003) : dommages-intérêts possibles, "
                    + "indicativement 3 mois de rémunération brute — à moduler selon le préjudice établi.");
        } else {
            msgs.add("Trajet de réintégration respecté : pas de dommages automatiques, mais le "
                    + "travailleur peut contester la rupture s'il démontre un manquement de l'employeur.");
        }
        if (avisMedecinTravailDate != null) {
            msgs.add("Avis du médecin du travail du " + avisMedecinTravailDate
                    + " : point de départ du calcul du trajet de réintégration.");
        }
        return msgs;
    }

    private static String formatMontant(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
