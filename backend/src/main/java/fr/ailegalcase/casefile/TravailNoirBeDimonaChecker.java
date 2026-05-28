package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * SF-219-26 : analyseur du travail dissimulé / non déclaré BE sous
 * l'angle DIMONA — fonction <b>pure</b> (sans dépendance HTTP /
 * persistance / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi-programme du 24/12/2002</b> (M.B. 31/12/2002) art.
 *       167 à 184 — instauration de la Déclaration Immédiate de
 *       l'Emploi (DIMONA) obligatoire avant toute occupation d'un
 *       travailleur salarié à l'ONSS.</li>
 *   <li><b>AR du 05/11/2002</b> (M.B. 20/11/2002) — modalités
 *       d'exécution de la DIMONA en application de l'art. 38 Loi
 *       du 26/07/1996.</li>
 *   <li><b>Code pénal social du 06/06/2010 art. 181</b> — sanction
 *       niveau 4 du défaut DIMONA (amende admin 300 à 3000 € OU
 *       pénale 600 à 6000 € ET/OU emprisonnement 6 mois à 3 ans).</li>
 *   <li><b>Code pénal social art. 103 § 2</b> — multiplication de
 *       l'amende par le nombre de travailleurs concernés (plafond
 *       100 × maximum unitaire personne morale art. 105 § 1).</li>
 *   <li><b>Code pénal social art. 105 § 1</b> — multiplication × 5
 *       pour les personnes morales.</li>
 *   <li><b>Code pénal social art. 110</b> — récidive légale dans
 *       le délai d'un an : doublement du plafond d'amende.</li>
 *   <li><b>Code pénal social art. 328 § 1</b> — présomption simple
 *       de salariat applicable à toute personne pour laquelle
 *       aucune DIMONA n'a été effectuée (renversable par tout moyen
 *       de preuve, Cass. BE 06/12/2010 S.10.0067.F).</li>
 *   <li><b>Loi du 27/06/1969</b> révisant l'arrêté-loi du 28/12/1944
 *       art. 1<sup>er</sup> — présomption générale d'assujettissement
 *       à la sécurité sociale salariée.</li>
 *   <li><b>Loi du 22/04/2003</b> portant adaptation des sanctions
 *       administratives art. 28 — amende ONSS forfaitaire = 3 × les
 *       cotisations dues en cas d'absence DIMONA.</li>
 *   <li><b>AR du 28/11/1969</b> — taux cotisations ONSS employeur
 *       ~25 % (taux global pondéré, hors mesures structurelles) +
 *       travailleur 13,07 % sur rémunération brute.</li>
 *   <li><b>Loi-programme I du 27/12/2006 art. 328 à 333</b> —
 *       cotisations de solidarité ONSS sur requalification et
 *       critères secteurs construction / transport / nettoyage.</li>
 *   <li><b>Cass. ch. néerl. 21/11/2017 P.17.0532.N</b> — qualification
 *       du travail non déclaré comme infraction niveau 4 art. 181
 *       C. pén. soc.</li>
 *   <li><b>Cour const. arrêt 102/2009 du 18/06/2009</b> — conformité
 *       de la présomption de salariat aux principes du procès
 *       équitable.</li>
 * </ul>
 *
 * <h2>Logique de l'analyseur</h2>
 * <ol>
 *   <li>Calcule la durée non déclarée en jours = entre
 *       {@code dateDebutOccupation} et :
 *       <ul>
 *         <li>{@code dateDimonaEffective} si statut TARDIVE et
 *             dateDimona renseignée et postérieure au début ;</li>
 *         <li>{@code dateControle} sinon.</li>
 *       </ul></li>
 *   <li>Calcule les cotisations ONSS rétroactives :
 *       <ul>
 *         <li>Employeur : salaireBrutMensuel × (durée/30) × 25 %.</li>
 *         <li>Travailleur : salaireBrutMensuel × (durée/30) × 13,07 %.</li>
 *       </ul>
 *       Multiplié par le nombre de travailleurs concernés.</li>
 *   <li>Calcule l'amende ONSS forfaitaire = 3 × cotisations totales
 *       (art. 28 Loi du 22/04/2003).</li>
 *   <li>Détermine le verdict selon le statut DIMONA + éléments de
 *       subordination.</li>
 *   <li>Restitue les bornes amende pénale art. 181 niveau 4 si
 *       applicable + flags majorations art. 103/105/110.</li>
 * </ol>
 */
