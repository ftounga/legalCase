package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-212-11 : moteur d'analyse de la modification du contrat refusée par le
 * salarié (FRANCE) — Cass. soc. sur la distinction modification du contrat
 * / changement des conditions de travail ; L. 1222-6 CT — modification pour
 * motif économique.
 *
 * <p>Distingue :
 * <ul>
 *   <li><b>Éléments essentiels du contrat</b> (modification = accord
 *       nécessaire) : rémunération, qualification professionnelle, durée du
 *       travail contractuellement stipulée, lieu de travail si clause
 *       explicite.</li>
 *   <li><b>Conditions de travail</b> (pouvoir de direction, pas d'accord
 *       nécessaire) : horaires dans la plage contractuelle, lieu dans le
 *       même secteur géographique, tâches dans la qualification.</li>
 * </ul>
 *
 * <p>Cas particulier <b>L. 1222-6 CT</b> (modification pour motif économique) :
 * notification écrite par LRAR + délai de réflexion 1 mois (2 mois si PSE).
 * À défaut, le silence du salarié ne vaut pas acceptation.</p>
 *
 * <p><b>Conséquences du refus</b> :
 * <ul>
 *   <li>Modification sans motif éco : l'employeur doit soit renoncer, soit
 *       licencier → si licenciement sans cause réelle et sérieuse si la
 *       modification est illégitime.</li>
 *   <li>Modification pour motif éco (L. 1222-6) : refus dans le délai →
 *       licenciement économique.</li>
 * </ul>
 *
 * <p><b>Pays</b> : FRANCE uniquement — la distinction française "modification
 * du contrat / changement des conditions de travail" est jurisprudentielle.
 * Pas d'équivalent direct en BE (régime de modification de l'élément essentiel
 * et acte équipollent à rupture CCT régime distinct).</p>
 */
public final class ModificationContratRefusCalculator {

    /** Verdict d'analyse de la qualification de la mesure. */
    public enum AnalyseModificationContrat {
        MODIFICATION_CONTRAT,
        CHANGEMENT_CONDITIONS_TRAVAIL,
        INCERTAIN
    }

    /** Code structuré d'un critère analysé (F-IA-03). */
    public enum CodeCritere {
        DT70_ELEMENT_CONTRACTUALISE,
        DT70_MOTIF_ECONOMIQUE,
        DT70_NOTIFICATION_L1222_6,
        DT70_DELAI_REFLEXION
    }

    /** Type de conséquence du refus. */
    public enum TypeConsequence {
        EMPLOYEUR_DOIT_RENONCER_OU_LICENCIER,
        LICENCIEMENT_SANS_CAUSE_REELLE_SERIEUSE_SI_ILLEGITIME,
        LICENCIEMENT_ECONOMIQUE_L1233_3,
        PROCEDURE_L1222_6_NON_RESPECTEE,
        ATTENTE_REPONSE_SALARIE,
        ACCORD_FORMEL_REQUIS,
        POUVOIR_DIRECTION_REFUS_FAUTIF
    }

    /** Conséquence du refus exposée dans la réponse API. */
    public record Consequence(
            TypeConsequence type,
            String description,
            String fondement
    ) {}

    /** Résultat calculé par {@link #compute}. */
    public record Result(
            AnalyseModificationContrat analyseModification,
            int scoreModificationContrat,
            List<Consequence> consequences,
            boolean alerteDelaiReflexion,
            List<String> basesJuridiques,
            List<String> messages,
            String country
    ) {}

    private ModificationContratRefusCalculator() {}

