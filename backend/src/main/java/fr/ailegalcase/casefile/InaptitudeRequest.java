package fr.ailegalcase.casefile;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-DT-15-01 : requête pour le calcul d'indemnité de licenciement pour inaptitude.
 *
 * <p>Le pays n'est pas transmis dans le body — il est dérivé de
 * {@code caseFile.getWorkspace().getCountry()} côté service.</p>
 *
 * @param salaireMensuelReference salaire mensuel de référence (>0)
 * @param ancienneteAnnees        ancienneté du salarié en années (≥ 0)
 * @param origineInaptitude       origine (enum {@link InaptitudeCalculator.OrigineInaptitude})
 * @param reclassementRespecte    l'employeur a-t-il respecté l'obligation de reclassement
 * @param avisMedecinTravailDate  date de l'avis du médecin du travail (nullable, format ISO)
 */
public record InaptitudeRequest(
        BigDecimal salaireMensuelReference,
        Integer ancienneteAnnees,
        InaptitudeCalculator.OrigineInaptitude origineInaptitude,
        Boolean reclassementRespecte,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate avisMedecinTravailDate
) {}
