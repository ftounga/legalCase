package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * SF-FA-25-01 : calculateur d'éligibilité d'une mesure de protection des
 * majeurs vulnérables — sauvegarde de justice (art. 433-441 Cciv) et
 * habilitation familiale (art. 494-1 et s. Cciv depuis 2016).
 *
 * <p>SF-FA-25-03 : extension explicite des régimes CURATELLE_SIMPLE
 * (art. 440 al. 1 Cciv — assistance pour actes patrimoniaux importants)
 * et CURATELLE_RENFORCEE (art. 472 Cciv — le curateur perçoit revenus +
 * paie dépenses parce que la personne ne peut plus gérer son budget
 * quotidien). Le critère pivot {@code incapaciteGestionQuotidienne}
 * distingue les deux.
 *
 * <p>L'outil :
 * <ul>
 *   <li>évalue un score 0-100 d'éligibilité (acceptabilité par le JCP)</li>
 *   <li>recommande le régime optimal entre les 6 régimes possibles</li>
 *   <li>donne un verdict (ELEVEE / MOYENNE / FAIBLE), le délai indicatif et
 *       la nécessité d'audition / expertise psy complémentaire</li>
 *   <li>SF-FA-25-03 : expose un drapeau {@code eligible} et la liste
 *       {@code criteresNonRemplis} pour le régime demandé</li>
 * </ul>
 *
 * <p>Scoring 0-100 :
 * <ul>
 *   <li>+30 certificat médical circonstancié (obligatoire art. 431)</li>
 *   <li>+25 altération des facultés mentales</li>
 *   <li>+20 consentement de la personne à protéger ET régime habilitation
 *       familiale demandé (consentement requis art. 494-1 al. 2)</li>
 *   <li>+15 demandeur familial proche (CONJOINT, ENFANT_MAJEUR, PARENT)</li>
 *   <li>+10 patrimoine significatif</li>
 * </ul>
 *
 * <p>Verdicts : ELEVEE (≥ 75), MOYENNE (50-74), FAIBLE (&lt; 50).
 *
 * <p>Régime optimal recommandé selon arbre de décision :
 * <ol>
 *   <li>urgence patrimoniale + altération mentale (provisoire suffit) →
 *       SAUVEGARDE_JUSTICE</li>
 *   <li>famille proche + consentement + altération mentale + non urgent →
 *       HABILITATION_FAMILIALE</li>
 *   <li>SF-FA-25-03 : altération + cert médical + incapacité gestion
 *       quotidienne sans consentement → CURATELLE_RENFORCEE</li>
 *   <li>gestion patrimoine + patrimoine significatif + pas de consentement →
 *       CURATELLE_RENFORCEE (compat SF-FA-25-01)</li>
 *   <li>SF-FA-25-03 : altération + cert médical + acte patrimonial important
 *       sans incapacité quotidienne → CURATELLE_SIMPLE</li>
 *   <li>altération mentale + isolement social + pas de consentement →
 *       TUTELLE</li>
 *   <li>sinon → fallback sur regimeProtectionDemande</li>
 * </ol>
 *
 * <p>Délai indicatif : 4 mois pour sauvegarde et habilitation, 8 mois pour
 * curatelle, tutelle et mandat de protection future.
 *
 * <p>Outil <b>single-country FR</b>. L'équivalent BE (administration
 * provisoire — art. 488 et s. Code civil BE) est une procédure distincte
 * (juge de paix), couverte par une SF jumelle au backlog.
 */
public final class MajeursProtegesCalculator {

    public static final int SEUIL_ELEVEE = 75;
    public static final int SEUIL_MOYENNE = 50;

    public static final int POIDS_CERTIFICAT_MEDICAL = 30;
    public static final int POIDS_ALTERATION_MENTALE = 25;
    public static final int POIDS_CONSENTEMENT_HABILITATION = 20;
    public static final int POIDS_FAMILLE_PROCHE = 15;
    public static final int POIDS_PATRIMOINE = 10;

    public static final int DELAI_COURT_MOIS = 4;
    public static final int DELAI_LONG_MOIS = 8;

