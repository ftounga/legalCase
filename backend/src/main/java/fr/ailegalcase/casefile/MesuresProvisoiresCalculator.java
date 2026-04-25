package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-FA-12-01 : calculateur pour les <b>mesures provisoires</b> de l'audience
 * d'orientation et sur mesures provisoires (AOMP) du JAF, art. 254-256 Cciv +
 * jurisprudence Cass. 1ère civ. Outil <b>single-country FR</b>.
 *
 * <p>Cinq mesures évaluées :
 * <ul>
 *   <li>pension alimentaire ad interim (≈ moitié du différentiel de revenus)</li>
 *   <li>attribution provisoire du logement (priorité protection si violences,
 *       sinon propriétaire ou plus faibles revenus)</li>
 *   <li>résidence provisoire des enfants (alternée si âge ≥ 6 ans + concordance
 *       du souhait, sinon exclusive)</li>
 *   <li>contribution aux charges du mariage (max revenus / 2)</li>
 *   <li>mesures conservatoires patrimoniales (uniquement si patrimoine
 *       significatif + demande explicite)</li>
 * </ul>
 *
 * <p>Score de cohésion : 100 - écarts entre demande et recommandation.
 * Verdicts : ELEVEE (≥75), MOYENNE (40-74), FAIBLE (&lt;40).
 *
 * <p>Prérequis des outils F-FA-08/09/10 (divorces contentieux FR).
 * BE = F-FA-11 (procédure distincte loi belge).
 */
public final class MesuresProvisoiresCalculator {

    public static final int AGE_MIN_RESIDENCE_ALTERNEE = 6;
    public static final int SEUIL_ELEVEE = 75;
    public static final int SEUIL_MOYENNE = 40;
    private static final String BASE_JURIDIQUE =
            "Art. 254-256 Cciv + jurisprudence Cass. 1ère civ.";

    private MesuresProvisoiresCalculator() {
    }

