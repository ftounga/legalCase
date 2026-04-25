package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * SF-FA-26-01 : calculateur de changement d'état civil — nom (art. 61-3-1
 * Cciv loi 2022), prénom (art. 60 Cciv loi 2016 simplifiée mairie), sexe
 * (art. 61-5 Cciv loi 2016).
 *
 * <p>Premier morceau backend de F-FA-26. L'outil :
 * <ul>
 *   <li>détermine la <b>compétence procédurale</b> (MAIRIE / JUGE /
 *       TRIBUNAL_JUDICIAIRE) selon le type de changement et le motif</li>
 *   <li>évalue un <b>score d'acceptabilité 0-100</b> sur la base du
 *       motif, des preuves, de la majorité du demandeur, de la concordance
 *       documentaire et de l'historique de changements</li>
 *   <li>donne un verdict (ELEVEE / MOYENNE / FAIBLE), le délai indicatif
 *       d'instruction et la liste des documents canoniques manquants</li>
 * </ul>
 *
 * <p>Scoring 0-100 :
 * <ul>
 *   <li>+30 motif reconnu (INTERET_LEGITIME, MARIAGE, RECTIFICATION_ERREUR,
 *       IDENTIFICATION_GENRE)</li>
 *   <li>+25 ≥ 2 preuves variées</li>
 *   <li>+15 majeur (ou mineur avec consentement parental)</li>
 *   <li>+15 pas de changement antérieur</li>
 *   <li>+15 dates des documents concordants</li>
 * </ul>
 *
 * <p>Verdicts : ELEVEE (≥ 70), MOYENNE (40-69), FAIBLE (&lt; 40).
 *
 * <p>Compétence selon arbre :
 * <ol>
 *   <li>PRENOM → MAIRIE (art. 60 Cciv depuis loi 2016)</li>
 *   <li>NOM + INTERET_LEGITIME + JUSTIFICATIF_USAGE_30ANS → MAIRIE
 *       (art. 61-3-1 Cciv loi 2022)</li>
 *   <li>NOM autres motifs → TRIBUNAL_JUDICIAIRE (art. 61 Cciv classique)</li>
 *   <li>SEXE → JUGE (TJ état des personnes, art. 61-5 Cciv)</li>
 *   <li>NOM_ET_PRENOM : MAIRIE si la partie NOM relèverait MAIRIE,
 *       sinon TRIBUNAL_JUDICIAIRE</li>
 * </ol>
 *
 * <p>Délais : 2 mois (MAIRIE), 3 mois (JUGE sexe), 6 mois (TJ nom classique).
 *
 * <p>Outil <b>single-country FR</b>. L'équivalent BE (changement de nom :
 * art. 370 Code judiciaire BE / changement prénom : loi 2017 BE) est une
 * procédure distincte couverte par une SF jumelle au backlog.
 */
public final class ChangementEtatCivilCalculator {

    public static final int SEUIL_ELEVEE = 70;
    public static final int SEUIL_MOYENNE = 40;

    public static final int POIDS_MOTIF_RECONNU = 30;
    public static final int POIDS_PREUVES_VARIEES = 25;
    public static final int POIDS_MAJEUR_OU_CONSENTEMENT = 15;
    public static final int POIDS_PAS_CHANGE_AUPARAVANT = 15;
    public static final int POIDS_DATES_CONCORDANTS = 15;

    public static final int SEUIL_PREUVES_VARIEES = 2;

    public static final int DELAI_MAIRIE_MOIS = 2;
    public static final int DELAI_JUGE_SEXE_MOIS = 3;
    public static final int DELAI_TJ_NOM_MOIS = 6;

    public static final Set<String> TYPES = new LinkedHashSet<>(List.of(
            "NOM", "PRENOM", "SEXE", "NOM_ET_PRENOM"));

    public static final Set<String> MOTIFS = new LinkedHashSet<>(List.of(
            "INTERET_LEGITIME",
            "MARIAGE",
            "RECTIFICATION_ERREUR",
            "IDENTIFICATION_GENRE",
            "AUTRE"));

    public static final Set<String> PREUVES = new LinkedHashSet<>(List.of(
            "JUSTIFICATIF_USAGE_30ANS",
            "LIVRET_FAMILLE",
            "CERTIFICAT_NAISSANCE",
            "ACTES_CIVILS",
            "TEMOIGNAGES",
            "EXPERTISE_MEDICALE",
            "AUTRE"));

    public static final Set<String> MOTIFS_RECONNUS = Set.of(
            "INTERET_LEGITIME",
            "MARIAGE",
            "RECTIFICATION_ERREUR",
            "IDENTIFICATION_GENRE");

