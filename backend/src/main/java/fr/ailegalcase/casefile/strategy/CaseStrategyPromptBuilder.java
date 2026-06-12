package fr.ailegalcase.casefile.strategy;

import fr.ailegalcase.casefile.DashboardTile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * F-286 / SF-286-01 — construit le prompt de la recommandation stratégique de dossier.
 *
 * <p>Couche de <b>synthèse en lecture</b> : le prompt consomme exclusivement les
 * <b>verdicts des outils CALCULÉS</b> (tuiles agrégées du tableau de bord — jamais les
 * tuiles pré-remplies non cliquées) et la <b>synthèse</b> du dossier. Il demande au
 * modèle une vue de plus haute altitude — il ne lui demande JAMAIS de recalculer,
 * réordonner ou contredire un outil. Invariant « 1 outil = 1 situation » respecté par
 * construction : la stratégie est une lecture, pas une mutation.</p>
 */
@Component
public class CaseStrategyPromptBuilder {

    static final String SYSTEM_PROMPT = """
            Tu es un avocat senior qui prépare la stratégie d'un dossier (droit du travail,
            droit de la famille ou droit des étrangers) à partir d'éléments DÉJÀ établis :
            la synthèse du dossier et les verdicts d'outils décisionnels que l'avocat a
            CALCULÉS.

            Ta mission : consolider ces éléments en une RECOMMANDATION STRATÉGIQUE lisible
            et actionnable, en français, structurée en markdown avec EXACTEMENT ces titres
            de niveau 2 (##), dans cet ordre :

            ## Voie procédurale
            Référé ou fond ? (et le cas échéant : procédure d'urgence, requête, conciliation
            préalable). Justifie en une à trois phrases, en t'appuyant sur les verdicts fournis.

            ## Posture
            Concilier / transiger ou plaider ? Évalue le rapport de force au vu des verdicts
            (forces, points de faiblesse, risques). Une à trois phrases.

            ## Priorisation des chefs de demande
            Liste à puces, du plus solide/prioritaire au plus accessoire, en t'appuyant sur
            les verdicts calculés (un chef = une demande). Indique brièvement le fondement.

            ## Séquencement
            Les prochaines actions concrètes dans l'ordre (ce qu'il faut faire d'abord, ensuite).
            Liste à puces courte.

            RÈGLES STRICTES :
            - Tu ne RECALCULES rien, tu ne CONTREDIS aucun verdict fourni : tu les consolides.
            - Tu n'inventes AUCUN verdict, montant ou délai absent des éléments fournis. Si un
              point manque, dis-le explicitement ("à confirmer", "non chiffré dans le dossier")
              plutôt que d'inventer.
            - Aucun jargon interne, aucun code de feature (ex. "F-DT-08"), aucun nom d'outil
              technique : raisonne comme un avocat qui parle à un confrère.
            - Pas de citation de jurisprudence inventée. Réponds UNIQUEMENT avec le markdown
              de la recommandation, sans préambule ni conclusion hors-sujet.
            """;

    /**
     * Construit le message utilisateur à partir des verdicts calculés et de la synthèse.
     *
     * @param caseTitle           titre du dossier (contexte), peut être {@code null}.
     * @param tiles               verdicts des outils CALCULÉS (jamais {@code null} ; déjà
     *                            filtrés « calculés » par {@code assembleDecisionToolTiles}).
     * @param synthesisJson       JSON brut de la synthèse {@code DONE}, ou {@code null}.
     * @param retainedStrategies  pistes stratégiques retenues de la synthèse, ou vide.
     */
    public String buildUserMessage(String caseTitle,
                                   List<DashboardTile> tiles,
                                   String synthesisJson,
                                   List<String> retainedStrategies) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== DOSSIER ===\n");
        sb.append(caseTitle != null && !caseTitle.isBlank() ? caseTitle : "(sans titre)").append('\n');

        sb.append("\n=== VERDICTS DES OUTILS DÉCISIONNELS CALCULÉS ===\n");
        if (tiles == null || tiles.isEmpty()) {
            sb.append("Aucun outil décisionnel calculé.\n");
        } else {
            for (DashboardTile t : tiles) {
                sb.append("- ").append(nullSafe(t.label())).append(" : ")
                        .append(nullSafe(t.primaryValue()));
                if (t.secondaryValue() != null && !t.secondaryValue().isBlank()) {
                    sb.append(" — ").append(t.secondaryValue());
                }
                if (t.alertLevel() != null && !t.alertLevel().isBlank()) {
                    sb.append(" [").append(t.alertLevel()).append(']');
                }
                sb.append('\n');
            }
        }

        sb.append("\n=== PISTES STRATÉGIQUES RETENUES (synthèse) ===\n");
        if (retainedStrategies == null || retainedStrategies.isEmpty()) {
            sb.append("Aucune piste stratégique retenue.\n");
        } else {
            for (String s : retainedStrategies) {
                if (s != null && !s.isBlank()) {
                    sb.append("- ").append(s.trim()).append('\n');
                }
            }
        }

        sb.append("\n=== SYNTHÈSE DU DOSSIER (JSON) ===\n");
        sb.append(synthesisJson != null && !synthesisJson.isBlank()
                ? synthesisJson
                : "Synthèse indisponible.").append('\n');

        sb.append("\nProduis la recommandation stratégique en respectant strictement le format demandé.");
        return sb.toString();
    }

    private static String nullSafe(String value) {
        return value == null ? "—" : value;
    }
}
