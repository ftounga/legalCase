package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-212-23 : moteur d'analyse d'une discrimination salariale fondée sur le
 * sexe (F-DT-56-egalite-salariale-femmes-hommes, FRANCE — L. 1142-7 à
 * L. 1142-10 CT — principe d'égalité de rémunération et obligations
 * employeur ; L. 1144-1 CT — charge de la preuve aménagée ; loi 05/09/2018
 * « avenir professionnel » instituant l'index égalité ; L. 1132-1 CT —
 * interdiction des discriminations ; L. 3221-2 CT — principe à travail égal
 * salaire égal ; L. 1134-5 CT — prescription 5 ans).
 *
 * <p>Verdict 3 niveaux :
 * <ul>
 *   <li>{@code DISCRIMINATION_PROBABLE} — écart salarial &gt; 20 % +
 *       comparants identifiés de sexe opposé mieux rémunérés à qualification
 *       et ancienneté équivalentes + absence de justifications objectives de
 *       l'employeur. La charge de la preuve aménagée (L. 1144-1) joue
 *       pleinement, dommages non plafonnés (hors barème Macron).</li>
 *   <li>{@code DISCRIMINATION_POSSIBLE} — écart modéré (entre 5 % et 20 %)
 *       OU comparants identifiés sans écart marqué OU index égalité faible
 *       (&lt; 75/100). La preuve d'une discrimination reste à étayer.</li>
 *   <li>{@code PAS_DE_DISCRIMINATION_APPARENTE} — écart négligeable
 *       (&lt; 5 %) OU justifications objectives produites par l'employeur OU
 *       absence de comparants identifiables.</li>
 * </ul>
 *
 * <p><b>Pays</b> : FRANCE uniquement — l'index égalité L. 1142-8 CT et la
 * charge de la preuve aménagée L. 1144-1 CT sont propres au droit français
 * (côté BE, régime distinct fondé sur la loi du 22/04/2012 et la CCT n°25).
 */
public final class EgaliteSalarialeFhCalculator {

    /** Verdict d'analyse de la discrimination salariale — 3 niveaux. */
    public enum AnalyseEgaliteSalariale {
        DISCRIMINATION_PROBABLE,
        DISCRIMINATION_POSSIBLE,
        PAS_DE_DISCRIMINATION_APPARENTE
    }

    /** Code structuré d'un facteur de disparité (F-IA-03). */
    public enum CodeCritere {
        DT56_ECART_SALAIRE,
        DT56_COMPARANTS,
        DT56_INDEX_EGALITE,
        DT56_JUSTIFICATIONS_OBJECTIVES
    }

    /** Facteur de disparité exposé dans la réponse API. */
    public record FacteurDisparite(
            CodeCritere code,
            String libelle,
            String fondement,
            int poids,
            String explication
    ) {}

    /** Résultat calculé par {@link #compute}. */
    public record Result(
            AnalyseEgaliteSalariale analyseEgaliteSalariale,
            int scoreDiscrimination,
            List<FacteurDisparite> facteursDisparite,
            int prescriptionActionAns,
            String alerteNonPlafonnement,
            List<String> basesJuridiques,
            List<String> messages,
            String country
    ) {}

    /** Prescription de l'action individuelle — 5 ans (L. 1134-5 CT). */
    private static final int PRESCRIPTION_ACTION_ANS = 5;

    /** Seuil au-delà duquel un écart en pourcentage est jugé significatif. */
    private static final double SEUIL_ECART_SIGNIFICATIF_PCT = 20.0;

    /** Seuil au-delà duquel un écart en pourcentage devient présomption forte. */
    private static final double SEUIL_ECART_MODERE_PCT = 5.0;

    /** Score conformité de l'index égalité (loi 05/09/2018 — pénalités si &lt;). */
    private static final int SEUIL_INDEX_EGALITE = 75;

    /** Alerte sur le non-plafonnement (invariant produit affiché en permanence). */
    private static final String ALERTE_NON_PLAFONNEMENT =
            "Dommages-intérêts non plafonnés — l'indemnisation d'une "
                    + "discrimination salariale échappe au barème Macron de "
                    + "l'art. L. 1235-3 CT (réservé au licenciement sans cause "
                    + "réelle et sérieuse). Cumul possible avec rappel de "
                    + "salaire sur 5 ans (L. 1134-5 CT).";

