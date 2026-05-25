package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-212-33 : moteur d'analyse de requalification d'un contrat à temps
 * partiel en temps complet (FRANCE) — fondements :
 * <ul>
 *   <li><b>L. 3123-1 à L. 3123-20 CT</b> — régime du temps partiel ;</li>
 *   <li><b>L. 3123-6 CT</b> — mentions obligatoires du contrat écrit
 *       (durée hebdomadaire / mensuelle, répartition des horaires entre
 *       jours de la semaine ou semaines du mois, conditions de
 *       modification de la répartition, limites des heures
 *       complémentaires) ; absence ⇒ <b>présomption de temps complet</b>
 *       réfragable (Cass. soc. 22/01/1992 n°88-44.196) ;</li>
 *   <li><b>L. 3123-9 CT</b> — plafond des heures complémentaires fixé à
 *       1/10 de la durée contractuelle, ou 1/3 en cas d'accord collectif
 *       (taux majoré 10 % au-delà de 1/10, 25 % au-delà de 1/10 pour
 *       accord). Dépassement systématique du 1/3 ⇒ requalification en
 *       temps complet (Cass. soc. 06/04/2011 n°09-72.165 ; Cass. soc.
 *       12/12/2018 n°17-15.587) ;</li>
 *   <li><b>L. 3123-12 CT</b> — modification de la répartition soumise à
 *       clause contractuelle, motif et délai de prévenance ≥ 7 jours
 *       ouvrés ; à défaut, refus salarié possible sans faute ;</li>
 *   <li><b>L. 3245-1 CT</b> — prescription triennale du rappel de
 *       salaire (point de départ : exigibilité, plafond 36 mois).</li>
 * </ul>
 *
 * <p>Verdict 3 niveaux :
 * <ul>
 *   <li><b>REQUALIFICATION_PROBABLE</b> : absence des mentions L. 3123-6
 *       OU heures complémentaires &gt; 1/3 de manière structurelle —
 *       le dossier est solide pour obtenir la requalification.</li>
 *   <li><b>REQUALIFICATION_POSSIBLE</b> : irrégularités partielles (HC
 *       au-dessus de 1/10 mais sous 1/3, modification unilatérale sans
 *       procédure) — possible avec faisceau d'indices.</li>
 *   <li><b>PAS_DE_REQUALIFICATION</b> : mentions présentes, HC ≤ 1/10,
 *       répartition respectée.</li>
 * </ul>
 *
 * <p><b>Pays</b> : FRANCE uniquement — le régime BE du temps partiel
 * (Loi 03/07/1978, CCT n°35) est juridiquement distinct.</p>
 */
public final class TempsPartielRequalificationCalculator {

    /** Verdict de l'analyse de requalification. */
    public enum AnalyseTempsPartielRequalification {
        REQUALIFICATION_PROBABLE,
        REQUALIFICATION_POSSIBLE,
        PAS_DE_REQUALIFICATION
    }

    /** Code structuré d'un facteur d'analyse F-IA-03. */
    public enum CodeCritere {
        DT49_MENTIONS_DUREE,
        DT49_MENTIONS_REPARTITION,
        DT49_HC_ABUSIVES,
        DT49_MODIFICATION_UNILATERALE
    }

    /** Facteur du dossier — exposé dans la réponse API. */
    public record FacteurRequalification(
            CodeCritere code,
            String libelle,
            String fondement,
            int poids
    ) {}

    /** Seuil légal d'heures complémentaires sans accord collectif (1/10 — L. 3123-9 CT). */
    public static final double SEUIL_HC_BASE = 0.10;

    /**
     * Plafond absolu d'heures complémentaires (1/3 — L. 3123-9 CT, accord
     * collectif). Dépassement systématique = requalification probable.
     */
    public static final double PLAFOND_HC_ABSOLU = 1.0 / 3.0;

    /** Prescription triennale du rappel de salaire (L. 3245-1 CT). */
    public static final int RAPPEL_SALAIRE_MAX_MOIS = 36;

