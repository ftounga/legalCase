package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-206-03 : requête HTTP pour l'endpoint de chiffrage du rappel de congés
 * payés acquis pendant un arrêt maladie (FRANCE).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.
 * La date du calcul est toujours {@code LocalDate.now()} côté service (les
 * tests qui veulent forcer une date passent par le calculator directement).</p>
 */
public record CongesPayesArretMaladieRequest(
        CongesPayesArretMaladieCalculator.TypeArret typeArret,
        Integer nombreMoisArret,
        Boolean salarieEncoreEnPoste,
        LocalDate dateRuptureContrat,
        BigDecimal joursCpDejaAccordes,
        BigDecimal salaireBrutMensuel
) {

    CongesPayesArretMaladieInput toInput() {
        return new CongesPayesArretMaladieInput(
                typeArret,
                nombreMoisArret,
                salarieEncoreEnPoste,
                dateRuptureContrat,
                joursCpDejaAccordes,
                salaireBrutMensuel,
                null /* dateCalcul — defaults to LocalDate.now() in calculator */
        );
    }
}
