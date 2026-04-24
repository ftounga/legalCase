package fr.ailegalcase.casefile;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-IM-08-03 : calculateur pour OQTF <b>sans</b> délai de départ volontaire (art. L.731-1 CESEDA).
 *
 * <p>Procédure d'<b>extrême urgence</b>, distincte de l'OQTF avec DDV (SF-IM-08-01) :
 * <ul>
 *   <li>Recours devant juge unique TA dans les <b>48 heures</b> de notification (L.614-8)</li>
 *   <li>Audience dans les <b>96 heures</b> (4 jours) du recours (R.776-26)</li>
 *   <li>Décision dans les <b>6 semaines</b> (42 jours — R.776-27)</li>
 *   <li>Effet suspensif du recours : éloignement impossible</li>
 * </ul>
 *
 * <p>Calcul en <b>heures</b> (ChronoUnit.HOURS) — les heures comptent, pas un délai franc.
 *
 * <p>Outil <b>single-country FR</b> : la procédure BE (annexe 13) est juridiquement distincte
 * et couverte par SF-IM-08-05.
 */
public final class OqtfSansDelaiCalculator {

    /** Délai de recours devant juge unique TA (art. L.614-8 CESEDA). */
    public static final int DELAI_RECOURS_HEURES = 48;

    /** Délai d'audience TA prévisionnel (R.776-26 CJA — 96 heures). */
    public static final int AUDIENCE_HEURES = 96;

    /** Délai de décision TA prévisionnel (R.776-27 CJA — 6 semaines = 42 jours). */
    public static final int DECISION_JOURS = 42;

    /** Seuil en heures pour passer en URGENT (≤ 24h restantes). */
    public static final int SEUIL_URGENT_HEURES = 24;

    /** Motifs OQTF sans délai valides (art. L.731-1 CESEDA). */
    public static final Set<String> MOTIFS_VALIDES = Set.of(
            "RISQUE_FUITE",
            "TROUBLE_ORDRE_PUBLIC",
            "OQTF_PRECEDENTE_INEXECUTEE",
            "AUTRE"
    );

    public static final String STATUT_DISPONIBLE = "DISPONIBLE";
    public static final String STATUT_URGENT = "URGENT";
    public static final String STATUT_EXPIRE = "EXPIRE";
    public static final String STATUT_RECOURS_FORME = "RECOURS_FORME";

    public static final String REFERE_LIBERTE = "REFERE_LIBERTE_L521_2";
    public static final String CONTROLE_JLD = "CONTROLE_JLD_LIBERTE";

    private static final String BASE_JURIDIQUE = "Art. L.731-1, L.614-8, R.776-26/27 CJA";

    private OqtfSansDelaiCalculator() {
    }

    public static OqtfSansDelaiResult compute(LocalDateTime dateHeureNotificationOqtf,
                                              String motifSansDelai,
                                              boolean placementCra,
                                              boolean recoursForme,
                                              LocalDateTime dateHeureRecours) {
        return compute(dateHeureNotificationOqtf, motifSansDelai, placementCra,
                recoursForme, dateHeureRecours,
                Clock.system(ZoneId.of("Europe/Paris")));
    }

