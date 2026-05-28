package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;

/**
 * SF-219-17 : validateur de clause d'écolage BE (art. 22bis Loi du
 * 03/07/1978 sur les contrats de travail, introduit par la Loi du
 * 27/12/2006 portant des dispositions diverses I — M.B. 28/12/2006)
 * — fonction <b>pure</b> (sans dépendance HTTP / persistance /
 * horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 03/07/1978 sur les contrats de travail, art. 22bis</b>
 *       (inséré par la Loi du 27/12/2006 portant des dispositions
 *       diverses I, M.B. 28/12/2006, et modifié par la Loi du
 *       22/12/2017 — Pacte de compétitivité) :
 *       <ul>
 *         <li>§ 1er, al. 1er : forme écrite obligatoire <i>au plus tard
 *             à l'entrée en formation</i> reprenant la description de
 *             la formation, sa durée, le lieu, le coût réel, la durée
 *             d'efficacité de la clause et le montant dégressif du
 *             remboursement.</li>
 *         <li>§ 2 : la formation doit être <i>spécifique</i> ; elle ne
 *             peut résulter d'une obligation légale ou réglementaire
 *             (recyclage obligatoire, formation sécurité imposée).
 *             Le coût réel doit excéder <b>½ × RMMMG mensuel</b>
 *             (revenu minimum mensuel moyen garanti, CCT n° 43 du CNT,
 *             indexé annuellement).</li>
 *         <li>§ 3 : la clause ne peut être actionnée en cas de rupture
 *             du contrat <i>du fait de l'employeur</i> (licenciement
 *             sans motif grave), de rupture pour motif grave dans le
 *             chef de l'employeur, ni à l'expiration normale d'un CDD.
 *             Seule la démission volontaire du travailleur ou le
 *             licenciement pour motif grave imputable à celui-ci peut
 *             déclencher le remboursement.</li>
 *         <li>§ 4, al. 1er : <b>durée d'efficacité maximale 3 ans</b>
 *             (36 mois) à compter de la fin de la formation.</li>
 *         <li>§ 4, al. 2 : <b>dégressivité par tiers</b> de la durée
 *             d'efficacité : 100 % la 1<sup>re</sup> période, 2/3
 *             (≈ 66,67 %) la 2<sup>e</sup>, 1/3 (≈ 33,33 %) la
 *             3<sup>e</sup>, rien au-delà.</li>
 *         <li>§ 4, al. 4 : <b>plafond absolu 80 %</b> du coût réel de
 *             la formation — quelle que soit la formule retenue.</li>
 *       </ul>
 *   </li>
 *   <li><b>CCT n° 13 du 02/02/2013</b> (CNT) — encadre subsidiairement
 *       l'écolage dans le cadre des formations qualifiantes
 *       sectorielles. Référence informative ; le cadre légal de
 *       l'art. 22bis prime.</li>
 *   <li><b>CCT n° 43 du 02/05/1988</b> (CNT) — RMMMG indexé
 *       annuellement, paramétré par le présent outil
 *       ({@code rmmmgMensuelEuros}) pour rester ajustable selon la
 *       date de signature de la clause.</li>
 * </ul>
 *
 * <h2>Hiérarchie des verdicts</h2>
 * <ol>
 *   <li><b>A_ANALYSER</b> — type formation
 *       {@link ClauseEcolageBeTypeFormation#INDETERMINE} (sortie
 *       immédiate).</li>
 *   <li><b>INOPPOSABLE_MOTIF_DEPART</b> — motif de départ exclu par
 *       l'art. 22bis § 3 (licenciement employeur sans motif grave,
 *       rupture motif grave employeur, fin CDD normale). Verdict
 *       prioritaire car il rend caduque toute analyse de validité —
 *       aucune somme exigible.</li>
 *   <li><b>NULLE_FORMATION_OBLIGATOIRE</b> — type
 *       {@link ClauseEcolageBeTypeFormation#OBLIGATOIRE_LEGALE}
 *       (art. 22bis § 2).</li>
 *   <li><b>NULLE_FORME_ECRITE_MANQUANTE</b> — clause non écrite à
 *       l'entrée en formation (art. 22bis § 1er, al. 1er).</li>
 *   <li><b>NULLE_COUT_INSUFFISANT</b> — coût réel ≤ ½ × RMMMG
 *       (art. 22bis § 2, al. 3, 2°).</li>
 *   <li><b>NULLE_DUREE_EXCESSIVE</b> — durée d'efficacité &gt; 36 mois
 *       (art. 22bis § 4, al. 1er).</li>
 *   <li><b>VALIDE_DUREE_EXPIREE</b> — clause valable formellement mais
 *       la durée d'efficacité est déjà expirée à la date de départ —
 *       aucun remboursement exigible.</li>
 *   <li><b>VALIDE_REMBOURSEMENT_DEGRESSIF</b> — clause valable, le
 *       montant dû final correspond à la quotité dégressive (100/66/33 %)
 *       du coût réel, plafonné à 80 %.</li>
 * </ol>
 */
