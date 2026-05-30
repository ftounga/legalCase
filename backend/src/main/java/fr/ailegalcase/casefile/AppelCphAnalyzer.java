package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-218-01 : analyseur de l'appel d'un jugement du Conseil de prud'hommes
 * (CPH) devant la chambre sociale de la Cour d'appel. Calcule le délai d'appel
 * d'un mois (art. 538 CPC ; R. 1461-1 CPC), le verdict de recevabilité et la
 * checklist des formalités de l'appel social (déclaration d'appel RPVA, chefs de
 * jugement critiqués, représentation obligatoire, procédure orale, constitution
 * de l'intimé). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Distinction essentielle (invariant CLAUDE.md — un outil = une situation
 * métier) : cet outil traite la <b>voie d'appel</b> ouverte contre un jugement
 * CPH <b>susceptible d'appel</b>. Lorsque le jugement est rendu en <b>dernier
 * ressort</b> (taux de compétence atteint, R. 1462-1 CPC), l'appel est fermé et
 * seul le <b>pourvoi en cassation</b> (chambre sociale de la Cour de cassation,
 * outil F-DT-87) est ouvert. La phase de conciliation prud'homale (BCO, F-DT-84)
 * et la procédure de première instance sont des situations distinctes.
 *
 * <p>Sources :
 * <ul>
 *   <li>R. 1461-1 et s. CPC — appel des décisions prud'homales ;</li>
 *   <li>art. 538 CPC — délai d'appel de droit commun : 1 mois ;</li>
 *   <li>art. 901 CPC — déclaration d'appel et chefs de jugement critiqués ;</li>
 *   <li>art. 946 CPC — procédure orale en appel social ;</li>
 *   <li>R. 1461-2 CPC — représentation obligatoire : avocat ou défenseur syndical ;</li>
 *   <li>R. 1462-1 CPC — taux de compétence en dernier ressort.</li>
 * </ul>
 */
public final class AppelCphAnalyzer {

    /** Délai d'appel d'un jugement CPH (art. 538 CPC ; R. 1461-1 CPC). */
    public static final int DELAI_APPEL_MOIS = 1;

    /** Seuil (jours calendaires) en deçà duquel le délai d'appel est qualifié d'urgent. */
    public static final int SEUIL_URGENCE_JOURS = 7;

    private static final String BASE_JURIDIQUE =
            "R. 1461-1 et s. CPC (appel des décisions prud'homales) ; art. 538 CPC "
                    + "(délai d'appel d'un mois) ; art. 901 CPC (déclaration d'appel et "
                    + "chefs de jugement critiqués) ; R. 1461-2 CPC (représentation "
                    + "obligatoire — avocat ou défenseur syndical) ; art. 946 CPC "
                    + "(procédure orale en appel social) ; R. 1462-1 CPC (taux de "
                    + "compétence en dernier ressort)";

    private static final String RENVOI_POURVOI_CASSATION =
            "Jugement rendu en dernier ressort (R. 1462-1 CPC) : la voie de l'appel "
                    + "est fermée. Seul un pourvoi en cassation devant la chambre sociale "
                    + "de la Cour de cassation est ouvert (délai de 2 mois, art. 612 CPC) "
                    + "— voir l'outil pourvoi en cassation sociale (F-DT-87).";

    private AppelCphAnalyzer() {
    }

    /** Surcharge utilisant la date du jour système comme référence. */
    public static AppelCphResult analyze(LocalDate dateNotificationJugement,
                                         AppelCphPartieAppelante partieAppelante,
                                         AppelCphModeNotification modeNotification,
                                         AppelCphRepresentation representationConstituee,
                                         boolean jugementEnDernierRessort) {
        return analyze(dateNotificationJugement, partieAppelante, modeNotification,
                representationConstituee, jugementEnDernierRessort, LocalDate.now());
    }

