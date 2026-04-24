package fr.ailegalcase.casefile;

import fr.ailegalcase.shared.BelgianBusinessDaysCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-DT-27-01 : calculateur de validité procédurale du licenciement pour motif grave en Belgique.
 *
 * <p>Vérifie le respect des deux délais de 3 jours ouvrables de l'article 35 de la Loi du 03/07/1978
 * sur les contrats de travail :
 * <ol>
 *   <li>Notification de la rupture dans les 3 jours ouvrables suivant la connaissance du fait.</li>
 *   <li>Notification des motifs par recommandé dans les 3 jours ouvrables suivant la rupture.</li>
 * </ol>
 *
 * <p>Si l'un des délais n'est pas respecté : le motif grave est procéduralement invalide,
 * l'indemnité compensatoire de préavis est due (loi 26/12/2013, approximation : 3 semaines par année
 * d'ancienneté, plafond 62 semaines) et l'indemnité pour licenciement manifestement déraisonnable
 * CCT 109 est potentiellement due (fourchette 3 à 17 semaines).
 *
 * <p>Cet outil est <b>single-country BE</b> : l'équivalent français (faute grave disciplinaire)
 * est couvert par F-DT-08 (validité disciplinaire) et F-DT-01 (licenciement simple).
 */
public final class MotifGraveBeCalculator {

    /** Délai maximum en jours ouvrables pour chaque notification (art. 35 Loi 03/07/1978). */
    public static final int DELAI_MAX_JOURS_OUVRABLES = 3;

    /** Plafond pratique d'approximation de la table de préavis belge (loi 26/12/2013). */
    public static final int PLAFOND_PREAVIS_SEMAINES = 62;

    /** Facteur semaines par année d'ancienneté (approximation simplifiée). */
    public static final int SEMAINES_PAR_ANNEE = 3;

    /** Diviseur mois → semaines (moyenne annuelle 52 sem / 12 mois ≈ 4.33). */
    public static final BigDecimal SEMAINES_PAR_MOIS = new BigDecimal("4.33");

    /** Bornes CCT 109 (indemnité licenciement manifestement déraisonnable) en semaines. */
    public static final int CCT109_MIN_SEMAINES = 3;
    public static final int CCT109_MAX_SEMAINES = 17;

    private static final String BASE_JURIDIQUE =
            "Art. 35 Loi 03/07/1978 sur les contrats de travail (double délai 3 jours ouvrables) "
                    + "+ Loi 26/12/2013 sur le statut unique (préavis) + CCT 109 (licenciement "
                    + "manifestement déraisonnable, si invalide)";

    private MotifGraveBeCalculator() {
    }

