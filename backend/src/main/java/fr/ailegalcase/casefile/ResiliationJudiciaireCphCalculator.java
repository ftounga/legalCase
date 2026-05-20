package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SF-206-07 : moteur d'analyse de la demande de <b>résiliation judiciaire</b>
 * du contrat de travail aux torts de l'employeur (FRANCE — Cass. soc.
 * 16/03/1989, Cass. soc. 20/01/1998, art. L.1411-1 CT, art. 1224 / 1227-1228
 * C.civ.).
 *
 * <p>Analyse, du point de vue de l'avocat du salarié, l'<b>opportunité</b>
 * d'une demande de résiliation judiciaire du contrat de travail aux torts de
 * l'employeur — voie procédurale dans laquelle le <b>salarié reste en poste
 * pendant l'instance</b> (contrairement à la prise d'acte qui rompt
 * immédiatement et unilatéralement le contrat). C'est une <b>voie moins
 * risquée</b> : en cas de rejet, le contrat se poursuit.</p>
 *
 * <p><b>Spécificités résiliation judiciaire</b> par rapport à la prise d'acte
 * (SF-206-05 / F-DT-39) :
 * <ul>
 *   <li>le juge apprécie les manquements <b>au jour de sa décision</b>
 *       (Cass. soc. 30/03/2010 n°08-44.236) — un grief régularisé ou non
 *       persistant pèse moins ;</li>
 *   <li>en cas de succès, effets d'un <b>licenciement sans cause réelle et
 *       sérieuse</b> à la date de la décision ({@link DateEffetProbable#DATE_DECISION}),
 *       ou à la date du licenciement si le salarié a été licencié en cours
 *       d'instance ({@link DateEffetProbable#DATE_LICENCIEMENT} — Cass. soc.
 *       21/12/2006 n°05-42.251) ;</li>
 *   <li>le rejet ne rompt pas le contrat (à signaler dans les
 *       {@code messages}).</li>
 * </ul></p>
 *
 * <p><b>Verdict</b> :
 * <ul>
 *   <li>{@link Verdict#RESILIATION_FAVORABLE} — manquements graves et persistants
 *       au jour de la demande, score &ge; seuil favorable ;</li>
 *   <li>{@link Verdict#RESILIATION_INCERTAINE} — issue incertaine ;</li>
 *   <li>{@link Verdict#RESILIATION_DEFAVORABLE} — manquements insuffisants ou
 *       régularisés, demande probable de rejet.</li>
 * </ul></p>
 *
 * <p><b>Score 0-100</b> : somme pondérée des manquements retenus, modulée par
 * {@code manquementsPersistantsAuJourDemande} (Cass. soc. 30/03/2010 — manquement
 * non persistant pèse moins, score divisé par 2).</p>
 *
 * <p><b>Pays</b> : FRANCE uniquement — la résiliation judiciaire CPH avec
 * effets licenciement sans cause est un mécanisme franco-français sans
 * équivalent direct en droit belge (la voie BE est la résolution / référé
 * devant le tribunal du travail — outil F-207-05 distinct).</p>
 */
public final class ResiliationJudiciaireCphCalculator {

    /** Solidité de la demande de résiliation judiciaire du point de vue de l'avocat du salarié. */
    public enum Verdict {
        RESILIATION_FAVORABLE,
        RESILIATION_INCERTAINE,
        RESILIATION_DEFAVORABLE
    }

    /**
     * Date d'effet probable de la résiliation prononcée (effets licenciement
     * sans cause réelle et sérieuse).
     *
     * <ul>
     *   <li>{@link #DATE_DECISION} : effets à la date du jugement de
     *       résiliation (cas nominal — Cass. soc. 11/01/2007 n°05-40.626).</li>
     *   <li>{@link #DATE_LICENCIEMENT} : effets reportés à la date du
     *       licenciement intervenu en cours d'instance (Cass. soc. 21/12/2006
     *       n°05-42.251).</li>
     * </ul>
     */
    public enum DateEffetProbable {
        DATE_DECISION,
        DATE_LICENCIEMENT
    }

    /** Code structuré d'un manquement retenu — exposé dans la réponse API. */
    public enum CodeManquement {
        DT40_DEFAUT_PAIEMENT_SALAIRE,
        DT40_HARCELEMENT,
        DT40_MANQUEMENT_SECURITE,
        DT40_MODIFICATION_UNILATERALE_CONTRAT,
        DT40_DECLASSEMENT,
        DT40_DISCRIMINATION,
        DT40_HEURES_SUP_NON_PAYEES,
        DT40_NON_RESPECT_DUREES_REPOS
    }

    /** Manquement retenu — structure exposée dans la réponse API. */
    public record ManquementRetenu(
            CodeManquement code,
            String libelle,
            String fondement,
            int poids,
            String explication
    ) {}

    // ── Poids des manquements (calibration cohérente avec SF-206-05 prise d'acte) ──
    /** Poids d'un manquement majeur (harcèlement, discrimination, sécurité, défaut paiement persistant). */
    static final int POIDS_MANQUEMENT_MAJEUR = 30;
    /** Poids d'un manquement significatif (modification contrat, heures sup, déclassement). */
    static final int POIDS_MANQUEMENT_SIGNIFICATIF = 15;
    /** Poids d'un manquement marginal (non-respect repos hebdo / quotidiens). */
    static final int POIDS_MANQUEMENT_MARGINAL = 10;
    /** Seuil de défaut de paiement considéré comme "significatif" (déclenche aggravation). */
    static final java.math.BigDecimal SEUIL_IMPAYES_SIGNIFICATIF = new java.math.BigDecimal("1500");
    private static final int SCORE_MAX = 100;
    private static final int SCORE_MIN = 0;

    /** Seuil de score pour verdict FAVORABLE. */
    static final int SEUIL_VERDICT_FAVORABLE = 50;
    /** Seuil de score pour verdict INCERTAINE. */
    static final int SEUIL_VERDICT_INCERTAINE = 25;

    private ResiliationJudiciaireCphCalculator() {}

    /**
     * Analyse l'opportunité d'une demande de résiliation judiciaire du contrat
     * de travail aux torts de l'employeur.
     *
     * @param input   données saisies par l'avocat (manquements employeur + modulateurs)
     * @param country pays du workspace ("FRANCE" uniquement supporté pour cette SF)
     * @return résultat structuré (manquements retenus, score, verdict, date d'effet probable, bases juridiques)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static ResiliationJudiciaireCphResult compute(
            ResiliationJudiciaireCphInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en FRANCE — la résiliation "
                            + "judiciaire CPH avec effets licenciement sans cause est un "
                            + "mécanisme franco-français (Cass. soc. 16/03/1989 ; "
                            + "Cass. soc. 20/01/1998 ; art. L.1411-1 CT ; art. 1224, "
                            + "1227-1228 C.civ.). La voie équivalente en droit belge "
                            + "relève du tribunal du travail (référé ou résolution "
                            + "judiciaire) — outil F-207-05 distinct.");
        }
        if (input.montantImpayesEur() != null
                && input.montantImpayesEur().signum() < 0) {
            throw new IllegalArgumentException(
                    "montantImpayesEur doit être positif ou nul");
        }
        if (input.manquementsCommentaire() != null
                && input.manquementsCommentaire().length() > 2000) {
            throw new IllegalArgumentException(
                    "manquementsCommentaire ne peut dépasser 2000 caractères");
        }

        List<ManquementRetenu> manquements = detecterManquements(input);
        int score = scorePour(manquements, input);
        Verdict verdict = verdictPour(score, manquements.isEmpty());
        DateEffetProbable dateEffet = dateEffetProbablePour(input);
        List<String> bases = basesJuridiques(manquements);
        List<String> messages = construireMessages(input, manquements, verdict, dateEffet);

        return new ResiliationJudiciaireCphResult(
                List.copyOf(manquements),
                score,
                verdict,
                dateEffet,
                bases,
                messages,
                countryNormalized
        );
    }

    /**
     * Détecte les manquements retenus selon les règles figées. Ordre stable
     * (LinkedHashSet de codes) pour des snapshots JSON déterministes.
     */
    private static List<ManquementRetenu> detecterManquements(ResiliationJudiciaireCphInput in) {
        Set<CodeManquement> codes = new LinkedHashSet<>();
        List<ManquementRetenu> manquements = new ArrayList<>();

        // 1 — Défaut de paiement du salaire (Cass. soc. 20/03/2013 n°11-26.770).
        if (Boolean.TRUE.equals(in.defautPaiementSalaire())) {
            int poids;
            String explication;
            boolean montantSignificatif = in.montantImpayesEur() != null
                    && in.montantImpayesEur().compareTo(SEUIL_IMPAYES_SIGNIFICATIF) >= 0;
            boolean persistant = Boolean.TRUE.equals(in.manquementsPersistantsAuJourDemande());
            if (montantSignificatif && persistant) {
                poids = POIDS_MANQUEMENT_MAJEUR;
                explication = "Défaut de paiement du salaire significatif (>= "
                        + SEUIL_IMPAYES_SIGNIFICATIF + " €) et persistant au jour de "
                        + "la demande — manquement grave constant en jurisprudence "
                        + "(Cass. soc. 20/03/2013, Cass. soc. 02/02/2022). Montant "
                        + "impayé constaté : "
                        + (in.montantImpayesEur() != null ? in.montantImpayesEur() : "n.r.")
                        + " €.";
            } else if (persistant) {
                poids = POIDS_MANQUEMENT_MAJEUR;
                explication = "Défaut de paiement du salaire persistant au jour de "
                        + "la demande — manquement grave constant en jurisprudence "
                        + "(Cass. soc. 20/03/2013).";
            } else {
                poids = POIDS_MANQUEMENT_SIGNIFICATIF;
                explication = "Défaut de paiement du salaire — à apprécier au "
                        + "regard de la persistance AU JOUR DE LA DÉCISION du juge "
                        + "(Cass. soc. 30/03/2010 n°08-44.236 — manquement régularisé "
                        + "pèse moins en résiliation judiciaire).";
            }
            codes.add(CodeManquement.DT40_DEFAUT_PAIEMENT_SALAIRE);
            manquements.add(new ManquementRetenu(
                    CodeManquement.DT40_DEFAUT_PAIEMENT_SALAIRE,
                    "Défaut ou retard de paiement du salaire",
                    "Cass. soc. 20/03/2013 n°11-26.770 ; Cass. soc. 02/02/2022 n°20-21.290 ; art. L.3241-1 CT",
                    poids,
                    explication));
        }

        // 2 — Harcèlement moral ou sexuel (Cass. soc. 26/01/2011 — manquement
        //     grave par nature).
        if (Boolean.TRUE.equals(in.harcelement())) {
            codes.add(CodeManquement.DT40_HARCELEMENT);
            manquements.add(new ManquementRetenu(
                    CodeManquement.DT40_HARCELEMENT,
                    "Harcèlement moral ou sexuel",
                    "Art. L.1152-1 et L.1153-1 CT ; L.1152-3 et L.1153-4 CT (nullité) ; Cass. soc. 26/01/2011 n°09-72.284",
                    POIDS_MANQUEMENT_MAJEUR,
                    "Le harcèlement moral ou sexuel constitue un manquement grave "
                            + "par nature — régulièrement retenu comme manquement "
                            + "suffisamment grave pour justifier la résiliation "
                            + "judiciaire aux torts de l'employeur."));
        }

        // 3 — Manquement à l'obligation de sécurité (Cass. soc. 22/06/2017 —
        //     obligation de moyens renforcée).
        if (Boolean.TRUE.equals(in.manquementSecurite())) {
            codes.add(CodeManquement.DT40_MANQUEMENT_SECURITE);
            manquements.add(new ManquementRetenu(
                    CodeManquement.DT40_MANQUEMENT_SECURITE,
                    "Manquement à l'obligation de sécurité",
                    "Art. L.4121-1 CT ; Cass. soc. 22/06/2017 n°16-15.507 ; Cass. soc. 25/11/2015 n°14-24.444",
                    POIDS_MANQUEMENT_MAJEUR,
                    "Le manquement à l'obligation de sécurité (obligation de moyens "
                            + "renforcée) est régulièrement retenu comme manquement "
                            + "suffisamment grave pour justifier la résiliation "
                            + "judiciaire aux torts de l'employeur."));
        }

        // 4 — Modification unilatérale du contrat (Cass. soc. 10/07/1996).
        if (Boolean.TRUE.equals(in.modificationUnilateraleContrat())) {
            codes.add(CodeManquement.DT40_MODIFICATION_UNILATERALE_CONTRAT);
            manquements.add(new ManquementRetenu(
                    CodeManquement.DT40_MODIFICATION_UNILATERALE_CONTRAT,
                    "Modification unilatérale d'un élément essentiel du contrat",
                    "Cass. soc. 10/07/1996 n°93-41.137 ; art. L.1221-1 CT",
                    POIDS_MANQUEMENT_SIGNIFICATIF,
                    "La modification unilatérale d'un élément essentiel du contrat "
                            + "(rémunération, qualification, durée du travail, lieu hors "
                            + "clause de mobilité valable) imposée sans accord du salarié "
                            + "constitue un manquement de l'employeur."));
        }

        // 5 — Déclassement / mise à l'écart (Cass. soc. 18/12/2013).
        if (Boolean.TRUE.equals(in.declassement())) {
            codes.add(CodeManquement.DT40_DECLASSEMENT);
            manquements.add(new ManquementRetenu(
                    CodeManquement.DT40_DECLASSEMENT,
                    "Déclassement professionnel / mise à l'écart",
                    "Cass. soc. 18/12/2013 n°12-13.681 ; art. L.1221-1 CT",
                    POIDS_MANQUEMENT_SIGNIFICATIF,
                    "Le déclassement (retrait des responsabilités, mise au placard) ou "
                            + "la mise à l'écart sans justification objective peut "
                            + "constituer un manquement grave selon les circonstances."));
        }

        // 6 — Discrimination (Cass. soc. 11/01/2012 — art. L.1132-1 CT).
        if (Boolean.TRUE.equals(in.discrimination())) {
            codes.add(CodeManquement.DT40_DISCRIMINATION);
            manquements.add(new ManquementRetenu(
                    CodeManquement.DT40_DISCRIMINATION,
                    "Discrimination (sexe, âge, origine, syndicale, état de santé, etc.)",
                    "Art. L.1132-1 et L.1132-4 CT (nullité) ; Cass. soc. 11/01/2012 n°10-28.213",
                    POIDS_MANQUEMENT_MAJEUR,
                    "La discrimination prohibée par L.1132-1 CT constitue un "
                            + "manquement grave par nature, régulièrement retenu comme "
                            + "suffisamment grave pour justifier la résiliation judiciaire."));
        }

        // 7 — Non-paiement des heures supplémentaires (Cass. soc. 30/05/2018).
        if (Boolean.TRUE.equals(in.heuresSupNonPayees())) {
            codes.add(CodeManquement.DT40_HEURES_SUP_NON_PAYEES);
            manquements.add(new ManquementRetenu(
                    CodeManquement.DT40_HEURES_SUP_NON_PAYEES,
                    "Non-paiement des heures supplémentaires",
                    "Art. L.3171-4 CT ; Cass. soc. 30/05/2018 n°16-22.890",
                    POIDS_MANQUEMENT_SIGNIFICATIF,
                    "Le non-paiement répété des heures supplémentaires effectuées "
                            + "constitue un manquement contractuel régulièrement retenu "
                            + "comme manquement sérieux (à apprécier selon le volume et la "
                            + "durée de l'impayé)."));
        }

        // 8 — Non-respect des durées maximales / repos (L.3132-1 et s. CT).
        if (Boolean.TRUE.equals(in.nonRespectDureesRepos())) {
            codes.add(CodeManquement.DT40_NON_RESPECT_DUREES_REPOS);
            manquements.add(new ManquementRetenu(
                    CodeManquement.DT40_NON_RESPECT_DUREES_REPOS,
                    "Non-respect des durées maximales de travail / temps de repos",
                    "Art. L.3121-18 s. CT ; L.3131-1 (repos quotidien) ; L.3132-2 (repos hebdo) ; directive 2003/88/CE",
                    POIDS_MANQUEMENT_MARGINAL,
                    "Le dépassement des durées maximales ou la privation de repos "
                            + "obligatoires constitue un manquement à part entière, "
                            + "souvent retenu en cumul avec d'autres manquements."));
        }

        return manquements;
    }

    /**
     * Score = somme pondérée des manquements, divisée par 2 si les manquements
     * ne sont pas persistants au jour de la demande (Cass. soc. 30/03/2010 —
     * le juge apprécie au jour de sa décision, un manquement non persistant
     * pèse moins). Plafonné à 100, plancher 0.
     */
    private static int scorePour(List<ManquementRetenu> manquements,
                                 ResiliationJudiciaireCphInput in) {
        int score = 0;
        for (ManquementRetenu m : manquements) {
            score += m.poids();
        }
        // Modulateur : manquements non persistants au jour de la demande → poids divisé par 2.
        if (Boolean.FALSE.equals(in.manquementsPersistantsAuJourDemande()) && !manquements.isEmpty()) {
            score = score / 2;
        }
        return Math.max(SCORE_MIN, Math.min(SCORE_MAX, score));
    }

    /**
     * Verdict piloté par le score et la présence d'au moins un manquement.
     *
     * <ul>
     *   <li>Aucun manquement → {@link Verdict#RESILIATION_DEFAVORABLE} (score 0).</li>
     *   <li>Score &ge; {@link #SEUIL_VERDICT_FAVORABLE} → {@link Verdict#RESILIATION_FAVORABLE}.</li>
     *   <li>Score &ge; {@link #SEUIL_VERDICT_INCERTAINE} → {@link Verdict#RESILIATION_INCERTAINE}.</li>
     *   <li>Sinon → {@link Verdict#RESILIATION_DEFAVORABLE}.</li>
     * </ul>
     */
    private static Verdict verdictPour(int score, boolean aucunManquement) {
        if (aucunManquement) {
            return Verdict.RESILIATION_DEFAVORABLE;
        }
        if (score >= SEUIL_VERDICT_FAVORABLE) {
            return Verdict.RESILIATION_FAVORABLE;
        }
        if (score >= SEUIL_VERDICT_INCERTAINE) {
            return Verdict.RESILIATION_INCERTAINE;
        }
        return Verdict.RESILIATION_DEFAVORABLE;
    }

    /**
     * Date d'effet probable :
     * <ul>
     *   <li>{@link DateEffetProbable#DATE_LICENCIEMENT} si un licenciement est
     *       intervenu en cours d'instance (Cass. soc. 21/12/2006 n°05-42.251) ;</li>
     *   <li>{@link DateEffetProbable#DATE_DECISION} sinon (cas nominal — effets à
     *       la date du jugement de résiliation, Cass. soc. 11/01/2007 n°05-40.626).</li>
     * </ul>
     */
    private static DateEffetProbable dateEffetProbablePour(ResiliationJudiciaireCphInput in) {
        if (Boolean.TRUE.equals(in.licenciementIntervenuEnCoursInstance())) {
            return DateEffetProbable.DATE_LICENCIEMENT;
        }
        return DateEffetProbable.DATE_DECISION;
    }

    private static List<String> basesJuridiques(List<ManquementRetenu> manquements) {
        Set<String> bases = new LinkedHashSet<>();
        bases.add("Cass. soc. 16/03/1989 (consécration de la résiliation judiciaire)");
        bases.add("Cass. soc. 20/01/1998 (effets licenciement sans cause réelle et sérieuse)");
        bases.add("Art. L.1411-1 CT (compétence du Conseil de prud'hommes)");
        bases.add("Art. 1224 et 1227-1228 C.civ. (résolution judiciaire pour inexécution)");
        bases.add("Cass. soc. 30/03/2010 n°08-44.236 (appréciation au jour de la décision)");
        bases.add("Cass. soc. 11/01/2007 n°05-40.626 (effets à la date de la décision)");
        bases.add("Cass. soc. 21/12/2006 n°05-42.251 (effets à la date du licenciement intervenu en cours d'instance)");
        for (ManquementRetenu m : manquements) {
            if (m.fondement() != null && !m.fondement().isBlank()) {
                bases.add(m.fondement());
            }
        }
        return new ArrayList<>(bases);
    }

    private static List<String> construireMessages(ResiliationJudiciaireCphInput in,
                                                   List<ManquementRetenu> manquements,
                                                   Verdict verdict,
                                                   DateEffetProbable dateEffet) {
        List<String> msgs = new ArrayList<>();
        switch (verdict) {
            case RESILIATION_FAVORABLE -> msgs.add(
                    "Résiliation judiciaire favorable : les manquements cumulés et "
                            + "leur gravité sont susceptibles d'être jugés suffisamment "
                            + "graves pour empêcher la poursuite du contrat. Effets "
                            + "probables : licenciement sans cause réelle et sérieuse à "
                            + libelleDateEffet(dateEffet) + ".");
            case RESILIATION_INCERTAINE -> msgs.add(
                    "Résiliation judiciaire incertaine : la solidité des manquements "
                            + "est variable. Sécuriser la preuve, vérifier la persistance "
                            + "AU JOUR DE LA DÉCISION du juge (Cass. soc. 30/03/2010), "
                            + "compléter les pièces avant l'audience.");
            case RESILIATION_DEFAVORABLE -> {
                if (manquements.isEmpty()) {
                    msgs.add("Résiliation judiciaire défavorable : aucun manquement "
                            + "n'est coché. En l'état, la demande paraît vouée au rejet — "
                            + "DÉCONSEILLER d'introduire l'instance.");
                } else {
                    msgs.add("Résiliation judiciaire défavorable : les manquements "
                            + "articulés ne paraissent pas suffisamment graves ou ne sont "
                            + "pas persistants au jour de la demande. Risque élevé de "
                            + "rejet — DÉCONSEILLER en l'état.");
                }
            }
        }

        // Rappel structurant — voie moins risquée que la prise d'acte.
        msgs.add("Voie moins risquée que la prise d'acte : un rejet de la "
                + "demande de résiliation judiciaire NE ROMPT PAS LE CONTRAT (le "
                + "salarié reste en poste pendant et après l'instance, "
                + "contrairement à la prise d'acte qui produit effet immédiat).");

        // Rappel persistance au jour de la décision.
        if (Boolean.FALSE.equals(in.manquementsPersistantsAuJourDemande()) && !manquements.isEmpty()) {
            msgs.add("Attention : un ou plusieurs manquements ont été pris en "
                    + "compte avec un poids réduit (manquement non persistant au "
                    + "jour de la demande — Cass. soc. 30/03/2010 n°08-44.236). "
                    + "Vérifier l'actualité de chaque manquement, le juge "
                    + "appréciant au jour de sa décision.");
        }

        // Recommandations procédurales (verdicts non défavorables).
        if (verdict != Verdict.RESILIATION_DEFAVORABLE) {
            msgs.add("Documenter chaque manquement par pièces probantes "
                    + "(bulletins, courriers, attestations, certificats médicaux) "
                    + "avant la saisine du CPH, et maintenir la veille jusqu'à "
                    + "l'audience (un manquement persistant à la décision pèse "
                    + "plus qu'un manquement régularisé en cours d'instance).");
        }

        // Information sur le report d'effet en cas de licenciement en cours d'instance.
        if (dateEffet == DateEffetProbable.DATE_LICENCIEMENT) {
            msgs.add("Un licenciement est intervenu en cours d'instance : si la "
                    + "résiliation est prononcée, ses effets seront reportés à la "
                    + "date du licenciement (Cass. soc. 21/12/2006 n°05-42.251).");
        }

        // Information statut salarié en poste (rappel pédagogique).
        if (Boolean.FALSE.equals(in.salarieToujoursEnPoste())
                && !Boolean.TRUE.equals(in.licenciementIntervenuEnCoursInstance())) {
            msgs.add("Vigilance : la résiliation judiciaire suppose le maintien "
                    + "du salarié en poste pendant l'instance. Si le contrat est "
                    + "déjà rompu pour un autre motif (démission, rupture "
                    + "conventionnelle, etc.), la voie de la résiliation judiciaire "
                    + "n'est plus ouverte — réorienter vers la contestation du "
                    + "motif de rupture.");
        }

        return msgs;
    }

    private static String libelleDateEffet(DateEffetProbable d) {
        return switch (d) {
            case DATE_DECISION -> "la date du jugement de résiliation";
            case DATE_LICENCIEMENT -> "la date du licenciement intervenu en cours d'instance";
        };
    }

    /**
     * Compatibilité de packaging — référence statique au seuil de défaut de
     * paiement pour les tests (publique en accès package).
     */
    static BigDecimal seuilImpayesSignificatif() {
        return SEUIL_IMPAYES_SIGNIFICATIF;
    }
}
