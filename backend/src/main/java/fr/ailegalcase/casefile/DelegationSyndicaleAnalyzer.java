package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-218-33 : analyseur du <b>statut et de la protection d'un délégué syndical
 * (DS) ou d'un représentant de section syndicale (RSS)</b> (art. L.2143-1 et s.,
 * L.2142-1-1, L.2143-3, L.2411-3 CT, F-DT-69). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Logique métier (invariant CLAUDE.md — un outil = une situation) :
 * <ul>
 *   <li><b>Checklist de désignation</b> — chaque item produit
 *       {@code { item, conforme, commentaire }} :
 *       <ol>
 *         <li>Effectif suffisant : DS → {@code effectif ≥ 50} ; RSS → pas de
 *             seuil d'effectif spécifique (toujours conforme à ce titre).</li>
 *         <li>Organisation représentative : DS → {@code syndicatRepresentatif}
 *             obligatoire ; RSS → {@code syndicatRepresentatif = false}
 *             attendu (le RSS existe précisément faute de représentativité).</li>
 *         <li>Score personnel (DS uniquement) :
 *             {@code pourcentageScorePersonnel ≥ 10} (L.2143-3, sauf
 *             exceptions).</li>
 *       </ol></li>
 *   <li><b>Verdict de désignation</b> {@code statutDesignation} :
 *       <ul>
 *         <li>item d'effectif ou de représentativité non conforme →
 *             {@code IRREGULIERE} ;</li>
 *         <li>DS sans {@code pourcentageScorePersonnel} renseigné →
 *             {@code A_VERIFIER} (score à confirmer) ;</li>
 *         <li>tous items conformes → {@code REGULIERE}.</li>
 *       </ul></li>
 *   <li><b>Protection</b> — DS et RSS sont des salariés protégés
 *       ({@code statutProtege = OUI}). Si {@code licenciementEnvisage} :
 *       autorisation de l'inspecteur du travail absente → {@code ELEVE}
 *       (nullité + réintégration, L.2411-3) ; présente → {@code FAIBLE} ;
 *       sinon → {@code SANS_OBJET}.</li>
 * </ul>
 *
 * <p>Hors périmètre : statut protégé RP général (F-DT-30), procédure détaillée
 * devant l'inspecteur du travail / recours, élections CSE / représentativité
 * (F-DT-65). Base juridique annotée « à vérifier par avocat ».
 */
public final class DelegationSyndicaleAnalyzer {

    /** Effectif minimal pour désigner un délégué syndical (art. L.2143-3 CT). */
    static final int EFFECTIF_MIN_DS = 50;

    /** Score personnel minimal requis pour un DS (10 % des suffrages, L.2143-3). */
    static final BigDecimal SCORE_MIN_DS = BigDecimal.valueOf(10);

    static final String BASE_JURIDIQUE =
            "art. L.2143-1 et suivants CT — désignation du délégué syndical par une "
                    + "organisation syndicale représentative dans les entreprises d'au "
                    + "moins 50 salariés ; art. L.2143-3 CT — le DS doit en principe avoir "
                    + "recueilli au moins 10 % des suffrages exprimés au 1er tour des "
                    + "dernières élections ; art. L.2142-1-1 CT — désignation d'un "
                    + "représentant de section syndicale (RSS) par un syndicat non "
                    + "représentatif ; art. L.2411-3 CT — protection du délégué syndical : "
                    + "le licenciement est soumis à l'autorisation préalable de l'inspecteur "
                    + "du travail, à défaut nullité et droit à réintégration "
                    + "(à vérifier par avocat)";

    private DelegationSyndicaleAnalyzer() {
    }