    public static OqtfSansDelaiResult compute(LocalDateTime dateHeureNotificationOqtf,
                                              String motifSansDelai,
                                              boolean placementCra,
                                              boolean recoursForme,
                                              LocalDateTime dateHeureRecours,
                                              Clock clock) {
        validateInputs(dateHeureNotificationOqtf, motifSansDelai, recoursForme,
                dateHeureRecours, clock);

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime dateHeureExpiration = dateHeureNotificationOqtf.plusHours(DELAI_RECOURS_HEURES);

        String statut;
        long heuresRestantes;
        LocalDateTime dateHeureAudience = null;
        java.time.LocalDate dateDecision = null;

        if (recoursForme) {
            statut = STATUT_RECOURS_FORME;
            heuresRestantes = 0L;
            dateHeureAudience = dateHeureRecours.plusHours(AUDIENCE_HEURES);
            dateDecision = dateHeureRecours.toLocalDate().plusDays(DECISION_JOURS);
        } else {
            heuresRestantes = ChronoUnit.HOURS.between(now, dateHeureExpiration);
            if (heuresRestantes < 0) {
                statut = STATUT_EXPIRE;
            } else if (heuresRestantes <= SEUIL_URGENT_HEURES) {
                statut = STATUT_URGENT;
            } else {
                statut = STATUT_DISPONIBLE;
            }
        }

        List<String> refereDisponibles = new ArrayList<>();
        refereDisponibles.add(REFERE_LIBERTE);
        if (placementCra) {
            refereDisponibles.add(CONTROLE_JLD);
        }

        List<String> messages = buildMessages(motifSansDelai, statut, placementCra);
        String formule = buildFormule(motifSansDelai, statut, dateHeureNotificationOqtf,
                dateHeureExpiration, heuresRestantes, recoursForme, dateHeureRecours,
                dateHeureAudience, dateDecision, placementCra);

        return new OqtfSansDelaiResult(
                dateHeureNotificationOqtf,
                motifSansDelai,
                placementCra,
                recoursForme,
                dateHeureRecours,
                dateHeureExpiration,
                heuresRestantes,
                statut,
                dateHeureAudience,
                dateDecision,
                List.copyOf(refereDisponibles),
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    private static void validateInputs(LocalDateTime dateHeureNotificationOqtf,
                                       String motifSansDelai,
                                       boolean recoursForme,
                                       LocalDateTime dateHeureRecours,
                                       Clock clock) {
        if (dateHeureNotificationOqtf == null) {
            throw new IllegalArgumentException("dateHeureNotificationOqtf est requise");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (dateHeureNotificationOqtf.isAfter(now)) {
            throw new IllegalArgumentException(
                    "dateHeureNotificationOqtf ne peut pas être dans le futur");
        }
        if (motifSansDelai == null || motifSansDelai.isBlank()) {
            throw new IllegalArgumentException("motifSansDelai est requis");
        }
        if (!MOTIFS_VALIDES.contains(motifSansDelai)) {
            throw new IllegalArgumentException(
                    "motifSansDelai inconnu — valeurs attendues : " + MOTIFS_VALIDES);
        }
        if (recoursForme) {
            if (dateHeureRecours == null) {
                throw new IllegalArgumentException(
                        "dateHeureRecours est requise si recoursForme=true");
            }
            if (dateHeureRecours.isBefore(dateHeureNotificationOqtf)) {
                throw new IllegalArgumentException(
                        "dateHeureRecours ne peut pas être antérieure à dateHeureNotificationOqtf");
            }
        } else if (dateHeureRecours != null && dateHeureRecours.isBefore(dateHeureNotificationOqtf)) {
            throw new IllegalArgumentException(
                    "dateHeureRecours ne peut pas être antérieure à dateHeureNotificationOqtf");
        }
    }

    private static List<String> buildMessages(String motif, String statut, boolean placementCra) {
        List<String> messages = new ArrayList<>();
        messages.add("DÉLAI CRITIQUE : 48 heures à compter de la notification (art. L.614-8 CESEDA). "
                + "Ce n'est PAS un délai franc — les heures comptent.");
        messages.add("Recours devant juge unique TA, audience sous 96 heures (R.776-26 CJA), "
                + "décision sous 6 semaines (R.776-27 CJA).");
        messages.add("Effet suspensif du recours (L.614-8) : éloignement impossible tant que le TA "
                + "n'a pas statué.");
        messages.add("Référé liberté (art. L.521-2 CJA) disponible si atteinte grave à une liberté "
                + "fondamentale — décision en 48h.");

        switch (motif) {
            case "RISQUE_FUITE" -> messages.add(
                    "Contester les indices de fuite retenus (art. L.731-1 1° CESEDA : documents "
                            + "d'identité, adresse non déclarée, refus d'obtempérer précédent, etc.).");
            case "TROUBLE_ORDRE_PUBLIC" -> messages.add(
                    "Contester la qualification de trouble à l'ordre public (art. L.731-1 2° CESEDA) "
                            + "— la jurisprudence exige une menace réelle, actuelle, suffisamment grave.");
            case "OQTF_PRECEDENTE_INEXECUTEE" -> messages.add(
                    "Contester l'inexécution de l'OQTF précédente (art. L.731-1 3° CESEDA) — "
                            + "vérifier si l'OQTF antérieure était régulière et si un recours avait "
                            + "été formé.");
            case "AUTRE" -> messages.add(
                    "Vérifier attentivement le motif invoqué par l'administration et l'article "
                            + "L.731-1 CESEDA cité dans la décision.");
            default -> { /* unreachable */ }
        }

        if (placementCra) {
            messages.add("Étranger placé en CRA — audience du JLD (L.742-1 CESEDA) toutes les 48h "
                    + "pour prolongation (max 90 jours). Coordonner les 2 procédures (TA + JLD).");
        }

        if (STATUT_URGENT.equals(statut)) {
            messages.add("DÉLAI URGENT : il reste moins de 24 heures avant expiration du délai de "
                    + "recours — déposer le recours immédiatement.");
        } else if (STATUT_EXPIRE.equals(statut)) {
            messages.add("DÉLAI EXPIRÉ : recours de plein contentieux forclos. Seule voie "
                    + "restante : référé liberté (L.521-2 CJA) en cas d'éloignement imminent et "
                    + "d'atteinte grave aux droits fondamentaux.");
        }
        return messages;
    }

    private static String buildFormule(String motif, String statut,
                                       LocalDateTime dateNotification,
                                       LocalDateTime dateExpiration,
                                       long heuresRestantes,
                                       boolean recoursForme,
                                       LocalDateTime dateRecours,
                                       LocalDateTime dateAudience,
                                       java.time.LocalDate dateDecision,
                                       boolean placementCra) {
        String cra = placementCra ? " (placement CRA)" : "";
        if (recoursForme) {
            return String.format(
                    "OQTF sans délai L.731-1 motif %s notifiée le %s%s — recours formé le %s. "
                            + "Audience prévisionnelle %s (96h), décision prévisionnelle %s (6 semaines).",
                    motif, dateNotification, cra, dateRecours, dateAudience, dateDecision);
        }
        return String.format(
                "OQTF sans délai L.731-1 motif %s notifiée le %s%s — délai de recours expire le %s "
                        + "(%d heures restantes, statut %s). Référé L.521-2 disponible.",
                motif, dateNotification, cra, dateExpiration, heuresRestantes, statut);
    }
}
