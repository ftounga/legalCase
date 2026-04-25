package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * SF-FA-19-03 : calculateur d'acceptabilité d'un changement de résidence
 * de l'enfant suite au déménagement d'un parent (art. 373-2 al. 3 Cciv).
 *
 * <p>2ème SF backend de F-FA-19 (1ère = SF-FA-19-01 autorité parentale exercice).
 * L'outil évalue la probabilité d'acceptation par le JAF d'un changement de
 * résidence en fonction du respect de l'obligation d'information préalable,
 * de la distance, de la raison du déménagement, du consentement de l'autre
 * parent et de l'impact sur la scolarité.
 *
 * <p>Scoring 0-100 :
 * <ul>
 *   <li>information préalable + délai ≥ 30j (obligation art. 373-2) : +30</li>
 *   <li>raison reconnue (≠ AUTRE) : +20</li>
 *   <li>distance &lt; 100 km : +20 ; 100-300 km : +10 ; &gt; 300 km : 0</li>
 *   <li>consentement de l'autre parent : +20</li>
 *   <li>pas d'impact scolarité majeur : +10</li>
 * </ul>
 *
 * <p>Verdicts : ELEVEE (≥ 70), MOYENNE (40-69), FAIBLE (&lt; 40).
 *
 * <p>Indicateurs dérivés :
 * <ul>
 *   <li>{@code expertisePsyEnfantRecommandee} = true si distance &gt; 300 km
 *       OU si tout enfant a un âge &gt; 10 ans</li>
 *   <li>{@code delaiPreavisLegalOk} =
 *       informePrealablement &amp;&amp; delaiInformationJours ≥ 30</li>
 *   <li>{@code obligationInformationRespectee} = idem</li>
 * </ul>
 *
 * <p>Outil single-country FR. Le droit belge a sa propre logique
 * (art. 374 Code civil BE / loi du 18 juillet 2006) — non couvert ici.
 */
public final class ChangementResidenceCalculator {

    public static final int SEUIL_ELEVEE = 70;
    public static final int SEUIL_MOYENNE = 40;

    public static final int POIDS_INFO_PREALABLE = 30;
    public static final int POIDS_RAISON_RECONNUE = 20;
    public static final int POIDS_DISTANCE_PROCHE = 20;
    public static final int POIDS_DISTANCE_MOYENNE = 10;
    public static final int POIDS_CONSENTEMENT = 20;
    public static final int POIDS_PAS_IMPACT_SCOLARITE = 10;

    public static final int SEUIL_DISTANCE_PROCHE = 100;
    public static final int SEUIL_DISTANCE_MOYENNE = 300;
    public static final int DELAI_PREAVIS_MINIMUM_JOURS = 30;
    public static final int AGE_ENFANT_SENSIBLE = 10;

    public static final Set<String> RAISONS = new LinkedHashSet<>(List.of(
            "TRAVAIL", "FAMILLE", "LOGEMENT", "RAPPROCHEMENT_FAMILIAL", "AUTRE"));

    public static final Set<String> MODES_RESIDENCE = new LinkedHashSet<>(List.of(
            "ALTERNEE", "EXCLUSIVE_DEMANDEUR", "EXCLUSIVE_DEFENDEUR"));

    private static final String BASE_JURIDIQUE =
            "Art. 373-2 al. 3 Cciv + jurisprudence Cass. 1ère civ.";

    private ChangementResidenceCalculator() {
    }

