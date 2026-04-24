package fr.ailegalcase.casefile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-IM-09-04 : calculateur pour l'Admission Exceptionnelle au Séjour (AES) au titre de
 * la voie étudiante — régularisation d'un étranger présent en France et poursuivant des
 * études sans titre adapté. Basé sur la circulaire Valls du 28/11/2012 (point III)
 * actualisée par la circulaire Darmanin 2024 et l'interprétation jurisprudentielle
 * constante (art. L.412-1 CESEDA).
 *
 * <p>Distinct de VLS_TS_ETUDIANT (étudiant classique primo-arrivant — F-IM-05/07),
 * de l'AES métiers en tension (F-IM-09-01), de l'AES famille L.435-1 (F-IM-09-02) et
 * de l'AES humanitaire L.435-2 (F-IM-09-03).
 *
 * <p>Conditions appréciées en faisceau d'indices (la circulaire fixe des seuils
 * indicatifs et non cumulatifs) :
 * <ul>
 *   <li>Présence ≥ 3 ans (seuil indicatif)</li>
 *   <li>Scolarité suivie avec assiduité en France ≥ 2 années consécutives</li>
 *   <li>Résultats académiques décents (EXCELLENT ou MOYEN_PASSABLE)</li>
 *   <li>Inscription validée dans un établissement supérieur reconnu</li>
 *   <li>Moyens de subsistance ou logement stables</li>
 *   <li>Pas de menace pour l'ordre public</li>
 *   <li>Parcours cohérent (pas de réorientations répétées suspectes)</li>
 * </ul>
 *
 * <p>Délai d'instruction indicatif : 6 mois à compter du dépôt.
 *
 * <p>Outil <b>single-country FR</b> — pas d'équivalent standard en droit belge.
 */
public final class AesEtudiantCalculator {

    /** Durée minimale indicative de présence en France (circulaire Valls 28/11/2012). */
    public static final int PRESENCE_MINIMALE_ANNEES = 3;

    /** Nombre minimum d'années de scolarité consécutives en France. */
    public static final int SCOLARITE_MINIMALE_ANNEES = 2;

    /** Délai d'instruction indicatif (mois). */
    public static final int DELAI_INSTRUCTION_MOIS = 6;

    /** Pondération d'un critère majeur (7 critères × ~14 pts ≈ 100). */
    public static final int POIDS_CRITERE_MAJEUR = 15;
    public static final int POIDS_CRITERE_ORDRE_PUBLIC = 10;

    /** Seuils de verdict. */
    public static final int SCORE_ELEVE = 80;
    public static final int SCORE_MOYEN = 50;

    public enum NiveauEtudes {
        LYCEE, BAC_PLUS_1_2, BAC_PLUS_3_4, BAC_PLUS_5_PLUS
    }

    public enum ResultatsAcademiques {
        EXCELLENT, MOYEN_PASSABLE, DIFFICULTES_REPETEES
    }

    public static final Set<String> NIVEAUX_VALIDES = Set.of(
            "LYCEE", "BAC_PLUS_1_2", "BAC_PLUS_3_4", "BAC_PLUS_5_PLUS");

    public static final Set<String> RESULTATS_VALIDES = Set.of(
            "EXCELLENT", "MOYEN_PASSABLE", "DIFFICULTES_REPETEES");

    private static final String BASE_JURIDIQUE =
            "Circulaire Valls 28/11/2012 (actualisée Darmanin) — L.412-1 CESEDA";

    private AesEtudiantCalculator() {
    }

    public static AesEtudiantResult compute(LocalDate dateEntreeFrance,
                                            int dureePresenceMois,
                                            int anneesScolariteEnFranceConsecutives,
                                            String niveauEtudesActuel,
                                            String resultatsAcademiques,
                                            boolean inscriptionEtablissementReconnu,
                                            boolean moyensSubsistance,
                                            boolean menaceOrdrePublic,
                                            boolean parcoursCoherent,
                                            LocalDate dateDepotDemande) {
        return compute(dateEntreeFrance, dureePresenceMois, anneesScolariteEnFranceConsecutives,
                niveauEtudesActuel, resultatsAcademiques, inscriptionEtablissementReconnu,
                moyensSubsistance, menaceOrdrePublic, parcoursCoherent, dateDepotDemande,
                Clock.system(ZoneId.of("Europe/Paris")));
    }

