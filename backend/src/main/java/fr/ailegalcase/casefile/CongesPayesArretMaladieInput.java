package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-206-03 : input du calcul de rappel de congés payés acquis pendant un
 * arrêt maladie (FRANCE).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans l'input.
 * {@link #dateCalcul()} permet d'injecter une horloge dans les tests
 * (ACTION_FORCLOSE déterministe) ; en production le service passe
 * {@code LocalDate.now()} ou laisse {@code null} (le calculator prend
 * {@code now()} à défaut).</p>
 */
public record CongesPayesArretMaladieInput(
        CongesPayesArretMaladieCalculator.TypeArret typeArret,
        Integer nombreMoisArret,
        Boolean salarieEncoreEnPoste,
        LocalDate dateRuptureContrat,
        BigDecimal joursCpDejaAccordes,
        BigDecimal salaireBrutMensuel,
        LocalDate dateCalcul
) {

    /** Builder utilitaire — facilite la lisibilité des tests unitaires. */
    public static class Builder {
        private CongesPayesArretMaladieCalculator.TypeArret typeArret;
        private Integer nombreMoisArret;
        private Boolean salarieEncoreEnPoste;
        private LocalDate dateRuptureContrat;
        private BigDecimal joursCpDejaAccordes;
        private BigDecimal salaireBrutMensuel;
        private LocalDate dateCalcul;

        public Builder typeArret(CongesPayesArretMaladieCalculator.TypeArret v) { this.typeArret = v; return this; }
        public Builder nombreMoisArret(Integer v) { this.nombreMoisArret = v; return this; }
        public Builder salarieEncoreEnPoste(Boolean v) { this.salarieEncoreEnPoste = v; return this; }
        public Builder dateRuptureContrat(LocalDate v) { this.dateRuptureContrat = v; return this; }
        public Builder joursCpDejaAccordes(BigDecimal v) { this.joursCpDejaAccordes = v; return this; }
        public Builder salaireBrutMensuel(BigDecimal v) { this.salaireBrutMensuel = v; return this; }
        public Builder dateCalcul(LocalDate v) { this.dateCalcul = v; return this; }

        public CongesPayesArretMaladieInput build() {
            return new CongesPayesArretMaladieInput(
                    typeArret,
                    nombreMoisArret,
                    salarieEncoreEnPoste,
                    dateRuptureContrat,
                    joursCpDejaAccordes,
                    salaireBrutMensuel,
                    dateCalcul
            );
        }
    }
}
