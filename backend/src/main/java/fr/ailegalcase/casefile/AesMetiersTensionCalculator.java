package fr.ailegalcase.casefile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-IM-09-01 : calculateur pour l'Admission Exceptionnelle au Séjour (AES) au titre des
 * métiers en tension — voie expérimentale issue de la loi n° 2024-42 du 26 janvier 2024
 * (article 26) et codifiée à l'article L.435-4 du CESEDA.
 *
 * <p>Conditions cumulatives :
 * <ul>
 *   <li>Présence en France depuis au moins 3 ans à la date de la demande</li>
 *   <li>Au moins 12 mois d'activité salariée dans les 24 derniers mois</li>
 *   <li>Exercice d'un métier figurant sur la liste régionale des métiers en tension
 *       (arrêté 1er avril 2021 et suivants)</li>
 *   <li>Absence de menace pour l'ordre public</li>
 *   <li>Contrat de travail ou promesse d'embauche valide</li>
 * </ul>
 *
 * <p>Délai d'instruction : 6 mois à compter du dépôt de la demande (silence vaut rejet).
 *
 * <p>Outil <b>single-country FR</b> — pas d'équivalent standard en droit belge.
 */
public final class AesMetiersTensionCalculator {

    /** Durée minimale de présence en France (art. 26 loi 26/01/2024). */
    public static final int PRESENCE_MINIMALE_ANNEES = 3;

    /** Nombre minimum de mois d'activité salariée sur les 24 derniers mois. */
    public static final int MOIS_ACTIVITE_MINIMUM = 12;

    /** Fenêtre glissante de référence pour l'activité salariée. */
    public static final int FENETRE_MOIS = 24;

    /** Délai d'instruction de la demande (mois). */
    public static final int DELAI_INSTRUCTION_MOIS = 6;

    private static final String BASE_JURIDIQUE =
            "Loi n° 2024-42 du 26/01/2024 art. 26 + CESEDA L.435-4";

    private AesMetiersTensionCalculator() {
    }

    public static AesMetiersTensionResult compute(LocalDate dateEntreeFrance,
                                                  int moisActiviteSalarieeDernieres24Mois,
                                                  boolean metierEstEnTension,
                                                  String codeMetier,
                                                  boolean menaceOrdrePublic,
                                                  boolean contratOuPromesseValide,
                                                  LocalDate dateDepotDemande) {
        return compute(dateEntreeFrance, moisActiviteSalarieeDernieres24Mois,
                metierEstEnTension, codeMetier, menaceOrdrePublic,
                contratOuPromesseValide, dateDepotDemande,
                Clock.system(ZoneId.of("Europe/Paris")));
    }

