package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-208-03 : résultat de l'analyse du recours hiérarchique devant la Commission
 * de recours contre les refus de visa (CRRV) — CESEDA L.312-1 à L.312-3.
 *
 * <p>Outil <b>single-country FR</b> (CRRV = institution française, pas d'équivalent BE).
 */
public record CrrvRefusVisaResult(
        LocalDate dateNotificationRefus,
        String typeVisa,
        String motifRefus,
        boolean recoursForme,
        LocalDate dateRecours,
        LocalDate dateExpirationRecoursCrrv,
        long joursRestants,
        String statut,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