    public static final Set<String> REGIMES = new LinkedHashSet<>(List.of(
            "SAUVEGARDE_JUSTICE",
            "HABILITATION_FAMILIALE",
            "CURATELLE_SIMPLE",
            "CURATELLE_RENFORCEE",
            "TUTELLE",
            "MANDAT_PROTECTION_FUTURE"));

    public static final Set<String> DEMANDEURS = new LinkedHashSet<>(List.of(
            "CONJOINT", "ENFANT_MAJEUR", "PARENT",
            "FRERE_SOEUR", "TIERS_PROCHE", "MINISTERE_PUBLIC"));

    public static final Set<String> ACTES = new LinkedHashSet<>(List.of(
            "GESTION_PATRIMOINE", "DECISIONS_LOGEMENT", "DECISIONS_SANTE",
            "DECISIONS_FAMILIALES", "ACTES_ETAT_CIVIL", "AUTRE"));

    public static final Set<String> FAMILLE_PROCHE = Set.of(
            "CONJOINT", "ENFANT_MAJEUR", "PARENT");

    public static final Set<String> REGIMES_LONGS = Set.of(
            "CURATELLE_SIMPLE", "CURATELLE_RENFORCEE",
            "TUTELLE", "MANDAT_PROTECTION_FUTURE");

    /** SF-FA-25-03 : actes patrimoniaux importants requérant curatelle. */
    public static final Set<String> ACTES_PATRIMONIAUX_IMPORTANTS = Set.of(
            "GESTION_PATRIMOINE", "DECISIONS_LOGEMENT");

    private static final String BASE_JURIDIQUE =
            "Art. 433-441 + 494-1 et s. Cciv";

    private MajeursProtegesCalculator() {
    }

    /**
     * Surcharge SF-FA-25-01 (backward compatible) : appelle la version
     * étendue avec {@code incapaciteGestionQuotidienne = false}.
     */
    public static MajeursProtegesResult compute(String regimeProtectionDemande,
                                                boolean altertationFacultesMentales,
                                                boolean altertationFacultesPhysiques,
                                                boolean certificatMedicalCirconstancie,
                                                LocalDate dateCertificatMedical,
                                                boolean consentementPersonneAProteger,
                                                String demandeurFamilial,
                                                List<String> actesEnvisages,
                                                boolean urgencePatrimoniale,
                                                boolean patrimoineSignificatif,
                                                boolean isolementSocial) {
        return compute(regimeProtectionDemande,
                altertationFacultesMentales,
                altertationFacultesPhysiques,
                certificatMedicalCirconstancie,
                dateCertificatMedical,
                consentementPersonneAProteger,
                demandeurFamilial,
                actesEnvisages,
                urgencePatrimoniale,
                patrimoineSignificatif,
                isolementSocial,
                false);
    }