public final class TravailNoirBeDimonaChecker {

    /** Taux cotisation ONSS employeur (taux global pondéré, AR 28/11/1969). */
    static final BigDecimal TAUX_ONSS_EMPLOYEUR = new BigDecimal("0.25");

    /** Taux cotisation ONSS travailleur (13,07 % légal). */
    static final BigDecimal TAUX_ONSS_TRAVAILLEUR = new BigDecimal("0.1307");

    /** Multiplicateur amende ONSS forfaitaire (art. 28 Loi 22/04/2003). */
    static final BigDecimal MULTIPLICATEUR_AMENDE_ONSS = new BigDecimal("3");

    /** Diviseur jours/mois pour proratisation des cotisations. */
    static final BigDecimal JOURS_PAR_MOIS = new BigDecimal("30");

    /** Bornes art. 181 niveau 4 (valeur de base, avant indexation). */
    static final int N4_ADMIN_MIN = 300;
    static final int N4_ADMIN_MAX = 3000;
    static final int N4_PENAL_MIN = 600;
    static final int N4_PENAL_MAX = 6000;
    static final int N4_EMPRISONNEMENT_MIN_MOIS = 6;
    static final int N4_EMPRISONNEMENT_MAX_MOIS = 36;

    static final String BASE_JURIDIQUE =
            "Loi-programme du 24/12/2002 art. 167 à 184 M.B."
                    + " 31/12/2002 instaurant la Déclaration Immédiate"
                    + " de l'Emploi DIMONA obligatoire avant toute"
                    + " occupation d'un travailleur salarié à l'ONSS ;"
                    + " AR du 05/11/2002 instaurant une déclaration"
                    + " immédiate de l'emploi en application de"
                    + " l'art. 38 de la Loi du 26/07/1996 portant"
                    + " modernisation de la sécurité sociale ; Code"
                    + " pénal social du 06/06/2010 art. 181 sanction"
                    + " niveau 4 défaut DIMONA — amende administrative"
                    + " 300 à 3000 € ou amende pénale 600 à 6000 €"
                    + " ET/OU emprisonnement 6 mois à 3 ans ; art."
                    + " 103 § 2 C. pén. soc. multiplication par le"
                    + " nombre de travailleurs concernés ; art. 105"
                    + " § 1 C. pén. soc. multiplication × 5 personne"
                    + " morale et plafond global 100 × maximum"
                    + " unitaire ; art. 110 C. pén. soc. récidive"
                    + " légale dans le délai d'un an doublement du"
                    + " plafond ; art. 328 § 1 C. pén. soc."
                    + " présomption simple de salariat applicable à"
                    + " toute personne pour laquelle aucune DIMONA"
                    + " n'a été effectuée ; Loi du 27/06/1969 art."
                    + " 1er présomption générale d'assujettissement à"
                    + " la sécurité sociale salariée ; Loi du"
                    + " 22/04/2003 art. 28 amende ONSS forfaitaire"
                    + " 3 × les cotisations dues en cas d'absence"
                    + " DIMONA ; AR du 28/11/1969 — taux cotisations"
                    + " ONSS employeur ~25 % global pondéré +"
                    + " travailleur 13,07 % sur rémunération brute ;"
                    + " Loi-programme I du 27/12/2006 art. 328 à 333"
                    + " cotisations de solidarité ONSS sur"
                    + " requalification et critères secteurs"
                    + " construction transport nettoyage ; Cass. ch."
                    + " néerl. 21/11/2017 P.17.0532.N qualification"
                    + " du travail non déclaré niveau 4 art. 181 ;"
                    + " Cour const. arrêt 102/2009 du 18/06/2009"
                    + " conformité présomption de salariat aux"
                    + " principes du procès équitable ; Cass. ch. fr."
                    + " 06/12/2010 S.10.0067.F présomption simple"
                    + " renversable par tout moyen de preuve.";

