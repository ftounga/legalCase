package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-212-31 : moteur de vérification de conformité de la procédure
 * d'élections du comité social et économique — CSE (FRANCE) — art. L. 2314-1
 * à L. 2314-37 CT ; R. 2314-1 et s. CT ; ordonnance n°2017-1386 du
 * 22/09/2017 instituant le CSE ; loi n°2018-217 du 29/03/2018 de
 * ratification.
 *
 * <p>4 axes de vérification :
 * <ul>
 *   <li><b>PAP</b> — protocole d'accord préélectoral négocié avec les OS
 *       représentatives (L. 2314-6 CT). Absent → IRREGULARITE_MAJEURE.</li>
 *   <li><b>Délai d'invitation OS</b> — au moins 2 mois avant l'expiration
 *       des mandats en cours (L. 2314-4 CT). Non respecté → IRREGULARITE_MINEURE.</li>
 *   <li><b>Collèges</b> — au moins 2 collèges (ouvriers/employés + agents
 *       de maîtrise/cadres) sauf accord majoritaire de regroupement
 *       (L. 2314-11 CT).</li>
 *   <li><b>Contestation des résultats</b> — délai de <b>15 jours</b> à
 *       compter de l'élection (L. 2314-32 CT). Au-delà, le tribunal
 *       judiciaire saisi en contestation est irrecevable.</li>
 * </ul>
 *
 * <p>L'obligation d'organiser des élections CSE s'applique aux entreprises
 * d'au moins 11 salariés (L. 2311-2 CT). En deçà, le moteur émet un
 * message « CSE non obligatoire ». Si un syndicat ne présente pas de
 * candidats au 1er tour, l'employeur dresse un procès-verbal de carence
 * (R. 2314-30 CT).</p>
 *
 * <p><b>Pays</b> : FRANCE uniquement — la procédure CSE est strictement
 * française. La BE dispose d'un régime distinct (conseil d'entreprise +
 * délégation syndicale, loi du 4/8/1996 + AR 25/2/1971), hors périmètre.</p>
 */
public final class ElectionsCseConformiteCalculator {

    /** Verdict de conformité — 3 niveaux. */
    public enum AnalyseElectionsCse {
        CONFORME,
        IRREGULARITE_MINEURE,
        IRREGULARITE_MAJEURE
    }

    /** Code structuré d'un point d'irrégularité F-IA-03. */
    public enum CodeCritere {
        DT65_PAP_NEGOCIE,
        DT65_DELAI_INVITATION,
        DT65_COLLEGES,
        DT65_CONTESTATION
    }

    /** Point d'irrégularité retourné dans la réponse API. */
    public record PointIrregularite(
            CodeCritere code,
            String libelle,
            String fondement
    ) {}

    /** Délai légal de contestation des résultats — L. 2314-32 CT (jours). */
    public static final int DELAI_CONTESTATION_JOURS = 15;

    /** Seuil légal d'obligation CSE — L. 2311-2 CT (effectif minimal). */
    public static final int SEUIL_OBLIGATION_CSE = 11;

    /** Résultat calculé par {@link #compute}. */
    public record Result(
            AnalyseElectionsCse analyseElectionsCse,
            int scoreConformite,
            List<PointIrregularite> pointsIrregularite,
            int delaiContestationJours,
            LocalDate dateLimitContestationSiConnue,
            boolean alerteDelaiContestation,
            List<String> basesJuridiques,
            List<String> messages,
            String country
    ) {}

    private ElectionsCseConformiteCalculator() {}