    /**
     * SF-FA-25-03 : version étendue avec critère
     * {@code incapaciteGestionQuotidienne} (pivot art. 472 pour curatelle
     * renforcée).
     */
    public static MajeursProtegesResult compute(String regimeProtectionDemande,
                                                boolean altertationFacultesMentales,
                                                boolean altertationFacultesPhysiques,
                                                boolean certificatMedicalCirconstancie,
                                                LocalDate dateCertificatMedical,
                                                boolean consentementPersonneAProteger,
                                                String demandeurFamilial,
                                                List<String> actesEnvisages,
                                                boolean urgencePatrimoniale,
                                                boolean patrimoineSignificatif,
                                                boolean isolementSocial,
                                                boolean incapaciteGestionQuotidienne) {
        validateRegime(regimeProtectionDemande);
        validateDemandeur(demandeurFamilial);
        List<String> actes = validateActes(actesEnvisages);

        int score = computeScore(certificatMedicalCirconstancie,
                altertationFacultesMentales,
                consentementPersonneAProteger,
                regimeProtectionDemande,
                demandeurFamilial,
                patrimoineSignificatif);
        String verdict = verdict(score);

        String regimeOptimal = recommandeRegime(regimeProtectionDemande,
                altertationFacultesMentales,
                altertationFacultesPhysiques,
                certificatMedicalCirconstancie,
                consentementPersonneAProteger,
                demandeurFamilial,
                actes,
                urgencePatrimoniale,
                patrimoineSignificatif,
                isolementSocial,
                incapaciteGestionQuotidienne);

        int delai = REGIMES_LONGS.contains(regimeOptimal)
                ? DELAI_LONG_MOIS : DELAI_COURT_MOIS;

        // art. 432 Cciv : audition obligatoire de la personne, sauf certificat
        // médical attestant l'impossibilité d'auditionner (non géré dans cette SF).
        boolean auditionObligatoire = true;

        boolean expertisePsy = expertisePsyRecommandee(
                altertationFacultesMentales,
                altertationFacultesPhysiques,
                certificatMedicalCirconstancie,
                actes);

        // SF-FA-25-03 : analyse d'éligibilité du régime DEMANDÉ
        // (eligible + criteresNonRemplis sont calculés sur le régime que
        // l'avocat a saisi, pas sur le régime optimal recommandé).
        List<String> criteresNonRemplis = analyseCriteres(
                regimeProtectionDemande,
                altertationFacultesMentales,
                altertationFacultesPhysiques,
                certificatMedicalCirconstancie,
                consentementPersonneAProteger,
                demandeurFamilial,
                actes,
                incapaciteGestionQuotidienne);
        boolean eligible = criteresNonRemplis.isEmpty();

        String formule = buildFormule(score, verdict, regimeOptimal,
                regimeProtectionDemande, certificatMedicalCirconstancie,
                consentementPersonneAProteger, urgencePatrimoniale, delai,
                eligible);

        List<String> messages = buildMessages(verdict, regimeOptimal,
                regimeProtectionDemande, certificatMedicalCirconstancie,
                consentementPersonneAProteger, urgencePatrimoniale,
                altertationFacultesMentales, altertationFacultesPhysiques,
                demandeurFamilial, actes, patrimoineSignificatif,
                isolementSocial, auditionObligatoire, expertisePsy,
                incapaciteGestionQuotidienne, criteresNonRemplis);

        return new MajeursProtegesResult(
                regimeProtectionDemande,
                altertationFacultesMentales,
                altertationFacultesPhysiques,
                certificatMedicalCirconstancie,
                dateCertificatMedical,
                consentementPersonneAProteger,
                demandeurFamilial,
                actes,
                urgencePatrimoniale,
                patrimoineSignificatif,
                isolementSocial,
                incapaciteGestionQuotidienne,
                score,
                regimeOptimal,
                verdict,
                delai,
                auditionObligatoire,
                expertisePsy,
                eligible,
                criteresNonRemplis,
                BASE_JURIDIQUE,
                formule,
                messages);
    }

    // =========================================================================
    // Score & verdict
    // =========================================================================

    private static int computeScore(boolean certificat,
                                    boolean alterationMentale,
                                    boolean consentement,
                                    String regimeDemande,
                                    String demandeur,
                                    boolean patrimoine) {
        int s = 0;
        if (certificat) s += POIDS_CERTIFICAT_MEDICAL;
        if (alterationMentale) s += POIDS_ALTERATION_MENTALE;
        if (consentement && "HABILITATION_FAMILIALE".equals(regimeDemande)) {
            s += POIDS_CONSENTEMENT_HABILITATION;
        }
        if (FAMILLE_PROCHE.contains(demandeur)) s += POIDS_FAMILLE_PROCHE;
        if (patrimoine) s += POIDS_PATRIMOINE;
        return Math.max(0, Math.min(100, s));
    }

    private static String verdict(int score) {
        if (score >= SEUIL_ELEVEE) return "ELEVEE";
        if (score >= SEUIL_MOYENNE) return "MOYENNE";
        return "FAIBLE";
    }

    // =========================================================================
    // Régime optimal & expertise
    // =========================================================================

