package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * SF-FA-19-05 : calculateur d'éligibilité au recours JAF en cas de désaccord
 * parental sur l'exercice de l'autorité parentale conjointe (art. 373-2-10 Cciv).
 *
 * <p>Troisième morceau backend de F-FA-19. L'outil évalue la probabilité
 * d'acceptation par le JAF d'une requête fondée sur l'art. 373-2-10 (saisine
 * directe et simple par requête) en cas de désaccord parental persistant sur
 * la scolarité, la santé, la religion, les loisirs/sports, les choix
 * éducatifs ou le déménagement.
 *
 * <p>Scoring 0-100 :
 * <ul>
 *   <li>domaineDesaccord reconnu (≠ AUTRE) : +20</li>
 *   <li>intensiteDesaccord MAJEUR : +30 ; MOYEN : +15 ; MINEUR : +5</li>
 *   <li>tentativesMediation ≥ 2 : +25 ; 1 (hors AUCUNE) : +10 ; 0 ou AUCUNE : 0</li>
 *   <li>interetSuperieurInvoque : +15</li>
 *   <li>urgence : +10</li>
 * </ul>
 *
 * <p>Verdicts : ELEVEE (≥ 70), MOYENNE (40-69), FAIBLE (&lt; 40).
 *
 * <p>Recommandations dérivées :
 * <ul>
 *   <li>mediationPrealableRecommandee = vrai si aucune tentative ou AUCUNE
 *       seul (art. 373-2-10 al. 2 — médiation judiciaire ordonnée par le JAF)</li>
 *   <li>auditionEnfantsRecommandee = vrai si au moins un enfant ≥ 13 ans
 *       (art. 388-1 Cciv)</li>
 *   <li>expertisePsyRecommandee = vrai si intensité MAJEUR et expertise non
 *       déjà réalisée</li>
 *   <li>delaiTraitementJoursPrevisionnel = 30 si urgence (référé), 90 sinon</li>
 * </ul>
 *
 * <p>Outil single-country FR. L'équivalent BE (art. 374 §1er Code civil BE)
 * a une procédure distincte non couverte ici.
 */
public final class DesaccordsParentauxCalculator {

    public static final int SEUIL_ELEVEE = 70;
    public static final int SEUIL_MOYENNE = 40;

    public static final int POIDS_DOMAINE_RECONNU = 20;
    public static final int POIDS_INTENSITE_MAJEUR = 30;
    public static final int POIDS_INTENSITE_MOYEN = 15;
    public static final int POIDS_INTENSITE_MINEUR = 5;
    public static final int POIDS_MEDIATION_PLUSIEURS = 25;
    public static final int POIDS_MEDIATION_UNE = 10;
    public static final int POIDS_INTERET_SUPERIEUR = 15;
    public static final int POIDS_URGENCE = 10;

    public static final int AGE_AUDITION_OBLIGATOIRE = 13;

    public static final int DELAI_NOMINAL_JOURS = 90;
    public static final int DELAI_REFERE_JOURS = 30;

    public static final Set<String> DOMAINES = new LinkedHashSet<>(List.of(
            "SCOLARITE", "SANTE", "RELIGION", "LOISIRS_SPORTS",
            "CHOIX_EDUCATIFS", "DEMENAGEMENT", "AUTRE"));

    public static final Set<String> INTENSITES = new LinkedHashSet<>(List.of(
            "MAJEUR", "MOYEN", "MINEUR"));

    public static final Set<String> TENTATIVES = new LinkedHashSet<>(List.of(
            "MEDIATION_FAMILIALE", "MEDIATION_JUDICIAIRE",
            "DISCUSSIONS_DIRECTES", "THERAPIE_FAMILIALE", "AUCUNE"));

    private static final String BASE_JURIDIQUE =
            "Art. 373-2-10 Cciv + jurisprudence Cass. 1ère civ.";

    private DesaccordsParentauxCalculator() {
    }

