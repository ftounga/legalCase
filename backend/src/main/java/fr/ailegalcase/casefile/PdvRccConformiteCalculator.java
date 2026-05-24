package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-212-35 : moteur d'analyse de la conformité d'un plan de départs
 * volontaires (PDV) ou d'une rupture conventionnelle collective (RCC)
 * (F-DT-46-pdv-rcc-conformite, FRANCE — L. 1237-17 à L. 1237-19-14 CT — RCC
 * instituée par l'ord. n°2017-1387 du 22/09/2017 ; décret 2017-1718 du
 * 20/12/2017 ; circulaire DGT du 19/09/2018).
 *
 * <p>Verdict 3 niveaux :
 * <ul>
 *   <li>{@code CONFORME} — dispositif validé : accord majoritaire pour la
 *       RCC, validation DREETS obtenue dans le délai de 15 jours, indemnités
 *       ≥ légales, adhésion individuelle libre, information complète. La
 *       RCC conforme ouvre droit à l'ARE pour les salariés volontaires.</li>
 *   <li>{@code PARTIELLEMENT_CONFORME} — irrégularités mineures qui peuvent
 *       être régularisées (délai DREETS dépassé, information partielle).</li>
 *   <li>{@code NON_CONFORME} — vice fondamental : accord non majoritaire,
 *       absence de validation DREETS, indemnités inférieures au plancher
 *       légal, adhésion sous contrainte.</li>
 * </ul>
 *
 * <p><b>Pays</b> : FRANCE uniquement — le dispositif RCC est franco-français,
 * introduit par l'ord. du 22/09/2017 et codifié aux articles L. 1237-17 et s.
 * du Code du travail (pas d'équivalent direct en droit du travail belge).
 */
public final class PdvRccConformiteCalculator {

    /** Verdict d'analyse de la conformité — 3 niveaux. */
    public enum AnalysePdvRcc {
        CONFORME,
        PARTIELLEMENT_CONFORME,
        NON_CONFORME
    }

    /** Code structuré d'un point d'irrégularité (F-IA-03). */
    public enum CodeCritere {
        DT46_ACCORD_MAJORITAIRE,
        DT46_VALIDATION_DREETS,
        DT46_INDEMNITEES,
        DT46_ADHESION_VOLONTAIRE
    }

    /** Point d'irrégularité exposé dans la réponse API. */
    public record PointIrregularite(
            CodeCritere code,
            String libelle,
            String fondement
    ) {}

    /** Résultat calculé par {@link #compute}. */
    public record Result(
            AnalysePdvRcc analysePdvRcc,
            int scoreConformite,
            List<PointIrregularite> pointsIrregularite,
            boolean droitAreConfirme,
            List<String> basesJuridiques,
            List<String> messages,
            String country
    ) {}

    private PdvRccConformiteCalculator() {}

    /**
     * Point d'entrée principal.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace
     * @return résultat calculé
     * @throws IllegalArgumentException si {@code country != "FRANCE"} ou si
     *         {@code input} est null, ou si la combinaison est aberrante.
     */
    public static Result compute(PdvRccConformiteInput input, String country) {
        if (!"FRANCE".equalsIgnoreCase(country)) {
            throw new IllegalArgumentException(
                    "Outil réservé au droit du travail FRANCE — pays détecté : " + country);
        }
        if (input == null) {
            throw new IllegalArgumentException("Données d'entrée manquantes");
        }
        if (input.typeDispositif() == null) {
            throw new IllegalArgumentException("Type de dispositif requis (RCC ou PDV)");
        }

        List<PointIrregularite> points = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        int score = 100;

        boolean isRcc = input.typeDispositif() == PdvRccConformiteInput.TypeDispositif.RCC;

        // ── 1. DT46_ACCORD_MAJORITAIRE — condition RCC L. 1237-19-1 CT ──────
        if (isRcc && !input.accordCollectifMajoritaire()) {
            points.add(new PointIrregularite(
                    CodeCritere.DT46_ACCORD_MAJORITAIRE,
                    "Accord collectif non majoritaire pour une RCC",
                    "L. 1237-19-1 CT — la RCC suppose un accord collectif majoritaire signé par des organisations syndicales représentatives ayant recueilli ≥ 50 % des suffrages exprimés au 1er tour"));
            bases.add("L. 1237-19-1 CT — majorité renforcée requise pour la RCC");
            score -= 40;
        } else if (isRcc) {
            bases.add("L. 1237-19-1 CT — accord collectif majoritaire confirmé");
        }

        // ── 2. DT46_VALIDATION_DREETS — L. 1237-19-3 CT ────────────────────
        if (isRcc && !input.validationDREETSObtenue()) {
            points.add(new PointIrregularite(
                    CodeCritere.DT46_VALIDATION_DREETS,
                    "Validation DREETS non obtenue",
                    "L. 1237-19-3 CT — la RCC nécessite la validation de la DREETS, à défaut de quoi l'accord n'est pas opposable et n'ouvre pas droit aux allocations chômage"));
            bases.add("L. 1237-19-3 CT — validation DREETS impérative");
            score -= 35;
        } else if (isRcc) {
            bases.add("L. 1237-19-3 CT — validation DREETS obtenue");
            // Sous-critère délai 15 j — non bloquant mais signalé.
            if (input.delaiValidation15JRespectee() != null
                    && !input.delaiValidation15JRespectee()) {
                points.add(new PointIrregularite(
                        CodeCritere.DT46_VALIDATION_DREETS,
                        "Délai de 15 jours dépassé pour la décision DREETS",
                        "L. 1237-19-3 CT — la DREETS dispose d'un délai de 15 jours pour valider l'accord ; au-delà, validation tacite mais point procédural à signaler"));
                score -= 5;
            }
        }

        // ── 3. DT46_INDEMNITEES — plancher légal L. 1237-19-1 al. 5 CT ─────
        if (!input.indemnitesAuMoinsLegales()) {
            points.add(new PointIrregularite(
                    CodeCritere.DT46_INDEMNITEES,
                    "Indemnités prévues inférieures à l'indemnité légale de licenciement",
                    "L. 1237-19-1 al. 5 CT — les indemnités de rupture prévues par la RCC ou le PDV doivent être au moins égales à l'indemnité légale de licenciement (L. 1234-9 et R. 1234-1 et s. CT)"));
            bases.add("L. 1237-19-1 al. 5 CT — plancher indemnitaire");
            bases.add("L. 1234-9 CT — indemnité légale de licenciement");
            score -= 25;
        } else {
            bases.add("L. 1237-19-1 al. 5 CT — plancher indemnitaire respecté");
        }

        // ── 4. DT46_ADHESION_VOLONTAIRE — L. 1237-19-2 CT ──────────────────
        if (!input.adhesionVolontaireIndividuelle()) {
            points.add(new PointIrregularite(
                    CodeCritere.DT46_ADHESION_VOLONTAIRE,
                    "Adhésion individuelle non libre ou non écrite",
                    "L. 1237-19-2 CT — l'adhésion du salarié au dispositif doit être libre, écrite, et respecter un délai de réflexion ; tout vice du consentement entraîne la nullité"));
            bases.add("L. 1237-19-2 CT — libre adhésion individuelle");
            score -= 25;
        }
        if (!input.informationIndividuelleComplete()) {
            points.add(new PointIrregularite(
                    CodeCritere.DT46_ADHESION_VOLONTAIRE,
                    "Information individuelle incomplète",
                    "L. 1237-19-2 CT et circulaire DGT 19/09/2018 — le salarié doit être informé de ses droits (indemnités, accompagnement, droit ARE, faculté de rétractation)"));
            score -= 10;
        }

        // ── 5. Verdict global ────────────────────────────────────────────────
        AnalysePdvRcc verdict;
        boolean rccConditionsCoeur = isRcc
                && input.accordCollectifMajoritaire()
                && input.validationDREETSObtenue();
        boolean pdvConditionsCoeur = !isRcc
                && input.indemnitesAuMoinsLegales()
                && input.adhesionVolontaireIndividuelle();

        if (points.isEmpty()) {
            verdict = AnalysePdvRcc.CONFORME;
            messages.add(isRcc
                    ? "RCC conforme — accord collectif majoritaire validé par la DREETS, indemnités ≥ légales, adhésion individuelle libre. Le salarié adhérent ouvre droit à l'allocation d'aide au retour à l'emploi (ARE)."
                    : "PDV conforme — indemnités ≥ légales, adhésion individuelle libre et information complète.");
        } else if ((isRcc && !rccConditionsCoeur) || (!isRcc && !pdvConditionsCoeur)
                || !input.indemnitesAuMoinsLegales()
                || !input.adhesionVolontaireIndividuelle()) {
            verdict = AnalysePdvRcc.NON_CONFORME;
            messages.add(isRcc
                    ? "RCC non conforme — au moins une condition fondamentale (accord majoritaire, validation DREETS, indemnités, libre adhésion) fait défaut. L'accord n'est pas opposable et le bénéfice de l'ARE n'est pas garanti."
                    : "PDV non conforme — au moins une condition fondamentale (indemnités plancher, libre adhésion) fait défaut.");
        } else {
            verdict = AnalysePdvRcc.PARTIELLEMENT_CONFORME;
            messages.add("Dispositif partiellement conforme — quelques irrégularités identifiées qui peuvent être régularisées avant la mise en œuvre individuelle.");
        }

        // ── 6. Droit ARE confirmé : uniquement si RCC CONFORME ──────────────
        boolean droitAreConfirme = isRcc && verdict == AnalysePdvRcc.CONFORME;
        if (droitAreConfirme) {
            messages.add("Droit ARE confirmé — la RCC instituée par accord collectif majoritaire validé par la DREETS ouvre droit à l'allocation d'aide au retour à l'emploi (différence majeure avec un PDV hors PSE, qui ne l'ouvre pas systématiquement).");
        }

        // ── 7. Bases juridiques génériques ───────────────────────────────────
        bases.add(0, isRcc
                ? "L. 1237-19 CT — rupture conventionnelle collective (ord. 22/09/2017)"
                : "L. 1237-17 CT — plan de départs volontaires");
        bases.add("Décret n°2017-1718 du 20/12/2017 — modalités d'application de la RCC");
        bases.add("Circulaire DGT du 19/09/2018 — questions-réponses RCC");

        // Borner le score à [0, 100]
        score = Math.max(0, Math.min(100, score));

        return new Result(
                verdict,
                score,
                List.copyOf(points),
                droitAreConfirme,
                List.copyOf(bases),
                List.copyOf(messages),
                "FRANCE"
        );
    }
}