    private static String recommandeRegime(String regimeDemande,
                                           boolean alterationMentale,
                                           boolean alterationPhysique,
                                           boolean certificat,
                                           boolean consentement,
                                           String demandeur,
                                           List<String> actes,
                                           boolean urgence,
                                           boolean patrimoine,
                                           boolean isolement,
                                           boolean incapaciteQuotidienne) {
        boolean alterationToute = alterationMentale || alterationPhysique;
        boolean actePat = actes.stream().anyMatch(ACTES_PATRIMONIAUX_IMPORTANTS::contains);

        // Sauvegarde de justice : urgence patrimoniale + altération (provisoire)
        if (urgence && alterationToute) {
            return "SAUVEGARDE_JUSTICE";
        }
        // Habilitation familiale : famille proche + consentement + altération
        // mentale + non urgent (art. 494-1 al. 2 — consentement requis)
        if (consentement
                && FAMILLE_PROCHE.contains(demandeur)
                && alterationMentale
                && !urgence) {
            return "HABILITATION_FAMILIALE";
        }
        // SF-FA-25-03 : Curatelle renforcée pivot art. 472 — incapacité de
        // gestion du budget quotidien + altération + cert médical
        if (incapaciteQuotidienne
                && alterationToute
                && certificat
                && !consentement) {
            return "CURATELLE_RENFORCEE";
        }
        // Curatelle renforcée (compat SF-FA-25-01) : gestion de patrimoine
        // significatif sans consentement
        if (actes.contains("GESTION_PATRIMOINE")
                && patrimoine
                && !consentement) {
            return "CURATELLE_RENFORCEE";
        }
        // SF-FA-25-03 : Curatelle simple — altération + cert médical + acte
        // patrimonial important + pas d'incapacité quotidienne + pas de
        // consentement (sinon habilitation prioritaire au-dessus)
        if (alterationToute
                && certificat
                && actePat
                && !incapaciteQuotidienne
                && !consentement
                && !isolement) {
            return "CURATELLE_SIMPLE";
        }
        // Tutelle : altération mentale + isolement social sans consentement
        if (alterationMentale && isolement && !consentement) {
            return "TUTELLE";
        }
        // Fallback : régime demandé
        return regimeDemande;
    }

    private static boolean expertisePsyRecommandee(boolean alterationMentale,
                                                   boolean alterationPhysique,
                                                   boolean certificat,
                                                   List<String> actes) {
        // altération non claire = mentale ET physique simultanées
        boolean alterationNonClaire = alterationMentale && alterationPhysique;
        // acte irréversible = santé ou état civil
        boolean acteIrreversible = actes.contains("DECISIONS_SANTE")
                || actes.contains("ACTES_ETAT_CIVIL");
        // pas de certificat = besoin objectif
        boolean besoinObjectif = !certificat;
        return alterationNonClaire || acteIrreversible || besoinObjectif;
    }

    // =========================================================================
    // SF-FA-25-03 : Analyse des critères d'éligibilité par régime
    // =========================================================================

    /**
     * Liste les critères NON REMPLIS pour le régime demandé.
     * Si la liste est vide → {@code eligible = true}.
     *
     * <p>Les critères vérifiés dépendent du régime ; tous les régimes
     * partagent les exigences communes (cert médical, altération).
     */
    private static List<String> analyseCriteres(String regime,
                                                boolean alterationMentale,
                                                boolean alterationPhysique,
                                                boolean certificat,
                                                boolean consentement,
                                                String demandeur,
                                                List<String> actes,
                                                boolean incapaciteQuotidienne) {
        List<String> ko = new ArrayList<>();
        boolean alterationToute = alterationMentale || alterationPhysique;
        boolean actePat = actes.stream().anyMatch(ACTES_PATRIMONIAUX_IMPORTANTS::contains);

        // Critères communs : certificat (art. 431) + altération (art. 425)
        if (!certificat) {
            ko.add("Certificat médical circonstancié obligatoire (art. 431 Cciv)");
        }
        if (!alterationToute) {
            ko.add("Altération des facultés (mentales ou physiques) requise (art. 425 Cciv)");
        }

        switch (regime) {
            case "CURATELLE_SIMPLE" -> {
                if (!actePat) {
                    ko.add("Au moins un acte patrimonial important "
                            + "(GESTION_PATRIMOINE ou DECISIONS_LOGEMENT) "
                            + "requis pour la curatelle simple (art. 440 al. 1)");
                }
                // curatelle simple : la personne reste autonome au quotidien
                if (incapaciteQuotidienne) {
                    ko.add("Incapacité de gestion quotidienne incompatible avec "
                            + "la curatelle simple — basculer vers la curatelle "
                            + "renforcée (art. 472)");
                }
            }
            case "CURATELLE_RENFORCEE" -> {
                if (!actePat) {
                    ko.add("Au moins un acte patrimonial important "
                            + "(GESTION_PATRIMOINE ou DECISIONS_LOGEMENT) "
                            + "requis pour la curatelle (art. 440)");
                }
                if (!incapaciteQuotidienne) {
                    ko.add("Incapacité de gestion des revenus / dépenses "
                            + "quotidiens requise pour la curatelle "
                            + "renforcée (art. 472 Cciv)");
                }
            }
            case "HABILITATION_FAMILIALE" -> {
                if (!consentement) {
                    ko.add("Consentement de la personne à protéger requis "
                            + "(art. 494-1 al. 2 Cciv)");
                }
                if (!FAMILLE_PROCHE.contains(demandeur)) {
                    ko.add("Demandeur familial proche requis (CONJOINT, "
                            + "ENFANT_MAJEUR ou PARENT — art. 494-1 al. 1)");
                }
            }
            case "SAUVEGARDE_JUSTICE", "TUTELLE", "MANDAT_PROTECTION_FUTURE" -> {
                // pas de critères spécifiques additionnels analysés ici
                // (couverts par le scoring 0-100 pour SAUVEGARDE / TUTELLE,
                // hors-scope pour MANDAT_PROTECTION_FUTURE)
            }
            default -> { }
        }
        return ko;
    }

