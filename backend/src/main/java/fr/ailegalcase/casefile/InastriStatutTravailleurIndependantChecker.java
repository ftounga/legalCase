package fr.ailegalcase.casefile;

/**
 * SF-219-27 : qualificateur du statut travailleur indépendant vs
 * salarié au sens de la Loi du 27/06/1969 + AR n° 38 du 27/07/1967 +
 * Loi-programme I du 27/12/2006 — fonction <b>pure</b> (sans
 * dépendance HTTP / persistance / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 27/06/1969</b> (M.B. 25/07/1969) révisant
 *       l'arrêté-loi du 28/12/1944 concernant la sécurité sociale
 *       des travailleurs salariés — art. 1er présomption générale
 *       d'assujettissement à la sécurité sociale salariée.</li>
 *   <li><b>AR n° 38 du 27/07/1967</b> (M.B. 29/07/1967) organisant le
 *       statut social des travailleurs indépendants — art. 3 § 1
 *       définition du travailleur indépendant et obligation
 *       d'affiliation à une caisse d'assurances sociales.</li>
 *   <li><b>AR du 19/12/1967</b> (M.B. 28/12/1967) portant règlement
 *       général en exécution de l'AR n° 38 — modalités d'affiliation,
 *       cotisations trimestrielles, base de calcul.</li>
 *   <li><b>Loi-programme I du 27/12/2006 art. 328 à 333</b>
 *       (M.B. 28/12/2006) — « doctrine Bart Buysse » :
 *       <ul>
 *         <li><b>art. 328 § 1</b> — présomption simple de salariat
 *             applicable à toute personne pour laquelle aucune
 *             DIMONA n'a été effectuée ;</li>
 *         <li><b>art. 333</b> — 4 critères généraux de qualification :
 *             volonté des parties, liberté d'organisation du temps
 *             de travail, liberté d'organisation du travail,
 *             exercice d'un contrôle hiérarchique.</li>
 *       </ul></li>
 *   <li><b>Loi-programme I du 27/12/2006 art. 337/1 à 337/3</b>
 *       (introduits par la Loi du 25/08/2012, M.B. 11/09/2012) —
 *       présomption sectorielle de salariat applicable aux secteurs
 *       construction (AR du 07/06/2013), transport de choses,
 *       gardiennage, nettoyage et agriculture/horticulture (AR du
 *       29/10/2013). 9 critères sectoriels ; présomption déclenchée
 *       si majorité (≥ 5) des critères indique le salariat.</li>
 *   <li><b>Cass. BE 23/12/2002 S.02.0021.F</b> — primauté de
 *       l'exécution réelle de la relation sur la qualification
 *       conventionnelle (« doctrine Bart Buysse » consacrée).</li>
 *   <li><b>Cass. BE 06/12/2010 S.10.0067.F</b> — caractère simple
 *       (réfragable) de la présomption art. 328 § 1 — renversable
 *       par tout moyen de preuve.</li>
 *   <li><b>Cour const. arrêt 167/2013 du 19/12/2013</b> —
 *       conformité de la liste sectorielle et de la présomption
 *       sectorielle art. 337/2 aux principes du procès équitable.</li>
 *   <li><b>Cour const. arrêt 102/2009 du 18/06/2009</b> — conformité
 *       de la présomption générale de salariat art. 328 § 1 aux
 *       principes constitutionnels.</li>
 *   <li><b>Code pénal social art. 181</b> — sanction niveau 4 du
 *       défaut DIMONA cumulée en cas de requalification salarié
 *       (voir SF-219-26 pour le chiffrage).</li>
 * </ul>
 *
 * <h2>Logique du qualificateur</h2>
 * <ol>
 *   <li>Calcule un score « indépendant » et un score « salarié » sur
 *       les 4 critères généraux art. 333 :
 *       <ul>
 *         <li>volonté INDEPENDANT_EXPLICITE → +1 indépendant ;</li>
 *         <li>volonté SALARIE_EXPLICITE → +1 salarié ;</li>
 *         <li>volonté IMPLICITE_OU_AMBIGUE → +0 (neutre) ;</li>
 *         <li>chaque liberté TOTALE_INDEPENDANT → +1 indépendant ;</li>
 *         <li>chaque liberté AUCUNE_SALARIE → +1 salarié ;</li>
 *         <li>chaque liberté PARTIELLE → +0 ;</li>
 *         <li>contrôle hiérarchique AUCUNE_SALARIE → +1 indépendant
 *             (inversé : pas de contrôle = autonomie) ;</li>
 *         <li>contrôle hiérarchique TOTALE_INDEPENDANT → +1 salarié
 *             (inversé : contrôle régulier = salariat).</li>
 *       </ul></li>
 *   <li>Détecte la présomption sectorielle art. 337/2 § 1 si le
 *       secteur est CONSTRUCTION, TRANSPORT_DE_CHOSES, GARDIENNAGE,
 *       NETTOYAGE ou AGRICULTURE_HORTICULTURE.</li>
 *   <li>Déclenche la présomption sectorielle si nbCriteresSectorielsSalarie
 *       ≥ 5 (majorité simple sur 9 critères).</li>
 *   <li>Mobilise la présomption générale art. 328 § 1 si dimonaPresente
 *       = false et statutDeclare = SANS_DECLARATION.</li>
 *   <li>Détecte l'indice mono-client (absence d'autre client) qui
 *       milite pour le salariat déguisé.</li>
 *   <li>Retient le verdict selon la hiérarchie :
 *       <ul>
 *         <li>présomption sectorielle déclenchée →
 *             {@link InastriStatutTravailleurIndependantVerdict#FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE} ;</li>
 *         <li>scores salarié strictement supérieur à indépendant →
 *             {@link InastriStatutTravailleurIndependantVerdict#SALARIE_REQUALIFIE} ;</li>
 *         <li>présomption générale art. 328 mobilisable et scores
 *             non concluants en faveur de l'indépendant →
 *             {@link InastriStatutTravailleurIndependantVerdict#PRESUMPTION_GENERALE_SALARIAT} ;</li>
 *         <li>scores indépendant strictement supérieur à salarié →
 *             {@link InastriStatutTravailleurIndependantVerdict#INDEPENDANT_CONFIRME} ;</li>
 *         <li>égalité ou contradiction →
 *             {@link InastriStatutTravailleurIndependantVerdict#A_QUALIFIER}.</li>
 *       </ul></li>
 * </ol>
 */
