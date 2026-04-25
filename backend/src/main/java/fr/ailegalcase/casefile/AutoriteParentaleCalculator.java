package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * SF-FA-19-01 : calculateur d'éligibilité au changement de régime d'exercice
 * de l'autorité parentale (art. 372-373 Cciv).
 *
 * <p>Premier morceau de F-FA-19. L'outil évalue la probabilité d'acceptation
 * par le JAF d'une demande de passage de l'exercice conjoint à un exercice
 * exclusif (mère, père) ou à une délégation à un tiers (art. 377 Cciv).
 *
 * <p>Scoring 0-100 :
 * <ul>
 *   <li>motifChangement reconnu (≠ AUTRE) : +25</li>
 *   <li>≥ 2 preuves produites : +25</li>
 *   <li>danger caractérisé : +25</li>
 *   <li>interférence dans la vie de l'enfant : +15</li>
 *   <li>consentement de l'autre parent : +10</li>
 * </ul>
 *
 * <p>Verdicts : ELEVEE (≥ 70), MOYENNE (40-69), FAIBLE (&lt; 40).
 *
 * <p>Recommandations dérivées :
 * <ul>
 *   <li>expertise psychiatrique recommandée si motif sensible
 *       (mise en danger, aliénation parentale)</li>
 *   <li>audition des enfants (art. 388-1 Cciv) recommandée si tout enfant
 *       a un âge ≥ 13 ans (sécurité : si un enfant est plus jeune, l'audition
 *       d'un seul enfant adolescent suffit, mais le calculator privilégie
 *       le cas où la procédure ne peut être évitée pour aucun enfant)</li>
 * </ul>
 *
 * <p>Outil single-country FR. Le droit belge a sa propre logique
 * (loi 13 avril 1995, art. 373 Code civil BE) — non couvert ici.
 */
public final class AutoriteParentaleCalculator {

    public static final int SEUIL_ELEVEE = 70;
    public static final int SEUIL_MOYENNE = 40;

    public static final int POIDS_MOTIF_RECONNU = 25;
    public static final int POIDS_PREUVES_VARIEES = 25;
    public static final int POIDS_DANGER = 25;
    public static final int POIDS_INTERFERENCE = 15;
    public static final int POIDS_CONSENTEMENT = 10;

    public static final int AGE_AUDITION_OBLIGATOIRE = 13;

    public static final Set<String> REGIMES = new LinkedHashSet<>(List.of(
            "CONJOINT", "EXCLUSIF_MERE", "EXCLUSIF_PERE", "DELEGATION_TIERS"));

    public static final Set<String> MOTIFS = new LinkedHashSet<>(List.of(
            "MISE_EN_DANGER", "DESINTERET_PROLONGE", "IMPOSSIBILITE_FAIT",
            "CONDAMNATION_PENALE", "ALIENATION_PARENTALE", "AUTRE"));

    public static final Set<String> PREUVES = new LinkedHashSet<>(List.of(
            "JUGEMENT_PENAL", "MAINS_COURANTES", "CERTIFICAT_MEDICAL",
            "TEMOIGNAGES_ECOLE", "TEMOIGNAGES_PROCHES", "RAPPORT_AED",
            "EXPERTISE_PSYCHIATRIQUE", "AUTRE"));

    public static final Set<String> MOTIFS_SENSIBLES = Set.of(
            "MISE_EN_DANGER", "ALIENATION_PARENTALE");

    private static final String BASE_JURIDIQUE =
            "Art. 372-373 Cciv + jurisprudence Cass. 1ère civ.";

    private AutoriteParentaleCalculator() {
    }

