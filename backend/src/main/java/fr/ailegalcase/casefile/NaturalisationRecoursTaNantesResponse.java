package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-214-31 : réponse de l'analyse du délai de recours devant le Tribunal
 * administratif de Nantes contre un refus de naturalisation par décret
 * (CJA L. 213-1, délai 2 mois ; Cciv 21-15). Outil single-country FR.
 */
public record NaturalisationRecoursTaNantesResponse(
        UUID caseFileId,
        LocalDate dateRefusDecret,
        String motivationRefus,
        boolean recoursPrerequis,
        LocalDate dateEcheanceRecoursTa,
        long joursRestants,
        String tribunalCompetent,
        List<String> basesJuridiques,
        List<String> motifsRecoursDisponibles,
        NaturalisationRecoursTaNantesStatut statut,
        String messagePrescription,
        String recommandation,
        String country
) {}