public final class InastriStatutTravailleurIndependantChecker {

    /** Seuil de déclenchement de la présomption sectorielle art. 337/2. */
    static final int SEUIL_PRESUMPTION_SECTORIELLE = 5;

    static final String BASE_JURIDIQUE =
            "Loi du 27/06/1969 M.B. 25/07/1969 révisant l'arrêté-loi"
                    + " du 28/12/1944 concernant la sécurité sociale"
                    + " des travailleurs salariés art. 1er présomption"
                    + " générale d'assujettissement ; AR n° 38 du"
                    + " 27/07/1967 M.B. 29/07/1967 organisant le statut"
                    + " social des travailleurs indépendants art. 3 § 1"
                    + " définition du travailleur indépendant et"
                    + " obligation d'affiliation à une caisse"
                    + " d'assurances sociales pour travailleurs"
                    + " indépendants ; AR du 19/12/1967 M.B. 28/12/1967"
                    + " portant règlement général en exécution de"
                    + " l'AR n° 38 modalités d'affiliation cotisations"
                    + " trimestrielles et base de calcul ; Loi-programme"
                    + " I du 27/12/2006 M.B. 28/12/2006 art. 328 à 333"
                    + " doctrine Bart Buysse — art. 328 § 1 présomption"
                    + " simple de salariat applicable à toute personne"
                    + " pour laquelle aucune DIMONA n'a été effectuée ;"
                    + " art. 333 4 critères généraux de qualification"
                    + " volonté des parties liberté d'organisation du"
                    + " temps de travail liberté d'organisation du"
                    + " travail exercice d'un contrôle hiérarchique ;"
                    + " art. 337/1 à 337/3 Loi-programme I 27/12/2006"
                    + " introduits par la Loi du 25/08/2012 M.B."
                    + " 11/09/2012 présomption sectorielle de salariat"
                    + " applicable aux secteurs construction AR du"
                    + " 07/06/2013 transport de choses gardiennage"
                    + " nettoyage et agriculture horticulture AR du"
                    + " 29/10/2013 9 critères sectoriels présomption"
                    + " déclenchée si majorité simple 5 sur 9 critères"
                    + " indique le salariat ; Cass. BE 23/12/2002"
                    + " S.02.0021.F primauté de l'exécution réelle de"
                    + " la relation sur la qualification conventionnelle"
                    + " doctrine Bart Buysse consacrée ; Cass. BE"
                    + " 06/12/2010 S.10.0067.F caractère simple"
                    + " réfragable de la présomption art. 328 § 1"
                    + " renversable par tout moyen de preuve ; Cour"
                    + " const. arrêt 167/2013 du 19/12/2013 conformité"
                    + " de la liste sectorielle et de la présomption"
                    + " sectorielle art. 337/2 aux principes du procès"
                    + " équitable ; Cour const. arrêt 102/2009 du"
                    + " 18/06/2009 conformité de la présomption"
                    + " générale de salariat art. 328 § 1 ; Code pénal"
                    + " social art. 181 sanction niveau 4 défaut"
                    + " DIMONA cumulée en cas de requalification"
                    + " salariat voir SF-219-26 pour le chiffrage.";