    public static MesuresProvisoiresResult compute(LocalDate dateAudienceAOMP,
                                                   BigDecimal revenusEpouxDemandeurEur,
                                                   BigDecimal revenusEpouxDefendeurEur,
                                                   String logementCommunDescription,
                                                   String logementProprietaire,
                                                   List<MesuresProvisoiresResult.EnfantMineurInfo> enfantsMineurs,
                                                   String souhaitResidenceEnfants,
                                                   boolean violencesAlleguees,
                                                   boolean patrimoineCommunIsignificatif,
                                                   boolean demandeMesureConservatoire) {
        validateInputs(dateAudienceAOMP, revenusEpouxDemandeurEur,
                revenusEpouxDefendeurEur, logementProprietaire,
                souhaitResidenceEnfants, enfantsMineurs);

        List<MesuresProvisoiresResult.EnfantMineurInfo> enfants = enfantsMineurs != null
                ? List.copyOf(enfantsMineurs) : List.of();

        BigDecimal differentielRevenus = revenusEpouxDemandeurEur
                .subtract(revenusEpouxDefendeurEur)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal pensionAlimentairePropose = differentielRevenus.abs()
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        if (pensionAlimentairePropose.signum() < 0) {
            pensionAlimentairePropose = BigDecimal.ZERO.setScale(2);
        }

        String attributionLogement = computeAttributionLogement(
                violencesAlleguees, logementProprietaire,
                revenusEpouxDemandeurEur, revenusEpouxDefendeurEur);

        String residenceEnfants = computeResidenceEnfants(
                enfants, souhaitResidenceEnfants, violencesAlleguees);

        BigDecimal contributionCharges = revenusEpouxDemandeurEur
                .max(revenusEpouxDefendeurEur)
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        boolean mesureConservatoireRecommande =
                patrimoineCommunIsignificatif && demandeMesureConservatoire;

        int scoreCohesionMesures = computeScore(
                souhaitResidenceEnfants, residenceEnfants,
                logementProprietaire, attributionLogement,
                violencesAlleguees, revenusEpouxDemandeurEur,
                revenusEpouxDefendeurEur);
        String verdictAcceptabilite = verdict(scoreCohesionMesures);

        List<String> messages = buildMessages(
                violencesAlleguees, residenceEnfants, attributionLogement,
                mesureConservatoireRecommande, enfants, souhaitResidenceEnfants);

        String formule = buildFormule(differentielRevenus, pensionAlimentairePropose,
                attributionLogement, residenceEnfants, contributionCharges,
                scoreCohesionMesures, verdictAcceptabilite);

        return new MesuresProvisoiresResult(
                dateAudienceAOMP,
                revenusEpouxDemandeurEur,
                revenusEpouxDefendeurEur,
                logementCommunDescription,
                logementProprietaire,
                enfants,
                souhaitResidenceEnfants,
                violencesAlleguees,
                patrimoineCommunIsignificatif,
                demandeMesureConservatoire,
                differentielRevenus,
                pensionAlimentairePropose,
                attributionLogement,
                residenceEnfants,
                contributionCharges,
                mesureConservatoireRecommande,
                scoreCohesionMesures,
                verdictAcceptabilite,
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    private static String computeAttributionLogement(boolean violencesAlleguees,
                                                     String logementProprietaire,
                                                     BigDecimal revenusDemandeur,
                                                     BigDecimal revenusDefendeur) {
        if (violencesAlleguees) {
            // Logement attribué à l'époux non-violent : par convention le demandeur
            // est l'époux signalant les violences (cas majoritaire des AOMP).
            return "DEMANDEUR";
        }
        switch (logementProprietaire) {
            case "PROPRIETE_DEMANDEUR":
                return "DEMANDEUR";
            case "PROPRIETE_DEFENDEUR":
                return "DEFENDEUR";
            case "EN_INDIVISION":
            case "LOCATION_COMMUNE":
            default:
                int cmp = revenusDemandeur.compareTo(revenusDefendeur);
                if (cmp == 0) {
                    return "INDIVISION_MAINTENUE";
                }
                // Le logement est attribué à l'époux aux revenus les plus faibles.
                return cmp < 0 ? "DEMANDEUR" : "DEFENDEUR";
        }
    }

    private static String computeResidenceEnfants(List<MesuresProvisoiresResult.EnfantMineurInfo> enfants,
                                                  String souhait,
                                                  boolean violencesAlleguees) {
        if (enfants == null || enfants.isEmpty()) {
            return "SANS_OBJET";
        }
        if (violencesAlleguees) {
            return "EXCLUSIVE_DEMANDEUR";
        }
        if ("ALTERNEE".equals(souhait)) {
            boolean tousAge6Plus = enfants.stream()
                    .allMatch(e -> e.age() >= AGE_MIN_RESIDENCE_ALTERNEE);
            if (tousAge6Plus) {
                return "ALTERNEE";
            }
            // Au moins un enfant < 6 ans : par défaut chez le demandeur.
            return "EXCLUSIVE_DEMANDEUR";
        }
        return souhait;
    }

    private static int computeScore(String souhait,
                                    String residenceRecommande,
                                    String logementProprietaire,
                                    String attributionLogement,
                                    boolean violencesAlleguees,
                                    BigDecimal revenusDemandeur,
                                    BigDecimal revenusDefendeur) {
        int score = 100;
        if (!"SANS_OBJET".equals(residenceRecommande)
                && !residenceRecommande.equals(souhait)) {
            score -= 20;
        }
        if (("PROPRIETE_DEMANDEUR".equals(logementProprietaire)
                && !"DEMANDEUR".equals(attributionLogement))
                || ("PROPRIETE_DEFENDEUR".equals(logementProprietaire)
                && !"DEFENDEUR".equals(attributionLogement))) {
            score -= 10;
        }
        if (violencesAlleguees && "DEFENDEUR".equals(attributionLogement)) {
            score -= 30;
        }
        if (revenusDemandeur.compareTo(revenusDefendeur) == 0
                && "INDIVISION_MAINTENUE".equals(attributionLogement)) {
            score -= 15;
        }
        return Math.max(0, Math.min(100, score));
    }

    private static String verdict(int score) {
        if (score >= SEUIL_ELEVEE) return "ELEVEE";
        if (score >= SEUIL_MOYENNE) return "MOYENNE";
        return "FAIBLE";
    }

    private static List<String> buildMessages(boolean violencesAlleguees,
                                              String residenceEnfants,
                                              String attributionLogement,
                                              boolean mesureConservatoireRecommande,
                                              List<MesuresProvisoiresResult.EnfantMineurInfo> enfants,
                                              String souhaitResidenceEnfants) {
        List<String> msg = new ArrayList<>();
        msg.add("Demande à formuler à l'audience d'orientation et sur mesures "
                + "provisoires (AOMP) — art. 254 Cciv. Le JAF statue à cette "
                + "audience sur l'ensemble des mesures provisoires.");
        msg.add("Pension alimentaire ad interim : méthode simplifiée (différentiel "
                + "revenus / 2). Le calcul détaillé doit être affiné avec l'outil "
                + "F-FA-02 (table de référence pension alimentaire).");
        msg.add("Contribution aux charges du mariage (art. 214 Cciv) : "
                + "calculée comme la moitié du revenu le plus élevé. À ajuster "
                + "selon le train de vie du couple et les besoins effectifs.");
        if (violencesAlleguees) {
            msg.add("Violences alléguées : prévoir cumulativement une ordonnance "
                    + "de protection (art. 515-9 Cciv) — voir F-FA-15. "
                    + "Le logement est attribué de plein droit à l'époux victime.");
        }
        if ("ALTERNEE".equals(residenceEnfants)) {
            msg.add("Résidence alternée recommandée : âge des enfants ≥ 6 ans et "
                    + "concordance du souhait. Joindre un calendrier alterné "
                    + "détaillé à la requête.");
        } else if (residenceEnfants.startsWith("EXCLUSIVE_")) {
            msg.add("Résidence exclusive recommandée : préciser le droit de "
                    + "visite et d'hébergement de l'autre parent (week-ends "
                    + "alternés + moitié des vacances scolaires sont la règle).");
        }
        boolean enfantsJeunes = enfants != null && enfants.stream()
                .anyMatch(e -> e.age() < AGE_MIN_RESIDENCE_ALTERNEE);
        if (enfantsJeunes && "ALTERNEE".equals(souhaitResidenceEnfants)
                && !"ALTERNEE".equals(residenceEnfants)) {
            msg.add("Au moins un enfant de moins de 6 ans (< 6 ans) : la résidence "
                    + "alternée demandée est peu probable, le JAF privilégie la "
                    + "stabilité affective — argumentaire renforcé requis.");
        }
        if ("INDIVISION_MAINTENUE".equals(attributionLogement)) {
            msg.add("Revenus équivalents et logement en indivision : le JAF peut "
                    + "renvoyer la question à l'audience suivante ou désigner "
                    + "un occupant à charge d'indemnité d'occupation.");
        }
        if (mesureConservatoireRecommande) {
            msg.add("Mesure conservatoire patrimoniale recommandée (art. 220-1 "
                    + "Cciv) : interdiction de disposer des biens communs, "
                    + "saisie conservatoire si risque avéré de dilapidation.");
        }
        return msg;
    }

    private static String buildFormule(BigDecimal differentielRevenus,
                                       BigDecimal pensionAlimentairePropose,
                                       String attributionLogement,
                                       String residenceEnfants,
                                       BigDecimal contributionCharges,
                                       int scoreCohesionMesures,
                                       String verdictAcceptabilite) {
        StringBuilder sb = new StringBuilder();
        sb.append("Mesures provisoires (art. 254 Cciv) : score cohésion ")
                .append(scoreCohesionMesures).append("/100 — ")
                .append(verdictAcceptabilite).append(".");
        sb.append(" PA = différentiel revenus ")
                .append(differentielRevenus.toPlainString())
                .append(" / 2 = ")
                .append(pensionAlimentairePropose.toPlainString())
                .append(" € ; logement attribué ")
                .append(attributionLogement).append(" ; résidence enfants ")
                .append(residenceEnfants)
                .append(" ; contribution charges ")
                .append(contributionCharges.toPlainString()).append(" €.");
        return sb.toString();
    }

    private static void validateInputs(LocalDate dateAudienceAOMP,
                                       BigDecimal revenusDemandeur,
                                       BigDecimal revenusDefendeur,
                                       String logementProprietaire,
                                       String souhaitResidenceEnfants,
                                       List<MesuresProvisoiresResult.EnfantMineurInfo> enfants) {
        if (dateAudienceAOMP == null) {
            throw new IllegalArgumentException("dateAudienceAOMP est requise");
        }
        if (revenusDemandeur == null) {
            throw new IllegalArgumentException("revenusEpouxDemandeurEur est requis");
        }
        if (revenusDefendeur == null) {
            throw new IllegalArgumentException("revenusEpouxDefendeurEur est requis");
        }
        if (revenusDemandeur.signum() < 0) {
            throw new IllegalArgumentException(
                    "revenusEpouxDemandeurEur doit être positif ou nul (Demandeur)");
        }
        if (revenusDefendeur.signum() < 0) {
            throw new IllegalArgumentException(
                    "revenusEpouxDefendeurEur doit être positif ou nul (Defendeur)");
        }
        if (logementProprietaire == null || logementProprietaire.isBlank()) {
            throw new IllegalArgumentException("logementProprietaire est requis");
        }
        if (!List.of("EN_INDIVISION", "PROPRIETE_DEMANDEUR",
                "PROPRIETE_DEFENDEUR", "LOCATION_COMMUNE").contains(logementProprietaire)) {
            throw new IllegalArgumentException(
                    "logementProprietaire invalide : " + logementProprietaire);
        }
        if (souhaitResidenceEnfants == null || souhaitResidenceEnfants.isBlank()) {
            throw new IllegalArgumentException("souhaitResidenceEnfants est requis");
        }
        if (!List.of("ALTERNEE", "EXCLUSIVE_DEMANDEUR",
                "EXCLUSIVE_DEFENDEUR").contains(souhaitResidenceEnfants)) {
            throw new IllegalArgumentException(
                    "souhaitResidenceEnfants invalide : " + souhaitResidenceEnfants);
        }
        if (enfants != null) {
            for (MesuresProvisoiresResult.EnfantMineurInfo e : enfants) {
                if (e == null) {
                    throw new IllegalArgumentException("enfantsMineurs : entrée nulle");
                }
                if (e.age() < 0 || e.age() >= 18) {
                    throw new IllegalArgumentException(
                            "Âge enfant invalide (mineur attendu, 0-17) : " + e.age());
                }
            }
        }
    }
}
