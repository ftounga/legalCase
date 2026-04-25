package fr.ailegalcase.casefile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SF-FA-20-01 : calculateur dissolution PACS (art. 515-7 et 515-7-1 Cciv).
 *
 * <h2>Modes de dissolution (art. 515-7 Cciv)</h2>
 * <ul>
 *   <li><b>DECLARATION_UNILATERALE</b> : signification par huissier au
 *       partenaire (notification obligatoire) puis remise au greffe</li>
 *   <li><b>DECLARATION_CONJOINTE</b> : déclaration commune au greffe</li>
 *   <li><b>MARIAGE_PARTENAIRES</b> : mariage des deux partenaires entre eux</li>
 *   <li><b>MARIAGE_TIERS</b> : mariage de l'un avec un tiers</li>
 *   <li><b>DECES</b> : décès d'un partenaire</li>
 * </ul>
 *
 * <h2>Créances entre partenaires (art. 515-7-1 + 515-8 Cciv + jurisprudence)</h2>
 * Pas de prestation compensatoire entre partenaires PACS — il s'agit
 * uniquement de créances : contribution déséquilibrée aux charges,
 * investissement dans un bien propre, enrichissement sans cause
 * (art. 1303 Cciv), prestation de travail non rémunérée. Prescription
 * 5 ans (art. 2224 Cciv).
 *
 * <h2>Score créances probables (0-100)</h2>
 * <ul>
 *   <li>Patrimoine commun significatif : +30</li>
 *   <li>≥ 2 créances alléguées : +25</li>
 *   <li>Durée union ≥ 3 ans : +20</li>
 *   <li>Régime ≠ SEPARATION_BIENS : +15</li>
 *   <li>Enfants communs ≥ 1 : +10</li>
 * </ul>
 *
 * <h2>Verdict</h2>
 * <ul>
 *   <li>≥ 70 → CONTENTIEUX_INEVITABLE</li>
 *   <li>40-69 → MEDIATION_OU_JAF</li>
 *   <li>1-39 → LIQUIDATION_AMIABLE</li>
 *   <li>0 → RIEN_A_FAIRE</li>
 * </ul>
 *
 * <p>Outil single-country FRANCE + DROIT_FAMILLE. Le PACS BE
 * (cohabitation légale, loi 23/11/1998) n'est pas couvert ici.
 */
public final class PacsDissolutionCalculator {

    public static final int DELAI_PRESCRIPTION_ANNEES = 5;
    public static final int SEUIL_DUREE_ELIGIBLE_CREANCES_ANS = 1;
    public static final int SEUIL_DUREE_LONGUE_ANS = 3;
    public static final int SEUIL_CREANCES_MULTIPLES = 2;

    public static final int SCORE_PATRIMOINE_COMMUN = 30;
    public static final int SCORE_CREANCES_MULTIPLES = 25;
    public static final int SCORE_DUREE_LONGUE = 20;
    public static final int SCORE_REGIME_NON_SEPARATION = 15;
    public static final int SCORE_ENFANTS = 10;

    public static final int SEUIL_CONTENTIEUX = 70;
    public static final int SEUIL_MEDIATION = 40;

    public static final String VERDICT_CONTENTIEUX = "CONTENTIEUX_INEVITABLE";
    public static final String VERDICT_MEDIATION = "MEDIATION_OU_JAF";
    public static final String VERDICT_LIQUIDATION = "LIQUIDATION_AMIABLE";
    public static final String VERDICT_RIEN = "RIEN_A_FAIRE";

    public static final String BASE_JURIDIQUE = "Art. 515-7+515-7-1+515-8 Cciv";

    public enum ModeDissolution {
        DECLARATION_UNILATERALE,
        DECLARATION_CONJOINTE,
        MARIAGE_PARTENAIRES,
        MARIAGE_TIERS,
        DECES
    }

    public enum RegimeBiens {
        SEPARATION_BIENS,
        INDIVISION_AMENAGEE,
        INDIVISION_PAR_DEFAUT
    }

    public enum CreanceType {
        CONTRIBUTION_DESEQUILIBRE,
        INVESTISSEMENT_BIEN_PROPRE,
        ENRICHISSEMENT_INJUSTE,
        PRESTATION_TRAVAIL_NON_REMUNEREE,
        AUCUNE
    }

    private PacsDissolutionCalculator() {
    }

