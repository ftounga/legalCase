package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-219-31 : analyseur du droit au <b>congé de paternité / naissance
 * BE</b> — fonction <b>pure</b> (sans dépendance HTTP / persistance /
 * horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 03/07/1978</b> relative aux contrats de travail —
 *       <b>art. 30 § 2</b> droit au congé de naissance pour le
 *       travailleur salarié, alinéa 2 délai légal de 4 mois à compter
 *       de la naissance pour la prise des jours, alinéa 4 cas de
 *       report (incapacité de la mère, hospitalisation de l'enfant),
 *       <b>art. 30 § 4</b> protection contre le licenciement durant
 *       5 mois à compter de la notification à l'employeur jusqu'à
 *       5 mois après la fin de la prise.</li>
 *   <li><b>Loi du 12/08/2000</b> portant des dispositions sociales,
 *       budgétaires et diverses — création initiale du congé de
 *       paternité 10 jours puis 3 jours obligatoires.</li>
 *   <li><b>Loi du 07/04/2023</b> (Deal pour l'emploi 2022) — extension
 *       du congé à 20 jours ouvrables pour les naissances à compter du
 *       01/01/2023, ouverture aux co-parents indépendamment du genre
 *       (comaternité, reconnaissance).</li>
 *   <li><b>AR du 17/10/1994</b> modalités d'information et de prise du
 *       congé — formalité d'avis à l'employeur, présentation d'un
 *       justificatif (acte de naissance, attestation hôpital).</li>
 *   <li><b>AR du 03/07/1996</b> (INAMI) — indemnisation du congé :
 *       3 premiers jours rémunérés par l'employeur à 100 %, jours
 *       suivants indemnisés par la mutuelle à 82 % du salaire plafonné.</li>
 *   <li><b>Loi du 13/04/2019</b> + <b>AR du 23/05/2019</b> — régime
 *       distinct du congé de naissance pour l'<b>indépendant</b>
 *       (20 jours forfaitaires indemnisés par l'INASTI).</li>
 *   <li><b>AR du 19/11/1998</b> art. 23 et suivants — régime sectoriel
 *       du congé de paternité pour l'<b>agent public statutaire</b>
 *       fédéral.</li>
 *   <li><b>Cass. BE 16/03/2015 S.13.0094.F</b> — protection 5 mois
 *       art. 30 § 4 opposable à l'employeur en cas de licenciement
 *       non fondé sur un motif étranger au congé.</li>
 *   <li><b>Cass. BE 25/11/2019 S.18.0086.F</b> — preuve du lien de
 *       filiation par tout document juridique probant (acte de
 *       naissance, reconnaissance, comaternité).</li>
 *   <li><b>Cass. BE 12/05/2014 S.13.0103.F</b> — articulation du congé
 *       de naissance avec les autres suspensions du contrat de travail
 *       (incapacité, interruption de carrière).</li>
 * </ul>
 *
 * <h2>Durée applicable</h2>
 * <ul>
 *   <li>Naissances <b>≥ 01/01/2023</b> → <b>20 jours ouvrables</b>
 *       (Loi 07/04/2023).</li>
 *   <li>Naissances <b>du 01/01/2021 au 31/12/2022</b> → <b>15 jours
 *       ouvrables</b> (Loi-programme 27/06/2021 art. 14).</li>
 *   <li>Naissances <b>≤ 31/12/2020</b> → <b>10 jours ouvrables</b>
 *       (Loi 03/07/1978 art. 30 § 2 dans sa rédaction initiale).</li>
 * </ul>
 *
 * <h2>Logique du qualificateur</h2>
 * <ol>
 *   <li>Statut non couvert (indépendant, agent public, autre) →
 *       {@link CongePaterniteNaissanceBeVerdict#INELIGIBLE_STATUT_NON_COUVERT}
 *       (orientation vers régime spécifique).</li>
 *   <li>Filiation non établie → {@link
 *       CongePaterniteNaissanceBeVerdict#INELIGIBLE_FILIATION_NON_ETABLIE}.</li>
 *   <li>Étape AVANT_NAISSANCE et pas de date de naissance →
 *       {@link CongePaterniteNaissanceBeVerdict#INELIGIBLE_NAISSANCE_FUTURE}
 *       (calcul indicatif).</li>
 *   <li>Délai 4 mois post-naissance dépassé et jours restants > 0 →
 *       {@link CongePaterniteNaissanceBeVerdict#DROIT_PERDU_DELAI_DEPASSE}.</li>
 *   <li>Étape NAISSANCE_SURVENUE ou NOTIFICATION_EMPLOYEUR_FAITE,
 *       jours déjà pris = 0 → {@link
 *       CongePaterniteNaissanceBeVerdict#ELIGIBLE_CONGE_OUVERT}.</li>
 *   <li>Étape CONGE_EN_COURS_DE_PRISE → {@link
 *       CongePaterniteNaissanceBeVerdict#CONGE_EN_COURS_PROTECTION_ACTIVE}.</li>
 *   <li>Étape CONGE_PRIS_ENTIEREMENT → {@link
 *       CongePaterniteNaissanceBeVerdict#CONGE_PRIS_PROTECTION_RESIDUELLE}.</li>
 * </ol>
 *
 * <h2>Calcul des indemnités</h2>
 * <ul>
 *   <li>{@code indemniteEmployeurJoursCent} = min(3, jours déjà pris)
 *       — 3 premiers jours rémunérés par l'employeur à 100 % du salaire
 *       (AR 03/07/1996 art. 1er).</li>
 *   <li>{@code indemniteMutuelleJoursQuatreVingtDeux} = max(0, jours
 *       pris − 3) — jours suivants indemnisés par la mutuelle à 82 %
 *       du salaire plafonné.</li>
 * </ul>
 */
public final class CongePaterniteNaissanceBeChecker {

    /** Durée du congé pour naissances ≥ 01/01/2023 (Loi 07/04/2023). */
    static final int DUREE_VINGT_JOURS = 20;

    /** Durée du congé pour naissances 01/01/2021 → 31/12/2022. */
    static final int DUREE_QUINZE_JOURS = 15;

    /** Durée du congé pour naissances ≤ 31/12/2020. */
    static final int DUREE_DIX_JOURS = 10;

    /** Délai légal de prise du congé (mois) art. 30 § 2 al. 2. */
    static final int DELAI_QUATRE_MOIS = 4;

    /** Durée de la protection contre le licenciement (mois) art. 30 § 4. */
    static final int PROTECTION_LICENCIEMENT_MOIS = 5;

    /** Nombre de jours rémunérés à 100 % par l'employeur (AR 03/07/1996). */
    static final int JOURS_EMPLOYEUR_CENT = 3;

    /** Pivot Loi 07/04/2023 — naissances à compter du 01/01/2023 → 20 jours. */
    static final LocalDate PIVOT_VINGT_JOURS = LocalDate.of(2023, 1, 1);

    /** Pivot Loi-programme 27/06/2021 — naissances à compter du 01/01/2021 → 15 jours. */
    static final LocalDate PIVOT_QUINZE_JOURS = LocalDate.of(2021, 1, 1);

    static final String BASE_JURIDIQUE =
            "Loi du 03/07/1978 M.B. 22/08/1978 relative aux contrats de"
                    + " travail art. 30 paragraphe 2 droit au congé de"
                    + " naissance pour le travailleur salarié alinéa 1"
                    + " ouverture du droit au père reconnu ou présumé et"
                    + " depuis la Loi du 07/04/2023 M.B. 19/05/2023 Deal"
                    + " pour l'emploi 2022 à tout co-parent indépendamment"
                    + " du genre comaternité art. 325/2 C. civ."
                    + " reconnaissance art. 327/1 C. civ. cohabitation"
                    + " légale ou mariage avec la mère alinéa 2 délai"
                    + " légal de quatre mois à compter de la naissance"
                    + " pour la prise des jours alinéa 3 jours fractionnables"
                    + " en demi-journées sans contrainte de continuité"
                    + " alinéa 4 cas de report incapacité prolongée de la"
                    + " mère hospitalisation de l'enfant article 30"
                    + " paragraphe 4 protection contre le licenciement"
                    + " pendant cinq mois à compter de la notification"
                    + " écrite à l'employeur et jusqu'à cinq mois après"
                    + " la fin de la prise effective sauf motif étranger"
                    + " au congé charge de la preuve sur l'employeur ;"
                    + " Loi du 12/08/2000 M.B. 31/08/2000 portant des"
                    + " dispositions sociales budgétaires et diverses"
                    + " création initiale du congé dix jours dont trois"
                    + " obligatoires ; Loi-programme du 27/06/2021 M.B."
                    + " 30/06/2021 art. 14 extension à quinze jours pour"
                    + " les naissances à compter du 01/01/2021 ; Loi du"
                    + " 07/04/2023 M.B. 19/05/2023 Deal pour l'emploi"
                    + " extension à vingt jours pour les naissances à"
                    + " compter du 01/01/2023 et ouverture du droit à"
                    + " tous les co-parents ; AR du 17/10/1994 M.B."
                    + " 16/12/1994 modalités d'information de l'employeur"
                    + " et de présentation des justificatifs acte de"
                    + " naissance attestation hôpital ; AR du 03/07/1996"
                    + " M.B. 31/07/1996 portant exécution de la loi"
                    + " relative à l'assurance obligatoire soins de santé"
                    + " et indemnités art. 1er indemnisation des trois"
                    + " premiers jours à cent pour cent par l'employeur"
                    + " jours suivants quatre-vingt-deux pour cent par la"
                    + " mutuelle du salaire journalier plafonné INAMI ;"
                    + " Loi du 13/04/2019 et AR du 23/05/2019 régime du"
                    + " congé de naissance pour le travailleur indépendant"
                    + " vingt jours forfaitaires indemnisés par l'INASTI ;"
                    + " AR du 19/11/1998 art. 23 et suivants régime"
                    + " sectoriel du congé de paternité pour l'agent"
                    + " public statutaire fédéral ; Cass. BE 16/03/2015"
                    + " S.13.0094.F protection cinq mois art. 30 § 4"
                    + " opposable à l'employeur en cas de licenciement"
                    + " non fondé sur un motif étranger au congé ;"
                    + " Cass. BE 25/11/2019 S.18.0086.F preuve du lien"
                    + " de filiation par tout document juridique probant ;"
                    + " Cass. BE 12/05/2014 S.13.0103.F articulation du"
                    + " congé de naissance avec les autres suspensions du"
                    + " contrat de travail incapacité interruption de"
                    + " carrière chômage temporaire.";

    static final String AVERT_AVOCAT_BE =
            "L'outil d'analyse du congé de paternité naissance BE produit"
                    + " un verdict indicatif fondé sur les déclarations"
                    + " de l'avocat sur l'éligibilité au droit au congé"
                    + " au regard de la Loi du 03/07/1978 art. 30"
                    + " paragraphe 2 et calcule la durée applicable selon"
                    + " la date de naissance vingt jours pour naissances"
                    + " à compter du 01/01/2023 Loi du 07/04/2023 quinze"
                    + " jours pour naissances 01/01/2021 au 31/12/2022"
                    + " Loi-programme 27/06/2021 dix jours pour naissances"
                    + " antérieures Loi 12/08/2000. L'outil ne se prononce"
                    + " PAS sur le calcul exact des indemnités INAMI à la"
                    + " main de la mutuelle ni sur l'éligibilité"
                    + " contributive aux indemnités journalières assurance"
                    + " maladie-invalidité. L'avocat spécialisé en droit"
                    + " social BE doit confirmer pour la situation"
                    + " considérée (i) le statut professionnel exact"
                    + " salarié sous contrat de travail Loi 03/07/1978 art."
                    + " 2 indépendant Loi 13/04/2019 agent public AR"
                    + " 19/11/1998 et l'absence de cumul avec un autre"
                    + " congé suspensif ; (ii) l'établissement juridique"
                    + " du lien de filiation acte de naissance"
                    + " reconnaissance art. 327/1 C. civ. comaternité art."
                    + " 325/2 C. civ. mariage ou cohabitation légale avec"
                    + " la mère pour le co-parent non biologique Loi"
                    + " 07/04/2023 ; (iii) la conformité de la"
                    + " notification écrite à l'employeur AR du 17/10/1994"
                    + " préalable ou immédiatement après le premier jour"
                    + " pris avec présentation d'un justificatif"
                    + " probant ; (iv) le respect du délai légal de quatre"
                    + " mois post-naissance art. 30 paragraphe 2 alinéa 2"
                    + " sous peine de perte du droit non encore utilisé"
                    + " sauf cas de report légal hospitalisation de"
                    + " l'enfant incapacité prolongée de la mère décès de"
                    + " la mère art. 30 paragraphe 2 alinéa 4 ; (v) le"
                    + " décompte exact des jours pris par fractionnement"
                    + " en demi-journées art. 30 paragraphe 2 alinéa 3 ;"
                    + " (vi) l'application de la protection contre le"
                    + " licenciement cinq mois art. 30 paragraphe 4 à"
                    + " compter de la notification écrite et jusqu'à cinq"
                    + " mois après la fin de la prise effective avec"
                    + " indemnité forfaitaire de six mois de rémunération"
                    + " brute en cas de licenciement irrégulier Cass. BE"
                    + " 16/03/2015 S.13.0094.F ; (vii) le déclenchement"
                    + " du paiement employeur cent pour cent pour les"
                    + " trois premiers jours AR 03/07/1996 art. 1er puis"
                    + " mutuelle quatre-vingt-deux pour cent pour les"
                    + " jours suivants plafonné au salaire journalier"
                    + " INAMI ; (viii) l'articulation avec le congé"
                    + " parental Loi 22/01/1985 et AR 29/10/1997 le congé"
                    + " d'adoption Loi 03/07/1978 art. 30ter et les"
                    + " autres suspensions Cass. BE 12/05/2014"
                    + " S.13.0103.F. La présente analyse constitue un"
                    + " éclairage indicatif et ne remplace pas la"
                    + " consultation d'un avocat compétent en droit"
                    + " social BE ou d'un conseiller mutuelle pour le"
                    + " calcul précis des indemnités journalières.";

    private CongePaterniteNaissanceBeChecker() {
    }

    public static CongePaterniteNaissanceBeResult analyze(
            CongePaterniteNaissanceBeRequest request) {
        validateInputs(request);

        int duree = dureeApplicableJours(request.dateNaissance());
        int joursPris = request.joursDejaPrisOuvrables() == null
                ? 0 : request.joursDejaPrisOuvrables();
        int joursRestants = Math.max(0, duree - joursPris);

        LocalDate echeance4Mois = request.dateNaissance() == null
                ? null
                : request.dateNaissance().plusMonths(DELAI_QUATRE_MOIS);
        LocalDate finProtection = computeFinProtection(request);
        boolean protectionActive = isProtectionActive(request);

        int indemniteEmployeur =
                Math.max(0, Math.min(JOURS_EMPLOYEUR_CENT, joursPris));
        int indemniteMutuelle =
                Math.max(0, joursPris - JOURS_EMPLOYEUR_CENT);

        // 1) Statut non couvert (indépendant, agent public, autre)
        if (request.statutTravailleur()
                != CongePaterniteNaissanceBeStatutTravailleur.SALARIE) {
            return buildStatutNonCouvert(request, duree, joursRestants,
                    echeance4Mois, finProtection, protectionActive,
                    indemniteEmployeur, indemniteMutuelle);
        }

        // 2) Contrat de travail non en cours → inéligible salarié
        if (!request.contratTravailEnCours()) {
            return buildStatutNonCouvert(request, duree, joursRestants,
                    echeance4Mois, finProtection, protectionActive,
                    indemniteEmployeur, indemniteMutuelle);
        }

        // 3) Filiation non établie
        if (!request.filiationEtablie()) {
            return buildFiliationNonEtablie(request, duree, joursRestants,
                    echeance4Mois, finProtection, protectionActive,
                    indemniteEmployeur, indemniteMutuelle);
        }

        // 4) Étape AVANT_NAISSANCE → calcul indicatif (naissance future)
        if (request.etapeProcedure()
                == CongePaterniteNaissanceBeEtape.AVANT_NAISSANCE) {
            return buildNaissanceFuture(request, duree, joursRestants,
                    echeance4Mois, finProtection, protectionActive,
                    indemniteEmployeur, indemniteMutuelle);
        }

        // 5) Délai 4 mois dépassé et jours restants > 0 → droit perdu
        if (request.etapeProcedure()
                == CongePaterniteNaissanceBeEtape.DELAI_QUATRE_MOIS_DEPASSE
                && joursRestants > 0) {
            return buildDroitPerduDelaiDepasse(request, duree, joursRestants,
                    echeance4Mois, finProtection, protectionActive,
                    indemniteEmployeur, indemniteMutuelle);
        }

        // 6) Branches selon étape
        return switch (request.etapeProcedure()) {
            case NAISSANCE_SURVENUE, NOTIFICATION_EMPLOYEUR_FAITE ->
                    buildEligibleOuvert(request, duree, joursRestants,
                            echeance4Mois, finProtection, protectionActive,
                            indemniteEmployeur, indemniteMutuelle);
            case CONGE_EN_COURS_DE_PRISE ->
                    buildCongeEnCours(request, duree, joursRestants,
                            echeance4Mois, finProtection, protectionActive,
                            indemniteEmployeur, indemniteMutuelle);
            case CONGE_PRIS_ENTIEREMENT ->
                    buildCongePris(request, duree, joursRestants,
                            echeance4Mois, finProtection, protectionActive,
                            indemniteEmployeur, indemniteMutuelle);
            case DELAI_QUATRE_MOIS_DEPASSE ->
                    // Si on arrive ici, c'est que joursRestants == 0
                    // (sinon traité en branche 5) → assimilé pris.
                    buildCongePris(request, duree, joursRestants,
                            echeance4Mois, finProtection, protectionActive,
                            indemniteEmployeur, indemniteMutuelle);
            // AVANT_NAISSANCE déjà traité en 4
            case AVANT_NAISSANCE ->
                    buildNaissanceFuture(request, duree, joursRestants,
                            echeance4Mois, finProtection, protectionActive,
                            indemniteEmployeur, indemniteMutuelle);
        };
    }

    // ── Helpers calculs ─────────────────────────────────────────────────────

    /**
     * Détermine la durée applicable en jours ouvrables en fonction de
     * la date de naissance. Si aucune date n'est connue, applique la
     * règle la plus récente (20 jours) à titre indicatif.
     */
    static int dureeApplicableJours(LocalDate dateNaissance) {
        if (dateNaissance == null) {
            return DUREE_VINGT_JOURS;
        }
        if (!dateNaissance.isBefore(PIVOT_VINGT_JOURS)) {
            return DUREE_VINGT_JOURS;
        }
        if (!dateNaissance.isBefore(PIVOT_QUINZE_JOURS)) {
            return DUREE_QUINZE_JOURS;
        }
        return DUREE_DIX_JOURS;
    }

    private static LocalDate computeFinProtection(
            CongePaterniteNaissanceBeRequest req) {
        // Fin de protection = max(notification + 5 mois, fin prise + 5 mois)
        LocalDate finPriseRef = req.dateFinPriseEffective();
        LocalDate notifRef = req.dateNotificationEmployeur();
        if (notifRef == null && finPriseRef == null) {
            return null;
        }
        LocalDate base = finPriseRef != null ? finPriseRef : notifRef;
        if (notifRef != null && finPriseRef != null
                && notifRef.isAfter(finPriseRef)) {
            base = notifRef;
        }
        return base.plusMonths(PROTECTION_LICENCIEMENT_MOIS);
    }

    private static boolean isProtectionActive(
            CongePaterniteNaissanceBeRequest req) {
        // Protection active dès la notification employeur (ou prise d'un jour)
        // tant que la fenêtre de 5 mois post-fin n'est pas écoulée.
        if (!req.notificationEmployeurFaite()
                && req.etapeProcedure()
                != CongePaterniteNaissanceBeEtape.CONGE_EN_COURS_DE_PRISE
                && req.etapeProcedure()
                != CongePaterniteNaissanceBeEtape.CONGE_PRIS_ENTIEREMENT) {
            return false;
        }
        return req.etapeProcedure()
                == CongePaterniteNaissanceBeEtape.NOTIFICATION_EMPLOYEUR_FAITE
                || req.etapeProcedure()
                == CongePaterniteNaissanceBeEtape.CONGE_EN_COURS_DE_PRISE
                || req.etapeProcedure()
                == CongePaterniteNaissanceBeEtape.CONGE_PRIS_ENTIEREMENT;
    }

    // ── Branches verdict ────────────────────────────────────────────────────

    private static CongePaterniteNaissanceBeResult buildStatutNonCouvert(
            CongePaterniteNaissanceBeRequest req, int duree,
            int joursRestants, LocalDate echeance4Mois,
            LocalDate finProtection, boolean protectionActive,
            int indemniteEmployeur, int indemniteMutuelle) {
        String orientation;
        switch (req.statutTravailleur()) {
            case INDEPENDANT -> orientation =
                    "Le travailleur indépendant relève d'un régime"
                            + " distinct Loi du 13/04/2019 et AR du"
                            + " 23/05/2019 vingt jours forfaitaires"
                            + " indemnisés par l'Institut National"
                            + " d'Assurances Sociales pour Travailleurs"
                            + " Indépendants INASTI. Voir SF-219-27"
                            + " inastri-statut-travailleur-independant"
                            + " pour la vérification du statut.";
            case AGENT_PUBLIC_STATUTAIRE -> orientation =
                    "L'agent public statutaire fédéral relève du régime"
                            + " sectoriel AR du 19/11/1998 art. 23 et"
                            + " suivants congé de paternité de droit"
                            + " administratif applicable aux services"
                            + " publics fédéraux. Régimes régionaux et"
                            + " communaux distincts.";
            case AUTRE -> orientation =
                    "Statut professionnel non qualifié — vérifier la"
                            + " nature exacte du lien de travail (contrat"
                            + " de travail Loi 03/07/1978 art. 2,"
                            + " indépendant, agent statutaire, intérimaire"
                            + " avec contrat utilisateur Loi 24/07/1987)"
                            + " avant de déterminer le régime applicable.";
            default -> orientation = "";
        }
        if (req.statutTravailleur()
                == CongePaterniteNaissanceBeStatutTravailleur.SALARIE
                && !req.contratTravailEnCours()) {
            orientation = "Le contrat de travail salarié n'est plus en"
                    + " cours au moment de la naissance ou de la prise"
                    + " du congé. La condition d'ouverture art. 30"
                    + " paragraphe 2 Loi du 03/07/1978 exige un contrat"
                    + " de travail effectivement suspendu pour la prise"
                    + " du congé — un contrat rompu (licenciement,"
                    + " démission, rupture conventionnelle) ferme le"
                    + " droit. L'avocat doit vérifier la date exacte de"
                    + " fin du contrat par rapport à la naissance.";
        }
        String synthese = "Le régime du congé de naissance prévu par la"
                + " Loi du 03/07/1978 art. 30 § 2 est réservé au"
                + " travailleur salarié sous contrat de travail. "
                + orientation + " Conséquences pratiques : (a) le"
                + " présent outil ne calcule pas la durée ni"
                + " l'indemnisation applicables au régime spécifique ;"
                + " (b) l'avocat doit confirmer le statut exact et"
                + " orienter vers le régime ad hoc (INASTI pour"
                + " l'indépendant, fonction publique pour l'agent"
                + " statutaire) ; (c) la durée légale et l'indemnisation"
                + " varient selon les régimes même si la finalité"
                + " (couverture du co-parent à la naissance) est"
                + " commune.";
        return build(req,
                CongePaterniteNaissanceBeVerdict.INELIGIBLE_STATUT_NON_COUVERT,
                duree, joursRestants, echeance4Mois, finProtection,
                protectionActive, indemniteEmployeur, indemniteMutuelle,
                "STATUT_HORS_CHAMP_LOI_03_07_1978_ART_30_PARAGRAPHE_2",
                synthese);
    }

    private static CongePaterniteNaissanceBeResult buildFiliationNonEtablie(
            CongePaterniteNaissanceBeRequest req, int duree,
            int joursRestants, LocalDate echeance4Mois,
            LocalDate finProtection, boolean protectionActive,
            int indemniteEmployeur, int indemniteMutuelle) {
        String synthese = "Le lien de filiation ou parental n'est pas"
                + " établi juridiquement à la date de l'analyse. Le"
                + " bénéfice du congé de naissance art. 30 § 2 Loi"
                + " 03/07/1978 est subordonné à l'existence d'un lien"
                + " juridique probant entre le travailleur et l'enfant —"
                + " présomption de paternité dans le mariage art. 315 C."
                + " civ., reconnaissance art. 327/1 C. civ., comaternité"
                + " art. 325/2 C. civ., ou qualité de cohabitant légal /"
                + " conjoint de la mère (Loi du 07/04/2023). La preuve"
                + " est apportée par l'acte de naissance, l'acte de"
                + " reconnaissance, l'acte de comaternité, l'acte de"
                + " mariage ou de cohabitation légale (Cass. BE"
                + " 25/11/2019 S.18.0086.F). Conséquences pratiques :"
                + " (a) le congé ne peut être pris légalement tant que"
                + " la filiation n'est pas régularisée — la"
                + " reconnaissance prénatale art. 328 C. civ. peut être"
                + " effectuée avant la naissance ; (b) l'employeur peut"
                + " légitimement refuser la prise du congé sur"
                + " production d'un acte non probant ; (c) l'avocat"
                + " doit accompagner la régularisation de la filiation"
                + " auprès de l'officier de l'état civil avant toute"
                + " notification employeur.";
        return build(req,
                CongePaterniteNaissanceBeVerdict.INELIGIBLE_FILIATION_NON_ETABLIE,
                duree, joursRestants, echeance4Mois, finProtection,
                protectionActive, indemniteEmployeur, indemniteMutuelle,
                "LIEN_FILIATION_NON_ETABLI_ART_30_PARAGRAPHE_2_LOI_03_07_1978",
                synthese);
    }

    private static CongePaterniteNaissanceBeResult buildNaissanceFuture(
            CongePaterniteNaissanceBeRequest req, int duree,
            int joursRestants, LocalDate echeance4Mois,
            LocalDate finProtection, boolean protectionActive,
            int indemniteEmployeur, int indemniteMutuelle) {
        String synthese = "La naissance n'est pas encore survenue. Le"
                + " droit au congé de naissance art. 30 § 2 Loi"
                + " 03/07/1978 est en formation et l'analyse fournit"
                + " un calcul indicatif sur la base d'une durée de "
                + duree + " jours ouvrables applicable selon la date"
                + " présumée de naissance. Points d'attention : (a) la"
                + " reconnaissance prénatale art. 328 C. civ. est"
                + " recommandée pour sécuriser le lien de filiation"
                + " avant la naissance — elle permet à l'officier de"
                + " l'état civil d'inscrire le co-parent à l'acte de"
                + " naissance dès la déclaration ; (b) la notification"
                + " écrite à l'employeur AR 17/10/1994 peut être faite"
                + " préalablement avec date présumée puis confirmée par"
                + " l'acte de naissance ; (c) le délai de quatre mois"
                + " post-naissance court à compter de la naissance"
                + " effective art. 30 § 2 al. 2 — la prise anticipée"
                + " n'est pas autorisée ; (d) la durée applicable est"
                + " vingt jours ouvrables pour les naissances à compter"
                + " du 01/01/2023 Loi 07/04/2023 quinze jours pour les"
                + " naissances 01/01/2021 au 31/12/2022 et dix jours"
                + " pour les naissances antérieures.";
        return build(req,
                CongePaterniteNaissanceBeVerdict.INELIGIBLE_NAISSANCE_FUTURE,
                duree, joursRestants, echeance4Mois, finProtection,
                protectionActive, indemniteEmployeur, indemniteMutuelle,
                "NAISSANCE_NON_SURVENUE_CALCUL_INDICATIF",
                synthese);
    }

    private static CongePaterniteNaissanceBeResult buildEligibleOuvert(
            CongePaterniteNaissanceBeRequest req, int duree,
            int joursRestants, LocalDate echeance4Mois,
            LocalDate finProtection, boolean protectionActive,
            int indemniteEmployeur, int indemniteMutuelle) {
        String echStr = echeance4Mois == null
                ? "non calculable (date de naissance non renseignée)"
                : echeance4Mois.toString();
        String synthese = "Le droit au congé de naissance est OUVERT —"
                + " la naissance est survenue, le statut salarié est"
                + " confirmé, la filiation est établie. Durée légale"
                + " applicable : " + duree + " jours ouvrables ("
                + (duree == DUREE_VINGT_JOURS
                ? "Loi du 07/04/2023 Deal pour l'emploi, naissances ≥ 01/01/2023"
                : duree == DUREE_QUINZE_JOURS
                ? "Loi-programme 27/06/2021, naissances 01/01/2021 → 31/12/2022"
                : "Loi du 12/08/2000, naissances ≤ 31/12/2020")
                + "). Jours restants à prendre : " + joursRestants + "."
                + " Échéance légale de prise (4 mois post-naissance) : "
                + echStr + " art. 30 § 2 al. 2 Loi 03/07/1978. Points"
                + " d'attention : (a) la prise est fractionnable en"
                + " demi-journées art. 30 § 2 al. 3 — pas de contrainte"
                + " de continuité ; (b) la notification écrite à"
                + " l'employeur AR 17/10/1994 conditionne le démarrage"
                + " de la protection contre le licenciement cinq mois"
                + " art. 30 § 4 ; (c) l'indemnisation suit la règle"
                + " trois premiers jours rémunérés par l'employeur à"
                + " cent pour cent puis mutuelle quatre-vingt-deux pour"
                + " cent du salaire journalier plafonné INAMI AR"
                + " 03/07/1996 art. 1er ; (d) cas de report légal du"
                + " délai quatre mois : incapacité prolongée de la mère,"
                + " hospitalisation de l'enfant, décès de la mère"
                + " art. 30 § 2 al. 4 ; (e) cumul possible avec congé"
                + " parental Loi 22/01/1985 mais pas simultané ;"
                + " (f) le travailleur peut demander à son employeur"
                + " une planification anticipée pour permettre"
                + " l'organisation de la suppléance.";
        return build(req,
                CongePaterniteNaissanceBeVerdict.ELIGIBLE_CONGE_OUVERT,
                duree, joursRestants, echeance4Mois, finProtection,
                protectionActive, indemniteEmployeur, indemniteMutuelle,
                "DROIT_OUVERT_DUREE_LEGALE_APPLICABLE",
                synthese);
    }

    private static CongePaterniteNaissanceBeResult buildCongeEnCours(
            CongePaterniteNaissanceBeRequest req, int duree,
            int joursRestants, LocalDate echeance4Mois,
            LocalDate finProtection, boolean protectionActive,
            int indemniteEmployeur, int indemniteMutuelle) {
        String finProtStr = finProtection == null
                ? "non calculable (notification ou fin de prise non renseignée)"
                : finProtection.toString();
        String synthese = "Le congé de naissance est EN COURS DE PRISE."
                + " Sur " + duree + " jours ouvrables légaux, "
                + (req.joursDejaPrisOuvrables() == null
                ? 0 : req.joursDejaPrisOuvrables())
                + " ont déjà été pris ; il reste " + joursRestants
                + " jours à prendre avant l'échéance des quatre mois"
                + " post-naissance art. 30 § 2 al. 2 Loi 03/07/1978."
                + " La PROTECTION CONTRE LE LICENCIEMENT cinq mois"
                + " art. 30 § 4 est ACTIVE depuis la notification"
                + " écrite à l'employeur et le restera jusqu'à cinq"
                + " mois après la fin de la prise effective —"
                + " échéance calculée : " + finProtStr + ". Indemnisation"
                + " déjà engagée : " + indemniteEmployeur + " jours à"
                + " la charge de l'employeur cent pour cent et "
                + indemniteMutuelle + " jours à la charge de la mutuelle"
                + " quatre-vingt-deux pour cent du salaire journalier"
                + " plafonné INAMI AR 03/07/1996. Points d'attention :"
                + " (a) tout licenciement de l'employeur durant cette"
                + " période est présumé représailles sauf motif étranger"
                + " au congé que l'employeur doit prouver Cass. BE"
                + " 16/03/2015 S.13.0094.F ; (b) en cas de licenciement"
                + " irrégulier le travailleur a droit à une indemnité"
                + " forfaitaire de SIX MOIS de rémunération brute en"
                + " plus du préavis ; (c) le solde des jours restants"
                + " est verrouillé par l'échéance des quatre mois — sauf"
                + " cas de report légal hospitalisation enfant"
                + " incapacité prolongée mère décès mère art. 30 § 2 al."
                + " 4 ; (d) le fractionnement en demi-jours est"
                + " préservé ; (e) la coordination avec d'autres"
                + " absences vacances arrêt maladie est gérée selon"
                + " l'ordre chronologique des suspensions Cass. BE"
                + " 12/05/2014 S.13.0103.F.";
        return build(req,
                CongePaterniteNaissanceBeVerdict.CONGE_EN_COURS_PROTECTION_ACTIVE,
                duree, joursRestants, echeance4Mois, finProtection,
                protectionActive, indemniteEmployeur, indemniteMutuelle,
                "CONGE_EN_COURS_PROTECTION_LICENCIEMENT_CINQ_MOIS_ACTIVE",
                synthese);
    }

    private static CongePaterniteNaissanceBeResult buildCongePris(
            CongePaterniteNaissanceBeRequest req, int duree,
            int joursRestants, LocalDate echeance4Mois,
            LocalDate finProtection, boolean protectionActive,
            int indemniteEmployeur, int indemniteMutuelle) {
        String finProtStr = finProtection == null
                ? "non calculable (date de fin de prise non renseignée)"
                : finProtection.toString();
        int pris = req.joursDejaPrisOuvrables() == null
                ? duree : req.joursDejaPrisOuvrables();
        String synthese = "Le congé de naissance a été pris ENTIÈREMENT"
                + " — " + pris + " jours ouvrables sur " + duree
                + " jours légaux. Le droit est clôturé. La PROTECTION"
                + " RÉSIDUELLE CONTRE LE LICENCIEMENT cinq mois art. 30"
                + " § 4 Loi 03/07/1978 court jusqu'à : " + finProtStr
                + " (cinq mois à compter de la date de fin de prise"
                + " effective). Indemnisation totale : "
                + indemniteEmployeur + " jours à la charge de"
                + " l'employeur cent pour cent (AR 03/07/1996 art. 1er)"
                + " et " + indemniteMutuelle + " jours à la charge de la"
                + " mutuelle quatre-vingt-deux pour cent du salaire"
                + " journalier plafonné INAMI. Points d'attention :"
                + " (a) tout licenciement durant la fenêtre de"
                + " protection résiduelle est présumé représailles sauf"
                + " motif étranger au congé que l'employeur doit prouver"
                + " Cass. BE 16/03/2015 S.13.0094.F ; (b) au-delà de"
                + " l'échéance la protection s'éteint mais l'employeur"
                + " reste tenu de la motivation usuelle Loi du 26/12/2013"
                + " sur la motivation du licenciement et des conventions"
                + " collectives applicables ; (c) le travailleur peut"
                + " demander à son employeur une attestation de prise"
                + " effective utile pour la mutuelle et l'historique"
                + " des suspensions ; (d) le crédit-temps congé parental"
                + " Loi 22/01/1985 reste accessible séparément.";
        return build(req,
                CongePaterniteNaissanceBeVerdict.CONGE_PRIS_PROTECTION_RESIDUELLE,
                duree, joursRestants, echeance4Mois, finProtection,
                protectionActive, indemniteEmployeur, indemniteMutuelle,
                "CONGE_PRIS_PROTECTION_LICENCIEMENT_RESIDUELLE_CINQ_MOIS",
                synthese);
    }

    private static CongePaterniteNaissanceBeResult buildDroitPerduDelaiDepasse(
            CongePaterniteNaissanceBeRequest req, int duree,
            int joursRestants, LocalDate echeance4Mois,
            LocalDate finProtection, boolean protectionActive,
            int indemniteEmployeur, int indemniteMutuelle) {
        String echStr = echeance4Mois == null
                ? "non calculable" : echeance4Mois.toString();
        String synthese = "Le délai légal de QUATRE MOIS POST-NAISSANCE"
                + " art. 30 § 2 al. 2 Loi 03/07/1978 est DÉPASSÉ — la"
                + " date butoir calculée était : " + echStr + ". Sur "
                + duree + " jours ouvrables légaux, " + joursRestants
                + " jours restaient à prendre et SONT PERDUS à défaut"
                + " de cas de report légal admis par l'art. 30 § 2 al."
                + " 4 (incapacité prolongée de la mère, hospitalisation"
                + " de l'enfant pendant au moins sept jours,"
                + " hospitalisation prolongée du nouveau-né en"
                + " néonatologie, décès de la mère). Conséquences"
                + " pratiques : (a) le droit aux jours non pris est"
                + " éteint et ne peut pas être reporté hors des cas"
                + " expressément prévus par la loi ; (b) la protection"
                + " contre le licenciement art. 30 § 4 ne couvre que"
                + " les jours effectivement pris dans le délai ; (c)"
                + " l'avocat doit examiner s'il existe un cas de report"
                + " légal non identifié initialement — l'attestation"
                + " hôpital d'hospitalisation prolongée constitue la"
                + " preuve principale ; (d) un licenciement reposant"
                + " sur le non-recours au congé est nul Cass. BE"
                + " 16/03/2015 S.13.0094.F mais ne saurait fonder une"
                + " réouverture du droit ; (e) le travailleur peut"
                + " demander à son employeur la régularisation des"
                + " jours pris avant l'échéance s'ils n'ont pas été"
                + " correctement décomptés.";
        return build(req,
                CongePaterniteNaissanceBeVerdict.DROIT_PERDU_DELAI_DEPASSE,
                duree, joursRestants, echeance4Mois, finProtection,
                protectionActive, indemniteEmployeur, indemniteMutuelle,
                "DELAI_QUATRE_MOIS_POST_NAISSANCE_DEPASSE_ART_30_PARAGRAPHE_2_ALINEA_2",
                synthese);
    }

    // ── Construction du résultat ────────────────────────────────────────────

    private static CongePaterniteNaissanceBeResult build(
            CongePaterniteNaissanceBeRequest req,
            CongePaterniteNaissanceBeVerdict verdict,
            int duree, int joursRestants, LocalDate echeance4Mois,
            LocalDate finProtection, boolean protectionActive,
            int indemniteEmployeur, int indemniteMutuelle,
            String raison, String synthese) {
        return new CongePaterniteNaissanceBeResult(
                req.statutTravailleur(),
                req.lienFiliation(),
                req.etapeProcedure(),
                req.filiationEtablie(),
                req.contratTravailEnCours(),
                req.notificationEmployeurFaite(),
                req.dateNaissance(),
                req.dateNotificationEmployeur(),
                req.joursDejaPrisOuvrables(),
                req.dateFinPriseEffective(),
                verdict,
                duree,
                joursRestants,
                echeance4Mois,
                finProtection,
                protectionActive,
                indemniteEmployeur,
                indemniteMutuelle,
                raison, synthese, BASE_JURIDIQUE, AVERT_AVOCAT_BE);
    }

    private static void validateInputs(
            CongePaterniteNaissanceBeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.statutTravailleur() == null) {
            throw new IllegalArgumentException(
                    "statutTravailleur est requis");
        }
        if (request.lienFiliation() == null) {
            throw new IllegalArgumentException("lienFiliation est requis");
        }
        if (request.etapeProcedure() == null) {
            throw new IllegalArgumentException("etapeProcedure est requis");
        }
        if (request.filiationEtablie() == null) {
            throw new IllegalArgumentException(
                    "filiationEtablie est requis");
        }
        if (request.contratTravailEnCours() == null) {
            throw new IllegalArgumentException(
                    "contratTravailEnCours est requis");
        }
        if (request.notificationEmployeurFaite() == null) {
            throw new IllegalArgumentException(
                    "notificationEmployeurFaite est requis");
        }
        if (request.joursDejaPrisOuvrables() != null
                && request.joursDejaPrisOuvrables() < 0) {
            throw new IllegalArgumentException(
                    "joursDejaPrisOuvrables ne peut pas être négatif");
        }
        // Cohérence date naissance : si l'étape ≥ NAISSANCE_SURVENUE, la date doit être présente
        if ((request.etapeProcedure()
                == CongePaterniteNaissanceBeEtape.NAISSANCE_SURVENUE
                || request.etapeProcedure()
                == CongePaterniteNaissanceBeEtape.NOTIFICATION_EMPLOYEUR_FAITE
                || request.etapeProcedure()
                == CongePaterniteNaissanceBeEtape.CONGE_EN_COURS_DE_PRISE
                || request.etapeProcedure()
                == CongePaterniteNaissanceBeEtape.CONGE_PRIS_ENTIEREMENT
                || request.etapeProcedure()
                == CongePaterniteNaissanceBeEtape.DELAI_QUATRE_MOIS_DEPASSE)
                && request.dateNaissance() == null) {
            throw new IllegalArgumentException(
                    "dateNaissance est requise à partir de l'étape"
                            + " NAISSANCE_SURVENUE");
        }
    }
}
