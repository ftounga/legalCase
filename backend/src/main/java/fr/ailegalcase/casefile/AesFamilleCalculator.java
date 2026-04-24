package fr.ailegalcase.casefile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-IM-09-02 : calculateur pour l'Admission Exceptionnelle au Séjour (AES) voie
 * familiale, art. <b>L.435-1 CESEDA</b> + circulaire Valls du 28/11/2012.
 *
 * <p>Appréciation souveraine du préfet selon un faisceau d'indices :
 * <ul>
 *   <li>Durée de présence en France (≥ 5 ans signal fort, ≥ 10 ans quasi-automatique)</li>
 *   <li>Liens personnels et familiaux (conjoint français ou régulier,
 *       enfants scolarisés en France)</li>
 *   <li>Insertion dans la société française (contrat, formation, logement)</li>
 *   <li>Absence de menace pour l'ordre public</li>
 * </ul>
 *
 * <p>Score pondéré 0-100 :
 * <ul>
 *   <li>+30 si présence ≥ 10 ans, sinon +20 si présence ≥ 5 ans</li>
 *   <li>+30 si liens familiaux OK + bonus +10 si durée scolarité aîné ≥ 5 ans</li>
 *   <li>+20 si preuves d'insertion</li>
 *   <li>+20 si pas de menace ordre public</li>
 * </ul>
 *
 * <p>Verdicts : ELEVEE (≥ 80), MOYENNE (50-79), FAIBLE (&lt; 50).
 *
 * <p>Délai d'instruction : 6 mois à compter du dépôt (silence vaut rejet).
 *
 * <p>Outil <b>single-country FR</b> — pas d'équivalent BE standard.
 */
public final class AesFamilleCalculator {

    /** Seuil de présence signal fort (art. L.435-1 jurisprudence CE). */
    public static final int PRESENCE_FORT_ANNEES = 5;

    /** Seuil de présence quasi-automatique (jurisprudence et circulaire Valls). */
    public static final int PRESENCE_TRES_FORT_ANNEES = 10;

    /** Seuil scolarité pour bonus (enfant installé durablement). */
    public static final int SCOLARITE_BONUS_ANNEES = 5;

    /** Délai d'instruction de la demande (mois). */
    public static final int DELAI_INSTRUCTION_MOIS = 6;

    /** Score minimum pour verdict ELEVEE. */
    public static final int SEUIL_ELEVEE = 80;

    /** Score minimum pour verdict MOYENNE. */
    public static final int SEUIL_MOYENNE = 50;

    private static final String BASE_JURIDIQUE =
            "Art. L.435-1 CESEDA + Circulaire Valls 28/11/2012";

    private AesFamilleCalculator() {
    }

    public static AesFamilleResult compute(LocalDate dateEntreeFrance,
                                           int dureePresenceMois,
                                           boolean conjointFrancaisOuRegulier,
                                           int enfantsScolarisesFrance,
                                           int dureeScolaritePlusAncienEnfantAnnees,
                                           boolean preuvesInsertion,
                                           boolean menaceOrdrePublic,
                                           LocalDate dateDepotDemande) {
        return compute(dateEntreeFrance, dureePresenceMois, conjointFrancaisOuRegulier,
                enfantsScolarisesFrance, dureeScolaritePlusAncienEnfantAnnees,
                preuvesInsertion, menaceOrdrePublic, dateDepotDemande,
                Clock.system(ZoneId.of("Europe/Paris")));
    }