    public static PacsDissolutionResult compute(LocalDate dateConclusionPacs,
                                                ModeDissolution modeDissolution,
                                                LocalDate dateDissolution,
                                                int dureeUnionAnnees,
                                                RegimeBiens regimeBiens,
                                                boolean patrimoineCommunSignificatif,
                                                List<CreanceType> creancesAlleguees,
                                                int enfantsCommuns,
                                                LocalDate dateNotificationPartenaire) {
        return compute(dateConclusionPacs, modeDissolution, dateDissolution,
                dureeUnionAnnees, regimeBiens, patrimoineCommunSignificatif,
                creancesAlleguees, enfantsCommuns, dateNotificationPartenaire,
                Clock.system(ZoneId.of("Europe/Paris")));
    }

    public static PacsDissolutionResult compute(LocalDate dateConclusionPacs,
                                                ModeDissolution modeDissolution,
                                                LocalDate dateDissolution,
                                                int dureeUnionAnnees,
                                                RegimeBiens regimeBiens,
                                                boolean patrimoineCommunSignificatif,
                                                List<CreanceType> creancesAlleguees,
                                                int enfantsCommuns,
                                                LocalDate dateNotificationPartenaire,
                                                Clock clock) {
        validateInputs(modeDissolution, regimeBiens, dateConclusionPacs,
                dateDissolution, dateNotificationPartenaire,
                dureeUnionAnnees, enfantsCommuns, clock);

        List<CreanceType> creances = creancesAlleguees != null
                ? creancesAlleguees : Collections.emptyList();

        boolean dissolutionValide = computeDissolutionValide(modeDissolution,
                dateDissolution, dateNotificationPartenaire);
        boolean delaiNotificationOk = computeDelaiNotificationOk(modeDissolution,
                dateDissolution, dateNotificationPartenaire);
        boolean dureeUnionEligibleCreances = dureeUnionAnnees >= SEUIL_DUREE_ELIGIBLE_CREANCES_ANS;

        int score = computeScore(patrimoineCommunSignificatif, creances,
                dureeUnionAnnees, regimeBiens, enfantsCommuns);
        String verdict = computeVerdict(score);

        List<CreanceType> creancesVisibles = computeCreancesVisibles(
                creances, dureeUnionEligibleCreances);

        List<String> messages = buildMessages(modeDissolution, dissolutionValide,
                delaiNotificationOk, dureeUnionEligibleCreances, regimeBiens,
                patrimoineCommunSignificatif, enfantsCommuns, verdict);

        String formule = buildFormule(modeDissolution, score, verdict,
                dureeUnionAnnees, regimeBiens, patrimoineCommunSignificatif,
                creances.size(), enfantsCommuns);

        return new PacsDissolutionResult(
                dateConclusionPacs,
                modeDissolution,
                dateDissolution,
                dureeUnionAnnees,
                regimeBiens,
                patrimoineCommunSignificatif,
                List.copyOf(creances),
                enfantsCommuns,
                dateNotificationPartenaire,
                dissolutionValide,
                delaiNotificationOk,
                dureeUnionEligibleCreances,
                score,
                verdict,
                creancesVisibles,
                DELAI_PRESCRIPTION_ANNEES,
                BASE_JURIDIQUE,
                formule,
                messages);
    }

    private static boolean computeDissolutionValide(ModeDissolution mode,
                                                    LocalDate dateDissolution,
                                                    LocalDate dateNotificationPartenaire) {
        if (mode == null || dateDissolution == null) {
            return false;
        }
        switch (mode) {
            case DECLARATION_UNILATERALE:
                return dateNotificationPartenaire != null
                        && !dateNotificationPartenaire.isBefore(dateDissolution);
            case DECLARATION_CONJOINTE:
            case MARIAGE_PARTENAIRES:
            case MARIAGE_TIERS:
            case DECES:
            default:
                return true;
        }
    }

    private static boolean computeDelaiNotificationOk(ModeDissolution mode,
                                                      LocalDate dateDissolution,
                                                      LocalDate dateNotificationPartenaire) {
        if (mode != ModeDissolution.DECLARATION_UNILATERALE) {
            return true;
        }
        if (dateDissolution == null || dateNotificationPartenaire == null) {
            return false;
        }
        return !dateNotificationPartenaire.isBefore(dateDissolution);
    }