public final class ClauseEcolageBeValidator {

    /** Durée d'efficacité maximale d'une clause d'écolage BE (mois). */
    public static final int DUREE_MAX_MOIS = 36;

    /** Plafond légal absolu du remboursement (art. 22bis § 4, al. 4) — 80 %. */
    public static final BigDecimal PLAFOND_RATIO = new BigDecimal("0.80");

    /** Ratio dû la 1<sup>re</sup> tranche : 100 %. */
    public static final BigDecimal QUOTITE_TIER_1 = new BigDecimal("1.0000");

    /** Ratio dû la 2<sup>e</sup> tranche : 2/3 ≈ 66,67 %. */
    public static final BigDecimal QUOTITE_TIER_2 = new BigDecimal("0.6667");

    /** Ratio dû la 3<sup>e</sup> tranche : 1/3 ≈ 33,33 %. */
    public static final BigDecimal QUOTITE_TIER_3 = new BigDecimal("0.3333");

    /** Ratio dû au-delà de la durée d'efficacité : 0 %. */
    public static final BigDecimal QUOTITE_EXPIREE = new BigDecimal("0.0000");

    static final String BASE_JURIDIQUE =
            "Loi du 03/07/1978 sur les contrats de travail, art. 22bis"
                    + " (inséré par la Loi du 27/12/2006 portant des"
                    + " dispositions diverses I, M.B. 28/12/2006, et"
                    + " modifié par la Loi du 22/12/2017 — Pacte de"
                    + " compétitivité) : § 1er al. 1er forme écrite"
                    + " obligatoire au plus tard à l'entrée en formation ;"
                    + " § 2 formation spécifique (non imposée par norme"
                    + " légale ou réglementaire) + coût réel strictement"
                    + " supérieur à la moitié du revenu minimum mensuel"
                    + " moyen garanti (RMMMG, CCT n° 43 du CNT) ; § 3"
                    + " clause inopposable en cas de licenciement sans"
                    + " motif grave par l'employeur, de rupture pour"
                    + " motif grave dans le chef de l'employeur ou"
                    + " d'expiration normale d'un CDD ; § 4 al. 1er"
                    + " durée d'efficacité maximale 3 ans (36 mois) à"
                    + " compter de la fin de la formation ; § 4 al. 2"
                    + " dégressivité par tiers de la durée d'efficacité"
                    + " (100 % la 1re période, 2/3 la 2e, 1/3 la 3e,"
                    + " rien au-delà) ; § 4 al. 4 plafond absolu 80 %"
                    + " du coût réel de la formation."
                    + " CCT n° 43 du 02/05/1988 du CNT (RMMMG indexé"
                    + " annuellement)."
                    + " CCT n° 13 du 02/02/2013 du CNT (cadre"
                    + " subsidiaire pour formations qualifiantes"
                    + " sectorielles).";