    private EgaliteSalarialeFhCalculator() {}

    /**
     * Point d'entrée principal.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace
     * @return résultat calculé
     * @throws IllegalArgumentException si {@code country != "FRANCE"} ou si
     *         {@code input} est null, ou si des valeurs sont aberrantes.
     */
    public static Result compute(EgaliteSalarialeFhInput input, String country) {
        if (!"FRANCE".equalsIgnoreCase(country)) {
            throw new IllegalArgumentException(
                    "Outil réservé au droit du travail FRANCE — pays détecté : " + country);
        }
        if (input == null) {
            throw new IllegalArgumentException("Données d'entrée manquantes");
        }
        if (input.sexeSalarie() == null) {
            throw new IllegalArgumentException("Sexe du salarié requis");
        }
        if (input.salaireMensuelBrutSalarieEuros() < 0) {
            throw new IllegalArgumentException(
                    "Le salaire mensuel brut ne peut pas être négatif");
        }
        if (input.ancienneteMois() < 0) {
            throw new IllegalArgumentException("L'ancienneté ne peut pas être négative");
        }
        if (input.nombreComparantsMieuxPayes() < 0) {
            throw new IllegalArgumentException("Le nombre de comparants ne peut pas être négatif");
        }
        if (input.ecartPourcentage() < 0) {
            throw new IllegalArgumentException("L'écart en pourcentage ne peut pas être négatif");
        }
        if (input.scoreIndexEgalite() != null
                && (input.scoreIndexEgalite() < 0 || input.scoreIndexEgalite() > 100)) {
            throw new IllegalArgumentException("Le score de l'index égalité doit être entre 0 et 100");
        }

        List<FacteurDisparite> facteurs = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        int score = 0;

        double ecartPct = input.ecartPourcentage();
        double ecartEuros = input.ecartSalaireMoyenComparantsEuros();
        int nbComparants = input.nombreComparantsMieuxPayes();
        boolean justifications = input.justificationsEmployeurObjectives();
        boolean indexConnu = input.indexEgaliteConnu();
        Integer indexScore = input.scoreIndexEgalite();

        // ── 1. DT56_ECART_SALAIRE — quantification de l'écart de rémunération ─
        if (ecartPct >= SEUIL_ECART_SIGNIFICATIF_PCT) {
            facteurs.add(new FacteurDisparite(
                    CodeCritere.DT56_ECART_SALAIRE,
                    "Écart salarial significatif (≥ 20 %)",
                    "L. 3221-2 CT — principe à travail égal salaire égal ; L. 1142-7 CT — obligation d'égalité de rémunération entre les femmes et les hommes",
                    40,
                    "Écart constaté de " + String.format(java.util.Locale.ROOT, "%.1f", ecartPct) + " % "
                            + "(" + String.format(java.util.Locale.ROOT, "%.2f", ecartEuros) + " € en moyenne) — "
                            + "présomption forte de disparité injustifiée."));
            bases.add("L. 3221-2 CT — à travail égal, salaire égal");
            bases.add("L. 1142-7 CT — égalité de rémunération femmes-hommes");
            score += 40;
        } else if (ecartPct >= SEUIL_ECART_MODERE_PCT) {
            facteurs.add(new FacteurDisparite(
                    CodeCritere.DT56_ECART_SALAIRE,
                    "Écart salarial modéré (entre 5 % et 20 %)",
                    "L. 3221-2 CT — principe à travail égal salaire égal",
                    20,
                    "Écart constaté de " + String.format(java.util.Locale.ROOT, "%.1f", ecartPct) + " % "
                            + "(" + String.format(java.util.Locale.ROOT, "%.2f", ecartEuros) + " € en moyenne) — "
                            + "écart à expliquer par des éléments objectifs."));
            bases.add("L. 3221-2 CT — à travail égal, salaire égal");
            score += 20;
        } else {
            facteurs.add(new FacteurDisparite(
                    CodeCritere.DT56_ECART_SALAIRE,
                    "Écart salarial négligeable (< 5 %)",
                    "L. 3221-2 CT — variation marginale tolérée par la jurisprudence",
                    0,
                    "Écart constaté de " + String.format(java.util.Locale.ROOT, "%.1f", ecartPct) + " % — "
                            + "non significatif au regard du principe d'égalité de rémunération."));
        }

        // ── 2. DT56_COMPARANTS — identification de salariés mieux rémunérés ──
        if (nbComparants >= 2) {
            facteurs.add(new FacteurDisparite(
                    CodeCritere.DT56_COMPARANTS,
                    "Comparants identifiés (≥ 2) à situation comparable",
                    "Cass. soc. constante — la comparaison doit porter sur des salariés de sexe opposé exerçant des fonctions identiques ou de valeur égale (L. 3221-4 CT)",
                    25,
                    nbComparants + " comparant(s) identifié(s) — base solide pour invoquer la charge "
                            + "de la preuve aménagée (L. 1144-1 CT)."));
            bases.add("L. 3221-4 CT — travail de valeur égale");
            bases.add("Cass. soc. constante — comparaison entre comparants à situation identique");
            score += 25;
        } else if (nbComparants == 1) {
            facteurs.add(new FacteurDisparite(
                    CodeCritere.DT56_COMPARANTS,
                    "Un seul comparant identifié",
                    "Cass. soc. constante — un seul comparant peut suffire si la situation est strictement comparable",
                    10,
                    "1 comparant identifié — élément de preuve à consolider par l'analyse des "
                            + "qualifications et de l'ancienneté."));
            score += 10;
        } else {
            facteurs.add(new FacteurDisparite(
                    CodeCritere.DT56_COMPARANTS,
                    "Aucun comparant identifié",
                    "Cass. soc. constante — l'absence de comparant mieux rémunéré rend la démonstration plus difficile",
                    0,
                    "Aucun comparant — la discrimination salariale ne peut pas être étayée par "
                            + "comparaison directe (sauf preuve par l'index égalité)."));
        }

        // ── 3. DT56_INDEX_EGALITE — loi 05/09/2018 « avenir professionnel » ──
        if (indexConnu && indexScore != null) {
            if (indexScore < SEUIL_INDEX_EGALITE) {
                facteurs.add(new FacteurDisparite(
                        CodeCritere.DT56_INDEX_EGALITE,
                        "Score index égalité faible (< 75/100)",
                        "L. 1142-8 CT — entreprises ≥ 50 salariés ; loi 05/09/2018 « avenir professionnel » — mesures correctives sous 3 ans si score < 75 ; sanction financière possible",
                        15,
                        "Score index égalité = " + indexScore + "/100 — élément de preuve aggravant ; "
                                + "l'entreprise est en zone de non-conformité publique."));
                bases.add("L. 1142-8 CT — index égalité ; loi 05/09/2018");
                score += 15;
            } else {
                facteurs.add(new FacteurDisparite(
                        CodeCritere.DT56_INDEX_EGALITE,
                        "Score index égalité conforme (≥ 75/100)",
                        "L. 1142-8 CT — score de conformité de l'index égalité",
                        0,
                        "Score index égalité = " + indexScore + "/100 — l'entreprise se prévaut "
                                + "d'un dispositif global conforme (preuve contraire à étayer dans le cas individuel)."));
            }
        } else {
            facteurs.add(new FacteurDisparite(
                    CodeCritere.DT56_INDEX_EGALITE,
                    "Index égalité inconnu ou non applicable",
                    "L. 1142-8 CT — index obligatoire au-delà de 50 salariés ; non opposable si non publié",
                    0,
                    "Index égalité non communiqué — à demander à l'employeur (preuve par tous moyens : "
                            + "site internet, rapport de situation comparée, DGT)."));
        }

        // ── 4. DT56_JUSTIFICATIONS_OBJECTIVES — L. 1144-1 CT inversion preuve ─
        if (justifications) {
            facteurs.add(new FacteurDisparite(
                    CodeCritere.DT56_JUSTIFICATIONS_OBJECTIVES,
                    "Justifications objectives produites par l'employeur",
                    "L. 1144-1 CT — charge de la preuve aménagée : si l'employeur prouve des éléments objectifs étrangers à toute discrimination, la présomption tombe",
                    -25,
                    "L'employeur fait état d'éléments objectifs (compétences, performances, "
                            + "expérience valorisée) — l'analyse doit en discuter la réalité et la pertinence."));
            bases.add("L. 1144-1 CT — charge de la preuve aménagée");
            score -= 25;
        } else {
            facteurs.add(new FacteurDisparite(
                    CodeCritere.DT56_JUSTIFICATIONS_OBJECTIVES,
                    "Absence de justifications objectives de l'employeur",
                    "L. 1144-1 CT — à défaut d'éléments objectifs étrangers à toute discrimination, la présomption persiste",
                    20,
                    "L'employeur n'apporte pas d'élément objectif pour justifier l'écart — la "
                            + "présomption posée par les faits du salarié n'est pas renversée."));
            bases.add("L. 1144-1 CT — charge de la preuve aménagée");
            score += 20;
        }

        // ── 5. Verdict global ────────────────────────────────────────────────
        AnalyseEgaliteSalariale verdict;
        boolean ecartFort = ecartPct >= SEUIL_ECART_SIGNIFICATIF_PCT;
        boolean comparantsIdentifies = nbComparants >= 1;
        boolean indexFaible = indexConnu && indexScore != null && indexScore < SEUIL_INDEX_EGALITE;

        if (ecartFort && comparantsIdentifies && !justifications) {
            verdict = AnalyseEgaliteSalariale.DISCRIMINATION_PROBABLE;
            messages.add("Discrimination salariale probable — l'écart constaté, les comparants "
                    + "identifiés et l'absence de justifications objectives caractérisent ensemble "
                    + "les faits laissant supposer une discrimination au sens de L. 1144-1 CT. "
                    + "La charge de la preuve est inversée — il appartient désormais à l'employeur "
                    + "de prouver que sa décision est étrangère à toute discrimination.");
        } else if (justifications && ecartPct < SEUIL_ECART_SIGNIFICATIF_PCT) {
            verdict = AnalyseEgaliteSalariale.PAS_DE_DISCRIMINATION_APPARENTE;
            messages.add("Pas de discrimination apparente — l'écart est modéré ou les justifications "
                    + "objectives de l'employeur paraissent recevables. L'action reste possible mais "
                    + "nécessite de discuter en détail la réalité et la pertinence des justifications.");
        } else if (ecartPct >= SEUIL_ECART_MODERE_PCT || comparantsIdentifies || indexFaible) {
            verdict = AnalyseEgaliteSalariale.DISCRIMINATION_POSSIBLE;
            messages.add("Discrimination possible — les éléments accumulés (écart, comparants, "
                    + "index) constituent un faisceau d'indices à consolider. L'action est "
                    + "envisageable mais le dossier de preuve doit être renforcé avant saisine.");
        } else {
            verdict = AnalyseEgaliteSalariale.PAS_DE_DISCRIMINATION_APPARENTE;
            messages.add("Pas de discrimination apparente — les éléments objectifs disponibles ne "
                    + "permettent pas en l'état d'établir des faits laissant supposer une discrimination "
                    + "salariale au sens de L. 1144-1 CT.");
        }

        // ── 6. Alerte invariante : non-plafonnement (toujours affichée) ──────
        messages.add(ALERTE_NON_PLAFONNEMENT);

        // ── 7. Bases juridiques génériques ───────────────────────────────────
        bases.add(0, "Loi n°2018-771 du 05/09/2018 « avenir professionnel » — index égalité");
        bases.add("L. 1132-1 CT — interdiction des discriminations");
        bases.add("L. 1134-5 CT — prescription " + PRESCRIPTION_ACTION_ANS + " ans de l'action individuelle");

        // Borner le score à [0, 100]
        score = Math.max(0, Math.min(100, score));

        return new Result(
                verdict,
                score,
                List.copyOf(facteurs),
                PRESCRIPTION_ACTION_ANS,
                ALERTE_NON_PLAFONNEMENT,
                List.copyOf(bases),
                List.copyOf(messages),
                "FRANCE"
        );
    }
}
