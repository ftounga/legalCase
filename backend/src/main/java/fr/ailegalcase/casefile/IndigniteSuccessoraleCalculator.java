package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * SF-216-19 : calculateur statique pour l'indignité successorale en droit
 * français (art. 726-729-1 Cciv + Loi n°2001-1135 du 3/12/2001 + Loi
 * n°2022-1617 du 23/12/2022).
 *
 * <p>Le calculateur :</p>
 * <ul>
 *   <li>distingue l'indignité <strong>de plein droit</strong> (art. 726 —
 *       meurtre ou tentative de meurtre du défunt après condamnation
 *       pénale définitive) de l'indignité <strong>judiciaire</strong>
 *       (art. 727 — autres causes : témoignage faux, obstruction au
 *       testament, violences graves, non-signalement homicide ;
 *       extension violences intrafamiliales par loi 2022-1617) ;</li>
 *   <li>neutralise l'indignité de plein droit en présence d'un pardon
 *       testamentaire explicite (art. 728 Cciv) ;</li>
 *   <li>évalue la possibilité de représentation des descendants de
 *       l'indigne (art. 729-1 Cciv — possible pour l'indignité
 *       judiciaire, refusée pour l'indignité de plein droit) ;</li>
 *   <li>évalue le délai d'action 5 ans (art. 729 Cciv).</li>
 * </ul>
 *
 * <p>Gate country : uniquement FRANCE. En Belgique, la matière est régie
 * par les art. 4.6 à 4.10 CC BE (réforme 2021) avec des conditions
 * distinctes (outil F-FA-BE-INDIGNITE futur, hors périmètre F-216).</p>
 */
public final class IndigniteSuccessoraleCalculator {

    static final String BASE_JURIDIQUE =
            "art. 726-729-1 Cciv + Loi n°2001-1135 du 3/12/2001 "
                    + "(extension causes d'indignité) + Loi n°2022-1617 du "
                    + "23/12/2022 (violences intrafamiliales)";

    /**
     * Motifs caractérisant l'indignité <strong>de plein droit</strong>
     * (art. 726 Cciv — meurtre ou tentative de meurtre du défunt après
     * condamnation pénale définitive).
     */
    static final Set<MotifIndigniteEnum> MOTIFS_PLEIN_DROIT = EnumSet.of(
            MotifIndigniteEnum.MEURTRE,
            MotifIndigniteEnum.MEURTRE_TENTE
    );

    /**
     * Motifs caractérisant l'indignité <strong>judiciaire</strong>
     * (art. 727 Cciv — autres causes nécessitant un jugement du TJ).
     */
    static final Set<MotifIndigniteEnum> MOTIFS_JUDICIAIRE = EnumSet.of(
            MotifIndigniteEnum.TEMOIGNAGE_FAUX,
            MotifIndigniteEnum.OBSTRUCTION_TESTAMENT,
            MotifIndigniteEnum.NON_SIGNALEMENT_HOMICIDE,
            MotifIndigniteEnum.VIOLENCES_GRAVES,
            MotifIndigniteEnum.AUTRE
    );

    /** Délai légal d'action en indignité judiciaire (art. 729 Cciv). */
    static final int DELAI_ACTION_ANNEES = 5;

    private IndigniteSuccessoraleCalculator() {}