    static final String AVERT_AVOCAT_BE =
            "L'outil d'analyse INASTI du statut travailleur indépendant"
                    + " produit un verdict indicatif fondé sur les"
                    + " déclarations de l'avocat. L'avocat spécialisé en"
                    + " droit social BE doit confirmer pour la situation"
                    + " considérée : (i) la qualification exacte des 4"
                    + " critères généraux art. 333 Loi-programme I du"
                    + " 27/12/2006 — volonté des parties telle"
                    + " qu'exprimée dans la convention écrite, liberté"
                    + " d'organisation du temps de travail observée"
                    + " concrètement, liberté d'organisation du travail,"
                    + " exercice d'un contrôle hiérarchique régulier ;"
                    + " (ii) l'application des critères sectoriels"
                    + " art. 337/2 § 1 si le secteur est construction"
                    + " transport de choses gardiennage nettoyage ou"
                    + " agriculture horticulture (9 critères : capital"
                    + " investi par le travailleur, risque financier"
                    + " personnel, responsabilité décisionnelle sur"
                    + " moyens, politique d'achat, politique de prix,"
                    + " obligation de résultat, possibilité d'engager du"
                    + " personnel, présentation comme entreprise face"
                    + " à des tiers, immeubles affectés à la profession) ;"
                    + " (iii) l'éventuelle requalification rétroactive"
                    + " et ses conséquences ONSS (cotisations rétroactives"
                    + " ~25 % employeur + 13,07 % travailleur, prescription"
                    + " quinquennale art. 42 Loi 27/06/1969 portée à 7 ans"
                    + " en cas d'intention frauduleuse) — voir SF-219-26"
                    + " pour le chiffrage exact ; (iv) la sanction pénale"
                    + " art. 181 C. pén. soc. niveau 4 cumulée en cas"
                    + " d'absence DIMONA (amende admin 300 à 3000 € ou"
                    + " pénale 600 à 6000 € ET/OU emprisonnement 6 mois"
                    + " à 3 ans, multipliée par le nombre de travailleurs"
                    + " art. 103 § 2, majorée × 5 personne morale art."
                    + " 105 § 1, doublement récidive art. 110) ; (v) le"
                    + " renversement éventuel de la présomption art. 328"
                    + " ou 337/2 par tout moyen de preuve (présomption"
                    + " simple, Cass. BE 06/12/2010 S.10.0067.F) ; (vi)"
                    + " l'éventuelle régularisation INASTI / ONSS via"
                    + " conciliation auprès de la Commission administrative"
                    + " de règlement de la relation de travail art. 329"
                    + " C. pén. soc. (procédure préventive permettant"
                    + " d'obtenir une décision sur la qualification avant"
                    + " litige) ; (vii) l'articulation avec les procédures"
                    + " connexes (cotisations rétroactives INASTI si"
                    + " requalification inverse, contentieux fiscal sur"
                    + " la nature des revenus art. 23 et s. CIR 1992,"
                    + " TVA art. 4 et s. C. TVA assujettissement de"
                    + " l'indépendant) ; (viii) les voies de recours"
                    + " tribunal du travail (art. 580 1° C. jud.) et"
                    + " action récursoire contre l'employeur en"
                    + " requalification salariat. La présente analyse"
                    + " constitue un éclairage indicatif et ne remplace"
                    + " pas la consultation d'un avocat compétent en"
                    + " droit social BE.";