    // =========================================================================
    // Formule & messages
    // =========================================================================

    private static String buildFormule(int score,
                                       String verdict,
                                       String regimeOptimal,
                                       String regimeDemande,
                                       boolean certificat,
                                       boolean consentement,
                                       boolean urgence,
                                       int delai,
                                       boolean eligible) {
        StringBuilder sb = new StringBuilder();
        sb.append("Score ").append(score)
                .append(" = acceptabilité ").append(verdict)
                .append(" (").append(seuilLabel(verdict)).append("). ");
        if (regimeOptimal.equals(regimeDemande)) {
            sb.append("Régime recommandé : ").append(regimeOptimal);
        } else {
            sb.append("Régime demandé ").append(regimeDemande)
                    .append(" → recommandé ").append(regimeOptimal);
        }
        sb.append(" (délai ~").append(delai).append(" mois). ");
        sb.append(eligible
                ? "Éligibilité : OUI (tous critères du régime demandé remplis)."
                : "Éligibilité : NON (critères manquants — voir liste).");
        if (!certificat) {
            sb.append(" Certificat médical circonstancié manquant — obligatoire art. 431.");
        }
        if (urgence) {
            sb.append(" Urgence patrimoniale invoquée.");
        }
        if (consentement) {
            sb.append(" Personne consent.");
        }
        return sb.toString();
    }

    private static String seuilLabel(String verdict) {
        return switch (verdict) {
            case "ELEVEE" -> "≥ 75";
            case "MOYENNE" -> "50-74";
            default -> "< 50";
        };
    }

