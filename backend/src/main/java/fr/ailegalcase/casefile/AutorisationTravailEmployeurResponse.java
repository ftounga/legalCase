package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-214-43 : réponse de l'analyse des obligations de l'employeur recrutant un
 * travailleur étranger hors UE (autorisation de travail, L. 5221-1 Code du
 * travail). Outil single-country FR.
 */
public record AutorisationTravailEmployeurResponse(
        UUID caseFileId,
        AutorisationTravailEmployeurTypeContrat typeContrat,
        String posteProposes,
        String nationaliteCandidat,
        Integer dureeContratMois,
        boolean autorisationRequise,
        List<String> obligationsDemande,
        Integer delaiInstructionOFIIMois,
        String taxeOFII,
        boolean refusAutorisation,
        LocalDate dateRefusAutorisation,
        boolean recoursPossible,
        LocalDate delaiRecoursTa,
        AutorisationTravailEmployeurStatut statut,
        String recommandation,
        String country,
        String baseJuridique
) {}