    private InastriStatutTravailleurIndependantChecker() {
    }

    public static InastriStatutTravailleurIndependantResult analyze(
            InastriStatutTravailleurIndependantRequest request) {
        validateInputs(request);

        // ── Score critères généraux art. 333 ───────────────────────────────
        int scoreIndep = 0;
        int scoreSal = 0;

        switch (request.volonteParties()) {
            case INDEPENDANT_EXPLICITE -> scoreIndep++;
            case SALARIE_EXPLICITE -> scoreSal++;
            case IMPLICITE_OU_AMBIGUE -> { /* neutre */ }
        }

        scoreIndep += pointIndep(request.liberteOrganisationTemps());
        scoreSal += pointSal(request.liberteOrganisationTemps());

        scoreIndep += pointIndep(request.liberteOrganisationTravail());
        scoreSal += pointSal(request.liberteOrganisationTravail());

        // Contrôle hiérarchique : inversion logique
        // (TOTALE = contrôle total = salariat, AUCUNE = autonomie = indépendance)
        switch (request.controleHierarchique()) {
            case TOTALE_INDEPENDANT -> scoreSal++;
            case AUCUNE_SALARIE -> scoreIndep++;
            case PARTIELLE -> { /* neutre */ }
        }

        // ── Présomption sectorielle art. 337/2 ─────────────────────────────
        boolean presomptSectApplicable =
                request.secteur() != InastriStatutTravailleurIndependantSecteur.AUTRE;
        boolean presomptSectDeclenchee = presomptSectApplicable
                && request.nbCriteresSectorielsSalarie() >= SEUIL_PRESUMPTION_SECTORIELLE;

        // ── Présomption générale art. 328 § 1 ──────────────────────────────
        boolean presomptGenMobilisable = !request.dimonaPresente()
                && request.statutDeclare()
                        == InastriStatutTravailleurIndependantStatutDeclare.SANS_DECLARATION;

        // ── Drapeaux complémentaires ───────────────────────────────────────
        boolean monoClient = !request.autreClientPresent();
        boolean dimonaCoherente = isDimonaCoherente(
                request.statutDeclare(), request.dimonaPresente());

        // ── Verdict ────────────────────────────────────────────────────────
        if (presomptSectDeclenchee) {
            return buildSectorielle(request, scoreIndep, scoreSal,
                    presomptSectApplicable, presomptGenMobilisable,
                    dimonaCoherente, monoClient);
        }
        if (scoreSal > scoreIndep) {
            return buildSalarieRequalifie(request, scoreIndep, scoreSal,
                    presomptSectApplicable, presomptGenMobilisable,
                    dimonaCoherente, monoClient);
        }
        if (presomptGenMobilisable && scoreIndep <= scoreSal) {
            return buildPresomptionGenerale(request, scoreIndep, scoreSal,
                    presomptSectApplicable, dimonaCoherente, monoClient);
        }
        if (scoreIndep > scoreSal) {
            return buildIndependantConfirme(request, scoreIndep, scoreSal,
                    presomptSectApplicable, presomptGenMobilisable,
                    dimonaCoherente, monoClient);
        }
        return buildAQualifier(request, scoreIndep, scoreSal,
                presomptSectApplicable, presomptGenMobilisable,
                dimonaCoherente, monoClient);
    }

