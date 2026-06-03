package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-223-02 : moteur décisionnel BE de la <b>recevabilité de l'adoption</b>
 * (loi du 24/04/2003 réformant l'adoption ; Code civil belge art. 343-1 et s.
 * — à vérifier par avocat belge, renumérotation CC post-réformes 2017-2019).
 *
 * <p>Outil multi-vues unique (1 outil = 1 situation « recevabilité de
 * l'adoption ») couvrant les trois types d'adoption :</p>
 * <ul>
 *   <li><b>PLENIERE</b> — rupture complète du lien de filiation d'origine
 *       (CC art. 356-1 et s. — à vérifier) ;</li>
 *   <li><b>SIMPLE</b> — adoption maintenant certains liens avec la famille
 *       d'origine (CC art. 353-1 et s. — à vérifier) ;</li>
 *   <li><b>CO_PARENTALE</b> — adoption de l'enfant du conjoint / cohabitant
 *       légal, exigeant un lien de mariage ou de cohabitation légale avec le
 *       parent biologique.</li>
 * </ul>
 *
 * <p><b>Pays</b> : BELGIQUE uniquement. ≠ adoption française (Cciv art. 343
 * et s., structurellement distincte). L'<b>adoption internationale</b>
 * (Convention de La Haye + autorité centrale) est différée (P4 F-224). La
 * contestation de filiation relève d'un outil distinct (F-217).</p>
 *
 * <p><b>Validation juridique requise</b> : les articles cités (CC art. 343-1
 * et s., loi du 24/04/2003) sont à <b>valider par un avocat belge avant
 * production</b> — renumérotation du Code civil post-réformes 2017-2019. Aucune
 * citation jurisprudentielle (F-JU-04 parké).</p>
 */
public final class AdoptionBeCalculator {

    /** Type d'adoption analysé (branche d'adoptant / d'effet). */
    public enum TypeAdoptionBe {
        PLENIERE,
        SIMPLE,
        CO_PARENTALE
    }

    /** Verdict de l'analyse de recevabilité (4 niveaux). */
    public enum AdoptionBeVerdict {
        RECEVABLE,
        RECEVABLE_SOUS_CONDITIONS,
        IRRECEVABLE,
        QUALIFICATION_INCOMPLETE
    }

    /** État du consentement des parents biologiques d'origine. */
    public enum ConsentementParentsBiologiques {
        OBTENU,
        REFUSE,
        SANS_OBJET
    }

    /** Âge minimal de l'adoptant (CC art. 343-1 — à vérifier). */
    private static final int AGE_ADOPTANT_MIN = 25;
    /** Écart d'âge minimal adoptant / adopté (CC art. 345 — à vérifier). */
    private static final int ECART_AGE_MIN = 15;
    /** Âge à partir duquel le consentement de l'adopté est requis (CC — à vérifier). */
    private static final int AGE_CONSENTEMENT_ADOPTE = 12;
    private static final int COMMENTAIRE_MAX = 1000;

    private AdoptionBeCalculator() {}

    /**
     * Applique l'arbre décisionnel BE de recevabilité de l'adoption sur les
     * éléments saisis, selon le type d'adoption choisi.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace ("BELGIQUE" uniquement supporté)
     * @return résultat structuré (verdict, motifs/conditions, actes à produire,
     *         bases juridiques, messages)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static AdoptionBeResult compute(AdoptionBeInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        if (!"BELGIQUE".equals(country.trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en BELGIQUE — recevabilité de l'adoption");
        }
        validateInputs(input);

        List<String> motifsIrrecevabilite = new ArrayList<>();
        List<String> conditionsASatisfaire = new ArrayList<>();
        List<String> actes = new ArrayList<>();

        int ecart = ecartEffectif(input);

        // --- Condition d'âge de l'adoptant (CC art. 343-1 — à vérifier) ---
        if (input.ageAdoptant() < AGE_ADOPTANT_MIN) {
            motifsIrrecevabilite.add(
                    "L'adoptant doit avoir au moins " + AGE_ADOPTANT_MIN + " ans (CC art. 343-1 "
                            + "— à vérifier). Âge saisi : " + input.ageAdoptant() + " ans.");
        }

        // --- Écart d'âge minimal adoptant / adopté (CC art. 345 — à vérifier) ---
        if (ecart < ECART_AGE_MIN) {
            motifsIrrecevabilite.add(
                    "L'écart d'âge entre l'adoptant et l'adopté doit être d'au moins "
                            + ECART_AGE_MIN + " ans (CC art. 345 — à vérifier). Écart constaté : "
                            + ecart + " an(s).");
        }

        // --- Lien exigé pour l'adoption co-parentale ---
        if (input.typeAdoption() == TypeAdoptionBe.CO_PARENTALE) {
            if (!Boolean.TRUE.equals(input.adoptantEnCoupleAvecParentBiologique())) {
                motifsIrrecevabilite.add(
                        "L'adoption co-parentale suppose que l'adoptant soit en couple avec le parent "
                                + "biologique de l'enfant (CC art. 343 — à vérifier).");
            }
            if (!Boolean.TRUE.equals(input.lienCohabitationLegaleOuMariage())) {
                motifsIrrecevabilite.add(
                        "L'adoption co-parentale exige un lien de mariage ou de cohabitation légale "
                                + "avec le parent biologique (CC art. 343 §1 — à vérifier).");
            }
        }

        // --- Consentements (adopté / parents biologiques) ---
        if (input.ageAdopte() >= AGE_CONSENTEMENT_ADOPTE
                && !Boolean.TRUE.equals(input.consentementAdopteOuRepresentants())) {
            motifsIrrecevabilite.add(
                    "Le consentement de l'adopté âgé d'au moins " + AGE_CONSENTEMENT_ADOPTE
                            + " ans (ou de ses représentants) doit être recueilli (CC art. 348-1 "
                            + "et s. — à vérifier).");
        }
        if (input.consentementParentsBiologiques() == ConsentementParentsBiologiques.REFUSE) {
            motifsIrrecevabilite.add(
                    "Le consentement des parents biologiques d'origine est refusé : l'adoption ne "
                            + "peut être prononcée qu'après une éventuelle décision passant outre le "
                            + "refus abusif devant le tribunal de la famille (CC art. 348-11 — à vérifier).");
        }

        // --- Si motifs d'irrecevabilité → IRRECEVABLE ---
        if (!motifsIrrecevabilite.isEmpty()) {
            actes.add("Informer le client que la requête en adoption sera déclarée irrecevable en "
                    + "l'état tant que les conditions ci-dessus ne sont pas réunies.");
            actes.add("Réexaminer la situation au regard des conditions de fond (âge, écart d'âge, "
                    + "lien de couple, consentements) avant toute saisine du tribunal de la famille.");
            return new AdoptionBeResult(
                    AdoptionBeVerdict.IRRECEVABLE,
                    motifsIrrecevabilite,
                    actes,
                    basesJuridiques(),
                    List.of("Conditions de recevabilité de l'adoption non réunies (loi du 24/04/2003 ; "
                            + "CC art. 343-1 et s. — à vérifier). La requête au tribunal de la famille "
                            + "ne peut prospérer en l'état."));
        }

        // --- Conditions suspensives → RECEVABLE_SOUS_CONDITIONS ---
        if (!Boolean.TRUE.equals(input.agrementObtenu())) {
            conditionsASatisfaire.add(
                    "Obtenir l'agrément préalable et l'enquête sociale du tribunal de la famille "
                            + "avant le prononcé de l'adoption (CC art. 346-1 et s. — à vérifier).");
        }
        if (input.consentementParentsBiologiques() == ConsentementParentsBiologiques.SANS_OBJET
                && input.typeAdoption() != TypeAdoptionBe.CO_PARENTALE) {
            conditionsASatisfaire.add(
                    "Vérifier l'état des consentements des parents biologiques d'origine (recueil "
                            + "ou constat de l'absence de filiation établie) avant le prononcé "
                            + "(CC art. 348-1 et s. — à vérifier).");
        }

        if (!conditionsASatisfaire.isEmpty()) {
            actes.add("Préparer le dossier d'agrément (enquête sociale) à soumettre au tribunal de "
                    + "la famille avant le dépôt de la requête en adoption.");
            actes.add("Rassembler les consentements requis et les actes d'état civil de l'adoptant "
                    + "et de l'adopté.");
            return new AdoptionBeResult(
                    AdoptionBeVerdict.RECEVABLE_SOUS_CONDITIONS,
                    conditionsASatisfaire,
                    actes,
                    basesJuridiques(),
                    List.of("Les conditions de fond paraissent réunies, sous réserve des conditions "
                            + "suspensives ci-dessus (agrément / consentements — CC art. 343-1 et s. "
                            + "— à vérifier) avant le prononcé par le tribunal de la famille."));
        }

        // --- Toutes conditions réunies → RECEVABLE ---
        actes.add("Préparer et déposer la requête en adoption " + libelleType(input.typeAdoption())
                + " devant le tribunal de la famille (CC art. 343-1 et s. — à vérifier).");
        actes.add("Joindre les actes d'état civil, l'agrément, l'enquête sociale et les consentements "
                + "recueillis.");
        return new AdoptionBeResult(
                AdoptionBeVerdict.RECEVABLE,
                List.of(),
                actes,
                basesJuridiques(),
                List.of("Les conditions de recevabilité de l'adoption " + libelleType(input.typeAdoption())
                        + " paraissent réunies (loi du 24/04/2003 ; CC art. 343-1 et s. — à vérifier). "
                        + "La requête peut être présentée au tribunal de la famille."));
    }

    // ---------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------

    private static void validateInputs(AdoptionBeInput in) {
        if (in.typeAdoption() == null) {
            throw new IllegalArgumentException(
                    "Le type d'adoption est requis (PLENIERE / SIMPLE / CO_PARENTALE)");
        }
        if (in.consentementParentsBiologiques() == null) {
            throw new IllegalArgumentException(
                    "L'état du consentement des parents biologiques est requis "
                            + "(OBTENU / REFUSE / SANS_OBJET)");
        }
        if (in.ageAdoptant() == null || in.ageAdoptant() <= 0 || in.ageAdoptant() > 120) {
            throw new IllegalArgumentException("L'âge de l'adoptant doit être un entier valide (1-120)");
        }
        if (in.ageAdopte() == null || in.ageAdopte() <= 0 || in.ageAdopte() > 120) {
            throw new IllegalArgumentException("L'âge de l'adopté doit être un entier valide (1-120)");
        }
        if (in.ecartAgeAnnees() != null && (in.ecartAgeAnnees() < 0 || in.ecartAgeAnnees() > 120)) {
            throw new IllegalArgumentException("L'écart d'âge, s'il est saisi, doit être un entier valide (0-120)");
        }
        if (in.commentaire() != null && in.commentaire().length() > COMMENTAIRE_MAX) {
            throw new IllegalArgumentException(
                    "Le commentaire ne peut dépasser " + COMMENTAIRE_MAX + " caractères");
        }
    }

    /**
     * Écart d'âge effectif : valeur saisie si fournie, sinon dérivée de la
     * différence des âges (adoptant - adopté).
     */
    private static int ecartEffectif(AdoptionBeInput in) {
        if (in.ecartAgeAnnees() != null) {
            return in.ecartAgeAnnees();
        }
        return in.ageAdoptant() - in.ageAdopte();
    }

    private static String libelleType(TypeAdoptionBe type) {
        return switch (type) {
            case PLENIERE -> "plénière";
            case SIMPLE -> "simple";
            case CO_PARENTALE -> "co-parentale";
        };
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static List<String> basesJuridiques() {
        return List.of(
                "Loi du 24/04/2003 réformant l'adoption (à vérifier)",
                "CC art. 343-1 et s. (à vérifier) — conditions générales de l'adoption (âge de l'adoptant)",
                "CC art. 345 (à vérifier) — écart d'âge minimal entre adoptant et adopté",
                "CC art. 346-1 et s. (à vérifier) — agrément préalable et enquête sociale",
                "CC art. 348-1 et s. (à vérifier) — consentements requis (adopté, parents biologiques)",
                "CC art. 353-1 et s. (à vérifier) — adoption simple (maintien de certains liens d'origine)",
                "CC art. 356-1 et s. (à vérifier) — adoption plénière (rupture du lien d'origine)");
    }
}
