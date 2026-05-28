package fr.ailegalcase.casefile;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-215-11 : body de la requête POST
 * {@code /api/v1/case-files/{id}/aesm-mena-be-analysis}.
 *
 * <p>Outil composite 2 volets — tutelle DGDE (Loi 04/05/2007) + AESM (loi 15/12/1980
 * art. 9bis adapté MENA + circulaire OE 15/09/2005). Mineurs uniquement : la
 * validation supplémentaire {@code ageActuel < 18} est portée par
 * {@link AesmMenaBeCalculator}.
 *
 * <p>Le champ {@code dureeScolaire} (en mois) est requis si {@code integrationScolaire
 * = true} — sinon il peut être null.
 */
public record AesmMenaBeRequest(
        @NotNull @Min(0) @Max(17) Integer ageActuel,
        @NotNull
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateArriveeBelgique,
        @NotNull Boolean tuteurDesigne,
        @NotNull Boolean integrationScolaire,
        @Min(0) @Max(120) Integer dureeScolaire,
        @NotNull Boolean projetVieElabore,
        @NotNull Boolean perspectiveAutonomie,
        @NotNull Boolean menaceOrdrePublic
) {}