    private static List<String> buildMessages(String verdict,
                                              String regimeOptimal,
                                              String regimeDemande,
                                              boolean certificat,
                                              boolean consentement,
                                              boolean urgence,
                                              boolean alterationMentale,
                                              boolean alterationPhysique,
                                              String demandeur,
                                              List<String> actes,
                                              boolean patrimoine,
                                              boolean isolement,
                                              boolean auditionObligatoire,
                                              boolean expertisePsy,
                                              boolean incapaciteQuotidienne,
                                              List<String> criteresNonRemplis) {
        List<String> msgs = new ArrayList<>();
        msgs.add("Saisine du juge des contentieux de la protection (ex-JAF "
                + "tutelles) : requête écrite + certificat médical "
                + "circonstancié obligatoire (art. 431 Cciv) délivré par "
                + "médecin inscrit sur la liste du procureur.");

        if (!certificat) {
            msgs.add("Certificat médical circonstancié MANQUANT : la requête "
                    + "sera déclarée irrecevable. Obtenir un certificat "
                    + "auprès d'un médecin inscrit sur la liste du procureur "
                    + "(art. 431 Cciv).");
        }

        if (auditionObligatoire) {
            msgs.add("Audition de la personne à protéger obligatoire par le "
                    + "juge (art. 432 Cciv), sauf si le certificat médical "
                    + "atteste l'impossibilité ou l'inutilité de cette audition.");
        }

        switch (regimeOptimal) {
            case "SAUVEGARDE_JUSTICE" -> msgs.add(
                    "SAUVEGARDE DE JUSTICE (art. 433-441 Cciv) : mesure de "
                            + "protection IMMÉDIATE et TEMPORAIRE (1 an "
                            + "renouvelable une fois). La personne conserve "
                            + "l'exercice de ses droits, mais ses actes peuvent "
                            + "être annulés ou réduits a posteriori. Adaptée à "
                            + "l'urgence patrimoniale.");
            case "HABILITATION_FAMILIALE" -> msgs.add(
                    "HABILITATION FAMILIALE (art. 494-1 et s. Cciv depuis 2016) "
                            + ": un proche est habilité à représenter la "
                            + "personne. Subsidiaire à la curatelle/tutelle, "
                            + "elle suppose le consentement de la personne "
                            + "(ou son impossibilité d'exprimer sa volonté "
                            + "constatée par certificat) et l'absence de "
                            + "conflit familial. Procédure souple (4 mois).");
            case "CURATELLE_RENFORCEE" -> msgs.add(
                    "CURATELLE RENFORCÉE (art. 472 Cciv) : le curateur perçoit "
                            + "seul les revenus de la personne et règle les "
                            + "dépenses (loyer, charges, courses, soins) ; "
                            + "l'excédent est reversé à la personne. Adaptée "
                            + "lorsque la personne ne peut plus gérer son "
                            + "budget quotidien (critère pivot art. 472). "
                            + "Compte annuel de gestion à soumettre au juge "
                            + "(art. 510). Mesure plus lourde (~ 8 mois).");
            case "TUTELLE" -> msgs.add(
                    "TUTELLE (art. 440 al. 3 et 473 Cciv) : représentation "
                            + "continue de la personne dans tous les actes de "
                            + "la vie civile. Mesure la plus protectrice, "
                            + "réservée aux altérations sévères avec isolement "
                            + "social et absence de consentement.");
            case "CURATELLE_SIMPLE" -> msgs.add(
                    "CURATELLE SIMPLE (art. 440 al. 1 Cciv) : ASSISTANCE pour "
                            + "les actes de disposition (vente immobilière, "
                            + "emprunt, bail), la personne reste AUTONOME pour "
                            + "ses actes courants et la gestion de son budget "
                            + "quotidien. Double signature requise pour les "
                            + "actes patrimoniaux importants (art. 467). Si la "
                            + "gestion quotidienne devient impossible, "
                            + "basculer vers CURATELLE_RENFORCEE (art. 472).");
            case "MANDAT_PROTECTION_FUTURE" -> msgs.add(
                    "MANDAT DE PROTECTION FUTURE (art. 477-494 Cciv) : "
                            + "anticipé par la personne avant l'altération. "
                            + "Activable sur certificat médical sans "
                            + "intervention du juge (mandat sous seing privé) "
                            + "ou avec contrôle juge (mandat notarié).");
            default -> { }
        }

        if (!regimeOptimal.equals(regimeDemande)) {
            msgs.add("Le régime demandé (" + regimeDemande + ") diverge de la "
                    + "recommandation. Vérifier les conditions de subsidiarité "
                    + "(art. 428 Cciv : nécessité, subsidiarité, "
                    + "proportionnalité).");
        }

        if (consentement
                && "HABILITATION_FAMILIALE".equals(regimeOptimal)) {
            msgs.add("Consentement de la personne : pièce déterminante "
                    + "pour l'habilitation familiale (art. 494-1 al. 2). "
                    + "Recueillir formellement.");
        }

        if (alterationMentale && alterationPhysique) {
            msgs.add("Altération mixte (mentale + physique) : expertise "
                    + "psychiatrique complémentaire fortement recommandée pour "
                    + "objectiver le degré d'altération.");
        }

        if (expertisePsy) {
            msgs.add("Expertise psychiatrique complémentaire recommandée "
                    + "(altération non claire, acte irréversible envisagé, "
                    + "ou certificat manquant).");
        }

        if (FAMILLE_PROCHE.contains(demandeur)) {
            msgs.add("Demandeur familial proche : qualité à agir établie "
                    + "(art. 430 Cciv). Privilégier l'habilitation familiale "
                    + "si conditions réunies (subsidiarité art. 494-1).");
        } else if ("MINISTERE_PUBLIC".equals(demandeur)) {
            msgs.add("Saisine par le Ministère Public (art. 430 al. 2) : "
                    + "souvent en l'absence de famille mobilisable. La "
                    + "subsidiarité de l'habilitation familiale ne peut alors "
                    + "être satisfaite.");
        }

        if (urgence) {
            msgs.add("Urgence patrimoniale : la sauvegarde de justice peut "
                    + "être prononcée en 15 jours (art. 433 al. 2 Cciv) sur "
                    + "déclaration médicale au procureur.");
        }

        if (actes.contains("DECISIONS_SANTE")) {
            msgs.add("Décisions de santé : actes strictement personnels — "
                    + "consentement éclairé de la personne reste requis "
                    + "(art. 459 Cciv). L'expertise psychiatrique complémentaire "
                    + "est recommandée pour objectiver la capacité.");
        }

        if (actes.contains("ACTES_ETAT_CIVIL")) {
            msgs.add("Actes d'état civil (mariage, PACS, reconnaissance) : "
                    + "actes strictement personnels — autorisation spéciale du "
                    + "juge requise (art. 460 Cciv).");
        }

        if (patrimoine) {
            msgs.add("Patrimoine significatif : inventaire à dresser dans les "
                    + "3 mois de la mise en place (art. 503 Cciv) et compte "
                    + "annuel de gestion à soumettre au juge.");
        }

        if (isolement) {
            msgs.add("Isolement social : facteur aggravant — privilégier une "
                    + "mesure protectrice solide (tutelle ou curatelle "
                    + "renforcée) plutôt qu'une habilitation familiale.");
        }

        if (incapaciteQuotidienne) {
            msgs.add("Incapacité de gestion quotidienne : critère pivot de la "
                    + "curatelle renforcée (art. 472 Cciv) — le curateur "
                    + "perçoit revenus et règle dépenses courantes.");
        }

        if (!criteresNonRemplis.isEmpty()) {
            msgs.add("Critères NON REMPLIS pour le régime demandé ("
                    + regimeDemande + ") : "
                    + String.join(" ; ", criteresNonRemplis)
                    + ".");
        }

        if ("ELEVEE".equals(verdict)) {
            msgs.add("Probabilité ÉLEVÉE d'acceptation par le JCP : préparer "
                    + "la requête complète + certificat médical + pièces sur "
                    + "le consentement et la subsidiarité.");
        } else if ("FAIBLE".equals(verdict)) {
            msgs.add("Probabilité FAIBLE : consolider le dossier (certificat, "
                    + "qualité du demandeur, subsidiarité art. 428) avant "
                    + "saisine ou envisager une mesure préalable (sauvegarde).");
        }

        return msgs;
    }