    /**
     * Point d'entrée principal.
     *
     * @param input     données saisies par l'avocat
     * @param country   pays du workspace
     * @param reference date courante de référence pour calculer l'alerte
     *                  de délai de contestation (typiquement {@link LocalDate#now()})
     * @return résultat calculé
     * @throws IllegalArgumentException si {@code country != "FRANCE"} ou si
     *         {@code input} est null.
     */
    public static Result compute(ElectionsCseConformiteInput input, String country, LocalDate reference) {
        if (!"FRANCE".equalsIgnoreCase(country)) {
            throw new IllegalArgumentException(
                    "Outil réservé au droit du travail FRANCE (CSE — L. 2314-1+ CT) — pays détecté : " + country);
        }
        if (input == null) {
            throw new IllegalArgumentException("Données d'entrée manquantes");
        }
        LocalDate today = reference != null ? reference : LocalDate.now();

        List<PointIrregularite> points = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        int score = 100;
        boolean majeure = false;
        boolean mineure = false;

        // ── 1. Effectif & obligation CSE ─────────────────────────────────────
        int effectif = input.effectifEntreprise() != null ? input.effectifEntreprise() : 0;
        if (effectif < SEUIL_OBLIGATION_CSE) {
            messages.add(String.format(
                    "CSE non obligatoire : effectif déclaré %d salariés < seuil légal %d (L. 2311-2 CT).",
                    effectif, SEUIL_OBLIGATION_CSE));
        } else {
            bases.add("L. 2311-2 CT — obligation d'élections CSE pour les entreprises d'au moins 11 salariés");
        }

        // ── 2. Protocole d'accord préélectoral (PAP) — L. 2314-6 CT ─────────
        boolean pap = Boolean.TRUE.equals(input.papNegocieAvecOS());
        if (!pap) {
            points.add(new PointIrregularite(
                    CodeCritere.DT65_PAP_NEGOCIE,
                    "Protocole d'accord préélectoral (PAP) absent ou non négocié avec les organisations syndicales représentatives",
                    "L. 2314-6 CT — exigence du PAP comme socle de la régularité du scrutin"));
            score -= 40;
            majeure = true;
            bases.add("L. 2314-6 CT — négociation du PAP avec les OS représentatives");
            messages.add("IRRÉGULARITÉ MAJEURE : absence de PAP négocié — fondement principal d'une demande d'annulation des élections.");
        } else {
            bases.add("L. 2314-6 CT — PAP négocié, condition de régularité du scrutin");
        }

        // ── 3. Délai d'invitation des OS — L. 2314-4 CT ─────────────────────
        boolean delaiInvitation = Boolean.TRUE.equals(input.delaiInvitationOSRespectee());
        if (!delaiInvitation) {
            points.add(new PointIrregularite(
                    CodeCritere.DT65_DELAI_INVITATION,
                    "Délai d'invitation des OS à la négociation du PAP non respecté (moins de 2 mois avant l'expiration des mandats)",
                    "L. 2314-4 CT — invitation des OS au moins 2 mois avant l'expiration des mandats"));
            score -= 15;
            mineure = true;
            bases.add("L. 2314-4 CT — délai d'invitation OS à la négociation PAP");
        }

        // ── 4. Collèges électoraux — L. 2314-11 CT ──────────────────────────
        boolean colleges = Boolean.TRUE.equals(input.collegesConformes());
        if (!colleges) {
            points.add(new PointIrregularite(
                    CodeCritere.DT65_COLLEGES,
                    "Collèges électoraux non conformes (moins de 2 collèges sans accord majoritaire de regroupement)",
                    "L. 2314-11 CT — au moins 2 collèges ; regroupement par accord majoritaire"));
            score -= 25;
            majeure = true;
            bases.add("L. 2314-11 CT — composition des collèges électoraux");
        }

        // ── 5. Contestation des résultats — L. 2314-32 CT (délai 15 jours) ──
        boolean contestes = Boolean.TRUE.equals(input.resultatsContestes());
        LocalDate dateElection = input.dateElection();
        LocalDate dateLimitContestation = null;
        boolean alerteDelai = false;
        if (dateElection != null) {
            dateLimitContestation = dateElection.plusDays(DELAI_CONTESTATION_JOURS);
            long joursRestants = ChronoUnit.DAYS.between(today, dateLimitContestation);
            if (joursRestants >= 0 && joursRestants <= DELAI_CONTESTATION_JOURS) {
                alerteDelai = true;
                messages.add(String.format(
                        "ALERTE DÉLAI : la contestation des résultats doit être engagée au plus tard le %s — il reste %d jour(s) (L. 2314-32 CT — délai 15 jours).",
                        dateLimitContestation, Math.max(0, joursRestants)));
            } else if (joursRestants < 0) {
                messages.add(String.format(
                        "DÉLAI EXPIRÉ : la date limite de contestation des résultats (%s) est dépassée — l'action devant le tribunal judiciaire est en l'état irrecevable (L. 2314-32 CT).",
                        dateLimitContestation));
            }
        }
        if (contestes) {
            points.add(new PointIrregularite(
                    CodeCritere.DT65_CONTESTATION,
                    "Résultats contestés — saisine du tribunal judiciaire dans le délai de 15 jours requise",
                    "L. 2314-32 CT — délai de 15 jours pour contester l'élection"));
            bases.add("L. 2314-32 CT — délai de contestation des élections (15 jours)");
            String motif = input.motifContestation();
            if (motif != null && !motif.isBlank()) {
                messages.add("Motif de contestation invoqué : " + motif.trim());
            }
        }

        // ── 6. Verdict ───────────────────────────────────────────────────────
        AnalyseElectionsCse verdict;
        if (majeure) {
            verdict = AnalyseElectionsCse.IRREGULARITE_MAJEURE;
        } else if (mineure) {
            verdict = AnalyseElectionsCse.IRREGULARITE_MINEURE;
        } else {
            verdict = AnalyseElectionsCse.CONFORME;
            messages.add(0, "Procédure électorale CSE conforme aux exigences principales (PAP, délais, collèges).");
        }
        if (verdict == AnalyseElectionsCse.IRREGULARITE_MAJEURE) {
            messages.add(0, "IRRÉGULARITÉ MAJEURE — la procédure présente une ou plusieurs irrégularités susceptibles d'entraîner l'annulation des élections.");
        } else if (verdict == AnalyseElectionsCse.IRREGULARITE_MINEURE) {
            messages.add(0, "IRRÉGULARITÉ MINEURE — la procédure présente une ou plusieurs irrégularités de forme à corriger ou à invoquer si elles ont eu une influence sur le scrutin (jurisprudence constante).");
        }

        // ── 7. Bases juridiques transverses ──────────────────────────────────
        bases.add("Ordonnance n°2017-1386 du 22/09/2017 — institution du comité social et économique (CSE)");
        bases.add("L. 2314-1 à L. 2314-37 CT — organisation et déroulement des élections CSE");

        // Borner le score à [0, 100].
        score = Math.max(0, Math.min(100, score));

        return new Result(
                verdict,
                score,
                List.copyOf(points),
                DELAI_CONTESTATION_JOURS,
                dateLimitContestation,
                alerteDelai,
                List.copyOf(bases),
                List.copyOf(messages),
                "FRANCE"
        );
    }
}
