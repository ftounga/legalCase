package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-212-01 : moteur de qualification de la faute disciplinaire (faute simple /
 * grave / lourde) et de calcul de l'impact financier sur les indemnités de
 * rupture (FRANCE, L. 1234-1 CT ; L. 1234-5 CT ; L. 1234-9 CT ;
 * Cass. soc. 18/06/2013 pour les CP sur faute lourde).
 *
 * <p>L'outil analyse, du point de vue de l'avocat du salarié <b>ou</b> de
 * l'employeur, la qualification d'une faute disciplinaire et son impact
 * financier sur les indemnités de rupture (préavis, indemnité légale de
 * licenciement, congés payés).</p>
 *
 * <p><b>Règle Cass. soc. 18/06/2013 n°11-14.393</b> : la faute lourde ne
 * prive plus le salarié de son indemnité compensatrice de congés payés.
 * Faute grave ET faute lourde privent du préavis et de l'IL, mais le
 * droit aux CP acquis est maintenu.</p>
 *
 * <p><b>Pays</b> : FRANCE uniquement — la distinction faute grave / faute
 * lourde est spécifiquement française (L. 1234-1 s. CT).</p>
 */
public final class LicenciementFauteGraveLourdCalculator {

    /** Qualification retenue de la faute disciplinaire. */
    public enum QualificationFaute {
        FAUTE_SIMPLE,
        FAUTE_GRAVE,
        FAUTE_LOURDE
    }

    /** Code structuré d'un facteur de qualification détecté. */
    public enum CodeFacteur {
        DT36_QUALIFICATION_FAUTE,
        DT36_PRESCRIPTION_FAUTE,
        DT36_INTENTION_NUIRE,
        DT36_PROCEDURE_DISCIPLINAIRE
    }

    /** Facteur de qualification détecté — exposé dans la réponse API. */
    public record FacteurQualification(
            CodeFacteur code,
            String libelle,
            String fondement,
            int poids,
            String explication
    ) {}

    /** Résultat calculé par {@link #compute}. */
    public record Result(
            QualificationFaute qualificationRetenue,
            int scoreQualification,
            List<FacteurQualification> facteursQualification,
            double indemnitePreavisEuros,
            double indemniteLegaleEuros,
            double indemnitesCongesPayesEuros,
            double totalIndemnitesDuesEuros,
            boolean alertePrescriptionFaute,
            List<String> basesJuridiques,
            List<String> messages,
            String country
    ) {}

    private LicenciementFauteGraveLourdCalculator() {}