    static final String AVERT_AVOCAT_BE =
            "Le montant du RMMMG mensuel (CCT n° 43 du CNT) est indexé"
                    + " annuellement ; la valeur applicable est celle en"
                    + " vigueur à la date de signature de la clause (à"
                    + " confirmer par un avocat spécialisé en droit"
                    + " social BE — voir circulaire SPF Emploi pour la"
                    + " période considérée). La clause d'écolage exige"
                    + " un écrit individualisé reprenant la description"
                    + " de la formation, sa durée, le lieu, le coût"
                    + " réel, la durée d'efficacité de la clause et le"
                    + " montant dégressif du remboursement (art. 22bis"
                    + " § 1er, al. 1er) ; le défaut d'écrit avant"
                    + " l'entrée en formation entraîne la nullité de"
                    + " plein droit. Le calcul exact des tiers de la"
                    + " durée d'efficacité doit tenir compte de"
                    + " l'arrondi conventionnel des durées (CCT"
                    + " sectorielle applicable, usage du Bureau de"
                    + " conciliation). En cas de doute sur la"
                    + " qualification 'formation spécifique' vs"
                    + " 'formation obligatoire', vérifier l'arrêté"
                    + " royal ou la CCT qui impose la formation"
                    + " (recyclage agréments AR 25/01/2001, formation"
                    + " sécurité Code du bien-être au travail Livre I /"
                    + " Titre 2) — la jurisprudence Cass. (BE) écarte la"
                    + " clause dès lors qu'une norme oblige"
                    + " l'employeur à dispenser la formation.";

    private ClauseEcolageBeValidator() {
    }