    /** Résultat calculé par {@link #compute}. */
    public record Result(
            AnalyseTempsPartielRequalification analyseRequalification,
            int scoreRequalification,
            List<FacteurRequalification> facteursRequalification,
            Double rappelSalaire3AnsEstimeEuros,
            List<String> basesJuridiques,
            List<String> messages,
            String country
    ) {}

    private TempsPartielRequalificationCalculator() {}

    /**
     * Point d'entrée principal.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace (doit être FRANCE)
     * @return résultat calculé
     * @throws IllegalArgumentException si {@code country != "FRANCE"} ou si
     *         {@code input} est null
     */
    public static Result compute(TempsPartielRequalificationInput input, String country) {
        if (!"FRANCE".equalsIgnoreCase(country)) {
            throw new IllegalArgumentException(
                    "Outil réservé au droit du travail FRANCE (régime du temps partiel L. 3123-1+ CT) — pays détecté : " + country);
        }
        if (input == null) {
            throw new IllegalArgumentException("Données d'entrée manquantes");
        }

        List<FacteurRequalification> facteurs = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        int score = 0;

        double duree = input.dureeContractuelleHeures() != null ? input.dureeContractuelleHeures() : 0.0;
        double hcReelles = input.heuresComplementairesReelleMoyenneHeures() != null
                ? input.heuresComplementairesReelleMoyenneHeures() : 0.0;
        boolean dureePresent = Boolean.TRUE.equals(input.mentionsDureePresentes());
        boolean repartitionPresent = Boolean.TRUE.equals(input.mentionsRepartitionPresentes());
        boolean hcMentionsPresent = Boolean.TRUE.equals(input.mentionsHCPresentes());
        boolean modifUnilaterale = Boolean.TRUE.equals(input.modificationRepartitionUnilaterale());

        // ── 1. Mention de la durée contractuelle ──────────────────────────────
        if (!dureePresent) {
            facteurs.add(new FacteurRequalification(
                    CodeCritere.DT49_MENTIONS_DUREE,
                    "Mention de la durée hebdomadaire / mensuelle absente du contrat écrit",
                    "L. 3123-6 CT — mentions obligatoires ; Cass. soc. 22/01/1992 — présomption de temps complet",
                    35));
            score += 35;
            bases.add("L. 3123-6 CT — mentions obligatoires du contrat de travail à temps partiel");
            bases.add("Cass. soc. 22/01/1992 n°88-44.196 — présomption de temps complet en l'absence des mentions obligatoires");
            messages.add("PRÉSOMPTION DE TEMPS COMPLET : la durée hebdomadaire / mensuelle n'est pas mentionnée dans le contrat (L. 3123-6 CT) — présomption réfragable de temps complet (Cass. soc. 22/01/1992). C'est à l'employeur de démontrer la durée réellement convenue ET que le salarié n'était pas placé dans l'impossibilité de prévoir son rythme de travail.");
        } else {
            facteurs.add(new FacteurRequalification(
                    CodeCritere.DT49_MENTIONS_DUREE,
                    "Mention de la durée présente dans le contrat",
                    "L. 3123-6 CT",
                    0));
        }

        // ── 2. Mention de la répartition entre jours / semaines ───────────────
        if (!repartitionPresent) {
            facteurs.add(new FacteurRequalification(
                    CodeCritere.DT49_MENTIONS_REPARTITION,
                    "Répartition des horaires (jours de la semaine / semaines du mois) absente du contrat écrit",
                    "L. 3123-6 CT — mentions obligatoires de la répartition",
                    25));
            score += 25;
            if (!bases.contains("L. 3123-6 CT — mentions obligatoires du contrat de travail à temps partiel")) {
                bases.add("L. 3123-6 CT — mentions obligatoires du contrat de travail à temps partiel");
            }
            messages.add("PRÉSOMPTION DE TEMPS COMPLET : la répartition des horaires entre les jours de la semaine ou les semaines du mois n'est pas définie au contrat (L. 3123-6 CT). Le salarié se trouvait dans l'impossibilité de prévoir son rythme de travail (Cass. soc. 22/01/1992).");
        } else {
            facteurs.add(new FacteurRequalification(
                    CodeCritere.DT49_MENTIONS_REPARTITION,
                    "Répartition des horaires mentionnée dans le contrat",
                    "L. 3123-6 CT",
                    0));
        }

        // ── 3. Heures complémentaires abusives ────────────────────────────────
        double ratioHC = duree > 0 ? hcReelles / duree : 0.0;
        boolean depassement13 = ratioHC > PLAFOND_HC_ABSOLU + 1e-9;
        boolean depassement110 = ratioHC > SEUIL_HC_BASE + 1e-9;
        if (depassement13) {
            facteurs.add(new FacteurRequalification(
                    CodeCritere.DT49_HC_ABUSIVES,
                    String.format(
                            "Heures complémentaires moyennes %.2f h/sem (= %.1f %% de la durée contractuelle %.2f h) — DÉPASSEMENT du plafond légal 1/3 (L. 3123-9 CT)",
                            hcReelles, ratioHC * 100.0, duree),
                    "L. 3123-9 CT — plafond 1/3 ; Cass. soc. 06/04/2011 n°09-72.165 ; Cass. soc. 12/12/2018 n°17-15.587",
                    30));
            score += 30;
            bases.add("L. 3123-9 CT — plafond des heures complémentaires (1/10 ou 1/3 avec accord collectif)");
            bases.add("Cass. soc. 06/04/2011 n°09-72.165 — requalification en cas de dépassement du plafond 1/3");
            messages.add(String.format(
                    "HEURES COMPLÉMENTAIRES ABUSIVES : moyenne %.2f h/sem (= %.1f %% de la durée contractuelle), au-delà du plafond légal 1/3 fixé par L. 3123-9 CT. Le contrat se trouve porté à un horaire équivalent au temps complet — requalification probable.",
                    hcReelles, ratioHC * 100.0));
        } else if (depassement110) {
            int poids = hcMentionsPresent ? 8 : 12;
            facteurs.add(new FacteurRequalification(
                    CodeCritere.DT49_HC_ABUSIVES,
                    String.format(
                            "Heures complémentaires moyennes %.2f h/sem (= %.1f %% de la durée contractuelle %.2f h) — au-dessus du seuil de base 1/10 mais sous 1/3 ; vérifier l'accord collectif applicable",
                            hcReelles, ratioHC * 100.0, duree),
                    "L. 3123-9 CT — seuil 1/10 sans accord collectif",
                    poids));
            score += poids;
            bases.add("L. 3123-9 CT — seuil de base 1/10 des heures complémentaires sans accord collectif");
            messages.add(String.format(
                    "HC > 1/10 (%.2f h/sem = %.1f %% de la durée contractuelle) : si aucun accord collectif n'autorise le seuil 1/3, le taux majoré 25 %% s'applique au-delà de 1/10 (L. 3123-19 CT).",
                    hcReelles, ratioHC * 100.0));
        } else {
            facteurs.add(new FacteurRequalification(
                    CodeCritere.DT49_HC_ABUSIVES,
                    String.format(
                            "Heures complémentaires %.2f h/sem (= %.1f %% de la durée contractuelle %.2f h) — sous le seuil 1/10",
                            hcReelles, ratioHC * 100.0, duree),
                    "L. 3123-9 CT",
                    0));
        }

        // ── 4. Modification unilatérale de la répartition ─────────────────────
        if (modifUnilaterale) {
            facteurs.add(new FacteurRequalification(
                    CodeCritere.DT49_MODIFICATION_UNILATERALE,
                    "Modification unilatérale de la répartition par l'employeur sans respect de la procédure (clause + motif + délai 7 jours ouvrés)",
                    "L. 3123-12 CT ; Cass. soc. 23/11/2010 n°09-66.072 — refus salarié non fautif",
                    15));
            score += 15;
            bases.add("L. 3123-12 CT — modification de la répartition soumise à clause contractuelle, motif et délai 7 jours ouvrés");
            messages.add("MODIFICATION UNILATÉRALE : à défaut de clause contractuelle prévoyant les cas de modification ET de respect du délai de prévenance de 7 jours ouvrés, le salarié peut refuser sans faute (Cass. soc. 23/11/2010 n°09-66.072).");
        } else {
            facteurs.add(new FacteurRequalification(
                    CodeCritere.DT49_MODIFICATION_UNILATERALE,
                    "Aucune modification unilatérale de la répartition documentée",
                    "L. 3123-12 CT",
                    0));
        }

        // ── 5. Verdict ────────────────────────────────────────────────────────
        AnalyseTempsPartielRequalification verdict;
        boolean presomption = !dureePresent || !repartitionPresent;
        if (presomption || depassement13) {
            verdict = AnalyseTempsPartielRequalification.REQUALIFICATION_PROBABLE;
            messages.add(0, "REQUALIFICATION PROBABLE — la présomption légale de temps complet (mentions L. 3123-6 absentes) ou le dépassement du plafond 1/3 (L. 3123-9 CT) ouvre la voie à la requalification en temps complet et au rappel de salaire sur 3 ans (L. 3245-1 CT).");
        } else if (depassement110 || modifUnilaterale) {
            verdict = AnalyseTempsPartielRequalification.REQUALIFICATION_POSSIBLE;
            messages.add(0, "REQUALIFICATION POSSIBLE — éléments d'irrégularité présents (HC au-dessus de 1/10, modification unilatérale). Constituer un faisceau d'indices pour étayer la demande.");
        } else {
            verdict = AnalyseTempsPartielRequalification.PAS_DE_REQUALIFICATION;
            messages.add(0, "PAS DE REQUALIFICATION : les mentions L. 3123-6 sont présentes, les heures complémentaires sont sous le seuil 1/10, la répartition est respectée. Le contrat à temps partiel est en l'état régulier.");
        }

        // ── 6. Rappel de salaire estimé (3 ans max — L. 3245-1) ──────────────
        Double rappelSalaire3Ans = null;
        boolean requalification =
                verdict == AnalyseTempsPartielRequalification.REQUALIFICATION_PROBABLE
                || verdict == AnalyseTempsPartielRequalification.REQUALIFICATION_POSSIBLE;
        Double salaire = input.salaireMensuelContractuelEuros();
        if (requalification && duree > 0 && salaire != null && salaire > 0) {
            // Le rappel = différence entre temps complet (35h/sem) et durée
            // contractuelle, projeté sur la période plafonnée à 36 mois.
            double dureeTempsComplet = 35.0;
            if (duree < dureeTempsComplet) {
                double ratio = (dureeTempsComplet - duree) / duree;
                double rappelMensuel = salaire * ratio;
                int anciennete = input.ancienneteMois() != null ? input.ancienneteMois() : 0;
                int mois = Math.max(0, Math.min(RAPPEL_SALAIRE_MAX_MOIS, anciennete));
                rappelSalaire3Ans = rappelMensuel * mois;
                bases.add("L. 3245-1 CT — prescription triennale du rappel de salaire (36 mois max)");
                messages.add(String.format(
                        "Rappel de salaire estimé : %.2f € (différence temps complet 35 h vs durée contractuelle %.2f h, sur %d mois d'ancienneté plafonnés à 36).",
                        rappelSalaire3Ans, duree, mois));
            }
        }

        // ── 7. Bases juridiques transverses ──────────────────────────────────
        bases.add("L. 3123-1 à L. 3123-20 CT — régime du temps partiel");
        if (!bases.contains("L. 3245-1 CT — prescription triennale du rappel de salaire (36 mois max)")) {
            bases.add("L. 3245-1 CT — prescription triennale du rappel de salaire (3 ans)");
        }

        // Borner le score à [0, 100].
        score = Math.max(0, Math.min(100, score));

        return new Result(
                verdict,
                score,
                List.copyOf(facteurs),
                rappelSalaire3Ans,
                List.copyOf(bases),
                List.copyOf(messages),
                "FRANCE"
        );
    }
}
