package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-FA-18-03 : calculateur de recevabilité d'une action en contestation de
 * paternité (FR — art. 332-335 + 311-1 + 321 Cciv).
 *
 * <p>L'action en contestation de paternité remet en cause un lien de filiation
 * paternelle déjà établi (par reconnaissance, par effet de la loi, ou par
 * possession d'état). 4 qualités à agir distinctes :</p>
 * <ul>
 *   <li><strong>PERE_DECLARE</strong> (art. 332-333) — le père légalement
 *       reconnu conteste sa propre paternité. Délai : 5 ans à compter de la
 *       connaissance de la non-filiation (333 al. 1).</li>
 *   <li><strong>PERE_BIOLOGIQUE_PRESUME</strong> (art. 333) — le père biologique
 *       présumé conteste la filiation établie au profit d'un autre. Délai : 5
 *       ans à compter de la connaissance.</li>
 *   <li><strong>MERE</strong> (art. 333) — la mère, le plus souvent pour
 *       permettre la reconnaissance par le père biologique. Délai : 5 ans à
 *       compter de la connaissance.</li>
 *   <li><strong>ENFANT_MAJEUR</strong> (art. 333 + 321) — l'enfant à sa
 *       majorité. Délai : 10 ans à compter de la majorité.</li>
 * </ul>
 *
 * <p>Fin de non-recevoir (art. 333 al. 2) : si la possession d'état conforme à
 * la filiation a duré 5 ans ou plus, l'action n'est plus recevable, sauf pour
 * l'enfant lui-même.</p>
 *
 * <p>Outil <strong>single-country FRANCE</strong>. La Belgique a un régime
 * distinct (CC art. 318, 330) qui sera traité par une feature jumelle au
 * backlog.</p>
 */
public final class ContestationPaterniteCalculator {

    /** Qualité à agir du contestant (art. 332-335 Cciv). */
    public enum QualiteAagir {
        /** Père légalement déclaré qui conteste sa propre paternité. */
        PERE_DECLARE,
        /** Père biologique présumé qui conteste la filiation déclarée d'un autre. */
        PERE_BIOLOGIQUE_PRESUME,
        /** Mère qui conteste la paternité du père déclaré. */
        MERE,
        /** Enfant majeur qui conteste sa filiation paternelle. */
        ENFANT_MAJEUR
    }

    /** Verdict de recevabilité de l'action en contestation. */
    public enum VerdictRecevabilite {
        ELEVEE,
        MOYENNE,
        FAIBLE
    }

    /** Délai de prescription pour les qualités hors enfant (art. 333 al. 1). */
    public static final int DELAI_PRESCRIPTION_ANS = 5;

    /** Délai de prescription pour l'enfant à compter de sa majorité (art. 321). */
    public static final int DELAI_PRESCRIPTION_ENFANT_ANS = 10;

    /** Durée de possession d'état déclenchant la fin de non-recevoir. */
    public static final int POSSESSION_ETAT_FIN_NON_RECEVOIR_ANS = 5;

    /** Base juridique consolidée. */
    private static final String BASE_JURIDIQUE =
            "Art. 332-335 + 311-1 + 321 + 372 Cciv";

    private ContestationPaterniteCalculator() {}

    /**
     * Évalue la recevabilité d'une action en contestation de paternité.
     *
     * @param qualiteAagir                 qualité à agir du contestant (obligatoire)
     * @param dateEtablissementFiliation   date à laquelle la filiation contestée a été établie
     * @param dateConnaissanceVerite       date à laquelle le contestant a su que la filiation
     *                                     ne correspondait pas à la vérité biologique
     * @param dateMajoriteEnfant           date de majorité de l'enfant (utilisée si qualité = ENFANT_MAJEUR)
     * @param possessionEtatConforme5Ans   la possession d'état conforme a-t-elle duré 5 ans ou plus ?
     * @param expertiseAdnDemandee         expertise ADN déjà demandée / envisagée ?
     * @param motifsSerieux                fondement sur des éléments précis et concordants ?
     * @param today                        date de référence pour les calculs de délai (utilisée pour les tests ; en prod = LocalDate.now())
     * @param country                      pays du workspace (FRANCE attendu)
     * @return résultat structuré
     * @throws IllegalArgumentException si paramètre invalide ou pays non supporté
     */
    public static ContestationPaterniteResult compute(QualiteAagir qualiteAagir,
                                                      LocalDate dateEtablissementFiliation,
                                                      LocalDate dateConnaissanceVerite,
                                                      LocalDate dateMajoriteEnfant,
                                                      Boolean possessionEtatConforme5Ans,
                                                      Boolean expertiseAdnDemandee,
                                                      Boolean motifsSerieux,
                                                      LocalDate today,
                                                      String country) {
        if (qualiteAagir == null) {
            throw new IllegalArgumentException("Qualité à agir requise");
        }
        if (dateEtablissementFiliation == null) {
            throw new IllegalArgumentException(
                    "Date d'établissement de la filiation contestée requise");
        }
        if (dateConnaissanceVerite == null) {
            throw new IllegalArgumentException(
                    "Date de connaissance de la non-filiation requise");
        }
        if (qualiteAagir == QualiteAagir.ENFANT_MAJEUR && dateMajoriteEnfant == null) {
            throw new IllegalArgumentException(
                    "Date de majorité de l'enfant requise pour une contestation par l'enfant majeur");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil non disponible pour le pays " + country
                            + " — l'équivalent BE (CC art. 318, 330 — délais, qualités à agir"
                            + " et régime ADN spécifiques) sera traité dans une feature jumelle"
                            + " dédiée au backlog.");
        }
        if (possessionEtatConforme5Ans == null) {
            possessionEtatConforme5Ans = false;
        }
        if (expertiseAdnDemandee == null) {
            expertiseAdnDemandee = false;
        }
        if (motifsSerieux == null) {
            motifsSerieux = false;
        }
        LocalDate referenceDate = (today != null) ? today : LocalDate.now();

        // Calcul du délai de prescription
        int delaiAns;
        LocalDate pointDepart;
        if (qualiteAagir == QualiteAagir.ENFANT_MAJEUR) {
            delaiAns = DELAI_PRESCRIPTION_ENFANT_ANS;
            pointDepart = dateMajoriteEnfant;
        } else {
            delaiAns = DELAI_PRESCRIPTION_ANS;
            pointDepart = dateConnaissanceVerite;
        }
        LocalDate dateForclusion = pointDepart.plusYears(delaiAns);
        long delaiPrescriptionRestantMois = ChronoUnit.MONTHS.between(referenceDate, dateForclusion);

        boolean prescrit = delaiPrescriptionRestantMois <= 0;

        // Fin de non-recevoir possession d'état (art. 333 al. 2) — sauf enfant
        boolean finNonRecevoirPossessionEtat = possessionEtatConforme5Ans
                && qualiteAagir != QualiteAagir.ENFANT_MAJEUR;

        // Calcul du score (0 à 100)
        int score = 0;
        if (!prescrit) {
            score += 30; // critère cardinal
            // Bonus si délai très peu consommé
            long delaiTotalMois = (long) delaiAns * 12;
            double pctRestant = delaiTotalMois > 0
                    ? (double) delaiPrescriptionRestantMois / delaiTotalMois : 0;
            if (pctRestant > 0.5) {
                score += 10;
            }
        }
        if (!finNonRecevoirPossessionEtat) {
            score += 20;
        }
        if (motifsSerieux) {
            score += 20;
        }
        if (expertiseAdnDemandee) {
            score += 20;
        }

        // Verdict
        VerdictRecevabilite verdict;
        if (prescrit) {
            verdict = VerdictRecevabilite.FAIBLE;
        } else if (finNonRecevoirPossessionEtat) {
            verdict = VerdictRecevabilite.FAIBLE;
        } else if (!motifsSerieux && !expertiseAdnDemandee) {
            verdict = VerdictRecevabilite.FAIBLE;
        } else if (score >= 80) {
            verdict = VerdictRecevabilite.ELEVEE;
        } else if (score >= 50) {
            verdict = VerdictRecevabilite.MOYENNE;
        } else {
            verdict = VerdictRecevabilite.FAIBLE;
        }

        // Recommandation expertise ADN
        boolean expertiseAdnRecommandee =
                !prescrit && !finNonRecevoirPossessionEtat
                        && (expertiseAdnDemandee || motifsSerieux);

        // Risques de refus
        List<String> risquesRefus = buildRisquesRefus(qualiteAagir, prescrit,
                finNonRecevoirPossessionEtat, motifsSerieux, expertiseAdnDemandee,
                possessionEtatConforme5Ans, delaiPrescriptionRestantMois);

        // Documents requis
        List<String> documents = buildDocuments(qualiteAagir, expertiseAdnDemandee);

        String formule = String.format(Locale.ROOT,
                "Qualité=%s + délai=%d ans (point départ=%s) + restant=%d mois + "
                        + "possession état 5 ans=%s + motifs sérieux=%s + ADN=%s "
                        + "→ score %d → verdict %s → ADN recommandée=%s, %d risque(s) de refus, "
                        + "%d document(s) requis",
                qualiteAagir.name(), delaiAns, pointDepart.toString(),
                delaiPrescriptionRestantMois, possessionEtatConforme5Ans,
                motifsSerieux, expertiseAdnDemandee,
                score, verdict.name(), expertiseAdnRecommandee,
                risquesRefus.size(), documents.size());

        List<String> messages = buildMessages(qualiteAagir, verdict, prescrit,
                finNonRecevoirPossessionEtat, motifsSerieux, expertiseAdnDemandee,
                expertiseAdnRecommandee, delaiAns, delaiPrescriptionRestantMois,
                possessionEtatConforme5Ans);

        return new ContestationPaterniteResult(
                qualiteAagir,
                dateEtablissementFiliation,
                dateConnaissanceVerite,
                dateMajoriteEnfant,
                possessionEtatConforme5Ans,
                expertiseAdnDemandee,
                motifsSerieux,
                countryNormalized,
                verdict,
                score,
                delaiAns,
                delaiPrescriptionRestantMois,
                expertiseAdnRecommandee,
                risquesRefus,
                documents,
                BASE_JURIDIQUE,
                formule,
                messages
        );
    }

    /** Surcharge sans paramètre {@code today} (utilise {@code LocalDate.now()}). */
    public static ContestationPaterniteResult compute(QualiteAagir qualiteAagir,
                                                      LocalDate dateEtablissementFiliation,
                                                      LocalDate dateConnaissanceVerite,
                                                      LocalDate dateMajoriteEnfant,
                                                      Boolean possessionEtatConforme5Ans,
                                                      Boolean expertiseAdnDemandee,
                                                      Boolean motifsSerieux,
                                                      String country) {
        return compute(qualiteAagir, dateEtablissementFiliation, dateConnaissanceVerite,
                dateMajoriteEnfant, possessionEtatConforme5Ans, expertiseAdnDemandee,
                motifsSerieux, null, country);
    }

    private static List<String> buildRisquesRefus(QualiteAagir qualite,
                                                  boolean prescrit,
                                                  boolean finNonRecevoir,
                                                  boolean motifsSerieux,
                                                  boolean expertiseAdn,
                                                  boolean possessionEtat5Ans,
                                                  long restantMois) {
        List<String> risques = new ArrayList<>();
        if (prescrit) {
            risques.add("Prescription acquise — l'action est forclose ("
                    + Math.abs(restantMois) + " mois après l'expiration du délai légal). "
                    + "Aucune voie de droit ouverte sauf cas exceptionnel (art. 2240 et s. Cciv).");
        } else if (restantMois < 12) {
            risques.add("Délai de prescription quasi écoulé (" + restantMois + " mois restants) "
                    + "— assignation à délivrer en urgence pour interrompre la prescription.");
        }
        if (finNonRecevoir) {
            risques.add("Fin de non-recevoir art. 333 al. 2 Cciv — la possession d'état conforme "
                    + "à la filiation pendant 5 ans ou plus rend l'action irrecevable de la part "
                    + "du contestant (autre que l'enfant lui-même).");
        }
        if (!motifsSerieux && !expertiseAdn) {
            risques.add("Absence de motifs sérieux et précis — la jurisprudence (Cass. 1ère civ. "
                    + "28 mars 2000) exige des éléments concordants (faits avérés, témoignages, "
                    + "circonstances) pour fonder l'expertise biologique. Une simple absence de "
                    + "ressemblance ou un doute n'est pas suffisant.");
        }
        if (possessionEtat5Ans && qualite == QualiteAagir.ENFANT_MAJEUR) {
            risques.add("Possession d'état conforme 5 ans — l'enfant majeur conserve la qualité "
                    + "à agir mais le juge peut prendre en compte la stabilité de la filiation "
                    + "vécue pour apprécier l'intérêt supérieur de l'enfant et la proportionnalité.");
        }
        if (!expertiseAdn && motifsSerieux && !prescrit && !finNonRecevoir) {
            risques.add("Expertise ADN non demandée à ce stade — il convient de la solliciter "
                    + "dans l'assignation (Cass. 1ère civ. 28 mars 2000 : l'expertise est de "
                    + "droit en matière de filiation hors motif légitime de refus).");
        }
        return risques;
    }

    private static List<String> buildDocuments(QualiteAagir qualite, boolean expertiseAdn) {
        List<String> docs = new ArrayList<>();
        docs.add("Acte de naissance intégral de l'enfant (de moins de 3 mois)");
        docs.add("Copie de l'acte de reconnaissance ou du jugement établissant la filiation contestée");
        docs.add("Pièce d'identité du demandeur (CNI ou passeport en cours de validité)");
        switch (qualite) {
            case PERE_DECLARE -> docs.add("Tout élément établissant la connaissance récente "
                    + "de la non-paternité (correspondances, attestations, expertise ADN privée)");
            case PERE_BIOLOGIQUE_PRESUME -> docs.add("Éléments établissant la paternité "
                    + "biologique du demandeur (relations avec la mère, ADN privé éventuel, "
                    + "attestations)");
            case MERE -> docs.add("Élément(s) établissant la paternité biologique "
                    + "d'un tiers (le plus souvent le père biologique réel)");
            case ENFANT_MAJEUR -> docs.add("Justificatif de majorité de l'enfant et "
                    + "tout élément remettant en cause la filiation paternelle déclarée");
        }
        if (expertiseAdn) {
            docs.add("Demande d'expertise génétique judiciaire à inclure dans l'assignation "
                    + "(art. 16-11 Cciv — laboratoire agréé, prélèvement contradictoire)");
        } else {
            docs.add("Note d'audience pour solliciter une expertise ADN au visa de l'art. "
                    + "16-11 Cciv et de la jurisprudence Cass. 1ère civ. 28 mars 2000");
        }
        docs.add("Justificatif de domicile du demandeur");
        return docs;
    }

    private static List<String> buildMessages(QualiteAagir qualite,
                                              VerdictRecevabilite verdict,
                                              boolean prescrit,
                                              boolean finNonRecevoir,
                                              boolean motifsSerieux,
                                              boolean expertiseAdn,
                                              boolean expertiseAdnRecommandee,
                                              int delaiAns,
                                              long restantMois,
                                              boolean possessionEtat5Ans) {
        List<String> msgs = new ArrayList<>();
        msgs.add("Qualité à agir : " + libelleQualite(qualite));
        msgs.add("Délai de prescription applicable : " + delaiAns + " ans à compter "
                + (qualite == QualiteAagir.ENFANT_MAJEUR
                        ? "de la majorité de l'enfant (art. 321 Cciv)."
                        : "de la connaissance de la non-filiation (art. 333 al. 1 Cciv)."));

        if (prescrit) {
            msgs.add("PRESCRIPTION ACQUISE — l'action est forclose ("
                    + Math.abs(restantMois) + " mois après l'expiration du délai). "
                    + "L'assignation serait déclarée irrecevable.");
        } else {
            msgs.add("Délai non encore prescrit — il reste " + restantMois
                    + " mois pour assigner.");
        }
        if (finNonRecevoir) {
            msgs.add("FIN DE NON-RECEVOIR (art. 333 al. 2) — la possession d'état conforme "
                    + "à la filiation depuis 5 ans ou plus rend l'action irrecevable de la "
                    + "part du contestant (sauf l'enfant lui-même).");
        }
        if (possessionEtat5Ans && qualite == QualiteAagir.ENFANT_MAJEUR) {
            msgs.add("Possession d'état conforme 5 ans — l'enfant majeur conserve la qualité "
                    + "à agir, mais le juge appréciera la proportionnalité avec la stabilité "
                    + "de la filiation vécue.");
        }

        if (motifsSerieux) {
            msgs.add("Motifs sérieux et concordants énoncés — conformes à l'exigence "
                    + "jurisprudentielle (Cass. 1ère civ. 28 mars 2000) pour fonder l'expertise.");
        } else {
            msgs.add("Motifs sérieux NON démontrés — la jurisprudence exige des éléments précis "
                    + "et concordants (au-delà d'un simple doute) pour ouvrir l'expertise ADN.");
        }
        if (expertiseAdn) {
            msgs.add("Expertise ADN demandée — l'expertise génétique est de droit en matière "
                    + "de filiation hors motif légitime de refus (art. 16-11 Cciv ; Cass. 1ère "
                    + "civ. 28 mars 2000).");
        }
        if (expertiseAdnRecommandee) {
            msgs.add("Expertise ADN RECOMMANDÉE — à solliciter dans l'assignation au visa de "
                    + "l'art. 16-11 Cciv (prélèvement contradictoire en laboratoire agréé).");
        }

        switch (verdict) {
            case ELEVEE -> msgs.add("VERDICT ELEVEE — l'action en contestation présente toutes "
                    + "les conditions de recevabilité (qualité à agir + délai + motifs + ADN). "
                    + "Engagement contentieux fortement recommandé.");
            case MOYENNE -> msgs.add("VERDICT MOYENNE — l'action est recevable mais comporte des "
                    + "facteurs de risque (motifs partiels, délai consommé, possession d'état). "
                    + "Préparer la défense avec une stratégie probatoire renforcée.");
            case FAIBLE -> msgs.add("VERDICT FAIBLE — l'action serait probablement déclarée "
                    + "irrecevable (prescription, fin de non-recevoir, ou absence de motifs). "
                    + "Étudier les voies alternatives (action en recherche de paternité 327, "
                    + "possession d'état 317) avant tout dépôt.");
        }

        msgs.add("Effets en cas de succès : la filiation paternelle déclarée est annulée "
                + "rétroactivement, perte des droits-devoirs (autorité parentale art. 372, "
                + "nom art. 311-21, succession), restitution éventuelle des prestations.");
        msgs.add("Tribunal compétent : tribunal judiciaire avec représentation obligatoire "
                + "par avocat (art. 318 al. 2 Cciv).");
        msgs.add("Base juridique : " + BASE_JURIDIQUE + ".");

        return msgs;
    }

    private static String libelleQualite(QualiteAagir q) {
        return switch (q) {
            case PERE_DECLARE -> "Père légalement déclaré contestant sa propre paternité (art. 332-333 Cciv)";
            case PERE_BIOLOGIQUE_PRESUME -> "Père biologique présumé contestant la filiation déclarée (art. 333 Cciv)";
            case MERE -> "Mère contestant la paternité du père déclaré (art. 333 Cciv)";
            case ENFANT_MAJEUR -> "Enfant majeur contestant sa filiation paternelle (art. 333 + 321 Cciv)";
        };
    }
}
