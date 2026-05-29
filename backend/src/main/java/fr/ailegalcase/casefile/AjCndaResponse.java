package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-214-19 : réponse de l'analyse d'éligibilité à l'aide juridictionnelle (AJ)
 * devant la CNDA et des délais (loi n° 91-647 ; L. 532-4 CESEDA). Outil single-country FR.
 */
public record AjCndaResponse(
        UUID caseFileId,
        LocalDate dateDecisionOFPRA,
        double ressourcesMensuellesNettes,
        boolean procedureAcceleree,
        boolean demandeAJDeposee,
        LocalDate dateDepotAJ,
        boolean eligibleAJ,
        LocalDate dateEcheanceRecoursCNDA,
        LocalDate dateEcheanceDemandeAJ,
        boolean procedureAccelereeDureeReduite,
        AjCndaStatut statut,
        List<String> piecesAJ,
        String recommandation,
        String country,
        String baseJuridique
) {}
