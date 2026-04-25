package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-DT-34-01 : calculateur du référé prud'homal FR (art. R.1454-1 et s. Code du travail).
 *
 * <p>Évalue la probabilité de succès d'un référé prud'homal devant la formation de référé
 * du Conseil de prud'hommes. Conditions cumulatives R.1454-1 :
 * <ul>
 *   <li>Urgence + absence de contestation sérieuse → provision (R.1454-1 al. 1)</li>
 *   <li>Mesures d'instruction utiles (expertises) → R.1454-1 al. 2</li>
 *   <li>Mesures conservatoires nécessaires → trouble manifestement illicite</li>
 * </ul>
 *
 * <p>Outil <b>single-country FR</b>. Le référé en BE relève d'une procédure différente
 * (art. 19 al. 3 Code judiciaire — référé absolue urgence devant le tribunal du travail) qui
 * fera l'objet d'une feature jumelle au backlog.
 */
public final class ReferePrudhomalCalculator {

    public static final int DELAI_AUDIENCE_JOURS = 15;        // R.1455-1 — bref délai (pratique)
    public static final int DELAI_ORDONNANCE_JOURS = 8;       // R.1455-2 — pratique constante

    public static final int BONUS_ABSENCE_CONTESTATION = 35;
    public static final int BONUS_PREUVES_AU_MOINS_2 = 25;
    public static final int BONUS_DOMMAGE_IMMEDIAT = 20;
    public static final int BONUS_TRESORERIE_DOUTEUSE = 10;
    public static final int BONUS_URGENCE_CARACTERISEE = 10;

    public static final int SEUIL_PROVISION_PROBABLE = 70;
    public static final int SEUIL_INSUFFISAMMENT_FONDE = 40;

    public static final Set<String> TYPES_REFERE = Set.of(
            "PROVISION_SALAIRES",
            "EXPERTISE_MEDICALE",
            "EXPERTISE_TECHNIQUE",
            "MESURES_CONSERVATOIRES",
            "REINTEGRATION_URGENCE",
            "AUTRE");

    public static final Set<String> NATURES_CREANCE = Set.of(
            "SALAIRES_NON_VERSES",
            "INDEMNITE_RUPTURE",
            "HEURES_SUPPLEMENTAIRES",
            "PRIMES",
            "CONGES_PAYES",
            "AUTRE");

    public static final Set<String> PREUVES_URGENCE = Set.of(
            "BULLETIN_PAIE",
            "RELANCE_EMPLOYEUR",
            "MISE_EN_DEMEURE",
            "CONSTAT_HUISSIER",
            "CERTIFICAT_MEDICAL",
            "CONTRAT",
            "AUTRE");

    public static final String VERDICT_PROVISION_PROBABLE = "PROVISION_PROBABLE";
    public static final String VERDICT_EXPERTISE_RECOMMANDEE = "EXPERTISE_RECOMMANDEE";
    public static final String VERDICT_INSUFFISAMMENT_FONDE = "INSUFFISAMMENT_FONDE";
    public static final String VERDICT_AUTRE_VOIE_RECOMMANDEE = "AUTRE_VOIE_RECOMMANDEE";

    private static final String BASE_JURIDIQUE =
            "Art. R.1454-1 + R.1454-3 + R.1455-1 Code travail";

    private ReferePrudhomalCalculator() {
    }

    public static ReferePrudhomalResult compute(String typeRefere,
                                                String natureCreance,
                                                BigDecimal montantProvisionDemandeeEur,
                                                boolean absenceContestationSerieuse,
                                                List<String> preuvesUrgenceProduites,
                                                boolean dommageImmediatCarac,
                                                boolean tresorerieEmployeurDouteuse,
                                                LocalDate dateMiseEnDemeure,
                                                Integer ancienneteContratMois) {
        return compute(typeRefere, natureCreance, montantProvisionDemandeeEur,
                absenceContestationSerieuse, preuvesUrgenceProduites,
                dommageImmediatCarac, tresorerieEmployeurDouteuse,
                dateMiseEnDemeure, ancienneteContratMois,
                Clock.system(ZoneId.of("Europe/Paris")));
    }

