package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SF-DT-14-01 : calculateur de validité d'un Plan de Sauvegarde de l'Emploi (PSE)
 * (FR — art. L.1233-24-1, L.1233-30, L.1233-57-2, L.1233-61, L.1235-7-1 Code du travail).
 *
 * <p>Évalue la validité procédurale du PSE selon 4 axes :</p>
 * <ul>
 *   <li><strong>Déclenchement (L.1233-24-1)</strong> : ≥ 10 licenciements sur 30 jours en
 *       entreprise ≥ 50 salariés.</li>
 *   <li><strong>Consultation CSE (L.1233-30)</strong> : avis du CSE obligatoire.</li>
 *   <li><strong>Statut DREETS (L.1233-57-2)</strong> : validation accord majoritaire ou
 *       homologation document unilatéral.</li>
 *   <li><strong>Contenu minimal (L.1233-61)</strong> : reclassement interne + au moins 2
 *       mesures distinctes.</li>
 * </ul>
 *
 * <p>Outil <strong>single-country FRANCE</strong> — l'équivalent BE (congé-réorganisation
 * Loi 1976 / Loi Renault) est traité dans une feature jumelle au backlog.</p>
 *
 * <p>Le délai de contestation est fixé à 60 jours (2 mois) devant le tribunal administratif
 * (art. L.1235-7-1, D.1235-21).</p>
 */
public final class PseCalculator {

    /** Mode d'adoption du PSE (L.1233-24-1). */
    public enum ModeAdoption {
        /** Accord collectif majoritaire — DREETS valide. */
        ACCORD_COLLECTIF_MAJORITAIRE,
        /** Document unilatéral employeur — DREETS homologue. */
        DOCUMENT_UNILATERAL
    }

    /** Avis du CSE (L.1233-30). */
    public enum AvisCse {
        FAVORABLE,
        DEFAVORABLE,
        NON_CONSULTE
    }

    /** Statut DREETS (L.1233-57-2). */
    public enum StatutDreets {
        /** Validation d'un accord majoritaire. */
        VALIDE,
        /** Homologation d'un document unilatéral. */
        HOMOLOGUE,
        /** Refus de validation/homologation par la DREETS. */
        REFUS,
        /** Procédure en cours (pas encore validée/homologuée). */
        EN_COURS
    }

    /** Mesures du contenu PSE (L.1233-61). */
    public enum ContenuMesure {
        RECLASSEMENT_INTERNE,
        RECLASSEMENT_EXTERNE,
        FORMATION,
        AIDE_CREATION,
        INDEMNITES_SUPRA,
        CONGE_RECLASSEMENT,
        CELLULE_RECLASSEMENT,
        AUTRE
    }

    /** Verdict global de validité du PSE. */
    public enum VerdictValidite {
        VALIDE,
        CONTESTABLE,
        NUL
    }

    /** Critères de validité évalués par le calculator. */
    public enum CritereValidite {
        CSE_CONSULTE,
        DREETS_VALIDE_OU_HOMOLOGUE,
        CONTENU_RECLASSEMENT,
        MODE_DREETS_COHERENT,
        MESURES_MULTIPLES
    }

    /** Délai de contestation devant le tribunal administratif (art. L.1235-7-1, D.1235-21). */
    public static final int DELAI_CONTESTATION_JOURS = 60;

    /** Seuil licenciements pour déclencher PSE (L.1233-24-1). */
    public static final int SEUIL_LICENCIEMENTS_PSE = 10;

    /** Seuil entreprise (effectif) pour déclencher PSE (L.1233-24-1). */
    public static final int SEUIL_ENTREPRISE_PSE = 50;

    /** Période de référence en jours (L.1233-24-1). */
    public static final int PERIODE_REFERENCE_JOURS = 30;

    private PseCalculator() {}