    // ── Branches verdict ────────────────────────────────────────────────────

    private static InastriStatutTravailleurIndependantResult buildSectorielle(
            InastriStatutTravailleurIndependantRequest req,
            int scoreI, int scoreS,
            boolean presomptSectApp, boolean presomptGen,
            boolean dimonaCoh, boolean mono) {
        String synthese = "La présomption sectorielle de salariat art."
                + " 337/2 § 1 Loi-programme I du 27/12/2006 est"
                + " DECLENCHEE : le secteur " + libelleSecteur(req.secteur())
                + " est visé par la liste sectorielle et "
                + req.nbCriteresSectorielsSalarie() + " critères sur 9"
                + " indiquent une relation salariée (seuil légal : 5/9)."
                + " La qualification d'indépendant est présumée renversée"
                + " — charge probatoire inversée à charge de la personne"
                + " qui invoque l'indépendance (présomption réfragable,"
                + " Cour const. arrêt 167/2013 du 19/12/2013). En"
                + " parallèle, les 4 critères généraux art. 333 cotent "
                + scoreI + " indépendance vs " + scoreS + " salariat."
                + " Conséquences en cas de requalification confirmée :"
                + " (a) rappel des cotisations ONSS rétroactives (~25 %"
                + " employeur + 13,07 % travailleur) sur la durée de"
                + " la relation, prescription quinquennale art. 42 Loi"
                + " 27/06/1969 (7 ans si fraude) ; (b) sanction pénale"
                + " art. 181 C. pén. soc. niveau 4 cumulée si la DIMONA"
                + " n'avait pas été effectuée ; (c) action éventuelle"
                + " du travailleur en paiement des arriérés salariaux"
                + " (Loi 12/04/1965) et indemnité de rupture (Loi"
                + " 03/07/1978). L'avocat peut demander la conciliation"
                + " auprès de la Commission administrative de règlement"
                + " de la relation de travail art. 329 C. pén. soc."
                + " pour obtenir une décision préventive sur la"
                + " qualification" + suffixeMonoClient(mono)
                + suffixeDimona(dimonaCoh, req.statutDeclare(),
                        req.dimonaPresente()) + ".";
        return build(req,
                InastriStatutTravailleurIndependantVerdict.FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE,
                scoreI, scoreS, presomptSectApp, true,
                presomptGen, true, dimonaCoh, mono,
                "PRESUMPTION_SECTORIELLE_DECLENCHEE_ART_337_2",
                synthese);
    }