    static final String AVERT_AVOCAT_BE =
            "L'outil d'analyse du travail dissimulé BE sous l'angle"
                    + " DIMONA produit un verdict indicatif fondé sur"
                    + " les déclarations de l'avocat. L'avocat"
                    + " spécialisé en droit social pénal BE doit"
                    + " confirmer pour la situation considérée : (i)"
                    + " la qualification exacte du statut DIMONA au"
                    + " moment du contrôle inspection sociale et la"
                    + " date d'engagement effective (les indices"
                    + " probants sont la déclaration ONSS, le"
                    + " contrat écrit signé, les badges d'accès, les"
                    + " feuilles de prestations) ; (ii) le calcul"
                    + " exact des cotisations ONSS rétroactives dues —"
                    + " l'outil applique un taux employeur global"
                    + " pondéré de 25 % indicatif (taux réel variable"
                    + " selon la commission paritaire, les mesures"
                    + " structurelles de réduction, le tarif accidents"
                    + " du travail, le pécule de vacances pour les"
                    + " ouvriers) et le taux travailleur légal de"
                    + " 13,07 % ; (iii) l'éventuelle prescription"
                    + " quinquennale des créances ONSS (art. 42 Loi"
                    + " du 27/06/1969 — prescription 5 ans à compter"
                    + " du dernier jour du trimestre civil considéré,"
                    + " portée à 7 ans en cas d'intention frauduleuse)"
                    + " ; (iv) l'indexation in concreto des amendes"
                    + " pénales art. 181 C. pén. soc. à la date des"
                    + " faits par les décimes additionnels art. 1er"
                    + " Loi du 05/03/1952 (+0,80 jusqu'au 31/12/2024 ;"
                    + " +0,90 depuis le 01/01/2025) ; (v) l'option"
                    + " administrative ou pénale de l'auditorat du"
                    + " travail art. 73-78 C. pén. soc. (« règle Una"
                    + " Via », non bis in idem opposable, Cour const."
                    + " 61/2014) ; (vi) la possibilité de renverser la"
                    + " présomption de salariat art. 328 C. pén. soc."
                    + " (présomption simple, Cass. 06/12/2010"
                    + " S.10.0067.F, renversable par tout moyen de"
                    + " preuve) ; (vii) la qualification éventuelle"
                    + " de faux indépendant au regard des 4 critères"
                    + " généraux art. 333 Loi-programme I du"
                    + " 27/12/2006 (volonté des parties, liberté"
                    + " d'organisation du travail, liberté"
                    + " d'organisation du temps de travail, contrôle"
                    + " hiérarchique) + critères sectoriels"
                    + " complémentaires AR sectoriels construction"
                    + " transport nettoyage agriculture horeca"
                    + " gardiennage ; (viii) la prescription pénale"
                    + " du défaut DIMONA art. 21 Titre préliminaire"
                    + " C. proc. pén. (5 ans correctionnelle, point"
                    + " de départ : jour de la dernière occupation"
                    + " non déclarée) ; (ix) la responsabilité civile"
                    + " solidaire de l'employeur pour les actes du"
                    + " préposé ou mandataire art. 109 C. pén. soc."
                    + " et la responsabilité pénale propre de la"
                    + " personne physique (gérant / administrateur /"
                    + " préposé) à côté de celle de la personne"
                    + " morale art. 5 C. pén. ; (x) l'articulation"
                    + " avec les procédures civiles parallèles"
                    + " (paiement des arriérés salariaux Loi"
                    + " 12/04/1965, indemnités de rupture Loi"
                    + " 03/07/1978, dommages et intérêts art. 1382"
                    + " anc. C. civ. / 5.83 nouveau C. civ., action"
                    + " ONSS en recouvrement des cotisations devant"
                    + " le tribunal du travail art. 580 1° C. jud.) ;"
                    + " (xi) les voies de recours : tribunal du"
                    + " travail si voie administrative ONSS"
                    + " (art. 580 C. jud.), tribunal correctionnel"
                    + " si voie pénale (art. 76 C. jud.), Cour du"
                    + " travail puis Cass. en cassation.";

    private TravailNoirBeDimonaChecker() {
    }