    /**
     * Analyse l'appel CPH et détermine la date limite, le verdict et la checklist.
     *
     * @param today date de référence injectée (testabilité).
     */
    public static AppelCphResult analyze(LocalDate dateNotificationJugement,
                                         AppelCphPartieAppelante partieAppelante,
                                         AppelCphModeNotification modeNotification,
                                         AppelCphRepresentation representationConstituee,
                                         boolean jugementEnDernierRessort,
                                         LocalDate today) {
        validate(dateNotificationJugement, partieAppelante, today);

        AppelCphModeNotification mode =
                modeNotification != null ? modeNotification : AppelCphModeNotification.SIGNIFICATION;
        AppelCphRepresentation representation =
                representationConstituee != null ? representationConstituee : AppelCphRepresentation.AUCUNE;

        LocalDate dateLimiteAppel = dateNotificationJugement.plusMonths(DELAI_APPEL_MOIS);
        long joursRestants = ChronoUnit.DAYS.between(today, dateLimiteAppel);

        AppelCphVerdict verdict = computeVerdict(jugementEnDernierRessort, joursRestants);

        List<AppelCphChecklistItem> checklist = buildChecklist(representation);

        String renvoi = jugementEnDernierRessort ? RENVOI_POURVOI_CASSATION : null;

        return new AppelCphResult(
                dateNotificationJugement,
                partieAppelante,
                mode,
                representation,
                jugementEnDernierRessort,
                dateLimiteAppel,
                joursRestants,
                verdict,
                checklist,
                renvoi,
                BASE_JURIDIQUE);
    }

    private static AppelCphVerdict computeVerdict(boolean jugementEnDernierRessort,
                                                  long joursRestants) {
        if (jugementEnDernierRessort) {
            return AppelCphVerdict.VOIE_FERMEE;
        }
        if (joursRestants < 0) {
            return AppelCphVerdict.DELAI_EXPIRE;
        }
        if (joursRestants <= SEUIL_URGENCE_JOURS) {
            return AppelCphVerdict.DELAI_URGENT;
        }
        return AppelCphVerdict.DELAI_OUVERT;
    }

    private static List<AppelCphChecklistItem> buildChecklist(AppelCphRepresentation representation) {
        List<AppelCphChecklistItem> items = new ArrayList<>();

        boolean representationManquante = representation == AppelCphRepresentation.AUCUNE;
        items.add(new AppelCphChecklistItem(
                representationManquante
                        ? "Constituer un représentant : la représentation est OBLIGATOIRE en appel "
                                + "social (avocat ou défenseur syndical) — aucune représentation n'est "
                                + "actuellement constituée"
                        : "Représentation obligatoire constituée (avocat ou défenseur syndical)",
                true,
                representationManquante,
                "R. 1461-2 CPC"));

        items.add(new AppelCphChecklistItem(
                "Déposer la déclaration d'appel par voie électronique via le RPVA dans le délai d'un mois",
                true,
                false,
                "art. 538 CPC ; R. 1461-1 CPC ; art. 930-1 CPC (communication électronique)"));

        items.add(new AppelCphChecklistItem(
                "Mentionner expressément les chefs de jugement critiqués dans la déclaration d'appel "
                        + "(à défaut, l'appel ne défère que les chefs ultérieurement visés)",
                true,
                false,
                "art. 901 CPC"));

        items.add(new AppelCphChecklistItem(
                "Préparer la procédure orale : l'affaire est instruite et plaidée oralement devant la "
                        + "chambre sociale de la Cour d'appel",
                true,
                false,
                "art. 946 CPC"));

        items.add(new AppelCphChecklistItem(
                "Anticiper la constitution de l'intimé et l'échange des conclusions / pièces",
                false,
                false,
                "R. 1461-1 et s. CPC ; art. 946 CPC"));

        return items;
    }

    private static void validate(LocalDate dateNotificationJugement,
                                 AppelCphPartieAppelante partieAppelante,
                                 LocalDate today) {
        if (dateNotificationJugement == null) {
            throw new IllegalArgumentException("dateNotificationJugement est requise");
        }
        if (partieAppelante == null) {
            throw new IllegalArgumentException("partieAppelante est requise");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        if (dateNotificationJugement.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateNotificationJugement ne peut pas être dans le futur");
        }
    }
}