    public static DesaccordsParentauxResult compute(String domaineDesaccord,
                                                    String intensiteDesaccord,
                                                    List<String> tentativesMediation,
                                                    List<Integer> ageEnfantsConcernes,
                                                    boolean interetSuperieurInvoque,
                                                    boolean expertiseDejaRealisee,
                                                    boolean urgence,
                                                    LocalDate dateRequete) {
        validateDomaine(domaineDesaccord);
        validateIntensite(intensiteDesaccord);
        List<String> tentatives = validateTentatives(tentativesMediation);
        List<Integer> enfants = validateAgeEnfants(ageEnfantsConcernes);
        if (dateRequete == null) {
            throw new IllegalArgumentException("dateRequete est requise");
        }

        int score = computeScore(domaineDesaccord, intensiteDesaccord, tentatives,
                interetSuperieurInvoque, urgence);
        String verdict = verdict(score);

        boolean mediation = tentatives.isEmpty()
                || (tentatives.size() == 1 && tentatives.contains("AUCUNE"));
        boolean audition = enfants.stream().anyMatch(a -> a >= AGE_AUDITION_OBLIGATOIRE);
        boolean expertisePsy = "MAJEUR".equals(intensiteDesaccord) && !expertiseDejaRealisee;
        int delai = urgence ? DELAI_REFERE_JOURS : DELAI_NOMINAL_JOURS;

        String formule = buildFormule(score, verdict, domaineDesaccord,
                intensiteDesaccord, tentatives, interetSuperieurInvoque, urgence);
        List<String> messages = buildMessages(score, verdict, domaineDesaccord,
                intensiteDesaccord, tentatives, mediation, audition, expertisePsy,
                enfants, urgence);

        return new DesaccordsParentauxResult(
                domaineDesaccord,
                intensiteDesaccord,
                tentatives,
                enfants,
                interetSuperieurInvoque,
                expertiseDejaRealisee,
                urgence,
                dateRequete,
                score,
                verdict,
                mediation,
                audition,
                expertisePsy,
                delai,
                BASE_JURIDIQUE,
                formule,
                messages
        );
    }

    private static int computeScore(String domaine, String intensite,
                                    List<String> tentatives,
                                    boolean interetSuperieur,
                                    boolean urgence) {
        int s = 0;
        if (!"AUTRE".equals(domaine)) {
            s += POIDS_DOMAINE_RECONNU;
        }
        s += switch (intensite) {
            case "MAJEUR" -> POIDS_INTENSITE_MAJEUR;
            case "MOYEN" -> POIDS_INTENSITE_MOYEN;
            case "MINEUR" -> POIDS_INTENSITE_MINEUR;
            default -> 0;
        };
        // tentatives effectives : exclure "AUCUNE"
        long effectives = tentatives.stream().filter(t -> !"AUCUNE".equals(t)).count();
        if (effectives >= 2) {
            s += POIDS_MEDIATION_PLUSIEURS;
        } else if (effectives == 1) {
            s += POIDS_MEDIATION_UNE;
        }
        if (interetSuperieur) {
            s += POIDS_INTERET_SUPERIEUR;
        }
        if (urgence) {
            s += POIDS_URGENCE;
        }
        return Math.min(100, s);
    }

    private static String verdict(int score) {
        if (score >= SEUIL_ELEVEE) return "ELEVEE";
        if (score >= SEUIL_MOYENNE) return "MOYENNE";
        return "FAIBLE";
    }

    private static String buildFormule(int score,
                                       String verdict,
                                       String domaine,
                                       String intensite,
                                       List<String> tentatives,
                                       boolean interetSuperieur,
                                       boolean urgence) {
        StringBuilder sb = new StringBuilder();
        long effectives = tentatives.stream().filter(t -> !"AUCUNE".equals(t)).count();
        sb.append("Domaine ").append(domaine)
                .append(" + intensité ").append(intensite)
                .append(" + ").append(effectives).append(" tentative(s) médiation");
        if (interetSuperieur) {
            sb.append(" + intérêt supérieur invoqué");
        }
        if (urgence) {
            sb.append(" + urgence (référé)");
        }
        sb.append(" = score ").append(score).append(" (").append(verdict).append(").");
        return sb.toString();
    }