    public static TravailNoirBeDimonaResult analyze(
            TravailNoirBeDimonaRequest request) {
        validateInputs(request);

        TravailNoirBeDimonaStatutDimona statut = request.statutDimona();
        TravailNoirBeDimonaElementsSubordination subord =
                request.elementsSubordination();

        // ── Durée non déclarée en jours ─────────────────────────────────────
        // Borne fin = min(dateDimonaEffective, dateControle) si TARDIVE et
        // DIMONA postérieure au début ; sinon dateControle.
        LocalDate borneFin;
        if (statut == TravailNoirBeDimonaStatutDimona.DECLAREE_TARDIVE
                && request.dateDimonaEffective() != null
                && request.dateDimonaEffective().isAfter(
                        request.dateDebutOccupation())) {
            LocalDate dimona = request.dateDimonaEffective();
            borneFin = dimona.isBefore(request.dateControle())
                    ? dimona
                    : request.dateControle();
        } else {
            borneFin = request.dateControle();
        }
        long jours;
        if (borneFin.isBefore(request.dateDebutOccupation())) {
            jours = 0;
        } else {
            jours = ChronoUnit.DAYS.between(
                    request.dateDebutOccupation(), borneFin);
        }

        // ── Cotisations ONSS rétroactives ───────────────────────────────────
        BigDecimal salaire = request.salaireBrutMensuel();
        BigDecimal nbTravailleurs = BigDecimal.valueOf(
                request.nombreTravailleursConcernes());
        BigDecimal joursBd = BigDecimal.valueOf(jours);

        // Base mensualisée × nombre travailleurs
        BigDecimal baseTotale = salaire
                .multiply(joursBd)
                .divide(JOURS_PAR_MOIS, 6, RoundingMode.HALF_UP)
                .multiply(nbTravailleurs);

        BigDecimal cotEmployeur = baseTotale
                .multiply(TAUX_ONSS_EMPLOYEUR)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal cotTravailleur = baseTotale
                .multiply(TAUX_ONSS_TRAVAILLEUR)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal cotTotal = cotEmployeur.add(cotTravailleur)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal amendeOnss = cotTotal
                .multiply(MULTIPLICATEUR_AMENDE_ONSS)
                .setScale(2, RoundingMode.HALF_UP);

        boolean multTravailleurs = request.nombreTravailleursConcernes() > 1;
        boolean majPM = request.personneMorale();
        boolean majRecidive = request.recidiveDansLAn();

        // ── Sélection du verdict ────────────────────────────────────────────
        return switch (statut) {
            case DECLAREE_AVANT_DEBUT -> buildConforme(request, jours);
            case DECLAREE_TARDIVE -> buildTardive(
                    request, jours, cotEmployeur, cotTravailleur, cotTotal,
                    amendeOnss, multTravailleurs, majPM, majRecidive);
            case ABSENTE -> buildAbsente(
                    request, jours, cotEmployeur, cotTravailleur, cotTotal,
                    amendeOnss, multTravailleurs, majPM, majRecidive, subord);
            case INDEPENDANT_FAUSSEMENT_QUALIFIE -> buildIndependant(
                    request, jours, cotEmployeur, cotTravailleur, cotTotal,
                    amendeOnss, multTravailleurs, majPM, majRecidive, subord);
        };
    }

    // ── Branches verdict ────────────────────────────────────────────────────

    private static TravailNoirBeDimonaResult buildConforme(
            TravailNoirBeDimonaRequest req, long jours) {
        String synthese = "DIMONA effectuée avant le début effectif de"
                + " l'occupation (art. 4 AR du 05/11/2002 — obligation"
                + " préalable). Aucune sanction pénale art. 181"
                + " C. pén. soc. ; aucune cotisation ONSS rétroactive"
                + " due (les cotisations normales sur la rémunération"
                + " brute sont payées via la DmfA trimestrielle"
                + " ordinaire). Rappel des obligations connexes : tenue"
                + " du dossier individuel art. 25 Loi du 16/03/1971"
                + " sur le travail, déclaration DmfA trimestrielle"
                + " art. 21 AR du 28/11/1969, registre du personnel"
                + " général art. 4 AR du 08/08/1980, contrat écrit si"
                + " temps partiel art. 11bis Loi du 03/07/1978 ou"
                + " CDD/intérim/étudiant art. 9.";
        return build(req, TravailNoirBeDimonaVerdict.DIMONA_CONFORME,
                jours, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, false,
                0, 0, 0, 0, false, 0, 0,
                false, false, false,
                "DIMONA_CONFORME_AVANT_DEBUT",
                synthese);
    }