    /**
     * Analyse le statut de désignation et la protection du DS / RSS : construit
     * la checklist, rend le verdict de désignation et apprécie le risque de
     * nullité du licenciement.
     */
    public static DelegationSyndicaleResult analyze(
            Integer effectif,
            MandatSyndicalType typeMandat,
            Boolean syndicatRepresentatif,
            BigDecimal pourcentageScorePersonnel,
            LocalDate dateDesignation,
            Boolean licenciementEnvisage,
            Boolean autorisationInspecteurTravail) {

        validate(effectif, typeMandat, syndicatRepresentatif, pourcentageScorePersonnel);

        int eff = effectif;
        boolean representatif = syndicatRepresentatif;
        boolean licenciement = licenciementEnvisage != null && licenciementEnvisage;
        boolean autorisation = autorisationInspecteurTravail != null && autorisationInspecteurTravail;
        boolean isDs = typeMandat == MandatSyndicalType.DELEGUE_SYNDICAL;

        List<DelegationSyndicaleChecklistItem> checklist = new ArrayList<>();
        List<String> consequences = new ArrayList<>();

        boolean blocking = false;     // item effectif/représentativité non conforme → IRREGULIERE
        boolean scoreAVerifier = false; // DS sans score → A_VERIFIER

        // ── Item 1 — effectif suffisant ─────────────────────────────────────
        boolean effectifConforme;
        if (isDs) {
            effectifConforme = eff >= EFFECTIF_MIN_DS;
            checklist.add(new DelegationSyndicaleChecklistItem(
                    "Effectif suffisant pour désigner un délégué syndical",
                    effectifConforme,
                    "Un délégué syndical peut être désigné dans les entreprises d'au moins "
                            + EFFECTIF_MIN_DS + " salariés (art. L.2143-3). Effectif renseigné : "
                            + eff + "."));
            if (!effectifConforme) {
                blocking = true;
                consequences.add("Effectif insuffisant (" + eff + " salariés) : la désignation "
                        + "d'un délégué syndical suppose un effectif d'au moins " + EFFECTIF_MIN_DS
                        + " salariés (art. L.2143-3). Envisager la désignation d'un représentant "
                        + "de section syndicale (RSS).");
            }
        } else {
            effectifConforme = true;
            checklist.add(new DelegationSyndicaleChecklistItem(
                    "Effectif suffisant pour désigner un représentant de section syndicale",
                    true,
                    "Le RSS peut être désigné sans condition d'effectif spécifique, par un "
                            + "syndicat non représentatif ayant constitué une section syndicale "
                            + "(art. L.2142-1-1). Effectif renseigné : " + eff + "."));
        }

        // ── Item 2 — organisation représentative ────────────────────────────
        boolean representativiteConforme;
        if (isDs) {
            representativiteConforme = representatif;
            checklist.add(new DelegationSyndicaleChecklistItem(
                    "Organisation syndicale représentative",
                    representativiteConforme,
                    "Le délégué syndical est désigné par une organisation syndicale "
                            + "représentative (≥ 10 % des suffrages au 1er tour CSE, "
                            + "art. L.2143-3)."));
            if (!representativiteConforme) {
                blocking = true;
                consequences.add("Organisation non représentative : seul un syndicat "
                        + "représentatif peut désigner un délégué syndical (art. L.2143-3). "
                        + "Une organisation non représentative peut désigner un représentant "
                        + "de section syndicale (RSS, art. L.2142-1-1).");
            }
        } else {
            representativiteConforme = !representatif;
            checklist.add(new DelegationSyndicaleChecklistItem(
                    "Organisation non représentative (cohérent avec le mandat RSS)",
                    representativiteConforme,
                    "Le représentant de section syndicale est désigné par un syndicat "
                            + "non représentatif (art. L.2142-1-1) : un syndicat représentatif "
                            + "désigne un délégué syndical, non un RSS."));
            if (!representativiteConforme) {
                blocking = true;
                consequences.add("Incohérence : un syndicat représentatif désigne un délégué "
                        + "syndical (art. L.2143-3), non un représentant de section syndicale. "
                        + "Vérifier la qualification du mandat.");
            }
        }

        // ── Item 3 — score personnel (DS uniquement) ────────────────────────
        if (isDs) {
            if (pourcentageScorePersonnel == null) {
                scoreAVerifier = true;
                checklist.add(new DelegationSyndicaleChecklistItem(
                        "Score personnel du candidat ≥ 10 %",
                        false,
                        "Score personnel non renseigné : le délégué syndical doit en principe "
                                + "avoir recueilli au moins 10 % des suffrages exprimés au 1er "
                                + "tour des dernières élections (art. L.2143-3), sauf exceptions. "
                                + "À confirmer."));
                consequences.add("Vérifier le score personnel du candidat aux dernières "
                        + "élections : la désignation d'un délégué syndical suppose en principe "
                        + "un score d'au moins 10 % des suffrages exprimés (art. L.2143-3, sauf "
                        + "exceptions).");
            } else {
                boolean scoreConforme = pourcentageScorePersonnel.compareTo(SCORE_MIN_DS) >= 0;
                checklist.add(new DelegationSyndicaleChecklistItem(
                        "Score personnel du candidat ≥ 10 %",
                        scoreConforme,
                        "Le délégué syndical doit en principe avoir recueilli au moins 10 % des "
                                + "suffrages exprimés au 1er tour des dernières élections "
                                + "(art. L.2143-3, sauf exceptions). Score renseigné : "
                                + pourcentageScorePersonnel + " %."));
                if (!scoreConforme) {
                    blocking = true;
                    consequences.add("Score personnel insuffisant (" + pourcentageScorePersonnel
                            + " %) : le délégué syndical doit en principe avoir recueilli au moins "
                            + "10 % des suffrages exprimés (art. L.2143-3, sauf exceptions) — "
                            + "désignation contestable.");
                }
            }
        }

        // ── Verdict de désignation ──────────────────────────────────────────
        DelegationSyndicaleStatutDesignation statutDesignation;
        if (blocking) {
            statutDesignation = DelegationSyndicaleStatutDesignation.IRREGULIERE;
        } else if (scoreAVerifier) {
            statutDesignation = DelegationSyndicaleStatutDesignation.A_VERIFIER;
        } else {
            statutDesignation = DelegationSyndicaleStatutDesignation.REGULIERE;
        }

        // ── Protection contre le licenciement ───────────────────────────────
        DelegationSyndicaleRisqueNullite risque;
        if (!licenciement) {
            risque = DelegationSyndicaleRisqueNullite.SANS_OBJET;
        } else if (!autorisation) {
            risque = DelegationSyndicaleRisqueNullite.ELEVE;
            consequences.add("Licenciement d'un salarié protégé sans autorisation de "
                    + "l'inspecteur du travail = nullité du licenciement et droit à "
                    + "réintégration (art. L.2411-3). Obtenir l'autorisation préalable de "
                    + "l'inspecteur du travail avant tout licenciement.");
        } else {
            risque = DelegationSyndicaleRisqueNullite.FAIBLE;
            consequences.add("Licenciement envisagé avec autorisation de l'inspecteur du "
                    + "travail : risque de nullité faible, sous réserve de la régularité de la "
                    + "procédure d'autorisation (art. L.2411-3).");
        }

        return new DelegationSyndicaleResult(
                eff,
                typeMandat,
                representatif,
                pourcentageScorePersonnel,
                dateDesignation,
                List.copyOf(checklist),
                statutDesignation,
                DelegationSyndicaleStatutProtege.OUI,
                licenciement,
                autorisation,
                risque,
                List.copyOf(consequences),
                BASE_JURIDIQUE);
    }

    private static void validate(Integer effectif,
                                 MandatSyndicalType typeMandat,
                                 Boolean syndicatRepresentatif,
                                 BigDecimal pourcentageScorePersonnel) {
        if (effectif == null) {
            throw new IllegalArgumentException("effectif est requis");
        }
        if (effectif <= 0) {
            throw new IllegalArgumentException("effectif doit être strictement positif");
        }
        if (typeMandat == null) {
            throw new IllegalArgumentException("typeMandat est requis");
        }
        if (syndicatRepresentatif == null) {
            throw new IllegalArgumentException("syndicatRepresentatif est requis");
        }
        if (pourcentageScorePersonnel != null
                && (pourcentageScorePersonnel.compareTo(BigDecimal.ZERO) < 0
                || pourcentageScorePersonnel.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException(
                    "pourcentageScorePersonnel doit être compris entre 0 et 100");
        }
    }
}
