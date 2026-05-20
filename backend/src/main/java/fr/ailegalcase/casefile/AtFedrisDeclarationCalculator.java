package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * SF-207-04 : calculateur du délai de <b>8 jours calendaires</b> imparti à
 * l'employeur belge pour déclarer un accident du travail à <b>Fedris</b>
 * (Agence fédérale des risques professionnels, fusion 2017 ex-Fonds des
 * Accidents du Travail).
 *
 * <p>Base juridique :
 * <ul>
 *   <li><b>Loi du 10 avril 1971 sur les accidents du travail, art. 62</b> —
 *       obligation pour l'employeur de déclarer l'accident à l'organisme
 *       assureur (Fedris ou assureur loi privé) dans les <b>8 jours</b>
 *       à compter du jour où l'employeur a eu connaissance de l'accident.</li>
 *   <li><b>AR du 21 décembre 1971 art. 25</b> — modalités pratiques de
 *       la déclaration (formulaire, mentions obligatoires, voies de
 *       transmission).</li>
 * </ul>
 *
 * <p>Sanction en cas de dépassement (art. 62 al. 2-3 Loi 1971) :
 * <ul>
 *   <li>préjudice subi par le salarié dans l'indemnisation de l'accident
 *       (déchéance partielle du droit aux indemnités d'incapacité ou
 *       retard de paiement) ;</li>
 *   <li>intérêts moratoires sur les indemnités dues ;</li>
 *   <li>responsabilité civile de l'employeur (action récursoire de
 *       Fedris / assureur loi).</li>
 * </ul>
 *
 * <p>Deux modes de calcul :
 * <ul>
 *   <li><b>Mode prospectif</b> ({@code dateDeclarationEffectuee == null}) —
 *       l'employeur n'a pas encore déclaré et veut savoir s'il est dans
 *       les temps :
 *     <ul>
 *       <li>{@code dateLimite = (dateConnaissanceEmployeur ?? dateAccident)
 *           + 8 jours}.</li>
 *       <li>{@code joursRestants = dateLimite - dateActionEnvisagee}
 *           (calendaires, fuseau Europe/Brussels).</li>
 *       <li>Verdict :
 *         <ul>
 *           <li>{@link AtFedrisDeclarationResult.Verdict#DELAI_OUVERT}
 *               si {@code joursRestants > 2}</li>
 *           <li>{@link AtFedrisDeclarationResult.Verdict#DELAI_IMMINENT}
 *               si {@code 0 ≤ joursRestants ≤ 2} (seuil inclusif)</li>
 *           <li>{@link AtFedrisDeclarationResult.Verdict#DELAI_DEPASSE}
 *               si {@code joursRestants < 0}</li>
 *         </ul>
 *       </li>
 *     </ul>
 *   </li>
 *   <li><b>Mode rétrospectif</b> ({@code dateDeclarationEffectuee != null}) —
 *       l'employeur a déjà déclaré et veut savoir si la déclaration est
 *       intervenue dans le délai :
 *     <ul>
 *       <li>Verdict {@link AtFedrisDeclarationResult.Verdict#DECLARATION_DANS_LES_TEMPS}
 *           si {@code dateDeclarationEffectuee ≤ dateLimite}.</li>
 *       <li>Verdict {@link AtFedrisDeclarationResult.Verdict#DECLARATION_HORS_DELAI}
 *           sinon.</li>
 *       <li>{@code joursRestants = dateLimite - dateDeclarationEffectuee}
 *           (positif si dans les temps, négatif = jours de retard).</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>Outil <b>BE-only</b> par construction — aucune logique FR (cf. mémoire
 * {@code feedback_belgique_never_forget}). L'équivalent FR (déclaration AT
 * employeur 48h, L.441-1 CSS) est juridiquement distinct (régime CPAM,
 * géré par {@link AtMpCalculator}).</p>
 */
public final class AtFedrisDeclarationCalculator {

    /** Délai de déclaration imparti à l'employeur (jours calendaires). */
    public static final int DELAI_DECLARATION_JOURS = 8;

    /** Seuil de joursRestants au-dessous duquel on bascule en IMMINENT (inclusif). */
    public static final int SEUIL_IMMINENT_JOURS = 2;

    public static final String REGLE_8_JOURS_LOI_1971_ART_62 = "8_JOURS_LOI_1971_ART_62";

    public static final String BASE_JURIDIQUE_COMPLETE =
            "Loi du 10 avril 1971 sur les accidents du travail, art. 62 ; "
                    + "AR 21/12/1971 art. 25";

    public static final String CONSEQUENCES_NON_RESPECT =
            "Préjudice salarié (perte d'indemnisation ou retard de paiement) ; "
                    + "intérêts moratoires sur les indemnités dues ; "
                    + "responsabilité civile de l'employeur "
                    + "(art. 62 al. 2-3 Loi du 10 avril 1971).";

    /** Fuseau horaire belge — cohérent avec les autres outils BE. */
    public static final ZoneId ZONE_BRUSSELS = ZoneId.of("Europe/Brussels");

    private AtFedrisDeclarationCalculator() {
    }

    /**
     * Calcule l'état du délai de déclaration AT Fedris pour un accident
     * donné. Fonction pure : aucun effet de bord, déterministe à inputs
     * égaux.
     *
     * @param request payload normalisé (validé en amont par le service
     *                pour les contrôles de cohérence inter-champs)
     * @return résultat structuré (verdict + date limite + jours restants +
     *         règle appliquée + base juridique + formule de calcul +
     *         conséquences du non-respect)
     * @throws IllegalArgumentException si {@code dateAccident} est null
     *         (le service applique déjà Bean Validation, ce cas est une
     *         défense en profondeur).
     */
    public static AtFedrisDeclarationResult compute(AtFedrisDeclarationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête de déclaration AT Fedris requise");
        }
        LocalDate dateAccident = request.dateAccident();
        if (dateAccident == null) {
            throw new IllegalArgumentException("dateAccident est requise");
        }

        // Si l'employeur n'a pas explicitement renseigné la date de
        // connaissance, on suppose qu'il a eu connaissance le jour même
        // de l'accident (cas le plus fréquent en pratique).
        LocalDate dateConnaissance = request.dateConnaissanceEmployeur() != null
                ? request.dateConnaissanceEmployeur()
                : dateAccident;

        LocalDate dateLimite = dateConnaissance.plusDays(DELAI_DECLARATION_JOURS);

        LocalDate dateDeclaration = request.dateDeclarationEffectuee();
        if (dateDeclaration != null) {
            // Mode rétrospectif : la déclaration a déjà été faite
            return retrospectif(request, dateAccident, dateConnaissance, dateLimite, dateDeclaration);
        }

        // Mode prospectif : la déclaration n'a pas encore été faite
        LocalDate dateAction = request.dateActionEnvisagee() != null
                ? request.dateActionEnvisagee()
                : LocalDate.now(ZONE_BRUSSELS);

        long joursRestants = ChronoUnit.DAYS.between(dateAction, dateLimite);
        String verdict = computeVerdictProspectif(joursRestants);
        String formule = buildFormuleProspectif(
                dateConnaissance, dateAction, dateLimite, joursRestants, verdict);

        return new AtFedrisDeclarationResult(
                dateAccident,
                dateConnaissance,
                dateAction,
                null,
                verdict,
                dateLimite,
                joursRestants,
                REGLE_8_JOURS_LOI_1971_ART_62,
                BASE_JURIDIQUE_COMPLETE,
                formule,
                CONSEQUENCES_NON_RESPECT);
    }

    // =========================================================================
    // Mode rétrospectif
    // =========================================================================

    private static AtFedrisDeclarationResult retrospectif(AtFedrisDeclarationRequest request,
                                                          LocalDate dateAccident,
                                                          LocalDate dateConnaissance,
                                                          LocalDate dateLimite,
                                                          LocalDate dateDeclaration) {
        // joursRestants : positif si déclaration dans les temps,
        // négatif = nombre de jours de retard.
        long joursRestants = ChronoUnit.DAYS.between(dateDeclaration, dateLimite);
        String verdict = !dateDeclaration.isAfter(dateLimite)
                ? AtFedrisDeclarationResult.Verdict.DECLARATION_DANS_LES_TEMPS.name()
                : AtFedrisDeclarationResult.Verdict.DECLARATION_HORS_DELAI.name();
        String formule = buildFormuleRetrospectif(
                dateConnaissance, dateLimite, dateDeclaration, joursRestants, verdict);

        return new AtFedrisDeclarationResult(
                dateAccident,
                dateConnaissance,
                request.dateActionEnvisagee(),
                dateDeclaration,
                verdict,
                dateLimite,
                joursRestants,
                REGLE_8_JOURS_LOI_1971_ART_62,
                BASE_JURIDIQUE_COMPLETE,
                formule,
                CONSEQUENCES_NON_RESPECT);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static String computeVerdictProspectif(long joursRestants) {
        if (joursRestants < 0) {
            return AtFedrisDeclarationResult.Verdict.DELAI_DEPASSE.name();
        }
        if (joursRestants <= SEUIL_IMMINENT_JOURS) {
            return AtFedrisDeclarationResult.Verdict.DELAI_IMMINENT.name();
        }
        return AtFedrisDeclarationResult.Verdict.DELAI_OUVERT.name();
    }

    private static String buildFormuleProspectif(LocalDate dateConnaissance,
                                                 LocalDate dateAction,
                                                 LocalDate dateLimite,
                                                 long joursRestants,
                                                 String verdict) {
        return "dateConnaissanceEmployeur (" + dateConnaissance + ") + "
                + DELAI_DECLARATION_JOURS + " jours = dateLimite (" + dateLimite
                + ") ; dateActionEnvisagee (" + dateAction
                + ") → joursRestants = " + joursRestants + " → " + verdict;
    }

    private static String buildFormuleRetrospectif(LocalDate dateConnaissance,
                                                   LocalDate dateLimite,
                                                   LocalDate dateDeclaration,
                                                   long joursRestants,
                                                   String verdict) {
        return "dateConnaissanceEmployeur (" + dateConnaissance + ") + "
                + DELAI_DECLARATION_JOURS + " jours = dateLimite (" + dateLimite
                + ") ; dateDeclarationEffectuee (" + dateDeclaration
                + ") → joursRestants = " + joursRestants + " → " + verdict;
    }
}