    public static AutoriteParentaleResult compute(String regimeExerciceActuel,
                                                  String regimeExerciceDemande,
                                                  String motifChangement,
                                                  boolean dangerCaracterise,
                                                  List<String> preuvesProduites,
                                                  List<Integer> ageEnfants,
                                                  boolean consentementAutreParent,
                                                  boolean interferenceVieEnfant,
                                                  LocalDate dateRequete) {
        validateRegime("regimeExerciceActuel", regimeExerciceActuel);
        validateRegime("regimeExerciceDemande", regimeExerciceDemande);
        if (regimeExerciceActuel.equals(regimeExerciceDemande)) {
            throw new IllegalArgumentException(
                    "regimeExerciceActuel et regimeExerciceDemande doivent différer "
                            + "(aucun changement à évaluer)");
        }
        validateMotif(motifChangement);
        List<String> preuves = validatePreuves(preuvesProduites);
        List<Integer> enfants = validateAgeEnfants(ageEnfants);
        if (dateRequete == null) {
            throw new IllegalArgumentException("dateRequete est requise");
        }

        int score = computeScore(motifChangement, preuves, dangerCaracterise,
                interferenceVieEnfant, consentementAutreParent);
        String verdict = verdict(score);
        boolean expertise = MOTIFS_SENSIBLES.contains(motifChangement);
        boolean auditionRecommandee = !enfants.isEmpty()
                && enfants.stream().allMatch(a -> a >= AGE_AUDITION_OBLIGATOIRE);

        String formule = buildFormule(score, motifChangement, preuves,
                dangerCaracterise, interferenceVieEnfant, consentementAutreParent);
        List<String> messages = buildMessages(score, verdict, motifChangement,
                preuves, dangerCaracterise, expertise, auditionRecommandee, enfants,
                regimeExerciceActuel, regimeExerciceDemande);

        return new AutoriteParentaleResult(
                regimeExerciceActuel,
                regimeExerciceDemande,
                motifChangement,
                dangerCaracterise,
                preuves,
                enfants,
                consentementAutreParent,
                interferenceVieEnfant,
                dateRequete,
                score,
                verdict,
                expertise,
                auditionRecommandee,
                BASE_JURIDIQUE,
                formule,
                messages
        );
    }

    private static int computeScore(String motif,
                                    List<String> preuves,
                                    boolean danger,
                                    boolean interference,
                                    boolean consentement) {
        int s = 0;
        if (!"AUTRE".equals(motif)) {
            s += POIDS_MOTIF_RECONNU;
        }
        if (preuves.size() >= 2) {
            s += POIDS_PREUVES_VARIEES;
        }
        if (danger) {
            s += POIDS_DANGER;
        }
        if (interference) {
            s += POIDS_INTERFERENCE;
        }
        if (consentement) {
            s += POIDS_CONSENTEMENT;
        }
        return Math.min(100, s);
    }

    private static String verdict(int score) {
        if (score >= SEUIL_ELEVEE) return "ELEVEE";
        if (score >= SEUIL_MOYENNE) return "MOYENNE";
        return "FAIBLE";
    }

    private static String buildFormule(int score,
                                       String motif,
                                       List<String> preuves,
                                       boolean danger,
                                       boolean interference,
                                       boolean consentement) {
        StringBuilder sb = new StringBuilder();
        sb.append("Score ").append(score).append("/100 = ");
        List<String> parts = new ArrayList<>();
        if (!"AUTRE".equals(motif)) {
            parts.add("motif " + motif + " reconnu (+" + POIDS_MOTIF_RECONNU + ")");
        }
        if (preuves.size() >= 2) {
            parts.add(preuves.size() + " preuves produites (+" + POIDS_PREUVES_VARIEES + ")");
        }
        if (danger) {
            parts.add("danger caractérisé (+" + POIDS_DANGER + ")");
        }
        if (interference) {
            parts.add("interférence vie enfant (+" + POIDS_INTERFERENCE + ")");
        }
        if (consentement) {
            parts.add("consentement autre parent (+" + POIDS_CONSENTEMENT + ")");
        }
        if (parts.isEmpty()) {
            sb.append("aucun critère retenu");
        } else {
            sb.append(String.join(" + ", parts));
        }
        sb.append(".");
        return sb.toString();
    }