    private static TravailNoirBeDimonaResult buildTardive(
            TravailNoirBeDimonaRequest req, long jours,
            BigDecimal cotEmp, BigDecimal cotTrav, BigDecimal cotTotal,
            BigDecimal amendeOnss, boolean multT, boolean majPM, boolean majR) {
        String synthese = "DIMONA effectuée tardivement, après le début"
                + " effectif de l'occupation (le " + req.dateDebutOccupation()
                + ") mais avant le contrôle inspection sociale (le "
                + req.dateControle() + "). Durée non déclarée : "
                + jours + " jour(s) calendrier. La situation est"
                + " partiellement régularisable : cotisations ONSS"
                + " rétroactives dues sur la période non déclarée"
                + " (employeur " + cotEmp + " € + travailleur "
                + cotTrav + " € = total " + cotTotal + " €) ; amende"
                + " administrative ONSS atténuée envisageable (l'art."
                + " 28 Loi du 22/04/2003 prévoit en principe 3 × les"
                + " cotisations dues soit " + amendeOnss + " €, montant"
                + " susceptible de modération par l'ONSS / Auditorat"
                + " en cas de régularisation spontanée avant contrôle)."
                + " Pas de sanction pénale niveau 4 art. 181 C. pén."
                + " soc. systématique : l'auditorat du travail dispose"
                + " d'un pouvoir d'appréciation et oriente"
                + " généralement vers la voie administrative en cas de"
                + " régularisation tardive non frauduleuse"
                + suffixeMajorations(majPM, multT,
                        req.nombreTravailleursConcernes(), majR);
        return build(req, TravailNoirBeDimonaVerdict.DIMONA_TARDIVE_REGULARISABLE,
                jours, cotEmp, cotTrav, cotTotal, amendeOnss,
                false,
                0, 0, 0, 0, false, 0, 0,
                multT, majPM, majR,
                "DIMONA_TARDIVE_REGULARISATION_ADMINISTRATIVE",
                synthese);
    }

