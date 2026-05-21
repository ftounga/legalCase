package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-217-14 : résultat structuré du moteur décisionnel BE de protection du
 * majeur incapable (loi du 17/03/2013).
 *
 * <p>{@code juridictionCompetente} peut être {@code null} pour
 * {@link ProtectionMajeurBeCalculator.ProtectionMajeurBeVerdict#MANDAT_EXTRA_JUDICIAIRE_VALABLE}
 * (voie privée — pas de saisine) ou
 * {@link ProtectionMajeurBeCalculator.ProtectionMajeurBeVerdict#QUALIFICATION_INCOMPLETE}.</p>
 */
public record ProtectionMajeurBeResult(
        ProtectionMajeurBeCalculator.ProtectionMajeurBeVerdict verdict,
        ProtectionMajeurBeCalculator.MesureProtectionBe mesureRecommandee,
        ProtectionMajeurBeCalculator.JuridictionProtectionBe juridictionCompetente,
        String fondementProcedural,
        List<ProtectionMajeurBeCalculator.ActeProtegeBe> actesProteges,
        List<String> actionsConcretes,
        List<String> basesJuridiques,
        List<String> messages,
        String country
) {}
