package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-223-04 : résultat structuré du moteur décisionnel BE de cadrage de la
 * filiation post-GPA.
 *
 * <p>{@code cheminContentieux} liste les étapes recommandées d'établissement de
 * la filiation, {@code risques} les risques / réserves (notamment
 * l'inopposabilité de la convention de GPA). {@code cheminContentieux},
 * {@code risques} et {@code messages} ne sont jamais vides.</p>
 */
public record GpaBeResult(
        GpaBeCalculator.GpaBeVerdict verdict,
        List<String> cheminContentieux,
        List<String> risques,
        List<String> basesJuridiques,
        List<String> messages
) {}
