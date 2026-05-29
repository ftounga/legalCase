package fr.ailegalcase.casefile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SF-214-11 : réponse de l'analyse du calcul de présence prouvée en France et
 * de l'éligibilité aux 4 voies AES (L. 435-1 / L. 435-3 CESEDA).
 */
public record AesPresenceProuveeResponse(
        UUID caseFileId,
        String country,
        List<AesPresenceProuveeResult.PeriodeNormalisee> periodesNormalisees,
        List<AesPresenceProuveeResult.PeriodeNormalisee> periodesFusionnees,
        int moisTotauxProuves,
        int anneesTotalesProuvees,
        Map<String, Boolean> eligibiliteParVoie,
        List<AesPresenceProuveeResult.Gap> gapsPeriodes,
        List<String> recommandationsPieces,
        String baseJuridique
) {}
