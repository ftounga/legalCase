package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SF-FA-14-01 : calculateur de l'<b>ordonnance de protection</b> (art. 515-9 à
 * 515-13 Cciv + Loi 30/07/2020 BAR). ~30 000/an FR, en forte hausse.
 *
 * <p>Le JAF statue dans un délai indicatif de <b>6 jours</b> (art. 515-11
 * Cciv). Mesures prévues : éviction du conjoint violent, interdiction
 * d'approcher, TGD (téléphone grave danger), BAR (bracelet anti-rapprochement
 * — Loi 30/07/2020), interdiction de paraître certains lieux, fixation
 * provisoire de la résidence des enfants, obligation de soin.
 *
 * <p>Charge de la preuve : <b>vraisemblance</b> des faits (pas certitude
 * pénale). L'outil produit un score 0-100, un verdict de probabilité
 * d'octroi, la liste des mesures recommandées (intersection entre demandes
 * et contexte) et le délai indicatif.
 *
 * <p>Scoring 0-100 :
 * <ul>
 *   <li>+30 si ≥ 3 catégories de violences alléguées</li>
 *   <li>+30 si ≥ 3 types de preuves</li>
 *   <li>+20 si dangerImmediat=true</li>
 *   <li>+10 si presenceEnfants=true</li>
 *   <li>+5 si victimeFinanciairementDependante=true</li>
 *   <li>+5 si demandeurDejaProtege=false (1ère demande, signal positif)</li>
 * </ul>
 *
 * <p>Verdicts : ELEVEE (≥ 75), MOYENNE (50-74), FAIBLE (&lt; 50).
 *
 * <p>Outil <b>single-country FR</b>. L'équivalent belge (art. 1253ter CJ) est
 * une procédure juridiquement distincte (Tribunal de la famille) — couverte
 * par une SF distincte conformément à l'invariant "un outil = une situation
 * métier".
 */
public final class OrdonnanceProtectionCalculator {

    public static final int SEUIL_ELEVEE = 75;
    public static final int SEUIL_MOYENNE = 50;

    public static final int POIDS_VIOLENCES_DIVERSIFIEES = 30;
    public static final int POIDS_PREUVES_DIVERSIFIEES = 30;
    public static final int POIDS_DANGER_IMMEDIAT = 20;
    public static final int POIDS_PRESENCE_ENFANTS = 10;
    public static final int POIDS_DEPENDANCE_FINANCIERE = 5;
    public static final int POIDS_PREMIERE_DEMANDE = 5;

    public static final int SEUIL_DIVERSIFICATION_VIOLENCES = 3;
    public static final int SEUIL_DIVERSIFICATION_PREUVES = 3;

    /** Délai indicatif de traitement par le JAF (art. 515-11 Cciv). */
    public static final int DELAI_TRAITEMENT_JOURS = 6;

    static final String BASE_JURIDIQUE =
            "Art. 515-9 à 515-13 Cciv + Loi 30/07/2020 (BAR)";

    private OrdonnanceProtectionCalculator() {
    }

    public static OrdonnanceProtectionResult compute(LocalDate dateRequete,
                                                     List<String> violencesAlleguees,
                                                     List<String> preuvesViolences,
                                                     boolean dangerImmediat,
                                                     boolean presenceEnfants,
                                                     List<Integer> ageEnfants,
                                                     boolean logementCommun,
                                                     boolean victimeFinanciairementDependante,
                                                     boolean demandeurDejaProtege,
                                                     List<String> demandeMesures,
                                                     boolean decEnvisage) {
        List<String> violences = validateAndNormalizeViolences(violencesAlleguees);
        List<String> preuves = validateAndNormalizePreuves(preuvesViolences);
        List<String> mesures = validateAndNormalizeMesures(demandeMesures);
        List<Integer> ages = ageEnfants != null
                ? new ArrayList<>(ageEnfants)
                : Collections.emptyList();

        if (violences.isEmpty()) {
            throw new IllegalArgumentException("violencesAlleguees ne peut être vide");
        }

        int score = computeScore(violences.size(), preuves.size(), dangerImmediat,
                presenceEnfants, victimeFinanciairementDependante, demandeurDejaProtege);
        String verdict = verdictProbabilite(score);

        List<String> mesuresRecommandees = computeMesuresRecommandees(
                mesures, dangerImmediat, presenceEnfants, logementCommun, decEnvisage);

        String formule = buildFormule(score, verdict, mesuresRecommandees,
                dangerImmediat, presenceEnfants);

        List<String> messages = buildMessages(score, verdict, mesuresRecommandees,
                dangerImmediat, presenceEnfants, victimeFinanciairementDependante,
                demandeurDejaProtege, preuves.size(), decEnvisage);

        return new OrdonnanceProtectionResult(
                dateRequete,
                violences,
                preuves,
                dangerImmediat,
                presenceEnfants,
                ages,
                logementCommun,
                victimeFinanciairementDependante,
                demandeurDejaProtege,
                mesures,
                decEnvisage,
                score,
                verdict,
                mesuresRecommandees,
                DELAI_TRAITEMENT_JOURS,
                BASE_JURIDIQUE,
                formule,
                messages);
    }

