package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-219-12 : vérificateur de l'éligibilité au régime <i>flexi-job
 * BE</i> — fonction <b>pure</b> (sans dépendance HTTP / persistance /
 * horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi-programme du 26 décembre 2013</b> (M.B. 31/12/2013),
 *       articles 13 à 28 — instauration du régime flexi-job pour le
 *       secteur HoReCa (CP 302).</li>
 *   <li><b>Loi du 16 novembre 2015</b> portant des dispositions
 *       diverses en matière sociale — extension boulangerie /
 *       coiffure (validation Cour const. arrêt 107/2017).</li>
 *   <li><b>Loi-programme du 30 octobre 2018</b> — extension commerce
 *       de détail (CP 201/202/311/312), agriculture (CP 132/144/145),
 *       soins de santé partiels + suppression du plafond cumul pour
 *       les pensionnés.</li>
 *   <li><b>Loi-programme du 28 décembre 2023</b> + <b>AR du 02/06/2024</b>
 *       — extension sport / culture / événementiel / déménagement /
 *       garages / transport autocar / assurances limité, indexation
 *       du flexi-salaire minimum, hausse du plafond annuel exonéré.</li>
 *   <li>Loi du 25/04/2014 portant des dispositions diverses en matière
 *       de sécurité sociale — corrections techniques (Dimona FLX,
 *       CITSS).</li>
 * </ul>
 *
 * <h2>Hiérarchie des verdicts</h2>
 * <ol>
 *   <li><b>INELIGIBLE_TRAVAILLEUR_HORS_CONDITION</b> — ni pensionné, ni
 *       4/5 ETP T-3 (sortie immédiate).</li>
 *   <li><b>INELIGIBLE_SECTEUR_NON_ELIGIBLE</b> — l'employeur n'opère pas
 *       dans un secteur ouvert (sortie immédiate).</li>
 *   <li><b>A_ANALYSER</b> — secteur en zone grise (extension récente
 *       sans AR d'application connu).</li>
 *   <li><b>INELIGIBLE_CUMUL_INTERDIT_MEME_EMPLOYEUR</b> — déjà occupé
 *       chez le même employeur (sortie immédiate après filtre 1 et 2).</li>
 *   <li><b>FRAGILE_CONTRAT_OU_DIMONA_MANQUANT</b> — conditions de fond
 *       OK mais contrat-cadre absent ou Dimona FLX non déclarée.</li>
 *   <li><b>FRAGILE_PLAFOND_DEPASSE</b> — flexi-salaire sous minimum OU
 *       revenu cumulé annuel ≥ plafond exonéré (sauf pensionné).</li>
 *   <li>Sinon : <b>ELIGIBLE</b> — occupation flexi-job régulière.</li>
 * </ol>
 *
 * <h2>Spécificité pension</h2>
 * <p>Depuis l'extension de 2018, les <b>pensionnés</b> bénéficient
 * d'une absence de plafond annuel : le test {@code revenu ≥ plafond}
 * est désactivé pour cette catégorie (la rémunération demeure non
 * soumise à l'impôt sur les revenus jusqu'à concurrence du plafond
 * indexé, le solde restant exonéré pour le pensionné — paramètre
 * fiscal traité hors outil).</p>
 */
public final class FlexiJobBeChecker {

    static final String BASE_JURIDIQUE =
            "Loi-programme du 26 décembre 2013 (M.B. 31/12/2013), art. 13"
                    + " à 28 — régime initial HoReCa (CP 302) ; Loi du"
                    + " 25/04/2014 (dispositions diverses sécurité sociale) ;"
                    + " Loi du 16/11/2015 (extension boulangerie / coiffure,"
                    + " validée par Cour const. arrêt 107/2017) ;"
                    + " Loi-programme du 30/10/2018 (extension commerce"
                    + " détail, agriculture, soins de santé, suppression"
                    + " plafond cumul pensionnés) ; Loi-programme du"
                    + " 28/12/2023 + AR du 02/06/2024 (extension secteurs"
                    + " 2024 : sport, culture, événementiel, secteur"
                    + " public limité, indexation flexi-salaire et"
                    + " plafond annuel). Dimona spécifique FLX (DmfA"
                    + " ONSS) obligatoire ; contrat-cadre écrit"
                    + " obligatoire (art. 16 Loi-prog. 26/12/2013).";

    static final String AVERT_AVOCAT_BE =
            "Le périmètre exact des commissions paritaires ouvertes au"
                    + " flexi-job et les valeurs indexées (flexi-salaire"
                    + " minimum, plafond annuel exonéré) sont à confirmer"
                    + " par un avocat spécialisé en droit social BE pour"
                    + " la date d'occupation considérée. En cas d'irrégularité"
                    + " (contrat-cadre absent, Dimona FLX manquante, secteur"
                    + " non ouvert), l'ONSS requalifie l'occupation au"
                    + " régime normal avec récupération des cotisations"
                    + " patronales et personnelles + intérêts + sanctions"
                    + " administratives. La part de rémunération au-delà"
                    + " du plafond annuel exonéré (hors pensionnés) reste"
                    + " soumise à l'impôt sur les revenus.";

    private FlexiJobBeChecker() {
    }

    public static FlexiJobBeResult analyze(FlexiJobBeRequest request) {
        validateInputs(request);

        // ── Hiérarchie 1 : travailleur hors condition ──────────────────────
        boolean travailleurEligible =
                request.statutTravailleur() == FlexiJobBeStatutTravailleur.PENSIONNE
                        || request.statutTravailleur()
                                == FlexiJobBeStatutTravailleur.SALARIE_4_5_TEMPS_T_MOINS_3;
        if (!travailleurEligible) {
            return ineligible(request,
                    FlexiJobBeVerdict.INELIGIBLE_TRAVAILLEUR_HORS_CONDITION,
                    "TRAVAILLEUR_HORS_CONDITION",
                    "Le travailleur n'est ni pensionné ni salarié à 4/5 ETP"
                            + " minimum chez un autre employeur au cours du"
                            + " 3e trimestre civil précédant l'occupation"
                            + " (T-3). Il ne remplit donc aucune des deux"
                            + " conditions personnelles d'accès au régime"
                            + " flexi-job (Loi-programme 26/12/2013, art. 13).",
                    false, false, true, false, false);
        }

        // ── Hiérarchie 2 : secteur non éligible ────────────────────────────
        if (request.secteurEmployeur()
                == FlexiJobBeSecteurEmployeur.AUTRE_NON_ELIGIBLE) {
            return ineligible(request,
                    FlexiJobBeVerdict.INELIGIBLE_SECTEUR_NON_ELIGIBLE,
                    "SECTEUR_NON_ELIGIBLE",
                    "L'employeur n'opère pas dans un secteur listé par les"
                            + " lois-programmes successives (HoReCa 2013 ;"
                            + " boulangerie/coiffure 2015 ; commerce détail,"
                            + " agriculture, soins de santé 2018 ; sport,"
                            + " culture, événementiel, secteur public limité"
                            + " 2023). Le régime flexi-job n'est pas ouvert"
                            + " à ce secteur.",
                    true, false, true, false, false);
        }

        // ── Hiérarchie 3 : zone grise extension récente ─────────────────────
        if (request.secteurEmployeur()
                == FlexiJobBeSecteurEmployeur.ZONE_GRISE_EXTENSION_RECENTE) {
            return ineligible(request,
                    FlexiJobBeVerdict.A_ANALYSER,
                    "SECTEUR_ZONE_GRISE",
                    "Le secteur invoqué relève d'une extension récente sans"
                            + " AR d'application clairement identifié ou"
                            + " d'une commission paritaire ambiguë. Une"
                            + " analyse juridique pointue est requise"
                            + " (avocat BE spécialisé droit social) avant"
                            + " de conclure sur l'applicabilité du régime"
                            + " flexi-job.",
                    true, false, true, false, false);
        }

        // ── Hiérarchie 4 : cumul interdit même employeur ───────────────────
        if (request.dejaOccupeChezMemeEmployeur()) {
            return ineligible(request,
                    FlexiJobBeVerdict.INELIGIBLE_CUMUL_INTERDIT_MEME_EMPLOYEUR,
                    "CUMUL_INTERDIT_MEME_EMPLOYEUR",
                    "Le travailleur est déjà occupé chez ce même employeur"
                            + " dans le cadre d'un contrat ordinaire. Le"
                            + " cumul avec une occupation flexi-job chez"
                            + " le même employeur est interdit (règle"
                            + " anti-substitution destinée à empêcher la"
                            + " conversion d'heures normales en heures"
                            + " flexi pour échapper aux cotisations"
                            + " ordinaires — Loi-programme 26/12/2013,"
                            + " art. 13 § 2).",
                    true, true, false, false, false);
        }

        // ── Évaluation formalisme + rémunération ───────────────────────────
        boolean formalismeRespecte = request.contratCadreSigne()
                && request.dimonaFlxDeclaree();

        boolean salaireConforme = request.flexiSalaireHoraireBrut()
                .compareTo(request.flexiSalaireMinimumApplicable()) >= 0;

        // Plafond non opposable aux pensionnés depuis ext. 2018
        boolean estPensionne = request.statutTravailleur()
                == FlexiJobBeStatutTravailleur.PENSIONNE;
        BigDecimal excedent = request.revenuAnnuelFlexiCumule()
                .subtract(request.plafondAnnuelExonere());
        boolean plafondDepasse = !estPensionne
                && excedent.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal montantExcedentaire = plafondDepasse
                ? excedent
                : BigDecimal.ZERO;

        boolean remunerationConforme = salaireConforme && !plafondDepasse;

        // ── Hiérarchie 5 : formalisme défaillant ───────────────────────────
        if (!formalismeRespecte) {
            String quoi = !request.contratCadreSigne()
                    && !request.dimonaFlxDeclaree()
                    ? "contrat-cadre écrit absent ET déclaration Dimona FLX"
                            + " non effectuée"
                    : !request.contratCadreSigne()
                            ? "contrat-cadre écrit absent (art. 16"
                                    + " Loi-prog. 26/12/2013)"
                            : "déclaration Dimona spécifique FLX (DmfA"
                                    + " ONSS) non effectuée";
            String synthese = "Statut flexi-job formellement irrégulier : "
                    + quoi + ". Risque de requalification ONSS au régime"
                    + " ordinaire avec récupération de l'intégralité des"
                    + " cotisations patronales et personnelles (~37 % + ~13 %"
                    + " selon CP), intérêts moratoires et sanctions"
                    + " administratives. À régulariser sans délai avant"
                    + " contrôle ONSS / inspection sociale.";
            return new FlexiJobBeResult(
                    request.datePremiereOccupationFlexi(),
                    request.statutTravailleur(),
                    request.secteurEmployeur(),
                    request.dejaOccupeChezMemeEmployeur(),
                    request.contratCadreSigne(),
                    request.dimonaFlxDeclaree(),
                    request.flexiSalaireHoraireBrut(),
                    request.flexiSalaireMinimumApplicable(),
                    request.revenuAnnuelFlexiCumule(),
                    request.plafondAnnuelExonere(),
                    FlexiJobBeVerdict.FRAGILE_CONTRAT_OU_DIMONA_MANQUANT,
                    true, true, true, false, remunerationConforme,
                    montantExcedentaire,
                    "CONTRAT_OU_DIMONA_MANQUANT",
                    synthese,
                    BASE_JURIDIQUE,
                    AVERT_AVOCAT_BE);
        }

        // ── Hiérarchie 6 : plafond / flexi-salaire défaillant ──────────────
        if (!remunerationConforme) {
            String diag;
            if (!salaireConforme && plafondDepasse) {
                diag = "flexi-salaire horaire brut ("
                        + request.flexiSalaireHoraireBrut()
                        + " €) sous le minimum légal applicable ("
                        + request.flexiSalaireMinimumApplicable()
                        + " €) ET revenu annuel cumulé ("
                        + request.revenuAnnuelFlexiCumule()
                        + " €) au-delà du plafond exonéré ("
                        + request.plafondAnnuelExonere()
                        + " €) — excédent : " + montantExcedentaire + " €";
            } else if (!salaireConforme) {
                diag = "flexi-salaire horaire brut ("
                        + request.flexiSalaireHoraireBrut()
                        + " €) sous le minimum légal indexé ("
                        + request.flexiSalaireMinimumApplicable() + " €)";
            } else {
                diag = "revenu annuel flexi cumulé ("
                        + request.revenuAnnuelFlexiCumule()
                        + " €) au-delà du plafond exonéré ("
                        + request.plafondAnnuelExonere()
                        + " €) — excédent imposable : " + montantExcedentaire
                        + " €";
            }
            String synthese = "Conditions de fond et formalisme OK, mais"
                    + " rémunération non conforme : " + diag + ". La partie"
                    + " irrégulière est soumise à cotisations sociales"
                    + " ordinaires + impôt sur les revenus selon le cas (le"
                    + " plafond annuel exonéré ne s'applique pas aux"
                    + " pensionnés depuis l'extension 2018).";
            return new FlexiJobBeResult(
                    request.datePremiereOccupationFlexi(),
                    request.statutTravailleur(),
                    request.secteurEmployeur(),
                    request.dejaOccupeChezMemeEmployeur(),
                    request.contratCadreSigne(),
                    request.dimonaFlxDeclaree(),
                    request.flexiSalaireHoraireBrut(),
                    request.flexiSalaireMinimumApplicable(),
                    request.revenuAnnuelFlexiCumule(),
                    request.plafondAnnuelExonere(),
                    FlexiJobBeVerdict.FRAGILE_PLAFOND_DEPASSE,
                    true, true, true, true, false,
                    montantExcedentaire,
                    "PLAFOND_OU_SALAIRE_DEFAILLANT",
                    synthese,
                    BASE_JURIDIQUE,
                    AVERT_AVOCAT_BE);
        }

        // ── Cas qualifiant : flexi-job pleinement éligible ─────────────────
        String synthese = "Occupation flexi-job pleinement régulière au sens"
                + " de la Loi-programme 26/12/2013 (et extensions"
                + " 2015/2018/2023) : travailleur " + libelleStatut(
                        request.statutTravailleur()) + ", secteur "
                + libelleSecteur(request.secteurEmployeur()) + ", contrat-"
                + "cadre écrit signé, Dimona FLX déclarée, flexi-salaire"
                + " horaire (" + request.flexiSalaireHoraireBrut()
                + " €) ≥ minimum légal ("
                + request.flexiSalaireMinimumApplicable() + " €), revenu"
                + " annuel cumulé (" + request.revenuAnnuelFlexiCumule()
                + " €) " + (estPensionne ? "(plafond inapplicable —"
                        + " pensionné)" : "≤ plafond exonéré ("
                        + request.plafondAnnuelExonere() + " €)") + ".";

        return new FlexiJobBeResult(
                request.datePremiereOccupationFlexi(),
                request.statutTravailleur(),
                request.secteurEmployeur(),
                request.dejaOccupeChezMemeEmployeur(),
                request.contratCadreSigne(),
                request.dimonaFlxDeclaree(),
                request.flexiSalaireHoraireBrut(),
                request.flexiSalaireMinimumApplicable(),
                request.revenuAnnuelFlexiCumule(),
                request.plafondAnnuelExonere(),
                FlexiJobBeVerdict.ELIGIBLE,
                true, true, true, true, true,
                montantExcedentaire,
                "FLEXI_JOB_REGULIER",
                synthese,
                BASE_JURIDIQUE,
                AVERT_AVOCAT_BE);
    }

    // ── Construction d'un résultat inéligible commun ───────────────────────

    private static FlexiJobBeResult ineligible(
            FlexiJobBeRequest request,
            FlexiJobBeVerdict verdict,
            String raison,
            String synthese,
            boolean travailleurEligible,
            boolean secteurEligible,
            boolean cumulRespecte,
            boolean formalismeRespecte,
            boolean remunerationConforme) {
        return new FlexiJobBeResult(
                request.datePremiereOccupationFlexi(),
                request.statutTravailleur(),
                request.secteurEmployeur(),
                request.dejaOccupeChezMemeEmployeur(),
                request.contratCadreSigne(),
                request.dimonaFlxDeclaree(),
                request.flexiSalaireHoraireBrut(),
                request.flexiSalaireMinimumApplicable(),
                request.revenuAnnuelFlexiCumule(),
                request.plafondAnnuelExonere(),
                verdict,
                travailleurEligible,
                secteurEligible,
                cumulRespecte,
                formalismeRespecte,
                remunerationConforme,
                BigDecimal.ZERO,
                raison,
                synthese,
                BASE_JURIDIQUE,
                AVERT_AVOCAT_BE);
    }

    private static String libelleStatut(FlexiJobBeStatutTravailleur s) {
        return switch (s) {
            case PENSIONNE -> "pensionné (cumul sans plafond depuis 2018)";
            case SALARIE_4_5_TEMPS_T_MOINS_3 -> "salarié ≥ 4/5 ETP au T-3";
            case AUTRE -> "hors catégorie";
        };
    }

    private static String libelleSecteur(FlexiJobBeSecteurEmployeur s) {
        return switch (s) {
            case HORECA_302 -> "HoReCa (CP 302)";
            case BOULANGERIE_COIFFURE -> "boulangerie / coiffure (ext. 2015)";
            case COMMERCE_DETAIL -> "commerce de détail (ext. 2018)";
            case AGRICULTURE -> "agriculture (ext. 2018)";
            case SOINS_DE_SANTE -> "soins de santé partiels (ext. 2018)";
            case SECTEUR_PUBLIC -> "secteur public limité (ext. 2023)";
            case SPORT_CULTURE_EVENEMENTIEL -> "sport / culture /"
                    + " événementiel (ext. 2023)";
            case AUTRE_NON_ELIGIBLE -> "secteur non listé";
            case ZONE_GRISE_EXTENSION_RECENTE -> "zone grise extension";
        };
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    private static void validateInputs(FlexiJobBeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.datePremiereOccupationFlexi() == null) {
            throw new IllegalArgumentException(
                    "datePremiereOccupationFlexi est requise");
        }
        if (request.statutTravailleur() == null) {
            throw new IllegalArgumentException(
                    "statutTravailleur est requis");
        }
        if (request.secteurEmployeur() == null) {
            throw new IllegalArgumentException(
                    "secteurEmployeur est requis");
        }
        if (request.dejaOccupeChezMemeEmployeur() == null) {
            throw new IllegalArgumentException(
                    "dejaOccupeChezMemeEmployeur est requis");
        }
        if (request.contratCadreSigne() == null) {
            throw new IllegalArgumentException(
                    "contratCadreSigne est requis");
        }
        if (request.dimonaFlxDeclaree() == null) {
            throw new IllegalArgumentException(
                    "dimonaFlxDeclaree est requis");
        }
        if (request.flexiSalaireHoraireBrut() == null) {
            throw new IllegalArgumentException(
                    "flexiSalaireHoraireBrut est requis");
        }
        if (request.flexiSalaireHoraireBrut()
                .compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "flexiSalaireHoraireBrut doit être positif ou nul");
        }
        if (request.flexiSalaireMinimumApplicable() == null) {
            throw new IllegalArgumentException(
                    "flexiSalaireMinimumApplicable est requis");
        }
        if (request.flexiSalaireMinimumApplicable()
                .compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "flexiSalaireMinimumApplicable doit être strictement"
                            + " positif");
        }
        if (request.revenuAnnuelFlexiCumule() == null) {
            throw new IllegalArgumentException(
                    "revenuAnnuelFlexiCumule est requis");
        }
        if (request.revenuAnnuelFlexiCumule()
                .compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "revenuAnnuelFlexiCumule doit être positif ou nul");
        }
        if (request.plafondAnnuelExonere() == null) {
            throw new IllegalArgumentException(
                    "plafondAnnuelExonere est requis");
        }
        if (request.plafondAnnuelExonere()
                .compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "plafondAnnuelExonere doit être strictement positif");
        }
    }
}
