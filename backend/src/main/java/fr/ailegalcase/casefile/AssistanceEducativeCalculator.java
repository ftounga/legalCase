package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-222-04 : analyseur statique de l'assistance éducative — mineur en danger
 * (art. 375 et s. Cciv), Famille FRANCE uniquement.
 *
 * <p>UN SEUL outil oriente vers les 4 issues d'UNE situation (mineur en danger),
 * selon les critères de l'art. 375 et s. (invariant « 1 situation = 1 outil ») :</p>
 *
 * <ol>
 *   <li><b>PAS_DE_MESURE</b> : le danger caractérisé de l'art. 375 n'est pas
 *       établi → pas d'assistance éducative.</li>
 *   <li><b>OPP_PLACEMENT</b> : danger caractérisé ET (urgence OU maintien dans le
 *       milieu familial impossible) → ordonnance de placement provisoire /
 *       placement (art. 375-3 / 375-5 Cciv), retrait du milieu familial. Le
 *       placement et l'urgence priment sur la mesure amiable.</li>
 *   <li><b>AED</b> : danger caractérisé, pas d'urgence, maintien possible,
 *       adhésion de la famille ET mesure amiable ASE envisageable → aide
 *       éducative à domicile (mesure administrative ASE, accord parental,
 *       art. L. 222-3 CASF). Pas d'intervention du juge.</li>
 *   <li><b>AEMO</b> : danger caractérisé, pas d'urgence, maintien possible, mais
 *       pas d'adhésion OU mesure amiable non envisageable → action éducative en
 *       milieu ouvert (mesure judiciaire, juge des enfants, art. 375-2 Cciv).</li>
 * </ol>
 *
 * <p>L'outil <b>conseille</b> l'avocat ; la mesure judiciaire d'assistance
 * éducative est ordonnée par le juge des enfants, la mesure administrative par
 * les services de l'aide sociale à l'enfance (ASE). Les critères de l'art. 375
 * et s. sont « à vérifier par l'avocat ».</p>
 */
public final class AssistanceEducativeCalculator {

    static final String BASE_375 =
            "art. 375 Cciv (assistance éducative — mineur dont la santé, la sécurité ou la moralité sont en danger,"
                    + " ou dont les conditions d'éducation ou le développement sont gravement compromis)";
    static final String BASE_375_2 =
            "art. 375-2 Cciv (maintien dans le milieu actuel — action éducative en milieu ouvert / AEMO)";
    static final String BASE_375_3 =
            "art. 375-3 Cciv (placement — confier le mineur à un tiers ou à un service)";
    static final String BASE_375_5 =
            "art. 375-5 Cciv (urgence — placement provisoire ordonné par le juge ou, en cas d'urgence, le procureur)";
    static final String BASE_L222_3 =
            "art. L. 222-3 CASF (aide éducative à domicile — AED, mesure administrative ASE, accord parental)";

    static final String JURIDICTION_ASE =
            "Services de l'aide sociale à l'enfance (ASE) — mesure administrative, accord des parents";
    static final String JURIDICTION_JUGE_ENFANTS =
            "Juge des enfants (tribunal judiciaire) — mesure judiciaire d'assistance éducative";
    static final String JURIDICTION_AUCUNE =
            "Aucune (danger caractérisé de l'art. 375 Cciv non établi)";

    static final String MSG_DECISION_JUGE =
            "Décision d'orientation : la mesure judiciaire d'assistance éducative (AEMO, placement) est ordonnée par "
                    + "le juge des enfants ; la mesure administrative (AED) relève des services de l'ASE. Cet outil "
                    + "évalue le danger (art. 375 Cciv) et conseille l'avocat ; il ne prononce pas la mesure. Les "
                    + "critères de l'art. 375 et s. Cciv sont à vérifier par l'avocat au regard des pièces.";

    private AssistanceEducativeCalculator() {}

