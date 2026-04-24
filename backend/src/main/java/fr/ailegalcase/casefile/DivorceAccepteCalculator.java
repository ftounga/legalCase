package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-FA-10-01 : calculateur pour le divorce accepté / acceptation du principe
 * de la rupture (art. <b>233-234 Code civil</b> + art. <b>1123 CPC</b>).
 *
 * <p>Les époux acceptent le principe de la rupture <b>sans considération des
 * faits</b>. L'acceptation est formalisée par un <b>procès-verbal d'acceptation
 * signé par les deux époux</b> et devient <b>irrévocable</b> une fois signée
 * (art. 233 al. 2 Cciv).
 *
 * <p>Le juge prononce le divorce et statue sur les conséquences :
 * <ul>
 *     <li>Pas de recherche de tort</li>
 *     <li>Pas de dommages-intérêts art. 266 Cciv (réservés au divorce pour
 *         faute — voir F-FA-09)</li>
 *     <li>Prestation compensatoire art. 270 Cciv <b>possible</b>
 *         indépendamment des torts</li>
 * </ul>
 *
 * <p>Scoring binaire simple (l'outil n'arbitre pas la validité juridique des
 * conséquences, il évalue uniquement le franchissement du seuil procédural) :
 * <ul>
 *     <li>Acceptation valide (PV signé + date renseignée) = 100
 *         → verdict <b>ELEVEE</b></li>
 *     <li>Sinon 0 → verdict <b>FAIBLE</b></li>
 * </ul>
 *
 * <p>Durée procédurale prévisionnelle : ~8-12 mois (moyenne <b>10 mois</b>
 * de l'acceptation à la décision).
 *
 * <p>La prestation compensatoire indicative suit la même formule que F-FA-08/09
 * (différentiel revenus × coefficient FR 0,30 × 12 mois × durée de référence 8 ans,
 * fourchette ±15 %), art. 271 Cciv.
 */
public final class DivorceAccepteCalculator {

    /** Durée procédurale moyenne (mois) — moyenne 8-12 mois. */
    public static final int DELAI_PROCEDURE_MOIS_PREVISIONNEL = 10;

    /** Score plein si acceptation valide. */
    public static final int SCORE_ACCEPTATION_VALIDE = 100;

    /** Score si acceptation invalide. */
    public static final int SCORE_ACCEPTATION_INVALIDE = 0;

    /** Coefficient prestation compensatoire France (art. 271 Cciv — usage JAF). */
    private static final BigDecimal COEFF_PRESTATION_FR = new BigDecimal("0.30");

    /** Durée de référence capitalisation (années). */
    private static final int DUREE_REFERENCE_ANS = 8;

    /** Fourchette haute ±15 %. */
    private static final BigDecimal FOURCHETTE_HAUTE = new BigDecimal("1.15");

    /** Fourchette basse ±15 %. */
    private static final BigDecimal FOURCHETTE_BASSE = new BigDecimal("0.85");

    private static final BigDecimal TWELVE = new BigDecimal("12");

    private static final String BASE_JURIDIQUE =
            "Art. 233-234 Code civil + art. 1123 CPC (prestation compensatoire : art. 270-271 Cciv)";

    private DivorceAccepteCalculator() {
    }

    public static DivorceAccepteResult compute(boolean acceptationPrincipeSignee,
                                               LocalDate dateAcceptationPV,
                                               int dureeMariageAnnees,
                                               BigDecimal revenusAnnuelsEpoux1Eur,
                                               BigDecimal revenusAnnuelsEpoux2Eur,
                                               boolean patrimoineCommun,
                                               LocalDate dateAssignation) {
        return compute(acceptationPrincipeSignee, dateAcceptationPV, dureeMariageAnnees,
                revenusAnnuelsEpoux1Eur, revenusAnnuelsEpoux2Eur, patrimoineCommun,
                dateAssignation, Clock.system(ZoneId.of("Europe/Paris")));
    }

    public static DivorceAccepteResult compute(boolean acceptationPrincipeSignee,
                                               LocalDate dateAcceptationPV,
                                               int dureeMariageAnnees,
                                               BigDecimal revenusAnnuelsEpoux1Eur,
                                               BigDecimal revenusAnnuelsEpoux2Eur,
                                               boolean patrimoineCommun,
                                               LocalDate dateAssignation,
                                               Clock clock) {
        validateInputs(dateAcceptationPV, dureeMariageAnnees,
                revenusAnnuelsEpoux1Eur, revenusAnnuelsEpoux2Eur,
                dateAssignation, clock);

        BigDecimal r1 = revenusAnnuelsEpoux1Eur != null ? revenusAnnuelsEpoux1Eur : BigDecimal.ZERO;
        BigDecimal r2 = revenusAnnuelsEpoux2Eur != null ? revenusAnnuelsEpoux2Eur : BigDecimal.ZERO;

        boolean acceptationValide = acceptationPrincipeSignee && dateAcceptationPV != null;
        boolean eligibilite = acceptationValide;
        // Pas de contestation d'ordre public sur l'acceptation du principe.
        boolean ordrePublic = true;

        int scoreGlobal = acceptationValide ? SCORE_ACCEPTATION_VALIDE : SCORE_ACCEPTATION_INVALIDE;
        String verdict = acceptationValide ? "ELEVEE" : "FAIBLE";

        BigDecimal[] fourchette = computePrestationCompensatoire(r1, r2);
        BigDecimal prestationMin = fourchette[0];
        BigDecimal prestationMax = fourchette[1];

        List<String> criteresNonRemplis = buildCriteresNonRemplis(acceptationPrincipeSignee, dateAcceptationPV);
        List<String> messages = buildMessages(acceptationValide, patrimoineCommun);
        String formule = buildFormule(acceptationValide, scoreGlobal, dureeMariageAnnees,
                r1, r2, prestationMin, prestationMax);

        return new DivorceAccepteResult(
                acceptationPrincipeSignee,
                dateAcceptationPV,
                dureeMariageAnnees,
                r1,
                r2,
                patrimoineCommun,
                dateAssignation,
                acceptationValide,
                ordrePublic,
                eligibilite,
                scoreGlobal,
                verdict,
                DELAI_PROCEDURE_MOIS_PREVISIONNEL,
                prestationMin,
                prestationMax,
                criteresNonRemplis,
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    private static BigDecimal[] computePrestationCompensatoire(BigDecimal r1, BigDecimal r2) {
        BigDecimal ecart = r1.subtract(r2).abs();
        BigDecimal base = ecart.multiply(COEFF_PRESTATION_FR)
                .multiply(TWELVE)
                .multiply(BigDecimal.valueOf(DUREE_REFERENCE_ANS));
        BigDecimal min = base.multiply(FOURCHETTE_BASSE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal max = base.multiply(FOURCHETTE_HAUTE).setScale(2, RoundingMode.HALF_UP);
        return new BigDecimal[]{min, max};
    }

    private static void validateInputs(LocalDate dateAcceptationPV,
                                       int dureeMariageAnnees,
                                       BigDecimal revenusAnnuelsEpoux1Eur,
                                       BigDecimal revenusAnnuelsEpoux2Eur,
                                       LocalDate dateAssignation,
                                       Clock clock) {
        LocalDate today = LocalDate.now(clock);
        if (dureeMariageAnnees < 0) {
            throw new IllegalArgumentException("dureeMariageAnnees doit être positif ou nul");
        }
        if (revenusAnnuelsEpoux1Eur != null && revenusAnnuelsEpoux1Eur.signum() < 0) {
            throw new IllegalArgumentException("revenusAnnuelsEpoux1Eur doit être positif ou nul");
        }
        if (revenusAnnuelsEpoux2Eur != null && revenusAnnuelsEpoux2Eur.signum() < 0) {
            throw new IllegalArgumentException("revenusAnnuelsEpoux2Eur doit être positif ou nul");
        }
        if (dateAcceptationPV != null && dateAcceptationPV.isAfter(today)) {
            throw new IllegalArgumentException("dateAcceptationPV ne peut pas être dans le futur");
        }
        if (dateAssignation != null && dateAssignation.isAfter(today)) {
            throw new IllegalArgumentException("dateAssignation ne peut pas être dans le futur");
        }
        if (dateAcceptationPV != null && dateAssignation != null
                && dateAssignation.isBefore(dateAcceptationPV)) {
            throw new IllegalArgumentException(
                    "dateAssignation ne peut pas être antérieure à dateAcceptationPV");
        }
    }

    private static List<String> buildCriteresNonRemplis(boolean acceptationPrincipeSignee,
                                                        LocalDate dateAcceptationPV) {
        List<String> criteres = new ArrayList<>();
        if (!acceptationPrincipeSignee) {
            criteres.add("Procès-verbal d'acceptation du principe non signé par les deux époux (art. 1123 CPC)");
        }
        if (dateAcceptationPV == null) {
            criteres.add("Date d'acceptation du PV non renseignée");
        }
        return criteres;
    }

    private static List<String> buildMessages(boolean acceptationValide, boolean patrimoineCommun) {
        List<String> messages = new ArrayList<>();
        messages.add("PV d'acceptation irrévocable une fois signé — pas de retour possible (art. 233 al. 2 Cciv).");
        messages.add("Pas de DI art. 266 (réservé divorce pour faute — voir F-FA-09).");
        messages.add("Prestation compensatoire art. 270 possible indépendamment.");
        messages.add("Formaliser convention avec avocat pour encadrer les conséquences "
                + "(contribution aux charges, prestation compensatoire, sort du logement).");
        if (!acceptationValide) {
            messages.add("Acceptation invalide : tant que le PV n'est pas signé par les deux époux, "
                    + "aucune procédure de divorce accepté ne peut être engagée.");
        }
        if (patrimoineCommun) {
            messages.add("Régime de communauté ou participation aux acquêts : prévoir "
                    + "la liquidation du régime matrimonial en parallèle (voir F-FA-04).");
        }
        return messages;
    }

    private static String buildFormule(boolean acceptationValide,
                                       int scoreGlobal,
                                       int dureeMariageAnnees,
                                       BigDecimal r1,
                                       BigDecimal r2,
                                       BigDecimal prestationMin,
                                       BigDecimal prestationMax) {
        StringBuilder sb = new StringBuilder();
        String verdict = acceptationValide ? "ELEVEE" : "FAIBLE";
        sb.append("Divorce accepté (art. 233 Cciv) : éligibilité ").append(verdict)
                .append(" (score ").append(scoreGlobal).append("/100) — acceptation ")
                .append(acceptationValide ? "valide" : "invalide").append(".");
        sb.append(" Durée procédurale prévisionnelle : ")
                .append(DELAI_PROCEDURE_MOIS_PREVISIONNEL).append(" mois (moyenne 8-12).");
        sb.append(" Prestation compensatoire indicative (art. 271) : différentiel revenus ")
                .append(r1.subtract(r2).abs().setScale(2, RoundingMode.HALF_UP))
                .append(" € × 0,30 × 12 × ").append(DUREE_REFERENCE_ANS)
                .append(" ans → fourchette ").append(prestationMin).append(" € - ")
                .append(prestationMax).append(" € (durée mariage ")
                .append(dureeMariageAnnees).append(" ans).");
        return sb.toString();
    }
}
