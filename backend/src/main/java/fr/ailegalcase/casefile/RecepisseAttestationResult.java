package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-214-15 : résultat de l'analyse récépissé vs attestation de prolongation
 * R. 311-4 / R. 311-6 CESEDA. Outil single-country FR.
 *
 * <p>Les dates sont sérialisées en String ISO ({@code yyyy-MM-dd}) pour rester
 * stables dans {@code result_data} (JSON).</p>
 */
public record RecepisseAttestationResult(
        String typeDocument,
        String dateDelivrance,
        String dateExpiration,
        Boolean mentionAutorisationTravail,
        boolean droitSejour,
        boolean droitTravail,
        Long dureeValiditeJours,
        boolean risqueEmployeur,
        List<String> recommandations,
        String baseJuridique
) {}