    /**
     * Point d'entrée principal.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace
     * @return résultat calculé
     * @throws IllegalArgumentException si {@code country != "FRANCE"} ou si
     *         {@code input} est null, ou si une valeur de délai est négative.
     */
    public static Result compute(ModificationContratRefusInput input, String country) {
        if (!"FRANCE".equalsIgnoreCase(country)) {
            throw new IllegalArgumentException(
                    "Outil réservé au droit du travail FRANCE — pays détecté : " + country);
        }
        if (input == null) {
            throw new IllegalArgumentException("Données d'entrée manquantes");
        }
        if (input.elementModifie() == null) {
            throw new IllegalArgumentException("Élément modifié requis");
        }
        if (input.reponseSalarie() == null) {
            throw new IllegalArgumentException("Réponse du salarié requise");
        }
        if (input.delaiReflexionRespecteMois() != null && input.delaiReflexionRespecteMois() < 0) {
            throw new IllegalArgumentException(
                    "Le délai de réflexion ne peut pas être négatif");
        }

        List<Consequence> consequences = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        boolean alerteDelaiReflexion = false;
        int score = 0;

        ModificationContratRefusInput.ElementModifie elem = input.elementModifie();
        boolean contractualise = input.elementExplicitementContractualise();
        boolean motifEco = input.motifEconomique();

        // ── 1. Qualification de la mesure : modification ou changement ? ──────
        AnalyseModificationContrat verdict;
        switch (elem) {
            // Éléments essentiels par nature — toujours modification du contrat
            // (Cass. soc. constante).
            case REMUNERATION:
                verdict = AnalyseModificationContrat.MODIFICATION_CONTRAT;
                score = 95;
                messages.add("La rémunération est un élément essentiel du contrat — toute "
                        + "modification (montant, structure, prime contractuelle) nécessite "
                        + "l'accord exprès du salarié (Cass. soc. 28/04/2011 n°09-40.487).");
                bases.add("Cass. soc. 28/04/2011 n°09-40.487 — la rémunération est un élément du contrat");
                break;
            case QUALIFICATION:
                verdict = AnalyseModificationContrat.MODIFICATION_CONTRAT;
                score = 90;
                messages.add("La qualification professionnelle est un élément essentiel du "
                        + "contrat — son changement implique l'accord du salarié, sauf "
                        + "promotion (Cass. soc. 10/07/1996 n°93-41.137).");
                bases.add("Cass. soc. 10/07/1996 n°93-41.137 — qualification professionnelle = élément essentiel");
                break;
            case DUREE_TRAVAIL:
                // La durée du travail contractuellement stipulée est essentielle
                // (Cass. soc. 20/10/1998 n°96-40.614 et constants).
                if (contractualise) {
                    verdict = AnalyseModificationContrat.MODIFICATION_CONTRAT;
                    score = 90;
                    messages.add("La durée du travail contractuellement stipulée est un "
                            + "élément essentiel — sa modification requiert l'accord du "
                            + "salarié (Cass. soc. 20/10/1998 n°96-40.614).");
                    bases.add("Cass. soc. 20/10/1998 n°96-40.614 — durée du travail contractualisée = élément essentiel");
                } else {
                    verdict = AnalyseModificationContrat.INCERTAIN;
                    score = 40;
                    messages.add("Durée du travail non explicitement contractualisée — "
                            + "qualification incertaine, à apprécier au regard du contrat "
                            + "et de la convention collective applicable.");
                }
                break;
            case LIEU_TRAVAIL:
                // Distinction clause de mobilité / secteur géographique.
                if (contractualise) {
                    verdict = AnalyseModificationContrat.MODIFICATION_CONTRAT;
                    score = 75;
                    messages.add("Le lieu de travail explicitement contractualisé (par clause "
                            + "claire et précise) constitue un élément du contrat — sa "
                            + "modification nécessite l'accord du salarié (Cass. soc. "
                            + "03/06/2003 n°01-40.376).");
                    bases.add("Cass. soc. 03/06/2003 n°01-40.376 — lieu de travail contractualisé");
                } else {
                    verdict = AnalyseModificationContrat.CHANGEMENT_CONDITIONS_TRAVAIL;
                    score = 25;
                    messages.add("Le lieu de travail non contractualisé peut être modifié "
                            + "dans le même secteur géographique sans accord du salarié "
                            + "(Cass. soc. 04/05/1999 n°97-40.576).");
                    bases.add("Cass. soc. 04/05/1999 n°97-40.576 — mobilité géographique sans clause");
                }
                break;
            case HORAIRES:
                // Horaires : changement de conditions de travail par principe,
                // sauf bouleversement de l'économie du contrat
                // (Cass. soc. 22/02/2000 n°97-44.339).
                if (contractualise) {
                    verdict = AnalyseModificationContrat.MODIFICATION_CONTRAT;
                    score = 70;
                    messages.add("Les horaires explicitement contractualisés ou impliquant un "
                            + "bouleversement de l'économie du contrat (jour ↔ nuit, "
                            + "passage en horaires variables) constituent une modification "
                            + "du contrat (Cass. soc. 22/02/2000 n°97-44.339).");
                    bases.add("Cass. soc. 22/02/2000 n°97-44.339 — bouleversement horaires");
                } else {
                    verdict = AnalyseModificationContrat.CHANGEMENT_CONDITIONS_TRAVAIL;
                    score = 20;
                    messages.add("Les horaires dans la plage contractuelle relèvent du pouvoir "
                            + "de direction et ne constituent pas une modification du contrat "
                            + "(Cass. soc. 22/02/2000 n°97-44.339).");
                    bases.add("Cass. soc. 22/02/2000 n°97-44.339 — pouvoir de direction sur horaires");
                }
                break;
            case TACHES:
                // Tâches dans la qualification = pouvoir de direction.
                verdict = AnalyseModificationContrat.CHANGEMENT_CONDITIONS_TRAVAIL;
                score = 15;
                messages.add("La modification des tâches dans le cadre de la qualification "
                        + "du salarié relève du pouvoir de direction de l'employeur et ne "
                        + "constitue pas une modification du contrat de travail.");
                bases.add("Cass. soc. — modification des tâches dans la qualification");
                break;
            case AUTRE:
            default:
                verdict = AnalyseModificationContrat.INCERTAIN;
                score = 35;
                messages.add("Élément non standard — la qualification (modification du contrat "
                        + "/ changement des conditions) doit être appréciée au cas d'espèce "
                        + "selon que l'élément est essentiel ou accessoire au sens de la "
                        + "jurisprudence (Cass. soc. constante).");
                break;
        }

        // ── 2. Cas particulier L. 1222-6 — modification pour motif économique ─
        if (motifEco) {
            bases.add("L. 1222-6 CT — modification du contrat pour motif économique");

            Boolean notif = input.notificationEcriteL1222_6();
            Integer delaiMois = input.delaiReflexionRespecteMois();

            if (Boolean.FALSE.equals(notif)) {
                // AC3 : notification absente alors que motif éco => alerte.
                alerteDelaiReflexion = true;
                messages.add("ALERTE : modification pour motif économique sans notification "
                        + "écrite L. 1222-6 — le silence du salarié ne vaut PAS acceptation "
                        + "(L. 1222-6 al. 2 CT). La procédure L. 1222-6 n'a pas été respectée.");
                consequences.add(new Consequence(
                        TypeConsequence.PROCEDURE_L1222_6_NON_RESPECTEE,
                        "L'absence de notification écrite par LRAR vide la procédure de sa "
                                + "valeur. Le silence du salarié ne peut pas être interprété "
                                + "comme une acceptation.",
                        "L. 1222-6 CT — notification écrite par LRAR obligatoire"));
            } else if (Boolean.TRUE.equals(notif)) {
                // Délai de réflexion : 1 mois standard, 2 mois si PSE.
                if (delaiMois != null && delaiMois < 1) {
                    alerteDelaiReflexion = true;
                    messages.add("ALERTE : délai de réflexion inférieur au minimum légal "
                            + "(1 mois standard L. 1222-6, 2 mois en cas de PSE).");
                    consequences.add(new Consequence(
                            TypeConsequence.PROCEDURE_L1222_6_NON_RESPECTEE,
                            "Le délai de réflexion accordé est inférieur au minimum légal de "
                                    + "1 mois (2 mois si PSE).",
                            "L. 1222-6 al. 2 CT — délai de réflexion 1 mois (2 mois si PSE)"));
                } else {
                    messages.add("Procédure L. 1222-6 respectée : notification écrite + "
                            + "délai de réflexion conforme (1 mois standard, 2 mois si PSE).");
                }
            } else {
                // Notification non documentée => alerte sécurité.
                alerteDelaiReflexion = true;
                messages.add("ATTENTION : la notification L. 1222-6 (LRAR) n'est pas "
                        + "documentée — vérifier la régularité de la procédure avant tout "
                        + "constat d'acceptation tacite (L. 1222-6 al. 2 CT).");
            }
        }

        // ── 3. Conséquences du refus du salarié ──────────────────────────────
        ModificationContratRefusInput.ReponseSalarie reponse = input.reponseSalarie();

        switch (reponse) {
            case REFUS:
                if (verdict == AnalyseModificationContrat.MODIFICATION_CONTRAT) {
                    if (motifEco) {
                        consequences.add(new Consequence(
                                TypeConsequence.LICENCIEMENT_ECONOMIQUE_L1233_3,
                                "Refus du salarié à une modification pour motif économique → "
                                        + "ouvre la voie au licenciement économique. La cause "
                                        + "réelle et sérieuse réside dans le motif économique, "
                                        + "non dans le refus lui-même.",
                                "L. 1222-6 CT ; L. 1233-3 CT — licenciement économique"));
                        bases.add("L. 1233-3 CT — définition du licenciement économique");
                        messages.add("Refus + motif économique → licenciement économique "
                                + "possible (procédure L. 1233-3+ à respecter).");
                    } else {
                        consequences.add(new Consequence(
                                TypeConsequence.EMPLOYEUR_DOIT_RENONCER_OU_LICENCIER,
                                "Modification du contrat sans motif économique : face au refus "
                                        + "du salarié, l'employeur doit soit renoncer à la "
                                        + "modification, soit prononcer un licenciement.",
                                "Cass. soc. constante — modification du contrat refusée"));
                        consequences.add(new Consequence(
                                TypeConsequence.LICENCIEMENT_SANS_CAUSE_REELLE_SERIEUSE_SI_ILLEGITIME,
                                "Si la modification est jugée illégitime (sans cause réelle "
                                        + "et sérieuse autre que le refus lui-même), le "
                                        + "licenciement sera dépourvu de cause réelle et "
                                        + "sérieuse (Cass. soc. 16/06/1998 n°96-40.881).",
                                "L. 1232-1 CT ; Cass. soc. 16/06/1998 n°96-40.881"));
                        bases.add("Cass. soc. 16/06/1998 n°96-40.881 — refus de modification = pas de cause");
                        messages.add("Refus + pas de motif économique → l'employeur doit "
                                + "renoncer ou licencier (avec risque de licenciement sans "
                                + "cause réelle et sérieuse).");
                    }
                } else if (verdict == AnalyseModificationContrat.CHANGEMENT_CONDITIONS_TRAVAIL) {
                    consequences.add(new Consequence(
                            TypeConsequence.POUVOIR_DIRECTION_REFUS_FAUTIF,
                            "Le refus d'un simple changement de conditions de travail (pouvoir "
                                    + "de direction) constitue un acte d'insubordination "
                                    + "susceptible de fonder un licenciement disciplinaire, "
                                    + "voire pour faute grave selon les circonstances.",
                            "Cass. soc. 10/07/1996 — refus de changement = insubordination"));
                    bases.add("Cass. soc. 10/07/1996 — refus changement conditions = faute");
                    messages.add("Refus + changement des conditions → risque de licenciement "
                            + "disciplinaire pour insubordination.");
                } else {
                    consequences.add(new Consequence(
                            TypeConsequence.ACCORD_FORMEL_REQUIS,
                            "La qualification de la mesure étant incertaine, sécuriser un "
                                    + "accord exprès du salarié avant toute mise en œuvre "
                                    + "(avenant signé) reste la voie la plus sûre.",
                            "Principe — sécurisation contractuelle"));
                    messages.add("Qualification incertaine — sécuriser un accord exprès "
                            + "avant toute mise en œuvre.");
                }
                break;
            case ACCEPTATION:
                consequences.add(new Consequence(
                        TypeConsequence.ACCORD_FORMEL_REQUIS,
                        "Acceptation du salarié : formaliser l'accord par avenant signé, "
                                + "daté, comportant les nouveaux termes (rémunération, "
                                + "horaires, lieu, qualification). L'acceptation tacite est "
                                + "à éviter (Cass. soc. 30/10/1991 n°88-44.038).",
                        "Cass. soc. 30/10/1991 n°88-44.038 — exigence d'un accord exprès"));
                bases.add("Cass. soc. 30/10/1991 n°88-44.038 — accord exprès du salarié");
                messages.add("Acceptation → formaliser un avenant signé.");
                break;
            case EN_ATTENTE:
            default:
                consequences.add(new Consequence(
                        TypeConsequence.ATTENTE_REPONSE_SALARIE,
                        "Réponse du salarié en attente — si motif économique, le silence ne "
                                + "vaut pas acceptation tant que la procédure L. 1222-6 n'a "
                                + "pas été respectée. Sans motif économique, le silence ne "
                                + "vaut pas non plus acceptation (Cass. soc. constante).",
                        motifEco ? "L. 1222-6 al. 2 CT" : "Cass. soc. — silence du salarié"));
                messages.add("Attendre la réponse du salarié — le silence ne vaut pas "
                        + "acceptation.");
                break;
        }

        // ── 4. Bases juridiques génériques ───────────────────────────────────
        bases.add(0, "Cass. soc. — distinction modification du contrat / changement des conditions de travail");
        if (motifEco) {
            bases.add("L. 1222-6 al. 2 CT — silence du salarié ne vaut pas acceptation");
        }

        // Borner le score à [0, 100]
        score = Math.max(0, Math.min(100, score));

        return new Result(
                verdict,
                score,
                List.copyOf(consequences),
                alerteDelaiReflexion,
                List.copyOf(bases),
                List.copyOf(messages),
                "FRANCE"
        );
    }
}
