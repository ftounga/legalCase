package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-214-37 : analyseur d'une interdiction du territoire français (ITF)
 * prononcée par un juge pénal comme peine complémentaire (C. pén. 131-30 à
 * 131-30-2). Calcule l'échéance du relèvement (5 ans minimum, C. pén.
 * 131-30-1 / 702-1), les voies de recours pénales et les conditions de la
 * requête en relèvement. Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Distinction essentielle (invariant CLAUDE.md — un outil = une situation
 * métier) : l'ITF (cet outil) est une <b>peine complémentaire</b> prononcée par
 * un <b>juge pénal</b> (C. pén.). L'IRTF (interdiction de retour, F-IM-20) est
 * une <b>mesure administrative</b> prononcée par le <b>préfet</b> (L. 612-6
 * CESEDA). Deux régimes, deux outils.
 *
 * <p>Sources :
 * <ul>
 *   <li>C. pén. 131-30 à 131-30-2 — ITF peine complémentaire ;</li>
 *   <li>C. pén. 131-30-1 / 702-1 — requête en relèvement (5 ans minimum) ;</li>
 *   <li>C. pr. pén. 380-1 — appel correctionnel, délai 10 j ;</li>
 *   <li>C. pr. pén. 568 — pourvoi en cassation criminelle, délai 5 j ;</li>
 *   <li>L. 631-3 CESEDA — lien entre ITF et droit des étrangers ;</li>
 *   <li>Cass. crim. 14 sept. 2016, n° 16-80.161 — conditions relèvement ITF.</li>
 * </ul>
 */
public final class ItfJudiciaireAnalyzer {

    public static final int INFRACTION_PRINCIPALE_MAX_LENGTH = 200;

    /** Délai minimum (en années) avant recevabilité d'une requête en relèvement (C. pén. 131-30-1 / 702-1). */
    public static final int DELAI_RELEVE_ANNEES = 5;

    /** Délai d'appel correctionnel (C. pr. pén. 380-1). */
    public static final int DELAI_APPEL_JOURS = 10;

    /** Délai de pourvoi en cassation criminelle (C. pr. pén. 568). */
    public static final int DELAI_CASSATION_JOURS = 5;

    private static final String BASE_JURIDIQUE =
            "C. pén. 131-30 à 131-30-2 (interdiction du territoire français, peine "
                    + "complémentaire) ; C. pén. 131-30-1 et 702-1 (requête en relèvement, "
                    + "5 ans minimum) ; C. pr. pén. 380-1 (appel correctionnel, 10 j) ; "
                    + "C. pr. pén. 568 (pourvoi en cassation criminelle, 5 j) ; "
                    + "L. 631-3 CESEDA ; Cass. crim. 14 sept. 2016, n° 16-80.161 (conditions "
                    + "de relèvement de l'ITF)";

    private static final String DISTINCTION_ITF_VS_IRTF =
            "ITF (cet outil) = interdiction du territoire français, PEINE COMPLÉMENTAIRE "
                    + "prononcée par un JUGE PÉNAL (C. pén. 131-30). Contestation par les "
                    + "voies de recours pénales (appel, cassation) puis relèvement (C. pén. "
                    + "702-1). À ne pas confondre avec l'IRTF (interdiction de retour sur le "
                    + "territoire français), MESURE ADMINISTRATIVE prononcée par le PRÉFET "
                    + "(L. 612-6 CESEDA), contestable devant le tribunal administratif — "
                    + "couverte par l'outil F-IM-20. Deux régimes juridiques distincts.";

    private static final List<String> REQUIS_RELEVE = List.of(
            "Bonne conduite et absence de nouvelle condamnation depuis la décision (gage de réinsertion)",
            "Intégration en France : ressources stables, emploi, logement, durée de présence",
            "Situation familiale : attaches familiales en France (conjoint, enfants), vie privée et familiale (art. 8 CEDH)",
            "Évolution de la situation personnelle depuis la condamnation (cf. Cass. crim. 14 sept. 2016, n° 16-80.161)");

    private ItfJudiciaireAnalyzer() {
    }

    /** Surcharge utilisant la date du jour système comme référence. */
    public static ItfJudiciaireResult analyze(LocalDate dateCondamnation,
                                              int dureeITFAnnees,
                                              String infractionPrincipale,
                                              boolean condamnationDefinitive,
                                              LocalDate dateEcheanceRecoursPenal) {
        return analyze(dateCondamnation, dureeITFAnnees, infractionPrincipale,
                condamnationDefinitive, dateEcheanceRecoursPenal, LocalDate.now());
    }

