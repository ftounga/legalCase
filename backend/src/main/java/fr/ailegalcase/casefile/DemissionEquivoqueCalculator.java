package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-212-21 : moteur d'analyse de la validité d'une démission au regard de la
 * jurisprudence exigeant une volonté claire et non équivoque
 * (F-DT-41-demission-validite-equivoque, FRANCE — L. 1237-1 CT — démission
 * régulière du CDI par le salarié ; Cass. soc. 09/05/2007 — la démission ne
 * peut résulter que d'une volonté claire et non équivoque ; Cass. soc.
 * constante — requalification possible en prise d'acte de la rupture aux
 * torts de l'employeur ou en licenciement sans cause réelle et sérieuse).
 *
 * <p>Verdict 3 niveaux :
 * <ul>
 *   <li>{@code VOLONTE_CLAIRE} — démission régulière, écrit formel, hors
 *       contexte particulier — la volonté du salarié paraît libre et
 *       éclairée. Pas d'élément d'équivocité significatif.</li>
 *   <li>{@code DEMISSION_EQUIVOQUE} — éléments d'équivocité caractérisés
 *       (altercation, pression, état émotionnel perturbé, manquements
 *       employeur contemporains) — la démission peut être requalifiée par
 *       le CPH en prise d'acte de la rupture ou en licenciement sans cause
 *       réelle et sérieuse.</li>
 *   <li>{@code RETRACTATION_POSSIBLE} — le salarié a déjà rétracté sa
 *       démission rapidement OU le délai pour rétracter est court et la
 *       volonté n'apparaît pas claire — la démission peut être contestée
 *       sur ce seul motif.</li>
 * </ul>
 *
 * <p><b>Pays</b> : FRANCE uniquement — l'exigence de volonté claire et non
 * équivoque relève de la jurisprudence française de la chambre sociale ; côté
 * BE, régime distinct fondé sur l'art. 32 et 37 de la loi du 03/07/1978
 * relative aux contrats de travail (non couvert ici).
 */
public final class DemissionEquivoqueCalculator {

    /** Verdict d'analyse de la démission — 3 niveaux. */
    public enum AnalyseDemission {
        VOLONTE_CLAIRE,
        DEMISSION_EQUIVOQUE,
        RETRACTATION_POSSIBLE
    }

    /** Code structuré d'un facteur d'équivocité (F-IA-03). */
    public enum CodeCritere {
        DT41_CONTEXTE_ALTERCATION,
        DT41_PRESSION,
        DT41_RETRACTATION,
        DT41_MANQUEMENTS_EMPLOYEUR
    }

    /** Facteur d'équivocité exposé dans la réponse API. */
    public record FacteurEquivocite(
            CodeCritere code,
            String libelle,
            String fondement,
            int poids,
            String explication
    ) {}

    /** Résultat calculé par {@link #compute}. */
    public record Result(
            AnalyseDemission analyseDemission,
            int scoreEquivocite,
            List<FacteurEquivocite> facteursEquivocite,
            boolean requalificationPossible,
            List<String> basesJuridiques,
            List<String> messages,
            String country
    ) {}

    /** Seuil au-delà duquel le score caractérise une démission équivoque. */
    private static final int SEUIL_EQUIVOCITE = 50;

    /**
     * Délai (en jours) au-delà duquel une rétractation perd son caractère
     * « rapide » et signal d'équivocité initiale. Jurisprudence non chiffrée
     * mais usage prétorien : quelques jours à quelques semaines.
     */
    private static final int SEUIL_RETRACTATION_RAPIDE_JOURS = 7;

    private DemissionEquivoqueCalculator() {}

    /**
     * Point d'entrée principal.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace
     * @return résultat calculé
     * @throws IllegalArgumentException si {@code country != "FRANCE"} ou si
     *         {@code input} est null, ou si des valeurs sont aberrantes.
     */
    public static Result compute(DemissionEquivoqueInput input, String country) {
        if (!"FRANCE".equalsIgnoreCase(country)) {
            throw new IllegalArgumentException(
                    "Outil réservé au droit du travail FRANCE — pays détecté : " + country);
        }
        if (input == null) {
            throw new IllegalArgumentException("Données d'entrée manquantes");
        }
        if (input.modeExpressionDemission() == null) {
            throw new IllegalArgumentException("Mode d'expression de la démission requis");
        }
        if (input.delaiRetractationJours() != null && input.delaiRetractationJours() < 0) {
            throw new IllegalArgumentException("Le délai de rétractation ne peut pas être négatif");
        }

        List<FacteurEquivocite> facteurs = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        int score = 0;

        var mode = input.modeExpressionDemission();
        boolean altercation = input.contexteAltercation();
        boolean pression = input.pressionOuMenace();
        boolean retractation = input.retractationDansDelai();
        Integer delaiRetractationJours = input.delaiRetractationJours();
        boolean manquements = input.manquementsEmployeurContemporains();
        boolean etatPerturbe = input.etatEmotionnelPerturbe();

        // ── 1. DT41_CONTEXTE_ALTERCATION — circonstances de la démission ────
        if (altercation) {
            int poids = 25;
            String explication =
                    "La démission a été donnée dans un contexte d'altercation, dispute ou "
                            + "réunion conflictuelle immédiatement antérieure ou concomitante. "
                            + "Cass. soc. constante — une démission donnée « à chaud » dans un "
                            + "contexte tendu n'exprime pas nécessairement une volonté libre et "
                            + "éclairée.";
            // Le mode impulsif (SMS, EMAIL, ORAL) majore l'effet de l'altercation.
            if (mode == DemissionEquivoqueInput.ModeExpressionDemission.SMS
                    || mode == DemissionEquivoqueInput.ModeExpressionDemission.EMAIL
                    || mode == DemissionEquivoqueInput.ModeExpressionDemission.ORAL) {
                poids += 10;
                explication += " Le mode d'expression « " + mode.name() + " » renforce ce caractère "
                        + "impulsif et la présomption d'équivocité.";
            }
            facteurs.add(new FacteurEquivocite(
                    CodeCritere.DT41_CONTEXTE_ALTERCATION,
                    "Démission donnée dans un contexte d'altercation",
                    "Cass. soc. 09/05/2007 — volonté claire et non équivoque exigée ; jurisprudence constante sur la démission « à chaud »",
                    poids,
                    explication));
            bases.add("Cass. soc. 09/05/2007 — exigence d'une volonté claire et non équivoque");
            score += poids;
        } else {
            facteurs.add(new FacteurEquivocite(
                    CodeCritere.DT41_CONTEXTE_ALTERCATION,
                    "Pas de contexte d'altercation documenté",
                    "Cass. soc. 09/05/2007 — facteur de validité de la démission",
                    0,
                    "Aucun élément documenté d'altercation immédiate avant la démission."));
        }

        // ── 2. DT41_PRESSION — pression ou menace ───────────────────────────
        if (pression) {
            int poids = 35;
            facteurs.add(new FacteurEquivocite(
                    CodeCritere.DT41_PRESSION,
                    "Démission donnée sous pression ou menace",
                    "Cass. soc. constante — la démission donnée sous contrainte n'est pas valable et peut être annulée ou requalifiée en licenciement sans cause réelle et sérieuse",
                    poids,
                    "La démission a été donnée sous pression, menace ou contrainte de l'employeur "
                            + "(menace de licenciement disciplinaire, mise en demeure de démissionner, "
                            + "harcèlement actif). La validité même de la démission est gravement "
                            + "remise en cause."));
            bases.add("Cass. soc. constante — nullité de la démission donnée sous contrainte");
            score += poids;
        } else {
            facteurs.add(new FacteurEquivocite(
                    CodeCritere.DT41_PRESSION,
                    "Pas de pression ou menace documentée",
                    "Cass. soc. constante — absence de contrainte = facteur de validité",
                    0,
                    "Aucun élément documenté de pression, menace ou contrainte sur le salarié."));
        }

        // ── 3. DT41_RETRACTATION — rétractation de la démission ─────────────
        if (retractation) {
            int poids;
            String explication;
            if (delaiRetractationJours != null && delaiRetractationJours <= SEUIL_RETRACTATION_RAPIDE_JOURS) {
                poids = 30;
                explication = "Le salarié a rétracté sa démission dans un délai très court ("
                        + delaiRetractationJours + " jour(s)) — élément factuel fort indiquant "
                        + "que la volonté initiale n'était pas claire et non équivoque.";
            } else if (delaiRetractationJours != null) {
                poids = 15;
                explication = "Le salarié a rétracté sa démission après " + delaiRetractationJours
                        + " jour(s) — élément à analyser : un délai long affaiblit l'argument "
                        + "d'équivocité immédiate mais reste recevable si la rétractation est "
                        + "circonstanciée.";
            } else {
                poids = 20;
                explication = "Le salarié a rétracté sa démission — élément factuel d'équivocité "
                        + "à étayer par le délai précis et les circonstances de la rétractation.";
            }
            facteurs.add(new FacteurEquivocite(
                    CodeCritere.DT41_RETRACTATION,
                    "Rétractation de la démission documentée",
                    "Cass. soc. constante — la rétractation rapide d'une démission est un indice fort d'équivocité",
                    poids,
                    explication));
            bases.add("Cass. soc. constante — rétractation = indice d'équivocité");
            score += poids;
        } else {
            facteurs.add(new FacteurEquivocite(
                    CodeCritere.DT41_RETRACTATION,
                    "Pas de rétractation de la démission",
                    "Cass. soc. constante — absence de rétractation = facteur de validité",
                    0,
                    "Le salarié n'a pas rétracté sa démission, ce qui tend à confirmer le caractère "
                            + "réfléchi de sa volonté."));
        }

        // ── 4. DT41_MANQUEMENTS_EMPLOYEUR — manquements contemporains ──────
        if (manquements) {
            int poids = 25;
            facteurs.add(new FacteurEquivocite(
                    CodeCritere.DT41_MANQUEMENTS_EMPLOYEUR,
                    "Manquements employeur contemporains de la démission",
                    "Cass. soc. constante — la démission concomitante à des manquements graves de l'employeur (impayés, harcèlement, modification unilatérale) ouvre la voie à une requalification en prise d'acte de la rupture aux torts de l'employeur",
                    poids,
                    "Des manquements graves de l'employeur (impayés de salaire, défaut de paiement "
                            + "d'heures supplémentaires, harcèlement, modification unilatérale du "
                            + "contrat) sont contemporains de la démission. Ces griefs révèlent que "
                            + "la démission peut être requalifiée en prise d'acte de la rupture aux "
                            + "torts de l'employeur, produisant les effets d'un licenciement sans "
                            + "cause réelle et sérieuse."));
            bases.add("Cass. soc. constante — requalification de la démission en prise d'acte aux torts de l'employeur");
            score += poids;
        } else {
            facteurs.add(new FacteurEquivocite(
                    CodeCritere.DT41_MANQUEMENTS_EMPLOYEUR,
                    "Pas de manquement employeur contemporain",
                    "Cass. soc. constante — absence de griefs = facteur de validité",
                    0,
                    "Aucun manquement employeur contemporain n'est documenté."));
        }

        // ── 5. Facteur additionnel — état émotionnel perturbé ──────────────
        if (etatPerturbe) {
            score += 15;
            messages.add("Le salarié était dans un état émotionnel perturbé (burn-out, deuil, "
                    + "syndrome dépressif) au moment de la démission — un certificat médical "
                    + "postérieur attestant d'un trouble psychique contemporain est un élément "
                    + "fort d'équivocité (Cass. soc. constante).");
            bases.add("Cass. soc. constante — état émotionnel perturbé contemporain = équivocité");
        }

        // ── 6. Verdict global ───────────────────────────────────────────────
        AnalyseDemission verdict;
        boolean requalificationPossible;

        boolean retractationRapide = retractation
                && (delaiRetractationJours == null
                || delaiRetractationJours <= SEUIL_RETRACTATION_RAPIDE_JOURS);

        if (retractationRapide && !pression && score < SEUIL_EQUIVOCITE) {
            // Cas particulier : rétractation rapide isolée sans pression — la
            // démission peut être contestée sur ce seul motif (volonté non
            // confirmée). L'avocat peut tenter la voie de l'invalidation pour
            // défaut de volonté claire et non équivoque.
            verdict = AnalyseDemission.RETRACTATION_POSSIBLE;
            requalificationPossible = true;
            messages.add("Rétractation rapide documentée — la volonté claire et non équivoque "
                    + "exigée par Cass. soc. 09/05/2007 n'est pas confirmée. Une demande "
                    + "d'invalidation de la démission peut être engagée sur ce seul motif.");
        } else if (score >= SEUIL_EQUIVOCITE) {
            verdict = AnalyseDemission.DEMISSION_EQUIVOQUE;
            requalificationPossible = true;
            messages.add("Démission équivoque — les éléments accumulés (contexte, pression, "
                    + "rétractation, manquements employeur) caractérisent ensemble l'absence "
                    + "de volonté claire et non équivoque exigée par Cass. soc. 09/05/2007. "
                    + "Le CPH peut requalifier la démission en prise d'acte de la rupture aux "
                    + "torts de l'employeur (si manquements) ou en licenciement sans cause "
                    + "réelle et sérieuse.");
        } else {
            verdict = AnalyseDemission.VOLONTE_CLAIRE;
            requalificationPossible = false;
            messages.add("Volonté claire et non équivoque — les éléments disponibles ne "
                    + "permettent pas en l'état de remettre en cause la validité de la "
                    + "démission au sens de Cass. soc. 09/05/2007.");
        }

        // ── 7. Bases juridiques génériques ─────────────────────────────────
        bases.add(0, "L. 1237-1 CT — démission régulière du CDI par le salarié");
        bases.add("Jurisprudence constante — la démission doit résulter d'une volonté claire et non équivoque");

        // Borner le score à [0, 100]
        score = Math.max(0, Math.min(100, score));

        return new Result(
                verdict,
                score,
                List.copyOf(facteurs),
                requalificationPossible,
                List.copyOf(bases),
                List.copyOf(messages),
                "FRANCE"
        );
    }
}
