package fr.ailegalcase.casefile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-IM-14-01 : calculateur pour la régularisation humanitaire "9bis" belge
 * (art. 9bis Loi 15/12/1980 sur l'accès au territoire, le séjour, l'établissement
 * et l'éloignement des étrangers, précisée par l'AR 17/05/2007).
 *
 * <p>Procédure de régularisation demandée à partir du territoire belge
 * (à la commune du lieu de résidence), contrairement à la voie classique depuis
 * l'étranger. Elle suppose des <b>circonstances exceptionnelles</b> empêchant
 * le retour temporaire au pays d'origine pour y demander un visa.
 *
 * <p>Instruction par l'Office des étrangers (OE), sans délai légal strict —
 * en pratique 3 à 24 mois, moyenne ~18 mois.
 *
 * <p>Appréciation en faisceau d'indices :
 * <ul>
 *   <li>Présence en Belgique (pas de durée minimale légale,
 *       mais ≥ 3 ans est un signal fort)</li>
 *   <li>Enracinement durable : liens familiaux, professionnels ou scolarité
 *       d'enfants en Belgique</li>
 *   <li>Absence de menace pour l'ordre public</li>
 * </ul>
 *
 * <p>Score pondéré 0-100 :
 * <ul>
 *   <li>+25 si présence ≥ 3 ans</li>
 *   <li>+40 si un lien constitutif (famille, travail ou scolarité) est établi</li>
 *   <li>+35 si pas de menace pour l'ordre public</li>
 * </ul>
 *
 * <p>Verdicts : ELEVEE (≥ 75), MOYENNE (40-74), FAIBLE (&lt; 40).
 *
 * <p>Alternative : l'art. <b>9ter</b> de la même loi couvre les motifs médicaux
 * (maladie grave sans traitement accessible au pays) — signal renvoyé dans les
 * messages si besoin.
 *
 * <p>Outil <b>single-country BELGIQUE</b>. L'équivalent français L.435-1 /
 * L.435-2 est couvert par F-IM-09.
 */
public final class Belgian9bisCalculator {

    /** Seuil signal fort de présence en Belgique (sans base légale stricte). */
    public static final int PRESENCE_SIGNAL_ANNEES = 3;

    /** Délai d'instruction OE moyen (mois). */
    public static final int DELAI_INSTRUCTION_MOIS = 18;

    /** Score minimum pour verdict ELEVEE. */
    public static final int SEUIL_ELEVEE = 75;

    /** Score minimum pour verdict MOYENNE. */
    public static final int SEUIL_MOYENNE = 40;

    /** Pondération présence ≥ 3 ans. */
    public static final int POIDS_PRESENCE = 25;

    /** Pondération liens constitutifs (famille OU travail OU scolarité). */
    public static final int POIDS_LIENS = 40;

    /** Pondération absence de menace ordre public. */
    public static final int POIDS_PAS_MENACE = 35;

    private static final String BASE_JURIDIQUE =
            "Loi 15/12/1980 art. 9bis + AR 17/05/2007";

    private Belgian9bisCalculator() {
    }

    public static Belgian9bisResult compute(LocalDate dateEntreeBelgique,
                                            int dureePresenceMois,
                                            boolean circonstancesExceptionnelles,
                                            boolean liensFamiliauxBe,
                                            boolean liensProfessionnels,
                                            boolean scolariteEnfantsBe,
                                            boolean menaceOrdrePublic,
                                            LocalDate dateDepotDemande) {
        return compute(dateEntreeBelgique, dureePresenceMois,
                circonstancesExceptionnelles, liensFamiliauxBe,
                liensProfessionnels, scolariteEnfantsBe,
                menaceOrdrePublic, dateDepotDemande,
                Clock.system(ZoneId.of("Europe/Brussels")));
    }