    public static ChangementResidenceResult compute(LocalDate dateChangementPrevu,
                                                    int distanceKm,
                                                    String raisonChangement,
                                                    boolean consentementAutreParent,
                                                    boolean informePrealablement,
                                                    int delaiInformationJours,
                                                    String modeResidenceActuel,
                                                    List<Integer> ageEnfants,
                                                    boolean scolariteImpactee,
                                                    boolean modificationDvhDemandee) {
        if (dateChangementPrevu == null) {
            throw new IllegalArgumentException("dateChangementPrevu est requise");
        }
        if (distanceKm < 0) {
            throw new IllegalArgumentException(
                    "distanceKm doit être >= 0 (reçu : " + distanceKm + ")");
        }
        validateRaison(raisonChangement);
        validateMode(modeResidenceActuel);
        if (delaiInformationJours < 0) {
            throw new IllegalArgumentException(
                    "delaiInformationJours doit être >= 0 (reçu : "
                            + delaiInformationJours + ")");
        }
        List<Integer> enfants = validateAgeEnfants(ageEnfants);

        boolean obligationOk = informePrealablement
                && delaiInformationJours >= DELAI_PREAVIS_MINIMUM_JOURS;
        boolean delaiPreavisOk = obligationOk;
        boolean expertisePsyRecommandee = distanceKm > SEUIL_DISTANCE_MOYENNE
                || (!enfants.isEmpty()
                        && enfants.stream().allMatch(a -> a > AGE_ENFANT_SENSIBLE));

        int score = computeScore(obligationOk, raisonChangement, distanceKm,
                consentementAutreParent, scolariteImpactee);
        String verdict = verdict(score);

        String formule = buildFormule(score, distanceKm, raisonChangement,
                modeResidenceActuel, scolariteImpactee, verdict);
        List<String> messages = buildMessages(score, verdict, obligationOk,
                delaiInformationJours, distanceKm, raisonChangement,
                modeResidenceActuel, scolariteImpactee, modificationDvhDemandee,
                expertisePsyRecommandee, consentementAutreParent);

        return new ChangementResidenceResult(
                dateChangementPrevu,
                distanceKm,
                raisonChangement,
                consentementAutreParent,
                informePrealablement,
                delaiInformationJours,
                modeResidenceActuel,
                enfants,
                scolariteImpactee,
                modificationDvhDemandee,
                score,
                verdict,
                obligationOk,
                expertisePsyRecommandee,
                delaiPreavisOk,
                BASE_JURIDIQUE,
                formule,
                messages
        );
    }

    private static int computeScore(boolean obligationOk,
                                    String raison,
                                    int distanceKm,
                                    boolean consentement,
                                    boolean scolariteImpactee) {
        int s = 0;
        if (obligationOk) {
            s += POIDS_INFO_PREALABLE;
        }
        if (!"AUTRE".equals(raison)) {
            s += POIDS_RAISON_RECONNUE;
        }
        if (distanceKm < SEUIL_DISTANCE_PROCHE) {
            s += POIDS_DISTANCE_PROCHE;
        } else if (distanceKm <= SEUIL_DISTANCE_MOYENNE) {
            s += POIDS_DISTANCE_MOYENNE;
        }
        if (consentement) {
            s += POIDS_CONSENTEMENT;
        }
        if (!scolariteImpactee) {
            s += POIDS_PAS_IMPACT_SCOLARITE;
        }
        return Math.min(100, s);
    }

    private static String verdict(int score) {
        if (score >= SEUIL_ELEVEE) return "ELEVEE";
        if (score >= SEUIL_MOYENNE) return "MOYENNE";
        return "FAIBLE";
    }

    private static String buildFormule(int score,
                                       int distanceKm,
                                       String raison,
                                       String modeResidence,
                                       boolean scolariteImpactee,
                                       String verdict) {
        StringBuilder sb = new StringBuilder();
        sb.append("Distance ").append(distanceKm).append(" km");
        sb.append(" + raison ").append(raison);
        sb.append(scolariteImpactee
                ? " + impact scolarité"
                : " + pas d'impact scolarité majeur");
        sb.append(" + résidence ").append(modeResidence);
        sb.append(" → score ").append(score);
        sb.append(" = acceptabilité ").append(verdict).append(".");
        return sb.toString();
    }

