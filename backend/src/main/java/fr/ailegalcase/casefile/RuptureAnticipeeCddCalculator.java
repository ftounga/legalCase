package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-212-17 : moteur d'analyse de la rupture anticipée d'un CDD
 * (F-DT-43-rupture-anticipee-cdd, FRANCE — L. 1243-1 à L. 1243-4 CT ;
 * L. 1243-8 CT — indemnité de précarité ; L. 1226-4-2 CT — inaptitude
 * applicable au CDD ; Cass. soc. constante — sanctions rupture irrégulière
 * par le salarié, dommages-intérêts au préjudice réel).
 *
 * <p>L'outil distingue trois verdicts :
 * <ul>
 *   <li>{@code LEGITIME} — le motif invoqué appartient à la liste des
 *       cas autorisés par L. 1243-1 à L. 1243-3 CT (accord, faute grave
 *       par employeur, force majeure, inaptitude par employeur, embauche
 *       CDI par salarié). Aucune indemnité spécifique à la rupture
 *       anticipée n'est due — l'indemnité de précarité L. 1243-8 reste
 *       en revanche due si CDD non requalifié en CDI.</li>
 *   <li>{@code ILLEGITIME_EMPLOYEUR} — rupture par l'employeur sans motif
 *       légal au sens de L. 1243-1 CT. Sanction L. 1243-4 CT :
 *       indemnisation au moins égale aux salaires que le salarié aurait
 *       perçus jusqu'au terme normal du CDD, à laquelle s'ajoute
 *       l'indemnité de précarité L. 1243-8.</li>
 *   <li>{@code ILLEGITIME_SALARIE} — rupture par le salarié sans motif
 *       légal (ni accord, ni force majeure, ni embauche CDI). Sanction
 *       L. 1243-4 al. 3 CT : dommages-intérêts à l'employeur égaux au
 *       préjudice subi (montant non chiffré par l'outil — appréciation
 *       judiciaire). Aucun versement au salarié.</li>
 * </ul>
 *
 * <p><b>Pays</b> : FRANCE uniquement — la rupture anticipée du CDD telle que
 * codifiée aux art. L. 1243-1 et s. CT n'a pas d'équivalent direct côté BE
 * (le CDD belge relève de la Loi du 03/07/1978 — régime distinct, rupture
 * anticipée moyennant indemnité forfaitaire).
 */
public final class RuptureAnticipeeCddCalculator {

    /** Verdict d'analyse de la rupture anticipée du CDD — 3 niveaux. */
    public enum AnalyseRuptureAnticipeeCdd {
        LEGITIME,
        ILLEGITIME_EMPLOYEUR,
        ILLEGITIME_SALARIE
    }

    /** Code structuré d'un point d'analyse (F-IA-03). */
    public enum CodeCritere {
        DT43_MOTIF_RUPTURE,
        DT43_AUTEUR_RUPTURE,
        DT43_DATE_TERME
    }

    /** Point d'analyse exposé dans la réponse API. */
    public record PointAnalyse(
            CodeCritere code,
            String libelle,
            String fondement,
            String conclusion
    ) {}

    /** Résultat calculé par {@link #compute}. */
    public record Result(
            AnalyseRuptureAnticipeeCdd analyseRuptureAnticipee,
            int moisRestantsContrat,
            List<PointAnalyse> pointsAnalyse,
            double indemnitesRuptureAnticipeeEuros,
            double indemnitePrecariteEuros,
            double totalDuSalarieEuros,
            List<String> basesJuridiques,
            List<String> messages,
            String country
    ) {}

    /** Taux légal de l'indemnité de précarité — L. 1243-8 CT. */
    private static final double TAUX_INDEMNITE_PRECARITE = 0.10;

    private RuptureAnticipeeCddCalculator() {}

