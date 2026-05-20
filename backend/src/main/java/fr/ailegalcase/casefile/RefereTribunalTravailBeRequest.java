package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-207-05 : payload de création/upsert d'une analyse d'éligibilité au
 * référé devant le président du tribunal du travail belge (CJ art. 584).
 *
 * <p>Bean Validation appliquée côté contrôleur (400 si incomplet). Les règles
 * de cohérence inter-champs (date future, antériorité démarche amiable) sont
 * portées par le service ({@link RefereTribunalTravailBeService}).</p>
 *
 * <p>Pattern mirroré de {@link C4OnemChecklistRequest} (SF-207-02) et
 * {@link AtFedrisDeclarationRequest} (SF-207-04) — record + Bean Validation
 * + dates ISO LocalDate.</p>
 *
 * @see RefereTribunalTravailBeCalculator
 */
public record RefereTribunalTravailBeRequest(
        @NotNull(message = "motifUrgence est requis")
        RefereTribunalTravailBeMotifUrgence motifUrgence,

        @NotBlank(message = "motifUrgenceDescription est requise")
        String motifUrgenceDescription,

        @NotNull(message = "dateFaitGenerateur est requise")
        LocalDate dateFaitGenerateur,

        /** Optionnel — date d'une démarche amiable préalable (mise en demeure, courrier, etc.). */
        LocalDate dateDemarcheAmiable,

        @NotNull(message = "preuveUrgenceJointe est requis (boolean)")
        Boolean preuveUrgenceJointe,

        @NotBlank(message = "mesureProvisoireDemandee est requise")
        String mesureProvisoireDemandee,

        @NotNull(message = "perilEnDemeure est requis (boolean)")
        Boolean perilEnDemeure,

        @NotNull(message = "competenceTerritorialeIdentifiee est requis (boolean)")
        Boolean competenceTerritorialeIdentifiee
) {}