    private static InastriStatutTravailleurIndependantResult buildSalarieRequalifie(
            InastriStatutTravailleurIndependantRequest req,
            int scoreI, int scoreS,
            boolean presomptSectApp, boolean presomptGen,
            boolean dimonaCoh, boolean mono) {
        String synthese = "La qualification d'indépendant doit être"
                + " écartée et la relation requalifiée en contrat de"
                + " travail salarié (Loi du 03/07/1978) : les 4 critères"
                + " généraux art. 333 Loi-programme I 27/12/2006 cotent "
                + scoreI + " indépendance vs " + scoreS + " salariat,"
                + " soit une majorité d'indices de subordination juridique."
                + " La doctrine Bart Buysse consacrée par Cass. BE"
                + " 23/12/2002 S.02.0021.F donne primauté à l'exécution"
                + " réelle de la relation sur la qualification"
                + " conventionnelle. Conséquences : (a) rappel des"
                + " cotisations ONSS rétroactives (~25 % employeur"
                + " + 13,07 % travailleur) sur la durée de la relation,"
                + " sous réserve de la prescription quinquennale"
                + " art. 42 Loi 27/06/1969 portée à 7 ans en cas"
                + " d'intention frauduleuse ; (b) amende ONSS"
                + " forfaitaire 3 × les cotisations dues art. 28 Loi"
                + " 22/04/2003 si absence DIMONA ; (c) sanction pénale"
                + " art. 181 C. pén. soc. niveau 4 cumulée si la DIMONA"
                + " n'avait pas été effectuée ; (d) action éventuelle"
                + " du travailleur en paiement des arriérés salariaux"
                + " Loi du 12/04/1965 et indemnité de rupture Loi du"
                + " 03/07/1978. La requalification peut être obtenue"
                + " par voie de conciliation auprès de la Commission"
                + " administrative de règlement de la relation de"
                + " travail art. 329 C. pén. soc., ou par voie"
                + " contentieuse devant le tribunal du travail"
                + " art. 580 1° C. jud" + suffixeMonoClient(mono)
                + suffixeDimona(dimonaCoh, req.statutDeclare(),
                        req.dimonaPresente()) + ".";
        return build(req,
                InastriStatutTravailleurIndependantVerdict.SALARIE_REQUALIFIE,
                scoreI, scoreS, presomptSectApp, false,
                presomptGen, true, dimonaCoh, mono,
                "CRITERES_GENERAUX_MAJORITAIRES_SALARIAT",
                synthese);
    }

    private static InastriStatutTravailleurIndependantResult buildPresomptionGenerale(
            InastriStatutTravailleurIndependantRequest req,
            int scoreI, int scoreS,
            boolean presomptSectApp, boolean dimonaCoh, boolean mono) {
        String synthese = "La présomption générale de salariat art."
                + " 328 § 1 C. pén. soc. est MOBILISABLE : aucune"
                + " DIMONA n'a été effectuée et aucune affiliation"
                + " INASTI n'est déclarée. Cette présomption est"
                + " simple — renversable par tout moyen de preuve"
                + " (Cass. BE 06/12/2010 S.10.0067.F). Les 4 critères"
                + " généraux art. 333 cotent " + scoreI + " indépendance"
                + " vs " + scoreS + " salariat — scores non concluants"
                + " en faveur de l'indépendance. L'auditeur du travail"
                + " ou l'ONSS peut, sur la base de cette présomption,"
                + " imposer un rappel rétroactif des cotisations ONSS"
                + " salariées et engager une sanction pénale art. 181"
                + " C. pén. soc. niveau 4 pour défaut DIMONA. La"
                + " charge probatoire est inversée : l'avocat doit"
                + " produire des éléments concrets caractérisant"
                + " l'absence de subordination (autonomie réelle,"
                + " pluralité de clients, risque financier personnel,"
                + " absence de contrôle hiérarchique, liberté"
                + " d'organisation du temps et du travail). À défaut,"
                + " la requalification salariat sera retenue avec ses"
                + " conséquences" + suffixeMonoClient(mono) + ".";
        return build(req,
                InastriStatutTravailleurIndependantVerdict.PRESUMPTION_GENERALE_SALARIAT,
                scoreI, scoreS, presomptSectApp, false,
                true, true, dimonaCoh, mono,
                "PRESUMPTION_GENERALE_ART_328_MOBILISABLE",
                synthese);
    }