    // =========================================================================
    // Validation & normalisation
    // =========================================================================

    private static List<String> validateAndNormalizeViolences(List<String> raw) {
        Set<String> valides = enumNames(OrdonnanceProtectionViolenceType.class);
        return validateEnumList(raw, valides, "Violence inconnue");
    }

    private static List<String> validateAndNormalizePreuves(List<String> raw) {
        Set<String> valides = enumNames(OrdonnanceProtectionPreuveType.class);
        return validateEnumList(raw, valides, "Preuve inconnue");
    }

    private static List<String> validateAndNormalizeMesures(List<String> raw) {
        Set<String> valides = enumNames(OrdonnanceProtectionMesureType.class);
        return validateEnumList(raw, valides, "Mesure inconnue");
    }

    private static <E extends Enum<E>> Set<String> enumNames(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static List<String> validateEnumList(List<String> raw,
                                                 Set<String> valides,
                                                 String errPrefix) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> deduplicated = new LinkedHashSet<>();
        for (String code : raw) {
            if (code == null || code.isBlank()) {
                continue;
            }
            String trimmed = code.trim();
            if (!valides.contains(trimmed)) {
                throw new IllegalArgumentException(errPrefix + " : " + trimmed
                        + ". Valeurs : " + valides);
            }
            deduplicated.add(trimmed);
        }
        return new ArrayList<>(deduplicated);
    }

    // =========================================================================
    // Score & verdict
    // =========================================================================

    private static int computeScore(int nbViolences,
                                    int nbPreuves,
                                    boolean dangerImmediat,
                                    boolean presenceEnfants,
                                    boolean dependance,
                                    boolean dejaProtege) {
        int score = 0;
        if (nbViolences >= SEUIL_DIVERSIFICATION_VIOLENCES) {
            score += POIDS_VIOLENCES_DIVERSIFIEES;
        }
        if (nbPreuves >= SEUIL_DIVERSIFICATION_PREUVES) {
            score += POIDS_PREUVES_DIVERSIFIEES;
        }
        if (dangerImmediat) {
            score += POIDS_DANGER_IMMEDIAT;
        }
        if (presenceEnfants) {
            score += POIDS_PRESENCE_ENFANTS;
        }
        if (dependance) {
            score += POIDS_DEPENDANCE_FINANCIERE;
        }
        if (!dejaProtege) {
            score += POIDS_PREMIERE_DEMANDE;
        }
        return Math.max(0, Math.min(100, score));
    }

    private static String verdictProbabilite(int score) {
        if (score >= SEUIL_ELEVEE) return "ELEVEE";
        if (score >= SEUIL_MOYENNE) return "MOYENNE";
        return "FAIBLE";
    }

    // =========================================================================
    // Mesures recommandées (intersection contextuelle)
    // =========================================================================

    private static List<String> computeMesuresRecommandees(List<String> demandeMesures,
                                                           boolean dangerImmediat,
                                                           boolean presenceEnfants,
                                                           boolean logementCommun,
                                                           boolean decEnvisage) {
        List<String> out = new ArrayList<>();
        for (String code : demandeMesures) {
            if (mesureAppropriee(code, dangerImmediat, presenceEnfants, logementCommun, decEnvisage)) {
                out.add(code);
            }
        }
        // SF-222-05 : le DEC se pilote par un toggle dédié (decEnvisage), pas par
        // la liste demandeMesures. Même condition juridique que le BAR : danger
        // immédiat caractérisé. Ajouté sans doublon.
        if (decEnvisage && dangerImmediat && !out.contains("DEC")) {
            out.add("DEC");
        }
        return out;
    }

    private static boolean mesureAppropriee(String code,
                                            boolean dangerImmediat,
                                            boolean presenceEnfants,
                                            boolean logementCommun,
                                            boolean decEnvisage) {
        OrdonnanceProtectionMesureType type = OrdonnanceProtectionMesureType.valueOf(code);
        return switch (type) {
            case EVICTION_CONJOINT -> logementCommun;
            case TGD, BAR -> dangerImmediat;
            case DEC -> dangerImmediat && decEnvisage;
            case RESIDENCE_ENFANTS -> presenceEnfants;
            case INTERDICTION_APPROCHER, INTERDICTION_PARAITRE, OBLIGATION_SOIN -> true;
        };
    }

    // =========================================================================
    // Formule & messages
    // =========================================================================

