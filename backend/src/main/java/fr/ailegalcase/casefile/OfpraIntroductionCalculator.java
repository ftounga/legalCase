package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SF-214-17 — calculateur de la procédure d'introduction de la demande d'asile à
 * l'OFPRA via le GUDA (guichet unique demande d'asile) et l'ADA (allocation
 * demandeur d'asile) : délai d'introduction de 90 jours à compter de l'arrivée en
 * France (art. R. 521-1 et s. CESEDA). Outil single-country FR.
 *
 * <p>Le délai est calculé en jours calendaires : {@code dateArriveeEnFrance + 90 j}.
 * Le statut bascule en URGENT lorsqu'il reste 21 jours ou moins, en EXPIRE une
 * fois l'échéance dépassée.
 *
 * <p>Sources :
 * <ul>
 *   <li>L. 521-1 à L. 521-15 CESEDA — demande d'asile, enregistrement ;</li>
 *   <li>L. 521-7 CESEDA — ADA (allocation demandeur d'asile) ;</li>
 *   <li>R. 521-1 à R. 521-14 CESEDA — procédure GUDA, délai 90 jours ;</li>
 *   <li>L. 531-24 à L. 531-31 CESEDA — procédure accélérée (pays sûrs, fraude) ;</li>
 *   <li>CE 5 juillet 2013 n° 370099 — conditions d'enregistrement.</li>
 * </ul>
 */
public final class OfpraIntroductionCalculator {

    /** Délai d'introduction de la demande d'asile à l'OFPRA — 90 jours (R. 521-1 et s.). */
    public static final int DELAI_INTRODUCTION_JOURS = 90;

    /** Seuil (inclusif) en jours restants en deçà duquel le statut bascule en URGENT. */
    static final int SEUIL_URGENT_JOURS = 21;

    /** Borne haute d'ancienneté de la date d'arrivée acceptée — 36 mois. */
    static final int MAX_ANCIENNETE_ARRIVEE_MOIS = 36;

    /**
     * Liste indicative des pays d'origine sûrs (L. 531-24). La liste effective est
     * fixée par le conseil d'administration de l'OFPRA et évolue par décision /
     * décret — cette constante doit être actualisée en conséquence.
     */
    // liste pays sûrs OFPRA — à actualiser par CA/décret
    static final Set<String> PAYS_SURS = Set.of(
            "ALBANIE",
            "BOSNIE-HERZEGOVINE",
            "CAP-VERT",
            "GEORGIE",
            "GHANA",
            "INDE",
            "KOSOVO",
            "MACEDOINE DU NORD",
            "MAURICE",
            "MOLDAVIE",
            "MONGOLIE",
            "MONTENEGRO",
            "SENEGAL",
            "SERBIE"
    );

    private static final String BASE_JURIDIQUE =
            "Art. R. 521-1 et s. CESEDA (procédure GUDA, délai d'introduction 90 jours) ; "
                    + "L. 521-1 à L. 521-15 CESEDA (demande d'asile, enregistrement) ; "
                    + "L. 521-7 CESEDA (ADA) ; L. 531-24 et s. CESEDA (procédure accélérée, pays sûrs)";

    private OfpraIntroductionCalculator() {
    }

    /** Surcharge utilisant la date du jour système (UTC) comme référence. */
    public static OfpraIntroductionResult compute(LocalDate dateArriveeEnFrance,
                                                  Boolean passageGudaEffectue,
                                                  LocalDate datePassageGuda,
                                                  Boolean adaRequise,
                                                  String paysOrigine) {
        return compute(dateArriveeEnFrance, passageGudaEffectue, datePassageGuda, adaRequise,
                paysOrigine, LocalDate.now());
    }

    /**
     * Calcule le délai, le statut et le plan d'action de l'introduction OFPRA.
     *
     * @param today date de référence injectée (testabilité) — typiquement {@code LocalDate.now()}.
     */
    public static OfpraIntroductionResult compute(LocalDate dateArriveeEnFrance,
                                                  Boolean passageGudaEffectue,
                                                  LocalDate datePassageGuda,
                                                  Boolean adaRequise,
                                                  String paysOrigine,
                                                  LocalDate today) {
        validate(dateArriveeEnFrance, datePassageGuda, today);

        boolean gudaEffectue = Boolean.TRUE.equals(passageGudaEffectue);
        boolean ada = Boolean.TRUE.equals(adaRequise);

        LocalDate dateEcheanceIntroduction = dateArriveeEnFrance.plusDays(DELAI_INTRODUCTION_JOURS);
        long joursRestants = ChronoUnit.DAYS.between(today, dateEcheanceIntroduction);

        OfpraIntroductionStatut statut;
        if (joursRestants <= 0) {
            statut = OfpraIntroductionStatut.EXPIRE;
        } else if (joursRestants <= SEUIL_URGENT_JOURS) {
            statut = OfpraIntroductionStatut.URGENT;
        } else {
            statut = OfpraIntroductionStatut.A_DEPOSER;
        }

        boolean procedureAccelereeRisque = estPaysSur(paysOrigine);

        List<String> etapes = buildEtapes(gudaEffectue, ada);
        List<String> pieces = buildPieces();
        String recommandation = buildRecommandation(statut, gudaEffectue, procedureAccelereeRisque);

        return new OfpraIntroductionResult(
                dateArriveeEnFrance,
                gudaEffectue,
                datePassageGuda,
                ada,
                paysOrigine,
                dateEcheanceIntroduction,
                joursRestants,
                statut,
                procedureAccelereeRisque,
                etapes,
                pieces,
                recommandation,
                BASE_JURIDIQUE
        );
    }

    /** Normalise un libellé de pays et teste l'appartenance à la liste des pays sûrs. */
    static boolean estPaysSur(String paysOrigine) {
        if (paysOrigine == null || paysOrigine.isBlank()) {
            return false;
        }
        String normalise = normalize(paysOrigine);
        return PAYS_SURS.contains(normalise);
    }

    private static String normalize(String value) {
        String upper = value.trim().toUpperCase(Locale.ROOT);
        // Suppression des diacritiques pour tolérer « Géorgie » → « GEORGIE ».
        String sansAccents = java.text.Normalizer.normalize(upper, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sansAccents;
    }

    private static List<String> buildEtapes(boolean gudaEffectue, boolean adaRequise) {
        List<String> etapes = new ArrayList<>();
        etapes.add("1. Passage au GUDA (guichet unique demande d'asile) — enregistrement de la demande"
                + (gudaEffectue ? " [déjà effectué]" : ""));
        etapes.add("2. Délivrance de l'attestation de demande d'asile / récépissé");
        if (adaRequise) {
            etapes.add("3. Demande d'allocation pour demandeur d'asile (ADA) auprès de l'OFII (L. 521-7)");
        }
        etapes.add((adaRequise ? "4" : "3") + ". Préparation du dossier OFPRA (formulaire, récit, pièces)");
        etapes.add((adaRequise ? "5" : "4") + ". Dépôt du dossier OFPRA dans le délai de 90 jours");
        return etapes;
    }

    private static List<String> buildPieces() {
        return List.of(
                "Formulaire de demande d'asile OFPRA",
                "Documents d'état civil et d'identité disponibles",
                "Photographies d'identité aux normes",
                "Exposé écrit et circonstancié des persécutions / craintes (récit d'asile)"
        );
    }

    private static String buildRecommandation(OfpraIntroductionStatut statut,
                                              boolean gudaEffectue,
                                              boolean procedureAccelereeRisque) {
        StringBuilder sb = new StringBuilder();
        switch (statut) {
            case A_DEPOSER -> sb.append(gudaEffectue
                    ? "Passage GUDA effectué — préparer et déposer le dossier OFPRA dans le délai de 90 jours."
                    : "Effectuer le passage au GUDA puis déposer le dossier OFPRA dans le délai de 90 jours.");
            case URGENT -> sb.append("Échéance des 90 jours imminente — finaliser et déposer le dossier "
                    + "OFPRA sans délai pour éviter une demande tardive.");
            case EXPIRE -> sb.append("Délai des 90 jours dépassé — déposer la demande au plus vite : "
                    + "l'introduction tardive expose à un examen en procédure accélérée (L. 531-27) "
                    + "mais ne ferme pas le droit de demander l'asile.");
        }
        if (procedureAccelereeRisque) {
            sb.append(" Pays d'origine en liste des pays sûrs (L. 531-24) — anticiper un examen "
                    + "en procédure accélérée (délais réduits, récit d'asile particulièrement étayé).");
        }
        return sb.toString();
    }

    private static void validate(LocalDate dateArriveeEnFrance,
                                 LocalDate datePassageGuda,
                                 LocalDate today) {
        if (dateArriveeEnFrance == null) {
            throw new IllegalArgumentException("dateArriveeEnFrance est requise");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        if (dateArriveeEnFrance.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateArriveeEnFrance ne peut pas être dans le futur");
        }
        if (dateArriveeEnFrance.isBefore(today.minusMonths(MAX_ANCIENNETE_ARRIVEE_MOIS))) {
            throw new IllegalArgumentException(
                    "dateArriveeEnFrance est antérieure à " + MAX_ANCIENNETE_ARRIVEE_MOIS
                            + " mois — introduction OFPRA hors périmètre de cet outil");
        }
        if (datePassageGuda != null && datePassageGuda.isBefore(dateArriveeEnFrance)) {
            throw new IllegalArgumentException(
                    "datePassageGuda ne peut pas être antérieure à dateArriveeEnFrance");
        }
    }
}
