package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-FA-24-07 : calculateur de la <b>réserve héréditaire</b> (FR — art. 913
 * + 914-1 Cciv) et de la recevabilité de l'<b>action en réduction</b> des
 * libéralités excédant la quotité disponible (art. 920-928 Cciv).
 *
 * <h2>Barème quotité disponible (art. 913)</h2>
 * <ul>
 *   <li>0 enfant + conjoint : QD = 75 % — réserve conjoint = 25 % (art. 914-1).</li>
 *   <li>0 enfant sans conjoint : QD = 100 % — pas de réserve.</li>
 *   <li>1 enfant : QD = 50 % — réserve = 50 %.</li>
 *   <li>2 enfants : QD = 33,33 % — réserve = 66,67 % (1/3 chacun).</li>
 *   <li>3+ enfants : QD = 25 % — réserve = 75 % (75/N chacun).</li>
 * </ul>
 *
 * <h2>Action en réduction (art. 920-928)</h2>
 * <ul>
 *   <li>Recevable si {@code excedent > 0} ET prescription non écoulée.</li>
 *   <li>Délai prescription = 5 ans à compter de l'ouverture (art. 921).</li>
 *   <li>Ordre de réduction (art. 923-924) : legs proportionnellement, puis
 *       donations dans l'ordre inverse de leur date.</li>
 * </ul>
 *
 * <p>Outil <b>single-country FRANCE</b> — équivalent BE (CC BE art. 913+,
 * barème différent) → feature jumelle dédiée backlog (F-FA-24-BE).</p>
 */
public final class ReserveHereditaireCalculator {

    /** Qualité du demandeur de l'action en réduction. */
    public enum QualiteDemandeur {
        /** Héritier réservataire descendant (art. 913). */
        HERITIER_RESERVATAIRE_DESCENDANT,
        /** Conjoint survivant (réservataire art. 914-1, uniquement à défaut de descendants). */
        CONJOINT_SURVIVANT
    }

    /** Verdict de recevabilité de l'action en réduction. */
    public enum VerdictRecevabilite {
        RECEVABLE,
        NON_RECEVABLE_PAS_EXCEDENT,
        NON_RECEVABLE_PRESCRIPTION,
        NON_RECEVABLE_QUALITE,
        NON_RECEVABLE
    }

    /** Délai de prescription en années (art. 921 al. 1). */
    public static final int PRESCRIPTION_ANS = 5;

    /** Base juridique consolidée. */
    public static final String BASE_JURIDIQUE = "Art. 913 + 914-1 + 920-928 Cciv";

    private ReserveHereditaireCalculator() {}

    /**
     * Calcule la réserve, la quotité disponible, l'excédent réductible et la
     * recevabilité de l'action en réduction.
     *
     * @throws IllegalArgumentException si paramètre invalide.
     */
    public static ReserveHereditaireResult compute(Integer nombreEnfants,
                                                   Boolean conjointSurvivant,
                                                   BigDecimal montantSuccession,
                                                   BigDecimal montantLibsTotal,
                                                   LocalDate dateOuvertureSuccession,
                                                   QualiteDemandeur qualiteDuDemandeur,
                                                   String country) {
        // ---- Validations strictes
        if (nombreEnfants == null || nombreEnfants < 0) {
            throw new IllegalArgumentException("nombreEnfants doit être ≥ 0");
        }
        if (conjointSurvivant == null) {
            throw new IllegalArgumentException("conjointSurvivant (oui/non) requis");
        }
        if (montantSuccession == null || montantSuccession.signum() <= 0) {
            throw new IllegalArgumentException("montantSuccession doit être > 0");
        }
        if (montantLibsTotal == null || montantLibsTotal.signum() < 0) {
            throw new IllegalArgumentException("montantLibsTotal doit être ≥ 0");
        }
        if (dateOuvertureSuccession == null) {
            throw new IllegalArgumentException("dateOuvertureSuccession requise");
        }
        LocalDate today = LocalDate.now();
        if (dateOuvertureSuccession.isAfter(today)) {
            throw new IllegalArgumentException("dateOuvertureSuccession ne peut être future");
        }
        if (qualiteDuDemandeur == null) {
            throw new IllegalArgumentException("qualiteDuDemandeur requise");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil non disponible pour le pays " + country
                            + " — l'équivalent BE (CC BE art. 913+ avec barème différent)"
                            + " sera traité dans une feature jumelle dédiée (F-FA-24-BE).");
        }