    private static String buildFormule(int score,
                                       String verdict,
                                       List<String> mesuresRecommandees,
                                       boolean dangerImmediat,
                                       boolean presenceEnfants) {
        StringBuilder sb = new StringBuilder();
        sb.append("Score ").append(score).append(" = vraisemblance des faits ")
                .append(verdictHumain(verdict))
                .append(" (").append(seuilLabel(verdict)).append("). ");

        if (mesuresRecommandees.isEmpty()) {
            sb.append("Aucune mesure recommandée — préciser la demande.");
            return sb.toString();
        }
        sb.append("Mesures ").append(String.join(" + ", mesuresRecommandees))
                .append(" justifiées par ");
        List<String> facteurs = new ArrayList<>();
        if (dangerImmediat) facteurs.add("danger immédiat");
        if (presenceEnfants) facteurs.add("enfants présents");
        if (facteurs.isEmpty()) facteurs.add("contexte de violences alléguées");
        sb.append(String.join(" + ", facteurs)).append(".");
        return sb.toString();
    }

    private static String verdictHumain(String verdict) {
        return switch (verdict) {
            case "ELEVEE" -> "élevée";
            case "MOYENNE" -> "moyenne";
            default -> "faible";
        };
    }

    private static String seuilLabel(String verdict) {
        return switch (verdict) {
            case "ELEVEE" -> "≥ 75";
            case "MOYENNE" -> "50-74";
            default -> "< 50";
        };
    }

    private static List<String> buildMessages(int score,
                                              String verdict,
                                              List<String> mesuresRecommandees,
                                              boolean dangerImmediat,
                                              boolean presenceEnfants,
                                              boolean dependance,
                                              boolean dejaProtege,
                                              int nbPreuves,
                                              boolean decEnvisage) {
        List<String> messages = new ArrayList<>();
        messages.add("Audience à demander en urgence — délai indicatif "
                + DELAI_TRAITEMENT_JOURS + " jours (art. 515-11 Cciv)");
        messages.add("Charge de la preuve : vraisemblance des faits, pas certitude pénale (art. 515-9 Cciv).");

        if (mesuresRecommandees.contains("TGD")) {
            messages.add("TGD (Téléphone Grave Danger) attribué au plus tard 24h après ordonnance (art. 41-3-1 CPP)");
        }
        if (mesuresRecommandees.contains("BAR")) {
            messages.add("BAR autorisé depuis Loi 30/07/2020 si danger immédiat caractérisé");
        }
        // SF-222-05 : DEC (Dispositif Électronique de Contrôle — suivi
        // électronique du respect de l'interdiction de rapprochement).
        if (mesuresRecommandees.contains("DEC")) {
            messages.add("DEC (Dispositif Électronique de Contrôle) recommandé : "
                    + "suivi électronique du respect de l'interdiction de rapprochement, "
                    + "en complément ou en alternative du BAR selon le besoin de surveillance du contact (art. 515-11 Cciv).");
        } else if (decEnvisage && !dangerImmediat) {
            messages.add("DEC envisagé : comme le BAR, le suivi électronique du contact suppose un "
                    + "danger immédiat caractérisé — à défaut, privilégier interdictions de paraître / d'approcher.");
        }
        if (mesuresRecommandees.contains("EVICTION_CONJOINT")) {
            messages.add("Éviction du conjoint violent du domicile commun — mesure phare de l'art. 515-11 Cciv.");
        }
        if (presenceEnfants) {
            messages.add("Présence d'enfants : fixation provisoire de la résidence et droit de visite peuvent être ordonnés (art. 515-11 al. 2 Cciv).");
        }
        if (dependance) {
            messages.add("Victime financièrement dépendante : la pension alimentaire peut être fixée à titre provisoire.");
        }
        if (dejaProtege) {
            messages.add("Demandeur déjà protégé antérieurement : motiver la persistance ou la réapparition du danger pour obtenir un renouvellement.");
        }
        if (nbPreuves < SEUIL_DIVERSIFICATION_PREUVES) {
            messages.add("Preuves limitées : compléter par certificat médical, main courante, témoignages — la diversification renforce la vraisemblance.");
        }
        if (!dangerImmediat && (mesuresRecommandees.isEmpty()
                || mesuresRecommandees.stream().noneMatch(m -> m.equals("TGD") || m.equals("BAR")))) {
            messages.add("Sans danger immédiat caractérisé, TGD et BAR ne peuvent être ordonnés — privilégier interdictions de paraître / d'approcher.");
        }
        if ("ELEVEE".equals(verdict)) {
            messages.add("Probabilité d'octroi élevée — préparer dossier complet (art. 515-9) et demander audience accélérée.");
        }
        if ("FAIBLE".equals(verdict)) {
            messages.add("Vraisemblance faible : étayer le dossier avant dépôt (preuves, plaintes, attestations) ou envisager dispositif distinct.");
        }
        messages.add("Durée de l'OP : 6 mois renouvelables (art. 515-12 Cciv).");
        return messages;
    }
}
