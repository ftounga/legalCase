package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-212-03 : moteur d'analyse de la validité d'une convention individuelle de
 * forfait jours sur l'année et de calcul du rappel d'heures supplémentaires
 * estimé en cas de nullité (FRANCE — L. 3121-58 à L. 3121-66 CT ; Cass. soc.
 * 29/06/2011 n°09-71.107 — accord Syntec ; L. 3245-1 CT prescription 3 ans).
 *
 * <p>L'outil analyse, du point de vue de l'avocat du salarié, la validité de
 * la convention de forfait jours au regard des 5 critères jurisprudentiels et
 * légaux :</p>
 * <ol>
 *   <li>Existence d'un accord collectif d'entreprise ou de branche autorisant
 *       le recours au forfait jours (L. 3121-63 CT).</li>
 *   <li>Garantie effective du suivi de la charge de travail dans l'accord
 *       (Cass. soc. 29/06/2011 — mécanisme d'alerte, entretien annuel,
 *       suivi des temps de repos).</li>
 *   <li>Entretien annuel individuel formalisé sur la charge de travail
 *       (L. 3121-65 CT).</li>
 *   <li>Document de contrôle mensuel des jours travaillés (L. 3121-66 CT).</li>
 *   <li>Cadre autonome ayant la maîtrise de son temps (L. 3121-58 CT).</li>
 * </ol>
 *
 * <p>Le nombre de jours du forfait est plafonné à 218 j/an par défaut
 * (L. 3121-64 CT), au-delà de quoi un accord collectif majoré est requis.</p>
 *
 * <p><b>Pays</b> : FRANCE uniquement — le forfait jours art. L. 3121-58+ CT
 * avec ses garanties post-Cass. 29/06/2011 est spécifiquement français.
 * Pas d'équivalent direct côté BE.</p>
 */
public final class ForfaitJoursValiditeCalculator {

    /** Verdict de validité de la convention de forfait jours. */
    public enum ValiditeForfait {
        VALIDE,
        PARTIELLEMENT_NULLE,
        NULLE
    }

    /** Code structuré d'un facteur d'invalidité détecté. */
    public enum CodeFacteur {
        DT50_ACCORD_COLLECTIF,
        DT50_SUIVI_CHARGE,
        DT50_ENTRETIEN_ANNUEL,
        DT50_SUIVI_JOURS,
        DT50_CATEGORIE_AUTONOME,
        DT50_NB_JOURS
    }

    /** Facteur d'invalidité détecté — exposé dans la réponse API. */
    public record FacteurInvalidite(
            CodeFacteur code,
            String libelle,
            String fondement,
            int poids,
            String explication
    ) {}

    /** Résultat calculé par {@link #compute}. */
    public record Result(
            ValiditeForfait validiteForfait,
            int scoreValidite,
            List<FacteurInvalidite> facteursInvalidite,
            Double rappelHsEstimeEuros,
            int prescriptionRappelAns,
            List<String> basesJuridiques,
            List<String> messages,
            String country
    ) {}

    /** Seuil légal par défaut du forfait jours (L. 3121-64 CT). */
    public static final int SEUIL_JOURS_LEGAL = 218;

    /** Seuil maximal absolu accord majoré (L. 3121-64 CT — accord supérieur). */
    public static final int SEUIL_JOURS_MAX_ACCORD_MAJORE = 235;

    /** Prescription du rappel de salaire pour heures supplémentaires (L. 3245-1 CT). */
    public static final int PRESCRIPTION_RAPPEL_ANS = 3;

    private ForfaitJoursValiditeCalculator() {}

