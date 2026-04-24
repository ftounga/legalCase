package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-IM-14-03 : calculateur pour le regroupement familial d'un citoyen UE (non-Belge)
 * en Belgique — art. 40bis Loi du 15/12/1980 sur l'accès au territoire, le séjour,
 * l'établissement et l'éloignement des étrangers + AR 08/10/1981 art. 52.
 *
 * <p>Le ressortissant d'un État membre de l'Union européenne (autre que la Belgique)
 * exerçant son droit de libre circulation peut être rejoint par les membres de sa
 * famille. L'art. 40bis définit les conditions :
 * <ol>
 *   <li>Lien familial : conjoint, partenaire enregistré, descendant (mineur ou majeur
 *       à charge), ascendant à charge.</li>
 *   <li>Le regroupant est citoyen UE (non-Belge — pour un Belge voir art. 40ter).</li>
 *   <li>Catégorie d'activité du regroupant : travailleur, étudiant ou inactif disposant
 *       de ressources suffisantes et d'une assurance maladie.</li>
 *   <li>Ressources suffisantes.</li>
 *   <li>Assurance maladie couvrant l'Union européenne.</li>
 *   <li>Logement suffisant (preuve, pas de surface minimale légale).</li>
 *   <li>Pas de menace pour l'ordre public.</li>
 * </ol>
 *
 * <p>Chaque condition remplie ajoute environ 15 points (6 × 15 ≈ 90) + 10 points si
 * la catégorie d'activité du regroupant est explicite et valide → score plafonné à
 * 100.
 *
 * <p>Verdicts : ELEVEE (≥ 75), MOYENNE (50-74), FAIBLE (&lt; 50).
 *
 * <p>Délai d'instruction : 6 mois à compter du dépôt (art. 42 §1 Loi 15/12/1980 —
 * regroupement familial citoyen UE).
 *
 * <p>Outil <b>single-country BE</b> — équivalent France = F-IM-03 / F-IM-12
 * (regroupement familial CESEDA). Distinct de 40ter (regroupant Belge, règles plus
 * strictes).
 */
public final class Belgian40bisCalculator {

    /** Liens familiaux acceptés au titre de l'art. 40bis §2 Loi 15/12/1980. */
    public static final Set<String> LIENS_VALIDES = Set.of(
            "CONJOINT",
            "PARTENAIRE_ENREGISTRE",
            "DESCENDANT_MINEUR",
            "DESCENDANT_MAJEUR_CHARGE",
            "ASCENDANT_CHARGE"
    );

    /** Catégories d'activité du regroupant au titre de la directive 2004/38/CE. */
    public static final Set<String> CATEGORIES_ACTIVITE_VALIDES = Set.of(
            "TRAVAILLEUR",
            "ETUDIANT",
            "INACTIF_AVEC_RESSOURCES",
            "AUTRE"
    );

    /** Points alloués à chaque condition remplie. */
    public static final int POINTS_PAR_CONDITION = 15;

    /** Bonus si catégorie d'activité explicite et solide (TRAVAILLEUR / ETUDIANT / INACTIF_AVEC_RESSOURCES). */
    public static final int BONUS_CATEGORIE_SOLIDE = 10;

    /** Délai d'instruction de la demande (mois). */
    public static final int DELAI_INSTRUCTION_MOIS = 6;

    /** Délai de recours devant le CCE en annulation (art. 39/2 §2 Loi 15/12/1980). */
    public static final int DELAI_RECOURS_CCE_JOURS = 30;

    /** Score minimum pour verdict ELEVEE. */
    public static final int SEUIL_ELEVEE = 75;

    /** Score minimum pour verdict MOYENNE. */
    public static final int SEUIL_MOYENNE = 50;

    private static final String BASE_JURIDIQUE =
            "Loi 15/12/1980 art. 40bis + AR 08/10/1981 art. 52 (Directive 2004/38/CE)";

    private Belgian40bisCalculator() {
    }

