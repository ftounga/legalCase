package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * SF-219-11 : vérificateur du droit au <i>congé-éducation payé
 * régionalisé</i> — fonction <b>pure</b> (sans dépendance HTTP /
 * persistance / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 22/01/1985 contenant des dispositions sociales</b>,
 *       Section 6 « Congé-éducation payé » (art. 108 à 117) — base
 *       fédérale historique.</li>
 *   <li><b>Loi spéciale du 06/01/2014</b> (6e réforme de l'État) —
 *       transfert de la compétence aux Régions au 01/07/2014. Les
 *       trois Régions ont depuis adopté leur propre régime :
 *       <ul>
 *         <li>Wallonie : Décret du 19/02/2014 + Arrêté GW
 *             19/12/2014.</li>
 *         <li>Flandre : Décret du 12/12/2014 (Vlaams
 *             Opleidingsverlof — VOV), en vigueur 01/09/2019.</li>
 *         <li>Bruxelles-Capitale : Ordonnance du 02/07/2015 +
 *             Arrêté GBR 14/04/2016.</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h2>Hiérarchie des verdicts</h2>
 * <ol>
 *   <li><b>INELIGIBLE_HORS_FORMATION_AGREEE</b> — formation absente
 *       de la liste agréée régionale → aucun droit (sortie immédiate).</li>
 *   <li><b>INELIGIBLE_OCCUPATION_INSUFFISANTE</b> — occupation sous
 *       le seuil régional (généralement mi-temps), sauf dérogation
 *       publics fragilisés en Flandre.</li>
 *   <li>Sinon : <b>ELIGIBLE_PLEIN_DROIT</b> (temps plein) ou
 *       <b>ELIGIBLE_PRORATA</b> (temps partiel ≥ seuil) — le plafond
 *       régional applicable dépend du type de formation et de la
 *       région.</li>
 * </ol>
 *
 * <h2>Plafonds régionaux applicables (post-réforme 2014)</h2>
 * <table>
 *   <tr><th>Type formation</th><th>WBR</th><th>FLA</th><th>BXL</th></tr>
 *   <tr><td>GENERALE</td><td>120 h</td><td>125 h</td><td>80 h</td></tr>
 *   <tr><td>PROFESSIONNELLE_QUALIFIANTE</td><td>180 h</td><td>125 h</td><td>100 h</td></tr>
 *   <tr><td>ENSEIGNEMENT_SUPERIEUR_ALTERNANCE</td><td>180 h</td><td>250 h</td><td>100 h</td></tr>
 *   <tr><td>RECONVERSION_PUBLIC_FRAGILISE</td><td>180 h</td><td>180 h</td><td>120 h</td></tr>
 * </table>
 *
 * <p>Les plafonds définitifs et la liste précise des formations
 * agréées doivent être confirmés par un avocat BE en fonction de
 * l'année de référence (les arrêtés régionaux sont périodiquement
 * révisés) — l'avertissement systématique le rappelle.</p>
 */
public final class CongeEducationPayeRegionChecker {

    static final String BASE_JURIDIQUE =
            "Loi du 22/01/1985 contenant des dispositions sociales, Section 6"
                    + " « Congé-éducation payé » (art. 108 à 117) — base"
                    + " fédérale historique ; Loi spéciale du 06/01/2014"
                    + " (6e réforme de l'État, transfert de compétence aux"
                    + " Régions au 01/07/2014) ; Décret WBR du 19/02/2014 +"
                    + " Arrêté GW du 19/12/2014 (Wallonie) ; Décret du"
                    + " 12/12/2014 instituant le Vlaams Opleidingsverlof —"
                    + " VOV (Flandre, en vigueur 01/09/2019) ; Ordonnance"
                    + " du 02/07/2015 + Arrêté GBR du 14/04/2016 (Région"
                    + " de Bruxelles-Capitale).";

    static final String AVERT_REGIONAL_AVOCAT =
            "Les plafonds d'heures et la liste des formations agréées sont"
                    + " fixés par arrêté régional, périodiquement révisés. La"
                    + " région compétente est celle du lieu de travail (pas"
                    + " du domicile). Pour un travailleur changeant de région"
                    + " en cours d'année, une analyse au cas par cas est"
                    + " requise (avocat BE spécialisé droit social). La"
                    + " présente analyse n'inclut pas le remboursement"
                    + " employeur via le Fonds régional concerné — circuit"
                    + " administratif distinct.";

    /** Seuil d'occupation minimum (mi-temps en règle générale). */
    static final BigDecimal SEUIL_OCCUPATION_MI_TEMPS = new BigDecimal("0.5");

    /** Seuil dérogatoire FLA pour publics fragilisés (1/5 temps). */
    static final BigDecimal SEUIL_OCCUPATION_FLA_FRAGILISE = new BigDecimal("0.2");

    /** Heures effectivement allouées = min(heures formation, plafond régional × taux occupation). */
    private CongeEducationPayeRegionChecker() {
    }

    public static CongeEducationPayeRegionResult analyze(
            CongeEducationPayeRegionRequest request) {
        validateInputs(request);

        String regime = nomRegimeRegional(request.region());

        // ── Hiérarchie 1 : formation hors liste agréée régionale ─────────
        if (!request.formationAgreeeRegion()
                || request.typeFormation()
                        == CongeEducationPayeRegionTypeFormation.HORS_LISTE_AGREEE) {
            return ineligible(request, regime,
                    CongeEducationPayeRegionVerdict.INELIGIBLE_HORS_FORMATION_AGREEE,
                    "FORMATION_HORS_LISTE_AGREEE",
                    "La formation suivie n'est pas inscrite à la liste"
                            + " agréée de la Région " + regime + ". Le"
                            + " congé-éducation payé est strictement réservé"
                            + " aux formations agréées (tenue par le ministère"
                            + " régional de l'emploi / formation, liste"
                            + " périodiquement mise à jour). Vérifier la liste"
                            + " officielle pour l'année de référence avant"
                            + " toute demande de congé à l'employeur.");
        }

        // ── Hiérarchie 2 : occupation insuffisante ──────────────────────
        BigDecimal seuilApplicable = (request.region()
                == CongeEducationPayeRegionRegion.FLANDRE
                && request.publicFragilise())
                ? SEUIL_OCCUPATION_FLA_FRAGILISE
                : SEUIL_OCCUPATION_MI_TEMPS;

        if (request.tauxOccupation().compareTo(seuilApplicable) < 0) {
            String motif = (request.region()
                    == CongeEducationPayeRegionRegion.FLANDRE
                    && request.publicFragilise())
                    ? "même avec la dérogation FLA \"public fragilisé\" (seuil"
                            + " ramené à " + SEUIL_OCCUPATION_FLA_FRAGILISE
                            + "), le taux d'occupation de "
                            + request.tauxOccupation() + " reste insuffisant"
                    : "taux d'occupation de " + request.tauxOccupation()
                            + " sous le seuil mi-temps ("
                            + SEUIL_OCCUPATION_MI_TEMPS + ") requis en région "
                            + regime;
            return ineligible(request, regime,
                    CongeEducationPayeRegionVerdict.INELIGIBLE_OCCUPATION_INSUFFISANTE,
                    "OCCUPATION_INSUFFISANTE_SEUIL_REGIONAL",
                    "Aucun droit au congé-éducation payé : " + motif + ". Le"
                            + " congé-éducation payé est conditionné à une"
                            + " occupation minimale (mi-temps en règle"
                            + " générale ; 1/5 temps en Flandre pour les"
                            + " publics fragilisés peu qualifiés au sens"
                            + " du décret VOV).");
        }

        // ── Cas qualifiant : calcul du plafond régional + prorata ─────────
        int plafondRegional = plafondRegional(request.region(),
                request.typeFormation());

        // Heures formation plafonnées par le plafond régional puis
        // proratisées au taux d'occupation.
        int plafondProratise = request.tauxOccupation()
                .multiply(BigDecimal.valueOf(plafondRegional))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        int heuresAllouees = Math.min(request.heuresFormationAnnuelles(),
                plafondProratise);

        boolean tempsPlein = request.tauxOccupation()
                .compareTo(BigDecimal.ONE) >= 0;

        CongeEducationPayeRegionVerdict verdict = tempsPlein
                ? CongeEducationPayeRegionVerdict.ELIGIBLE_PLEIN_DROIT
                : CongeEducationPayeRegionVerdict.ELIGIBLE_PRORATA;

        String synthese = "Droit au congé-éducation payé reconnu en région "
                + regime + " (régime " + regimeDecretLibelle(request.region())
                + ") : " + heuresAllouees + " h de congé allouées sur "
                + request.heuresFormationAnnuelles() + " h de formation"
                + " demandées (plafond régional applicable : "
                + plafondRegional + " h pour le type "
                + request.typeFormation() + ", proratisé au taux d'occupation "
                + request.tauxOccupation() + " → " + plafondProratise
                + " h). " + (tempsPlein
                        ? "Travailleur à temps plein — accès au plafond"
                                + " régional intégral."
                        : "Travailleur à temps partiel ≥ seuil mi-temps —"
                                + " plafond proratisé.");

        return new CongeEducationPayeRegionResult(
                request.region(),
                request.typeFormation(),
                request.heuresFormationAnnuelles(),
                request.tauxOccupation(),
                request.publicFragilise(),
                request.formationAgreeeRegion(),
                verdict,
                plafondRegional,
                heuresAllouees,
                regime,
                "DROIT_RECONNU",
                synthese,
                BASE_JURIDIQUE,
                AVERT_REGIONAL_AVOCAT);
    }

    // ── Construction d'un résultat inéligible ──────────────────────────────

    private static CongeEducationPayeRegionResult ineligible(
            CongeEducationPayeRegionRequest request,
            String regime,
            CongeEducationPayeRegionVerdict verdict,
            String raison, String synthese) {
        return new CongeEducationPayeRegionResult(
                request.region(),
                request.typeFormation(),
                request.heuresFormationAnnuelles(),
                request.tauxOccupation(),
                request.publicFragilise(),
                request.formationAgreeeRegion(),
                verdict,
                0,
                0,
                regime,
                raison,
                synthese,
                BASE_JURIDIQUE,
                AVERT_REGIONAL_AVOCAT);
    }

    // ── Branchement régional du plafond d'heures ──────────────────────────

    static int plafondRegional(CongeEducationPayeRegionRegion region,
                               CongeEducationPayeRegionTypeFormation type) {
        return switch (region) {
            case WALLONIE -> switch (type) {
                case GENERALE -> 120;
                case PROFESSIONNELLE_QUALIFIANTE,
                        ENSEIGNEMENT_SUPERIEUR_ALTERNANCE,
                        RECONVERSION_PUBLIC_FRAGILISE -> 180;
                case HORS_LISTE_AGREEE -> 0;
            };
            case FLANDRE -> switch (type) {
                case GENERALE, PROFESSIONNELLE_QUALIFIANTE -> 125;
                case ENSEIGNEMENT_SUPERIEUR_ALTERNANCE -> 250;
                case RECONVERSION_PUBLIC_FRAGILISE -> 180;
                case HORS_LISTE_AGREEE -> 0;
            };
            case BRUXELLES -> switch (type) {
                case GENERALE -> 80;
                case PROFESSIONNELLE_QUALIFIANTE,
                        ENSEIGNEMENT_SUPERIEUR_ALTERNANCE -> 100;
                case RECONVERSION_PUBLIC_FRAGILISE -> 120;
                case HORS_LISTE_AGREEE -> 0;
            };
        };
    }

    private static String nomRegimeRegional(
            CongeEducationPayeRegionRegion region) {
        return switch (region) {
            case WALLONIE -> "Wallonie";
            case FLANDRE -> "Flandre";
            case BRUXELLES -> "Bruxelles-Capitale";
        };
    }

    private static String regimeDecretLibelle(
            CongeEducationPayeRegionRegion region) {
        return switch (region) {
            case WALLONIE -> "Décret WBR 19/02/2014 + AGW 19/12/2014";
            case FLANDRE -> "Vlaams Opleidingsverlof VOV (Décret 12/12/2014)";
            case BRUXELLES -> "Ordonnance 02/07/2015 + AGBR 14/04/2016";
        };
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    private static void validateInputs(
            CongeEducationPayeRegionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.region() == null) {
            throw new IllegalArgumentException("region est requise");
        }
        if (request.typeFormation() == null) {
            throw new IllegalArgumentException("typeFormation est requis");
        }
        if (request.heuresFormationAnnuelles() == null) {
            throw new IllegalArgumentException(
                    "heuresFormationAnnuelles est requis");
        }
        if (request.heuresFormationAnnuelles() < 0) {
            throw new IllegalArgumentException(
                    "heuresFormationAnnuelles doit être positif ou nul");
        }
        if (request.tauxOccupation() == null) {
            throw new IllegalArgumentException("tauxOccupation est requis");
        }
        if (request.tauxOccupation().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "tauxOccupation doit être positif ou nul");
        }
        if (request.tauxOccupation().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(
                    "tauxOccupation doit être au plus 1.0");
        }
        if (request.publicFragilise() == null) {
            throw new IllegalArgumentException("publicFragilise est requis");
        }
        if (request.formationAgreeeRegion() == null) {
            throw new IllegalArgumentException(
                    "formationAgreeeRegion est requis");
        }
    }
}
