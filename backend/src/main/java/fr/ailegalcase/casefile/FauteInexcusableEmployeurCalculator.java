package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-212-09 : moteur d'analyse de la reconnaissance de la faute inexcusable
 * de l'employeur (FRANCE) — art. L. 452-1 à L. 452-5 CSS ; Cass. ass. plén.
 * 24/06/2005 ; art. L. 4121-1 CT.
 *
 * <p>Vérifie les conditions de la faute inexcusable
 * (Cass. ass. plén. 24/06/2005) :
 * <ul>
 *   <li>L'employeur avait ou aurait dû avoir <b>conscience du danger</b>
 *       auquel était exposé le salarié.</li>
 *   <li>L'employeur n'a <b>pas pris les mesures nécessaires</b> pour l'en
 *       préserver (obligation de sécurité L. 4121-1 CT).</li>
 * </ul>
 *
 * <p>Outil distinct de {@code AtMpCalculator} (F-DT-33) : ici on évalue
 * la reconnaissance judiciaire de la faute inexcusable et la majoration
 * de la rente / réparation des préjudices personnels indemnisables
 * (Cass. ass. plén. 24/06/2005 ; CC décision 2010-8 QPC).</p>
 *
 * <p><b>Invariant procédural</b> : action devant le <b>pôle social du TJ</b>
 * (non le CPH) — la réponse contient toujours
 * {@link Result#alerteProcedurePolesSocial()} renseignée
 * (jamais omise, jamais conditionnelle).</p>
 *
 * <p><b>Pays</b> : FRANCE uniquement — la faute inexcusable telle que
 * définie par la chambre sociale et l'assemblée plénière est française.
 * Pas d'équivalent direct en BE (régimes faute grave / intentionnelle
 * distincts).</p>
 */
public final class FauteInexcusableEmployeurCalculator {

    /** Verdict d'évaluation de la faute inexcusable. */
    public enum EvaluationFauteInexcusable {
        FAUTE_INEXCUSABLE_PROBABLE,
        FAUTE_INEXCUSABLE_POSSIBLE,
        FAUTE_INEXCUSABLE_PEU_PROBABLE
    }

    /** Code structuré d'un facteur de faute inexcusable. */
    public enum CodeFacteur {
        DT91_CONSCIENCE_DANGER,
        DT91_SIGNALEMENT_PRIOR,
        DT91_MESURES_PREVENTION,
        DT91_DUER,
        DT91_FORMATION_SECURITE
    }

    /** Facteur de faute inexcusable détecté — exposé dans la réponse API. */
    public record FacteurFauteInexcusable(
            CodeFacteur code,
            String libelle,
            String fondement,
            int poids,
            String explication
    ) {}

    /** Phrase invariante de l'alerte procédure pôle social (toujours présente). */
    public static final String ALERTE_PROCEDURE_POLES_SOCIAL =
            "Action devant le pôle social du TJ — non devant le CPH "
                    + "(art. L. 142-1 CSS, L. 142-2 CSS — contentieux de la sécurité sociale, "
                    + "compétence exclusive du pôle social du tribunal judiciaire).";

    /** Résultat calculé par {@link #compute}. */
    public record Result(
            EvaluationFauteInexcusable evaluationFauteInexcusable,
            int scoreFauteInexcusable,
            List<FacteurFauteInexcusable> facteursFauteInexcusable,
            Double majorationRenteEstimeeEuros,
            String alerteProcedurePolesSocial,
            List<String> basesJuridiques,
            List<String> messages,
            String country
    ) {}

    private FauteInexcusableEmployeurCalculator() {}

