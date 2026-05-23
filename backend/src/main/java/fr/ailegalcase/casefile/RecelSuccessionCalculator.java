package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * SF-216-21 : calculateur statique pour le recel successoral en droit
 * français (art. 778 Cciv + Cass. 1ère civ., 14/11/2012, n° 11-20.582).
 *
 * <p>Le calculateur :</p>
 * <ul>
 *   <li>qualifie le recel à partir du type invoqué (dissimulation de bien,
 *       de donation, destruction de testament, recel de créance) et de la
 *       nature de la preuve disponible ;</li>
 *   <li>évalue la sanction civile (art. 778 al. 2 — privation sur le bien
 *       recelé + obligation de rapporter le bien sans émolument) ;</li>
 *   <li>évalue la suffisance probatoire (charge sur les cohéritiers
 *       demandeurs ; faisceau d'indices admis, Cass. 1ère civ.,
 *       14/11/2012) ;</li>
 *   <li>évalue le délai d'action (prescription quinquennale depuis
 *       l'ouverture de la succession ou la découverte du recel) ;</li>
 *   <li>signale l'articulation pénale (art. 441-8 CP — destruction de
 *       titre) en cas de destruction de testament.</li>
 * </ul>
 *
 * <p>Gate country : uniquement FRANCE. En Belgique, la matière est régie
 * par les art. 4.45 et s. CC BE (réforme 2021) avec des conditions
 * distinctes (outil F-FA-BE-RECEL futur, hors périmètre F-216).</p>
 */
public final class RecelSuccessionCalculator {

    static final String BASE_JURIDIQUE =
            "art. 778 Cciv (recel successoral — dissimulation intentionnelle "
                    + "+ sanction civile) + Cass. 1ère civ., 14/11/2012, "
                    + "n° 11-20.582 (faisceau d'indices) + art. 441-8 CP "
                    + "(volet pénal destruction de titre, signalé)";

    /** Types de recel relevant strictement de l'art. 778 Cciv. */
    static final Set<TypeRecelEnum> TYPES_RECEL_SUCCESSORAL = EnumSet.of(
            TypeRecelEnum.DISSIMULATION_BIEN,
            TypeRecelEnum.DISSIMULATION_DONATION,
            TypeRecelEnum.DESTRUCTION_TESTAMENT,
            TypeRecelEnum.RECEL_CREANCE,
            TypeRecelEnum.AUTRE
    );

    /** Preuves considérées comme suffisantes pour caractériser le recel. */
    static final Set<PreuveRecelEnum> PREUVES_SUFFISANTES = EnumSet.of(
            PreuveRecelEnum.AVEUX,
            PreuveRecelEnum.DOCUMENT,
            PreuveRecelEnum.EXPERTISE
    );

    /** Preuves probables (admises par jurisprudence — faisceau / témoignage). */
    static final Set<PreuveRecelEnum> PREUVES_PROBABLES = EnumSet.of(
            PreuveRecelEnum.TEMOIGNAGE,
            PreuveRecelEnum.FAISCEAU_INDICES
    );

    /** Délai légal de prescription (5 ans — art. 2224 Cciv applicable au recel). */
    static final int DELAI_ACTION_ANNEES = 5;

    private RecelSuccessionCalculator() {}

