package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-212-07 : moteur d'analyse de la conformité de la proposition de CSP
 * (Contrat de Sécurisation Professionnelle) lors d'un licenciement économique
 * dans une entreprise de moins de 1 000 salariés, et calcul de l'ASP
 * (Allocation Spécifique de Reclassement) estimée — FRANCE uniquement
 * (L. 1233-65 à L. 1233-70 CT ; ANI CSP 19/07/2011 révisé ; DARES).
 *
 * <p>L'outil vérifie, du point de vue de l'avocat du salarié :</p>
 * <ol>
 *   <li>L'obligation pour l'employeur de proposer le CSP au sein d'une
 *       entreprise &lt; 1 000 salariés (L. 1233-66 CT).</li>
 *   <li>La remise du document d'information CSP par l'employeur.</li>
 *   <li>La mention du délai de réflexion de 21 jours calendaires.</li>
 *   <li>La date de remise — lors de l'entretien préalable (procédure
 *       individuelle) ou à la date de notification individuelle (PSE).</li>
 *   <li>Le calcul de l'ASP estimée — 75 % du SJR pendant 12 mois (régime de
 *       droit commun, ANI CSP / DARES).</li>
 * </ol>
 *
 * <p>L'adhésion (acceptation) du salarié constitue une rupture amiable hors
 * préavis (L. 1233-67 CT) ; le refus ramène à un licenciement économique
 * normal avec préavis. Ce flag ne fait pas basculer le verdict mais alimente
 * le message d'orientation.</p>
 *
 * <p><b>Pays</b> : FRANCE uniquement — le CSP L. 1233-65+ CT est strictement
 * français (entreprises &lt; 1 000 salariés). Au-delà : congé de reclassement
 * L. 1233-71+ CT (F-DT-45, P3 backlog F-218). En BE, l'outplacement
 * obligatoire (CCT 82 / 51 / 24) relève d'un régime distinct (F-207).</p>
 */
public final class CspCrpConformiteCalculator {

    /** Verdict de conformité de la proposition CSP. */
    public enum ConformiteCsp {
        CONFORME,
        PARTIELLEMENT_CONFORME,
        NON_CONFORME
    }

    /** Code structuré d'un point de non-conformité détecté. */
    public enum CodeNonConformite {
        DT44_OBLIGATION_CSP,
        DT44_DOCUMENT_REMIS,
        DT44_DELAI_REFLEXION,
        DT44_DATE_REMISE
    }

    /** Point de non-conformité détecté — exposé dans la réponse API. */
    public record PointNonConformite(
            CodeNonConformite code,
            String libelle,
            String fondement,
            int poids,
            String explication
    ) {}

    /** Résultat calculé par {@link #compute}. */
    public record Result(
            boolean obligationCspApplicable,
            ConformiteCsp conformiteCsp,
            int scoreConformite,
            List<PointNonConformite> pointsNonConformite,
            Double aspEstimeeJournaliereEuros,
            Double aspEstimeeAnnuelleEuros,
            int dureeAspMois,
            List<String> basesJuridiques,
            List<String> messages,
            String country
    ) {}

    /**
     * Seuil d'applicabilité du CSP — entreprises de moins de 1 000 salariés
     * (L. 1233-66 CT). Au-delà : congé de reclassement L. 1233-71+ CT.
     */
    public static final int SEUIL_EFFECTIF_CSP = 1_000;

    /**
     * Délai de réflexion accordé au salarié pour accepter ou refuser
     * la proposition de CSP — 21 jours calendaires (L. 1233-67 CT).
     */
    public static final int DELAI_REFLEXION_JOURS = 21;

    /**
     * Taux légal de l'ASP en régime droit commun (75 % du SJR — ANI CSP
     * 19/07/2011 révisé, DARES). Distinct du taux ARE classique (57 % / 40 %
     * selon palier).
     */
    public static final double TAUX_ASP_REGIME_COMMUN = 0.75;

    /**
     * Durée d'indemnisation ASP en régime droit commun — 12 mois
     * (ANI CSP / décret 13/06/2014).
     */
    public static final int DUREE_ASP_MOIS = 12;

    /**
     * Diviseur du salaire journalier de référence — 365 jours (base annuelle
     * brut / 365, modulé par le coefficient ASP).
     */
    public static final double JOURS_ANNEE = 365.0;

    private CspCrpConformiteCalculator() {}

