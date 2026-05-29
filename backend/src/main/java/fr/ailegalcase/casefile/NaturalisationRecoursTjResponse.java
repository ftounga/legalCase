package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-214-29 : réponse de l'analyse du délai de recours devant le Tribunal
 * judiciaire contre un refus de déclaration de nationalité française (Cciv 26-3).
 * Outil single-country FR.
 */
public record NaturalisationRecoursTjResponse(
        UUID caseFileId,
        NaturalisationRecoursTjVoieEnum voieNaturalisation,
        LocalDate dateRefusDeclaration,
        NaturalisationRecoursTjTypeRefusEnum typeRefus,
        LocalDate dateEcheanceRecoursJudicaire,
        long joursRestants,
        String tribunalCompetent,
        List<String> basesJuridiques,
        List<String> motifsRecoursDisponibles,
        NaturalisationRecoursTjStatut statut,
        String messagePrescription,
        String recommandation,
        String country
) {}
