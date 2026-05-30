package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.BaremeSaisieRemuneration.TrancheSaisie;

import java.util.List;

/**
 * SF-218-07 : calculateur de la quotité saisissable d'une rémunération
 * (art. R. 3252-2, R. 3252-3 et L. 3252-3 Code du travail). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Trois situations sont distinguées (invariant CLAUDE.md — un outil = une
 * situation métier) :
 * <ul>
 *   <li><b>Rémunération inférieure à la fraction insaisissable</b> : aucune
 *       somme ne peut être saisie, la part égale au montant forfaitaire RSA est
 *       absolument insaisissable (art. L. 3252-3 CT) → {@code INSAISISSABLE}.</li>
 *   <li><b>Créance alimentaire</b> : le créancier d'aliments peut recourir au
 *       paiement direct (loi du 2 janvier 1973), qui prime sur le barème
 *       classique ; seule la fraction insaisissable reste réservée au débiteur
 *       → {@code ALIMENTAIRE_PAIEMENT_DIRECT}.</li>
 *   <li><b>Créance ordinaire</b> : la part saisissable résulte du barème
 *       progressif par tranches (art. R. 3252-2 CT), les seuils étant majorés
 *       par personne à charge (art. R. 3252-3 CT) → {@code SAISISSABLE} si la
 *       quotité est strictement positive.</li>
 * </ul>
 *
 * <p>Sources :
 * <ul>
 *   <li>art. R. 3252-1 à R. 3252-5 CT — procédure et barème de la saisie des
 *       rémunérations ;</li>
 *   <li>art. R. 3252-2 CT — barème annuel des quotités saisissables par
 *       tranches (révisé chaque année par décret) ;</li>
 *   <li>art. R. 3252-3 CT — majoration des seuils par personne à charge ;</li>
 *   <li>art. L. 3252-2 CT — fractions saisissables ;</li>
 *   <li>art. L. 3252-3 CT — fraction absolument insaisissable (montant
 *       forfaitaire RSA).</li>
 * </ul>
 */
public final class SaisieRemunerationCalculator {

    static final String BASE_JURIDIQUE =
            "art. R. 3252-1 à R. 3252-5 Code du travail (procédure et barème de la "
                    + "saisie des rémunérations) ; art. R. 3252-2 Code du travail (barème "
                    + "annuel des quotités saisissables par tranches, révisé chaque année "
                    + "par décret) ; art. R. 3252-3 Code du travail (majoration des seuils "
                    + "par personne à charge) ; art. L. 3252-2 et L. 3252-3 Code du travail "
                    + "(fractions saisissables et fraction absolument insaisissable égale au "
                    + "montant forfaitaire du RSA)";

    private SaisieRemunerationCalculator() {
    }