    private static int computeScore(boolean patrimoineCommunSignificatif,
                                    List<CreanceType> creances,
                                    int dureeUnionAnnees,
                                    RegimeBiens regimeBiens,
                                    int enfantsCommuns) {
        int score = 0;
        if (patrimoineCommunSignificatif) {
            score += SCORE_PATRIMOINE_COMMUN;
        }
        long nbCreancesEffectives = creances.stream()
                .filter(c -> c != null && c != CreanceType.AUCUNE)
                .distinct()
                .count();
        if (nbCreancesEffectives >= SEUIL_CREANCES_MULTIPLES) {
            score += SCORE_CREANCES_MULTIPLES;
        }
        if (dureeUnionAnnees >= SEUIL_DUREE_LONGUE_ANS) {
            score += SCORE_DUREE_LONGUE;
        }
        if (regimeBiens != null && regimeBiens != RegimeBiens.SEPARATION_BIENS) {
            score += SCORE_REGIME_NON_SEPARATION;
        }
        if (enfantsCommuns >= 1) {
            score += SCORE_ENFANTS;
        }
        return score;
    }

    private static String computeVerdict(int score) {
        if (score == 0) return VERDICT_RIEN;
        if (score >= SEUIL_CONTENTIEUX) return VERDICT_CONTENTIEUX;
        if (score >= SEUIL_MEDIATION) return VERDICT_MEDIATION;
        return VERDICT_LIQUIDATION;
    }

    private static List<CreanceType> computeCreancesVisibles(List<CreanceType> creances,
                                                             boolean eligible) {
        if (!eligible) {
            return List.of();
        }
        List<CreanceType> out = new ArrayList<>();
        for (CreanceType c : creances) {
            if (c != null && c != CreanceType.AUCUNE && !out.contains(c)) {
                out.add(c);
            }
        }
        return out;
    }

    private static void validateInputs(ModeDissolution mode,
                                       RegimeBiens regime,
                                       LocalDate dateConclusionPacs,
                                       LocalDate dateDissolution,
                                       LocalDate dateNotificationPartenaire,
                                       int dureeUnionAnnees,
                                       int enfantsCommuns,
                                       Clock clock) {
        LocalDate today = LocalDate.now(clock);
        if (mode == null) {
            throw new IllegalArgumentException("modeDissolution est requis");
        }
        if (regime == null) {
            throw new IllegalArgumentException("regimeBiens est requis");
        }
        if (dureeUnionAnnees < 0) {
            throw new IllegalArgumentException("dureeUnionAnnees doit être positif ou nul");
        }
        if (enfantsCommuns < 0) {
            throw new IllegalArgumentException("enfantsCommuns doit être positif ou nul");
        }
        if (dateConclusionPacs != null && dateConclusionPacs.isAfter(today)) {
            throw new IllegalArgumentException("dateConclusionPacs ne peut pas être dans le futur");
        }
        if (dateDissolution != null && dateDissolution.isAfter(today)) {
            throw new IllegalArgumentException("dateDissolution ne peut pas être dans le futur");
        }
        if (dateConclusionPacs != null && dateDissolution != null
                && dateDissolution.isBefore(dateConclusionPacs)) {
            throw new IllegalArgumentException(
                    "dateDissolution ne peut pas être antérieure à dateConclusionPacs");
        }
        if (dateNotificationPartenaire != null && dateNotificationPartenaire.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateNotificationPartenaire ne peut pas être dans le futur");
        }
    }