        List<String> messages = new ArrayList<>();

        // ============================================================
        // 1. Quotité disponible et réserve (art. 913 + 914-1)
        // ============================================================
        double quotiteDisponiblePct;
        double reservePct;
        if (nombreEnfants == 0) {
            if (conjointSurvivant) {
                quotiteDisponiblePct = 75.0;
                reservePct = 25.0;
                messages.add("Pas de descendant — conjoint survivant réservataire (art. 914-1) "
                        + ": réserve 25 %, quotité disponible 75 %.");
            } else {
                quotiteDisponiblePct = 100.0;
                reservePct = 0.0;
                messages.add("Pas de descendant ni conjoint réservataire : pas de réserve, "
                        + "quotité disponible totale (100 %). Le défunt pouvait disposer "
                        + "librement de l'intégralité du patrimoine.");
            }
        } else if (nombreEnfants == 1) {
            quotiteDisponiblePct = 50.0;
            reservePct = 50.0;
            messages.add("1 enfant (art. 913) : réserve 50 %, quotité disponible 50 %.");
        } else if (nombreEnfants == 2) {
            quotiteDisponiblePct = 100.0 / 3.0;          // 33,333…
            reservePct = 200.0 / 3.0;                    // 66,666…
            messages.add("2 enfants (art. 913) : réserve 2/3, quotité disponible 1/3 — "
                    + "chaque enfant a une réserve individuelle de 1/3.");
        } else {
            quotiteDisponiblePct = 25.0;
            reservePct = 75.0;
            messages.add(nombreEnfants + " enfants (art. 913) : réserve 3/4, quotité "
                    + "disponible 1/4 — chaque enfant a une réserve individuelle de "
                    + String.format(Locale.ROOT, "%.2f", 75.0 / nombreEnfants) + " %.");
        }

        // ============================================================
        // 2. Montants en euros (BigDecimal, scale 2)
        // ============================================================
        BigDecimal montantReserveEur = montantSuccession
                .multiply(BigDecimal.valueOf(reservePct))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal montantQuotiteDispoEur = montantSuccession
                .multiply(BigDecimal.valueOf(quotiteDisponiblePct))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal excedentReductibleEur = montantLibsTotal.subtract(montantQuotiteDispoEur);
        if (excedentReductibleEur.signum() < 0) {
            excedentReductibleEur = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            excedentReductibleEur = excedentReductibleEur.setScale(2, RoundingMode.HALF_UP);
        }

        // ============================================================
        // 3. Prescription (art. 921) — délai 5 ans depuis ouverture
        // ============================================================
        Period elapsed = Period.between(dateOuvertureSuccession, today);
        long elapsedTotalMonths = (long) elapsed.getYears() * 12L + elapsed.getMonths();
        long prescriptionTotalMonths = (long) PRESCRIPTION_ANS * 12L;
        long delaiPrescriptionRestantMois = Math.max(0L,
                prescriptionTotalMonths - elapsedTotalMonths);
        boolean prescritDelai5Ans = delaiPrescriptionRestantMois <= 0L;

        // ============================================================
        // 4. Recevabilité de l'action en réduction (art. 920-928)
        // ============================================================
        VerdictRecevabilite verdictRecevabilite;
        boolean actionReductionRecevable = false;

        // Qualité du demandeur : conjoint évincé si descendants présents
        boolean qualiteValide;
        if (qualiteDuDemandeur == QualiteDemandeur.HERITIER_RESERVATAIRE_DESCENDANT) {
            qualiteValide = nombreEnfants > 0;
            if (!qualiteValide) {
                messages.add("Demandeur invoque la qualité d'héritier réservataire descendant "
                        + "mais aucun enfant n'est déclaré — qualité non valide.");
            }
        } else { // CONJOINT_SURVIVANT
            qualiteValide = conjointSurvivant && nombreEnfants == 0;
            if (!qualiteValide && conjointSurvivant && nombreEnfants > 0) {
                messages.add("Le conjoint survivant n'est PAS réservataire en présence de "
                        + "descendants (art. 914-1 a contrario) — il ne peut pas exercer "
                        + "l'action en réduction sur la base de la réserve héréditaire.");
            }
            if (!qualiteValide && !conjointSurvivant) {
                messages.add("Le demandeur indiqué comme conjoint survivant alors que la "
                        + "case 'conjointSurvivant' est false : qualité non valide.");
            }
        }

