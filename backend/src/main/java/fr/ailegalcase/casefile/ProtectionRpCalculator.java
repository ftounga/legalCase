package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-DT-30-01 : calculateur de régularité de la procédure de licenciement
 * d'un salarié protégé (FR — art. L.2411-1 et s. Code du travail).
 *
 * <p>Évalue la régularité procédurale du licenciement d'un représentant du personnel
 * selon 4 critères structurants :</p>
 * <ul>
 *   <li><strong>Statut protégé (L.2411-1 et s.)</strong> : membre CSE, délégué syndical,
 *       conseiller du salarié, conseiller prud'hommes, défenseur syndical, médecin du travail.</li>
 *   <li><strong>Période de protection (L.2411-1 al. 2)</strong> : pendant le mandat
 *       <strong>+ 6 mois après expiration</strong>.</li>
 *   <li><strong>Procédure d'autorisation</strong> : autorisation préalable de l'inspection
 *       du travail (L.2411-3, L.2411-22, L.2411-24…).</li>
 *   <li><strong>Indemnités si nullité (L.2422-1)</strong> : ≥ 6 mois de salaire +
 *       réintégration de droit + salaires éviction.</li>
 * </ul>
 *
 * <p>Outil <strong>single-country FRANCE</strong> — l'équivalent BE (statut protégé belge,
 * loi du 19 mars 1991 sur les délégués du personnel) est traité dans une feature jumelle
 * au backlog.</p>
 *
 * <p>Le délai de contestation d'un refus d'autorisation est fixé à 2 mois (60 jours)
 * devant le tribunal administratif (R.2422-1).</p>
 */
public final class ProtectionRpCalculator {

    /** Statut protégé du salarié (L.2411-1 et s.). */
    public enum StatutProtege {
        /** Membre CSE titulaire (L.2411-1). */
        MEMBRE_CSE_TITULAIRE,
        /** Membre CSE suppléant (L.2411-1). */
        MEMBRE_CSE_SUPPLEANT,
        /** Délégué syndical (L.2411-3). */
        DELEGUE_SYNDICAL,
        /** Représentant de section syndicale (L.2411-4). */
        REPRESENTANT_SECTION_SYNDICALE,
        /** Conseiller prud'hommes (L.2411-22). */
        CONSEILLER_PRUDHOMMES,
        /** Conseiller du salarié (L.2411-21). */
        CONSEILLER_SALARIE,
        /** Défenseur syndical (L.2411-24). */
        DEFENSEUR_SYNDICAL,
        /** Médecin du travail (L.2411-5). */
        MEDECIN_TRAVAIL,
        /** Membre CHSCT historique (mandat résiduel). */
        MEMBRE_CHSCT_HISTORIQUE
    }

    /** Procédure suivie par l'employeur. */
    public enum ProcedureSuivie {
        /** Autorisation obtenue (avis favorable IT). */
        AUTORISATION_OBTENUE,
        /** Autorisation refusée (avis défavorable IT). */
        AUTORISATION_REFUSEE,
        /** Procédure démarrée mais non finalisée. */
        EN_COURS_INSTRUCTION,
        /** L'employeur n'a pas demandé d'autorisation — défaut grave. */
        AUCUNE_DEMANDE
    }

    /** Motif de licenciement (enum standard FR). */
    public enum MotifLicenciement {
        FAUTE_GRAVE,
        INSUFFISANCE_PRO,
        ECONOMIQUE,
        INAPTITUDE,
        AUTRE
    }

    /** Verdict global de légalité du licenciement. */
    public enum VerdictLegalite {
        VALIDE,
        CONTESTABLE,
        NUL
    }

    /** Critères de régularité procédurale évalués par le calculator. */
    public enum CritereRegularite {
        STATUT_PROTEGE_VALIDE,
        PROCEDURE_AUTORISATION_DEMANDEE,
        AUTORISATION_OBTENUE,
        LICENCIEMENT_HORS_PROCEDURE_EN_COURS
    }

    /** Délai de contestation devant le tribunal administratif (art. R.2422-1). */
    public static final int DELAI_CONTESTATION_JOURS = 60;

    /** Période de protection post-mandat (L.2411-1 al. 2) — 6 mois. */
    public static final int PROTECTION_POST_MANDAT_MOIS = 6;

