package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-212-13 : moteur d'analyse de la validité d'une clause de mobilité et des
 * conséquences d'un refus de mutation (FRANCE) — jurisprudence Cass. soc.
 * constante sur la clause de mobilité.
 *
 * <p>Vérifie quatre axes principaux :
 * <ul>
 *   <li><b>Existence</b> d'une clause de mobilité contractualisée.</li>
 *   <li><b>Précision de la zone géographique</b> (Cass. soc. 07/06/2006
 *       n°04-45.846 ; Cass. soc. 24/01/2008 n°06-45.088 — "France entière"
 *       insuffisant sauf justification par le secteur d'activité).</li>
 *   <li><b>Mise en œuvre de bonne foi</b> : intérêt légitime de l'entreprise
 *       (Cass. soc. 23/02/2005 — pas de détournement de pouvoir), motif
 *       professionnel objectif, délai de prévenance raisonnable (Cass. soc.
 *       03/03/2010 — délai trop court = modification du contrat).</li>
 *   <li><b>Atteinte à la vie personnelle / familiale</b> (Cass. soc.
 *       14/10/2008 n°07-40.523).</li>
 * </ul>
 *
 * <p><b>Verdict 3 niveaux</b> :
 * <ul>
 *   <li>{@code CLAUSE_VALIDE} — clause présente + zone précise + intérêt
 *       légitime + délai suffisant + motif professionnel : refus du salarié
 *       caractérise une faute pouvant fonder un licenciement.</li>
 *   <li>{@code CLAUSE_INVALIDE} — clause présente mais entachée d'un vice
 *       (zone imprécise, intérêt non démontré, délai insuffisant, atteinte
 *       disproportionnée) : refus = pas de faute ; licenciement éventuel =
 *       sans cause réelle et sérieuse.</li>
 *   <li>{@code ABSENCE_CLAUSE} — pas de clause de mobilité : la mutation hors
 *       secteur géographique constitue une modification du contrat de travail
 *       refusable sans faute.</li>
 * </ul>
 *
 * <p><b>Pays</b> : FRANCE uniquement — la clause de mobilité est un concept
 * français. En droit belge, le régime distinct relève des conditions
 * essentielles du contrat (Loi du 03/07/1978, modification unilatérale d'un
 * élément essentiel).</p>
 */
public final class MutationClauseMobiliteCalculator {

    /** Verdict d'analyse de la validité de la clause de mobilité. */
    public enum AnalyseMutation {
        CLAUSE_VALIDE,
        CLAUSE_INVALIDE,
        ABSENCE_CLAUSE
    }

    /** Code structuré d'un critère analysé (F-IA-03). */
    public enum CodeCritere {
        DT71_CLAUSE_PRESENTE,
        DT71_ZONE_PRECISE,
        DT71_DELAI_PREVENANCE,
        DT71_MOTIF_PROFESSIONNEL
    }

    /** Type de conséquence du refus de mutation. */
    public enum TypeConsequence {
        REFUS_FAUTE_LICENCIEMENT_POSSIBLE,
        REFUS_SANS_FAUTE_CLAUSE_INVALIDE,
        REFUS_SANS_FAUTE_ABSENCE_CLAUSE,
        MODIFICATION_CONTRAT_REQUISE,
        DELAI_PREVENANCE_INSUFFISANT,
        ATTENTE_REPONSE_SALARIE,
        ACCORD_FORMEL_REQUIS
    }

    /** Point d'analyse exposé dans la réponse API. */
    public record PointAnalyse(
            CodeCritere code,
            String libelle,
            String fondement,
            String conclusion
    ) {}

    /** Conséquence du refus exposée dans la réponse API. */
    public record Consequence(
            TypeConsequence type,
            String description,
            String fondement
    ) {}

    /** Résultat calculé par {@link #compute}. */
    public record Result(
            AnalyseMutation analyseMutation,
            int scoreMutation,
            List<PointAnalyse> pointsAnalyse,
            List<Consequence> consequences,
            String consequencesRefus,
            List<String> basesJuridiques,
            List<String> messages,
            String country
    ) {}

    /** Seuil de délai de prévenance minimal raisonnable (Cass. soc. 03/03/2010). */
    private static final int DELAI_PREVENANCE_MIN_SEMAINES = 2;

    private MutationClauseMobiliteCalculator() {}

    /**
     * Point d'entrée principal.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace
     * @return résultat calculé
     * @throws IllegalArgumentException si {@code country != "FRANCE"} ou si
     *         {@code input} est null, ou si une valeur de délai est négative.
     */
    public static Result compute(MutationClauseMobiliteInput input, String country) {
        if (!"FRANCE".equalsIgnoreCase(country)) {
            throw new IllegalArgumentException(
                    "Outil réservé au droit du travail FRANCE — pays détecté : " + country);
        }
        if (input == null) {
            throw new IllegalArgumentException("Données d'entrée manquantes");
        }
        if (input.reponseSalarie() == null) {
            throw new IllegalArgumentException("Réponse du salarié requise");
        }
        if (input.delaiPrevenanceSemaines() != null && input.delaiPrevenanceSemaines() < 0) {
            throw new IllegalArgumentException(
                    "Le délai de prévenance ne peut pas être négatif");
        }

        List<PointAnalyse> points = new ArrayList<>();
        List<Consequence> consequences = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        int score = 0;

        boolean clausePresente = input.clauseMobilitePresente();
        boolean zonePrecise = input.zoneGeographiquePrecise();
        boolean interetLegitime = input.interetLegitimeEmployeur();
        boolean motifPro = input.motifMutationProfessionnel();
        Integer delai = input.delaiPrevenanceSemaines();
        boolean situationFamiliale = input.situationFamilialeSalarieContraignante();

        // ── 1. DT71_CLAUSE_PRESENTE — existence de la clause de mobilité ─────
        if (clausePresente) {
            points.add(new PointAnalyse(
                    CodeCritere.DT71_CLAUSE_PRESENTE,
                    "Clause de mobilité contractuelle présente",
                    "Cass. soc. 03/06/2003 n°01-40.376 — la clause de mobilité doit être stipulée par écrit dans le contrat de travail ou un avenant signé",
                    "OK — clause de mobilité documentée dans le contrat de travail."));
            bases.add("Cass. soc. 03/06/2003 n°01-40.376 — exigence d'une clause écrite");
        } else {
            points.add(new PointAnalyse(
                    CodeCritere.DT71_CLAUSE_PRESENTE,
                    "Absence de clause de mobilité contractuelle",
                    "Cass. soc. 04/05/1999 n°97-40.576 — sans clause de mobilité, la mutation hors secteur géographique constitue une modification du contrat de travail",
                    "ABSENCE — pas de clause de mobilité opposable au salarié."));
        }

        // ── 2. DT71_ZONE_PRECISE — précision de la zone géographique ─────────
        if (clausePresente) {
            if (zonePrecise) {
                points.add(new PointAnalyse(
                        CodeCritere.DT71_ZONE_PRECISE,
                        "Zone géographique de la clause définie avec précision",
                        "Cass. soc. 07/06/2006 n°04-45.846 ; Cass. soc. 24/01/2008 n°06-45.088 — la zone doit être suffisamment précise (région, départements listés, rayon kilométrique)",
                        "OK — zone géographique opposable au salarié."));
                bases.add("Cass. soc. 07/06/2006 n°04-45.846 — exigence de précision de la zone");
            } else {
                points.add(new PointAnalyse(
                        CodeCritere.DT71_ZONE_PRECISE,
                        "Zone géographique imprécise (« France entière » sans justification, formulation vague)",
                        "Cass. soc. 24/01/2008 n°06-45.088 — « France entière » insuffisant sauf si le secteur d'activité l'impose (commerciaux itinérants, secteur national)",
                        "VICE — la clause est privée d'effet par imprécision de la zone."));
                bases.add("Cass. soc. 24/01/2008 n°06-45.088 — zone « France entière » insuffisante");
            }
        }

        // ── 3. DT71_DELAI_PREVENANCE — délai accordé pour rejoindre le poste ─
        if (clausePresente) {
            if (delai == null) {
                points.add(new PointAnalyse(
                        CodeCritere.DT71_DELAI_PREVENANCE,
                        "Délai de prévenance non documenté",
                        "Cass. soc. 03/03/2010 n°08-44.859 — l'employeur doit accorder un délai de prévenance raisonnable pour permettre au salarié de s'organiser",
                        "À DOCUMENTER — le délai accordé doit apparaître dans la lettre de mutation."));
            } else if (delai < DELAI_PREVENANCE_MIN_SEMAINES) {
                consequences.add(new Consequence(
                        TypeConsequence.DELAI_PREVENANCE_INSUFFISANT,
                        "Délai de prévenance inférieur à " + DELAI_PREVENANCE_MIN_SEMAINES
                                + " semaines — risque de requalification en modification du "
                                + "contrat de travail (Cass. soc. 03/03/2010).",
                        "Cass. soc. 03/03/2010 n°08-44.859 — délai insuffisant = modification"));
                points.add(new PointAnalyse(
                        CodeCritere.DT71_DELAI_PREVENANCE,
                        "Délai de prévenance insuffisant (" + delai + " semaine"
                                + (delai > 1 ? "s" : "") + ")",
                        "Cass. soc. 03/03/2010 n°08-44.859 — un délai trop court vide la clause de sa valeur et caractérise une modification unilatérale du contrat de travail",
                        "VICE — délai inférieur au seuil raisonnable de " + DELAI_PREVENANCE_MIN_SEMAINES + " semaines."));
                bases.add("Cass. soc. 03/03/2010 n°08-44.859 — délai de prévenance raisonnable");
            } else {
                points.add(new PointAnalyse(
                        CodeCritere.DT71_DELAI_PREVENANCE,
                        "Délai de prévenance raisonnable (" + delai + " semaines)",
                        "Cass. soc. 03/03/2010 n°08-44.859 — délai d'organisation suffisant",
                        "OK — délai conforme à l'exigence de bonne foi dans la mise en œuvre."));
            }
        }

        // ── 4. DT71_MOTIF_PROFESSIONNEL — légitimité de la mise en œuvre ─────
        if (clausePresente) {
            if (motifPro && interetLegitime) {
                points.add(new PointAnalyse(
                        CodeCritere.DT71_MOTIF_PROFESSIONNEL,
                        "Motif professionnel objectif + intérêt légitime de l'entreprise",
                        "Cass. soc. 23/02/2005 n°03-42.018 — la mise en œuvre de la clause de mobilité doit reposer sur un intérêt légitime, sans détournement de pouvoir",
                        "OK — motif professionnel documenté."));
                bases.add("Cass. soc. 23/02/2005 n°03-42.018 — intérêt légitime de l'entreprise");
            } else if (!motifPro) {
                points.add(new PointAnalyse(
                        CodeCritere.DT71_MOTIF_PROFESSIONNEL,
                        "Motif non professionnel (mutation personnelle / disciplinaire déguisée)",
                        "Cass. soc. 23/02/2005 n°03-42.018 — détournement de pouvoir caractérisé par un motif non professionnel",
                        "VICE — la mise en œuvre détourne la clause de sa finalité."));
                bases.add("Cass. soc. 23/02/2005 n°03-42.018 — détournement de pouvoir");
            } else {
                points.add(new PointAnalyse(
                        CodeCritere.DT71_MOTIF_PROFESSIONNEL,
                        "Motif professionnel sans intérêt légitime démontré",
                        "Cass. soc. 23/02/2005 n°03-42.018 — l'employeur doit pouvoir démontrer l'intérêt légitime de la mutation",
                        "À DOCUMENTER — l'intérêt légitime doit être étayé par des éléments objectifs."));
            }
        }

        // ── 5. Verdict global ────────────────────────────────────────────────
        AnalyseMutation verdict;
        boolean delaiInsuffisant = clausePresente && delai != null && delai < DELAI_PREVENANCE_MIN_SEMAINES;

        if (!clausePresente) {
            verdict = AnalyseMutation.ABSENCE_CLAUSE;
            score = 10;
            messages.add("Aucune clause de mobilité opposable : la mutation hors secteur "
                    + "géographique constitue une modification du contrat de travail. "
                    + "Le refus du salarié n'est pas fautif (Cass. soc. 04/05/1999 "
                    + "n°97-40.576).");
        } else if (!zonePrecise || delaiInsuffisant || !interetLegitime || !motifPro) {
            verdict = AnalyseMutation.CLAUSE_INVALIDE;
            score = 30;
            if (!zonePrecise) {
                messages.add("Clause invalide — zone géographique insuffisamment précise "
                        + "(Cass. soc. 24/01/2008 n°06-45.088). Le refus du salarié n'est "
                        + "pas fautif.");
            }
            if (delaiInsuffisant) {
                messages.add("Clause mise en œuvre avec un délai de prévenance insuffisant "
                        + "(Cass. soc. 03/03/2010 n°08-44.859) — caractérise une "
                        + "modification du contrat de travail.");
            }
            if (!interetLegitime) {
                messages.add("Mise en œuvre sans intérêt légitime démontré — "
                        + "Cass. soc. 23/02/2005 n°03-42.018.");
            }
            if (!motifPro) {
                messages.add("Motif non professionnel — détournement de pouvoir "
                        + "caractérisé (Cass. soc. 23/02/2005 n°03-42.018).");
            }
        } else {
            verdict = AnalyseMutation.CLAUSE_VALIDE;
            score = 85;
            if (situationFamiliale) {
                score -= 15;
                messages.add("Situation familiale du salarié contraignante — "
                        + "atteinte disproportionnée à la vie personnelle possible "
                        + "(Cass. soc. 14/10/2008 n°07-40.523). À apprécier au cas d'espèce.");
                bases.add("Cass. soc. 14/10/2008 n°07-40.523 — atteinte à la vie personnelle");
            } else {
                messages.add("Clause valide et bien mise en œuvre — le refus du salarié "
                        + "caractérise une faute pouvant fonder un licenciement.");
            }
        }

        // ── 6. Conséquences du refus ─────────────────────────────────────────
        MutationClauseMobiliteInput.ReponseSalarie reponse = input.reponseSalarie();
        String consequencesRefus;

        switch (reponse) {
            case REFUS:
                if (verdict == AnalyseMutation.CLAUSE_VALIDE && !situationFamiliale) {
                    consequences.add(new Consequence(
                            TypeConsequence.REFUS_FAUTE_LICENCIEMENT_POSSIBLE,
                            "Refus du salarié à la mise en œuvre d'une clause de mobilité "
                                    + "valide → caractérise une faute pouvant fonder un "
                                    + "licenciement disciplinaire, voire pour faute grave "
                                    + "selon les circonstances (Cass. soc. 04/01/2000 "
                                    + "n°97-44.566).",
                            "Cass. soc. 04/01/2000 n°97-44.566 — refus de mutation = faute"));
                    bases.add("Cass. soc. 04/01/2000 n°97-44.566 — refus de mutation = faute");
                    consequencesRefus = "Le refus du salarié à la mise en œuvre d'une clause "
                            + "de mobilité valide caractérise une faute pouvant justifier un "
                            + "licenciement disciplinaire.";
                } else if (verdict == AnalyseMutation.CLAUSE_INVALIDE) {
                    consequences.add(new Consequence(
                            TypeConsequence.REFUS_SANS_FAUTE_CLAUSE_INVALIDE,
                            "Refus du salarié face à une clause de mobilité entachée d'un "
                                    + "vice (zone imprécise, délai insuffisant, intérêt non "
                                    + "démontré) → pas de faute. Un licenciement consécutif "
                                    + "serait dépourvu de cause réelle et sérieuse.",
                            "Cass. soc. 24/01/2008 n°06-45.088 ; Cass. soc. 03/03/2010 n°08-44.859"));
                    bases.add("L. 1235-3 CT — licenciement sans cause réelle et sérieuse");
                    consequencesRefus = "La clause étant invalide, le refus du salarié n'est "
                            + "pas fautif. Un licenciement éventuel serait dépourvu de cause "
                            + "réelle et sérieuse.";
                } else if (verdict == AnalyseMutation.ABSENCE_CLAUSE) {
                    consequences.add(new Consequence(
                            TypeConsequence.REFUS_SANS_FAUTE_ABSENCE_CLAUSE,
                            "Refus du salarié face à une mutation hors secteur géographique "
                                    + "sans clause de mobilité → la mutation constitue une "
                                    + "modification du contrat de travail. L'employeur doit "
                                    + "soit renoncer, soit licencier sur un autre motif.",
                            "Cass. soc. 04/05/1999 n°97-40.576 — mobilité sans clause"));
                    consequences.add(new Consequence(
                            TypeConsequence.MODIFICATION_CONTRAT_REQUISE,
                            "Sans clause, la mutation hors secteur géographique requiert "
                                    + "l'accord exprès du salarié (avenant signé). À défaut, "
                                    + "l'employeur ne peut pas imposer la mesure.",
                            "Cass. soc. 04/05/1999 n°97-40.576 ; Cass. soc. constante"));
                    bases.add("Cass. soc. 04/05/1999 n°97-40.576 — modification du contrat");
                    consequencesRefus = "Sans clause de mobilité opposable, le refus du "
                            + "salarié n'est pas fautif. La mutation requiert un avenant "
                            + "signé ; à défaut, l'employeur doit renoncer.";
                } else {
                    // CLAUSE_VALIDE + situation familiale contraignante.
                    consequences.add(new Consequence(
                            TypeConsequence.REFUS_SANS_FAUTE_CLAUSE_INVALIDE,
                            "Refus motivé par une atteinte disproportionnée à la vie "
                                    + "personnelle et familiale → appréciation au cas "
                                    + "d'espèce. La clause peut être paralysée si l'atteinte "
                                    + "est avérée (Cass. soc. 14/10/2008 n°07-40.523).",
                            "Cass. soc. 14/10/2008 n°07-40.523"));
                    consequencesRefus = "Le refus du salarié pourrait être justifié par une "
                            + "atteinte disproportionnée à sa vie personnelle et familiale. "
                            + "Apprécier au cas d'espèce.";
                }
                break;
            case ACCEPTATION:
                consequences.add(new Consequence(
                        TypeConsequence.ACCORD_FORMEL_REQUIS,
                        "Acceptation du salarié — formaliser la mutation par un avenant "
                                + "signé précisant le nouveau lieu de travail, la date "
                                + "d'effet et les éventuelles mesures d'accompagnement.",
                        "Cass. soc. — exigence de formalisation de l'avenant"));
                consequencesRefus = "Acceptation par le salarié — formaliser la mutation par avenant signé.";
                break;
            case EN_ATTENTE:
            default:
                consequences.add(new Consequence(
                        TypeConsequence.ATTENTE_REPONSE_SALARIE,
                        "Réponse du salarié en attente — laisser un délai raisonnable de "
                                + "réflexion avant toute mesure unilatérale.",
                        "Cass. soc. — exigence de bonne foi dans la mise en œuvre"));
                consequencesRefus = "Réponse du salarié en attente — patienter le délai de "
                        + "prévenance avant toute mesure.";
                break;
        }

        // ── 7. Bases juridiques génériques ───────────────────────────────────
        bases.add(0, "Cass. soc. — jurisprudence constante sur la clause de mobilité (validité + mise en œuvre)");
        bases.add("L. 1221-1 CT — formation et exécution du contrat de travail");

        // Borner le score à [0, 100]
        score = Math.max(0, Math.min(100, score));

        return new Result(
                verdict,
                score,
                List.copyOf(points),
                List.copyOf(consequences),
                consequencesRefus,
                List.copyOf(bases),
                List.copyOf(messages),
                "FRANCE"
        );
    }
}
