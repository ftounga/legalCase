package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * SF-214-33 — calculateur des délais d'appel devant la Cour administrative d'appel
 * (CAA) et de cassation devant le Conseil d'État (CE) dans les contentieux des
 * étrangers, après un jugement du tribunal administratif (TA). Outil single-country FR.
 *
 * <p>Délai d'appel CAA :
 * <ul>
 *   <li>droit commun : {@code dateJugementTA + 1 mois} (art. R. 811-2 CJA) ;</li>
 *   <li>OQTF sans délai de départ volontaire : {@code dateJugementTA + 15 jours}
 *       (délai spécial du contentieux de l'éloignement).</li>
 * </ul>
 *
 * <p>Le délai de cassation devant le CE est de 2 mois à compter de la notification de
 * l'arrêt de la CAA (art. R. 821-1 CJA) — donné à titre informatif, le point de départ
 * (date de l'arrêt CAA) étant ultérieur et non connu à ce stade.
 *
 * <p>En matière d'OQTF, le pourvoi en cassation au CE est soumis à la procédure
 * préalable d'admission (art. L. 821-2 CJA) : {@code filtrePourvoisCassation = true}.
 *
 * <p>Le statut bascule en URGENT lorsqu'il reste 15 jours ou moins, et PRESCRIT
 * lorsque le délai est dépassé (joursRestants ≤ 0).
 *
 * <p>Sources :
 * <ul>
 *   <li>CJA L. 811-1 et R. 811-2 — appel CAA, délai d'un mois ;</li>
 *   <li>CJA L. 821-1 et R. 821-1 — cassation CE, délai de deux mois ;</li>
 *   <li>CJA L. 821-2 — admission préalable des pourvois (filtre OQTF) ;</li>
 *   <li>CESEDA L. 614-6 et s. — délais spéciaux du contentieux de l'éloignement.</li>
 * </ul>
 */
public final class AppelCaaCassationCalculator {

    /** Délai d'appel CAA de droit commun — 1 mois calendaire (art. R. 811-2 CJA). */
    public static final int DELAI_APPEL_DROIT_COMMUN_MOIS = 1;

    /** Délai spécial d'appel CAA en OQTF sans délai — 15 jours calendaires. */
    public static final int DELAI_APPEL_OQTF_SPECIAL_JOURS = 15;

    /** Délai de cassation devant le CE — 2 mois (art. R. 821-1 CJA), à titre informatif. */
    public static final int DELAI_CASSATION_CE_MOIS = 2;

    /** Seuil (inclusif) en jours restants en deçà duquel le statut bascule en URGENT. */
    static final int SEUIL_URGENT_JOURS = 15;

    private static final String BASE_JURIDIQUE =
            "Art. L. 811-1 et R. 811-2 CJA (appel CAA, délai d'un mois) ; "
                    + "art. L. 821-1 et R. 821-1 CJA (cassation CE, délai de deux mois) ; "
                    + "art. L. 821-2 CJA (admission préalable des pourvois) ; "
                    + "CESEDA L. 614-6 et s. (délais spéciaux du contentieux de l'éloignement)";

    private static final List<String> MOTIFS_APPEL = List.of(
            "Erreur de droit (mauvaise application ou interprétation d'une règle de droit)",
            "Dénaturation des pièces du dossier ou des faits",
            "Vice de procédure (irrégularité affectant le jugement de première instance)",
            "Moyen d'ordre public (à soulever d'office, ex. incompétence, défaut de motivation)");

    private AppelCaaCassationCalculator() {
    }

    /** Surcharge utilisant la date du jour système comme référence. */
    public static AppelCaaCassationResult compute(LocalDate dateJugementTA,
                                                  AppelCaaCassationTypeDecisionEnum typeDecisionTA,
                                                  AppelCaaCassationTypeContentieuxEnum typeContentieux,
                                                  Boolean delaiSpecialOQTF) {
        return compute(dateJugementTA, typeDecisionTA, typeContentieux, delaiSpecialOQTF,
                LocalDate.now());
    }

    /**
     * Calcule les délais d'appel CAA / cassation CE et le statut associé.
     *
     * @param today date de référence injectée (testabilité) — typiquement {@code LocalDate.now()}.
     */
    public static AppelCaaCassationResult compute(LocalDate dateJugementTA,
                                                  AppelCaaCassationTypeDecisionEnum typeDecisionTA,
                                                  AppelCaaCassationTypeContentieuxEnum typeContentieux,
                                                  Boolean delaiSpecialOQTF,
                                                  LocalDate today) {
        validate(dateJugementTA, typeDecisionTA, typeContentieux, today);

        boolean special = Boolean.TRUE.equals(delaiSpecialOQTF);

        LocalDate dateEcheanceAppelCaa = special
                ? dateJugementTA.plusDays(DELAI_APPEL_OQTF_SPECIAL_JOURS)
                : dateJugementTA.plusMonths(DELAI_APPEL_DROIT_COMMUN_MOIS);

        long joursRestants = ChronoUnit.DAYS.between(today, dateEcheanceAppelCaa);

        AppelCaaCassationStatut statut;
        if (joursRestants <= 0) {
            statut = AppelCaaCassationStatut.PRESCRIT;
        } else if (joursRestants <= SEUIL_URGENT_JOURS) {
            statut = AppelCaaCassationStatut.URGENT;
        } else {
            statut = AppelCaaCassationStatut.APPEL_POSSIBLE;
        }

        boolean filtrePourvois = typeContentieux == AppelCaaCassationTypeContentieuxEnum.OQTF;

        String courAppel = "CAA territorialement compétente selon le ressort du TF ayant statué "
                + "(art. R. 221-3 et s. CJA) — vérifier le siège du TA dans le jugement attaqué";

        String recommandation = buildRecommandation(statut, special, filtrePourvois);

        return new AppelCaaCassationResult(
                dateJugementTA,
                typeDecisionTA,
                typeContentieux,
                special,
                dateEcheanceAppelCaa,
                joursRestants,
                courAppel,
                MOTIFS_APPEL,
                filtrePourvois,
                DELAI_CASSATION_CE_MOIS,
                statut,
                recommandation,
                BASE_JURIDIQUE
        );
    }

    private static String buildRecommandation(AppelCaaCassationStatut statut,
                                              boolean special,
                                              boolean filtrePourvois) {
        String base = switch (statut) {
            case APPEL_POSSIBLE -> special
                    ? "Délai spécial d'appel de 15 jours (OQTF sans délai) : interjeter appel "
                            + "devant la CAA sans tarder en motivant les moyens (erreur de droit, "
                            + "dénaturation, vice de procédure)."
                    : "Délai d'appel de droit commun d'un mois (art. R. 811-2 CJA) : préparer et "
                            + "déposer la requête d'appel devant la CAA en motivant les moyens.";
            case URGENT -> "Échéance d'appel imminente (≤ 15 jours) — déposer la requête d'appel "
                    + "devant la CAA en priorité, quitte à compléter les moyens par mémoire ultérieur.";
            case PRESCRIT -> "Délai d'appel devant la CAA expiré : l'appel est en principe "
                    + "irrecevable — vérifier la régularité de la notification du jugement (mentions "
                    + "des voies et délais de recours) avant de renoncer.";
        };
        if (filtrePourvois) {
            base += " En cas de pourvoi ultérieur, noter que la cassation devant le CE est soumise "
                    + "à l'admission préalable du pourvoi (art. L. 821-2 CJA) en matière d'OQTF.";
        }
        return base;
    }

    private static void validate(LocalDate dateJugementTA,
                                 AppelCaaCassationTypeDecisionEnum typeDecisionTA,
                                 AppelCaaCassationTypeContentieuxEnum typeContentieux,
                                 LocalDate today) {
        if (dateJugementTA == null) {
            throw new IllegalArgumentException("dateJugementTA est requise");
        }
        if (typeDecisionTA == null) {
            throw new IllegalArgumentException("typeDecisionTA est requis");
        }
        if (typeContentieux == null) {
            throw new IllegalArgumentException("typeContentieux est requis");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        if (dateJugementTA.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateJugementTA ne peut pas être dans le futur");
        }
    }
}