    /** Indemnité forfaitaire minimale en mois de salaire (L.2422-1). */
    public static final int INDEMNITE_FORFAITAIRE_MIN_MOIS = 6;

    private ProtectionRpCalculator() {}

    /**
     * Évalue la régularité de la procédure de licenciement d'un salarié protégé.
     *
     * @param statutProtege         statut protégé du salarié (obligatoire)
     * @param dateExpirationMandat  date d'expiration du mandat (obligatoire)
     * @param datePresumeeRupture   date présumée de rupture du contrat (obligatoire)
     * @param procedureSuivie       procédure suivie par l'employeur (obligatoire)
     * @param motifLicenciement     motif du licenciement (obligatoire)
     * @param salaireMensuelBrutEur salaire mensuel brut en euros (≥ 0, optionnel — 0 par défaut)
     * @param country               pays du workspace (FRANCE attendu)
     * @return résultat structuré (verdict + score + critères + indemnités + messages)
     * @throws IllegalArgumentException si paramètre invalide ou pays non supporté
     */
    public static ProtectionRpResult compute(StatutProtege statutProtege,
                                              LocalDate dateExpirationMandat,
                                              LocalDate datePresumeeRupture,
                                              ProcedureSuivie procedureSuivie,
                                              MotifLicenciement motifLicenciement,
                                              double salaireMensuelBrutEur,
                                              String country) {
        if (statutProtege == null) {
            throw new IllegalArgumentException("Statut protégé requis");
        }
        if (dateExpirationMandat == null) {
            throw new IllegalArgumentException("Date d'expiration du mandat requise");
        }
        if (datePresumeeRupture == null) {
            throw new IllegalArgumentException("Date présumée de rupture requise");
        }
        if (procedureSuivie == null) {
            throw new IllegalArgumentException("Procédure suivie requise");
        }
        if (motifLicenciement == null) {
            throw new IllegalArgumentException("Motif de licenciement requis");
        }
        if (salaireMensuelBrutEur < 0) {
            throw new IllegalArgumentException("Salaire mensuel brut doit être ≥ 0");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil non disponible pour le pays " + country
                            + " — l'équivalent BE (statut protégé belge, loi du 19 mars 1991)"
                            + " sera traité dans une feature jumelle dédiée.");
        }

        // 1) Période de protection : mandat + 6 mois (L.2411-1 al. 2)
        LocalDate finProtection = dateExpirationMandat.plusMonths(PROTECTION_POST_MANDAT_MOIS);
        boolean salarieEncoreProtege = !datePresumeeRupture.isAfter(finProtection);

        if (!salarieEncoreProtege) {
            // Salarié hors période de protection — procédure de droit commun applicable
            List<String> msgsHors = new ArrayList<>();
            msgsHors.add("Salarié hors période de protection (L.2411-1 al. 2) — la protection "
                    + "spéciale a expiré le " + finProtection
                    + " (mandat + " + PROTECTION_POST_MANDAT_MOIS + " mois). "
                    + "La procédure de droit commun est applicable.");
            String formuleHors = String.format(Locale.ROOT,
                    "Mandat expiré le %s + %d mois → protection terminée le %s. "
                            + "Date présumée rupture %s → hors protection → verdict VALIDE",
                    dateExpirationMandat, PROTECTION_POST_MANDAT_MOIS, finProtection,
                    datePresumeeRupture);
            return new ProtectionRpResult(
                    statutProtege,
                    dateExpirationMandat,
                    datePresumeeRupture,
                    procedureSuivie,
                    motifLicenciement,
                    salaireMensuelBrutEur,
                    countryNormalized,
                    false,
                    100,
                    VerdictLegalite.VALIDE,
                    List.of(),
                    List.of(),
                    0.0,
                    0.0,
                    DELAI_CONTESTATION_JOURS,
                    "Art. L.2411-1 + L.2411-3 + L.2411-22 + L.2422-1 + R.2422-1 Code du travail",
                    formuleHors,
                    msgsHors
            );
        }

        // 2) Salarié encore protégé — évaluation des critères
        List<CritereRegularite> criteresRemplis = new ArrayList<>();
        List<CritereRegularite> criteresManquants = new ArrayList<>();

        // Critère 1 : statut protégé (toujours rempli si non null)
        criteresRemplis.add(CritereRegularite.STATUT_PROTEGE_VALIDE);