    /**
     * Calcule la validité d'un PSE selon les 4 axes du Code du travail.
     *
     * @param tailleEntrepriseSalaries     effectif de l'entreprise (≥ 0)
     * @param nombreLicenciementsEnvisages nombre de licenciements économiques envisagés (≥ 0)
     * @param periodeJours                 période de référence en jours (typiquement 30)
     * @param modeAdoption                 mode d'adoption du PSE (obligatoire)
     * @param csaeConsulteAvis             avis du CSE (obligatoire)
     * @param dreetsStatut                 statut DREETS (obligatoire)
     * @param dateNotificationDreets       date de notification DREETS (optionnelle)
     * @param contenuMesures               mesures du PSE (peut être vide ; null traité comme vide)
     * @param dateProjet                   date du projet de licenciement (obligatoire)
     * @param country                      pays du workspace (FRANCE attendu)
     * @return résultat structuré (déclenchement + score + verdict + critères + messages)
     * @throws IllegalArgumentException si paramètre invalide ou pays non supporté
     */
    public static PseResult compute(int tailleEntrepriseSalaries,
                                     int nombreLicenciementsEnvisages,
                                     int periodeJours,
                                     ModeAdoption modeAdoption,
                                     AvisCse csaeConsulteAvis,
                                     StatutDreets dreetsStatut,
                                     LocalDate dateNotificationDreets,
                                     List<ContenuMesure> contenuMesures,
                                     LocalDate dateProjet,
                                     String country) {
        if (modeAdoption == null) {
            throw new IllegalArgumentException("Mode d'adoption requis");
        }
        if (csaeConsulteAvis == null) {
            throw new IllegalArgumentException("Avis du CSE requis");
        }
        if (dreetsStatut == null) {
            throw new IllegalArgumentException("Statut DREETS requis");
        }
        if (dateProjet == null) {
            throw new IllegalArgumentException("Date du projet requise");
        }
        if (tailleEntrepriseSalaries < 0) {
            throw new IllegalArgumentException("Taille entreprise (salariés) doit être ≥ 0");
        }
        if (nombreLicenciementsEnvisages < 0) {
            throw new IllegalArgumentException("Nombre de licenciements envisagés doit être ≥ 0");
        }
        if (periodeJours < 0) {
            throw new IllegalArgumentException("Période (jours) doit être ≥ 0");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil non disponible pour le pays " + country
                            + " — l'équivalent BE (congé-réorganisation Loi 1976 / Loi Renault)"
                            + " sera traité dans une feature jumelle dédiée.");
        }

        List<ContenuMesure> mesures = dedupAndNullSafe(contenuMesures);

        // 1) Déclenchement (L.1233-24-1)
        boolean pseRequis = nombreLicenciementsEnvisages >= SEUIL_LICENCIEMENTS_PSE
                && periodeJours <= PERIODE_REFERENCE_JOURS
                && tailleEntrepriseSalaries >= SEUIL_ENTREPRISE_PSE;

        if (!pseRequis) {
            // PSE non requis — procédure normale applicable, pas de verdict de validité PSE.
            List<String> msgsNon = new ArrayList<>();
            msgsNon.add("PSE non requis (L.1233-24-1) : seuil non atteint — procédure normale "
                    + "de licenciement économique applicable.");
            String formuleNon = String.format(Locale.ROOT,
                    "PSE non requis (%d licenciements / %d jours / %d salariés) — score 100 → VALIDE",
                    nombreLicenciementsEnvisages, periodeJours, tailleEntrepriseSalaries);
            return new PseResult(
                    tailleEntrepriseSalaries,
                    nombreLicenciementsEnvisages,
                    periodeJours,
                    modeAdoption,
                    csaeConsulteAvis,
                    dreetsStatut,
                    dateNotificationDreets,
                    mesures,
                    dateProjet,
                    countryNormalized,
                    false,
                    100,
                    VerdictValidite.VALIDE,
                    List.of(),
                    List.of(),
                    DELAI_CONTESTATION_JOURS,
                    "Art. L.1233-24-1 + L.1233-30 + L.1233-57-2 + L.1233-61 + L.1235-7-1 Code du travail",
                    formuleNon,
                    msgsNon
            );
        }

        // 2) Évaluation des critères de validité (PSE requis)
        List<CritereValidite> criteresRemplis = new ArrayList<>();
        List<CritereValidite> criteresManquants = new ArrayList<>();
        int score = 100;
        boolean blocant = false;

