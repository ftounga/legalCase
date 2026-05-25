package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * SF-213-02 : calculateur de rappel de salaire BE — fonction pure.
 *
 * <p>Calcule pour une créance d'arriérés de salaire en droit belge du travail :
 * <ul>
 *   <li>les intérêts moratoires (<b>10 %</b> annuel — Loi du 12/04/1965 sur la
 *       protection de la rémunération des travailleurs, art. 10), <b>distincts
 *       du taux légal civil</b> et du taux légal français ;</li>
 *   <li>le délai de prescription applicable (Loi du 03/07/1978 art. 15) :
 *       <ul>
 *         <li><b>5 ans</b> pour les arriérés exigibles pendant le contrat ;</li>
 *         <li><b>1 an</b> à compter de la rupture pour les créances ex-contrat
 *             post-rupture ;</li>
 *         <li>pour {@code MIXTE} : V1 = prescription la plus stricte (1 an
 *             post-rupture) applicable à toute la créance — l'avocat est invité
 *             à splitter manuellement.</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h2>Calcul d'intérêts</h2>
 * <pre>
 *   dureeAnnees     = (dateActionEnvisagee - dateFinPeriode) en jours / 365
 *   interetsCourus  = montantBrut * 0.10 * dureeAnnees
 *   totalReclame    = montantBrut + interetsCourus
 * </pre>
 *
 * <p>Tous les montants en {@link BigDecimal} avec arrondi {@code HALF_UP} 2
 * décimales — garantit la stabilité du round-trip JSON.</p>
 *
 * <p>Single-country BELGIQUE — l'équivalent FR (rappel-salaire FR) suit un
 * régime distinct (intérêts légaux variables semestriels — art. 1153 C.civ.
 * + prescription 3 ans C. trav. art. L. 3245-1).</p>
 */
public final class RappelSalaireBeCalculator {

    /** Taux d'intérêts moratoires BE applicable aux créances salariales (10 %). */
    public static final BigDecimal TAUX_MORATOIRE = new BigDecimal("0.10");

    /** Délai de prescription pendant le contrat (5 ans — Loi 03/07/1978 art. 15). */
    public static final int DELAI_PENDANT_CONTRAT_ANNEES = 5;

    /** Délai de prescription post-rupture (1 an — Loi 03/07/1978 art. 15). */
    public static final int DELAI_POST_RUPTURE_ANNEES = 1;

    /** Seuil "imminent" (en jours). */
    public static final long IMMINENT_SEUIL_JOURS = 30L;

    private static final String TAUX_MORATOIRE_LABEL =
            "10% (Loi 12/04/1965 art. 10)";

    private static final String BASE_JURIDIQUE =
            "Loi 12/04/1965 art. 10 ; Loi 03/07/1978 art. 15";

    private static final String AVERTISSEMENT_DEFAUT =
            "Taux d'intérêt moratoire spécifique BE (10 %) applicable aux créances "
                    + "salariales — distinct du taux légal civil et du taux légal FR. "
                    + "Pour les créances mixtes (pendant + post-rupture), la "
                    + "prescription 1 an post-rupture s'applique : splitter la "
                    + "créance manuellement pour bénéficier de la prescription 5 ans "
                    + "sur la part exigible pendant le contrat.";

    private RappelSalaireBeCalculator() {
    }

    public static RappelSalaireBeResult calculate(RappelSalaireBeRequest request) {
        validateInputs(request);

        BigDecimal montantBrut = request.montantBrut().setScale(2, RoundingMode.HALF_UP);
        LocalDate dateDebut = request.dateDebutPeriode();
        LocalDate dateFin = request.dateFinPeriode();
        LocalDate dateRupture = request.dateRupture();
        LocalDate dateAction = request.dateActionEnvisagee() != null
                ? request.dateActionEnvisagee()
                : LocalDate.now();
        RappelSalaireBeTypeArriereEnum typeArriere = request.typeArriere();

        // --- Intérêts moratoires (10 %) ---
        // dureeAnnees = (dateAction - dateFin) en jours / 365 (prorata décimal)
        BigDecimal dureeAnnees = computeDureeAnnees(dateFin, dateAction);
        BigDecimal interetsCourus = montantBrut
                .multiply(TAUX_MORATOIRE)
                .multiply(dureeAnnees)
                .setScale(2, RoundingMode.HALF_UP);
        if (interetsCourus.signum() < 0) {
            // Si dateAction < dateFin (action anticipée), pas d'intérêts.
            interetsCourus = BigDecimal.ZERO.setScale(2);
        }
        BigDecimal totalReclame = montantBrut.add(interetsCourus).setScale(2, RoundingMode.HALF_UP);

        // --- Prescription ---
        LocalDate dateLimite = computeDateLimitePrescription(typeArriere, dateFin, dateRupture);
        long joursRestants = ChronoUnit.DAYS.between(dateAction, dateLimite);
        RappelSalaireBeStatutPrescription statut = computeStatut(joursRestants);

        String formule = formatFormule(montantBrut, dureeAnnees, interetsCourus, totalReclame);

        return new RappelSalaireBeResult(
                montantBrut,
                dateDebut,
                dateFin,
                dateRupture,
                dateAction,
                typeArriere,
                interetsCourus,
                TAUX_MORATOIRE_LABEL,
                totalReclame,
                dateLimite,
                joursRestants,
                statut,
                BASE_JURIDIQUE,
                formule,
                AVERTISSEMENT_DEFAUT
        );
    }