    public static AesEtudiantResult compute(LocalDate dateEntreeFrance,
                                            int dureePresenceMois,
                                            int anneesScolariteEnFranceConsecutives,
                                            String niveauEtudesActuel,
                                            String resultatsAcademiques,
                                            boolean inscriptionEtablissementReconnu,
                                            boolean moyensSubsistance,
                                            boolean menaceOrdrePublic,
                                            boolean parcoursCoherent,
                                            LocalDate dateDepotDemande,
                                            Clock clock) {
        validateInputs(dateEntreeFrance, dureePresenceMois,
                anneesScolariteEnFranceConsecutives,
                niveauEtudesActuel, resultatsAcademiques,
                dateDepotDemande, clock);

        LocalDate today = LocalDate.now(clock);

        boolean presence3AnsOk = !dateEntreeFrance.isAfter(today.minusYears(PRESENCE_MINIMALE_ANNEES))
                || dureePresenceMois >= PRESENCE_MINIMALE_ANNEES * 12;
        boolean scolarite2AnsConsecutivesOk =
                anneesScolariteEnFranceConsecutives >= SCOLARITE_MINIMALE_ANNEES;
        boolean resultatsAcceptables =
                "EXCELLENT".equals(resultatsAcademiques)
                        || "MOYEN_PASSABLE".equals(resultatsAcademiques);
        boolean inscriptionValide = inscriptionEtablissementReconnu;
        boolean moyensOk = moyensSubsistance;
        boolean pasMenace = !menaceOrdrePublic;
        boolean parcoursCoherentOk = parcoursCoherent;

        int scoreGlobal = computeScore(presence3AnsOk, scolarite2AnsConsecutivesOk,
                resultatsAcceptables, inscriptionValide, moyensOk, pasMenace,
                parcoursCoherentOk);

        String verdict = scoreGlobal >= SCORE_ELEVE
                ? "ELEVEE"
                : (scoreGlobal >= SCORE_MOYEN ? "MOYENNE" : "FAIBLE");

        List<String> criteresNonRemplis = buildCriteresNonRemplis(presence3AnsOk,
                scolarite2AnsConsecutivesOk, resultatsAcceptables, inscriptionValide,
                moyensOk, pasMenace, parcoursCoherentOk);

        LocalDate dateExpirationInstruction = dateDepotDemande != null
                ? dateDepotDemande.plusMonths(DELAI_INSTRUCTION_MOIS)
                : null;

        List<String> messages = buildMessages(niveauEtudesActuel, resultatsAcademiques,
                verdict, dateExpirationInstruction);

        String formule = buildFormule(scoreGlobal, verdict, criteresNonRemplis);

        return new AesEtudiantResult(
                dateEntreeFrance,
                dureePresenceMois,
                anneesScolariteEnFranceConsecutives,
                niveauEtudesActuel,
                resultatsAcademiques,
                inscriptionEtablissementReconnu,
                moyensSubsistance,
                menaceOrdrePublic,
                parcoursCoherent,
                dateDepotDemande,
                presence3AnsOk,
                scolarite2AnsConsecutivesOk,
                resultatsAcceptables,
                inscriptionValide,
                moyensOk,
                pasMenace,
                parcoursCoherentOk,
                scoreGlobal,
                verdict,
                criteresNonRemplis,
                dateExpirationInstruction,
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    private static int computeScore(boolean presence3AnsOk,
                                    boolean scolarite2AnsConsecutivesOk,
                                    boolean resultatsAcceptables,
                                    boolean inscriptionValide,
                                    boolean moyensOk,
                                    boolean pasMenace,
                                    boolean parcoursCoherentOk) {
        int score = 0;
        if (presence3AnsOk) score += POIDS_CRITERE_MAJEUR;
        if (scolarite2AnsConsecutivesOk) score += POIDS_CRITERE_MAJEUR;
        if (resultatsAcceptables) score += POIDS_CRITERE_MAJEUR;
        if (inscriptionValide) score += POIDS_CRITERE_MAJEUR;
        if (moyensOk) score += POIDS_CRITERE_MAJEUR;
        if (pasMenace) score += POIDS_CRITERE_ORDRE_PUBLIC;
        if (parcoursCoherentOk) score += POIDS_CRITERE_MAJEUR;
        return Math.min(score, 100);
    }

    private static void validateInputs(LocalDate dateEntreeFrance,
                                       int dureePresenceMois,
                                       int anneesScolariteEnFranceConsecutives,
                                       String niveauEtudesActuel,
                                       String resultatsAcademiques,
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
        if (dureePresenceMois < 0 || dureePresenceMois > 600) {
            throw new IllegalArgumentException(
                    "dureePresenceMois doit être compris entre 0 et 600");
        }
        if (anneesScolariteEnFranceConsecutives < 0
                || anneesScolariteEnFranceConsecutives > 20) {
            throw new IllegalArgumentException(
                    "anneesScolariteEnFranceConsecutives doit être compris entre 0 et 20");
        }
        if (niveauEtudesActuel == null || !NIVEAUX_VALIDES.contains(niveauEtudesActuel)) {
            throw new IllegalArgumentException(
                    "niveauEtudesActuel invalide — attendu LYCEE, BAC_PLUS_1_2, "
                            + "BAC_PLUS_3_4 ou BAC_PLUS_5_PLUS");
        }
        if (resultatsAcademiques == null
                || !RESULTATS_VALIDES.contains(resultatsAcademiques)) {
            throw new IllegalArgumentException(
                    "resultatsAcademiques invalide — attendu EXCELLENT, "
                            + "MOYEN_PASSABLE ou DIFFICULTES_REPETEES");
        }
        if (dateDepotDemande != null && dateDepotDemande.isBefore(dateEntreeFrance)) {
            throw new IllegalArgumentException(
                    "dateDepotDemande ne peut pas être antérieure à dateEntreeFrance");
        }
    }

    private static List<String> buildCriteresNonRemplis(boolean presence3AnsOk,
                                                        boolean scolarite2AnsConsecutivesOk,
                                                        boolean resultatsAcceptables,
                                                        boolean inscriptionValide,
                                                        boolean moyensOk,
                                                        boolean pasMenace,
                                                        boolean parcoursCoherentOk) {
        List<String> criteres = new ArrayList<>();
        if (!presence3AnsOk) {
            criteres.add("Moins de 3 ans de présence en France (seuil indicatif)");
        }
        if (!scolarite2AnsConsecutivesOk) {
            criteres.add("Moins de 2 années consécutives de scolarité en France");
        }
        if (!resultatsAcceptables) {
            criteres.add("Résultats académiques insuffisants (difficultés répétées)");
        }
        if (!inscriptionValide) {
            criteres.add("Pas d'inscription validée dans un établissement supérieur reconnu");
        }
        if (!moyensOk) {
            criteres.add("Pas de moyens de subsistance ou d'hébergement stables");
        }
        if (!pasMenace) {
            criteres.add("Menace ordre public présumée");
        }
        if (!parcoursCoherentOk) {
            criteres.add("Parcours scolaire incohérent (réorientations répétées)");
        }
        return criteres;
    }

    private static List<String> buildMessages(String niveauEtudesActuel,
                                              String resultatsAcademiques,
                                              String verdict,
                                              LocalDate dateExpirationInstruction) {
        List<String> messages = new ArrayList<>();
        messages.add("Régularisation au titre de la circulaire Valls du 28/11/2012 "
                + "(point III) — appréciation en faisceau d'indices, les seuils ne sont "
                + "pas cumulatifs stricts.");
        messages.add("Preuves à constituer : certificats de scolarité consécutifs, "
                + "bulletins scolaires, attestations d'assiduité, justificatifs "
                + "d'hébergement/ressources, inscription en cours.");

        if ("BAC_PLUS_3_4".equals(niveauEtudesActuel)
                || "BAC_PLUS_5_PLUS".equals(niveauEtudesActuel)) {
            messages.add("Niveau licence-master éligible étudiant standard — envisager "
                    + "changement de statut vers VLS-TS étudiant plutôt que AES.");
        }
        if ("DIFFICULTES_REPETEES".equals(resultatsAcademiques)) {
            messages.add("Risque rejet — conseiller réorientation formation qualifiante "
                    + "avant demande AES.");
        }
        if ("LYCEE".equals(niveauEtudesActuel)) {
            messages.add("Lycéen : attention aux conditions d'âge (>18 ans pour une "
                    + "demande AES) — envisager AES voie famille L.435-1 si parents "
                    + "présents en France.");
        }
        if ("FAIBLE".equals(verdict)) {
            messages.add("En cas de rejet : envisager AES humanitaire L.435-2 (F-IM-09-03) "
                    + "ou AES famille L.435-1 (F-IM-09-02) si critères pertinents.");
        }
        if (dateExpirationInstruction != null) {
            messages.add("Délai d'instruction indicatif : 6 mois — réponse attendue au "
                    + dateExpirationInstruction + ".");
        }
        return messages;
    }

    private static String buildFormule(int scoreGlobal, String verdict,
                                       List<String> criteresNonRemplis) {
        if (criteresNonRemplis.isEmpty()) {
            return String.format(
                    "AES voie étudiante (circulaire Valls 28/11/2012) : tous critères "
                            + "remplis, score %d/100, probabilité acceptation %s.",
                    scoreGlobal, verdict);
        }
        return String.format(
                "AES voie étudiante (circulaire Valls 28/11/2012) : score %d/100, "
                        + "probabilité acceptation %s — critères non remplis : %s.",
                scoreGlobal, verdict, String.join(", ", criteresNonRemplis));
    }
}