    public static AesMetiersTensionResult compute(LocalDate dateEntreeFrance,
                                                  int moisActiviteSalarieeDernieres24Mois,
                                                  boolean metierEstEnTension,
                                                  String codeMetier,
                                                  boolean menaceOrdrePublic,
                                                  boolean contratOuPromesseValide,
                                                  LocalDate dateDepotDemande,
                                                  Clock clock) {
        validateInputs(dateEntreeFrance, moisActiviteSalarieeDernieres24Mois,
                dateDepotDemande, clock);

        LocalDate today = LocalDate.now(clock);

        boolean presence3Ans = !dateEntreeFrance.isAfter(today.minusYears(PRESENCE_MINIMALE_ANNEES));
        boolean activite12MoisOk = moisActiviteSalarieeDernieres24Mois >= MOIS_ACTIVITE_MINIMUM;

        boolean conditionsReunies = presence3Ans
                && activite12MoisOk
                && metierEstEnTension
                && !menaceOrdrePublic
                && contratOuPromesseValide;

        LocalDate dateEligibiliteAtteinte = presence3Ans
                ? null
                : dateEntreeFrance.plusYears(PRESENCE_MINIMALE_ANNEES);

        LocalDate dateExpirationInstruction = dateDepotDemande != null
                ? dateDepotDemande.plusMonths(DELAI_INSTRUCTION_MOIS)
                : null;

        List<String> criteresNonRemplis = buildCriteresNonRemplis(
                presence3Ans, activite12MoisOk, metierEstEnTension,
                menaceOrdrePublic, contratOuPromesseValide);

        List<String> messages = buildMessages(
                conditionsReunies, metierEstEnTension, dateEligibiliteAtteinte);

        String formule = buildFormule(conditionsReunies, presence3Ans, activite12MoisOk,
                metierEstEnTension, codeMetier, menaceOrdrePublic,
                contratOuPromesseValide, dateEligibiliteAtteinte,
                dateExpirationInstruction);

        return new AesMetiersTensionResult(
                dateEntreeFrance,
                moisActiviteSalarieeDernieres24Mois,
                metierEstEnTension,
                codeMetier,
                menaceOrdrePublic,
                contratOuPromesseValide,
                dateDepotDemande,
                presence3Ans,
                activite12MoisOk,
                conditionsReunies,
                criteresNonRemplis,
                dateEligibiliteAtteinte,
                dateExpirationInstruction,
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    private static void validateInputs(LocalDate dateEntreeFrance,
                                       int moisActiviteSalarieeDernieres24Mois,
                                       LocalDate dateDepotDemande,
                                       Clock clock) {
        if (dateEntreeFrance == null) {
            throw new IllegalArgumentException("dateEntreeFrance est requise");
        }
        LocalDate today = LocalDate.now(clock);
        if (dateEntreeFrance.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateEntreeFrance ne peut pas être dans le futur");
        }
        if (moisActiviteSalarieeDernieres24Mois < 0 || moisActiviteSalarieeDernieres24Mois > 24) {
            throw new IllegalArgumentException(
                    "moisActiviteSalarieeDernieres24Mois doit être compris entre 0 et 24");
        }
        if (dateDepotDemande != null && dateDepotDemande.isBefore(dateEntreeFrance)) {
            throw new IllegalArgumentException(
                    "dateDepotDemande ne peut pas être antérieure à dateEntreeFrance");
        }
    }

    private static List<String> buildCriteresNonRemplis(boolean presence3Ans,
                                                        boolean activite12MoisOk,
                                                        boolean metierEstEnTension,
                                                        boolean menaceOrdrePublic,
                                                        boolean contratOuPromesseValide) {
        List<String> criteres = new ArrayList<>();
        if (!presence3Ans) {
            criteres.add("Moins de 3 ans de présence en France");
        }
        if (!activite12MoisOk) {
            criteres.add("Moins de 12 mois d'activité salariée dans les 24 derniers mois");
        }
        if (!metierEstEnTension) {
            criteres.add("Métier ne figurant pas sur liste tension");
        }
        if (menaceOrdrePublic) {
            criteres.add("Menace ordre public présumée");
        }
        if (!contratOuPromesseValide) {
            criteres.add("Pas de contrat ou promesse valide");
        }
        return criteres;
    }

    private static List<String> buildMessages(boolean conditionsReunies,
                                              boolean metierEstEnTension,
                                              LocalDate dateEligibiliteAtteinte) {
        List<String> messages = new ArrayList<>();
        messages.add("Procédure expérimentale loi 26/01/2024, ouverte du 1er janvier 2024 "
                + "au 31 décembre 2026.");
        messages.add("Preuves à constituer : bulletins de paie (12+ derniers), contrat de "
                + "travail, promesse d'embauche éventuelle.");
        messages.add("En cas de rejet : possibilité d'AES voie humanitaire L.435-2 ou AES "
                + "L.435-1 liens personnels et familiaux — outils F-IM-09-02/03/04.");

        if (!metierEstEnTension) {
            messages.add("Vérifier l'arrêté consolidé de la région — métiers en tension mis "
                    + "à jour annuellement.");
        }
        if (dateEligibiliteAtteinte != null) {
            messages.add("Condition de présence de 3 ans atteinte le "
                    + dateEligibiliteAtteinte + " — dépôt possible à compter de cette date.");
        }
        if (conditionsReunies) {
            messages.add("Conditions réunies : titre délivré = carte de séjour temporaire "
                    + "\"salarié\" ou \"travailleur temporaire\".");
        }
        return messages;
    }

    private static String buildFormule(boolean conditionsReunies,
                                       boolean presence3Ans,
                                       boolean activite12MoisOk,
                                       boolean metierEstEnTension,
                                       String codeMetier,
                                       boolean menaceOrdrePublic,
                                       boolean contratOuPromesseValide,
                                       LocalDate dateEligibiliteAtteinte,
                                       LocalDate dateExpirationInstruction) {
        if (conditionsReunies) {
            String suffix = dateExpirationInstruction != null
                    ? " — délai d'instruction jusqu'au " + dateExpirationInstruction + " (silence vaut rejet)"
                    : "";
            String codeSuffix = codeMetier != null && !codeMetier.isBlank()
                    ? " (métier " + codeMetier + ")"
                    : "";
            return String.format(
                    "AES métier en tension (L.435-4) : conditions réunies%s%s.",
                    codeSuffix, suffix);
        }
        StringBuilder sb = new StringBuilder(
                "AES métier en tension (L.435-4) : conditions non réunies — ");
        List<String> parts = new ArrayList<>();
        parts.add("présence 3 ans " + (presence3Ans ? "OK" : "KO"));
        parts.add("12 mois activité " + (activite12MoisOk ? "OK" : "KO"));
        parts.add("métier tension " + (metierEstEnTension ? "OK" : "KO"));
        parts.add("ordre public " + (menaceOrdrePublic ? "KO" : "OK"));
        parts.add("contrat " + (contratOuPromesseValide ? "OK" : "KO"));
        sb.append(String.join(", ", parts)).append(".");
        if (dateEligibiliteAtteinte != null) {
            sb.append(" Éligibilité atteinte le ").append(dateEligibiliteAtteinte).append(".");
        }
        return sb.toString();
    }
}