    /**
     * Évalue les conditions du recel successoral et émet le verdict + les
     * alertes correspondantes.
     *
     * @param req     requête validée
     * @param country pays du workspace ("FRANCE" attendu)
     * @param today   date du jour (injectable pour les tests)
     * @return résultat du calcul
     * @throws IllegalArgumentException si {@code country != FRANCE}.
     */
    public static RecelSuccessionResult compute(
            RecelSuccessionRequest req, String country, LocalDate today) {

        if (!"FRANCE".equals(country)) {
            throw new IllegalArgumentException(
                    "Outil F-FA-RECEL-SUCCESSION applicable uniquement "
                            + "en France (art. 778 Cciv).");
        }

        List<String> messages = new ArrayList<>();
        List<String> alertes = new ArrayList<>();

        TypeRecelEnum type = req.typeRecel();
        PreuveRecelEnum preuve = req.preuveRecel();
        ReceleurQualiteEnum qualite = req.receleurQualite();
        LocalDate dateOuverture = req.dateOuvertureSuccession();
        boolean actionIntentee = Boolean.TRUE.equals(req.actionIntentee());
        Integer valeur = req.bienCeleValeurEur();

        // 1. Évaluation du délai 5 ans.
        boolean delaiForclos = false;
        String delaiAction;
        if (dateOuverture != null) {
            Period ecoule = Period.between(dateOuverture, today);
            int anneesEcoulees = ecoule.getYears();
            delaiForclos = anneesEcoulees >= DELAI_ACTION_ANNEES;
            delaiAction = "Prescription quinquennale (art. 2224 Cciv) : "
                    + "5 ans depuis l'ouverture de la succession ("
                    + dateOuverture + ") ou la découverte du recel — "
                    + "années écoulées : " + anneesEcoulees + ". "
                    + "Le point de départ peut être reporté à la "
                    + "découverte du recel (jurisprudence constante).";
        } else {
            delaiAction = "Prescription quinquennale (art. 2224 Cciv) : "
                    + "5 ans depuis l'ouverture de la succession ou la "
                    + "découverte du recel — date d'ouverture non renseignée.";
        }

        // 2. Volet pénal signalé pour destruction de testament.
        boolean voletPenalSignale = type == TypeRecelEnum.DESTRUCTION_TESTAMENT;
        if (voletPenalSignale) {
            messages.add("Destruction de testament : volet pénal "
                    + "complémentaire signalé (art. 441-8 CP — destruction "
                    + "de titre, peine de 3 ans d'emprisonnement et 45 000 € "
                    + "d'amende). Articulation civile (sanction art. 778 al. 2) "
                    + "+ pénale possible. La preuve de la destruction peut être "
                    + "rapportée par tous moyens (Cass. 1ère civ.).");
        }

        // 3. Qualité du receleur exclue (tiers complice).
        if (qualite == ReceleurQualiteEnum.TIERS_COMPLICITE) {
            alertes.add("Qualité « tiers complice » : la sanction civile "
                    + "art. 778 al. 2 (privation + rapport sans émolument) ne "
                    + "s'applique qu'aux héritiers, légataires et donataires. "
                    + "Pour un tiers complice, voie pénale (recel de chose "
                    + "art. 321-1 CP) ou action délictuelle (art. 1240 Cciv) "
                    + "à examiner — hors scope de cet outil.");
            return new RecelSuccessionResult(
                    "HORS_PERIMETRE",
                    "QUALITE_RECELEUR_EXCLUE",
                    "Inapplicable : sanction civile art. 778 réservée aux "
                            + "héritiers, légataires et donataires.",
                    "Sans objet : la qualité de tiers exclut la sanction "
                            + "successorale. Voie pénale (art. 321-1 CP) ou "
                            + "action en responsabilité délictuelle à étudier.",
                    delaiAction,
                    delaiForclos,
                    voletPenalSignale,
                    BASE_JURIDIQUE,
                    messages,
                    alertes);
        }

        // 4. Type de recel absent ou non caractéristique.
        if (type == null || !TYPES_RECEL_SUCCESSORAL.contains(type)) {
            alertes.add("Type de recel non caractéristique ou non renseigné. "
                    + "Le recel successoral (art. 778 Cciv) suppose une "
                    + "dissimulation intentionnelle d'effets, d'une donation, "
                    + "d'une créance ou la destruction d'un titre testamentaire.");
            return new RecelSuccessionResult(
                    "PAS_DE_RECEL",
                    "PAS_DE_RECEL",
                    "Sans objet : aucun type de recel caractérisable.",
                    "Sans objet.",
                    delaiAction,
                    delaiForclos,
                    voletPenalSignale,
                    BASE_JURIDIQUE,
                    messages,
                    alertes);
        }

        // 5. Délai forclos → bloque.
        if (delaiForclos) {
            alertes.add("Délai 5 ans (art. 2224 Cciv) expiré depuis "
                    + "l'ouverture de la succession (" + dateOuverture + "). "
                    + "L'action en recel est en principe prescrite. "
                    + "Vérifier si le point de départ peut être reporté à "
                    + "la date de la découverte du recel (jurisprudence "
                    + "constante — exception au délai).");
            return new RecelSuccessionResult(
                    detectQualif(preuve),
                    "DELAI_FORCLOS",
                    "Privation de la part successorale sur le bien recelé "
                            + "+ obligation de rapporter sans émolument "
                            + "(art. 778 al. 2 Cciv) — sanction théorique, "
                            + "action en principe forclose.",
                    libellePreuve(preuve),
                    delaiAction,
                    true,
                    voletPenalSignale,
                    BASE_JURIDIQUE,
                    messages,
                    alertes);
        }

        // 6. Évaluation par suffisance de la preuve.
        String qualification;
        String verdict;
        if (preuve == null || preuve == PreuveRecelEnum.AUCUNE) {
            alertes.add("Aucune preuve disponible : l'action en recel "
                    + "(art. 778 Cciv) suppose que les cohéritiers demandeurs "
                    + "rapportent la preuve de l'élément matériel "
                    + "(dissimulation) ET de l'élément intentionnel "
                    + "(volonté de fraude). À défaut, l'action est vouée à "
                    + "l'échec.");
            qualification = "INSUFFISANT";
            verdict = "PREUVE_INSUFFISANTE";
        } else if (PREUVES_SUFFISANTES.contains(preuve)) {
            qualification = "CARACTERISE";
            verdict = "RECEL_CARACTERISE";
            messages.add("Preuve directe disponible (" + preuve.name()
                    + ") : le recel peut être caractérisé devant le TJ. "
                    + "Sanction civile art. 778 al. 2 applicable : privation "
                    + "de toute part sur le bien recelé + obligation de "
                    + "rapporter le bien sans pouvoir prétendre à aucune "
                    + "portion.");
        } else if (PREUVES_PROBABLES.contains(preuve)) {
            qualification = "PROBABLE";
            verdict = "RECEL_PROBABLE_FAISCEAU";
            messages.add("Preuve indirecte (" + preuve.name() + ") : "
                    + "la Cass. 1ère civ., 14/11/2012, admet le faisceau "
                    + "d'indices et le témoignage pour caractériser le recel. "
                    + "Le juge du fond apprécie souverainement la "
                    + "concordance des indices.");
            if (preuve == PreuveRecelEnum.FAISCEAU_INDICES) {
                messages.add("Faisceau d'indices : à étayer par plusieurs "
                        + "éléments concordants (relevés bancaires non "
                        + "communiqués, déplacements suspects, déclarations "
                        + "fiscales incohérentes, opérations notariales "
                        + "tardives, etc.).");
            }
        } else {
            qualification = "INSUFFISANT";
            verdict = "PREUVE_INSUFFISANTE";
        }

        // 7. Messages spécifiques au type de recel.
        switch (type) {
            case DISSIMULATION_BIEN -> messages.add("Dissimulation de bien "
                    + "(art. 778 Cciv) : tout bien mobilier ou immobilier "
                    + "de la succession non déclaré à l'actif (compte "
                    + "bancaire occulté, bijoux, parts sociales, immobilier "
                    + "non révélé). Sanction : privation sur ce bien + "
                    + "rapport sans émolument.");
            case DISSIMULATION_DONATION -> messages.add("Dissimulation de "
                    + "donation (art. 778 + 850 Cciv) : donation reçue du "
                    + "défunt et non rapportée à la succession. La "
                    + "jurisprudence assimile ce comportement au recel "
                    + "successoral lorsqu'il est intentionnel.");
            case RECEL_CREANCE -> messages.add("Recel de créance : "
                    + "dissimulation d'une créance dont le défunt était "
                    + "titulaire (prêt informel, créance fiscale, "
                    + "indemnité). Visé par la jurisprudence comme un "
                    + "recel art. 778.");
            case AUTRE -> messages.add("Type « autre » : caractériser "
                    + "précisément la dissimulation intentionnelle dans les "
                    + "écritures (élément matériel + élément intentionnel "
                    + "exigés par la Cass. 1ère civ.).");
            default -> { /* destruction testament déjà signalée plus haut */ }
        }

        // 8. Mention valeur si renseignée.
        if (valeur != null && valeur > 0) {
            messages.add("Valeur du bien / créance recelé(e) renseignée : "
                    + valeur + " €. La sanction art. 778 al. 2 porte sur "
                    + "TOUTE la part qui aurait dû revenir au receleur sur "
                    + "ce bien — calcul par référence à la masse partageable.");
        }

        // 9. Mention action déjà intentée.
        if (!actionIntentee) {
            alertes.add("Aucune action en recel documentée comme engagée. "
                    + "L'action doit être formée devant le TJ par les "
                    + "cohéritiers — délai 5 ans depuis l'ouverture ou la "
                    + "découverte du recel (art. 2224 Cciv).");
        }

        return new RecelSuccessionResult(
                qualification,
                verdict,
                "Privation de la part successorale sur le bien recelé + "
                        + "obligation de rapporter le bien à la masse SANS "
                        + "pouvoir prétendre à aucune portion sur ce bien "
                        + "(art. 778 al. 2 Cciv).",
                libellePreuve(preuve),
                delaiAction,
                false,
                voletPenalSignale,
                BASE_JURIDIQUE,
                messages,
                alertes);
    }

    private static String detectQualif(PreuveRecelEnum p) {
        if (p == null || p == PreuveRecelEnum.AUCUNE) return "INSUFFISANT";
        if (PREUVES_SUFFISANTES.contains(p)) return "CARACTERISE";
        if (PREUVES_PROBABLES.contains(p)) return "PROBABLE";
        return "INSUFFISANT";
    }

    private static String libellePreuve(PreuveRecelEnum preuve) {
        if (preuve == null || preuve == PreuveRecelEnum.AUCUNE) {
            return "Charge probatoire sur les cohéritiers demandeurs : "
                    + "élément matériel (dissimulation) + élément "
                    + "intentionnel (volonté de fraude). Sans pièce ni "
                    + "indice, action vouée à l'échec.";
        }
        return "Charge probatoire sur les cohéritiers demandeurs "
                + "(art. 1353 Cciv) : élément matériel + élément intentionnel. "
                + "Cass. 1ère civ., 14/11/2012, n° 11-20.582 — faisceau "
                + "d'indices admis. Preuve invoquée : " + preuve.name() + ".";
    }
}