    private static InastriStatutTravailleurIndependantResult buildIndependantConfirme(
            InastriStatutTravailleurIndependantRequest req,
            int scoreI, int scoreS,
            boolean presomptSectApp, boolean presomptGen,
            boolean dimonaCoh, boolean mono) {
        String synthese = "La qualification d'indépendant est CONFIRMEE :"
                + " les 4 critères généraux art. 333 Loi-programme I"
                + " 27/12/2006 cotent " + scoreI + " indépendance vs "
                + scoreS + " salariat. La relation est compatible avec"
                + " une activité d'indépendant économiquement autonome"
                + " (AR n° 38 du 27/07/1967 art. 3 § 1). L'affiliation"
                + " à une caisse d'assurances sociales pour travailleurs"
                + " indépendants et le paiement des cotisations"
                + " trimestrielles (AR du 19/12/1967) sont les"
                + " obligations principales. Rappel des obligations"
                + " connexes : déclaration TVA art. 53 C. TVA, déclaration"
                + " fiscale revenus d'indépendant art. 23 et s. CIR 1992,"
                + " inscription Banque-Carrefour des Entreprises art."
                + " III.49 C. droit éco. La qualification reste"
                + " susceptible d'être contestée par l'ONSS / l'auditeur"
                + " du travail si l'exécution effective de la relation"
                + " révèle des indices de subordination — l'outil ne"
                + " préjuge pas du résultat d'un éventuel contentieux"
                + suffixeMonoClient(mono)
                + suffixeDimona(dimonaCoh, req.statutDeclare(),
                        req.dimonaPresente()) + ".";
        return build(req,
                InastriStatutTravailleurIndependantVerdict.INDEPENDANT_CONFIRME,
                scoreI, scoreS, presomptSectApp, false,
                presomptGen, false, dimonaCoh, mono,
                "CRITERES_GENERAUX_MAJORITAIRES_INDEPENDANCE",
                synthese);
    }