        // Critère 1 : CSE consulté (L.1233-30) — bloquant si non consulté
        if (csaeConsulteAvis == AvisCse.NON_CONSULTE) {
            criteresManquants.add(CritereValidite.CSE_CONSULTE);
            score -= 50;
            blocant = true;
        } else {
            criteresRemplis.add(CritereValidite.CSE_CONSULTE);
        }

        // Critère 2 : DREETS VALIDE ou HOMOLOGUE (L.1233-57-2)
        if (dreetsStatut == StatutDreets.REFUS) {
            criteresManquants.add(CritereValidite.DREETS_VALIDE_OU_HOMOLOGUE);
            score -= 50;
            blocant = true;
        } else if (dreetsStatut == StatutDreets.EN_COURS) {
            criteresManquants.add(CritereValidite.DREETS_VALIDE_OU_HOMOLOGUE);
            score -= 25;
            // Pas bloquant mais CONTESTABLE
        } else {
            criteresRemplis.add(CritereValidite.DREETS_VALIDE_OU_HOMOLOGUE);
        }

        // Critère 3 : Mode adoption / DREETS cohérent (L.1233-57-2)
        boolean modeCoherent = isModeCoherent(modeAdoption, dreetsStatut);
        if (modeCoherent) {
            criteresRemplis.add(CritereValidite.MODE_DREETS_COHERENT);
        } else if (dreetsStatut != StatutDreets.REFUS && dreetsStatut != StatutDreets.EN_COURS) {
            // Mismatch uniquement quand DREETS a tranché (mais en mauvais mode)
            criteresManquants.add(CritereValidite.MODE_DREETS_COHERENT);
            score -= 20;
        } else {
            // DREETS non tranchée → cohérence non encore évaluable
            criteresManquants.add(CritereValidite.MODE_DREETS_COHERENT);
        }

        // Critère 4 : Contenu reclassement interne (L.1233-61)
        if (mesures.contains(ContenuMesure.RECLASSEMENT_INTERNE)) {
            criteresRemplis.add(CritereValidite.CONTENU_RECLASSEMENT);
        } else {
            criteresManquants.add(CritereValidite.CONTENU_RECLASSEMENT);
            score -= 20;
        }

        // Critère 5 : Mesures multiples (L.1233-61)
        if (mesures.size() >= 2) {
            criteresRemplis.add(CritereValidite.MESURES_MULTIPLES);
        } else {
            criteresManquants.add(CritereValidite.MESURES_MULTIPLES);
            score -= 10;
        }

        if (score < 0) score = 0;
        if (score > 100) score = 100;

        // 3) Verdict
        VerdictValidite verdict;
        if (blocant) {
            verdict = VerdictValidite.NUL;
        } else if (!criteresManquants.isEmpty() || score < 80) {
            verdict = VerdictValidite.CONTESTABLE;
        } else {
            verdict = VerdictValidite.VALIDE;
        }

        String baseJuridique = "Art. L.1233-24-1 + L.1233-30 + L.1233-57-2 + L.1233-61 + L.1235-7-1 Code du travail";
        String formule = String.format(Locale.ROOT,
                "PSE requis (%d licenciements / %d jours / %d salariés) + %d critères remplis / %d manquants = score %d → verdict %s",
                nombreLicenciementsEnvisages, periodeJours, tailleEntrepriseSalaries,
                criteresRemplis.size(), criteresManquants.size(), score, verdict.name());

        List<String> messages = buildMessages(modeAdoption, csaeConsulteAvis, dreetsStatut,
                mesures, criteresRemplis, criteresManquants);