    /**
     * Analyse l'ITF et détermine échéance de relèvement, voies de recours et statut.
     *
     * @param dateEcheanceRecoursPenal date d'expiration du délai de recours pénal
     *        (appel) — optionnelle ; si absente, déduite de la date de condamnation + 10 j.
     * @param today date de référence injectée (testabilité).
     */
    public static ItfJudiciaireResult analyze(LocalDate dateCondamnation,
                                              int dureeITFAnnees,
                                              String infractionPrincipale,
                                              boolean condamnationDefinitive,
                                              LocalDate dateEcheanceRecoursPenal,
                                              LocalDate today) {
        validate(dateCondamnation, dureeITFAnnees, infractionPrincipale, today);

        LocalDate dateEcheanceReleve = dateCondamnation.plusYears(DELAI_RELEVE_ANNEES);

        // Échéance du recours pénal : fournie ou déduite (condamnation + 10 j d'appel).
        LocalDate echeanceAppel = dateEcheanceRecoursPenal != null
                ? dateEcheanceRecoursPenal
                : dateCondamnation.plusDays(DELAI_APPEL_JOURS);

        ItfJudiciaireStatut statut = computeStatut(
                condamnationDefinitive, echeanceAppel, dateEcheanceReleve, today);

        List<String> voiesRecours = buildVoiesRecours(statut);

        return new ItfJudiciaireResult(
                dateCondamnation,
                dureeITFAnnees,
                infractionPrincipale,
                condamnationDefinitive,
                dateEcheanceReleve,
                voiesRecours,
                REQUIS_RELEVE,
                DISTINCTION_ITF_VS_IRTF,
                statut,
                BASE_JURIDIQUE);
    }

    private static ItfJudiciaireStatut computeStatut(boolean condamnationDefinitive,
                                                     LocalDate echeanceAppel,
                                                     LocalDate dateEcheanceReleve,
                                                     LocalDate today) {
        if (!condamnationDefinitive) {
            // Condamnation non définitive : l'appel reste possible tant que le délai court.
            return !today.isAfter(echeanceAppel)
                    ? ItfJudiciaireStatut.APPEL_POSSIBLE
                    : ItfJudiciaireStatut.RECOURS_PRESCRIT;
        }
        // Condamnation définitive : relèvement possible une fois le délai de 5 ans écoulé.
        return !today.isBefore(dateEcheanceReleve)
                ? ItfJudiciaireStatut.RELEVE_POSSIBLE
                : ItfJudiciaireStatut.EN_COURS_PURGE;
    }

    private static List<String> buildVoiesRecours(ItfJudiciaireStatut statut) {
        return switch (statut) {
            case APPEL_POSSIBLE -> List.of(
                    "Appel correctionnel devant la cour d'appel — délai de "
                            + DELAI_APPEL_JOURS + " jours à compter du prononcé (C. pr. pén. 380-1)",
                    "Pourvoi en cassation devant la chambre criminelle — délai de "
                            + DELAI_CASSATION_JOURS + " jours à compter de l'arrêt d'appel (C. pr. pén. 568)");
            case RECOURS_PRESCRIT -> List.of(
                    "Délai d'appel (" + DELAI_APPEL_JOURS + " j, C. pr. pén. 380-1) expiré : voies "
                            + "de recours ordinaires closes",
                    "Requête en relèvement de l'ITF recevable après un délai minimum de "
                            + DELAI_RELEVE_ANNEES + " ans (C. pén. 702-1) — vérifier la date d'échéance");
            case RELEVE_POSSIBLE -> List.of(
                    "Requête en relèvement de l'ITF devant la juridiction qui a prononcé la peine "
                            + "(C. pén. 702-1) — délai de " + DELAI_RELEVE_ANNEES + " ans écoulé, requête recevable",
                    "Demande de suspension / aménagement à invoquer au regard de la vie privée et "
                            + "familiale (art. 8 CEDH)");
            case EN_COURS_PURGE -> List.of(
                    "Condamnation définitive : voies de recours ordinaires épuisées",
                    "Requête en relèvement (C. pén. 702-1) non encore recevable — délai de "
                            + DELAI_RELEVE_ANNEES + " ans à compter de la condamnation non écoulé (cf. date "
                            + "d'échéance du relèvement)");
        };
    }

    private static void validate(LocalDate dateCondamnation,
                                 int dureeITFAnnees,
                                 String infractionPrincipale,
                                 LocalDate today) {
        if (dateCondamnation == null) {
            throw new IllegalArgumentException("dateCondamnation est requise");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        if (dateCondamnation.isAfter(today)) {
            throw new IllegalArgumentException("dateCondamnation ne peut pas être dans le futur");
        }
        if (dureeITFAnnees < 0) {
            throw new IllegalArgumentException("dureeITFAnnees ne peut pas être négative");
        }
        if (infractionPrincipale != null
                && infractionPrincipale.length() > INFRACTION_PRINCIPALE_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "infractionPrincipale ne peut pas dépasser "
                            + INFRACTION_PRINCIPALE_MAX_LENGTH + " caractères");
        }
    }
}