        // Critère 2 : procédure d'autorisation demandée
        if (procedureSuivie == ProcedureSuivie.AUCUNE_DEMANDE) {
            criteresManquants.add(CritereRegularite.PROCEDURE_AUTORISATION_DEMANDEE);
        } else {
            criteresRemplis.add(CritereRegularite.PROCEDURE_AUTORISATION_DEMANDEE);
        }

        // Critère 3 : autorisation obtenue
        if (procedureSuivie == ProcedureSuivie.AUTORISATION_OBTENUE) {
            criteresRemplis.add(CritereRegularite.AUTORISATION_OBTENUE);
        } else {
            criteresManquants.add(CritereRegularite.AUTORISATION_OBTENUE);
        }

        // Critère 4 : licenciement prononcé hors instruction en cours
        if (procedureSuivie == ProcedureSuivie.EN_COURS_INSTRUCTION) {
            criteresManquants.add(CritereRegularite.LICENCIEMENT_HORS_PROCEDURE_EN_COURS);
        } else {
            criteresRemplis.add(CritereRegularite.LICENCIEMENT_HORS_PROCEDURE_EN_COURS);
        }

        // 3) Verdict + score
        VerdictLegalite verdict;
        int score;
        switch (procedureSuivie) {
            case AUTORISATION_OBTENUE -> {
                verdict = VerdictLegalite.VALIDE;
                score = 100;
            }
            case EN_COURS_INSTRUCTION -> {
                verdict = VerdictLegalite.CONTESTABLE;
                score = 30;
            }
            case AUCUNE_DEMANDE, AUTORISATION_REFUSEE -> {
                verdict = VerdictLegalite.NUL;
                score = 0;
            }
            default -> {
                verdict = VerdictLegalite.CONTESTABLE;
                score = 30;
            }
        }

        // 4) Indemnités si NUL (L.2422-1)
        double indemniteForfaitaire = 0.0;
        double salaireEviction = 0.0;
        if (verdict == VerdictLegalite.NUL && salaireMensuelBrutEur > 0) {
            indemniteForfaitaire = INDEMNITE_FORFAITAIRE_MIN_MOIS * salaireMensuelBrutEur;
            // Estimation période d'éviction : mois entre datePresumeeRupture et fin protection,
            // borne minimale 1 mois pour l'estimation indicative.
            long moisEviction = Math.max(1L,
                    java.time.temporal.ChronoUnit.MONTHS.between(
                            datePresumeeRupture.withDayOfMonth(1),
                            finProtection.withDayOfMonth(1)));
            salaireEviction = moisEviction * salaireMensuelBrutEur;
        }

        String baseJuridique = "Art. L.2411-1 + L.2411-3 + L.2411-22 + L.2422-1 + R.2422-1 Code du travail";
        String formule = String.format(Locale.ROOT,
                "Salarié protégé (%s) + procédure %s + motif %s → %d critères remplis / %d manquants = score %d → verdict %s",
                statutProtege.name(), procedureSuivie.name(), motifLicenciement.name(),
                criteresRemplis.size(), criteresManquants.size(), score, verdict.name());

        List<String> messages = buildMessages(statutProtege, procedureSuivie, motifLicenciement,
                verdict, indemniteForfaitaire, salaireEviction);