    private static List<String> buildMessages(int score,
                                              String verdict,
                                              boolean obligationOk,
                                              int delaiInformationJours,
                                              int distanceKm,
                                              String raison,
                                              String modeResidence,
                                              boolean scolariteImpactee,
                                              boolean modificationDvhDemandee,
                                              boolean expertisePsyRecommandee,
                                              boolean consentement) {
        List<String> msg = new ArrayList<>();
        msg.add("Le JAF apprécie souverainement l'intérêt de l'enfant "
                + "(art. 373-2-6 Cciv) ; la charge de la preuve incombe au "
                + "parent qui déménage.");
        msg.add("Notification 30j avant déménagement obligatoire "
                + "(art. 373-2 al. 3 Cciv) : tout changement de résidence d'un "
                + "parent qui modifie les modalités d'exercice de l'autorité "
                + "parentale doit être préalablement et en temps utile communiqué "
                + "à l'autre parent. Le délai usuel admis par la jurisprudence "
                + "est d'au moins un mois (30 jours).");

        if (!obligationOk) {
            msg.add("Obligation d'information préalable non respectée : "
                    + "délai de " + delaiInformationJours + " jours "
                    + "(seuil minimum 30 jours). Le déménagement non notifié "
                    + "expose à une saisine en référé du JAF par l'autre "
                    + "parent et au paiement de dommages-intérêts.");
        }

        if (distanceKm > SEUIL_DISTANCE_MOYENNE) {
            msg.add("Distance > 300 km : impact majeur sur le maintien des "
                    + "liens (DVH, école, soins). Une expertise psychologique "
                    + "de l'enfant est fortement recommandée pour étayer la "
                    + "demande.");
        } else if (distanceKm >= SEUIL_DISTANCE_PROCHE) {
            msg.add("Distance " + distanceKm + " km : impact modéré sur le DVH ; "
                    + "préparer une nouvelle organisation des trajets et "
                    + "weekends élargis.");
        }

        if (expertisePsyRecommandee && distanceKm <= SEUIL_DISTANCE_MOYENNE) {
            msg.add("Expertise psychologique recommandée : enfants de plus de "
                    + AGE_ENFANT_SENSIBLE + " ans — leur audition par le juge "
                    + "(art. 388-1 Cciv) ou un examen par un expert peut être "
                    + "déterminant.");
        }

        if (scolariteImpactee) {
            msg.add("Scolarité impactée : prévoir un calendrier d'inscription "
                    + "dans le nouvel établissement, justifier la continuité "
                    + "pédagogique et anticiper la motivation du JAF "
                    + "(art. 373-2-6 al. 2 Cciv).");
        }

        if (!consentement) {
            msg.add("Pas de consentement de l'autre parent : saisir le JAF "
                    + "par requête (art. 373-2-13 Cciv) ; en l'absence d'accord "
                    + "le déménagement reste légalement possible mais ouvre "
                    + "droit à modification du DVH ou de la résidence par le juge.");
        }

        if (modificationDvhDemandee) {
            msg.add("Modification du DVH demandée : la requête au JAF doit "
                    + "préciser le nouveau régime souhaité (week-ends élargis, "
                    + "vacances scolaires, prise en charge des trajets "
                    + "art. 373-2-9 Cciv).");
        }

        if ("AUTRE".equals(raison)) {
            msg.add("Raison du déménagement non rattachée à un motif reconnu : "
                    + "le JAF peut considérer le changement comme insuffisamment "
                    + "justifié — caractériser précisément l'intérêt de l'enfant "
                    + "ou un motif sérieux du parent (santé, opportunité "
                    + "professionnelle, rapprochement familial).");
        }

        if ("ALTERNEE".equals(modeResidence) && distanceKm >= SEUIL_DISTANCE_PROCHE) {
            msg.add("Résidence alternée + distance ≥ 100 km : la jurisprudence "
                    + "considère la résidence alternée comme difficilement "
                    + "compatible au-delà de 50-80 km. Une révision du mode de "
                    + "résidence vers un exclusif avec DVH élargi est "
                    + "probable (art. 373-2-9 Cciv).");
        }

        if ("ELEVEE".equals(verdict)) {
            msg.add("Probabilité élevée d'acceptation : préparer la requête "
                    + "(saisine du JAF par requête conjointe ou unilatérale) "
                    + "avec justificatifs (contrat de travail, attestation "
                    + "logement, certificat de scolarité du nouvel établissement).");
        } else if ("FAIBLE".equals(verdict)) {
            msg.add("Probabilité faible d'acceptation : envisager une médiation "
                    + "familiale (art. 373-2-10 Cciv), renforcer la motivation "
                    + "(intérêt supérieur de l'enfant) ou différer le projet "
                    + "pour mieux respecter le délai d'information.");
        }

        return msg;
    }

    private static void validateRaison(String raison) {
        if (raison == null || raison.isBlank()) {
            throw new IllegalArgumentException("raisonChangement est requise");
        }
        if (!RAISONS.contains(raison)) {
            throw new IllegalArgumentException("raisonChangement invalide : "
                    + raison + ". Valeurs : " + RAISONS);
        }
    }

    private static void validateMode(String mode) {
        if (mode == null || mode.isBlank()) {
            throw new IllegalArgumentException("modeResidenceActuel est requis");
        }
        if (!MODES_RESIDENCE.contains(mode)) {
            throw new IllegalArgumentException("modeResidenceActuel invalide : "
                    + mode + ". Valeurs : " + MODES_RESIDENCE);
        }
    }

    private static List<Integer> validateAgeEnfants(List<Integer> ages) {
        if (ages == null || ages.isEmpty()) {
            return List.of();
        }
        List<Integer> out = new ArrayList<>(ages.size());
        for (Integer a : ages) {
            if (a == null) {
                throw new IllegalArgumentException("ageEnfants : valeur null interdite");
            }
            if (a < 0 || a > 17) {
                throw new IllegalArgumentException(
                        "ageEnfants : âge invalide (mineur attendu, 0-17) : " + a);
            }
            out.add(a);
        }
        return out;
    }
}