    private static TravailNoirBeDimonaResult buildAbsente(
            TravailNoirBeDimonaRequest req, long jours,
            BigDecimal cotEmp, BigDecimal cotTrav, BigDecimal cotTotal,
            BigDecimal amendeOnss, boolean multT, boolean majPM, boolean majR,
            TravailNoirBeDimonaElementsSubordination subord) {
        // Si éléments subordination INDETERMINE : on bascule en A_QUALIFIER
        // pour éviter d'engager un verdict pénal en l'état.
        if (subord == TravailNoirBeDimonaElementsSubordination.INDETERMINE) {
            String synthese = "Aucune DIMONA enregistrée à la date du"
                    + " contrôle (le " + req.dateControle() + "). Toutefois"
                    + " les éléments de subordination caractérisant le"
                    + " salariat sont INDETERMINES à ce stade : l'avocat"
                    + " doit caractériser concrètement l'existence (ou"
                    + " l'absence) de subordination juridique au sens"
                    + " des critères horaires imposés, instructions"
                    + " hiérarchiques, intégration dans l'organisation,"
                    + " fourniture du matériel, lieu de travail imposé,"
                    + " exclusivité, avant de retenir une qualification"
                    + " définitive. La présomption simple de salariat"
                    + " art. 328 § 1 C. pén. soc. est mobilisable par"
                    + " l'auditorat mais reste renversable par tout"
                    + " moyen de preuve (Cass. BE 06/12/2010"
                    + " S.10.0067.F). Cotisations ONSS rétroactives"
                    + " indicatives sur la base d'une qualification"
                    + " salariée hypothétique : employeur " + cotEmp
                    + " € + travailleur " + cotTrav + " € = total "
                    + cotTotal + " €. Analyse complémentaire avocat"
                    + " requise avant prise de position définitive.";
            return build(req, TravailNoirBeDimonaVerdict.A_QUALIFIER,
                    jours, cotEmp, cotTrav, cotTotal, amendeOnss,
                    false,
                    0, 0, 0, 0, false, 0, 0,
                    multT, majPM, majR,
                    "ABSENCE_DIMONA_SUBORDINATION_INDETERMINEE",
                    synthese);
        }
        // Si éléments subordination NON : alerter sur l'incohérence
        // (absence DIMONA + relation potentiellement non salariée).
        if (subord == TravailNoirBeDimonaElementsSubordination.NON) {
            String synthese = "Aucune DIMONA enregistrée mais aucun"
                    + " élément de subordination caractérisé : la"
                    + " relation pourrait relever de l'indépendance"
                    + " (cas où la DIMONA n'est pas requise). Toutefois"
                    + " l'auditorat du travail mobilisera la présomption"
                    + " simple de salariat art. 328 § 1 C. pén. soc. et"
                    + " il appartiendra à l'avocat de la renverser par"
                    + " la production d'éléments établissant l'absence"
                    + " de subordination juridique (Cass. BE 06/12/2010"
                    + " S.10.0067.F — présomption simple). À défaut,"
                    + " les conséquences ABSENCE_DIMONA_SANCTION_NIVEAU_4"
                    + " s'appliquent : cotisations ONSS rétroactives"
                    + " (employeur " + cotEmp + " € + travailleur "
                    + cotTrav + " € = total " + cotTotal + " €) +"
                    + " amende ONSS forfaitaire " + amendeOnss + " € +"
                    + " sanction pénale art. 181 niveau 4"
                    + suffixeMajorations(majPM, multT,
                            req.nombreTravailleursConcernes(), majR);
            return build(req, TravailNoirBeDimonaVerdict.A_QUALIFIER,
                    jours, cotEmp, cotTrav, cotTotal, amendeOnss,
                    true,
                    N4_ADMIN_MIN, N4_ADMIN_MAX, N4_PENAL_MIN, N4_PENAL_MAX,
                    true, N4_EMPRISONNEMENT_MIN_MOIS,
                    N4_EMPRISONNEMENT_MAX_MOIS,
                    multT, majPM, majR,
                    "ABSENCE_DIMONA_SUBORDINATION_NON_CARACTERISEE",
                    synthese);
        }
        // OUI — sanction art. 181 niveau 4 ferme
        String synthese = "Aucune DIMONA enregistrée à la date du"
                + " contrôle (le " + req.dateControle() + ") alors que"
                + " les éléments de subordination caractérisant le"
                + " salariat sont établis : infraction art. 181"
                + " C. pén. soc. niveau 4 de l'échelle art. 101 § 1"
                + " (Loi du 06/06/2010 introduisant le Code pénal"
                + " social, Cass. ch. néerl. 21/11/2017 P.17.0532.N)."
                + " Durée non déclarée : " + jours + " jour(s)"
                + " calendrier. Conséquences cumulatives : (a)"
                + " cotisations ONSS rétroactives sur la période non"
                + " déclarée — employeur " + cotEmp + " € (taux ~25 %"
                + " global pondéré AR du 28/11/1969) + travailleur "
                + cotTrav + " € (taux légal 13,07 %) = total "
                + cotTotal + " € ; (b) amende administrative ONSS"
                + " forfaitaire art. 28 Loi du 22/04/2003 = 3 × les"
                + " cotisations dues, soit " + amendeOnss + " € ; (c)"
                + " sanction pénale art. 181 niveau 4 — amende admin"
                + " 300 à 3000 € OU amende pénale 600 à 6000 € (au"
                + " choix de l'auditorat règle Una Via art. 73-78"
                + " C. pén. soc.) ET/OU emprisonnement 6 mois à 3 ans"
                + suffixeMajorations(majPM, multT,
                        req.nombreTravailleursConcernes(), majR);
        return build(req,
                TravailNoirBeDimonaVerdict.ABSENCE_DIMONA_SANCTION_NIVEAU_4,
                jours, cotEmp, cotTrav, cotTotal, amendeOnss,
                true,
                N4_ADMIN_MIN, N4_ADMIN_MAX, N4_PENAL_MIN, N4_PENAL_MAX,
                true, N4_EMPRISONNEMENT_MIN_MOIS,
                N4_EMPRISONNEMENT_MAX_MOIS,
                multT, majPM, majR,
                "ABSENCE_DIMONA_NIVEAU_4_FERME",
                synthese);
    }