    /**
     * Point d'entrée principal.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace
     * @return résultat calculé
     * @throws IllegalArgumentException si {@code country != "FRANCE"} ou si
     *         {@code input} est null, ou si des valeurs numériques sont
     *         négatives ou aberrantes.
     */
    public static Result compute(FauteInexcusableEmployeurInput input, String country) {
        if (!"FRANCE".equalsIgnoreCase(country)) {
            throw new IllegalArgumentException(
                    "Outil réservé au droit du travail FRANCE — pays détecté : " + country);
        }
        if (input == null) {
            throw new IllegalArgumentException("Données d'entrée manquantes");
        }
        if (input.tauxIpp() < 0 || input.tauxIpp() > 100) {
            throw new IllegalArgumentException(
                    "Le taux d'IPP doit être compris entre 0 et 100 — valeur reçue : "
                            + input.tauxIpp());
        }
        if (input.salaireMensuelBrutEuros() < 0) {
            throw new IllegalArgumentException(
                    "Le salaire mensuel brut ne peut pas être négatif");
        }
        if (input.renteMensuelleEuros() != null && input.renteMensuelleEuros() < 0) {
            throw new IllegalArgumentException(
                    "La rente mensuelle ne peut pas être négative");
        }

        List<FacteurFauteInexcusable> facteurs = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        boolean conscience = Boolean.TRUE.equals(input.conscienceDangerEmployeurEtablie());
        boolean signalement = Boolean.TRUE.equals(input.signalementDangerPrior());
        boolean mesuresPrises = Boolean.TRUE.equals(input.mesuresPreventionPrises());
        boolean duer = Boolean.TRUE.equals(input.documentUniqueEvalue());
        boolean formation = Boolean.TRUE.equals(input.formationSecuriteProdiguee());

        // Score cumulatif — incrément si facteur penche pour la faute inexcusable.
        int score = 0;

        // ── 1. Conscience du danger ──────────────────────────────────────────
        if (conscience) {
            facteurs.add(new FacteurFauteInexcusable(
                    CodeFacteur.DT91_CONSCIENCE_DANGER,
                    "Conscience du danger établie chez l'employeur",
                    "Cass. ass. plén. 24/06/2005 ; art. L. 4121-1 CT",
                    35,
                    "L'employeur avait ou aurait dû avoir conscience du danger auquel "
                            + "était exposé le salarié — première condition de la faute "
                            + "inexcusable au sens de Cass. ass. plén. 24/06/2005. La conscience "
                            + "s'apprécie objectivement au regard de la connaissance qu'aurait dû "
                            + "avoir un employeur normalement diligent."));
            score += 35;
            bases.add("Cass. ass. plén. 24/06/2005 — conscience du danger");
            bases.add("L. 4121-1 CT — obligation de sécurité de l'employeur");
        } else {
            facteurs.add(new FacteurFauteInexcusable(
                    CodeFacteur.DT91_CONSCIENCE_DANGER,
                    "Conscience du danger NON établie",
                    "Cass. ass. plén. 24/06/2005",
                    0,
                    "Aucune conscience du danger documentée. La première condition de la "
                            + "faute inexcusable n'est pas réunie en l'état des pièces produites."));
        }

        // ── 2. Signalement antérieur du danger ───────────────────────────────
        if (signalement) {
            facteurs.add(new FacteurFauteInexcusable(
                    CodeFacteur.DT91_SIGNALEMENT_PRIOR,
                    "Signalement antérieur du danger par le salarié ou un tiers",
                    "Cass. soc. — signalement = présomption de conscience du danger",
                    25,
                    "ALERTE : un signalement du danger (par le salarié, un collègue, le CSE, "
                            + "le médecin du travail ou l'inspection du travail) est antérieur à "
                            + "l'AT/MP. La jurisprudence considère ce signalement comme un indice "
                            + "fort de la conscience du danger par l'employeur — facteur déterminant."));
            score += 25;
            messages.add("ALERTE : signalement antérieur du danger — indice fort de conscience.");
            bases.add("Cass. soc. — valeur probante du signalement préalable");
        }

        // ── 3. Mesures de prévention prises ──────────────────────────────────
        if (!mesuresPrises) {
            facteurs.add(new FacteurFauteInexcusable(
                    CodeFacteur.DT91_MESURES_PREVENTION,
                    "Absence de mesures de prévention nécessaires",
                    "L. 4121-1 CT ; Cass. ass. plén. 24/06/2005",
                    30,
                    "L'employeur n'a pas pris les mesures nécessaires pour préserver le "
                            + "salarié du danger — seconde condition cumulative de la faute "
                            + "inexcusable. L'obligation de sécurité de l'employeur est une "
                            + "obligation de moyens renforcée (Cass. soc. 22/06/2017)."));
            score += 30;
            messages.add("Manquement à l'obligation de sécurité (L. 4121-1 CT) — facteur "
                    + "déterminant de la faute inexcusable.");
            bases.add("Cass. soc. 22/06/2017 n°16-15.507 — obligation de moyens renforcée");
        } else {
            facteurs.add(new FacteurFauteInexcusable(
                    CodeFacteur.DT91_MESURES_PREVENTION,
                    "Mesures de prévention documentées",
                    "L. 4121-1 CT",
                    0,
                    "Des mesures de prévention sont documentées — l'employeur peut "
                            + "démontrer qu'il a respecté son obligation de sécurité."));
        }

        // ── 4. Document unique d'évaluation des risques (DUERP) ──────────────
        if (!duer) {
            facteurs.add(new FacteurFauteInexcusable(
                    CodeFacteur.DT91_DUER,
                    "Document unique d'évaluation des risques absent ou non mis à jour",
                    "R. 4121-1 CT ; art. L. 4121-3 CT",
                    15,
                    "L'absence ou la non-mise à jour du DUERP est un manquement supplémentaire "
                            + "à l'obligation d'évaluation des risques (R. 4121-1 CT), pris en "
                            + "compte par la jurisprudence comme facteur aggravant de la faute "
                            + "inexcusable."));
            score += 15;
            messages.add("DUERP non évalué/à jour — facteur aggravant.");
            bases.add("R. 4121-1 CT ; L. 4121-3 CT — évaluation des risques (DUERP)");
        }

        // ── 5. Formation à la sécurité ───────────────────────────────────────
        if (!formation) {
            facteurs.add(new FacteurFauteInexcusable(
                    CodeFacteur.DT91_FORMATION_SECURITE,
                    "Absence de formation à la sécurité",
                    "L. 4141-2 CT",
                    10,
                    "L'employeur a l'obligation d'organiser une formation pratique et "
                            + "appropriée en matière de sécurité (L. 4141-2 CT). L'absence "
                            + "de formation est un manquement contribuant à la qualification "
                            + "de faute inexcusable."));
            score += 10;
            messages.add("Absence de formation sécurité (L. 4141-2 CT).");
            bases.add("L. 4141-2 CT — formation à la sécurité");
        }

        // ── 6. Verdict ──────────────────────────────────────────────────────
        EvaluationFauteInexcusable verdict;
        // Critère central Cass. ass. plén. 24/06/2005 : conscience + absence
        // de mesures. Quand les deux sont réunis, la faute inexcusable est
        // probable, quel que soit le score additionnel.
        if (conscience && !mesuresPrises) {
            verdict = EvaluationFauteInexcusable.FAUTE_INEXCUSABLE_PROBABLE;
            messages.add(0, "Faute inexcusable PROBABLE — conscience du danger établie + "
                    + "absence de mesures de prévention. Les deux conditions cumulatives "
                    + "de Cass. ass. plén. 24/06/2005 sont réunies.");
        } else if (score >= 40) {
            verdict = EvaluationFauteInexcusable.FAUTE_INEXCUSABLE_POSSIBLE;
            messages.add(0, "Faute inexcusable POSSIBLE — des indices convergents "
                    + "(signalement, manquements documentés) la rendent crédible mais l'une "
                    + "des deux conditions cumulatives Cass. ass. plén. 24/06/2005 n'est pas "
                    + "pleinement démontrée.");
        } else {
            verdict = EvaluationFauteInexcusable.FAUTE_INEXCUSABLE_PEU_PROBABLE;
            messages.add(0, "Faute inexcusable PEU PROBABLE — les conditions cumulatives "
                    + "Cass. ass. plén. 24/06/2005 (conscience du danger + absence de mesures "
                    + "de prévention) ne sont pas réunies en l'état des pièces.");
        }

        // ── 7. Majoration de rente — L. 452-2 CSS ────────────────────────────
        // La majoration de la rente est portée au maximum légal si la faute
        // inexcusable est reconnue. Le maximum correspond au taux d'IPP.
        Double majoration = null;
        if (verdict == EvaluationFauteInexcusable.FAUTE_INEXCUSABLE_PROBABLE
                && input.tauxIpp() > 0
                && input.renteMensuelleEuros() != null
                && input.renteMensuelleEuros() > 0) {
            // Majoration maximale L. 452-2 — la rente passe au maximum admis,
            // proportionnel au taux d'IPP. Modèle simplifié pour estimation
            // indicative : majoration = rente actuelle × (tauxIPP / 100).
            // L'avocat valide la formule au cas d'espèce avec la CPAM.
            majoration = input.renteMensuelleEuros() * (input.tauxIpp() / 100.0);
            // Arrondi 2 décimales.
            majoration = Math.round(majoration * 100.0) / 100.0;
            messages.add("Majoration de rente estimée (L. 452-2 CSS) : "
                    + majoration + " €/mois — valeur indicative à valider avec la CPAM.");
            bases.add("L. 452-2 CSS — majoration de la rente AT/MP en cas de faute inexcusable");
        }

        // Préjudices personnels indemnisables (toujours rappelés en cas de
        // faute inexcusable PROBABLE / POSSIBLE).
        if (verdict != EvaluationFauteInexcusable.FAUTE_INEXCUSABLE_PEU_PROBABLE) {
            messages.add("Préjudices personnels indemnisables (Cass. ass. plén. 24/06/2005 ; "
                    + "CC décision 2010-8 QPC) : souffrances physiques et morales, préjudice "
                    + "esthétique, agrément, sexuel, établissement.");
            messages.add("Recours subrogation de la CPAM contre l'employeur — L. 452-3-1 CSS.");
            bases.add("Cass. ass. plén. 24/06/2005 — préjudices personnels indemnisables");
            bases.add("CC décision 2010-8 QPC — réparation intégrale du préjudice");
            bases.add("L. 452-3-1 CSS — recours subrogation CPAM");
        }

        // Borner le score à [0, 100]
        score = Math.max(0, Math.min(100, score));

        return new Result(
                verdict,
                score,
                List.copyOf(facteurs),
                majoration,
                ALERTE_PROCEDURE_POLES_SOCIAL,
                List.copyOf(bases),
                List.copyOf(messages),
                "FRANCE"
        );
    }
}
