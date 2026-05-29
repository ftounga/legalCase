package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-214-27 : analyseur de la procédure d'évaluation d'âge d'un mineur non
 * accompagné (MNA) refusé par l'ASE, incluant les recours devant le juge des
 * enfants (JE) et la contestation des examens osseux. Outil single-country FR.
 *
 * <p>Gate métier : l'âge déclaré (calculé depuis la date de naissance déclarée)
 * doit être strictement inférieur à 18 ans — sinon l'outil n'a pas de sens
 * (la personne se déclare majeure). Le gate est levé via
 * {@link IllegalArgumentException}, traduit en 400 par le service.</p>
 *
 * <p>Source juridique :
 * <ul>
 *   <li>Cciv 375 — mesures de protection de l'enfant (ordonnance JE)</li>
 *   <li>Cciv 388 — mineur et détermination de l'âge (limites des examens
 *       osseux : le doute profite à l'intéressé)</li>
 *   <li>CE 25 juillet 2013, n° 371334 — contestation de la fiabilité des
 *       examens osseux</li>
 *   <li>Circulaire Taubira du 31 mai 2013 — procédure d'évaluation MNA</li>
 *   <li>Arrêté du 17 novembre 2016 — protocole d'évaluation MNA</li>
 *   <li>L. 425-3 CESEDA — APS pour MNA pris en charge par l'ASE après 16 ans</li>
 * </ul>
 */
public final class MnaEvaluationAgeAnalyzer {

    /** Délai d'urgence pour saisir le juge des enfants après un refus ASE (jours). */
    public static final int DELAI_SAISINE_JE_JOURS = 5;

    /** Âge de majorité civile (Cciv 388). */
    public static final int AGE_MAJORITE = 18;

    public static final String BASE_JURIDIQUE =
            "Cciv 375 et 388 ; circulaire Taubira du 31 mai 2013 ; arrêté du "
            + "17 novembre 2016 ; CE 25 juillet 2013 n° 371334 ; L. 425-3 CESEDA.";

    private final LocalDate today;

    public MnaEvaluationAgeAnalyzer(LocalDate today) {
        this.today = today;
    }

    /**
     * Analyse la situation d'évaluation d'âge MNA.
     *
     * @param dateNaissanceDeclaree date de naissance déclarée (requise)
     * @param evaluationASERefusee   true si l'ASE a refusé la prise en charge
     * @param dateRefusASE           date du refus ASE (utilisée pour l'échéance JE)
     * @param examenOsseuxOrdonne    true si un examen osseux a été ordonné
     * @param resultatExamenOsseux   résultat libre de l'examen osseux (nullable)
     * @return résultat de l'analyse
     * @throws IllegalArgumentException entrée invalide / âge déclaré ≥ 18 ans
     */
    public MnaEvaluationAgeResult analyze(LocalDate dateNaissanceDeclaree,
                                          boolean evaluationASERefusee,
                                          LocalDate dateRefusASE,
                                          boolean examenOsseuxOrdonne,
                                          String resultatExamenOsseux) {
        if (dateNaissanceDeclaree == null) {
            throw new IllegalArgumentException("dateNaissanceDeclaree est requise");
        }
        if (dateNaissanceDeclaree.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateNaissanceDeclaree ne peut pas être dans le futur");
        }

        int ageDeclare = Period.between(dateNaissanceDeclaree, today).getYears();
        if (ageDeclare >= AGE_MAJORITE) {
            throw new IllegalArgumentException(
                    "L'âge déclaré (" + ageDeclare + " ans) est supérieur ou égal à "
                    + AGE_MAJORITE + " ans : l'outil d'évaluation d'âge MNA ne "
                    + "s'applique qu'aux personnes se déclarant mineures");
        }

        LocalDate dateEcheanceSaisineJE = null;
        if (evaluationASERefusee && dateRefusASE != null) {
            dateEcheanceSaisineJE = dateRefusASE.plusDays(DELAI_SAISINE_JE_JOURS);
        }

        MnaEvaluationAgeStatut statut = determineStatut(
                evaluationASERefusee, examenOsseuxOrdonne);

        List<String> procedureASE = buildProcedureASE();
        List<String> contestationExamenOsseux =
                examenOsseuxOrdonne ? buildContestationExamenOsseux() : List.of();
        List<String> droitsAttaches = buildDroitsAttaches();

        return new MnaEvaluationAgeResult(
                dateNaissanceDeclaree,
                ageDeclare,
                evaluationASERefusee,
                dateRefusASE,
                dateEcheanceSaisineJE,
                examenOsseuxOrdonne,
                resultatExamenOsseux,
                statut,
                procedureASE,
                contestationExamenOsseux,
                droitsAttaches,
                BASE_JURIDIQUE);
    }