    private static List<String> buildMessages(ModeDissolution mode,
                                              boolean dissolutionValide,
                                              boolean delaiNotificationOk,
                                              boolean dureeUnionEligibleCreances,
                                              RegimeBiens regime,
                                              boolean patrimoineCommunSignificatif,
                                              int enfantsCommuns,
                                              String verdict) {
        List<String> messages = new ArrayList<>();
        messages.add("Pas de prestation compensatoire entre partenaires PACS — "
                + "uniquement créances entre partenaires (art. 515-7-1 Cciv).");
        messages.add("Prescription des actions en créances : " + DELAI_PRESCRIPTION_ANNEES
                + " ans à compter de la dissolution (art. 2224 Cciv).");

        if (mode == ModeDissolution.DECLARATION_UNILATERALE) {
            if (!delaiNotificationOk) {
                messages.add("Déclaration unilatérale : la signification par huissier "
                        + "au partenaire est obligatoire (art. 515-7 Cciv) avant remise "
                        + "au greffe — vérifier la date de notification.");
            } else {
                messages.add("Déclaration unilatérale : signification par huissier OK, "
                        + "remise au greffe pour effet erga omnes.");
            }
        }
        if (mode == ModeDissolution.DECLARATION_CONJOINTE) {
            messages.add("Déclaration conjointe au greffe : modalité simple, "
                    + "pas de signification requise.");
        }
        if (mode == ModeDissolution.MARIAGE_PARTENAIRES) {
            messages.add("Mariage des partenaires entre eux : dissolution automatique "
                    + "(art. 515-7 al. 4 Cciv) — bascule régime époux.");
        }
        if (mode == ModeDissolution.MARIAGE_TIERS) {
            messages.add("Mariage de l'un avec un tiers : dissolution automatique. "
                    + "Notifier le partenaire et signaler au greffe.");
        }
        if (mode == ModeDissolution.DECES) {
            messages.add("Décès d'un partenaire : dissolution de plein droit. "
                    + "Liquidation du régime + droits successoraux à examiner.");
        }

        if (!dissolutionValide) {
            messages.add("Dissolution incomplète : compléter les formalités "
                    + "avant toute action en créance.");
        }
        if (!dureeUnionEligibleCreances) {
            messages.add("Union < 1 an : difficile de caractériser un déséquilibre "
                    + "durable — créances peu probables.");
        }
        if (regime == RegimeBiens.INDIVISION_PAR_DEFAUT) {
            messages.add("PACS conclu avant 2007 — régime indivision par défaut. "
                    + "Vérifier les biens acquis pendant l'union.");
        }
        if (regime == RegimeBiens.INDIVISION_AMENAGEE) {
            messages.add("Régime d'indivision aménagée : partage selon convention. "
                    + "Cf. F-FA-22 (indivision post-communautaire) si applicable.");
        }
        if (patrimoineCommunSignificatif) {
            messages.add("Patrimoine commun significatif : prévoir une liquidation "
                    + "formalisée (notaire si bien immobilier).");
        }
        if (enfantsCommuns >= 1) {
            messages.add("Enfants communs : prévoir autorité parentale "
                    + "et pension alimentaire (cf. F-FA-02 et F-FA-19).");
        }

        switch (verdict) {
            case VERDICT_CONTENTIEUX:
                messages.add("Verdict CONTENTIEUX_INEVITABLE : préparer assignation devant "
                        + "le JAF (art. 515-7-1 Cciv) — médiation préalable possible mais "
                        + "issue probable contentieuse.");
                break;
            case VERDICT_MEDIATION:
                messages.add("Verdict MEDIATION_OU_JAF : tenter une médiation, "
                        + "à défaut saisir le JAF compétent.");
                break;
            case VERDICT_LIQUIDATION:
                messages.add("Verdict LIQUIDATION_AMIABLE : régler les créances par "
                        + "convention écrite entre partenaires.");
                break;
            case VERDICT_RIEN:
            default:
                messages.add("Verdict RIEN_A_FAIRE : aucun déséquilibre patrimonial "
                        + "manifeste, dissolution simple.");
                break;
        }
        return messages;
    }

    private static String buildFormule(ModeDissolution mode,
                                       int score,
                                       String verdict,
                                       int dureeUnionAnnees,
                                       RegimeBiens regime,
                                       boolean patrimoineCommunSignificatif,
                                       int nbCreancesAlleguees,
                                       int enfantsCommuns) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dissolution PACS (art. 515-7 Cciv) mode ").append(mode)
                .append(" — score créances ").append(score)
                .append("/100 → ").append(verdict).append(".");
        sb.append(" Décomposition : patrimoine commun=")
                .append(patrimoineCommunSignificatif ? "+30" : "0")
                .append(", créances alléguées=").append(nbCreancesAlleguees)
                .append(nbCreancesAlleguees >= SEUIL_CREANCES_MULTIPLES ? " (+25)" : " (0)")
                .append(", durée=").append(dureeUnionAnnees).append(" ans")
                .append(dureeUnionAnnees >= SEUIL_DUREE_LONGUE_ANS ? " (+20)" : " (0)")
                .append(", régime=").append(regime)
                .append(regime != null && regime != RegimeBiens.SEPARATION_BIENS
                        ? " (+15)" : " (0)")
                .append(", enfants=").append(enfantsCommuns)
                .append(enfantsCommuns >= 1 ? " (+10)" : " (0)").append(".");
        sb.append(" Prescription créances : ").append(DELAI_PRESCRIPTION_ANNEES)
                .append(" ans (art. 2224 Cciv).");
        return sb.toString();
    }
}