    private static List<String> buildMessages(int score,
                                              String verdict,
                                              String motif,
                                              List<String> preuves,
                                              boolean danger,
                                              boolean expertise,
                                              boolean auditionRecommandee,
                                              List<Integer> enfants,
                                              String regimeActuel,
                                              String regimeDemande) {
        List<String> msg = new ArrayList<>();
        msg.add("Charge de la preuve sur le demandeur (art. 9 CPC). Le JAF "
                + "apprécie souverainement l'intérêt de l'enfant (art. 373-2-6 Cciv).");
        msg.add("Régime actuel : " + regimeActuel + " → demandé : " + regimeDemande
                + ". Le principe est l'exercice conjoint (art. 372 Cciv) ; "
                + "le passage à l'exclusif suppose des motifs caractérisés.");

        if (auditionRecommandee) {
            msg.add("Audition enfants ≥ 13 ans obligatoire (art. 388-1 Cciv) : "
                    + "le mineur doit être informé de son droit à être entendu, "
                    + "le refus du juge doit être motivé.");
        } else if (!enfants.isEmpty()) {
            msg.add("Audition des enfants : facultative pour les enfants de moins "
                    + "de 13 ans, mais le juge peut l'ordonner d'office "
                    + "(art. 388-1 Cciv) si le discernement est suffisant.");
        }

        if (expertise) {
            msg.add("Expertise psychiatrique / psychologique recommandée : motif "
                    + motif + " — la jurisprudence exige des éléments objectifs "
                    + "(rapport circonstancié, certificats médicaux concordants).");
        }

        if (preuves.size() < 2) {
            msg.add("Preuves insuffisantes : la jurisprudence exige des éléments "
                    + "convergents et variés (jugement pénal, certificats médicaux, "
                    + "témoignages école/proches, rapport AED). Compléter le dossier.");
        }

        if (danger && !"MISE_EN_DANGER".equals(motif)) {
            msg.add("Danger caractérisé sans motif de mise en danger explicite : "
                    + "envisager une requalification du motif ou une saisine "
                    + "complémentaire du juge des enfants (art. 375 Cciv).");
        }

        if ("DELEGATION_TIERS".equals(regimeDemande)) {
            msg.add("Délégation d'autorité parentale (art. 377 Cciv) : suppose des "
                    + "circonstances exigeant cette mesure et l'accord du tiers "
                    + "(membre de la famille, proche digne de confiance, service "
                    + "départemental de l'aide sociale à l'enfance).");
        }

        if ("ELEVEE".equals(verdict)) {
            msg.add("Probabilité élevée d'acceptation : préparer la requête "
                    + "(saisine du JAF par requête conjointe ou unilatérale) "
                    + "avec la liste des pièces et le projet de droit de visite "
                    + "de l'autre parent (sauf danger).");
        } else if ("FAIBLE".equals(verdict)) {
            msg.add("Probabilité faible : envisager une médiation familiale "
                    + "(art. 373-2-10 Cciv) ou consolider les preuves avant saisine.");
        }

        return msg;
    }

    private static void validateRegime(String label, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " est requis");
        }
        if (!REGIMES.contains(value)) {
            throw new IllegalArgumentException(label + " invalide : " + value
                    + ". Valeurs : " + REGIMES);
        }
    }

    private static void validateMotif(String motif) {
        if (motif == null || motif.isBlank()) {
            throw new IllegalArgumentException("motifChangement est requis");
        }
        if (!MOTIFS.contains(motif)) {
            throw new IllegalArgumentException("motifChangement invalide : " + motif
                    + ". Valeurs : " + MOTIFS);
        }
    }

    private static List<String> validatePreuves(List<String> preuves) {
        if (preuves == null || preuves.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> dedup = new LinkedHashSet<>();
        for (String p : preuves) {
            if (p == null || p.isBlank()) continue;
            String trimmed = p.trim();
            if (!PREUVES.contains(trimmed)) {
                throw new IllegalArgumentException("preuvesProduites contient une "
                        + "valeur invalide : " + trimmed + ". Valeurs : " + PREUVES);
            }
            dedup.add(trimmed);
        }
        return new ArrayList<>(dedup);
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