    /**
     * Statut de la procédure :
     * <ul>
     *   <li>{@code EXAMEN_OSSEUX_CONTESTE} si un examen osseux a été ordonné
     *       (priorité — la contestation médicale prime sur le calendrier JE)</li>
     *   <li>{@code RECOURS_JE_URGENT} si l'ASE a refusé (saisine JE à mener)</li>
     *   <li>{@code PRIS_EN_CHARGE} si aucune contestation : minorité admise</li>
     *   <li>{@code EN_ATTENTE_EVALUATION} sinon (cas par défaut)</li>
     * </ul>
     */
    private MnaEvaluationAgeStatut determineStatut(boolean evaluationASERefusee,
                                                   boolean examenOsseuxOrdonne) {
        if (examenOsseuxOrdonne) {
            return MnaEvaluationAgeStatut.EXAMEN_OSSEUX_CONTESTE;
        }
        if (evaluationASERefusee) {
            return MnaEvaluationAgeStatut.RECOURS_JE_URGENT;
        }
        return MnaEvaluationAgeStatut.PRIS_EN_CHARGE;
    }

    private List<String> buildProcedureASE() {
        List<String> etapes = new ArrayList<>();
        etapes.add("1. Entretien d'évaluation de la minorité et de l'isolement par "
                + "le service du conseil départemental (ASE) — protocole de l'arrêté "
                + "du 17 novembre 2016.");
        etapes.add("2. En cas de refus de prise en charge par l'ASE, saisine en "
                + "urgence du juge des enfants (Cciv 375) — délai indicatif de "
                + DELAI_SAISINE_JE_JOURS + " jours pour ne pas laisser l'intéressé "
                + "sans protection.");
        etapes.add("3. Demande d'ordonnance de placement provisoire (OPP) au juge "
                + "des enfants afin d'assurer l'hébergement et la protection "
                + "immédiate de l'intéressé.");
        etapes.add("4. Placement à l'ASE et reconnaissance de la minorité — mise en "
                + "place du projet pour l'enfant (scolarisation, prise en charge "
                + "éducative).");
        return etapes;
    }

    private List<String> buildContestationExamenOsseux() {
        List<String> moyens = new ArrayList<>();
        moyens.add("Contester la fiabilité scientifique de la méthode Greulich-Pyle, "
                + "établie à partir d'une population non représentative et inadaptée "
                + "à la détermination d'un âge civil précis.");
        moyens.add("Invoquer la marge d'erreur reconnue de l'ordre de 2 ans qui "
                + "interdit de conclure à la majorité avec certitude (Cciv 388).");
        moyens.add("Rappeler que l'examen osseux ne peut être l'unique fondement de "
                + "la décision et n'intervient qu'à titre subsidiaire, après accord "
                + "de l'intéressé et autorisation judiciaire (Cciv 388).");
        moyens.add("Faire valoir que le doute profite à l'intéressé : en cas de "
                + "doute sur l'âge, la minorité doit être retenue "
                + "(CE 25 juillet 2013 n° 371334 ; Cciv 388).");
        return moyens;
    }

    private List<String> buildDroitsAttaches() {
        List<String> droits = new ArrayList<>();
        droits.add("Hébergement d'urgence et mise à l'abri immédiate pendant "
                + "l'évaluation et l'instance devant le juge des enfants.");
        droits.add("Scolarisation : droit à l'instruction du mineur, quelle que "
                + "soit sa situation administrative.");
        droits.add("Pour le MNA pris en charge par l'ASE après 16 ans : "
                + "délivrance d'une autorisation provisoire de séjour (APS) au titre "
                + "de l'art. L. 425-3 CESEDA, sous conditions de formation.");
        return droits;
    }
}