    /**
     * dureeAnnees en {@link BigDecimal} décimal — base 365 j (convention
     * commerciale standard, cohérente avec la pratique BE).
     * Renvoie 0 si dateAction ≤ dateFin (pas d'intérêts).
     */
    private static BigDecimal computeDureeAnnees(LocalDate dateFin, LocalDate dateAction) {
        long jours = ChronoUnit.DAYS.between(dateFin, dateAction);
        if (jours <= 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(jours).divide(new BigDecimal("365"), 6, RoundingMode.HALF_UP);
    }

    /**
     * Calcule la date limite de prescription selon le type d'arriéré.
     *
     * <ul>
     *   <li>{@code PENDANT_CONTRAT} : {@code dateFinPeriode + 5 ans}.</li>
     *   <li>{@code POST_RUPTURE}    : {@code dateRupture + 1 an}.</li>
     *   <li>{@code MIXTE}           : prescription la plus stricte
     *       (1 an post-rupture) — V1 conservative.</li>
     * </ul>
     */
    private static LocalDate computeDateLimitePrescription(
            RappelSalaireBeTypeArriereEnum type,
            LocalDate dateFin,
            LocalDate dateRupture) {
        switch (type) {
            case PENDANT_CONTRAT:
                return dateFin.plusYears(DELAI_PENDANT_CONTRAT_ANNEES);
            case POST_RUPTURE:
                // dateRupture est obligatoire (cf. validateInputs).
                return dateRupture.plusYears(DELAI_POST_RUPTURE_ANNEES);
            case MIXTE:
                // Prescription la plus stricte applicable en V1.
                if (dateRupture != null) {
                    return dateRupture.plusYears(DELAI_POST_RUPTURE_ANNEES);
                }
                return dateFin.plusYears(DELAI_POST_RUPTURE_ANNEES);
            default:
                throw new IllegalStateException("typeArriere inattendu : " + type);
        }
    }

    private static RappelSalaireBeStatutPrescription computeStatut(long joursRestants) {
        if (joursRestants <= 0) {
            return RappelSalaireBeStatutPrescription.PRESCRIT;
        }
        if (joursRestants <= IMMINENT_SEUIL_JOURS) {
            return RappelSalaireBeStatutPrescription.IMMINENT;
        }
        return RappelSalaireBeStatutPrescription.NON_PRESCRIT;
    }

    private static String formatFormule(BigDecimal montantBrut,
                                        BigDecimal dureeAnnees,
                                        BigDecimal interets,
                                        BigDecimal total) {
        return String.format(
                "%s € × 10%% × %s an(s) = %s € intérêts ; total = %s €",
                formatMontant(montantBrut),
                dureeAnnees.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                formatMontant(interets),
                formatMontant(total));
    }

    private static String formatMontant(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static void validateInputs(RappelSalaireBeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.montantBrut() == null || request.montantBrut().signum() <= 0) {
            throw new IllegalArgumentException(
                    "montantBrut requis et strictement positif");
        }
        if (request.dateDebutPeriode() == null) {
            throw new IllegalArgumentException("dateDebutPeriode requise");
        }
        if (request.dateFinPeriode() == null) {
            throw new IllegalArgumentException("dateFinPeriode requise");
        }
        if (request.dateFinPeriode().isBefore(request.dateDebutPeriode())) {
            throw new IllegalArgumentException(
                    "Période invalide : dateFinPeriode doit être ≥ dateDebutPeriode");
        }
        if (request.typeArriere() == null) {
            throw new IllegalArgumentException("typeArriere obligatoire");
        }
        if (request.typeArriere() == RappelSalaireBeTypeArriereEnum.POST_RUPTURE
                && request.dateRupture() == null) {
            throw new IllegalArgumentException(
                    "dateRupture requise pour POST_RUPTURE");
        }
    }
}
