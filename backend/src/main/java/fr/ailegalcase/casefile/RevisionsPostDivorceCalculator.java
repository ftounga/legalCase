package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * SF-FA-13-01 : calculateur des révisions post-divorce FR.
 *
 * <p>Couvre 5 types de révision :
 * <ul>
 *   <li>{@code PENSION_ALIMENTAIRE} — art. 209 Cciv : modification substantielle
 *       des ressources (≥ 20 % d'écart) ou des besoins ;</li>
 *   <li>{@code RESIDENCE} — art. 373-2-13 Cciv : « éléments nouveaux » justifiant
 *       la modification des modalités d'exercice de l'autorité parentale ;</li>
 *   <li>{@code DROIT_VISITE} — art. 373-2-9 Cciv : intérêt supérieur de l'enfant +
 *       voix de l'enfant (art. 388-1) — pertinence accrue préadolescents ≥ 13 ans ;</li>
 *   <li>{@code PRESTATION_COMPENSATOIRE} — art. 279 / 276-3 / 280-1 Cciv : révision
 *       très restrictive, conditions strictes de modification des ressources ;</li>
 *   <li>{@code DEMENAGEMENT_PARENT} — art. 373-2 al. 3 Cciv : information préalable
 *       à l'autre parent + appréciation de l'impact sur le lien parental.</li>
 * </ul>
 *
 * <h2>Scoring 0-100</h2>
 * <ul>
 *   <li>+50 si {@code modificationSubstantielle}</li>
 *   <li>+25 si {@code motivationSuffisante} (changement décrit ≥ 30 caractères)</li>
 *   <li>+15 si {@code ancienneteDecisionMois ≥ 6}</li>
 *   <li>+10 selon critère spécifique du type :
 *     <ul>
 *       <li>PENSION_ALIMENTAIRE : enfants encore à charge (≥ 1)</li>
 *       <li>RESIDENCE : âge enfants compatible (≥ 6 ans pour la majorité)</li>
 *       <li>DROIT_VISITE : enfant préadolescent (≥ 13 ans)</li>
 *       <li>PRESTATION_COMPENSATOIRE : ancienneté décision ≥ 12 mois</li>
 *       <li>DEMENAGEMENT_PARENT : information préalable de l'autre parent (true)</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Verdicts</h2>
 * <ul>
 *   <li>{@code ELEVEE} si score ≥ 75</li>
 *   <li>{@code MOYENNE} si score 50-74</li>
 *   <li>{@code FAIBLE} si score &lt; 50</li>
 * </ul>
 *
 * <p>Outil <b>single-country FR</b>.</p>
 */
public final class RevisionsPostDivorceCalculator {

    public static final int SEUIL_ELEVEE = 75;
    public static final int SEUIL_MOYENNE = 50;

    /** Seuil d'écart de revenus (en %) à partir duquel la modification est dite substantielle. */
    public static final int SEUIL_MODIFICATION_SUBSTANTIELLE_PCT = 20;

    /** Longueur minimale en caractères pour considérer la motivation comme suffisante. */
    public static final int LONGUEUR_MOTIVATION_MIN = 30;

    /** Ancienneté (en mois) à partir de laquelle la révision n'est plus prématurée. */
    public static final int ANCIENNETE_MIN_MOIS = 6;

    /** Âge minimal d'enfant pour validité du critère résidence. */
    public static final int AGE_RESIDENCE_MIN = 6;

    /** Âge minimal d'enfant (« voix de l'enfant ») pour le critère droit de visite. */
    public static final int AGE_DROIT_VISITE_MIN = 13;

    /** Ancienneté minimale (en mois) requise pour la révision de la prestation compensatoire. */
    public static final int ANCIENNETE_PRESTATION_MIN_MOIS = 12;

    public static final int POIDS_MODIFICATION = 50;
    public static final int POIDS_MOTIVATION = 25;
    public static final int POIDS_ANCIENNETE = 15;
    public static final int POIDS_CRITERE_SPECIFIQUE = 10;

    private RevisionsPostDivorceCalculator() {
    }

    public static RevisionsPostDivorceResult compute(
            String typeRevisionStr,
            LocalDate dateDecisionInitiale,
            String changementCirconstance,
            BigDecimal revenusInitialsDebiteurEur,
            BigDecimal revenusActuelsDebiteurEur,
            BigDecimal revenusInitialsCreancierEur,
            BigDecimal revenusActuelsCreancierEur,
            Integer nbEnfantsACharge,
            List<Integer> ageEnfants,
            String modeResidenceActuelStr,
            String modeResidenceDemandeStr,
            Boolean informationPrealable,
            Integer distanceDemenagementKm,
            LocalDate today) {

        RevisionsPostDivorceTypeRevision typeRevision = validateTypeRevision(typeRevisionStr);
        validateModeResidence(modeResidenceActuelStr);
        validateModeResidence(modeResidenceDemandeStr);
        validateNumericInputs(
                dateDecisionInitiale,
                revenusInitialsDebiteurEur,
                revenusActuelsDebiteurEur,
                revenusInitialsCreancierEur,
                revenusActuelsCreancierEur,
                nbEnfantsACharge,
                ageEnfants,
                distanceDemenagementKm,
                today);

        List<Integer> safeAges = ageEnfants == null ? Collections.emptyList()
                : ageEnfants.stream().filter(a -> a != null && a >= 0).toList();

        int ancienneteDecisionMois = computeAncienneteMois(dateDecisionInitiale, today);

        Integer ecartRevenusPct = computeEcartRevenusPct(
                typeRevision,
                revenusInitialsDebiteurEur, revenusActuelsDebiteurEur,
                revenusInitialsCreancierEur, revenusActuelsCreancierEur);

        boolean modificationSubstantielle = computeModificationSubstantielle(
                typeRevision, ecartRevenusPct, modeResidenceActuelStr, modeResidenceDemandeStr,
                safeAges, distanceDemenagementKm, informationPrealable);

        boolean motivationSuffisante = computeMotivationSuffisante(changementCirconstance);

        int scoreGlobal = computeScore(
                typeRevision, modificationSubstantielle, motivationSuffisante,
                ancienneteDecisionMois, nbEnfantsACharge, safeAges,
                informationPrealable);

        String verdict = verdictRevisionPossible(scoreGlobal);
        String baseJuridique = baseJuridique(typeRevision);
        String formule = buildFormule(
                typeRevision, ecartRevenusPct, modificationSubstantielle,
                motivationSuffisante, ancienneteDecisionMois, scoreGlobal, verdict,
                modeResidenceActuelStr, modeResidenceDemandeStr,
                distanceDemenagementKm, informationPrealable);

        List<String> messages = buildMessages(
                typeRevision, modificationSubstantielle, motivationSuffisante,
                ancienneteDecisionMois, ecartRevenusPct, verdict);

        return new RevisionsPostDivorceResult(
                typeRevision.name(),
                dateDecisionInitiale,
                changementCirconstance,
                revenusInitialsDebiteurEur,
                revenusActuelsDebiteurEur,
                revenusInitialsCreancierEur,
                revenusActuelsCreancierEur,
                nbEnfantsACharge,
                safeAges,
                modeResidenceActuelStr,
                modeResidenceDemandeStr,
                informationPrealable,
                distanceDemenagementKm,
                ecartRevenusPct,
                ancienneteDecisionMois,
                modificationSubstantielle,
                motivationSuffisante,
                scoreGlobal,
                verdict,
                baseJuridique,
                formule,
                messages
        );
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    private static RevisionsPostDivorceTypeRevision validateTypeRevision(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("typeRevision est requis");
        }
        try {
            return RevisionsPostDivorceTypeRevision.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("typeRevision inconnu : " + raw
                    + ". Valeurs : " + Arrays.toString(RevisionsPostDivorceTypeRevision.values()));
        }
    }

    private static void validateModeResidence(String raw) {
        if (raw == null || raw.isBlank()) return;
        try {
            RevisionsPostDivorceModeResidence.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("modeResidence inconnu : " + raw
                    + ". Valeurs : " + Arrays.toString(RevisionsPostDivorceModeResidence.values()));
        }
    }

    private static void validateNumericInputs(LocalDate dateDecisionInitiale,
                                              BigDecimal r1, BigDecimal r2,
                                              BigDecimal r3, BigDecimal r4,
                                              Integer nbEnfantsACharge,
                                              List<Integer> ageEnfants,
                                              Integer distanceDemenagementKm,
                                              LocalDate today) {
        LocalDate ref = today != null ? today : LocalDate.now();
        if (dateDecisionInitiale != null && dateDecisionInitiale.isAfter(ref)) {
            throw new IllegalArgumentException("dateDecisionInitiale ne peut être dans le futur");
        }
        checkNonNeg(r1, "revenusInitialsDebiteurEur");
        checkNonNeg(r2, "revenusActuelsDebiteurEur");
        checkNonNeg(r3, "revenusInitialsCreancierEur");
        checkNonNeg(r4, "revenusActuelsCreancierEur");
        if (nbEnfantsACharge != null && nbEnfantsACharge < 0) {
            throw new IllegalArgumentException("nbEnfantsACharge ne peut être négatif");
        }
        if (ageEnfants != null) {
            for (Integer a : ageEnfants) {
                if (a != null && a < 0) {
                    throw new IllegalArgumentException("ageEnfants ne peut contenir un âge négatif");
                }
            }
        }
        if (distanceDemenagementKm != null && distanceDemenagementKm < 0) {
            throw new IllegalArgumentException("distanceDemenagementKm ne peut être négatif");
        }
    }

    private static void checkNonNeg(BigDecimal v, String name) {
        if (v != null && v.signum() < 0) {
            throw new IllegalArgumentException(name + " ne peut être négatif");
        }
    }

    // -------------------------------------------------------------------------
    // Calcul
    // -------------------------------------------------------------------------

    private static int computeAncienneteMois(LocalDate dateDecisionInitiale, LocalDate today) {
        if (dateDecisionInitiale == null) return 0;
        LocalDate ref = today != null ? today : LocalDate.now();
        long months = ChronoUnit.MONTHS.between(dateDecisionInitiale, ref);
        return (int) Math.max(0, months);
    }

    /**
     * Calcule l'écart de revenus en pourcentage (signé, arrondi).
     *
     * <p>Pour PENSION_ALIMENTAIRE : on regarde l'écart côté débiteur en priorité,
     * puis côté créancier si débiteur indisponible. Pour PRESTATION_COMPENSATOIRE,
     * idem (côté débiteur de la prestation). Pour les autres types : non pertinent
     * → null.</p>
     */
    private static Integer computeEcartRevenusPct(
            RevisionsPostDivorceTypeRevision type,
            BigDecimal revenusInitialsDebiteurEur,
            BigDecimal revenusActuelsDebiteurEur,
            BigDecimal revenusInitialsCreancierEur,
            BigDecimal revenusActuelsCreancierEur) {

        if (type != RevisionsPostDivorceTypeRevision.PENSION_ALIMENTAIRE
                && type != RevisionsPostDivorceTypeRevision.PRESTATION_COMPENSATOIRE) {
            return null;
        }
        Integer ecartDebiteur = pctEcart(revenusInitialsDebiteurEur, revenusActuelsDebiteurEur);
        if (ecartDebiteur != null) return ecartDebiteur;
        Integer ecartCreancier = pctEcart(revenusInitialsCreancierEur, revenusActuelsCreancierEur);
        return ecartCreancier;
    }

    private static Integer pctEcart(BigDecimal initiaux, BigDecimal actuels) {
        if (initiaux == null || actuels == null) return null;
        if (initiaux.signum() <= 0) return null;
        BigDecimal delta = actuels.subtract(initiaux);
        BigDecimal pct = delta.divide(initiaux, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        return pct.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private static boolean computeModificationSubstantielle(
            RevisionsPostDivorceTypeRevision type,
            Integer ecartRevenusPct,
            String modeResidenceActuelStr,
            String modeResidenceDemandeStr,
            List<Integer> ageEnfants,
            Integer distanceDemenagementKm,
            Boolean informationPrealable) {

        switch (type) {
            case PENSION_ALIMENTAIRE, PRESTATION_COMPENSATOIRE -> {
                return ecartRevenusPct != null
                        && Math.abs(ecartRevenusPct) >= SEUIL_MODIFICATION_SUBSTANTIELLE_PCT;
            }
            case RESIDENCE -> {
                // Substantielle si demande de changement de mode + au moins un enfant pour qui c'est pertinent
                boolean modesDifferents = modeResidenceActuelStr != null
                        && modeResidenceDemandeStr != null
                        && !modeResidenceActuelStr.equalsIgnoreCase(modeResidenceDemandeStr);
                return modesDifferents;
            }
            case DROIT_VISITE -> {
                // Substantielle si l'un des enfants atteint l'âge où sa voix est entendue
                if (ageEnfants != null) {
                    for (Integer a : ageEnfants) {
                        if (a != null && a >= AGE_DROIT_VISITE_MIN) return true;
                    }
                }
                // ... ou si un changement de mode de résidence est demandé en parallèle
                return modeResidenceActuelStr != null
                        && modeResidenceDemandeStr != null
                        && !modeResidenceActuelStr.equalsIgnoreCase(modeResidenceDemandeStr);
            }
            case DEMENAGEMENT_PARENT -> {
                // Substantielle si distance significative (≥ 50 km) OU défaut d'information préalable
                boolean distanceSignificative = distanceDemenagementKm != null
                        && distanceDemenagementKm >= 50;
                boolean defautInformation = Boolean.FALSE.equals(informationPrealable);
                return distanceSignificative || defautInformation;
            }
        }
        return false;
    }

    private static boolean computeMotivationSuffisante(String changementCirconstance) {
        if (changementCirconstance == null) return false;
        String trimmed = changementCirconstance.trim();
        return trimmed.length() >= LONGUEUR_MOTIVATION_MIN;
    }

    private static int computeScore(
            RevisionsPostDivorceTypeRevision type,
            boolean modificationSubstantielle,
            boolean motivationSuffisante,
            int ancienneteDecisionMois,
            Integer nbEnfantsACharge,
            List<Integer> ageEnfants,
            Boolean informationPrealable) {

        int score = 0;
        if (modificationSubstantielle) score += POIDS_MODIFICATION;
        if (motivationSuffisante) score += POIDS_MOTIVATION;
        if (ancienneteDecisionMois >= ANCIENNETE_MIN_MOIS) score += POIDS_ANCIENNETE;

        boolean critereSpecifiqueOk = false;
        switch (type) {
            case PENSION_ALIMENTAIRE -> critereSpecifiqueOk =
                    nbEnfantsACharge != null && nbEnfantsACharge >= 1;
            case RESIDENCE -> {
                if (ageEnfants != null) {
                    for (Integer a : ageEnfants) {
                        if (a != null && a >= AGE_RESIDENCE_MIN) {
                            critereSpecifiqueOk = true;
                            break;
                        }
                    }
                }
            }
            case DROIT_VISITE -> {
                if (ageEnfants != null) {
                    for (Integer a : ageEnfants) {
                        if (a != null && a >= AGE_DROIT_VISITE_MIN) {
                            critereSpecifiqueOk = true;
                            break;
                        }
                    }
                }
            }
            case PRESTATION_COMPENSATOIRE -> critereSpecifiqueOk =
                    ancienneteDecisionMois >= ANCIENNETE_PRESTATION_MIN_MOIS;
            case DEMENAGEMENT_PARENT -> critereSpecifiqueOk =
                    Boolean.TRUE.equals(informationPrealable);
        }
        if (critereSpecifiqueOk) score += POIDS_CRITERE_SPECIFIQUE;

        return Math.min(score, 100);
    }

    private static String verdictRevisionPossible(int score) {
        if (score >= SEUIL_ELEVEE) return "ELEVEE";
        if (score >= SEUIL_MOYENNE) return "MOYENNE";
        return "FAIBLE";
    }

    private static String baseJuridique(RevisionsPostDivorceTypeRevision type) {
        return switch (type) {
            case PENSION_ALIMENTAIRE ->
                    "Art. 209 Cciv (pension alimentaire) + jurisprudence Cass. 1ère civ.";
            case RESIDENCE ->
                    "Art. 373-2-13 Cciv (modification résidence enfants — éléments nouveaux)";
            case DROIT_VISITE ->
                    "Art. 373-2-9 Cciv + art. 388-1 Cciv (audition de l'enfant)";
            case PRESTATION_COMPENSATOIRE ->
                    "Art. 279 + 276-3 + 280-1 Cciv (révision prestation compensatoire)";
            case DEMENAGEMENT_PARENT ->
                    "Art. 373-2 al. 3 Cciv (information préalable + autorité parentale)";
        };
    }

    private static String buildFormule(
            RevisionsPostDivorceTypeRevision type,
            Integer ecartRevenusPct,
            boolean modificationSubstantielle,
            boolean motivationSuffisante,
            int ancienneteDecisionMois,
            int scoreGlobal,
            String verdict,
            String modeResidenceActuel,
            String modeResidenceDemande,
            Integer distanceDemenagementKm,
            Boolean informationPrealable) {

        StringBuilder sb = new StringBuilder();
        sb.append(type.name()).append(" : ");
        switch (type) {
            case PENSION_ALIMENTAIRE, PRESTATION_COMPENSATOIRE -> {
                if (ecartRevenusPct != null) {
                    String sens = ecartRevenusPct < 0 ? "Baisse" : (ecartRevenusPct > 0 ? "Hausse" : "Stagnation");
                    int abs = Math.abs(ecartRevenusPct);
                    sb.append(sens).append(' ').append(abs).append("% revenus = modification ");
                    sb.append(modificationSubstantielle ? "substantielle (≥ 20%)" : "non substantielle (< 20%)");
                    sb.append(". ");
                } else {
                    sb.append("Revenus initiaux/actuels non renseignés. ");
                }
            }
            case RESIDENCE -> {
                if (modeResidenceActuel != null && modeResidenceDemande != null) {
                    sb.append("Mode actuel ").append(modeResidenceActuel)
                            .append(" → demandé ").append(modeResidenceDemande).append(". ");
                } else {
                    sb.append("Mode de résidence non renseigné. ");
                }
            }
            case DROIT_VISITE -> sb.append("Critère âge enfant + intérêt supérieur. ");
            case DEMENAGEMENT_PARENT -> {
                if (distanceDemenagementKm != null) {
                    sb.append("Distance ").append(distanceDemenagementKm).append(" km. ");
                }
                if (informationPrealable != null) {
                    sb.append("Information préalable ")
                            .append(Boolean.TRUE.equals(informationPrealable) ? "OUI" : "NON")
                            .append(". ");
                }
            }
        }
        sb.append(String.format(Locale.ROOT,
                "Score %d/100 (%s) — modification %s, motivation %s, ancienneté %d mois.",
                scoreGlobal, verdict,
                modificationSubstantielle ? "OUI" : "NON",
                motivationSuffisante ? "OUI" : "NON",
                ancienneteDecisionMois));
        return sb.toString();
    }

    private static List<String> buildMessages(
            RevisionsPostDivorceTypeRevision type,
            boolean modificationSubstantielle,
            boolean motivationSuffisante,
            int ancienneteDecisionMois,
            Integer ecartRevenusPct,
            String verdict) {
        List<String> messages = new ArrayList<>();
        switch (type) {
            case PENSION_ALIMENTAIRE -> {
                messages.add("Art. 209 Cciv : la révision suppose une modification substantielle des ressources ou des besoins. La jurisprudence retient usuellement un seuil indicatif de 20 % d'écart.");
                messages.add("Charge de la preuve : pièces démontrant la nouvelle situation économique (avis d'imposition, fiches de paie, attestation Pôle emploi, justificatifs de charges).");
            }
            case RESIDENCE -> {
                messages.add("Art. 373-2-13 Cciv : modification possible « en cas d'éléments nouveaux » — l'intérêt supérieur de l'enfant prime (art. 371-1 Cciv).");
                messages.add("Le JAF apprécie souverainement l'opportunité du changement. Préparer un projet pédagogique étayé (école, hébergement, organisation pratique).");
            }
            case DROIT_VISITE -> {
                messages.add("Art. 373-2-9 Cciv : révision possible si l'intérêt de l'enfant l'exige.");
                messages.add("Audition possible de l'enfant ≥ 13 ans (art. 388-1 Cciv) — sa voix est prise en compte sans le lier juridiquement.");
            }
            case PRESTATION_COMPENSATOIRE -> {
                messages.add("Art. 279 Cciv : la prestation compensatoire est en principe forfaitaire et irrévocable.");
                messages.add("Révision exceptionnelle uniquement (art. 276-3 Cciv pour la rente, 280-1 pour le capital échelonné) — conditions strictes : changement important dans les ressources ou les besoins.");
            }
            case DEMENAGEMENT_PARENT -> {
                messages.add("Art. 373-2 al. 3 Cciv : tout changement de résidence du parent doit être communiqué préalablement à l'autre.");
                messages.add("À défaut d'accord : saisine du JAF pour fixer ou modifier les modalités de la résidence et du droit de visite. Le déménagement long-distance peut justifier le retrait/aménagement de la résidence alternée.");
            }
        }

        if (!modificationSubstantielle) {
            messages.add("⚠️ Modification non substantielle : la demande risque d'être rejetée ou de produire un effet limité.");
        }
        if (!motivationSuffisante) {
            messages.add("⚠️ Motivation insuffisante : étoffer la description du changement de circonstances (faits précis, dates, pièces).");
        }
        if (ancienneteDecisionMois < ANCIENNETE_MIN_MOIS) {
            messages.add("⚠️ Décision initiale très récente : le juge peut considérer la demande prématurée sans circonstance exceptionnelle.");
        }
        if ("ELEVEE".equals(verdict)) {
            messages.add("Probabilité élevée que la révision soit recevable. Préparer la requête en évitant tout abus du droit d'agir.");
        }
        if (ecartRevenusPct != null
                && (type == RevisionsPostDivorceTypeRevision.PENSION_ALIMENTAIRE
                || type == RevisionsPostDivorceTypeRevision.PRESTATION_COMPENSATOIRE)) {
            messages.add(String.format(Locale.ROOT,
                    "Écart de revenus calculé : %d %%.", ecartRevenusPct));
        }
        return messages;
    }
}