    private static InastriStatutTravailleurIndependantResult buildAQualifier(
            InastriStatutTravailleurIndependantRequest req,
            int scoreI, int scoreS,
            boolean presomptSectApp, boolean presomptGen,
            boolean dimonaCoh, boolean mono) {
        String synthese = "Les 4 critères généraux art. 333"
                + " Loi-programme I 27/12/2006 cotent à égalité "
                + scoreI + " indépendance vs " + scoreS + " salariat —"
                + " la qualification ne peut être tranchée sur la base"
                + " des éléments fournis. L'avocat doit caractériser"
                + " concrètement chacun des 4 critères en se référant"
                + " à l'exécution effective de la relation (primauté"
                + " sur la qualification conventionnelle, Cass. BE"
                + " 23/12/2002 S.02.0021.F) et, le cas échéant, mobiliser"
                + " les critères sectoriels art. 337/2 § 1 si le secteur"
                + " est visé. L'outil recommande une analyse"
                + " complémentaire avant prise de position définitive."
                + " La voie de la conciliation auprès de la Commission"
                + " administrative de règlement de la relation de"
                + " travail art. 329 C. pén. soc. est une alternative"
                + " préventive permettant d'obtenir une décision"
                + " administrative sur la qualification"
                + suffixeMonoClient(mono)
                + suffixeDimona(dimonaCoh, req.statutDeclare(),
                        req.dimonaPresente()) + ".";
        return build(req,
                InastriStatutTravailleurIndependantVerdict.A_QUALIFIER,
                scoreI, scoreS, presomptSectApp, false,
                presomptGen, false, dimonaCoh, mono,
                "CRITERES_GENERAUX_NON_CONCLUANTS",
                synthese);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static int pointIndep(
            InastriStatutTravailleurIndependantLiberte l) {
        return l == InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT
                ? 1 : 0;
    }

    private static int pointSal(
            InastriStatutTravailleurIndependantLiberte l) {
        return l == InastriStatutTravailleurIndependantLiberte.AUCUNE_SALARIE
                ? 1 : 0;
    }

    private static String libelleSecteur(
            InastriStatutTravailleurIndependantSecteur s) {
        return switch (s) {
            case CONSTRUCTION -> "construction (AR du 07/06/2013)";
            case TRANSPORT_DE_CHOSES -> "transport de choses pour le"
                    + " compte de tiers (art. 337/2 § 1, 2°)";
            case GARDIENNAGE -> "gardiennage et sécurité privée"
                    + " (art. 337/2 § 1, 3°)";
            case NETTOYAGE -> "nettoyage (art. 337/2 § 1, 4°)";
            case AGRICULTURE_HORTICULTURE -> "agriculture / horticulture"
                    + " (AR du 29/10/2013, art. 337/2 § 1, 5°)";
            case AUTRE -> "non visé par la liste sectorielle";
        };
    }

    private static String suffixeMonoClient(boolean monoClient) {
        if (!monoClient) {
            return "";
        }
        return " ; l'absence d'autre client (mono-client) est un indice"
                + " supplémentaire de salariat déguisé (Cass. BE"
                + " 23/12/2002 S.02.0021.F — l'autonomie économique"
                + " suppose une pluralité réelle de relations"
                + " contractuelles)";
    }

    private static String suffixeDimona(
            boolean dimonaCoherente,
            InastriStatutTravailleurIndependantStatutDeclare statut,
            boolean dimonaPresente) {
        if (dimonaCoherente) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(" ; incohérence entre le statut déclaré (")
          .append(statut.name())
          .append(") et la présence de DIMONA (")
          .append(dimonaPresente ? "OUI" : "NON")
          .append(") — situation à clarifier auprès de l'ONSS / INASTI");
        return sb.toString();
    }

    private static boolean isDimonaCoherente(
            InastriStatutTravailleurIndependantStatutDeclare statut,
            boolean dimonaPresente) {
        return switch (statut) {
            case INDEPENDANT_INASTI -> !dimonaPresente;
            case SALARIE_DIMONA -> dimonaPresente;
            case SANS_DECLARATION -> !dimonaPresente;
        };
    }

    private static InastriStatutTravailleurIndependantResult build(
            InastriStatutTravailleurIndependantRequest req,
            InastriStatutTravailleurIndependantVerdict verdict,
            int scoreI, int scoreS,
            boolean presomptSectApp, boolean presomptSectDecl,
            boolean presomptGen, boolean requalif,
            boolean dimonaCoh, boolean mono,
            String raison, String synthese) {
        return new InastriStatutTravailleurIndependantResult(
                req.volonteParties(),
                req.liberteOrganisationTemps(),
                req.liberteOrganisationTravail(),
                req.controleHierarchique(),
                req.secteur(),
                req.nbCriteresSectorielsSalarie(),
                req.statutDeclare(),
                req.dimonaPresente(),
                req.autreClientPresent(),
                verdict,
                scoreI, scoreS,
                presomptSectApp, presomptSectDecl,
                presomptGen, requalif,
                dimonaCoh, mono,
                raison, synthese, BASE_JURIDIQUE, AVERT_AVOCAT_BE);
    }

    private static void validateInputs(
            InastriStatutTravailleurIndependantRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.volonteParties() == null) {
            throw new IllegalArgumentException(
                    "volonteParties est requis");
        }
        if (request.liberteOrganisationTemps() == null) {
            throw new IllegalArgumentException(
                    "liberteOrganisationTemps est requis");
        }
        if (request.liberteOrganisationTravail() == null) {
            throw new IllegalArgumentException(
                    "liberteOrganisationTravail est requis");
        }
        if (request.controleHierarchique() == null) {
            throw new IllegalArgumentException(
                    "controleHierarchique est requis");
        }
        if (request.secteur() == null) {
            throw new IllegalArgumentException("secteur est requis");
        }
        if (request.nbCriteresSectorielsSalarie() == null
                || request.nbCriteresSectorielsSalarie() < 0
                || request.nbCriteresSectorielsSalarie() > 9) {
            throw new IllegalArgumentException(
                    "nbCriteresSectorielsSalarie doit être entre 0 et 9");
        }
        if (request.statutDeclare() == null) {
            throw new IllegalArgumentException(
                    "statutDeclare est requis");
        }
        if (request.dimonaPresente() == null) {
            throw new IllegalArgumentException(
                    "dimonaPresente est requis");
        }
        if (request.autreClientPresent() == null) {
            throw new IllegalArgumentException(
                    "autreClientPresent est requis");
        }
    }
}