    /**
     * Évalue les conditions de l'indignité successorale et émet le verdict
     * + les alertes correspondantes.
     *
     * @param req     requête validée (gates pays/domaine vérifiés par le service)
     * @param country pays du workspace ("FRANCE" attendu)
     * @param today   date du jour (injectable pour les tests)
     * @return résultat du calcul
     * @throws IllegalArgumentException si {@code country != FRANCE}.
     */
    public static IndigniteSuccessoraleResult compute(
            IndigniteSuccessoraleRequest req, String country, LocalDate today) {

        if (!"FRANCE".equals(country)) {
            throw new IllegalArgumentException(
                    "Outil F-FA-INDIGNITE-SUCCESSORALE applicable uniquement "
                            + "en France (art. 726 Cciv).");
        }

        List<String> messages = new ArrayList<>();
        List<String> alertes = new ArrayList<>();

        MotifIndigniteEnum motif = req.motifIndignite();
        boolean condamne = Boolean.TRUE.equals(req.condamnationPrononcee());
        boolean indigniteDemandee = Boolean.TRUE.equals(req.indigniteJudiciaireDemandee());
        boolean pardon = Boolean.TRUE.equals(req.pardonTestamentaireDetecte());
        LocalDate dateOuverture = req.dateOuvertureSuccession();

        // 1. Évaluation du délai 5 ans (art. 729 Cciv).
        boolean delaiForclos = false;
        String delaiAction;
        if (dateOuverture != null) {
            Period ecoule = Period.between(dateOuverture, today);
            int anneesEcoulees = ecoule.getYears();
            delaiForclos = anneesEcoulees >= DELAI_ACTION_ANNEES;
            delaiAction = "Délai 5 ans (art. 729 Cciv) à compter de l'ouverture "
                    + "de la succession (" + dateOuverture + ") ou de la "
                    + "condamnation pénale définitive — années écoulées : "
                    + anneesEcoulees + ".";
        } else {
            delaiAction = "Délai 5 ans (art. 729 Cciv) à compter de l'ouverture "
                    + "de la succession ou de la condamnation pénale définitive — "
                    + "date d'ouverture non renseignée.";
        }

        // 2. Branche indignité de plein droit (art. 726 Cciv).
        if (motif != null && MOTIFS_PLEIN_DROIT.contains(motif)) {
            // 2.a. Pardon testamentaire (art. 728 Cciv) → neutralise.
            if (pardon) {
                messages.add("Pardon testamentaire détecté (art. 728 Cciv) : "
                        + "le défunt a expressément pardonné l'héritier indigne "
                        + "dans son testament. L'indignité de plein droit "
                        + "(art. 726) est neutralisée. Vérifier que le pardon "
                        + "est exprès et postérieur aux faits.");
                return new IndigniteSuccessoraleResult(
                        "PARDONNEE",
                        "PARDON_NEUTRALISANT",
                        true,
                        true,
                        delaiAction,
                        delaiForclos,
                        "Aucune exclusion : l'héritier conserve ses droits "
                                + "successoraux malgré le motif d'indignité (pardon "
                                + "testamentaire art. 728 Cciv).",
                        BASE_JURIDIQUE,
                        messages,
                        alertes);
            }
            // 2.b. Pas de condamnation prononcée → bloque (ipso jure exige
            //      condamnation pénale définitive).
            if (!condamne) {
                alertes.add("Indignité de plein droit (art. 726 Cciv) "
                        + "envisagée pour meurtre / tentative de meurtre du "
                        + "défunt, mais aucune condamnation pénale définitive "
                        + "n'est documentée. Le mécanisme ipso jure exige une "
                        + "condamnation définitive : à défaut, l'indignité ne "
                        + "peut être que judiciaire (art. 727) et reste à "
                        + "demander devant le TJ.");
                return new IndigniteSuccessoraleResult(
                        "AUCUNE",
                        "CONDAMNATION_REQUISE",
                        false,
                        true,
                        delaiAction,
                        delaiForclos,
                        "Sans condamnation pénale définitive, l'héritier "
                                + "conserve ses droits successoraux. Voie possible : "
                                + "indignité judiciaire (art. 727) — délai 5 ans.",
                        BASE_JURIDIQUE,
                        messages,
                        alertes);
            }
            // 2.c. Indignité de plein droit caractérisée.
            messages.add("Indignité de plein droit caractérisée (art. 726 "
                    + "Cciv) : meurtre ou tentative de meurtre du défunt avec "
                    + "condamnation pénale définitive. L'héritier est écarté "
                    + "ipso jure, sans jugement civil supplémentaire.");
            alertes.add("Représentation des descendants exclue (art. 729-1 "
                    + "Cciv) : en cas d'indignité de plein droit, les enfants "
                    + "de l'indigne ne peuvent PAS représenter leur parent. "
                    + "La part de l'indigne accroît directement les autres "
                    + "héritiers de même rang.");
            return new IndigniteSuccessoraleResult(
                    "PLEIN_DROIT",
                    "INDIGNITE_PLEIN_DROIT",
                    false,
                    false,
                    delaiAction,
                    delaiForclos,
                    "L'indigne est écarté de la succession ipso jure "
                            + "(art. 726). Sa part est répartie entre les autres "
                            + "cohéritiers de même rang — la représentation par "
                            + "les descendants de l'indigne est exclue (art. 729-1).",
                    BASE_JURIDIQUE,
                    messages,
                    alertes);
        }

        // 3. Branche indignité judiciaire (art. 727 Cciv).
        if (motif != null && MOTIFS_JUDICIAIRE.contains(motif)) {
            // 3.a. Délai forclos → bloque.
            if (delaiForclos) {
                alertes.add("Délai 5 ans (art. 729 Cciv) expiré depuis "
                        + "l'ouverture de la succession (" + dateOuverture
                        + "). L'action en indignité judiciaire est forclose. "
                        + "Vérifier si le point de départ peut être reporté à "
                        + "la date de la condamnation pénale.");
                return new IndigniteSuccessoraleResult(
                        "JUDICIAIRE",
                        "DELAI_FORCLOS",
                        false,
                        true,
                        delaiAction,
                        true,
                        "Action forclose — l'héritier conserve ses droits "
                                + "successoraux faute d'action engagée dans les 5 ans "
                                + "(art. 729 Cciv).",
                        BASE_JURIDIQUE,
                        messages,
                        alertes);
            }
            // 3.b. Pardon testamentaire (art. 728 Cciv) → neutralise.
            if (pardon) {
                messages.add("Pardon testamentaire détecté (art. 728 Cciv) : "
                        + "le défunt a expressément pardonné l'héritier indigne "
                        + "dans son testament. L'indignité judiciaire (art. 727) "
                        + "est neutralisée. Vérifier que le pardon est exprès "
                        + "et postérieur aux faits.");
                return new IndigniteSuccessoraleResult(
                        "PARDONNEE",
                        "PARDON_NEUTRALISANT",
                        true,
                        true,
                        delaiAction,
                        false,
                        "Aucune exclusion : l'héritier conserve ses droits "
                                + "successoraux malgré le motif d'indignité (pardon "
                                + "testamentaire art. 728 Cciv).",
                        BASE_JURIDIQUE,
                        messages,
                        alertes);
            }
            // 3.c. Indignité judiciaire envisageable — demande à former.
            if (motif == MotifIndigniteEnum.VIOLENCES_GRAVES) {
                messages.add("Cause de violences graves (art. 727 Cciv) : "
                        + "extension par la Loi n°2022-1617 du 23/12/2022 — "
                        + "couvre désormais les violences intrafamiliales "
                        + "ayant entraîné la mort sans intention de la "
                        + "donner, tortures et actes de barbarie commis "
                        + "contre le défunt.");
            } else if (motif == MotifIndigniteEnum.TEMOIGNAGE_FAUX) {
                messages.add("Cause de témoignage faux (art. 727 al. 2 "
                        + "Cciv) : l'héritier doit avoir porté un faux "
                        + "témoignage en justice contre le défunt — preuve "
                        + "par jugement pénal ou décision civile motivée.");
            } else if (motif == MotifIndigniteEnum.OBSTRUCTION_TESTAMENT) {
                messages.add("Cause d'obstruction au testament (art. 727 "
                        + "al. 4 Cciv) : l'héritier doit avoir détruit, "
                        + "recelé ou modifié sciemment le testament du "
                        + "défunt — preuve par tous moyens devant le TJ.");
            } else if (motif == MotifIndigniteEnum.NON_SIGNALEMENT_HOMICIDE) {
                messages.add("Cause de non-signalement d'homicide (art. 727 "
                        + "al. 3 Cciv) : l'héritier doit avoir eu connaissance "
                        + "d'un homicide commis sur le défunt et s'être "
                        + "abstenu de le signaler aux autorités.");
            }
            if (!indigniteDemandee) {
                alertes.add("Indignité judiciaire (art. 727 Cciv) "
                        + "envisageable mais aucune demande n'est documentée "
                        + "comme engagée. L'action doit être formée devant "
                        + "le TJ par les autres héritiers ou le ministère "
                        + "public, dans le délai 5 ans (art. 729).");
            }
            messages.add("Représentation des descendants possible (art. 729-1 "
                    + "Cciv) : en cas d'indignité judiciaire, les enfants de "
                    + "l'indigne peuvent représenter leur parent à la "
                    + "succession (à charge pour eux de rapporter les dons "
                    + "reçus par l'indigne).");
            return new IndigniteSuccessoraleResult(
                    "JUDICIAIRE",
                    "INDIGNITE_JUDICIAIRE_POSSIBLE",
                    false,
                    true,
                    delaiAction,
                    false,
                    "Si l'indignité judiciaire est prononcée par le TJ, "
                            + "l'indigne est écarté de la succession. Ses descendants "
                            + "peuvent toutefois venir par représentation (art. 729-1) — "
                            + "ils ne sont pas écartés par l'indignité de leur parent.",
                    BASE_JURIDIQUE,
                    messages,
                    alertes);
        }

        // 4. Motif absent ou non caractéristique.
        alertes.add("Motif d'indignité non caractéristique ou non renseigné. "
                + "L'indignité successorale (art. 726-727 Cciv) suppose un "
                + "motif limitativement énuméré (meurtre, tentative, violences "
                + "graves, témoignage faux, obstruction au testament, "
                + "non-signalement d'homicide).");
        return new IndigniteSuccessoraleResult(
                "AUCUNE",
                "PAS_D_INDIGNITE",
                false,
                true,
                delaiAction,
                delaiForclos,
                "Aucune exclusion : l'héritier conserve ses droits "
                        + "successoraux faute de motif caractérisé.",
                BASE_JURIDIQUE,
                messages,
                alertes);
    }
}
