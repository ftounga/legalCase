package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SF-DT-35-01 : calculateur de la <b>contestation des décisions France Travail</b>
 * (ex-Pôle emploi) — refus d'ouverture de droits, contestation montant/durée
 * d'indemnité ARE, radiation, trop-perçu.
 *
 * <p>Base juridique : art. L.5422-1 et s. + R.5422-1 Code du travail + Loi 18/12/2023
 * (plein emploi — Pôle emploi devenu France Travail au 1er janvier 2024).
 *
 * <p>Procédure type :
 * <ol>
 *   <li>Recours hiérarchique préalable obligatoire devant France Travail dans un
 *       délai pratique standard de <b>2 mois</b> (60 jours) après notification.</li>
 *   <li>Réponse explicite ou décision implicite de rejet à l'expiration de 2 mois
 *       (art. L.231-4 CRPA).</li>
 *   <li>Recours contentieux devant le tribunal administratif dans les 2 mois
 *       suivants (art. R.421-1 CJA).</li>
 * </ol>
 *
 * <p>Score de probabilité de succès 0-100 :
 * <ul>
 *   <li>+30 si {@code preuves.size() >= 2}</li>
 *   <li>+25 si motif reconnu (≠ AUTRE)</li>
 *   <li>+20 si {@code !demandeurDejaSaisiTribunal}</li>
 *   <li>+15 si {@code delaiContestationRespecte}</li>
 *   <li>+10 si {@code montantContesteEur >= 500 €} (montant significatif)</li>
 * </ul>
 *
 * <p>Verdict :
 * <ul>
 *   <li>{@code RECOURS_HIERARCHIQUE_PRIORITAIRE} si score ≥ 70</li>
 *   <li>{@code CONTENTIEUX_TA_DIRECT_POSSIBLE} si 40 ≤ score &lt; 70</li>
 *   <li>{@code INSUFFISAMMENT_FONDE} si score &lt; 40</li>
 *   <li>{@code AUTRE_VOIE} si type AUTRE et motif AUTRE (cumulatif)</li>
 * </ul>
 *
 * <p>Outil <b>single-country FRANCE</b>. L'équivalent belge (réclamation ONEM
 * + juridiction du travail) est une procédure juridiquement distincte — feature
 * jumelle backlog.
 */
public final class ContestationAreCalculator {

    public static final int SEUIL_VERDICT_PRIORITAIRE = 70;
    public static final int SEUIL_VERDICT_TA_DIRECT = 40;

    public static final int POIDS_PREUVES_DIVERSIFIEES = 30;
    public static final int POIDS_MOTIF_RECONNU = 25;
    public static final int POIDS_PAS_DEJA_TRIBUNAL = 20;
    public static final int POIDS_DELAI_RESPECTE = 15;
    public static final int POIDS_MONTANT_SIGNIFICATIF = 10;

    public static final int SEUIL_PREUVES_DIVERSIFIEES = 2;
    public static final BigDecimal SEUIL_MONTANT_SIGNIFICATIF = new BigDecimal("500.00");

    /** Délai pratique standard du recours hiérarchique préalable (jours). */
    public static final int DELAI_RECOURS_HIERARCHIQUE_JOURS = 60;

    /** Délai prévisionnel d'instruction du recours hiérarchique (mois). */
    public static final int DELAI_INSTRUCTION_MOIS_PREVISIONNEL = 4;

    static final String BASE_JURIDIQUE =
            "Art. L.5422-1 et s. + R.5422-1 Code travail + Loi 18/12/2023 France Travail";

    private ContestationAreCalculator() {
    }

    public static ContestationAreResult compute(String typeDecisionContestee,
                                                String motifContestation,
                                                LocalDate dateNotificationDecision,
                                                LocalDate dateRecoursHierarchiquePropose,
                                                List<String> preuvesProduites,
                                                BigDecimal montantContesteEur,
                                                boolean demandeurDejaSaisiTribunal,
                                                boolean delaiContestationRespecte) {
        if (dateNotificationDecision == null) {
            throw new IllegalArgumentException("dateNotificationDecision est requise");
        }
        ContestationAreTypeDecision typeEnum = parseType(typeDecisionContestee);
        ContestationAreMotif motifEnum = parseMotif(motifContestation);
        List<String> preuves = validateAndNormalizePreuves(preuvesProduites);
        BigDecimal montant = validateMontant(montantContesteEur);

        boolean delaiHierarchiqueOk = computeDelaiHierarchiqueOk(
                dateNotificationDecision, dateRecoursHierarchiquePropose);
        boolean delaiContentieuxOk = delaiContestationRespecte;

        int score = computeScore(preuves.size(), motifEnum, demandeurDejaSaisiTribunal,
                delaiContestationRespecte, montant);
        ContestationAreVerdict verdictEnum = computeVerdict(score, typeEnum, motifEnum);
        boolean expertiseRecommandee = motifEnum == ContestationAreMotif.ERREUR_CALCUL_REMUNERATION_REFERENCE;

        String formule = buildFormule(score, verdictEnum, preuves.size(), motifEnum,
                demandeurDejaSaisiTribunal, delaiContestationRespecte, montant);
        List<String> messages = buildMessages(verdictEnum, typeEnum, motifEnum,
                delaiHierarchiqueOk, delaiContentieuxOk, expertiseRecommandee,
                preuves.size(), demandeurDejaSaisiTribunal);

        return new ContestationAreResult(
                typeEnum.name(),
                motifEnum.name(),
                dateNotificationDecision,
                dateRecoursHierarchiquePropose,
                preuves,
                montant,
                demandeurDejaSaisiTribunal,
                delaiContestationRespecte,
                delaiHierarchiqueOk,
                delaiContentieuxOk,
                score,
                verdictEnum.name(),
                DELAI_INSTRUCTION_MOIS_PREVISIONNEL,
                expertiseRecommandee,
                BASE_JURIDIQUE,
                formule,
                messages);
    }