    public static Belgian9bisResult compute(LocalDate dateEntreeBelgique,
                                            int dureePresenceMois,
                                            boolean circonstancesExceptionnelles,
                                            boolean liensFamiliauxBe,
                                            boolean liensProfessionnels,
                                            boolean scolariteEnfantsBe,
                                            boolean menaceOrdrePublic,
                                            LocalDate dateDepotDemande,
                                            Clock clock) {
        validateInputs(dateEntreeBelgique, dureePresenceMois, dateDepotDemande, clock);

        int anneesPresence = dureePresenceMois / 12;
        boolean presence3AnsOk = anneesPresence >= PRESENCE_SIGNAL_ANNEES;
        boolean liensConstitutifsOk =
                liensFamiliauxBe || liensProfessionnels || scolariteEnfantsBe;
        boolean pasMenace = !menaceOrdrePublic;

        int scoreGlobal = computeScore(presence3AnsOk, liensConstitutifsOk, pasMenace);
        String verdict = verdict(scoreGlobal);

        LocalDate dateExpirationInstruction = dateDepotDemande != null
                ? dateDepotDemande.plusMonths(DELAI_INSTRUCTION_MOIS)
                : null;

        List<String> criteresNonRemplis = buildCriteresNonRemplis(
                presence3AnsOk, liensConstitutifsOk, pasMenace,
                circonstancesExceptionnelles);

        List<String> messages = buildMessages(verdict,
                presence3AnsOk, liensConstitutifsOk,
                liensFamiliauxBe, liensProfessionnels, scolariteEnfantsBe,
                circonstancesExceptionnelles);

        String formule = buildFormule(verdict, scoreGlobal,
                presence3AnsOk, liensConstitutifsOk, pasMenace,
                dateExpirationInstruction);

        return new Belgian9bisResult(
                dateEntreeBelgique,
                dureePresenceMois,
                circonstancesExceptionnelles,
                liensFamiliauxBe,
                liensProfessionnels,
                scolariteEnfantsBe,
                menaceOrdrePublic,
                dateDepotDemande,
                presence3AnsOk,
                liensConstitutifsOk,
                pasMenace,
                scoreGlobal,
                verdict,
                criteresNonRemplis,
                dateExpirationInstruction,
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    private static int computeScore(boolean presence3AnsOk,
                                    boolean liensConstitutifsOk,
                                    boolean pasMenace) {
        int score = 0;
        if (presence3AnsOk) score += POIDS_PRESENCE;
        if (liensConstitutifsOk) score += POIDS_LIENS;
        if (pasMenace) score += POIDS_PAS_MENACE;
        return Math.min(score, 100);
    }

    private static String verdict(int score) {
        if (score >= SEUIL_ELEVEE) return "ELEVEE";
        if (score >= SEUIL_MOYENNE) return "MOYENNE";
        return "FAIBLE";
    }

    private static void validateInputs(LocalDate dateEntreeBelgique,
                                       int dureePresenceMois,
                                       LocalDate dateDepotDemande,
                                       Clock clock) {
        if (dateEntreeBelgique == null) {
            throw new IllegalArgumentException("dateEntreeBelgique est requise");
        }
        LocalDate today = LocalDate.now(clock);
        if (dateEntreeBelgique.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateEntreeBelgique ne peut pas être dans le futur");
        }
        if (dureePresenceMois < 0) {
            throw new IllegalArgumentException(
                    "dureePresenceMois doit être positif ou nul");
        }
        if (dateDepotDemande != null && dateDepotDemande.isBefore(dateEntreeBelgique)) {
            throw new IllegalArgumentException(
                    "dateDepotDemande ne peut pas être antérieure à dateEntreeBelgique");
        }
    }

    private static List<String> buildCriteresNonRemplis(boolean presence3AnsOk,
                                                        boolean liensConstitutifsOk,
                                                        boolean pasMenace,
                                                        boolean circonstancesExceptionnelles) {
        List<String> criteres = new ArrayList<>();
        if (!presence3AnsOk) {
            criteres.add("Présence en Belgique inférieure à 3 ans (signal faible)");
        }
        if (!liensConstitutifsOk) {
            criteres.add("Aucun lien constitutif (ni famille BE, ni travail, ni scolarité d'enfants)");
        }
        if (!pasMenace) {
            criteres.add("Menace pour l'ordre public présumée");
        }
        if (!circonstancesExceptionnelles) {
            criteres.add("Circonstances exceptionnelles non documentées (condition de recevabilité 9bis)");
        }
        return criteres;
    }

    private static List<String> buildMessages(String verdict,
                                              boolean presence3AnsOk,
                                              boolean liensConstitutifsOk,
                                              boolean liensFamiliauxBe,
                                              boolean liensProfessionnels,
                                              boolean scolariteEnfantsBe,
                                              boolean circonstancesExceptionnelles) {
        List<String> messages = new ArrayList<>();
        messages.add("Pouvoir d'appréciation discrétionnaire de l'Office des "
                + "étrangers (OE) : faisceau d'indices, aucun droit automatique.");
        messages.add("La demande 9bis doit être introduite à la commune du lieu de "
                + "résidence et démontrer des circonstances exceptionnelles "
                + "empêchant le retour au pays d'origine pour y solliciter un visa.");
        messages.add("Preuves à constituer : attestation de résidence, bail, "
                + "factures, compositions de ménage, attestations scolaires, "
                + "contrat de travail ou promesse, certificats médicaux éventuels, "
                + "attestations d'intégration (associations, cours de français/néerlandais).");
        messages.add("Délai d'instruction OE : variable 3 à 24 mois, moyenne ~18 mois.");

        if (!circonstancesExceptionnelles) {
            messages.add("Attention : sans circonstances exceptionnelles "
                    + "documentées, la demande est généralement déclarée "
                    + "irrecevable (art. 9bis § 1 Loi 15/12/1980).");
        }

        if (!presence3AnsOk) {
            messages.add("Présence < 3 ans : signal d'enracinement faible — "
                    + "renforcer impérativement les autres éléments.");
        }

        if (!liensConstitutifsOk) {
            messages.add("Sans lien constitutif (famille BE, travail ou "
                    + "scolarité), privilégier une autre voie (regroupement "
                    + "familial art. 10-11, protection internationale, ou 9ter "
                    + "si motif médical).");
        }

        if (liensFamiliauxBe && !liensProfessionnels && !scolariteEnfantsBe) {
            messages.add("Lien familial constitutif : vérifier aussi si un "
                    + "regroupement familial art. 10 / 10bis / 40bis est plus "
                    + "adapté (droit plutôt qu'appréciation discrétionnaire).");
        }

        messages.add("Alternative à évaluer en parallèle : art. 9ter "
                + "(régularisation médicale — maladie grave sans traitement "
                + "accessible au pays d'origine).");

        if ("ELEVEE".equals(verdict)) {
            messages.add("Probabilité élevée : en cas d'acceptation, délivrance "
                    + "d'un titre de séjour (CIRE) temporaire, renouvelable puis "
                    + "permanent.");
        }
        return messages;
    }

    private static String buildFormule(String verdict,
                                       int scoreGlobal,
                                       boolean presence3AnsOk,
                                       boolean liensConstitutifsOk,
                                       boolean pasMenace,
                                       LocalDate dateExpirationInstruction) {
        StringBuilder sb = new StringBuilder();
        sb.append("Régularisation 9bis humanitaire BE (art. 9bis Loi 15/12/1980) : probabilité ")
                .append(verdict)
                .append(" (score ").append(scoreGlobal).append("/100)");

        List<String> parts = new ArrayList<>();
        parts.add("présence " + (presence3AnsOk ? "≥ 3 ans" : "< 3 ans"));
        parts.add("liens " + (liensConstitutifsOk ? "OK" : "KO"));
        parts.add("ordre public " + (pasMenace ? "OK" : "KO"));
        sb.append(" — ").append(String.join(", ", parts)).append(".");

        if (dateExpirationInstruction != null) {
            sb.append(" Instruction OE prévisionnelle jusqu'au ~")
                    .append(dateExpirationInstruction)
                    .append(" (moyenne 18 mois, variable).");
        }
        return sb.toString();
    }
}
