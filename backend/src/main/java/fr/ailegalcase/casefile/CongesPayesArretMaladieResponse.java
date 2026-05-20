package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-206-03 : réponse de l'endpoint de chiffrage du rappel de congés payés
 * acquis pendant un arrêt maladie (FRANCE).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI) ET les
 * sorties calculées (jours acquis, jours rappel, valorisation, date limite,
 * verdict, bases juridiques).</p>
 */
public record CongesPayesArretMaladieResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        CongesPayesArretMaladieCalculator.TypeArret typeArret,
        Integer nombreMoisArret,
        Boolean salarieEncoreEnPoste,
        LocalDate dateRuptureContrat,
        BigDecimal joursCpDejaAccordes,
        BigDecimal salaireBrutMensuel,
        // --- Outputs calculés ---
        CongesPayesArretMaladieCalculator.Verdict verdict,
        BigDecimal joursCpAcquis,
        BigDecimal joursCpRappel,
        BigDecimal valorisationIndicativeEur,
        LocalDate dateLimiteAction,
        boolean actionEncoreOuverte,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