    /**
     * Point d'entrée principal.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace
     * @return résultat calculé
     * @throws IllegalArgumentException si {@code country != "FRANCE"} ou si
     *         ancienneté / salaire sont invalides
     */
    public static Result compute(LicenciementFauteGraveLourdInput input, String country) {
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

        List<FacteurQualification> facteurs = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        // ── 1. Détection de la prescription ──────────────────────────────────
        boolean alertePrescription = Boolean.TRUE.equals(input.prescriptionFauteVerifiee());
        if (alertePrescription) {
            facteurs.add(new FacteurQualification(
                    CodeFacteur.DT36_PRESCRIPTION_FAUTE,
                    "Prescription de la faute",
                    "L. 1332-4 CT — délai de 2 mois",
                    40,
                    "Les faits reprochés sont prescrits (> 2 mois avant engagement de la procédure). " +
                    "La sanction est irrégulière et susceptible d'annulation."));
            messages.add("ALERTE : les faits reprochés semblent prescrits (L. 1332-4 CT). " +
                    "Vérifier la date de connaissance des faits par l'employeur.");
            bases.add("L. 1332-4 CT — prescription de la faute disciplinaire (2 mois)");
        }

        // ── 2. Détection de l'intention de nuire ─────────────────────────────
        boolean intentionNuire = Boolean.TRUE.equals(input.intentionNuireAlleeguee())
                && input.preuveIntentionNuire() != null
                && !input.preuveIntentionNuire().isBlank();
        if (intentionNuire) {
            facteurs.add(new FacteurQualification(
                    CodeFacteur.DT36_INTENTION_NUIRE,
                    "Intention de nuire caractérisée",
                    "Cass. soc. — faute lourde : intention de nuire à l'employeur",
                    50,
                    "L'intention de nuire est alléguée et documentée : " + input.preuveIntentionNuire()));
            bases.add("Cass. soc. — faute lourde caractérisée par l'intention de nuire");
        }

        // ── 3. Vérification procédure disciplinaire ───────────────────────────
        boolean procedureReguliere = Boolean.TRUE.equals(input.attenteConvocation())
                && Boolean.TRUE.equals(input.motivationLettreAdequate());
        if (!Boolean.TRUE.equals(input.attenteConvocation())) {
            facteurs.add(new FacteurQualification(
                    CodeFacteur.DT36_PROCEDURE_DISCIPLINAIRE,
                    "Irrégularité de procédure — convocation",
                    "L. 1232-2 CT — convocation à entretien préalable",
                    30,
                    "L'entretien préalable n'a pas été tenu ou la convocation n'a pas respecté " +
                    "les formes légales (délai 5 jours ouvrables, mentions obligatoires)."));
            messages.add("Irrégularité procédurale : l'entretien préalable semble absent ou irrégulier.");
            bases.add("L. 1232-2 CT — convocation à entretien préalable");
        }
        if (!Boolean.TRUE.equals(input.motivationLettreAdequate())) {
            facteurs.add(new FacteurQualification(
                    CodeFacteur.DT36_PROCEDURE_DISCIPLINAIRE,
                    "Motivation de la lettre de licenciement insuffisante",
                    "L. 1232-6 CT — lettre de licenciement motivée",
                    20,
                    "La lettre de licenciement n'énonce pas des motifs précis et vérifiables."));
            bases.add("L. 1232-6 CT — lettre de licenciement motivée");
        }

        // ── 4. Qualification finale ───────────────────────────────────────────
        QualificationFaute qualificationRetenue;
        int score;

        // La qualification retenue est la plus haute entre celle alléguée par l'employeur
        // et celle imposée par les faits (intention de nuire → lourde ; pas d'intention → grave/simple)
        QualificationFaute qualificationEmployeur = input.qualificationEmployeur();

        if (intentionNuire || qualificationEmployeur == QualificationFaute.FAUTE_LOURDE) {
            qualificationRetenue = QualificationFaute.FAUTE_LOURDE;
            score = calcScore(facteurs, alertePrescription, 75);
        } else if (qualificationEmployeur == QualificationFaute.FAUTE_GRAVE) {
            qualificationRetenue = QualificationFaute.FAUTE_GRAVE;
            score = calcScore(facteurs, alertePrescription, 65);
        } else {
            qualificationRetenue = QualificationFaute.FAUTE_SIMPLE;
            score = calcScore(facteurs, alertePrescription, 30);
        }

        // Ajuster score si prescription détectée (fragilise la qualification)
        if (alertePrescription) {
            score = Math.max(0, score - 40);
        }

        // Facteur qualification principale
        facteurs.add(0, new FacteurQualification(
                CodeFacteur.DT36_QUALIFICATION_FAUTE,
                "Qualification de la faute retenue : " + qualificationRetenue.name(),
                fondementQualification(qualificationRetenue),
                weightQualification(qualificationRetenue),
                justificationQualification(qualificationRetenue, intentionNuire)));

        bases.add(0, fondementQualification(qualificationRetenue));

        // ── 5. Calcul financier ───────────────────────────────────────────────
        double salaire = input.salaireMensuelBrutEuros();
        int ancienneteMois = input.ancienneteMois();

        // Indemnité compensatrice de préavis
        double indemnitePreavis;
        if (qualificationRetenue == QualificationFaute.FAUTE_GRAVE
                || qualificationRetenue == QualificationFaute.FAUTE_LOURDE) {
            indemnitePreavis = 0.0;
        } else {
            indemnitePreavis = calculerPreavis(salaire, ancienneteMois);
        }

        // Indemnité légale de licenciement (L. 1234-9 CT — 1/4 de mois par année jusqu'à 10 ans,
        // 1/3 par année au-delà, applicables dès 8 mois d'ancienneté)
        double indemniteLegale;
        if (qualificationRetenue == QualificationFaute.FAUTE_GRAVE
                || qualificationRetenue == QualificationFaute.FAUTE_LOURDE) {
            indemniteLegale = 0.0;
        } else {
            indemniteLegale = calculerIndemniteLegale(salaire, ancienneteMois);
        }

        // CP acquis : maintenus quelle que soit la faute depuis Cass. soc. 18/06/2013
        double cp = salaire * 0.1; // approximation 1 mois * 10 % (valeur indicative)

        // Total
        double total = indemnitePreavis + indemniteLegale + cp;

        // Messages complémentaires
        if (qualificationRetenue == QualificationFaute.FAUTE_GRAVE) {
            messages.add("Faute grave : privation de l'indemnité compensatrice de préavis " +
                    "(L. 1234-5 CT) et de l'indemnité légale de licenciement (L. 1234-9 CT).");
            messages.add("Maintien du droit aux congés payés acquis (Cass. soc. 18/06/2013).");
            bases.add("L. 1234-5 CT — privation indemnité compensatrice de préavis");
            bases.add("L. 1234-9 CT — privation indemnité légale de licenciement");
            bases.add("Cass. soc. 18/06/2013 — maintien des congés payés acquis");
        } else if (qualificationRetenue == QualificationFaute.FAUTE_LOURDE) {
            messages.add("Faute lourde : privation de l'indemnité compensatrice de préavis " +
                    "et de l'indemnité légale de licenciement.");
            messages.add("Maintien du droit aux congés payés acquis (Cass. soc. 18/06/2013 n°11-14.393).");
            messages.add("Possibilité de dommages-intérêts à la charge du salarié si préjudice avéré.");
            bases.add("Cass. soc. 18/06/2013 n°11-14.393 — CP maintenus même en faute lourde");
        } else {
            messages.add("Faute simple : préavis, indemnité légale et congés payés maintenus.");
        }

        return new Result(
                qualificationRetenue,
                Math.min(100, Math.max(0, score)),
                List.copyOf(facteurs),
                Math.round(indemnitePreavis * 100.0) / 100.0,
                Math.round(indemniteLegale * 100.0) / 100.0,
                Math.round(cp * 100.0) / 100.0,
                Math.round(total * 100.0) / 100.0,
                alertePrescription,
                List.copyOf(bases),
                List.copyOf(messages),
                "FRANCE"
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static int calcScore(List<FacteurQualification> facteurs, boolean prescription, int base) {
        int s = base;
        for (FacteurQualification f : facteurs) {
            if (f.code() == CodeFacteur.DT36_INTENTION_NUIRE) s = Math.min(100, s + 15);
        }
        return s;
    }

    private static int weightQualification(QualificationFaute q) {
        return switch (q) {
            case FAUTE_LOURDE -> 60;
            case FAUTE_GRAVE -> 50;
            case FAUTE_SIMPLE -> 20;
        };
    }

    private static String fondementQualification(QualificationFaute q) {
        return switch (q) {
            case FAUTE_LOURDE -> "Cass. soc. — faute lourde : intention de nuire à l'employeur";
            case FAUTE_GRAVE -> "L. 1234-1 CT — faute grave : impossibilité de maintien pendant le préavis";
            case FAUTE_SIMPLE -> "L. 1232-1 CT — cause réelle et sérieuse sans gravité suffisante";
        };
    }

    private static String justificationQualification(QualificationFaute q, boolean intentionNuire) {
        return switch (q) {
            case FAUTE_LOURDE -> intentionNuire
                    ? "L'intention de nuire caractérisée à l'employeur justifie la qualification en faute lourde."
                    : "La qualification en faute lourde est alléguée par l'employeur sans preuve d'intention de nuire documentée.";
            case FAUTE_GRAVE -> "La faute rend impossible le maintien du salarié dans l'entreprise " +
                    "pendant la durée du préavis (L. 1234-1 CT).";
            case FAUTE_SIMPLE -> "La faute constitue une cause réelle et sérieuse sans atteindre " +
                    "le niveau de gravité d'une faute grave.";
        };
    }

    /**
     * Calcule l'indemnité compensatrice de préavis (faute simple uniquement).
     * Durée légale : 1 mois si ancienneté [6m-2ans[, 2 mois si ≥ 2 ans.
     */
    private static double calculerPreavis(double salaire, int ancienneteMois) {
        if (ancienneteMois < 6) return 0.0;
        if (ancienneteMois < 24) return salaire; // 1 mois
        return salaire * 2; // 2 mois (plafond légal sans convention collective)
    }

    /**
     * Calcule l'indemnité légale de licenciement (L. 1234-9 CT).
     * Applicable dès 8 mois d'ancienneté.
     * Taux : 1/4 de mois par année jusqu'à 10 ans + 1/3 de mois par année au-delà.
     */
    private static double calculerIndemniteLegale(double salaire, int ancienneteMois) {
        if (ancienneteMois < 8) return 0.0;
        double annees = ancienneteMois / 12.0;
        double il;
        if (annees <= 10.0) {
            il = salaire * (1.0 / 4.0) * annees;
        } else {
            il = salaire * (1.0 / 4.0) * 10.0
                    + salaire * (1.0 / 3.0) * (annees - 10.0);
        }
        return il;
    }
}