    /**
     * Calcule la quotité saisissable, le montant laissé au salarié, le nombre de
     * mois de recouvrement et le verdict.
     *
     * @param remunerationNetteMensuelle rémunération nette mensuelle (assiette,
     *        strictement positive).
     * @param nombrePersonnesACharge nombre de personnes à charge (≥ 0 ; null
     *        traité comme 0).
     * @param creanceTotale montant de la créance à recouvrer (strictement positif).
     * @param creanceAlimentaire true si la créance est alimentaire.
     */
    public static SaisieRemunerationResult calculate(Double remunerationNetteMensuelle,
                                                     Integer nombrePersonnesACharge,
                                                     Double creanceTotale,
                                                     Boolean creanceAlimentaire) {
        validate(remunerationNetteMensuelle, nombrePersonnesACharge, creanceTotale);

        int personnesACharge = nombrePersonnesACharge == null ? 0 : nombrePersonnesACharge;
        boolean alimentaire = creanceAlimentaire != null && creanceAlimentaire;
        double remuneration = remunerationNetteMensuelle;
        double fractionInsaisissable = BaremeSaisieRemuneration.FRACTION_INSAISISSABLE_RSA_2026;

        double quotite;
        SaisieRemunerationVerdict verdict;

        if (remuneration <= fractionInsaisissable) {
            // Toute la rémunération est protégée par la fraction insaisissable.
            quotite = 0.0;
            verdict = SaisieRemunerationVerdict.INSAISISSABLE;
        } else if (alimentaire) {
            // Paiement direct (loi 1973) : seule la fraction insaisissable est
            // réservée au débiteur, le reste est mobilisable hors barème.
            quotite = round2(remuneration - fractionInsaisissable);
            verdict = SaisieRemunerationVerdict.ALIMENTAIRE_PAIEMENT_DIRECT;
        } else {
            quotite = round2(quotiteBaremeMensuelle(remuneration, personnesACharge));
            // La part saisissable ne peut jamais entamer la fraction insaisissable.
            double maxSaisissable = remuneration - fractionInsaisissable;
            if (quotite > maxSaisissable) {
                quotite = round2(maxSaisissable);
            }
            verdict = quotite > 0
                    ? SaisieRemunerationVerdict.SAISISSABLE
                    : SaisieRemunerationVerdict.INSAISISSABLE;
        }

        double montantLaisse = round2(remuneration - quotite);
        if (montantLaisse < fractionInsaisissable) {
            montantLaisse = round2(fractionInsaisissable);
        }

        int nombreMois = quotite > 0
                ? (int) Math.ceil(creanceTotale / quotite)
                : 0;

        return new SaisieRemunerationResult(
                round2(remuneration),
                personnesACharge,
                round2(creanceTotale),
                alimentaire,
                quotite,
                montantLaisse,
                round2(fractionInsaisissable),
                nombreMois,
                verdict,
                BASE_JURIDIQUE);
    }

    /**
     * Applique le barème progressif par tranches (art. R. 3252-2 CT) sur la
     * rémunération mensuelle, les bornes annuelles étant ramenées au mois et
     * majorées par personne à charge (art. R. 3252-3 CT).
     */
    private static double quotiteBaremeMensuelle(double remunerationMensuelle, int personnesACharge) {
        double majorationAnnuelle =
                personnesACharge * BaremeSaisieRemuneration.MAJORATION_PAR_PERSONNE_A_CHARGE_2026;
        List<TrancheSaisie> bareme = BaremeSaisieRemuneration.BAREME_SAISIE_REMUNERATION_2026;

        double quotite = 0.0;
        double borneBasseMensuelle = 0.0;
        for (TrancheSaisie tranche : bareme) {
            double borneHauteMensuelle;
            if (tranche.borneSuperieureAnnuelle() == Double.MAX_VALUE) {
                borneHauteMensuelle = Double.MAX_VALUE;
            } else {
                borneHauteMensuelle = (tranche.borneSuperieureAnnuelle() + majorationAnnuelle) / 12.0;
            }
            if (remunerationMensuelle <= borneBasseMensuelle) {
                break;
            }
            double plafondTranche = Math.min(remunerationMensuelle, borneHauteMensuelle);
            double largeurTranche = plafondTranche - borneBasseMensuelle;
            if (largeurTranche > 0) {
                quotite += largeurTranche * tranche.fraction();
            }
            borneBasseMensuelle = borneHauteMensuelle;
        }
        return quotite;
    }

    private static void validate(Double remunerationNetteMensuelle,
                                 Integer nombrePersonnesACharge,
                                 Double creanceTotale) {
        if (remunerationNetteMensuelle == null || remunerationNetteMensuelle <= 0) {
            throw new IllegalArgumentException("remunerationNetteMensuelle doit être strictement positive");
        }
        if (creanceTotale == null || creanceTotale <= 0) {
            throw new IllegalArgumentException("creanceTotale doit être strictement positive");
        }
        if (nombrePersonnesACharge != null && nombrePersonnesACharge < 0) {
            throw new IllegalArgumentException("nombrePersonnesACharge ne peut pas être négatif");
        }
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
