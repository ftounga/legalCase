package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-217-12 : résultat structuré du moteur décisionnel BE de choix d'option
 * successorale (acceptation pure / sous bénéfice d'inventaire / renonciation).
 *
 * <p>{@code optionRecommandee}, {@code dateLimiteOption}, {@code joursRestants} et
 * {@code delaiStatut} peuvent être {@code null} en cas de
 * {@link SuccessionBeAcceptationRenonciationCalculator.SuccessionBeAcceptationRenonciationVerdict#QUALIFICATION_INCOMPLETE}.</p>
 */
public record SuccessionBeAcceptationRenonciationResult(
        SuccessionBeAcceptationRenonciationCalculator.SuccessionBeAcceptationRenonciationVerdict verdict,
        SuccessionBeAcceptationRenonciationCalculator.OptionRecommandeeSuccessionBe optionRecommandee,
        LocalDate dateLimiteOption,
        Integer joursRestants,
        SuccessionBeAcceptationRenonciationCalculator.DelaiStatutBe delaiStatut,
        List<String> actionsConcretes,
        List<SuccessionBeAcceptationRenonciationCalculator.RisqueSuccessionBe> risques,
        List<String> basesJuridiques,
        List<String> messages,
        String country
) {}
