package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SF-206-05 : moteur d'analyse de la prise d'acte de la rupture aux torts de
 * l'employeur (FRANCE — Cass. soc. 25/06/2003 n°01-42.679, jurisprudence
 * consolidée).
 *
 * <p>Analyse, du point de vue de l'avocat du salarié, la <b>solidité de la
 * prise d'acte</b> avant qu'elle ne soit notifiée : la prise d'acte est une
 * rupture <b>unilatérale et immédiate</b> du salarié, qui n'a d'effet
 * « licenciement sans cause réelle et sérieuse » que si le juge retient que
 * les manquements imputés à l'employeur étaient <i>suffisamment graves pour
 * empêcher la poursuite du contrat de travail</i> (Cass. soc. 26/03/2014
 * n°12-23.634).</p>
 *
 * <p><b>Verdict</b> :
 * <ul>
 *   <li>{@link Verdict#PRISE_ACTE_FAVORABLE} — effets {@link EffetProbable#LICENCIEMENT_SANS_CAUSE}
 *       ou {@link EffetProbable#LICENCIEMENT_NUL} probables ;</li>
 *   <li>{@link Verdict#PRISE_ACTE_RISQUEE} — issue incertaine ;</li>
 *   <li>{@link Verdict#PRISE_ACTE_DEFAVORABLE} — effets {@link EffetProbable#DEMISSION}
 *       probables.</li>
 * </ul></p>
 *
 * <p><b>Bascule {@code LICENCIEMENT_NUL}</b> : si harcèlement OU discrimination
 * est retenu, l'effet probable est {@link EffetProbable#LICENCIEMENT_NUL}
 * (plus fort que l'absence de cause — art. L.1132-4 et L.1152-3 CT).</p>
 *
 * <p><b>Score 0-100</b> : somme pondérée des griefs retenus, modulée par les
 * deux modulateurs {@code griefsActuelsEtPersistants} (Cass. soc. 26/03/2014
 * n°12-23.634 — un grief ancien régularisé pèse moins) et
 * {@code griefRendImpossiblePoursuite} (le critère jurisprudentiel central
 * Cass. soc. 26/03/2014 n°12-21.372).</p>
 *
 * <p><b>Pays</b> : FRANCE uniquement — la prise d'acte CPH avec effets
 * licenciement / démission est un mécanisme franco-français sans équivalent
 * direct en droit belge.</p>
 */
public final class PriseActeRuptureCalculator {

    /** Solidité de la prise d'acte du point de vue de l'avocat du salarié. */
    public enum Verdict {
        PRISE_ACTE_FAVORABLE,
        PRISE_ACTE_RISQUEE,
        PRISE_ACTE_DEFAVORABLE
    }

    /** Effet probable du jugement CPH sur la qualification de la rupture. */
    public enum EffetProbable {
        LICENCIEMENT_SANS_CAUSE,
        LICENCIEMENT_NUL,
        DEMISSION
    }

    /** Code structuré d'un grief retenu — exposé dans la réponse API. */
    public enum CodeGrief {
        DT39_DEFAUT_PAIEMENT_SALAIRE,
        DT39_HARCELEMENT,
        DT39_MANQUEMENT_SECURITE,
        DT39_MODIFICATION_UNILATERALE_CONTRAT,
        DT39_DECLASSEMENT,
        DT39_DISCRIMINATION,
        DT39_HEURES_SUP_NON_PAYEES,
        DT39_NON_RESPECT_DUREES_REPOS
    }

    /** Grief retenu — structure exposée dans la réponse API. */
    public record GriefRetenu(
            CodeGrief code,
            String libelle,
            String fondement,
            int poids,
            String explication
    ) {}

    // ── Poids des griefs (arbitrage cohérent avec POIDS_MOTIF_MAJEUR=50 / POIDS_IRREGULARITE_FORME=20 de SF-206-01) ──
    /** Poids d'un grief majeur (harcèlement, discrimination, sécurité, défaut paiement persistant). */
    static final int POIDS_GRIEF_MAJEUR = 30;
    /** Poids d'un grief significatif (modification contrat, heures sup, déclassement). */
    static final int POIDS_GRIEF_SIGNIFICATIF = 15;
    /** Poids d'un grief marginal (non-respect repos hebdo / quotidiens). */
    static final int POIDS_GRIEF_MARGINAL = 10;
    /** Modulateur additif si le critère jurisprudentiel "impossible poursuite" est coché. */
    static final int BONUS_GRIEF_IMPOSSIBLE_POURSUITE = 20;
    /** Seuil de défaut de paiement considéré comme "significatif" (déclenche aggravation). */
    static final java.math.BigDecimal SEUIL_IMPAYES_SIGNIFICATIF = new java.math.BigDecimal("1500");
    private static final int SCORE_MAX = 100;
    private static final int SCORE_MIN = 0;

    /** Seuil de score pour verdict FAVORABLE (effets licenciement probables). */
    static final int SEUIL_VERDICT_FAVORABLE = 50;
    /** Seuil de score pour verdict RISQUEE (issue incertaine). */
    static final int SEUIL_VERDICT_RISQUEE = 25;

    private PriseActeRuptureCalculator() {}

    /**
     * Analyse la solidité d'une prise d'acte de la rupture.
     *
     * @param input   données saisies par l'avocat (griefs imputés à l'employeur + modulateurs)
     * @param country pays du workspace ("FRANCE" uniquement supporté pour cette SF)
     * @return résultat structuré (griefs retenus, score, verdict, effet probable, bases juridiques)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static PriseActeRuptureResult compute(
            PriseActeRuptureInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en FRANCE — la prise d'acte de la "
                            + "rupture aux torts de l'employeur avec effets licenciement / "
                            + "démission est un mécanisme franco-français (Cass. soc. "
                            + "25/06/2003 n°01-42.679).");
        }
        if (input.montantImpayesEur() != null
                && input.montantImpayesEur().signum() < 0) {
            throw new IllegalArgumentException(
                    "montantImpayesEur doit être positif ou nul");
        }
        if (input.griefsCommentaire() != null
                && input.griefsCommentaire().length() > 2000) {
            throw new IllegalArgumentException(
                    "griefsCommentaire ne peut dépasser 2000 caractères");
        }

        List<GriefRetenu> griefs = detecterGriefs(input);
        int score = scorePour(griefs, input);
        Verdict verdict = verdictPour(score, griefs.isEmpty());
        EffetProbable effet = effetProbablePour(verdict, griefs);
        List<String> bases = basesJuridiques(griefs);
        List<String> messages = construireMessages(input, griefs, verdict, effet);

        return new PriseActeRuptureResult(
                List.copyOf(griefs),
                score,
                verdict,
                effet,
                bases,
                messages,
                countryNormalized
        );
    }

    /**
     * Détecte les griefs retenus selon les règles figées. Ordre stable
     * (LinkedHashSet de codes) pour des snapshots JSON déterministes.
     */
    private static List<GriefRetenu> detecterGriefs(PriseActeRuptureInput in) {
        Set<CodeGrief> codes = new LinkedHashSet<>();
        List<GriefRetenu> griefs = new ArrayList<>();

        // 1 — Défaut de paiement du salaire (Cass. soc. 20/03/2013 n°11-26.770)
        if (Boolean.TRUE.equals(in.defautPaiementSalaire())) {
            int poids;
            String explication;
            boolean montantSignificatif = in.montantImpayesEur() != null
                    && in.montantImpayesEur().compareTo(SEUIL_IMPAYES_SIGNIFICATIF) >= 0;
            boolean persistant = Boolean.TRUE.equals(in.griefsActuelsEtPersistants());
            if (montantSignificatif && persistant) {
                poids = POIDS_GRIEF_MAJEUR;
                explication = "Défaut de paiement du salaire significatif (>= "
                        + SEUIL_IMPAYES_SIGNIFICATIF + " €) et persistant — manquement "
                        + "grave constant en jurisprudence (Cass. soc. 20/03/2013, "
                        + "Cass. soc. 02/02/2022). Montant impayé constaté : "
                        + (in.montantImpayesEur() != null ? in.montantImpayesEur() : "n.r.") + " €.";
            } else if (persistant) {
                poids = POIDS_GRIEF_MAJEUR;
                explication = "Défaut de paiement du salaire persistant — manquement "
                        + "grave constant en jurisprudence (Cass. soc. 20/03/2013).";
            } else {
                poids = POIDS_GRIEF_SIGNIFICATIF;
                explication = "Défaut de paiement du salaire — manquement à apprécier "
                        + "au regard du caractère ancien ou ponctuel (un défaut isolé "
                        + "régularisé pèse moins, Cass. soc. 26/03/2014 n°12-23.634).";
            }
            codes.add(CodeGrief.DT39_DEFAUT_PAIEMENT_SALAIRE);
            griefs.add(new GriefRetenu(
                    CodeGrief.DT39_DEFAUT_PAIEMENT_SALAIRE,
                    "Défaut ou retard de paiement du salaire",
                    "Cass. soc. 20/03/2013 n°11-26.770 ; Cass. soc. 02/02/2022 n°20-21.290 ; art. L.3241-1 CT",
                    poids,
                    explication));
        }

        // 2 — Harcèlement moral ou sexuel (Cass. soc. 26/01/2011 — manquement
        //     grave par nature). Bascule LICENCIEMENT_NUL (L.1152-3 CT).
        if (Boolean.TRUE.equals(in.harcelement())) {
            codes.add(CodeGrief.DT39_HARCELEMENT);
            griefs.add(new GriefRetenu(
                    CodeGrief.DT39_HARCELEMENT,
                    "Harcèlement moral ou sexuel",
                    "Art. L.1152-1 et L.1153-1 CT ; L.1152-3 et L.1153-4 CT (nullité) ; Cass. soc. 26/01/2011 n°09-72.284",
                    POIDS_GRIEF_MAJEUR,
                    "Le harcèlement moral ou sexuel constitue un manquement grave par "
                            + "nature ; la prise d'acte fondée sur ce grief produit les "
                            + "effets d'un licenciement nul (L.1152-3 / L.1153-4 CT — "
                            + "indemnités plancher 6 mois de salaire)."));
        }

        // 3 — Manquement à l'obligation de sécurité (Cass. soc. 22/06/2017
        //     n°16-15.507 — obligation de moyens renforcée).
        if (Boolean.TRUE.equals(in.manquementSecurite())) {
            codes.add(CodeGrief.DT39_MANQUEMENT_SECURITE);
            griefs.add(new GriefRetenu(
                    CodeGrief.DT39_MANQUEMENT_SECURITE,
                    "Manquement à l'obligation de sécurité",
                    "Art. L.4121-1 CT ; Cass. soc. 22/06/2017 n°16-15.507 ; Cass. soc. 25/11/2015 n°14-24.444",
                    POIDS_GRIEF_MAJEUR,
                    "Le manquement à l'obligation de sécurité (obligation de moyens "
                            + "renforcée) est régulièrement retenu comme grief "
                            + "suffisamment grave pour justifier la prise d'acte aux "
                            + "torts de l'employeur."));
        }

        // 4 — Modification unilatérale du contrat (rémunération, fonctions, lieu)
        //     — Cass. soc. 10/07/1996 n°93-41.137 et jurisprudence consolidée.
        if (Boolean.TRUE.equals(in.modificationUnilateraleContrat())) {
            codes.add(CodeGrief.DT39_MODIFICATION_UNILATERALE_CONTRAT);
            griefs.add(new GriefRetenu(
                    CodeGrief.DT39_MODIFICATION_UNILATERALE_CONTRAT,
                    "Modification unilatérale d'un élément essentiel du contrat",
                    "Cass. soc. 10/07/1996 n°93-41.137 ; art. L.1221-1 CT",
                    POIDS_GRIEF_SIGNIFICATIF,
                    "La modification unilatérale d'un élément essentiel du contrat "
                            + "(rémunération, qualification, durée du travail, lieu hors "
                            + "clause de mobilité valable) imposée sans accord du salarié "
                            + "constitue un manquement de l'employeur."));
        }

        // 5 — Déclassement / mise à l'écart (Cass. soc. 18/12/2013 n°12-13.681).
        if (Boolean.TRUE.equals(in.declassement())) {
            codes.add(CodeGrief.DT39_DECLASSEMENT);
            griefs.add(new GriefRetenu(
                    CodeGrief.DT39_DECLASSEMENT,
                    "Déclassement professionnel / mise à l'écart",
                    "Cass. soc. 18/12/2013 n°12-13.681 ; art. L.1221-1 CT",
                    POIDS_GRIEF_SIGNIFICATIF,
                    "Le déclassement (retrait des responsabilités, mise au placard) ou "
                            + "la mise à l'écart sans justification objective peut "
                            + "constituer un manquement grave selon les circonstances."));
        }

        // 6 — Discrimination (Cass. soc. 11/01/2012 — bascule LICENCIEMENT_NUL,
        //     art. L.1134-4 CT).
        if (Boolean.TRUE.equals(in.discrimination())) {
            codes.add(CodeGrief.DT39_DISCRIMINATION);
            griefs.add(new GriefRetenu(
                    CodeGrief.DT39_DISCRIMINATION,
                    "Discrimination (sexe, âge, origine, syndicale, état de santé, etc.)",
                    "Art. L.1132-1 et L.1132-4 CT (nullité) ; Cass. soc. 11/01/2012 n°10-28.213",
                    POIDS_GRIEF_MAJEUR,
                    "La discrimination prohibée par L.1132-1 CT constitue un "
                            + "manquement grave par nature ; la prise d'acte fondée sur "
                            + "ce grief produit les effets d'un licenciement nul (L.1132-4 "
                            + "CT — indemnités plancher 6 mois de salaire)."));
        }

        // 7 — Non-paiement des heures supplémentaires (Cass. soc. 30/05/2018
        //     n°16-22.890 — manquement répété significatif).
        if (Boolean.TRUE.equals(in.heuresSupNonPayees())) {
            codes.add(CodeGrief.DT39_HEURES_SUP_NON_PAYEES);
            griefs.add(new GriefRetenu(
                    CodeGrief.DT39_HEURES_SUP_NON_PAYEES,
                    "Non-paiement des heures supplémentaires",
                    "Art. L.3171-4 CT ; Cass. soc. 30/05/2018 n°16-22.890",
                    POIDS_GRIEF_SIGNIFICATIF,
                    "Le non-paiement répété des heures supplémentaires effectuées "
                            + "constitue un manquement contractuel régulièrement retenu "
                            + "comme grief sérieux (à apprécier selon le volume et la "
                            + "durée de l'impayé)."));
        }

        // 8 — Non-respect des durées maximales / repos (L.3132-1 et s. CT —
        //     manquement marginal sauf cumul ou risque sanitaire).
        if (Boolean.TRUE.equals(in.nonRespectDureesRepos())) {
            codes.add(CodeGrief.DT39_NON_RESPECT_DUREES_REPOS);
            griefs.add(new GriefRetenu(
                    CodeGrief.DT39_NON_RESPECT_DUREES_REPOS,
                    "Non-respect des durées maximales de travail / temps de repos",
                    "Art. L.3121-18 s. CT ; L.3131-1 (repos quotidien) ; L.3132-2 (repos hebdo) ; directive 2003/88/CE",
                    POIDS_GRIEF_MARGINAL,
                    "Le dépassement des durées maximales ou la privation de repos "
                            + "obligatoires constitue un manquement à part entière, "
                            + "souvent retenu en cumul avec d'autres griefs."));
        }

        return griefs;
    }

    /**
     * Score = somme pondérée des griefs, divisée par 2 si griefs anciens
     * régularisés (Cass. soc. 26/03/2014 n°12-23.634), augmentée de
     * {@link #BONUS_GRIEF_IMPOSSIBLE_POURSUITE} si le critère jurisprudentiel
     * central est coché. Plafonné à 100, plancher 0.
     */
    private static int scorePour(List<GriefRetenu> griefs, PriseActeRuptureInput in) {
        int score = 0;
        for (GriefRetenu g : griefs) {
            score += g.poids();
        }
        // Modulateur 1 : griefs anciens régularisés → poids divisé par 2.
        if (Boolean.FALSE.equals(in.griefsActuelsEtPersistants()) && !griefs.isEmpty()) {
            score = score / 2;
        }
        // Modulateur 2 : critère jurisprudentiel central (impossibilité de poursuite).
        if (Boolean.TRUE.equals(in.griefRendImpossiblePoursuite()) && !griefs.isEmpty()) {
            score += BONUS_GRIEF_IMPOSSIBLE_POURSUITE;
        }
        return Math.max(SCORE_MIN, Math.min(SCORE_MAX, score));
    }

    /**
     * Verdict piloté par le score et la présence d'au moins un grief.
     *
     * <ul>
     *   <li>Aucun grief → {@link Verdict#PRISE_ACTE_DEFAVORABLE} (score 0).</li>
     *   <li>Score >= {@link #SEUIL_VERDICT_FAVORABLE} → {@link Verdict#PRISE_ACTE_FAVORABLE}.</li>
     *   <li>Score >= {@link #SEUIL_VERDICT_RISQUEE} → {@link Verdict#PRISE_ACTE_RISQUEE}.</li>
     *   <li>Sinon → {@link Verdict#PRISE_ACTE_DEFAVORABLE}.</li>
     * </ul>
     */
    private static Verdict verdictPour(int score, boolean aucunGrief) {
        if (aucunGrief) {
            return Verdict.PRISE_ACTE_DEFAVORABLE;
        }
        if (score >= SEUIL_VERDICT_FAVORABLE) {
            return Verdict.PRISE_ACTE_FAVORABLE;
        }
        if (score >= SEUIL_VERDICT_RISQUEE) {
            return Verdict.PRISE_ACTE_RISQUEE;
        }
        return Verdict.PRISE_ACTE_DEFAVORABLE;
    }

    /**
     * Effet probable : LICENCIEMENT_NUL si harcèlement OU discrimination (peu
     * importe le verdict — la nullité prime sur l'absence de cause) ; sinon
     * LICENCIEMENT_SANS_CAUSE pour FAVORABLE/RISQUEE ; DEMISSION pour
     * DEFAVORABLE.
     */
    private static EffetProbable effetProbablePour(Verdict verdict, List<GriefRetenu> griefs) {
        boolean nullite = griefs.stream().anyMatch(g ->
                g.code() == CodeGrief.DT39_HARCELEMENT
                        || g.code() == CodeGrief.DT39_DISCRIMINATION);
        if (verdict == Verdict.PRISE_ACTE_DEFAVORABLE) {
            return EffetProbable.DEMISSION;
        }
        if (nullite) {
            return EffetProbable.LICENCIEMENT_NUL;
        }
        return EffetProbable.LICENCIEMENT_SANS_CAUSE;
    }

    private static List<String> basesJuridiques(List<GriefRetenu> griefs) {
        Set<String> bases = new LinkedHashSet<>();
        bases.add("Cass. soc. 25/06/2003 n°01-42.679 (consécration de la prise d'acte)");
        bases.add("Cass. soc. 26/03/2014 n°12-21.372 (critère : manquement grave empêchant la poursuite du contrat)");
        bases.add("Cass. soc. 26/03/2014 n°12-23.634 (un grief ancien régularisé pèse moins)");
        for (GriefRetenu g : griefs) {
            if (g.fondement() != null && !g.fondement().isBlank()) {
                bases.add(g.fondement());
            }
        }
        return new ArrayList<>(bases);
    }

    private static List<String> construireMessages(PriseActeRuptureInput in,
                                                   List<GriefRetenu> griefs,
                                                   Verdict verdict,
                                                   EffetProbable effet) {
        List<String> msgs = new ArrayList<>();
        switch (verdict) {
            case PRISE_ACTE_FAVORABLE -> msgs.add(
                    "Prise d'acte favorable : les griefs cumulés et leur gravité "
                            + "sont susceptibles d'être jugés suffisamment graves pour "
                            + "empêcher la poursuite du contrat. Effet probable : "
                            + libelleEffet(effet) + ".");
            case PRISE_ACTE_RISQUEE -> msgs.add(
                    "Prise d'acte risquée : la solidité des griefs est incertaine. "
                            + "Avant de notifier la prise d'acte, sécuriser la preuve, "
                            + "envisager une résiliation judiciaire (le contrat n'est "
                            + "pas rompu pendant l'instance) ou un autre levier "
                            + "(SF-206-07).");
            case PRISE_ACTE_DEFAVORABLE -> {
                if (griefs.isEmpty()) {
                    msgs.add("Prise d'acte défavorable : aucun grief n'est coché. "
                            + "En l'état, la prise d'acte produirait les effets d'une "
                            + "démission — DÉCONSEILLER.");
                } else {
                    msgs.add("Prise d'acte défavorable : les griefs articulés ne "
                            + "paraissent pas suffisamment graves pour empêcher la "
                            + "poursuite du contrat. Risque élevé d'effets démission. "
                            + "DÉCONSEILLER en l'état.");
                }
            }
        }
        // Recommandations procédurales.
        if (verdict != Verdict.PRISE_ACTE_DEFAVORABLE) {
            msgs.add("Documenter chaque grief par pièces probantes (bulletins, "
                    + "courriers, attestations, certificats médicaux) avant la "
                    + "notification de la prise d'acte au CPH.");
        }
        if (Boolean.FALSE.equals(in.griefsActuelsEtPersistants()) && !griefs.isEmpty()) {
            msgs.add("Attention : un ou plusieurs griefs anciens régularisés ont été "
                    + "pris en compte avec un poids réduit (Cass. soc. 26/03/2014 "
                    + "n°12-23.634). Vérifier l'actualité de chaque grief.");
        }
        if (effet == EffetProbable.LICENCIEMENT_NUL) {
            msgs.add("Un grief de harcèlement ou de discrimination est articulé : "
                    + "en cas de succès, l'effet probable est licenciement NUL "
                    + "(art. L.1132-4 / L.1152-3 CT — indemnités plancher 6 mois de "
                    + "salaire, sans plafond Macron).");
        }
        return msgs;
    }

    private static String libelleEffet(EffetProbable e) {
        return switch (e) {
            case LICENCIEMENT_SANS_CAUSE -> "licenciement sans cause réelle et sérieuse";
            case LICENCIEMENT_NUL -> "licenciement nul";
            case DEMISSION -> "démission";
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