        return new PseResult(
                tailleEntrepriseSalaries,
                nombreLicenciementsEnvisages,
                periodeJours,
                modeAdoption,
                csaeConsulteAvis,
                dreetsStatut,
                dateNotificationDreets,
                mesures,
                dateProjet,
                countryNormalized,
                true,
                score,
                verdict,
                criteresRemplis,
                criteresManquants,
                DELAI_CONTESTATION_JOURS,
                baseJuridique,
                formule,
                messages
        );
    }

    /**
     * Cohérence mode d'adoption / statut DREETS (L.1233-57-2) :
     * <ul>
     *   <li>{@code ACCORD_COLLECTIF_MAJORITAIRE} → DREETS doit {@code VALIDE}</li>
     *   <li>{@code DOCUMENT_UNILATERAL} → DREETS doit {@code HOMOLOGUE}</li>
     * </ul>
     */
    private static boolean isModeCoherent(ModeAdoption mode, StatutDreets dreets) {
        if (mode == ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE) {
            return dreets == StatutDreets.VALIDE;
        }
        if (mode == ModeAdoption.DOCUMENT_UNILATERAL) {
            return dreets == StatutDreets.HOMOLOGUE;
        }
        return false;
    }

    private static List<String> buildMessages(ModeAdoption mode,
                                              AvisCse cse,
                                              StatutDreets dreets,
                                              List<ContenuMesure> mesures,
                                              List<CritereValidite> remplis,
                                              List<CritereValidite> manquants) {
        List<String> msgs = new ArrayList<>();
        msgs.add("PSE requis (L.1233-24-1) — la procédure spéciale s'applique.");

        if (cse == AvisCse.NON_CONSULTE) {
            msgs.add("CSE non consulté (L.1233-30) : vice de procédure absolu — PSE NUL. "
                    + "La consultation préalable du CSE est obligatoire.");
        } else if (cse == AvisCse.DEFAVORABLE) {
            msgs.add("CSE consulté avec avis DÉFAVORABLE (L.1233-30) : la procédure peut "
                    + "se poursuivre, mais l'avis défavorable du CSE renforce la position des "
                    + "salariés en cas de contentieux.");
        } else {
            msgs.add("CSE consulté avec avis FAVORABLE (L.1233-30) ✓");
        }

        switch (dreets) {
            case VALIDE -> msgs.add("DREETS a validé l'accord majoritaire (L.1233-57-2) ✓");
            case HOMOLOGUE -> msgs.add("DREETS a homologué le document unilatéral (L.1233-57-2) ✓");
            case REFUS -> msgs.add("DREETS a refusé la validation/homologation (L.1233-57-2) : "
                    + "PSE NUL — recommencer la procédure ou contester en référé administratif.");
            case EN_COURS -> msgs.add("DREETS en cours d'instruction (L.1233-57-2) : procédure "
                    + "non finalisée — la mise en œuvre du PSE est prématurée.");
        }

        if (!isModeCoherent(mode, dreets) && dreets != StatutDreets.REFUS && dreets != StatutDreets.EN_COURS) {
            String attendu = mode == ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE ? "VALIDE" : "HOMOLOGUE";
            msgs.add("Incohérence mode d'adoption / DREETS : un " + mode.name()
                    + " requiert un statut " + attendu + " — risque de contestation.");
        }

        if (!mesures.contains(ContenuMesure.RECLASSEMENT_INTERNE)) {
            msgs.add("Reclassement interne absent du PSE (L.1233-61) : contenu insuffisant — "
                    + "le reclassement interne est obligatoire avant tout licenciement.");
        }

        if (mesures.size() < 2) {
            msgs.add("Moins de 2 mesures distinctes (L.1233-61) : contenu trop maigre pour "
                    + "satisfaire l'exigence de mesures de reclassement, formation, aide à la création.");
        } else {
            msgs.add("Contenu PSE : " + mesures.size() + " mesures distinctes (L.1233-61) ✓");
        }

        msgs.add("Délai de contestation devant le tribunal administratif : "
                + DELAI_CONTESTATION_JOURS + " jours à compter de la notification "
                + "(art. L.1235-7-1, D.1235-21).");

        return msgs;
    }

    /** Déduplique en préservant l'ordre, null-safe. */
    private static <T> List<T> dedupAndNullSafe(List<T> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        Set<T> seen = new LinkedHashSet<>();
        for (T t : input) {
            if (t != null) seen.add(t);
        }
        return List.copyOf(seen);
    }
}