    /**
     * Point d'entrée principal.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace
     * @return résultat calculé
     * @throws IllegalArgumentException si {@code country != "FRANCE"}, si
     *         {@code input} est null, si l'auteur ou le motif manquent,
     *         si une date manque, si {@code dateRupture > dateTermeCdd},
     *         ou si une valeur monétaire est négative.
     */
    public static Result compute(RuptureAnticipeeCddInput input, String country) {
        if (!"FRANCE".equalsIgnoreCase(country)) {
            throw new IllegalArgumentException(
                    "Outil réservé au droit du travail FRANCE — pays détecté : " + country);
        }
        if (input == null) {
            throw new IllegalArgumentException("Données d'entrée manquantes");
        }
        if (input.auteurRupture() == null) {
            throw new IllegalArgumentException("Auteur de la rupture requis");
        }
        if (input.motifRupture() == null) {
            throw new IllegalArgumentException("Motif de la rupture requis");
        }
        if (input.dateTermeCdd() == null) {
            throw new IllegalArgumentException("Date de terme du CDD requise");
        }
        if (input.dateRupture() == null) {
            throw new IllegalArgumentException("Date de rupture requise");
        }
        if (input.dateRupture().isAfter(input.dateTermeCdd())) {
            throw new IllegalArgumentException(
                    "La date de rupture ne peut pas être postérieure au terme du CDD");
        }
        if (input.salaireMensuelBrutEuros() < 0) {
            throw new IllegalArgumentException(
                    "Le salaire mensuel brut ne peut pas être négatif");
        }
        if (input.remunerationBruteTotaleContratEuros() < 0) {
            throw new IllegalArgumentException(
                    "La rémunération brute totale ne peut pas être négative");
        }

        RuptureAnticipeeCddInput.AuteurRupture auteur = input.auteurRupture();
        RuptureAnticipeeCddInput.MotifRupture motif = input.motifRupture();
        LocalDate termeCdd = input.dateTermeCdd();
        LocalDate dateRupture = input.dateRupture();
        double salaireMensuel = input.salaireMensuelBrutEuros();
        double remunTotale = input.remunerationBruteTotaleContratEuros();

        List<PointAnalyse> points = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        // ── 1. DT43_AUTEUR_RUPTURE — partie ayant rompu ─────────────────────
        if (auteur == RuptureAnticipeeCddInput.AuteurRupture.EMPLOYEUR) {
            points.add(new PointAnalyse(
                    CodeCritere.DT43_AUTEUR_RUPTURE,
                    "Rupture à l'initiative de l'EMPLOYEUR",
                    "L. 1243-1 CT — la rupture anticipée du CDD par l'employeur n'est licite qu'en cas de faute grave du salarié, de force majeure, ou d'inaptitude médicale constatée",
                    "Le motif invoqué par l'employeur est appréhendé au regard des cas autorisés L. 1243-1 CT."));
        } else {
            points.add(new PointAnalyse(
                    CodeCritere.DT43_AUTEUR_RUPTURE,
                    "Rupture à l'initiative du SALARIE",
                    "L. 1243-2 CT — le salarié ne peut rompre anticipativement le CDD qu'en justifiant d'une embauche en CDI, sous réserve d'un préavis ; ou par accord, force majeure, faute grave employeur",
                    "Le motif invoqué par le salarié est appréhendé au regard des cas autorisés L. 1243-1 et L. 1243-2 CT."));
        }

        // ── 2. DT43_MOTIF_RUPTURE — qualification du motif ──────────────────
        switch (motif) {
            case ACCORD_PARTIES -> {
                points.add(new PointAnalyse(
                        CodeCritere.DT43_MOTIF_RUPTURE,
                        "Rupture par accord des parties",
                        "L. 1243-1 CT, L. 1243-3 CT — la rupture anticipée par accord est licite des deux côtés ; aucune indemnité spécifique de rupture anticipée n'est due (sauf clause de l'accord)",
                        "LEGITIME — aucune indemnité de rupture anticipée due (sauf accord)."));
                bases.add("L. 1243-1 CT — motifs autorisés de rupture anticipée");
                bases.add("L. 1243-3 CT — rupture par accord des parties");
            }
            case FAUTE_GRAVE -> {
                if (auteur == RuptureAnticipeeCddInput.AuteurRupture.EMPLOYEUR) {
                    points.add(new PointAnalyse(
                            CodeCritere.DT43_MOTIF_RUPTURE,
                            "Rupture pour faute grave du salarié",
                            "L. 1243-1 CT — la faute grave du salarié autorise l'employeur à rompre anticipativement le CDD sans indemnité de rupture",
                            "LEGITIME — aucune indemnité de rupture anticipée due au salarié."));
                    bases.add("L. 1243-1 CT — faute grave du salarié");
                } else {
                    points.add(new PointAnalyse(
                            CodeCritere.DT43_MOTIF_RUPTURE,
                            "Rupture invoquée par le salarié pour faute grave de l'employeur",
                            "L. 1243-1 CT — le salarié ne peut pas rompre anticipativement le CDD pour faute grave employeur sans qualifier autrement (force majeure, prise d'acte) ; la voie ordinaire est l'action en résiliation judiciaire",
                            "ILLEGITIME — la faute grave employeur n'est pas un motif autorisé de rupture anticipée à l'initiative du salarié."));
                    bases.add("L. 1243-1 CT — motifs limitatifs côté salarié");
                }
            }
            case FORCE_MAJEURE -> {
                points.add(new PointAnalyse(
                        CodeCritere.DT43_MOTIF_RUPTURE,
                        "Rupture pour force majeure",
                        "L. 1243-1 CT — la force majeure (événement imprévisible, irrésistible et extérieur rendant impossible l'exécution) autorise la rupture anticipée du CDD",
                        "LEGITIME — aucune indemnité de rupture anticipée due (sous réserve maintien salaire chômage technique L. 5122-1 CT le cas échéant)."));
                bases.add("L. 1243-1 CT — force majeure");
            }
            case INAPTITUDE -> {
                if (auteur == RuptureAnticipeeCddInput.AuteurRupture.EMPLOYEUR) {
                    points.add(new PointAnalyse(
                            CodeCritere.DT43_MOTIF_RUPTURE,
                            "Rupture pour inaptitude médicale du salarié",
                            "L. 1226-4-2 CT applicable au CDD via L. 1243-1 CT — l'inaptitude médicale constatée par le médecin du travail autorise la rupture anticipée sous réserve de la procédure de reclassement",
                            "LEGITIME — aucune indemnité de rupture anticipée due (l'indemnité de licenciement spécifique inaptitude pourrait être due, hors périmètre de cet outil)."));
                    bases.add("L. 1226-4-2 CT — inaptitude médicale et CDD");
                } else {
                    points.add(new PointAnalyse(
                            CodeCritere.DT43_MOTIF_RUPTURE,
                            "Inaptitude médicale invoquée par le salarié comme motif de rupture",
                            "L. 1243-1 CT — l'inaptitude médicale est un motif de rupture à l'initiative de l'employeur après constat du médecin du travail, non du salarié",
                            "ILLEGITIME — l'inaptitude n'est pas un motif autorisé de rupture anticipée à l'initiative du salarié."));
                    bases.add("L. 1243-1 CT — motifs limitatifs côté salarié");
                }
            }
            case CDI_EMBAUCHE -> {
                if (auteur == RuptureAnticipeeCddInput.AuteurRupture.SALARIE) {
                    points.add(new PointAnalyse(
                            CodeCritere.DT43_MOTIF_RUPTURE,
                            "Rupture par le salarié pour embauche en CDI ailleurs",
                            "L. 1243-2 CT — le salarié peut rompre anticipativement le CDD en justifiant d'une embauche en CDI, sous réserve d'un préavis (calculé selon ancienneté, plafonné à 2 semaines)",
                            "LEGITIME — aucune indemnité due ; vérifier le respect du préavis L. 1243-2 al. 2 CT."));
                    bases.add("L. 1243-2 CT — rupture anticipée pour embauche en CDI");
                } else {
                    points.add(new PointAnalyse(
                            CodeCritere.DT43_MOTIF_RUPTURE,
                            "Embauche en CDI invoquée par l'employeur comme motif de rupture",
                            "L. 1243-2 CT — l'embauche en CDI est un motif réservé au salarié, non à l'employeur",
                            "ILLEGITIME — l'embauche en CDI ailleurs n'est pas un motif autorisé de rupture par l'employeur."));
                    bases.add("L. 1243-2 CT — motif réservé au salarié");
                }
            }
            case AUTRE -> {
                points.add(new PointAnalyse(
                        CodeCritere.DT43_MOTIF_RUPTURE,
                        "Aucun motif légal invoqué",
                        "L. 1243-1 CT — la rupture anticipée du CDD hors des cas légalement autorisés (accord, faute grave, force majeure, inaptitude, embauche CDI) est irrégulière et engage la responsabilité de l'auteur",
                        "ILLEGITIME — déclenche les sanctions L. 1243-4 CT (indemnité au salarié si employeur ; dommages-intérêts à l'employeur si salarié)."));
                bases.add("L. 1243-1 CT — caractère limitatif des motifs");
                bases.add("L. 1243-4 CT — sanctions de la rupture irrégulière");
            }
        }

        // ── 3. DT43_DATE_TERME — mois restants jusqu'au terme ───────────────
        int moisRestants = computeMoisRestants(dateRupture, termeCdd);
        points.add(new PointAnalyse(
                CodeCritere.DT43_DATE_TERME,
                "Mois restants jusqu'au terme du CDD : " + moisRestants,
                "L. 1243-4 CT — l'indemnité due au salarié en cas de rupture irrégulière par l'employeur est au moins égale aux rémunérations qu'il aurait perçues jusqu'au terme normal du contrat",
                "Base de calcul de l'indemnité L. 1243-4 si rupture irrégulière par l'employeur."));

        // ── 4. Verdict global ───────────────────────────────────────────────
        AnalyseRuptureAnticipeeCdd verdict = computeVerdict(auteur, motif);

        // ── 5. Calcul des indemnités ────────────────────────────────────────
        double indemniteRuptureAnticipee = 0.0;
        double indemnitePrecarite = 0.0;
        double totalDu = 0.0;

        if (verdict == AnalyseRuptureAnticipeeCdd.ILLEGITIME_EMPLOYEUR) {
            // L. 1243-4 al. 1 CT — indemnité au moins égale aux salaires
            // restants jusqu'au terme.
            indemniteRuptureAnticipee = Math.round(salaireMensuel * moisRestants * 100.0) / 100.0;
            // L. 1243-8 CT — indemnité de précarité 10 % rémunération totale.
            indemnitePrecarite = Math.round(remunTotale * TAUX_INDEMNITE_PRECARITE * 100.0) / 100.0;
            totalDu = Math.round((indemniteRuptureAnticipee + indemnitePrecarite) * 100.0) / 100.0;
            bases.add("L. 1243-4 CT — indemnité rupture anticipée employeur");
            bases.add("L. 1243-8 CT — indemnité de précarité 10 %");
            messages.add("Rupture irrégulière par l'employeur — le salarié peut prétendre à "
                    + "une indemnité au moins égale aux salaires qu'il aurait perçus jusqu'au "
                    + "terme normal du CDD (L. 1243-4 CT), augmentée de l'indemnité de "
                    + "précarité 10 % (L. 1243-8 CT). Les indemnités effectivement perçues "
                    + "au titre d'un nouveau contrat pendant la période restante peuvent être "
                    + "déduites par le juge.");
        } else if (verdict == AnalyseRuptureAnticipeeCdd.ILLEGITIME_SALARIE) {
            // L. 1243-4 al. 3 CT — dommages-intérêts à l'employeur, préjudice réel.
            // Non chiffré par l'outil — décision judiciaire.
            bases.add("L. 1243-4 al. 3 CT — dommages-intérêts dus à l'employeur");
            messages.add("Rupture irrégulière par le salarié — l'employeur peut demander des "
                    + "dommages-intérêts correspondant au préjudice réellement subi "
                    + "(L. 1243-4 al. 3 CT). Le montant n'est pas chiffré par l'outil : "
                    + "il dépend de l'appréciation judiciaire (coût de remplacement, perte "
                    + "de production, etc.). Aucun versement n'est dû au salarié.");
        } else {
            // LEGITIME — pas d'indemnité de rupture anticipée. L'indemnité de
            // précarité reste due en fin de CDD (L. 1243-8 CT) sauf cas
            // d'exclusion (faute grave salarié, CDI embauche, refus CDI).
            messages.add("Rupture anticipée légitime au regard de L. 1243-1 à L. 1243-3 CT. "
                    + "Aucune indemnité spécifique de rupture anticipée n'est due. "
                    + "L'indemnité de précarité (L. 1243-8 CT) reste due en fin de contrat "
                    + "sauf cas d'exclusion (faute grave salarié, CDI proposé refusé, etc.).");
        }

        // ── 6. Bases juridiques génériques ──────────────────────────────────
        bases.add(0, "Cass. soc. constante — sanctions de la rupture irrégulière du CDD");

        return new Result(
                verdict,
                moisRestants,
                List.copyOf(points),
                indemniteRuptureAnticipee,
                indemnitePrecarite,
                totalDu,
                List.copyOf(bases),
                List.copyOf(messages),
                "FRANCE"
        );
    }