        if (!qualiteValide) {
            verdictRecevabilite = VerdictRecevabilite.NON_RECEVABLE_QUALITE;
        } else if (excedentReductibleEur.signum() <= 0) {
            verdictRecevabilite = VerdictRecevabilite.NON_RECEVABLE_PAS_EXCEDENT;
            messages.add("Les libéralités totales (" + montantLibsTotal.toPlainString()
                    + " €) ne dépassent pas la quotité disponible ("
                    + montantQuotiteDispoEur.toPlainString()
                    + " €) : pas d'atteinte à la réserve, action en réduction sans objet.");
        } else if (prescritDelai5Ans) {
            verdictRecevabilite = VerdictRecevabilite.NON_RECEVABLE_PRESCRIPTION;
            messages.add("Délai de prescription de 5 ans (art. 921) écoulé depuis l'ouverture "
                    + "de la succession le " + dateOuvertureSuccession + " — action prescrite. "
                    + "Le délai second de 2 ans à compter de la connaissance de l'atteinte "
                    + "(art. 921 al. 2) doit être vérifié manuellement.");
        } else {
            verdictRecevabilite = VerdictRecevabilite.RECEVABLE;
            actionReductionRecevable = true;
            messages.add("Action en réduction RECEVABLE (art. 920-928 Cciv) : excédent de "
                    + excedentReductibleEur.toPlainString() + " € à réduire sur les libéralités. "
                    + "Ordre de réduction (art. 923-924) : legs réduits d'abord proportionnellement, "
                    + "puis donations dans l'ordre INVERSE de leur date (les plus récentes d'abord). "
                    + "Délai de prescription restant : " + delaiPrescriptionRestantMois + " mois.");
        }

        // ============================================================
        // 5. Score d'éligibilité
        // ============================================================
        int score = computeScore(verdictRecevabilite, qualiteValide,
                excedentReductibleEur.signum() > 0, !prescritDelai5Ans);

        String formule = String.format(Locale.ROOT,
                "QD = %.2f%% × %s = %s € | réserve = %.2f%% × %s = %s € | "
                        + "libs = %s € | excédent = max(0, libs − QD) = %s € | "
                        + "ouverture %s, écoulé %d mois, prescription restante %d mois | "
                        + "qualité=%s validée=%s → verdict %s → score %d",
                quotiteDisponiblePct,
                montantSuccession.toPlainString(), montantQuotiteDispoEur.toPlainString(),
                reservePct,
                montantSuccession.toPlainString(), montantReserveEur.toPlainString(),
                montantLibsTotal.toPlainString(), excedentReductibleEur.toPlainString(),
                dateOuvertureSuccession, elapsedTotalMonths, delaiPrescriptionRestantMois,
                qualiteDuDemandeur.name(), qualiteValide,
                verdictRecevabilite.name(), score);

        messages.add("Base juridique : " + BASE_JURIDIQUE + ".");

        return new ReserveHereditaireResult(
                nombreEnfants,
                conjointSurvivant,
                montantSuccession.setScale(2, RoundingMode.HALF_UP),
                montantLibsTotal.setScale(2, RoundingMode.HALF_UP),
                dateOuvertureSuccession,
                qualiteDuDemandeur,
                countryNormalized,
                round(quotiteDisponiblePct),
                round(reservePct),
                montantReserveEur,
                montantQuotiteDispoEur,
                excedentReductibleEur,
                actionReductionRecevable,
                delaiPrescriptionRestantMois,
                verdictRecevabilite,
                score,
                BASE_JURIDIQUE,
                formule,
                messages
        );
    }

    private static double round(double pct) {
        return BigDecimal.valueOf(pct).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static int computeScore(VerdictRecevabilite verdict, boolean qualiteValide,
                                    boolean excedent, boolean nonPrescrit) {
        int score = 0;
        if (qualiteValide) score += 30;
        if (excedent) score += 30;
        if (nonPrescrit) score += 30;
        if (verdict == VerdictRecevabilite.RECEVABLE) score += 10;
        return Math.min(score, 100);
    }
}
