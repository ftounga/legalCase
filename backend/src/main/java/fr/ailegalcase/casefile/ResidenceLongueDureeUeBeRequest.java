package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-221-03 : body de la requête POST
 * {@code /api/v1/case-files/{id}/residence-longue-duree-ue-be-analysis}.
 *
 * <p>Éligibilité au statut de RÉSIDENT LONGUE DURÉE UE (art. 15bis Loi 15/12/1980,
 * directive 2003/109/CE) : 5 ans (= 60 mois) de séjour légal ininterrompu + ressources
 * stables et suffisantes + assurance maladie + condition d'intégration, sans absences
 * hors UE excessives.
 */
public record ResidenceLongueDureeUeBeRequest(
        @NotNull LocalDate dateDebutSejourLegal,
        @NotNull Boolean sejourLegalIninterrompu,
        @NotNull Boolean ressourcesStablesSuffisantes,
        @NotNull Boolean assuranceMaladie,
        @NotNull Boolean conditionIntegrationRemplie,
        @NotNull Boolean absencesHorsUeExcessives
) {}
