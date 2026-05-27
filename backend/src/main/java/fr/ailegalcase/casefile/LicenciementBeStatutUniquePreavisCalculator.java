package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * SF-213-03 : calculateur du préavis statut unique BE — fonction pure.
 *
 * <p>Calcule la durée du délai de préavis applicable à un licenciement
 * notifié par l'employeur selon le <b>statut unique belge</b>
 * (<b>Loi du 26 décembre 2013</b> — barème art. 37/1 et seq.), pour les
 * contrats signés ou les périodes d'ancienneté à compter du 01/01/2014,
 * ainsi que l'indemnité compensatoire de préavis (rémunération
 * hebdomadaire × nombre de semaines).</p>
 *
 * <h2>Barème (côté EMPLOYEUR)</h2>
 * <p>Voir constante {@link #BAREME_STATUT_UNIQUE} — table des tranches.
 * La résolution d'une ancienneté en tranche est faite par troncature
 * (années complètes) — les mois supplémentaires servent uniquement à
 * documenter la finesse de l'ancienneté dans le {@code formuleCalcul}.</p>
 *
 * <h2>Date de fin de préavis</h2>
 * <pre>
 *   dateFinPreavis = dateNotificationLicenciement + dureePreavisEnSemaines × 7 jours
 * </pre>
 * <p>Règle simple : addition de N × 7 jours à la date de notification. Les
 * règles plus fines (point de départ au lundi suivant, prise en compte du
 * délai de notification de 3 jours ouvrables — art. 37 §1 al. 3) sont
 * documentées dans l'{@code avertissement} comme "à confirmer avec avocat
 * BE pour règle exacte" (cf. mini-spec).</p>
 *
 * <h2>Indemnité compensatoire</h2>
 * <pre>
 *   indemniteCompensatoire = salaireHebdomadaireBrut × dureePreavisEnSemaines
 * </pre>
 * <p>(Loi du 03/07/1978 art. 39, adapté statut unique.)</p>
 *
 * <p>Single-country BELGIQUE — un préavis post-2014 100 % statut unique est
 * distinct de la Formule Claeys (SF-213-04, pré-2014). Si l'utilisateur
 * indique {@code partieStatutUniqueSeulement = false}, un avertissement est
 * émis pour rappeler que la partie pré-2014 doit être calculée séparément.</p>
 */
public final class LicenciementBeStatutUniquePreavisCalculator {

    private static final String BASE_JURIDIQUE =
            "Loi 26/12/2013 — statut unique — barème art. 37/1 et seq. (côté employeur)";

    private static final String AVERTISSEMENT_PRE_2014 =
            "Contrat antérieur au 01/01/2014 : préavis pré-2014 à calculer séparément via "
                    + "l'outil Formule Claeys (SF-213-04). Cumuler ensuite les deux préavis.";

    private static final String AVERTISSEMENT_DEFAUT =
            "Barème statut unique côté employeur — à confirmer avec avocat BE pour règles "
                    + "de point de départ (lundi suivant, délai 3 jours ouvrables art. 37 §1 al. 3). "
                    + "Préavis salarié (démission) plafonné à 13 semaines, hors périmètre de cet outil.";

    /**
     * Barème officiel statut unique (côté EMPLOYEUR) — Loi 26/12/2013.
     *
     * <p>Délégué à {@link LicenciementBeBaremeStatutUnique#BAREME_STATUT_UNIQUE}
     * (refactor DRY SF-213-04 — table partagée avec
     * {@link LicenciementBeFormuleClaeysCalculator} pour la clause de
     * sauvegarde art. 67).</p>
     */
    static final int[] BAREME_STATUT_UNIQUE = LicenciementBeBaremeStatutUnique.BAREME_STATUT_UNIQUE;

    /** Préavis pour ancienneté &lt; 3 mois (statut unique). */
    static final int PREAVIS_SEMAINES_MOINS_3_MOIS =
            LicenciementBeBaremeStatutUnique.PREAVIS_SEMAINES_MOINS_3_MOIS;

    /** Préavis plafond pour ancienneté ≥ 20 ans (statut unique). */
    static final int PREAVIS_SEMAINES_PLAFOND_20_ANS =
            LicenciementBeBaremeStatutUnique.PREAVIS_SEMAINES_PLAFOND_20_ANS;

    private LicenciementBeStatutUniquePreavisCalculator() {
    }

    public static LicenciementBeStatutUniquePreavisResult calculate(
            LicenciementBeStatutUniquePreavisRequest request) {
        validateInputs(request);

        int annees = request.ancienneteAnnees();
        int moisSupp = request.ancienneteMoisSupplementaires() != null
                ? request.ancienneteMoisSupplementaires() : 0;
        BigDecimal salaireHebdo = request.salaireHebdomadaireBrut().setScale(2, RoundingMode.HALF_UP);
        LocalDate dateNotif = request.dateNotificationLicenciement();
        boolean statutUniqueSeulement = request.partieStatutUniqueSeulement() == null
                || request.partieStatutUniqueSeulement();

        int dureePreavisEnSemaines = resolveDureePreavis(annees, moisSupp);
        LocalDate dateFinPreavis = dateNotif.plusDays((long) dureePreavisEnSemaines * 7);
        BigDecimal indemniteCompensatoire = salaireHebdo
                .multiply(BigDecimal.valueOf(dureePreavisEnSemaines))
                .setScale(2, RoundingMode.HALF_UP);

        String formuleCalcul = buildFormule(annees, moisSupp, dureePreavisEnSemaines,
                salaireHebdo, indemniteCompensatoire);
        String avertissement = statutUniqueSeulement ? AVERTISSEMENT_DEFAUT : AVERTISSEMENT_PRE_2014;

        return new LicenciementBeStatutUniquePreavisResult(
                annees,
                moisSupp,
                salaireHebdo,
                dateNotif,
                statutUniqueSeulement,
                dureePreavisEnSemaines,
                dateFinPreavis,
                indemniteCompensatoire,
                formuleCalcul,
                BASE_JURIDIQUE,
                avertissement);
    }

    /**
     * Résout la durée du préavis selon le barème statut unique.
     *
     * <ul>
     *   <li>Ancienneté = 0 année complète, mois &lt; 3 → 2 semaines.</li>
     *   <li>Ancienneté = 0 année complète, mois ≥ 3 → 4 semaines (tranche 0-1 an).</li>
     *   <li>1 à 19 années complètes → tranche du barème indexée sur les années.</li>
     *   <li>≥ 20 années complètes → 62 semaines (plafond).</li>
     * </ul>
     */
    static int resolveDureePreavis(int anneesCompletes, int moisSupplementaires) {
        return LicenciementBeBaremeStatutUnique.resolveDureePreavis(
                anneesCompletes, moisSupplementaires);
    }

    private static String buildFormule(int annees, int moisSupp, int semaines,
                                       BigDecimal salaireHebdo, BigDecimal indemnite) {
        String ancienneteLabel;
        if (annees <= 0 && moisSupp < 3) {
            ancienneteLabel = "< 3 mois";
        } else if (annees <= 0) {
            ancienneteLabel = moisSupp + " mois";
        } else if (moisSupp > 0) {
            ancienneteLabel = annees + " an" + (annees > 1 ? "s" : "") + " " + moisSupp + " mois";
        } else {
            ancienneteLabel = annees + " an" + (annees > 1 ? "s" : "");
        }
        return String.format(
                "%s ancienneté → %d semaines (loi 26/12/2013) ; ICP = %s € × %d = %s €",
                ancienneteLabel,
                semaines,
                formatMontant(salaireHebdo),
                semaines,
                formatMontant(indemnite));
    }

    private static String formatMontant(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static void validateInputs(LicenciementBeStatutUniquePreavisRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.ancienneteAnnees() == null || request.ancienneteAnnees() < 0) {
            throw new IllegalArgumentException("ancienneteAnnees requise et ≥ 0");
        }
        if (request.ancienneteMoisSupplementaires() != null
                && (request.ancienneteMoisSupplementaires() < 0
                    || request.ancienneteMoisSupplementaires() > 11)) {
            throw new IllegalArgumentException("ancienneteMoisSupplementaires doit être entre 0 et 11");
        }
        if (request.salaireHebdomadaireBrut() == null
                || request.salaireHebdomadaireBrut().signum() <= 0) {
            throw new IllegalArgumentException(
                    "salaireHebdomadaireBrut requis et strictement positif");
        }
        if (request.dateNotificationLicenciement() == null) {
            throw new IllegalArgumentException("dateNotificationLicenciement requise");
        }
    }
}
