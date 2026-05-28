package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-219-18 : vérificateur de conformité d'une demande / mise en
 * place d'une <i>semaine de 4 jours BE</i> (Loi du 03/10/2022
 * « Deal pour l'emploi » M.B. 10/11/2022, art. 5) — fonction
 * <b>pure</b> (sans dépendance HTTP / persistance / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 03/10/2022</b> portant des dispositions diverses
 *       en matière de travail dite « Deal pour l'emploi » (M.B.
 *       10/11/2022), <b>art. 5</b> — semaine de 4 jours à la
 *       demande du travailleur à temps plein : compression de la
 *       durée hebdomadaire de travail (38 à 40 h) sur 4 jours sans
 *       réduction. Journée plafonnée à 9 h 30 (10 h si CCT
 *       sectorielle), accord employeur formalisé par annexe écrite
 *       au contrat de travail (durée max 6 mois renouvelables),
 *       refus motivé par écrit dans le mois, protection contre le
 *       licenciement motivé par la demande (indemnité = 6 mois de
 *       rémunération).</li>
 *   <li><b>Loi du 03/10/2022, art. 6</b> — procédure simplifiée
 *       de modification du règlement de travail pour inscrire
 *       l'horaire compressé (Loi 08/04/1965 sur le règlement de
 *       travail, art. 12 — pas de procédure standard requise).</li>
 *   <li><b>Loi du 16/03/1971</b> sur le travail (M.B. 30/03/1971),
 *       art. 19 — limites quotidienne (9 h) et hebdomadaire (40 h)
 *       de droit commun ; l'art. 5 § 3 Loi 03/10/2022 constitue
 *       une dérogation conditionnelle.</li>
 *   <li><b>Loi du 03/07/1978</b> sur les contrats de travail (M.B.
 *       22/08/1978), art. 25 — modification d'un élément essentiel
 *       du contrat (ici l'horaire) requiert l'accord du
 *       travailleur ; l'avenant écrit (art. 5 § 4 Loi 03/10/2022)
 *       formalise cet accord.</li>
 * </ul>
 *
 * <h2>Hiérarchie des verdicts (court-circuits successifs)</h2>
 * <ol>
 *   <li><b>A_ANALYSER</b> — statut {@link
 *       Semaine4JoursBeStatutDemande#INDETERMINE}.</li>
 *   <li><b>LICENCIEMENT_REPRESAILLES_PRESUME</b> — statut
 *       {@code LICENCIE_APRES_DEMANDE} sans motif objectif établi.</li>
 *   <li><b>REFUS_EMPLOYEUR_NON_MOTIVE</b> — statut
 *       {@code REFUSE_SANS_MOTIVATION_ECRITE} ou
 *       {@code REFUSE_MOTIVE_PAR_ECRIT} sans motivation écrite.</li>
 *   <li><b>NON_ELIGIBLE_TEMPS_PARTIEL</b> — travailleur à temps
 *       partiel.</li>
 *   <li><b>NON_CONFORME_DEMANDE_ECRITE_MANQUANTE</b> — pas de
 *       demande écrite du travailleur.</li>
 *   <li><b>NON_CONFORME_JOURNEE_DEPASSE_9H30</b> — journée
 *       quotidienne &gt; 9 h 30 sans CCT 10 h.</li>
 *   <li><b>NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT</b> — avenant
 *       contrat ou règlement de travail non formalisé.</li>
 *   <li><b>NON_CONFORME_DUREE_DEPASSE_6_MOIS</b> — avenant &gt; 6
 *       mois sans renouvellement.</li>
 *   <li>Si statut {@code EN_ATTENTE_REPONSE_EMPLOYEUR} → renvoie
 *       {@code A_ANALYSER} avec avertissement délai 1 mois.</li>
 *   <li>Sinon (statut {@code ACCORDE_AVENANT_SIGNE}) →
 *       <b>CONFORME_REGIME_4_JOURS_VALIDE</b>.</li>
 * </ol>
 */
public final class Semaine4JoursBeChecker {

    /** Plafond journalier par défaut (art. 5 § 3 Loi 03/10/2022). */
    static final BigDecimal PLAFOND_JOURNEE_DEFAUT = new BigDecimal("9.50");

    /** Plafond journalier maximal autorisé par CCT sectorielle. */
    static final BigDecimal PLAFOND_JOURNEE_CCT_10H = new BigDecimal("10.00");

    /** Durée maximale d'un avenant non renouvelé (mois). */
    static final BigDecimal DUREE_MAX_AVENANT_MOIS = new BigDecimal("6");

    static final String BASE_JURIDIQUE =
            "Loi du 03/10/2022 portant des dispositions diverses en"
                    + " matière de travail dite « Deal pour l'emploi »"
                    + " (M.B. 10/11/2022), art. 5 (semaine de 4 jours à"
                    + " la demande du travailleur à temps plein —"
                    + " compression de la durée hebdomadaire 38 à 40 h"
                    + " sur 4 jours, journée plafonnée à 9 h 30 ou 10 h"
                    + " par CCT sectorielle, accord formalisé par annexe"
                    + " écrite au contrat de travail durée max 6 mois"
                    + " renouvelables, refus motivé écrit dans le mois,"
                    + " protection contre le licenciement) + art. 6"
                    + " (procédure simplifiée de modification du"
                    + " règlement de travail) ; Loi du 16/03/1971 sur"
                    + " le travail (M.B. 30/03/1971), art. 19 (limites"
                    + " quotidienne 9 h et hebdomadaire 40 h de droit"
                    + " commun — l'art. 5 § 3 Loi 03/10/2022 constitue"
                    + " une dérogation conditionnelle) ; Loi du 03/07/1978"
                    + " sur les contrats de travail, art. 25 (modification"
                    + " d'un élément essentiel du contrat — l'horaire —"
                    + " requiert l'accord du travailleur, formalisé par"
                    + " l'avenant art. 5 § 4 Loi 03/10/2022) ; Loi du"
                    + " 08/04/1965 instituant les règlements de travail,"
                    + " art. 12 (inscription des horaires applicables —"
                    + " procédure simplifiée par renvoi art. 6 Loi"
                    + " 03/10/2022).";

    static final String AVERT_AVOCAT_BE =
            "Le « Deal pour l'emploi » (Loi 03/10/2022) est récent et"
                    + " la jurisprudence post-2023 reste limitée. L'avocat"
                    + " spécialisé en droit social BE doit confirmer pour"
                    + " la situation considérée : (i) le décompte exact"
                    + " du délai d'1 mois pour la réponse employeur et"
                    + " l'effet du silence (silence ≠ accord tacite mais"
                    + " expose à l'action en abus de droit) ; (ii)"
                    + " l'existence d'une CCT sectorielle portant la"
                    + " journée à 10 h et ses conditions d'application ;"
                    + " (iii) le contenu minimum de l'avenant écrit"
                    + " (durée, horaire concret, jours travaillés,"
                    + " modalités de fin anticipée) ; (iv) la procédure"
                    + " de modification du règlement de travail si la"
                    + " demande est massive (≥ X travailleurs) — la"
                    + " procédure simplifiée art. 6 ne dispense pas de"
                    + " la consultation du conseil d'entreprise / CPPT"
                    + " si l'horaire concerne plusieurs travailleurs ;"
                    + " (v) le quantum exact de l'indemnité de protection"
                    + " en cas de licenciement présumé représailles"
                    + " (6 mois de rémunération de base par défaut, calcul"
                    + " et plafond à confirmer selon la rémunération"
                    + " réelle, ancienneté et CCT applicable) ; (vi)"
                    + " l'articulation avec le télétravail structurel"
                    + " CCT n° 85 et l'horaire flottant Loi 17/03/1987"
                    + " si plusieurs régimes coexistent dans l'entreprise.";

    private Semaine4JoursBeChecker() {
    }

    public static Semaine4JoursBeResult analyze(
            Semaine4JoursBeRequest request) {
        validateInputs(request);

        // Calculs dérivés communs
        BigDecimal journeeMaximaleAutorisee = request.cctAutorise10h()
                ? PLAFOND_JOURNEE_CCT_10H : PLAFOND_JOURNEE_DEFAUT;
        boolean eligibiliteRespectee = request.travailleurTempsPlein();
        boolean demandeRespectee = request.demandeEcriteTravailleur();
        boolean journeeRespectee = request.journeeMaximaleHeures()
                .compareTo(journeeMaximaleAutorisee) <= 0;
        boolean formalisationRespectee = request.avenantEcritSigne()
                && request.reglementTravailModifie();
        boolean dureeRespectee = request.dureeAvenantMois()
                .compareTo(DUREE_MAX_AVENANT_MOIS) <= 0
                || request.avenantRenouvele();

        Semaine4JoursBeStatutDemande statut = request.statutDemande();

        // ── Hiérarchie 1 : statut indéterminé ────────────────────────────────
        if (statut == Semaine4JoursBeStatutDemande.INDETERMINE) {
            String synthese = "Statut de la demande de semaine de 4 jours"
                    + " indéterminé : pas encore d'instruction sur"
                    + " l'éventuel dépôt d'une demande écrite, l'accord"
                    + " ou le refus de l'employeur, ou un licenciement"
                    + " subséquent. Une analyse au cas par cas est"
                    + " requise pour qualifier la situation au regard"
                    + " de la Loi du 03/10/2022 (art. 5).";
            return build(request, Semaine4JoursBeVerdict.A_ANALYSER,
                    eligibiliteRespectee, demandeRespectee, journeeRespectee,
                    formalisationRespectee, dureeRespectee,
                    journeeMaximaleAutorisee,
                    "STATUT_DEMANDE_INDETERMINE",
                    synthese);
        }

        // ── Hiérarchie 2 : licenciement après demande ───────────────────────
        if (statut == Semaine4JoursBeStatutDemande.LICENCIE_APRES_DEMANDE) {
            if (request.motifLicenciementObjectifEtabli()) {
                String synthese = "Le travailleur a été licencié après"
                        + " avoir demandé le bénéfice de la semaine de"
                        + " 4 jours mais l'employeur établit un motif"
                        + " objectif sans rapport avec la demande"
                        + " (art. 5 § 6 Loi 03/10/2022). La présomption"
                        + " de représailles est renversée, mais l'avocat"
                        + " doit vérifier le caractère réel et sérieux"
                        + " du motif allégué (la jurisprudence applique"
                        + " la même rigueur que pour les protections"
                        + " spéciales — Cass. constante).";
                return build(request,
                        Semaine4JoursBeVerdict.A_ANALYSER,
                        eligibiliteRespectee, demandeRespectee, journeeRespectee,
                        formalisationRespectee, dureeRespectee,
                        journeeMaximaleAutorisee,
                        "LICENCIEMENT_MOTIF_OBJECTIF_A_VERIFIER",
                        synthese);
            }
            String synthese = "Le travailleur a été licencié après avoir"
                    + " demandé le bénéfice de la semaine de 4 jours et"
                    + " l'employeur n'établit pas de motif objectif sans"
                    + " rapport avec la demande. L'art. 5 § 6 de la Loi"
                    + " 03/10/2022 organise une protection : la charge"
                    + " de la preuve d'un motif étranger à la demande"
                    + " pèse sur l'employeur, à défaut une indemnité"
                    + " forfaitaire de protection de 6 mois de"
                    + " rémunération est due, sans préjudice de"
                    + " l'indemnité de préavis de droit commun.";
            return build(request,
                    Semaine4JoursBeVerdict.LICENCIEMENT_REPRESAILLES_PRESUME,
                    eligibiliteRespectee, demandeRespectee, journeeRespectee,
                    formalisationRespectee, dureeRespectee,
                    journeeMaximaleAutorisee,
                    "LICENCIEMENT_REPRESAILLES_PRESUME",
                    synthese);
        }

        // ── Hiérarchie 3 : refus employeur non motivé ───────────────────────
        if (statut == Semaine4JoursBeStatutDemande.REFUSE_SANS_MOTIVATION_ECRITE
                || (statut == Semaine4JoursBeStatutDemande.REFUSE_MOTIVE_PAR_ECRIT
                        && !request.refusMotiveParEcrit())) {
            String synthese = "L'employeur a refusé la demande sans"
                    + " motivation écrite ou hors du délai d'1 mois"
                    + " (art. 5 § 5 Loi 03/10/2022). Le silence ne"
                    + " vaut pas accord tacite, mais l'absence de"
                    + " motivation expose l'employeur à l'action en"
                    + " abus de droit (art. 1134 al. 3 anc. C.civ. /"
                    + " art. 5.73 nouveau C.civ. — exécution de bonne"
                    + " foi). L'avocat évalue l'opportunité d'un recours"
                    + " (tribunal du travail) et les chances d'obtenir"
                    + " la régularisation ou des dommages-intérêts.";
            return build(request,
                    Semaine4JoursBeVerdict.REFUS_EMPLOYEUR_NON_MOTIVE,
                    eligibiliteRespectee, demandeRespectee, journeeRespectee,
                    formalisationRespectee, dureeRespectee,
                    journeeMaximaleAutorisee,
                    "REFUS_EMPLOYEUR_NON_MOTIVE",
                    synthese);
        }

        // Si refus régulièrement motivé, refus opposable : on s'arrête ici
        // (régime non en place, pas de pathologie supplémentaire à signaler).
        if (statut == Semaine4JoursBeStatutDemande.REFUSE_MOTIVE_PAR_ECRIT) {
            String synthese = "Refus de l'employeur régulièrement motivé"
                    + " par écrit dans le délai d'1 mois (art. 5 § 5 Loi"
                    + " 03/10/2022). Refus juridiquement opposable : la"
                    + " semaine de 4 jours n'est pas mise en place. Le"
                    + " travailleur peut redéposer une demande après"
                    + " évolution de sa situation ou de l'organisation"
                    + " de l'entreprise.";
            return build(request,
                    Semaine4JoursBeVerdict.A_ANALYSER,
                    eligibiliteRespectee, demandeRespectee, journeeRespectee,
                    formalisationRespectee, dureeRespectee,
                    journeeMaximaleAutorisee,
                    "REFUS_EMPLOYEUR_MOTIVE_OPPOSABLE",
                    synthese);
        }

        // ── Hiérarchie 3 bis : en attente réponse employeur ─────────────────
        // (la formalisation / durée n'est pas pertinente tant que
        // l'employeur n'a pas répondu — on signale uniquement les
        // anomalies d'éligibilité ou de demande déjà observables)
        if (statut == Semaine4JoursBeStatutDemande.EN_ATTENTE_REPONSE_EMPLOYEUR
                && eligibiliteRespectee && demandeRespectee) {
            String syntheseAttente = "Demande de semaine de 4 jours déposée le "
                    + request.dateDemande() + ", l'employeur dispose"
                    + " d'1 mois pour notifier sa réponse motivée par"
                    + " écrit (art. 5 § 5 Loi 03/10/2022). Le silence"
                    + " ne vaut pas accord tacite — à l'expiration du"
                    + " délai, l'absence de réponse expose l'employeur"
                    + " à l'action en abus de droit. À reévaluer dès"
                    + " réception de la réponse.";
            return build(request,
                    Semaine4JoursBeVerdict.A_ANALYSER,
                    eligibiliteRespectee, demandeRespectee, journeeRespectee,
                    formalisationRespectee, dureeRespectee,
                    journeeMaximaleAutorisee,
                    "EN_ATTENTE_REPONSE_EMPLOYEUR",
                    syntheseAttente);
        }

        // ── Hiérarchie 4 : non éligibilité (temps partiel) ──────────────────
        if (!eligibiliteRespectee) {
            String synthese = "La semaine de 4 jours instaurée par"
                    + " l'art. 5 Loi 03/10/2022 est réservée aux"
                    + " travailleurs à temps plein. Le travailleur"
                    + " étant à temps partiel, le régime n'est pas"
                    + " ouvert (art. 5 § 1). Des aménagements"
                    + " conventionnels d'horaire restent possibles"
                    + " (modification de l'avenant temps partiel,"
                    + " horaires flottants Loi 17/03/1987) mais hors"
                    + " du cadre du « Deal pour l'emploi ».";
            return build(request,
                    Semaine4JoursBeVerdict.NON_ELIGIBLE_TEMPS_PARTIEL,
                    false, demandeRespectee, journeeRespectee,
                    formalisationRespectee, dureeRespectee,
                    journeeMaximaleAutorisee,
                    "TRAVAILLEUR_TEMPS_PARTIEL",
                    synthese);
        }

        // ── Hiérarchie 5 : demande écrite manquante ─────────────────────────
        if (!demandeRespectee) {
            String synthese = "L'art. 5 § 2 Loi 03/10/2022 impose une"
                    + " demande <i>écrite</i> du travailleur (lettre"
                    + " recommandée ou remise en main propre contre"
                    + " accusé de réception) pour ouvrir le régime de"
                    + " la semaine de 4 jours. À défaut, la mise en"
                    + " place est irrégulière et peut être requalifiée"
                    + " en horaire unilatéralement imposé par"
                    + " l'employeur — modification d'un élément essentiel"
                    + " du contrat (art. 25 Loi 03/07/1978) ouvrant"
                    + " droit à dommages-intérêts.";
            return build(request,
                    Semaine4JoursBeVerdict.NON_CONFORME_DEMANDE_ECRITE_MANQUANTE,
                    eligibiliteRespectee, false, journeeRespectee,
                    formalisationRespectee, dureeRespectee,
                    journeeMaximaleAutorisee,
                    "DEMANDE_ECRITE_MANQUANTE",
                    synthese);
        }

        // ── Hiérarchie 6 : journée > plafond autorisé ───────────────────────
        if (!journeeRespectee) {
            String synthese = "La journée de travail (" + request
                    .journeeMaximaleHeures() + " h) excède le plafond"
                    + " autorisé (" + journeeMaximaleAutorisee + " h —"
                    + (request.cctAutorise10h() ? " CCT sectorielle"
                            + " autorisant 10 h" : " art. 5 § 3 Loi"
                                    + " 03/10/2022 à défaut de CCT")
                    + "). Violation des limites quotidiennes : sursalaire"
                    + " pour les heures effectuées au-delà du plafond,"
                    + " action SPF Emploi Travail et Concertation Sociale"
                    + " possible, et nullité de l'avenant pour"
                    + " contravention à une disposition d'ordre public.";
            return build(request,
                    Semaine4JoursBeVerdict.NON_CONFORME_JOURNEE_DEPASSE_9H30,
                    eligibiliteRespectee, demandeRespectee, false,
                    formalisationRespectee, dureeRespectee,
                    journeeMaximaleAutorisee,
                    "JOURNEE_DEPASSE_PLAFOND",
                    synthese);
        }

        // ── Hiérarchie 7 : avenant ou règlement manquant ────────────────────
        if (!formalisationRespectee) {
            String synthese = "L'art. 5 § 4 Loi 03/10/2022 impose une"
                    + " annexe écrite au contrat de travail (avenant"
                    + " signé) ET l'art. 6 prévoit l'inscription de"
                    + " l'horaire compressé au règlement de travail."
                    + " L'absence de l'un ou l'autre rend la mise en"
                    + " place irrégulière. L'avenant est requis pour"
                    + " formaliser l'accord (art. 25 Loi 03/07/1978),"
                    + " le règlement de travail pour rendre l'horaire"
                    + " opposable aux tiers (Loi 08/04/1965, art. 12).";
            return build(request,
                    Semaine4JoursBeVerdict.NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT,
                    eligibiliteRespectee, demandeRespectee, journeeRespectee,
                    false, dureeRespectee,
                    journeeMaximaleAutorisee,
                    "AVENANT_OU_REGLEMENT_MANQUANT",
                    synthese);
        }

        // ── Hiérarchie 8 : durée > 6 mois sans renouvellement ───────────────
        if (!dureeRespectee) {
            String synthese = "La durée de l'avenant (" + request
                    .dureeAvenantMois() + " mois) excède le maximum de"
                    + " 6 mois prévu par l'art. 5 § 4 Loi 03/10/2022 et"
                    + " l'avenant n'a pas été explicitement renouvelé."
                    + " Fragilité contractuelle : à l'échéance des 6"
                    + " mois, l'employeur peut imposer le retour à"
                    + " l'horaire de droit commun sans préavis"
                    + " supplémentaire. L'avocat invite à formaliser"
                    + " le renouvellement (nouvel avenant) avant"
                    + " l'expiration pour éviter une rupture de fait"
                    + " du régime.";
            return build(request,
                    Semaine4JoursBeVerdict.NON_CONFORME_DUREE_DEPASSE_6_MOIS,
                    eligibiliteRespectee, demandeRespectee, journeeRespectee,
                    formalisationRespectee, false,
                    journeeMaximaleAutorisee,
                    "DUREE_DEPASSE_6_MOIS",
                    synthese);
        }

        // ── Cas qualifiant : régime valide (accord avenant signé) ───────────
        // Si on arrive ici en EN_ATTENTE_REPONSE_EMPLOYEUR, c'est qu'on
        // avait été aiguillé sur une anomalie d'éligibilité ou de demande
        // (gérée plus haut). Le seul statut qui sort « CONFORME » est
        // ACCORDE_AVENANT_SIGNE — pour les autres on renvoie A_ANALYSER
        // par sécurité (garde-fou logique).
        if (statut != Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE) {
            String syntheseGarde = "Configuration ambigüe (statut "
                    + statut + ") n'aboutissant à aucune des règles"
                    + " principales — analyse cas par cas requise.";
            return build(request,
                    Semaine4JoursBeVerdict.A_ANALYSER,
                    eligibiliteRespectee, demandeRespectee, journeeRespectee,
                    formalisationRespectee, dureeRespectee,
                    journeeMaximaleAutorisee,
                    "STATUT_NON_CONFORMANT",
                    syntheseGarde);
        }
        String synthese = "Semaine de 4 jours valablement mise en place"
                + " au titre de l'art. 5 Loi 03/10/2022 : travailleur à"
                + " temps plein, demande écrite, accord employeur"
                + " formalisé par avenant et inscription au règlement"
                + " de travail, journée ≤ " + journeeMaximaleAutorisee
                + " h, durée de l'avenant ≤ 6 mois"
                + (request.avenantRenouvele() ? " (renouvelable)" : "")
                + ". Le travailleur bénéficie en outre de la protection"
                + " contre le licenciement motivé par la demande"
                + " (art. 5 § 6) — indemnité forfaitaire de 6 mois de"
                + " rémunération à charge de l'employeur en cas de"
                + " licenciement représailles.";
        return build(request,
                Semaine4JoursBeVerdict.CONFORME_REGIME_4_JOURS_VALIDE,
                true, true, true, true, true,
                journeeMaximaleAutorisee,
                "CONFORME_REGIME_4_JOURS_VALIDE",
                synthese);
    }

    private static Semaine4JoursBeResult build(
            Semaine4JoursBeRequest request,
            Semaine4JoursBeVerdict verdict,
            boolean eligibiliteRespectee,
            boolean demandeRespectee,
            boolean journeeRespectee,
            boolean formalisationRespectee,
            boolean dureeRespectee,
            BigDecimal journeeMaximaleAutorisee,
            String raison,
            String synthese) {
        return new Semaine4JoursBeResult(
                request.statutDemande(),
                request.travailleurTempsPlein(),
                request.demandeEcriteTravailleur(),
                request.dateDemande(),
                request.dureeHebdomadaireHeures(),
                request.journeeMaximaleHeures(),
                request.cctAutorise10h(),
                request.avenantEcritSigne(),
                request.reglementTravailModifie(),
                request.dureeAvenantMois(),
                request.avenantRenouvele(),
                request.refusMotiveParEcrit(),
                request.dateLicenciement(),
                request.motifLicenciementObjectifEtabli(),
                verdict,
                eligibiliteRespectee,
                demandeRespectee,
                journeeRespectee,
                formalisationRespectee,
                dureeRespectee,
                journeeMaximaleAutorisee,
                raison,
                synthese,
                BASE_JURIDIQUE,
                AVERT_AVOCAT_BE);
    }

    private static void validateInputs(Semaine4JoursBeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.statutDemande() == null) {
            throw new IllegalArgumentException("statutDemande est requis");
        }
        if (request.travailleurTempsPlein() == null) {
            throw new IllegalArgumentException(
                    "travailleurTempsPlein est requis");
        }
        if (request.demandeEcriteTravailleur() == null) {
            throw new IllegalArgumentException(
                    "demandeEcriteTravailleur est requise");
        }
        if (request.dateDemande() == null) {
            throw new IllegalArgumentException("dateDemande est requise");
        }
        if (request.dureeHebdomadaireHeures() == null) {
            throw new IllegalArgumentException(
                    "dureeHebdomadaireHeures est requise");
        }
        if (request.dureeHebdomadaireHeures().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "dureeHebdomadaireHeures doit être strictement positive");
        }
        if (request.journeeMaximaleHeures() == null) {
            throw new IllegalArgumentException(
                    "journeeMaximaleHeures est requise");
        }
        if (request.journeeMaximaleHeures().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "journeeMaximaleHeures doit être strictement positive");
        }
        if (request.cctAutorise10h() == null) {
            throw new IllegalArgumentException(
                    "cctAutorise10h est requis");
        }
        if (request.avenantEcritSigne() == null) {
            throw new IllegalArgumentException(
                    "avenantEcritSigne est requis");
        }
        if (request.reglementTravailModifie() == null) {
            throw new IllegalArgumentException(
                    "reglementTravailModifie est requis");
        }
        if (request.dureeAvenantMois() == null) {
            throw new IllegalArgumentException(
                    "dureeAvenantMois est requise");
        }
        if (request.dureeAvenantMois().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "dureeAvenantMois doit être positive ou nulle");
        }
        if (request.avenantRenouvele() == null) {
            throw new IllegalArgumentException(
                    "avenantRenouvele est requis");
        }
        if (request.refusMotiveParEcrit() == null) {
            throw new IllegalArgumentException(
                    "refusMotiveParEcrit est requis");
        }
        if (request.motifLicenciementObjectifEtabli() == null) {
            throw new IllegalArgumentException(
                    "motifLicenciementObjectifEtabli est requis");
        }
    }
}