    // =========================================================================
    // Validation & normalisation
    // =========================================================================

    private static ContestationAreTypeDecision parseType(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("typeDecisionContestee est requis");
        }
        try {
            return ContestationAreTypeDecision.valueOf(code.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Type décision contestée inconnu : "
                    + code + ". Valeurs : " + enumNames(ContestationAreTypeDecision.class));
        }
    }

    private static ContestationAreMotif parseMotif(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("motifContestation est requis");
        }
        try {
            return ContestationAreMotif.valueOf(code.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Motif contestation inconnu : "
                    + code + ". Valeurs : " + enumNames(ContestationAreMotif.class));
        }
    }

    private static List<String> validateAndNormalizePreuves(List<String> raw) {
        Set<String> valides = enumNames(ContestationArePreuveType.class);
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        Set<String> deduplicated = new LinkedHashSet<>();
        for (String code : raw) {
            if (code == null || code.isBlank()) {
                continue;
            }
            String trimmed = code.trim();
            if (!valides.contains(trimmed)) {
                throw new IllegalArgumentException("Preuve inconnue : " + trimmed
                        + ". Valeurs : " + valides);
            }
            deduplicated.add(trimmed);
        }
        return new ArrayList<>(deduplicated);
    }

    private static BigDecimal validateMontant(BigDecimal montant) {
        if (montant == null) {
            return null;
        }
        if (montant.signum() < 0) {
            throw new IllegalArgumentException("montantContesteEur ne peut être négatif");
        }
        return montant;
    }

    private static <E extends Enum<E>> Set<String> enumNames(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // =========================================================================
    // Délais
    // =========================================================================

    private static boolean computeDelaiHierarchiqueOk(LocalDate dateNotification,
                                                     LocalDate dateRecoursPropose) {
        if (dateRecoursPropose == null) {
            // Pas de date proposée → on considère le délai respecté tant qu'il n'y a
            // pas de débordement constaté (champ optionnel).
            return true;
        }
        long jours = ChronoUnit.DAYS.between(dateNotification, dateRecoursPropose);
        return jours >= 0 && jours <= DELAI_RECOURS_HIERARCHIQUE_JOURS;
    }

    // =========================================================================
    // Score & verdict
    // =========================================================================

    private static int computeScore(int nbPreuves,
                                    ContestationAreMotif motif,
                                    boolean dejaSaisiTribunal,
                                    boolean delaiRespecte,
                                    BigDecimal montant) {
        int score = 0;
        if (nbPreuves >= SEUIL_PREUVES_DIVERSIFIEES) {
            score += POIDS_PREUVES_DIVERSIFIEES;
        }
        if (motif != ContestationAreMotif.AUTRE) {
            score += POIDS_MOTIF_RECONNU;
        }
        if (!dejaSaisiTribunal) {
            score += POIDS_PAS_DEJA_TRIBUNAL;
        }
        if (delaiRespecte) {
            score += POIDS_DELAI_RESPECTE;
        }
        if (montant != null && montant.compareTo(SEUIL_MONTANT_SIGNIFICATIF) >= 0) {
            score += POIDS_MONTANT_SIGNIFICATIF;
        }
        return Math.max(0, Math.min(100, score));
    }

    private static ContestationAreVerdict computeVerdict(int score,
                                                         ContestationAreTypeDecision type,
                                                         ContestationAreMotif motif) {
        if (type == ContestationAreTypeDecision.AUTRE && motif == ContestationAreMotif.AUTRE) {
            return ContestationAreVerdict.AUTRE_VOIE;
        }
        if (score >= SEUIL_VERDICT_PRIORITAIRE) {
            return ContestationAreVerdict.RECOURS_HIERARCHIQUE_PRIORITAIRE;
        }
        if (score >= SEUIL_VERDICT_TA_DIRECT) {
            return ContestationAreVerdict.CONTENTIEUX_TA_DIRECT_POSSIBLE;
        }
        return ContestationAreVerdict.INSUFFISAMMENT_FONDE;
    }

    // =========================================================================
    // Formule & messages
    // =========================================================================

    private static String buildFormule(int score,
                                       ContestationAreVerdict verdict,
                                       int nbPreuves,
                                       ContestationAreMotif motif,
                                       boolean dejaSaisiTribunal,
                                       boolean delaiRespecte,
                                       BigDecimal montant) {
        StringBuilder sb = new StringBuilder();
        sb.append("Score ").append(score).append(" = ");
        List<String> facteurs = new ArrayList<>();
        if (nbPreuves >= SEUIL_PREUVES_DIVERSIFIEES) {
            facteurs.add("preuves≥2 (+" + POIDS_PREUVES_DIVERSIFIEES + ")");
        }
        if (motif != ContestationAreMotif.AUTRE) {
            facteurs.add("motif reconnu (+" + POIDS_MOTIF_RECONNU + ")");
        }
        if (!dejaSaisiTribunal) {
            facteurs.add("pas de tribunal saisi (+" + POIDS_PAS_DEJA_TRIBUNAL + ")");
        }
        if (delaiRespecte) {
            facteurs.add("délai respecté (+" + POIDS_DELAI_RESPECTE + ")");
        }
        if (montant != null && montant.compareTo(SEUIL_MONTANT_SIGNIFICATIF) >= 0) {
            facteurs.add("montant significatif (+" + POIDS_MONTANT_SIGNIFICATIF + ")");
        }
        if (facteurs.isEmpty()) {
            sb.append("aucun critère favorable");
        } else {
            sb.append(String.join(" + ", facteurs));
        }
        sb.append(" → verdict ").append(verdict.name()).append(".");
        return sb.toString();
    }

    private static List<String> buildMessages(ContestationAreVerdict verdict,
                                              ContestationAreTypeDecision type,
                                              ContestationAreMotif motif,
                                              boolean delaiHierarchiqueOk,
                                              boolean delaiContentieuxOk,
                                              boolean expertiseRecommandee,
                                              int nbPreuves,
                                              boolean dejaSaisiTribunal) {
        List<String> messages = new ArrayList<>();
        messages.add("Recours préalable obligatoire avant TA (art. R.421-1 CJA + L.231-4 CRPA).");
        messages.add("Délai pratique de saisine France Travail : "
                + DELAI_RECOURS_HIERARCHIQUE_JOURS + " jours après notification.");

        if (!delaiHierarchiqueOk) {
            messages.add("Délai recours hiérarchique dépassé : la voie préalable est forclose, "
                    + "envisager la saisine directe du TA si le délai contentieux est encore ouvert.");
        }
        if (!delaiContentieuxOk) {
            messages.add("Délai contentieux non respecté : forclusion probable, vérifier les "
                    + "exceptions (notification irrégulière, absence d'accusé de réception, etc.).");
        }
        if (expertiseRecommandee) {
            messages.add("Expertise sur le calcul du salaire journalier de référence (SJR) "
                    + "fortement recommandée (motif ERREUR_CALCUL_REMUNERATION_REFERENCE).");
        }
        if (nbPreuves < SEUIL_PREUVES_DIVERSIFIEES) {
            messages.add("Preuves limitées : compléter par bulletins de paie, attestation employeur, "
                    + "lettre de licenciement — la diversification renforce la probabilité de succès.");
        }
        if (dejaSaisiTribunal) {
            messages.add("Tribunal déjà saisi : risque d'irrecevabilité pour litispendance, "
                    + "vérifier la nature précise de la procédure parallèle.");
        }
        if (motif == ContestationAreMotif.MAUVAISE_QUALIFICATION_RUPTURE) {
            messages.add("Mauvaise qualification de la rupture : croiser avec l'attestation "
                    + "employeur et la lettre de licenciement, voir aussi un éventuel jugement prud'homal.");
        }

        switch (verdict) {
            case RECOURS_HIERARCHIQUE_PRIORITAIRE -> messages.add(
                    "Recours hiérarchique fortement recommandé — préparer dossier complet "
                            + "(motif, preuves, montant chiffré) avant saisine France Travail.");
            case CONTENTIEUX_TA_DIRECT_POSSIBLE -> messages.add(
                    "Recours hiérarchique conseillé en priorité ; saisine TA possible "
                            + "à l'expiration du délai d'instruction (4 mois) ou en cas de refus.");
            case INSUFFISAMMENT_FONDE -> messages.add(
                    "Dossier à étayer avant saisine — réunir preuves complémentaires "
                            + "et/ou faire intervenir un expert.");
            case AUTRE_VOIE -> messages.add(
                    "Type et motif non standardisés : qualifier précisément la décision "
                            + "et le moyen juridique avant toute saisine.");
        }

        messages.add("Délai prévisionnel d'instruction par France Travail : "
                + DELAI_INSTRUCTION_MOIS_PREVISIONNEL + " mois.");
        messages.add("Décision implicite de rejet à 2 mois → délai de 2 mois pour saisir le TA "
                + "(art. R.421-1 CJA).");
        return messages;
    }
}
