package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-214-09 : analyseur de la catégorie d'OQTF de l'article L. 611-1 CESEDA
 * (1° à 7°) et des moyens de défense spécifiques à chaque catégorie. Outil
 * single-country FR.
 *
 * <p>Complémentaire de F-IM-08 (OQTF avec/sans délai) : F-IM-08 calcule les
 * délais et les contestations génériques, F-IM-29 (cet outil) identifie la
 * catégorie L. 611-1 et les moyens de défense qui lui sont propres.
 *
 * <p>Source juridique :
 * <ul>
 *   <li>L. 611-1 1° à 7° CESEDA — 7 catégories d'OQTF (recodification 2021,
 *       anciens L. 511-1 I 1° à 7°)</li>
 *   <li>L. 614-5 CESEDA — OQTF avec délai de départ volontaire, recours 30 j
 *       devant le TA</li>
 *   <li>L. 614-1 CESEDA — OQTF sans délai, recours 48 h</li>
 *   <li>L. 612-6 CESEDA — IRTF (interdiction de retour) possible notamment en
 *       cas de menace pour l'ordre public</li>
 *   <li>CE 5 décembre 2014 n° 373520 — proportionnalité OQTF menace ordre
 *       public (catégorie 6)</li>
 *   <li>CE 10 octobre 2013 n° 366528 — OQTF entrée irrégulière (catégorie 1),
 *       vices de procédure</li>
 *   <li>Loi 26 janvier 2024 (Darmanin) — modifications délais et catégories</li>
 * </ul>
 */
public final class OqtfCategoriesAnalyzer {

    public static final int MOTIF_OQTF_MAX_LENGTH = 300;

    /** OQTF avec délai de départ volontaire : recours 30 j (L. 614-5). */
    public static final int DELAI_RECOURS_AVEC_DELAI_JOURS = 30;

    /** OQTF sans délai : recours 48 h (L. 614-1). */
    public static final int DELAI_RECOURS_SANS_DELAI_HEURES = 48;

    public static final String TYPE_RECOURS_AVEC_DELAI = "AVEC_DELAI_30J_L614-5";
    public static final String TYPE_RECOURS_SANS_DELAI = "SANS_DELAI_48H_L614-1";

    /** Renvois vers d'autres outils / procédures parallèles. */
    public static final String PROCEDURE_DUBLIN_F_IM_22 =
            "OQTF prise dans le cadre d'une procédure Dublin — vérifier le recours "
            + "contre la décision de transfert (outil F-IM-22 Dublin recours, "
            + "règlement (UE) n° 604/2013).";
    public static final String PROCEDURE_IRTF_L612_6 =
            "Une interdiction de retour sur le territoire français (IRTF) peut accompagner "
            + "l'OQTF en cas de menace pour l'ordre public (L. 612-6) — contester sa durée "
            + "et son principe au regard de la proportionnalité.";

    private OqtfCategoriesAnalyzer() {}

    /**
     * Analyse la catégorie d'OQTF L. 611-1 et produit les moyens de défense.
     *
     * @param categorie           catégorie L. 611-1 (1° à 7°)
     * @param dateNotificationOqtf date de notification de l'OQTF (≤ aujourd'hui)
     * @param motifOqtf            motif libre de l'OQTF (≤ 300 caractères, nullable)
     * @return résultat de l'analyse
     */
    public static OqtfCategoriesResult analyze(OqtfCategorieL611 categorie,
                                               LocalDate dateNotificationOqtf,
                                               String motifOqtf) {
        validateInputs(categorie, dateNotificationOqtf, motifOqtf);

        String libelle = categorieLibelle(categorie);
        String baseJuridique = baseJuridique(categorie);
        List<String> moyensDefense = moyensDefense(categorie);
        boolean sansDelai = isOqtfSansDelaiParDefaut(categorie);
        String delaiRecours = sansDelai ? TYPE_RECOURS_SANS_DELAI : TYPE_RECOURS_AVEC_DELAI;
        Integer delaiRecoursJours = sansDelai ? null : DELAI_RECOURS_AVEC_DELAI_JOURS;
        Integer delaiRecoursHeures = sansDelai ? DELAI_RECOURS_SANS_DELAI_HEURES : null;
        String procedureParallele = procedureParallele(categorie);

        return new OqtfCategoriesResult(
                categorie,
                libelle,
                dateNotificationOqtf,
                motifOqtf,
                baseJuridique,
                moyensDefense,
                delaiRecours,
                delaiRecoursJours,
                delaiRecoursHeures,
                procedureParallele);
    }

    private static String categorieLibelle(OqtfCategorieL611 c) {
        return switch (c) {
            case CAT_1 -> "1° Entrée irrégulière sur le territoire français";
            case CAT_2 -> "2° Maintien au-delà de la durée de séjour autorisée (séjour expiré)";
            case CAT_3 -> "3° Comportement frauduleux pour l'obtention d'un titre (fraude au titre)";
            case CAT_4 -> "4° Refus de délivrance ou de renouvellement d'un titre de séjour";
            case CAT_5 -> "5° Retrait d'un titre de séjour, récépissé ou autorisation provisoire";
            case CAT_6 -> "6° Comportement constituant une menace pour l'ordre public";
            case CAT_7 -> "7° OQTF prise dans le cadre d'une procédure Dublin (remise à l'État responsable)";
        };
    }

