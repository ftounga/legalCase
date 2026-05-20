package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * SF-207-03 : calculateur des <b>deux délais successifs</b> de contestation
 * d'une décision ONEM (exclusion / sanction).
 *
 * <p>Base juridique :
 * <ul>
 *   <li><b>AR du 25 novembre 1991 portant réglementation du chômage,
 *       art. 144</b> — recours administratif préalable auprès du Directeur
 *       du Bureau du chômage, <b>1 mois</b> à compter de la notification
 *       de la décision ONEM.</li>
 *   <li><b>Code judiciaire art. 580, 2°</b> — recours juridictionnel devant
 *       le tribunal du travail, <b>3 mois</b> à compter de la notification
 *       de la décision du Directeur (ou de l'expiration du délai de
 *       réponse).</li>
 *   <li><b>Loi du 3 juillet 1978 relative aux contrats de travail</b> —
 *       référence générale.</li>
 * </ul>
 *
 * <p>Logique :
 * <ul>
 *   <li><b>Cas A</b> — {@code recoursAdminDejaForme=false} (palier ADMIN) :
 *       <ul>
 *         <li>{@code dateLimiteAdmin = dateNotificationDecisionOnem + 1 mois}</li>
 *         <li>{@code joursRestantsAdmin = dateLimiteAdmin - dateActionEnvisagee}</li>
 *         <li>Verdict {@link ContestationC4OnemResult.Verdict#RECOURS_ADMIN_OUVERT}
 *             si {@code joursRestants > 7} ;
 *             {@link ContestationC4OnemResult.Verdict#RECOURS_ADMIN_IMMINENT}
 *             si {@code 0 < joursRestants ≤ 7} ;
 *             {@link ContestationC4OnemResult.Verdict#RECOURS_ADMIN_PRESCRIT}
 *             si {@code joursRestants ≤ 0}.</li>
 *         <li>Palier TRIBUNAL renvoyé avec dates / jours à {@code null}
 *             (indéterminé tant que la décision Directeur n'est pas
 *             notifiée).</li>
 *         <li>{@code etapeSuivante = RECOURS_ADMIN_DIRECTEUR} si admin
 *             ouvert / imminent ; {@code RECOURS_TRIBUNAL_TRAVAIL} si admin
 *             prescrit (saut palier admin, à valider par l'avocat).</li>
 *       </ul>
 *   </li>
 *   <li><b>Cas B</b> — {@code recoursAdminDejaForme=true} avec
 *       {@code dateDecisionDirecteur} non null (palier TRIBUNAL) :
 *       <ul>
 *         <li>{@code dateLimiteTribunal = dateDecisionDirecteur + 3 mois}</li>
 *         <li>{@code joursRestantsTribunal = dateLimiteTribunal - dateActionEnvisagee}</li>
 *         <li>Verdict TRIBUNAL avec seuils 14 j (IMMINENT) / 0 (PRESCRIT).</li>
 *         <li>Palier ADMIN renvoyé en informatif (prescrit, jours négatifs).</li>
 *         <li>{@code etapeSuivante = RECOURS_TRIBUNAL_TRAVAIL} si tribunal
 *             ouvert / imminent ; {@code FORCLUSION_TOTALE} si tribunal
 *             prescrit.</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <p>Calculs en fuseau {@link #ZONE_BRUSSELS Europe/Brussels}. L'ajustement
 * « 1 mois » / « 3 mois » est délégué à {@link LocalDate#plusMonths(long)},
 * qui gère automatiquement les fins de mois (31 janvier + 1 mois →
 * 28 ou 29 février).</p>
 *
 * <p>Outil <b>BE-only</b> par construction — aucune logique FR (cf. mémoire
 * {@code feedback_belgique_never_forget}). L'équivalent FR (contestation
 * des décisions France Travail / Pôle emploi — {@link ContestationAreCalculator})
 * est un régime juridiquement distinct géré par F-DT-35.</p>
 */
public final class ContestationC4OnemCalculator {

    /** Délai du recours administratif au Directeur du Bureau du chômage (mois). */
    public static final int DELAI_ADMIN_MOIS = 1;

    /** Délai du recours juridictionnel devant le tribunal du travail (mois). */
    public static final int DELAI_TRIBUNAL_MOIS = 3;

    /** Seuil de joursRestants pour basculer en IMMINENT côté ADMIN (inclusif). */
    public static final int SEUIL_IMMINENT_ADMIN_JOURS = 7;

    /** Seuil de joursRestants pour basculer en IMMINENT côté TRIBUNAL (inclusif). */
    public static final int SEUIL_IMMINENT_TRIBUNAL_JOURS = 14;

    public static final String BASE_ADMIN = "AR du 25 novembre 1991 art. 144";
    public static final String BASE_TRIBUNAL = "Code judiciaire art. 580, 2°";

    public static final String BASE_JURIDIQUE_COMPLET =
            "AR du 25 novembre 1991 art. 144 ; CJ art. 580, 2° ; Loi du 3 juillet 1978";

    /** Fuseau horaire belge — cohérent avec les autres outils BE. */
    public static final ZoneId ZONE_BRUSSELS = ZoneId.of("Europe/Brussels");

    private ContestationC4OnemCalculator() {
    }

    /**
     * Calcule l'état de la voie de contestation d'une décision ONEM. Fonction
     * pure : aucun effet de bord, déterministe à inputs égaux.
     *
     * @param request payload (Bean Validation amont garantit
     *                {@code dateNotificationDecisionOnem != null}).
     * @return résultat structuré (verdict + 2 paliers + étape suivante +
     *         base juridique + formule de calcul).
     * @throws IllegalArgumentException si {@code request} est null, si
     *         {@code dateNotificationDecisionOnem} est dans le futur,
     *         si {@code recoursAdminDejaForme=true} sans
     *         {@code dateDecisionDirecteur}, si
     *         {@code dateDecisionDirecteur < dateNotificationDecisionOnem},
     *         ou si {@code dateActionEnvisagee} est strictement antérieure
     *         à {@code dateNotificationDecisionOnem}.
     */
    public static ContestationC4OnemResult compute(ContestationC4OnemRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête de contestation C4 ONEM requise");
        }
        LocalDate dateNotif = request.dateNotificationDecisionOnem();
        if (dateNotif == null) {
            throw new IllegalArgumentException(
                    "dateNotificationDecisionOnem est requise (date de notification de la décision ONEM contestée)");
        }
        LocalDate today = LocalDate.now(ZONE_BRUSSELS);
        if (dateNotif.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateNotificationDecisionOnem ne peut être dans le futur");
        }

        LocalDate dateAction = request.dateActionEnvisagee() != null
                ? request.dateActionEnvisagee()
                : today;
        if (dateAction.isBefore(dateNotif)) {
            throw new IllegalArgumentException(
                    "dateActionEnvisagee doit être postérieure ou égale à dateNotificationDecisionOnem");
        }

        boolean recoursAdminDejaForme = Boolean.TRUE.equals(request.recoursAdminDejaForme());
        LocalDate dateDecisionDirecteur = request.dateDecisionDirecteur();

        if (recoursAdminDejaForme && dateDecisionDirecteur == null) {
            throw new IllegalArgumentException(
                    "dateDecisionDirecteur est requise lorsque recoursAdminDejaForme=true");
        }
        if (dateDecisionDirecteur != null && dateDecisionDirecteur.isBefore(dateNotif)) {
            throw new IllegalArgumentException(
                    "dateDecisionDirecteur doit être postérieure ou égale à dateNotificationDecisionOnem");
        }

        if (recoursAdminDejaForme) {
            return computeCasB(dateNotif, dateAction, dateDecisionDirecteur);
        }
        return computeCasA(dateNotif, dateAction);
    }

    // =========================================================================
    // Cas A — recours administratif au Directeur (palier ADMIN actif).
    // =========================================================================

    private static ContestationC4OnemResult computeCasA(LocalDate dateNotif,
                                                        LocalDate dateAction) {
        LocalDate dateLimiteAdmin = dateNotif.plusMonths(DELAI_ADMIN_MOIS);
        int joursRestantsAdmin = (int) ChronoUnit.DAYS.between(dateAction, dateLimiteAdmin);

        ContestationC4OnemResult.Verdict verdict = computeVerdictAdmin(joursRestantsAdmin);
        ContestationC4OnemResult.EtapeSuivante etape =
                verdict == ContestationC4OnemResult.Verdict.RECOURS_ADMIN_PRESCRIT
                        ? ContestationC4OnemResult.EtapeSuivante.RECOURS_TRIBUNAL_TRAVAIL
                        : ContestationC4OnemResult.EtapeSuivante.RECOURS_ADMIN_DIRECTEUR;

        List<ContestationC4OnemResult.Palier> paliers = List.of(
                new ContestationC4OnemResult.Palier(
                        ContestationC4OnemResult.PalierType.ADMIN,
                        dateLimiteAdmin,
                        joursRestantsAdmin,
                        BASE_ADMIN),
                new ContestationC4OnemResult.Palier(
                        ContestationC4OnemResult.PalierType.TRIBUNAL,
                        null,
                        null,
                        BASE_TRIBUNAL));

        String formule = "Notification ONEM (" + dateNotif + ") + " + DELAI_ADMIN_MOIS
                + " mois = dateLimiteAdmin (" + dateLimiteAdmin + ") ; joursRestants = "
                + joursRestantsAdmin + " → verdict " + verdict.name()
                + " ; tribunal indéterminé tant que décision Directeur non notifiée.";

        return new ContestationC4OnemResult(
                dateNotif,
                dateAction,
                false,
                null,
                verdict,
                paliers,
                etape,
                BASE_JURIDIQUE_COMPLET,
                formule);
    }

    // =========================================================================
    // Cas B — recours administratif déjà formé (palier TRIBUNAL actif).
    // =========================================================================

    private static ContestationC4OnemResult computeCasB(LocalDate dateNotif,
                                                        LocalDate dateAction,
                                                        LocalDate dateDecisionDirecteur) {
        LocalDate dateLimiteTribunal = dateDecisionDirecteur.plusMonths(DELAI_TRIBUNAL_MOIS);
        int joursRestantsTribunal = (int) ChronoUnit.DAYS.between(dateAction, dateLimiteTribunal);

        ContestationC4OnemResult.Verdict verdict = computeVerdictTribunal(joursRestantsTribunal);
        ContestationC4OnemResult.EtapeSuivante etape =
                verdict == ContestationC4OnemResult.Verdict.RECOURS_TRIBUNAL_PRESCRIT
                        ? ContestationC4OnemResult.EtapeSuivante.FORCLUSION_TOTALE
                        : ContestationC4OnemResult.EtapeSuivante.RECOURS_TRIBUNAL_TRAVAIL;

        // Palier ADMIN informatif : recalcul du délai au regard de la date d'action
        // pour traçabilité (peut être négatif — la voie admin est typiquement
        // dépassée si l'avocat a déjà formé le recours admin).
        LocalDate dateLimiteAdmin = dateNotif.plusMonths(DELAI_ADMIN_MOIS);
        int joursRestantsAdmin = (int) ChronoUnit.DAYS.between(dateAction, dateLimiteAdmin);

        List<ContestationC4OnemResult.Palier> paliers = List.of(
                new ContestationC4OnemResult.Palier(
                        ContestationC4OnemResult.PalierType.ADMIN,
                        dateLimiteAdmin,
                        joursRestantsAdmin,
                        BASE_ADMIN),
                new ContestationC4OnemResult.Palier(
                        ContestationC4OnemResult.PalierType.TRIBUNAL,
                        dateLimiteTribunal,
                        joursRestantsTribunal,
                        BASE_TRIBUNAL));

        String formule = "Décision Directeur (" + dateDecisionDirecteur + ") + "
                + DELAI_TRIBUNAL_MOIS + " mois = dateLimiteTribunal ("
                + dateLimiteTribunal + ") ; joursRestants = " + joursRestantsTribunal
                + " → verdict " + verdict.name()
                + " ; palier admin informatif (joursRestants = " + joursRestantsAdmin + ").";

        return new ContestationC4OnemResult(
                dateNotif,
                dateAction,
                true,
                dateDecisionDirecteur,
                verdict,
                paliers,
                etape,
                BASE_JURIDIQUE_COMPLET,
                formule);
    }

    // =========================================================================
    // Helpers verdict
    // =========================================================================

    private static ContestationC4OnemResult.Verdict computeVerdictAdmin(int joursRestants) {
        if (joursRestants <= 0) {
            return ContestationC4OnemResult.Verdict.RECOURS_ADMIN_PRESCRIT;
        }
        if (joursRestants <= SEUIL_IMMINENT_ADMIN_JOURS) {
            return ContestationC4OnemResult.Verdict.RECOURS_ADMIN_IMMINENT;
        }
        return ContestationC4OnemResult.Verdict.RECOURS_ADMIN_OUVERT;
    }

    private static ContestationC4OnemResult.Verdict computeVerdictTribunal(int joursRestants) {
        if (joursRestants <= 0) {
            return ContestationC4OnemResult.Verdict.RECOURS_TRIBUNAL_PRESCRIT;
        }
        if (joursRestants <= SEUIL_IMMINENT_TRIBUNAL_JOURS) {
            return ContestationC4OnemResult.Verdict.RECOURS_TRIBUNAL_IMMINENT;
        }
        return ContestationC4OnemResult.Verdict.RECOURS_TRIBUNAL_OUVERT;
    }
}