    public static ClauseEcolageBeResult validate(ClauseEcolageBeRequest request) {
        validateInputs(request);

        BigDecimal coutMinLegal = request.rmmmgMensuelEuros()
                .divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        BigDecimal coutReel = request.coutReelFormationEuros()
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal plafond80 = coutReel.multiply(PLAFOND_RATIO)
                .setScale(2, RoundingMode.HALF_UP);

        int moisEcoules = computeMoisEntiers(
                request.dateFinFormation(), request.dateDepartTravailleur());
        int tier = computeTier(moisEcoules, request.dureeEfficaciteMois());
        BigDecimal quotite = quotiteFromTier(tier);
        BigDecimal montantBrut = coutReel.multiply(quotite)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal montantFinal = montantBrut.min(plafond80)
                .setScale(2, RoundingMode.HALF_UP);

        boolean isIndetermine =
                request.typeFormation() == ClauseEcolageBeTypeFormation.INDETERMINE;
        boolean isObligatoire =
                request.typeFormation() == ClauseEcolageBeTypeFormation.OBLIGATOIRE_LEGALE;
        boolean motifInopposable = isMotifInopposable(request.motifDepart());

        // ── Hiérarchie 1 : type indéterminé → A_ANALYSER (sortie immédiate) ─────
        if (isIndetermine) {
            return build(request, ClauseEcolageBeVerdict.A_ANALYSER,
                    coutMinLegal, moisEcoules, tier, quotite, montantBrut,
                    plafond80, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    "TYPE_FORMATION_INDETERMINE",
                    "Qualification de la formation indéterminée"
                            + " (spécifique au sens de l'art. 22bis § 2 ?"
                            + " obligatoire en vertu d'une norme légale"
                            + " ou réglementaire ?). Analyse au cas par"
                            + " cas requise : vérifier l'AR / la CCT"
                            + " sectorielle qui impose éventuellement la"
                            + " formation avant de qualifier la clause.");
        }

        // ── Hiérarchie 2 : motif inopposable (art. 22bis § 3) ───────────────────
        if (motifInopposable) {
            return build(request, ClauseEcolageBeVerdict.INOPPOSABLE_MOTIF_DEPART,
                    coutMinLegal, moisEcoules, tier, quotite, montantBrut,
                    plafond80, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    "MOTIF_DEPART_EXCLU",
                    "Motif de départ exclu par l'art. 22bis § 3 de la"
                            + " Loi du 03/07/1978 : "
                            + describeMotif(request.motifDepart())
                            + ". La clause d'écolage est inopposable au"
                            + " travailleur — l'employeur ne peut"
                            + " réclamer aucune somme,"
                            + " indépendamment de la régularité"
                            + " formelle de la clause et du montant"
                            + " théorique dégressif.");
        }

        // ── Hiérarchie 3 : formation obligatoire (art. 22bis § 2) ───────────────
        if (isObligatoire) {
            return build(request, ClauseEcolageBeVerdict.NULLE_FORMATION_OBLIGATOIRE,
                    coutMinLegal, moisEcoules, tier, quotite, montantBrut,
                    plafond80, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    "FORMATION_OBLIGATOIRE_LEGALE",
                    "Formation imposée par une norme légale ou"
                            + " réglementaire (AR de recyclage, CCT"
                            + " sectorielle obligatoire, Code du"
                            + " bien-être au travail) — l'art. 22bis"
                            + " § 2 de la Loi du 03/07/1978 exclut"
                            + " expressément qu'une telle formation"
                            + " fasse l'objet d'une clause d'écolage."
                            + " Clause nulle de plein droit.");
        }

        // ── Hiérarchie 4 : forme écrite manquante (art. 22bis § 1er) ────────────
        if (!request.clauseEcriteAvantEntreeFormation()) {
            return build(request, ClauseEcolageBeVerdict.NULLE_FORME_ECRITE_MANQUANTE,
                    coutMinLegal, moisEcoules, tier, quotite, montantBrut,
                    plafond80, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    "FORME_ECRITE_MANQUANTE",
                    "Aucune clause écrite n'a été établie au plus"
                            + " tard à l'entrée en formation (art."
                            + " 22bis § 1er, al. 1er de la Loi du"
                            + " 03/07/1978). L'écrit individualisé,"
                            + " reprenant la description de la"
                            + " formation, sa durée, le lieu, le coût"
                            + " réel, la durée d'efficacité de la"
                            + " clause et le montant dégressif du"
                            + " remboursement, est une condition de"
                            + " validité substantielle. Clause nulle"
                            + " de plein droit.");
        }

        // ── Hiérarchie 5 : coût insuffisant (art. 22bis § 2, al. 3, 2°) ─────────
        if (coutReel.compareTo(coutMinLegal) <= 0) {
            return build(request, ClauseEcolageBeVerdict.NULLE_COUT_INSUFFISANT,
                    coutMinLegal, moisEcoules, tier, quotite, montantBrut,
                    plafond80, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    "COUT_INFERIEUR_MOITIE_RMMMG",
                    "Le coût réel de la formation (" + coutReel
                            + " €) n'excède pas la moitié du revenu"
                            + " minimum mensuel moyen garanti — RMMMG"
                            + " (½ × " + request.rmmmgMensuelEuros()
                            + " € = " + coutMinLegal + " €,"
                            + " CCT n° 43 du CNT). L'art. 22bis § 2,"
                            + " al. 3, 2° de la Loi du 03/07/1978"
                            + " exige un coût strictement supérieur à"
                            + " ce seuil. Clause nulle de plein"
                            + " droit.");
        }

        // ── Hiérarchie 6 : durée excessive (art. 22bis § 4, al. 1er) ────────────
        if (request.dureeEfficaciteMois() > DUREE_MAX_MOIS) {
            return build(request, ClauseEcolageBeVerdict.NULLE_DUREE_EXCESSIVE,
                    coutMinLegal, moisEcoules, tier, quotite, montantBrut,
                    plafond80, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    "DUREE_EFFICACITE_EXCESSIVE",
                    "La durée d'efficacité de la clause ("
                            + request.dureeEfficaciteMois()
                            + " mois) dépasse le plafond légal de 36"
                            + " mois (3 ans) fixé par l'art. 22bis § 4,"
                            + " al. 1er de la Loi du 03/07/1978."
                            + " Clause nulle de plein droit.");
        }

        // ── Hiérarchie 7 : durée expirée ────────────────────────────────────────
        if (tier > 3) {
            return build(request, ClauseEcolageBeVerdict.VALIDE_DUREE_EXPIREE,
                    coutMinLegal, moisEcoules, tier, quotite, montantBrut,
                    plafond80, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    "DUREE_EFFICACITE_EXPIREE",
                    "La clause est formellement valable, mais la"
                            + " durée d'efficacité ("
                            + request.dureeEfficaciteMois()
                            + " mois) est expirée à la date de départ"
                            + " ("
                            + moisEcoules
                            + " mois écoulés depuis la fin de la"
                            + " formation, > "
                            + request.dureeEfficaciteMois()
                            + " mois). Aucun remboursement n'est"
                            + " exigible — art. 22bis § 4, al. 2 in"
                            + " fine.");
        }

        // ── Cas qualifiant : valide, remboursement dégressif plafonné ───────────
        String synthese = "Clause d'écolage valable au sens de l'art."
                + " 22bis de la Loi du 03/07/1978. Départ au tier "
                + tier + "/3 de la durée d'efficacité (" + moisEcoules
                + " mois écoulés sur " + request.dureeEfficaciteMois()
                + " mois) → quotité due "
                + ratioPercentDisplay(quotite)
                + " × " + coutReel + " € = " + montantBrut + " €"
                + (montantFinal.compareTo(montantBrut) < 0
                        ? ", plafonnée à 80 % du coût réel = "
                                + plafond80 + " €. Montant dû final : "
                                + montantFinal + " €."
                        : ". Montant dû final : " + montantFinal + " €"
                                + " (plafond 80 % = " + plafond80
                                + " € non atteint).");
        return build(request, ClauseEcolageBeVerdict.VALIDE_REMBOURSEMENT_DEGRESSIF,
                coutMinLegal, moisEcoules, tier, quotite, montantBrut,
                plafond80, montantFinal,
                "VALIDE_DEGRESSIF",
                synthese);
    }