    // =========================================================================
    // Validation
    // =========================================================================

    private static void validateRegime(String regime) {
        if (regime == null || regime.isBlank()) {
            throw new IllegalArgumentException(
                    "regimeProtectionDemande est requis");
        }
        if (!REGIMES.contains(regime)) {
            throw new IllegalArgumentException(
                    "regimeProtectionDemande invalide : " + regime
                            + ". Valeurs : " + REGIMES);
        }
    }

    private static void validateDemandeur(String demandeur) {
        if (demandeur == null || demandeur.isBlank()) {
            throw new IllegalArgumentException(
                    "demandeurFamilial est requis");
        }
        if (!DEMANDEURS.contains(demandeur)) {
            throw new IllegalArgumentException(
                    "demandeurFamilial invalide : " + demandeur
                            + ". Valeurs : " + DEMANDEURS);
        }
    }

    private static List<String> validateActes(List<String> actes) {
        if (actes == null || actes.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> dedup = new LinkedHashSet<>();
        for (String a : actes) {
            if (a == null || a.isBlank()) continue;
            String t = a.trim();
            if (!ACTES.contains(t)) {
                throw new IllegalArgumentException(
                        "actesEnvisages contient une valeur invalide : " + t
                                + ". Valeurs : " + ACTES);
            }
            dedup.add(t);
        }
        return new ArrayList<>(dedup);
    }
}
