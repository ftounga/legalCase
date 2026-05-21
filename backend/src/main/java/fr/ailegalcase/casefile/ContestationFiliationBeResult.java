package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-217-18 : résultat structuré du moteur décisionnel BE de la contestation
 * de filiation.
 *
 * <p>{@code dateLimiteAction}, {@code joursRestants} et {@code delaiStatut}
 * peuvent être {@code null} en cas de verdicts d'irrecevabilité pour qualité
 * non reconnue ou possession d'état (le calcul du délai n'est pas pertinent
 * dans ces cas).</p>
 */
public record ContestationFiliationBeResult(
        ContestationFiliationBeCalculator.ContestationFiliationBeVerdict verdict,
        LocalDate dateLimiteAction,
        Integer joursRestants,
        ContestationFiliationBeCalculator.DelaiStatutBe delaiStatut,
        ContestationFiliationBeCalculator.VoieProceduraleFiliationBe voieProcedurale,
        List<ContestationFiliationBeCalculator.MotifIrrecevabiliteFiliationBe> motifsIrrecevabilite,
        List<String> actionsConcretes,
        List<String> basesJuridiques,
        List<String> messages,
        String country
) {}