    /**
     * Point d'entrée principal.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace
     * @return résultat calculé
     * @throws IllegalArgumentException si {@code country != "FRANCE"} ou si
     *         des entrées numériques sont invalides
     */
    public static Result compute(ForfaitJoursValiditeInput input, String country) {
        if (!"FRANCE".equalsIgnoreCase(country)) {
            throw new IllegalArgumentException(
                    "Outil réservé au droit du travail FRANCE — pays détecté : " + country);
        }
        if (input == null) {
            throw new IllegalArgumentException("Données d'entrée manquantes");
        }
        if (input.ancienneteMois() < 0) {
            throw new IllegalArgumentException("L'ancienneté ne peut pas être négative");
        }
        if (input.salaireMensuelBrutEuros() < 0) {
            throw new IllegalArgumentException("Le salaire mensuel ne peut pas être négatif");
        }
        if (input.nbJoursForfait() < 0) {
            throw new IllegalArgumentException("Le nombre de jours du forfait ne peut pas être négatif");
        }
        if (input.hsEstimeesParSemaine() < 0) {
            throw new IllegalArgumentException("Les HS estimées par semaine ne peuvent pas être négatives");
        }
        if (input.nbSemainesParAn() < 0) {
            throw new IllegalArgumentException("Le nombre de semaines/an ne peut pas être négatif");
        }

        List<FacteurInvalidite> facteurs = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        // ── 1. Accord collectif autorisant le forfait (L. 3121-63 CT) ────────
        boolean accordExiste = Boolean.TRUE.equals(input.accordCollectifExiste());
        if (!accordExiste) {
            facteurs.add(new FacteurInvalidite(
                    CodeFacteur.DT50_ACCORD_COLLECTIF,
                    "Absence d'accord collectif autorisant le forfait jours",
                    "L. 3121-63 CT — recours au forfait jours subordonné à un accord d'entreprise ou de branche",
                    50,
                    "Aucun accord collectif d'entreprise ou de branche autorisant le recours " +
                    "au forfait jours n'a été identifié. Le forfait est nul par défaut (L. 3121-63 CT)."));
            messages.add("ALERTE : aucun accord collectif autorisant le forfait jours — nullité automatique.");
            bases.add("L. 3121-63 CT — accord collectif préalable obligatoire");
        }

        // ── 2. Garantie effective du suivi de la charge (Cass. soc. 29/06/2011) ──
        boolean accordGarantit = Boolean.TRUE.equals(input.accordGarantitSuiviCharge());
        if (accordExiste && !accordGarantit) {
            facteurs.add(new FacteurInvalidite(
                    CodeFacteur.DT50_SUIVI_CHARGE,
                    "Accord collectif sans garantie effective du suivi de la charge",
                    "Cass. soc. 29/06/2011 n°09-71.107 — accord Syntec ; jurisprudence constante",
                    40,
                    "L'accord collectif n'institue pas un mécanisme effectif d'alerte, " +
                    "de suivi régulier de la charge de travail et de protection de la santé du salarié " +
                    "(exigence post-Cass. soc. 29/06/2011 reprise par la jurisprudence constante)."));
            messages.add("L'accord collectif n'offre pas de garantie effective de suivi de la charge — nullité partielle.");
            bases.add("Cass. soc. 29/06/2011 n°09-71.107 — exigence de garantie effective");
        }

        // ── 3. Entretien annuel charge de travail (L. 3121-65 CT) ────────────
        boolean entretienOk = Boolean.TRUE.equals(input.entretienAnnuelRealise());
        if (!entretienOk) {
            facteurs.add(new FacteurInvalidite(
                    CodeFacteur.DT50_ENTRETIEN_ANNUEL,
                    "Entretien annuel sur la charge de travail non réalisé",
                    "L. 3121-65 CT — entretien annuel obligatoire sur la charge de travail",
                    35,
                    "L'employeur n'a pas organisé l'entretien annuel individuel formalisé portant sur la charge " +
                    "de travail, l'organisation du travail, l'articulation vie pro/vie perso et la rémunération " +
                    "du salarié au forfait jours. L'absence de cet entretien fragilise la validité du forfait."));
            bases.add("L. 3121-65 CT — entretien annuel charge de travail");
        }

        // ── 4. Document de contrôle mensuel des jours (L. 3121-66 CT) ────────
        boolean controleOk = Boolean.TRUE.equals(input.documentControleMensuelExiste());
        if (!controleOk) {
            facteurs.add(new FacteurInvalidite(
                    CodeFacteur.DT50_SUIVI_JOURS,
                    "Document de contrôle mensuel des jours travaillés absent",
                    "L. 3121-66 CT — document de contrôle mensuel obligatoire",
                    25,
                    "L'employeur n'a pas mis en place le document de contrôle (relevé déclaratif " +
                    "ou logiciel) recensant mensuellement les jours travaillés et les jours de repos. " +
                    "Ce manquement fragilise la preuve du décompte du forfait."));
            bases.add("L. 3121-66 CT — document de contrôle mensuel");
        }

        // ── 5. Catégorie cadre autonome (L. 3121-58 CT) ──────────────────────
        boolean categorieAutonome = Boolean.TRUE.equals(input.categorieAutonomeConfirmee());
        if (!categorieAutonome) {
            facteurs.add(new FacteurInvalidite(
                    CodeFacteur.DT50_CATEGORIE_AUTONOME,
                    "Salarié hors catégorie cadre autonome / ETAM maîtrisant son temps",
                    "L. 3121-58 CT — cadre autonome ou salarié ayant la maîtrise de son temps",
                    30,
                    "Le salarié ne remplit pas les conditions de catégorie cadre autonome " +
                    "(autonomie réelle dans l'organisation du temps de travail) ni de salarié " +
                    "non-cadre ayant la maîtrise de son temps requises pour un forfait jours."));
            bases.add("L. 3121-58 CT — catégorie de salarié éligible");
        }

        // ── 6. Plafond du nombre de jours (L. 3121-64 CT) ────────────────────
        if (input.nbJoursForfait() > SEUIL_JOURS_LEGAL) {
            int poids;
            String exp;
            if (input.nbJoursForfait() > SEUIL_JOURS_MAX_ACCORD_MAJORE) {
                poids = 30;
                exp = "Le nombre de jours dépasse le plafond maximal de " + SEUIL_JOURS_MAX_ACCORD_MAJORE
                        + " jours admis même en cas d'accord collectif majoré.";
            } else {
                poids = 15;
                exp = "Le nombre de jours dépasse le seuil légal de " + SEUIL_JOURS_LEGAL
                        + " jours et requiert un accord collectif spécifique autorisant la majoration "
                        + "jusqu'à " + SEUIL_JOURS_MAX_ACCORD_MAJORE + " jours maximum (L. 3121-64 CT).";
            }
            facteurs.add(new FacteurInvalidite(
                    CodeFacteur.DT50_NB_JOURS,
                    "Nombre de jours du forfait > " + SEUIL_JOURS_LEGAL + " (plafond légal)",
                    "L. 3121-64 CT — plafond légal 218 jours / accord majoré max 235",
                    poids,
                    exp));
            bases.add("L. 3121-64 CT — plafond du nombre de jours du forfait");
        }

        // ── 7. Verdict ───────────────────────────────────────────────────────
        ValiditeForfait validite;
        if (!accordExiste) {
            // Pas d'accord = nullité automatique du forfait.
            validite = ValiditeForfait.NULLE;
        } else if (!entretienOk && !controleOk && !categorieAutonome) {
            // Carences multiples sur les garanties matérielles = nullité.
            validite = ValiditeForfait.NULLE;
        } else if (!accordGarantit || !entretienOk || !controleOk
                || !categorieAutonome
                || input.nbJoursForfait() > SEUIL_JOURS_MAX_ACCORD_MAJORE) {
            validite = ValiditeForfait.PARTIELLEMENT_NULLE;
        } else if (input.nbJoursForfait() > SEUIL_JOURS_LEGAL) {
            validite = ValiditeForfait.PARTIELLEMENT_NULLE;
        } else {
            validite = ValiditeForfait.VALIDE;
        }

        // Score : 100 si valide, 0 si nul. Décroissance via poids des facteurs.
        int score = 100;
        for (FacteurInvalidite f : facteurs) {
            score -= f.poids();
        }
        score = Math.max(0, Math.min(100, score));

        // Messages génériques selon verdict
        switch (validite) {
            case VALIDE -> {
                messages.add("Le forfait jours est valide au regard des 5 critères analysés.");
                bases.add(0, "L. 3121-58 à L. 3121-66 CT — régime du forfait jours");
            }
            case PARTIELLEMENT_NULLE -> {
                messages.add("Le forfait jours est partiellement nul : un ou plusieurs critères de validité " +
                        "ne sont pas remplis. Le salarié peut prétendre à un rappel d'heures supplémentaires " +
                        "sur la période de prescription (3 ans, L. 3245-1 CT).");
                bases.add(0, "L. 3121-58 à L. 3121-66 CT — régime du forfait jours");
            }
            case NULLE -> {
                messages.add("Le forfait jours est nul : le salarié relève du régime de droit commun de la " +
                        "durée du travail. Il peut prétendre à un rappel d'heures supplémentaires sur la " +
                        "période de prescription (3 ans, L. 3245-1 CT).");
                bases.add(0, "L. 3121-58 à L. 3121-66 CT — régime du forfait jours");
            }
        }

        // ── 8. Calcul du rappel HS si forfait nul ou partiellement nul ───────
        Double rappelHs = null;
        if (validite != ValiditeForfait.VALIDE) {
            rappelHs = calculerRappelHs(
                    input.salaireMensuelBrutEuros(),
                    input.hsEstimeesParSemaine(),
                    input.nbSemainesParAn());
            messages.add("Rappel HS estimé sur 3 ans : "
                    + java.text.NumberFormat.getNumberInstance(java.util.Locale.FRANCE)
                            .format(Math.round(rappelHs))
                    + " € (10h/sem × " + input.nbSemainesParAn() + " sem × 3 ans, majorations 25 %/50 %).");
            bases.add("L. 3245-1 CT — prescription 3 ans rappel salaire");
            bases.add("L. 3121-28 / L. 3121-36 CT — majorations heures supplémentaires");
        }

        return new Result(
                validite,
                score,
                List.copyOf(facteurs),
                rappelHs,
                PRESCRIPTION_RAPPEL_ANS,
                List.copyOf(bases),
                List.copyOf(messages),
                "FRANCE"
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Calcule le rappel d'heures supplémentaires estimé sur 3 ans.
     *
     * <p>Hypothèse de calcul (modulable par l'avocat via les inputs) :
     * <code>hsEstimeesParSemaine × nbSemainesParAn × 3 ans</code>.
     * Les 8 premières heures (au-delà de 35h) sont majorées à 25 %,
     * les suivantes à 50 % (L. 3121-28 / L. 3121-36 CT).</p>
     *
     * <p>Taux horaire = salaireMensuelBrutEuros / 151,67 h
     * (durée légale mensuelle 35h × 52/12).</p>
     *
     * @param salaireMensuelBrutEuros salaire de référence
     * @param hsParSemaine heures supplémentaires estimées par semaine
     * @param nbSemainesParAn nombre de semaines travaillées dans l'année
     * @return montant estimé arrondi au centime
     */
    private static double calculerRappelHs(double salaireMensuelBrutEuros,
                                            int hsParSemaine,
                                            int nbSemainesParAn) {
        if (salaireMensuelBrutEuros <= 0 || hsParSemaine <= 0 || nbSemainesParAn <= 0) {
            return 0.0;
        }
        double tauxHoraire = salaireMensuelBrutEuros / 151.67;
        // Décompose les HS par semaine en deux blocs : 25 % (≤ 8 HS) puis 50 %
        int hsA25 = Math.min(hsParSemaine, 8);
        int hsA50 = Math.max(0, hsParSemaine - 8);

        double rappelAnnuel = (
                (hsA25 * tauxHoraire * 1.25)
              + (hsA50 * tauxHoraire * 1.50)
        ) * nbSemainesParAn;

        double rappel3Ans = rappelAnnuel * PRESCRIPTION_RAPPEL_ANS;
        return Math.round(rappel3Ans * 100.0) / 100.0;
    }
}