    private static TravailNoirBeDimonaResult buildIndependant(
            TravailNoirBeDimonaRequest req, long jours,
            BigDecimal cotEmp, BigDecimal cotTrav, BigDecimal cotTotal,
            BigDecimal amendeOnss, boolean multT, boolean majPM, boolean majR,
            TravailNoirBeDimonaElementsSubordination subord) {
        // Cas faux indépendant : si subordination OUI → requalification
        // salariat + DIMONA jamais effectuée. Si NON / INDETERMINE →
        // A_QUALIFIER (à charge de l'avocat de caractériser).
        if (subord == TravailNoirBeDimonaElementsSubordination.OUI) {
            String synthese = "Relation faussement qualifiée"
                    + " d'indépendante (faux indépendant) : les éléments"
                    + " de subordination juridique caractérisent en"
                    + " réalité un contrat de travail salarié (4"
                    + " critères généraux art. 333 Loi-programme I du"
                    + " 27/12/2006 — volonté des parties, liberté"
                    + " d'organisation du travail, liberté"
                    + " d'organisation du temps de travail, contrôle"
                    + " hiérarchique). Requalification en salariat avec"
                    + " obligation DIMONA rétroactive depuis le début"
                    + " de la relation (le "
                    + req.dateDebutOccupation() + ") jamais remplie."
                    + " Durée non déclarée : " + jours + " jour(s)."
                    + " Conséquences : (a) cotisations ONSS"
                    + " rétroactives sur la totalité de la période —"
                    + " employeur " + cotEmp + " € + travailleur "
                    + cotTrav + " € = total " + cotTotal + " €"
                    + " (l'avocat vérifie la prescription quinquennale"
                    + " art. 42 Loi du 27/06/1969, portée à 7 ans en"
                    + " cas d'intention frauduleuse) ; (b) amende ONSS"
                    + " forfaitaire 3 × art. 28 Loi du 22/04/2003 = "
                    + amendeOnss + " € ; (c) sanction pénale art. 181"
                    + " niveau 4 C. pén. soc. — amende admin 300 à"
                    + " 3000 € OU pénale 600 à 6000 € ET/OU"
                    + " emprisonnement 6 mois à 3 ans"
                    + suffixeMajorations(majPM, multT,
                            req.nombreTravailleursConcernes(), majR);
            return build(req,
                    TravailNoirBeDimonaVerdict.INDEPENDANT_REQUALIFIE_NON_DECLARE,
                    jours, cotEmp, cotTrav, cotTotal, amendeOnss,
                    true,
                    N4_ADMIN_MIN, N4_ADMIN_MAX, N4_PENAL_MIN, N4_PENAL_MAX,
                    true, N4_EMPRISONNEMENT_MIN_MOIS,
                    N4_EMPRISONNEMENT_MAX_MOIS,
                    multT, majPM, majR,
                    "FAUX_INDEPENDANT_REQUALIFIE_NIVEAU_4",
                    synthese);
        }
        // OUI éléments + statut INDEPENDANT : présomption salariat
        if (subord == TravailNoirBeDimonaElementsSubordination.INDETERMINE) {
            String synthese = "Statut déclaré INDEPENDANT mais éléments"
                    + " de subordination INDETERMINES. La présomption"
                    + " simple de salariat art. 328 § 1 C. pén. soc."
                    + " peut être mobilisée par l'auditorat compte"
                    + " tenu de l'absence de DIMONA. L'avocat doit"
                    + " caractériser concrètement les 4 critères"
                    + " généraux art. 333 Loi-programme I du"
                    + " 27/12/2006 + les critères sectoriels"
                    + " complémentaires applicables (construction,"
                    + " transport, nettoyage, agriculture, horeca,"
                    + " gardiennage). Analyse complémentaire requise"
                    + " avant prise de position.";
            return build(req, TravailNoirBeDimonaVerdict.A_QUALIFIER,
                    jours, cotEmp, cotTrav, cotTotal, amendeOnss,
                    false,
                    0, 0, 0, 0, false, 0, 0,
                    multT, majPM, majR,
                    "INDEPENDANT_SUBORDINATION_INDETERMINEE",
                    synthese);
        }
        // NON : relation indépendante caractérisée — DIMONA non requise
        String synthese = "Statut INDEPENDANT confirmé : aucun élément"
                + " de subordination juridique caractérisé. La DIMONA"
                + " n'est pas requise pour un travailleur indépendant"
                + " (la déclaration relève alors du régime des"
                + " travailleurs indépendants — INASTI, art. 3 § 1"
                + " AR n° 38 du 27/07/1967). Aucune cotisation ONSS"
                + " rétroactive ni amende art. 181 C. pén. soc. due."
                + " Toutefois l'avocat doit vérifier que les autres"
                + " obligations propres aux indépendants ont été"
                + " respectées (affiliation caisse d'assurances"
                + " sociales pour travailleurs indépendants, TVA,"
                + " déclarations fiscales) — hors du périmètre du"
                + " présent outil.";
        return build(req, TravailNoirBeDimonaVerdict.A_QUALIFIER,
                jours, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, false,
                0, 0, 0, 0, false, 0, 0,
                false, false, false,
                "INDEPENDANT_NON_SALARIE_DIMONA_NON_REQUISE",
                synthese);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static String suffixeMajorations(
            boolean personneMorale, boolean multTravailleurs,
            int nbTravailleurs, boolean recidive) {
        StringBuilder sb = new StringBuilder();
        if (personneMorale) {
            sb.append(" ; l'employeur étant une personne morale,"
                    + " l'amende est multipliée par 5 (art. 105 § 1"
                    + " C. pén. soc.)");
        }
        if (multTravailleurs && nbTravailleurs > 1) {
            sb.append(" ; l'amende est multipliée par le nombre de"
                    + " travailleurs concernés, soit ")
              .append(nbTravailleurs)
              .append(" (art. 103 § 2 C. pén. soc.), avec un plafond"
                    + " global de 100 fois le maximum unitaire pour"
                    + " une personne morale (art. 105 § 1)");
        }
        if (recidive) {
            sb.append(" ; la récidive dans l'année à compter de la"
                    + " condamnation pénale ou de la décision"
                    + " administrative définitive antérieure pour une"
                    + " infraction de même niveau emporte doublement"
                    + " du plafond de l'amende (art. 110 C. pén. soc.)");
        }
        sb.append(".");
        return sb.toString();
    }

    private static TravailNoirBeDimonaResult build(
            TravailNoirBeDimonaRequest req,
            TravailNoirBeDimonaVerdict verdict,
            long jours,
            BigDecimal cotEmp, BigDecimal cotTrav, BigDecimal cotTotal,
            BigDecimal amendeOnss,
            boolean penaleN4,
            int adminMin, int adminMax, int penalMin, int penalMax,
            boolean emp, int empMin, int empMax,
            boolean multT, boolean majPM, boolean majR,
            String raison, String synthese) {
        return new TravailNoirBeDimonaResult(
                req.statutDimona(),
                req.dateDebutOccupation(),
                req.dateDimonaEffective(),
                req.dateControle(),
                req.salaireBrutMensuel(),
                req.nombreTravailleursConcernes(),
                req.personneMorale(),
                req.recidiveDansLAn(),
                req.elementsSubordination(),
                verdict,
                jours,
                cotEmp, cotTrav, cotTotal, amendeOnss,
                penaleN4,
                adminMin, adminMax, penalMin, penalMax,
                emp, empMin, empMax,
                multT, majPM, majR,
                raison, synthese,
                BASE_JURIDIQUE, AVERT_AVOCAT_BE);
    }

    private static void validateInputs(TravailNoirBeDimonaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.statutDimona() == null) {
            throw new IllegalArgumentException("statutDimona est requis");
        }
        if (request.dateDebutOccupation() == null) {
            throw new IllegalArgumentException(
                    "dateDebutOccupation est requis");
        }
        if (request.dateControle() == null) {
            throw new IllegalArgumentException("dateControle est requis");
        }
        if (request.dateControle().isBefore(request.dateDebutOccupation())) {
            throw new IllegalArgumentException(
                    "dateControle doit être ≥ dateDebutOccupation");
        }
        if (request.salaireBrutMensuel() == null) {
            throw new IllegalArgumentException(
                    "salaireBrutMensuel est requis");
        }
        if (request.salaireBrutMensuel().signum() < 0) {
            throw new IllegalArgumentException(
                    "salaireBrutMensuel doit être positif ou nul");
        }
        if (request.nombreTravailleursConcernes() == null
                || request.nombreTravailleursConcernes() < 1) {
            throw new IllegalArgumentException(
                    "nombreTravailleursConcernes doit être ≥ 1");
        }
        if (request.personneMorale() == null) {
            throw new IllegalArgumentException("personneMorale est requis");
        }
        if (request.recidiveDansLAn() == null) {
            throw new IllegalArgumentException(
                    "recidiveDansLAn est requis");
        }
        if (request.elementsSubordination() == null) {
            throw new IllegalArgumentException(
                    "elementsSubordination est requis");
        }
        // Si TARDIVE : dateDimonaEffective doit être renseignée
        if (request.statutDimona()
                == TravailNoirBeDimonaStatutDimona.DECLAREE_TARDIVE
                && request.dateDimonaEffective() == null) {
            throw new IllegalArgumentException(
                    "dateDimonaEffective est requis lorsque"
                            + " statutDimona = DECLAREE_TARDIVE");
        }
    }
}
