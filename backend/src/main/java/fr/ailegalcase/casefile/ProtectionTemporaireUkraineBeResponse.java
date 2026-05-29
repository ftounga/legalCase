package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-215-19 : payload de réponse HTTP pour
 * {@code /api/v1/case-files/{id}/protection-temporaire-ukraine-be-analysis}.
 */
public record ProtectionTemporaireUkraineBeResponse(
        UUID caseFileId,
        LocalDate dateArrivee,
        boolean nationaliteUkrainienne,
        boolean residenceUkraineAvant24Fev2022,
        boolean apatridesUkraine,
        boolean membreFamilleProtege,
        ProtectionTemporaireUkraineBeTitreSejourEnum titreSejourBE,
        boolean eligible,
        LocalDate dateFinProtection,
        long dureeProtectionRestante,
        boolean prochainRenouvellement,
        String droitsTravail,
        List<String> droitsAides,
        List<String> cheminProcedure,
        String recommandation,
        String baseJuridique
) {}