    public static Belgian40bisResult compute(String lienFamilial,
                                             boolean regroupantCitoyenUe,
                                             String regroupantActiviteCategorie,
                                             boolean ressourcesSuffisantes,
                                             boolean assuranceMaladieUe,
                                             boolean logementSuffisant,
                                             boolean menaceOrdrePublic,
                                             LocalDate dateDepotDemande) {
        validateInputs(lienFamilial, regroupantActiviteCategorie, dateDepotDemande);

        boolean lienValide = LIENS_VALIDES.contains(lienFamilial);
        boolean categorieActiviteValide = CATEGORIES_ACTIVITE_VALIDES.contains(regroupantActiviteCategorie);
        boolean regroupantValide = regroupantCitoyenUe && categorieActiviteValide;
        boolean ressourcesOk = ressourcesSuffisantes;
        boolean assuranceOk = assuranceMaladieUe;
        boolean logementOk = logementSuffisant;
        boolean pasMenace = !menaceOrdrePublic;
        boolean categorieSolide = categorieActiviteValide && !"AUTRE".equals(regroupantActiviteCategorie);

        int scoreGlobal = computeScore(lienValide, regroupantValide, ressourcesOk,
                assuranceOk, logementOk, pasMenace, categorieSolide);
        String verdict = verdict(scoreGlobal);

        LocalDate dateExpirationInstruction = dateDepotDemande != null
                ? dateDepotDemande.plusMonths(DELAI_INSTRUCTION_MOIS)
                : null;

        List<String> criteresNonRemplis = buildCriteresNonRemplis(lienValide,
                regroupantCitoyenUe, categorieActiviteValide, ressourcesOk,
                assuranceOk, logementOk, pasMenace);

        List<String> messages = buildMessages(verdict);

        String formule = buildFormule(verdict, scoreGlobal, lienValide,
                regroupantValide, ressourcesOk, assuranceOk, logementOk, pasMenace,
                dateExpirationInstruction);

        return new Belgian40bisResult(
                lienFamilial,
                regroupantCitoyenUe,
                regroupantActiviteCategorie,
                ressourcesSuffisantes,
                assuranceMaladieUe,
                logementSuffisant,
                menaceOrdrePublic,
                dateDepotDemande,
                lienValide,
                regroupantValide,
                ressourcesOk,
                assuranceOk,
                logementOk,
                pasMenace,
                scoreGlobal,
                verdict,
                criteresNonRemplis,
                dateExpirationInstruction,
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    private static int computeScore(boolean lienValide,
                                    boolean regroupantValide,
                                    boolean ressourcesOk,
                                    boolean assuranceOk,
                                    boolean logementOk,
                                    boolean pasMenace,
                                    boolean categorieSolide) {
        int score = 0;
        if (lienValide) score += POINTS_PAR_CONDITION;
        if (regroupantValide) score += POINTS_PAR_CONDITION;
        if (ressourcesOk) score += POINTS_PAR_CONDITION;
        if (assuranceOk) score += POINTS_PAR_CONDITION;
        if (logementOk) score += POINTS_PAR_CONDITION;
        if (pasMenace) score += POINTS_PAR_CONDITION;
        if (regroupantValide && categorieSolide) score += BONUS_CATEGORIE_SOLIDE;
        return Math.min(score, 100);
    }

    private static String verdict(int score) {
        if (score >= SEUIL_ELEVEE) return "ELEVEE";
        if (score >= SEUIL_MOYENNE) return "MOYENNE";
        return "FAIBLE";
    }

    private static void validateInputs(String lienFamilial,
                                       String regroupantActiviteCategorie,
                                       LocalDate dateDepotDemande) {
        if (lienFamilial == null || lienFamilial.isBlank()) {
            throw new IllegalArgumentException("lienFamilial est requis");
        }
        if (!LIENS_VALIDES.contains(lienFamilial)) {
            throw new IllegalArgumentException(
                    "lienFamilial invalide (attendus : " + LIENS_VALIDES + ")");
        }
        if (regroupantActiviteCategorie == null || regroupantActiviteCategorie.isBlank()) {
            throw new IllegalArgumentException("regroupantActiviteCategorie est requis");
        }
        if (!CATEGORIES_ACTIVITE_VALIDES.contains(regroupantActiviteCategorie)) {
            throw new IllegalArgumentException(
                    "regroupantActiviteCategorie invalide (attendus : "
                            + CATEGORIES_ACTIVITE_VALIDES + ")");
        }
        if (dateDepotDemande != null && dateDepotDemande.isAfter(LocalDate.now().plusDays(1))) {
            throw new IllegalArgumentException(
                    "dateDepotDemande ne peut pas être dans le futur");
        }
    }

    private static List<String> buildCriteresNonRemplis(boolean lienValide,
                                                        boolean regroupantCitoyenUe,
                                                        boolean categorieActiviteValide,
                                                        boolean ressourcesOk,
                                                        boolean assuranceOk,
                                                        boolean logementOk,
                                                        boolean pasMenace) {
        List<String> criteres = new ArrayList<>();
        if (!lienValide) {
            criteres.add("Lien familial non reconnu par l'art. 40bis §2 "
                    + "(conjoint, partenaire enregistré, descendant mineur, "
                    + "descendant majeur à charge ou ascendant à charge)");
        }
        if (!regroupantCitoyenUe) {
            criteres.add("Le regroupant doit être citoyen UE (autre que Belge — "
                    + "pour un regroupant belge, voir art. 40ter)");
        }
        if (!categorieActiviteValide) {
            criteres.add("Catégorie d'activité du regroupant invalide");
        }
        if (!ressourcesOk) {
            criteres.add("Ressources suffisantes non démontrées");
        }
        if (!assuranceOk) {
            criteres.add("Assurance maladie couvrant l'UE non démontrée");
        }
        if (!logementOk) {
            criteres.add("Logement suffisant non démontré "
                    + "(pas de surface minimale mais preuve requise)");
        }
        if (!pasMenace) {
            criteres.add("Menace pour l'ordre public présumée");
        }
        return criteres;
    }

    private static List<String> buildMessages(String verdict) {
        List<String> messages = new ArrayList<>();
        messages.add("Carte F (membre de famille d'un citoyen UE) délivrée si demande acceptée, "
                + "valable 5 ans renouvelable (art. 42 §1 Loi 15/12/1980).");
        messages.add("En cas de refus : recours en annulation devant le Conseil du contentieux "
                + "des étrangers (CCE) dans les 30 jours à compter de la notification "
                + "(art. 39/2 §2 Loi 15/12/1980).");
        messages.add("Pièces à constituer : preuve du lien familial (acte de mariage, "
                + "contrat de cohabitation légale, acte de naissance, preuve de prise en "
                + "charge), preuve de la nationalité UE du regroupant, preuve d'activité "
                + "(contrat, inscription, ressources), assurance maladie, preuve de logement, "
                + "extrait casier judiciaire.");
        messages.add("Délai d'instruction de 6 mois à compter du dépôt — prorogeable "
                + "exceptionnellement en cas de dossier complexe (art. 42 §1 al. 3).");
        if ("ELEVEE".equals(verdict)) {
            messages.add("Probabilité d'acceptation élevée : préparer le dossier avec "
                    + "pièces originales ou copies certifiées conformes.");
        }
        if ("FAIBLE".equals(verdict)) {
            messages.add("Probabilité faible : régulariser d'abord les critères manquants "
                    + "avant dépôt ou envisager une autre base juridique (ex. art. 9bis "
                    + "humanitaire ou 9ter médical si applicable).");
        }
        messages.add("Distinct de l'art. 40ter (regroupant belge — règles plus strictes "
                + "notamment en termes de ressources et de logement). Voir F-IM-14-04.");
        return messages;
    }

    private static String buildFormule(String verdict,
                                       int scoreGlobal,
                                       boolean lienValide,
                                       boolean regroupantValide,
                                       boolean ressourcesOk,
                                       boolean assuranceOk,
                                       boolean logementOk,
                                       boolean pasMenace,
                                       LocalDate dateExpirationInstruction) {
        StringBuilder sb = new StringBuilder();
        sb.append("Regroupement familial 40bis (citoyen UE) : probabilité ")
                .append(verdict)
                .append(" (score ").append(scoreGlobal).append("/100)");

        List<String> parts = new ArrayList<>();
        parts.add("lien " + (lienValide ? "OK" : "KO"));
        parts.add("regroupant UE " + (regroupantValide ? "OK" : "KO"));
        parts.add("ressources " + (ressourcesOk ? "OK" : "KO"));
        parts.add("assurance " + (assuranceOk ? "OK" : "KO"));
        parts.add("logement " + (logementOk ? "OK" : "KO"));
        parts.add("ordre public " + (pasMenace ? "OK" : "KO"));
        sb.append(" — ").append(String.join(", ", parts)).append(".");

        if (dateExpirationInstruction != null) {
            sb.append(" Délai d'instruction jusqu'au ")
                    .append(dateExpirationInstruction)
                    .append(" (6 mois, art. 42 §1 Loi 15/12/1980).");
        }
        return sb.toString();
    }
}
