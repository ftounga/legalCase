package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-13-01 : requête d'analyse de révision post-divorce FR.
 *
 * <p>Tous les champs sont optionnels au niveau record — le service applique les
 * validations contractuelles ({@code typeRevision} requis, dates non futures,
 * valeurs numériques positives).</p>
 */
public record RevisionsPostDivorceRequest(
        String typeRevision,
        LocalDate dateDecisionInitiale,
        String changementCirconstance,
        BigDecimal revenusInitialsDebiteurEur,
        BigDecimal revenusActuelsDebiteurEur,
        BigDecimal revenusInitialsCreancierEur,
        BigDecimal revenusActuelsCreancierEur,
        Integer nbEnfantsACharge,
        List<Integer> ageEnfants,
        String modeResidenceActuel,
        String modeResidenceDemande,
        Boolean informationPrealable,
        Integer distanceDemenagementKm
) {}
