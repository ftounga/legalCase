package fr.ailegalcase.casefile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-208-04 : analyseur d'éligibilité au titre de séjour vie privée et familiale
 * pour victime de violences (CESEDA L.425-6, ancien L.316-3) — articulé avec
 * l'ordonnance de protection JAF (Cciv 515-9 à 515-13).
 *
 * <p>Verdict :
 * <ul>
 *   <li>{@code ELIGIBLE_PLEIN_DROIT} : ordonnance JAF en cours, durée non expirée</li>
 *   <li>{@code ELIGIBLE_SOUS_RESERVE} : ordonnance expirée mais récente, ou critères partiels</li>
 *   <li>{@code NON_ELIGIBLE} : aucune ordonnance ou ordonnance expirée depuis longtemps</li>
 * </ul>
 *
 * <p>Outil <b>single-country FR</b>.
 */
public final class VictimeViolencesL4256Analyzer {

    /** Durée par défaut d'une ordonnance de protection (Cciv 515-12). */
    public static final int DUREE_PROTECTION_DEFAUT_MOIS = 6;

    /** Durée du titre de séjour vie privée et familiale "victime de violences" (L.425-7). */
    public static final int DUREE_TITRE_SEJOUR_MOIS = 12;

    /** Marge de tolérance après expiration de l'ordonnance pour rester ELIGIBLE_SOUS_RESERVE. */
    public static final int MARGE_POST_EXPIRATION_MOIS = 3;

    public static final String VERDICT_ELIGIBLE_PLEIN_DROIT = "ELIGIBLE_PLEIN_DROIT";
    public static final String VERDICT_ELIGIBLE_SOUS_RESERVE = "ELIGIBLE_SOUS_RESERVE";
    public static final String VERDICT_NON_ELIGIBLE = "NON_ELIGIBLE";

    private static final String BASE_JURIDIQUE =
            "CESEDA L.425-6 à L.425-8 (ancien L.316-3) ; Cciv 515-9 à 515-13 (ordonnance protection JAF)";

    private VictimeViolencesL4256Analyzer() {
    }

    public static VictimeViolencesL4256Result analyze(LocalDate dateOrdonnanceProtection,
                                                      String juridiction,
                                                      int dureeProtectionMois,
                                                      LocalDate dateExpirationProtection,
                                                      int enfantsAcharge,
                                                      String nationalite) {
        return analyze(dateOrdonnanceProtection, juridiction, dureeProtectionMois,
                dateExpirationProtection, enfantsAcharge, nationalite,
                Clock.system(ZoneId.of("Europe/Paris")));
    }