    static final String BASE_JURIDIQUE =
            "Art. 60 (prénom) + 61-3-1 (nom) + 61-5 (sexe) Cciv";

    private ChangementEtatCivilCalculator() {
    }

    public static ChangementEtatCivilResult compute(String typeChangement,
                                                    String motifInvoque,
                                                    List<String> preuvesProduites,
                                                    boolean majeurDemandeur,
                                                    boolean consentementParental,
                                                    boolean datesDocsConcordants,
                                                    boolean dejaChangeAuparavant,
                                                    java.time.LocalDate dateNaissanceDemandeur,
                                                    String departementDeclaration) {
        validateType(typeChangement);
        validateMotif(motifInvoque);
        List<String> preuves = validatePreuves(preuvesProduites);

        String competence = computeCompetence(typeChangement, motifInvoque, preuves);
        int delai = computeDelai(competence, typeChangement);

        int score = computeScore(motifInvoque, preuves, majeurDemandeur,
                consentementParental, datesDocsConcordants, dejaChangeAuparavant);
        String verdict = computeVerdict(score);

        List<String> documentsManquants = computeDocumentsManquants(
                typeChangement, motifInvoque, preuves);

        String formule = buildFormule(typeChangement, motifInvoque,
                competence, delai, score, verdict, preuves);

        List<String> messages = buildMessages(typeChangement, motifInvoque,
                competence, delai, verdict, score, preuves, majeurDemandeur,
                consentementParental, datesDocsConcordants, dejaChangeAuparavant,
                documentsManquants);

        return new ChangementEtatCivilResult(
                typeChangement,
                motifInvoque,
                preuves,
                majeurDemandeur,
                consentementParental,
                datesDocsConcordants,
                dejaChangeAuparavant,
                dateNaissanceDemandeur,
                departementDeclaration,
                competence,
                delai,
                score,
                verdict,
                documentsManquants,
                BASE_JURIDIQUE,
                formule,
                messages);
    }

    // =========================================================================
    // Compétence procédurale
    // =========================================================================

    private static String computeCompetence(String type, String motif,
                                            List<String> preuves) {
        return switch (type) {
            case "PRENOM" -> "MAIRIE";
            case "NOM" -> nomCompetence(motif, preuves);
            case "SEXE" -> "JUGE";
            case "NOM_ET_PRENOM" -> {
                String nomCompet = nomCompetence(motif, preuves);
                yield "MAIRIE".equals(nomCompet) ? "MAIRIE" : "TRIBUNAL_JUDICIAIRE";
            }
            default -> throw new IllegalStateException("type inattendu: " + type);
        };
    }

    private static String nomCompetence(String motif, List<String> preuves) {
        if ("INTERET_LEGITIME".equals(motif)
                && preuves.contains("JUSTIFICATIF_USAGE_30ANS")) {
            return "MAIRIE";
        }
        return "TRIBUNAL_JUDICIAIRE";
    }

    private static int computeDelai(String competence, String type) {
        return switch (competence) {
            case "MAIRIE" -> DELAI_MAIRIE_MOIS;
            case "JUGE" -> DELAI_JUGE_SEXE_MOIS;
            case "TRIBUNAL_JUDICIAIRE" -> DELAI_TJ_NOM_MOIS;
            default -> DELAI_TJ_NOM_MOIS;
        };
    }

    // =========================================================================
    // Score & verdict
    // =========================================================================

    private static int computeScore(String motif,
                                    List<String> preuves,
                                    boolean majeur,
                                    boolean consentementParental,
                                    boolean datesConcordants,
                                    boolean dejaChange) {
        int s = 0;
        if (MOTIFS_RECONNUS.contains(motif)) {
            s += POIDS_MOTIF_RECONNU;
        }
        if (preuves.size() >= SEUIL_PREUVES_VARIEES) {
            s += POIDS_PREUVES_VARIEES;
        }
        // Bonus majorité : si majeur OU mineur avec consentement parental
        if (majeur || consentementParental) {
            s += POIDS_MAJEUR_OU_CONSENTEMENT;
        }
        if (!dejaChange) {
            s += POIDS_PAS_CHANGE_AUPARAVANT;
        }
        if (datesConcordants) {
            s += POIDS_DATES_CONCORDANTS;
        }
        return Math.max(0, Math.min(100, s));
    }

    private static String computeVerdict(int score) {
        if (score >= SEUIL_ELEVEE) return "ELEVEE";
        if (score >= SEUIL_MOYENNE) return "MOYENNE";
        return "FAIBLE";
    }

    // =========================================================================
    // Documents requis manquants
    // =========================================================================

