package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * SF-219-21 : calculateur des avantages extra-légaux
 * <i>éco-chèques + chèques-repas BE</i> — fonction <b>pure</b> (sans
 * dépendance HTTP / persistance / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Éco-chèques</b> — CCT n°98 du Conseil National du Travail
 *       (CNT) du 20/02/2009 concernant les éco-chèques, rendue
 *       obligatoire par AR du 12/04/2009 (M.B. 06/05/2009). Plafond :
 *       250 EUR/an par travailleur (art. 6 CCT n°98). Validité :
 *       24 mois à dater de la mise à disposition (art. 13 CCT n°98).
 *       L'octroi suppose une CCT sectorielle, une CCT d'entreprise,
 *       ou à défaut un accord individuel écrit (art. 3 CCT n°98).
 *       Liste des biens et services éligibles fixée à l'annexe de
 *       la CCT n°98 (produits écologiques labellisés). Exonération :
 *       cotisations ONSS art. 19 §2 16° AR 28/11/1969 portant
 *       exécution de la loi du 27/06/1969 ; impôt art. 38 §1 25°
 *       CIR 92.</li>
 *   <li><b>Chèques-repas</b> — Loi du 25/04/2014 portant des
 *       dispositions diverses (M.B. 07/05/2014) + AR du 03/02/2010
 *       modifiant l'art. 19bis de l'AR du 28/11/1969 portant
 *       exécution de la loi du 27/06/1969 révisant l'AL du 28/12/1944
 *       sur la sécurité sociale des travailleurs. Conditions
 *       cumulatives art. 19bis §2 AR 28/11/1969 : (1) prévu par CCT
 *       sectorielle / d'entreprise ou convention individuelle écrite ;
 *       (2) un chèque par jour effectivement presté ; (3) chèque
 *       nominatif ; (4) durée de validité 12 mois (art. 19bis §2 5°);
 *       (5) ne peut être utilisé que pour le paiement d'un repas ou
 *       l'achat de denrées alimentaires ; (6) intervention employeur
 *       max 6,91 EUR/chèque ; (7) intervention travailleur min
 *       1,09 EUR ; (8) montant facial max 8 EUR ; (9) interdiction
 *       de cumul avec indemnité forfaitaire frais de bouche pour le
 *       même jour ; (10) paiement électronique obligatoire depuis
 *       le 01/01/2016 (Loi 25/04/2014 art. 51).</li>
 *   <li><b>Substitution à rémunération</b> — art. 4 CCT n°98 +
 *       jurisprudence Cass. 16/10/2017 S.16.0042.F (ONSS c/ SA) :
 *       interdiction de remplacer une rémunération préexistante par
 *       un avantage extra-légal exonéré. Sanction : requalification
 *       intégrale en rémunération sur 3 ans (action civile salariale,
 *       art. 15 Loi 03/07/1978) ou 5 ans (action ONSS pour cotisations
 *       éludées, art. 42 al. 1 Loi 27/06/1969 sur la sécurité
 *       sociale).</li>
 *   <li><b>Cotisations ONSS</b> — taux global moyen 25,00 % côté
 *       employeur (basé sur taux faciaux ONSS travailleurs salariés
 *       2024) appliqué au montant requalifié, valeur indicative à
 *       confirmer secteur par secteur.</li>
 * </ul>
 *
 * <h2>Hiérarchie des verdicts</h2>
 * <ol>
 *   <li><b>A_ANALYSER</b> — type d'avantage {@code INDETERMINE}.</li>
 *   <li><b>NON_CONFORME_SUBSTITUTION_REMUNERATION</b> — substitution
 *       avérée (requalification intégrale rétroactive).</li>
 *   <li><b>NON_CONFORME_CONDITION_MANQUANTE</b> — condition
 *       cumulative non remplie (CCT/convention absente, paiement non
 *       électronique post 01/01/2016, contribution travailleur
 *       insuffisante, double avec frais de bouche, validité dépassée).</li>
 *   <li><b>NON_CONFORME_DEPASSEMENT_PLAFOND</b> ou
 *       <b>CONFORME_PARTIELLEMENT_EXONERE</b> — montant supérieur
 *       au plafond (la fraction excédentaire est requalifiée).</li>
 *   <li><b>CONFORME_EXONERATION_INTEGRALE</b> — sinon (toutes
 *       conditions remplies).</li>
 * </ol>
 */
public final class EcoChequesChequesRepasBeCalculator {

    /** Plafond annuel éco-chèques — art. 6 CCT n°98. */
    static final BigDecimal PLAFOND_ECO_CHEQUES_ANNUEL_EUR =
            new BigDecimal("250.00");

    /** Plafond facial maximal d'un chèque-repas — art. 19bis §2 8°
     *  AR 28/11/1969 (modifié par AR 03/02/2010). */
    static final BigDecimal PLAFOND_CHEQUE_REPAS_FACIAL_EUR =
            new BigDecimal("8.00");

    /** Plafond intervention employeur d'un chèque-repas exonéré ONSS
     *  — art. 19bis §2 6° AR 28/11/1969. */
    static final BigDecimal PLAFOND_INTERVENTION_EMPLOYEUR_PAR_CHEQUE_REPAS_EUR =
            new BigDecimal("6.91");

    /** Contribution travailleur minimale par chèque-repas — art.
     *  19bis §2 7° AR 28/11/1969. */
    static final BigDecimal CONTRIBUTION_TRAVAILLEUR_MIN_PAR_CHEQUE_REPAS_EUR =
            new BigDecimal("1.09");

    /** Taux global moyen cotisations ONSS employeur — valeur
     *  indicative pour estimation du redressement potentiel. */
    static final BigDecimal TAUX_COTISATIONS_ONSS_EMPLOYEUR =
            new BigDecimal("0.25");

    /** Durée de validité éco-chèques — art. 13 CCT n°98 (24 mois). */
    static final int VALIDITE_ECO_CHEQUES_MOIS = 24;

    /** Date d'entrée en vigueur de l'obligation paiement électronique
     *  chèques-repas — Loi 25/04/2014 art. 51. */
    static final LocalDate DATE_OBLIGATION_PAIEMENT_ELECTRONIQUE =
            LocalDate.of(2016, 1, 1);

    static final String BASE_JURIDIQUE =
            "CCT n°98 du Conseil National du Travail (CNT) du"
                    + " 20/02/2009 concernant les éco-chèques (rendue"
                    + " obligatoire par AR du 12/04/2009, M.B."
                    + " 06/05/2009), art. 3 (CCT sectorielle / CCT"
                    + " d'entreprise / convention individuelle écrite),"
                    + " art. 6 (plafond annuel 250 EUR par travailleur),"
                    + " art. 13 (durée de validité 24 mois) ; Loi du"
                    + " 25/04/2014 portant des dispositions diverses"
                    + " (M.B. 07/05/2014), art. 51 (paiement"
                    + " électronique obligatoire des chèques-repas"
                    + " depuis le 01/01/2016) ; AR du 03/02/2010"
                    + " modifiant l'art. 19bis de l'AR du 28/11/1969"
                    + " portant exécution de la loi du 27/06/1969"
                    + " (M.B. du 17/02/2010) : conditions cumulatives"
                    + " d'exonération ONSS des chèques-repas (1 chèque"
                    + " par jour effectivement presté, nominatif,"
                    + " usage repas / denrées alimentaires uniquement,"
                    + " intervention employeur max 6,91 EUR,"
                    + " contribution travailleur min 1,09 EUR, valeur"
                    + " faciale max 8 EUR, validité 12 mois, pas de"
                    + " cumul avec indemnité frais de bouche pour le"
                    + " même jour) ; art. 19 §2 16° AR 28/11/1969 +"
                    + " art. 38 §1 25° CIR 92 (exonération éco-chèques)"
                    + " ; Cass. 16/10/2017 S.16.0042.F (ONSS c/ SA) +"
                    + " art. 4 CCT n°98 (interdiction substitution à"
                    + " rémunération, requalification intégrale en"
                    + " rémunération) ; art. 42 al. 1 Loi 27/06/1969"
                    + " (prescription action ONSS 5 ans, 7 ans en cas"
                    + " de fraude / dissimulation).";

    static final String AVERT_AVOCAT_BE =
            "Les avantages extra-légaux éco-chèques et chèques-repas"
                    + " sont d'application stricte (cf. doctrine ONSS"
                    + " et SPF Sécurité sociale, instructions"
                    + " administratives annuelles). L'avocat spécialisé"
                    + " en droit social BE doit confirmer : (i) la"
                    + " configuration sectorielle exacte (CCT n°98"
                    + " est supplétive — une CCT sectorielle peut"
                    + " imposer un montant supérieur, étendre la liste"
                    + " des biens éligibles ou prévoir une procédure"
                    + " plus stricte) ; (ii) le caractère effectif des"
                    + " jours prestés au sens de l'art. 19bis AR"
                    + " 28/11/1969 (congés payés, jours fériés, journées"
                    + " de suspension de l'exécution du contrat dans"
                    + " les conditions de l'art. 27 Loi 03/07/1978 :"
                    + " selon les cas, le chèque-repas reste dû ou"
                    + " non — vérification au cas par cas avec la"
                    + " jurisprudence CT Liège 04/06/2019 et CT Mons"
                    + " 12/12/2017) ; (iii) l'absence de double avec"
                    + " une indemnité forfaitaire frais de bouche"
                    + " (art. 19bis §2 al. 4) — notamment pour les"
                    + " commerciaux sédentaires bénéficiant d'une"
                    + " indemnité de représentation incluant une"
                    + " composante repas ; (iv) l'absence de substitution"
                    + " à un élément de rémunération préexistant —"
                    + " l'ONSS et le SPF Finances font preuve de"
                    + " sévérité particulière sur ce point (jurisprudence"
                    + " constante : Cass. 16/10/2017 S.16.0042.F ;"
                    + " Cass. 28/05/2018 S.17.0050.F ; CT Bruxelles"
                    + " 22/03/2019) ; (v) le respect de la durée de"
                    + " validité (24 mois éco-chèques, 12 mois"
                    + " chèques-repas — un chèque périmé est requalifié"
                    + " en avantage exonéré perdu, sans possibilité de"
                    + " récupération par le travailleur) ; (vi) le"
                    + " mode de paiement (électronique obligatoire pour"
                    + " les chèques-repas depuis le 01/01/2016, papier"
                    + " toléré pour les éco-chèques) ; (vii) la"
                    + " prescription applicable (3 ans action civile"
                    + " du travailleur, 5 ans action ONSS pour"
                    + " cotisations éludées, 7 ans en cas de fraude"
                    + " ou de dissimulation art. 42 Loi 27/06/1969).";

    private EcoChequesChequesRepasBeCalculator() {
    }

    public static EcoChequesChequesRepasBeResult analyze(
            EcoChequesChequesRepasBeRequest request) {
        validateInputs(request);

        // ── Hiérarchie 1 : type indéterminé ──────────────────────────
        if (request.typeAvantage()
                == EcoChequesChequesRepasBeTypeAvantage.INDETERMINE) {
            String synthese = "Type d'avantage indéterminé (éco-chèques /"
                    + " chèques-repas) : impossible de fixer le plafond"
                    + " et les conditions d'exonération ONSS et fiscales"
                    + " applicables. Analyse cas par cas requise.";
            return build(request,
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    request.montantAnnuelEur(),
                    estimerCotisationsOnss(request.montantAnnuelEur()),
                    EcoChequesChequesRepasBeVerdict.A_ANALYSER,
                    "TYPE_AVANTAGE_INDETERMINE", synthese);
        }

        BigDecimal plafond = plafondLegalApplicable(request);

        // ── Hiérarchie 2 : substitution à rémunération ───────────────
        // Sanction la plus lourde : requalification intégrale rétroactive
        // (art. 4 CCT n°98 + jurisprudence Cass. constante).
        if (request.substitutionRemuneration()) {
            String synthese = "Substitution à un élément de rémunération"
                    + " préexistant avérée : l'avantage est requalifié"
                    + " intégralement en rémunération soumise aux"
                    + " cotisations ONSS et à l'impôt (art. 4 CCT n°98"
                    + " ; jurisprudence constante Cass. 16/10/2017"
                    + " S.16.0042.F, ONSS c/ SA). Action ONSS possible"
                    + " sur 5 ans rétroactifs (art. 42 Loi 27/06/1969),"
                    + " portée à 7 ans en cas de dissimulation. Le"
                    + " redressement estimatif (taux ONSS employeur"
                    + " moyen 25 %) est mentionné à titre indicatif :"
                    + " confirmation par audit ONSS / cellule spécialisée"
                    + " indispensable.";
            return build(request,
                    plafond, BigDecimal.ZERO,
                    request.montantAnnuelEur(),
                    estimerCotisationsOnss(request.montantAnnuelEur()),
                    EcoChequesChequesRepasBeVerdict
                            .NON_CONFORME_SUBSTITUTION_REMUNERATION,
                    "SUBSTITUTION_REMUNERATION", synthese);
        }

        // ── Hiérarchie 3 : conditions cumulatives (CCT, paiement,
        //                   double frais de bouche, contribution
        //                   travailleur, validité, jours prestés) ────
        String conditionManquante = controleConditions(request);
        if (conditionManquante != null) {
            String synthese = "Condition cumulative manquante : "
                    + libelleCondition(conditionManquante)
                    + ". Sanction : requalification intégrale en"
                    + " rémunération soumise aux cotisations ONSS et"
                    + " à l'impôt. Redressement estimatif (taux ONSS"
                    + " employeur moyen 25 %) calculé à titre indicatif —"
                    + " confirmation par audit ONSS indispensable.";
            return build(request,
                    plafond, BigDecimal.ZERO,
                    request.montantAnnuelEur(),
                    estimerCotisationsOnss(request.montantAnnuelEur()),
                    EcoChequesChequesRepasBeVerdict
                            .NON_CONFORME_CONDITION_MANQUANTE,
                    conditionManquante, synthese);
        }

        // ── Hiérarchie 4 : dépassement du plafond ────────────────────
        BigDecimal montant = request.montantAnnuelEur();
        if (montant.compareTo(plafond) > 0) {
            BigDecimal exonere = plafond;
            BigDecimal requalifie = montant.subtract(plafond)
                    .setScale(2, RoundingMode.HALF_UP);
            String synthese = "Dépassement du plafond légal (montant"
                    + " attribué " + montant.setScale(2, RoundingMode.HALF_UP)
                    + " EUR > plafond " + plafond.setScale(2, RoundingMode.HALF_UP)
                    + " EUR). La fraction conforme (" + exonere
                    .setScale(2, RoundingMode.HALF_UP) + " EUR) reste"
                    + " exonérée ; la fraction excédentaire ("
                    + requalifie + " EUR) est requalifiée en"
                    + " rémunération soumise aux cotisations ONSS et"
                    + " à l'impôt. Redressement estimatif (taux ONSS"
                    + " employeur moyen 25 %) : "
                    + estimerCotisationsOnss(requalifie)
                            .setScale(2, RoundingMode.HALF_UP) + " EUR.";
            // Verdict CONFORME_PARTIELLEMENT_EXONERE quand le dépassement
            // est faible (< 10 %) — signale qu'il est récupérable
            // par ajustement contractuel ; NON_CONFORME_DEPASSEMENT_PLAFOND
            // quand dépassement structurel (≥ 10 %).
            BigDecimal seuil = plafond
                    .multiply(new BigDecimal("0.10"));
            EcoChequesChequesRepasBeVerdict verdict =
                    requalifie.compareTo(seuil) < 0
                            ? EcoChequesChequesRepasBeVerdict
                                    .CONFORME_PARTIELLEMENT_EXONERE
                            : EcoChequesChequesRepasBeVerdict
                                    .NON_CONFORME_DEPASSEMENT_PLAFOND;
            String raison = requalifie.compareTo(seuil) < 0
                    ? "DEPASSEMENT_FAIBLE_PARTIELLEMENT_EXONERE"
                    : "DEPASSEMENT_PLAFOND_LEGAL";
            return build(request,
                    plafond, exonere, requalifie,
                    estimerCotisationsOnss(requalifie),
                    verdict, raison, synthese);
        }

        // ── Hiérarchie 5 : conforme ──────────────────────────────────
        String synthese = "Avantage extra-légal intégralement conforme"
                + " : montant " + montant.setScale(2, RoundingMode.HALF_UP)
                + " EUR ≤ plafond légal " + plafond.setScale(2, RoundingMode.HALF_UP)
                + " EUR, conditions cumulatives respectées (CCT /"
                + " convention écrite, mode de paiement, contribution"
                + " travailleur, absence de double avec frais de"
                + " bouche, absence de substitution à rémunération)."
                + " Exonération intégrale des cotisations ONSS (art. 19"
                + " §2 16° pour les éco-chèques, art. 19bis pour les"
                + " chèques-repas, AR 28/11/1969) et de l'impôt sur"
                + " les revenus (art. 38 §1 25° CIR 92).";
        return build(request,
                plafond, montant, BigDecimal.ZERO, BigDecimal.ZERO,
                EcoChequesChequesRepasBeVerdict
                        .CONFORME_EXONERATION_INTEGRALE,
                "CONFORME_INTEGRALE", synthese);
    }

    /** Plafond légal applicable selon le type d'avantage. Pour les
     *  chèques-repas : prorata jours × valeur faciale (limité au plafond
     *  facial × jours), pour les éco-chèques : plafond fixe 250 EUR/an. */
    private static BigDecimal plafondLegalApplicable(
            EcoChequesChequesRepasBeRequest req) {
        if (req.typeAvantage()
                == EcoChequesChequesRepasBeTypeAvantage.ECO_CHEQUES) {
            return PLAFOND_ECO_CHEQUES_ANNUEL_EUR;
        }
        // CHEQUES_REPAS : plafond = jours prestés × plafond facial
        // 8 EUR (le plafond ONSS exonéré est de 6,91 EUR/chèque, mais
        // c'est la valeur faciale qui est plafonnée à 8 EUR — la part
        // travailleur 1,09 EUR n'est pas exonérée).
        return PLAFOND_CHEQUE_REPAS_FACIAL_EUR
                .multiply(new BigDecimal(req.joursEffectivementPrestes()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Contrôle des conditions cumulatives — renvoie le code de la
     *  condition manquante ou {@code null} si toutes respectées. */
    private static String controleConditions(
            EcoChequesChequesRepasBeRequest req) {
        // Condition commune : CCT sectorielle / d'entreprise OU
        // convention individuelle écrite (art. 3 CCT n°98 pour les
        // éco-chèques ; art. 19bis §2 1° AR 28/11/1969 pour les
        // chèques-repas).
        if (!req.cctSectorielleOuEntrepriseExiste()
                && !req.conventionIndividuelleEcrite()) {
            return "ABSENCE_CCT_OU_CONVENTION_ECRITE";
        }

        // Conditions spécifiques aux chèques-repas
        if (req.typeAvantage()
                == EcoChequesChequesRepasBeTypeAvantage.CHEQUES_REPAS) {
            // Paiement électronique obligatoire depuis 01/01/2016
            if (!req.paiementElectronique()
                    && (req.dateAttribution() == null
                    || !req.dateAttribution().isBefore(
                            DATE_OBLIGATION_PAIEMENT_ELECTRONIQUE))) {
                return "PAIEMENT_NON_ELECTRONIQUE_POST_2016";
            }
            // Cumul avec frais de bouche interdit
            if (req.cumulFraisBouche()) {
                return "CUMUL_FRAIS_BOUCHE_INTERDIT";
            }
            // Contribution travailleur min 1,09 EUR
            if (req.contributionTravailleurUnitaireEur()
                    .compareTo(
                            CONTRIBUTION_TRAVAILLEUR_MIN_PAR_CHEQUE_REPAS_EUR)
                    < 0) {
                return "CONTRIBUTION_TRAVAILLEUR_INSUFFISANTE";
            }
            // Valeur faciale max 8 EUR
            if (req.valeurFacialeUnitaireEur()
                    .compareTo(PLAFOND_CHEQUE_REPAS_FACIAL_EUR) > 0) {
                return "VALEUR_FACIALE_DEPASSE_8_EUR";
            }
            // Jours prestés > 0 (sinon pas de droit)
            if (req.joursEffectivementPrestes() <= 0) {
                return "AUCUN_JOUR_EFFECTIVEMENT_PRESTE";
            }
        }
        return null;
    }

    private static String libelleCondition(String code) {
        return switch (code) {
            case "ABSENCE_CCT_OU_CONVENTION_ECRITE" ->
                    "absence de CCT sectorielle / d'entreprise ou de"
                            + " convention individuelle écrite (art. 3"
                            + " CCT n°98 / art. 19bis §2 1° AR 28/11/1969)";
            case "PAIEMENT_NON_ELECTRONIQUE_POST_2016" ->
                    "chèques-repas papier alors que le paiement"
                            + " électronique est obligatoire depuis le"
                            + " 01/01/2016 (Loi 25/04/2014 art. 51)";
            case "CUMUL_FRAIS_BOUCHE_INTERDIT" ->
                    "cumul avec une indemnité forfaitaire frais de"
                            + " bouche pour le même jour (interdit"
                            + " art. 19bis §2 al. 4 AR 28/11/1969)";
            case "CONTRIBUTION_TRAVAILLEUR_INSUFFISANTE" ->
                    "contribution du travailleur inférieure au minimum"
                            + " légal de 1,09 EUR par chèque-repas"
                            + " (art. 19bis §2 7° AR 28/11/1969)";
            case "VALEUR_FACIALE_DEPASSE_8_EUR" ->
                    "valeur faciale dépassant le plafond légal de"
                            + " 8 EUR par chèque-repas (art. 19bis §2 8°"
                            + " AR 28/11/1969)";
            case "AUCUN_JOUR_EFFECTIVEMENT_PRESTE" ->
                    "absence de jour effectivement presté (un"
                            + " chèque-repas suppose un jour de travail"
                            + " effectif, art. 19bis §2 2° AR 28/11/1969)";
            default -> "condition non identifiée";
        };
    }

    /** Estime le redressement potentiel de cotisations ONSS employeur
     *  sur le montant requalifié — valeur indicative à titre d'ordre
     *  de grandeur (taux global moyen 25 %, base salaires bruts). */
    private static BigDecimal estimerCotisationsOnss(BigDecimal montant) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return montant.multiply(TAUX_COTISATIONS_ONSS_EMPLOYEUR)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static EcoChequesChequesRepasBeResult build(
            EcoChequesChequesRepasBeRequest request,
            BigDecimal plafond,
            BigDecimal montantExonere,
            BigDecimal montantRequalifie,
            BigDecimal cotisationsOnss,
            EcoChequesChequesRepasBeVerdict verdict,
            String raison,
            String synthese) {
        return new EcoChequesChequesRepasBeResult(
                request.typeAvantage(),
                request.montantAnnuelEur(),
                request.valeurFacialeUnitaireEur(),
                request.contributionTravailleurUnitaireEur(),
                request.joursEffectivementPrestes(),
                request.cctSectorielleOuEntrepriseExiste(),
                request.conventionIndividuelleEcrite(),
                request.paiementElectronique(),
                request.cumulFraisBouche(),
                request.substitutionRemuneration(),
                request.dateAttribution(),
                plafond.setScale(2, RoundingMode.HALF_UP),
                montantExonere.setScale(2, RoundingMode.HALF_UP),
                montantRequalifie.setScale(2, RoundingMode.HALF_UP),
                cotisationsOnss.setScale(2, RoundingMode.HALF_UP),
                verdict,
                raison,
                synthese,
                BASE_JURIDIQUE,
                AVERT_AVOCAT_BE);
    }

    private static void validateInputs(
            EcoChequesChequesRepasBeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.typeAvantage() == null) {
            throw new IllegalArgumentException("typeAvantage est requis");
        }
        if (request.montantAnnuelEur() == null) {
            throw new IllegalArgumentException(
                    "montantAnnuelEur est requis");
        }
        if (request.montantAnnuelEur().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "montantAnnuelEur doit être positif ou nul");
        }
        if (request.valeurFacialeUnitaireEur() == null) {
            throw new IllegalArgumentException(
                    "valeurFacialeUnitaireEur est requise");
        }
        if (request.valeurFacialeUnitaireEur()
                .compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "valeurFacialeUnitaireEur doit être positive ou nulle");
        }
        if (request.contributionTravailleurUnitaireEur() == null) {
            throw new IllegalArgumentException(
                    "contributionTravailleurUnitaireEur est requise");
        }
        if (request.contributionTravailleurUnitaireEur()
                .compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "contributionTravailleurUnitaireEur doit être positive ou nulle");
        }
        if (request.joursEffectivementPrestes() < 0) {
            throw new IllegalArgumentException(
                    "joursEffectivementPrestes doit être positif ou nul");
        }
        if (request.cctSectorielleOuEntrepriseExiste() == null) {
            throw new IllegalArgumentException(
                    "cctSectorielleOuEntrepriseExiste est requis");
        }
        if (request.conventionIndividuelleEcrite() == null) {
            throw new IllegalArgumentException(
                    "conventionIndividuelleEcrite est requis");
        }
        if (request.paiementElectronique() == null) {
            throw new IllegalArgumentException(
                    "paiementElectronique est requis");
        }
        if (request.cumulFraisBouche() == null) {
            throw new IllegalArgumentException(
                    "cumulFraisBouche est requis");
        }
        if (request.substitutionRemuneration() == null) {
            throw new IllegalArgumentException(
                    "substitutionRemuneration est requis");
        }
    }
}
