package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-212-15 : moteur d'analyse de la conformité du dispositif de télétravail
 * et des litiges courants (FRANCE) — L. 1222-9 à L. 1222-11 CT ; ANI
 * télétravail du 26/11/2020.
 *
 * <p>Vérifie quatre axes principaux :
 * <ul>
 *   <li><b>Cadre juridique</b> (L. 1222-9) : accord collectif, charte
 *       unilatérale consultée CSE, ou accord individuel.</li>
 *   <li><b>Double volontariat</b> (L. 1222-9 al. 1) : employeur et salarié
 *       — sauf circonstances exceptionnelles L. 1222-11.</li>
 *   <li><b>Indemnité d'occupation</b> (ANI 2020 art. 6.2 ; tolérance URSSAF
 *       2,60 €/jour ou justificatifs).</li>
 *   <li><b>Litiges courants</b> : accident à domicile (présomption
 *       L. 1222-9 al. 4), retour au bureau imposé, refus de télétravail
 *       invoqué dans un licenciement (L. 1222-9 al. 6 — interdit).</li>
 * </ul>
 *
 * <p><b>Verdict 3 niveaux</b> :
 * <ul>
 *   <li>{@code CONFORME} — cadre juridique présent, double volontariat
 *       respecté, indemnité versée, aucun litige caractérisé.</li>
 *   <li>{@code NON_CONFORME} — absence de cadre juridique ou violation
 *       d'un élément structurel (double volontariat, indemnité due
 *       non versée).</li>
 *   <li>{@code LITIGE_IDENTIFIE} — cadre conforme mais un ou plusieurs
 *       litiges sont caractérisés (accident à domicile, retour bureau
 *       imposé, refus invoqué comme cause).</li>
 * </ul>
 *
 * <p><b>Pays</b> : FRANCE uniquement — l'ANI 2020 et l'article L. 1222-9 sont
 * des concepts du droit français du travail. En droit belge, le télétravail
 * relève de la CCT n°85 (télétravail structurel) et de la Loi du 05/03/2017
 * sur le télétravail occasionnel — régime distinct.</p>
 */
public final class TeletravailAccordCalculator {

    /** Verdict d'analyse de la conformité du dispositif de télétravail. */
    public enum AnalyseTeletravail {
        CONFORME,
        NON_CONFORME,
        LITIGE_IDENTIFIE
    }

    /** Code structuré d'un critère analysé (F-IA-03). */
    public enum CodeCritere {
        DT82_CADRE_JURIDIQUE,
        DT82_DOUBLE_VOLONTARIAT,
        DT82_INDEMNITE_OCCUPATION,
        DT82_ACCIDENT_DOMICILE
    }

    /** Point d'analyse exposé dans la réponse API. */
    public record PointTeletravail(
            CodeCritere code,
            String libelle,
            String fondement,
            String conclusion
    ) {}

    /** Résultat calculé par {@link #compute}. */
    public record Result(
            AnalyseTeletravail analyseTeletravail,
            int scoreTeletravail,
            List<PointTeletravail> pointsTeletravail,
            boolean alerteAccidentTravailDomicile,
            boolean alerteRefusTeletravailLicenciement,
            Double indemniteDueEstimeeEuros,
            List<String> basesJuridiques,
            List<String> messages,
            String country
    ) {}

    /** Tolérance URSSAF du remboursement de frais professionnels en télétravail (€/jour). */
    private static final double INDEMNITE_URSSAF_JOURNALIERE_EUROS = 2.60;

    /** Estimation forfaitaire mensuelle de jours télétravaillés pour l'arriéré (~20 jours / mois × 12). */
    private static final int JOURS_TELETRAVAIL_ANNUEL_ESTIMATIF = 200;

    private TeletravailAccordCalculator() {}