    /**
     * Nombre de mois entiers écoulés entre la fin de la formation et le
     * départ (≥ 0). Si le départ est antérieur à la fin de la formation
     * (configuration anormale), renvoie 0.
     */
    private static int computeMoisEntiers(java.time.LocalDate fin,
                                          java.time.LocalDate depart) {
        if (depart.isBefore(fin)) {
            return 0;
        }
        long mois = ChronoUnit.MONTHS.between(fin, depart);
        return (int) Math.max(0L, mois);
    }

    /**
     * Tier (1 / 2 / 3 / 4) dans lequel se situe le départ par rapport à
     * la dégressivité de l'art. 22bis § 4, al. 2 :
     * <ul>
     *   <li>tier 1 (100 %) : départ dans le 1<sup>er</sup> tiers ;</li>
     *   <li>tier 2 (66 %) : 2<sup>e</sup> tiers ;</li>
     *   <li>tier 3 (33 %) : 3<sup>e</sup> tiers ;</li>
     *   <li>tier 4 (0 %) : au-delà de la durée d'efficacité.</li>
     * </ul>
     */
    static int computeTier(int moisEcoules, int dureeMois) {
        if (moisEcoules >= dureeMois) {
            return 4;
        }
        // Bornes des tiers (arrondi math : double permis ici car borne
        // entière, pas un montant monétaire). dureeMois/3 ; 2×dureeMois/3.
        int borne1 = dureeMois / 3;
        int borne2 = (2 * dureeMois) / 3;
        if (moisEcoules < borne1) {
            return 1;
        }
        if (moisEcoules < borne2) {
            return 2;
        }
        return 3;
    }

    private static BigDecimal quotiteFromTier(int tier) {
        return switch (tier) {
            case 1 -> QUOTITE_TIER_1;
            case 2 -> QUOTITE_TIER_2;
            case 3 -> QUOTITE_TIER_3;
            default -> QUOTITE_EXPIREE;
        };
    }