    /**
     * Calcule le nombre de mois entiers restants entre la date de rupture
     * et le terme prévu du CDD. Tronqué à 0 si la rupture est postérieure au
     * terme (cas déjà rejeté en amont, sécurité défensive).
     */
    private static int computeMoisRestants(LocalDate dateRupture, LocalDate termeCdd) {
        if (dateRupture == null || termeCdd == null) return 0;
        if (!dateRupture.isBefore(termeCdd)) return 0;
        long days = ChronoUnit.DAYS.between(dateRupture, termeCdd);
        // Approximation 30 jours par mois — cohérente avec usage CT.
        return (int) Math.max(0, Math.round(days / 30.0));
    }

    /**
     * Détermine le verdict global selon le couple (auteur, motif).
     *
     * <ul>
     *   <li>ACCORD_PARTIES / FORCE_MAJEURE : LEGITIME des deux côtés.</li>
     *   <li>FAUTE_GRAVE / INAPTITUDE : LEGITIME côté employeur,
     *       ILLEGITIME_SALARIE côté salarié.</li>
     *   <li>CDI_EMBAUCHE : LEGITIME côté salarié, ILLEGITIME_EMPLOYEUR
     *       côté employeur.</li>
     *   <li>AUTRE : ILLEGITIME du côté qui a rompu.</li>
     * </ul>
     */
    private static AnalyseRuptureAnticipeeCdd computeVerdict(
            RuptureAnticipeeCddInput.AuteurRupture auteur,
            RuptureAnticipeeCddInput.MotifRupture motif) {
        return switch (motif) {
            case ACCORD_PARTIES, FORCE_MAJEURE -> AnalyseRuptureAnticipeeCdd.LEGITIME;
            case FAUTE_GRAVE, INAPTITUDE ->
                    auteur == RuptureAnticipeeCddInput.AuteurRupture.EMPLOYEUR
                            ? AnalyseRuptureAnticipeeCdd.LEGITIME
                            : AnalyseRuptureAnticipeeCdd.ILLEGITIME_SALARIE;
            case CDI_EMBAUCHE ->
                    auteur == RuptureAnticipeeCddInput.AuteurRupture.SALARIE
                            ? AnalyseRuptureAnticipeeCdd.LEGITIME
                            : AnalyseRuptureAnticipeeCdd.ILLEGITIME_EMPLOYEUR;
            case AUTRE ->
                    auteur == RuptureAnticipeeCddInput.AuteurRupture.EMPLOYEUR
                            ? AnalyseRuptureAnticipeeCdd.ILLEGITIME_EMPLOYEUR
                            : AnalyseRuptureAnticipeeCdd.ILLEGITIME_SALARIE;
        };
    }
}