    private static List<String> computeDocumentsManquants(String type,
                                                          String motif,
                                                          List<String> preuves) {
        List<String> manquants = new ArrayList<>();

        switch (type) {
            case "PRENOM" -> {
                if (!preuves.contains("CERTIFICAT_NAISSANCE")) {
                    manquants.add("CERTIFICAT_NAISSANCE");
                }
                if (!preuves.contains("LIVRET_FAMILLE")) {
                    manquants.add("LIVRET_FAMILLE");
                }
            }
            case "NOM" -> {
                if (!preuves.contains("CERTIFICAT_NAISSANCE")) {
                    manquants.add("CERTIFICAT_NAISSANCE");
                }
                if ("INTERET_LEGITIME".equals(motif)
                        && !preuves.contains("JUSTIFICATIF_USAGE_30ANS")) {
                    manquants.add("JUSTIFICATIF_USAGE_30ANS");
                }
                if (!preuves.contains("ACTES_CIVILS")) {
                    manquants.add("ACTES_CIVILS");
                }
            }
            case "SEXE" -> {
                if (!preuves.contains("CERTIFICAT_NAISSANCE")) {
                    manquants.add("CERTIFICAT_NAISSANCE");
                }
                if (!preuves.contains("TEMOIGNAGES")
                        && !preuves.contains("EXPERTISE_MEDICALE")) {
                    manquants.add("TEMOIGNAGES");
                }
            }
            case "NOM_ET_PRENOM" -> {
                if (!preuves.contains("CERTIFICAT_NAISSANCE")) {
                    manquants.add("CERTIFICAT_NAISSANCE");
                }
                if (!preuves.contains("LIVRET_FAMILLE")) {
                    manquants.add("LIVRET_FAMILLE");
                }
                if ("INTERET_LEGITIME".equals(motif)
                        && !preuves.contains("JUSTIFICATIF_USAGE_30ANS")) {
                    manquants.add("JUSTIFICATIF_USAGE_30ANS");
                }
            }
            default -> { }
        }
        return manquants;
    }

    // =========================================================================
    // Formule & messages
    // =========================================================================

    private static String buildFormule(String type,
                                       String motif,
                                       String competence,
                                       int delai,
                                       int score,
                                       String verdict,
                                       List<String> preuves) {
        StringBuilder sb = new StringBuilder();
        sb.append("Type ").append(type)
                .append(" + ").append(humanMotif(motif));
        if ("MAIRIE".equals(competence) && "NOM".equals(type)
                && preuves.contains("JUSTIFICATIF_USAGE_30ANS")) {
            sb.append(" + 30 ans usage");
        }
        sb.append(" = ").append(humanCompetence(competence)).append(" compétente");
        sb.append(", ").append(delai).append(" mois.");
        sb.append(" Score ").append(score).append(" → ").append(verdict).append(".");
        return sb.toString();
    }

    private static String humanCompetence(String competence) {
        return switch (competence) {
            case "MAIRIE" -> "mairie";
            case "JUGE" -> "juge (TJ état des personnes)";
            case "TRIBUNAL_JUDICIAIRE" -> "tribunal judiciaire";
            default -> competence;
        };
    }

    private static String humanMotif(String motif) {
        return switch (motif) {
            case "INTERET_LEGITIME" -> "intérêt légitime";
            case "MARIAGE" -> "mariage";
            case "RECTIFICATION_ERREUR" -> "rectification d'erreur";
            case "IDENTIFICATION_GENRE" -> "identification de genre";
            case "AUTRE" -> "autre motif";
            default -> motif;
        };
    }

