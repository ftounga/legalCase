package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-220-06 : analyseur de la contestation / radiation d'un signalement aux fins
 * de non-admission dans le Système d'information Schengen (SIS)
 * (F-IM-52-signalement-sis-fr, Règl. UE 2018/1860 / CESEDA L.312-3). Outil
 * single-country FR.
 *
 * <p><b>Objet</b> : un signalement SIS « non-admission » bloque l'entrée dans
 * l'espace Schengen même lorsque l'étranger détient un titre de séjour valide.
 * L'outil identifie la voie de contestation / radiation pertinente selon
 * l'<b>État signalant</b> :
 * <ul>
 *   <li>signalement français → contestation devant l'autorité française et
 *       radiation liée à l'effacement de la mesure sous-jacente ;</li>
 *   <li>signalement par un autre État membre → la radiation relève de l'État
 *       signalant ; orientation vers le droit d'accès / rectification (autorité
 *       de contrôle / point de contact national) ;</li>
 *   <li>titre de séjour français valide + signalement étranger → conflit (titre
 *       valide vs non-admission) appelant une procédure de consultation entre
 *       États avant non-admission.</li>
 * </ul>
 * </p>
 *
 * <p>Distinct de F-IM-20 (mesures d'éloignement : expulsion / IRTF / IAT) :
 * l'IRTF est la mesure nationale d'interdiction de retour, tandis que le
 * <b>signalement SIS</b> est l'inscription dans la base Schengen qui en découle
 * ou qui existe indépendamment (signalement par un autre État membre). L'outil
 * traite le signalement lui-même (sa contestation / radiation), non l'IRTF.</p>
 *
 * <p>Toutes ces appréciations sont annotées « à vérifier par avocat » : l'outil
 * aiguille sur la voie de contestation, il ne se substitue pas à l'appréciation
 * du juge ni de l'autorité compétente.</p>
 */
public final class SignalementSisAnalyzer {

    // Verdicts (actionPossible).
    public static final String RADIATION_AUTORITE_FR = "RADIATION_AUTORITE_FR";
    public static final String RADIATION_ETAT_SIGNALANT = "RADIATION_ETAT_SIGNALANT";
    public static final String DROIT_ACCES_RECTIFICATION = "DROIT_ACCES_RECTIFICATION";
    public static final String CONSULTATION_ENTRE_ETATS = "CONSULTATION_ENTRE_ETATS";
    public static final String INDETERMINE = "INDETERMINE";

    // État signalant.
    public static final String ETAT_FRANCE = "FRANCE";
    public static final String ETAT_AUTRE_ETAT_MEMBRE = "AUTRE_ETAT_MEMBRE";
    public static final String ETAT_INCONNU = "INCONNU";
    public static final Set<String> ETAT_SIGNALANT_VALEURS = Set.of(
            ETAT_FRANCE, ETAT_AUTRE_ETAT_MEMBRE, ETAT_INCONNU);

    // Motif du signalement.
    public static final String MOTIF_IRTF = "IRTF";
    public static final String MOTIF_MESURE_ELOIGNEMENT_ETRANGERE = "MESURE_ELOIGNEMENT_ETRANGERE";
    public static final String MOTIF_MENACE_ORDRE_PUBLIC = "MENACE_ORDRE_PUBLIC";
    public static final String MOTIF_AUTRE = "AUTRE";
    public static final Set<String> MOTIF_SIGNALEMENT_VALEURS = Set.of(
            MOTIF_IRTF, MOTIF_MESURE_ELOIGNEMENT_ETRANGERE, MOTIF_MENACE_ORDRE_PUBLIC, MOTIF_AUTRE);

    private static final String BASE_REGL_2018_1860 =
            "Règlement (UE) 2018/1860 du 28/11/2018 (utilisation du SIS aux fins de retour) "
                    + "— à vérifier par avocat";
    private static final String BASE_CESEDA_L312_3 =
            "CESEDA L.312-3 (non-admission / signalement) — à vérifier par avocat";
    private static final String BASE_DROIT_ACCES =
            "Droit d'accès et de rectification des données SIS (autorité de contrôle / point de "
                    + "contact national de l'État signalant) — à vérifier par avocat";

    private SignalementSisAnalyzer() {}

    /**
     * Analyse la voie de contestation / radiation d'un signalement SIS.
     *
     * @param signalementConnu   true si le signalement est connu / documenté (nullable)
     * @param etatSignalant      État à l'origine du signalement (whitelist, nullable)
     * @param motifSignalement   motif du signalement (whitelist, nullable)
     * @param titreSejourValide  true si l'étranger détient un titre de séjour FR valide (nullable)
     * @return résultat d'analyse
     */
    public static SignalementSisResult analyze(Boolean signalementConnu,
                                               String etatSignalant,
                                               String motifSignalement,
                                               Boolean titreSejourValide) {

        List<String> demarches = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        bases.add(BASE_REGL_2018_1860);
        bases.add(BASE_CESEDA_L312_3);

        messages.add("Rappel : un signalement aux fins de non-admission dans le SIS bloque l'entrée "
                + "dans l'espace Schengen même avec un titre de séjour valide. La voie de contestation "
                + "dépend de l'État qui a inscrit le signalement. À vérifier par avocat.");

        // Distinction explicite avec l'IRTF (F-IM-20).
        messages.add("Distinction : l'IRTF (interdiction de retour, F-IM-20) est la mesure NATIONALE "
                + "d'éloignement ; le signalement SIS est son INSCRIPTION dans la base Schengen (ou un "
                + "signalement existant d'un autre État). Cet outil traite la contestation / radiation du "
                + "signalement lui-même, pas de l'IRTF. À vérifier par avocat.");

        boolean titreValide = Boolean.TRUE.equals(titreSejourValide);
        boolean signalementFrancais = ETAT_FRANCE.equals(etatSignalant);
        boolean signalementEtranger = ETAT_AUTRE_ETAT_MEMBRE.equals(etatSignalant);

        if (Boolean.FALSE.equals(signalementConnu)) {
            messages.add("Signalement non confirmé : avant toute démarche, demander l'accès aux données "
                    + "SIS (droit d'accès) pour établir l'existence, l'État signalant et le motif du "
                    + "signalement. À vérifier par avocat.");
        }

        String actionPossible;
        String autoriteCompetente;

        if (signalementFrancais) {
            actionPossible = RADIATION_AUTORITE_FR;
            autoriteCompetente = "Autorité française à l'origine du signalement (préfecture / ministère "
                    + "de l'Intérieur), puis juge administratif (TA / Conseil d'État selon l'acte)";
            demarches.add("Contester la mesure sous-jacente devant l'autorité française : recours "
                    + "administratif (gracieux / hiérarchique) puis recours contentieux.");
            demarches.add("Demander la radiation du signalement SIS liée à l'effacement / annulation de "
                    + "la mesure sous-jacente (la radiation suit l'effacement de la décision).");
            messages.add("Signalement d'origine française : la radiation est obtenue en faisant tomber "
                    + "la mesure sous-jacente (IRTF / éloignement) devant l'autorité et le juge français. "
                    + "À vérifier par avocat.");
        } else if (signalementEtranger) {
            if (titreValide) {
                actionPossible = CONSULTATION_ENTRE_ETATS;
                autoriteCompetente = "État membre signalant (autorité de contrôle / point de contact "
                        + "national SIRENE), en lien avec l'autorité française pour la consultation";
                demarches.add("Faire valoir le titre de séjour français valide : un titre valide peut "
                        + "justifier une procédure de consultation entre États avant toute non-admission "
                        + "(article SIS dédié).");
                demarches.add("Saisir en parallèle l'autorité de l'État signalant pour faire vérifier / "
                        + "rectifier le signalement (droit d'accès et de rectification).");
                bases.add(BASE_DROIT_ACCES);
                messages.add("Conflit titre valide vs non-admission : la détention d'un titre de séjour "
                        + "français en cours de validité doit déclencher une consultation entre l'État "
                        + "signalant et la France avant une décision de non-admission. À vérifier par "
                        + "avocat.");
            } else {
                actionPossible = RADIATION_ETAT_SIGNALANT;
                autoriteCompetente = "État membre signalant (autorité compétente / autorité de contrôle "
                        + "/ point de contact national)";
                demarches.add("Exercer le droit d'accès puis de rectification des données SIS auprès de "
                        + "l'autorité de l'État signalant (la radiation relève de cet État).");
                demarches.add("Si nécessaire, saisir l'autorité de contrôle / la juridiction compétente "
                        + "de l'État signalant : la France ne peut radier un signalement émis par un autre "
                        + "État.");
                bases.add(BASE_DROIT_ACCES);
                messages.add("Signalement émis par un autre État membre : la radiation relève de l'État "
                        + "signalant, pas de l'autorité française. Orienter vers le droit d'accès / "
                        + "rectification de cet État. À vérifier par avocat.");
            }
        } else {
            // ETAT_INCONNU, null, ou état non déterminé.
            actionPossible = DROIT_ACCES_RECTIFICATION;
            autoriteCompetente = "Exercer d'abord le droit d'accès SIS pour identifier l'État signalant "
                    + "et l'autorité compétente";
            demarches.add("Exercer le droit d'accès aux données SIS pour déterminer l'État signalant, "
                    + "le motif et la base du signalement.");
            demarches.add("Une fois l'État signalant identifié : France → contestation de la mesure "
                    + "sous-jacente ; autre État → droit de rectification auprès de cet État.");
            bases.add(BASE_DROIT_ACCES);
            messages.add("État signalant non déterminé : la première étape est l'exercice du droit "
                    + "d'accès pour identifier qui a inscrit le signalement, avant de choisir la voie de "
                    + "radiation. À vérifier par avocat.");
        }

        if (titreValide && signalementFrancais) {
            messages.add("Titre de séjour français valide alors que la France a elle-même signalé : "
                    + "cohérence à vérifier (un titre valide est en principe incompatible avec une mesure "
                    + "d'éloignement / IRTF active). À vérifier par avocat.");
        }

        return new SignalementSisResult(
                signalementConnu,
                etatSignalant,
                motifSignalement,
                titreSejourValide,
                actionPossible,
                demarches,
                autoriteCompetente,
                bases,
                messages);
    }
}
