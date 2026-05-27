package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * SF-213-04 : calculateur du préavis selon la <b>Formule Claeys</b> (ancien
 * art. 82 Loi du 03/07/1978, avant la loi du 26/12/2013) — fonction pure.
 *
 * <h2>Périmètre</h2>
 * <p>Applique aux contrats antérieurs au 01/01/2014 (statut unique) pour les
 * employés au-dessus du seuil de rémunération (anciennement ≈ 32 254 €/an).
 * La <b>clause de sauvegarde</b> de la loi du 26/12/2013 art. 67 permet de
 * cumuler ce préavis Claeys (partie pré-2014) avec le préavis statut unique
 * pour la partie d'ancienneté post-2014.</p>
 *
 * <h2>Formule Claeys (jurisprudence)</h2>
 * <pre>
 *   preavisEnMois = 0.8 × [ancienneté (années) + 0.04 × rémunération annuelle brute (K€)]
 *   preavisEnSemaines = arrondi(preavisEnMois × 4.333)
 * </pre>
 *
 * <h2>Clause de sauvegarde art. 67 (optionnelle)</h2>
 * <pre>
 *   preavisTotalSemaines = preavisClaeysSemaines + preavisStatutUniquesSemaines
 *   indemniteTotaleBrute = salaireHebdomadaireBrut × preavisTotalSemaines
 * </pre>
 * <p>Le barème statut unique post-2014 est délégué à
 * {@link LicenciementBeBaremeStatutUnique} (refactor DRY — source de vérité
 * partagée avec {@link LicenciementBeStatutUniquePreavisCalculator}).</p>
 *
 * <h2>Avertissement (obligatoire)</h2>
 * <p>La Formule Claeys est <b>issue de la jurisprudence</b> (Cour du travail,
 * arrêt Claeys et constants), pas du texte écrit strict. Les seuils exacts
 * de rémunération applicables au cas d'espèce doivent être validés avec un
 * avocat BE — un avertissement est <b>toujours</b> émis dans la réponse.</p>
 */
public final class LicenciementBeFormuleClaeysCalculator {

    private static final String BASE_JURIDIQUE_SAUVEGARDE =
            "Loi 03/07/1978 art. 82 (avant loi 26/12/2013) ; Loi 26/12/2013 art. 67 (clause sauvegarde)";

    private static final String BASE_JURIDIQUE_CLAEYS_SEUL =
            "Loi 03/07/1978 art. 82 (avant loi 26/12/2013) — formule Claeys jurisprudentielle";

    private static final String AVERTISSEMENT_DEFAUT =
            "Formule Claeys issue de la jurisprudence — à valider avec un avocat BE pour les "
                    + "seuils exacts de rémunération applicables au cas d'espèce (anciennement "
                    + "≈ 32 254 €/an). Cumul Claeys + statut unique encore fréquemment contesté "
                    + "en contentieux 2026 — privilégier l'audition préalable.";

    /** Coefficient de la formule Claeys (jurisprudence constante). */
    private static final BigDecimal COEFFICIENT_CLAEYS = new BigDecimal("0.8");

    /** Coefficient du facteur rémunération de la formule Claeys (jurisprudence). */
    private static final BigDecimal COEFFICIENT_REMUNERATION = new BigDecimal("0.04");

    /** Nombre moyen de semaines par mois (52/12). */
    private static final BigDecimal SEMAINES_PAR_MOIS = new BigDecimal("4.333");

    /** Échelle BigDecimal pour les calculs d'ancienneté (4 décimales). */
    private static final int SCALE_ANCIENNETE = 4;

    /** Échelle BigDecimal pour les montants (2 décimales). */
    private static final int SCALE_MONTANTS = 2;

    private LicenciementBeFormuleClaeysCalculator() {
    }