    /**
     * Point d'entrée principal.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace
     * @return résultat calculé
     * @throws IllegalArgumentException si {@code country != "FRANCE"} ou si
     *         des entrées numériques sont invalides
     */
    public static Result compute(CspCrpConformiteInput input, String country) {
        if (!"FRANCE".equalsIgnoreCase(country)) {
            throw new IllegalArgumentException(
                    "Outil réservé au droit du travail FRANCE — pays détecté : " + country);
        }
        if (input == null) {
            throw new IllegalArgumentException("Données d'entrée manquantes");
        }
        if (input.effectifEntreprise() < 0) {
            throw new IllegalArgumentException("L'effectif de l'entreprise ne peut pas être négatif");
        }
        if (input.salaireMensuelBrutEuros() < 0) {
            throw new IllegalArgumentException("Le salaire mensuel ne peut pas être négatif");
        }
        if (input.remunerationBrute12MoisEuros() < 0) {
            throw new IllegalArgumentException("La rémunération 12 mois ne peut pas être négative");
        }

        List<PointNonConformite> points = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        boolean obligationApplicable = input.effectifEntreprise() > 0
                && input.effectifEntreprise() < SEUIL_EFFECTIF_CSP;

        if (!obligationApplicable) {
            // CSP non applicable — entreprise hors champ du dispositif. Pas
            // de point de non-conformité, l'outil retourne un état descriptif.
            messages.add("Entreprise de " + input.effectifEntreprise() + " salariés — "
                    + "le CSP est réservé aux entreprises de moins de "
                    + SEUIL_EFFECTIF_CSP + " salariés (L. 1233-66 CT). "
                    + "Au-delà, le régime applicable est le congé de reclassement "
                    + "L. 1233-71+ CT.");
            bases.add("L. 1233-66 CT — champ d'application du CSP (entreprises < 1 000 salariés)");
            // Pas d'ASP calculée si non applicable.
            return new Result(
                    false,
                    ConformiteCsp.NON_CONFORME,
                    0,
                    List.of(),
                    null,
                    null,
                    0,
                    List.copyOf(bases),
                    List.copyOf(messages),
                    "FRANCE"
            );
        }

        // ── 1. Obligation de proposition du CSP (L. 1233-66 CT) ───────────────
        boolean cspPropose = Boolean.TRUE.equals(input.cspPropose());
        if (!cspPropose) {
            points.add(new PointNonConformite(
                    CodeNonConformite.DT44_OBLIGATION_CSP,
                    "Obligation de proposition du CSP non satisfaite",
                    "L. 1233-66 CT — obligation de l'employeur de proposer le CSP",
                    50,
                    "L'employeur n'a pas proposé le CSP au salarié alors que l'entreprise "
                    + "compte moins de " + SEUIL_EFFECTIF_CSP + " salariés (effectif déclaré : "
                    + input.effectifEntreprise() + "). La sanction est l'obligation de verser "
                    + "à Pôle emploi une contribution équivalente à 2 mois de salaire brut "
                    + "moyen et d'indemniser le salarié du préjudice subi."));
            messages.add("ALERTE : aucune proposition de CSP — obligation employeur non satisfaite (L. 1233-66 CT).");
            bases.add("L. 1233-66 CT — obligation de proposition du CSP");
        }

        // ── 2. Document d'information remis ──────────────────────────────────
        boolean documentRemis = Boolean.TRUE.equals(input.documentInformationRemis());
        if (cspPropose && !documentRemis) {
            points.add(new PointNonConformite(
                    CodeNonConformite.DT44_DOCUMENT_REMIS,
                    "Document d'information CSP non remis au salarié",
                    "ANI CSP 19/07/2011 — document d'information obligatoire",
                    25,
                    "L'employeur n'a pas remis le document d'information sur le CSP "
                    + "(contenu de la convention, droits ouverts, opérateur en charge "
                    + "du suivi, coordonnées). Ce manquement vicie la proposition et "
                    + "peut fonder une demande de dommages-intérêts."));
            bases.add("ANI CSP 19/07/2011 — document d'information CSP");
        }

        // ── 3. Délai de réflexion mentionné (21 jours, L. 1233-67 CT) ─────────
        boolean delaiMentionne = Boolean.TRUE.equals(input.delaiReflexionMentionne());
        if (cspPropose && !delaiMentionne) {
            points.add(new PointNonConformite(
                    CodeNonConformite.DT44_DELAI_REFLEXION,
                    "Délai de réflexion de " + DELAI_REFLEXION_JOURS + " jours non mentionné",
                    "L. 1233-67 CT — délai de réflexion 21 jours calendaires",
                    20,
                    "La proposition de CSP doit informer expressément le salarié du délai "
                    + "de réflexion de " + DELAI_REFLEXION_JOURS + " jours calendaires à "
                    + "compter de la remise. L'absence de mention prive le salarié d'un "
                    + "consentement éclairé."));
            bases.add("L. 1233-67 CT — délai de réflexion 21 jours");
        }

        // ── 4. Cohérence date de remise vs entretien préalable ───────────────
        boolean dateRemiseIncoherente = false;
        if (cspPropose && input.dateRemise() != null && input.dateEntretienPrealable() != null) {
            // La remise doit intervenir lors de l'entretien préalable ou à la
            // notification individuelle (PSE). Une remise antérieure à
            // l'entretien préalable est anormale. Une remise très postérieure
            // (> 7 jours après l'entretien) signale une notification tardive.
            long ecartJours = ChronoUnit.DAYS.between(
                    input.dateEntretienPrealable(), input.dateRemise());
            if (ecartJours < 0 || ecartJours > 7) {
                dateRemiseIncoherente = true;
                points.add(new PointNonConformite(
                        CodeNonConformite.DT44_DATE_REMISE,
                        "Date de remise du CSP incohérente avec l'entretien préalable",
                        "L. 1233-65 CT — remise lors de l'entretien préalable",
                        15,
                        "La proposition de CSP doit être remise lors de l'entretien "
                        + "préalable (ou à la notification individuelle en cas de PSE). "
                        + "Écart constaté avec la date de l'entretien préalable : "
                        + ecartJours + " jour(s). Une remise hors séquence procédurale "
                        + "fragilise la régularité de la procédure."));
                bases.add("L. 1233-65 CT — remise du CSP lors de l'entretien préalable");
            }
        }

        // ── 5. Verdict de conformité ─────────────────────────────────────────
        ConformiteCsp conformite;
        if (!cspPropose) {
            // Pas de proposition = non-conformité radicale.
            conformite = ConformiteCsp.NON_CONFORME;
        } else if (points.isEmpty()) {
            conformite = ConformiteCsp.CONFORME;
        } else if (dateRemiseIncoherente && !documentRemis && !delaiMentionne) {
            // Plusieurs vices simultanés = non-conformité globale.
            conformite = ConformiteCsp.NON_CONFORME;
        } else {
            conformite = ConformiteCsp.PARTIELLEMENT_CONFORME;
        }

        // Score décroissant via le poids des points de non-conformité.
        int score = 100;
        for (PointNonConformite p : points) {
            score -= p.poids();
        }
        score = Math.max(0, Math.min(100, score));

        // ── 6. Messages d'orientation selon le verdict ───────────────────────
        switch (conformite) {
            case CONFORME -> {
                messages.add("La proposition de CSP est conforme aux exigences "
                        + "L. 1233-65 à L. 1233-70 CT.");
                bases.add(0, "L. 1233-65 à L. 1233-70 CT — régime du CSP");
            }
            case PARTIELLEMENT_CONFORME -> {
                messages.add("La proposition de CSP comporte un ou plusieurs vices. "
                        + "Le salarié peut contester la régularité de la procédure et "
                        + "solliciter une indemnisation du préjudice subi.");
                bases.add(0, "L. 1233-65 à L. 1233-70 CT — régime du CSP");
            }
            case NON_CONFORME -> {
                messages.add("La proposition de CSP est non conforme. L'employeur "
                        + "s'expose à une contribution Pôle emploi (équivalente à 2 mois "
                        + "de salaire brut moyen — L. 1233-66 CT) et à des dommages-intérêts "
                        + "au profit du salarié.");
                bases.add(0, "L. 1233-65 à L. 1233-70 CT — régime du CSP");
            }
        }

        // ── 7. Information adhésion / refus du salarié ───────────────────────
        if (input.adhesionSalarie() != null) {
            if (Boolean.TRUE.equals(input.adhesionSalarie())) {
                messages.add("Adhésion du salarié au CSP : la rupture est amiable hors "
                        + "préavis (L. 1233-67 CT) — le salarié relève du dispositif CSP "
                        + "pendant " + DUREE_ASP_MOIS + " mois.");
                bases.add("L. 1233-67 CT — adhésion CSP = rupture amiable hors préavis");
            } else {
                messages.add("Refus du salarié : licenciement économique normal avec préavis "
                        + "et indemnisation Pôle emploi en régime ARE classique (non ASP).");
            }
        }

        // ── 8. Calcul de l'ASP estimée (75 % SJR × 12 mois) ──────────────────
        Double aspJournaliere = null;
        Double aspAnnuelle = null;
        if (input.remunerationBrute12MoisEuros() > 0) {
            double sjr = input.remunerationBrute12MoisEuros() / JOURS_ANNEE;
            aspJournaliere = Math.round(sjr * TAUX_ASP_REGIME_COMMUN * 100.0) / 100.0;
            aspAnnuelle = Math.round(aspJournaliere * JOURS_ANNEE * 100.0) / 100.0;
            messages.add("ASP estimée (régime droit commun, 75 % du SJR) : "
                    + java.text.NumberFormat.getNumberInstance(java.util.Locale.FRANCE)
                            .format(Math.round(aspAnnuelle))
                    + " € / an pendant " + DUREE_ASP_MOIS + " mois.");
            bases.add("ANI CSP 19/07/2011 / DARES — taux ASP 75 % du SJR pendant 12 mois");
        }

        return new Result(
                obligationApplicable,
                conformite,
                score,
                List.copyOf(points),
                aspJournaliere,
                aspAnnuelle,
                DUREE_ASP_MOIS,
                List.copyOf(bases),
                List.copyOf(messages),
                "FRANCE"
        );
    }

    /** Helper de calcul du nombre de jours entre deux dates (gestion nulls). */
    static long joursEntre(LocalDate from, LocalDate to) {
        if (from == null || to == null) return 0L;
        return ChronoUnit.DAYS.between(from, to);
    }
}