    public static MotifGraveBeResult compute(LocalDate dateConnaissanceFait,
                                             LocalDate dateNotificationRupture,
                                             LocalDate dateNotificationMotifs,
                                             int anciennetteAnnees,
                                             BigDecimal salaireMensuelReference) {
        validateInputs(dateConnaissanceFait, dateNotificationRupture, dateNotificationMotifs,
                anciennetteAnnees, salaireMensuelReference);

        int delaiRupture = BelgianBusinessDaysCalculator.countBusinessDays(
                dateConnaissanceFait, dateNotificationRupture);
        int delaiMotifs = BelgianBusinessDaysCalculator.countBusinessDays(
                dateNotificationRupture, dateNotificationMotifs);

        boolean ruptureOk = delaiRupture <= DELAI_MAX_JOURS_OUVRABLES;
        boolean motifsOk = delaiMotifs <= DELAI_MAX_JOURS_OUVRABLES;
        boolean valide = ruptureOk && motifsOk;

        BigDecimal salaireRef = salaireMensuelReference.setScale(2, RoundingMode.HALF_UP);
        BigDecimal salaireHebdo = salaireRef.divide(SEMAINES_PAR_MOIS, 4, RoundingMode.HALF_UP);

        BigDecimal indemnitePreavis;
        BigDecimal fourchetteMin;
        BigDecimal fourchetteMax;
        String formule;
        List<String> messages = new ArrayList<>();

        if (valide) {
            indemnitePreavis = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            fourchetteMin = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            fourchetteMax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            formule = String.format(
                    "Délais respectés (%d j ouvrables ≤ 3 + %d j ouvrables ≤ 3) — motif grave "
                            + "procéduralement valable → zéro préavis, zéro indemnité de rupture.",
                    delaiRupture, delaiMotifs);
            messages.add("Le motif grave est procéduralement valable. Sous réserve du caractère "
                    + "grave substantiel du fait (question de fond à démontrer devant le tribunal "
                    + "du travail en cas de contestation).");
            messages.add("Charge de la preuve (art. 35 al. 3) : l'employeur doit prouver la date "
                    + "de connaissance du fait et la date de notification du recommandé motivé.");
        } else {
            int preavisSemaines = Math.min(anciennetteAnnees * SEMAINES_PAR_ANNEE, PLAFOND_PREAVIS_SEMAINES);
            indemnitePreavis = salaireHebdo
                    .multiply(BigDecimal.valueOf(preavisSemaines))
                    .setScale(2, RoundingMode.HALF_UP);
            fourchetteMin = salaireHebdo
                    .multiply(BigDecimal.valueOf(CCT109_MIN_SEMAINES))
                    .setScale(2, RoundingMode.HALF_UP);
            fourchetteMax = salaireHebdo
                    .multiply(BigDecimal.valueOf(CCT109_MAX_SEMAINES))
                    .setScale(2, RoundingMode.HALF_UP);
            formule = String.format(
                    "Délais non respectés (rupture %d j%s ; motifs %d j%s) — motif grave "
                            + "procéduralement invalide. Préavis = min(%d × 3 ; 62) = %d sem × "
                            + "(salaire mensuel %s € / 4.33) = %s €. CCT 109 fourchette 3→17 sem "
                            + "= [%s € ; %s €].",
                    delaiRupture, ruptureOk ? " ✓" : " ✗ > 3",
                    delaiMotifs, motifsOk ? " ✓" : " ✗ > 3",
                    anciennetteAnnees, preavisSemaines,
                    formatMontant(salaireRef), formatMontant(indemnitePreavis),
                    formatMontant(fourchetteMin), formatMontant(fourchetteMax));
            if (!ruptureOk) {
                messages.add("Délai de notification de la rupture dépassé : " + delaiRupture
                        + " jours ouvrables > 3 (art. 35 al. 2 Loi 03/07/1978).");
            }
            if (!motifsOk) {
                messages.add("Délai de notification des motifs par recommandé dépassé : "
                        + delaiMotifs + " jours ouvrables > 3 (art. 35 al. 3 Loi 03/07/1978).");
            }
            messages.add("Conséquence : le motif grave est procéduralement invalide. "
                    + "L'employeur doit l'indemnité compensatoire de préavis (loi 26/12/2013).");
            messages.add("Indemnité complémentaire potentielle pour licenciement manifestement "
                    + "déraisonnable (CCT 109) : de 3 à 17 semaines de rémunération selon "
                    + "appréciation du tribunal du travail.");
            messages.add("Approximation : le préavis est calculé ici à 3 semaines par année "
                    + "d'ancienneté (plafonné à 62 semaines). La table exacte de la loi 26/12/2013 "
                    + "peut varier légèrement pour les anciennetés > 20 ans ou les cadres.");
        }

        return new MotifGraveBeResult(
                dateConnaissanceFait,
                dateNotificationRupture,
                dateNotificationMotifs,
                anciennetteAnnees,
                salaireRef,
                delaiRupture,
                delaiMotifs,
                valide,
                indemnitePreavis,
                fourchetteMin,
                fourchetteMax,
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    private static void validateInputs(LocalDate dateConnaissanceFait,
                                       LocalDate dateNotificationRupture,
                                       LocalDate dateNotificationMotifs,
                                       int anciennetteAnnees,
                                       BigDecimal salaireMensuelReference) {
        if (dateConnaissanceFait == null) {
            throw new IllegalArgumentException("dateConnaissanceFait est requise");
        }
        if (dateNotificationRupture == null) {
            throw new IllegalArgumentException("dateNotificationRupture est requise");
        }
        if (dateNotificationMotifs == null) {
            throw new IllegalArgumentException("dateNotificationMotifs est requise");
        }
        if (dateNotificationRupture.isBefore(dateConnaissanceFait)) {
            throw new IllegalArgumentException(
                    "dateNotificationRupture ne peut pas être antérieure à dateConnaissanceFait");
        }
        if (dateNotificationMotifs.isBefore(dateNotificationRupture)) {
            throw new IllegalArgumentException(
                    "dateNotificationMotifs ne peut pas être antérieure à dateNotificationRupture");
        }
        if (anciennetteAnnees < 0) {
            throw new IllegalArgumentException("anciennetteAnnees doit être ≥ 0");
        }
        if (salaireMensuelReference == null || salaireMensuelReference.signum() <= 0) {
            throw new IllegalArgumentException(
                    "salaireMensuelReference requis et strictement positif");
        }
    }

    private static String formatMontant(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