    private static List<String> buildMessages(int score,
                                              String verdict,
                                              String domaine,
                                              String intensite,
                                              List<String> tentatives,
                                              boolean mediation,
                                              boolean audition,
                                              boolean expertisePsy,
                                              List<Integer> enfants,
                                              boolean urgence) {
        List<String> msg = new ArrayList<>();
        msg.add("JAF saisi par requête simple (art. 373-2-10 Cciv) : "
                + "le juge tranche en fonction de l'intérêt supérieur de l'enfant "
                + "(art. 373-2-6) après avoir entendu chaque parent.");

        if (urgence) {
            msg.add("Procédure de référé : le JAF statue dans un délai indicatif "
                    + "de quelques semaines. L'urgence doit être caractérisée "
                    + "(décision à prendre avant date butoir : rentrée scolaire, "
                    + "intervention médicale, etc.).");
        } else {
            msg.add("Procédure au fond : délai prévisionnel ~90 jours. "
                    + "Le JAF peut renvoyer en médiation préalable (art. 373-2-10 al. 2).");
        }

        if (mediation) {
            msg.add("Médiation préalable recommandée : le JAF peut enjoindre les "
                    + "parties de rencontrer un médiateur familial (art. 373-2-10 al. 2). "
                    + "Une tentative de médiation préalable renforce significativement "
                    + "le dossier.");
        }

        if (audition) {
            msg.add("Audition enfants ≥ 13 ans (art. 388-1 Cciv) : le mineur capable "
                    + "de discernement doit être informé de son droit à être entendu, "
                    + "le refus du juge doit être motivé. Particulièrement déterminant "
                    + "sur SCOLARITE, RELIGION et CHOIX_EDUCATIFS.");
        } else if (!enfants.isEmpty()) {
            msg.add("Audition des enfants : facultative pour les enfants de moins "
                    + "de 13 ans, mais le juge peut l'ordonner d'office (art. 388-1 Cciv) "
                    + "si le discernement est suffisant.");
        }

        if (expertisePsy) {
            msg.add("Expertise psychologique recommandée : intensité MAJEUR et "
                    + "aucune expertise n'a encore été réalisée. La jurisprudence "
                    + "valorise les éléments objectifs sur l'impact réel du désaccord "
                    + "sur l'enfant.");
        }

        if ("DEMENAGEMENT".equals(domaine)) {
            msg.add("Déménagement : le parent qui souhaite changer la résidence de "
                    + "l'enfant doit en informer l'autre parent en temps utile et "
                    + "préalable (art. 373-2 al. 3 Cciv). Un déménagement modifiant "
                    + "les modalités d'exercice peut justifier une saisine combinée.");
        }

        if ("SANTE".equals(domaine)) {
            msg.add("Santé : la jurisprudence distingue les actes usuels (un seul "
                    + "parent suffit) des actes non usuels (vaccins facultatifs, "
                    + "opérations programmées, suivi psy long terme) qui requièrent "
                    + "l'accord des deux parents.");
        }

        if ("RELIGION".equals(domaine)) {
            msg.add("Religion : choix imposant une marque durable (baptême, "
                    + "circoncision rituelle, scolarisation confessionnelle) "
                    + "= acte non usuel. La jurisprudence privilégie le statu quo "
                    + "en cas de désaccord persistant sans intérêt prouvé.");
        }

        if ("ELEVEE".equals(verdict)) {
            msg.add("Probabilité élevée d'acceptation : préparer la requête "
                    + "(saisine directe sur formulaire CERFA ou requête écrite) "
                    + "avec exposé du désaccord, pièces sur les tentatives de "
                    + "médiation et propositions concrètes pour chaque alternative.");
        } else if ("FAIBLE".equals(verdict)) {
            msg.add("Probabilité faible : envisager une médiation familiale "
                    + "préalable (art. 373-2-10 al. 2) ou consolider l'argumentation "
                    + "sur l'intérêt de l'enfant avant saisine.");
        }

        return msg;
    }

    private static void validateDomaine(String domaine) {
        if (domaine == null || domaine.isBlank()) {
            throw new IllegalArgumentException("domaineDesaccord est requis");
        }
        if (!DOMAINES.contains(domaine)) {
            throw new IllegalArgumentException("domaineDesaccord invalide : "
                    + domaine + ". Valeurs : " + DOMAINES);
        }
    }

    private static void validateIntensite(String intensite) {
        if (intensite == null || intensite.isBlank()) {
            throw new IllegalArgumentException("intensiteDesaccord est requis");
        }
        if (!INTENSITES.contains(intensite)) {
            throw new IllegalArgumentException("intensiteDesaccord invalide : "
                    + intensite + ". Valeurs : " + INTENSITES);
        }
    }

    private static List<String> validateTentatives(List<String> tentatives) {
        if (tentatives == null || tentatives.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> dedup = new LinkedHashSet<>();
        for (String t : tentatives) {
            if (t == null || t.isBlank()) continue;
            String trimmed = t.trim();
            if (!TENTATIVES.contains(trimmed)) {
                throw new IllegalArgumentException("tentativesMediation contient une "
                        + "valeur invalide : " + trimmed + ". Valeurs : " + TENTATIVES);
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
                throw new IllegalArgumentException(
                        "ageEnfantsConcernes : valeur null interdite");
            }
            if (a < 0 || a > 17) {
                throw new IllegalArgumentException(
                        "ageEnfantsConcernes : âge invalide (mineur attendu, 0-17) : " + a);
            }
            out.add(a);
        }
        return out;
    }
}
