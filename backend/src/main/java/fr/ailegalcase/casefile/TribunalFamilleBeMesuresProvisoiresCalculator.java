package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-211-03 : calculateur des mesures provisoires devant le tribunal de la famille belge.
 * CJ art. 1280 (mesures urgentes et provisoires) + art. 1253ter+ (tribunal de la famille).
 *
 * <p>Scoring d'urgence :
 * <ul>
 *   <li>violence familiale → +3</li>
 *   <li>déplacement enfant imminent → +3</li>
 *   <li>dilapidation patrimoine → +2</li>
 * </ul>
 *
 * <p>Niveaux : HAUTE (≥ 3) / MOYENNE (2) / BASSE (1) / AUCUNE (0).
 *
 * <p>Outil <b>single-country BE</b>.
 */
public final class TribunalFamilleBeMesuresProvisoiresCalculator {

    public static final int POINTS_VIOLENCE = 3;
    public static final int POINTS_DEPLACEMENT = 3;
    public static final int POINTS_DILAPIDATION = 2;

    public static final String URGENCE_HAUTE = "HAUTE";
    public static final String URGENCE_MOYENNE = "MOYENNE";
    public static final String URGENCE_BASSE = "BASSE";
    public static final String URGENCE_AUCUNE = "AUCUNE";

    public static final String VERDICT_URGENT_REFERE = "URGENT_REFERE";
    public static final String VERDICT_NORMAL_AUDIENCE = "NORMAL_AUDIENCE";
    public static final String VERDICT_AUCUNE_MESURE = "AUCUNE_MESURE";

    public static final String MESURE_RESIDENCE_SEPAREE = "RESIDENCE_SEPAREE";
    public static final String MESURE_CONTRIBUTION = "CONTRIBUTION_ALIMENTAIRE_PROVISOIRE";
    public static final String MESURE_AUTORITE_EXCLUSIVE = "AUTORITE_PARENTALE_EXCLUSIVE_TEMPORAIRE";

    private static final String BASE_JURIDIQUE =
            "CJ art. 1280 (mesures urgentes et provisoires) ; art. 1253ter+ (tribunal de la famille).";

    private TribunalFamilleBeMesuresProvisoiresCalculator() {
    }

    public static TribunalFamilleBeMesuresProvisoiresResult compute(
            boolean violenceFamiliale,
            boolean deplacementEnfantImminent,
            boolean dilapidationPatrimoine,
            boolean besoinResidenceSeparee,
            boolean besoinContributionAlimentaire,
            boolean besoinAutoriteParentaleExclusive) {

        if (!violenceFamiliale && !deplacementEnfantImminent && !dilapidationPatrimoine
                && !besoinResidenceSeparee && !besoinContributionAlimentaire
                && !besoinAutoriteParentaleExclusive) {
            throw new IllegalArgumentException(
                    "Aucun indicateur d'urgence ni besoin de mesure exprimé — l'outil ne s'applique pas.");
        }

        int score = 0;
        if (violenceFamiliale) score += POINTS_VIOLENCE;
        if (deplacementEnfantImminent) score += POINTS_DEPLACEMENT;
        if (dilapidationPatrimoine) score += POINTS_DILAPIDATION;

        String urgenceLevel;
        if (score >= 3) urgenceLevel = URGENCE_HAUTE;
        else if (score == 2) urgenceLevel = URGENCE_MOYENNE;
        else if (score == 1) urgenceLevel = URGENCE_BASSE;
        else urgenceLevel = URGENCE_AUCUNE;

        List<String> mesures = new ArrayList<>();
        if (besoinResidenceSeparee) mesures.add(MESURE_RESIDENCE_SEPAREE);
        if (besoinContributionAlimentaire) mesures.add(MESURE_CONTRIBUTION);
        if (besoinAutoriteParentaleExclusive) mesures.add(MESURE_AUTORITE_EXCLUSIVE);

        String verdict;
        if (URGENCE_HAUTE.equals(urgenceLevel)) {
            verdict = VERDICT_URGENT_REFERE;
        } else if (mesures.isEmpty()) {
            verdict = VERDICT_AUCUNE_MESURE;
        } else {
            verdict = VERDICT_NORMAL_AUDIENCE;
        }

        List<String> messages = buildMessages(urgenceLevel, verdict, mesures,
                violenceFamiliale, deplacementEnfantImminent, dilapidationPatrimoine);
        String formule = buildFormule(urgenceLevel, verdict, score, mesures);

        return new TribunalFamilleBeMesuresProvisoiresResult(
                violenceFamiliale,
                deplacementEnfantImminent,
                dilapidationPatrimoine,
                besoinResidenceSeparee,
                besoinContributionAlimentaire,
                besoinAutoriteParentaleExclusive,
                score,
                urgenceLevel,
                mesures,
                verdict,
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    private static List<String> buildMessages(String urgenceLevel, String verdict,
                                              List<String> mesures,
                                              boolean violence,
                                              boolean deplacement,
                                              boolean dilapidation) {
        List<String> messages = new ArrayList<>();
        messages.add("Saisine : tribunal de la famille, section référé (art. 1253ter CJ).");
        messages.add("Mesures provisoires possibles : résidence séparée, contribution alimentaire "
                + "provisoire, autorité parentale exclusive temporaire (art. 1280 CJ).");

        if (violence) {
            messages.add("Violence familiale : envisager protection en parallèle "
                    + "(Loi 24/11/1997 + art. 22bis Const., ordonnance de protection).");
        }
        if (deplacement) {
            messages.add("Déplacement enfant imminent : agir en référé extrême urgence "
                    + "(art. 584 CJ) — interdiction de sortie du territoire au besoin.");
        }
        if (dilapidation) {
            messages.add("Dilapidation patrimoine : demander mesures conservatoires "
                    + "(art. 1280 CJ + art. 1413+ CJ saisies).");
        }
        if (mesures.isEmpty()) {
            messages.add("Aucune mesure provisoire concrète n'est demandée — clarifier "
                    + "les besoins du client avant saisine.");
        }
        switch (verdict) {
            case VERDICT_URGENT_REFERE -> messages.add(
                    "Urgence HAUTE — saisine en référé extrême urgence sans attendre l'audience ordinaire.");
            case VERDICT_NORMAL_AUDIENCE -> messages.add(
                    "Urgence non extrême — audience ordinaire référé compétente.");
            case VERDICT_AUCUNE_MESURE -> messages.add(
                    "Aucune mesure recommandée à ce stade — réévaluer si la situation évolue.");
            default -> { /* unreachable */ }
        }
        return messages;
    }

    private static String buildFormule(String urgenceLevel, String verdict,
                                       int score, List<String> mesures) {
        return String.format(
                "Mesures provisoires Tribunal de la famille BE — score d'urgence %d (%s), verdict %s. "
                        + "Mesures demandées : %s.",
                score, urgenceLevel, verdict,
                mesures.isEmpty() ? "aucune" : String.join(", ", mesures));
    }
}