    /**
     * Point d'entrée principal.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace
     * @return résultat calculé
     * @throws IllegalArgumentException si {@code country != "FRANCE"} ou si
     *         {@code input} est null, ou si une valeur de montant est négative.
     */
    public static Result compute(TeletravailAccordInput input, String country) {
        if (!"FRANCE".equalsIgnoreCase(country)) {
            throw new IllegalArgumentException(
                    "Outil réservé au droit du travail FRANCE — pays détecté : " + country);
        }
        if (input == null) {
            throw new IllegalArgumentException("Données d'entrée manquantes");
        }
        if (input.cadreTeletravail() == null) {
            throw new IllegalArgumentException("Cadre juridique du télétravail requis");
        }
        if (input.montantIndemniteJournalierEuros() != null
                && input.montantIndemniteJournalierEuros() < 0) {
            throw new IllegalArgumentException(
                    "Le montant journalier d'indemnité ne peut pas être négatif");
        }

        List<PointTeletravail> points = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        int score = 0;

        TeletravailAccordInput.CadreTeletravail cadre = input.cadreTeletravail();
        boolean doubleVolontariat = input.doubleVolontariatRespectee();
        boolean indemniteVersee = input.indemniteOccupationVersee();
        Double montantJournalier = input.montantIndemniteJournalierEuros();
        boolean accidentDomicile = input.accidentDomicileDetecte();
        boolean retourImpose = input.retourBureauImposeUnilateralement();
        boolean refusIncrimine = input.refusTeletravailCauseIncrimination();

        // ── 1. DT82_CADRE_JURIDIQUE — existence d'un cadre juridique ─────────
        boolean cadrePresent = cadre != TeletravailAccordInput.CadreTeletravail.AUCUN;
        switch (cadre) {
            case ACCORD_COLLECTIF -> {
                points.add(new PointTeletravail(
                        CodeCritere.DT82_CADRE_JURIDIQUE,
                        "Accord collectif encadrant le télétravail (accord d'entreprise ou de branche)",
                        "L. 1222-9 al. 1 CT — le télétravail est mis en place dans le cadre d'un accord collectif",
                        "OK — cadre juridique le plus protecteur, conditions négociées avec les partenaires sociaux."));
                bases.add("L. 1222-9 al. 1 CT — accord collectif");
            }
            case CHARTE_UNILATERALE -> {
                points.add(new PointTeletravail(
                        CodeCritere.DT82_CADRE_JURIDIQUE,
                        "Charte unilatérale élaborée par l'employeur",
                        "L. 1222-9 al. 2 CT — à défaut d'accord collectif, charte élaborée après consultation du CSE",
                        "OK — cadre admis, vérifier la consultation effective du CSE et le contenu de la charte."));
                bases.add("L. 1222-9 al. 2 CT — charte unilatérale");
            }
            case ACCORD_INDIVIDUEL -> {
                points.add(new PointTeletravail(
                        CodeCritere.DT82_CADRE_JURIDIQUE,
                        "Accord individuel salarié/employeur (par tout moyen)",
                        "L. 1222-9 al. 3 CT — à défaut d'accord collectif ou de charte, accord formalisé par tout moyen",
                        "OK — cadre admis mais le plus fragile : à formaliser par écrit (avenant)."));
                bases.add("L. 1222-9 al. 3 CT — accord individuel");
            }
            case AUCUN -> points.add(new PointTeletravail(
                    CodeCritere.DT82_CADRE_JURIDIQUE,
                    "Aucun cadre juridique formalisé pour le télétravail",
                    "L. 1222-9 CT — le télétravail requiert un cadre (accord collectif, charte ou accord individuel)",
                    "ABSENCE — pratique informelle sans base juridique opposable."));
        }

        // ── 2. DT82_DOUBLE_VOLONTARIAT — volontariat employeur + salarié ─────
        if (doubleVolontariat) {
            points.add(new PointTeletravail(
                    CodeCritere.DT82_DOUBLE_VOLONTARIAT,
                    "Double volontariat employeur + salarié respecté",
                    "L. 1222-9 al. 1 CT — le télétravail repose sur le volontariat des deux parties",
                    "OK — accord mutuel documenté."));
        } else {
            points.add(new PointTeletravail(
                    CodeCritere.DT82_DOUBLE_VOLONTARIAT,
                    "Double volontariat non respecté (télétravail imposé unilatéralement)",
                    "L. 1222-9 al. 1 CT — hors circonstances exceptionnelles L. 1222-11 (pandémie, force majeure), le télétravail ne peut être imposé",
                    "VICE — sauf circonstances exceptionnelles, le télétravail imposé est illicite."));
            bases.add("L. 1222-9 al. 1 CT — double volontariat exigé");
            bases.add("L. 1222-11 CT — exception circonstances exceptionnelles");
        }

        // ── 3. DT82_INDEMNITE_OCCUPATION — remboursement frais télétravail ───
        Double indemniteDue = null;
        if (indemniteVersee) {
            points.add(new PointTeletravail(
                    CodeCritere.DT82_INDEMNITE_OCCUPATION,
                    "Indemnité d'occupation / remboursement de frais versés"
                            + (montantJournalier != null
                                ? " (" + montantJournalier + " €/jour)" : ""),
                    "ANI télétravail 26/11/2020 art. 6.2 — l'employeur prend en charge les frais professionnels liés au télétravail (tolérance URSSAF 2,60 €/jour ou justificatifs)",
                    "OK — obligation de remboursement respectée."));
            bases.add("ANI 26/11/2020 art. 6.2 — frais professionnels télétravail");
        } else {
            // Estimation indemnité due (forfait URSSAF × jours estimés / an).
            indemniteDue = INDEMNITE_URSSAF_JOURNALIERE_EUROS * JOURS_TELETRAVAIL_ANNUEL_ESTIMATIF;
            points.add(new PointTeletravail(
                    CodeCritere.DT82_INDEMNITE_OCCUPATION,
                    "Indemnité d'occupation / remboursement de frais non versée",
                    "ANI télétravail 26/11/2020 art. 6.2 — l'employeur doit prendre en charge les frais (tolérance URSSAF 2,60 €/jour)",
                    "VICE — l'absence de prise en charge des frais expose l'employeur à un rappel."));
            bases.add("ANI 26/11/2020 art. 6.2 — frais professionnels télétravail");
            bases.add("URSSAF — tolérance 2,60 €/jour télétravaillé");
        }

        // ── 4. DT82_ACCIDENT_DOMICILE — présomption AT à domicile ────────────
        if (accidentDomicile) {
            points.add(new PointTeletravail(
                    CodeCritere.DT82_ACCIDENT_DOMICILE,
                    "Accident survenu au domicile du salarié pendant la plage horaire de télétravail",
                    "L. 1222-9 al. 4 CT — présomption d'accident du travail pour l'accident survenu sur le lieu et pendant les horaires de télétravail",
                    "ALERTE — la qualification AT bénéficie d'une présomption ; l'employeur supporte la charge de la preuve contraire."));
            bases.add("L. 1222-9 al. 4 CT — présomption AT télétravail");
        } else {
            points.add(new PointTeletravail(
                    CodeCritere.DT82_ACCIDENT_DOMICILE,
                    "Aucun accident à domicile signalé",
                    "L. 1222-9 al. 4 CT — présomption AT non déclenchée",
                    "OK — pas de contentieux AT télétravail à ce stade."));
        }

        // ── 5. Verdict global ────────────────────────────────────────────────
        AnalyseTeletravail verdict;
        boolean litige = accidentDomicile || retourImpose || refusIncrimine;

        if (!cadrePresent) {
            verdict = AnalyseTeletravail.NON_CONFORME;
            score = 10;
            messages.add("Aucun cadre juridique de télétravail (L. 1222-9 CT) : "
                    + "régulariser par accord collectif, charte unilatérale (après "
                    + "consultation CSE) ou accord individuel écrit.");
        } else if (!doubleVolontariat && !litige) {
            verdict = AnalyseTeletravail.NON_CONFORME;
            score = 25;
            messages.add("Double volontariat non respecté (L. 1222-9 al. 1 CT) — "
                    + "sauf circonstances exceptionnelles L. 1222-11 (pandémie, "
                    + "force majeure), le télétravail ne peut être imposé.");
        } else if (!indemniteVersee && !litige) {
            verdict = AnalyseTeletravail.NON_CONFORME;
            score = 40;
            messages.add("Indemnité d'occupation non versée (ANI 26/11/2020 art. 6.2) "
                    + "— l'employeur s'expose à un rappel chiffré (~"
                    + INDEMNITE_URSSAF_JOURNALIERE_EUROS + " €/jour × jours télétravaillés).");
        } else if (litige) {
            verdict = AnalyseTeletravail.LITIGE_IDENTIFIE;
            score = 50;
            if (accidentDomicile) {
                messages.add("Accident à domicile détecté — présomption d'accident "
                        + "du travail (L. 1222-9 al. 4 CT). Reconstituer la plage "
                        + "horaire et le lieu de survenance.");
            }
            if (retourImpose) {
                messages.add("Retour au bureau imposé unilatéralement — vérifier le "
                        + "délai de prévenance prévu par l'accord ou la charte, ainsi "
                        + "que les circonstances justificatives (L. 1222-10 CT — "
                        + "principe de réversibilité encadrée).");
                bases.add("L. 1222-10 CT — réversibilité encadrée du télétravail");
            }
            if (refusIncrimine) {
                messages.add("Refus de télétravailler invoqué comme cause / motif "
                        + "aggravant — L. 1222-9 al. 6 CT INTERDIT cette qualification : "
                        + "le refus ne peut constituer en soi un motif de rupture.");
                bases.add("L. 1222-9 al. 6 CT — refus de télétravail ≠ motif de rupture");
            }
            // Ajout indemnité due si non versée même en cas de litige (cumul).
            if (!indemniteVersee) {
                messages.add("Indemnité d'occupation non versée — rappel chiffré "
                        + "indépendant des autres litiges (cumul possible).");
            } else {
                // Si versée et litige, pas d'indemnité due.
                indemniteDue = null;
            }
        } else {
            verdict = AnalyseTeletravail.CONFORME;
            score = 90;
            messages.add("Dispositif de télétravail conforme : cadre juridique "
                    + "présent, double volontariat respecté, indemnité versée, aucun "
                    + "litige caractérisé.");
        }

        // ── 6. Bases juridiques de référence ─────────────────────────────────
        bases.add(0, "L. 1222-9 à L. 1222-11 CT — régime du télétravail");
        bases.add("ANI télétravail 26/11/2020 — accord national interprofessionnel");

        // Borner le score à [0, 100]
        score = Math.max(0, Math.min(100, score));

        return new Result(
                verdict,
                score,
                List.copyOf(points),
                accidentDomicile,
                refusIncrimine,
                indemniteDue,
                List.copyOf(bases),
                List.copyOf(messages),
                "FRANCE"
        );
    }
}
