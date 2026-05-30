package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-218-21 : analyseur du régime du <b>stagiaire en milieu professionnel</b>
 * (art. L.124-1 et s. du code de l'éducation, F-DT-109). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Logique métier (invariant CLAUDE.md — un outil = une situation) :
 * <ul>
 *   <li><b>Seuil de gratification</b> (art. L.124-6 + D.124-6) : la gratification
 *       minimale est obligatoire au-delà de <b>2 mois de présence</b>, soit plus
 *       de <b>44 jours / 308 h</b> sur une même année d'enseignement. En deçà,
 *       aucune gratification minimale n'est légalement due.</li>
 *   <li><b>Gratification minimale</b> : {@code due = tauxHoraire × heures}, où
 *       {@code heures = nombreJoursPresence × 7} et {@code tauxHoraire} est le
 *       plus favorable entre le minimum légal (15 % du plafond horaire de la
 *       Sécurité sociale) et le taux horaire conventionnel éventuel. Le rappel
 *       exigible vaut {@code max(0, due − versée)} où la gratification versée
 *       totale = mensuelle × nombre de mois de stage.</li>
 *   <li><b>Risque de requalification en CDI</b> (art. L.124-5 durée maximale ;
 *       art. L.124-8 poste permanent) : score à partir de trois indices —
 *       missions hors projet pédagogique, occupation d'un poste de travail
 *       permanent, dépassement de la durée maximale de 6 mois (924 h).
 *       <ul>
 *         <li>ELEVE : ≥ 2 indices présents OU dépassement de la durée maximale ;</li>
 *         <li>MODERE : exactement 1 indice présent ;</li>
 *         <li>FAIBLE : aucun indice.</li>
 *       </ul></li>
 *   <li><b>Verdict global</b> :
 *     <ul>
 *       <li>REQUALIFICATION_PROBABLE si {@code risque = ELEVE} ;</li>
 *       <li>RAPPEL_GRATIFICATION si un rappel reste dû (risque non élevé) ;</li>
 *       <li>STAGE_CONFORME sinon.</li>
 *     </ul></li>
 * </ul>
 *
 * <p>Hors périmètre : chiffrage complet des indemnités de rupture en cas de
 * requalification (renvoi vers les calculateurs IL existants), stages de la
 * formation professionnelle continue, apprentissage (F-DT-110). Base juridique
 * annotée « à vérifier par avocat ».
 */
public final class StagiaireGratificationAnalyzer {

    /** Heures effectives par jour de présence (1 jour = 7 h, art. D.124-6). */
    static final BigDecimal HEURES_PAR_JOUR = new BigDecimal("7");

    /** Seuil de déclenchement de la gratification : au-delà de 44 jours de présence. */
    static final int SEUIL_JOURS_PRESENCE = 44;

    /** Durée maximale du stage : 6 mois par année d'enseignement (art. L.124-5). */
    static final int DUREE_MAX_MOIS = 6;

    // Plafond horaire de la Sécurité sociale 2025 (à actualiser annuellement).
    private static final BigDecimal PLAFOND_HORAIRE_SS = new BigDecimal("29.00");

    /**
     * gratification minimale = 15 % du plafond horaire SS — taux à actualiser
     * annuellement (plafond SS).
     */
    static final BigDecimal TAUX_HORAIRE_GRATIFICATION =
            PLAFOND_HORAIRE_SS.multiply(new BigDecimal("0.15"));

    static final String BASE_JURIDIQUE =
            "art. L.124-1 et s. du code de l'éducation — périodes de formation en "
                    + "milieu professionnel et stages ; art. L.124-6 + D.124-6 "
                    + "(gratification minimale obligatoire au-delà de 2 mois / 44 jours "
                    + "/ 308 h, égale à 15 % du plafond horaire de la Sécurité sociale) ; "
                    + "art. L.124-5 (durée maximale du stage : 6 mois / 924 h par année "
                    + "d'enseignement) ; art. L.124-8 (interdiction d'occuper un poste de "
                    + "travail permanent ; requalification en contrat de travail). Taux de "
                    + "gratification et plafond SS à actualiser annuellement. "
                    + "Qualification et conséquences à vérifier par l'avocat "
                    + "(à vérifier par avocat)";

    private StagiaireGratificationAnalyzer() {
    }

    /**
     * Analyse le régime du stagiaire : vérifie le seuil de gratification, calcule
     * la gratification minimale due et le rappel exigible, puis apprécie le risque
     * de requalification du stage en CDI et rend le verdict global.
     *
     * @param dateDebutStage date de début (requise).
     * @param dateFinStage date de fin (requise, ≥ dateDebutStage).
     * @param nombreJoursPresence jours de présence effective (requis, ≥ 0).
     * @param gratificationMensuelleVersee gratification mensuelle perçue (défaut 0, ≥ 0).
     * @param tauxHoraireConventionnel taux horaire conventionnel (optionnel, ≥ 0).
     * @param missionsHorsProjetPedagogique indice de requalification (défaut false).
     * @param posteTravailPermanent indice de requalification (défaut false).
     */
    public static StagiaireGratificationResult analyze(LocalDate dateDebutStage,
                                                       LocalDate dateFinStage,
                                                       Integer nombreJoursPresence,
                                                       BigDecimal gratificationMensuelleVersee,
                                                       BigDecimal tauxHoraireConventionnel,
                                                       Boolean missionsHorsProjetPedagogique,
                                                       Boolean posteTravailPermanent) {
        validate(dateDebutStage, dateFinStage, nombreJoursPresence,
                gratificationMensuelleVersee, tauxHoraireConventionnel);

        int jours = nombreJoursPresence;
        BigDecimal versee = gratificationMensuelleVersee != null
                ? gratificationMensuelleVersee : BigDecimal.ZERO;
        boolean missionsHors = missionsHorsProjetPedagogique != null && missionsHorsProjetPedagogique;
        boolean postePermanent = posteTravailPermanent != null && posteTravailPermanent;

        long dureeStageJours = ChronoUnit.DAYS.between(dateDebutStage, dateFinStage) + 1;
        // Nombre de mois calendaires entamés (au moins 1 dès qu'il y a présence).
        long moisStage = Math.max(1, ChronoUnit.MONTHS.between(dateDebutStage, dateFinStage) + 1);
        boolean depassementDureeMax =
                ChronoUnit.MONTHS.between(dateDebutStage, dateFinStage) >= DUREE_MAX_MOIS;

        boolean seuilAtteint = jours > SEUIL_JOURS_PRESENCE;
        boolean gratificationObligatoire = seuilAtteint;

        BigDecimal heuresPresence = HEURES_PAR_JOUR.multiply(BigDecimal.valueOf(jours));

        BigDecimal tauxHoraireApplique = TAUX_HORAIRE_GRATIFICATION;
        if (tauxHoraireConventionnel != null
                && tauxHoraireConventionnel.compareTo(tauxHoraireApplique) > 0) {
            tauxHoraireApplique = tauxHoraireConventionnel;
        }
        tauxHoraireApplique = tauxHoraireApplique.setScale(2, RoundingMode.HALF_UP);

        BigDecimal gratificationMinimaleDue = gratificationObligatoire
                ? heuresPresence.multiply(tauxHoraireApplique).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal gratificationVerseeTotale = versee
                .multiply(BigDecimal.valueOf(moisStage)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal rappelGratification = gratificationMinimaleDue
                .subtract(gratificationVerseeTotale);
        if (rappelGratification.compareTo(BigDecimal.ZERO) < 0) {
            rappelGratification = BigDecimal.ZERO;
        }
        rappelGratification = rappelGratification.setScale(2, RoundingMode.HALF_UP);

        List<String> motifs = new ArrayList<>();
        int indices = 0;
        if (missionsHors) {
            indices++;
            motifs.add("Missions exécutées sans lien avec le projet pédagogique (art. L.124-8).");
        }
        if (postePermanent) {
            indices++;
            motifs.add("Occupation d'un poste de travail permanent de l'entreprise (art. L.124-8).");
        }
        if (depassementDureeMax) {
            motifs.add("Dépassement de la durée maximale légale du stage (6 mois / 924 h, art. L.124-5).");
        }

        StagiaireRisqueRequalification risque;
        if (indices >= 2 || depassementDureeMax) {
            risque = StagiaireRisqueRequalification.ELEVE;
            motifs.add("Requalification en CDI possible : rappel de salaire sur la base du "
                    + "SMIC / minimum conventionnel + indemnités de rupture (à chiffrer).");
        } else if (indices == 1) {
            risque = StagiaireRisqueRequalification.MODERE;
        } else {
            risque = StagiaireRisqueRequalification.FAIBLE;
            motifs.add("Aucun indice de requalification : objet pédagogique et durées légales respectés.");
        }

        StagiaireGratificationVerdict verdict;
        if (risque == StagiaireRisqueRequalification.ELEVE) {
            verdict = StagiaireGratificationVerdict.REQUALIFICATION_PROBABLE;
        } else if (rappelGratification.compareTo(BigDecimal.ZERO) > 0) {
            verdict = StagiaireGratificationVerdict.RAPPEL_GRATIFICATION;
        } else {
            verdict = StagiaireGratificationVerdict.STAGE_CONFORME;
        }

        return new StagiaireGratificationResult(
                dateDebutStage,
                dateFinStage,
                jours,
                heuresPresence.setScale(2, RoundingMode.HALF_UP),
                dureeStageJours,
                seuilAtteint,
                gratificationObligatoire,
                tauxHoraireApplique,
                gratificationMinimaleDue,
                gratificationVerseeTotale,
                rappelGratification,
                depassementDureeMax,
                missionsHors,
                postePermanent,
                risque,
                List.copyOf(motifs),
                verdict,
                BASE_JURIDIQUE);
    }

    private static void validate(LocalDate dateDebutStage,
                                 LocalDate dateFinStage,
                                 Integer nombreJoursPresence,
                                 BigDecimal gratificationMensuelleVersee,
                                 BigDecimal tauxHoraireConventionnel) {
        if (dateDebutStage == null) {
            throw new IllegalArgumentException("dateDebutStage est requise");
        }
        if (dateFinStage == null) {
            throw new IllegalArgumentException("dateFinStage est requise");
        }
        if (dateFinStage.isBefore(dateDebutStage)) {
            throw new IllegalArgumentException("dateFinStage ne peut pas précéder dateDebutStage");
        }
        if (nombreJoursPresence == null) {
            throw new IllegalArgumentException("nombreJoursPresence est requis");
        }
        if (nombreJoursPresence < 0) {
            throw new IllegalArgumentException("nombreJoursPresence ne peut pas être négatif");
        }
        if (gratificationMensuelleVersee != null
                && gratificationMensuelleVersee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("gratificationMensuelleVersee ne peut pas être négative");
        }
        if (tauxHoraireConventionnel != null
                && tauxHoraireConventionnel.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("tauxHoraireConventionnel ne peut pas être négatif");
        }
    }
}