    public static LicenciementBeFormuleClaeysResult calculate(
            LicenciementBeFormuleClaeysRequest request) {
        validateInputs(request);

        int anneesPre = request.ancienneteAnneesPreStatutUnique();
        int moisPre = request.ancienneteMoisPreStatutUnique() != null
                ? request.ancienneteMoisPreStatutUnique() : 0;
        BigDecimal remunerationKEur = request.remunerationAnnuelleBruteEnMilliers()
                .setScale(SCALE_MONTANTS, RoundingMode.HALF_UP);
        boolean appliquerSauvegarde = request.appliquerClauseSauvegarde() == null
                || request.appliquerClauseSauvegarde();

        // ── Partie Claeys (pré-2014) ─────────────────────────────────────────
        BigDecimal ancienneteTotaleAnnees = BigDecimal.valueOf(anneesPre)
                .add(BigDecimal.valueOf(moisPre).divide(
                        BigDecimal.valueOf(12), SCALE_ANCIENNETE, RoundingMode.HALF_UP));

        // preavisClaeysMois = 0.8 × (anciennete + 0.04 × remuneration_K€)
        BigDecimal facteurRemuneration = COEFFICIENT_REMUNERATION.multiply(remunerationKEur);
        BigDecimal somme = ancienneteTotaleAnnees.add(facteurRemuneration);
        BigDecimal preavisClaeysMois = COEFFICIENT_CLAEYS.multiply(somme)
                .setScale(SCALE_MONTANTS, RoundingMode.HALF_UP);

        // preavisClaeysSemaines = round(preavisClaeysMois × 4.333)
        int preavisClaeysSemaines = preavisClaeysMois.multiply(SEMAINES_PAR_MOIS)
                .setScale(0, RoundingMode.HALF_UP).intValue();

        // ── Partie statut unique (post-2014, si clause sauvegarde) ────────────
        int preavisStatutUniquesSemaines;
        BigDecimal salaireHebdo = null;
        BigDecimal indemniteClaeysBrute = null;
        BigDecimal indemniteTotaleBrute = null;

        if (appliquerSauvegarde) {
            int anneesPost = request.ancienneteAnneesPostStatutUnique();
            salaireHebdo = request.salaireHebdomadaireBrut()
                    .setScale(SCALE_MONTANTS, RoundingMode.HALF_UP);

            preavisStatutUniquesSemaines =
                    LicenciementBeBaremeStatutUnique.resolveDureePreavisAnneesEntieres(anneesPost);

            indemniteClaeysBrute = salaireHebdo
                    .multiply(BigDecimal.valueOf(preavisClaeysSemaines))
                    .setScale(SCALE_MONTANTS, RoundingMode.HALF_UP);

            int preavisTotalSemaines = preavisClaeysSemaines + preavisStatutUniquesSemaines;
            indemniteTotaleBrute = salaireHebdo
                    .multiply(BigDecimal.valueOf(preavisTotalSemaines))
                    .setScale(SCALE_MONTANTS, RoundingMode.HALF_UP);
        } else {
            preavisStatutUniquesSemaines = 0;
        }

        int preavisTotalSemaines = preavisClaeysSemaines + preavisStatutUniquesSemaines;

        String formuleClaeys = buildFormule(anneesPre, moisPre, remunerationKEur,
                preavisClaeysMois, preavisClaeysSemaines);
        String baseJuridique = appliquerSauvegarde ? BASE_JURIDIQUE_SAUVEGARDE : BASE_JURIDIQUE_CLAEYS_SEUL;

        return new LicenciementBeFormuleClaeysResult(
                anneesPre,
                moisPre,
                remunerationKEur,
                appliquerSauvegarde,
                appliquerSauvegarde ? request.ancienneteAnneesPostStatutUnique() : null,
                salaireHebdo,
                preavisClaeysMois,
                preavisClaeysSemaines,
                preavisStatutUniquesSemaines,
                preavisTotalSemaines,
                indemniteClaeysBrute,
                indemniteTotaleBrute,
                formuleClaeys,
                baseJuridique,
                AVERTISSEMENT_DEFAUT);
    }

    private static String buildFormule(int anneesPre, int moisPre,
                                       BigDecimal remunerationKEur,
                                       BigDecimal preavisMois,
                                       int preavisSemaines) {
        String ancienneteLabel;
        if (moisPre > 0) {
            ancienneteLabel = anneesPre + " ans " + moisPre + " mois";
        } else {
            ancienneteLabel = anneesPre + " ans";
        }
        return String.format(
                "0.8 × (%s + 0.04 × %s K€) = %s mois ≈ %d semaines",
                ancienneteLabel,
                stripTrailingZeros(remunerationKEur),
                stripTrailingZeros(preavisMois),
                preavisSemaines);
    }

    private static String stripTrailingZeros(BigDecimal bd) {
        BigDecimal stripped = bd.stripTrailingZeros();
        // Évite la notation scientifique (ex : 6E+1 → 60)
        return stripped.scale() < 0 ? stripped.setScale(0).toPlainString() : stripped.toPlainString();
    }

    private static void validateInputs(LicenciementBeFormuleClaeysRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.ancienneteAnneesPreStatutUnique() == null
                || request.ancienneteAnneesPreStatutUnique() < 0) {
            throw new IllegalArgumentException("ancienneteAnneesPreStatutUnique requise et ≥ 0");
        }
        if (request.ancienneteMoisPreStatutUnique() != null
                && (request.ancienneteMoisPreStatutUnique() < 0
                || request.ancienneteMoisPreStatutUnique() > 11)) {
            throw new IllegalArgumentException(
                    "ancienneteMoisPreStatutUnique doit être entre 0 et 11");
        }
        if (request.remunerationAnnuelleBruteEnMilliers() == null
                || request.remunerationAnnuelleBruteEnMilliers().signum() <= 0) {
            throw new IllegalArgumentException(
                    "remunerationAnnuelleBruteEnMilliers requise et strictement positive");
        }

        boolean appliquerSauvegarde = request.appliquerClauseSauvegarde() == null
                || request.appliquerClauseSauvegarde();
        if (appliquerSauvegarde) {
            if (request.ancienneteAnneesPostStatutUnique() == null
                    || request.ancienneteAnneesPostStatutUnique() < 0) {
                throw new IllegalArgumentException(
                        "ancienneteAnneesPostStatutUnique requise et ≥ 0 quand la clause de "
                                + "sauvegarde est appliquée");
            }
            if (request.salaireHebdomadaireBrut() == null
                    || request.salaireHebdomadaireBrut().signum() <= 0) {
                throw new IllegalArgumentException(
                        "salaireHebdomadaireBrut requis et strictement positif quand la clause "
                                + "de sauvegarde est appliquée");
            }
        }
    }
}