    private static boolean isMotifInopposable(ClauseEcolageBeMotifDepart motif) {
        return motif == ClauseEcolageBeMotifDepart.LICENCIEMENT_EMPLOYEUR_SANS_MOTIF_GRAVE
                || motif == ClauseEcolageBeMotifDepart.RUPTURE_MOTIF_GRAVE_EMPLOYEUR
                || motif == ClauseEcolageBeMotifDepart.FIN_CDD_NORMALE;
    }

    private static String describeMotif(ClauseEcolageBeMotifDepart motif) {
        return switch (motif) {
            case LICENCIEMENT_EMPLOYEUR_SANS_MOTIF_GRAVE ->
                    "licenciement sans motif grave par l'employeur";
            case RUPTURE_MOTIF_GRAVE_EMPLOYEUR ->
                    "rupture pour motif grave dans le chef de l'employeur";
            case FIN_CDD_NORMALE ->
                    "expiration normale d'un contrat à durée déterminée";
            case DEMISSION_TRAVAILLEUR -> "démission volontaire du travailleur";
            case LICENCIEMENT_MOTIF_GRAVE_TRAVAILLEUR ->
                    "licenciement pour motif grave imputable au travailleur";
        };
    }

    private static String ratioPercentDisplay(BigDecimal quotite) {
        // Format simple humain : 100 % / 66 % / 33 % / 0 % (sans décimales).
        BigDecimal pct = quotite.multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP);
        return pct + " %";
    }

    private static ClauseEcolageBeResult build(
            ClauseEcolageBeRequest req,
            ClauseEcolageBeVerdict verdict,
            BigDecimal coutMinLegal,
            int moisEcoules,
            int tier,
            BigDecimal quotite,
            BigDecimal montantBrut,
            BigDecimal plafond80,
            BigDecimal montantFinal,
            String raison,
            String synthese) {
        return new ClauseEcolageBeResult(
                req.typeFormation(),
                req.clauseEcriteAvantEntreeFormation(),
                req.coutReelFormationEuros().setScale(2, RoundingMode.HALF_UP),
                req.rmmmgMensuelEuros().setScale(2, RoundingMode.HALF_UP),
                req.dureeEfficaciteMois(),
                req.dateFinFormation(),
                req.dateDepartTravailleur(),
                req.motifDepart(),
                verdict,
                coutMinLegal,
                moisEcoules,
                tier,
                quotite,
                montantBrut,
                plafond80,
                montantFinal,
                raison,
                synthese,
                BASE_JURIDIQUE,
                AVERT_AVOCAT_BE);
    }

    private static void validateInputs(ClauseEcolageBeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.typeFormation() == null) {
            throw new IllegalArgumentException("typeFormation est requis");
        }
        if (request.clauseEcriteAvantEntreeFormation() == null) {
            throw new IllegalArgumentException(
                    "clauseEcriteAvantEntreeFormation est requise");
        }
        if (request.coutReelFormationEuros() == null) {
            throw new IllegalArgumentException(
                    "coutReelFormationEuros est requis");
        }
        if (request.coutReelFormationEuros().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "coutReelFormationEuros doit être positif ou nul");
        }
        if (request.rmmmgMensuelEuros() == null) {
            throw new IllegalArgumentException(
                    "rmmmgMensuelEuros est requis");
        }
        if (request.rmmmgMensuelEuros().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "rmmmgMensuelEuros doit être strictement positif");
        }
        if (request.dureeEfficaciteMois() == null) {
            throw new IllegalArgumentException(
                    "dureeEfficaciteMois est requise");
        }
        if (request.dureeEfficaciteMois() < 1) {
            throw new IllegalArgumentException(
                    "dureeEfficaciteMois doit être ≥ 1");
        }
        if (request.dateFinFormation() == null) {
            throw new IllegalArgumentException(
                    "dateFinFormation est requise");
        }
        if (request.dateDepartTravailleur() == null) {
            throw new IllegalArgumentException(
                    "dateDepartTravailleur est requise");
        }
        if (request.motifDepart() == null) {
            throw new IllegalArgumentException("motifDepart est requis");
        }
    }
}