    public static ReferePrudhomalResult compute(String typeRefere,
                                                String natureCreance,
                                                BigDecimal montantProvisionDemandeeEur,
                                                boolean absenceContestationSerieuse,
                                                List<String> preuvesUrgenceProduites,
                                                boolean dommageImmediatCarac,
                                                boolean tresorerieEmployeurDouteuse,
                                                LocalDate dateMiseEnDemeure,
                                                Integer ancienneteContratMois,
                                                Clock clock) {
        validate(typeRefere, natureCreance, montantProvisionDemandeeEur,
                preuvesUrgenceProduites, dateMiseEnDemeure, ancienneteContratMois, clock);

        List<String> preuvesNormalized = preuvesUrgenceProduites != null
                ? List.copyOf(preuvesUrgenceProduites)
                : List.of();

        // Urgence caractérisée si dommage immédiat OU mise en demeure ≥ 7 jours sans réponse
        boolean urgenceCaracterisee = dommageImmediatCarac
                || (dateMiseEnDemeure != null
                        && java.time.temporal.ChronoUnit.DAYS.between(dateMiseEnDemeure,
                                LocalDate.now(clock)) >= 7);

        int score = 0;
        if (absenceContestationSerieuse) {
            score += BONUS_ABSENCE_CONTESTATION;
        }
        if (preuvesNormalized.size() >= 2) {
            score += BONUS_PREUVES_AU_MOINS_2;
        }
        if (dommageImmediatCarac) {
            score += BONUS_DOMMAGE_IMMEDIAT;
        }
        if (tresorerieEmployeurDouteuse) {
            score += BONUS_TRESORERIE_DOUTEUSE;
        }
        if (urgenceCaracterisee) {
            score += BONUS_URGENCE_CARACTERISEE;
        }
        score = Math.min(100, score);

        String verdict = computeVerdict(typeRefere, score);

        BigDecimal montantRecommande;
        if (montantProvisionDemandeeEur != null
                && montantProvisionDemandeeEur.signum() > 0
                && absenceContestationSerieuse
                && "PROVISION_SALAIRES".equals(typeRefere)) {
            montantRecommande = montantProvisionDemandeeEur.setScale(2, RoundingMode.HALF_UP);
        } else {
            montantRecommande = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        List<String> messages = buildMessages(typeRefere, natureCreance, score, verdict,
                absenceContestationSerieuse, urgenceCaracterisee, tresorerieEmployeurDouteuse,
                preuvesNormalized.size());

        String formule = buildFormule(typeRefere, score, verdict,
                absenceContestationSerieuse, preuvesNormalized.size(),
                dommageImmediatCarac);

        return new ReferePrudhomalResult(
                typeRefere,
                natureCreance,
                montantProvisionDemandeeEur,
                absenceContestationSerieuse,
                preuvesNormalized,
                dommageImmediatCarac,
                tresorerieEmployeurDouteuse,
                dateMiseEnDemeure,
                ancienneteContratMois,
                score,
                verdict,
                DELAI_AUDIENCE_JOURS,
                DELAI_ORDONNANCE_JOURS,
                montantRecommande,
                BASE_JURIDIQUE,
                formule,
                messages);
    }

    private static String computeVerdict(String typeRefere, int score) {
        if ("EXPERTISE_MEDICALE".equals(typeRefere) || "EXPERTISE_TECHNIQUE".equals(typeRefere)) {
            return VERDICT_EXPERTISE_RECOMMANDEE;
        }
        if (score < SEUIL_INSUFFISAMMENT_FONDE) {
            return VERDICT_INSUFFISAMMENT_FONDE;
        }
        if ("PROVISION_SALAIRES".equals(typeRefere) && score >= SEUIL_PROVISION_PROBABLE) {
            return VERDICT_PROVISION_PROBABLE;
        }
        return VERDICT_AUTRE_VOIE_RECOMMANDEE;
    }

    private static List<String> buildMessages(String typeRefere,
                                              String natureCreance,
                                              int score,
                                              String verdict,
                                              boolean absenceContestationSerieuse,
                                              boolean urgenceCaracterisee,
                                              boolean tresorerieEmployeurDouteuse,
                                              int nbPreuves) {
        List<String> msgs = new ArrayList<>();
        msgs.add("Référé statué dans " + DELAI_AUDIENCE_JOURS
                + " jours en moyenne entre l'assignation et l'audience (R.1455-1, bref délai).");
        msgs.add("Ordonnance rendue dans environ " + DELAI_ORDONNANCE_JOURS
                + " jours après l'audience (R.1455-2, pratique constante).");
        msgs.add("Conditions R.1454-1 al. 1 : urgence + absence de contestation sérieuse.");
        msgs.add("Conditions R.1454-1 al. 2 : mesures d'instruction utiles (expertises).");

        if (!absenceContestationSerieuse
                && ("PROVISION_SALAIRES".equals(typeRefere) || "MESURES_CONSERVATOIRES".equals(typeRefere))) {
            msgs.add("Contestation sérieuse non écartée — risque de renvoi au fond. "
                    + "Renforcer le dossier (preuves matérielles, constat huissier).");
        }
        if (nbPreuves < 2 && !"AUTRE".equals(typeRefere)) {
            msgs.add("Moins de 2 preuves d'urgence produites — produire bulletins, mises en demeure, "
                    + "constats huissier pour caractériser l'urgence.");
        }
        if (tresorerieEmployeurDouteuse) {
            msgs.add("Trésorerie employeur douteuse — argument supplémentaire pour la provision "
                    + "et les mesures conservatoires.");
        }
        if (!urgenceCaracterisee) {
            msgs.add("Urgence non caractérisée — saisir le bureau de jugement par voie ordinaire "
                    + "ou compléter la mise en demeure.");
        }

        switch (typeRefere) {
            case "PROVISION_SALAIRES" -> msgs.add(
                    "Provision sur salaires (R.1454-1 al. 1) — créance non sérieusement contestable obligatoire.");
            case "EXPERTISE_MEDICALE" -> msgs.add(
                    "Expertise médicale (R.1454-1 al. 2) — utile en AT/MP, harcèlement, inaptitude. "
                            + "Mesure d'instruction non subordonnée à l'urgence.");
            case "EXPERTISE_TECHNIQUE" -> msgs.add(
                    "Expertise technique (R.1454-1 al. 2) — utile pour calculs heures sup., primes, "
                            + "comparaison salaires. Mesure d'instruction.");
            case "MESURES_CONSERVATOIRES" -> msgs.add(
                    "Mesures conservatoires — trouble manifestement illicite ou dommage imminent.");
            case "REINTEGRATION_URGENCE" -> msgs.add(
                    "Réintégration en urgence — applicable si licenciement nul (salarié protégé, "
                            + "harcèlement, etc.) — voie alternative au fond.");
            case "AUTRE" -> msgs.add(
                    "Type AUTRE — caractériser la mesure demandée et vérifier la compétence.");
            default -> { /* validated */ }
        }

        switch (natureCreance) {
            case "SALAIRES_NON_VERSES" -> msgs.add(
                    "Créance salariale — privilège général + super-privilège en cas de procédure collective.");
            case "INDEMNITE_RUPTURE" -> msgs.add(
                    "Indemnité de rupture — vérifier que la rupture est notifiée et l'indemnité chiffrable.");
            case "HEURES_SUPPLEMENTAIRES" -> msgs.add(
                    "Heures supplémentaires — souvent contestable (sérieux), expertise technique recommandée.");
            case "PRIMES" -> msgs.add(
                    "Primes — vérifier le caractère certain (usage, accord d'entreprise, contrat).");
            case "CONGES_PAYES" -> msgs.add(
                    "Indemnité compensatrice de congés payés — créance liquide en fin de contrat.");
            case "AUTRE" -> msgs.add(
                    "Nature AUTRE — préciser le fondement contractuel ou conventionnel.");
            default -> { /* validated */ }
        }

        switch (verdict) {
            case VERDICT_PROVISION_PROBABLE -> msgs.add(
                    "Verdict : provision probable — assigner en référé devant la formation de référé du CPH.");
            case VERDICT_EXPERTISE_RECOMMANDEE -> msgs.add(
                    "Verdict : expertise recommandée — voie utile même sans urgence (R.1454-1 al. 2).");
            case VERDICT_INSUFFISAMMENT_FONDE -> msgs.add(
                    "Verdict : référé insuffisamment fondé — privilégier la voie ordinaire (bureau de jugement).");
            case VERDICT_AUTRE_VOIE_RECOMMANDEE -> msgs.add(
                    "Verdict : autre voie recommandée — réexaminer la stratégie (fond ou compléments).");
            default -> { /* unreachable */ }
        }

        msgs.add("Score " + score + "/100 — indicatif, le juge des référés conserve son pouvoir d'appréciation.");
        return msgs;
    }

    private static String buildFormule(String typeRefere,
                                       int score,
                                       String verdict,
                                       boolean absenceContestationSerieuse,
                                       int nbPreuves,
                                       boolean dommageImmediatCarac) {
        StringBuilder sb = new StringBuilder();
        sb.append("Type ").append(typeRefere);
        if (absenceContestationSerieuse) {
            sb.append(" + créance non contestée");
        }
        if (nbPreuves >= 2) {
            sb.append(" + ").append(nbPreuves).append(" preuves");
        }
        if (dommageImmediatCarac) {
            sb.append(" + dommage immédiat");
        }
        sb.append(" → score ").append(score).append(" → ").append(verdict);
        return sb.toString();
    }

    private static void validate(String typeRefere,
                                 String natureCreance,
                                 BigDecimal montantProvisionDemandeeEur,
                                 List<String> preuvesUrgenceProduites,
                                 LocalDate dateMiseEnDemeure,
                                 Integer ancienneteContratMois,
                                 Clock clock) {
        if (typeRefere == null || typeRefere.isBlank()) {
            throw new IllegalArgumentException("typeRefere est requis");
        }
        if (!TYPES_REFERE.contains(typeRefere)) {
            throw new IllegalArgumentException(
                    "typeRefere inconnu — valeurs attendues : " + TYPES_REFERE);
        }
        if (natureCreance == null || natureCreance.isBlank()) {
            throw new IllegalArgumentException("natureCreance est requise");
        }
        if (!NATURES_CREANCE.contains(natureCreance)) {
            throw new IllegalArgumentException(
                    "natureCreance inconnue — valeurs attendues : " + NATURES_CREANCE);
        }
        if (montantProvisionDemandeeEur != null && montantProvisionDemandeeEur.signum() < 0) {
            throw new IllegalArgumentException(
                    "montantProvisionDemandeeEur ne peut être négatif");
        }
        if (preuvesUrgenceProduites != null) {
            for (String p : preuvesUrgenceProduites) {
                if (p == null || !PREUVES_URGENCE.contains(p)) {
                    throw new IllegalArgumentException(
                            "preuveUrgence inconnue : " + p
                                    + " — valeurs attendues : " + PREUVES_URGENCE);
                }
            }
        }
        if (dateMiseEnDemeure != null) {
            LocalDate today = LocalDate.now(clock);
            if (dateMiseEnDemeure.isAfter(today)) {
                throw new IllegalArgumentException(
                        "dateMiseEnDemeure ne peut pas être dans le futur");
            }
        }
        if (ancienneteContratMois != null && ancienneteContratMois < 0) {
            throw new IllegalArgumentException(
                    "ancienneteContratMois ne peut être négative");
        }
    }
}