    private static List<String> buildMessages(String type,
                                              String motif,
                                              String competence,
                                              int delai,
                                              String verdict,
                                              int score,
                                              List<String> preuves,
                                              boolean majeur,
                                              boolean consentementParental,
                                              boolean datesConcordants,
                                              boolean dejaChange,
                                              List<String> documentsManquants) {
        List<String> msgs = new ArrayList<>();

        switch (competence) {
            case "MAIRIE" -> msgs.add(
                    "Procédure mairie depuis loi 2016/2022 — recours TGI/TJ "
                            + "possible si refus de l'officier d'état civil "
                            + "(art. 60 al. 3 / 61-3-1 al. 4 Cciv).");
            case "JUGE" -> msgs.add(
                    "Procédure devant le juge du tribunal judiciaire statuant "
                            + "en matière d'état des personnes (art. 61-5 Cciv "
                            + "depuis loi 2016) — démédicalisée, sur "
                            + "déclaration et faisceau d'indices.");
            case "TRIBUNAL_JUDICIAIRE" -> msgs.add(
                    "Procédure de changement de nom devant le tribunal "
                            + "judiciaire (art. 61 Cciv) : décret au Journal "
                            + "Officiel après avis du Conseil d'État ou Garde "
                            + "des Sceaux selon motif.");
            default -> { }
        }

        switch (type) {
            case "PRENOM" -> msgs.add(
                    "Changement de prénom (art. 60 Cciv depuis loi 2016) : "
                            + "déclaration directement à l'officier d'état civil "
                            + "de la mairie du lieu de naissance ou de domicile, "
                            + "sans publication au JO. Délai indicatif " + delai
                            + " mois.");
            case "NOM" -> {
                if ("MAIRIE".equals(competence)) {
                    msgs.add("Changement de nom en mairie (art. 61-3-1 Cciv loi "
                            + "2022) : procédure simplifiée pour reprendre le "
                            + "nom de l'autre parent ou justifier un usage "
                            + "constant de 30 ans (acte de notoriété ou "
                            + "attestations).");
                } else {
                    msgs.add("Changement de nom par voie classique (art. 61 "
                            + "Cciv) : décret + insertion JO. Motifs admis : "
                            + "nom à consonance étrangère, ridicule, "
                            + "intérêt légitime familial. Délai instruction "
                            + delai + " mois.");
                }
            }
            case "SEXE" -> msgs.add(
                    "Changement de sexe à l'état civil (art. 61-5 et 61-6 Cciv "
                            + "loi 2016) : démédicalisé — preuve par tout "
                            + "moyen, notamment usage public d'un genre, nom "
                            + "social, attestations. Aucune intervention "
                            + "chirurgicale ni stérilisation requise.");
            case "NOM_ET_PRENOM" -> msgs.add(
                    "Changement combiné nom + prénom : la procédure de la "
                            + "partie la plus exigeante prime — si la partie "
                            + "nom relève du TJ, l'ensemble passe au TJ.");
            default -> { }
        }

        if (!documentsManquants.isEmpty()) {
            msgs.add("Documents canoniques manquants : "
                    + String.join(", ", documentsManquants)
                    + ". Compléter le dossier avant dépôt.");
        }

        if (!majeur && !consentementParental) {
            msgs.add("Demandeur mineur sans consentement parental : la "
                    + "demande sera irrecevable. Recueillir l'accord des "
                    + "deux parents (ou autorisation du JAF en cas de "
                    + "désaccord — art. 60 al. 2 Cciv).");
        } else if (!majeur) {
            msgs.add("Demandeur mineur : audition obligatoire si discernement "
                    + "(art. 60 al. 2 Cciv). Joindre consentement parental "
                    + "écrit.");
        }

        if (dejaChange) {
            msgs.add("Changement antérieur : motiver précisément la nouvelle "
                    + "demande (art. 61-2 Cciv — l'intérêt légitime doit être "
                    + "renforcé en cas de changement déjà opéré).");
        }

        if (!datesConcordants) {
            msgs.add("Dates des documents non concordantes : risque de refus "
                    + "ou de demande de pièces complémentaires. Régulariser "
                    + "avant dépôt.");
        }

        if (preuves.size() < SEUIL_PREUVES_VARIEES) {
            msgs.add("Preuves limitées : diversifier (acte de naissance, "
                    + "livret de famille, attestations, justificatifs "
                    + "d'usage) pour renforcer l'acceptabilité.");
        }

        if ("ELEVEE".equals(verdict)) {
            msgs.add("Acceptabilité ÉLEVÉE — préparer dossier final pour "
                    + "dépôt.");
        } else if ("FAIBLE".equals(verdict)) {
            msgs.add("Acceptabilité FAIBLE — consolider preuves et motif "
                    + "avant dépôt pour éviter un refus.");
        }

        return msgs;
    }

    // =========================================================================
    // Validation
    // =========================================================================

    private static void validateType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("typeChangement est requis");
        }
        if (!TYPES.contains(type)) {
            throw new IllegalArgumentException(
                    "typeChangement invalide : " + type
                            + ". Valeurs : " + TYPES);
        }
    }

    private static void validateMotif(String motif) {
        if (motif == null || motif.isBlank()) {
            throw new IllegalArgumentException("motifInvoque est requis");
        }
        if (!MOTIFS.contains(motif)) {
            throw new IllegalArgumentException(
                    "motifInvoque invalide : " + motif
                            + ". Valeurs : " + MOTIFS);
        }
    }

    private static List<String> validatePreuves(List<String> preuves) {
        if (preuves == null || preuves.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> dedup = new LinkedHashSet<>();
        for (String p : preuves) {
            if (p == null || p.isBlank()) continue;
            String t = p.trim();
            if (!PREUVES.contains(t)) {
                throw new IllegalArgumentException(
                        "preuvesProduites contient une valeur invalide : " + t
                                + ". Valeurs : " + PREUVES);
            }
            dedup.add(t);
        }
        return new ArrayList<>(dedup);
    }
}