    private static String baseJuridique(OqtfCategorieL611 c) {
        String numero = switch (c) {
            case CAT_1 -> "1°";
            case CAT_2 -> "2°";
            case CAT_3 -> "3°";
            case CAT_4 -> "4°";
            case CAT_5 -> "5°";
            case CAT_6 -> "6°";
            case CAT_7 -> "7°";
        };
        return "CESEDA L. 611-1 " + numero + " (recodification 2021, ancien L. 511-1 I " + numero + ")";
    }

    private static List<String> moyensDefense(OqtfCategorieL611 c) {
        List<String> moyens = new ArrayList<>();
        switch (c) {
            case CAT_1 -> {
                moyens.add("Vérifier la régularité de la notification de l'OQTF (signature, "
                        + "compétence de l'auteur, motivation en fait et en droit).");
                moyens.add("Contester la matérialité de l'entrée irrégulière (preuve d'une entrée "
                        + "régulière : visa, tampon d'entrée, justificatifs de franchissement).");
                moyens.add("Soulever l'absence ou l'insuffisance de base légale et les vices de "
                        + "procédure (CE 10 octobre 2013 n° 366528).");
            }
            case CAT_2 -> {
                moyens.add("Établir le maintien d'un droit au séjour (demande de titre en cours, "
                        + "récépissé valide, prolongation de visa).");
                moyens.add("Contester le calcul de la durée de séjour autorisée et la date "
                        + "d'expiration retenue.");
                moyens.add("Invoquer les circonstances de la vie privée et familiale (art. 8 CEDH) "
                        + "faisant obstacle à l'éloignement.");
            }
            case CAT_3 -> {
                moyens.add("Contester la caractérisation de la fraude : l'administration doit "
                        + "rapporter la preuve de l'intention frauduleuse.");
                moyens.add("Démontrer la bonne foi et l'exactitude des éléments produits lors de la "
                        + "demande de titre.");
                moyens.add("Soulever le respect du contradictoire et le droit d'être entendu avant "
                        + "la décision (art. 41 Charte UE).");
            }
            case CAT_4 -> {
                moyens.add("Contester par voie d'exception la légalité du refus de titre qui fonde "
                        + "l'OQTF (illégalité de la décision support).");
                moyens.add("Démontrer l'éligibilité au titre refusé (réunir les conditions de fond "
                        + "du titre demandé).");
                moyens.add("Invoquer l'atteinte disproportionnée à la vie privée et familiale "
                        + "(art. 8 CEDH).");
            }
            case CAT_5 -> {
                moyens.add("Contester la légalité du retrait du titre (motivation, réalité des "
                        + "motifs, respect de la procédure contradictoire).");
                moyens.add("Vérifier l'existence d'une base légale du retrait et son caractère "
                        + "proportionné.");
                moyens.add("Invoquer la confiance légitime et les droits acquis tirés du titre "
                        + "détenu.");
            }
            case CAT_6 -> {
                moyens.add("Procéder à l'examen de proportionnalité de la mesure au regard de "
                        + "l'art. 8 CEDH (CE 5 décembre 2014 n° 373520).");
                moyens.add("Contester la réalité et l'actualité de la menace pour l'ordre public "
                        + "(faits anciens, réinsertion, absence de récidive).");
                moyens.add("Faire valoir l'ancienneté et l'intensité des liens en France pour "
                        + "relativiser la menace invoquée.");
            }
            case CAT_7 -> {
                moyens.add("Vérifier la régularité de la procédure Dublin et de la détermination de "
                        + "l'État membre responsable (règlement (UE) n° 604/2013).");
                moyens.add("Invoquer les clauses discrétionnaire et de souveraineté ainsi que le "
                        + "risque de traitement inhumain ou dégradant dans l'État responsable.");
                moyens.add("Contester le délai de transfert et l'expiration éventuelle de la "
                        + "responsabilité de l'État requis.");
            }
        }
        return moyens;
    }

    /**
     * Détermine si la catégorie correspond, par défaut, à une OQTF sans délai de
     * départ volontaire (recours 48 h, L. 614-1). En pratique, les catégories
     * « menace pour l'ordre public » (6°) et « procédure Dublin » (7°) donnent
     * lieu à une OQTF sans délai ; les autres catégories ouvrent en principe le
     * délai de départ volontaire (recours 30 j, L. 614-5).
     */
    private static boolean isOqtfSansDelaiParDefaut(OqtfCategorieL611 c) {
        return c == OqtfCategorieL611.CAT_6 || c == OqtfCategorieL611.CAT_7;
    }

    private static String procedureParallele(OqtfCategorieL611 c) {
        return switch (c) {
            case CAT_6 -> PROCEDURE_IRTF_L612_6;
            case CAT_7 -> PROCEDURE_DUBLIN_F_IM_22;
            default -> null;
        };
    }

    private static void validateInputs(OqtfCategorieL611 categorie,
                                       LocalDate dateNotificationOqtf,
                                       String motifOqtf) {
        if (categorie == null) {
            throw new IllegalArgumentException(
                    "categorieL611 inconnue — valeurs attendues : CAT_1 à CAT_7");
        }
        if (dateNotificationOqtf == null) {
            throw new IllegalArgumentException("dateNotificationOqtf est requise");
        }
        if (dateNotificationOqtf.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("dateNotificationOqtf ne peut pas être dans le futur");
        }
        if (motifOqtf != null && motifOqtf.length() > MOTIF_OQTF_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "motifOqtf ne peut pas dépasser " + MOTIF_OQTF_MAX_LENGTH + " caractères");
        }
    }
}
