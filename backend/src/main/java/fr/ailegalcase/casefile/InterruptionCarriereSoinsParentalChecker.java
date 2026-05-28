package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * SF-219-32 : analyseur d'éligibilité au <b>congé parental BE</b>
 * (interruption de carrière pour soins parental) — fonction <b>pure</b>
 * (sans dépendance HTTP / persistance / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi de redressement du 22/01/1985</b> M.B. 24/01/1985
 *       contenant des dispositions sociales — <b>art. 99 à 107quater</b>
 *       régime général de l'interruption de carrière professionnelle
 *       dans le secteur privé ; <b>art. 101</b> protection contre le
 *       licenciement depuis le jour de la demande jusqu'à 3 mois
 *       après la fin de l'interruption, indemnité 6 mois rémunération
 *       brute en cas de licenciement irrégulier.</li>
 *   <li><b>AR du 29/10/1997</b> M.B. 07/11/1997 introduisant un droit
 *       au congé parental dans le cadre d'une interruption de la
 *       carrière professionnelle :
 *       <ul>
 *         <li><b>art. 2</b> — droit individuel de 4 mois équivalent
 *             temps plein par enfant pour chaque travailleur et par
 *             parent.</li>
 *         <li><b>art. 3</b> — formes : interruption complète temps
 *             plein 4 mois, réduction de moitié 8 mois, réduction
 *             d'un cinquième 20 mois.</li>
 *         <li><b>art. 4</b> — conditions enfant : moins de 12 ans à la
 *             date du début, ou moins de 21 ans si handicap physique
 *             mental ou sensoriel ≥ 66%.</li>
 *         <li><b>art. 5</b> — ancienneté de 12 mois auprès du même
 *             employeur dans la période de 15 mois précédant la
 *             demande.</li>
 *         <li><b>art. 6</b> — formalisme : notification 2 à 3 mois
 *             avant le début souhaité, par lettre recommandée ou
 *             remise en main propre contre accusé de réception.</li>
 *         <li><b>art. 7</b> — droit de l'employeur de différer
 *             l'interruption maximum 6 mois pour raisons
 *             organisationnelles motivées et notifiées dans le mois.</li>
 *       </ul>
 *   </li>
 *   <li><b>CCT n° 64 du 29/04/1997</b> (rendue obligatoire AR
 *       29/10/1997) — conclue au sein du CNT établissant un droit au
 *       congé parental, art. 3 à 5 droit du travailleur d'opter pour
 *       la forme la plus adaptée.</li>
 *   <li><b>AR du 12/08/1991</b> M.B. 31/08/1991 portant exécution
 *       chapitre IV section 5 Loi 22/01/1985 — allocations
 *       d'interruption à charge de l'ONEM, <b>art. 4</b> montants
 *       forfaitaires mensuels selon la forme (montants indexés
 *       annuellement par arrêté ministériel).</li>
 *   <li><b>Cass. BE 26/05/2008 S.07.0040.F</b> — protection licenciement
 *       art. 101 Loi 22/01/1985 opposable même si la demande n'a pas
 *       été notifiée dans le strict délai dès lors qu'elle a été
 *       acceptée ou consommée par l'employeur.</li>
 *   <li><b>Cass. BE 22/06/2009 S.08.0102.N</b> — interprétation du
 *       motif grave et des raisons étrangères à la demande pour la
 *       levée de la protection.</li>
 *   <li><b>Cass. BE 16/01/2017 S.15.0102.N</b> — cumul congé parental
 *       et allocation ONEM de droit dès que les conditions sont
 *       remplies, refus employeur ou ONEM constitutif d'une atteinte
 *       au droit subjectif de l'art. 99 Loi 22/01/1985.</li>
 * </ul>
 *
 * <h2>Logique du qualificateur</h2>
 * <ol>
 *   <li>Vérification ancienneté ≥ 12 mois (art. 5 AR 29/10/1997).</li>
 *   <li>Vérification âge enfant : &lt; 12 ans (144 mois) ou &lt; 21 ans
 *       (252 mois) si handicap (art. 4 AR 29/10/1997).</li>
 *   <li>Vérification solde individuel restant ≥ 4 mois ETP (art. 2).</li>
 *   <li>Vérification formalisme : délai 2 à 3 mois avant le début et
 *       mode lettre recommandée ou remise en main (art. 6).</li>
 *   <li>Si employeur a différé motivé → DIFFERE_EMPLOYEUR (art. 7).</li>
 *   <li>Sinon → ELIGIBLE_COMPLET (toutes conditions OK) ou
 *       ELIGIBLE_AVEC_RESERVES (formalisme limite).</li>
 * </ol>
 *
 * <h2>Calculs</h2>
 * <ul>
 *   <li>Durée effective : TEMPS_PLEIN 4 mois, MI_TEMPS 8 mois,
 *       CINQUIEME_TEMPS 20 mois (AR 29/10/1997 art. 3).</li>
 *   <li>Allocation ONEM mensuelle forfaitaire (AR 12/08/1991 art. 4) :
 *       TEMPS_PLEIN ≈ 250 €, MI_TEMPS ≈ 350 €, CINQUIEME_TEMPS ≈
 *       450 € — montants V1 indicatifs à confirmer par avocat selon
 *       indexation en vigueur.</li>
 *   <li>Date de fin = date début + durée mois.</li>
 *   <li>Fin protection licenciement = date fin + 3 mois (art. 101 Loi
 *       22/01/1985).</li>
 *   <li>Indemnité protection = 6 × rémunération mensuelle brute
 *       (art. 101 Loi 22/01/1985).</li>
 * </ul>
 */
public final class InterruptionCarriereSoinsParentalChecker {

    /** Ancienneté minimale en mois (art. 5 AR 29/10/1997). */
    static final int ANCIENNETE_MIN_MOIS = 12;

    /** Plafond d'âge enfant en mois — 12 ans (art. 4 AR 29/10/1997). */
    static final int AGE_PLAFOND_MOIS = 12 * 12;

    /** Plafond d'âge enfant handicapé en mois — 21 ans (art. 4 al. 2). */
    static final int AGE_PLAFOND_HANDICAP_MOIS = 21 * 12;

    /** Droit individuel en mois ETP par enfant et par parent (art. 2). */
    static final int DROIT_INDIVIDUEL_MOIS_ETP = 4;

    /** Délai minimum notification avant début (art. 6 AR 29/10/1997). */
    static final int DELAI_NOTIF_MIN_JOURS = 60;

    /** Délai maximum notification avant début (art. 6 AR 29/10/1997). */
    static final int DELAI_NOTIF_MAX_JOURS = 90;

    /** Durée protection post-fin en mois (art. 101 Loi 22/01/1985). */
    static final int PROTECTION_POST_FIN_MOIS = 3;

    /** Multiplicateur indemnité protection — 6 mois (art. 101). */
    static final int INDEMNITE_PROTECTION_MOIS = 6;

    /** Allocation ONEM forfaitaire TEMPS_PLEIN — montant V1 indicatif. */
    static final BigDecimal ALLOCATION_TEMPS_PLEIN =
            new BigDecimal("250.00");

    /** Allocation ONEM forfaitaire MI_TEMPS — montant V1 indicatif. */
    static final BigDecimal ALLOCATION_MI_TEMPS =
            new BigDecimal("350.00");

    /** Allocation ONEM forfaitaire CINQUIEME_TEMPS — montant V1 indicatif. */
    static final BigDecimal ALLOCATION_CINQUIEME_TEMPS =
            new BigDecimal("450.00");

    /** Durée TEMPS_PLEIN en mois (AR 29/10/1997 art. 3). */
    static final int DUREE_TEMPS_PLEIN_MOIS = 4;

    /** Durée MI_TEMPS en mois (AR 29/10/1997 art. 3). */
    static final int DUREE_MI_TEMPS_MOIS = 8;

    /** Durée CINQUIEME_TEMPS en mois (AR 12/12/2001 art. 1). */
    static final int DUREE_CINQUIEME_TEMPS_MOIS = 20;

    static final String BASE_JURIDIQUE =
            "Loi de redressement du 22/01/1985 M.B. 24/01/1985 contenant"
                    + " des dispositions sociales art. 99 à 107quater"
                    + " régime général de l'interruption de carrière"
                    + " professionnelle dans le secteur privé art. 101"
                    + " protection contre le licenciement depuis le jour"
                    + " de la demande écrite jusqu'à trois mois après la"
                    + " fin de l'interruption sauf motif grave ou raisons"
                    + " étrangères à la demande indemnité de protection"
                    + " égale à six mois de rémunération brute en cas de"
                    + " licenciement irrégulier ; AR du 29/10/1997 M.B."
                    + " 07/11/1997 relatif à l'introduction d'un droit au"
                    + " congé parental dans le cadre d'une interruption"
                    + " de la carrière professionnelle art. 2 droit"
                    + " individuel de quatre mois équivalent temps plein"
                    + " par enfant pour chaque travailleur et par parent"
                    + " art. 3 formes interruption complète temps plein"
                    + " quatre mois ou réduction de moitié huit mois ou"
                    + " réduction d'un cinquième vingt mois art. 4"
                    + " conditions enfant moins de douze ans à la date"
                    + " du début de l'interruption ou moins de vingt et"
                    + " un ans si handicap physique mental ou sensoriel"
                    + " de soixante-six pour cent et plus art. 5"
                    + " ancienneté de douze mois auprès du même employeur"
                    + " dans la période de quinze mois précédant la"
                    + " demande art. 6 formalisme demande écrite notifiée"
                    + " à l'employeur deux à trois mois avant le début"
                    + " souhaité par lettre recommandée ou remise en main"
                    + " propre contre accusé de réception art. 7 droit de"
                    + " l'employeur de différer l'interruption maximum"
                    + " six mois pour des raisons organisationnelles"
                    + " motivées et notifiées dans le mois ; AR du"
                    + " 12/12/2001 M.B. 18/01/2002 modifiant l'AR du"
                    + " 29/10/1997 introduction du cinquième temps comme"
                    + " forme reconnue ; AR du 14/04/2009 M.B. 27/04/2009"
                    + " portant extension du droit au congé parental aux"
                    + " enfants de moins de douze ans ; AR du 05/05/2019"
                    + " M.B. 22/05/2019 modifiant l'AR du 12/12/2001"
                    + " introduction de la forme un dixième quarante mois"
                    + " sur autorisation employeur uniquement hors scope"
                    + " V1 ; CCT n° 64 du 29/04/1997 rendue obligatoire"
                    + " par AR du 29/10/1997 M.B. 07/11/1997 conclue au"
                    + " sein du Conseil National du Travail établissant"
                    + " un droit au congé parental dispositions"
                    + " complémentaires articles 3 à 5 droit du"
                    + " travailleur d'opter pour la forme la plus"
                    + " adaptée ; AR du 12/08/1991 M.B. 31/08/1991 portant"
                    + " exécution chapitre IV section 5 Loi de"
                    + " redressement du 22/01/1985 réglant les allocations"
                    + " d'interruption à charge de l'ONEM art. 4 montant"
                    + " forfaitaire mensuel selon la forme temps plein"
                    + " environ deux cent cinquante euros mi-temps"
                    + " environ trois cent cinquante euros cinquième"
                    + " environ quatre cent cinquante euros montants"
                    + " indexés annuellement par arrêté ministériel ;"
                    + " Cass. BE 26/05/2008 S.07.0040.F protection"
                    + " licenciement art. 101 Loi 22/01/1985 opposable"
                    + " même si la demande n'a pas été notifiée dans le"
                    + " strict délai dès lors qu'elle a été acceptée ou"
                    + " consommée par l'employeur ; Cass. BE 22/06/2009"
                    + " S.08.0102.N interprétation du motif grave et des"
                    + " raisons étrangères à la demande pour la levée de"
                    + " la protection ; Cass. BE 16/01/2017 S.15.0102.N"
                    + " cumul congé parental et allocation ONEM de droit"
                    + " dès lors que les conditions sont remplies refus"
                    + " employeur ou ONEM constitutif d'une atteinte au"
                    + " droit subjectif de l'art. 99 Loi 22/01/1985.";

    static final String AVERT_AVOCAT_BE =
            "L'outil d'analyse d'interruption de carrière pour congé"
                    + " parental BE produit un verdict indicatif fondé"
                    + " sur les déclarations de l'avocat sur les"
                    + " conditions d'éligibilité au regard de l'AR du"
                    + " 29/10/1997 et de la Loi du 22/01/1985 art. 99 à"
                    + " 107quater. L'outil ne se substitue PAS aux"
                    + " formulaires officiels ONEM C61 demande"
                    + " d'allocations d'interruption ni aux demandes"
                    + " spécifiques que le travailleur doit déposer"
                    + " auprès de son employeur et de l'ONEM. Les"
                    + " montants forfaitaires d'allocation ONEM affichés"
                    + " sont des ordres de grandeur V1 indicatifs (deux"
                    + " cent cinquante euros temps plein, trois cent"
                    + " cinquante euros mi-temps, quatre cent cinquante"
                    + " euros cinquième), à confirmer selon l'indexation"
                    + " annuelle en vigueur et le profil du travailleur"
                    + " majoration éventuelle pour les travailleurs de"
                    + " plus de cinquante ans ou les familles"
                    + " monoparentales. L'avocat spécialisé en droit"
                    + " social BE doit confirmer pour la situation"
                    + " considérée : (i) la qualité de travailleur du"
                    + " secteur privé soumis à la Loi du 22/01/1985 (les"
                    + " agents statutaires de la fonction publique"
                    + " relèvent d'un régime distinct) ; (ii)"
                    + " l'ancienneté effective douze mois auprès du même"
                    + " employeur dans la période de quinze mois"
                    + " précédant la demande art. 5 AR 29/10/1997 ;"
                    + " (iii) l'âge effectif de l'enfant à la date du"
                    + " début de l'interruption (moins de douze ans ou"
                    + " moins de vingt et un ans en cas de handicap"
                    + " physique mental ou sensoriel de soixante-six pour"
                    + " cent et plus art. 4 AR 29/10/1997) ; (iv) le"
                    + " solde restant du droit individuel quatre mois ETP"
                    + " par enfant et par parent art. 2 AR 29/10/1997 en"
                    + " tenant compte des consommations antérieures sous"
                    + " toutes les formes ; (v) la conformité du"
                    + " formalisme de la demande lettre recommandée ou"
                    + " remise en main propre contre accusé de réception"
                    + " deux à trois mois avant le début souhaité art. 6"
                    + " AR 29/10/1997 ; (vi) le déclenchement effectif"
                    + " de la protection contre le licenciement art. 101"
                    + " Loi 22/01/1985 depuis le jour de la demande"
                    + " écrite jusqu'à trois mois après la fin de"
                    + " l'interruption et le calcul de l'indemnité six"
                    + " mois rémunération brute en cas de licenciement"
                    + " irrégulier ; (vii) la possibilité pour l'employeur"
                    + " de différer l'interruption maximum six mois pour"
                    + " des raisons organisationnelles motivées art. 7"
                    + " AR 29/10/1997 et la procédure de notification du"
                    + " différé dans le mois ; (viii) le cumul avec les"
                    + " allocations d'interruption ONEM forfaitaires AR"
                    + " 12/08/1991 art. 4 et les démarches déclaration"
                    + " C61 dans les deux mois du début de"
                    + " l'interruption ; (ix) l'articulation avec le"
                    + " crédit-temps CCT 103 outil F-DT-29 credit-temps-be"
                    + " distinct du congé parental car régime universel"
                    + " sans motif spécifique et la possibilité de"
                    + " cumuler ou de basculer entre les deux régimes ;"
                    + " (x) le régime spécifique des familles"
                    + " monoparentales et des parents isolés bénéficiant"
                    + " d'une majoration d'allocation et d'une durée"
                    + " étendue selon la composition du ménage. La"
                    + " présente analyse constitue un éclairage indicatif"
                    + " et ne remplace pas la consultation d'un avocat"
                    + " compétent en droit social BE.";

    private InterruptionCarriereSoinsParentalChecker() {
    }

    public static InterruptionCarriereSoinsParentalResult analyze(
            InterruptionCarriereSoinsParentalRequest request) {
        validateInputs(request);

        boolean ancienneteOk = request.ancienneteMois() >= ANCIENNETE_MIN_MOIS;
        boolean ageEnfantOk = isAgeEnfantOk(request);
        boolean formalismeOk = isFormalismeOk(request);

        int dureeMois = dureeForForme(request.forme());
        int soldeApres = request.soldeRestantMoisEtp()
                - DROIT_INDIVIDUEL_MOIS_ETP;
        BigDecimal allocMensuelle =
                request.cumulAllocationOnemDemande()
                        ? allocationMensuelle(request.forme())
                        : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal allocTotale = allocMensuelle
                .multiply(BigDecimal.valueOf(dureeMois))
                .setScale(2, RoundingMode.HALF_UP);

        LocalDate dateFin = request.dateDebutInterruption() == null
                ? null
                : request.dateDebutInterruption().plusMonths(dureeMois);
        LocalDate finProtection = dateFin == null
                ? null
                : dateFin.plusMonths(PROTECTION_POST_FIN_MOIS);
        boolean protectionActive = request.dateDebutInterruption() != null;

        BigDecimal indemniteProtection =
                request.remunerationMensuelleBrute() == null
                        ? null
                        : request.remunerationMensuelleBrute()
                                .multiply(BigDecimal.valueOf(
                                        INDEMNITE_PROTECTION_MOIS))
                                .setScale(2, RoundingMode.HALF_UP);

        // 1) Priorité absolue : ancienneté insuffisante
        if (!ancienneteOk) {
            return buildAnciennete(request, ancienneteOk, ageEnfantOk,
                    formalismeOk, dureeMois, soldeApres, allocMensuelle,
                    allocTotale, dateFin, finProtection, protectionActive,
                    indemniteProtection);
        }

        // 2) Âge enfant hors plafond
        if (!ageEnfantOk) {
            return buildAgeEnfant(request, ancienneteOk, ageEnfantOk,
                    formalismeOk, dureeMois, soldeApres, allocMensuelle,
                    allocTotale, dateFin, finProtection, protectionActive,
                    indemniteProtection);
        }

        // 3) Solde insuffisant
        if (request.soldeRestantMoisEtp() < DROIT_INDIVIDUEL_MOIS_ETP) {
            return buildSoldeInsuffisant(request, ancienneteOk, ageEnfantOk,
                    formalismeOk, dureeMois, soldeApres, allocMensuelle,
                    allocTotale, dateFin, finProtection, protectionActive,
                    indemniteProtection);
        }

        // 4) Formalisme non conforme
        if (!formalismeOk) {
            return buildFormalisme(request, ancienneteOk, ageEnfantOk,
                    formalismeOk, dureeMois, soldeApres, allocMensuelle,
                    allocTotale, dateFin, finProtection, protectionActive,
                    indemniteProtection);
        }

        // 5) Différé employeur
        if (request.employeurAdiffere()) {
            return buildDiffereEmployeur(request, ancienneteOk, ageEnfantOk,
                    formalismeOk, dureeMois, soldeApres, allocMensuelle,
                    allocTotale, dateFin, finProtection, protectionActive,
                    indemniteProtection);
        }

        // 6) Éligible — avec ou sans réserves
        if (!request.employeurAccepte()
                || hasReservesFormalismeLimite(request)) {
            return buildEligibleAvecReserves(request, ancienneteOk,
                    ageEnfantOk, formalismeOk, dureeMois, soldeApres,
                    allocMensuelle, allocTotale, dateFin, finProtection,
                    protectionActive, indemniteProtection);
        }

        return buildEligibleComplet(request, ancienneteOk, ageEnfantOk,
                formalismeOk, dureeMois, soldeApres, allocMensuelle,
                allocTotale, dateFin, finProtection, protectionActive,
                indemniteProtection);
    }

    // ── Branches verdict ────────────────────────────────────────────────────

    private static InterruptionCarriereSoinsParentalResult buildAnciennete(
            InterruptionCarriereSoinsParentalRequest req,
            boolean ancienneteOk, boolean ageEnfantOk, boolean formalismeOk,
            int dureeMois, int soldeApres,
            BigDecimal allocMensuelle, BigDecimal allocTotale,
            LocalDate dateFin, LocalDate finProtection,
            boolean protectionActive, BigDecimal indemniteProtection) {
        String synthese = "INELIGIBLE — ancienneté insuffisante. Le"
                + " travailleur compte " + req.ancienneteMois() + " mois"
                + " auprès de l'employeur actuel, alors que l'art. 5 de"
                + " l'AR du 29/10/1997 exige au minimum douze mois dans"
                + " la période de quinze mois précédant la demande de"
                + " congé parental. La demande sera refusée tant par"
                + " l'employeur que par l'ONEM (formulaire C61 rejeté"
                + " pour défaut d'ancienneté). Points d'attention pour"
                + " l'avocat : (a) vérifier la computation exacte de"
                + " l'ancienneté en cumulant tous les contrats successifs"
                + " interruptions de courte durée éventuelles transferts"
                + " art. 7 CCT 32bis cession conventionnelle d'entreprise"
                + " ou Loi du 21/03/1991 cession publique ; (b) attendre"
                + " l'atteinte du seuil pour redéposer la demande ; (c)"
                + " envisager le crédit-temps CCT 103 outil F-DT-29 qui"
                + " exige une ancienneté différente (deux ans général /"
                + " cinq ans pour le motif soins) selon le motif."
                + " L'éligibilité doit être recalculée à la date prévue"
                + " du début de l'interruption — non à la date de la"
                + " demande.";
        return build(req,
                InterruptionCarriereSoinsParentalVerdict.INELIGIBLE_ANCIENNETE,
                ancienneteOk, ageEnfantOk, formalismeOk, dureeMois,
                soldeApres, allocMensuelle, allocTotale, dateFin,
                finProtection, protectionActive, indemniteProtection,
                "ANCIENNETE_INFERIEURE_DOUZE_MOIS_ART_5_AR_29_10_1997",
                synthese);
    }

    private static InterruptionCarriereSoinsParentalResult buildAgeEnfant(
            InterruptionCarriereSoinsParentalRequest req,
            boolean ancienneteOk, boolean ageEnfantOk, boolean formalismeOk,
            int dureeMois, int soldeApres,
            BigDecimal allocMensuelle, BigDecimal allocTotale,
            LocalDate dateFin, LocalDate finProtection,
            boolean protectionActive, BigDecimal indemniteProtection) {
        String plafond = req.enfantHandicap()
                ? "vingt et un ans (252 mois) pour un enfant en situation"
                        + " de handicap physique mental ou sensoriel de"
                        + " soixante-six pour cent et plus"
                : "douze ans (144 mois) pour un enfant sans handicap"
                        + " reconnu";
        String synthese = "INELIGIBLE — enfant hors plafond d'âge. L'enfant"
                + " a " + req.ageEnfantMois() + " mois (environ "
                + (req.ageEnfantMois() / 12) + " ans), alors que l'art."
                + " 4 de l'AR du 29/10/1997 limite le droit au congé"
                + " parental à " + plafond + ". La demande sera refusée"
                + " par l'employeur et l'ONEM. Points d'attention pour"
                + " l'avocat : (a) vérifier la date exacte du début"
                + " souhaité de l'interruption — la condition d'âge"
                + " s'apprécie à cette date (et non à la date de la"
                + " demande) ; (b) en cas d'enfant en situation de"
                + " handicap, vérifier la reconnaissance officielle du"
                + " taux de soixante-six pour cent par le SPF Sécurité"
                + " Sociale Direction générale Personnes handicapées"
                + " (DGPH) ou par la mutuelle ; (c) envisager d'autres"
                + " formes d'aide aux familles (congé pour assistance"
                + " médicale CCT 64 ; congé pour soins palliatifs"
                + " art. 100bis Loi 22/01/1985 ; aide aux aidants"
                + " proches Loi du 12/05/2014 si applicable).";
        return build(req,
                InterruptionCarriereSoinsParentalVerdict.INELIGIBLE_AGE_ENFANT,
                ancienneteOk, ageEnfantOk, formalismeOk, dureeMois,
                soldeApres, allocMensuelle, allocTotale, dateFin,
                finProtection, protectionActive, indemniteProtection,
                "AGE_ENFANT_HORS_PLAFOND_ART_4_AR_29_10_1997",
                synthese);
    }

    private static InterruptionCarriereSoinsParentalResult buildSoldeInsuffisant(
            InterruptionCarriereSoinsParentalRequest req,
            boolean ancienneteOk, boolean ageEnfantOk, boolean formalismeOk,
            int dureeMois, int soldeApres,
            BigDecimal allocMensuelle, BigDecimal allocTotale,
            LocalDate dateFin, LocalDate finProtection,
            boolean protectionActive, BigDecimal indemniteProtection) {
        String synthese = "INELIGIBLE — solde individuel insuffisant. Le"
                + " travailleur a déjà consommé tout ou partie de son"
                + " droit individuel de quatre mois ETP par enfant et"
                + " par parent (art. 2 AR 29/10/1997). Solde restant : "
                + req.soldeRestantMoisEtp() + " mois ETP, alors que la"
                + " demande actuelle nécessite quatre mois ETP. Points"
                + " d'attention pour l'avocat : (a) vérifier le décompte"
                + " ONEM des périodes de congé parental antérieures"
                + " toutes formes confondues (temps plein, mi-temps,"
                + " cinquième) ; (b) le droit est individuel et par"
                + " enfant — un autre enfant ouvre un nouveau droit de"
                + " quatre mois ETP ; (c) le droit est individuel et par"
                + " parent — l'autre parent (conjoint cohabitant légal"
                + " ou non) conserve son propre droit de quatre mois ETP"
                + " ; (d) envisager le crédit-temps CCT 103 outil F-DT-29"
                + " comme alternative (régime universel sans plafond par"
                + " enfant) ; (e) pour les soins d'un enfant gravement"
                + " malade ou handicapé, envisager le congé pour"
                + " assistance médicale art. 100bis Loi 22/01/1985 régime"
                + " distinct.";
        return build(req,
                InterruptionCarriereSoinsParentalVerdict.INELIGIBLE_SOLDE_INSUFFISANT,
                ancienneteOk, ageEnfantOk, formalismeOk, dureeMois,
                soldeApres, allocMensuelle, allocTotale, dateFin,
                finProtection, protectionActive, indemniteProtection,
                "SOLDE_INDIVIDUEL_INSUFFISANT_ART_2_AR_29_10_1997",
                synthese);
    }

    private static InterruptionCarriereSoinsParentalResult buildFormalisme(
            InterruptionCarriereSoinsParentalRequest req,
            boolean ancienneteOk, boolean ageEnfantOk, boolean formalismeOk,
            int dureeMois, int soldeApres,
            BigDecimal allocMensuelle, BigDecimal allocTotale,
            LocalDate dateFin, LocalDate finProtection,
            boolean protectionActive, BigDecimal indemniteProtection) {
        StringBuilder defauts = new StringBuilder();
        if (req.modeNotification()
                == InterruptionCarriereSoinsParentalModeNotification.AUTRE) {
            defauts.append(" mode de notification non conforme art. 6 AR"
                    + " 29/10/1997 (la demande doit être faite par lettre"
                    + " recommandée ou remise en main propre contre"
                    + " accusé de réception daté et signé) ;");
        }
        if (req.dateNotification() != null
                && req.dateDebutInterruption() != null) {
            long jours = ChronoUnit.DAYS.between(req.dateNotification(),
                    req.dateDebutInterruption());
            if (jours < DELAI_NOTIF_MIN_JOURS) {
                defauts.append(" délai de notification trop court (")
                        .append(jours)
                        .append(" jours) — l'art. 6 AR 29/10/1997 impose"
                                + " une notification au minimum deux mois"
                                + " avant le début souhaité ;");
            } else if (jours > DELAI_NOTIF_MAX_JOURS) {
                defauts.append(" délai de notification trop long (")
                        .append(jours)
                        .append(" jours) — l'art. 6 AR 29/10/1997 impose"
                                + " une notification au maximum trois"
                                + " mois avant le début souhaité ;");
            }
        }
        String synthese = "INELIGIBLE — formalisme non conforme :"
                + defauts + " Conséquences pratiques : (a) l'employeur"
                + " peut refuser la demande ou en demander la"
                + " régularisation, repoussant d'autant la date de début"
                + " effective ; (b) la fenêtre de protection contre le"
                + " licenciement art. 101 Loi 22/01/1985 peut être"
                + " fragilisée si l'irrégularité est soulevée par"
                + " l'employeur — la jurisprudence Cass. BE 26/05/2008"
                + " S.07.0040.F admet toutefois que la protection"
                + " s'applique dès lors que la demande a été acceptée ou"
                + " consommée par l'employeur ; (c) l'ONEM peut refuser"
                + " l'allocation C61 pour défaut de demande conforme."
                + " Action requise : régulariser la demande par lettre"
                + " recommandée dans le délai légal deux à trois mois"
                + " avant le début souhaité.";
        return build(req,
                InterruptionCarriereSoinsParentalVerdict.INELIGIBLE_FORMALISME,
                ancienneteOk, ageEnfantOk, formalismeOk, dureeMois,
                soldeApres, allocMensuelle, allocTotale, dateFin,
                finProtection, protectionActive, indemniteProtection,
                "FORMALISME_NON_CONFORME_ART_6_AR_29_10_1997",
                synthese);
    }

    private static InterruptionCarriereSoinsParentalResult buildDiffereEmployeur(
            InterruptionCarriereSoinsParentalRequest req,
            boolean ancienneteOk, boolean ageEnfantOk, boolean formalismeOk,
            int dureeMois, int soldeApres,
            BigDecimal allocMensuelle, BigDecimal allocTotale,
            LocalDate dateFin, LocalDate finProtection,
            boolean protectionActive, BigDecimal indemniteProtection) {
        String synthese = "DEMANDE DIFFEREE PAR L'EMPLOYEUR — l'employeur"
                + " a notifié dans le mois (art. 7 AR 29/10/1997) un"
                + " différé motivé par des raisons organisationnelles"
                + " (besoin du service, remplacement difficile,"
                + " surcharge). Durée maximale du différé : six mois à"
                + " compter de la date de début initialement demandée."
                + " La demande reste valide et le droit au congé parental"
                + " n'est PAS perdu — il est simplement reporté. Points"
                + " d'attention pour l'avocat : (a) vérifier la"
                + " motivation effective du différé — le motif doit être"
                + " sérieux et étayé (art. 7 al. 2 AR 29/10/1997) — un"
                + " différé non motivé ou abusif peut être contesté"
                + " devant le tribunal du travail art. 578 C. jud. ; (b)"
                + " vérifier le respect du délai d'un mois pour la"
                + " notification du différé — passé ce délai, l'employeur"
                + " est réputé avoir accepté la demande au sens initial ;"
                + " (c) la protection contre le licenciement art. 101"
                + " Loi 22/01/1985 court dès la demande initiale, même"
                + " en cas de différé, jusqu'à trois mois après la fin"
                + " effective de l'interruption ; (d) si l'employeur abuse"
                + " du différé pour faire pression sur le travailleur,"
                + " envisager une action en cessation art. 588 C. jud. ;"
                + " (e) recalculer la fenêtre exacte du report en"
                + " cumulant la durée du différé avec la durée initiale"
                + " demandée.";
        return build(req,
                InterruptionCarriereSoinsParentalVerdict.DIFFERE_EMPLOYEUR,
                ancienneteOk, ageEnfantOk, formalismeOk, dureeMois,
                soldeApres, allocMensuelle, allocTotale, dateFin,
                finProtection, protectionActive, indemniteProtection,
                "DIFFERE_EMPLOYEUR_MOTIVE_ART_7_AR_29_10_1997",
                synthese);
    }

    private static InterruptionCarriereSoinsParentalResult buildEligibleComplet(
            InterruptionCarriereSoinsParentalRequest req,
            boolean ancienneteOk, boolean ageEnfantOk, boolean formalismeOk,
            int dureeMois, int soldeApres,
            BigDecimal allocMensuelle, BigDecimal allocTotale,
            LocalDate dateFin, LocalDate finProtection,
            boolean protectionActive, BigDecimal indemniteProtection) {
        String formeLib = formeLibelle(req.forme());
        String allocLib = req.cumulAllocationOnemDemande()
                ? "Allocation ONEM forfaitaire mensuelle estimée : "
                        + allocMensuelle.toPlainString() + " EUR, soit "
                        + allocTotale.toPlainString() + " EUR sur "
                        + dureeMois + " mois (AR 12/08/1991 art. 4)."
                : "Pas de cumul allocation ONEM demandé.";
        String dateFinLib = dateFin == null
                ? "non calculable (date de début non renseignée)"
                : dateFin.toString();
        String synthese = "ELIGIBILITE COMPLETE au congé parental BE sous"
                + " la forme " + formeLib + " pour une durée effective"
                + " de " + dureeMois + " mois. Le travailleur remplit"
                + " toutes les conditions cumulatives : ancienneté de "
                + req.ancienneteMois() + " mois >= douze mois (art. 5 AR"
                + " 29/10/1997), enfant éligible (art. 4), solde"
                + " individuel suffisant (art. 2), formalisme respecté"
                + " (art. 6). Date de fin calculée : " + dateFinLib + "."
                + " " + allocLib + " Points d'attention : (a) la"
                + " protection contre le licenciement art. 101 Loi"
                + " 22/01/1985 est ACTIVE depuis la notification de la"
                + " demande et court jusqu'à trois mois après la fin"
                + " effective de l'interruption — toute mesure"
                + " défavorable prise par l'employeur dans cette fenêtre"
                + " est présumée représailles et ouvre droit à une"
                + " indemnité de six mois de rémunération brute en cas"
                + " de licenciement irrégulier ; (b) déposer le"
                + " formulaire C61 demande d'allocations d'interruption"
                + " auprès de l'ONEM dans les deux mois du début de"
                + " l'interruption pour ne pas perdre le droit à"
                + " l'allocation ; (c) le solde individuel restant après"
                + " imputation est de " + soldeApres + " mois ETP (utile"
                + " pour de futures demandes pour le même enfant) ; (d)"
                + " le droit est individuel par enfant ET par parent —"
                + " l'autre parent conserve son propre droit ; (e) la"
                + " forme choisie est définitive sauf accord mutuel"
                + " employeur-travailleur sur une modification.";
        return build(req,
                InterruptionCarriereSoinsParentalVerdict.ELIGIBLE_COMPLET,
                ancienneteOk, ageEnfantOk, formalismeOk, dureeMois,
                soldeApres, allocMensuelle, allocTotale, dateFin,
                finProtection, protectionActive, indemniteProtection,
                "ELIGIBILITE_COMPLETE_CONDITIONS_CUMULATIVES_REUNIES",
                synthese);
    }

    private static InterruptionCarriereSoinsParentalResult buildEligibleAvecReserves(
            InterruptionCarriereSoinsParentalRequest req,
            boolean ancienneteOk, boolean ageEnfantOk, boolean formalismeOk,
            int dureeMois, int soldeApres,
            BigDecimal allocMensuelle, BigDecimal allocTotale,
            LocalDate dateFin, LocalDate finProtection,
            boolean protectionActive, BigDecimal indemniteProtection) {
        StringBuilder reserves = new StringBuilder();
        if (!req.employeurAccepte()) {
            reserves.append(" l'employeur n'a pas encore formellement"
                    + " accepté la demande — vigilance sur un éventuel"
                    + " différé motivé art. 7 AR 29/10/1997 dans le"
                    + " délai d'un mois ;");
        }
        if (hasReservesFormalismeLimite(req)) {
            reserves.append(" délai de notification limite (proche du"
                    + " minimum deux mois ou du maximum trois mois) —"
                    + " préconstituer la preuve de la date d'envoi par"
                    + " lettre recommandée art. 6 AR 29/10/1997 ;");
        }
        String formeLib = formeLibelle(req.forme());
        String synthese = "ELIGIBILITE confirmée au congé parental BE"
                + " sous la forme " + formeLib + " (" + dureeMois + " mois"
                + " effectifs), MAIS avec points de vigilance :"
                + reserves + " Le travailleur remplit les conditions de"
                + " fond (ancienneté, âge enfant, solde, formalisme"
                + " principal) mais l'opérationnel reste à sécuriser."
                + " Points d'attention : (a) la protection contre le"
                + " licenciement art. 101 Loi 22/01/1985 court dès la"
                + " demande écrite — opposable même en cas de"
                + " contestation procédurale ultérieure Cass. BE"
                + " 26/05/2008 S.07.0040.F ; (b) en cas de différé"
                + " employeur art. 7 AR 29/10/1997, vérifier la"
                + " motivation effective et le respect du délai d'un"
                + " mois pour notifier le différé ; (c) déposer le"
                + " formulaire C61 ONEM dans les deux mois du début"
                + " effectif pour préserver le droit à l'allocation"
                + " forfaitaire (AR 12/08/1991 art. 4) ; (d) si"
                + " l'employeur abuse de la procédure, action en"
                + " cessation art. 588 C. jud. envisageable.";
        return build(req,
                InterruptionCarriereSoinsParentalVerdict.ELIGIBLE_AVEC_RESERVES,
                ancienneteOk, ageEnfantOk, formalismeOk, dureeMois,
                soldeApres, allocMensuelle, allocTotale, dateFin,
                finProtection, protectionActive, indemniteProtection,
                "ELIGIBILITE_AVEC_RESERVES_PROCEDURALES",
                synthese);
    }

    // ── Helpers calculs ─────────────────────────────────────────────────────

    private static boolean isAgeEnfantOk(
            InterruptionCarriereSoinsParentalRequest req) {
        int plafond = req.enfantHandicap()
                ? AGE_PLAFOND_HANDICAP_MOIS
                : AGE_PLAFOND_MOIS;
        return req.ageEnfantMois() < plafond;
    }

    private static boolean isFormalismeOk(
            InterruptionCarriereSoinsParentalRequest req) {
        if (req.modeNotification()
                == InterruptionCarriereSoinsParentalModeNotification.AUTRE) {
            return false;
        }
        if (req.dateNotification() == null
                || req.dateDebutInterruption() == null) {
            // Pas de date — défaut de présomption favorable
            return true;
        }
        long jours = ChronoUnit.DAYS.between(req.dateNotification(),
                req.dateDebutInterruption());
        return jours >= DELAI_NOTIF_MIN_JOURS
                && jours <= DELAI_NOTIF_MAX_JOURS;
    }

    private static boolean hasReservesFormalismeLimite(
            InterruptionCarriereSoinsParentalRequest req) {
        if (req.dateNotification() == null
                || req.dateDebutInterruption() == null) {
            return false;
        }
        long jours = ChronoUnit.DAYS.between(req.dateNotification(),
                req.dateDebutInterruption());
        // Limite : proche du minimum (< 65 j) ou du maximum (> 85 j)
        return (jours >= DELAI_NOTIF_MIN_JOURS && jours < 65)
                || (jours > 85 && jours <= DELAI_NOTIF_MAX_JOURS);
    }

    private static int dureeForForme(
            InterruptionCarriereSoinsParentalForme forme) {
        return switch (forme) {
            case TEMPS_PLEIN -> DUREE_TEMPS_PLEIN_MOIS;
            case MI_TEMPS -> DUREE_MI_TEMPS_MOIS;
            case CINQUIEME_TEMPS -> DUREE_CINQUIEME_TEMPS_MOIS;
        };
    }

    private static BigDecimal allocationMensuelle(
            InterruptionCarriereSoinsParentalForme forme) {
        return switch (forme) {
            case TEMPS_PLEIN -> ALLOCATION_TEMPS_PLEIN;
            case MI_TEMPS -> ALLOCATION_MI_TEMPS;
            case CINQUIEME_TEMPS -> ALLOCATION_CINQUIEME_TEMPS;
        };
    }

    private static String formeLibelle(
            InterruptionCarriereSoinsParentalForme forme) {
        return switch (forme) {
            case TEMPS_PLEIN -> "TEMPS PLEIN (interruption complète quatre"
                    + " mois art. 3 AR 29/10/1997)";
            case MI_TEMPS -> "MI-TEMPS (réduction de moitié huit mois"
                    + " art. 3 AR 29/10/1997)";
            case CINQUIEME_TEMPS -> "CINQUIEME TEMPS (réduction d'un"
                    + " cinquième vingt mois art. 1 AR 12/12/2001)";
        };
    }

    // ── Construction du résultat ────────────────────────────────────────────

    private static InterruptionCarriereSoinsParentalResult build(
            InterruptionCarriereSoinsParentalRequest req,
            InterruptionCarriereSoinsParentalVerdict verdict,
            boolean ancienneteOk, boolean ageEnfantOk, boolean formalismeOk,
            int dureeMois, int soldeApres,
            BigDecimal allocMensuelle, BigDecimal allocTotale,
            LocalDate dateFin, LocalDate finProtection,
            boolean protectionActive, BigDecimal indemniteProtection,
            String raison, String synthese) {
        return new InterruptionCarriereSoinsParentalResult(
                req.forme(),
                req.ancienneteMois(),
                req.ageEnfantMois(),
                req.enfantHandicap(),
                req.soldeRestantMoisEtp(),
                req.modeNotification(),
                req.employeurAccepte(),
                req.employeurAdiffere(),
                req.cumulAllocationOnemDemande(),
                req.dateDebutInterruption(),
                req.dateNotification(),
                req.remunerationMensuelleBrute(),
                verdict,
                ancienneteOk,
                ageEnfantOk,
                formalismeOk,
                dureeMois,
                soldeApres,
                allocMensuelle,
                allocTotale,
                dateFin,
                finProtection,
                protectionActive,
                indemniteProtection,
                raison, synthese, BASE_JURIDIQUE, AVERT_AVOCAT_BE);
    }

    private static void validateInputs(
            InterruptionCarriereSoinsParentalRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.forme() == null) {
            throw new IllegalArgumentException("forme est requis");
        }
        if (request.ancienneteMois() == null) {
            throw new IllegalArgumentException("ancienneteMois est requis");
        }
        if (request.ancienneteMois() < 0) {
            throw new IllegalArgumentException(
                    "ancienneteMois ne peut pas être négatif");
        }
        if (request.ageEnfantMois() == null) {
            throw new IllegalArgumentException("ageEnfantMois est requis");
        }
        if (request.ageEnfantMois() < 0) {
            throw new IllegalArgumentException(
                    "ageEnfantMois ne peut pas être négatif");
        }
        if (request.enfantHandicap() == null) {
            throw new IllegalArgumentException("enfantHandicap est requis");
        }
        if (request.soldeRestantMoisEtp() == null) {
            throw new IllegalArgumentException(
                    "soldeRestantMoisEtp est requis");
        }
        if (request.soldeRestantMoisEtp() < 0) {
            throw new IllegalArgumentException(
                    "soldeRestantMoisEtp ne peut pas être négatif");
        }
        if (request.modeNotification() == null) {
            throw new IllegalArgumentException(
                    "modeNotification est requis");
        }
        if (request.employeurAccepte() == null) {
            throw new IllegalArgumentException("employeurAccepte est requis");
        }
        if (request.employeurAdiffere() == null) {
            throw new IllegalArgumentException(
                    "employeurAdiffere est requis");
        }
        if (request.cumulAllocationOnemDemande() == null) {
            throw new IllegalArgumentException(
                    "cumulAllocationOnemDemande est requis");
        }
        if (request.remunerationMensuelleBrute() != null
                && request.remunerationMensuelleBrute()
                        .signum() < 0) {
            throw new IllegalArgumentException(
                    "remunerationMensuelleBrute ne peut pas être négative");
        }
    }
}