        return new ProtectionRpResult(
                statutProtege,
                dateExpirationMandat,
                datePresumeeRupture,
                procedureSuivie,
                motifLicenciement,
                salaireMensuelBrutEur,
                countryNormalized,
                true,
                score,
                verdict,
                criteresRemplis,
                criteresManquants,
                indemniteForfaitaire,
                salaireEviction,
                DELAI_CONTESTATION_JOURS,
                baseJuridique,
                formule,
                messages
        );
    }

    private static List<String> buildMessages(StatutProtege statut,
                                              ProcedureSuivie procedure,
                                              MotifLicenciement motif,
                                              VerdictLegalite verdict,
                                              double indemnite,
                                              double salaireEviction) {
        List<String> msgs = new ArrayList<>();
        msgs.add("Salarié dans la période de protection (L.2411-1) — la procédure spéciale "
                + "d'autorisation de l'inspection du travail s'applique.");

        msgs.add("Statut protégé : " + libelleStatut(statut) + " (" + articleLoiStatut(statut) + ")");

        switch (procedure) {
            case AUTORISATION_OBTENUE -> msgs.add("Autorisation de l'inspection du travail obtenue ✓ — "
                    + "le licenciement peut être prononcé.");
            case AUTORISATION_REFUSEE -> msgs.add("Autorisation de l'inspection du travail REFUSÉE — "
                    + "tout licenciement prononcé est NUL (réintégration de droit + indemnités L.2422-1). "
                    + "Recours hiérarchique possible devant le ministre du travail dans les 2 mois.");
            case EN_COURS_INSTRUCTION -> msgs.add("Procédure d'autorisation EN COURS d'instruction — "
                    + "tout licenciement prononcé avant la décision est CONTESTABLE (irrégulier).");
            case AUCUNE_DEMANDE -> msgs.add("AUCUNE demande d'autorisation déposée à l'inspection du travail "
                    + "— vice de procédure absolu. Le licenciement est NUL — réintégration de droit "
                    + "+ indemnités forfaitaires (L.2422-1).");
        }

        msgs.add("Motif invoqué : " + motif.name() + " — non opposable tant que la procédure spéciale "
                + "n'est pas régulière.");

        if (verdict == VerdictLegalite.NUL) {
            msgs.add("VERDICT NUL — Conséquences (L.2422-1) :");
            msgs.add(" • Réintégration de droit du salarié dans son emploi ou un emploi équivalent");
            if (indemnite > 0) {
                msgs.add(" • Indemnité forfaitaire minimale : "
                        + String.format(Locale.ROOT, "%.2f €", indemnite)
                        + " (≥ " + INDEMNITE_FORFAITAIRE_MIN_MOIS + " mois de salaire)");
            } else {
                msgs.add(" • Indemnité forfaitaire minimale ≥ "
                        + INDEMNITE_FORFAITAIRE_MIN_MOIS + " mois de salaire (salaire non renseigné)");
            }
            if (salaireEviction > 0) {
                msgs.add(" • Salaires correspondant à la période d'éviction : "
                        + String.format(Locale.ROOT, "%.2f €", salaireEviction) + " (estimation)");
            }
            msgs.add(" • Dommages-intérêts complémentaires possibles selon le préjudice subi");
        } else if (verdict == VerdictLegalite.CONTESTABLE) {
            msgs.add("VERDICT CONTESTABLE — la procédure est en cours, la mise en œuvre du licenciement "
                    + "est prématurée. Risque élevé d'annulation devant le juge administratif.");
        } else {
            msgs.add("VERDICT VALIDE — procédure régulière, sous réserve du contrôle de fond par "
                    + "l'inspection du travail (réalité du motif, lien avec le mandat).");
        }

        msgs.add("Délai de contestation devant le tribunal administratif : "
                + DELAI_CONTESTATION_JOURS + " jours (R.2422-1) à compter de la notification "
                + "de la décision de l'inspection du travail.");

        return msgs;
    }

    private static String libelleStatut(StatutProtege s) {
        return switch (s) {
            case MEMBRE_CSE_TITULAIRE -> "Membre CSE titulaire";
            case MEMBRE_CSE_SUPPLEANT -> "Membre CSE suppléant";
            case DELEGUE_SYNDICAL -> "Délégué syndical";
            case REPRESENTANT_SECTION_SYNDICALE -> "Représentant de section syndicale";
            case CONSEILLER_PRUDHOMMES -> "Conseiller prud'hommes";
            case CONSEILLER_SALARIE -> "Conseiller du salarié";
            case DEFENSEUR_SYNDICAL -> "Défenseur syndical";
            case MEDECIN_TRAVAIL -> "Médecin du travail";
            case MEMBRE_CHSCT_HISTORIQUE -> "Membre CHSCT (mandat résiduel)";
        };
    }

    private static String articleLoiStatut(StatutProtege s) {
        return switch (s) {
            case MEMBRE_CSE_TITULAIRE, MEMBRE_CSE_SUPPLEANT -> "L.2411-1";
            case DELEGUE_SYNDICAL -> "L.2411-3";
            case REPRESENTANT_SECTION_SYNDICALE -> "L.2411-4";
            case CONSEILLER_PRUDHOMMES -> "L.2411-22";
            case CONSEILLER_SALARIE -> "L.2411-21";
            case DEFENSEUR_SYNDICAL -> "L.2411-24";
            case MEDECIN_TRAVAIL -> "L.2411-5";
            case MEMBRE_CHSCT_HISTORIQUE -> "ancien art. L.4612-1 et s.";
        };
    }
}