    /**
     * Analyse la situation du mineur et oriente vers la mesure adaptée.
     *
     * @param req     requête validée (gate pays vérifié par le service)
     * @param country pays du workspace ("FRANCE" attendu)
     * @return résultat de l'analyse
     * @throws IllegalArgumentException si les pré-requis pays ne sont pas respectés
     */
    public static AssistanceEducativeResult compute(AssistanceEducativeRequest req, String country) {
        if (!"FRANCE".equals(country)) {
            throw new IllegalArgumentException(
                    "Outil F-FA-ASSISTANCE-EDUCATIVE applicable uniquement en France (art. 375 et s. Cciv).");
        }
        if (req == null) {
            throw new IllegalArgumentException("Requête assistance éducative manquante.");
        }

        boolean danger = Boolean.TRUE.equals(req.dangerCaracterise());
        boolean urgence = Boolean.TRUE.equals(req.urgence());
        boolean adhesion = Boolean.TRUE.equals(req.adhesionFamille());
        boolean maintien = Boolean.TRUE.equals(req.maintienMilieuFamilialPossible());
        boolean amiable = Boolean.TRUE.equals(req.mesureAmiableASEEnvisageable());

        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        bases.add(BASE_375);

        // 1. Pas de danger caractérisé → pas d'assistance éducative.
        if (!danger) {
            messages.add("Le danger caractérisé de l'art. 375 Cciv (santé, sécurité, moralité, conditions "
                    + "d'éducation ou développement gravement compromis) n'est pas établi : aucune mesure "
                    + "d'assistance éducative ne se justifie en l'état. Réévaluer si de nouveaux éléments de "
                    + "danger apparaissent.");
            messages.add(MSG_DECISION_JUGE);
            return new AssistanceEducativeResult(
                    VerdictAssistanceEducativeEnum.PAS_DE_MESURE,
                    JURIDICTION_AUCUNE,
                    "Aucune mesure d'assistance éducative",
                    bases,
                    messages);
        }

        // 2. Urgence OU maintien impossible → OPP / placement (le retrait prime).
        if (urgence || !maintien) {
            bases.add(BASE_375_3);
            bases.add(BASE_375_5);
            String motif = urgence
                    ? "danger immédiat (urgence)"
                    : "maintien dans le milieu familial impossible";
            messages.add("Danger caractérisé avec " + motif + " : orienter vers une ordonnance de placement "
                    + "provisoire (OPP) ou un placement (art. 375-3 / 375-5 Cciv), avec retrait du mineur de son "
                    + "milieu familial. En cas d'urgence, le placement provisoire peut être ordonné par le juge "
                    + "des enfants ou, à titre exceptionnel, par le procureur de la République (art. 375-5 al. 2).");
            messages.add(MSG_DECISION_JUGE);
            return new AssistanceEducativeResult(
                    VerdictAssistanceEducativeEnum.OPP_PLACEMENT,
                    JURIDICTION_JUGE_ENFANTS,
                    "Ordonnance de placement provisoire (OPP) / placement",
                    bases,
                    messages);
        }

        // 3. Danger, pas d'urgence, maintien possible, adhésion + amiable → AED (administrative).
        if (adhesion && amiable) {
            bases.add(BASE_L222_3);
            messages.add("Danger caractérisé mais mineur maintenu dans son milieu familial, adhésion des "
                    + "titulaires de l'autorité parentale et mesure amiable envisageable : orienter en priorité "
                    + "vers une aide éducative à domicile (AED), mesure administrative contractualisée avec les "
                    + "services de l'ASE (art. L. 222-3 CASF), sans saisine du juge des enfants.");
            messages.add(MSG_DECISION_JUGE);
            return new AssistanceEducativeResult(
                    VerdictAssistanceEducativeEnum.AED,
                    JURIDICTION_ASE,
                    "Aide éducative à domicile (AED — mesure administrative ASE)",
                    bases,
                    messages);
        }

        // 4. Danger, maintien possible, mais pas d'adhésion / amiable non envisageable → AEMO (judiciaire).
        bases.add(BASE_375_2);
        String motif = !adhesion
                ? "absence d'adhésion des titulaires de l'autorité parentale"
                : "mesure amiable ASE non envisageable";
        messages.add("Danger caractérisé, mineur maintenu dans son milieu familial mais " + motif + " : la "
                + "mesure amiable est insuffisante. Orienter vers une action éducative en milieu ouvert (AEMO), "
                + "mesure judiciaire ordonnée par le juge des enfants (art. 375-2 Cciv).");
        messages.add(MSG_DECISION_JUGE);
        return new AssistanceEducativeResult(
                VerdictAssistanceEducativeEnum.AEMO,
                JURIDICTION_JUGE_ENFANTS,
                "Action éducative en milieu ouvert (AEMO — mesure judiciaire)",
                bases,
                messages);
    }
}