    public static VictimeViolencesL4256Result analyze(LocalDate dateOrdonnanceProtection,
                                                      String juridiction,
                                                      int dureeProtectionMois,
                                                      LocalDate dateExpirationProtection,
                                                      int enfantsAcharge,
                                                      String nationalite,
                                                      Clock clock) {
        validateInputs(dateOrdonnanceProtection, juridiction, dureeProtectionMois,
                enfantsAcharge, clock);

        LocalDate today = LocalDate.now(clock);

        // Recalcul de la date d'expiration effective si non fournie
        LocalDate dateExpirationEffective = dateExpirationProtection;
        if (dateExpirationEffective == null) {
            dateExpirationEffective = dateOrdonnanceProtection.plusMonths(dureeProtectionMois);
        }

        List<String> criteresValides = new ArrayList<>();
        List<String> criteresManquants = new ArrayList<>();

        // Critère 1 : ordonnance JAF présente
        criteresValides.add("Ordonnance de protection JAF rendue le " + dateOrdonnanceProtection);

        // Critère 2 : juridiction = JAF (heuristique : contient "JAF")
        if (juridiction != null && juridiction.toUpperCase().contains("JAF")) {
            criteresValides.add("Juridiction compétente : " + juridiction);
        } else {
            criteresManquants.add("La juridiction renseignée (" + juridiction
                    + ") ne mentionne pas explicitement le JAF — vérifier que l'ordonnance de "
                    + "protection émane bien du juge aux affaires familiales (Cciv 515-9).");
        }

        // Critère 3 : ordonnance non expirée
        boolean ordonnanceEnCours = !today.isAfter(dateExpirationEffective);
        if (ordonnanceEnCours) {
            criteresValides.add("Ordonnance en cours de validité (expire le "
                    + dateExpirationEffective + ").");
        } else {
            long moisDepuisExpiration = java.time.temporal.ChronoUnit.MONTHS.between(
                    dateExpirationEffective, today);
            if (moisDepuisExpiration <= MARGE_POST_EXPIRATION_MOIS) {
                criteresManquants.add("Ordonnance expirée le " + dateExpirationEffective
                        + " (depuis " + moisDepuisExpiration + " mois) — délai de tolérance "
                        + "encore ouvert (≤ 3 mois). Demander prolongation Cciv 515-12 al. 2.");
            } else {
                criteresManquants.add("Ordonnance expirée depuis plus de "
                        + MARGE_POST_EXPIRATION_MOIS + " mois (le " + dateExpirationEffective
                        + ") — demander une nouvelle ordonnance ou justifier de violences "
                        + "persistantes (L.425-9 étranger malade ou autre fondement).");
            }
        }

        // Verdict
        String verdict;
        if (ordonnanceEnCours
                && juridiction != null
                && juridiction.toUpperCase().contains("JAF")) {
            verdict = VERDICT_ELIGIBLE_PLEIN_DROIT;
        } else if (criteresManquants.size() == 1) {
            verdict = VERDICT_ELIGIBLE_SOUS_RESERVE;
        } else {
            verdict = VERDICT_NON_ELIGIBLE;
        }

        List<String> messages = buildMessages(verdict, enfantsAcharge, nationalite);
        String formule = buildFormule(verdict, dateOrdonnanceProtection, dateExpirationEffective,
                enfantsAcharge, nationalite);

        return new VictimeViolencesL4256Result(
                dateOrdonnanceProtection,
                juridiction,
                dureeProtectionMois,
                dateExpirationEffective,
                enfantsAcharge,
                nationalite,
                verdict,
                criteresValides,
                criteresManquants,
                DUREE_TITRE_SEJOUR_MOIS,
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    private static void validateInputs(LocalDate dateOrdonnanceProtection,
                                       String juridiction,
                                       int dureeProtectionMois,
                                       int enfantsAcharge,
                                       Clock clock) {
        if (dateOrdonnanceProtection == null) {
            throw new IllegalArgumentException("dateOrdonnanceProtection est requise");
        }
        LocalDate today = LocalDate.now(clock);
        if (dateOrdonnanceProtection.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateOrdonnanceProtection ne peut pas être dans le futur");
        }
        if (juridiction == null || juridiction.isBlank()) {
            throw new IllegalArgumentException("juridiction est requise");
        }
        if (dureeProtectionMois <= 0) {
            throw new IllegalArgumentException("dureeProtectionMois doit être > 0");
        }
        if (enfantsAcharge < 0) {
            throw new IllegalArgumentException("enfantsAcharge ne peut pas être négatif");
        }
    }

    private static List<String> buildMessages(String verdict, int enfantsAcharge,
                                              String nationalite) {
        List<String> messages = new ArrayList<>();
        messages.add("Carte de séjour mention 'vie privée et familiale' délivrée DE PLEIN DROIT à "
                + "l'étranger qui bénéficie d'une ordonnance de protection JAF (CESEDA L.425-6).");
        messages.add("Durée du titre : 1 an renouvelable (L.425-7). Renouvellement de plein droit "
                + "tant que l'ordonnance est en cours.");
        messages.add("Articulation avec l'ordonnance JAF (Cciv 515-9 à 515-13) : la durée de "
                + "l'ordonnance peut être prolongée si une procédure de divorce, séparation de "
                + "corps ou relative à l'autorité parentale est engagée (Cciv 515-12 al. 2).");
        messages.add("Pièces : ordonnance JAF, formulaire Cerfa 16108 ou demande spontanée, "
                + "documents identité, justificatifs de violences (mains courantes, dépôts de "
                + "plainte, certificats médicaux).");

        if (enfantsAcharge > 0) {
            messages.add(String.format(
                    "%d enfant(s) à charge — articuler avec l'autorité parentale + droit de "
                            + "l'enfant français (L.423-7) si applicable.", enfantsAcharge));
        }

        if (nationalite != null && !nationalite.isBlank()
                && nationalite.toLowerCase().contains("alg")) {
            messages.add("Nationalité algérienne — Accord franco-algérien du 27/12/1968 "
                    + "(régime spécial) prévaut sur CESEDA. Vérifier équivalence titre algérien "
                    + "art. 6 §5 et 7 bis.");
        }

        switch (verdict) {
            case VERDICT_ELIGIBLE_PLEIN_DROIT -> messages.add(
                    "Verdict : ÉLIGIBLE DE PLEIN DROIT — déposer la demande sans délai en "
                            + "préfecture (rendez-vous ANEF ou guichet selon département).");
            case VERDICT_ELIGIBLE_SOUS_RESERVE -> messages.add(
                    "Verdict : ÉLIGIBLE SOUS RÉSERVE — vérifier les critères manquants. "
                            + "Possibilité de demander prolongation de l'ordonnance JAF "
                            + "(Cciv 515-12 al. 2) avant dépôt en préfecture.");
            case VERDICT_NON_ELIGIBLE -> messages.add(
                    "Verdict : NON ÉLIGIBLE sur le fondement L.425-6 — explorer L.425-9 "
                            + "(étranger malade), L.423-7 (parent enfant français), L.435-1 "
                            + "(admission exceptionnelle au séjour) ou demande nouvelle ordonnance JAF.");
            default -> { /* unreachable */ }
        }
        return messages;
    }

    private static String buildFormule(String verdict, LocalDate dateOrdonnance,
                                       LocalDate dateExpiration, int enfantsAcharge,
                                       String nationalite) {
        return String.format(
                "Demande titre L.425-6 — ordonnance JAF du %s (expire le %s)%s%s — verdict %s, "
                        + "titre VPF 1 an renouvelable.",
                dateOrdonnance,
                dateExpiration,
                enfantsAcharge > 0 ? ", " + enfantsAcharge + " enfant(s) à charge" : "",
                nationalite != null && !nationalite.isBlank() ? " (nationalité " + nationalite + ")" : "",
                verdict);
    }
}
