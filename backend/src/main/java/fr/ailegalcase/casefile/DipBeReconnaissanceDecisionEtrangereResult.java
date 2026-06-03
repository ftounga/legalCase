package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-223-08 : résultat structuré du moteur décisionnel BE de reconnaissance /
 * exequatur d'une décision familiale étrangère (CDIP art. 22-27).
 *
 * <p>{@code motifs} et {@code messages} ne sont jamais vides. {@code actesAProduire}
 * liste les démarches (légalisation/apostille, requête en exequatur…) — vide
 * pour {@code QUALIFICATION_INCOMPLETE}. Aucune citation jurisprudentielle
 * (F-JU-04 parké).</p>
 */
public record DipBeReconnaissanceDecisionEtrangereResult(
        DipBeReconnaissanceDecisionEtrangereCalculator.DipBeReconnaissanceVerdict verdict,
        List<String> motifs,
        List<String> conseils,
        List<String> actesAProduire,
        List<String> basesJuridiques,
        List<String> messages
) {}