    public static AesFamilleResult compute(LocalDate dateEntreeFrance,
                                           int dureePresenceMois,
                                           boolean conjointFrancaisOuRegulier,
                                           int enfantsScolarisesFrance,
                                           int dureeScolaritePlusAncienEnfantAnnees,
                                           boolean preuvesInsertion,
                                           boolean menaceOrdrePublic,
                                           LocalDate dateDepotDemande,
                                           Clock clock) {
        validateInputs(dateEntreeFrance, dureePresenceMois,
                enfantsScolarisesFrance, dureeScolaritePlusAncienEnfantAnnees,
                dateDepotDemande, clock);

        int anneesPresence = dureePresenceMois / 12;
        boolean presence5AnsOk = anneesPresence >= PRESENCE_FORT_ANNEES;
        boolean presence10AnsOk = anneesPresence >= PRESENCE_TRES_FORT_ANNEES;
        boolean liensFamiliauxOk = conjointFrancaisOuRegulier || enfantsScolarisesFrance >= 1;
        boolean insertionOk = preuvesInsertion;
        boolean pasMenace = !menaceOrdrePublic;

        int scoreGlobal = computeScore(presence5AnsOk, presence10AnsOk,
                liensFamiliauxOk, dureeScolaritePlusAncienEnfantAnnees,
                insertionOk, pasMenace);

        String verdict = verdict(scoreGlobal);

        LocalDate dateExpirationInstruction = dateDepotDemande != null
                ? dateDepotDemande.plusMonths(DELAI_INSTRUCTION_MOIS)
                : null;

        List<String> criteresNonRemplis = buildCriteresNonRemplis(
                presence5AnsOk, liensFamiliauxOk, insertionOk, pasMenace);

        List<String> messages = buildMessages(verdict, presence10AnsOk,
                liensFamiliauxOk, insertionOk);

        String formule = buildFormule(verdict, scoreGlobal,
                presence5AnsOk, presence10AnsOk, liensFamiliauxOk,
                insertionOk, pasMenace, dateExpirationInstruction);

        return new AesFamilleResult(
                dateEntreeFrance,
                dureePresenceMois,
                conjointFrancaisOuRegulier,
                enfantsScolarisesFrance,
                dureeScolaritePlusAncienEnfantAnnees,
                preuvesInsertion,
                menaceOrdrePublic,
                dateDepotDemande,
                presence5AnsOk,
                presence10AnsOk,
                liensFamiliauxOk,
                insertionOk,
                pasMenace,
                scoreGlobal,
                verdict,
                criteresNonRemplis,
                dateExpirationInstruction,
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    private static int computeScore(boolean presence5AnsOk,
                                    boolean presence10AnsOk,
                                    boolean liensFamiliauxOk,
                                    int dureeScolaritePlusAncienEnfantAnnees,
                                    boolean insertionOk,
                                    boolean pasMenace) {
        int score = 0;
        if (presence10AnsOk) {
            score += 30;
        } else if (presence5AnsOk) {
            score += 20;
        }
        if (liensFamiliauxOk) {
            score += 30;
            if (dureeScolaritePlusAncienEnfantAnnees >= SCOLARITE_BONUS_ANNEES) {
                score += 10;
            }
        }
        if (insertionOk) {
            score += 20;
        }
        if (pasMenace) {
            score += 20;
        }
        return Math.min(score, 100);
    }

    private static String verdict(int score) {
        if (score >= SEUIL_ELEVEE) return "ELEVEE";
        if (score >= SEUIL_MOYENNE) return "MOYENNE";
        return "FAIBLE";
    }

    private static void validateInputs(LocalDate dateEntreeFrance,
                                       int dureePresenceMois,
                                       int enfantsScolarisesFrance,
                                       int dureeScolaritePlusAncienEnfantAnnees,
                                       LocalDate dateDepotDemande,
                                       Clock clock) {
        if (dateEntreeFrance == null) {
            throw new IllegalArgumentException("dateEntreeFrance est requise");
        }
        LocalDate today = LocalDate.now(clock);
        if (dateEntreeFrance.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateEntreeFrance ne peut pas être dans le futur");
        }
        if (dureePresenceMois < 0) {
            throw new IllegalArgumentException(
                    "dureePresenceMois doit être positif ou nul");
        }
        if (enfantsScolarisesFrance < 0) {
            throw new IllegalArgumentException(
                    "enfantsScolarisesFrance doit être positif ou nul");
        }
        if (dureeScolaritePlusAncienEnfantAnnees < 0) {
            throw new IllegalArgumentException(
                    "dureeScolaritePlusAncienEnfantAnnees doit être positif ou nul");
        }
        if (dateDepotDemande != null && dateDepotDemande.isBefore(dateEntreeFrance)) {
            throw new IllegalArgumentException(
                    "dateDepotDemande ne peut pas être antérieure à dateEntreeFrance");
        }
    }

    private static List<String> buildCriteresNonRemplis(boolean presence5AnsOk,
                                                        boolean liensFamiliauxOk,
                                                        boolean insertionOk,
                                                        boolean pasMenace) {
        List<String> criteres = new ArrayList<>();
        if (!presence5AnsOk) {
            criteres.add("Présence en France inférieure à 5 ans (signal faible)");
        }
        if (!liensFamiliauxOk) {
            criteres.add("Pas de conjoint français/régulier ni d'enfant scolarisé");
        }
        if (!insertionOk) {
            criteres.add("Preuves d'insertion insuffisantes (contrat, formation, logement)");
        }
        if (!pasMenace) {
            criteres.add("Menace ordre public présumée");
        }
        return criteres;
    }

    private static List<String> buildMessages(String verdict,
                                              boolean presence10AnsOk,
                                              boolean liensFamiliauxOk,
                                              boolean insertionOk) {
        List<String> messages = new ArrayList<>();
        messages.add("Appréciation souveraine du préfet : faisceau d'indices, "
                + "pas de droit automatique même si tous critères OK.");
        messages.add("Preuves à constituer : justificatifs de domicile (quittances, "
                + "factures), attestations d'hébergement, bulletins scolaires, "
                + "actes de naissance enfants, contrat de travail/formation, "
                + "avis d'imposition, attestations d'intégration (associations, "
                + "formations linguistiques).");
        messages.add("Jurisprudence : CE, 21/11/2018, n° 411756 — la durée de "
                + "séjour est un élément essentiel mais pas suffisant, "
                + "l'appréciation doit être globale.");
        messages.add("Alternative possible : AES voie humanitaire L.435-2 "
                + "(considérations humanitaires ou motifs exceptionnels) — "
                + "outil F-IM-09-03.");

        if (presence10AnsOk) {
            messages.add("Présence ≥ 10 ans : signal quasi-automatique "
                    + "(circulaire Valls 28/11/2012 § 2.1.1).");
        }
        if (!liensFamiliauxOk) {
            messages.add("Sans liens familiaux, privilégier l'AES humanitaire "
                    + "(L.435-2) ou métiers en tension (L.435-4).");
        }
        if (!insertionOk) {
            messages.add("Renforcer les preuves d'insertion : contrat de travail "
                    + "ou promesse, formation professionnelle, maîtrise du français, "
                    + "bail de location stable.");
        }
        if ("ELEVEE".equals(verdict)) {
            messages.add("Probabilité d'acceptation élevée : titre délivré = "
                    + "carte de séjour \"vie privée et familiale\" 1 an renouvelable.");
        }
        return messages;
    }

    private static String buildFormule(String verdict,
                                       int scoreGlobal,
                                       boolean presence5AnsOk,
                                       boolean presence10AnsOk,
                                       boolean liensFamiliauxOk,
                                       boolean insertionOk,
                                       boolean pasMenace,
                                       LocalDate dateExpirationInstruction) {
        StringBuilder sb = new StringBuilder();
        sb.append("AES liens personnels et familiaux (L.435-1) : probabilité ")
                .append(verdict)
                .append(" (score ").append(scoreGlobal).append("/100)");

        List<String> parts = new ArrayList<>();
        if (presence10AnsOk) {
            parts.add("présence ≥ 10 ans");
        } else if (presence5AnsOk) {
            parts.add("présence ≥ 5 ans");
        } else {
            parts.add("présence < 5 ans");
        }
        parts.add("liens familiaux " + (liensFamiliauxOk ? "OK" : "KO"));
        parts.add("insertion " + (insertionOk ? "OK" : "KO"));
        parts.add("ordre public " + (pasMenace ? "OK" : "KO"));
        sb.append(" — ").append(String.join(", ", parts)).append(".");

        if (dateExpirationInstruction != null) {
            sb.append(" Délai d'instruction jusqu'au ")
                    .append(dateExpirationInstruction)
                    .append(" (silence vaut rejet).");
        }
        return sb.toString();
    }
}
