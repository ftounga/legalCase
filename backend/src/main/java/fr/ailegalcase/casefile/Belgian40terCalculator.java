package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-IM-14-04 : calculateur pour le regroupement familial d'un Belge
 * (art. <b>40ter</b> de la Loi du 15/12/1980 sur l'accès au territoire, le
 * séjour, l'établissement et l'éloignement des étrangers).
 *
 * <p>Conditions de fond plus strictes que 40bis (regroupement UE non-Belge)
 * car le regroupant est ressortissant Belge :
 * <ul>
 *   <li>Lien familial éligible (conjoint / partenaire légalement enregistré,
 *       descendant mineur ou majeur à charge, ascendant à charge handicapé)</li>
 *   <li>Regroupant = ressortissant Belge</li>
 *   <li><b>Ressources stables et suffisantes</b> ≥ 120 % du RIS
 *       (Revenu d'Intégration Sociale) — ~1740 € net/mois en 2025 (indexé)</li>
 *   <li>Assurance maladie couvrant tous les risques</li>
 *   <li>Logement suffisant pour la famille</li>
 *   <li>Absence de menace pour l'ordre public</li>
 * </ul>
 *
 * <p>Score pondéré 0-100 : 6 conditions × ~18 points chacune (= 108, plafonné
 * à 100). Bonus +10 si le différentiel de revenus dépasse 20 % du seuil.
 *
 * <p>Verdicts : ELEVEE (≥ 75), MOYENNE (50-74), FAIBLE (&lt; 50).
 *
 * <p>Délai d'instruction : 6 mois à compter du dépôt (art. 42 Loi 15/12/1980).
 *
 * <p>Outil <b>single-country BE</b> — pas d'équivalent FR strict
 * (le regroupement français pour un Français relève du CESEDA art. L.434-7).
 */
public final class Belgian40terCalculator {

    /** Revenu d'intégration sociale belge — valeur indicative 2025 (mensuel net €). */
    public static final BigDecimal SEUIL_120_PCT_RIS_DEFAUT = new BigDecimal("1740");

    /** Pondération par condition (6 × ~18 = 108, plafonné 100). */
    public static final int POINTS_PAR_CONDITION = 18;

    /** Bonus si différentiel revenus > 20 % du seuil (solidité financière). */
    public static final int BONUS_DIFFERENTIEL_SOLIDE = 10;

    /** Seuil du bonus : 20 % du seuil de ressources. */
    public static final BigDecimal SEUIL_DIFFERENTIEL_SOLIDE_PCT = new BigDecimal("0.20");

    /** Délai d'instruction de la demande (mois, art. 42 Loi 15/12/1980). */
    public static final int DELAI_INSTRUCTION_MOIS = 6;

    /** Score minimum pour verdict ELEVEE. */
    public static final int SEUIL_ELEVEE = 75;

    /** Score minimum pour verdict MOYENNE. */
    public static final int SEUIL_MOYENNE = 50;

    private static final String BASE_JURIDIQUE =
            "Loi 15/12/1980 art. 40ter + AR 08/10/1981 + AR 07/10/1981 (seuil 120 % RIS)";

    private Belgian40terCalculator() {
    }

    public static Belgian40terResult compute(String lienFamilial,
                                             boolean regroupantBelge,
                                             BigDecimal revenusMensuelsNetsEur,
                                             BigDecimal seuil120PctRisEur,
                                             boolean assuranceMaladie,
                                             boolean logementSuffisant,
                                             boolean menaceOrdrePublic,
                                             LocalDate dateDepotDemande) {
        return compute(lienFamilial, regroupantBelge, revenusMensuelsNetsEur,
                seuil120PctRisEur, assuranceMaladie, logementSuffisant,
                menaceOrdrePublic, dateDepotDemande,
                Clock.system(ZoneId.of("Europe/Brussels")));
    }

    public static Belgian40terResult compute(String lienFamilial,
                                             boolean regroupantBelge,
                                             BigDecimal revenusMensuelsNetsEur,
                                             BigDecimal seuil120PctRisEur,
                                             boolean assuranceMaladie,
                                             boolean logementSuffisant,
                                             boolean menaceOrdrePublic,
                                             LocalDate dateDepotDemande,
                                             Clock clock) {
        validateInputs(lienFamilial, revenusMensuelsNetsEur, seuil120PctRisEur,
                dateDepotDemande, clock);

        BigDecimal seuilEffectif = seuil120PctRisEur != null
                ? seuil120PctRisEur
                : SEUIL_120_PCT_RIS_DEFAUT;

        boolean lienValide = isLienValide(lienFamilial);
        boolean regroupantBelgeOk = regroupantBelge;
        boolean revenusSuffisantsOk =
                revenusMensuelsNetsEur.compareTo(seuilEffectif) >= 0;
        boolean assuranceOk = assuranceMaladie;
        boolean logementOk = logementSuffisant;
        boolean pasMenace = !menaceOrdrePublic;

        BigDecimal differentielRevenus = revenusMensuelsNetsEur
                .subtract(seuilEffectif)
                .setScale(2, RoundingMode.HALF_UP);

        int scoreGlobal = computeScore(lienValide, regroupantBelgeOk,
                revenusSuffisantsOk, assuranceOk, logementOk, pasMenace,
                differentielRevenus, seuilEffectif);

        String verdict = verdict(scoreGlobal);

        LocalDate dateExpirationInstruction = dateDepotDemande != null
                ? dateDepotDemande.plusMonths(DELAI_INSTRUCTION_MOIS)
                : null;

        List<String> criteresNonRemplis = buildCriteresNonRemplis(
                lienValide, regroupantBelgeOk, revenusSuffisantsOk,
                assuranceOk, logementOk, pasMenace, differentielRevenus);

        List<String> messages = buildMessages(verdict, revenusSuffisantsOk,
                differentielRevenus, seuilEffectif);

        String formule = buildFormule(verdict, scoreGlobal,
                lienValide, regroupantBelgeOk, revenusSuffisantsOk,
                assuranceOk, logementOk, pasMenace,
                differentielRevenus, dateExpirationInstruction);

        return new Belgian40terResult(
                lienFamilial,
                regroupantBelge,
                revenusMensuelsNetsEur,
                seuilEffectif,
                assuranceMaladie,
                logementSuffisant,
                menaceOrdrePublic,
                dateDepotDemande,
                lienValide,
                regroupantBelgeOk,
                revenusSuffisantsOk,
                assuranceOk,
                logementOk,
                pasMenace,
                differentielRevenus,
                scoreGlobal,
                verdict,
                criteresNonRemplis,
                dateExpirationInstruction,
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    private static int computeScore(boolean lienValide,
                                    boolean regroupantBelgeOk,
                                    boolean revenusSuffisantsOk,
                                    boolean assuranceOk,
                                    boolean logementOk,
                                    boolean pasMenace,
                                    BigDecimal differentielRevenus,
                                    BigDecimal seuil) {
        int score = 0;
        if (lienValide) score += POINTS_PAR_CONDITION;
        if (regroupantBelgeOk) score += POINTS_PAR_CONDITION;
        if (revenusSuffisantsOk) score += POINTS_PAR_CONDITION;
        if (assuranceOk) score += POINTS_PAR_CONDITION;
        if (logementOk) score += POINTS_PAR_CONDITION;
        if (pasMenace) score += POINTS_PAR_CONDITION;

        BigDecimal seuilBonus = seuil.multiply(SEUIL_DIFFERENTIEL_SOLIDE_PCT);
        if (revenusSuffisantsOk && differentielRevenus.compareTo(seuilBonus) > 0) {
            score += BONUS_DIFFERENTIEL_SOLIDE;
        }
        return Math.min(score, 100);
    }

    private static String verdict(int score) {
        if (score >= SEUIL_ELEVEE) return "ELEVEE";
        if (score >= SEUIL_MOYENNE) return "MOYENNE";
        return "FAIBLE";
    }

    private static boolean isLienValide(String lienFamilial) {
        if (lienFamilial == null) return false;
        return switch (lienFamilial) {
            case "CONJOINT",
                 "PARTENAIRE_LEGAL_ENREGISTRE",
                 "DESCENDANT_MINEUR",
                 "DESCENDANT_MAJEUR_CHARGE",
                 "ASCENDANT_CHARGE_HANDICAP" -> true;
            default -> false;
        };
    }

    private static void validateInputs(String lienFamilial,
                                       BigDecimal revenusMensuelsNetsEur,
                                       BigDecimal seuil120PctRisEur,
                                       LocalDate dateDepotDemande,
                                       Clock clock) {
        if (lienFamilial == null || lienFamilial.isBlank()) {
            throw new IllegalArgumentException("lienFamilial est requis");
        }
        if (!isLienValide(lienFamilial)) {
            throw new IllegalArgumentException(
                    "lienFamilial invalide : " + lienFamilial
                            + " (attendus : CONJOINT, PARTENAIRE_LEGAL_ENREGISTRE, "
                            + "DESCENDANT_MINEUR, DESCENDANT_MAJEUR_CHARGE, "
                            + "ASCENDANT_CHARGE_HANDICAP)");
        }
        if (revenusMensuelsNetsEur == null) {
            throw new IllegalArgumentException("revenusMensuelsNetsEur est requis");
        }
        if (revenusMensuelsNetsEur.signum() < 0) {
            throw new IllegalArgumentException(
                    "revenusMensuelsNetsEur doit être positif ou nul");
        }
        if (seuil120PctRisEur != null && seuil120PctRisEur.signum() <= 0) {
            throw new IllegalArgumentException(
                    "seuil120PctRisEur doit être strictement positif");
        }
        if (dateDepotDemande != null) {
            LocalDate today = LocalDate.now(clock);
            if (dateDepotDemande.isAfter(today)) {
                throw new IllegalArgumentException(
                        "dateDepotDemande ne peut pas être dans le futur");
            }
        }
    }

    private static List<String> buildCriteresNonRemplis(boolean lienValide,
                                                        boolean regroupantBelgeOk,
                                                        boolean revenusSuffisantsOk,
                                                        boolean assuranceOk,
                                                        boolean logementOk,
                                                        boolean pasMenace,
                                                        BigDecimal differentielRevenus) {
        List<String> criteres = new ArrayList<>();
        if (!lienValide) {
            criteres.add("Lien familial non éligible art. 40ter");
        }
        if (!regroupantBelgeOk) {
            criteres.add("Regroupant non Belge (régime 40ter réservé aux Belges)");
        }
        if (!revenusSuffisantsOk) {
            criteres.add("Revenus stables et suffisants < 120 % RIS (manque "
                    + differentielRevenus.abs() + " €/mois)");
        }
        if (!assuranceOk) {
            criteres.add("Assurance maladie couvrant la famille non justifiée");
        }
        if (!logementOk) {
            criteres.add("Logement suffisant non justifié");
        }
        if (!pasMenace) {
            criteres.add("Menace ordre public présumée");
        }
        return criteres;
    }

    private static List<String> buildMessages(String verdict,
                                              boolean revenusSuffisantsOk,
                                              BigDecimal differentielRevenus,
                                              BigDecimal seuil) {
        List<String> messages = new ArrayList<>();
        messages.add("Carte F (membre de famille d'un Belge) délivrée après "
                + "5 ans de séjour légal et ininterrompu — art. 40ter §2.");
        messages.add("Attention si ressources juste au seuil : la jurisprudence "
                + "Conseil du contentieux des étrangers (CCE) contrôle la "
                + "méthode de calcul de l'Office des Étrangers — contester "
                + "tout refus si calcul OE erroné.");
        messages.add("Recours CCE annulation : 30 jours à compter de la "
                + "notification de la décision (art. 39/57 Loi 15/12/1980).");
        messages.add("Pièces à joindre : acte de mariage/cohabitation légale, "
                + "fiches de paie / preuves de revenus 12 derniers mois, "
                + "attestation mutuelle, bail ou titre de propriété, "
                + "extrait casier judiciaire belge et étranger.");

        if (!revenusSuffisantsOk) {
            BigDecimal manque = differentielRevenus.abs();
            messages.add("Ressources insuffisantes : augmenter les revenus de "
                    + manque + " €/mois ou justifier d'aides sociales non "
                    + "éligibles à l'exclusion (art. 40ter §2 al. 2).");
        }
        BigDecimal seuilBonus = seuil.multiply(SEUIL_DIFFERENTIEL_SOLIDE_PCT);
        if (revenusSuffisantsOk && differentielRevenus.compareTo(seuilBonus) > 0) {
            messages.add("Ressources confortables (> 120 % du seuil 120 % RIS) : "
                    + "dossier financièrement solide.");
        }
        if ("ELEVEE".equals(verdict)) {
            messages.add("Probabilité d'acceptation élevée : préparer l'audit "
                    + "documentaire OE et anticiper la convocation commune.");
        } else if ("FAIBLE".equals(verdict)) {
            messages.add("Probabilité d'acceptation faible : envisager une voie "
                    + "alternative (40bis si regroupant UE non-Belge, "
                    + "humanitaire art. 9bis).");
        }
        return messages;
    }

    private static String buildFormule(String verdict,
                                       int scoreGlobal,
                                       boolean lienValide,
                                       boolean regroupantBelgeOk,
                                       boolean revenusSuffisantsOk,
                                       boolean assuranceOk,
                                       boolean logementOk,
                                       boolean pasMenace,
                                       BigDecimal differentielRevenus,
                                       LocalDate dateExpirationInstruction) {
        StringBuilder sb = new StringBuilder();
        sb.append("40ter familial Belge BE : probabilité ")
                .append(verdict)
                .append(" (score ").append(scoreGlobal).append("/100)");

        List<String> parts = new ArrayList<>();
        parts.add("lien " + (lienValide ? "OK" : "KO"));
        parts.add("regroupant belge " + (regroupantBelgeOk ? "OK" : "KO"));
        parts.add("revenus " + (revenusSuffisantsOk ? "OK" : "KO")
                + " (différentiel " + differentielRevenus + " €)");
        parts.add("assurance " + (assuranceOk ? "OK" : "KO"));
        parts.add("logement " + (logementOk ? "OK" : "KO"));
        parts.add("ordre public " + (pasMenace ? "OK" : "KO"));
        sb.append(" — ").append(String.join(", ", parts)).append(".");

        if (dateExpirationInstruction != null) {
            sb.append(" Délai d'instruction jusqu'au ")
                    .append(dateExpirationInstruction)
                    .append(" (art. 42).");
        }
        return sb.toString();
    }
}
