package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-220-05 : analyseur de la validité d'une mesure (envisagée ou prononcée) de
 * déchéance de la nationalité française (Code civil art. 25 et 25-1,
 * F-IM-51-decheance-nationalite-fr). Outil single-country FR.
 *
 * <p><b>Objet</b> : apprécier la régularité d'une mesure de déchéance au regard
 * de ses deux conditions structurantes — l'<b>interdiction d'apatridie</b> (la
 * déchéance ne peut viser qu'un binational, Cciv 25) et le <b>délai</b> entre
 * acquisition de la nationalité et faits reprochés (Cciv 25-1) — puis calculer
 * le délai de recours pour excès de pouvoir devant le Conseil d'État (2 mois) si
 * la mesure a été prononcée par décret.</p>
 *
 * <p>Distinct de F-IM-13 (acquisition de la nationalité) : l'objet est ici la
 * <b>perte par déchéance</b>. Distinct de F-IM-39/40 (recours refus
 * naturalisation TJ / TA Nantes).</p>
 *
 * <p>Toutes ces appréciations sont annotées « à vérifier par avocat » : l'outil
 * aiguille sur la régularité de la mesure et les voies de recours, il ne se
 * substitue pas à l'appréciation du juge ni au contrôle de proportionnalité.</p>
 *
 * <p>Source juridique (à vérifier par avocat) :
 * <ul>
 *   <li>Code civil art. 25 (motifs de déchéance, interdiction d'apatridie)</li>
 *   <li>Code civil art. 25-1 (délais entre acquisition et faits)</li>
 *   <li>Recours pour excès de pouvoir devant le Conseil d'État (délai 2 mois)</li>
 * </ul>
 * </p>
 */
public final class DecheanceNationaliteAnalyzer {

    // Verdicts de validité.
    public static final String CONDITIONS_REUNIES = "CONDITIONS_REUNIES";
    public static final String MESURE_CONTESTABLE = "MESURE_CONTESTABLE";
    public static final String MESURE_IRREGULIERE = "MESURE_IRREGULIERE";
    public static final String INDETERMINE = "INDETERMINE";

    // Motifs de déchéance (Cciv 25).
    public static final String MOTIF_TERRORISME = "TERRORISME";
    public static final String MOTIF_ATTEINTE_INTERETS_NATION = "ATTEINTE_INTERETS_NATION";
    public static final String MOTIF_FRAUDE_ACQUISITION = "FRAUDE_ACQUISITION";
    public static final String MOTIF_AUTRE = "AUTRE";
    public static final Set<String> MOTIF_VALEURS = Set.of(
            MOTIF_TERRORISME, MOTIF_ATTEINTE_INTERETS_NATION, MOTIF_FRAUDE_ACQUISITION, MOTIF_AUTRE);

    // Délai de recours REP devant le Conseil d'État (jours) — à vérifier par avocat.
    public static final int DELAI_RECOURS_CE_JOURS = 60;

    /**
     * Délai indicatif (années) entre l'acquisition de la nationalité et les faits
     * reprochés (Cciv 25-1) — à vérifier par avocat. La mesure suppose des faits
     * commis dans ce délai à compter de l'acquisition.
     */
    public static final int DELAI_FAITS_ANNEES = 15;

    private static final String BASE_CCIV_25 =
            "Code civil art. 25 (motifs de déchéance, interdiction d'apatridie) — à vérifier par avocat";
    private static final String BASE_CCIV_25_1 =
            "Code civil art. 25-1 (délais entre acquisition de la nationalité et faits) — à vérifier par avocat";
    private static final String BASE_REP_CE =
            "Recours pour excès de pouvoir contre le décret devant le Conseil d'État (délai 2 mois) "
                    + "— à vérifier par avocat";

    private DecheanceNationaliteAnalyzer() {}

    /**
     * Analyse la validité d'une mesure de déchéance de nationalité.
     *
     * @param motif                     motif de déchéance (whitelist)
     * @param binational                true si la personne est binationale (nullable)
     * @param dateAcquisitionNationalite date d'acquisition de la nationalité française (nullable)
     * @param dateFaits                 date des faits reprochés (nullable)
     * @param mesurePrononcee           true si la mesure a déjà été prononcée par décret
     * @param dateDecret                date du décret de déchéance (nullable)
     * @return résultat d'analyse de validité
     */
    public static DecheanceNationaliteResult analyze(String motif,
                                                     Boolean binational,
                                                     LocalDate dateAcquisitionNationalite,
                                                     LocalDate dateFaits,
                                                     boolean mesurePrononcee,
                                                     LocalDate dateDecret) {

        List<String> manquantes = new ArrayList<>();
        List<String> voiesRecours = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        bases.add(BASE_CCIV_25);
        bases.add(BASE_CCIV_25_1);

        messages.add("Rappel : la déchéance de nationalité (Cciv 25) est une mesure exceptionnelle, "
                + "soumise à un contrôle de proportionnalité et à l'interdiction de créer un apatride. "
                + "L'analyse ci-dessous est indicative. À vérifier par avocat.");

        // Condition 1 — interdiction d'apatridie : la déchéance suppose un binational.
        boolean apatridieRisque = Boolean.FALSE.equals(binational);
        boolean binationaliteInconnue = binational == null;
        if (apatridieRisque) {
            manquantes.add("Personne non binationale : la déchéance la rendrait apatride, ce qui est "
                    + "prohibé (Cciv 25). Mesure impossible en l'état.");
            messages.add("La déchéance ne peut être prononcée si elle a pour effet de rendre la personne "
                    + "apatride : une seconde nationalité est requise. À vérifier par avocat.");
        } else if (binationaliteInconnue) {
            manquantes.add("Binationalité non établie — la condition d'absence d'apatridie (Cciv 25) "
                    + "doit être vérifiée (seconde nationalité effective).");
        }

        // Condition 2 — délai Cciv 25-1 entre acquisition de la nationalité et faits reprochés.
        boolean horsDelai = false;
        boolean delaiVerifiable = dateAcquisitionNationalite != null && dateFaits != null;
        if (delaiVerifiable) {
            long anneesEcoulees = ChronoUnit.YEARS.between(dateAcquisitionNationalite, dateFaits);
            if (dateFaits.isBefore(dateAcquisitionNationalite)) {
                manquantes.add("Faits reprochés antérieurs à l'acquisition de la nationalité : "
                        + "la déchéance suppose des faits postérieurs (Cciv 25-1).");
                horsDelai = true;
            } else if (anneesEcoulees > DELAI_FAITS_ANNEES) {
                manquantes.add("Faits commis plus de " + DELAI_FAITS_ANNEES + " ans après l'acquisition "
                        + "de la nationalité (" + anneesEcoulees + " ans) — hors du délai indicatif Cciv 25-1.");
                horsDelai = true;
            }
        } else {
            manquantes.add("Délai Cciv 25-1 non vérifiable : date d'acquisition de la nationalité et/ou "
                    + "date des faits non factualisées.");
        }

        // Motif (Cciv 25) : élément d'appréciation de la solidité de la mesure.
        if (MOTIF_AUTRE.equals(motif) || motif == null) {
            manquantes.add("Motif de déchéance non rattaché à un cas légal de l'art. 25 — base de la "
                    + "mesure à clarifier.");
        }

        // Voies de recours : calcul du délai REP si la mesure a été prononcée.
        Integer delaiRecoursJours = null;
        if (mesurePrononcee) {
            bases.add(BASE_REP_CE);
            voiesRecours.add("Recours pour excès de pouvoir contre le décret de déchéance devant le "
                    + "Conseil d'État (délai 2 mois à compter de la publication / notification).");
            delaiRecoursJours = DELAI_RECOURS_CE_JOURS;
            if (dateDecret != null) {
                LocalDate echeance = dateDecret.plusDays(DELAI_RECOURS_CE_JOURS);
                long restant = ChronoUnit.DAYS.between(LocalDate.now(), echeance);
                if (restant < 0) {
                    messages.add("Délai de recours de 2 mois apparemment expiré (décret du "
                            + dateDecret + ", échéance indicative " + echeance + "). Vérifier une éventuelle "
                            + "prorogation ou un recours gracieux. À vérifier par avocat.");
                } else {
                    messages.add("Délai de recours en cours : échéance indicative " + echeance
                            + " (environ " + restant + " jour(s) restant(s)). À vérifier par avocat.");
                }
            } else {
                messages.add("Date du décret non factualisée : le point de départ du délai de 2 mois "
                        + "(REP Conseil d'État) reste à établir. À vérifier par avocat.");
            }
        } else {
            voiesRecours.add("Mesure non encore prononcée : observations en défense devant l'administration "
                    + "(procédure contradictoire préalable au décret). À vérifier par avocat.");
        }

        // Verdict de synthèse.
        String validite;
        if (apatridieRisque || horsDelai) {
            // Apatridie ou hors délai = irrégularité structurante.
            validite = MESURE_IRREGULIERE;
            messages.add("Mesure irrégulière en l'état : une condition légale structurante (absence "
                    + "d'apatridie ou délai Cciv 25-1) n'est pas remplie. À vérifier par avocat.");
        } else if (!manquantes.isEmpty()) {
            // Conditions à factualiser ou motif fragile.
            validite = MESURE_CONTESTABLE;
            messages.add("Mesure contestable : un ou plusieurs éléments (binationalité, délai, motif) "
                    + "restent à factualiser ou présentent une fragilité exploitable en recours. "
                    + "À vérifier par avocat.");
        } else {
            validite = CONDITIONS_REUNIES;
            messages.add("Conditions légales réunies en l'état (binationalité établie, faits dans le "
                    + "délai Cciv 25-1, motif rattaché à l'art. 25). Le contrôle de proportionnalité "
                    + "reste à apprécier. À vérifier par avocat.");
        }

        return new DecheanceNationaliteResult(motif, binational, mesurePrononcee, validite,
                manquantes, voiesRecours, delaiRecoursJours, bases, messages);
    }
}
